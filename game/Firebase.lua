-- Firebase.lua
-- حسابات التطبيق + مزامنة Realtime Database.
-- هذا الوضع يحفظ سجل الحساب كبيانات Firebase، ولا يحفظ كلمة المرور الخام أبدًا.
FIREBASE_BASE_URL = "https://asdf-e3a96-default-rtdb.firebaseio.com"
FIREBASE_WEB_API_KEY = ""
FIREBASE_TIMEOUT_MS = 12000
FIREBASE_ENABLED = true
FIREBASE_REQUIRE_AUTH = false
FIREBASE_ACCOUNT_MODE = "account_record"

local function jsonEscape(value)
  value = tostring(value or "")
  return '"' .. value:gsub("\\", "\\\\"):gsub('"', '\\"'):gsub("\r", "\\r"):gsub("\n", "\\n") .. '"'
end

local function isArray(t)
  if type(t) ~= "table" then return false end
  local n = 0
  for k in pairs(t) do if type(k) ~= "number" then return false end; n = math.max(n, k) end
  for i = 1, n do if t[i] == nil then return false end end
  return true
end

local function jsonEncode(value)
  if value == nil then return "null" end
  if type(value) == "number" then return tostring(value) end
  if type(value) == "boolean" then return value and "true" or "false" end
  if type(value) == "string" then return jsonEscape(value) end
  if type(value) ~= "table" then return jsonEscape(value) end
  local parts = {}
  if isArray(value) then
    for i = 1, #value do table.insert(parts, jsonEncode(value[i])) end
    return "[" .. table.concat(parts, ",") .. "]"
  end
  for key, item in pairs(value) do table.insert(parts, jsonEscape(key) .. ":" .. jsonEncode(item)) end
  return "{" .. table.concat(parts, ",") .. "}"
end

function firebaseJsonObject(values) return jsonEncode(values or {}) end

local function firebaseUrl(path)
  path = path or ""
  if path:sub(1, 1) ~= "/" then path = "/" .. path end
  return FIREBASE_BASE_URL .. path .. ".json"
end

function firebaseEncodeKey(value)
  local ok, encoded = pcall(function() return URLEncoder.encode(tostring(value or ""), "UTF-8") end)
  return ok and encoded or tostring(value or "")
end

