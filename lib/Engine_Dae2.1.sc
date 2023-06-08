Engine_Dae21 : CroneEngine {

	// var <params;
	var synth, winenv, winenv2, winenv3, winenv4, z, y, w, v, compGain,bufs;
	var oscs;

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {

        var n, mu, unit, expandCurve, compressCurve;

        n = 512*2;
        mu = 255*2;
        unit = Array.fill(n, {|i| i.linlin(0, n-1, -1, 1) });

        compressCurve = unit.collect({ |x|
            x.sign * log(1 + mu * x.abs) / log(1 + mu);
        });
		

        bufs = Dictionary.new();
		oscs = Dictionary.new();

        bufs.put("daeTape",Buffer.alloc(context.server, context.server.sampleRate * 8.0, 1));
        bufs.put("sine",Buffer.alloc(context.server,512,1));
        bufs.put("compress",Buffer.loadCollection(context.server,Signal.newFrom(compressCurve).asWavetableNoWrap));
        bufs.at("sine").sine2([2],[0.5],false); // https://ableton-production.imgix.net/manual/en/Saturator.png?auto=compress%2Cformat&w=716

		oscs.put("position",OSCFunc({ 
			arg msg;
				if(msg[2]==69,{NetAddr("127.0.0.1",10111).sendMsg("position",1,  msg[3]);},{});
                if(msg[2]==66,{NetAddr("127.0.0.1",10111).sendMsg("snd_Signal",1,  msg[3]);},{});
				//msg.postln;    
		}, '/position'));

        context.server.sync;

		SynthDef(\DaemonBuf, {

			arg buf = 0,decimator = 0, frames, brick = 1.5, in_db = -6.0, overdub_db = -inf,
            rec_db = -6.0, grain_db = -6.0, play_db = -6.0, direct_db = -24.0, master_db = -3.0,
            burstRate = 1, grainRate = 5, rateRec = 1, rateRecLag = 0, start = 0, recLoop = 1,
            ratePlay = 1,ratePlayLag = 2, playLoop = 1, outBus = 0,
            compress_curve_wet=0,compress_curve_drive=1,bufCompress = 0;
			var snd, grainz, play, recz, direct, recIn, local, rTrigz, gTrig, ptrRec, ptrPlay, max1, grainz_PV;

			/////// compressor from nathan.ho.name

			compGain = { |in, threshold, slope, attack = 0.05, release = 0.3|
				var inArray, amplitude;
				inArray = if(in.isArray) { in } { [in] };
				amplitude = (Amplitude.ar(inArray, attack, release).sum / inArray.size.sqrt).max(-100.dbamp).ampdb;
				((amplitude - threshold).max(0) * (slope - 1)).lag(attack).dbamp;
			};

			//////

             ////// Windows for grain
			winenv = Env([0, 1, 0], [0.001, 0.05], [1, -8]);
			z = Buffer.sendCollection(context.server, winenv.discretize, 1);

			winenv2 = Env([0, 1, 0], [0.3, 0.05], [1, -8]);
			y = Buffer.sendCollection(context.server, winenv2.discretize, 1);

			winenv3 = Env([0, 1, 0.9, 0.8, 0], [0.00001, 0.002, 0.05, 0.01], [1,-1,]);
			w = Buffer.sendCollection(context.server,winenv3.discretize,1);

			winenv4 = Env([0, 1, 0], [0.3, 0.05, 0.5], [1, -8]);
			v = Buffer.sendCollection(context.server,winenv4.discretize,1);

            frames = BufFrames.ir(buf); //number of frames
            
            rTrigz = Trig1.ar(Dust.kr(burstRate-0.2));
			gTrig = Trig1.ar(Dust.kr(grainRate-0.2));

            /////////
            ////////

            snd = Silent.ar();

			// direct = Mix.ar(SoundIn.ar([0,1],1)); // Direct hardware input

            direct = SoundIn.ar([0,1],1); // Direct hardware input

			local = LocalIn.ar(2);

			recIn = (direct * in_db.dbamp); // receive the audio from the mix

			recIn = recIn + (local * overdub_db.dbamp); // mix it with the overdub stuff

			//TODO: How to modify the playback position

			ptrRec = Phasor.ar(
				trig: \trigRec.tr(1),
				rate: BufRateScale.kr(buf) * rateRec.lag(rateRecLag),
				start: 0,
				end: frames
				);

			ptrPlay = Phasor.ar(
				trig: \trigPlay.tr(1),
				rate: BufRateScale.kr(buf) * ratePlay.lag(ratePlayLag),
				start: 0,
				end: frames
                );

			

			SendReply.kr(Impulse.kr(10), '/position', [ptrRec/frames, ptrPlay/frames], 69);

			recz = BufWr.ar(
				inputArray: recIn.sum * rec_db.dbamp, //audioIn
				bufnum: buf,
				phase: ptrRec,
				loop: recLoop
			);

			play = BufRd.ar(
                numChannels: 1,
				bufnum: buf,
				phase: ptrPlay,
				loop: playLoop
			);

            SendReply.kr(Impulse.kr(10),'/position',values: [Amplitude.kr(play), Amplitude.kr(recz)], replyID: 66);

			grainz = GrainBuf.ar(
				numChannels: 1,
				trigger: gTrig,
				dur: \durGrain.kr(0.2),
				sndbuf: buf,
				rate: TChoose.kr(gTrig, [2,4,1.3333,0.25,0.5, 1]) * TChoose.kr(gTrig,[1, 1, 1, 0, -1, -1, 2, -2, -4, 0.333, 4]),
				envbufnum: TChoose.kr(gTrig, [z,y,w,v])
			);

		
			// snd = snd + (direct * direct_db.dbamp); // mix direct signal
            // snd = snd + (play * play_db.dbamp); // mix looper signal
            // snd = snd + (grainz * grain_db.dbamp); // mix grainz signal
            
            snd = snd + (play * play_db.dbamp) + (grainz * grain_db.dbamp);

			////// FX

            // PhaseVocoder Brickwall filter
            
			grainz_PV = FFT(LocalBuf(2048), grainz);
			grainz_PV = PV_BrickWall(grainz_PV, TRand.kr(-1,1,gTrig));
			grainz_PV = Pan2.ar(IFFT(grainz_PV),TRand.kr(-1,1,gTrig));

            grainz = ((brick<1)* grainz) + ((brick>1)*grainz_PV);

			snd =  ((decimator<1) * snd) + ((decimator>1) * Decimator.ar(snd, TChoose.kr(rTrigz, [44100,8000, 16000, 12000, 6000, 35000,2]).lag(0), decimator));
            
            snd = snd * -6.dbamp;

			//snd = LeakDC.ar(snd);

			// snd = compGain.(snd, threshold: -15, slope: -2, attack: 0.1, release: 0.3);
            
            // compress curve
            snd=SelectX.ar(Lag.kr(compress_curve_wet),[snd,Shaper.ar(bufCompress,snd*compress_curve_drive)]);


            LocalOut.ar(snd);

            snd = LeakDC.ar(snd);

            snd = snd + (direct * direct_db.dbamp);
			
			snd = snd * master_db.dbamp; 

			Out.ar(outBus, snd);
		}).add;


		context.server.sync;

		synth = Synth(\DaemonBuf, [\bufCompress, bufs.at("compress"), \buf, bufs.at("daeTape"), \outBus, context.out_b.index], target: context.xg);

        context.server.sync;
		
		this.addCommand("decimator", "f", {|msg|
			synth.set(\decimator, msg[1]);
		});

		this.addCommand("brick", "f", {|msg|
			synth.set(\brick, msg[1]);
		});

		this.addCommand("in_db", "f", {|msg|
			synth.set(\in_db, msg[1]);
		});


		this.addCommand("overdub_db", "f", {|msg|
			synth.set(\overdub_db, msg[1]);
		});


		this.addCommand("rec_db", "f", {|msg|
			synth.set(\rec_db, msg[1]);
		});

		this.addCommand("grain_db", "f", {|msg|
			synth.set(\grain_db, msg[1]);
		});

		this.addCommand("play_db", "f", {|msg|
			synth.set(\play_db, msg[1]);
		});

		this.addCommand("direct_db", "f", {|msg|
			synth.set(\direct_db, msg[1]);
		});

		this.addCommand("master_db", "f", {|msg|
			synth.set(\master_db, msg[1]);
		});

		this.addCommand("burstRate", "f", {|msg|
			synth.set(\burstRate, msg[1]);
		});

		this.addCommand("grainRate", "f", {|msg|
			synth.set(\grainRate, msg[1]);
		});

		this.addCommand("rateRec", "f", {|msg|
			synth.set(\rateRate, msg[1]);
		});

		this.addCommand("rateRecLag", "f", {|msg|
			synth.set(\rateRateLag, msg[1]);
		});

		this.addCommand("ratePlay", "f", {|msg|
			synth.set(\ratePlay, msg[1]);
		});

		this.addCommand("ratePlayLag", "f", {|msg|
			synth.set(\ratePlayLag, msg[1]);
		});

		this.addCommand("start", "f", {|msg|
			synth.set(\start, msg[1]);
		});

		this.addCommand("recLoop", "f", {|msg|
			synth.set(\recLoop, msg[1]);
		});

		this.addCommand("playLoop", "f", {|msg|
			synth.set(\playLoop, msg[1]);
		});

		this.addCommand("trigRec", "i", {|msg|
			synth.set(\trigRec, msg[1]);
		});

		this.addCommand("trigPlay", "i", {|msg|
			synth.set(\trigRec, msg[1]);
		});



		free {
            
			synth.free;
            bufs.keysValuesDo({ arg buf, val;
				val.free;
			});

	} // free
} // alloc

} 