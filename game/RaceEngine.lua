-- RaceEngine.lua
-- محرك السباق: لا توجد شرطة أو إسعاف؛ التركيز على السباق والصوت والتنبيه.
handler = Handler(Looper.getMainLooper())

local function announceRaceClip(soundKey,cooldown,volume,force)
  if not Settings.announcerEnabled or not playAnnouncer then return false end
  local now=os.time()
  if not force and cooldown and now-(GameState.lastAnnouncerAt or 0)<cooldown then return false end
  local stream=playAnnouncer(soundKey,1.0,volume or Settings.announcerVolume or 0.82)
  if stream and stream~=0 then GameState.lastAnnouncerAt=now; return true end
  return false
end

local function scheduleNextRaceCrate()
  GameState.pendingCrate=nil; GameState.crateExpiresAt=0; GameState.nextCrateAt=(GameState.timeElapsed or 0)+math.random(8,18)
end

local function ensureRaceResultSummary()
  if tostring(GameState.lastRaceSummary or "")~="" then return end
  local outcome=tostring(GameState.raceOutcome or "خسارة"); local reason=tostring(GameState.lossReason or "لم تكتمل المسافة المطلوبة.")
  local tips=(GameState.lastRaceCollisionCount or 0)>2 and "طوّر الهيكل والفرامل من الورشة." or ((GameState.lastRaceFuel or 100)<25 and "اشترِ خزان وقود من محطة البنزين." or "حسّن السيارة من المتجر وجرّب تغيير المسار مبكرًا.")
  GameState.lastRaceSummary="النتيجة: "..outcome..". النقاط: "..tostring(math.floor(GameState.lastRaceScore or 0))..". العملات: "..tostring(math.floor(GameState.lastRaceReward or 0))..". المسافة: "..tostring(math.floor(GameState.lastRaceDistance or 0)).." متر. التجاوزات: "..tostring(math.floor(GameState.lastRaceOvertakes or GameState.overtakeCount or 0))..". الصناديق: "..tostring(math.floor(GameState.lastRaceCrates or 0))..". السبب أو الملاحظة: "..reason..". الاقتراح: "..tips
end
local function captureRaceSnapshot()
  GameState.lastRaceSpeed=math.floor(GameState.speed or 0)
  GameState.lastRaceDistance=math.floor(GameState.distance or 0)
  GameState.lastRaceFuel=math.floor(GameState.fuel or 0)
  GameState.lastRaceCrates=math.floor(GameState.collectedCrates or 0)
  GameState.lastRaceCollisionCount=math.floor(GameState.collisionCount or 0)
  GameState.lastRaceWeather=tostring(GameState.weather or "صافي")
  GameState.lastRaceRoad=tostring(GameState.roadCondition or "جاف")
  ensureRaceResultSummary(); pcall(saveGameData)
end

function collectRaceCrate()
  local crate=GameState.pendingCrate
  if not crate or not crate.ready then say("لا يوجد صندوق جاهز للاستلام الآن.",true); return false end
  if (GameState.timeElapsed or 0)>(GameState.crateExpiresAt or 0) then scheduleNextRaceCrate(); playDynamicSFX("a11y_error",0,1,0,0.55); say("انتهت مدة استلام الصندوق.",true); updateRaceScreen(); return false end
  GameState.pendingCrate=nil; GameState.crateExpiresAt=0; GameState.collectedCrates=(GameState.collectedCrates or 0)+1; playerStats.rewardChests=(playerStats.rewardChests or 0)+1; if missionsRecord then missionsRecord("crate",1) end
  playDynamicSFX("race_crate_collect",0,1,0,0.96); if SmartAnnouncer and SmartAnnouncer.commentary then SmartAnnouncer.commentary("race_event",playerStats.username,"تم استلام صندوق",false) end; say("تم استلام الصندوق، حفظ في الحقيبة. يمكنك فتحه بعد السباق.",true); saveGameData(); if firebaseSyncProfile then firebaseSyncProfile(function() end) end; scheduleNextRaceCrate(); updateRaceScreen(); return true
end

