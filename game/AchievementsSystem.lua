-- AchievementsSystem.lua
-- مهام قصيرة وطويلة المدى مرتبطة بأحداث اللعبة والسباقات.

AchievementDefinitions={
  first_race={title="أول انطلاقة",description="أكمل سباقك الأول.",target=1,field="racesFinished",reward=500},
  three_wins={title="ثلاثة انتصارات",description="حقق ثلاثة انتصارات.",target=3,field="wins",reward=1000},
  five_wins={title="متسابق ثابت",description="حقق خمسة انتصارات.",target=5,field="wins",reward=2500},
  distance_1000={title="ألف متر",description="اقطع مسافة ألف متر في السباقات.",target=1000,field="distance",reward=600},
  distance_10000={title="عشرة آلاف متر",description="اقطع مسافة عشرة آلاف متر في السباقات.",target=10000,field="distance",reward=5000},
  speed_300={title="سرعة عالية",description="سجل سرعة 300 أو أكثر.",target=300,field="bestSpeed",reward=1800},
  multiplayer_race={title="روح المنافسة",description="أكمل سباقًا جماعيًا.",target=1,field="multiplayerFinished",reward=2000},
  robot_win={title="التفوق على الروبوت",description="انتصر في سباق ضد الروبوت.",target=1,field="robotWins",reward=2200},
  long_race={title="نَفَس طويل",description="أكمل سباقًا مدته خمس دقائق أو أكثر.",target=1,field="longRaces",reward=1200},
  social_room={title="عضو اجتماعي",description="أكمل سباقًا داخل غرفة مع لاعبين.",target=1,field="roomRaces",reward=1500},
  tournament_ready={title="جاهز للبطولة",description="أكمل سباقًا جماعيًا واستعد للتصفيات.",target=1,field="tournamentReady",reward=2500}
}

local achievementSyncAt=0

local function ensureAchievements()
  Achievements=Achievements or {}
  for id,def in pairs(AchievementDefinitions) do
    local row=Achievements[id]
    if type(row)~="table" then row={progress=0,completed=false}; Achievements[id]=row end
    row.progress=tonumber(row.progress) or 0; row.completed=row.completed==true; row.rewardClaimed=row.rewardClaimed==true
  end
end

local function valueFor(id,value)
  local def=AchievementDefinitions[id]; if not def then return 0 end
  if type(value)=="number" then return value end
  return tonumber(value) or 0
end

local function mark(id,value)
  ensureAchievements(); local def=AchievementDefinitions[id]; if not def then return false end
  local row=Achievements[id]; row.progress=math.max(row.progress,valueFor(id,value)); local justCompleted=false; row.rewardClaimed=row.rewardClaimed==true
  if row.progress>=def.target and not row.completed then
    row.completed=true; row.completedAt=os.time(); justCompleted=true
    playDynamicSFX("achievement_unlock",0,1,0,0.82); say("إنجاز جديد: "..def.title..". "..def.description,true)
  end
  if row.completed and not row.rewardClaimed then
    local reward=tonumber(def.reward) or 0
    if reward>0 then local before=playerStats.money or 0; playerStats.money=before+reward; row.rewardClaimed=true; playDynamicSFX("coin_reward",0,1,0,0.88); say("مكافأة المهمة: "..tostring(reward).." عملة.",true); saveGameData(); local opId="achievement_"..tostring(id).."_"..tostring(row.completedAt or os.time()); if firebaseRecordEconomyLedger then firebaseRecordEconomyLedger("achievement",reward,def.title,opId,function() end) end; if wrAudit then wrAudit("achievement_reward",{id=id,reward=reward,before=before,after=playerStats.money}) end end
  end
  return justCompleted
end

function achievementsInitialize()
  ensureAchievements()
end

function achievementsRecordRaceStart(mode,duration)
  ensureAchievements(); GameState.achievementRaceMode=mode or "solo"; GameState.achievementRaceDuration=tonumber(duration) or 120
end

function achievementsRecordRaceTick(distance,speed)
  ensureAchievements(); mark("distance_1000",distance); mark("distance_10000",distance); mark("speed_300",speed); local previous=tonumber(GameState.lastMissionDistance or 0) or 0; local delta=math.max(0,(tonumber(distance) or 0)-previous); GameState.lastMissionDistance=tonumber(distance) or previous; if missionsRecord and delta>0 then missionsRecord("distance",delta) end; if not GameState.lastAchievementSaveAt or os.time()-GameState.lastAchievementSaveAt>=10 then GameState.lastAchievementSaveAt=os.time(); saveGameData() end
end

function achievementsRecordRaceFinish(won,score,duration,mode)
  ensureAchievements(); mark("first_race",1); mark("distance_1000",playerStats.bestDistance or 0); mark("distance_10000",playerStats.bestDistance or 0); mark("speed_300",playerStats.bestSpeed or 0)
  if won then mark("three_wins",playerStats.wins or 0); mark("five_wins",playerStats.wins or 0) end
  if mode=="PLAYING_MULTIPLAYER" then mark("multiplayer_race",1); mark("social_room",1); mark("tournament_ready",1) end
  if Multiplayer.botEnabled and won then mark("robot_win",1) end
  if tonumber(duration or GameState.raceDurationSeconds or 0)>=300 then mark("long_race",1) end
  if missionsRecord then missionsRecord("finish",1); if won then missionsRecord("win",1) end; if mode=="PLAYING_MULTIPLAYER" then missionsRecord("social",1) end end
  saveGameData()
  if firebaseSyncProfile and os.time()-(achievementSyncAt or 0)>=3 then achievementSyncAt=os.time(); firebaseSyncProfile(function() end) end
end

function achievementsList()
  ensureAchievements(); local list={}
  for id,def in pairs(AchievementDefinitions) do
    local row=Achievements[id]; table.insert(list,{id=id,title=def.title,description=def.description,progress=row.progress,target=def.target,completed=row.completed,reward=def.reward or 0,rewardClaimed=row.rewardClaimed==true})
  end
  table.sort(list,function(a,b) if a.completed~=b.completed then return a.completed end return a.id<b.id end); return list
end

function achievementsSummary()
  local list=achievementsList(); local done=0
  for _,row in ipairs(list) do if row.completed then done=done+1 end end
  return done,#list
end

achievementsInitialize()
