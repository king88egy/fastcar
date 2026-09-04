-- SmartAnnouncer.lua
-- مذيع سباق ديناميكي: يركّب عبارات عربية جديدة بأسماء اللاعبين بدل الاعتماد على مقاطع ثابتة فقط.

SmartAnnouncer = {lastAt=0,lastKind="",lastLeader="",lastTimeMark=0,lastProgressMark=0}

local function displayName(username)
  username=tostring(username or "")
  if username=="" then return "المتسابق" end
  if username==playerStats.username and tostring(playerStats.name or "")~="" then return tostring(playerStats.name) end
  local profile=Multiplayer.remoteProfiles and Multiplayer.remoteProfiles[username] or nil
  if profile and tostring(profile.name or "")~="" then return tostring(profile.name) end
  return username
end

local function speak(text,interrupt)
  if not Settings.announcerEnabled or Settings.smartAnnouncerEnabled==false or Settings.ttsEnabled==false or not tts then return false end
  local ok=pcall(function()
    local queue=interrupt and TextToSpeech.QUEUE_FLUSH or TextToSpeech.QUEUE_ADD
    tts.setSpeechRate(math.max(0.05,tonumber(Settings.voiceSpeed) or 1.0)); tts.setPitch(math.max(0.05,tonumber(Settings.voicePitch) or 1.0)); tts.speak(tostring(text),queue,nil)
  end)
  if ok then SmartAnnouncer.lastAt=os.time(); return true end
  return false
end

local function canSpeak(kind,cooldown,force)
  local now=os.time()
  local elapsed=now-(SmartAnnouncer.lastAt or 0)
  if force and (kind=="commentary_race_start" or kind=="commentary_result") then SmartAnnouncer.lastKind=kind; SmartAnnouncer.lastAt=now; return true end
  if SmartAnnouncer.lastKind==kind and elapsed<cooldown then return false end
  if elapsed<5 then return false end
  SmartAnnouncer.lastKind=kind; SmartAnnouncer.lastAt=now; return true
end

local function bestOpponent(localDistance)
  local leaderUser=""; local leaderDistance=localDistance or 0
  for username,state in pairs(Multiplayer.remoteRacePlayers or {}) do
    if username~=playerStats.username and (tonumber(state.distance) or 0)>leaderDistance then
      leaderUser=username; leaderDistance=tonumber(state.distance) or 0
    end
  end
  return leaderUser,leaderDistance
end

