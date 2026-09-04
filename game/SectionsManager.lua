local titles = {
  MENU="World Racing", PROFILE="الملف الشخصي", GAME_INVITES="دعوات اللعب", DAILY_REWARD="المكافأة اليومية", RACE_MODE_SELECT="اختيار طريقة اللعب", RACE_DURATION="مدة السباق",
  CREATE_ROOM="إنشاء غرفة", JOIN_ROOM="الانضمام إلى غرفة", HOTSPOT_MODE="اللعب عبر Hotspot", FRIENDS_MENU="قائمة الأصدقاء والدعوات والدردشة", NOTIFICATIONS="الإشعارات", PRIVATE_MESSAGES="الرسائل الخاصة", BLOCKED_USERS="المحظورون",
  FRIENDS_LIST_PLAY="الأصدقاء", FRIEND_REQUESTS="طلبات الصداقة", ONLINE_PLAYERS="اللاعبون المتصلون", PLAYER_PROFILE="ملف لاعب",
  LEADERBOARD="قائمة المتصدرين", ACHIEVEMENTS="المهام والإنجازات", WAITING_ROOM="غرفة الانتظار", ROOM_ACTIONS="إجراءات الغرفة", ROOM_PLAYERS="الموجودون في الغرفة",
  ROOM_CHAT="مراسلة الغرفة", SETTINGS="الإعدادات", STORE="المتجر", CAR_DEALER="معرض السيارات", INVENTORY="الحقيبة",
  GAS_STATION="محطة البنزين", GARAGE="ورشة الصيانة", ABOUT="حول اللعبة", PLAYING="السباق الفردي", PLAYING_MULTIPLAYER="السباق الجماعي", WIN="نتيجة السباق", LOSE="نتيجة السباق"
}
local function announce(title)
  say(title or titles[currentSection] or "World Racing", true); refreshVisibleScreen()
  if currentMenuOptions and currentMenuOptions[1] then say(currentMenuOptions[currentMenuIndex], false) end
