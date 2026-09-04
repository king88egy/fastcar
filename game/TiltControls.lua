-- TiltControls.lua
-- توجيه السيارة بإمالة الهاتف مع إبقاء السحب متاحًا حسب إعداد اللاعب.

TiltControls = TiltControls or {manager=nil,sensor=nil,listener=nil,running=false,lastRaw=0,lastLaneAt=0}

local function tiltInRace()
  return currentSection=="PLAYING" or currentSection=="PLAYING_MULTIPLAYER"
end

local function clampTilt(value)
  return math.max(-1,math.min(1,tonumber(value) or 0))
end

local function applyTiltLane(raw)
  TiltControls.lastRaw=tonumber(raw) or 0
  if not tiltInRace() or GameState.isCrashed then return end
  local center=tonumber(Settings.tiltCenter or 0) or 0
  local sensitivity=math.max(0.15,tonumber(Settings.tiltSensitivity or 2.4) or 2.4)
  local deadZone=math.max(0.05,math.min(4,tonumber(Settings.tiltDeadZone or 0.65) or 0.65))
  local delta=TiltControls.lastRaw-center
  if Settings.invertTilt==true then delta=-delta end
  if math.abs(delta)<deadZone then return end
  local normalized=clampTilt(delta/sensitivity)
  GameState.tiltValue=normalized
  local target=normalized>0 and 1 or -1
  local now=System.currentTimeMillis()
  if GameState.lane~=target and now-(TiltControls.lastLaneAt or 0)>=260 then
    GameState.lane=target; TiltControls.lastLaneAt=now
    playDynamicSFX(target>0 and "race_turn_right" or "race_turn_left",0,1.0,target,0.68)
  end
end

function calibrateTiltControl(callback)
  Settings.tiltCenter=tonumber(TiltControls.lastRaw or 0) or 0; saveGameData(); playDynamicSFX("settings_change",0,1,0,0.68); say("تمت معايرة مركز الإمالة.",true); if callback then callback(true) end
end

function stopTiltControl()
  if TiltControls.manager and TiltControls.listener then pcall(function() TiltControls.manager.unregisterListener(TiltControls.listener) end) end
  TiltControls.running=false
end

function startTiltControl()
  if Settings.steeringMode~="الإمالة" and Settings.steeringMode~="مختلط" then stopTiltControl(); return false end
  if TiltControls.running then return true end
  local ok=pcall(function()
    if not TiltControls.manager then TiltControls.manager=activity.getSystemService(Context.SENSOR_SERVICE) end
    if not TiltControls.sensor then TiltControls.sensor=TiltControls.manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) end
    if not TiltControls.sensor then error("sensor_unavailable") end
    if not TiltControls.listener then
      TiltControls.listener=SensorEventListener{
        onSensorChanged=function(event)
          local values=event and event.values
          if values then applyTiltLane(values[0] or 0) end
        end,
        onAccuracyChanged=function(sensor,accuracy) end
      }
    end
    TiltControls.manager.registerListener(TiltControls.listener,TiltControls.sensor,SensorManager.SENSOR_DELAY_GAME)
    TiltControls.running=true
  end)
  if not ok then TiltControls.running=false; say("حساس الإمالة غير متاح على هذا الجهاز.",true); return false end
  return true
end

function setSteeringMode(mode)
  mode=tostring(mode or "السحب")
  Settings.steeringMode=mode
  saveGameData()
  if mode=="الإمالة" or mode=="مختلط" then startTiltControl() else stopTiltControl() end
  playDynamicSFX("settings_change",0,1,0,0.68)
  say("طريقة التوجيه: "..mode,true)
  refreshVisibleScreen()
end