SmartAnnouncer.commentarySets={
  race_start={"جاهزين يا {name}، السباق بدأ.","انطلق يا {name}، الطريق قدامك.","شد حيلك يا {name}، البداية مهمة.","بسم الله يا {name}، ورينا سرعتك.","كل العيون على السباق يا {name}.","ابدأ بثبات يا {name}، وخليك مركز."},
  lead={"الله عليك يا {name}، أنت في المقدمة.","يا سلام يا {name}، الصدارة معاك.","ممتاز يا {name}، حافظ على المركز الأول.","أداء قوي يا {name}، أنت سابق الجميع.","يا {name}، قيادتك نظيفة وسريعة.","الطريق ملكك يا {name}، كمل بنفس القوة."},
  push={"زود يا {name}، الفرصة في إيدك.","اضغط يا {name}، قربت من منافسك.","شد السرعة يا {name}، النيترو جاهز.","أداء ممتاز يا {name}، لا ترفع رجلك.","خليك هجومي يا {name}، السباق لسه طويل.","أنت قادر يا {name}، ادخل المنعطف بثقة."},
  behind={"شد حيلك يا {name}، المنافس متقدم.","لسه في وقت يا {name}، ارجع للمنافسة.","ركز يا {name}، الفارق ممكن يتعوض.","ما تستسلمش يا {name}، الطريق لسه مفتوح.","خفف الأخطاء يا {name}، وابدأ المطاردة.","المركز قدامك يا {name}، استغل كل متر."},
  overtake={"يا سلام يا {name}، تجاوز رائع.","الله عليك يا {name}، عدّيت المنافس بذكاء.","مناورة ممتازة يا {name}، كمل.","تجاوز نظيف يا {name}، حافظ على خطك.","برافو يا {name}، استعد للمنافس التالي.","سرعة وتركيز يا {name}، لقطة جميلة."},
  near_finish={"اقتربت من النهاية يا {name}، لا تضيّع الصدارة.","آخر الأمتار يا {name}، ركز في الخط.","النهاية أمامك يا {name}، حافظ على سرعتك.","أنت قريب جدًا يا {name}، أكمل بثبات.","باقي القليل يا {name}، لا تتوتر.","خط النهاية يناديك يا {name}، هات آخر ما عندك."},
  time={"يا {name}، الوقت بيجري، خليك مركز.","متبقي وقت قليل يا {name}، ارفع الإيقاع.","يا {name}، كل ثانية مهمة الآن.","اقتربت لحظة الحسم يا {name}.","العداد ينزل يا {name}، لا تترك فرصة.","حافظ على تركيزك يا {name}، النهاية قريبة."},
  race_event={"المحرك مستجيب يا {name}.","الطريق مزدحم، خليك صاحي يا {name}.","منعطف حاد أمامك يا {name}.","إشارة جانبية، انتبه للمنافس يا {name}.","فرملة محسوبة يا {name}، تحكم ممتاز.","المنافس قريب جدًا يا {name}، حافظ على مسارك."},
  flight={"وضع الطيران فعّال يا {name}، تجاوز نظيف فوق المنافس.","يا {name}، طلعت فوق المنافس، استغل الثواني بحساب.","قفزة ذكية يا {name}، ارجع للمسار بثبات بعد الهبوط."},
  engine={"المحرك اشتغل يا {name}، خليك جاهز للحركة.","صوت المحرك مضبوط، الطريق ينتظر قرارك يا {name}.","البنزين يستجيب يا {name}، راقب المسار."},
  result={"الله عليك يا {name}، فوز مستحق.","أداء قوي يا {name}، نتيجة تليق بك.","أحسنت يا {name}، سباق ممتاز.","انتهى السباق يا {name}، خذ نفسًا واستعد للجولة القادمة.","ليست النهاية يا {name}، تعلم من السباق القادم.","حاول مرة أخرى يا {name}، كل سباق يرفع مستواك."},
  menu_enter={"أهلًا يا {name}، القائمة جاهزة.","يا {name}، اختر العنصر الذي تريده بهدوء.","كل الخيارات أمامك يا {name}.","تم فتح القسم بنجاح يا {name}.","تحرك براحتك يا {name}، نحن معك.","يا {name}، القائمة مرتبة وجاهزة للاستخدام."},
  account_ready={"حسابك جاهز يا {name}، أهلاً بعودتك.","مرحبًا يا {name}، تم تجهيز ملفك.","بياناتك محفوظة يا {name}، نكمل السباق.","أهلًا يا {name}، ملفك جاهز الآن.","تم اعتماد بياناتك يا {name}، نورت اللعبة.","يا {name}، كل شيء جاهز للانطلاق."},
  social={"يا {name}، مجتمع اللاعبين جاهز.","شوف أصدقاءك يا {name}، يمكن حد مستنيك.","اللاعبون المتصلون موجودون يا {name}.","يا {name}، ابعت طلب صداقة ووسع فريقك.","الصداقة بداية سباق ممتع يا {name}.","تابع طلباتك يا {name}، ممكن يكون عندك دعوة جديدة."},
  chat={"وصلت لك مساحة الدردشة يا {name}.","يا {name}، اكتب رسالتك وخليك محترم.","الدردشة جاهزة، صوتك مهم يا {name}.","يا {name}، تقدر تبعت رسالة للفريق الآن.","رسالة بسيطة ممكن تبدأ سباق قوي يا {name}.","خلّي تواصلك واضحًا يا {name}."},
  store={"يا {name}، اختار معداتك بعناية.","المتجر جاهز يا {name}، راجع رصيدك أولًا.","سيارتك تستحق الأفضل يا {name}.","يا {name}، كل اختيار في المتجر له تأثير.","جهز عربيتك قبل السباق يا {name}.","تسوق بهدوء يا {name}، وخلي رصيدك محسوب."},
  achievement={"أحسنت يا {name}، تقدمك واضح.","يا {name}، مهمة جديدة في انتظارك.","كل إنجاز يقربك من القمة يا {name}.","يا {name}، راجع مهماتك وابدأ التحدي.","مستواك بيتحسن يا {name}، كمل.","الإنجازات تسجل مجهودك يا {name}."}
}

