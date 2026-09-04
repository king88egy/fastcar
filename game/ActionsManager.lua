local function sel() return currentMenuOptions and currentMenuOptions[currentMenuIndex] or "" end
local function selectedUsername(label) return tostring(label or ""):match("@([A-Za-z0-9_]+)") or tostring(label or "") end
local function claimRewardSafely(claimKey,reward,reason,onApplied)
  local apply=function(ok,msg,already)
    if not ok then if onApplied then onApplied(false,"تعذر حفظ المكافأة على الخادم.",false) end; return end
    if already then if onApplied then onApplied(false,"هذه المكافأة مسجلة بالفعل.",true) end; return end
    local amount=tonumber(reward and reward.amount or 0) or 0
    if reward and reward.field then playerStats[reward.field]=(playerStats[reward.field] or 0)+amount elseif amount>0 then playerStats.money=(playerStats.money or 0)+amount; if firebaseRecordEconomyLedger then firebaseRecordEconomyLedger(reason or "reward",amount,tostring(reward.label or "مكافأة"),claimKey,function() end) end end
    saveGameData(); if firebaseSyncProfile then firebaseSyncProfile(function() end) end; if onApplied then onApplied(true,msg or "تم تسجيل المكافأة.",false) end
  end
  if firebaseClaimIdempotentReward then firebaseClaimIdempotentReward(claimKey,reward,reason or "reward",apply) else apply(true,"تم تسجيل المكافأة محليًا.",false) end
end
local function ensureVerifiedAccount(next)
  if not firebaseVerifyCurrentAccount then next(true); return end
  firebaseVerifyCurrentAccount(function(ok,msg)
    if not ok then playDynamicSFX("network_error",0,1,0,0.78); say(msg or "يجب التحقق من الحساب على الخادم أولًا.",true); next(false); return end
    next(true)
  end)
end
function confirmExitGame()
  playDynamicSFX("exit_confirm",0,1,0,0.72)
  showInternalConfirm("تأكيد الخروج","هل تريد الخروج من اللعبة؟ سيتم حفظ تقدمك تلقائيًا.",function() stopSoundsAndExit() end,function() say("تم إلغاء الخروج.",true) end)
end
function confirmLeaveRoom()
  playDynamicSFX("exit_confirm",0,1,0,0.72)
  showInternalConfirm("تأكيد مغادرة الغرفة","هل تريد مغادرة الغرفة؟ سيتم حذف تسجيلاتك الصوتية من الغرفة، وستبقى بيانات حسابك محفوظة.",function()
    firebaseLeaveCurrentRoom(function(ok,empty)
      playDynamicSFX(ok and "room_player_leave" or "network_error",0,1,0,0.72); say(ok and (empty and "غادرت الغرفة وتم تنظيف تسجيلاتها الصوتية." or "غادرت الغرفة وتم حذف تسجيلاتك الصوتية.") or "تعذر مغادرة الغرفة.",true); if ok then loadSection("RACE_MODE_SELECT") end
    end)
  end,function() say("تم إلغاء المغادرة.",true) end)
end
local function acceptInviteFromNotification(item)
  local raw=item and item.raw; local data=raw and raw.optJSONObject("data"); local room=data and tostring(data.optString("room","")) or ""
  firebaseGetGameInvites(function(ok,list)
    local chosen=nil; for _,invite in ipairs(list or {}) do if room=="" or tostring(invite.room or "")==room then chosen=invite; break end end
    if not chosen then say("الدعوة لم تعد متاحة. افتح قسم الدعوات للتحديث.",true); loadSection("GAME_INVITES"); return end
    say("جاري فتح الغرفة من الدعوة.",true); firebaseAcceptGameInvite(chosen,function(joined,msg) if joined then playDynamicSFX("room_player_join",0,1,0,0.74); say("تم فتح الغرفة. أنت الآن في غرفة الانتظار.",true); loadSection("WAITING_ROOM") else say(msg or "تعذر فتح الغرفة.",true); loadSection("GAME_INVITES") end end)
  end)
end
local roomCreateInFlight=false
local function createRoomNow(mode, inviteTarget, requestedName)
  if roomCreateInFlight then say("جاري إنشاء الغرفة، انتظر لحظة.",true); return end
  local roomName=tostring(requestedName or ""):gsub("^%s+",""):gsub("%s+$","")
  if roomName=="" then roomName=tostring(playerStats.name or ""):gsub("^%s+",""):gsub("%s+$","") end
  if roomName=="" then roomName=tostring(playerStats.username or ""):gsub("^%s+",""):gsub("%s+$","") end
  if roomName=="" then say("تعذر تحديد اسم الغرفة. أكمل بيانات الحساب أولًا.",true); return end
  local duration=math.max(60,math.min(720,math.floor(tonumber(Multiplayer.selectedDuration or Settings.raceDurationSeconds or 300) or 300)))
  roomCreateInFlight=true; setVerificationProgress(0,"جاري إنشاء الغرفة باسم اللاعب... 0%"); say("جاري التحقق من الحساب ثم إنشاء الغرفة باسم "..roomName.." لمدة "..tostring(math.floor(duration/60)).." دقيقة.",true)
  ensureVerifiedAccount(function(verified)
    if not verified then roomCreateInFlight=false; setVerificationProgress(0,"تعذر التحقق من الحساب."); return end
    setVerificationProgress(45,"تم التحقق. جاري حفظ الغرفة على الخادم...")
    Multiplayer.currentRoom=roomName; Multiplayer.roomMode=mode; Multiplayer.roomOwner=playerStats.username; Multiplayer.roomStatus="waiting"; Multiplayer.selectedDuration=duration; Settings.raceDurationSeconds=duration; saveGameData()
    local stamp=(firebaseServerNowSec and firebaseServerNowSec() or os.time())
    local payload=firebaseJsonObject({name=roomName,owner=playerStats.username,displayName=roomName,mode=mode,status="waiting",maxPlayers=4,duration=duration,createdAt=stamp,race={status="waiting",startAt=0,goal=0,duration=duration},players={}})
    firebasePut("/rooms/"..firebaseEncodeKey(roomName),payload,function(ok,body)
      if not ok then roomCreateInFlight=false; setVerificationProgress(0,"تعذر حفظ الغرفة على الخادم."); say("تعذر إنشاء الغرفة عبر الإنترنت. تحقق من الاتصال ثم حاول مرة أخرى.",true); return end
      setVerificationProgress(70,"تم إنشاء الغرفة. جاري تسجيل دخولك إليها..."); say("تم إنشاء الغرفة باسم اللاعب. جاري إدخالك إليها.",true)
      firebasePut("/rooms/"..firebaseEncodeKey(roomName).."/players/"..firebaseEncodeKey(playerStats.username),firebaseJsonObject({username=playerStats.username,name=playerStats.name,joinedAt=(firebaseServerNowSec and firebaseServerNowSec() or os.time())}),function(joined)
        if not joined then roomCreateInFlight=false; setVerificationProgress(0,"تعذر تسجيل دخولك إلى الغرفة."); say("تم إنشاء الغرفة لكن تعذر تسجيل دخولك إليها. اضغط إنشاء الغرفة مرة أخرى لمحاولة الانضمام.",true); return end
        if firebaseRefreshRoom then firebaseRefreshRoom(function() end) end
        local function finishRoom()
          roomCreateInFlight=false; setVerificationProgress(100,"اكتمل إنشاء الغرفة."); if firebasePublishRoomActivity then firebasePublishRoomActivity("room_join",{room=roomName},function() end) end; playDynamicSFX("room_player_join",0,1,0,0.74); say(inviteTarget and "تم إنشاء الغرفة الخاصة وإرسال الدعوة." or "تم إنشاء الغرفة باسم اللاعب. أنت المالك.",true); loadSection("WAITING_ROOM")
        end
        if inviteTarget and inviteTarget~="" then firebaseSendRoomInvite(inviteTarget,function() finishRoom() end) else finishRoom() end
      end)
    end)
  end)
end
local function openRoomDialog(mode, inviteTarget, requestedName)
  createRoomNow(mode,inviteTarget,requestedName)
