-- UIRenderer.lua
-- عرض مرئي قابل للمس مع نطق متزامن.
local sectionTitles = {
  MENU="القائمة الرئيسية", PROFILE="الملف الشخصي", DAILY_REWARD="المكافأة اليومية", RACE_MODE_SELECT="اختيار طريقة اللعب", CREATE_ROOM="إنشاء غرفة", JOIN_ROOM="الانضمام إلى غرفة", HOTSPOT_MODE="اللعب عبر Hotspot", FRIENDS_MENU="مجتمع المتسابقين", FRIENDS_LIST_PLAY="الأصدقاء", FRIEND_REQUESTS="طلبات الصداقة", ONLINE_PLAYERS="اللاعبون المتصلون", PLAYER_PROFILE="ملف لاعب", LEADERBOARD="قائمة المتصدرين", WAITING_ROOM="غرفة الانتظار", ROOM_ACTIONS="إجراءات الغرفة", ROOM_PLAYERS="الموجودون في الغرفة", ROOM_CHAT="مراسلة الغرفة", SETTINGS="الإعدادات", SETTINGS_GENERAL="الإعدادات العامة", SETTINGS_AUDIO="إعدادات الصوت", SETTINGS_ACCESSIBILITY="إعدادات الوصول", SETTINGS_RACE="إعدادات السباق", SETTINGS_ACCOUNT="إعدادات الحساب والتحديث", STORE="المتجر", CAR_DEALER="معرض السيارات", INVENTORY="الحقيبة", GAS_STATION="محطة البنزين", GARAGE="ورشة الصيانة", ABOUT="حول اللعبة", PLAYING="السباق الفردي", PLAYING_MULTIPLAYER="السباق الجماعي", WIN="نتيجة السباق", LOSE="نتيجة السباق"
}

local function setText(view, value) if view then pcall(function() view.setText(tostring(value or "")) end) end end
local function isBlind() return Settings.accessibilityMode == "وضع المكفوفين" end
local blindTapTimes = {}
local function blindTapKey(view,label) return tostring(currentSection or "").."|"..tostring(label or "") end

function moveMenuBy(delta)
  if not currentMenuOptions or #currentMenuOptions==0 then return end
  local target=(currentMenuIndex or 1)+(tonumber(delta) or 0)
  if target<1 then
    currentMenuIndex=1; playDynamicSFX("a11y_boundary_first_light",0,1.0,0.0,0.24); say(tostring(currentMenuOptions[1]),true); refreshVisibleScreen(); return
  end
  if target>#currentMenuOptions then
    currentMenuIndex=#currentMenuOptions; playDynamicSFX("a11y_boundary_last_light",0,1.0,0.0,0.24); say(tostring(currentMenuOptions[#currentMenuOptions]),true); refreshVisibleScreen(); return
  end
  currentMenuIndex=target
  playDynamicSFX(delta>0 and "focus_next_loud" or "focus_prev_loud",0,1.0,0.0,0.98)
  say(currentMenuOptions[currentMenuIndex],true); refreshVisibleScreen()
end

local function updateRaceCarVisual()
  if not race_car_image then return end
  local speed=tonumber(GameState.speed) or 0
  local moving=GameState.isEngineOn==true and speed>1 and not GameState.countdownActive and not GameState.isCrashed
  local lane=math.max(-1,math.min(1,tonumber(GameState.lane) or 0))
  pcall(function() race_car_image.setAlpha(moving and 1.0 or 0.88) end)
  pcall(function() race_car_image.setTranslationX(lane*18) end)
  pcall(function() race_car_image.setRotation(moving and math.sin((tonumber(GameState.timeElapsed) or 0)*7)*0.7 or 0) end)
  pcall(function() local gap=math.floor(math.abs((GameState.opponentDistance or 0)-(GameState.distance or 0))); local relation=((GameState.opponentDistance or 0)>((GameState.distance or 0)+1)) and "المنافس أمامك" or (((GameState.distance or 0)>((GameState.opponentDistance or 0)+1)) and "أنت متقدم" or "المنافس قريب"); local state=moving and "سيارة السباق تتحرك" or "سيارة السباق متوقفة"; race_car_image.setContentDescription(state.."، السرعة "..math.floor(speed).."، المسار "..tostring(lane).."، "..relation.." بفارق "..tostring(gap).." متر، الطريق "..tostring(GameState.roadCondition or "جاف")) end)
end

local function configureAccessibilityView(view, label)
  if not view then return end
  pcall(function()
    view.setContentDescription(tostring(label or ""))
    view.setFocusable(true)
    if isBlind() then
      view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
    else
      view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES)
    end
  end)
end