local extraCommentary={
  race_start={"يا {name}، البداية هادئة ثم السرعة تصنع الفارق.","انطلق يا {name}، وخلي عينك على المسار.","هيا يا {name}، أول منعطف هو بداية الحكاية."},
  lead={"يا {name}، الصدارة ثابتة والقرار في إيدك.","تقدم جميل يا {name}، حافظ على الهدوء.","يا {name}، كل متر الآن يحسب لك."},
  push={"يا {name}، ارفع الإيقاع بحساب.","اضغط بثبات يا {name}، ولا تترك المسار.","يا {name}، المنافس قريب فخليك حاضر."},
  behind={"يا {name}، الفارق يتعوض بتركيز واحد.","هدئ القيادة ثم ارجع بقوة يا {name}.","يا {name}، لا تزال الفرصة كاملة أمامك."},
  overtake={"تجاوز محسوب يا {name}، لقطة ممتازة.","يا {name}، دخلت المساحة في الوقت الصحيح.","مناورة قوية يا {name}، استمر."},
  near_finish={"يا {name}، لا تبالغ الآن، ثبت الخط للنهاية.","آخر لحظات يا {name}، خليك متماسك.","يا {name}، خط النهاية صار قريبًا جدًا."},
  time={"يا {name}، الوقت المتبقي يحتاج قرارًا سريعًا.","كل ثانية يا {name} تقربك من النتيجة.","يا {name}، حافظ على الإيقاع حتى النهاية."},
  race_event={"يا {name}، استمع للطريق وخليك جاهز.","تركيزك يا {name} أهم من السرعة الآن.","يا {name}، تحكم هادئ وقيادة نظيفة."},
  result={"يا {name}، خذ النتيجة كخطوة للسباق القادم.","أداء محترم يا {name}، والسباق التالي ينتظرك.","يا {name}، سجلت جولة قوية بكل المقاييس."},
  menu_enter={"يا {name}، تحرك بين الأيقونات براحتك.","القائمة أمامك يا {name}، اختر ما يناسبك.","يا {name}، كل عنصر جاهز للتفعيل بالضغطتين."},
  store={"يا {name}، راجع السعر ثم أكمل اختيارك.","اختيار ذكي يا {name}، تجهيز السيارة يبدأ من هنا.","يا {name}، الرصيد والمعدات يحتاجان توازنًا."},
  achievement={"يا {name}، إنجاز جديد يعني خطوة أقرب للصدارة.","واصل يا {name}، مهماتك تحسب تقدمك.","يا {name}، اجمع إنجازاتك وطور قيادتك."}
}
for kind,lines in pairs(extraCommentary) do
  local target=SmartAnnouncer.commentarySets[kind]
  if target then for _,line in ipairs(lines) do table.insert(target,line) end end
end

