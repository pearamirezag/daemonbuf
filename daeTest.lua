-- test for Daemon Buffer
engine.name = "Dae21"

-- create something to navegate through parameters

trig1 = 0 --add variable to hold trigger
trig2 = 0 --add variable to hold trigger

dbParamSel = 1 --hold the variable that will go thru the params
rateParamSel = 1 --hold the variable that will go thru the params
ratePointer = 1 -- variable to point the rateScale
fxParamSel = 1 --hold the variable that will go thru the params
fxPointer = 1 -- -- variable to point the rateScale
pGroupSel = 1 --hold variable for iterating over the parameter groups 

signal = 0 --to check the levels 
pointer = 0


dbParams = {
    "in_db",
    "overdub_db",
    "rec_db",
    "grain_db",
    "play_db",
    "direct_db",
    "master_db"
}

rateParams = {
    "rateRec",
    "ratePlay",
    "rateRecLag",
    "ratePlayLag",
    "grainRate",
    "burstRate"
}

fxParams = {
  "decimator",
  "brick"
}

dbValues = {
    -6,
    -100,
    -100,
    -100,
    -100,
    -100,
    -6
}

rateScale = {
  -4,
  -2,
  -1.5,
  -1,
  -0.5,
  -0.333,
  0.5,
  1,
  1.5,
  2,
  4
}

rateValues = {
    1, -- "rateRec"
    1, --  "ratePlay",
    1, --  "rateRecLag",
    1, --  "ratePlayLag",
    15, --   "grainRate",
    8 --     "burstRate"
}

fxValues = {
  1.5, -- decimator
  1.5 -- brick
}

decScale = {
  8,
  12,
  16,
  24
}

paramGroup = {
  'levels',
  'rates',
  'effects'
}

--

function init()
    message = "DaemonTools" ----------------- set our initial message
    screen_dirty = true ------------------------ ensure we only redraw when something changes
    redraw_clock_id = clock.run(redraw_clock) -- create a "redraw_clock" and note the id
end



function osc_in(path, args, from)
  
  
  --print(path)
  if path == "/hello" then
    print("hi!")
  elseif path == "snd_Signal" then
    signal = args[2]
    print(args[3])
  elseif path == "position" then
    x = args[1]
    pointer = args[2]
   --print('aqui')
  else
    -- print(path)
    --tab.print(args)
  end
  
  screen_dirty = true
--print("osc from " .. from[1] .. " port " .. from[2])
end

osc.event = osc_in


function redraw_clock() ----- a clock that draws space
    while true do ------------- "while true do" means "do this forever"
      clock.sleep(1/15) ------- pause for a fifteenth of a second (aka 15fps)
      if screen_dirty then ---- only if something changed
        redraw() -------------- redraw space
        screen_dirty = false -- and everything is clean again
      end
    end
end

function dump(o)     --- Funcion to dump values to the console
  if type(o) == 'table' then
     local s = '{ '
     for k,v in pairs(o) do
        if type(k) ~= 'number' then k = '"'..k..'"' end
        s = s .. '['..k..'] = ' .. dump(v) .. ','
     end
     return s .. '} '
  else
     return tostring(o)
  end
end

function key(n, z)

    if z == 0 then return end --------- do nothing when you release a key
    if k == 2 then press_down(2) end -- but press_down(2)
    if k == 3 then press_down(3) end -- and press_down(3)

    if n == 2 and z == 1 then
        trig1 = (trig1 + 1) % 2
        engine.trigRec(trig1)
        print("trigRec:" .. " " .. trig1)
        screen_dirty = true
    end

    if n == 3 and z == 1 then
        trig2 = (trig2 + 1) % 2
        engine.trigRec(trig2)
        print("trigPlay:" .. " " .. trig2)
        screen_dirty = true
    end

    screen_dirty = true
end