local function blindTap(view, label, callback, requiresDoubleTap)
  if not isBlind() then callback(); return end
  -- تم تعديل المنطق هنا ليكون الاعتماد دائماً على النقر المزدوج بغض النظر عن القيمة الم تمررة إذا أردت فرض النقر المزدوج على العناصر
  if not requiresDoubleTap then
    playDynamicSFX("a11y_press_confirm", 0, 1.0, 0, 0.98)
    callback()
    return
  end
  local now = System.currentTimeMillis()
  local key = blindTapKey(view,label)
  local last = tonumber(blindTapTimes[key] or 0) or 0
  if last > 0 and now - last <= 700 then
    blindTapTimes[key]=nil
    playDynamicSFX("a11y_double_tap", 0, 1.0, 0, 0.98)
    playDynamicSFX("a11y_press_confirm", 0, 1.0, 0, 0.98)
    callback()
  else
    blindTapTimes[key]=now
    playDynamicSFX("a11y_press_light", 0, 1.0, 0, 0.22)
    say(label, true)
  end
end
local function iconFor(label)
  local icons={
    ["بدء السباق"]="▶", ["الملف الشخصي"]="👤", ["الإعدادات"]="⚙", ["مجتمع المتسابقين"]="👥", ["قائمة المتصدرين"]="🏆", ["المتجر"]="🛒", ["الحقيبة"]="🎒", ["محطة البنزين"]="⛽", ["ورشة الصيانة"]="🔧", ["خروج"]="↩", ["الزمور"]="🔊", ["الميكروفون"]="🎙", ["البنزين"]="⛽", ["الفرامل"]="⏹", ["تشغيل المحرك"]="⏻", ["إيقاف المحرك"]="⏻", ["نيترو"]="⚡", ["إغلاق"]="✕", ["قائمة الغرفة"]="☰", ["مراسلة الغرفة"]="💬", ["تدوير عجلة الحظ"]="◉", ["معرض السيارات"]="🚗", ["متجر الأدوات"]="🧰", ["المهام والإنجازات"]="★", ["المكافأة اليومية"]="◉"
  }
  if icons[label] then return icons[label] end
  if label:find("رجوع") then return "↩" end
  if label:find("شراء") then return "🛒" end
  if label:find("تحديث") then return "↻" end
  if label:find("إعداد") or label:find("تغيير") then return "⚙" end
  if label:find("دعوة") or label:find("صديق") then return "👥" end
  if label:find("غرفة") then return "☰" end
  if label:find("الميكروفون") then return "🎙" end
  if label:find("الزمور") then return "🔊" end
  if label:find("البنزين") then return "⛽" end
  if label:find("رسالة") or label:find("دردشة") then return "💬" end
  if label:find("مدة") or label:find("دقيقة") then return "◷" end
  return "◆"
end

local function safeClick(handler)
  return function(view)
    local index = tonumber(tostring(view.getTag() or "0")) or 0
    local label = tostring((currentMenuOptions or {})[index] or "الخيار")
    -- فرض النقر المزدوج (جعل القيمة true دائماً لـ requiresDoubleTap)
    blindTap(view, label, function() handler(index) end, true)
  end
end