end
local function joinRoomByName(mode,roomName)
  if not roomName or roomName=="" then say("لم يتم تحديد الغرفة.",true); return end
  ensureVerifiedAccount(function(verified) if not verified then return end
  firebaseGet("/rooms/" .. firebaseEncodeKey(roomName), function(ok, content)
    if not ok or not content or content == "null" then say("الغرفة غير موجودة أو لا تسمح بالقراءة.", true); return end
    local escapedUser=firebaseEncodeKey(playerStats.username):gsub("([^%w])","%%%1")
    if content:match('"banned"%s*:%s*{[^}]*"'..escapedUser..'"') then say("لا يمكنك الانضمام إلى هذه الغرفة لأن المالك حظر حسابك.",true); return end
    if content:match('"private"%s*:%s*true') or content:match('"mode"%s*:%s*"private_duel"') then say("هذه مباراة خاصة بين لاعبين محددين ولا تقبل انضمامًا عامًا.",true); return end
    if content:match('"locked"%s*:%s*true') then say("الغرفة مقفلة من مالكها ولا تستقبل لاعبين جددًا.",true); return end
    local count=0; for _ in string.gmatch(content,'"username"%s*:') do count=count+1 end; local maxPlayers=tonumber(content:match('"maxPlayers"%s*:%s*(%d+)')) or 4; maxPlayers=math.max(2,math.min(4,maxPlayers)); if count>=maxPlayers then say("الغرفة ممتلئة. الحد الأقصى "..tostring(maxPlayers).." لاعبين.",true); return end
    Multiplayer.currentRoom=roomName; Multiplayer.roomMode=mode; Multiplayer.roomOwner=content:match('"owner"%s*:%s*"(.-)"') or ""; Multiplayer.roomStatus="waiting"
    firebasePut("/rooms/" .. firebaseEncodeKey(roomName) .. "/players/" .. firebaseEncodeKey(playerStats.username), firebaseJsonObject({username=playerStats.username,name=playerStats.name,joinedAt=os.time()}), function(joined)
      if joined then firebaseRefreshRoom(function() if firebasePublishRoomActivity then firebasePublishRoomActivity("room_join",{room=roomName},function() end) end; playDynamicSFX("room_player_join",0,1,0,0.74); say("تم الانضمام إلى غرفة "..tostring(Multiplayer.roomOwner)..".", true); loadSection("WAITING_ROOM") end) else say("تعذر تسجيل انضمامك.", true) end
    end)
  end)
  end)
end
local function joinRoomDialog(mode,initialRoom)
  if initialRoom and initialRoom~="" then joinRoomByName(mode,initialRoom); return end
  showSingleInput("الانضمام إلى غرفة " .. mode, "اسم الغرفة", Multiplayer.currentRoom, false, function(roomName) joinRoomByName(mode,roomName) end)
end
local function sendFriendRequest(target)
  target=selectedUsername(target)
  if target=="" or target==playerStats.username then say("اسم اللاعب غير صالح.", true); return end
  firebaseSendFriendRequest(target, function(ok) if ok then playDynamicSFX("friend_request",0,1,0,0.72); saveGameData(); say("تم إرسال طلب الصداقة.", true) else playDynamicSFX("network_error",0,1,0,0.72); say("تعذر إرسال الطلب.", true) end end)
end
local function chooseRoomPlayer(onDone)
  if Multiplayer.selectedPlayer and Multiplayer.selectedPlayer ~= "" then onDone(Multiplayer.selectedPlayer); return end
  loadSection("ROOM_PLAYERS")
end
local raceDurationOptions={60,120,180,240,300,360,420,480,540,600,660,720}
local raceDurationLabels={"دقيقة","دقيقتان","ثلاث دقائق","أربع دقائق","خمس دقائق","ست دقائق","سبع دقائق","ثماني دقائق","تسع دقائق","عشر دقائق","إحدى عشرة دقيقة","اثنتا عشرة دقيقة"}
local pendingRaceDurationCallback=nil
local function applyRaceDuration(seconds,onDone)
  seconds=math.max(60,math.min(720,math.floor(tonumber(seconds) or 300))); Multiplayer.selectedDuration=seconds; Settings.raceDurationSeconds=seconds; saveGameData(); playDynamicSFX("settings_change",0,1,0,0.45); say("مدة السباق: "..(seconds%60==0 and tostring(seconds/60).." دقيقة" or tostring(seconds).." ثانية"),true); if onDone then onDone(seconds) end
end
local function finishRaceDurationChoice(seconds)
  local callback=pendingRaceDurationCallback; pendingRaceDurationCallback=nil; applyRaceDuration(seconds)
  if callback then callback(seconds) else loadSection("RACE_MODE_SELECT") end
end
local function showCustomRaceDuration(onDone)
  pendingRaceDurationCallback=onDone
  loadSection("CUSTOM_RACE_DURATION")
end
local function chooseRaceDuration(onDone)
  pendingRaceDurationCallback=onDone; loadSection("RACE_DURATION")
end
local function confirmPurchase(label,price,onYes)
  playDynamicSFX("purchase_confirm",0,1,0,0.82); say("تأكيد الشراء: "..tostring(label).." مقابل "..tostring(price).." عملة.",true)
  showInternalConfirm("تأكيد الشراء","هل تريد شراء "..tostring(label).." مقابل "..tostring(price).." عملة؟",function() playDynamicSFX("purchase_success",0,1,0,0.90); if onYes then onYes() end end,function() playDynamicSFX("menu_back",0,1,0,0.45); say("تم إلغاء الشراء.",true) end)
