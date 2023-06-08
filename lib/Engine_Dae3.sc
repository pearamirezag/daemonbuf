Engine_Dae3 : CroneEngine {
	
	var synth, bufs,o;
	
	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}
	
	alloc {
		///////////////////////////////////////\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
		
		// version 1.5 06.06.23
		
		//changed how the buffer is allocated by copying the method in AmenBreak1
		
		bufs = Dictionary.new();
		
		bufs.put("daeTape",Buffer.alloc(context.server, context.server.sampleRate * 8.0, 1));

		// o = OSCFunc({ arg msg;
		// 	//msg.postln;
			
		// 	// if(msg[2]==66,{NetAddr("127.0.0.1",10111).sendMsg("ptr1",1,  msg[3]);},{});
		// 	if(msg[2]==55,{NetAddr("127.0.0.1",10111).sendMsg("snd_Signal",1,  msg[3]);},{});
		// 	if(msg[2]==66,{NetAddr("127.0.0.1",10111).sendMsg("pointer",1,  msg[3]);},{});
		// 	// if(msg[2]==99,{NetAddr("127.0.0.1",10111).sendMsg("ptr2",1,  msg[3]);},{});
			
		// },'/ptrVal'); 
		
		context.server.sync;

		
			/*
			var buf;
			buf = Buffer.alloc(s, s.sampleRate * 2, 1);*/
			
			

				SynthDef(\test01, {
					arg buf = 0;
					var snd, ptr, ptrP, direct, play, rec, bufR;
				
					ptr = Phasor.ar(\trig.tr(1), BufRateScale.kr(buf) * \rate.kr(1),0,BufFrames.ir(buf));
				
					ptrP = Phasor.ar(\trigP.tr(1), BufRateScale.kr(buf) * \rateP.kr(1),0,BufFrames.ir(buf));
				
				
					// ptr = Gate.ar(ptr,\trig.tr(1));
				
					//SendReply.kr(Impulse.kr(10),'/ptrVal',values:ptr/BufFrames.ir(buf), replyID:99);
					snd = Silent.ar();
				
					direct = SoundIn.ar([0,1],1).sum;
				
					rec = BufWr.ar(direct,buf,ptr,loop:1);
					// rec = rec * 0.0;
				
					bufR = BufRd.ar(
						numChannels:1,
						bufnum: buf,
						phase: ptrP,
						loop: \loop.kr(1)
					);
				
					play = PlayBuf.ar(
						numChannels:1,
						bufnum: buf,
						rate: \rate1.kr(1),
						trigger: \trig1.tr(0),
						loop: \loop1.kr(1)
					);
				
					// SendReply.kr(Impulse.kr(10),'/ptrVal',values:Amplitude.kr(bufR), replyID:55);
				
					// SendReply.kr(Impulse.kr(10),'/ptrVal',values:ptrP/BufFrames.ir(buf), replyID:66);
				
				
					snd = snd + direct * \direct_db.kr(-12).dbamp;
					snd = snd + play * \play_db.kr(-12).dbamp;
					snd = snd + bufR * \bufR_db.kr(-12).dbamp;
				
					Out.ar(\outBus.kr(0),snd)
				}
				).add;
				
				
			
		
		// ).send(s);
		
		context.server.sync;
		
		
		synth = Synth(\test01, [\buf, bufs.at("daeTape"), \outBus, context.out_b.index ], target: context.xg);

		// synth = Synth(\DaemonBuf, [\outBus, context.out_b.index], target: context.xg);

		

	
		
		this.addCommand("trig", "i", {|msg|
			synth.set(\trig, msg[1]);
			//msg[1].postln;
		});

		this.addCommand("trig1", "i", {|msg|
			synth.set(\trig1, msg[1]);
			//msg[1].postln;
		});

		this.addCommand("trigP", "i", {|msg|
			synth.set(\trigP, msg[1]);
			//msg[1].postln;
		});
		
			
		this.addCommand("rate", "f", {|msg|
			synth.set(\rate, msg[1]);
			//msg[1].postln;
		});

		this.addCommand("rate1", "f", {|msg|
			synth.set(\rate1, msg[1]);
			//msg[1].postln;
		});

		this.addCommand("rateP", "f", {|msg|
			synth.set(\rateP, msg[1]);
			//msg[1].postln;
		});
		
		this.addCommand("loop", "f", {|msg|
			synth.set(\loop, msg[1]);
			//msg[1].postln;
		});

		this.addCommand("loop1", "f", {|msg|
			synth.set(\loop1, msg[1]);
			//msg[1].postln;
		});

		this.addCommand("direct_db", "f", {|msg|
			synth.set(\direct_db, msg[1]);
			//msg[1].postln;
		});

		this.addCommand("play_db", "f", {|msg|
			synth.set(\play_db, msg[1]);
			//msg[1].postln;
		});

		this.addCommand("bufR_db", "f", {|msg|
			synth.set(\bufR_db, msg[1]);
			//msg[1].postln;
		});
		
		free {
			synth.free;
			//Buffer.freeAll;

			bufs.keysValuesDo({ arg buf, val;
				val.free;
			});
			
		} //free
	} // alloc
} //Engine