SmartAnnouncer.personas={
  commentator={name="المعلّق",pitch=1.08,speed=1.0},
  challenger={name="المنافس المرح",pitch=0.86,speed=1.10},
  friend={name="الصديق المشجع",pitch=1.24,speed=0.92}
}
SmartAnnouncer.banterSets={
  start={
    {who="commentator",text="يا جماعة، كل واحد ماسك دركسيونه، واللي يضحك آخرًا هو اللي يكسب."},
    {who="challenger",text="أنا سايب لكم أول ثانيتين بس، ما حدش يقول إني استعجلت."},
    {who="friend",text="ركزوا يا أبطال، الطريق واسع والضحك بعد خط النهاية."}
  },
  overtake={
    {who="commentator",text="يا {player}، تجاوز نظيف! المنافس بيبص في المراية وبيسأل: مين عدّى؟"},
    {who="challenger",text="استنى يا {player}، أنا كنت بديك فرصة تصورني من الخلف بس."},
    {who="friend",text="ردّ عليه في الطريق يا {player}، وخلي المزاح خفيف والسرعة عالية."}
  },
  lead={
    {who="commentator",text="{player} في المقدمة، والمنافس بدأ يحسب المسافة بدل ما يحسب الفوز."},
    {who="challenger",text="ما تفرحش بدري يا {player}، أنا لسه بدوّر على زر السرعة."},
    {who="friend",text="خليك ثابت يا {player}، الصدارة حلوة لما نحافظ عليها."}
  },
  behind={
    {who="commentator",text="{player} متأخر قليلًا، لكن السباق لسه فيه مفاجآت."},
    {who="challenger",text="تعالى يا {player}، الطريق مش محجوز باسمي، بس أنا مستمتع بالمقدمة."},
    {who="friend",text="ولا يهمك يا {player}، خذ نفسًا وابدأ المطاردة بهدوء."}
  },
  collision={
    {who="commentator",text="خبطة صغيرة يا {player}، العربية زعلت لكن لسه عندها سبع فرص."},
    {who="challenger",text="الحائط سلّم عليك يا {player}، وقال لك جرّب المسار الثاني."},
    {who="friend",text="ولا يهمك، عدّل المسار وكمل، إحنا جايين ننبسط مش نتخانق مع الحواجز."}
  },
  loss={
    {who="commentator",text="الجولة للمنافس هذه المرة، لكن الضحكة على الخسارة أخف من الخسارة نفسها."},
    {who="challenger",text="فوز لطيف، لا تقلقوا، لن أحتفل أكثر من اللازم."},
    {who="friend",text="جولة وانتهت يا بطل، نصلح العربية ونرجع أقوى."}
  },
  win={
    {who="commentator",text="فوز مستحق! {player} خلّى المنافس يراجع خطة الهروب."},
    {who="challenger",text="أعترف بالفوز يا {player}، لكن الجولة القادمة فيها رد محترم."},
    {who="friend",text="أحسنت يا {player}، الفوز جميل والأجمل إننا كنا بنضحك طول الطريق."}
  },
  live={
    {who="commentator",text="المحرك يغني يا {player}، والطريق يسمع جيدًا."},
    {who="challenger",text="يا {player}، لا تخليني أقول إن البنزين عندك في إجازة."},
    {who="friend",text="تقدم جميل، خليك منتبه للصندوق والحاجز معًا."}
  }
}
SmartAnnouncer.lastBanterAt=0
SmartAnnouncer.banterIndex=0
SmartAnnouncer.activeRaceSession=0
local function raceSessionActive(session)
  return (currentSection=="PLAYING" or currentSection=="PLAYING_MULTIPLAYER") and tonumber(GameState.raceSessionId or 0)==tonumber(session or GameState.raceSessionId or 0)
end
function SmartAnnouncer.cancelRaceVoices()
  SmartAnnouncer.activeRaceSession=-1; SmartAnnouncer.lastAt=os.time(); SmartAnnouncer.lastBanterAt=os.time(); SmartAnnouncer.lastGeneratedClipAt=os.time()
