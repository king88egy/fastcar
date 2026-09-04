-- TouchHandler.lua
-- نظام اللمس والتوجيه للسباق والقوائم.
local pointers = {}
local lastClickTime = 0
local lastClickLabel = ""
local swipeThreshold = 70

local function clampLane(value)
  return math.max(-1, math.min(1, tonumber(value) or 0))
end

local function isRaceSection()
  return currentSection == "PLAYING" or currentSection == "PLAYING_MULTIPLAYER"
end

local function steeringPan()
  local lane=tonumber(GameState.lane or 0) or 0
  return lane>0 and 0.45 or (lane<0 and -0.45 or 0)
end

local function adjustFocusedAudioSetting(direction)
  if currentSection ~= "SETTINGS_AUDIO" then return false end
  local choice = tostring((currentMenuOptions or {})[currentMenuIndex] or "")
  local step = direction > 0 and 0.05 or -0.05
  local label = ""
  local level = 0
  if choice == "مستوى الموسيقى بالسحب" then
    Settings.bgmVolume = math.max(0, math.min(1, (Settings.bgmVolume or 0.03) + step)); label = "مستوى الموسيقى"; level=Settings.bgmVolume
  elseif choice == "مستوى المؤثرات بالسحب" then
    Settings.sfxVolume = math.max(0, math.min(1, (Settings.sfxVolume or 0.42) + step)); label = "مستوى المؤثرات"; level=Settings.sfxVolume
  elseif choice == "مستوى المذيع بالسحب" then
    Settings.announcerVolume = math.max(0, math.min(1, (Settings.announcerVolume or 0.20) + step)); label = "مستوى المذيع"; level=Settings.announcerVolume
  elseif choice == "سرعة الناطق بالسحب بلا حد" then
    Settings.voiceSpeed = math.max(0.05, (tonumber(Settings.voiceSpeed) or 3.15) + step)
    if tts then pcall(function() tts.setSpeechRate(Settings.voiceSpeed) end) end; label = "سرعة الناطق"; level=Settings.voiceSpeed
  elseif choice == "حدة الناطق بالسحب" then
    Settings.voicePitch = math.max(0.5, math.min(2, (Settings.voicePitch or 1) + step))
    if tts then pcall(function() tts.setPitch(Settings.voicePitch) end) end; label = "حدة الناطق"; level=Settings.voicePitch
  end
  if label ~= "" then
    saveGameData(); playDynamicSFX("a11y_press_light", 0, direction > 0 and 1.08 or 0.92, 0, 0.22); say(label .. " " .. tostring(math.floor(level * 100)) .. " بالمئة", true); return true
  end
  return false
end

local function useAvailableRaceCrateOrFuel()
  if GameState.pendingCrate and GameState.pendingCrate.ready and collectRaceCrate then collectRaceCrate(); return true end
  if (playerStats.fuelTanks or 0)>0 and (GameState.fuel or 0)<100 then
    playerStats.fuelTanks=playerStats.fuelTanks-1; GameState.fuel=100; GameState.fuelWarned=false; playDynamicSFX("fuel_fill",0,1,0,0.72); say("تم تعبئة الوقود.",true); saveGameData(); return true
  end
  say("لا يوجد صندوق متاح أو خزان وقود إضافي.",false); return false
end

local function handleRaceSwipe(pData,dx,dy,width)
  if GameState.isCrashed then return end
  if GameState.actionDrawerOpen and math.abs(dx)>math.abs(dy) and math.abs(dx)>=swipeThreshold then moveRaceAction(dx>0 and 1 or -1); pData.isSwiped=true; return end
  if GameState.actionDrawerOpen and dy>=swipeThreshold and math.abs(dy)>math.abs(dx) then openRaceActionDrawer(); pData.isSwiped=true; return end
  if pData.startX <= width/2 then
    if math.abs(dx)>math.abs(dy) and math.abs(dx)>=swipeThreshold and Settings.steeringMode~="الإمالة" then
      if dx>0 then GameState.lane=clampLane(GameState.lane+1); playDynamicSFX("race_turn_right",0,1.0,0.85,0.88); say("يمين.",false) else GameState.lane=clampLane(GameState.lane-1); playDynamicSFX("race_turn_left",0,1.0,-0.85,0.88); say("يسار.",false) end
      pData.isSwiped=true
    elseif math.abs(dy)>math.abs(dx) and dy>=swipeThreshold then
      if openRaceActionDrawer then openRaceActionDrawer() end; pData.isSwiped=true
    elseif math.abs(dy)>math.abs(dx) and dy<=-swipeThreshold then
      useAvailableRaceCrateOrFuel(); pData.isSwiped=true
    end
  elseif pData.startX > width/2 and math.abs(dy)>math.abs(dx) and dy<=-swipeThreshold then
    if activateRaceFlightMode then activateRaceFlightMode() else say("وضع الطيران غير متاح.",true) end; pData.isSwiped=true
  end
end

