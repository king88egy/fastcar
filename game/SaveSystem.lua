-- SaveSystem.lua
-- حفظ محلي للحالة، بينما بيانات الحساب السحابية تُدار عبر Firebase Authentication.
local prefs = activity.getSharedPreferences("HackerCenterProSettings", Context.MODE_PRIVATE)

local function saveList(editor, key, list)
  local values = {}
  for _, value in ipairs(list or {}) do table.insert(values, tostring(value)) end
  editor.putString(key, table.concat(values, ","))
end

local function loadList(key)
  local result, value = {}, prefs.getString(key, "")
  if value ~= "" then for item in string.gmatch(value, "([^,]+)") do table.insert(result, item) end end
  return result
end

function saveGameData()
  local editor = prefs.edit()
  editor.putFloat("version", PRO_VERSION); editor.putInt("schemaVersion", 4)
  editor.putString("name", playerStats.name or "")
  editor.putString("username", playerStats.username or "")
  editor.putString("accountEmail", playerStats.accountEmail or "")
  editor.putString("authProvider", playerStats.authProvider or "")
  editor.putString("idToken", playerStats.idToken or "")
  editor.putBoolean("sessionActive", playerStats.sessionActive == true)
  editor.putString("passwordSalt", playerStats.passwordSalt or "")
  editor.putString("passwordHash", playerStats.passwordHash or "")
  editor.putString("googleFallbackEmail", playerStats.googleFallbackEmail or "")
  editor.putString("googleFallbackPassword", playerStats.googleFallbackPassword or "")
  editor.putString("localId", playerStats.localId or "")
  editor.putString("refreshToken", playerStats.refreshToken or "")
  editor.putString("gender", playerStats.gender or "")
  editor.putString("status", playerStats.status or playerStats.bio or "")
  editor.putString("bio", playerStats.bio or "متسابق محترف")
  editor.putInt("money", math.floor(playerStats.money or 0)); editor.putInt("level", math.floor(playerStats.level or 1))
  editor.putInt("reputation", math.floor(playerStats.reputation or 0)); editor.putInt("wins", math.floor(playerStats.wins or 0))
  editor.putInt("bestScore", math.floor(playerStats.bestScore or 0)); editor.putInt("bestDistance", math.floor(playerStats.bestDistance or 0)); editor.putInt("bestSpeed", math.floor(playerStats.bestSpeed or 0)); editor.putInt("leaderboardOvertakes", math.floor(playerStats.leaderboardOvertakes or 0)); editor.putBoolean("leaderboardQualified", playerStats.leaderboardQualified==true); editor.putBoolean("leaderboardFinalRewarded", playerStats.leaderboardFinalRewarded==true)
  editor.putInt("shields", math.floor(playerStats.shields or 0)); editor.putInt("emps", math.floor(playerStats.emps or 0))
  editor.putInt("drones", math.floor(playerStats.drones or 0)); editor.putInt("fuelTanks", math.floor(playerStats.fuelTanks or 0))
  editor.putInt("damageEngine", math.floor((playerStats.carDamage and playerStats.carDamage.engine) or 0)); editor.putInt("damageTires", math.floor((playerStats.carDamage and playerStats.carDamage.tires) or 0)); editor.putInt("damageBody", math.floor((playerStats.carDamage and playerStats.carDamage.body) or 0)); editor.putInt("damageBrakes", math.floor((playerStats.carDamage and playerStats.carDamage.brakes) or 0))
  editor.putInt("nitro", math.floor(playerStats.nitro or 0)); editor.putInt("repairKits", math.floor(playerStats.repairKits or 0)); editor.putInt("rewardChests", math.floor(playerStats.rewardChests or 0)); editor.putLong("wheelLastSpinAt", math.floor(playerStats.wheelLastSpinAt or 0))
  editor.putInt("skillPoints", math.floor(playerStats.skillPoints or 0))
  editor.putInt("armorBonus", math.floor(playerStats.skills.armorBonus or 0)); editor.putInt("nitroBonus", math.floor(playerStats.skills.nitroBonus or 0))
  editor.putFloat("moneyMultiplier", playerStats.skills.moneyMultiplier or 1.0); editor.putInt("activeCarId", activeCarId or 1)
  saveList(editor, "inventory", Inventory)
  editor.putFloat("vSpeed", Settings.voiceSpeed or 3.15); editor.putFloat("vPitch", Settings.voicePitch or 1.0); editor.putInt("lastRaceSpeed", math.floor(GameState.lastRaceSpeed or 0)); editor.putInt("lastRaceDistance", math.floor(GameState.lastRaceDistance or 0)); editor.putInt("lastRaceFuel", math.floor(GameState.lastRaceFuel or 100)); editor.putInt("lastRaceCrates", math.floor(GameState.lastRaceCrates or 0)); editor.putInt("lastRaceCollisionCount", math.floor(GameState.lastRaceCollisionCount or 0)); editor.putString("lastRaceWeather", tostring(GameState.lastRaceWeather or "صافي")); editor.putString("lastRaceRoad", tostring(GameState.lastRaceRoad or "جاف"))
  editor.putBoolean("ttsEnabled", Settings.ttsEnabled ~= false); editor.putBoolean("sfxEnabled", Settings.sfxEnabled ~= false); editor.putFloat("sfxVolume", Settings.sfxVolume or 1.0)
  editor.putBoolean("bgmEnabled", Settings.bgmEnabled ~= false); editor.putBoolean("announcerEnabled", Settings.announcerEnabled ~= false); editor.putBoolean("vibrationEnabled", Settings.vibrationEnabled ~= false)
  editor.putFloat("bgmVolume", Settings.bgmVolume or 0.03); editor.putFloat("announcerVolume", Settings.announcerVolume or 0.20); editor.putBoolean("powerSaveMode", Settings.powerSaveMode == true)
  editor.putBoolean("visibleText", Settings.visibleText ~= false); editor.putString("accessibilityMode", Settings.accessibilityMode or "وضع المكفوفين")
  editor.putString("keyboardMode", Settings.keyboardMode or "نظام"); editor.putString("controlMode", Settings.controlMode or "الإيماءات والأزرار"); editor.putString("orientationMode", Settings.orientationMode or "أفقي"); editor.putString("steeringMode", Settings.steeringMode or "السحب"); editor.putFloat("tiltSensitivity", Settings.tiltSensitivity or 2.4); editor.putFloat("tiltDeadZone", Settings.tiltDeadZone or 0.65); editor.putFloat("tiltCenter", Settings.tiltCenter or 0); editor.putBoolean("invertTilt", Settings.invertTilt == true); editor.putInt("warningSeconds", Settings.warningSeconds or 3)
  editor.putString("radarQuality", Settings.radarQuality or "عالية"); editor.putBoolean("doubleTapMenus", Settings.doubleTapMenus == true); editor.putBoolean("raceRealismEnabled", Settings.raceRealismEnabled ~= false); editor.putBoolean("opponentProximityEnabled", Settings.opponentProximityEnabled ~= false); editor.putInt("periodicRaceReportSeconds", math.max(10,math.min(60,math.floor(Settings.periodicRaceReportSeconds or 15))) ); editor.putString("language", Settings.language or "العربية"); editor.putBoolean("languageSelected", Settings.languageSelected == true)
  editor.putBoolean("showActionButtons", Settings.showActionButtons ~= false); editor.putBoolean("autoUpdateCheck", Settings.autoUpdateCheck ~= false); editor.putInt("raceDurationSeconds", math.max(60,math.min(720,math.floor(Settings.raceDurationSeconds or 300)))); editor.putBoolean("usageCardsSeen", Settings.usageCardsSeen == true); editor.putBoolean("dailyClaimed", playerStats.dailyClaimed == true); editor.putLong("wheelLastSpinAt", math.floor(playerStats.wheelLastSpinAt or 0)); editor.putBoolean("smartAnnouncerEnabled", Settings.smartAnnouncerEnabled ~= false); editor.putBoolean("termsAccepted", Settings.termsAccepted == true)
  local achievementJson=JSONObject(); for id,row in pairs(Achievements or {}) do local item=JSONObject(); item.put("progress",tonumber(row.progress) or 0); item.put("completed",row.completed==true); item.put("completedAt",tonumber(row.completedAt) or 0); item.put("rewardClaimed",row.rewardClaimed==true); achievementJson.put(tostring(id),item) end; editor.putString("achievements",achievementJson.toString())
  saveList(editor, "friendsList", Multiplayer.friends); saveList(editor, "friendRequests", Multiplayer.friendRequests); saveList(editor, "sentRequests", Multiplayer.sentRequests)
  local blockedList={}; for username,_ in pairs(Multiplayer.blockedUsers or {}) do table.insert(blockedList,username) end; saveList(editor,"blockedUsers",blockedList)
  editor.commit()
