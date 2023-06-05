-- test for Daemon Buffer
engine.name = "Dae2"

-- create something to navegate through parameters

trig1 = 0 --add variable to hold trigger
trig2 = 0 --add variable to hold trigger

paramSel = 0 --hold the variable that will go thru the params
pGroupSel = 0 --hold variable for iterating over the parameter groups 

-- redraw()
dbParams = {
    "in_db",
    "overdub_db",
    "rec_db",
    "grain_db",
    "loop_db",
    "direct_db",
    "master_db"
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

paramGroup = {
  'levels',
  'rates',
  'effects'
}

function key(n, z)
    if n == 2 and z == 1 then
        trig1 = (trig1 + 1) % 2
        engine.trigRec(trig1)
        print("trigRec:" .. " " .. trig1)
        redraw()
    end

    if n == 3 and z == 1 then
        trig2 = (trig2 + 1) % 2
        engine.trigRec(trig2)
        print("trigPlay:" .. " " .. trig2)
        redraw()
    end

    redraw()
end





function enc(n, d)
  if n == 1 then
    pGroupSel = (pGroupSel + d - 1) % (#paramGroup) + 1
    redraw()
    end
  
    if n == 2 then
        paramSel = (paramSel + d - 1) % (#dbParams) + 1
        --print(paramSel .. ' ' .. dbParams[paramSel])
        redraw()
    end

    if n == 3 then
        dbValues[paramSel] = util.clamp(dbValues[paramSel] + d, -100, 6)
        
        -- engine.dbValues[paramSel](dbValue) -- this is what ChatGPT said i should use
        
        if paramSel == 1 then
            engine.in_db(dbValues[1])
        elseif paramSel == 2 then
            engine.overdub_db(dbValues[2])
        elseif paramSel == 3 then
            engine.rec_db(dbValues[3])
        elseif paramSel == 4 then
            engine.grain_db(dbValues[4])
        elseif paramSel == 5 then
            engine.loop_db(dbValues[5])
        elseif paramSel == 6 then
            engine.direct_db(dbValues[6])
        elseif paramSel == 7 then
            engine.master_db(dbValues[7])
        else
            -- Code for default case
            print("Invalid paramSel")
        end

        redraw()
    end
end

function redraw()
    screen.clear()
    
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
      
      
    
    screen.move(5, 5)
    -- screen.level(1)
    -- screen.text(dbParams[paramSel-1])
    -- screen.move(60, 5)
    -- screen.text(dbValues[paramSel-1] .. "" .. "db")
    
    screen.move(5, 15)
    screen.level(15)
    screen.text(dbParams[paramSel]) -- gets the volume params
    screen.move(60, 15)
    screen.text(dbValues[paramSel] .. "" .. "db")
    
    -- screen.move(5, 25)
    -- screen.level(1)
    -- screen.text(dbParams[paramSel+1])
    -- screen.move(60, 25)
    -- screen.text(dbValues[paramSel] .. "" .. "db")
    

    screen.circle(10, 40, 5)
    screen.stroke()
    screen.circle(20, 40, 5)
    screen.stroke()
    screen.move(5, 50)
    screen.text("rec")
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

    screen.update()
end