end
SmartAnnouncer.generatedVoiceClips={start="announcer_female_intro_reverb",overtake="race_chat_female_a",lead="race_chat_male_a",behind="race_chat_female_b",collision="race_chat_male_b",loss="race_chat_male_a",win="race_chat_female_a",live="race_chat_male_b"}
SmartAnnouncer.lastGeneratedClipAt=0
local function speakPersona(persona,text,interrupt)
  if not tts then return false end
  local p=SmartAnnouncer.personas[persona] or SmartAnnouncer.personas.commentator
  local ok=pcall(function()
    local queue=interrupt and TextToSpeech.QUEUE_FLUSH or TextToSpeech.QUEUE_ADD
    local baseSpeed=math.max(0.05,tonumber(Settings.voiceSpeed) or 1.0); local basePitch=math.max(0.05,tonumber(Settings.voicePitch) or 1.0)
    tts.setSpeechRate(math.max(0.05,baseSpeed*(tonumber(p.speed) or 1.0))); tts.setPitch(math.max(0.05,basePitch*(tonumber(p.pitch) or 1.0))); tts.speak(tostring(text),queue,nil)
  end)
  if ok then SmartAnnouncer.lastAt=os.time(); return true end
  return false
end
function SmartAnnouncer.banter(kind,username,extra,force)
  if not Settings.announcerEnabled or Settings.smartAnnouncerEnabled==false or Settings.sfxEnabled==false then return false end
  local now=os.time(); local cooldown=(kind=="start" or kind=="win" or kind=="loss") and 1 or 8
  if not force and now-(SmartAnnouncer.lastBanterAt or 0)<cooldown then return false end
  local clip=SmartAnnouncer.generatedVoiceClips[kind]
  if clip and playRaceVoiceClip and (force or now-(SmartAnnouncer.lastGeneratedClipAt or 0)>=cooldown) then
    local played=playRaceVoiceClip(clip,Settings.announcerVolume or 0.82)
    if played and played~=0 then SmartAnnouncer.lastBanterAt=now; SmartAnnouncer.lastGeneratedClipAt=now; return true end
  end
  local set=SmartAnnouncer.banterSets[kind] or SmartAnnouncer.banterSets.live; if not set or #set==0 or not tts then return false end
  SmartAnnouncer.banterIndex=(SmartAnnouncer.banterIndex or 0)+1; local start=((SmartAnnouncer.banterIndex-1)%#set)+1; local player=displayName(username or playerStats.username); local first=set[start]; local second=set[(start%#set)+1]; local third=set[((start+1)%#set)+1]
  local function fill(text) return tostring(text or ""):gsub("{player}",player):gsub("{extra}",tostring(extra or "")) end
  local session=GameState.raceSessionId; SmartAnnouncer.activeRaceSession=session; if not raceSessionActive(session) then return false end
  SmartAnnouncer.lastBanterAt=now; speakPersona(first.who,fill(first.text),false)
  -- تعليق واحد فقط لكل حدث؛ يمنع تتابع ثلاثة أصوات فوق الناطق الداخلي.
  return true
end

function SmartAnnouncer.commentary(kind,username,extra,force)
  if not Settings.announcerEnabled or Settings.smartAnnouncerEnabled==false or Settings.ttsEnabled==false then return false end
  local set=SmartAnnouncer.commentarySets[kind] or SmartAnnouncer.commentarySets.race_event; if #set==0 then return false end
  local cooldown=(kind=="race_start" or kind=="result") and 1 or 6
  if not canSpeak("commentary_"..tostring(kind),cooldown,force==true) then return false end
  SmartAnnouncer.templateSeed=(tonumber(SmartAnnouncer.templateSeed) or 0)+1; local index=((math.abs(os.time())+SmartAnnouncer.templateSeed) % #set)+1; local text=set[index]:gsub("{name}",displayName(username or playerStats.username)):gsub("{extra}",tostring(extra or ""))
  if extra and extra~="" and not text:find(tostring(extra),1,true) then text=text.." "..tostring(extra) end
  return speak(text,force==true)
end

function SmartAnnouncer.startRace()
  SmartAnnouncer.lastAt=0; SmartAnnouncer.lastKind=""; SmartAnnouncer.lastLeader=""; SmartAnnouncer.lastTimeMark=0; SmartAnnouncer.lastProgressMark=0; SmartAnnouncer.lastCommentaryAt=0; SmartAnnouncer.lastBanterAt=0; SmartAnnouncer.lastGeneratedClipAt=0; SmartAnnouncer.banterIndex=0; local session=GameState.raceSessionId; SmartAnnouncer.activeRaceSession=session
  Handler().postDelayed(Runnable({run=function() if SmartAnnouncer.activeRaceSession==session and raceSessionActive(session) then SmartAnnouncer.commentary("race_start",playerStats.username,"",true); SmartAnnouncer.banter("start",playerStats.username,"",true) end end}),900)
end

function SmartAnnouncer.update(state)
  if not state or not Settings.announcerEnabled or Settings.smartAnnouncerEnabled==false or Settings.ttsEnabled==false then return end
  local distance=tonumber(state.distance) or 0; local goal=math.max(1,tonumber(state.goal) or 300); local remaining=math.max(0,tonumber(state.timeRemaining) or 0); local name=displayName(playerStats.username)
  local leaderUser,leaderDistance=bestOpponent(distance); local diff=distance-leaderDistance
  if leaderUser=="" and diff>=15 and SmartAnnouncer.lastLeader~="self" then
    SmartAnnouncer.lastLeader="self"
    SmartAnnouncer.commentary("lead",playerStats.username,"الفارق "..math.floor(diff).." متر",false)
  elseif leaderUser~="" and diff<=-15 and SmartAnnouncer.lastLeader~=leaderUser then
    SmartAnnouncer.lastLeader=leaderUser
    SmartAnnouncer.commentary("behind",playerStats.username,"المتقدم الآن هو "..displayName(leaderUser).." والفارق "..math.floor(math.abs(diff)).." متر",false)
  elseif leaderUser=="" and diff>=15 then
    SmartAnnouncer.commentary("lead",playerStats.username,"حافظ على الصدارة",false)
  elseif leaderUser~="" and diff<=-15 then
    SmartAnnouncer.commentary("behind",playerStats.username,"الحاق بـ"..displayName(leaderUser),false)
  end
  local timeMark=math.floor(remaining/30)*30
  if timeMark>0 and timeMark~=SmartAnnouncer.lastTimeMark and remaining<=60 then
    SmartAnnouncer.lastTimeMark=timeMark
    SmartAnnouncer.commentary("time",playerStats.username,"متبقي "..timeMark.." ثانية",false)
  end
  local progressMark=math.floor((distance/goal)*4)
  if progressMark>=1 and SmartAnnouncer.lastProgressMark<1 then
    SmartAnnouncer.lastProgressMark=1; playDynamicSFX("race_checkpoint",0,1,0,0.92); SmartAnnouncer.commentary("race_event",playerStats.username,"نقطة تقدم أولى",false)
  elseif progressMark>=3 and SmartAnnouncer.lastProgressMark<3 then
    SmartAnnouncer.lastProgressMark=3
    SmartAnnouncer.commentary("near_finish",playerStats.username,"",false)
  elseif progressMark>=2 and SmartAnnouncer.lastProgressMark<2 then
    SmartAnnouncer.lastProgressMark=2
    SmartAnnouncer.commentary("push",playerStats.username,"تجاوزت نصف المسافة",false)
  end
end

function SmartAnnouncer.finish(won,score)
  SmartAnnouncer.commentary("result",playerStats.username,won and ("النتيجة "..math.floor(tonumber(score) or 0).." نقطة") or "",true)
  SmartAnnouncer.banter(won and "win" or "loss",playerStats.username,"",true)
end

function SmartAnnouncer.speakNamed(kind,username,extra)
  if not Settings.announcerEnabled or Settings.smartAnnouncerEnabled==false then return end
  local name=displayName(username); local text
  if kind=="lead" then text="الله عليك يا "..name.."، أنت في المقدمة" elseif kind=="behind" then text="شد حيلك يا "..name.."، أنت متأخر قليلًا" elseif kind=="near_finish" then text="اقتربت من النهاية يا "..name else text="أحسنت يا "..name end
  if extra and extra~="" then text=text.."، "..extra end
  speak(text,false)
end