end

function loadGameData()
  local savedVersion = prefs.getFloat("version", 0.0); Multiplayer.schemaVersion=prefs.getInt("schemaVersion",1)
  if savedVersion > 0.0 and savedVersion < PRO_VERSION then
    prefs.edit().putFloat("version", PRO_VERSION).putBoolean("termsAccepted", false).commit()
    Settings.termsAccepted=false
  end
  playerStats.name = prefs.getString("name", ""); playerStats.username = prefs.getString("username", "")
  playerStats.accountEmail = prefs.getString("accountEmail", ""); playerStats.authProvider = prefs.getString("authProvider", "")
  playerStats.idToken = prefs.getString("idToken", ""); playerStats.localId = prefs.getString("localId", ""); playerStats.refreshToken = prefs.getString("refreshToken", "")
  playerStats.sessionActive = prefs.getBoolean("sessionActive", false)
  playerStats.passwordSalt = prefs.getString("passwordSalt", ""); playerStats.passwordHash = prefs.getString("passwordHash", ""); playerStats.googleFallbackEmail = prefs.getString("googleFallbackEmail", ""); playerStats.googleFallbackPassword = prefs.getString("googleFallbackPassword", "")
  playerStats.gender = prefs.getString("gender", ""); playerStats.status = prefs.getString("status", prefs.getString("bio", "متسابق محترف")); playerStats.bio = prefs.getString("bio", playerStats.status)
  playerStats.money = prefs.getInt("money", 15000); playerStats.level = prefs.getInt("level", 1); playerStats.reputation = prefs.getInt("reputation", 0)
  playerStats.wins = prefs.getInt("wins", 0); playerStats.bestScore = prefs.getInt("bestScore", 0); playerStats.bestDistance = prefs.getInt("bestDistance", 0); playerStats.bestSpeed = prefs.getInt("bestSpeed", 0); playerStats.leaderboardOvertakes=prefs.getInt("leaderboardOvertakes",0); playerStats.leaderboardQualified=prefs.getBoolean("leaderboardQualified",false); playerStats.leaderboardFinalRewarded=prefs.getBoolean("leaderboardFinalRewarded",false)
  playerStats.shields = prefs.getInt("shields", 1); playerStats.emps = prefs.getInt("emps", 1); playerStats.drones = prefs.getInt("drones", 1)
  playerStats.fuelTanks = prefs.getInt("fuelTanks", 3); playerStats.nitro = prefs.getInt("nitro", 2); playerStats.repairKits = prefs.getInt("repairKits", 1); playerStats.rewardChests = prefs.getInt("rewardChests", 0); playerStats.wheelLastSpinAt = prefs.getLong("wheelLastSpinAt", 0)
  playerStats.carDamage = {engine=prefs.getInt("damageEngine",0), tires=prefs.getInt("damageTires",0), body=prefs.getInt("damageBody",0), brakes=prefs.getInt("damageBrakes",0)}
  playerStats.skillPoints = prefs.getInt("skillPoints", 0); playerStats.skills.armorBonus = prefs.getInt("armorBonus", 0)
  playerStats.skills.nitroBonus = prefs.getInt("nitroBonus", 0); playerStats.skills.moneyMultiplier = prefs.getFloat("moneyMultiplier", 1.0)
  activeCarId = prefs.getInt("activeCarId", 1); playerStats.dailyClaimed = prefs.getBoolean("dailyClaimed", false)
  Settings.voiceSpeed = prefs.getFloat("vSpeed", 3.15); if Settings.voiceSpeed<=0 then Settings.voiceSpeed=3.15 end; Settings.voicePitch = prefs.getFloat("vPitch", 1.0); GameState.lastRaceSpeed=prefs.getInt("lastRaceSpeed",0); GameState.lastRaceDistance=prefs.getInt("lastRaceDistance",0); GameState.lastRaceFuel=prefs.getInt("lastRaceFuel",100); GameState.lastRaceCrates=prefs.getInt("lastRaceCrates",0); GameState.lastRaceCollisionCount=prefs.getInt("lastRaceCollisionCount",0); GameState.lastRaceWeather=prefs.getString("lastRaceWeather","صافي"); GameState.lastRaceRoad=prefs.getString("lastRaceRoad","جاف")
  Settings.ttsEnabled = prefs.getBoolean("ttsEnabled", true); Settings.sfxEnabled = prefs.getBoolean("sfxEnabled", true); Settings.sfxVolume = prefs.getFloat("sfxVolume", 1.0); Settings.bgmEnabled = prefs.getBoolean("bgmEnabled", true); Settings.announcerEnabled = prefs.getBoolean("announcerEnabled", true)
  Settings.vibrationEnabled = prefs.getBoolean("vibrationEnabled", true); Settings.bgmVolume = prefs.getFloat("bgmVolume", 0.03); Settings.announcerVolume = prefs.getFloat("announcerVolume", 0.20); Settings.powerSaveMode = prefs.getBoolean("powerSaveMode", false)
  Settings.visibleText = prefs.getBoolean("visibleText", true); Settings.accessibilityMode = prefs.getString("accessibilityMode", "وضع المكفوفين"); if Settings.accessibilityMode=="الوضع المختلط" or Settings.accessibilityMode=="الوضع المرئي" then Settings.accessibilityMode="وضع المبصرين" end
  Settings.keyboardMode = prefs.getString("keyboardMode", "نظام"); Settings.controlMode = prefs.getString("controlMode", "الإيماءات والأزرار"); Settings.orientationMode = prefs.getString("orientationMode", "أفقي"); Settings.steeringMode = prefs.getString("steeringMode", "السحب"); if Settings.steeringMode~="السحب" and Settings.steeringMode~="الإمالة" and Settings.steeringMode~="مختلط" then Settings.steeringMode="السحب" end; Settings.tiltSensitivity = math.max(0.15,math.min(8,prefs.getFloat("tiltSensitivity",2.4))); Settings.tiltDeadZone = math.max(0.05,math.min(4,prefs.getFloat("tiltDeadZone",0.65))); Settings.tiltCenter = prefs.getFloat("tiltCenter",0); Settings.invertTilt = prefs.getBoolean("invertTilt",false); Settings.warningSeconds = prefs.getInt("warningSeconds", 3); Settings.radarQuality = prefs.getString("radarQuality", "عالية"); Settings.raceRealismEnabled = prefs.getBoolean("raceRealismEnabled", true); Settings.opponentProximityEnabled = prefs.getBoolean("opponentProximityEnabled", true); Settings.periodicRaceReportSeconds = math.max(10,math.min(60,prefs.getInt("periodicRaceReportSeconds",15)))
  Settings.doubleTapMenus = prefs.getBoolean("doubleTapMenus", Settings.accessibilityMode == "وضع المكفوفين"); if Settings.accessibilityMode == "وضع المكفوفين" then Settings.doubleTapMenus = true end; Settings.showActionButtons = prefs.getBoolean("showActionButtons", true); Settings.autoUpdateCheck = prefs.getBoolean("autoUpdateCheck", true); Settings.raceDurationSeconds=math.max(60,math.min(720,prefs.getInt("raceDurationSeconds",300))); Settings.usageCardsSeen=prefs.getBoolean("usageCardsSeen",false); Settings.smartAnnouncerEnabled = prefs.getBoolean("smartAnnouncerEnabled", true); Settings.termsAccepted = prefs.getBoolean("termsAccepted", false); Settings.language=prefs.getString("language","العربية"); Settings.languageSelected=prefs.getBoolean("languageSelected",false)
  local storedAchievements=prefs.getString("achievements",""); if storedAchievements~="" then pcall(function() local root=JSONObject(storedAchievements); Achievements={}; local keys=root.keys(); while keys.hasNext() do local id=tostring(keys.next()); local item=root.optJSONObject(id); if item then Achievements[id]={progress=tonumber(item.optDouble("progress",0)) or 0,completed=item.optBoolean("completed",false),completedAt=tonumber(item.optDouble("completedAt",0)) or 0,rewardClaimed=item.optBoolean("rewardClaimed",false)} end end end) end
  local invStr = prefs.getString("inventory", "1"); Inventory = {}
  if invStr ~= "" then for value in string.gmatch(invStr, "([^,]+)") do local id = tonumber(value); if id then table.insert(Inventory, id) end end end
  if #Inventory == 0 then Inventory = {1} end
  Multiplayer.friends = loadList("friendsList"); Multiplayer.friendRequests = loadList("friendRequests"); Multiplayer.sentRequests = loadList("sentRequests"); Multiplayer.blockedUsers={}; for _,username in ipairs(loadList("blockedUsers")) do Multiplayer.blockedUsers[username]=true end
end

loadGameData()