main_view.setOnTouchListener(function(v,event)
  local ok,result=pcall(function()
    local action=event.getActionMasked(); local pointerIndex=event.getActionIndex(); local pointerId=event.getPointerId(pointerIndex); local x=event.getX(pointerIndex); local y=event.getY(pointerIndex); local width=activity.getWidth(); local height=activity.getHeight()
    if action==MotionEvent.ACTION_DOWN or action==MotionEvent.ACTION_POINTER_DOWN then
      pointers[pointerId]={startX=x,startY=y,isSwiped=false,time=System.currentTimeMillis(),isHornHold=false,isGasHold=false,isBrakeTouch=false}
      if isRaceSection() and not GameState.isCrashed then
        if x<=width*0.25 and y<height*0.30 then toggleRaceEngine()
        elseif x>=width*0.75 and y<height*0.30 then pointers[pointerId].isHornHold=true; startHornSound()
        elseif x>=width*0.75 and y>=height*0.70 then pointers[pointerId].isGasHold=true; if startRaceGasHold then startRaceGasHold() end
        elseif x<=width*0.25 and y>=height*0.70 then pointers[pointerId].isBrakeTouch=true; GameState.isBrakePressed=true; playDynamicSFX("race_brake_squeal",0,1.0,steeringPan(),0.98) end
      end
    elseif action==MotionEvent.ACTION_MOVE then
      for i=0,event.getPointerCount()-1 do
        local pId=event.getPointerId(i); local px=event.getX(i); local py=event.getY(i); local pData=pointers[pId]
        if pData then
          local dx=px-pData.startX; local dy=py-pData.startY
          if math.abs(dx)>=swipeThreshold or math.abs(dy)>=swipeThreshold then
            if isRaceSection() then if not pData.isSwiped then handleRaceSwipe(pData,dx,dy,width) end
            elseif currentSection=="SETTINGS_AUDIO" and math.abs(dy)>math.abs(dx) then if not pData.isSwiped then adjustFocusedAudioSetting(dy<0 and 1 or -1); pData.isSwiped=true end
            elseif math.abs(dx)>math.abs(dy) and not pData.isSwiped then moveMenuBy(dx>0 and 1 or -1); pData.isSwiped=true
            elseif math.abs(dy)>math.abs(dx) and not pData.isSwiped and px<=width*0.35 then
              if dy<0 then Settings.bgmEnabled=true; saveGameData(); playDynamicSFX("menu_open",0,1,0,0.75); restoreMenuMusic(); say("تم تشغيل الموسيقى.",true) else Settings.bgmEnabled=false; saveGameData(); playDynamicSFX("menu_close",0,1,0,0.75); stopMusicPlayback(); say("تم إيقاف الموسيقى.",true) end; pData.isSwiped=true
            end
          end
        end
      end
    elseif action==MotionEvent.ACTION_UP or action==MotionEvent.ACTION_POINTER_UP then
      local pData=pointers[pointerId]
      if pData then
        if pData.isHornHold then stopHornSound() end; if pData.isGasHold and stopRaceGasHold then stopRaceGasHold() end; if pData.isBrakeTouch then GameState.isBrakePressed=false end
        if not pData.isSwiped and not isRaceSection() and System.currentTimeMillis()-pData.time<350 then
          local now=System.currentTimeMillis(); local label=tostring((currentMenuOptions or {})[currentMenuIndex] or "")
          -- فرض النقر المزدوج (Double Tap) للتفعيل والنقر المفرد للنطق والتحديد
          if lastClickLabel==label and now-lastClickTime<700 then
            playDynamicSFX("a11y_double_tap",0,1,0,0.92); playDynamicSFX("a11y_activate",0,1,0,0.92)
            handleMenuSelection()
            lastClickLabel=""
            lastClickTime=0
          else
            playDynamicSFX("a11y_press_light",0,1,0,0.72)
            say(label,true)
            lastClickLabel=label
            lastClickTime=now
          end
        end
      end
      pointers[pointerId]=nil
    elseif action==MotionEvent.ACTION_CANCEL then
      for _,pData in pairs(pointers) do if pData.isHornHold then stopHornSound() end; if pData.isGasHold and stopRaceGasHold then stopRaceGasHold() end; if pData.isBrakeTouch then GameState.isBrakePressed=false end end
      pointers={}; GameState.isGasPressed=false; GameState.isBrakePressed=false
    end
    if isRaceSection() then
      local gas,brake=false,false
      for i=0,event.getPointerCount()-1 do local pData=pointers[event.getPointerId(i)]; if pData then if pData.isGasHold then gas=true end; if pData.isBrakeTouch then brake=true end end end
      GameState.isGasPressed=gas; GameState.isBrakePressed=brake
    end
    return true
  end)
  if not ok then pcall(function() playDynamicSFX("input_error",0,1,0,0.30); say("حدث خطأ في اللمس.",false) end) end
  return result==true
end)

-- الميكروفون لا يعمل بالسحب؛ الزر نفسه يتطلب الضغطتين.