local function buttonFor(text, index)
  local label=tostring(text); local shown=iconFor(label).."  "..label
  local button = Button(activity); button.setText(shown); button.setTag(index); button.setTextSize(10); button.setAllCaps(false); button.setSingleLine(true)
  configureAccessibilityView(button, label)
  button.setMinHeight(0); button.setMinWidth(0); button.setPadding(3, 2, 3, 2); pcall(function() button.setBackgroundColor(index==currentMenuIndex and 0xFF355E85 or 0xFF1C2838) end)
  local touchStartX,touchStartY,touchMoved,pendingDirection=0,0,false,0
  button.setOnTouchListener(function(view,event)
    if not isBlind() or currentSection=="PLAYING" or currentSection=="PLAYING_MULTIPLAYER" then return false end
    local action=event.getActionMasked()
    if action==MotionEvent.ACTION_DOWN then
      touchStartX=event.getX(); touchStartY=event.getY(); touchMoved=false; pendingDirection=0
    elseif action==MotionEvent.ACTION_MOVE and not touchMoved then
      local dx=event.getX()-touchStartX; local dy=event.getY()-touchStartY
      if math.abs(dx)>=100 and math.abs(dx)>math.abs(dy) then
        touchMoved=true; pendingDirection=dx>0 and 1 or -1; return true
      end
    elseif action==MotionEvent.ACTION_UP then
      if touchMoved then
        local direction=pendingDirection; touchMoved=false; pendingDirection=0
        if direction~=0 then moveMenuBy(direction) end
        return true
      end
    elseif action==MotionEvent.ACTION_CANCEL and touchMoved then
      touchMoved=false; pendingDirection=0; return true
    end
    return false
  end)
  button.setOnClickListener(safeClick(function(i) currentMenuIndex=i; playDynamicSFX(i==1 and "first_select" or (i==#(currentMenuOptions or {}) and "last_select" or "menu_select"),0,1,0,0.50); handleMenuSelection(); refreshVisibleScreen() end))
  return button
end

function toggleRaceEngine()
  if GameState.isEngineOn then
    GameState.isEngineOn=false; GameState.isGasPressed=false; GameState.gasPressure=0; GameState.gasPointerId=nil; stopEngineSound(); local stopSfx=playDynamicSFX("car_stop_external",0,1.0,0,0.90); if stopSfx==0 then playDynamicSFX("start_alt",0,0.55,0,0.88) end; say("توقف المحرك.",true)
  elseif GameState.fuel>0 and GameState.engineTemp<110 and not GameState.isCrashed then
    GameState.isEngineOn=true; local activeCar=Store[activeCarId] or Store[1]; AudioSystem.engineSoundKey=activeCar.engineSound or "engine_low"; startEngineCrankSound(); startEngineSound(); if SmartAnnouncer and SmartAnnouncer.commentary then SmartAnnouncer.commentary("engine",playerStats.username,"",false) end; say("يعمل المحرك.",true)
  else say("لا يمكن تشغيل المحرك الآن.",true) end
  refreshVisibleScreen()
end

function toggleRaceHorn()
  if AudioSystem.hornStreamId and AudioSystem.hornStreamId~=0 then
    stopHornSound(); say("توقف الزمور.",true)
  else
    startHornSound(); say("الزمور يعمل.",true)
  end
  refreshVisibleScreen()
end

function toggleRaceGas()
  if not GameState.isEngineOn then toggleRaceEngine(); if not GameState.isEngineOn then return end end
  if GameState.isGasPressed then
    GameState.isGasPressed=false; GameState.gasPressure=0; GameState.gasPointerId=nil; say("توقف البنزين.",true)
  else
    GameState.isGasPressed=true; GameState.gasPressure=0.12; GameState.gasPointerId="button"; say("البنزين يعمل.",true)
  end
  refreshVisibleScreen()
end

function startRaceGasHold()
  if not GameState.isEngineOn then toggleRaceEngine(); if not GameState.isEngineOn then return end end
  GameState.isGasPressed=true; GameState.gasPressure=0.12; GameState.gasPointerId="hold"; playDynamicSFX("race_engine_rev",0,0.82,0,0.55); refreshVisibleScreen()
end
function stopRaceGasHold()
  if GameState.gasPointerId=="hold" then GameState.isGasPressed=false; GameState.gasPressure=0; GameState.gasPointerId=nil; refreshVisibleScreen() end
end
function startRaceHornHold() startHornSound() end
function stopRaceHornHold() stopHornSound() end

function toggleRaceMic()
  if not Multiplayer.isMicEnabled then say("الميكروفون موقوف في هذه الغرفة.", true); return end
  Multiplayer.isRecordingVoice = not Multiplayer.isRecordingVoice
  if Multiplayer.isRecordingVoice then
    playDynamicSFX("mic_on", 0, 1, 0); say("الميكروفون، اضغط مرتين للإيقاف.", true)
    if Multiplayer.directCallTarget ~= "" then firebaseSendVoiceSignal(Multiplayer.directCallTarget, true, function() end) end
  else
    playDynamicSFX("mic_off", 0, 1, 0); say("الميكروفون، اضغط مرتين للتفعيل.", true)
    if Multiplayer.directCallTarget ~= "" then firebaseSendVoiceSignal(Multiplayer.directCallTarget, false, function() end) end
  end
  refreshVisibleScreen()
end

function useRaceFuel()
  if playerStats.fuelTanks and playerStats.fuelTanks > 0 and GameState.fuel < 100 then playerStats.fuelTanks=playerStats.fuelTanks-1; GameState.fuel=100; GameState.fuelWarned=false; saveGameData(); say("تم استخدام خزان الوقود.", true) else say("لا يوجد وقود إضافي أو الخزان ممتلئ.", true) end
end

function useRaceNitro()
  if (playerStats.nitro or 0) <= 0 then say("لا يوجد نيترو.", true); return end
  if not GameState.isEngineOn then say("شغل المحرك أولاً.", true); return end
  playerStats.nitro=playerStats.nitro-1; GameState.nitroActive=true; GameState.speed=math.min(GameState.maxSpeed+80,GameState.speed+90); saveGameData(); playDynamicSFX("race_boost",0,1,0,0.98); if playAnnouncer then playAnnouncer("announcer_push",1.0,0.82) end; say("تم تفعيل النيترو.", true)
  Handler().postDelayed(Runnable({run=function() GameState.nitroActive=false end}), 1800)
end
function activateRaceFlightMode()
  local cost=tonumber(GameState.flightCost or 4000) or 4000
  if GameState.flightMode then say("وضع الطيران مفعل بالفعل.",true); return false end
  if not GameState.isEngineOn then say("شغل المحرك أولاً.",true); return false end
  if (playerStats.money or 0)<cost then playDynamicSFX("purchase_error",0,1,0,0.78); say("الرصيد غير كافٍ لتفعيل وضع الطيران. التكلفة "..tostring(cost).." عملة.",true); return false end
  local before=playerStats.money; playerStats.money=before-cost; GameState.flightMode=true; GameState.flightEndsAt=System.currentTimeMillis()+7000; GameState.speed=math.min(GameState.maxSpeed+110,math.max(GameState.speed or 0,GameState.maxSpeed*0.85)); GameState.distance=math.max(GameState.distance or 0,(GameState.opponentDistance or 0)+6); saveGameData(); if firebaseRecordEconomyLedger then firebaseRecordEconomyLedger("flight_mode",-cost,"تفعيل وضع الطيران","flight_"..tostring(System.currentTimeMillis()),function() end) end; if firebasePublishRaceEvent then firebasePublishRaceEvent("flight_mode",{cost=cost,direction=0,soundKey="race_boost",relativePosition="over_opponent"},function() end) end; playDynamicSFX("race_boost",0,1.12,0,0.98); if playAnnouncer then playAnnouncer("announcer_push",1.0,0.90) end; if SmartAnnouncer and SmartAnnouncer.commentary then SmartAnnouncer.commentary("flight",playerStats.username,"",true) end; say("تم تفعيل وضع الطيران. تجاوزت المنافس مؤقتًا مقابل "..tostring(cost).." عملة.",true); return true
end

local raceActionOptions={"قائمة الغرفة","مراسلة الغرفة","الأصدقاء","الرسائل الخاصة","دعوة صديق","إيقاف أو استئناف السباق","رجوع","مغادرة الغرفة","إغلاق"}
function openRaceActionDrawer()
  GameState.actionDrawerOpen = not GameState.actionDrawerOpen; GameState.actionMenuIndex=1; playDynamicSFX(GameState.actionDrawerOpen and "drawer_open" or "drawer_close",0,1,0,0.55); refreshVisibleScreen(); say(GameState.actionDrawerOpen and "فُتحت قائمة الإجراءات." or "أُغلقت قائمة الإجراءات.", true)
end
function moveRaceAction(delta)
  if not GameState.actionDrawerOpen then return end
  GameState.actionMenuIndex=math.max(1,math.min(#raceActionOptions,GameState.actionMenuIndex+delta)); playDynamicSFX(GameState.actionMenuIndex==1 and "first_focus" or (GameState.actionMenuIndex==#raceActionOptions and "last_focus" or "focus"),0,1,0,0.30); say(raceActionOptions[GameState.actionMenuIndex],true); refreshVisibleScreen()
end
function chooseRaceAction()
  local choice=raceActionOptions[GameState.actionMenuIndex]
  if choice=="قائمة الغرفة" then if currentSection=="PLAYING_MULTIPLAYER" then Multiplayer.raceReturnSection="PLAYING_MULTIPLAYER"; loadSection("ROOM_ACTIONS") else say("هذه الأدوات متاحة في السباق الجماعي.",true) end
  elseif choice=="مراسلة الغرفة" then if currentSection=="PLAYING_MULTIPLAYER" then Multiplayer.raceReturnSection="PLAYING_MULTIPLAYER"; loadSection("ROOM_CHAT") end
  elseif choice=="الأصدقاء" then if currentSection=="PLAYING_MULTIPLAYER" then Multiplayer.socialReturnSection="PLAYING_MULTIPLAYER"; loadSection("FRIENDS_MENU") end
  elseif choice=="الرسائل الخاصة" then if currentSection=="PLAYING_MULTIPLAYER" then Multiplayer.socialReturnSection="PLAYING_MULTIPLAYER"; loadSection("PRIVATE_MESSAGES") end
  elseif choice=="دعوة صديق" then if currentSection=="PLAYING_MULTIPLAYER" then Multiplayer.socialReturnSection="PLAYING_MULTIPLAYER"; loadSection("FRIENDS_LIST_PLAY") end
  elseif choice=="إيقاف أو استئناف السباق" then
    local nextPaused=not GameState.isPaused
    if currentSection=="PLAYING_MULTIPLAYER" then
      if Multiplayer.roomOwner~=playerStats.username then say("إيقاف السباق الجماعي متاح لمالك الغرفة فقط.",true); return end
      firebaseSetRoomPause(nextPaused,function(ok)
        if ok then GameState.isPaused=nextPaused; Multiplayer.roomPaused=nextPaused; GameState.isEngineOn=false; GameState.isGasPressed=false; stopEngineSound(); say(nextPaused and "تم إيقاف السباق للجميع مؤقتًا." or "تم استئناف السباق للجميع.",true); refreshVisibleScreen() else say("تعذر مزامنة إيقاف السباق.",true) end
      end)
    else
      GameState.isPaused=nextPaused; GameState.isEngineOn=false; GameState.isGasPressed=false; stopEngineSound(); say(nextPaused and "تم إيقاف السباق مؤقتًا." or "تم استئناف السباق.",true); refreshVisibleScreen()
    end
  elseif choice=="رجوع" then GameState.actionDrawerOpen=false; refreshVisibleScreen(); say("أُغلقت قائمة الإجراءات.",true)
  elseif choice=="مغادرة الغرفة" then GameState.actionDrawerOpen=false; refreshVisibleScreen(); confirmLeaveRoom()
  elseif choice=="إغلاق" then GameState.actionDrawerOpen=false; refreshVisibleScreen(); say("أُغلقت قائمة الإجراءات.",true)
  end
end

local function addRaceButton(parent, label, callback, description, large, requiresDoubleTap)
  local b=Button(activity); b.setText(iconFor(label).."  "..label); b.setAllCaps(false); b.setTextSize(large and 18 or 16); b.setMinHeight(large and 110 or 92); b.setMinimumHeight(large and 110 or 92); b.setPadding(12,8,12,8)
  configureAccessibilityView(b, description or label)
  local lastTapAt=0; local touchStartX,touchStartY=0,0; local drawerButton=(parent==race_actions_container); local touchMoved=false
  b.setOnTouchListener(function(view,event)
    local action=event.getActionMasked()
    if action==MotionEvent.ACTION_DOWN then
      touchStartX=event.getX(); touchStartY=event.getY(); touchMoved=false; b.setPressed(true); return true
    elseif action==MotionEvent.ACTION_MOVE and drawerButton and not touchMoved then
      local dx=event.getX()-touchStartX; local dy=event.getY()-touchStartY
      if math.abs(dx)>=60 and math.abs(dx)>math.abs(dy) then touchMoved=true; moveRaceAction(dx>0 and 1 or -1); return true end
      if dy>=60 and math.abs(dy)>math.abs(dx) then touchMoved=true; openRaceActionDrawer(); return true end
      return true
    elseif action==MotionEvent.ACTION_UP then
      b.setPressed(false)
      if touchMoved then touchMoved=false; return true end
      -- فرض النقر المزدوج هنا كذلك بغض النظر عن قيمة المتغير الم تمرر
      local now=System.currentTimeMillis()
      if now-lastTapAt<=700 then
        lastTapAt=0; playDynamicSFX("a11y_double_tap",0,1,0,0.98); callback(); pcall(refreshVisibleScreen)
      else
        lastTapAt=now; playDynamicSFX("a11y_press_light",0,1,0,0.24); say(description or label,true)
      end
      return true
    elseif action==MotionEvent.ACTION_CANCEL then b.setPressed(false); return true end
    return true
  end)
  local params=(parent.getOrientation and parent.getOrientation()==LinearLayout.VERTICAL) and LinearLayout.LayoutParams(-1,0,1) or LinearLayout.LayoutParams(0,-2,1)
  parent.addView(b, params); return b
end

local function addHoldRaceButton(parent, label, onStart, onStop, description)
  local b=Button(activity); b.setText(iconFor(label).."  "..label); b.setAllCaps(false); b.setTextSize(label=="البنزين" and 22 or 18); b.setMinHeight(label=="البنزين" and 180 or 120); b.setMinimumHeight(label=="البنزين" and 180 or 120); b.setGravity(Gravity.CENTER); b.setPadding(14,10,14,10)
  configureAccessibilityView(b, description or label)
  local armed=false
  b.setOnTouchListener(function(view,event)
    local action=event.getActionMasked()
    if action==MotionEvent.ACTION_DOWN then
      if not armed then armed=true; playDynamicSFX("a11y_press_confirm",0,1,0,0.92); onStart() end
      return true
    elseif action==MotionEvent.ACTION_UP or action==MotionEvent.ACTION_CANCEL then
      if armed then armed=false; onStop() end
      return true
    end
    return true
  end)
  local weight=(label=="البنزين") and 2 or 1
  local params=(parent.getOrientation and parent.getOrientation()==LinearLayout.VERTICAL) and LinearLayout.LayoutParams(-1,0,weight) or LinearLayout.LayoutParams(0,-2,1)
  parent.addView(b, params); return b
end

local function renderRaceControls()
  if not race_controls or not race_actions_container then return end
  race_controls.setVisibility(View.VISIBLE); race_controls.removeAllViews(); race_actions_container.removeAllViews()
  local landscape=LinearLayout(activity); landscape.setOrientation(LinearLayout.HORIZONTAL); landscape.setGravity(Gravity.CENTER_VERTICAL); race_controls.addView(landscape,LinearLayout.LayoutParams(-1,-1))
  local leftColumn=LinearLayout(activity); leftColumn.setOrientation(LinearLayout.VERTICAL); leftColumn.setGravity(Gravity.CENTER); landscape.addView(leftColumn,LinearLayout.LayoutParams(0,-1,1))
  local centerColumn=LinearLayout(activity); centerColumn.setOrientation(LinearLayout.VERTICAL); centerColumn.setGravity(Gravity.CENTER); landscape.addView(centerColumn,LinearLayout.LayoutParams(0,-1,1))
  local rightColumn=LinearLayout(activity); rightColumn.setOrientation(LinearLayout.VERTICAL); rightColumn.setGravity(Gravity.CENTER); landscape.addView(rightColumn,LinearLayout.LayoutParams(0,-1,1))
  addRaceButton(leftColumn, GameState.isEngineOn and "إيقاف المحرك" or "تشغيل المحرك", toggleRaceEngine, "تشغيل أو إيقاف المحرك. هذا الزر أعلى جهة الكاميرا.", false, true)
  local steering=TextView(activity); local steeringText=(Settings.steeringMode=="الإمالة" and "إمالة الهاتف يمينًا: انعطاف يمين\nإمالة الهاتف يسارًا: انعطاف يسار") or (Settings.steeringMode=="مختلط" and "السحب أو الإمالة يمينًا: انعطاف يمين\nالسحب أو الإمالة يسارًا: انعطاف يسار") or "السحب يمينًا من اليسار: انعطاف يمين\nالسحب يسارًا من اليسار: انعطاف يسار"; steering.setText(steeringText); steering.setGravity(Gravity.CENTER); steering.setTextSize(12); steering.setTextColor(0xFFA9C7FF); steering.setContentDescription("منطقة التوجيه. طريقة التوجيه الحالية: "..tostring(Settings.steeringMode or "السحب")); leftColumn.addView(steering,LinearLayout.LayoutParams(-1,0,1))
  addRaceButton(leftColumn, "الفرامل", function() GameState.isBrakePressed=true; local lane=tonumber(GameState.lane or 0) or 0; local brakePan=lane>0 and 0.5 or (lane<0 and -0.5 or 0); playDynamicSFX("race_brake_squeal",0,1,brakePan,0.98); say("فرامل.",false); Handler().postDelayed(Runnable({run=function() GameState.isBrakePressed=false end}),650) end, "زر الفرامل الكبير. المسه لتخفيف السرعة.", true, true)
  local cameraLabel=TextView(activity); cameraLabel.setText("صورة السيارة\nومنطقة السباق"); cameraLabel.setGravity(Gravity.CENTER); cameraLabel.setTextSize(12); cameraLabel.setTextColor(0xFFA9C7FF); centerColumn.addView(cameraLabel,LinearLayout.LayoutParams(-1,0,1))
  addHoldRaceButton(rightColumn, "الزمور", startRaceHornHold, stopRaceHornHold, "الزمور. يستمر طوال بقاء الإصبع على الزر.")
  local micLabel=Multiplayer.isRecordingVoice and "الميكروفون Microphone يعمل. اضغط مرتين للإيقاف" or "الميكروفون Microphone متوقف. اضغط مرتين للتشغيل"
  local micButton=addRaceButton(rightColumn, "الميكروفون", function() if Multiplayer.isRecordingVoice then stopVoiceRecording() else startVoiceRecording(Multiplayer.currentRoom) end end, micLabel, false, true)
  raceMicButton=micButton; pcall(function() micButton.setBackgroundColor(Multiplayer.isRecordingVoice and 0xFF9E2A2A or (Multiplayer.isMicEnabled and 0xFF23527A or 0xFF555555)) end)
  addHoldRaceButton(rightColumn, "البنزين", startRaceGasHold, stopRaceGasHold, "البنزين. مربع لمس واسع. يستمر رفع السرعة طوال بقاء الإصبع على الزر.")
  if GameState.actionDrawerOpen then
    local title=TextView(activity); title.setText("قائمة الإجراءات — اسحب يمينًا أو يسارًا، واضغط مرتين للتنفيذ"); title.setTextSize(18); title.setTextColor(0xFFFFFFFF); race_actions_container.addView(title)
    for i,label in ipairs(raceActionOptions) do local b=addRaceButton(race_actions_container,label,function() GameState.actionMenuIndex=i; chooseRaceAction() end,label,false,true); if i==GameState.actionMenuIndex then b.setSelected(true); pcall(function() b.setBackgroundColor(0xFF3F6F9F) end) end end
    race_actions_container.setVisibility(View.VISIBLE)
  else race_actions_container.setVisibility(View.GONE) end
end

customDurationValue=customDurationValue or ""
customDurationUnit=customDurationUnit or "دقائق"
customDurationInput=nil
function readCustomDurationInput()
  if customDurationInput then pcall(function() customDurationValue=tostring(customDurationInput.getText().toString()) end) end
  return tostring(customDurationValue or "")
end
local function renderCustomRaceDuration()
  if not screen_options_container then return end
  screen_options_container.removeAllViews(); screen_options_container.setVisibility(View.VISIBLE)
  local panel=LinearLayout(activity); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(8,4,8,4)
  local info=TextView(activity); info.setText("اكتب عددًا من 1 إلى 12 للدقائق، أو من 60 إلى 720 للثواني. اختر الوحدة ثم تطبيق المدة."); info.setTextSize(16); info.setTextColor(0xFFFFFFFF); panel.addView(info,LinearLayout.LayoutParams(-1,-2))
  customDurationInput=EditText(activity); customDurationInput.setHint("العدد"); customDurationInput.setSingleLine(true); customDurationInput.setText(customDurationValue or ""); customDurationInput.setTextSize(18); customDurationInput.setContentDescription("حقل عدد مدة السباق المخصصة"); pcall(function() customDurationInput.setInputType(2) end); panel.addView(customDurationInput,LinearLayout.LayoutParams(-1,-2))
  local optionsPanel=LinearLayout(activity); optionsPanel.setOrientation(LinearLayout.HORIZONTAL); panel.addView(optionsPanel,LinearLayout.LayoutParams(-1,64))
  local unitText=(customDurationUnit=="ثوانٍ") and "الوحدة: ثواني" or "الوحدة: دقائق"
  local unitButton=buttonFor(unitText,1); optionsPanel.addView(unitButton,LinearLayout.LayoutParams(0,-1,1))
  local applyButton=buttonFor("تطبيق المدة",2); optionsPanel.addView(applyButton,LinearLayout.LayoutParams(0,-1,1))
  local backButton=buttonFor("رجوع",3); optionsPanel.addView(backButton,LinearLayout.LayoutParams(0,-1,1))
  attachGameKeyboard(panel,{customDurationInput})
  screen_options_container.addView(panel,LinearLayout.LayoutParams(-1,-1))
end
function refreshVisibleScreen()
  if not screen_title or not screen_status then return end
  if startupGateActive then
    if race_controls then race_controls.setVisibility(View.GONE) end; if race_actions_container then race_actions_container.setVisibility(View.GONE) end; if race_car_image then race_car_image.setVisibility(View.GONE) end
    return
  end
  setText(screen_title, sectionTitles[currentSection] or "World Racing")
  if currentSection=="PLAYING" or currentSection=="PLAYING_MULTIPLAYER" then
    currentInternalDialog=nil
    if screen_options_container then screen_options_container.removeAllViews(); screen_options_container.setVisibility(View.GONE) end
    if screen_countdown then setText(screen_countdown, GameState.countdownActive and (GameState.countdownNumber > 0 and tostring(GameState.countdownNumber) or "انطلق") or "") end
    local opponent="\n"..tostring(GameState.opponentName or "الكمبيوتر")..": "..math.floor(GameState.opponentDistance or 0).." متر"; local diff=GameState.distance-(GameState.opponentDistance or 0); local lead=(diff>=0 and "متقدم" or "متأخر").." "..string.format("%.1f",math.abs(diff)).." متر"; local opponentLane="\nمسار المنافس: "..tostring(GameState.opponentLane or "غير معروف"); local incident=(GameState.opponentIncident or "")~="" and "\nآخر حالة للمنافس: "..GameState.opponentIncident or ""
    local pauseLine=(GameState.isPaused or Multiplayer.roomPaused) and "\nالحالة: السباق متوقف مؤقتًا" or "\nالحالة: السباق يعمل"
    local crateLine=(GameState.pendingCrate and GameState.pendingCrate.ready) and "\nالصندوق: متاح الآن، اسحب لأعلى من اليسار" or ((GameState.pendingCrate and "\nالصندوق: قادم" or "\nالصناديق المحفوظة: "..tostring(playerStats.rewardChests or 0))); local turnLine=(GameState.turnDirective or "")~="" and "\nالتوجيه القادم: انعطف "..GameState.turnDirective or "\nالتوجيه: الطريق مستقيم"; local roadLine="\nالطقس: "..tostring(GameState.weather or "صافي").." | الطريق: "..tostring(GameState.roadCondition or "جاف").." | التماسك: "..tostring(math.floor((GameState.traction or 1)*100)).."% | الغيار: "..tostring(GameState.currentGear or 1)
    setText(screen_status, string.format("السرعة: %d\nالمسافة: %d / %d\nالمسار: %d\nالوقود: %d%%\nالنيترو: %d\nصحة السيارة: %d\nالفارق: %s\nالزمن المتبقي: %d ثانية%s%s",math.floor(GameState.speed or 0),math.floor(GameState.distance or 0),math.floor((LevelsData[math.min(playerStats.level,#LevelsData)] or {}).goal or 0),GameState.lane or 0,math.floor(GameState.fuel or 0),playerStats.nitro or 0,math.floor(playerCarHp or 0),lead,math.floor(GameState.timeRemaining or 0),opponent,opponentLane,incident,turnLine,roadLine,pauseLine)..crateLine)
    if screen_options_container then screen_options_container.removeAllViews(); screen_options_container.setVisibility(View.GONE) end
    if race_car_image then race_car_image.setVisibility(View.VISIBLE); updateRaceCarVisual() end
    if race_controls then race_controls.setVisibility(View.VISIBLE) end; renderRaceControls(); if screen_hint then screen_hint.setVisibility(View.GONE); setText(screen_hint,"") end
    return
  end
  if screen_countdown then setText(screen_countdown, "") end
  if race_car_image then
    if currentSection=="MENU" then race_car_image.setVisibility(View.VISIBLE); pcall(function() race_car_image.setContentDescription("صورة World Racing. العنصر المحدد: "..tostring((currentMenuOptions or {})[currentMenuIndex] or "الخيار")..". اضغط مرتين في الصورة للتفعيل.") end)
    else race_car_image.setVisibility(View.GONE) end
  end
  if race_controls then race_controls.setVisibility(View.GONE) end; if race_actions_container then race_actions_container.setVisibility(View.GONE) end; if screen_hint then screen_hint.setVisibility(View.VISIBLE) end
  local connectionLabel=wrConnectionLabel and wrConnectionLabel() or tostring(Multiplayer.serverStatus or "غير متصل"); local steeringLabel=(currentSection=="PLAYING" or currentSection=="PLAYING_MULTIPLAYER") and ("    التوجيه: "..tostring(Settings.steeringMode or "السحب")) or ""; if not readerScreenWarningActive then if currentSection=="WIN" or currentSection=="LOSE" then setText(screen_status,tostring(GameState.lastRaceSummary or "تم حفظ نتيجة السباق. اختر موافق للمتابعة.").."\nالرصيد الحالي: "..tostring(playerStats.money or 0).."    الاتصال: "..connectionLabel) else setText(screen_status,"اللاعب: "..(playerStats.name~="" and playerStats.name or "غير مسجل").."    الرصيد: "..tostring(playerStats.money or 0).."    @"..(playerStats.username or "").."    الاتصال: "..connectionLabel..steeringLabel) end end
  if currentSection=="CUSTOM_RACE_DURATION" then
    renderCustomRaceDuration(); setText(screen_hint,"اللمسة للنطق، والضغطتان للتفعيل. اكتب المدة في الحقل الداخلي ثم اختر الوحدة والتطبيق."); return
  end
  if screen_options_container then
    screen_options_container.removeAllViews(); screen_options_container.setVisibility(View.VISIBLE)
    local bar=HorizontalScrollView(activity); bar.setHorizontalScrollBarEnabled(false); bar.setFillViewport(true)
    local row=LinearLayout(activity); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER)
    bar.addView(row,LinearLayout.LayoutParams(-2,-1)); screen_options_container.addView(bar,LinearLayout.LayoutParams(-1,-1))
    for i, option in ipairs(currentMenuOptions or {}) do
      local b=buttonFor(option,i); b.setMinWidth(280); if i==currentMenuIndex then b.setSelected(true); pcall(function() b.setBackgroundColor(0xFF355E85) end) end
      row.addView(b,LinearLayout.LayoutParams(-2,-1))
    end
  end
  local hint=isBlind() and "العناصر في شريط أفقي واحد. اسحب يمينًا أو يسارًا للتنقل، اللمسة تنطق، واضغط مرتين للتفعيل." or "العناصر في شريط أفقي واحد؛ المس الزر لتنفيذه."; setText(screen_hint,hint)
end

function updateRaceScreen() if currentSection=="PLAYING" or currentSection=="PLAYING_MULTIPLAYER" then refreshVisibleScreen() end end
