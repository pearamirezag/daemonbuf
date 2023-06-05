engine.name='Dae2'

-- engine.brick(1.5)

-- engine.directLvl(0.4)
-- engine.grainLvl(-4)

--  engine.inLvl(1)


-- how to tell i'm receiving sound and that everything is starting as is???

-- a way to initialize the buffer playing and recording
-- nb. single or double quotes doesn't matter, just don't mix + match pairs!

s = require 'sequins'
-- see https://monome.org/docs/norns/reference/lib/sequins for more info

m = midi.connect()


function midi_to_hz(note)
  local hz = (440 / 32) * (2 ^ ((note -9) / 12))
  return hz
end




m.event = function(data)
  local d = midi.to_msg(data)
  if d.type == "note_on" then
    engine.amp(d.vel / 127)
    engine.hz(midi_to_hz(d.note))
    print(d.note)
  end

  if d.type == "cc" then
    if d.cc == 22 then -- if CC number is 33 then...
      engine.cutoff(util.linexp(0,127,300,12000,d.val))
    end
  end

end


cutoff_param = 0
param_sel = 0
res_param = 0
sd_param = 0

function init()
  mults = s{1, 2.25, s{0.25, 1.5, 3.5, 2, 3, 0.75} } -- create a sequins of hz multiples
  playing = false
  base_hz = 200
  sequence = clock.run(
    function()
      while true do
        clock.sync(1/3)
        if playing then
          engine.hz(base_hz * mults() * math.random(2))
        end
      end
    end
  )
end

function key(n,z)
  if n == 3 and z == 1 then
    playing = not playing
    mults:reset() -- resets 'mults' index to 1
    redraw()
  end
end

function enc(n,d)
  if n==2 then
    if param_sel == 0 then
      cutoff_param = cutoff_param + d * 0.01
      cutoff_param2 = util.clamp(cutoff_param,1, 5)
      engine.attack(cutoff_param)
      -- print(cutoff_param, d)
      redraw()
    elseif param_sel == 1 then
      res_param = res_param + d * 0.01
      engine.resonance(res_param)
      redraw()
    elseif param_sel == 2 then
      sd_param = sd_param + d * 0.01
      engine.sub_div(sd_param)
      redraw()
    end
      
  elseif n == 3 then
    params:delta("clock_tempo", d)
    redraw()
  elseif n == 1 then
    param_sel = ((param_sel +d) % 3)
    
    redraw()
  end
  
  
  
end


function redraw()
  screen.clear()
  screen.move(64,32)
  screen.text(playing and "K3: turn off" or "K3: turn on")
  screen.move( 64, 40)
  screen.level(5)
  screen.text("cutoff" .. " " .. cutoff_param)
  screen.move( 64, 50)
  screen.level(5)
  screen.text("resonance" .. " " .. res_param)
  screen.move( 64, 60)
  screen.level(5)
  screen.text("subdiv" .. " " .. sd_param)
  screen.move( 64, 10)
  screen.text(params:get("clock_tempo"))
  screen.move( 0, 10)
  screen.text(param_sel)
  
  if param_sel == 0 then
      screen.move( 64, 40)
      screen.level(15)
      screen.text("cutoff" .. " " .. cutoff_param)
  elseif param_sel == 1 then
      screen.move( 64, 50)
      screen.level(15)
      screen.text("resonance" .. " " .. res_param)
  elseif param_sel == 2 then
      screen.move( 64, 60)
      screen.level(15)
      screen.text("subdiv" .. " " .. sd_param)
    
  end
  
  
  screen.update()
end