function enc(n, d)
   
  if n == 1 then  -- WHEN THE ENC1 ROTATES 
    pGroupSel = (pGroupSel + d - 1) % (#paramGroup) + 1
    --print(pGroupSel)
    screen_dirty = true
    end

  --- LEVELS ----

  -- When ENC2 rotates
  if pGroupSel == 1 and n == 2 then -- pGroupSel = 1 is levels 
      dbParamSel = (dbParamSel + d - 1) % (#dbParams) + 1
      -- print('groupSel ' .. pGroupSel .. ' ' .. dbParamSel .. ' ' .. dbParams[dbParamSel])
      screen_dirty = true
  elseif pGroupSel == 2 and n == 2 then -- pGroupSel =2 is rates
      rateParamSel = (rateParamSel + d - 1) % (#rateParams) + 1
      -- print('groupSel ' .. pGroupSel .. ' ' .. rateParamSel .. ' ' .. rateParams[rateParamSel])
      screen_dirty = true 
  elseif pGroupSel == 3 and n == 2 then
      fxParamSel = (fxParamSel + d - 1) % (#fxParams) + 1
      -- print('groupSel ' .. pGroupSel .. ' ' .. fxParamSel .. ' ' .. fxParams[fxParamSel])
      screen_dirty = true 
  end
 
  -- WHEN ENC3 ROTATES

  -- DB -- 
  if pGroupSel == 1 and n == 3 then
      dbValues[dbParamSel] = util.clamp(dbValues[dbParamSel] + d, -100, 6)
      --print(dbValues)
      if dbParamSel == 1 then
          engine.in_db(dbValues[1])
          --print(dbValues[1])
      elseif dbParamSel == 2 then
          engine.overdub_db(dbValues[2])
      elseif dbParamSel == 3 then
          engine.rec_db(dbValues[3])
      elseif dbParamSel == 4 then
          engine.grain_db(dbValues[4])
      elseif dbParamSel == 5 then
          engine.play_db(dbValues[5])
      elseif dbParamSel == 6 then
          engine.direct_db(dbValues[6])
      elseif dbParamSel == 7 then
          engine.master_db(dbValues[7])
      else
          -- Code for default case
          print("Invalid paramSel")
      end
      screen_dirty = true
  end -- ENC3 - dbs

      --- RATES ---
  if pGroupSel == 2 and n == 3 then --Update the values in the table 
    if rateParamSel <= 2 then
      ratePointer = (ratePointer + d -1) % (#rateScale) + 1
      rateValues[rateParamSel] = rateScale[ratePointer]
      
      if rateParamSel == 1 then
        engine.rateRec(rateValues[1])
      elseif rateParamSel == 2 then
        engine.ratePlay(rateValues[2])
     
       -- print(ratePointer .. ' ' .. rateScale[ratePointer])
      screen_dirty = true
      -- rateValues[rateParamSel] = rateScale[] + d 
      -- print(rateScale)
      end -- if rateparamSel is 2
  end --rateParamSel <2
      
      
    if rateParamSel > 2 then 
      
      rateValues[rateParamSel] = util.clamp(rateValues[rateParamSel] + d, 0, 20) 
      
      if  rateParamSel == 3 then
        engine.rateRecLag(rateValues[3])
      elseif  rateParamSel == 4 then
        engine.ratePlayLag(rateValues[4])
      elseif  rateParamSel == 5 then
        engine.grainRate(rateValues[5])
      elseif  rateParamSel == 6 then
        engine.burstRate(rateValues[6])
      
      -- print(dump(rateValues) .. ' ~~ ' .. rateParamSel ..  ' ~~ ' .. pGroupSel)
      screen_dirty = true 
      end
end -- rateparamsel>2
  
end -- rates



  -- FX  -- 
  if pGroupSel == 3 and n == 3 then --
          if fxParamSel == 1 then
            fxPointer = (fxPointer + d -1) % (#decScale) + 1
            fxValues[fxParamSel] = decScale[fxPointer]
            engine.decimator(fxValues[1]) -- engine
          elseif fxParamSel == 2 then
            
            fxValues[2] = (fxValues[2] + d) % 2
            print(fxValues[2])
            engine.brick(fxValues[2])
          end
    end -- fx

end -- function enc


function redraw()
    screen.clear() --------------- clear space
    screen.aa(1) ----------------- enable anti-aliasing
    screen.font_face(1) ---------- set the font face to "04B_03"
    screen.font_size(8) ---------- set the size to 8
    
    if pGroupSel == 1 then
      screen.move(5,5)
      screen.level(15)
      screen.text('levels')
      screen.move_rel(15,0)
      screen.level(1)
      screen.text('rates')
      screen.move_rel(15,0)
      screen.level(1)
      screen.text('fx')
    elseif pGroupSel == 2 then
      screen.move(5,5)
      screen.level(1)
      screen.text('levels')
      screen.move_rel(15,0)
      screen.level(15)
      screen.text('rates')
      screen.move_rel(15,0)
      screen.level(1)
      screen.text('fx')
    else
      screen.move(5,5)
      screen.level(1)
      screen.text('levels')
      screen.move_rel(15,0)
      screen.level(1)
      screen.text('rates')
      screen.move_rel(15,0)
      screen.level(15)
      screen.text('fx')
    end
      
      
    if pGroupSel == 1 then
    -- screen.move(5, 5)    
    screen.move(5, 15)
    screen.level(15)
    screen.text(dbParams[dbParamSel]) -- gets the volume params
    screen.move(60, 15)
    screen.text(dbValues[dbParamSel] .. "" .. "db")
    
    elseif pGroupSel == 2 then -- what happens when the group is rates
        screen.move(5, 15)
        screen.level(15)
        screen.text(rateParams[rateParamSel]) -- gets the volume params
        screen.move(60, 15)
        screen.text(rateValues[rateParamSel])
      
      
    else -- what happens when the group is fx
        screen.move(5, 15)
        screen.level(15)
        screen.text(fxParams[fxParamSel]) 
        screen.move(60, 15)
        screen.text(fxValues[fxParamSel])
        
    end  --end the ifs
    
    -- BUTTONS FOR TRIG REC/PLAY
    --screen.circle(10, 40, 5)
    screen.stroke()
    screen.circle(20, 40, 5)
    screen.stroke(1)
    screen.move(5, 50)
    screen.text('rec')
    screen.move(10, 10)
    
    -- screen.font_face(1)

    -- for i, v in ipairs(dbParams) do
    --     screen.text(v)
    --     screen.move(80,7*i)
    --   end

    -- screen.move()

    if trig1 == 1 then
        screen.circle(10, 40, 5)
        screen.fill()
    end

    if trig2 == 1 then
        screen.circle(20, 40, 5)
        screen.fill()
    end
    
    screen.move(100,50)
    screen.text(signal)
    screen.move(100,40)
    screen.text(pointer)

    screen.update()
end

function r() ----------------------------- execute r() in the repl to quickly rerun this script
    norns.script.load(norns.state.script) -- https://github.com/monome/norns/blob/main/lua/core/state.lua
  end
  
function cleanup() --------------- cleanup() is automatically called on script close
    clock.cancel(redraw_clock_id) -- melt our clock vie the id we noted
end


function intEngine()
  engine.in_db(-3)
  engine.overdub_db(-24)
  engine.rec_db(-3)
  engine.grain_db(-6)
  engine.play_db(-6)
  engine.direct_db(-12)
  engine.master_db(-3)
 end