raceRunnable = Runnable{
  run = function()
    if (currentSection ~= "PLAYING" and currentSection ~= "PLAYING_MULTIPLAYER") or GameState.isCrashed then return end
    local actualLevel = math.min(playerStats.level, #LevelsData)
    local levelData = LevelsData[actualLevel]
    local tickSeconds=Settings.powerSaveMode and 0.8 or 0.5
    if GameState.isPaused or (currentSection=="PLAYING_MULTIPLAYER" and Multiplayer.roomPaused) then
      GameState.isPaused=true; GameState.isGasPressed=false; GameState.gasPressure=0; stopEngineSound(); updateRaceScreen(); handler.postDelayed(raceRunnable, Settings.powerSaveMode and 800 or 500); return
    end
    if not GameState.countdownActive then
      GameState.timeElapsed=(GameState.timeElapsed or 0)+tickSeconds
      local timeLimit=math.max(60,math.min(720,tonumber(GameState.raceDurationSeconds or Settings.raceDurationSeconds or Multiplayer.selectedDuration or 300) or 300)); GameState.timeRemaining=math.max(0,timeLimit-GameState.timeElapsed)
      if GameState.flightMode then
        if System.currentTimeMillis() >= (tonumber(GameState.flightEndsAt) or 0) then GameState.flightMode=false; playDynamicSFX("race_finish_chime",0,1,0,0.72); say("انتهى وضع الطيران. استمر في القيادة.",false)
        else GameState.speed=math.min(GameState.maxSpeed+110,math.max(GameState.speed or 0,GameState.maxSpeed*0.85)); GameState.distance=math.max(GameState.distance or 0,(GameState.opponentDistance or 0)+5) end
      end
      if not GameState.announced30 and (GameState.timeRemaining or 0)<=30 and (GameState.timeRemaining or 0)>0 then
        GameState.announced30=true; announceRaceClip("announcer_30sec",0,0.78,true); if SmartAnnouncer and SmartAnnouncer.commentary then SmartAnnouncer.commentary("time",playerStats.username,"متبقي 30 ثانية",true) end
      end
      local botRacers=GameState.botRacers or {}
      if #botRacers>0 then
        local farthest=0; local leadName=""; local leadSpeed=0; local nowElapsed=GameState.timeElapsed or 0
        for _,bot in ipairs(botRacers) do
          local skill=math.max(0.35,math.min(0.88,tonumber(bot.skill) or 0.55)); local targetSpeed=GameState.maxSpeed*(0.56+skill*0.34)*(tonumber(bot.traction) or GameState.surfaceGrip or 1.0)
          if nowElapsed>=(tonumber(bot.nextDecisionAt) or 0) then
            bot.nextDecisionAt=nowElapsed+math.random(2,5); local mistakeChance=math.max(0.05,0.24-skill*0.16)
            if math.random()<mistakeChance then bot.mistakeUntil=nowElapsed+math.random(1,3); bot.lane=math.max(-1,math.min(1,(tonumber(bot.lane) or 0)+(math.random()>0.5 and 1 or -1))); GameState.opponentIncident="المنافس أخطأ في المسار"; playDynamicSFX("race_near_miss",0,0.92,bot.lane or 0,0.48); if SmartAnnouncer and SmartAnnouncer.commentary then SmartAnnouncer.commentary("race_event",bot.name,"المنافس أخطأ في المسار",false) end
            else bot.lane=math.random(-1,1) end
          end
          if nowElapsed>=(tonumber(bot.nextIncidentAt) or 0) then
            bot.nextIncidentAt=nowElapsed+math.random(16,28); if math.random()<0.34 then bot.collisionCount=(tonumber(bot.collisionCount) or 0)+1; bot.speed=math.max(20,(tonumber(bot.speed) or targetSpeed)*0.48); bot.mistakeUntil=nowElapsed+2.5; GameState.opponentCollisionCount=bot.collisionCount; GameState.opponentIncident="المنافس اصطدم وتعطل قليلًا"; playDynamicSFX("race_opponent_crash",0,0.92,bot.lane or 0,0.56); if SmartAnnouncer and SmartAnnouncer.commentary then SmartAnnouncer.commentary("race_event",bot.name,"المنافس اصطدم",false) end end
          end
          local penalty=(nowElapsed<(tonumber(bot.mistakeUntil) or 0)) and 0.56 or 1.0; local acceleration=math.max(3,GameState.maxSpeed*0.018)*(tonumber(bot.traction) or 1.0); bot.speed=math.min(targetSpeed,(tonumber(bot.speed) or 0)+acceleration); bot.speed=math.max(15,bot.speed*penalty)
          bot.distance=(tonumber(bot.distance) or 0)+(bot.speed/200)
          if bot.distance>farthest then farthest=bot.distance; leadName=bot.name or "المنافس الآلي"; leadSpeed=bot.speed; GameState.opponentLane=bot.lane or 0 end
          if bot.distance>=levelData.goal and GameState.distance<levelData.goal then
            GameState.lossReason="سبقك "..tostring(bot.name or "المنافس الآلي").." إلى خط النهاية"
            if firebasePublishRaceEvent then firebasePublishRaceEvent("win",{score=0,reason="bot_finished",bot=bot.name},function() end) end; announceRaceClip("announcer_loss",0,0.76,true); say(GameState.lossReason..".",true); GameState.isCrashed=true; stopEngineSound(); captureRaceSnapshot(); loadSection("LOSE"); return
          end
        end
        GameState.opponentDistance=farthest; GameState.opponentName=leadName; GameState.opponentSpeed=leadSpeed
      end
    end

    if GameState.isEngineOn then
      local throttle=math.max(0,math.min(1,tonumber(GameState.gasPressure) or (GameState.isGasPressed and 1 or 0))); GameState.fuel = GameState.fuel - ((GameState.speed / 10000) * (0.55 + throttle*0.90))
      if GameState.fuel < 20 and not GameState.fuelWarned then
        say("تحذير، الوقود منخفض.", true); GameState.fuelWarned = true
      elseif GameState.fuel <= 0 then
        GameState.isEngineOn = false; GameState.speed = 0; stopEngineSound(); announceRaceClip("announcer_loss",0,0.88,true); say("نفد الوقود.", true); captureRaceSnapshot(); loadSection("LOSE"); return
      end

      local surfaceGrip=math.max(0.70,math.min(1.0,tonumber(GameState.surfaceGrip) or 1.0)); local carGrip=math.max(0.55,math.min(1.0,tonumber(GameState.activeCarGrip) or 0.70)); GameState.traction=math.max(0.58,math.min(1.0,surfaceGrip*(0.78+carGrip*0.22)))
      if GameState.isGasPressed then GameState.gasPressure=math.min(1,(GameState.gasPressure or 0)+0.08); throttle=GameState.gasPressure; GameState.engineTemp = GameState.engineTemp + (0.16 + throttle*0.24)
      else GameState.gasPressure=math.max(0,(GameState.gasPressure or 0)-0.14); throttle=GameState.gasPressure; GameState.engineTemp = math.max(50, GameState.engineTemp - 0.6) end
      if GameState.engineTemp > 100 and not GameState.tempWarned then
        say("الحرارة مرتفعة.", true); GameState.tempWarned = true
      elseif GameState.engineTemp > 120 then
        GameState.isEngineOn = false; GameState.speed = 0; stopEngineSound(); announceRaceClip("announcer_loss",0,0.76,true); say("احترق المحرك.", true); captureRaceSnapshot(); loadSection("LOSE"); return
      elseif GameState.engineTemp < 90 then GameState.tempWarned = false end

      local speedRatio=(GameState.speed or 0)/math.max(1,GameState.maxSpeed or 1); local targetGear=math.max(1,math.min(6,math.floor(speedRatio*6)+1))
      if targetGear~=(GameState.currentGear or 1) then local oldGear=GameState.currentGear or 1; GameState.currentGear=targetGear; playDynamicSFX(targetGear>oldGear and "race_gear_up" or "race_gear_down",0,0.86+targetGear*0.05,0,0.58) end
      local laneChanged=(GameState.lane or 0)~=(GameState.lastLane or 0)
      if laneChanged then
        local nowElapsed=GameState.timeElapsed or 0; local since=nowElapsed-(GameState.lastLaneChangeAt or 0); GameState.lastLane=GameState.lane; GameState.lastLaneChangeAt=nowElapsed
        if since<0.9 and GameState.speed>GameState.maxSpeed*0.45 then local slideLoss=(1-GameState.traction)*0.18+0.05; GameState.speed=math.max(0,GameState.speed*(1-slideLoss)); playDynamicSFX("drift",0,0.92,GameState.lane or 0,0.62); say(GameState.roadCondition=="جاف" and "مناورة حادة، خفف السرعة." or "المسار زلق، ثبت السيارة.",false) end
      end
      if GameState.nitroActive then
        GameState.speed = math.min(GameState.maxSpeed + 80, GameState.speed + 18*GameState.traction)
      elseif GameState.isGasPressed then
        local acceleration=(5+(11*(GameState.gasPressure or 0)))*(0.70+GameState.traction*0.30); GameState.speed=math.min(GameState.maxSpeed*GameState.traction,GameState.speed+acceleration)
      elseif GameState.isBrakePressed then
        local brakePower=52+(GameState.speed>GameState.maxSpeed*0.55 and 18 or 0); GameState.speed=math.max(0,GameState.speed-brakePower); GameState.engineTemp=math.max(50,GameState.engineTemp-0.28)
      else GameState.speed=math.max(0,GameState.speed-(7+(1-GameState.traction)*8)) end

      updateEngineFeedback(GameState.speed,GameState.maxSpeed,GameState.isGasPressed,GameState.gasPressure)
      GameState.distance = GameState.distance + (GameState.speed / 200); playerStats.bestSpeed=math.max(playerStats.bestSpeed or 0,math.floor(GameState.speed)); if achievementsRecordRaceTick then achievementsRecordRaceTick(GameState.distance,GameState.speed) end; if SmartAnnouncer and SmartAnnouncer.update then SmartAnnouncer.update({distance=GameState.distance,goal=levelData.goal,timeRemaining=GameState.timeRemaining,speed=GameState.speed}) end

      local gap=(GameState.opponentDistance or 0)-(GameState.distance or 0)
      if Settings.opponentProximityEnabled~=false and math.abs(gap)<=30 and (GameState.timeElapsed or 0)>=(GameState.nextProximityAt or 0) then
        local pan=gap>=0 and 1 or -1; local proximity=math.max(0,1-math.abs(gap)/30); local passVolume=0.16+proximity*0.46; local proximitySound=math.abs(gap)>18 and (pan>0 and "race_opponent_far_right" or "race_opponent_far_left") or (pan>0 and "race_opponent_pass_right" or "race_opponent_pass_left"); playDynamicSFX(proximitySound,0,0.78+proximity*0.42,pan,passVolume); GameState.nextProximityAt=(GameState.timeElapsed or 0)+5
        if math.abs(gap)<=7 then local relation=gap>=0 and "أمامك" or "خلفك"; say("المنافس قريب "..relation.." بفارق "..tostring(math.max(1,math.floor(math.abs(gap)))).." متر.",false) end
      end
      if (GameState.timeElapsed or 0)>=(GameState.nextStatusReportAt or 15) then
        local ownLeft=math.max(0,math.floor(levelData.goal-(GameState.distance or 0))); local rivalLeft=math.max(0,math.floor(levelData.goal-(GameState.opponentDistance or 0))); local relation=gap>=0 and "المنافس متقدم" or "أنت متقدم"
        say("تقرير: "..relation.."، الفارق "..tostring(math.floor(math.abs(gap))).." متر، المتبقي "..tostring(ownLeft).." متر.",false)
        GameState.nextStatusReportAt=(GameState.timeElapsed or 0)+math.max(10,tonumber(Settings.periodicRaceReportSeconds) or 15)
      end

      -- مزامنة فعلية مع حالة الغرفة: كل لاعب ينشر حالته ويقرأ حالات المنافسين من الخادم.
      if currentSection == "PLAYING_MULTIPLAYER" and (not GameState.lastSyncAt or os.time() - GameState.lastSyncAt >= 1) and not GameState.raceSyncInFlight then
        GameState.lastSyncAt = os.time(); GameState.raceSyncInFlight = true
        firebaseSyncRaceState(GameState.distance, GameState.speed, GameState.lane, false, function(ok, players)
          GameState.raceSyncInFlight = false; Multiplayer.lastSyncOk = ok == true
          if ok and players then
            local farthest = 0; local leader = ""
            for username,state in pairs(players) do
              if username ~= playerStats.username and (state.distance or 0) > farthest then farthest = state.distance; leader = username end
            end
            Multiplayer.opponentDistance = farthest; Multiplayer.opponentName = leader ~= "" and leader or Multiplayer.opponentName
          end
        end)
      end

      -- توجيه مسار استباقي: يحدد الانعطاف قبل الوصول ولا يطلبه بعد فواته.
      if not GameState.nextTurnAt then GameState.nextTurnAt=(GameState.distance or 0)+math.random(55,95) end
      if not GameState.turnDirective or GameState.turnDirective=="" then
        if GameState.distance >= (GameState.nextTurnAt-12) then
          GameState.turnTargetLane=math.random()>0.5 and 1 or -1; GameState.turnDirective=GameState.turnTargetLane==1 and "يمين" or "يسار"; playDynamicSFX(GameState.turnTargetLane==1 and "race_turn_right" or "race_turn_left",0,1.0,GameState.turnTargetLane,0.82); say("استعد للانعطاف "..GameState.turnDirective.." بعد قليل.",true)
        end
      elseif GameState.distance >= GameState.nextTurnAt then
        if GameState.lane==GameState.turnTargetLane then say("انعطاف "..GameState.turnDirective.." ناجح.",false); playDynamicSFX("race_miss_success",0,1.0,GameState.turnTargetLane,0.72)
        else GameState.speed=math.max(0,GameState.speed*0.72); playDynamicSFX("race_miss_fail",0,0.92,GameState.turnTargetLane,0.82); say("فاتك انعطاف "..GameState.turnDirective..". خففت السرعة.",true) end
        GameState.turnDirective=""; GameState.turnTargetLane=nil; GameState.nextTurnAt=GameState.distance+math.random(55,95)
      end

      -- توليد صناديق مكافآت عشوائية مستقلة عن تفادي العقبات.
      if not GameState.pendingCrate and (GameState.timeElapsed or 0)>=(GameState.nextCrateAt or 0) then
        local crateLane=math.random(-1,1); GameState.pendingCrate={dist=GameState.distance+math.max(3.0,(GameState.speed/200)*3+1),lane=crateLane,type="صندوق مكافأة",ready=false}; playDynamicSFX("race_crate_appear",0,1.0,crateLane,0.86); say("صندوق مكافأة قادم. استعد للسحب لأعلى لاستلامه.",true)
      end
      if GameState.pendingCrate then
        local crate=GameState.pendingCrate; local crateDistance=(crate.dist or GameState.distance)-GameState.distance
        if not crate.ready and crateDistance<=math.max(0.8,(GameState.speed/200)*0.8) then crate.ready=true; GameState.crateExpiresAt=(GameState.timeElapsed or 0)+10; playDynamicSFX("race_crate_appear",0,1,crate.lane or 0,0.92); if SmartAnnouncer and SmartAnnouncer.commentary then SmartAnnouncer.commentary("race_event",playerStats.username,"صندوق متاح",false) end; say("الصندوق متاح الآن لمدة عشر ثوانٍ. اسحب لأعلى من جهة اليسار لاستلامه.",true)
        elseif crate.ready and (GameState.timeElapsed or 0)>(GameState.crateExpiresAt or 0) then GameState.pendingCrate=nil; GameState.crateExpiresAt=0; playDynamicSFX("race_crate_expire",0,1,0,0.86); if SmartAnnouncer and SmartAnnouncer.commentary then SmartAnnouncer.commentary("race_event",playerStats.username,"انتهت فرصة الصندوق",false) end; say("انتهت مدة الصندوق.",false); scheduleNextRaceCrate() end
      end

      -- عقبات سباق فقط؛ مسافة التحذير تحسب على أساس سرعة اللاعب في الثانية لتصل قبل ثلاث ثوانٍ تقريبًا.
      if (GameState.timeElapsed or 0)>=(GameState.nextObstacleAt or 0) and GameState.speed > 50 and (currentSection == "PLAYING" or currentSection == "PLAYING_MULTIPLAYER") then
        local lane = math.random(-1, 1); local warningSeconds=3
        local types = {"سيارة منافسة", "حاجز", "حاجز مطاطي", "مخروط سباق"}
        local obstacle = {dist = GameState.distance + math.max(4.0, (GameState.speed / 100) * warningSeconds + 1.0), lane = lane, type = types[math.random(1, #types)], passed = false, warned = false, warningSeconds=warningSeconds}
        GameState.nextObstacleAt=(GameState.timeElapsed or 0)+math.random(5,8); table.insert(GameState.obstacles, obstacle)
        playDynamicSFX("race_obstacle_far", 0, 1.0, lane, 0.58)
        local side = lane == 1 and "يمين" or (lane == -1 and "يسار" or "منتصف")
        GameState.lastObstacleNotice=obstacle.type.." في ال"..side
      end

      for i = #GameState.obstacles, 1, -1 do
        local o = GameState.obstacles[i]
        local d = o.dist - GameState.distance
        local lookAhead = math.max(1.0, (GameState.speed / 100) * (tonumber(o.warningSeconds) or Settings.warningSeconds or 3))
        if d > 0 and d <= lookAhead and not o.warned then
          o.warned = true
          local side = o.lane == 1 and "يمين" or (o.lane == -1 and "يسار" or "منتصف")
          playDynamicSFX("race_obstacle_close", 0, 1.0, o.lane, 0.96); announceRaceClip("announcer_caution",6,0.76,false); if SmartAnnouncer and SmartAnnouncer.commentary then SmartAnnouncer.commentary("race_event",playerStats.username,"تنبيه، "..o.type.." في ال"..side,false) end
          say("تنبيه، " .. o.type .. " في ال" .. side .. ". أمامك " .. math.max(1, math.floor(d / math.max(0.1, GameState.speed / 100))) .. " ثوانٍ.", true)
        elseif d > 0 and d <= math.max(1.0,lookAhead*0.55) and math.abs(GameState.lane-o.lane)==1 and not o.sideWarned then
          o.sideWarned=true; local sideDirection=o.lane==1 and 1 or -1; playDynamicSFX("side_signal",0,0.95, sideDirection,0.55); if o.type=="سيارة منافسة" and firebasePublishRaceEvent then firebasePublishRaceEvent("nearby",{direction=sideDirection,relativePosition="side",soundKey="nearby",opponentType=o.type},function() end) end; say("مركبة بجوارك من ال"..(o.lane==1 and "يمين" or "يسار"),false); if SmartAnnouncer and SmartAnnouncer.commentary then SmartAnnouncer.commentary("race_event",playerStats.username,"مركبة بجوارك",false) end
        end
        if d > 0 and d < 0.4 and GameState.lane ~= o.lane and not o.passed then o.passed = true; GameState.overtakeCount=(GameState.overtakeCount or 0)+1; if missionsRecord then missionsRecord("overtake",1) end; local passDirection=o.lane==1 and 1 or -1; if firebasePublishRaceEvent then firebasePublishRaceEvent(o.type=="سيارة منافسة" and "opponent_pass" or "overtake",{overtakeCount=GameState.overtakeCount,direction=passDirection,relativePosition="front",soundKey=(passDirection<0 and "race_opponent_pass_left" or "race_opponent_pass_right")},function() end) end; playDynamicSFX("race_overtake", 0, 1.0, passDirection, 0.96); if SmartAnnouncer and SmartAnnouncer.commentary then SmartAnnouncer.commentary("overtake",playerStats.username,"تجاوز رقم "..tostring(GameState.overtakeCount),false) end; if SmartAnnouncer and SmartAnnouncer.banter then SmartAnnouncer.banter("overtake",playerStats.username,"تجاوز رقم "..tostring(GameState.overtakeCount),false) end end
        if d <= 0 then
          if GameState.lane == o.lane and not GameState.flightMode then
            local damageField = string.find(tostring(o.type),"حاجز",1,true) and "body" or (o.type=="سيارة منافسة" and "engine" or "tires")
            playerStats.carDamage[damageField]=math.min(100,(playerStats.carDamage[damageField] or 0)+20); GameState.collisionCount=math.min(GameState.maxCollisions or 7,(GameState.collisionCount or 0)+1); playerCarHp=math.max(0,100-((GameState.collisionCount or 0)*(100/(GameState.maxCollisions or 7)))); GameState.speed = GameState.speed / 2; if firebasePublishRaceEvent then firebasePublishRaceEvent("collision",{collisionCount=GameState.collisionCount,obstacle=o.type,severity=(damageField=="body" and "heavy" or "light"),direction=GameState.lane,soundKey=(damageField=="body" and "race_collision_heavy" or "race_collision_light")},function() end) end; playDynamicSFX(damageField=="body" and "race_body_hit" or "race_tire_hit", 0, 1.0, 0.0, 0.98)
            if Settings.vibrationEnabled and vibrator then pcall(function() vibrator.vibrate(350) end) end
            if SmartAnnouncer and SmartAnnouncer.banter then SmartAnnouncer.banter("collision",playerStats.username,"اصطدام رقم "..tostring(GameState.collisionCount),false) end; if GameState.collisionCount >= (GameState.maxCollisions or 7) then GameState.isCrashed = true; stopEngineSound(); announceRaceClip("announcer_loss",0,0.88,true); say("انتهت نقاط تحمل السيارة بعد سبع اصطدامات.", true); if SmartAnnouncer and SmartAnnouncer.banter then SmartAnnouncer.banter("loss",playerStats.username,"",true) end; captureRaceSnapshot(); loadSection("LOSE"); return else say("اصطدام رقم "..tostring(GameState.collisionCount).." من سبعة. تبقى "..tostring((GameState.maxCollisions or 7)-GameState.collisionCount).." اصطدامات.", true) end
          elseif math.abs(GameState.lane - o.lane) == 1 or GameState.flightMode then
            playerStats.money = playerStats.money + math.floor(150 * playerStats.skills.moneyMultiplier); GameState.overtakeCount=(GameState.overtakeCount or 0)+1; if missionsRecord then missionsRecord("overtake",1) end; if firebasePublishRaceEvent then firebasePublishRaceEvent("overtake",{overtakeCount=GameState.overtakeCount,direction=(o.lane==1 and 1 or -1),relativePosition="side",soundKey="race_miss_success"},function() end) end; playDynamicSFX("race_miss_success", 0, 1.0, 0.0, 0.96); say("مراوغة ممتازة.", false); if SmartAnnouncer and SmartAnnouncer.commentary then SmartAnnouncer.commentary("overtake",playerStats.username,"مراوغة ممتازة، تجاوز رقم "..tostring(GameState.overtakeCount),false) end; if SmartAnnouncer and SmartAnnouncer.banter then SmartAnnouncer.banter("overtake",playerStats.username,"مراوغة ممتازة",false) end; saveGameData()
          end
          table.remove(GameState.obstacles, i)
        end
      end

      if GameState.lane > 1 then
        GameState.collisionCount=math.min(GameState.maxCollisions or 7,(GameState.collisionCount or 0)+1); playerStats.carDamage.tires=math.min(100,(playerStats.carDamage.tires or 0)+12); playerCarHp=math.max(0,100-((GameState.collisionCount or 0)*(100/(GameState.maxCollisions or 7)))); if firebasePublishRaceEvent then firebasePublishRaceEvent("collision",{collisionCount=GameState.collisionCount,obstacle="حافة الطريق",severity="light",direction=1,soundKey="race_collision_light"},function() end) end; say("حافة الطريق يميناً. اصطدام رقم "..tostring(GameState.collisionCount).." من سبعة.", false); GameState.lane = 1; GameState.speed = GameState.speed / 2; playDynamicSFX("race_collision_light", 0, 1.0, 1.0, 0.96)
      elseif GameState.lane < -1 then
        GameState.collisionCount=math.min(GameState.maxCollisions or 7,(GameState.collisionCount or 0)+1); playerStats.carDamage.tires=math.min(100,(playerStats.carDamage.tires or 0)+12); playerCarHp=math.max(0,100-((GameState.collisionCount or 0)*(100/(GameState.maxCollisions or 7)))); if firebasePublishRaceEvent then firebasePublishRaceEvent("collision",{collisionCount=GameState.collisionCount,obstacle="حافة الطريق",severity="light",direction=-1,soundKey="race_collision_light"},function() end) end; say("حافة الطريق يساراً. اصطدام رقم "..tostring(GameState.collisionCount).." من سبعة.", false); GameState.lane = -1; GameState.speed = GameState.speed / 2; playDynamicSFX("race_collision_light", 0, 1.0, -1.0, 0.96)
      end
      if (GameState.collisionCount or 0) >= (GameState.maxCollisions or 7) and not GameState.isCrashed then GameState.isCrashed = true; stopEngineSound(); announceRaceClip("announcer_loss",0,0.88,true); say("تحطمت السيارة بعد سبع اصطدامات.", true); captureRaceSnapshot(); loadSection("LOSE"); return end

      -- تقرير الحالة يبقى كلامًا ديناميكيًا، أما عبارات التشجيع الجاهزة فتُشغّل من المذيع.
      if currentSection == "PLAYING_MULTIPLAYER" then
        local diff = GameState.distance - (Multiplayer.botEnabled and GameState.opponentDistance or (Multiplayer.opponentDistance or 0))
        if diff >= 20 and not GameState.leadAnnounced then GameState.leadAnnounced=true; announceRaceClip("announcer_lead",10,0.80,false); if SmartAnnouncer and SmartAnnouncer.banter then SmartAnnouncer.banter("lead",playerStats.username,"الفارق "..math.floor(diff).." متر",false) end
        elseif diff <= -20 and not GameState.behindAnnounced then GameState.behindAnnounced=true; announceRaceClip("announcer_behind",10,0.80,false); if SmartAnnouncer and SmartAnnouncer.banter then SmartAnnouncer.banter("behind",playerStats.username,"الفارق "..math.floor(math.abs(diff)).." متر",false) end end
        if math.random() > 0.93 and SmartAnnouncer and SmartAnnouncer.commentary then SmartAnnouncer.commentary(diff>=0 and "lead" or "behind",playerStats.username,"الفارق "..math.floor(math.abs(diff)).." متر",false) end
      elseif math.random() > 0.95 then
        if SmartAnnouncer and SmartAnnouncer.commentary then SmartAnnouncer.commentary("race_event",playerStats.username,"السرعة "..math.floor(GameState.speed).."، المتبقي "..math.max(0,math.floor(levelData.goal-GameState.distance)).." متر",false) else say("السرعة " .. math.floor(GameState.speed) .. ". المتبقي " .. math.max(0, math.floor(levelData.goal - GameState.distance)) .. " متر. الوقت المتبقي " .. math.floor(GameState.timeRemaining or 0) .. " ثانية.", false) end; if SmartAnnouncer and SmartAnnouncer.banter and math.random()>0.55 then SmartAnnouncer.banter("live",playerStats.username,"",false) end
      end

      if (GameState.timeRemaining or 1) <= 0 and GameState.distance < levelData.goal then
        announceRaceClip("announcer_loss",0,0.88,true); say("انتهى الوقت. حاول مرة أخرى.",true); GameState.isCrashed=true; stopEngineSound(); captureRaceSnapshot(); loadSection("LOSE"); return
      end
      if GameState.distance >= levelData.goal then
        if currentSection == "PLAYING" and levelData.hasBoss and GameState.speed < levelData.bossSpeed then
          announceRaceClip("announcer_loss",0,0.88,true); say("لم تتجاوز سرعة الزعيم.", true); playerCarHp = 0; GameState.isCrashed = true; stopEngineSound(); captureRaceSnapshot(); loadSection("LOSE"); return
        end
        local reward = math.floor(levelData.reward * playerStats.skills.moneyMultiplier)
        local score = math.floor(GameState.distance * math.max(1, GameState.speed))
        GameState.raceOutcome="فوز"; GameState.lastRaceScore=score; GameState.lastRaceReward=reward; GameState.lossReason=""; GameState.lastRaceOvertakes=GameState.overtakeCount or 0
        playerStats.money = playerStats.money + reward; playerStats.level = math.min(#LevelsData, playerStats.level + 1); playerStats.wins = (playerStats.wins or 0) + 1
        GameState.lastRaceOvertakes=GameState.overtakeCount or 0; GameState.lastRaceSummary="النتيجة: فوز. النقاط: "..tostring(score)..". العملات: "..tostring(reward)..". المسافة: "..tostring(math.floor(GameState.distance)).." متر. التجاوزات: "..tostring(GameState.lastRaceOvertakes)..". الصناديق: "..tostring(math.floor(GameState.collectedCrates or 0))..". الاقتراح: حافظ على الوقود وطوّر الإطارات للوصول إلى زمن أفضل."; playerStats.bestDistance = math.max(playerStats.bestDistance or 0, math.floor(GameState.distance)); playerStats.bestScore = math.max(playerStats.bestScore or 0, score)
        if firebasePublishRaceEvent then firebasePublishRaceEvent("win",{score=score,reward=reward},function() end) end; announceRaceClip("announcer_win",0,0.90,true); if SmartAnnouncer and SmartAnnouncer.finish then SmartAnnouncer.finish(true,score) end; say("فزت. حصلت على " .. reward .. " عملة." .. ((GameState.collectedCrates or 0)>0 and " تم حفظ "..tostring(GameState.collectedCrates).." صندوقًا في الحقيبة." or ""), true)
        if achievementsRecordRaceFinish then achievementsRecordRaceFinish(true,score,GameState.raceDurationSeconds,currentSection) end; GameState.raceOutcomeRecorded=true; saveGameData(); playDynamicSFX("race_finish_line", 0, 1, 0, 0.98); firebasePublishRaceResult("finished",score,function() end); firebaseSubmitLeaderboard(); stopEngineSound(); captureRaceSnapshot(); loadSection("WIN"); return
      end
    end
    updateRaceScreen()
    handler.postDelayed(raceRunnable, Settings.powerSaveMode and 800 or 500)
  end
}

function startRaceCountdown(sharedStartAt,sharedStartAtMs,sessionId)
  handler.removeCallbacks(raceRunnable)
  GameState.isPaused=false; Multiplayer.roomPaused=false; GameState.lossReason=""; GameState.raceServerStartAtMs=tonumber(sharedStartAtMs or 0) or 0
  GameState.countdownActive = true; GameState.countdownNumber = 3; GameState.isEngineOn = false; GameState.isGasPressed = false; GameState.gasPressure = 0; GameState.gasPointerId = nil; GameState.isBrakePressed = false; if SmartAnnouncer and SmartAnnouncer.startRace then SmartAnnouncer.startRace() end
  playDynamicSFX("race_start_grid", 0, 1, 0, 0.65)
  local startAt=tonumber(sharedStartAt or 0) or 0; local startAtMs=tonumber(sharedStartAtMs or 0) or 0; if startAtMs<=0 and startAt>0 then startAtMs=startAt*1000 end
  local function nowMs() return (firebaseServerNowMs and firebaseServerNowMs()) or (os.time()*1000) end
  local function tick(number)
    if currentSection ~= "PLAYING" and currentSection ~= "PLAYING_MULTIPLAYER" then GameState.countdownActive=false; return end
    if sessionId and sessionId~="" and Multiplayer.raceSessionId and Multiplayer.raceSessionId~="" and Multiplayer.raceSessionId~=sessionId then GameState.countdownActive=false; return end
    GameState.countdownNumber = number; updateRaceScreen()
    local soundKey=number>0 and ("countdown_"..number) or "countdown_go"; local fallback=number>0 and tostring(number) or "انطلق"
    if Settings.announcerEnabled then
      if not announceRaceClip(soundKey,0,0.90,true) then say(fallback,true) end
    else playDynamicSFX("voice_beep",0,0.95+(3-number)*0.05,0,0.75) end
    if number > 0 then
      local nextDelay=1200
      if startAtMs>0 then nextDelay=math.max(0,startAtMs-nowMs()) end
      if number>1 and startAtMs>0 then nextDelay=math.min(1000,nextDelay) end
      Handler().postDelayed(Runnable({run=function() tick(number - 1) end}),nextDelay)
    else
      GameState.countdownActive = false; GameState.countdownNumber = 0; playDynamicSFX("race_intro",0,1,0,0.9); updateRaceScreen(); Handler().postDelayed(Runnable({run=function() startRaceLoop() end}),250)
    end
  end
  if startAtMs>0 then
    local remaining=startAtMs-nowMs(); local countdownStart=startAtMs-3000
    if remaining<=0 then tick(0) elseif nowMs()<countdownStart then Handler().postDelayed(Runnable({run=function() tick(3) end}),countdownStart-nowMs()) else local number=math.max(1,math.min(3,math.floor(remaining/1000)+1)); tick(number) end
  else
    local delay=math.max(0,(startAt-os.time()-3)*1000); if delay>0 then Handler().postDelayed(Runnable({run=function() tick(3) end}),delay) else tick(3) end
  end
end

function startRaceLoop()
  handler.removeCallbacks(raceRunnable); GameState.lastSyncAt = 0; handler.postDelayed(raceRunnable, 500)
end