end
function handleMenuSelection()
  local choice=sel(); if choice=="" then return end; playDynamicSFX("menu_select",0,1.0,0.0)
  if currentSection=="MENU" then
    if choice=="بدء السباق" then loadSection("RACE_MODE_SELECT") elseif choice=="الملف الشخصي" then loadSection("PROFILE") elseif choice=="قائمة المتصدرين" then loadSection("LEADERBOARD") elseif choice=="البطولات" then loadSection("TOURNAMENTS") elseif choice=="المهام والإنجازات" then loadSection("ACHIEVEMENTS") elseif choice=="المكافأة اليومية" then loadSection("DAILY_REWARD") elseif choice=="قائمة الأصدقاء والدعوات والدردشة" or choice=="مجتمع المتسابقين" then loadSection("FRIENDS_MENU") elseif choice=="الحقيبة" then loadSection("INVENTORY") elseif choice=="المتجر" then loadSection("STORE") elseif choice=="محطة البنزين" then loadSection("GAS_STATION") elseif choice=="ورشة الصيانة" then loadSection("GARAGE") elseif choice=="الإعدادات" then loadSection("SETTINGS") elseif choice=="حول المطورين" then loadSection("ABOUT") elseif choice=="خروج" then confirmExitGame() end
  elseif currentSection=="PROFILE" then
    if choice=="فتح ملفي التفاعلي" then showProfileEditor() elseif choice=="عرض بياناتي" then say("الاسم: "..playerStats.name..". اسم المستخدم: "..playerStats.username..". الحالة: "..(playerStats.status or "")..". الوصف: "..(playerStats.bio or "")..". الانتصارات: "..playerStats.wins, true)
    elseif choice=="قسم الدعوات" then loadSection("GAME_INVITES") elseif choice=="تغيير كلمة المرور" then showChangePasswordDialog() elseif choice=="تغيير وضع العرض" then showAccessibilityModeDialog() elseif choice=="تسجيل الخروج" then firebaseSignOut(); currentSection="LOGIN"; say("تم تسجيل الخروج.", true); showAuthEntry() elseif choice=="رجوع للقائمة" then loadSection("MENU") end
  elseif currentSection=="GAME_INVITES" then
    if choice=="تحديث الدعوات" then loadSection("GAME_INVITES") elseif choice=="رجوع للملف الشخصي" then loadSection("PROFILE") elseif choice:match("^دعوة من") then
      local invite=Multiplayer.gameInvites and Multiplayer.gameInvites[currentMenuIndex]
      if not invite then say("الدعوة غير متاحة الآن.",true); return end
      local inviter=tostring(invite.fromName or invite.from); local destination=invite.private and "مباراة خاصة" or "غرفة جماعية"; say("تم اختيار الدعوة من "..inviter..". جاري فتح "..destination..".",true)
      firebaseAcceptGameInvite(invite,function(ok,msg) if ok then Multiplayer.waitingForPlayers=false; playDynamicSFX("room_player_join",0,1,0,0.74); say("تم فتح الغرفة. أنت الآن في غرفة الانتظار.",true); loadSection("WAITING_ROOM") else playDynamicSFX("network_error",0,1,0,0.72); say(msg or "تعذر فتح الدعوة.",true); loadSection("GAME_INVITES") end end)
    end
  elseif currentSection=="RACE_MODE_SELECT" then
    if choice=="اللعب مع الكمبيوتر" then chooseRaceDuration(function(seconds) Settings.raceDurationSeconds=seconds; saveGameData(); loadSection("PLAYING") end) elseif choice=="إنشاء غرفة عبر الإنترنت" then loadSection("CREATE_ROOM") elseif choice=="الانضمام إلى غرفة عبر الإنترنت" then loadSection("JOIN_ROOM") elseif choice=="اللعب مع صديق" then loadSection("FRIENDS_LIST_PLAY") elseif choice=="اللعب عبر Hotspot" then loadSection("HOTSPOT_MODE") elseif choice=="مدة السباق" then loadSection("RACE_DURATION") elseif choice=="رجوع للقائمة" then loadSection("MENU") end
  elseif currentSection=="RACE_DURATION" then
    local fixed={ ["دقيقة"]=60, ["دقيقتان"]=120, ["ثلاث دقائق"]=180, ["أربع دقائق"]=240, ["خمس دقائق"]=300, ["ست دقائق"]=360, ["سبع دقائق"]=420, ["ثماني دقائق"]=480, ["تسع دقائق"]=540, ["عشر دقائق"]=600, ["إحدى عشرة دقيقة"]=660, ["اثنتا عشرة دقيقة"]=720 }
    if fixed[choice] then finishRaceDurationChoice(fixed[choice]) elseif choice=="مدة مخصصة" then loadSection("CUSTOM_RACE_DURATION") elseif choice=="رجوع" then pendingRaceDurationCallback=nil; loadSection("RACE_MODE_SELECT") end
  elseif currentSection=="CUSTOM_RACE_DURATION" then
    if choice:match("^الوحدة") then readCustomDurationInput(); customDurationUnit=(customDurationUnit=="ثوانٍ") and "دقائق" or "ثوانٍ"; currentMenuOptions[1]="الوحدة: "..(customDurationUnit=="ثوانٍ" and "ثواني" or "دقائق"); playDynamicSFX("settings_change",0,1,0,0.45); say("الوحدة: "..(customDurationUnit=="ثوانٍ" and "ثواني" or "دقائق"),true); refreshVisibleScreen()
    elseif choice=="تطبيق المدة" then
      local number=tonumber(readCustomDurationInput()); local secondsValue=(customDurationUnit=="ثوانٍ") and number or (number and number*60 or nil)
      if not secondsValue then say("اكتب رقمًا في حقل المدة أولًا.",true) elseif secondsValue<60 or secondsValue>720 then say("أدخل مدة من 60 إلى 720 ثانية.",true) else finishRaceDurationChoice(secondsValue) end
    elseif choice=="رجوع" then pendingRaceDurationCallback=nil; loadSection("RACE_DURATION") end
  elseif currentSection=="HOTSPOT_MODE" then
    if choice=="استضافة سباق Hotspot" then say("فعّل نقطة الاتصال من إعدادات الهاتف ثم أدخل اسم الغرفة.",true); openRoomDialog("Hotspot مستضافة") elseif choice=="الانضمام إلى سباق Hotspot" then say("اتصل بشبكة Hotspot أولًا ثم أدخل اسم الغرفة.",true); joinRoomDialog("Hotspot منضمة") elseif choice=="رجوع" then loadSection("RACE_MODE_SELECT") end
  elseif currentSection=="CREATE_ROOM" then if choice=="غرفة عامة" or choice=="إنشاء غرفة عامة" then Multiplayer.botEnabled=false; Multiplayer.bots={}; chooseRaceDuration(function() createRoomNow("عامة") end) elseif choice=="غرفة خاصة" or choice=="إنشاء غرفة خاصة" then Multiplayer.botEnabled=false; Multiplayer.bots={}; chooseRaceDuration(function() createRoomNow("خاصة") end) elseif choice=="غرفة مع روبوت" or choice=="إنشاء غرفة مع روبوت" then Multiplayer.botEnabled=true; Multiplayer.botName="روبوت الطريق 1"; Multiplayer.botSkill=0.52+math.random()*0.22; Multiplayer.bots={{id="bot_1",name=Multiplayer.botName,skill=Multiplayer.botSkill}}; chooseRaceDuration(function() createRoomNow("مع روبوت") end) elseif choice=="رجوع" then loadSection("RACE_MODE_SELECT") end
  elseif currentSection=="JOIN_ROOM" then if choice=="إدخال اسم الغرفة" then joinRoomDialog("عبر الإنترنت") elseif choice=="تحديث قائمة الغرف" then loadSection("JOIN_ROOM") elseif choice=="رجوع" then loadSection("RACE_MODE_SELECT") elseif choice:match("^غرفة:") then Multiplayer.selectedPublicRoomData=Multiplayer.publicRoomDirectory and Multiplayer.publicRoomDirectory[choice] or {room=Multiplayer.roomDirectory and Multiplayer.roomDirectory[choice] or "",displayName=choice}; Multiplayer.selectedPublicRoom=Multiplayer.selectedPublicRoomData.room; loadSection("PUBLIC_ROOM_OPTIONS") end
  elseif currentSection=="PUBLIC_ROOM_OPTIONS" then
    local room=Multiplayer.selectedPublicRoomData or {}; local roomName=tostring(Multiplayer.selectedPublicRoom or room.room or "")
    if choice=="إظهار المتواجدين" then
      if roomName=="" then say("لم تُحدد غرفة.",true) else firebaseGet("/rooms/"..firebaseEncodeKey(roomName).."/players",function(ok,body) local names={}; if ok and body and body~="null" then for username in string.gmatch(body,'"username"%s*:%s*"(.-)"') do table.insert(names,username) end; if #names==0 then for username in string.gmatch(body,'"([^"\\]+)"%s*:%s*{') do if username~="players" then table.insert(names,username) end end end end; say(#names>0 and "المتواجدون في الغرفة: "..table.concat(names,"، ") or "لا يوجد متسابقون مسجلون في الغرفة الآن.",true) end) end
    elseif choice=="الانضمام إلى الغرفة" then joinRoomByName("عبر الإنترنت",roomName)
    elseif choice=="رجوع إلى قائمة الغرف" then loadSection("JOIN_ROOM") end
  elseif currentSection=="FRIENDS_MENU" then if choice=="قائمة الأصدقاء" then loadSection("FRIENDS_LIST_PLAY") elseif choice=="الطلبات المستلمة" then loadSection("FRIEND_REQUESTS") elseif choice=="اللاعبون المتصلون" then loadSection("ONLINE_PLAYERS") elseif choice=="الإشعارات" then loadSection("NOTIFICATIONS") elseif choice=="الرسائل الخاصة" then loadSection("PRIVATE_MESSAGES") elseif choice=="المحظورون" then loadSection("BLOCKED_USERS") elseif choice=="السجل الاجتماعي" then say("السجل الاجتماعي يتضمن طلبات الصداقة والإشعارات والرسائل الخاصة المحفوظة.",true) elseif choice=="رجوع للقائمة" then local back=Multiplayer.socialReturnSection or "MENU"; Multiplayer.socialReturnSection=nil; loadSection(back) end
  elseif currentSection=="NOTIFICATIONS" then
    if choice=="تحديث الإشعارات" then loadSection("NOTIFICATIONS") elseif choice=="رجوع" then loadSection("FRIENDS_MENU") elseif choice=="لا توجد إشعارات جديدة" then say("لا توجد إشعارات جديدة.",true) elseif choice:match("^إشعار:") then local item=Multiplayer.notifications and Multiplayer.notifications[currentMenuIndex]; if item then playDynamicSFX("message_receive",0,1,0,0.72); if firebaseMarkNotificationRead then firebaseMarkNotificationRead(item.id,function() end) end; if item.kind=="game_invite" or item.kind=="room_invite" then acceptInviteFromNotification(item) elseif item.kind=="private_message" or item.kind=="private_voice" then local raw=item.raw; local data=raw and raw.optJSONObject("data"); Multiplayer.pendingMessageId=data and tostring(data.optString("messageId","")) or ""; say(item.kind=="private_voice" and "تم فتح الرسالة الصوتية." or "تم فتح الرسالة الخاصة.",true); loadSection("PRIVATE_MESSAGES") else say(item.text or "إشعار اجتماعي.",true) end end end
  elseif currentSection=="PRIVATE_MESSAGES" then
    if choice=="إرسال رسالة خاصة" then showSingleInput("اسم المستخدم","اسم المستخدم","",false,function(targetText) local target=selectedUsername(targetText); if target=="" or target==playerStats.username then say("اسم المستخدم غير صالح.",true); return end; showSingleInput("رسالة خاصة","نص الرسالة","",false,function(message) if message~="" then firebaseSendPrivateMessage(target,message,function(ok) playDynamicSFX(ok and "chat_send_soft" or "network_error",0,1,0,0.96); say(ok and "تم إرسال الرسالة الخاصة." or "تعذر إرسال الرسالة الخاصة.",true); loadSection("PRIVATE_MESSAGES") end) end end) end) elseif choice=="تحديث الرسائل" then loadSection("PRIVATE_MESSAGES") elseif choice=="رجوع" then local back=Multiplayer.socialReturnSection or "FRIENDS_MENU"; Multiplayer.socialReturnSection=nil; loadSection(back) elseif choice=="لا توجد رسائل خاصة" then say("لا توجد رسائل خاصة.",true) elseif choice:match("^رسالة من ") then local item=Multiplayer.privateMessages and Multiplayer.privateMessages[currentMenuIndex]; if item then if item.voice then say("رسالة صوتية من "..tostring(item.fromName or item.from)..". جاري تشغيلها.",true); if playArchivedVoiceMessage then playArchivedVoiceMessage(item.voiceChannel,item.voiceKey,item.fromName or item.from) end else say("رسالة من "..tostring(item.fromName or item.from)..": "..tostring(item.text or ""),true) end; playDynamicSFX("chat_receive_soft",0,1,0,0.92); if firebaseMarkPrivateMessageRead then firebaseMarkPrivateMessageRead(item.id,function() end) end end end
  elseif currentSection=="BLOCKED_USERS" then
    if choice=="تحديث المحظورين" then loadSection("BLOCKED_USERS") elseif choice=="رجوع" then loadSection("FRIENDS_MENU") elseif choice=="لا يوجد لاعبون محظورون" then say("لا يوجد لاعبون محظورون.",true) elseif choice:match("^فك حظر:") then local target=choice:gsub("^فك حظر:%s*",""); if firebaseUnblockUser then firebaseUnblockUser(target,function(ok) say(ok and "تم فك حظر اللاعب." or "تعذر فك الحظر.",true); loadSection("BLOCKED_USERS") end) end end
  elseif currentSection=="FRIENDS_LIST_PLAY" then
    if choice=="تحديث الأصدقاء المتصلين" then loadSection("FRIENDS_LIST_PLAY") elseif choice=="رجوع" then local back=Multiplayer.socialReturnSection or "RACE_MODE_SELECT"; Multiplayer.socialReturnSection=nil; loadSection(back) elseif choice:match("^دعوة ") then
      local target=selectedUsername(choice); if target=="" then say("تعذر تحديد الصديق.",true); return end
      if Multiplayer.socialReturnSection=="WAITING_ROOM" then firebaseSendRoomInvite(target,function(ok) playDynamicSFX(ok and "invite_sent" or "network_error",0,1,0,0.76); say(ok and "تم إرسال دعوة الانضمام إلى الغرفة." or "تعذر إرسال الدعوة.",true); Multiplayer.socialReturnSection=nil; loadSection("WAITING_ROOM") end) else
        chooseRaceDuration(function(seconds)
          say("جاري تجهيز مباراة خاصة ثنائية مع "..target..".",true)
          firebaseCreatePrivateDuel(target,seconds,function(ok,msg)
            if ok then playDynamicSFX("invite_sent",0,1,0,0.76); say("تم إرسال دعوة اللعب. ستبدأ المباراة عند قبول صديقك.",true) else say(msg or "تعذر إرسال دعوة اللعب.",true) end
          end)
        end)
      end
    elseif choice=="لا يوجد أصدقاء متصلون الآن" then say("لا يوجد أصدقاء متصلون الآن.",true) end
  elseif currentSection=="FRIEND_REQUESTS" then
    if choice=="تحديث الطلبات" then firebaseGet("/friendRequests/"..firebaseEncodeKey(playerStats.username),function(ok,content) if ok then Multiplayer.friendRequests={}; for req in string.gmatch(content or "",'"([^"\\]+)"%s*:') do table.insert(Multiplayer.friendRequests,req) end; saveGameData(); loadSection("FRIEND_REQUESTS") else say("تعذر تحديث الطلبات.",true) end end)
    elseif choice=="رجوع" then loadSection("FRIENDS_MENU") elseif choice=="لا توجد طلبات صداقة" then say("لا توجد طلبات صداقة.",true) elseif choice:match("^قبول طلب:") then local target=choice:gsub("^قبول طلب:%s*",""); firebaseAcceptFriendRequest(target,function(ok) if ok then playDynamicSFX("friend_accept",0,1,0,0.72); say("تم قبول الطلب وتحديث قائمة الأصدقاء.",true); loadSection("FRIENDS_MENU") else playDynamicSFX("network_error",0,1,0,0.72); say("تعذر قبول الطلب حاليًا.",true) end end) elseif choice:match("^رفض طلب:") then local target=choice:gsub("^رفض طلب:%s*",""); if firebaseRejectFriendRequest then firebaseRejectFriendRequest(target,function(ok) playDynamicSFX(ok and "menu_select" or "network_error",0,1,0,0.72); say(ok and "تم رفض طلب الصداقة." or "تعذر رفض الطلب.",true); loadSection("FRIENDS_MENU") end) end end
  elseif currentSection=="ONLINE_PLAYERS" then
    if choice=="تحديث قائمة اللاعبين" then loadSection("ONLINE_PLAYERS") elseif choice=="رجوع" then loadSection("FRIENDS_MENU") else Multiplayer.selectedPlayer=selectedUsername(choice); loadSection("PLAYER_PROFILE") end
  elseif currentSection=="PLAYER_PROFILE" then
    local target=Multiplayer.selectedPlayer
    if choice=="مكالمة الأصدقاء" then Multiplayer.directCallTarget=target; firebaseSendVoiceSignal(target,true,function(ok) playDynamicSFX(ok and "friend_message" or "network_error",0,1,0,0.72); say(ok and "تم إرسال طلب مكالمة الأصدقاء." or "تعذر إرسال طلب المكالمة.",true) end)
    elseif choice=="إرسال رسالة خاصة" then showSingleInput("رسالة خاصة","اكتب الرسالة","",false,function(message) if message~="" then firebaseSendPrivateMessage(target,message,function(ok) playDynamicSFX(ok and "chat_send_soft" or "network_error",0,1,0,0.96); say(ok and "تم إرسال الرسالة الخاصة." or "تعذر إرسال الرسالة الخاصة.",true) end) end end)
    elseif choice=="دعوة إلى الغرفة" then if Multiplayer.currentRoom=="" then say("أنشئ غرفة أولاً.",true) else firebaseSendRoomInvite(target,function(ok) playDynamicSFX(ok and "invite_sent" or "network_error",0,1,0,0.72); say(ok and "تم إرسال دعوة إلى الغرفة." or "تعذر إرسال الدعوة.",true) end) end
    elseif choice=="إرسال طلب صداقة" then sendFriendRequest(target) elseif choice=="حظر اللاعب" then if firebaseBlockUser then firebaseBlockUser(target,function(ok,msg) say(ok and "تم حظر اللاعب." or (msg or "تعذر حظر اللاعب."),true); loadSection("ONLINE_PLAYERS") end) end elseif choice=="فك حظر اللاعب" then if firebaseUnblockUser then firebaseUnblockUser(target,function(ok) say(ok and "تم فك حظر اللاعب." or "تعذر فك الحظر.",true); loadSection("ONLINE_PLAYERS") end) end elseif choice=="رجوع" then loadSection("ONLINE_PLAYERS") end
  elseif currentSection=="LEADERBOARD" then
    if choice=="أضفني إلى قائمة المتصدرين" then firebaseRequestLeaderboardQualification(GameState.lastRaceOvertakes or 0,function(ok,message) playDynamicSFX(ok and "achievement_unlock" or "input_error",0,1,0,0.72); say(message,true); if ok then loadSection("LEADERBOARD") end end)
    elseif choice=="حالة تأهلي" then local count=GameState.lastRaceOvertakes or 0; say(playerStats.leaderboardQualified and "طلب تأهلك مُرسل للمراجعة. الجائزة النهائية المعتمدة هي 500 ألف عملة." or "تجاوزت "..tostring(count).." من أربعة متسابقين مطلوبين في آخر سباق جماعي.",true)
    elseif choice=="تحديث قائمة المتصدرين" then loadSection("LEADERBOARD") elseif choice=="رجوع" then loadSection("MENU") end
  elseif currentSection=="TOURNAMENTS" then
    if choice=="إنشاء بطولة" then
      showSingleInput("إنشاء بطولة","اسم البطولة","بطولة العربيات",false,function(name) chooseRaceDuration(function(seconds) firebaseCreateTournament(name,seconds,function(ok,msg) say(msg,true); if ok then loadSection("TOURNAMENTS") end end) end) end)
    elseif choice=="بدء البطولة الحالية" then firebaseStartTournament(Tournament.currentId,function(ok,msg) playDynamicSFX(ok and "race_start" or "network_error",0,1,0,0.72); say(msg,true); if ok then loadSection("TOURNAMENTS") end end)
    elseif choice=="مغادرة البطولة الحالية" then firebaseLeaveTournament(function(ok,msg) say(msg,true); if ok then loadSection("TOURNAMENTS") end end)
    elseif choice=="تحديث البطولات" then loadSection("TOURNAMENTS")
    elseif choice=="رجوع للقائمة" then loadSection("MENU")
    elseif choice:match("^بطولة:") then local row=Multiplayer.tournamentDirectory[choice]; if row then firebaseJoinTournament(row.id,function(ok,msg) say(msg,true); if ok then loadSection("TOURNAMENTS") end end) else say("البطولة غير متاحة الآن.",true) end end
  elseif currentSection=="ACHIEVEMENTS" then
    if choice=="رجوع للقائمة" then loadSection("MENU") elseif choice=="تحديث المهام" then if missionsInitialize then missionsInitialize() end; loadSection("ACHIEVEMENTS") elseif currentMissionOptionIds and currentMissionOptionIds[choice] then if missionsClaim then missionsClaim(currentMissionOptionIds[choice],function(ok,msg) playDynamicSFX(ok and "coin_reward" or "input_error",0,1,0,0.84); say(msg,true); loadSection("ACHIEVEMENTS") end) end else say(choice,true) end
  elseif currentSection=="WAITING_ROOM" then
    if choice=="قائمة إجراءات الغرفة" then loadSection("ROOM_ACTIONS") elseif choice=="الأصدقاء" then Multiplayer.socialReturnSection="WAITING_ROOM"; loadSection("FRIENDS_MENU") elseif choice=="الرسائل الخاصة" then Multiplayer.socialReturnSection="WAITING_ROOM"; loadSection("PRIVATE_MESSAGES") elseif choice=="دعوة صديق" then Multiplayer.socialReturnSection="WAITING_ROOM"; loadSection("FRIENDS_LIST_PLAY") elseif choice=="قائمة الموجودين" then loadSection("ROOM_PLAYERS") elseif choice=="مراسلة الغرفة" then loadSection("ROOM_CHAT") elseif choice=="أنا جاهز" or choice=="إلغاء الجاهزية" then local nextReady=choice=="أنا جاهز"; if firebaseSetPlayerReady then firebaseSetPlayerReady(nextReady,function(ok) playDynamicSFX(ok and "room_ready" or "network_error",0,1,0,0.74); say(ok and (nextReady and "تم تسجيل جاهزيتك." or "تم إلغاء جاهزيتك.") or "تعذر تحديث الجاهزية.",true); loadSection("WAITING_ROOM") end) else Multiplayer.playerReady=nextReady; loadSection("WAITING_ROOM") end elseif choice:match("^الجاهزون:") then firebaseRefreshRoom(function() say("عدد الجاهزين: "..tostring((function() local n=0; for _,v in pairs(Multiplayer.readyPlayers or {}) do if v then n=n+1 end end; return n end)()).." من "..tostring(#(Multiplayer.roomPlayers or {})),true); refreshVisibleScreen() end) elseif choice:match("^اختيار مدة السباق") then if Multiplayer.roomOwner==playerStats.username then chooseRaceDuration(function() loadSection("WAITING_ROOM") end) else say("مالك الغرفة هو الذي يحدد المدة.",true) end elseif choice:match("^ضبط عدد الروبوتات") then
      if Multiplayer.roomOwner ~= playerStats.username then say("مالك الغرفة هو الذي يضبط الروبوتات.", true); return end
      showSingleInput("عدد الروبوتات","اكتب رقمًا من صفر إلى ثلاثة",tostring(#(Multiplayer.bots or {})),false,function(value)
        local count=tonumber(value); if not count or count<0 or count>3 then say("أدخل عددًا من صفر إلى ثلاثة.",true); return end
        firebaseSetRoomBotCount(math.floor(count),function(ok) playDynamicSFX(ok and "robot_add" or "network_error",0,1,0,0.74); say(ok and "تم ضبط عدد الروبوتات للجميع." or "تعذر مزامنة الروبوتات.",true); loadSection("WAITING_ROOM") end)
      end) elseif choice=="تحديث حالة الغرفة" then firebaseRefreshRoom(function() say("تم تحديث الغرفة.",true); refreshVisibleScreen() end) elseif choice=="بدء السباق الآن" then
      if Multiplayer.roomOwner ~= playerStats.username then say("مالك الغرفة هو الذي يبدأ السباق.", true); return end
      local readyCount=0; for _,isReady in pairs(Multiplayer.readyPlayers or {}) do if isReady then readyCount=readyCount+1 end end; local humanCount=#(Multiplayer.roomPlayers or {}); if humanCount>0 and readyCount<humanCount then say("لا يبدأ السباق قبل جاهزية الجميع: "..tostring(readyCount).." من "..tostring(humanCount)..".",true); return end
      local goal=(LevelsData[math.min(playerStats.level,#LevelsData)] or {}).goal or 300
      firebaseStartRoomRace(goal,Multiplayer.selectedDuration or Settings.raceDurationSeconds or 300, function(ok)
        if not ok then say("تعذر مزامنة بداية السباق.", true); return end
        Multiplayer.waitingForPlayers=false; loadSection("PLAYING_MULTIPLAYER")
      end) elseif choice=="إلغاء والرجوع" then confirmLeaveRoom() end
  elseif currentSection=="ROOM_ACTIONS" then
    local owner=Multiplayer.roomOwner==playerStats.username; local target=Multiplayer.selectedPlayer or ""
    if choice=="الأصدقاء" then Multiplayer.socialReturnSection="WAITING_ROOM"; loadSection("FRIENDS_MENU") elseif choice=="الرسائل الخاصة" then Multiplayer.socialReturnSection="WAITING_ROOM"; loadSection("PRIVATE_MESSAGES") elseif choice=="دعوة صديق" then Multiplayer.socialReturnSection="WAITING_ROOM"; loadSection("FRIENDS_LIST_PLAY") elseif choice=="قائمة الموجودين" then loadSection("ROOM_PLAYERS") elseif choice=="إرسال رسالة للغرفة" then loadSection("ROOM_CHAT") elseif choice=="طلب إيقاف ميكروفوني" then Multiplayer.isMicEnabled=false; firebaseRoomAction("mute_self",playerStats.username,function() say("تم إيقاف ميكروفونك في الغرفة.",true) end)
    elseif choice=="كتم الجميع" or choice=="إلغاء كتم الجميع" then if not owner then say("هذا الإجراء للمالك فقط.",true) else firebaseSetRoomModeration(choice=="كتم الجميع" and "mute_all" or "unmute_all","",function(ok) say(ok and (choice=="كتم الجميع" and "تم كتم الجميع." or "تم إلغاء كتم الجميع.") or "تعذر تنفيذ الإجراء.",true); loadSection("ROOM_ACTIONS") end) end
    elseif choice=="كتم اللاعب المحدد" or choice=="إلغاء كتم اللاعب المحدد" or choice=="طرد اللاعب المحدد" or choice=="حظر اللاعب المحدد" or choice=="إلغاء حظر اللاعب المحدد" then
      if not owner then say("هذا الإجراء للمالك فقط.",true); return end
      if target=="" or target==playerStats.username then say("اختر لاعبًا آخر من قائمة الموجودين أولاً.",true); return end
      local actions={ ["كتم اللاعب المحدد"]="mute", ["إلغاء كتم اللاعب المحدد"]="unmute", ["طرد اللاعب المحدد"]="kick", ["حظر اللاعب المحدد"]="ban", ["إلغاء حظر اللاعب المحدد"]="unban" }
      firebaseSetRoomModeration(actions[choice],target,function(ok) local eventSound=({ban="moderation_ban",kick="moderation_kick",mute="moderation_mute",unmute="moderation_unmute",unban="moderation_unban"})[actions[choice]] or "settings_change"; playDynamicSFX(ok and eventSound or "network_error",0,1,0,0.94); say(ok and "تم تنفيذ الإجراء على "..target.."." or "تعذر تنفيذ الإجراء.",true); loadSection("ROOM_ACTIONS") end)
    elseif choice=="تغيير اسم الغرفة" then showSingleInput("تغيير اسم الغرفة","الاسم الجديد",Multiplayer.currentRoom,false,function(newName) if newName~="" then firebaseRoomAction("rename",newName,function(ok) if ok then Multiplayer.currentRoom=newName; say("تم تغيير الاسم.",true) end end) end end)
    elseif choice=="قفل الغرفة" or choice=="فتح الغرفة" then if not owner then say("هذا الإجراء للمالك فقط.",true) else local locked=choice=="قفل الغرفة"; firebaseSetRoomSettings(locked,nil,function(ok) playDynamicSFX(ok and (locked and "room_lock" or "room_unlock") or "network_error",0,1,0,0.94); say(ok and (locked and "تم قفل الغرفة." or "تم فتح الغرفة.") or "تعذر تحديث حالة الغرفة.",true); loadSection("ROOM_ACTIONS") end) end
    elseif choice=="تغيير حد اللاعبين" then if not owner then say("هذا الإجراء للمالك فقط.",true) else showSingleInput("حد اللاعبين","اكتب رقمًا من 2 إلى 4",tostring(Multiplayer.roomMaxPlayers or 4),false,function(value) local maxPlayers=math.floor(tonumber(value) or 0); if maxPlayers<2 or maxPlayers>4 then say("الحد يجب أن يكون من 2 إلى 4.",true) else firebaseSetRoomSettings(nil,maxPlayers,function(ok) playDynamicSFX(ok and "room_max_change" or "network_error",0,1,0,0.94); say(ok and "تم تغيير الحد الأقصى للاعبين." or "تعذر تغيير الحد الأقصى.",true); loadSection("ROOM_ACTIONS") end) end end) end
    elseif choice=="تعيين مساعد للمالك" then if not owner then say("هذا الإجراء للمالك فقط.",true) elseif target=="" or target==playerStats.username then say("اختر لاعبًا آخر أولًا.",true) else firebaseSetRoomModerator(target,true,function(ok) playDynamicSFX(ok and "moderator_assign" or "network_error",0,1,0,0.94); say(ok and "تم تعيين اللاعب مساعدًا للمالك." or "تعذر تعيين المساعد.",true); loadSection("ROOM_ACTIONS") end) end
    elseif choice=="تشغيل موسيقى الغرفة" then if not owner then say("مالك الغرفة فقط يمكنه تشغيل الموسيقى.",true) else showSingleInput("موسيقى الغرفة","أدخل رابط HTTPS مباشر لملف صوتي فقط","",false,function(url) firebaseSetRoomMusic(url,true,function(ok,msg) playDynamicSFX(ok and "room_ready" or "network_error",0,1,0,0.78); say(ok and "بدأت موسيقى الغرفة عند الجميع." or (msg or "تعذر تشغيل الموسيقى."),true); loadSection("ROOM_ACTIONS") end) end) end
    elseif choice=="إيقاف موسيقى الغرفة" then if not owner then say("مالك الغرفة فقط يمكنه إيقاف الموسيقى.",true) else firebaseStopRoomMusic(function(ok,msg) playDynamicSFX(ok and "menu_select" or "network_error",0,1,0,0.78); say(ok and "توقفت موسيقى الغرفة عند الجميع." or (msg or "تعذر إيقاف الموسيقى."),true); loadSection("ROOM_ACTIONS") end) end
    elseif choice=="سجل إجراءات الغرفة" then firebaseGet("/rooms/"..firebaseEncodeKey(Multiplayer.currentRoom).."/actions",function(ok,body) if not ok or not body or body=="null" then say("لا يوجد سجل إجراءات بعد.",true); return end; local n=0; for _ in string.gmatch(body,'"action"%s*:') do n=n+1 end; playDynamicSFX("leaderboard_refresh",0,1,0,0.72); say("سجل إجراءات الغرفة يحتوي على "..tostring(n).." إجراء.",true) end)
    elseif choice=="رجوع" then local back=Multiplayer.raceReturnSection or "WAITING_ROOM"; Multiplayer.raceReturnSection=nil; loadSection(back) end
  elseif currentSection=="ROOM_PLAYERS" then
    if choice=="تحديث القائمة" then loadSection("ROOM_PLAYERS") elseif choice=="رجوع" then loadSection("WAITING_ROOM") else Multiplayer.selectedPlayer=selectedUsername(choice); loadSection("ROOM_ACTIONS") end
  elseif currentSection=="ROOM_CHAT" then
    if choice=="كتابة رسالة" then showSingleInput("رسالة الغرفة","اكتب الرسالة","",false,function(message) if message~="" then firebaseSendChat(message,function(ok) playDynamicSFX(ok and "chat_send_soft" or "network_error",0,1,0,0.96); say(ok and "تم إرسال الرسالة." or "تعذر إرسال الرسالة.",true); loadSection("ROOM_CHAT") end) end end)
    elseif choice=="قراءة آخر رسالة" then local last=Multiplayer.chatMessages and Multiplayer.chatMessages[#Multiplayer.chatMessages] or ""; playDynamicSFX("message_receive",0,1,0,0.70); say(last~="" and ("آخر رسالة: "..last) or "لا توجد رسائل محفوظة.",true)
    elseif choice=="تسجيل رسالة صوتية" then playDynamicSFX("mic_on",0,1,0,0.72); startVoiceRecording(Multiplayer.currentRoom)
    elseif choice=="تشغيل آخر رسالة صوتية" then playDynamicSFX("chat_receive",0,1,0,0.70); playLatestVoiceMessage()
    elseif choice=="تحديث الرسائل" then loadSection("ROOM_CHAT") elseif choice=="رجوع" then local back=Multiplayer.raceReturnSection or "WAITING_ROOM"; Multiplayer.raceReturnSection=nil; loadSection(back) end
  elseif currentSection=="DAILY_REWARD" then
    if choice=="تدوير عجلة الحظ" then
      local now=os.time(); local last=tonumber(playerStats.wheelLastSpinAt or 0) or 0; local remaining=18000-(now-last)
      if remaining>0 then local hours=math.floor(remaining/3600); local minutes=math.floor((remaining%3600)/60); say("العجلة متاحة مرة كل خمس ساعات. المتبقي "..tostring(hours).." ساعة و"..tostring(minutes).." دقيقة.",true) elseif GameState.wheelSpinning then say("العجلة تدور الآن. انتظر النتيجة.",true) else
        local reward=WheelRewards[math.random(1,#WheelRewards)]; local claimKey=(wrUniqueId and wrUniqueId("wheel") or (tostring(now).."_wheel_"..tostring(math.random(1000,9999)))); GameState.wheelSpinning=true; playDynamicSFX("wheel_spin_long",0,1,0,0.78); say("تدور عجلة الحظ الآن.",true)
        Handler().postDelayed(Runnable({run=function()
          claimRewardSafely(claimKey,reward,"wheel",function(ok,msg,already)
            GameState.wheelSpinning=false
            if not ok then playDynamicSFX("network_error",0,1,0,0.78); say(msg or "تعذر اعتماد نتيجة العجلة.",true); loadSection("DAILY_REWARD"); return end
            playerStats.dailyClaimed=false; playerStats.wheelLastSpinAt=os.time(); playDynamicSFX("wheel_reward",0,1,0,0.90)
            if reward.noWin or (tonumber(reward.amount or 0) or 0)<=0 then say("لم تربح هذه المرة. يمكنك تدوير العجلة بعد خمس ساعات.",true) else say("لقد ربحت "..tostring(reward.amount).." عملة. يمكنك تدوير العجلة بعد خمس ساعات.",true) end
            loadSection("DAILY_REWARD")
          end)
        end}),5200)
      end
    elseif choice=="حالة المكافأة اليومية" then local remaining=18000-(os.time()-(tonumber(playerStats.wheelLastSpinAt or 0) or 0)); if remaining>0 then say("العجلة غير متاحة بعد. المتبقي "..tostring(math.floor(remaining/3600)).." ساعة و"..tostring(math.floor((remaining%3600)/60)).." دقيقة.",true) else say("عجلة الحظ متاحة الآن.",true) end
    elseif choice=="رجوع للقائمة" then loadSection("MENU") end
  elseif currentSection=="SETTINGS" then
    if choice=="الإعدادات العامة" then loadSection("SETTINGS_GENERAL") elseif choice=="إعدادات الصوت" then loadSection("SETTINGS_AUDIO") elseif choice=="إعدادات الوصول" then loadSection("SETTINGS_ACCESSIBILITY") elseif choice=="إعدادات السباق" then loadSection("SETTINGS_RACE") elseif choice=="إعدادات الحساب والتحديث" then loadSection("SETTINGS_ACCOUNT") elseif choice=="رجوع للقائمة" then loadSection("MENU") end
  elseif currentSection=="SETTINGS_GENERAL" then
    if choice=="تشغيل وإيقاف الاهتزاز" then Settings.vibrationEnabled=not Settings.vibrationEnabled; saveGameData(); say(Settings.vibrationEnabled and "تم تشغيل الاهتزاز." or "تم إيقاف الاهتزاز.",true) elseif choice=="تفعيل وضع حفظ الطاقة" then Settings.powerSaveMode=not Settings.powerSaveMode; saveGameData(); say(Settings.powerSaveMode and "تم تفعيل حفظ الطاقة." or "تم إيقاف حفظ الطاقة.",true) elseif choice=="فحص التحديثات" then checkForUpdates() elseif choice=="فتح التواصل عبر واتساب" then openWhatsAppContact() elseif choice=="رجوع للإعدادات" then loadSection("SETTINGS") end
  elseif currentSection=="SETTINGS_AUDIO" then
    if choice=="سرعة الناطق بالسحب بلا حد" or choice=="حدة الناطق بالسحب" or choice=="مستوى المؤثرات بالسحب" or choice=="مستوى الموسيقى بالسحب" or choice=="مستوى المذيع بالسحب" then say("اسحب لأعلى للرفع أو لأسفل للخفض. اللمس لا يغير المستوى.",true)
    elseif choice=="تشغيل وإيقاف النطق" then Settings.ttsEnabled=not Settings.ttsEnabled; saveGameData(); say(Settings.ttsEnabled and "تم تشغيل النطق." or "تم إيقاف النطق.",true) elseif choice=="تسريع النطق" then Settings.voiceSpeed=math.max(0.05,tonumber(Settings.voiceSpeed or 1)+0.1); if tts then tts.setSpeechRate(Settings.voiceSpeed) end; saveGameData(); say("سرعة الناطق: "..tostring(Settings.voiceSpeed),true) elseif choice=="تبطيء النطق" then Settings.voiceSpeed=math.max(0.2,Settings.voiceSpeed-0.1); if tts then tts.setSpeechRate(Settings.voiceSpeed) end; saveGameData(); say("سرعة الناطق: "..tostring(Settings.voiceSpeed),true) elseif choice=="رفع حدة الناطق" then Settings.voicePitch=math.min(2,Settings.voicePitch+0.1); if tts then tts.setPitch(Settings.voicePitch) end; saveGameData(); say("تم رفع حدة الناطق.",true) elseif choice=="خفض حدة الناطق" then Settings.voicePitch=math.max(0.5,Settings.voicePitch-0.1); if tts then tts.setPitch(Settings.voicePitch) end; saveGameData(); say("تم خفض حدة الناطق.",true) elseif choice=="تشغيل وإيقاف المؤثرات" then Settings.sfxEnabled=not Settings.sfxEnabled; saveGameData(); say(Settings.sfxEnabled and "تم تشغيل المؤثرات." or "تم إيقاف المؤثرات.",true) elseif choice=="رفع مستوى المؤثرات" then Settings.sfxVolume=math.min(1,Settings.sfxVolume+0.1); saveGameData(); say("مستوى المؤثرات: "..Settings.sfxVolume,true) elseif choice=="خفض مستوى المؤثرات" then Settings.sfxVolume=math.max(0,Settings.sfxVolume-0.1); saveGameData(); say("مستوى المؤثرات: "..Settings.sfxVolume,true) elseif choice=="تشغيل وإيقاف الموسيقى" then Settings.bgmEnabled=not Settings.bgmEnabled; if Settings.bgmEnabled then pcall(function() if bgmPlayer then bgmPlayer.start() end end) else stopMusicPlayback() end; saveGameData(); playDynamicSFX("menu_toggle",0,1,0,0.30); say(Settings.bgmEnabled and "تم تشغيل الموسيقى." or "تم إيقاف الموسيقى.",true) elseif choice=="المقطع الموسيقي التالي" then nextMusicTrack(); say("تم تشغيل المقطع الموسيقي التالي.",true) elseif choice=="المقطع الموسيقي السابق" then previousMusicTrack(); say("تم تشغيل المقطع الموسيقي السابق.",true) elseif choice=="رفع مستوى الموسيقى" then Settings.bgmVolume=math.min(1,Settings.bgmVolume+0.1); restoreMenuMusic(); saveGameData(); say("مستوى الموسيقى: "..Settings.bgmVolume,true) elseif choice=="خفض مستوى الموسيقى" then Settings.bgmVolume=math.max(0,Settings.bgmVolume-0.1); restoreMenuMusic(); saveGameData(); say("مستوى الموسيقى: "..Settings.bgmVolume,true) elseif choice=="تشغيل وإيقاف المذيع" then Settings.announcerEnabled=not Settings.announcerEnabled; saveGameData(); say(Settings.announcerEnabled and "تم تشغيل المذيع." or "تم إيقاف المذيع.",true) elseif choice=="تشغيل وإيقاف التعليق الذكي" then Settings.smartAnnouncerEnabled=not Settings.smartAnnouncerEnabled; saveGameData(); if firebaseSyncProfile then firebaseSyncProfile(function() end) end; say(Settings.smartAnnouncerEnabled and "تم تشغيل التعليق الذكي." or "تم إيقاف التعليق الذكي.",true) elseif choice=="رفع مستوى المذيع" then Settings.announcerVolume=math.min(1,Settings.announcerVolume+0.1); saveGameData(); say("مستوى المذيع: "..Settings.announcerVolume,true) elseif choice=="خفض مستوى المذيع" then Settings.announcerVolume=math.max(0,Settings.announcerVolume-0.1); saveGameData(); say("مستوى المذيع: "..Settings.announcerVolume,true) elseif choice=="رجوع للإعدادات" then loadSection("SETTINGS") end
  elseif currentSection=="SETTINGS_ACCESSIBILITY" then
    if choice=="اختيار اللغة" then showLanguagePicker(function() loadSection("SETTINGS_ACCESSIBILITY") end) elseif choice=="اختيار وضع التفاعل" or choice=="تغيير وضع العرض" then showAccessibilityModeDialog() elseif choice=="اختيار لوحة المفاتيح" then showKeyboardModeDialog() elseif choice=="تفعيل النقر المزدوج للمكفوفين" then Settings.doubleTapMenus=true; saveGameData(); say("في وضع المكفوفين، كل الأيقونات تُفعّل بالضغطتين.",true) elseif choice=="اختيار اتجاه الشاشة" then chooseOrientationDialog() elseif choice=="رجوع للإعدادات" then loadSection("SETTINGS") end
  elseif currentSection=="SETTINGS_RACE" then
    if choice=="اختيار نمط التحكم" then chooseControlModeDialog() elseif choice=="اختيار طريقة التوجيه" then chooseSteeringModeDialog() elseif choice=="معايرة الإمالة" then if calibrateTiltControl then calibrateTiltControl() else say("معايرة الإمالة غير متاحة.",true) end elseif choice=="حساسية الإمالة" then chooseTiltSensitivityDialog() elseif choice=="عكس اتجاه الإمالة" then Settings.invertTilt=not (Settings.invertTilt==true); saveGameData(); playDynamicSFX("settings_change",0,1,0,0.68); say(Settings.invertTilt and "تم عكس اتجاه الإمالة." or "تم إلغاء عكس اتجاه الإمالة.",true) elseif choice=="تغيير مهلة تنبيه الاصطدام" then Settings.warningSeconds=Settings.warningSeconds==3 and 5 or (Settings.warningSeconds==5 and 2 or 3); saveGameData(); say("المهلة "..Settings.warningSeconds.." ثوانٍ.",true) elseif choice=="تغيير جودة الرادار" then Settings.radarQuality=Settings.radarQuality=="عالية" and "متوسطة" or (Settings.radarQuality=="متوسطة" and "منخفضة" or "عالية"); saveGameData(); say("جودة الرادار: "..Settings.radarQuality,true) elseif choice=="تشغيل وإيقاف أزرار السباق" then Settings.showActionButtons=not Settings.showActionButtons; saveGameData(); refreshVisibleScreen(); say(Settings.showActionButtons and "تم إظهار أزرار السباق." or "تم إخفاء أزرار السباق.",true) elseif choice=="تشغيل وإيقاف الواقعية المتقدمة" then Settings.raceRealismEnabled=Settings.raceRealismEnabled==false; saveGameData(); say(Settings.raceRealismEnabled and "تم تشغيل الواقعية المتقدمة." or "تم إيقاف الواقعية المتقدمة.",true) elseif choice=="تشغيل وإيقاف صوت قرب المنافس" then Settings.opponentProximityEnabled=Settings.opponentProximityEnabled==false; saveGameData(); say(Settings.opponentProximityEnabled and "تم تشغيل صوت قرب المنافس." or "تم إيقاف صوت قرب المنافس.",true) elseif choice=="تغيير فترة تقرير السباق" then Settings.periodicRaceReportSeconds=Settings.periodicRaceReportSeconds==15 and 30 or (Settings.periodicRaceReportSeconds==30 and 10 or 15); saveGameData(); say("تقرير السباق كل "..tostring(Settings.periodicRaceReportSeconds).." ثانية.",true) elseif choice=="رجوع للإعدادات" then loadSection("SETTINGS") end
  elseif currentSection=="SETTINGS_ACCOUNT" then
    if choice=="اختبار الاتصال" then if firebaseRefreshConnectionQuality then firebaseRefreshConnectionQuality(function(ok,latency) say(ok and ("الاتصال متاح. زمن الاستجابة "..tostring(latency).." ميلي ثانية.") or "تعذر الاتصال بالخادم.",true) end) else firebaseGet("/",function(ok) say(ok and "الاتصال متاح." or "تعذر الاتصال.",true) end) end elseif choice=="مساحة الرسائل الصوتية" then local used=tonumber(playerStats.voiceStorageBytes or 0) or 0; local limit=tonumber(playerStats.voiceStorageLimitBytes or 20971520) or 20971520; local remaining=math.max(0,limit-used); say("استخدمت "..string.format("%.2f",used/1048576).." ميغابايت. المتبقي "..string.format("%.2f",remaining/1048576).." ميغابايت من أصل 20 ميغابايت.",true) elseif choice=="فحص تحديثات التطبيق" then checkForUpdates() elseif choice=="تسجيل الخروج" then firebaseSignOut(); currentSection="LOGIN"; showAuthEntry() elseif choice=="مسح بيانات اللعبة وإعادة الضبط" then activity.getSharedPreferences("HackerCenterProSettings",Context.MODE_PRIVATE).edit().clear().commit(); firebaseSignOut(); showAuthEntry() elseif choice=="رجوع للإعدادات" then loadSection("SETTINGS") end
  elseif currentSection=="STORE" then if choice=="معرض السيارات" then loadSection("CAR_DEALER") elseif choice=="متجر الأدوات" then loadSection("STORE_ITEMS") elseif choice=="رجوع للقائمة" then loadSection("MENU") end
  elseif currentSection=="CAR_DEALER" then if choice=="رجوع للمتجر" then loadSection("STORE") else local car=Store[currentMenuIndex]; if car and playerStats.money>=car.price then local owned=false; for _,id in ipairs(Inventory) do if id==car.id then owned=true end end; if owned then say("تمتلك السيارة بالفعل.",true) else confirmPurchase(car.name or "السيارة",car.price,function() local before=playerStats.money; playerStats.money=before-car.price; table.insert(Inventory,car.id); saveGameData(); if firebaseRecordEconomyLedger then firebaseRecordEconomyLedger("purchase",-car.price,"شراء "..tostring(car.name or "السيارة"),"purchase_car_"..tostring(car.id).."_"..tostring(os.time()),function() end) end; if wrAudit then wrAudit("purchase",{item=car.name or "السيارة",price=car.price,before=before,after=playerStats.money}) end; firebaseSyncProfile(function() end); say("تم شراء "..tostring(car.name or "السيارة")..".",true); loadSection("CAR_DEALER") end) end else say("الرصيد غير كاف.",true) end end
  elseif currentSection=="STORE_ITEMS" then if choice=="رجوع للمتجر" then loadSection("STORE") else local item=StoreItems[currentMenuIndex]; if item and playerStats.money>=item.price then confirmPurchase(item.name or "الأداة",item.price,function() local before=playerStats.money; playerStats.money=before-item.price; playerStats[item.field]=(playerStats[item.field] or 0)+(item.amount or 1); saveGameData(); if firebaseRecordEconomyLedger then firebaseRecordEconomyLedger("purchase",-item.price,"شراء "..tostring(item.name or "الأداة"),"purchase_item_"..tostring(item.id or item.field).."_"..tostring(os.time()),function() end) end; if wrAudit then wrAudit("purchase",{item=item.name or "الأداة",price=item.price,before=before,after=playerStats.money}) end; firebaseSyncProfile(function() end); say("تم شراء "..tostring(item.name or "الأداة")..".",true); loadSection("STORE_ITEMS") end) elseif item then say("الرصيد غير كاف.",true) end end
  elseif currentSection=="GAS_STATION" then if choice=="شراء خزان وقود كامل (500)" then if playerStats.money>=500 then confirmPurchase("خزان وقود كامل",500,function() local before=playerStats.money; playerStats.money=before-500; playerStats.fuelTanks=playerStats.fuelTanks+1; saveGameData(); if firebaseRecordEconomyLedger then firebaseRecordEconomyLedger("purchase",-500,"شراء خزان وقود","purchase_fuel_"..tostring(os.time()),function() end) end; if wrAudit then wrAudit("purchase",{item="خزان وقود",price=500,before=before,after=playerStats.money}) end; playDynamicSFX("fuel_fill",0,1,0,0.90); say("تم شراء الوقود.",true); loadSection("GAS_STATION") end) else say("الرصيد غير كاف.",true) end elseif choice=="رجوع للقائمة" then loadSection("MENU") end
  elseif currentSection=="GARAGE" then
    if choice=="رجوع للقائمة" then loadSection("MENU") elseif choice=="لا توجد أعطال حالية" then say("السيارة سليمة ولا تحتاج إلى إصلاح.",true) else
      local field, label, factor = nil, "", 1
      if choice:match("المحرك") then field="engine"; label="المحرك"; factor=5 elseif choice:match("الإطارات") then field="tires"; label="الإطارات"; factor=4 elseif choice:match("الهيكل") then field="body"; label="الهيكل"; factor=3 elseif choice:match("الفرامل") then field="brakes"; label="الفرامل"; factor=4 end
      local damage=field and ((playerStats.carDamage and playerStats.carDamage[field]) or 0) or 0; local cost=math.max(100,damage*factor)
      if field and damage>0 and playerStats.money>=cost then playerStats.money=playerStats.money-cost; playerStats.carDamage[field]=0; local car=Store[activeCarId] or Store[1]; playerCarHp=math.min(car.maxHp,playerCarHp+damage); saveGameData(); playDynamicSFX("repair_start",0,1,0,0.58); playDynamicSFX("repair_done",0,1,0,0.78); firebaseSyncProfile(function() end); say("تم إصلاح "..label..".",true); loadSection("GARAGE") else say(field and "الرصيد غير كافٍ أو الجزء سليم." or "الجزء غير موجود.",true) end
    end
  elseif currentSection=="INVENTORY" then
    if choice=="رجوع للقائمة" then loadSection("MENU")
    elseif choice:match("^فتح صندوق محفوظ") then
      if (playerStats.rewardChests or 0)<=0 then say("لا توجد صناديق محفوظة.",true) else
        local rewards={{kind="money",amount=5000,label="5,000 عملة"},{kind="money",amount=8000,label="8,000 عملة"},{kind="money",amount=12000,label="12,000 عملة"},{kind="money",amount=18000,label="18,000 عملة"},{kind="money",amount=25000,label="25,000 عملة"},{kind="money",amount=35000,label="35,000 عملة"}}
        local reward=rewards[math.random(1,#rewards)]; local claimKey=(wrUniqueId and wrUniqueId("chest") or (tostring(os.time()).."_chest_"..tostring(math.random(1000,9999))))
        claimRewardSafely(claimKey,reward,"chest",function(ok,msg,already)
          if not ok then playDynamicSFX("network_error",0,1,0,0.78); say(msg or "تعذر اعتماد الصندوق.",true); return end
          playerStats.rewardChests=math.max(0,(playerStats.rewardChests or 0)-1); saveGameData(); playDynamicSFX("wheel_reward",0,1,0,0.90); say("فتحت الصندوق وحفظت النتيجة في حسابك. حصلت على "..tostring(reward.amount).." عملة.",true); loadSection("INVENTORY")
        end)
      end
    else
      local id=Inventory[currentMenuIndex]; if id then local selectedCar=Store[id] or Store[1]; activeCarId=id; AudioSystem.engineSoundKey=selectedCar.engineSound or "engine_compact"; playerCarHp=selectedCar.maxHp; saveGameData(); playDynamicSFX("car_select",0,1,0,0.72); say("تم تجهيز السيارة. صوت المحرك: "..tostring(AudioSystem.engineSoundKey)..".",true); loadSection("MENU") end
    end
  elseif currentSection=="WIN" or currentSection=="LOSE" then if choice=="موافق" then say("تم حفظ نتيجة السباق. نراك في الجولة التالية.",true); loadSection("MENU") elseif choice=="ورشة الصيانة" then loadSection("GARAGE") elseif choice=="محطة البنزين" then loadSection("GAS_STATION") elseif choice=="قائمة المتصدرين" then loadSection("LEADERBOARD") elseif choice=="نصائح التحسين" then say(tostring(GameState.lastRaceSummary or "طوّر الفرامل والإطارات، وحافظ على الوقود، واستعمل السحب المبكر لتفادي العقبات."),true) elseif choice=="رجوع للقائمة" then loadSection("MENU") end
  elseif currentSection=="ABOUT" then
    if choice=="عن World Racing" then say("المطور: Ahmed El King. يمكنك الوصول إلى الدعم والقناة والمجموعة من هذه الصفحة.",true)
    elseif choice=="التواصل عبر واتساب" then openWhatsAppContact()
    elseif choice=="قناة التحديثات" then openUpdatesChannel()
    elseif choice=="مجموعة المجتمع" then openCommunityGroup()
    elseif choice=="شروط الاستخدام والخصوصية" then showTermsFromAbout()
    elseif choice=="رجوع للقائمة" then loadSection("MENU") end
  end
end