local function sha256(value)
  local ok, result = pcall(function()
    local digest = MessageDigest.getInstance("SHA-256")
    local bytes = digest.digest(String(tostring(value or "")).getBytes("UTF-8"))
    local out = {}
    for i = 0, bytes.length - 1 do local b = bytes[i]; if b < 0 then b = b + 256 end; table.insert(out, string.format("%02x", b)) end
    return table.concat(out)
  end)
  if ok and result then return result end
  local fallback = tostring(value or ""):gsub("[^%w]", "_")
  return fallback .. "_" .. tostring(#fallback)
end

local function newJsonObject(body)
  local ok, obj = pcall(function() return JSONObject(tostring(body or "{}")) end)
  return ok and obj or nil
end

local function optString(obj, key, fallback)
  if not obj then return fallback or "" end
  local ok, value = pcall(function() return obj.optString(key, fallback or "") end)
  return ok and tostring(value or fallback or "") or (fallback or "")
end

local function optInt(obj, key, fallback)
  if not obj then return fallback or 0 end
  local ok, value = pcall(function() return obj.optInt(key, fallback or 0) end)
  return ok and tonumber(value) or (fallback or 0)
end

local function optBool(obj, key, fallback)
  if not obj then return fallback == true end
  local ok, value = pcall(function() return obj.optBoolean(key, fallback == true) end)
  return ok and value == true or fallback == true
end

local function optObj(obj, key)
  if not obj then return nil end
  local ok, value = pcall(function() return obj.optJSONObject(key) end)
  return ok and value or nil
end

local function optArray(obj, key)
  if not obj then return nil end
  local ok, value = pcall(function() return obj.optJSONArray(key) end)
  return ok and value or nil
end

local function jsonList(obj)
  local list = {}
  if not obj then return list end
  local okKeys, keys = pcall(function() return obj.keys() end)
  if okKeys and keys then
    while keys.hasNext() do table.insert(list, tostring(keys.next())) end
    return list
  end
  local okLength, length = pcall(function() return obj.length() end)
  if okLength and length and length > 0 then
    for i = 0, length - 1 do local ok, value = pcall(function() return obj.optString(i, "") end); if ok and value and value ~= "" then table.insert(list, tostring(value)) end end
  end
  return list
end

local function httpRequest(method, urlString, body, useAuth, callback, contentType)
  local work = function()
    local ok, response, code = pcall(function()
      local connection = URL(urlString).openConnection()
      connection.setConnectTimeout(FIREBASE_TIMEOUT_MS); connection.setReadTimeout(FIREBASE_TIMEOUT_MS); connection.setRequestMethod(method); connection.setUseCaches(false)
      connection.setRequestProperty("Accept", "application/json")
      if useAuth and playerStats.idToken and playerStats.idToken ~= "" then connection.setRequestProperty("Authorization", "Bearer " .. playerStats.idToken) end
      if body ~= nil and (method == "PUT" or method == "PATCH" or method == "POST") then
        connection.setDoOutput(true); connection.setRequestProperty("Content-Type", contentType or "application/json; charset=UTF-8")
        local output = connection.getOutputStream(); output.write(String(tostring(body)).getBytes("UTF-8")); output.flush(); output.close()
      end
      local status = connection.getResponseCode(); local stream = (status >= 200 and status < 400) and connection.getInputStream() or connection.getErrorStream(); local result = ""
      if stream then local reader = BufferedReader(InputStreamReader(stream, "UTF-8")); local line = reader.readLine(); while line do result = result .. line; line = reader.readLine() end; reader.close() end
      connection.disconnect(); return result, status
    end)
    local success = ok and code and code >= 200 and code < 300; local message = ok and (response or "") or tostring(response)
    if callback then Handler(Looper.getMainLooper()).post(Runnable({run=function() callback(success, message, code or 0) end})) end
  end
  local started = pcall(function() Thread(Runnable({run=work})).start() end)
  if not started and callback then callback(false, "تعذر تشغيل اتصال الشبكة.", 0) end
end

local function officialAuthEnabled() return FIREBASE_WEB_API_KEY and tostring(FIREBASE_WEB_API_KEY) ~= "" end
local function formEncode(value) local ok,result=pcall(function() return URLEncoder.encode(tostring(value or ""),"UTF-8") end); return ok and result or tostring(value or "") end
local function authErrorText(body)
  local root=newJsonObject(body); local errorObj=root and optObj(root,"error"); local message=errorObj and optString(errorObj,"message","") or ""
  if message=="EMAIL_EXISTS" then return "هذا البريد لديه حساب بالفعل." end
  if message=="EMAIL_NOT_FOUND" or message=="INVALID_PASSWORD" then return "البريد أو كلمة المرور غير صحيحة." end
  if message=="OPERATION_NOT_ALLOWED" then return "تسجيل الدخول بهذا الأسلوب غير مفعّل في إعدادات الحساب." end
  return message~="" and message or "تعذر إتمام المصادقة الرسمية."
end
local function applyOfficialAuth(body,provider,fallbackEmail)
  local root=newJsonObject(body); if not root then return false end
  local idToken=optString(root,"idToken",""); local refreshToken=optString(root,"refreshToken",""); local localId=optString(root,"localId",""); local email=optString(root,"email",fallbackEmail or "")
  if idToken=="" or localId=="" then return false end
  playerStats.idToken=idToken; playerStats.refreshToken=refreshToken; playerStats.localId=localId; playerStats.accountEmail=string.lower(email); playerStats.authProvider=provider or playerStats.authProvider; playerStats.sessionActive=true; return true
end
local function firebaseAuthRequest(endpoint,payload,callback)
  if not officialAuthEnabled() then if callback then callback(false,"لم يتم إعداد المصادقة الرسمية.",0) end; return end
  local url="https://identitytoolkit.googleapis.com/v1/"..endpoint.."?key="..tostring(FIREBASE_WEB_API_KEY)
  httpRequest("POST",url,firebaseJsonObject(payload),false,callback)
end
local function refreshOfficialToken(callback)
  if not officialAuthEnabled() or not playerStats.refreshToken or playerStats.refreshToken=="" then callback(false); return end
  local url="https://securetoken.googleapis.com/v1/token?key="..tostring(FIREBASE_WEB_API_KEY); local body="grant_type=refresh_token&refresh_token="..formEncode(playerStats.refreshToken)
  httpRequest("POST",url,body,false,function(ok,response)
    if not ok then callback(false); return end
    local root=newJsonObject(response); local idToken=optString(root,"id_token",""); local refreshToken=optString(root,"refresh_token","");
    if idToken=="" then callback(false); return end
    playerStats.idToken=idToken; if refreshToken~="" then playerStats.refreshToken=refreshToken end; callback(true)
  end,"application/x-www-form-urlencoded; charset=UTF-8")
end

function firebaseRequest(method, path, body, callback)
  if not FIREBASE_ENABLED then if callback then callback(false, "خدمة الحساب غير متاحة حاليًا.", 0) end; return end
  if path ~= "/" and officialAuthEnabled() and not playerStats.idToken then if callback then callback(false, "يلزم تسجيل الدخول السحابي أولاً.", 401) end; return end
  if FIREBASE_REQUIRE_AUTH and path ~= "/" and not playerStats.idToken and not playerStats.sessionActive then if callback then callback(false, "يلزم تسجيل الدخول السحابي أولاً.", 401) end; return end
  httpRequest(method, firebaseUrl(path), body, true, callback)
end
function firebaseGet(path, callback) firebaseRequest("GET", path, nil, callback) end
function firebasePost(path, body, callback) firebaseRequest("POST", path, body, callback) end
function firebasePut(path, body, callback) firebaseRequest("PUT", path, body, callback) end
function firebasePatch(path, body, callback) firebaseRequest("PATCH", path, body, callback) end
function firebaseDelete(path, callback) firebaseRequest("DELETE", path, nil, callback) end

local function accountKey(email) return "email_" .. sha256(string.lower(tostring(email or ""))) end
local function accountPath(key) return "/accounts/" .. tostring(key or "") end
local function makeSalt() return sha256(tostring(os.time()) .. tostring(math.random()) .. tostring(playerStats.accountEmail or "")):sub(1, 32) end
local function passwordDigest(password, salt) return sha256(tostring(salt or "") .. ":" .. tostring(password or "")) end

local function resetLocalAccountData()
  playerStats.name=""; playerStats.username=""; playerStats.bio="متسابق محترف"; playerStats.status="متسابق محترف"; playerStats.gender=""; playerStats.money=15000; playerStats.level=1; playerStats.reputation=0; playerStats.wins=0; playerStats.bestScore=0; playerStats.bestDistance=0; playerStats.leaderboardOvertakes=0; playerStats.leaderboardQualified=false; playerStats.leaderboardFinalRewarded=false; playerStats.shields=1; playerStats.emps=1; playerStats.drones=1; playerStats.fuelTanks=3; playerStats.nitro=2; playerStats.repairKits=1; playerStats.dailyClaimed=false; playerStats.skills={armorBonus=0,nitroBonus=0,moneyMultiplier=1}; Inventory={1}; activeCarId=1; playerCarHp=100; Multiplayer.friends={}; Multiplayer.friendRequests={}; Multiplayer.sentRequests={}; Multiplayer.remoteProfiles={}; Multiplayer.currentRoom=""; Multiplayer.currentRoom=""; saveGameData()
end
local function nonNull(body) return body and body ~= "" and body ~= "null" end
local function errorText(body)
  local root=newJsonObject(body); if not root then return "تعذر إتمام الطلب السحابي." end
  local nested=optObj(root,"error"); local message=optString(root,"message",""); if nested then message=optString(nested,"message",message) end
  if message=="PERMISSION_DENIED" or message=="Permission denied" then return "لا تسمح قاعدة البيانات بهذه العملية." end
  if message=="write" or message=="read" then return "تعذر الوصول إلى بيانات الحساب." end
  return message~="" and message or "تعذر إتمام الطلب السحابي."
end

local function listAsMap(list)
  local result = {}; for _, value in ipairs(list or {}) do result[tostring(value)] = true end; return result
end

local function makeAccountRecord()
  local record={
    accountKey = playerStats.localId, email = playerStats.accountEmail, provider = playerStats.authProvider,     profile = {name=playerStats.name, username=playerStats.username, bio=playerStats.bio, status=playerStats.status, gender=playerStats.gender},
    progress = {money=playerStats.money, level=playerStats.level, reputation=playerStats.reputation, wins=playerStats.wins, bestScore=playerStats.bestScore, bestDistance=playerStats.bestDistance, bestSpeed=playerStats.bestSpeed, leaderboardOvertakes=playerStats.leaderboardOvertakes or 0, leaderboardQualified=playerStats.leaderboardQualified==true, leaderboardFinalRewarded=playerStats.leaderboardFinalRewarded==true, shields=playerStats.shields, emps=playerStats.emps, drones=playerStats.drones, fuelTanks=playerStats.fuelTanks, nitro=playerStats.nitro, repairKits=playerStats.repairKits, rewardChests=playerStats.rewardChests or 0, voiceStorageBytes=playerStats.voiceStorageBytes or 0, voiceStorageLimitBytes=playerStats.voiceStorageLimitBytes or 20971520, activeCarId=activeCarId, inventory=Inventory, dailyClaimed=playerStats.dailyClaimed, carHp=playerCarHp, carDamage=playerStats.carDamage, skills=playerStats.skills},
    achievements = Achievements or {},
    social = {friends=listAsMap(Multiplayer.friends), friendRequests=listAsMap(Multiplayer.friendRequests), sentRequests=listAsMap(Multiplayer.sentRequests)},
    settings = {ttsEnabled=Settings.ttsEnabled, voiceSpeed=Settings.voiceSpeed, voicePitch=Settings.voicePitch, sfxEnabled=Settings.sfxEnabled, sfxVolume=Settings.sfxVolume, bgmEnabled=Settings.bgmEnabled, bgmVolume=Settings.bgmVolume, announcerEnabled=Settings.announcerEnabled, announcerVolume=Settings.announcerVolume, smartAnnouncerEnabled=Settings.smartAnnouncerEnabled, raceDurationSeconds=Settings.raceDurationSeconds, vibrationEnabled=Settings.vibrationEnabled, accessibilityMode=Settings.accessibilityMode, keyboardMode=Settings.keyboardMode, controlMode=Settings.controlMode, orientationMode=Settings.orientationMode, steeringMode=Settings.steeringMode, tiltSensitivity=Settings.tiltSensitivity, tiltDeadZone=Settings.tiltDeadZone, tiltCenter=Settings.tiltCenter, invertTilt=Settings.invertTilt, warningSeconds=Settings.warningSeconds, radarQuality=Settings.radarQuality, powerSaveMode=Settings.powerSaveMode, doubleTapMenus=Settings.doubleTapMenus, showActionButtons=Settings.showActionButtons},
    updatedAt = os.time()
  }
  if not officialAuthEnabled() then record.passwordSalt=playerStats.passwordSalt; record.passwordHash=playerStats.passwordHash end
  return record
end

function firebaseRestoreAccount(body, providerOverride)
  local root = newJsonObject(body); if not root then return false end
  local profile, progress, social, settings = optObj(root, "profile") or root, optObj(root, "progress") or root, optObj(root, "social"), optObj(root, "settings")
  playerStats.localId = optString(root, "accountKey", playerStats.localId); playerStats.accountEmail = optString(root, "email", playerStats.accountEmail); playerStats.authProvider = providerOverride or optString(root, "provider", playerStats.authProvider)
  playerStats.name = optString(profile, "name", playerStats.name); playerStats.username = optString(profile, "username", playerStats.username); playerStats.bio = optString(profile, "bio", playerStats.bio); playerStats.status = optString(profile, "status", playerStats.bio); playerStats.gender = optString(profile, "gender", playerStats.gender)
  playerStats.money = optInt(progress, "money", playerStats.money); playerStats.level = optInt(progress, "level", playerStats.level); playerStats.reputation = optInt(progress, "reputation", playerStats.reputation); playerStats.wins = optInt(progress, "wins", playerStats.wins); playerStats.bestScore = optInt(progress, "bestScore", playerStats.bestScore); playerStats.bestDistance = optInt(progress, "bestDistance", playerStats.bestDistance); playerStats.bestSpeed = optInt(progress, "bestSpeed", playerStats.bestSpeed); playerStats.leaderboardOvertakes=optInt(progress,"leaderboardOvertakes",playerStats.leaderboardOvertakes or 0); playerStats.leaderboardQualified=optBool(progress,"leaderboardQualified",playerStats.leaderboardQualified); playerStats.leaderboardFinalRewarded=optBool(progress,"leaderboardFinalRewarded",playerStats.leaderboardFinalRewarded); playerStats.shields = optInt(progress, "shields", playerStats.shields); playerStats.emps = optInt(progress, "emps", playerStats.emps); playerStats.drones = optInt(progress, "drones", playerStats.drones); playerStats.fuelTanks = optInt(progress, "fuelTanks", playerStats.fuelTanks); playerStats.nitro = optInt(progress, "nitro", playerStats.nitro); playerStats.repairKits = optInt(progress, "repairKits", playerStats.repairKits); playerStats.rewardChests = optInt(progress, "rewardChests", playerStats.rewardChests or 0); playerStats.voiceStorageBytes=optInt(progress,"voiceStorageBytes",playerStats.voiceStorageBytes or 0); playerStats.voiceStorageLimitBytes=optInt(progress,"voiceStorageLimitBytes",playerStats.voiceStorageLimitBytes or 20971520); activeCarId = optInt(progress, "activeCarId", activeCarId); playerCarHp = optInt(progress, "carHp", playerCarHp); playerStats.dailyClaimed = optBool(progress, "dailyClaimed", playerStats.dailyClaimed)
  local damage=optObj(progress,"carDamage"); if damage then playerStats.carDamage={engine=optInt(damage,"engine",0),tires=optInt(damage,"tires",0),body=optInt(damage,"body",0),brakes=optInt(damage,"brakes",0)} end
  local skills=optObj(progress,"skills"); if skills then playerStats.skills.armorBonus=optInt(skills,"armorBonus",playerStats.skills.armorBonus); playerStats.skills.nitroBonus=optInt(skills,"nitroBonus",playerStats.skills.nitroBonus); local okMultiplier,multiplier=pcall(function() return skills.optDouble("moneyMultiplier",playerStats.skills.moneyMultiplier) end); if okMultiplier then playerStats.skills.moneyMultiplier=tonumber(multiplier) or playerStats.skills.moneyMultiplier end end
  local achObj=optObj(root,"achievements"); if achObj then Achievements={}; local keys=achObj.keys(); while keys and keys.hasNext() do local id=tostring(keys.next()); local item=achObj.optJSONObject(id); if item then Achievements[id]={progress=tonumber(item.optDouble("progress",0)) or 0,completed=item.optBoolean("completed",false),completedAt=tonumber(item.optDouble("completedAt",0)) or 0} end end end
  local inventory = optArray(progress, "inventory"); if inventory then Inventory = {}; for _, id in ipairs(jsonList(inventory)) do local number = tonumber(id); if number then table.insert(Inventory, number) end end end; if #Inventory == 0 then Inventory={1} end
  if settings then Settings.ttsEnabled=optBool(settings,"ttsEnabled",Settings.ttsEnabled); Settings.voiceSpeed=tonumber(settings.optDouble("voiceSpeed",Settings.voiceSpeed)); Settings.voicePitch=tonumber(settings.optDouble("voicePitch",Settings.voicePitch)); Settings.sfxEnabled=optBool(settings,"sfxEnabled",Settings.sfxEnabled); Settings.sfxVolume=tonumber(settings.optDouble("sfxVolume",Settings.sfxVolume)); Settings.bgmEnabled=optBool(settings,"bgmEnabled",Settings.bgmEnabled); Settings.bgmVolume=tonumber(settings.optDouble("bgmVolume",Settings.bgmVolume)); Settings.announcerEnabled=optBool(settings,"announcerEnabled",Settings.announcerEnabled); Settings.announcerVolume=tonumber(settings.optDouble("announcerVolume",Settings.announcerVolume)); Settings.vibrationEnabled=optBool(settings,"vibrationEnabled",Settings.vibrationEnabled); Settings.accessibilityMode=optString(settings,"accessibilityMode",Settings.accessibilityMode); Settings.keyboardMode=optString(settings,"keyboardMode",Settings.keyboardMode); Settings.controlMode=optString(settings,"controlMode",Settings.controlMode); Settings.orientationMode=optString(settings,"orientationMode",Settings.orientationMode); Settings.steeringMode=optString(settings,"steeringMode",Settings.steeringMode or "السحب"); Settings.tiltSensitivity=tonumber(settings.optDouble("tiltSensitivity",Settings.tiltSensitivity or 2.4)); Settings.tiltDeadZone=tonumber(settings.optDouble("tiltDeadZone",Settings.tiltDeadZone or 0.65)); Settings.tiltCenter=tonumber(settings.optDouble("tiltCenter",Settings.tiltCenter or 0)); Settings.invertTilt=optBool(settings,"invertTilt",Settings.invertTilt); Settings.warningSeconds=optInt(settings,"warningSeconds",Settings.warningSeconds); Settings.radarQuality=optString(settings,"radarQuality",Settings.radarQuality); Settings.powerSaveMode=optBool(settings,"powerSaveMode",Settings.powerSaveMode); Settings.doubleTapMenus=optBool(settings,"doubleTapMenus",Settings.doubleTapMenus); Settings.showActionButtons=optBool(settings,"showActionButtons",Settings.showActionButtons); Settings.smartAnnouncerEnabled=optBool(settings,"smartAnnouncerEnabled",Settings.smartAnnouncerEnabled); Settings.raceDurationSeconds=math.max(60,math.min(300,optInt(settings,"raceDurationSeconds",Settings.raceDurationSeconds or 120))) end
  if social then Multiplayer.friends=jsonList(optObj(social,"friends")); Multiplayer.friendRequests=jsonList(optObj(social,"friendRequests")); Multiplayer.sentRequests=jsonList(optObj(social,"sentRequests")) end
  playerStats.sessionActive=true; saveGameData(); return true
end

function firebaseSyncProfile(callback)
  if not playerStats.sessionActive or not playerStats.accountEmail then if callback then callback(false,"لا توجد جلسة حساب.") end; return end
  if not playerStats.localId or playerStats.localId=="" then playerStats.localId=accountKey(playerStats.accountEmail) end
  local payload=firebaseJsonObject(makeAccountRecord())
  firebasePut(accountPath(playerStats.localId),payload,function(ok,body,code)
    if not ok then if callback then callback(false,body,code) end; return end
    local username=playerStats.username or ""; if username=="" then if callback then callback(true,body,code) end; return end
    local public=firebaseJsonObject({name=playerStats.name,username=username,bio=playerStats.bio,status=playerStats.status,gender=playerStats.gender,wins=playerStats.wins,bestScore=playerStats.bestScore,bestDistance=playerStats.bestDistance,leaderboardOvertakes=playerStats.leaderboardOvertakes or 0,leaderboardQualified=playerStats.leaderboardQualified==true,updatedAt=os.time()})
    firebasePut("/players/"..firebaseEncodeKey(username),public,function(ok2,body2,code2) if ok2 then firebaseSyncAdminPlayerSnapshot(function() end) end; if callback then callback(ok2,body2,code2) end end)
  end)
end
function firebaseVerifyCurrentAccount(callback)
  if not FIREBASE_ENABLED or not playerStats.sessionActive or not playerStats.accountEmail or playerStats.accountEmail=="" then if callback then callback(false,"لا توجد جلسة حساب سحابية.") end; return end
  local key=playerStats.localId and playerStats.localId~="" and playerStats.localId or accountKey(playerStats.accountEmail)
  firebaseGet(accountPath(key),function(ok,body,code)
    if not ok or not nonNull(body) then if callback then callback(false,"تعذر التحقق من سجل الحساب على الخادم.",code) end; return end
    local root=newJsonObject(body); local storedEmail=root and string.lower(optString(root,"email","")) or ""
    if storedEmail~="" and storedEmail~=string.lower(tostring(playerStats.accountEmail or "")) then if callback then callback(false,"سجل الحساب لا يطابق البريد الحالي.",code) end; return end
    playerStats.localId=key; if callback then callback(true,"تم التحقق من الحساب.",code) end
  end)
end

local function officialEnsureProfile(provider,email,callback)
  firebaseGet(accountPath(playerStats.localId),function(ok,body)
    if not ok then callback(false,errorText(body)); return end
    if nonNull(body) then
      if not firebaseRestoreAccount(body,provider) then callback(false,"بيانات ملف اللاعب تالفة."); return end
      playerStats.idToken=playerStats.idToken; playerStats.sessionActive=true; saveGameData(); callback(true,"تم استرجاع ملف اللاعب.")
    else
      resetLocalAccountData(); playerStats.accountEmail=string.lower(tostring(email or playerStats.accountEmail or "")); playerStats.authProvider=provider; playerStats.sessionActive=true
      firebaseSyncProfile(function(saved) if saved then callback(true,"تم إنشاء ملف اللاعب.") else callback(false,"تعذر إنشاء ملف اللاعب.") end end)
    end
  end)
end

function firebaseAuthSignUp(email,password,callback)
  email=string.lower(tostring(email or ""));
  if officialAuthEnabled() then
    firebaseAuthRequest("accounts:signUp",{email=email,password=password,returnSecureToken=true},function(ok,body)
      if not ok then callback(false,authErrorText(body)); return end
      if not applyOfficialAuth(body,"email",email) then callback(false,"تعذر قراءة هوية الحساب الرسمية."); return end
      officialEnsureProfile("email",email,callback)
    end)
    return
  end
  local key=accountKey(email)
  firebaseGet(accountPath(key),function(ok,body)
    if not ok then callback(false,errorText(body)); return end
    if nonNull(body) then callback(false,"هذا البريد لديه حساب بالفعل."); return end
    resetLocalAccountData(); playerStats.accountEmail=email; playerStats.localId=key; playerStats.authProvider="email"; playerStats.passwordSalt=makeSalt(); playerStats.passwordHash=passwordDigest(password,playerStats.passwordSalt); playerStats.sessionActive=true; saveGameData()
    firebasePut(accountPath(key), firebaseJsonObject(makeAccountRecord()), function(saved,response,code) if saved then callback(true,"تم إنشاء الحساب.") else callback(false,errorText(response) or "تعذر حفظ سجل الحساب.",code) end end)
  end)
end

function firebaseAuthSignIn(email,password,callback)
  email=string.lower(tostring(email or ""));
  if officialAuthEnabled() then
    firebaseAuthRequest("accounts:signInWithPassword",{email=email,password=password,returnSecureToken=true},function(ok,body)
      if not ok then callback(false,authErrorText(body)); return end
      if not applyOfficialAuth(body,"email",email) then callback(false,"تعذر قراءة هوية الحساب الرسمية."); return end
      officialEnsureProfile("email",email,callback)
    end)
    return
  end
  local key=accountKey(email)
  firebaseGet(accountPath(key),function(ok,body)
    if not ok then callback(false,errorText(body)); return end
    if not nonNull(body) then callback(false,"لا يوجد حساب بهذا البريد."); return end
    local root=newJsonObject(body); local salt=optString(root,"passwordSalt",""); local stored=optString(root,"passwordHash","")
    if salt=="" or stored=="" or stored~=passwordDigest(password,salt) then callback(false,"البريد أو كلمة المرور غير صحيحة."); return end
    playerStats.accountEmail=email; playerStats.localId=key; playerStats.passwordSalt=salt; playerStats.passwordHash=stored; if not firebaseRestoreAccount(body,"email") then callback(false,"بيانات الحساب تالفة."); return end; callback(true,"تم تسجيل الدخول.")
  end)
end

local function googleTokenPostBody(token)
  return "access_token="..formEncode(token).."&providerId=google.com"
end

function firebaseSignInWithGoogleToken(token,fallbackEmail,callback)
  token=tostring(token or ""); fallbackEmail=string.lower(tostring(fallbackEmail or ""))
  if token=="" then if callback then callback(false,"تعذر الحصول على رمز Google.") end; return end
  if officialAuthEnabled() then
    firebaseAuthRequest("accounts:signInWithIdp",{postBody=googleTokenPostBody(token),requestUri="http://localhost",returnSecureToken=true,returnIdpCredential=true},function(ok,body)
      if not ok then if callback then callback(false,authErrorText(body)) end; return end
      if not applyOfficialAuth(body,"google.com",fallbackEmail) then if callback then callback(false,"تعذر قراءة هوية Google الرسمية.") end; return end
      officialEnsureProfile("google.com",fallbackEmail,callback)
    end)
    return
  end
  -- وضع الحساب الحالي لا يملك مفتاح Firebase Auth الرسمي؛ نتحقق من رمز Google عبر نقطة معلومات OIDC قبل إنشاء/استعادة سجل الحساب.
  local infoUrl="https://openidconnect.googleapis.com/v1/userinfo?access_token="..formEncode(token)
  httpRequest("GET",infoUrl,nil,false,function(ok,body)
    if not ok then if callback then callback(false,"تعذر التحقق من حساب Google عبر الشبكة.") end; return end
    local root=newJsonObject(body); local email=string.lower(optString(root,"email",fallbackEmail)); local verified=optBool(root,"email_verified",false)
    if email=="" or not verified then if callback then callback(false,"لم يؤكد Google البريد الإلكتروني لهذا الحساب.") end; return end
    local key="google_"..accountKey(email)
    firebaseGet(accountPath(key),function(readOk,record)
      if not readOk then if callback then callback(false,errorText(record)) end; return end
      playerStats.accountEmail=email; playerStats.localId=key; playerStats.authProvider="google.com"; playerStats.sessionActive=true; playerStats.idToken=""; playerStats.refreshToken=""; playerStats.passwordSalt=""; playerStats.passwordHash=""
      if nonNull(record) then
        if not firebaseRestoreAccount(record,"google.com") then if callback then callback(false,"بيانات حساب Google تالفة.") end; return end
        saveGameData(); if callback then callback(true,"تم تسجيل الدخول عبر Google.") end
      else
        resetLocalAccountData(); playerStats.accountEmail=email; playerStats.localId=key; playerStats.authProvider="google.com"; playerStats.sessionActive=true; playerStats.idToken=""; saveGameData()
        firebaseSyncProfile(function(saved) if callback then callback(saved,saved and "تم إنشاء ملفك عبر Google." or "تعذر حفظ ملف Google على الخادم.") end end)
      end
    end)
  end)
end

function firebaseSendPasswordReset(email,callback)
  email=string.lower(tostring(email or ""))
  if not officialAuthEnabled() then if callback then callback(false,"استعادة كلمة المرور الرسمية غير مهيأة على الخادم.") end; return end
  if email=="" then if callback then callback(false,"أدخل البريد الإلكتروني أولًا.") end; return end
  firebaseAuthRequest("accounts:sendOobCode",{requestType="PASSWORD_RESET",email=email},function(ok,body)
    if callback then callback(ok,ok and "تم إرسال رسالة استعادة الحساب إلى بريدك الإلكتروني." or authErrorText(body)) end
  end)
end

function firebaseRefreshSession(callback)
  if not playerStats.sessionActive or not playerStats.accountEmail then if callback then callback(false,"لا توجد جلسة.") end; return end
  local function readProfile()
    firebaseGet(accountPath(playerStats.localId or accountKey(playerStats.accountEmail)),function(ok,body)
      if not ok or not nonNull(body) then if callback then callback(false,"تعذر استرجاع الحساب.") end; return end
      local restored=firebaseRestoreAccount(body,playerStats.authProvider)
      if not restored then if callback then callback(false,"بيانات الحساب تالفة.") end; return end
      firebasePullSocial(function() if callback then callback(true,"تم استرجاع الحساب.") end end)
    end)
  end
  if officialAuthEnabled() and playerStats.refreshToken and playerStats.refreshToken~="" then refreshOfficialToken(function(ok) if ok then readProfile() else if callback then callback(false,"انتهت جلسة المصادقة الرسمية. سجل الدخول مرة أخرى.") end end end) else readProfile() end
end

function firebaseSignOut()
  if stopMessagePolling then stopMessagePolling() end; if firebaseStopSocialSession then firebaseStopSocialSession() end; if firebaseStopPrivateDuelWatch then firebaseStopPrivateDuelWatch() end
  resetLocalAccountData(); playerStats.sessionActive=false; playerStats.idToken=""; playerStats.refreshToken=""; playerStats.localId=""; playerStats.passwordHash=""; playerStats.passwordSalt=""; playerStats.accountEmail=""; playerStats.authProvider=""; saveGameData()
end

function firebaseCheckUsername(username,callback)
  firebaseGet("/usernames/"..firebaseEncodeKey(username),function(ok,body) if not ok then callback(false,false,body); return end; callback(true,nonNull(body),body) end)
end
function firebaseClaimUsername(username,callback) firebasePut("/usernames/"..firebaseEncodeKey(username),firebaseJsonObject({uid=playerStats.localId,email=playerStats.accountEmail,claimedAt=os.time()}),callback) end
function firebaseSubmitLeaderboard(callback) firebaseSyncProfile(callback) end

function firebaseRequestLeaderboardQualification(overtakes,callback)
  local count=math.max(0,math.floor(tonumber(overtakes) or 0))
  if count<4 then if callback then callback(false,"يلزم تجاوز أربعة متسابقين في سباق جماعي مكتمل قبل طلب الإضافة.") end; return end
  if GameState.lastRaceWasMultiplayer~=true then if callback then callback(false,"يُقبل طلب المتصدرين من نتائج السباقات الجماعية فقط.") end; return end
  local key=firebaseEncodeKey(playerStats.username); local candidate={username=playerStats.username,name=playerStats.name,overtakes=count,wins=playerStats.wins or 0,bestScore=playerStats.bestScore or 0,qualifiedAt=os.time(),status="pending_review",finalReward=500000}
  firebasePut("/leaderboardQualification/"..key,firebaseJsonObject(candidate),function(ok,body,code)
    if ok then playerStats.leaderboardOvertakes=math.max(playerStats.leaderboardOvertakes or 0,count); playerStats.leaderboardQualified=true; firebaseSyncProfile(function() end); firebasePublishAdminFeedEvent("leaderboard_qualification_requested",candidate,function() end) end
    if callback then callback(ok,ok and "تم إرسال طلب التأهل للمراجعة." or "تعذر إرسال طلب التأهل.",code) end
  end)
end

function firebaseGetPublicProfile(username,callback) firebaseGet("/players/"..firebaseEncodeKey(username),callback) end

function firebaseSendFriendRequest(target,callback)
  local payload=firebaseJsonObject({from=playerStats.username,name=playerStats.name,createdAt=os.time()})
  firebasePut("/friendRequests/"..firebaseEncodeKey(target).."/"..firebaseEncodeKey(playerStats.username),payload,function(ok,body,code) if ok then table.insert(Multiplayer.sentRequests,target); firebaseAppendNotification(target,"friend_request","لديك طلب صداقة جديد من "..tostring(playerStats.name or playerStats.username),{from=playerStats.username},function() end); firebaseSyncProfile(function() end) end; if callback then callback(ok,body,code) end end)
end
function firebaseAcceptFriendRequest(target,callback)
  firebasePut("/friends/"..firebaseEncodeKey(playerStats.username).."/"..firebaseEncodeKey(target),firebaseJsonObject({since=os.time()}),function(ok,body,code) if ok then table.insert(Multiplayer.friends,target); firebaseAppendNotification(target,"friend_accept","تم قبول طلب الصداقة من "..tostring(playerStats.name or playerStats.username),{from=playerStats.username},function() end); for i,v in ipairs(Multiplayer.friendRequests) do if v==target then table.remove(Multiplayer.friendRequests,i); break end end; firebaseSyncProfile(function() end) end; if callback then callback(ok,body,code) end end)
end

function firebaseSyncAdminPlayerSnapshot(active,callback)
  if type(active)=="function" then callback=active; active=true end
  if active==nil then active=true end
  if not playerStats.sessionActive or not playerStats.username or playerStats.username=="" then if callback then callback(false) end; return end
  local key=firebaseEncodeKey(playerStats.username)
  local snapshot=firebaseJsonObject({username=playerStats.username,name=playerStats.name or playerStats.username,status=playerStats.status or "",wins=playerStats.wins or 0,money=playerStats.money or 0,level=playerStats.level or 1,reputation=playerStats.reputation or 0,bestScore=playerStats.bestScore or 0,bestDistance=playerStats.bestDistance or 0,room=Multiplayer.currentRoom or "",lastSeen=os.time(),active=active==true,updatedAt=os.time()})
  firebasePut("/adminFeed/users/"..key,snapshot,callback)
end

function firebasePublishAdminFeedEvent(kind,details,callback)
  if not playerStats.sessionActive or not playerStats.username or playerStats.username=="" then if callback then callback(false) end; return end
  local eventId=tostring(os.time()).."_"..tostring(math.random(100000,999999))
  local payload=firebaseJsonObject({kind=tostring(kind or "event"),actor=playerStats.username,room=Multiplayer.currentRoom or "",details=details or {},at=os.time()})
  firebasePut("/adminFeed/events/"..eventId,payload,callback)
end

function firebaseSyncAdminRoomSnapshot(roomName,empty,callback)
  local name=tostring(roomName or Multiplayer.currentRoom or ""); if name=="" then if callback then callback(false) end; return end
  local path="/adminFeed/rooms/"..firebaseEncodeKey(name)
  if empty==true then firebaseDelete(path,callback); return end
  local payload=firebaseJsonObject({room=name,owner=Multiplayer.roomOwner or "",status=Multiplayer.roomStatus or "",players=Multiplayer.roomPlayers or {},playerCount=#(Multiplayer.roomPlayers or {}),bots=Multiplayer.bots or {},botCount=#(Multiplayer.bots or {}),paused=Multiplayer.roomPaused==true,updatedAt=os.time()})
  firebasePut(path,payload,callback)
end

function firebaseSetPresence(active,callback)
  if not playerStats.sessionActive or not playerStats.username or playerStats.username=="" then if callback then callback(false,"لا يوجد ملف لاعب.") end; return end
  local key=firebaseEncodeKey(playerStats.username); local payload=firebaseJsonObject({username=playerStats.username,name=playerStats.name or playerStats.username,active=active==true,lastSeen=(firebaseServerNowSec and firebaseServerNowSec() or os.time()),room=Multiplayer.currentRoom or ""})
  firebasePut("/presence/"..key,payload,function(ok,body,code)
    if ok then firebaseSyncAdminPlayerSnapshot(active==true,function() end) end
    if callback then callback(ok,body,code) end
  end)
end

function firebaseGetOnlineFriends(callback)
  local friends={}; for _,friend in ipairs(Multiplayer.friends or {}) do friends[tostring(friend)]=true end
  firebaseGet("/presence",function(ok,body)
    local result={}; if ok and nonNull(body) then local root=newJsonObject(body); local keys=root and root.keys(); while keys and keys.hasNext() do local username=tostring(keys.next()); local item=root.optJSONObject(username); local seen=item and optInt(item,"lastSeen",0) or 0; local active=item and optBool(item,"active",false) or false; if friends[username] and active and (firebaseServerNowSec and firebaseServerNowSec() or os.time())-seen<=35 then table.insert(result,username); Multiplayer.remoteProfiles[username]=Multiplayer.remoteProfiles[username] or {username=username,name=optString(item,"name",username),status="متصل"} end end end
    Multiplayer.onlineFriends=result; if callback then callback(ok,result) end
  end)
end

function firebaseGetOnlinePlayers(callback)
  firebaseGet("/presence",function(ok,body)
    local result={}; if ok and nonNull(body) then local root=newJsonObject(body); local keys=root and root.keys(); while keys and keys.hasNext() do local username=tostring(keys.next()); local item=root.optJSONObject(username); local seen=item and optInt(item,"lastSeen",0) or 0; local active=item and optBool(item,"active",false) or false; if username~=playerStats.username and active and (firebaseServerNowSec and firebaseServerNowSec() or os.time())-seen<=35 then table.insert(result,username); Multiplayer.remoteProfiles[username]=Multiplayer.remoteProfiles[username] or {username=username,name=optString(item,"name",username),status=optString(item,"room","")~="" and "داخل غرفة" or "متصل"} end end end
    table.sort(result); Multiplayer.onlinePlayers=result; if callback then callback(ok,result) end
  end)
end

function firebaseSendGameInvite(target,callback)
  if not target or target=="" or target==playerStats.username then if callback then callback(false,"اسم الصديق غير صالح.") end; return end
  if not Multiplayer.currentRoom or Multiplayer.currentRoom=="" then if callback then callback(false,"لا توجد جلسة خاصة جاهزة.") end; return end
  local payload=firebaseJsonObject({from=playerStats.username,fromName=playerStats.name,to=target,room=Multiplayer.currentRoom,roomName=Multiplayer.currentRoomName or "سباق خاص",roomMode="private_duel",private=true,maxPlayers=2,createdAt=os.time(),status="pending"})
  firebasePut("/gameInvites/"..firebaseEncodeKey(target).."/"..firebaseEncodeKey(playerStats.username),payload,function(ok,body,code) if ok then Multiplayer.sentGameInvites[target]=true; firebaseAppendNotification(target,"game_invite","دعوة سباق جديدة من "..tostring(playerStats.name or playerStats.username),{room=Multiplayer.currentRoom},function() end) end; if callback then callback(ok,body,code) end end)
end
function firebaseSendRoomInvite(target,callback)
  target=tostring(target or "")
  if target=="" or target==playerStats.username then if callback then callback(false,"اسم الصديق غير صالح.") end; return end
  if not Multiplayer.currentRoom or Multiplayer.currentRoom=="" then if callback then callback(false,"لا توجد غرفة مفتوحة.") end; return end
  local payload=firebaseJsonObject({from=playerStats.username,fromName=playerStats.name,to=target,room=Multiplayer.currentRoom,roomName=Multiplayer.currentRoom,roomMode=Multiplayer.roomMode or "عامة",private=false,maxPlayers=Multiplayer.roomMaxPlayers or 4,createdAt=os.time(),status="pending"})
  firebasePut("/gameInvites/"..firebaseEncodeKey(target).."/"..firebaseEncodeKey(playerStats.username),payload,function(ok,body,code)
    if ok then Multiplayer.sentGameInvites[target]=true; firebaseAppendNotification(target,"room_invite","دعوة إلى غرفة من "..tostring(playerStats.name or playerStats.username),{room=Multiplayer.currentRoom},function() end) end
    if callback then callback(ok,body,code) end
  end)
end

function firebaseCreatePrivateDuel(target,duration,callback)
  if not target or target=="" or target==playerStats.username then if callback then callback(false,"اسم الصديق غير صالح.") end; return end
  duration=math.max(60,math.min(720,tonumber(duration) or Settings.raceDurationSeconds or 300)); local room="duel_"..firebaseEncodeKey(playerStats.username).."_"..tostring(os.time()); local goal=(LevelsData[math.min(playerStats.level,#LevelsData)] or {}).goal or 300
  local payload={name="",displayName=(playerStats.name~="" and playerStats.name or playerStats.username).." وسباق خاص",owner=playerStats.username,mode="private_duel",private=true,maxPlayers=2,status="waiting",createdAt=os.time(),players={},race={status="waiting",startAt=0,goal=goal,duration=duration}}
  payload.players[playerStats.username]={username=playerStats.username,name=playerStats.name,joinedAt=os.time()}
  Multiplayer.currentRoom=room; Multiplayer.currentRoomName="سباق خاص مع "..tostring(target); Multiplayer.roomMode="private_duel"; Multiplayer.roomOwner=playerStats.username; Multiplayer.roomStatus="waiting"; Multiplayer.privateMatch=true; Multiplayer.privateMatchId=room; Multiplayer.selectedDuration=duration
  firebasePut("/rooms/"..firebaseEncodeKey(room),firebaseJsonObject(payload),function(ok,body,code)
    if not ok then if callback then callback(false,"تعذر تجهيز السباق الخاص.",code) end; return end
    firebaseSendGameInvite(target,function(sent,inviteBody,inviteCode) if not sent then callback(false,"تعذر إرسال دعوة اللعب.",inviteCode); return end; firebaseWatchPrivateDuel(); if callback then callback(true) end end)
  end)
end

function firebaseGetGameInvites(callback)
  firebaseGet("/gameInvites/"..firebaseEncodeKey(playerStats.username),function(ok,body)
    local result={}; if ok and nonNull(body) then local root=newJsonObject(body); local keys=root and root.keys(); while keys and keys.hasNext() do local key=tostring(keys.next()); local item=root.optJSONObject(key); if item and optString(item,"status","pending")=="pending" then table.insert(result,{from=optString(item,"from",key),fromName=optString(item,"fromName",key),room=optString(item,"room",""),roomName=optString(item,"roomName","سباق خاص"),roomMode=optString(item,"roomMode","private_duel"),private=optBool(item,"private",false),createdAt=optInt(item,"createdAt",0),status="pending"}) end end end
    Multiplayer.gameInvites=result; if callback then callback(ok,result) end
  end)
end
function firebaseRejectGameInvite(invite,callback)
  local from=tostring((invite or {}).from or ""); if from=="" then if callback then callback(false,"دعوة غير صالحة.") end; return end
  firebasePatch("/gameInvites/"..firebaseEncodeKey(playerStats.username).."/"..firebaseEncodeKey(from),firebaseJsonObject({status="rejected",rejectedAt=os.time()}),callback)
end

function firebaseAcceptGameInvite(invite,callback)
  invite=invite or {}; local room=tostring(invite.room or ""); local from=tostring(invite.from or "")
  if room=="" or from=="" then if callback then callback(false,"بيانات الدعوة غير مكتملة.") end; return end
    firebaseGet("/rooms/"..firebaseEncodeKey(room),function(ok,body)
    if not ok or not nonNull(body) then if callback then callback(false,"الجلسة الخاصة لم تعد متاحة.") end; return end
    local root=newJsonObject(body); local isPrivate=optBool(root,"private",false) or optString(root,"mode","")=="private_duel"; local players=optObj(root,"players"); local count=0; local keys=players and players.keys(); while keys and keys.hasNext() do keys.next(); count=count+1 end
    local limit=isPrivate and 2 or 4; if count>=limit then if callback then callback(false,isPrivate and "المباراة الخاصة اكتملت بالفعل." or "الغرفة ممتلئة، الحد الأقصى أربعة لاعبين.") end; return end
    Multiplayer.currentRoom=room; Multiplayer.currentRoomName=optString(root,"displayName",room); Multiplayer.roomOwner=optString(root,"owner",from); Multiplayer.roomMode=optString(root,"mode","خاصة"); Multiplayer.roomStatus=optString(root,"status","waiting"); Multiplayer.privateMatch=isPrivate; Multiplayer.privateMatchId=isPrivate and room or ""
    firebasePut("/rooms/"..firebaseEncodeKey(room).."/players/"..firebaseEncodeKey(playerStats.username),firebaseJsonObject({username=playerStats.username,name=playerStats.name,joinedAt=os.time()}),function(joined,joinBody,joinCode)
      if not joined then if callback then callback(false,"تعذر الانضمام إلى المباراة الخاصة.",joinCode) end; return end
      local race=optObj(root,"race"); local startAt=optInt(race,"startAt",0); local startAtMs=tonumber(race and race.optDouble("startAtMs",startAt*1000) or startAt*1000) or startAt*1000; local goal=optInt(race,"goal",300); local duration=optInt(race,"duration",Settings.raceDurationSeconds or 120); Multiplayer.raceStartAt=startAt; Multiplayer.raceStartAtMs=startAtMs; Multiplayer.raceSessionId=optString(race,"raceSessionId",""); Multiplayer.raceGoal=goal; Multiplayer.selectedDuration=duration; Multiplayer.raceStatus=optString(race,"status","waiting")
      firebasePatch("/gameInvites/"..firebaseEncodeKey(playerStats.username).."/"..firebaseEncodeKey(from),firebaseJsonObject({status="accepted",acceptedAt=os.time()}),function() firebaseRefreshRoom(function() if callback then callback(true) end end) end)
    end)
  end)
end

function firebaseWatchGameInvites()
  if Multiplayer.invitePollActive then return end; Multiplayer.invitePollActive=true
  local function poll()
    if not Multiplayer.invitePollActive or not playerStats.sessionActive or not playerStats.username or playerStats.username=="" then Multiplayer.invitePollActive=false; return end
    firebaseGetGameInvites(function(ok,list)
      if ok and list and #list>(tonumber(Multiplayer.lastInviteCount) or 0) then local newest=list[#list]; playDynamicSFX("invite_received",0,1,0,0.76); say("دعوة لعب جديدة من "..tostring(newest.fromName or newest.from),true) end
      Multiplayer.lastInviteCount=list and #list or 0; Handler().postDelayed(Runnable({run=poll}),3500)
    end)
  end
  poll()
end
function firebaseStopGameInviteWatch() Multiplayer.invitePollActive=false end
function firebaseStartSocialSession()
  local start=function()
    firebaseSetPresence(true,function() end); firebaseWatchGameInvites(); if firebaseGetBlockedUsers then firebaseGetBlockedUsers(function() end) end; if firebaseStartCommunityPolling then firebaseStartCommunityPolling() end; if firebaseGetRemoteConfig then firebaseGetRemoteConfig(function() end) end; if firebaseRefreshConnectionQuality then firebaseRefreshConnectionQuality(function() end) end; if wrStartHeartbeat then wrStartHeartbeat() end
    if Multiplayer.onlinePresencePollActive then return end; Multiplayer.onlinePresencePollActive=true
    local function heartbeat()
      if not Multiplayer.onlinePresencePollActive or not playerStats.sessionActive then Multiplayer.onlinePresencePollActive=false; return end
      firebaseSetPresence(true,function() Handler().postDelayed(Runnable({run=heartbeat}),15000) end)
    end
    Handler().postDelayed(Runnable({run=heartbeat}),15000)
  end
  if firebaseSyncServerClock then firebaseSyncServerClock(function() start() end) else start() end
end
function firebaseStopSocialSession()
  Multiplayer.onlinePresencePollActive=false; firebaseStopGameInviteWatch(); if firebaseStopCommunityPolling then firebaseStopCommunityPolling() end; if wrStopHeartbeat then wrStopHeartbeat() end; firebaseSetPresence(false,function() end)
end
function firebaseWatchPrivateDuel()
  if Multiplayer.presencePollActive then return end; Multiplayer.presencePollActive=true
  local function poll()
    if not Multiplayer.presencePollActive or not Multiplayer.privateMatch or not Multiplayer.currentRoom then Multiplayer.presencePollActive=false; return end
    firebaseRefreshRoom(function(ok)
      if ok and Multiplayer.raceStatus=="racing" and currentSection~="PLAYING_MULTIPLAYER" then Multiplayer.presencePollActive=false; loadSection("PLAYING_MULTIPLAYER"); return end
      Handler().postDelayed(Runnable({run=poll}),1200)
    end)
  end
  poll()
end
function firebaseStopPrivateDuelWatch() Multiplayer.presencePollActive=false end

function firebasePublishRoomActivity(eventType,extra,callback)
  if not Multiplayer.currentRoom or Multiplayer.currentRoom=="" or not playerStats.username or playerStats.username=="" then if callback then callback(false) end; return end
  local atMs=(firebaseServerNowMs and firebaseServerNowMs() or os.time()*1000); local payload={}
  for key,value in pairs(extra or {}) do payload[key]=value end
  payload.eventId=tostring(payload.eventId or (tostring(atMs).."_"..firebaseEncodeKey(playerStats.username).."_"..tostring(math.random(1000,9999)))); payload.type=eventType or "activity"; payload.by=playerStats.username; payload.name=playerStats.name or playerStats.username; payload.at=math.floor(atMs/1000); payload.atMs=atMs; payload.raceSessionId=Multiplayer.raceSessionId or ""; payload.source="room_activity"
  firebasePut("/rooms/"..firebaseEncodeKey(Multiplayer.currentRoom).."/activity/"..firebaseEncodeKey(payload.eventId),firebaseJsonObject(payload),callback)
end
function firebaseSendChat(message,callback)
  if not Multiplayer.currentRoom or Multiplayer.currentRoom=="" then if callback then callback(false,"لا توجد غرفة.") end; return end
  local stamp=(firebaseServerNowSec and firebaseServerNowSec() or os.time()); local payload=firebaseJsonObject({from=playerStats.username,name=playerStats.name,text=message,sentAt=stamp}); firebasePut("/rooms/"..firebaseEncodeKey(Multiplayer.currentRoom).."/chat/"..tostring(stamp).."_"..firebaseEncodeKey(playerStats.username),payload,function(ok,body,code) if ok and firebasePublishRoomActivity then firebasePublishRoomActivity("chat",{text=message},function() end) end; if callback then callback(ok,body,code) end end)
end
function firebaseSendPrivateMessage(target,message,callback)
  if not target or target=="" or not message or message=="" then if callback then callback(false,"بيانات الرسالة غير مكتملة.") end; return end
  local key=tostring(firebaseServerNowSec and firebaseServerNowSec() or os.time()).."_"..firebaseEncodeKey(playerStats.username)
  firebasePut("/privateMessages/"..firebaseEncodeKey(target).."/"..key,firebaseJsonObject({from=playerStats.username,name=playerStats.name,to=target,text=message,voice=false,sentAt=(firebaseServerNowSec and firebaseServerNowSec() or os.time()),read=false}),function(ok,body,code) if ok then firebaseAppendNotification(target,"private_message","رسالة خاصة جديدة من "..tostring(playerStats.name or playerStats.username),{messageId=key},function() end) end; if callback then callback(ok,body,code) end end)
end
function firebaseSendPrivateVoiceNotice(target,channel,voiceKey,callback)
  target=tostring(target or ""); channel=tostring(channel or ""); voiceKey=tostring(voiceKey or "")
  if target=="" or channel=="" or voiceKey=="" then if callback then callback(false,"بيانات الرسالة الصوتية غير مكتملة.") end; return end
  local key=tostring(firebaseServerNowSec and firebaseServerNowSec() or os.time()).."_voice_"..firebaseEncodeKey(playerStats.username)
  local payload=firebaseJsonObject({from=playerStats.username,name=playerStats.name,to=target,text="",voice=true,voiceChannel=channel,voiceKey=voiceKey,sentAt=(firebaseServerNowSec and firebaseServerNowSec() or os.time()),read=false})
  firebasePut("/privateMessages/"..firebaseEncodeKey(target).."/"..key,payload,function(ok,body,code) if ok then firebaseAppendNotification(target,"private_voice","رسالة صوتية جديدة من "..tostring(playerStats.name or playerStats.username),{messageId=key,voiceChannel=channel,voiceKey=voiceKey},function() end) end; if callback then callback(ok,body,code) end end)
end
function firebaseWatchRoomChat()
  if Multiplayer.roomMessagePollActive then return end
  Multiplayer.roomMessagePollActive=true
  local function poll()
    if not Multiplayer.roomMessagePollActive or not Multiplayer.currentRoom or Multiplayer.currentRoom=="" or not playerStats.sessionActive then Multiplayer.roomMessagePollActive=false; return end
    firebaseGet("/rooms/"..firebaseEncodeKey(Multiplayer.currentRoom).."/chat",function(ok,body)
      if ok and nonNull(body) then
        local root=newJsonObject(body); local keys=root and root.keys(); local newest=tonumber(Multiplayer.lastChatAt or 0) or 0; local newestFrom=""; local newestText=""
        while keys and keys.hasNext() do local key=tostring(keys.next()); local item=root.optJSONObject(key); local sent=item and optInt(item,"sentAt",0) or 0; local from=item and optString(item,"from","") or ""; if sent>newest and from~=playerStats.username then newest=sent; newestFrom=from; newestText=item and optString(item,"text","") or "" end end
        if newestFrom~="" then Multiplayer.lastChatAt=newest; Multiplayer.lastRoomMessageText=newestText; playDynamicSFX("chat_receive_soft",0,1,0,0.96); local preview=#newestText>180 and newestText:sub(1,180) or newestText; say("رسالة جديدة في الغرفة من "..newestFrom..(preview~="" and (": "..preview) or ""),true) end
      end
      Handler().postDelayed(Runnable({run=poll}),2500)
    end)
  end
  poll()
end
function firebaseWatchPrivateMessages()
  if Multiplayer.privateMessagePollActive then return end
  Multiplayer.privateMessagePollActive=true
  local function poll()
    if not Multiplayer.privateMessagePollActive or not playerStats.sessionActive or not playerStats.username or playerStats.username=="" then Multiplayer.privateMessagePollActive=false; return end
    firebaseGet("/privateMessages/"..firebaseEncodeKey(playerStats.username),function(ok,body)
      if ok and nonNull(body) then
        local root=newJsonObject(body); local keys=root and root.keys(); local newest=tonumber(Multiplayer.lastPrivateMessageAt or 0) or 0; local newestFrom=""; local newestText=""
        while keys and keys.hasNext() do local key=tostring(keys.next()); local item=root.optJSONObject(key); local sent=item and optInt(item,"sentAt",0) or 0; local from=item and optString(item,"from","") or ""; if sent>newest and from~=playerStats.username then newest=sent; newestFrom=from; newestText=item and optString(item,"text","") or "" end end
        if newestFrom~="" then Multiplayer.lastPrivateMessageAt=newest; Multiplayer.lastPrivateMessageText=newestText; playDynamicSFX("chat_receive_soft",0,1,0,0.96); local preview=#newestText>180 and newestText:sub(1,180) or newestText; say("رسالة خاصة جديدة من "..newestFrom..(preview~="" and (": "..preview) or ""),true) end
      end
      Handler().postDelayed(Runnable({run=poll}),3000)
    end)
  end
  poll()
end
function stopRoomMessagePolling() Multiplayer.roomMessagePollActive=false end
function stopPrivateMessagePolling() Multiplayer.privateMessagePollActive=false end
function stopMessagePolling() stopRoomMessagePolling(); stopPrivateMessagePolling() end
function firebaseRoomAction(action,target,callback) local stamp=(firebaseServerNowSec and firebaseServerNowSec() or os.time()); firebasePut("/rooms/"..firebaseEncodeKey(Multiplayer.currentRoom).."/actions/"..tostring(stamp).."_"..firebaseEncodeKey(playerStats.username),firebaseJsonObject({action=action,target=target or "",by=playerStats.username,at=stamp,atMs=(firebaseServerNowMs and firebaseServerNowMs() or stamp*1000),raceSessionId=Multiplayer.raceSessionId or ""}),function(ok,body,code) if ok and firebasePublishRoomActivity then firebasePublishRoomActivity("room_action",{action=action,target=target or ""},function() end) end; if callback then callback(ok,body,code) end end) end
function firebaseSendVoiceSignal(target,enabled,callback) firebasePut("/voiceSignals/"..firebaseEncodeKey(target).."/"..firebaseEncodeKey(playerStats.username),firebaseJsonObject({from=playerStats.username,to=target,enabled=enabled==true,at=(firebaseServerNowSec and firebaseServerNowSec() or os.time())}),callback) end
local function roomPlayersMap(players)
  local result={}; for _,username in ipairs(players or {}) do result[tostring(username)]=true end; return result
end

local function cleanupOwnRoomVoiceMessages(roomName,callback)
  local channel="room_"..firebaseEncodeKey(roomName)
  firebaseGet("/voiceMessages/"..channel,function(ok,body)
    if ok and nonNull(body) then
      local root=newJsonObject(body); local keys=root and root.keys()
      while keys and keys.hasNext() do local key=tostring(keys.next()); local item=root.optJSONObject(key); if item and optString(item,"from","")==playerStats.username then firebaseDelete("/voiceMessages/"..channel.."/"..key,function() end) end end
    end
    if callback then callback() end
  end)
end
local function deleteEmptyRoomArtifacts(roomName,callback)
  local roomKey=firebaseEncodeKey(roomName); local paths={
    "/rooms/"..roomKey,
    "/voiceLive/room_"..roomKey,
    "/voiceMessages/room_"..roomKey,
    "/adminFeed/rooms/"..roomKey
  }
  local index=1
  local function nextDelete()
    local path=paths[index]; index=index+1
    if not path then if callback then callback(true) end; return end
    firebaseDelete(path,function(ok)
      if not ok then if callback then callback(false) end; return end
      nextDelete()
    end)
  end
  nextDelete()
end

function firebaseLeaveCurrentRoom(callback)
  local roomName=Multiplayer.currentRoom
  if not roomName or roomName=="" then if callback then callback(true,true) end; return end
  local roomKey=firebaseEncodeKey(roomName); local userKey=firebaseEncodeKey(playerStats.username); local wasOwner=Multiplayer.roomOwner==playerStats.username
  pcall(function() if stopVoiceRecording then stopVoiceRecording() end end)
  local function finish(empty,nextOwner,leaveOk)
    if empty then pcall(function() if stopRemoteRoomMusic then stopRemoteRoomMusic() end end); deleteEmptyRoomArtifacts(roomName,function() end) else
      Multiplayer.roomOwner=nextOwner or Multiplayer.roomOwner; firebaseSyncAdminRoomSnapshot(roomName,false,function() end)
    end
    firebasePublishAdminFeedEvent("room_leave",{room=roomName,empty=empty,nextOwner=nextOwner or ""},function() end)
    Multiplayer.currentRoom=""; Multiplayer.roomPlayers={}; Multiplayer.roomOwner=""; Multiplayer.roomStatus=""; Multiplayer.roomMusicUrl=""; Multiplayer.roomMusicPlaying=false; Multiplayer.waitingForPlayers=false; Multiplayer.bots={}; Multiplayer.botEnabled=false
    if callback then callback(leaveOk==true,empty,nextOwner) end
  end
  local function removePlayerFromRoom()
    firebaseDelete("/rooms/"..roomKey.."/players/"..userKey,function(ok)
      firebaseDelete("/rooms/"..roomKey.."/readiness/"..userKey,function() end)
      cleanupOwnRoomVoiceMessages(roomName,function()
      firebaseGet("/rooms/"..roomKey.."/players",function(playersOk,body)
        local remaining={}; local nextOwner=""; local oldest=math.huge
        if playersOk and nonNull(body) then
          local root=newJsonObject(body); local keys=root and root.keys()
          while keys and keys.hasNext() do local username=tostring(keys.next()); table.insert(remaining,username); local item=root.optJSONObject(username); local joined=item and optInt(item,"joinedAt",0) or 0; if joined<=0 then joined=os.time() end; if joined<oldest then oldest=joined; nextOwner=username end end
        end
        local empty=#remaining==0
        local function afterTransfer() finish(empty,nextOwner,ok) end
        if not empty and wasOwner then
          firebasePatch("/rooms/"..roomKey,firebaseJsonObject({owner=nextOwner,ownerChangedAt=(firebaseServerNowSec and firebaseServerNowSec() or os.time()),status="waiting"}),function(transferred) if transferred then firebaseAppendRoomLog(roomName,"انتقلت ملكية الغرفة إلى "..nextOwner); Multiplayer.roomOwner=nextOwner end; afterTransfer() end)
        else afterTransfer() end
      end)
    end)
    end)
  end
  if firebasePublishRoomActivity then firebasePublishRoomActivity("room_leave",{room=roomName},function() removePlayerFromRoom() end) else removePlayerFromRoom() end
end
function firebaseSetRoomMusic(url,playing,callback)
  if Multiplayer.roomOwner~=playerStats.username then if callback then callback(false,"مالك الغرفة فقط يمكنه تشغيل موسيقى الغرفة.") end; return end
  if not Multiplayer.currentRoom or Multiplayer.currentRoom=="" then if callback then callback(false,"لا توجد غرفة.") end; return end
  url=tostring(url or ""); if url=="" then if callback then callback(false,"أدخل رابطًا مباشرًا لملف صوتي.") end; return end
  if not url:match("^https://[%w%._%-%/%%%?%=&]+$") or url:lower():match("%.(mp4|webm|m3u8)([?&]|$)") then if callback then callback(false,"يجب استخدام رابط HTTPS مباشر لملف صوتي فقط، وليس فيديو.") end; return end
  Multiplayer.roomMusicUrl=url; Multiplayer.roomMusicPlaying=playing~=false; Multiplayer.roomMusicUpdatedAt=(firebaseServerNowSec and firebaseServerNowSec() or os.time())
  firebasePatch("/rooms/"..firebaseEncodeKey(Multiplayer.currentRoom).."/music",firebaseJsonObject({url=url,playing=Multiplayer.roomMusicPlaying,by=playerStats.username,updatedAt=Multiplayer.roomMusicUpdatedAt}),function(ok,body,code) if ok then firebaseAppendRoomLog(Multiplayer.currentRoom,Multiplayer.roomMusicPlaying and "شغّل المالك موسيقى الغرفة." or "أوقف المالك موسيقى الغرفة.") end; if callback then callback(ok,body,code) end end)
end
function firebaseStopRoomMusic(callback)
  if Multiplayer.roomOwner~=playerStats.username then if callback then callback(false,"مالك الغرفة فقط يمكنه إيقاف موسيقى الغرفة.") end; return end
  if not Multiplayer.currentRoom or Multiplayer.currentRoom=="" then if callback then callback(false,"لا توجد غرفة.") end; return end
  Multiplayer.roomMusicPlaying=false; firebasePatch("/rooms/"..firebaseEncodeKey(Multiplayer.currentRoom).."/music",firebaseJsonObject({playing=false,updatedAt=(firebaseServerNowSec and firebaseServerNowSec() or os.time()),by=playerStats.username}),function(ok,body,code) if ok then if stopRemoteRoomMusic then stopRemoteRoomMusic() end; if callback then callback(true,body,code) end else if callback then callback(false,body,code) end end end)
end
function firebaseDeleteEmptyRoom(roomName,callback)
  roomName=tostring(roomName or ""); if roomName=="" then if callback then callback(false,"لا توجد غرفة.") end; return end
  local path="/rooms/"..firebaseEncodeKey(roomName); firebaseGet(path.."/players",function(ok,body)
    if not ok then if callback then callback(false,"تعذر التحقق من متواجدي الغرفة.") end; return end
    local empty=not nonNull(body)
    if not empty and body then local root=newJsonObject(body); local keys=root and root.keys(); empty=not (keys and keys.hasNext()) end
    if empty then firebaseDelete(path,function(deleted,response,code) if deleted then firebaseDelete("/voiceMessages/room_"..firebaseEncodeKey(roomName),function() end); firebaseDelete("/voiceLive/room_"..firebaseEncodeKey(roomName),function() end); firebaseDelete("/adminFeed/rooms/"..firebaseEncodeKey(roomName),function() end) end; if callback then callback(deleted,response,code) end end) else if callback then callback(false,"الغرفة ما زال فيها لاعبون، لم تُحذف.") end end
  end)
end
function firebaseCleanupEmptyRooms(callback)
  firebaseGet("/rooms",function(ok,body)
    if not ok then if callback then callback(false,0,"تعذر قراءة الغرف.") end; return end
    if not nonNull(body) then if callback then callback(true,0) end; return end
    local root=newJsonObject(body); local keys=root and root.keys(); local names={}; while keys and keys.hasNext() do table.insert(names,tostring(keys.next())) end
    local index,removed=1,0
    local function nextRoom()
      local encoded=names[index]; index=index+1
      if not encoded then if callback then callback(true,removed) end; return end
      local path="/rooms/"..firebaseEncodeKey(encoded); firebaseGet(path.."/players",function(playersOk,playersBody)
        if playersOk then local empty=not nonNull(playersBody); if not empty and playersBody then local pRoot=newJsonObject(playersBody); local pKeys=pRoot and pRoot.keys(); empty=not (pKeys and pKeys.hasNext()) end; if empty then firebaseDeleteEmptyRoom(encoded,function(deleted) if deleted then removed=removed+1 end; nextRoom() end) else nextRoom() end else nextRoom() end
      end)
    end
    nextRoom()
  end)
end

function firebaseRefreshRoom(callback)
  if not Multiplayer.currentRoom or Multiplayer.currentRoom=="" then if callback then callback(false,"لا توجد غرفة.") end; return end
  firebaseGet("/rooms/"..firebaseEncodeKey(Multiplayer.currentRoom),function(ok,body,code)
    if ok and nonNull(body) then
      local root=newJsonObject(body); Multiplayer.roomOwner=optString(root,"owner",optString(root,"host",Multiplayer.roomOwner)); Multiplayer.roomStatus=optString(root,"status",Multiplayer.roomStatus); Multiplayer.roomPlayers=jsonList(optObj(root,"players")); Multiplayer.readyPlayers={}; local readiness=optObj(root,"readiness"); local readinessKeys=readiness and readiness.keys(); while readinessKeys and readinessKeys.hasNext() do local readyUser=tostring(readinessKeys.next()); local readyItem=readiness.optJSONObject(readyUser); if readyItem and optBool(readyItem,"ready",false) then Multiplayer.readyPlayers[readyUser]=true end end; Multiplayer.playerReady=Multiplayer.readyPlayers[playerStats.username]==true; local currentPresence=roomPlayersMap(Multiplayer.roomPlayers); local previousPresence=Multiplayer.roomPresenceSnapshot or {}; if Multiplayer.roomPresenceReady then for username in pairs(currentPresence) do if not previousPresence[username] and username~=playerStats.username then playDynamicSFX("room_player_join",0,1,0,0.72); say("انضم "..username.." إلى الغرفة.",false); firebaseAppendRoomLog(Multiplayer.currentRoom, "انضم المتسابق "..username) end end; for username in pairs(previousPresence) do if not currentPresence[username] and username~=playerStats.username then playDynamicSFX("room_player_leave",0,1,0,0.68); say("غادر "..username.." الغرفة.",false); firebaseAppendRoomLog(Multiplayer.currentRoom, "غادر المتسابق "..username) end end end; Multiplayer.roomPresenceSnapshot=currentPresence; Multiplayer.roomPresenceReady=true
      local race=optObj(root,"race")
      if race then Multiplayer.raceStatus=optString(race,"status",Multiplayer.raceStatus or "waiting"); Multiplayer.raceStartAt=optInt(race,"startAt",Multiplayer.raceStartAt or 0); Multiplayer.raceStartAtMs=tonumber(race.optDouble("startAtMs",(Multiplayer.raceStartAt or 0)*1000)) or ((Multiplayer.raceStartAt or 0)*1000); Multiplayer.raceSessionId=optString(race,"raceSessionId",Multiplayer.raceSessionId or ""); Multiplayer.raceSeed=optInt(race,"raceSeed",Multiplayer.raceSeed or 0); Multiplayer.raceGoal=optInt(race,"goal",Multiplayer.raceGoal or 0); Multiplayer.selectedDuration=optInt(race,"duration",Multiplayer.selectedDuration or Settings.raceDurationSeconds or 300); Multiplayer.roomPaused=optBool(race,"paused",false); Multiplayer.pauseUpdatedAt=optInt(race,"pausedAt",Multiplayer.pauseUpdatedAt or 0) end
      local bot=optObj(root,"bot")
      if bot then Multiplayer.botEnabled=optBool(bot,"enabled",Multiplayer.botEnabled); Multiplayer.botName=optString(bot,"name",Multiplayer.botName); local okSkill,skill=pcall(function() return bot.optDouble("skill",Multiplayer.botSkill) end); if okSkill then Multiplayer.botSkill=tonumber(skill) or Multiplayer.botSkill end end
      local botsObj=optObj(root,"bots"); if botsObj then Multiplayer.bots={}; local botKeys=botsObj.keys(); while botKeys and botKeys.hasNext() do local botKey=tostring(botKeys.next()); local item=botsObj.optJSONObject(botKey); if item then local okSkill,skill=pcall(function() return item.optDouble("skill",0.55) end); table.insert(Multiplayer.bots,{id=botKey,name=optString(item,"name",botKey),skill=tonumber(skill) or 0.55}) end end; Multiplayer.botEnabled=#Multiplayer.bots>0 end
      local moderation=optObj(root,"moderation"); Multiplayer.mutedPlayers={}; Multiplayer.bannedPlayers={}; if moderation then Multiplayer.roomMuteAll=optBool(moderation,"muteAll",false); local muted=optObj(moderation,"muted"); local mutedKeys=muted and muted.keys(); while mutedKeys and mutedKeys.hasNext() do Multiplayer.mutedPlayers[tostring(mutedKeys.next())]=true end; local banned=optObj(moderation,"banned"); local bannedKeys=banned and banned.keys(); while bannedKeys and bannedKeys.hasNext() do Multiplayer.bannedPlayers[tostring(bannedKeys.next())]=true end else Multiplayer.roomMuteAll=false end
      local settings=optObj(root,"settings"); if settings then Multiplayer.roomLocked=optBool(settings,"locked",false); Multiplayer.roomMaxPlayers=math.max(2,math.min(4,optInt(settings,"maxPlayers",4))) else Multiplayer.roomLocked=false; Multiplayer.roomMaxPlayers=4 end
      local moderators=optObj(root,"moderators"); Multiplayer.roomModerators={}; local moderatorKeys=moderators and moderators.keys(); while moderatorKeys and moderatorKeys.hasNext() do Multiplayer.roomModerators[tostring(moderatorKeys.next())]=true end
      local music=optObj(root,"music"); local oldMusicUrl=Multiplayer.roomMusicUrl or ""; local oldMusicPlaying=Multiplayer.roomMusicPlaying==true; local newMusicUrl=music and optString(music,"url","") or ""; local newMusicPlaying=music and optBool(music,"playing",false) or false; local musicChanged=(oldMusicUrl~=newMusicUrl or oldMusicPlaying~=newMusicPlaying); Multiplayer.roomMusicUrl=newMusicUrl; Multiplayer.roomMusicPlaying=newMusicPlaying; Multiplayer.roomMusicUpdatedAt=music and optInt(music,"updatedAt",Multiplayer.roomMusicUpdatedAt or 0) or 0; if musicChanged then if newMusicPlaying and newMusicUrl~="" and (currentSection=="WAITING_ROOM" or currentSection=="PLAYING_MULTIPLAYER") then if playRemoteRoomMusic then playRemoteRoomMusic(newMusicUrl) end elseif not newMusicPlaying or newMusicUrl=="" then if stopRemoteRoomMusic then stopRemoteRoomMusic() end end end
      if Multiplayer.roomMuteAll or Multiplayer.mutedPlayers[playerStats.username] then Multiplayer.isMicEnabled=false else Multiplayer.isMicEnabled=true end
      firebaseSyncAdminRoomSnapshot(Multiplayer.currentRoom,false,function() end)
      if callback then callback(true,body,code) end
    else if callback then callback(false,body,code) end end
  end)
end

function firebaseListPublicRooms(body)
  local result={}; local root=newJsonObject(body); local keys=root and root.keys()
  while keys and keys.hasNext() do
    local roomName=tostring(keys.next()); local item=root.optJSONObject(roomName)
    if item then
      local private=optBool(item,"private",false) or optString(item,"mode","")=="private_duel"; local status=optString(item,"status","waiting")
      if not private and status~="closed" then
        local players=optObj(item,"players"); local playerCount=0; local playerKeys=players and players.keys(); while playerKeys and playerKeys.hasNext() do playerKeys.next(); playerCount=playerCount+1 end
        local maxPlayers=math.max(2,math.min(4,optInt(item,"maxPlayers",4)))
        table.insert(result,{room=roomName,displayName=optString(item,"displayName",optString(item,"name",roomName)),owner=optString(item,"owner",optString(item,"host",roomName)),status=status,players=players,playerCount=playerCount,maxPlayers=maxPlayers})
      end
    end
  end
  return result
end

function firebaseSetRoomBot(enabled,name,skill,callback)
  if Multiplayer.roomOwner~=playerStats.username then if callback then callback(false,"مالك الغرفة فقط يمكنه تغيير الروبوتات.") end; return end
  if not Multiplayer.currentRoom or Multiplayer.currentRoom=="" then if callback then callback(false,"لا توجد غرفة.") end; return end
  Multiplayer.botEnabled=enabled==true; Multiplayer.botName=name or Multiplayer.botName; Multiplayer.botSkill=skill or Multiplayer.botSkill
  if Multiplayer.botEnabled and #(Multiplayer.bots or {})==0 then Multiplayer.bots={{id="bot_1",name=Multiplayer.botName,skill=Multiplayer.botSkill}} end
  firebasePatch("/rooms/"..firebaseEncodeKey(Multiplayer.currentRoom),firebaseJsonObject({bot={enabled=Multiplayer.botEnabled,name=Multiplayer.botName,skill=Multiplayer.botSkill},bots=Multiplayer.bots}),callback)
end

function firebaseSetRoomBotCount(count,callback)
  if Multiplayer.roomOwner~=playerStats.username then if callback then callback(false,"مالك الغرفة فقط يمكنه تغيير الروبوتات.") end; return end
  if not Multiplayer.currentRoom or Multiplayer.currentRoom=="" then if callback then callback(false,"لا توجد غرفة.") end; return end
  count=math.max(0,math.min(3,math.floor(tonumber(count) or 0))); local bots={}
  for i=1,count do table.insert(bots,{id="bot_"..i,name="روبوت الطريق "..i,skill=0.50+math.random()*0.25}) end
  Multiplayer.bots=bots; Multiplayer.botEnabled=count>0; Multiplayer.botName=count>0 and bots[1].name or ""; Multiplayer.botSkill=count>0 and bots[1].skill or 0.55
  firebasePatch("/rooms/"..firebaseEncodeKey(Multiplayer.currentRoom),firebaseJsonObject({bot={enabled=Multiplayer.botEnabled,name=Multiplayer.botName,skill=Multiplayer.botSkill},bots=bots}),callback)
end

function firebaseSetRoomModeration(action,target,callback)
  local canManage=(Multiplayer.roomOwner==playerStats.username) or (Multiplayer.roomModerators and Multiplayer.roomModerators[playerStats.username])
  if not canManage then if callback then callback(false,"هذا الإجراء متاح لمالك الغرفة أو مساعده فقط.") end; return end
  if not Multiplayer.currentRoom or Multiplayer.currentRoom=="" then if callback then callback(false,"لا توجد غرفة.") end; return end
  local room="/rooms/"..firebaseEncodeKey(Multiplayer.currentRoom); local key=firebaseEncodeKey(target or "")
  if action=="mute_all" or action=="unmute_all" then
    local muted=action=="mute_all"; Multiplayer.roomMuteAll=muted
    firebasePatch(room.."/moderation",firebaseJsonObject({muteAll=muted,updatedAt=os.time(),by=playerStats.username}),callback)
  elseif action=="mute" then Multiplayer.mutedPlayers[target]=true; firebasePut(room.."/moderation/muted/"..key,"true",callback)
  elseif action=="unmute" then Multiplayer.mutedPlayers[target]=nil; firebaseDelete(room.."/moderation/muted/"..key,callback)
  elseif action=="ban" then
    Multiplayer.bannedPlayers[target]=true
    firebasePut(room.."/moderation/banned/"..key,firebaseJsonObject({by=playerStats.username,at=os.time()}),function(ok,body,code)
      if ok then firebaseDelete(room.."/players/"..key,function() if callback then callback(true,body,code) end end) else if callback then callback(false,body,code) end end
    end)
  elseif action=="unban" then Multiplayer.bannedPlayers[target]=nil; firebaseDelete(room.."/moderation/banned/"..key,callback)
  elseif action=="kick" then firebaseDelete(room.."/players/"..key,callback)
  else if callback then callback(false,"إجراء إدارة غير معروف.") end end
end

function firebaseSetRoomSettings(locked,maxPlayers,callback)
  if Multiplayer.roomOwner~=playerStats.username then if callback then callback(false,"مالك الغرفة فقط يمكنه تغيير الإعدادات.") end; return end
  if not Multiplayer.currentRoom or Multiplayer.currentRoom=="" then if callback then callback(false,"لا توجد غرفة.") end; return end
  local room="/rooms/"..firebaseEncodeKey(Multiplayer.currentRoom); local patch={updatedAt=os.time(),updatedBy=playerStats.username}
  if locked~=nil then Multiplayer.roomLocked=locked==true; patch.locked=Multiplayer.roomLocked end
  if maxPlayers~=nil then Multiplayer.roomMaxPlayers=math.max(2,math.min(4,math.floor(tonumber(maxPlayers) or 4))); patch.maxPlayers=Multiplayer.roomMaxPlayers end
  local logTarget=(locked~=nil and ((locked==true) and "locked" or "unlocked")) or ("max_"..tostring(maxPlayers or Multiplayer.roomMaxPlayers))
  firebasePatch(room.."/settings",firebaseJsonObject(patch),function(ok,body,code) if ok then firebaseRoomAction("room_settings",logTarget,function() end) end; if callback then callback(ok,body,code) end end)
end
function firebaseSetRoomModerator(target,enabled,callback)
  if Multiplayer.roomOwner~=playerStats.username then if callback then callback(false,"مالك الغرفة فقط يمكنه تعيين المساعدين.") end; return end
  if not Multiplayer.currentRoom or not target or target=="" then if callback then callback(false,"بيانات المساعد غير مكتملة.") end; return end
  local key=firebaseEncodeKey(target); Multiplayer.roomModerators=Multiplayer.roomModerators or {}; Multiplayer.roomModerators[target]=enabled==true
  local path="/rooms/"..firebaseEncodeKey(Multiplayer.currentRoom).."/moderators/"..key
  local done=function(ok,body,code) if ok then firebaseRoomAction(enabled and "promote_moderator" or "remove_moderator",target,function() end) end; if callback then callback(ok,body,code) end end
  if enabled then firebasePut(path,"true",done) else firebaseDelete(path,done) end
end

function firebaseSetRoomPause(paused,callback)
  if Multiplayer.roomOwner~=playerStats.username then if callback then callback(false,"مالك الغرفة فقط يمكنه إيقاف السباق.") end; return end
  if not Multiplayer.currentRoom or Multiplayer.currentRoom=="" then if callback then callback(false,"لا توجد غرفة.") end; return end
  Multiplayer.roomPaused=paused==true; Multiplayer.pauseUpdatedAt=os.time(); GameState.isPaused=Multiplayer.roomPaused
  firebasePatch("/rooms/"..firebaseEncodeKey(Multiplayer.currentRoom).."/race",firebaseJsonObject({paused=Multiplayer.roomPaused,pausedAt=Multiplayer.pauseUpdatedAt,status=Multiplayer.roomPaused and "paused" or "racing",updatedAt=os.time()}),function(ok,body,code)
    if ok then firebasePatch("/rooms/"..firebaseEncodeKey(Multiplayer.currentRoom),firebaseJsonObject({status=Multiplayer.roomPaused and "paused" or "racing"}),function() if callback then callback(true,body,code) end end) else if callback then callback(false,body,code) end end
  end)
end

function firebaseAppendRoomLog(roomId, message)
  if Multiplayer.roomOwner~=playerStats.username then return false end
  local logEntry = { time = os.time(), text = message, sender = playerStats.name }
  local key=tostring(logEntry.time).."_"..firebaseEncodeKey(playerStats.username).."_"..firebaseEncodeKey(message):sub(1,24)
  return firebasePut("/rooms/"..firebaseEncodeKey(roomId).."/logs/"..key, firebaseJsonObject(logEntry))
end

function firebaseStartRoomRace(goal, duration, callback)
  if Multiplayer.roomOwner~=playerStats.username then if callback then callback(false,"مالك الغرفة فقط يمكنه بدء السباق للجميع.") end; return end
  if not Multiplayer.currentRoom or Multiplayer.currentRoom=="" then if callback then callback(false,"لا توجد غرفة.") end; return end
  if type(duration)=="function" then callback=duration; duration=Multiplayer.selectedDuration or Settings.raceDurationSeconds or 300 end
  duration=math.max(60,math.min(720,tonumber(duration) or 300))
  local function publish()
    local startAtMs=(firebaseServerNowMs and firebaseServerNowMs() or (os.time()*1000))+5000; local startAt=math.floor(startAtMs/1000); local sessionId=tostring(startAtMs).."_"..tostring(math.random(1000,9999)); local raceSeed=math.floor((firebaseServerNowMs and firebaseServerNowMs() or os.time()*1000)%2147483647); Multiplayer.raceStartAt=startAt; Multiplayer.raceStartAtMs=startAtMs; Multiplayer.raceSessionId=sessionId; Multiplayer.raceSeed=raceSeed; Multiplayer.raceStatus="racing"; Multiplayer.raceGoal=goal or 0; Multiplayer.selectedDuration=duration
    local room=firebaseEncodeKey(Multiplayer.currentRoom); Multiplayer.roomPaused=false; GameState.isPaused=false; firebaseAppendRoomLog(Multiplayer.currentRoom,"بدأ المالك السباق للجميع. الجلسة "..sessionId)
    local racePayload={status="racing",startAt=startAt,startAtMs=startAtMs,serverNowAtPublish=(firebaseServerNowMs and firebaseServerNowMs() or os.time()*1000),goal=goal or 0,duration=duration,paused=false,startedBy=playerStats.username,updatedAt=(firebaseServerNowSec and firebaseServerNowSec() or os.time()),raceSessionId=sessionId,raceSeed=raceSeed}
    firebasePatch("/rooms/"..room,firebaseJsonObject({status="racing",race=racePayload}),callback)
  end
  if firebaseSyncServerClock then firebaseSyncServerClock(function() publish() end) else publish() end
end

local function parseRacePlayers(content)
  local result={}; local root=newJsonObject(content); local players=root and optObj(root,"players")
  if not players then return result end
  local keys=players.keys()
  while keys and keys.hasNext() do
    local username=tostring(keys.next()); local item=players.optJSONObject(username)
    if item then result[username]={distance=tonumber(item.optDouble("distance",0)) or 0,speed=tonumber(item.optDouble("speed",0)) or 0,lane=optInt(item,"lane",0),collisionCount=optInt(item,"collisionCount",0),overtakeCount=optInt(item,"overtakeCount",0),isCrashed=optBool(item,"isCrashed",false),finished=optBool(item,"finished",false),updatedAt=optInt(item,"updatedAt",0)} end
  end
  return result
end

function firebaseSyncRaceState(distance,speed,lane,finished,callback)
  if not Multiplayer.currentRoom or Multiplayer.currentRoom=="" or not playerStats.username or playerStats.username=="" then if callback then callback(false,{}) end; return end
  local room=firebaseEncodeKey(Multiplayer.currentRoom); local user=firebaseEncodeKey(playerStats.username)
  local payload=firebaseJsonObject({username=playerStats.username,distance=distance or 0,speed=speed or 0,lane=lane or 0,collisionCount=GameState.collisionCount or 0,overtakeCount=GameState.overtakeCount or 0,isCrashed=GameState.isCrashed==true,finished=finished==true,updatedAt=(firebaseServerNowSec and firebaseServerNowSec() or os.time()),updatedAtMs=(firebaseServerNowMs and firebaseServerNowMs() or os.time()*1000),raceSessionId=Multiplayer.raceSessionId or ""})
  firebasePatch("/rooms/"..room.."/players/"..user,payload,function(ok)
    if not ok then if callback then callback(false,{}) end; return end
    firebaseGet("/rooms/"..room,function(readOk,body)
      if not readOk then if callback then callback(false,{}) end; return end
      local roomRoot=newJsonObject(body); local syncedRace=roomRoot and optObj(roomRoot,"race") or nil; if syncedRace then Multiplayer.roomPaused=optBool(syncedRace,"paused",false); GameState.isPaused=Multiplayer.roomPaused end
      local players=parseRacePlayers(body); Multiplayer.remoteRacePlayers=players; Multiplayer.remoteProfiles=Multiplayer.remoteProfiles or {}; local farthest=0
      for username,state in pairs(players) do
        if username~=playerStats.username then
          if (state.distance or 0)>farthest then farthest=state.distance end
          if not Multiplayer.remoteProfiles[username] then firebaseGet("/players/"..firebaseEncodeKey(username),function(profileOk,profileBody) if profileOk and nonNull(profileBody) then local profile=newJsonObject(profileBody); if profile then Multiplayer.remoteProfiles[username]={username=username,name=optString(profile,"name",username),bio=optString(profile,"bio",""),status=optString(profile,"status","")} end end end) end
        end
      end
      Multiplayer.opponentDistance=farthest; if callback then callback(true,players) end
    end)
  end)
end

function firebasePublishRaceEvent(eventType,extra,callback)
  if not Multiplayer.currentRoom or Multiplayer.currentRoom=="" or not playerStats.username or playerStats.username=="" then if callback then callback(false) end; return end
  local atMs=(firebaseServerNowMs and firebaseServerNowMs() or os.time()*1000); local payload={}
  for key,value in pairs(extra or {}) do payload[key]=value end
  local eventId=tostring(payload.eventId or (tostring(atMs).."_"..firebaseEncodeKey(playerStats.username).."_"..tostring(math.random(1000,9999))))
  payload.eventId=eventId; payload.type=eventType or "event"; payload.by=playerStats.username; payload.name=playerStats.name or playerStats.username; payload.distance=tonumber(payload.distance or GameState.distance or 0) or 0; payload.lane=tonumber(payload.lane or GameState.lane or 0) or 0; payload.speed=tonumber(payload.speed or GameState.speed or 0) or 0; payload.direction=tonumber(payload.direction or payload.lane or 0) or 0; payload.soundKey=tostring(payload.soundKey or ""); payload.at=math.floor(atMs/1000); payload.atMs=atMs; payload.source="client_event"; payload.raceSessionId=Multiplayer.raceSessionId or ""
  local eventKey=firebaseEncodeKey(eventId); firebasePut("/rooms/"..firebaseEncodeKey(Multiplayer.currentRoom).."/raceEvents/"..eventKey,firebaseJsonObject(payload),callback)
end

function firebaseWatchRaceEvents()
  if Multiplayer.raceEventPollActive then return end; Multiplayer.raceEventPollActive=true; Multiplayer.processedRaceEvents=Multiplayer.processedRaceEvents or {}; Multiplayer.lastRaceEventAtMs=(firebaseServerNowMs and firebaseServerNowMs() or os.time()*1000)-3000
  local function poll()
    if not Multiplayer.raceEventPollActive or not Multiplayer.currentRoom or Multiplayer.currentRoom=="" or not playerStats.sessionActive then Multiplayer.raceEventPollActive=false; return end
    firebaseGet("/rooms/"..firebaseEncodeKey(Multiplayer.currentRoom).."/raceEvents",function(ok,body)
      if ok and nonNull(body) then
        local root=newJsonObject(body); local keys=root and root.keys(); local newest=tonumber(Multiplayer.lastRaceEventAtMs or 0) or 0
        while keys and keys.hasNext() do local key=tostring(keys.next()); local item=root.optJSONObject(key); local atMs=item and tonumber(item.optDouble("atMs",0)) or 0; local by=item and optString(item,"by","") or ""; local eventId=item and optString(item,"eventId",key) or key; local eventSession=item and optString(item,"raceSessionId","") or ""
          if item and by~=playerStats.username and not Multiplayer.processedRaceEvents[eventId] and atMs>=newest and (eventSession=="" or eventSession==(Multiplayer.raceSessionId or "")) then
            Multiplayer.processedRaceEvents[eventId]=true; newest=math.max(newest,atMs); local eventType=optString(item,"type","event"); local eventDistance=tonumber(item.optDouble("distance",GameState.distance or 0)) or (GameState.distance or 0); local gap=math.abs((GameState.distance or 0)-eventDistance); local volume=math.max(0.14,math.min(0.90,0.90-(gap/120))); local direction=tonumber(item.optDouble("direction",item.optDouble("lane",0))) or 0; local severity=optString(item,"severity","light"); local requestedSound=optString(item,"soundKey",""); local sound=requestedSound~="" and requestedSound or ((eventType=="collision" and (severity=="heavy" and "race_collision_heavy" or "race_collision_light")) or ((eventType=="overtake" or eventType=="opponent_pass") and (direction<0 and "race_opponent_pass_left" or "race_opponent_pass_right")) or (eventType=="nearby" and "nearby") or (eventType=="win" and "result_win") or (eventType=="finish" and "finish") or "race_caution"); local eventAtMs=tonumber(item.optDouble("atMs",0)) or atMs; local function emitEvent() playDynamicSFX(sound,0,1.0,math.max(-1,math.min(1,direction)),volume); if eventType=="collision" then say("اصطدام اللاعب "..tostring(optString(item,"name",by))..".",false) elseif eventType=="opponent_pass" or eventType=="overtake" then say("مرت سيارة اللاعب "..tostring(optString(item,"name",by)).." من "..(direction<0 and "اليسار" or "اليمين")..".",false) elseif eventType=="nearby" then say("سيارة بجوارك.",false) elseif eventType=="win" then say("فاز اللاعب "..tostring(optString(item,"name",by)).." بالسباق.",true) elseif eventType=="finish" then say("وصل اللاعب "..tostring(optString(item,"name",by)).." إلى النهاية.",false) end end; if scheduleSynchronizedRaceEvent then scheduleSynchronizedRaceEvent(eventAtMs,emitEvent) else emitEvent() end
          end
        end
        Multiplayer.lastRaceEventAtMs=newest
      end
      Handler().postDelayed(Runnable({run=poll}),900)
    end)
  end
  poll()
end
function firebaseStopRaceEventWatch() Multiplayer.raceEventPollActive=false end

function firebaseWatchRoomActivity()
  if Multiplayer.roomActivityPollActive then return end
  Multiplayer.roomActivityPollActive=true; Multiplayer.processedRoomActivities=Multiplayer.processedRoomActivities or {}; Multiplayer.lastRoomActivityAtMs=(firebaseServerNowMs and firebaseServerNowMs() or os.time()*1000)-3000
  local function poll()
    if not Multiplayer.roomActivityPollActive or not Multiplayer.currentRoom or Multiplayer.currentRoom=="" or not playerStats.sessionActive then Multiplayer.roomActivityPollActive=false; return end
    firebaseGet("/rooms/"..firebaseEncodeKey(Multiplayer.currentRoom).."/activity",function(ok,body)
      if ok and nonNull(body) then
        local root=newJsonObject(body); local keys=root and root.keys(); local newest=tonumber(Multiplayer.lastRoomActivityAtMs or 0) or 0
        while keys and keys.hasNext() do
          local key=tostring(keys.next()); local item=root.optJSONObject(key); local atMs=item and tonumber(item.optDouble("atMs",0)) or 0; local eventId=item and optString(item,"eventId",key) or key; local by=item and optString(item,"by","") or ""
          if item and by~=playerStats.username and not Multiplayer.processedRoomActivities[eventId] and atMs>=newest then
            Multiplayer.processedRoomActivities[eventId]=true; newest=math.max(newest,atMs); local eventType=optString(item,"type","activity"); local name=optString(item,"name",by); local action=optString(item,"action",""); local direction=tonumber(item.optDouble("direction",0)) or 0
            local sound=(eventType=="room_join" and "room_player_join") or (eventType=="room_leave" and "room_player_leave") or (eventType=="chat" and "room_message") or (eventType=="room_action" and ((action=="kick" and "room_kick") or (action=="ban" and "room_ban") or (action=="mute" and "moderation_mute") or (action=="unmute" and "moderation_unmute") or (action=="invite" and "invite_received") or "settings_change")) or "room_message"
            local eventAtMs=tonumber(item.optDouble("atMs",0)) or atMs
            local function emitActivity()
              playDynamicSFX(sound,0,1.0,math.max(-1,math.min(1,direction)),0.72)
              if eventType=="room_join" then say("انضم "..name.." إلى الغرفة.",false) elseif eventType=="room_leave" then say("غادر "..name.." الغرفة.",false) elseif eventType=="chat" then say("رسالة جديدة في الغرفة.",false) elseif eventType=="room_action" then say("تم تنفيذ إجراء غرفة: "..action..".",false) end
            end
            if currentSection=="PLAYING_MULTIPLAYER" and scheduleSynchronizedRaceEvent then scheduleSynchronizedRaceEvent(eventAtMs,emitActivity) else emitActivity() end
          end
        end
        Multiplayer.lastRoomActivityAtMs=newest
      end
      Handler().postDelayed(Runnable({run=poll}),1200)
    end)
  end
  poll()
end
function firebaseStopRoomActivityWatch() Multiplayer.roomActivityPollActive=false end

function firebasePublishRaceResult(result,score,callback)
  if not Multiplayer.currentRoom or Multiplayer.currentRoom=="" then if callback then callback(false) end; return end
  local key=firebaseEncodeKey(playerStats.username); local path="/rooms/"..firebaseEncodeKey(Multiplayer.currentRoom).."/results/"..key
  firebasePut(path,firebaseJsonObject({username=playerStats.username,result=result or "finished",score=score or 0,at=(firebaseServerNowSec and firebaseServerNowSec() or os.time()),raceSessionId=Multiplayer.raceSessionId or ""}),callback)
end

function firebaseWatchRoomRace()
  if Multiplayer.racePollActive then return end
  Multiplayer.racePollActive=true
  local function poll()
    if not Multiplayer.racePollActive or not Multiplayer.currentRoom or Multiplayer.currentRoom=="" or currentSection~="WAITING_ROOM" then Multiplayer.racePollActive=false; return end
    firebaseRefreshRoom(function(ok)
      if ok and currentSection=="WAITING_ROOM" and Multiplayer.raceStatus=="racing" and (Multiplayer.raceStartAt or 0)>0 then
        Multiplayer.waitingForPlayers=false; Multiplayer.racePollActive=false; loadSection("PLAYING_MULTIPLAYER"); return
      end
      Handler().postDelayed(Runnable({run=poll}),1000)
    end)
  end
  poll()
end

function firebaseStopRoomRaceWatch() Multiplayer.racePollActive=false end

function firebasePullSocial(callback)
  if not playerStats.sessionActive or playerStats.username == "" then if callback then callback(false) end; return end
  local doneFriends, doneRequests = false, false
  local function finish() if doneFriends and doneRequests then saveGameData(); if callback then callback(true) end end end
  firebaseGet("/friends/" .. firebaseEncodeKey(playerStats.username), function(ok, body)
    if ok then local root=newJsonObject(body); Multiplayer.friends=jsonList(root) end; doneFriends=true; finish()
  end)
  firebaseGet("/friendRequests/" .. firebaseEncodeKey(playerStats.username), function(ok, body)
    if ok then local root=newJsonObject(body); local requests={}; if root then local keys=root.keys(); while keys.hasNext() do table.insert(requests,tostring(keys.next())) end end; Multiplayer.friendRequests=requests end; doneRequests=true; finish()
  end)
end

function firebaseChangePassword(currentPassword, newPassword, callback)
  if playerStats.authProvider ~= "email" or playerStats.localId == "" then if callback then callback(false,"هذا الخيار متاح للحسابات البريدية فقط.") end; return end
  if not newPassword or #newPassword < 6 then if callback then callback(false,"كلمة المرور الجديدة يجب أن تكون ستة أحرف على الأقل.") end; return end
  firebaseGet(accountPath(playerStats.localId), function(ok, body)
    if not ok or not nonNull(body) then if callback then callback(false,"تعذر قراءة سجل الحساب.") end; return end
    local root=newJsonObject(body); local salt=optString(root,"passwordSalt",""); local stored=optString(root,"passwordHash","")
    if salt=="" or stored~=passwordDigest(currentPassword,salt) then if callback then callback(false,"كلمة المرور الحالية غير صحيحة.") end; return end
    local newSalt=makeSalt(); local newHash=passwordDigest(newPassword,newSalt)
    firebasePatch(accountPath(playerStats.localId), firebaseJsonObject({passwordSalt=newSalt,passwordHash=newHash,updatedAt=os.time()}), function(saved, response, code)
      if saved then playerStats.passwordSalt=newSalt; playerStats.passwordHash=newHash; saveGameData() end
      if callback then callback(saved, saved and "تم تغيير كلمة المرور." or "تعذر حفظ كلمة المرور الجديدة.", code) end
    end)
  end)
end


-- بطولات اللاعبين: بياناتها منفصلة عن الغرف العادية وقابلة للقراءة من تطبيق الإدارة.
function firebaseCreateTournament(name,duration,callback)
  name=tostring(name or ""):gsub("^%s+",""):gsub("%s+$","")
  if name=="" then if callback then callback(false,"أدخل اسم البطولة.") end; return end
  duration=math.max(60,math.min(720,math.floor(tonumber(duration) or Settings.raceDurationSeconds or 300)))
  local id="tournament_"..firebaseEncodeKey(playerStats.username).."_"..tostring(os.time())
  local payload={name=name,owner=playerStats.username,ownerName=playerStats.name,status="registration",createdAt=os.time(),maxPlayers=16,duration=duration,round=0,participants={}}
  payload.participants[playerStats.username]={username=playerStats.username,name=playerStats.name,joinedAt=os.time(),wins=0,eliminated=false}
  firebasePut("/tournaments/"..firebaseEncodeKey(id),firebaseJsonObject(payload),function(ok,body,code)
    if ok then Tournament.currentId=id; Tournament.currentName=name; Tournament.owner=playerStats.username; Tournament.status="registration"; Tournament.selectedDuration=duration; Tournament.participants={playerStats.username}; firebasePut("/adminFeed/tournaments/"..firebaseEncodeKey(id),firebaseJsonObject({name=name,owner=playerStats.username,status="registration",participants=1,maxPlayers=16,duration=duration,updatedAt=os.time()}),function() end) end
    if callback then callback(ok,ok and "تم إنشاء البطولة." or "تعذر إنشاء البطولة.",code) end
  end)
end

function firebaseListTournaments(callback)
  firebaseGet("/tournaments",function(ok,body)
    local list={}
    if ok and nonNull(body) then
      local root=newJsonObject(body); local keys=root and root.keys()
      while keys and keys.hasNext() do
        local id=tostring(keys.next()); local item=root.optJSONObject(id)
        if item then table.insert(list,{id=id,name=optString(item,"name",id),owner=optString(item,"owner",""),status=optString(item,"status","registration"),duration=optInt(item,"duration",300),maxPlayers=optInt(item,"maxPlayers",16),participants=optObj(item,"participants")}) end
      end
    end
    table.sort(list,function(a,b) return tostring(a.name)<tostring(b.name) end); Multiplayer.tournamentDirectory={}; for _,row in ipairs(list) do Multiplayer.tournamentDirectory[row.id]=row end
    if callback then callback(ok,list) end
  end)
end

function firebaseJoinTournament(id,callback)
  id=tostring(id or ""); if id=="" then if callback then callback(false,"لم يتم اختيار بطولة.") end; return end
  firebaseGet("/tournaments/"..firebaseEncodeKey(id),function(ok,body)
    if not ok or not nonNull(body) then if callback then callback(false,"البطولة غير موجودة.") end; return end
    local root=newJsonObject(body); local status=optString(root,"status","registration"); if status~="registration" then if callback then callback(false,"التسجيل في هذه البطولة مغلق.") end; return end
    local participants=optObj(root,"participants"); local count=0; local exists=false; local keys=participants and participants.keys()
    while keys and keys.hasNext() do local key=tostring(keys.next()); count=count+1; if key==playerStats.username then exists=true end end
    local maxPlayers=optInt(root,"maxPlayers",16); if not exists and count>=maxPlayers then if callback then callback(false,"البطولة مكتملة العدد.") end; return end
    if exists then Tournament.currentId=id; Tournament.currentName=optString(root,"name",id); Tournament.owner=optString(root,"owner",""); Tournament.status=status; Tournament.selectedDuration=optInt(root,"duration",300); if callback then callback(true,"أنت مسجل بالفعل في البطولة.") end; return end
    local payload=firebaseJsonObject({username=playerStats.username,name=playerStats.name,joinedAt=os.time(),wins=0,eliminated=false})
    firebasePut("/tournaments/"..firebaseEncodeKey(id).."/participants/"..firebaseEncodeKey(playerStats.username),payload,function(joined)
      if joined then Tournament.currentId=id; Tournament.currentName=optString(root,"name",id); Tournament.owner=optString(root,"owner",""); Tournament.status=status; Tournament.selectedDuration=optInt(root,"duration",300); playDynamicSFX("room_player_join",0,1,0,0.74); say("تم الانضمام إلى البطولة.",true) end
      if callback then callback(joined,joined and "تم تسجيلك في البطولة." or "تعذر الانضمام إلى البطولة.") end
    end)
  end)
end

function firebaseStartTournament(id,callback)
  id=tostring(id or Tournament.currentId or ""); if id=="" then if callback then callback(false,"لم يتم اختيار بطولة.") end; return end
  firebaseGet("/tournaments/"..firebaseEncodeKey(id),function(ok,body)
    if not ok or not nonNull(body) then if callback then callback(false,"البطولة غير موجودة.") end; return end
    local root=newJsonObject(body); if optString(root,"owner","")~=playerStats.username then if callback then callback(false,"مالك البطولة هو الذي يبدأها.") end; return end
    local patch=firebaseJsonObject({status="active",startedAt=os.time(),round=1,updatedAt=os.time()})
    firebasePatch("/tournaments/"..firebaseEncodeKey(id),patch,function(started) if started then Tournament.status="active"; Tournament.bracketRound=1 end; if callback then callback(started,started and "بدأت البطولة." or "تعذر بدء البطولة.") end end)
  end)
end

function firebaseLeaveTournament(callback)
  local id=Tournament.currentId; if not id or id=="" then if callback then callback(false,"لا توجد بطولة حالية.") end; return end
  firebaseDelete("/tournaments/"..firebaseEncodeKey(id).."/participants/"..firebaseEncodeKey(playerStats.username),function(ok)
    if ok then Tournament.currentId=""; Tournament.currentName=""; Tournament.status=""; Tournament.participants={} end
    if callback then callback(ok,ok and "غادرت البطولة." or "تعذر مغادرة البطولة.") end
  end)
end

-- طبقة المجتمع المتوافقة مع تدفق TableEx: إشعارات، رسائل خاصة، وحظر محلي/سحابي.
function firebaseAppendNotification(target, kind, text, data, callback)
  target=tostring(target or ""); if target=="" then if callback then callback(false) end; return end
  local key=tostring(os.time()).."_"..firebaseEncodeKey(playerStats.username).."_"..tostring(math.random(1000,9999))
  local payload={kind=tostring(kind or "general"),text=tostring(text or ""),from=playerStats.username,fromName=playerStats.name or playerStats.username,at=os.time(),read=false,data=data or {}}
  firebasePut("/notifications/"..firebaseEncodeKey(target).."/"..key,firebaseJsonObject(payload),callback)
end
function firebaseGetNotifications(callback)
  firebaseGet("/notifications/"..firebaseEncodeKey(playerStats.username),function(ok,body)
    local list={}
    if ok and nonNull(body) then
      local root=newJsonObject(body); local keys=root and root.keys()
      while keys and keys.hasNext() do local key=tostring(keys.next()); local item=root.optJSONObject(key); if item then table.insert(list,{id=key,kind=optString(item,"kind","general"),text=optString(item,"text",""),from=optString(item,"from",""),fromName=optString(item,"fromName",optString(item,"from","")),at=optInt(item,"at",0),read=optBool(item,"read",false),raw=item}) end end
    end
    table.sort(list,function(a,b) return (a.at or 0)>(b.at or 0) end); Multiplayer.notifications=list; if callback then callback(ok,list) end
  end)
end
function firebaseMarkNotificationRead(id,callback)
  if not id or id=="" then if callback then callback(false) end; return end
  firebasePatch("/notifications/"..firebaseEncodeKey(playerStats.username).."/"..tostring(id),firebaseJsonObject({read=true}),callback)
end
function firebaseGetPrivateMessages(callback)
  firebaseGet("/privateMessages/"..firebaseEncodeKey(playerStats.username),function(ok,body)
    local list={}
    if ok and nonNull(body) then
      local root=newJsonObject(body); local keys=root and root.keys()
      while keys and keys.hasNext() do local key=tostring(keys.next()); local item=root.optJSONObject(key); if item then table.insert(list,{id=key,from=optString(item,"from",""),fromName=optString(item,"name",optString(item,"from","")),to=optString(item,"to",playerStats.username),text=optString(item,"text",""),voice=optBool(item,"voice",false),voiceChannel=optString(item,"voiceChannel",""),voiceKey=optString(item,"voiceKey",""),sentAt=optInt(item,"sentAt",0),read=optBool(item,"read",false)}) end end
    end
    table.sort(list,function(a,b) return (a.sentAt or 0)>(b.sentAt or 0) end); Multiplayer.privateMessages=list; if callback then callback(ok,list) end
  end)
end
function firebaseMarkPrivateMessageRead(id,callback)
  if not id or id=="" then if callback then callback(false) end; return end
  firebasePatch("/privateMessages/"..firebaseEncodeKey(playerStats.username).."/"..tostring(id),firebaseJsonObject({read=true}),callback)
end
function firebaseBlockUser(target,callback)
  target=tostring(target or ""); if target=="" or target==playerStats.username then if callback then callback(false,"لا يمكن حظر هذا اللاعب.") end; return end
  firebasePut("/blocks/"..firebaseEncodeKey(playerStats.username).."/"..firebaseEncodeKey(target),firebaseJsonObject({username=target,at=os.time()}),function(ok,body,code)
    if ok then Multiplayer.blockedUsers=Multiplayer.blockedUsers or {}; Multiplayer.blockedUsers[target]=true; firebaseAppendNotification(target,"blocked","قام لاعب بحظرك.",{},function() end) end
    if callback then callback(ok,body,code) end
  end)
end
function firebaseUnblockUser(target,callback)
  target=tostring(target or ""); firebaseDelete("/blocks/"..firebaseEncodeKey(playerStats.username).."/"..firebaseEncodeKey(target),function(ok,body,code) if ok and Multiplayer.blockedUsers then Multiplayer.blockedUsers[target]=nil end; if callback then callback(ok,body,code) end end)
end
function firebaseGetBlockedUsers(callback)
  firebaseGet("/blocks/"..firebaseEncodeKey(playerStats.username),function(ok,body)
    local result={}; if ok and nonNull(body) then local root=newJsonObject(body); local keys=root and root.keys(); while keys and keys.hasNext() do table.insert(result,tostring(keys.next())) end end
    Multiplayer.blockedUsers={}; for _,username in ipairs(result) do Multiplayer.blockedUsers[username]=true end; if callback then callback(ok,result) end
  end)
end

function firebaseStartCommunityPolling()
  if Multiplayer.communityPollActive then return end
  Multiplayer.communityPollActive=true; Multiplayer.lastNotificationCount=0; Multiplayer.lastPrivateMessageCount=0
  local first=true
  local function poll()
    if not Multiplayer.communityPollActive or not playerStats.sessionActive then Multiplayer.communityPollActive=false; return end
    firebaseGetNotifications(function(ok,notifications)
      if ok and not first and #notifications>(Multiplayer.lastNotificationCount or 0) then playDynamicSFX("social_online",0,1,0,0.70); say("إشعار اجتماعي جديد.",false) end
      Multiplayer.lastNotificationCount=#notifications
      firebaseGetPrivateMessages(function(msgOk,messages)
        if msgOk and not first and #messages>(Multiplayer.lastPrivateMessageCount or 0) then
          local latest=messages[1] or {}; local sender=tostring(latest.fromName or latest.from or "صديقك"); local preview=tostring(latest.text or ""); if #preview>120 then preview=preview:sub(1,120).."..." end
          playDynamicSFX("chat_receive_soft",0,1,0,0.86); if latest.voice then say("رسالة صوتية جديدة من "..sender..". اضغط عليها لتشغيلها.",false) else say("رسالة جديدة من "..sender..(preview~="" and (": " .. preview) or ""),false) end
        end
        Multiplayer.lastPrivateMessageCount=#messages; first=false; Handler().postDelayed(Runnable({run=poll}),5000)
      end)
    end)
  end
  poll()
end
function firebaseStopCommunityPolling() Multiplayer.communityPollActive=false end

function firebaseRejectFriendRequest(target,callback)
  target=tostring(target or ""); if target=="" then if callback then callback(false,"طلب غير صالح.") end; return end
  firebaseDelete("/friendRequests/"..firebaseEncodeKey(playerStats.username).."/"..firebaseEncodeKey(target),function(ok,body,code)
    if ok then firebaseAppendNotification(target,"friend_reject","تم رفض طلب الصداقة.",{from=playerStats.username},function() end) end
    if callback then callback(ok,body,code) end
  end)
end

-- ساعة موحّدة للغرف والسباق: لا يبدأ العميل من لحظة وصول الرسالة، بل من موعد الخادم.
function firebaseServerNowMs()
  local offset=tonumber(Multiplayer.serverTimeOffsetMs or 0) or 0
  return (os.time()*1000)+offset
end
function firebaseServerNowSec() return math.floor(firebaseServerNowMs()/1000) end
function firebaseSyncServerClock(callback)
  local started=System.currentTimeMillis(); firebaseGet("/",function(ok,body,code)
    local latency=System.currentTimeMillis()-started
    if ok then Multiplayer.serverTimeOffsetMs=0; Multiplayer.serverClockReady=true; Multiplayer.lastServerClockAt=os.time(); Multiplayer.lastLatencyMs=latency; Multiplayer.serverStatus=latency<180 and "ممتاز" or (latency<600 and "جيد" or "ضعيف"); if callback then callback(true,0,code) end
    else Multiplayer.serverClockReady=false; Multiplayer.serverStatus="غير متصل"; if callback then callback(false,0,code) end end
  end)
end

function scheduleSynchronizedRaceEvent(eventAtMs,emit)
  local now=(firebaseServerNowMs and firebaseServerNowMs()) or (os.time()*1000); local target=tonumber(eventAtMs or 0) or 0; local delay=target>0 and (target+450-now) or 0; delay=math.max(0,math.min(1200,delay)); Handler().postDelayed(Runnable({run=function() if currentSection=="PLAYING_MULTIPLAYER" then pcall(emit) end end}),delay)
end

-- خدمات الإصدار الموسع: جاهزية الغرفة، idempotency، الاقتصاد، التقارير، والتحديث عن بعد.
function firebaseSetPlayerReady(ready,callback)
  if not Multiplayer.currentRoom or Multiplayer.currentRoom=="" or not playerStats.username or playerStats.username=="" then if callback then callback(false,"لا توجد غرفة.") end; return end
  local value=ready==true; Multiplayer.playerReady=value; local room=firebaseEncodeKey(Multiplayer.currentRoom); local user=firebaseEncodeKey(playerStats.username); local stamp=(firebaseServerNowSec and firebaseServerNowSec() or os.time())
  firebasePatch("/rooms/"..room.."/readiness/"..user,firebaseJsonObject({username=playerStats.username,ready=value,updatedAt=stamp}),function(ok,body,code) if ok then firebaseAppendRoomLog(Multiplayer.currentRoom,value and "أصبح اللاعب جاهزًا." or "ألغى اللاعب جاهزيته.") elseif wrQueue then wrQueue("ready",{ready=value}) end; if callback then callback(ok,body,code) end end)
end
function firebaseRefreshConnectionQuality(callback)
  local started=System.currentTimeMillis(); firebaseGet("/",function(ok,body,code) local latency=System.currentTimeMillis()-started; if ok then Multiplayer.serverTimeOffsetMs=0; Multiplayer.serverClockReady=true; Multiplayer.lastLatencyMs=latency; Multiplayer.serverStatus=latency<180 and "ممتاز" or (latency<600 and "جيد" or "ضعيف"); if wrSetConnection then wrSetConnection(Multiplayer.serverStatus,latency,"rest_root") end; if firebaseFlushPendingActions then firebaseFlushPendingActions(function() end) end else Multiplayer.serverStatus="غير متصل"; if wrSetConnection then wrSetConnection("غير متصل",latency,"rest_root") end end; if callback then callback(ok,latency,code) end end)
end
function firebaseRecordEconomyLedger(kind,delta,reason,operationId,callback)
  if not playerStats.username or playerStats.username=="" then if callback then callback(false,"لا يوجد لاعب.") end; return end
  local key=tostring(operationId or (wrUniqueId and wrUniqueId("economy") or (tostring(os.time()).."_"..tostring(math.random(1000,9999))))); if wrMarkProcessed and not wrMarkProcessed(key) then if callback then callback(true,"تمت معالجة العملية سابقًا.",key) end; return end
  local entry={id=key,kind=tostring(kind or "adjustment"),delta=tonumber(delta) or 0,before=tonumber(playerStats.money or 0)-(tonumber(delta) or 0),after=tonumber(playerStats.money or 0),reason=tostring(reason or ""),at=(firebaseServerNowSec and firebaseServerNowSec() or os.time()),username=playerStats.username}
  firebasePut("/economyLedger/"..firebaseEncodeKey(playerStats.username).."/"..firebaseEncodeKey(key),firebaseJsonObject(entry),function(ok,body,code) if ok and firebaseSyncProfile then firebaseSyncProfile(function() end) end; if callback then callback(ok,body,code,key) end end)
end
function firebaseClaimIdempotentReward(claimKey,reward,reason,callback)
  claimKey=tostring(claimKey or ""); if claimKey=="" or not playerStats.username or playerStats.username=="" then if callback then callback(false,"بيانات المكافأة غير مكتملة.") end; return end
  local path="/rewardClaims/"..firebaseEncodeKey(playerStats.username).."/"..firebaseEncodeKey(claimKey)
  firebaseGet(path,function(readOk,body) if readOk and nonNull(body) then if callback then callback(true,"المكافأة مستلمة سابقًا.",true) end; return end; local payload=firebaseJsonObject({claimKey=claimKey,reward=reward or {},reason=reason or "",at=(firebaseServerNowSec and firebaseServerNowSec() or os.time())}); firebasePut(path,payload,function(ok,putBody,code) if callback then callback(ok,ok and "تم تسجيل المكافأة." or "تعذر تسجيل المكافأة.",false,code) end end) end)
end
function firebasePublishClientTelemetry(kind,details,callback)
  if not playerStats.sessionActive or not playerStats.username or playerStats.username=="" then if callback then callback(false) end; return end
  local id=wrUniqueId and wrUniqueId("telemetry") or (tostring(os.time()).."_"..tostring(math.random(1000,9999))); local payload=firebaseJsonObject({id=id,kind=tostring(kind or "event"),details=details or {},at=(firebaseServerNowSec and firebaseServerNowSec() or os.time()),appVersion=tostring(PRO_VERSION or ""),username=playerStats.username}); firebasePut("/adminFeed/telemetry/"..firebaseEncodeKey(playerStats.username).."/"..firebaseEncodeKey(id),payload,callback)
end
function firebaseReportPlayer(target,reason,callback)
  target=tostring(target or ""); if target=="" or target==playerStats.username then if callback then callback(false,"لا يمكن الإبلاغ عن هذا اللاعب.") end; return end
  local id=wrUniqueId and wrUniqueId("report") or (tostring(os.time()).."_"..tostring(math.random(1000,9999))); local report={id=id,from=playerStats.username,target=target,reason=tostring(reason or "غير محدد"),room=Multiplayer.currentRoom or "",at=(firebaseServerNowSec and firebaseServerNowSec() or os.time()),status="open"}; firebasePut("/moderationReports/"..firebaseEncodeKey(id),firebaseJsonObject(report),callback)
end
function firebaseSaveRaceSnapshot(snapshot,callback)
  if not Multiplayer.currentRoom or Multiplayer.currentRoom=="" or not playerStats.username or playerStats.username=="" then if callback then callback(false) end; return end
  local id=wrUniqueId and wrUniqueId("snapshot") or tostring(os.time()); local data=snapshot or {}; data.id=id; data.username=playerStats.username; data.sessionId=Multiplayer.raceSessionId or ""; data.atMs=firebaseServerNowMs and firebaseServerNowMs() or os.time()*1000; firebasePut("/rooms/"..firebaseEncodeKey(Multiplayer.currentRoom).."/snapshots/"..firebaseEncodeKey(playerStats.username),firebaseJsonObject(data),callback)
end
function firebasePublishRaceCheckpoint(checkpoint,distance,callback)
  firebasePublishRaceEvent("checkpoint",{checkpoint=tostring(checkpoint or ""),distance=distance or GameState.distance or 0},callback)
end
function firebaseGetRemoteConfig(callback)
  firebaseGet("/remoteConfig/WorldRacing",function(ok,body,code) local root=ok and nonNull(body) and newJsonObject(body) or nil; if root and wrApplyFeatureFlags then local flags=optObj(root,"flags"); if flags then local keys=flags.keys(); while keys and keys.hasNext() do local key=tostring(keys.next()); wrSetFeatureFlag(key,optBool(flags,key,false)) end end end; if callback then callback(ok,root,code) end end)
end
function firebaseGetUpdateManifest(callback)
  firebaseGet("/remoteConfig/update",function(ok,body,code) if callback then callback(ok,ok and newJsonObject(body) or nil,code) end end)
end
function firebaseCleanupRoomTemporaryMedia(roomName,callback)
  roomName=tostring(roomName or Multiplayer.currentRoom or ""); if roomName=="" then if callback then callback(false) end; return end
  firebaseDelete("/voiceLive/room_"..firebaseEncodeKey(roomName),function() firebaseDelete("/voiceMessages/room_"..firebaseEncodeKey(roomName),function() if callback then callback(true) end end) end)
end

function firebaseEnforceVoiceStorageLimit(channel)
  channel=tostring(channel or ""); if channel=="" or not playerStats.username or playerStats.username=="" then return end
  local limit=tonumber(playerStats.voiceStorageLimitBytes or 20971520) or 20971520
  firebaseGet("/voiceMessages/"..channel,function(ok,body)
    if not ok or not nonNull(body) then return end
    local root=newJsonObject(body); local keys=root and root.keys(); local own={}; local total=0
    while keys and keys.hasNext() do local key=tostring(keys.next()); local item=root.optJSONObject(key); local from=item and optString(item,"from","") or ""; local data=item and optString(item,"data","") or ""; local bytes=math.floor(#data*0.75); if from==playerStats.username then total=total+bytes; table.insert(own,{key=key,bytes=bytes,at=tonumber(item.optDouble("sentAt",0)) or 0}) end end
    table.sort(own,function(a,b) if a.at==b.at then return a.key<b.key end return a.at<b.at end)
    local index=1; local function trimNext()
      if total<=limit or not own[index] then playerStats.voiceStorageBytes=math.max(0,total); saveGameData(); if firebaseSyncProfile then firebaseSyncProfile(function() end) end; return end
      local item=own[index]; index=index+1; total=math.max(0,total-item.bytes); firebaseDelete("/voiceMessages/"..channel.."/"..firebaseEncodeKey(item.key),function() trimNext() end)
    end
    trimNext()
  end)
end

function firebaseFlushPendingActions(callback)
  if not wrFlushQueue then if callback then callback(false) end; return end
  wrFlushQueue(function(item,done)
    local payload=item.payload or {}
    if item.action=="ready" then firebaseSetPlayerReady(payload.ready==true,done) elseif item.action=="telemetry" then firebasePublishClientTelemetry(payload.kind,payload.details,done) elseif item.action=="report" then firebaseReportPlayer(payload.target,payload.reason,done) else done(false) end
  end)
  if callback then callback(true) end
end