end
local function setOptions(options, title)
  currentMenuOptions = options or {}; currentMenuIndex = math.max(1, math.min(currentMenuIndex or 1, #currentMenuOptions)); announce(title)
end
local function parseRemoteList(content)
  local items = {}
  for username, object in string.gmatch(content or "", '"([^"\\]+)"%s*:%s*{([^{}]*)}') do
    local name = object:match('"name"%s*:%s*"(.-)"') or username
    local bio = object:match('"bio"%s*:%s*"(.-)"') or ""
    Multiplayer.remoteProfiles[username] = {username=username, name=name, bio=bio, raw=object}
    if username ~= playerStats.username then table.insert(items, name .. " (@" .. username .. ")") end
  end
  return items
end
function loadSection(sectionName)
  local previousSection=currentSection
  if sectionName=="LOSE" and not GameState.raceOutcomeRecorded and (previousSection=="PLAYING" or previousSection=="PLAYING_MULTIPLAYER") then
    GameState.raceOutcomeRecorded=true; if achievementsRecordRaceFinish then achievementsRecordRaceFinish(false,0,GameState.raceDurationSeconds,previousSection) end; if SmartAnnouncer and SmartAnnouncer.finish then SmartAnnouncer.finish(false,0) end
  end
  if sectionName ~= "PLAYING" and sectionName ~= "PLAYING_MULTIPLAYER" and restoreMenuMusic then restoreMenuMusic() end
  if (previousSection=="PLAYING" or previousSection=="PLAYING_MULTIPLAYER") and sectionName~="PLAYING" and sectionName~="PLAYING_MULTIPLAYER" then
    GameState.lastRaceSpeed=math.floor(GameState.speed or 0); GameState.lastRaceDistance=math.floor(GameState.distance or 0); GameState.lastRaceFuel=math.floor(GameState.fuel or 0); GameState.lastRaceCrates=math.floor(GameState.collectedCrates or 0); GameState.lastRaceCollisionCount=math.floor(GameState.collisionCount or 0); GameState.lastRaceWeather=tostring(GameState.weather or "صافي"); GameState.lastRaceRoad=tostring(GameState.roadCondition or "جاف"); pcall(saveGameData)
    GameState.isGasPressed=false; GameState.isBrakePressed=false; if stopTiltControl then stopTiltControl() end; if stopEngineSound then stopEngineSound() end; if stopRaceEnvironmentSound then stopRaceEnvironmentSound() end; if stopHornSound then stopHornSound() end; if SmartAnnouncer and SmartAnnouncer.cancelRaceVoices then SmartAnnouncer.cancelRaceVoices() end; if tts then pcall(function() tts.stop() end) end
  end
  if VoiceSystem and VoiceSystem.stopVoicePolling then
    if sectionName ~= "WAITING_ROOM" and sectionName ~= "PLAYING_MULTIPLAYER" and sectionName ~= "ROOM_CHAT" then VoiceSystem.stopVoicePolling() end
  end
  if sectionName == "WAITING_ROOM" or sectionName == "PLAYING_MULTIPLAYER" or sectionName == "ROOM_CHAT" then if firebaseWatchRoomChat then firebaseWatchRoomChat() end; if firebaseWatchRoomActivity then firebaseWatchRoomActivity() end elseif stopRoomMessagePolling then stopRoomMessagePolling(); if firebaseStopRoomActivityWatch then firebaseStopRoomActivityWatch() end end
  if sectionName=="PLAYING_MULTIPLAYER" then if firebaseWatchRaceEvents then firebaseWatchRaceEvents() end elseif firebaseStopRaceEventWatch then firebaseStopRaceEventWatch() end
  currentSection = sectionName; currentMenuIndex = 1
  if sectionName == "MENU" then
    setOptions({"بدء السباق", "الملف الشخصي", "قائمة المتصدرين", "البطولات", "المهام والإنجازات", "المكافأة اليومية", "قائمة الأصدقاء والدعوات والدردشة", "الحقيبة", "المتجر", "محطة البنزين", "ورشة الصيانة", "الإعدادات", "حول المطورين", "خروج"}, "القائمة الرئيسية. الرصيد " .. playerStats.money)
  elseif sectionName == "PROFILE" then
    setOptions({"فتح ملفي التفاعلي", "عرض بياناتي", "قسم الدعوات", "تغيير كلمة المرور", "تغيير وضع العرض", "تسجيل الخروج", "رجوع للقائمة"}, "الملف الشخصي")
    firebaseGetGameInvites(function(ok,list) local count=(list and #list or 0); say(count>0 and "لديك "..count.." دعوة لعب جديدة." or "لا توجد دعوات لعب جديدة.",true); refreshVisibleScreen() end)
  elseif sectionName == "GAME_INVITES" then
    local options={}; for i,invite in ipairs(Multiplayer.gameInvites or {}) do table.insert(options,"دعوة من "..tostring(invite.fromName or invite.from).." إلى "..tostring(invite.roomName or "سباق خاص")) end; if #options==0 then table.insert(options,"لا توجد دعوات حالية") end; table.insert(options,"تحديث الدعوات"); table.insert(options,"رجوع للملف الشخصي"); setOptions(options,"دعوات اللعب. لديك "..tostring(math.max(0,#(Multiplayer.gameInvites or {}))).." دعوة")
    firebaseGetGameInvites(function(ok) refreshVisibleScreen(); if ok and #(Multiplayer.gameInvites or {})>0 then say("لديك دعوات لعب من أصدقائك.",true) end end)
  elseif sectionName == "ACHIEVEMENTS" then
    local options={}; local done,total=achievementsSummary(); currentMissionOptionIds={}; for _,item in ipairs(achievementsList()) do local state=item.completed and (item.rewardClaimed and "مكتمل، المكافأة مستلمة" or "مكتمل، استلم المكافأة") or (tostring(math.floor(item.progress)).."/"..tostring(item.target)); table.insert(options,state..": "..item.title.." — "..item.description.." — المكافأة "..tostring(item.reward or 0).." عملة") end; if missionsSummary then for _,row in ipairs(missionsSummary()) do local label="مهمة "..row.group..": "..tostring(math.floor(row.progress)).."/"..tostring(row.target).." — "..row.title.." — المكافأة "..tostring(row.reward).." عملة"; table.insert(options,label); currentMissionOptionIds[label]=row.id end end; table.insert(options,"تحديث المهام"); table.insert(options,"رجوع للقائمة"); setOptions(options,"المهام والإنجازات. المكتمل "..done.." من "..total)
  elseif sectionName == "DAILY_REWARD" then
    setOptions({"تدوير عجلة الحظ", "حالة المكافأة اليومية", "رجوع للقائمة"}, "المكافأة اليومية وعجلة الحظ")
  elseif sectionName == "RACE_MODE_SELECT" then
    setOptions({"اللعب مع الكمبيوتر", "إنشاء غرفة عبر الإنترنت", "الانضمام إلى غرفة عبر الإنترنت", "اللعب مع صديق", "اللعب عبر Hotspot", "مدة السباق", "رجوع للقائمة"}, "اختر طريقة اللعب")
  elseif sectionName == "RACE_DURATION" then
    setOptions({"دقيقة", "دقيقتان", "ثلاث دقائق", "أربع دقائق", "خمس دقائق", "ست دقائق", "سبع دقائق", "ثماني دقائق", "تسع دقائق", "عشر دقائق", "إحدى عشرة دقيقة", "اثنتا عشرة دقيقة", "مدة مخصصة", "رجوع"}, "مدة السباق. اختر من دقيقة إلى اثنتي عشرة دقيقة أو اختر مدة مخصصة بالدقائق أو الثواني.")
  elseif sectionName == "CUSTOM_RACE_DURATION" then
    setOptions({"الوحدة: دقائق", "تطبيق المدة", "رجوع"}, "مدة مخصصة. اكتب العدد في الحقل، واختر دقائق أو ثواني، ثم اختر تطبيق المدة.")
  elseif sectionName == "HOTSPOT_MODE" then
    setOptions({"استضافة سباق Hotspot", "الانضمام إلى سباق Hotspot", "رجوع"}, "اختر طريقة اللعب عبر Hotspot")
  elseif sectionName == "CREATE_ROOM" then
    setOptions({"إنشاء غرفة عامة", "إنشاء غرفة خاصة", "إنشاء غرفة مع روبوت", "رجوع"}, "إنشاء غرفة. اختر نوع الغرفة ثم المدة.")
  elseif sectionName == "JOIN_ROOM" then
    Multiplayer.roomDirectory={}; Multiplayer.publicRoomDirectory={}; setOptions({"تحديث قائمة الغرف", "إدخال اسم الغرفة", "رجوع"}, "الانضمام إلى غرفة عامة")
    local function renderRooms(content)
      if not content or content=="null" then say("لا توجد غرف عامة متاحة حاليًا.",true); refreshVisibleScreen(); return end
      local options={}; local rooms=firebaseListPublicRooms and firebaseListPublicRooms(content) or {}; for _,room in ipairs(rooms) do local label="غرفة: "..tostring(room.displayName).."، المالك: "..tostring(room.owner).."، المتواجدون: "..tostring(room.playerCount or 0).." من "..tostring(room.maxPlayers or 4); Multiplayer.roomDirectory[label]=room.room; Multiplayer.publicRoomDirectory[label]=room; table.insert(options,label) end
      table.insert(options,"تحديث قائمة الغرف"); table.insert(options,"إدخال اسم الغرفة"); table.insert(options,"رجوع"); currentMenuOptions=options; currentMenuIndex=1; say(#rooms>0 and "اختر غرفة لعرض المتواجدين أو الانضمام." or "لا توجد غرف عامة متاحة حاليًا.",true); refreshVisibleScreen()
    end
    if firebaseCleanupEmptyRooms then firebaseCleanupEmptyRooms(function(cleanOk,removed) firebaseGet("/rooms",function(ok,content) if ok then renderRooms(content) else say("تعذر تحديث الغرف العامة.",true); refreshVisibleScreen() end end) end) else firebaseGet("/rooms",function(ok,content) if ok then renderRooms(content) else say("تعذر تحديث الغرف العامة.",true); refreshVisibleScreen() end end) end
  elseif sectionName == "PUBLIC_ROOM_OPTIONS" then
    local room=Multiplayer.selectedPublicRoomData or {}; setOptions({"إظهار المتواجدين","الانضمام إلى الغرفة","رجوع إلى قائمة الغرف"},"الغرفة: "..tostring(room.displayName or room.room or "غير محددة").."، المالك: "..tostring(room.owner or "غير محدد").."، المتواجدون: "..tostring(room.playerCount or 0).." من "..tostring(room.maxPlayers or 4))
  elseif sectionName == "FRIENDS_MENU" then
    setOptions({"قائمة الأصدقاء", "الطلبات المستلمة", "اللاعبون المتصلون", "الإشعارات", "الرسائل الخاصة", "المحظورون", "السجل الاجتماعي", "رجوع للقائمة"}, "قائمة الأصدقاء والدعوات والدردشة")
  elseif sectionName == "NOTIFICATIONS" then
    local options={}; for _,item in ipairs(Multiplayer.notifications or {}) do local preview=tostring(item.text or ""); if #preview>90 then preview=preview:sub(1,90).."..." end; table.insert(options,"إشعار: "..preview) end; if #options==0 then table.insert(options,"لا توجد إشعارات جديدة") end; table.insert(options,"تحديث الإشعارات"); table.insert(options,"رجوع")
    setOptions(options,"الإشعارات")
    if firebaseGetNotifications then firebaseGetNotifications(function(ok,list) local refreshed={}; for _,item in ipairs(list or {}) do local preview=tostring(item.text or ""); if #preview>90 then preview=preview:sub(1,90).."..." end; table.insert(refreshed,"إشعار: "..preview) end; if #refreshed==0 then table.insert(refreshed,"لا توجد إشعارات جديدة") end; table.insert(refreshed,"تحديث الإشعارات"); table.insert(refreshed,"رجوع"); currentMenuOptions=refreshed; currentMenuIndex=1; say(#list>0 and "لديك إشعارات جديدة." or "لا توجد إشعارات جديدة.",true); refreshVisibleScreen() end) end
  elseif sectionName == "PRIVATE_MESSAGES" then
    local options={}; for _,item in ipairs(Multiplayer.privateMessages or {}) do local preview=item.voice and "رسالة صوتية" or tostring(item.text or ""); if #preview>90 then preview=preview:sub(1,90).."..." end; table.insert(options,"رسالة من "..tostring(item.fromName or item.from)..": "..preview) end; if #options==0 then table.insert(options,"لا توجد رسائل خاصة") end; table.insert(options,"إرسال رسالة خاصة"); table.insert(options,"تحديث الرسائل"); table.insert(options,"رجوع")
    setOptions(options,"الرسائل الخاصة")
    if firebaseGetPrivateMessages then firebaseGetPrivateMessages(function(ok,list) local refreshed={}; local desired=tostring(Multiplayer.pendingMessageId or ""); local selected=1; for index,item in ipairs(list or {}) do local preview=item.voice and "رسالة صوتية" or tostring(item.text or ""); if #preview>90 then preview=preview:sub(1,90).."..." end; table.insert(refreshed,"رسالة من "..tostring(item.fromName or item.from)..": "..preview); if desired~="" and tostring(item.id)==desired then selected=index end end; if #refreshed==0 then table.insert(refreshed,"لا توجد رسائل خاصة") end; table.insert(refreshed,"إرسال رسالة خاصة"); table.insert(refreshed,"تحديث الرسائل"); table.insert(refreshed,"رجوع"); currentMenuOptions=refreshed; currentMenuIndex=selected; Multiplayer.pendingMessageId=""; say(#list>0 and "تم فتح الرسالة الخاصة المحددة." or "لا توجد رسائل خاصة.",true); refreshVisibleScreen() end) end
  elseif sectionName == "BLOCKED_USERS" then
    local options={}; for username,_ in pairs(Multiplayer.blockedUsers or {}) do table.insert(options,"فك حظر: "..username) end; if #options==0 then table.insert(options,"لا يوجد لاعبون محظورون") end; table.insert(options,"تحديث المحظورين"); table.insert(options,"رجوع")
    setOptions(options,"المحظورون")
    if firebaseGetBlockedUsers then firebaseGetBlockedUsers(function(ok,list) local refreshed={}; for _,username in ipairs(list or {}) do table.insert(refreshed,"فك حظر: "..username) end; if #refreshed==0 then table.insert(refreshed,"لا يوجد لاعبون محظورون") end; table.insert(refreshed,"تحديث المحظورين"); table.insert(refreshed,"رجوع"); currentMenuOptions=refreshed; currentMenuIndex=1; refreshVisibleScreen() end) end
  elseif sectionName == "FRIENDS_LIST_PLAY" then
    local title=(Multiplayer.socialReturnSection=="WAITING_ROOM") and "دعوة صديق إلى الغرفة" or "اللعب مع صديق. يتم عرض الأصدقاء المتصلين فقط"
    setOptions({"تحديث الأصدقاء المتصلين","لا يوجد أصدقاء متصلون الآن","رجوع"}, title)
    firebaseGetOnlineFriends(function(ok,list) local options={}; for _,friend in ipairs(list or {}) do local p=Multiplayer.remoteProfiles[friend] or {}; table.insert(options,"دعوة "..tostring(p.name or friend).." (@"..friend..")") end; if #options==0 then table.insert(options,"لا يوجد أصدقاء متصلون الآن") end; table.insert(options,"تحديث الأصدقاء المتصلين"); table.insert(options,"رجوع"); currentMenuOptions=options; currentMenuIndex=1; say(#list>0 and "الأصدقاء المتصلون: "..#list or "لا يوجد أصدقاء متصلون الآن.",true); refreshVisibleScreen() end)
  elseif sectionName == "FRIEND_REQUESTS" then
    local options = {}; for _, request in ipairs(Multiplayer.friendRequests or {}) do table.insert(options, "قبول طلب: " .. request); table.insert(options, "رفض طلب: " .. request) end; if #options==0 then table.insert(options,"لا توجد طلبات صداقة") end; table.insert(options, "تحديث الطلبات"); table.insert(options, "رجوع")
    setOptions(options, "طلبات الصداقة المستلمة")
  elseif sectionName == "ONLINE_PLAYERS" then
    setOptions({"تحديث قائمة اللاعبين", "رجوع"}, "اللاعبون المتصلون الآن")
    firebaseGetOnlinePlayers(function(ok,list)
      if ok then
        Multiplayer.serverStatus = "متصل"; local names={}
        for _,username in ipairs(list or {}) do local p=Multiplayer.remoteProfiles[username] or {}; table.insert(names,tostring(p.name or username).." (@"..tostring(username)..")") end
        if #names > 0 then currentMenuOptions=names; table.insert(currentMenuOptions,"تحديث قائمة اللاعبين"); table.insert(currentMenuOptions,"رجوع") else currentMenuOptions={"لا يوجد لاعبون متصلون الآن","تحديث قائمة اللاعبين","رجوع"} end
        say(#names > 0 and "اللاعبون المتصلون الآن: "..tostring(#names) or "لا يوجد لاعبون متصلون الآن.",true)
      else Multiplayer.serverStatus="غير متصل"; say("تعذر تحديث الحضور الحالي.",true) end
      refreshVisibleScreen()
    end)
  elseif sectionName == "PLAYER_PROFILE" then
    local p = Multiplayer.remoteProfiles[Multiplayer.selectedPlayer] or {}
    local profileOptions={"مكالمة الأصدقاء", "إرسال رسالة خاصة", "دعوة إلى الغرفة", "إرسال طلب صداقة"}; if Multiplayer.blockedUsers and Multiplayer.blockedUsers[Multiplayer.selectedPlayer] then table.insert(profileOptions,"فك حظر اللاعب") else table.insert(profileOptions,"حظر اللاعب") end; table.insert(profileOptions,"رجوع"); setOptions(profileOptions, "ملف " .. tostring(p.name or Multiplayer.selectedPlayer) .. ". الحالة: " .. tostring(p.bio or "غير متاحة"))
  elseif sectionName == "LEADERBOARD" then
    setOptions({"أضفني إلى قائمة المتصدرين", "حالة تأهلي", "تحديث قائمة المتصدرين", "رجوع"}, "قائمة المتصدرين. يلزم تجاوز أربعة متسابقين في سباق جماعي مكتمل لطلب الإضافة.")
    firebaseGet("/leaderboard", function(ok, content)
      if ok then
        local entries = {}
        for username, object in string.gmatch(content or "", '"([^"\\]+)"%s*:%s*{([^{}]*)}') do
          local name = object:match('"name"%s*:%s*"(.-)"') or username; local score = tonumber(object:match('"score"%s*:%s*([%-%d%.]+)')) or 0
          table.insert(entries, {label=name .. " (@" .. username .. ") - " .. math.floor(score) .. " نقطة", score=score})
        end
        table.sort(entries, function(a,b) return a.score > b.score end); currentMenuOptions = {}
        for i=1,math.min(#entries,20) do table.insert(currentMenuOptions, tostring(i) .. ". " .. entries[i].label) end
        table.insert(currentMenuOptions, "أضفني إلى قائمة المتصدرين"); table.insert(currentMenuOptions, "حالة تأهلي"); table.insert(currentMenuOptions, "تحديث قائمة المتصدرين"); table.insert(currentMenuOptions, "رجوع"); say(#entries > 0 and "تم تحديث المتصدرين." or "لا توجد نتائج منشورة بعد.", true)
      else say("تعذر الاتصال بالمتصدرين.", true) end; refreshVisibleScreen()
    end)
  elseif sectionName == "TOURNAMENTS" then
    setOptions({"إنشاء بطولة", "بدء البطولة الحالية", "مغادرة البطولة الحالية", "تحديث البطولات", "رجوع للقائمة"}, "البطولات. أنشئ بطولة أو انضم إلى بطولة متاحة.")
    firebaseListTournaments(function(ok,list)
      local options={}; for _,row in ipairs(list or {}) do local label="بطولة: "..tostring(row.name).." | "..tostring(row.duration/60).." دقيقة | "..tostring(row.status); Multiplayer.tournamentDirectory[label]=row; table.insert(options,label) end
      table.insert(options,"إنشاء بطولة"); table.insert(options,"بدء البطولة الحالية"); table.insert(options,"مغادرة البطولة الحالية"); table.insert(options,"تحديث البطولات"); table.insert(options,"رجوع للقائمة"); currentMenuOptions=options; currentMenuIndex=1; say(#list>0 and "اختر بطولة للانضمام إليها." or "لا توجد بطولات متاحة حاليًا.",true); refreshVisibleScreen()
    end)
  elseif sectionName == "WAITING_ROOM" then
    Multiplayer.waitingForPlayers = true; if VoiceSystem and VoiceSystem.startVoicePolling then VoiceSystem.startVoicePolling() end; if firebaseWatchRoomRace then firebaseWatchRoomRace() end; if firebaseWatchRoomChat then firebaseWatchRoomChat() end
    local botCount=#(Multiplayer.bots or {}); if botCount==0 and Multiplayer.botEnabled then botCount=1 end; local botLabel="ضبط عدد الروبوتات ("..tostring(botCount).." من 3)"; local durationLabel="اختيار مدة السباق ("..tostring(math.floor((Multiplayer.selectedDuration or Settings.raceDurationSeconds or 300)/60)).." دقيقة)"; local readyCount=0; for _,isReady in pairs(Multiplayer.readyPlayers or {}) do if isReady then readyCount=readyCount+1 end end; local readyLabel=Multiplayer.playerReady and "إلغاء الجاهزية" or "أنا جاهز"; setOptions({"قائمة إجراءات الغرفة", "الأصدقاء", "الرسائل الخاصة", "دعوة صديق", "قائمة الموجودين", "مراسلة الغرفة", readyLabel, "الجاهزون: "..tostring(readyCount).."/"..tostring(#(Multiplayer.roomPlayers or {})), durationLabel, botLabel, "تحديث حالة الغرفة", "بدء السباق الآن", "إلغاء والرجوع"}, "غرفة الانتظار: " .. (Multiplayer.currentRoom or "غير محددة"))
  elseif sectionName == "ROOM_ACTIONS" then
    local owner = Multiplayer.roomOwner == playerStats.username; local selected=Multiplayer.selectedPlayer and Multiplayer.selectedPlayer~="" and (" اللاعب المحدد: "..Multiplayer.selectedPlayer) or " اختر لاعبًا من قائمة الموجودين أولاً"
    if owner then
      local muteAllLabel=Multiplayer.roomMuteAll and "إلغاء كتم الجميع" or "كتم الجميع"
      local lockLabel=Multiplayer.roomLocked and "فتح الغرفة" or "قفل الغرفة"
      setOptions({"الأصدقاء", "الرسائل الخاصة", "دعوة صديق", "قائمة الموجودين", muteAllLabel, "كتم اللاعب المحدد", "إلغاء كتم اللاعب المحدد", "طرد اللاعب المحدد", "حظر اللاعب المحدد", "إلغاء حظر اللاعب المحدد", "إرسال رسالة للغرفة", "تغيير اسم الغرفة", lockLabel, "تغيير حد اللاعبين", "تعيين مساعد للمالك", "تشغيل موسيقى الغرفة", "إيقاف موسيقى الغرفة", "سجل إجراءات الغرفة", "رجوع"}, "إدارة الغرفة. أنت المالك."..selected)
    else setOptions({"الأصدقاء", "الرسائل الخاصة", "دعوة صديق", "قائمة الموجودين", "إرسال رسالة للغرفة", "طلب إيقاف ميكروفوني", "رجوع"}, "إجراءات الغرفة. المالك: " .. tostring(Multiplayer.roomOwner)) end
  elseif sectionName == "ROOM_PLAYERS" then
    local options = {}; for _, user in ipairs(Multiplayer.roomPlayers or {}) do table.insert(options, user) end; table.insert(options, "تحديث القائمة"); table.insert(options, "رجوع")
    setOptions(options, "الموجودون في الغرفة")
    firebaseRefreshRoom(function() refreshVisibleScreen() end)
  elseif sectionName == "ROOM_CHAT" then
    playDynamicSFX("chat_open",0,1,0,0.70); if SmartAnnouncer and SmartAnnouncer.commentary then SmartAnnouncer.commentary("chat",playerStats.username,"",true) end
    setOptions({"كتابة رسالة", "قراءة آخر رسالة", "تسجيل رسالة صوتية", "تشغيل آخر رسالة صوتية", "تحديث الرسائل", "رجوع"}, "مراسلة الغرفة")
    firebaseGet("/rooms/" .. firebaseEncodeKey(Multiplayer.currentRoom) .. "/chat", function(ok, content)
      if ok then
        Multiplayer.chatMessages = {}; for _, text in string.gmatch(content or "", '"text"%s*:%s*"(.-)"') do table.insert(Multiplayer.chatMessages, text) end
        say(#Multiplayer.chatMessages > 0 and Multiplayer.chatMessages[#Multiplayer.chatMessages] or "لا توجد رسائل بعد.", true); refreshVisibleScreen()
      end
    end)
  elseif sectionName == "SETTINGS" then
    setOptions({"الإعدادات العامة", "إعدادات الصوت", "إعدادات الوصول", "إعدادات السباق", "إعدادات الحساب والتحديث", "رجوع للقائمة"}, "الإعدادات")
  elseif sectionName == "SETTINGS_GENERAL" then
    setOptions({"تشغيل وإيقاف الاهتزاز", "تفعيل وضع حفظ الطاقة", "فحص التحديثات", "فتح التواصل عبر واتساب", "رجوع للإعدادات"}, "الإعدادات العامة")
  elseif sectionName == "SETTINGS_AUDIO" then
    setOptions({"تشغيل وإيقاف النطق", "سرعة الناطق بالسحب بلا حد", "حدة الناطق بالسحب", "تشغيل وإيقاف المؤثرات", "مستوى المؤثرات بالسحب", "تشغيل وإيقاف الموسيقى", "مستوى الموسيقى بالسحب", "المقطع الموسيقي التالي", "المقطع الموسيقي السابق", "تشغيل وإيقاف المذيع", "تشغيل وإيقاف التعليق الذكي", "مستوى المذيع بالسحب", "رجوع للإعدادات"}, "إعدادات الصوت. اسحب لأعلى للرفع ولأسفل للخفض عند الوقوف على مستوى.")
  elseif sectionName == "SETTINGS_ACCESSIBILITY" then
    setOptions({"اختيار اللغة", "اختيار وضع التفاعل", "اختيار لوحة المفاتيح", "اختيار اتجاه الشاشة", "رجوع للإعدادات"}, "إعدادات الوصول")
  elseif sectionName == "SETTINGS_RACE" then
    setOptions({"اختيار نمط التحكم", "اختيار طريقة التوجيه", "معايرة الإمالة", "حساسية الإمالة", "عكس اتجاه الإمالة", "تغيير مهلة تنبيه الاصطدام", "تغيير جودة الرادار", "تشغيل وإيقاف أزرار السباق", "تشغيل وإيقاف الواقعية المتقدمة", "تشغيل وإيقاف صوت قرب المنافس", "تغيير فترة تقرير السباق", "رجوع للإعدادات"}, "إعدادات السباق. التوجيه الحالي: "..tostring(Settings.steeringMode or "السحب"))
  elseif sectionName == "SETTINGS_ACCOUNT" then
    setOptions({"اختبار الاتصال", "مساحة الرسائل الصوتية", "فحص تحديثات التطبيق", "تسجيل الخروج", "مسح بيانات اللعبة وإعادة الضبط", "رجوع للإعدادات"}, "إعدادات الحساب والتحديث")
  elseif sectionName == "STORE" then setOptions({"معرض السيارات", "متجر الأدوات", "رجوع للقائمة"}, "المتجر")
  elseif sectionName == "CAR_DEALER" then local options={}; for _,car in ipairs(Store) do table.insert(options, car.name .. " بـ " .. car.price .. " عملة") end; table.insert(options,"رجوع للمتجر"); setOptions(options,"معرض السيارات")
  elseif sectionName == "STORE_ITEMS" then local options={}; for _,item in ipairs(StoreItems) do table.insert(options,item.name .. " بـ " .. item.price .. " عملة") end; table.insert(options,"رجوع للمتجر"); setOptions(options,"متجر الأدوات والموارد")
  elseif sectionName == "INVENTORY" then local options={}; for _,id in ipairs(Inventory) do local car=Store[id]; if car then table.insert(options, car.name .. (id==activeCarId and " (مجهزة)" or "")) end end; table.insert(options,"فتح صندوق محفوظ ("..tostring(playerStats.rewardChests or 0)..")"); table.insert(options,"رجوع للقائمة"); setOptions(options,"حقيبة السيارات والصناديق")
  elseif sectionName == "GAS_STATION" then setOptions({"شراء خزان وقود كامل (500)", "رجوع للقائمة"}, "محطة البنزين. تملك " .. playerStats.fuelTanks .. " خزانات")
  elseif sectionName == "GARAGE" then
    local options={}; local damage=playerStats.carDamage or {}; if (damage.engine or 0)>0 then table.insert(options,"إصلاح المحرك ("..math.max(100,damage.engine*5).." عملة)") end; if (damage.tires or 0)>0 then table.insert(options,"إصلاح الإطارات ("..math.max(100,damage.tires*4).." عملة)") end; if (damage.body or 0)>0 then table.insert(options,"إصلاح الهيكل ("..math.max(100,damage.body*3).." عملة)") end; if (damage.brakes or 0)>0 then table.insert(options,"إصلاح الفرامل ("..math.max(100,damage.brakes*4).." عملة)") end; if #options==0 then table.insert(options,"لا توجد أعطال حالية") end; table.insert(options,"رجوع للقائمة"); setOptions(options,"ورشة الصيانة. صحة السيارة " .. playerCarHp)
  elseif sectionName == "ABOUT" then setOptions({"عن World Racing", "التواصل عبر واتساب", "قناة التحديثات", "مجموعة المجتمع", "شروط الاستخدام والخصوصية", "رجوع للقائمة"}, "World Racing. سباق صوتي ومرئي وجماعي")
  elseif sectionName == "WIN" then setOptions({"موافق", "ورشة الصيانة", "محطة البنزين", "قائمة المتصدرين", "رجوع للقائمة"}, "لقد فزت")
  elseif sectionName == "LOSE" then setOptions({"موافق", "ورشة الصيانة", "محطة البنزين", "نصائح التحسين", "رجوع للقائمة"}, "انتهى السباق")
  elseif sectionName == "PLAYING_MULTIPLAYER" or sectionName == "PLAYING" then
    if sectionName == "PLAYING_MULTIPLAYER" and VoiceSystem and VoiceSystem.startVoicePolling then VoiceSystem.startVoicePolling() end
    if playerCarHp <= 0 then say("سيارتك محطمة.", true); loadSection("MENU"); return end
    local levelData = LevelsData[math.min(playerStats.level, #LevelsData)]; if sectionName=="PLAYING_MULTIPLAYER" and tonumber(Multiplayer.raceSeed or 0)>0 then math.randomseed(tonumber(Multiplayer.raceSeed)) end; local surfaceProfiles={{weather="صافي",road="جاف",grip=1.0},{weather="غائم",road="جاف",grip=0.98},{weather="مطر خفيف",road="مبلل",grip=0.88},{weather="رذاذ",road="زلق",grip=0.82}}; local surface=surfaceProfiles[math.random(1,#surfaceProfiles)]
    GameState.speed,GameState.distance,GameState.lane=0,0,0; GameState.isCrashed=false; GameState.isEngineOn=false; GameState.fuelWarned=false; GameState.tempWarned=false; GameState.collisionWarned={}; GameState.collisionWarningAt={}; GameState.engineTemp=50; GameState.fuel=100; GameState.obstacles={}; GameState.actionDrawerOpen=false; GameState.actionMenuIndex=1; GameState.opponentDistance=0; GameState.opponentSpeed=0; GameState.opponentLane=0; GameState.opponentIncident=""; GameState.lastObstacleNotice=""; GameState.timeElapsed=0; GameState.pendingCrate=nil; GameState.crateExpiresAt=0; GameState.nextCrateAt=math.random(8,18); GameState.nextObstacleAt=math.random(4,8); GameState.collectedCrates=0; GameState.crateNoticeAt=0; GameState.lastRaceWasMultiplayer=sectionName=="PLAYING_MULTIPLAYER"; GameState.turnDirective=""; GameState.turnTargetLane=nil; GameState.nextTurnAt=math.random(55,95); GameState.raceDurationSeconds=math.max(60,math.min(720,tonumber(sectionName=="PLAYING_MULTIPLAYER" and Multiplayer.selectedDuration or Settings.raceDurationSeconds or 300) or 300)); GameState.timeRemaining=GameState.raceDurationSeconds; GameState.collisionCount=0; GameState.maxCollisions=7; GameState.overtakeCount=0; GameState.raceOutcomeRecorded=false; GameState.raceOutcome=""; GameState.lastRaceSummary=""; GameState.lastRaceScore=0; GameState.lastRaceReward=0; GameState.isPaused=false; GameState.weather=surface.weather; GameState.roadCondition=surface.road; GameState.surfaceGrip=surface.grip; GameState.traction=surface.grip; GameState.currentGear=1; GameState.lastLane=0; GameState.lastLaneChangeAt=0; GameState.nextStatusReportAt=Settings.periodicRaceReportSeconds or 15; GameState.nextProximityAt=0; GameState.opponentCollisionCount=0; GameState.opponentIncident=""; GameState.raceSessionId=(GameState.raceSessionId or 0)+1; Multiplayer.roomPaused=false; GameState.botRacers={}; local configuredBots=Multiplayer.bots or {}; if #configuredBots==0 and Multiplayer.botEnabled then configuredBots={{id="bot_1",name=Multiplayer.botName~="" and Multiplayer.botName or "روبوت الطريق",skill=Multiplayer.botSkill or 0.55}} end; for i,bot in ipairs(configuredBots) do table.insert(GameState.botRacers,{id=bot.id or ("bot_"..i),name=bot.name or ("روبوت الطريق "..i),skill=tonumber(bot.skill) or 0.55,distance=0,speed=0,lane=0,nextDecisionAt=0,nextIncidentAt=math.random(10,22),collisionCount=0,traction=surface.grip}) end; if sectionName=="PLAYING" and #GameState.botRacers==0 then table.insert(GameState.botRacers,{id="computer_rival",name="المنافس الآلي",skill=0.60,distance=0,speed=0,lane=0,nextDecisionAt=0,nextIncidentAt=math.random(10,22),collisionCount=0,traction=surface.grip}) end; if achievementsRecordRaceStart then achievementsRecordRaceStart(sectionName,GameState.raceDurationSeconds) end; GameState.announced30=false; GameState.leadAnnounced=false; GameState.behindAnnounced=false; GameState.lastAnnouncerAt=0
    local activeCar=Store[1]; for _,car in ipairs(Store) do if car.id==activeCarId then activeCar=car; break end end; GameState.maxSpeed=activeCar.speed; GameState.activeCarGrip=tonumber(activeCar.grip) or 0.7; GameState.opponentName=(GameState.botRacers and #GameState.botRacers>0 and GameState.botRacers[1].name) or (sectionName=="PLAYING_MULTIPLAYER" and "المنافس" or "الكمبيوتر")
    setRaceMusicLevel(0.03); say((sectionName=="PLAYING_MULTIPLAYER" and "استعد للسباق الجماعي. " or "استعد. ")..levelData.name..". الطقس: "..GameState.weather..". سطح الطريق: "..GameState.roadCondition, true); refreshVisibleScreen(); if (Settings.steeringMode=="الإمالة" or Settings.steeringMode=="مختلط") and startTiltControl then startTiltControl() end; startRaceCountdown(sectionName=="PLAYING_MULTIPLAYER" and Multiplayer.raceStartAt or 0,sectionName=="PLAYING_MULTIPLAYER" and Multiplayer.raceStartAtMs or 0,sectionName=="PLAYING_MULTIPLAYER" and Multiplayer.raceSessionId or "")
  end
end
