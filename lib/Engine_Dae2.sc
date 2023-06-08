Engine_Dae2 : CroneEngine {

	var <params;
	var synth, winenv, winenv2, winenv3, winenv4, z, y, w, v, compGain;
	var oscs;

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {
		///////////////////////////////////////\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
        // TODO: ADD OUT VOLUME.... CHECK WHAT'S GOING ON!
		// version 1.4 04.06.23

		// added master db
		// added compressor
		oscs = Dictionary.new();

		oscs.put("position",OSCFunc({ 
			arg msg;
				 NetAddr("127.0.0.1", 10111).sendMsg("progress",msg[3]); 
				//msg.postln;
		}, '/position'));


		SynthDef(\DaemonBuf, {

			arg decimator = 0, frames, brick = 1.5,

			in_db = -6.0,
			overdub_db = -60.0,
			rec_db = -60.0,
			grain_db = -60.0,
			loop_db = -60.0,
			direct_db = -3.0,
			master_db = -6.0,

			burstRate = 1, grainRate = 5,
			rateRec = 1, rateRecLag = 0, start = 0, recLoop = 1,
			ratePlay = 1,ratePlayLag = 2, playLoop = 1,

			//trigRec = 0, trigPlay = 0, //as named controls

			outBus = 0;
			var grainz, play, recz, mix, direct,recIn, local, rTrigz, gTrig, ptrRec, ptrPlay, max1, grainz_PV, buf;

			////// Windows for grain
			winenv = Env([0, 1, 0], [0.001, 0.05], [1, -8]);
			z = Buffer.sendCollection(context.server, winenv.discretize, 1);

			winenv2 = Env([0, 1, 0], [0.3, 0.05], [1, -8]);
			y = Buffer.sendCollection(context.server, winenv2.discretize, 1);

			winenv3 = Env([0, 1, 0.9, 0.8, 0], [0.00001, 0.002, 0.05, 0.01], [1,-1,]);
			w = Buffer.sendCollection(context.server,winenv3.discretize,1);

			winenv4 = Env([0, 1, 0], [0.3, 0.05, 0.5], [1, -8]);
			v = Buffer.sendCollection(context.server,winenv4.discretize,1);


			/////// compressor from nathan.ho.name

			compGain = { |in, threshold, slope, attack = 0.05, release = 0.3|
				var inArray, amplitude;
				inArray = if(in.isArray) { in } { [in] };
				amplitude = (Amplitude.ar(inArray, attack, release).sum / inArray.size.sqrt).max(-100.dbamp).ampdb;
				((amplitude - threshold).max(0) * (slope - 1)).lag(attack).dbamp;
			};

			//////

			buf = Buffer.alloc(context.server, context.server.sampleRate* 8, 1);

			// direct = In.ar(\inBus.ar(~input01),1);
			direct = Mix.ar(SoundIn.ar([0,1],1));


			frames = buf.numFrames;

			local = LocalIn.ar(2);

			recIn = (direct * in_db.dbamp); // receive the audio from the mix
			recIn = recIn + (local* overdub_db.dbamp); // mix it with the overdub stuff

			rTrigz = Trig1.ar(Dust.kr(burstRate-0.2));
			gTrig = Trig1.ar(Dust.kr(grainRate-0.2));

			//TODO: How to modify the playback position

			ptrRec = Phasor.ar(
				trig: \trigRec.tr(0),
				rate: BufRateScale.kr(buf.bufnum)*rateRec.lag(rateRecLag),
				start: 0,
				end: frames,
				resetPos: start / frames);

			ptrPlay = Phasor.ar(
				trig: \trigPlay.tr(1),
				rate: BufRateScale.kr(buf.bufnum)*ratePlay.lag(ratePlayLag),
				start: 0,
				end: frames,
				resetPos: start / frames);

			//SendReply.kr(Impulse.kr(1),'/ptrVal',values: [Amplitude.kr(rec), Amplitude.kr(in)], replyID: 66);

			SendReply.kr(Impulse.kr(18), '/position', [ptrRec/frames, ptrPlay/frames], 69);

			recz = BufWr.ar(
				inputArray: recIn.sum * rec_db.dbamp, //audioIn
				bufnum: buf.bufnum,
				phase: ptrRec,
				loop: recLoop
			);
			0.0;

			play = BufRd.ar(
				numChannels: 1,
				bufnum: buf.bufnum,
				phase: ptrPlay,
				loop: playLoop
			);

			grainz = GrainBuf.ar(
				numChannels: 1,
				trigger: gTrig,
				dur: \durGrain.kr(0.2),
				sndbuf: buf,
				rate: TChoose.kr(gTrig, [2,4,1.3333,0.25,0.5, 1])*TChoose.kr(gTrig,[1, 1, 1, 0, -1, -1]),
				envbufnum: TChoose.kr(gTrig, [z,y,w,v])
			);

			// PhaseVocoder Brickwall filter
			grainz_PV = FFT(LocalBuf(2048), grainz);
			grainz_PV = PV_BrickWall(grainz_PV, TRand.kr(-1,1,gTrig));
			grainz_PV = Pan2.ar(IFFT(grainz_PV),TRand.kr(-1,1,gTrig));



			grainz = ((brick<1)*grainz) + ((brick>1)*grainz_PV);

			mix=(grainz * grain_db.dbamp) + (play * loop_db.dbamp) + (direct * direct_db.dbamp);

			// effects

			mix =  ((decimator<1)*mix)+((decimator>1)*Decimator.ar(mix, TChoose.kr(rTrigz, [44100,8000, 16000, 12000, 6000, 35000,2]).lag(0), decimator));

			mix = LeakDC.ar(mix);

			LocalOut.ar(mix);

			mix = mix * compGain.(mix, threshold: -15, slope: -2, attack: 0.1, release: 0.3);
			
			mix = mix * master_db.dbamp; 

			Out.ar(outBus, mix);
		}).add;


		context.server.sync;

		synth = Synth(\DaemonBuf, [\outBus, context.out_b.index], target: context.xg);

	
		
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

		this.addCommand("loop_db", "f", {|msg|
			synth.set(\loop_db, msg[1]);
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

	}}
} 