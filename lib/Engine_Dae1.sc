Engine_Dae1 : CroneEngine {

	var <params;
	var synth, winenv, winenv2, winenv3, winenv4, z, y, w, v;

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {

		/*SynthDef(\Dae1, {
			arg rate = 1, recLoop = 1, outBus = 0, start = 0, selRate = 0.5, in_db = -6.0;
			var snd, ptr, in, rec, frames, buf;

			buf = Buffer.alloc(context.server, context.server.sampleRate* 2, 1);

			in = Mix.ar(SoundIn.ar([0,1],1)) * in_db.dbamp;

			frames = BufFrames.kr(buf.bufnum);

			ptr = Phasor.ar(start, BufRateScale.kr(buf.bufnum) * rate, 0, frames);

			SendReply.kr(Impulse.kr(1),'/ptrVal',values:ptr, replyID:99);
			//ptr.range(0,1).scope;

			rec = BufWr.ar(in,buf.bufnum,ptr,recLoop);

			//SendReply.kr(Impulse.kr(10),'/ptrVal',values: Amplitude.kr(rec), replyID: 66);

			snd = BufRd.ar(1, buf.bufnum, SelectX.ar((LFNoise0.kr(selRate)>0.5),[ptr,(-1*ptr)]));

			// LocalOut.ar();
			//b.plot;
			Out.ar(outBus, snd.tanh);

		}).add;*/

		///////////////////////////////////////\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\



		SynthDef(\DaemonBuf, {

			arg decimator = 0, frames = 0, brick = 1.5,

			in_db = -6.0,
			overdub_db = -12.0,
			rec_db = -6.0,
			grain_db = -3.0,
			loop_db = -3.0,
			direct_db = -3.0,

			burstRate = 1, grainRate = 5,
			rateRec = 1, rateRecLag = 0, start = 0, recLoop = 1, playLoop = 1,

			outBus = 0;
			var grainz, play, recz, mix, direct,recIn, local, rTrigz, gTrig, ptrRec, ptrPlay, max1, grainz_PV, buf;

			/////
			winenv = Env([0, 1, 0], [0.001, 0.05], [1, -8]);
			z = Buffer.sendCollection(context.server, winenv.discretize, 1);

			winenv2 = Env([0, 1, 0], [0.3, 0.05], [1, -8]);
			y = Buffer.sendCollection(context.server, winenv2.discretize, 1);

			winenv3 = Env([0, 1, 0.9, 0.8, 0], [0.00001, 0.002, 0.05, 0.01], [1,-1,]);
			w = Buffer.sendCollection(context.server,winenv3.discretize,1);

			winenv4 = Env([0, 1, 0], [0.3, 0.05, 0.5], [1, -8]);
			v = Buffer.sendCollection(context.server,winenv4.discretize,1);
			//////

			buf = Buffer.alloc(context.server, context.server.sampleRate* 8, 1);

			// direct = In.ar(\inBus.ar(~input01),1);
			direct = Mix.ar(SoundIn.ar([0,1],1));


			frames = buf.numFrames;

			local = LocalIn.ar(2);

			recIn = (direct * in_db.dbamp) + (local* overdub_db.dbamp); // receive the audio from the mix

			rTrigz = Trig1.ar(Dust.kr(burstRate-0.2));
			gTrig = Trig1.ar(Dust.kr(grainRate-0.2));

			//TODO: How to modify the playback position

			ptrRec = Phasor.ar(
				trig: \trigRec.tr(1),
				rate: BufRateScale.kr(buf.bufnum)*rateRec.lag(rateRecLag),
				start: 0,
				end: frames,
				resetPos: start / frames);

			ptrPlay = Phasor.ar(
				trig: \trigPlay.tr(1),
				rate: BufRateScale.kr(buf.bufnum)*\ratePlay.kr(0.5).lag(\ratePlayLag.kr(0)),
				start: 0,
				end: frames,
				resetPos: start / frames);

			SendReply.kr(Impulse.kr(18), '/ptrs', [ptrRec/frames, ptrPlay/frames], 69);

			recz = BufWr.ar(
				inputArray: recIn.sum * rec_db, //audioIn
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

			Out.ar(outBus, mix.tanh);
		}).add;


		context.server.sync;

		synth = Synth(\DaemonBuf, [\outBus, context.out_b.index], target: context.xg);

		/*params = Dictionary.newFrom([
		\rate, 1,
		\recLoop, 1,
		\outBus, 0,
		\selRate, 0.5
		]);*/


		/*		this.addCommand("rate", "i", {|msg|
		synth.set(\rate, msg[1]);
		});

		this.addCommand("recLoop", "i", {|msg|
		synth.set(\recLoop, msg[1]);
		});

		this.addCommand("selRate", "i", {|msg|
		synth.set(\selRate, msg[1]);
		});

		this.addCommand("in_db", "i", {|msg|
		synth.set(\in_db, msg[1]);
		});*/



		/*	decimator = 0, frames = 0, brick = 1.5,

		in_db = -6.0,
		overdub_db = -12.0,
		rec_db = -6.0,
		grain_db = -3.0,
		loop_db = -3.0,
		direct_db = -3.0,

		burstRate = 1, grainRate = 5,
		rateRec = 1, rateRecLag = 0, start = 0, recLoop = 1, playLoop = 1,

		outBus = 0;*/

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

		this.addCommand("start", "f", {|msg|
			synth.set(\start, msg[1]);
		});

		this.addCommand("recLoop", "f", {|msg|
			synth.set(\recLoop, msg[1]);
		});

		this.addCommand("playLoop", "f", {|msg|
			synth.set(\playLoop, msg[1]);
		});



		free {
			synth.free;

	}}
}