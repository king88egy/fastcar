-- Database.lua
-- الحالة المشتركة للتطبيق. لا تُعتبر الجلسة السحابية صالحة إلا بعد مصادقة صريحة.
PRO_VERSION = 13.8

playerStats = {
  name = "", username = "", bio = "متسابق محترف", accountEmail = "",
  authProvider = "", idToken = "", localId = "", refreshToken = "", sessionActive = false, passwordSalt = "", passwordHash = "", googleFallbackEmail = "", googleFallbackPassword = "",
  gender = "", status = "متسابق محترف", money = 15000, level = 1,
  reputation = 0, wins = 0, bestScore = 0, bestDistance = 0, bestSpeed = 0, leaderboardOvertakes = 0, leaderboardQualified = false, leaderboardFinalRewarded = false,
  shields = 1, emps = 1, drones = 1, fuelTanks = 3, carDamage = {engine=0, tires=0, body=0, brakes=0},
  nitro = 2, repairKits = 1, dailyClaimed = false, wheelLastSpinAt = 0,
  skills = {armorBonus = 0, nitroBonus = 0, moneyMultiplier = 1}, rewardChests = 0, voiceStorageBytes = 0, voiceStorageLimitBytes = 20971520
}
Inventory = {1}
activeCarId = 1

Settings = {
  ttsEnabled = true, voiceSpeed = 3.15, voicePitch = 1.0,
  sfxEnabled = true, sfxVolume = 1.0, bgmEnabled = true, bgmVolume = 0.03, announcerEnabled = true, announcerVolume = 0.20, vibrationEnabled = true,
  accessibilityMode = "وضع المكفوفين", keyboardMode = "نظام", controlMode = "الإيماءات والأزرار", orientationMode = "أفقي",
  warningSeconds = 3, visibleText = true, powerSaveMode = false, steeringMode = "السحب", tiltSensitivity = 2.4, tiltDeadZone = 0.65, tiltCenter = 0, invertTilt = false,
  radarQuality = "عالية", doubleTapMenus = true, showActionButtons = true, autoUpdateCheck = true, raceDurationSeconds = 300, smartAnnouncerEnabled = true, raceRealismEnabled = true, opponentProximityEnabled = true, periodicRaceReportSeconds = 15, termsAccepted = false, usageCardsSeen = false, language = "العربية", languageSelected = false
}

Multiplayer = {
  friends = {}, friendRequests = {}, sentRequests = {}, gameInvites = {}, sentGameInvites = {}, notifications = {}, privateMessages = {}, blockedUsers = {}, onlineFriends = {}, publicRooms = {}, remoteProfiles = {},
  currentRoom = "", roomMode = "عامة", roomOwner = "", roomStatus = "", roomMusicUrl = "", roomMusicPlaying = false, roomMusicUpdatedAt = 0,
  roomPlayers = {}, readyPlayers = {}, playerReady = false, mutedPlayers = {}, bannedPlayers = {}, roomModerators = {}, roomLocked = false, roomMaxPlayers = 4, chatMessages = {},
  directCallTarget = "", voiceChannel = "", isRecordingVoice = false,
  isMicEnabled = true, waitingForPlayers = false, opponentDistance = 0, botEnabled = false, botName = "", botSkill = 0.55, bots = {}, roomMuteAll = false, roomPaused = false, pauseUpdatedAt = 0,
  lastSyncOk = false, serverStatus = "غير متصل", nearbyCars = 0, roomActionLog = {}, serverTimeOffsetMs = 0, serverClockReady = false, lastServerClockAt = 0,
  raceStatus = "waiting", raceStartAt = 0, raceStartAtMs = 0, raceSessionId = "", raceSeed = 0, raceGoal = 0, selectedDuration = 300, remoteRacePlayers = {}, racePollActive = false,
  selectedPlayer = "", lastChatAt = 0, invitePollActive = false, presencePollActive = false, onlinePresencePollActive = false, raceEventPollActive = false, lastRaceEventAt = 0, lastRaceEventAtMs = 0, processedRaceEvents = {}, roomActivityPollActive = false, lastRoomActivityAtMs = 0, processedRoomActivities = {}, sentInvitePollActive = false, privateMatch = false, privateMatchId = "", tournamentDirectory = {}, tournamentParticipants = {}, pendingNetworkActions = {}, lastLatencyMs = 0, connectionSource = "local", schemaVersion = 5
}

Tournament = {currentId="", currentName="", owner="", status="", selectedDuration=300, participants={}, maxPlayers=16, bracketRound=0}

LevelsData = {
  {name="المرحلة الأولى: البداية", goal=300, obstacleRate=0.04, difficulty="سهلة", reward=1000, hasBoss=false},
  {name="المرحلة الثانية: التحدي", goal=600, obstacleRate=0.10, difficulty="متوسطة", reward=3000, hasBoss=false},
  {name="المرحلة الثالثة: الاحتراف", goal=1000, obstacleRate=0.18, difficulty="صعبة", reward=8000, hasBoss=false},
  {name="المرحلة الرابعة: الخطر", goal=1500, obstacleRate=0.28, difficulty="صعبة جداً", reward=15000, hasBoss=false},
  {name="المرحلة الخامسة: الأسطورة", goal=2500, obstacleRate=0.45, difficulty="الأصعب", reward=50000, hasBoss=true, bossSpeed=450, bossName="الزعيم الأخير"}
}

Store = {
  {id=1, name="Apex One 2026", speed=180, maxHp=100, price=0, nitroCap=1, grip=0.62, fuelEfficiency=1.0, rarity="مبتدئة", engineSound="engine_variant_01"},
  {id=2, name="Nova Pulse 2026", speed=194, maxHp=100, price=7000, nitroCap=1, grip=0.626, fuelEfficiency=0.996, rarity="مبتدئة", engineSound="engine_variant_02"},
  {id=3, name="Vector S 2026", speed=208, maxHp=100, price=8200, nitroCap=1, grip=0.632, fuelEfficiency=0.992, rarity="مبتدئة", engineSound="engine_variant_03"},
  {id=4, name="Volt Arrow 2026", speed=222, maxHp=132, price=9600, nitroCap=1, grip=0.638, fuelEfficiency=0.988, rarity="مبتدئة", engineSound="engine_variant_04"},
  {id=5, name="Raptor X 2026", speed=236, maxHp=132, price=11200, nitroCap=2, grip=0.644, fuelEfficiency=0.984, rarity="مبتدئة", engineSound="engine_variant_05"},
  {id=6, name="Lumina GT 2026", speed=250, maxHp=132, price=13100, nitroCap=2, grip=0.65, fuelEfficiency=0.98, rarity="مبتدئة", engineSound="engine_variant_06"},
  {id=7, name="Phantom E 2026", speed=264, maxHp=164, price=15300, nitroCap=2, grip=0.656, fuelEfficiency=0.976, rarity="مبتدئة", engineSound="engine_variant_07"},
  {id=8, name="Thunder RS 2026", speed=278, maxHp=164, price=18000, nitroCap=2, grip=0.662, fuelEfficiency=0.972, rarity="مبتدئة", engineSound="engine_variant_08"},
  {id=9, name="Vortex Q 2026", speed=292, maxHp=164, price=21000, nitroCap=3, grip=0.668, fuelEfficiency=0.968, rarity="متقدمة", engineSound="engine_variant_09"},
  {id=10, name="Spark GT 2026", speed=306, maxHp=196, price=24600, nitroCap=3, grip=0.674, fuelEfficiency=0.964, rarity="متقدمة", engineSound="engine_variant_10"},
  {id=11, name="Mirage S 2026", speed=320, maxHp=196, price=28800, nitroCap=3, grip=0.68, fuelEfficiency=0.96, rarity="متقدمة", engineSound="engine_variant_11"},
  {id=12, name="Stormline 2026", speed=334, maxHp=196, price=33600, nitroCap=3, grip=0.686, fuelEfficiency=0.956, rarity="متقدمة", engineSound="engine_variant_12"},
  {id=13, name="Eagle Pro 2026", speed=348, maxHp=228, price=39400, nitroCap=4, grip=0.692, fuelEfficiency=0.952, rarity="متقدمة", engineSound="engine_variant_13"},
  {id=14, name="Cheetah R 2026", speed=362, maxHp=228, price=46100, nitroCap=4, grip=0.698, fuelEfficiency=0.948, rarity="متقدمة", engineSound="engine_variant_14"},
  {id=15, name="Falcon GT 2026", speed=376, maxHp=228, price=53900, nitroCap=4, grip=0.704, fuelEfficiency=0.944, rarity="متقدمة", engineSound="engine_variant_15"},
  {id=16, name="Comet X 2026", speed=390, maxHp=260, price=63100, nitroCap=4, grip=0.71, fuelEfficiency=0.94, rarity="متقدمة", engineSound="engine_variant_16"},
  {id=17, name="Wave Runner 2026", speed=404, maxHp=260, price=73800, nitroCap=5, grip=0.716, fuelEfficiency=0.936, rarity="متقدمة", engineSound="engine_variant_17"},
  {id=18, name="Meteor S 2026", speed=418, maxHp=260, price=86300, nitroCap=5, grip=0.722, fuelEfficiency=0.932, rarity="متقدمة", engineSound="engine_variant_18"},
  {id=19, name="Cannon XR 2026", speed=432, maxHp=292, price=101000, nitroCap=5, grip=0.728, fuelEfficiency=0.928, rarity="متقدمة", engineSound="engine_variant_19"},
  {id=20, name="Cyclone GT 2026", speed=446, maxHp=292, price=118200, nitroCap=5, grip=0.734, fuelEfficiency=0.924, rarity="متقدمة", engineSound="engine_variant_20"},
  {id=21, name="Sentinel R 2026", speed=460, maxHp=292, price=138200, nitroCap=6, grip=0.74, fuelEfficiency=0.92, rarity="متقدمة", engineSound="engine_variant_21"},
  {id=22, name="Striker S 2026", speed=474, maxHp=324, price=161700, nitroCap=6, grip=0.746, fuelEfficiency=0.916, rarity="متقدمة", engineSound="engine_variant_22"},
  {id=23, name="Driftmaster 2026", speed=488, maxHp=324, price=189200, nitroCap=6, grip=0.752, fuelEfficiency=0.912, rarity="متقدمة", engineSound="engine_variant_23"},
  {id=24, name="Racer Pro 2026", speed=502, maxHp=324, price=221400, nitroCap=6, grip=0.758, fuelEfficiency=0.908, rarity="متقدمة", engineSound="engine_variant_24"},
  {id=25, name="Skybird GT 2026", speed=516, maxHp=356, price=259000, nitroCap=7, grip=0.764, fuelEfficiency=0.904, rarity="نادرة", engineSound="engine_variant_25"},
  {id=26, name="Ironclad X 2026", speed=530, maxHp=356, price=303100, nitroCap=7, grip=0.77, fuelEfficiency=0.9, rarity="نادرة", engineSound="engine_variant_26"},
  {id=27, name="Volcano R 2026", speed=544, maxHp=356, price=354600, nitroCap=7, grip=0.776, fuelEfficiency=0.896, rarity="نادرة", engineSound="engine_variant_27"},
  {id=28, name="Cutter RS 2026", speed=558, maxHp=388, price=414900, nitroCap=7, grip=0.782, fuelEfficiency=0.892, rarity="نادرة", engineSound="engine_variant_28"},
  {id=29, name="Claw GT 2026", speed=572, maxHp=388, price=485400, nitroCap=8, grip=0.788, fuelEfficiency=0.888, rarity="نادرة", engineSound="engine_variant_29"},
  {id=30, name="Lance X 2026", speed=586, maxHp=388, price=567900, nitroCap=8, grip=0.794, fuelEfficiency=0.884, rarity="نادرة", engineSound="engine_variant_30"},
  {id=31, name="Titan Pro 2026", speed=600, maxHp=420, price=664500, nitroCap=8, grip=0.8, fuelEfficiency=0.88, rarity="نادرة", engineSound="engine_variant_31"},
  {id=32, name="Nightbat 2026", speed=614, maxHp=420, price=777500, nitroCap=8, grip=0.806, fuelEfficiency=0.876, rarity="نادرة", engineSound="engine_variant_32"},
  {id=33, name="Wolf RS 2026", speed=628, maxHp=420, price=909600, nitroCap=9, grip=0.812, fuelEfficiency=0.872, rarity="نادرة", engineSound="engine_variant_33"},
  {id=34, name="Lynx GT 2026", speed=642, maxHp=452, price=1064300, nitroCap=9, grip=0.818, fuelEfficiency=0.868, rarity="نادرة", engineSound="engine_variant_34"},
  {id=35, name="Black Volt 2026", speed=656, maxHp=452, price=1245200, nitroCap=9, grip=0.824, fuelEfficiency=0.864, rarity="نادرة", engineSound="engine_variant_35"},
  {id=36, name="Starline X 2026", speed=670, maxHp=452, price=1456900, nitroCap=9, grip=0.83, fuelEfficiency=0.86, rarity="نادرة", engineSound="engine_variant_36"},
  {id=37, name="Orbit S 2026", speed=684, maxHp=484, price=1704500, nitroCap=10, grip=0.836, fuelEfficiency=0.856, rarity="نادرة", engineSound="engine_variant_37"},
  {id=38, name="Destroyer R 2026", speed=698, maxHp=484, price=1994300, nitroCap=10, grip=0.842, fuelEfficiency=0.852, rarity="نادرة", engineSound="engine_variant_38"},
  {id=39, name="Spine GT 2026", speed=712, maxHp=484, price=2333300, nitroCap=10, grip=0.848, fuelEfficiency=0.848, rarity="نادرة", engineSound="engine_variant_39"},
  {id=40, name="Range Pro 2026", speed=726, maxHp=516, price=2730000, nitroCap=10, grip=0.854, fuelEfficiency=0.844, rarity="نادرة", engineSound="engine_variant_40"},
  {id=41, name="Maxspeed X 2026", speed=740, maxHp=516, price=3194100, nitroCap=11, grip=0.86, fuelEfficiency=0.84, rarity="نادرة", engineSound="engine_variant_41"},
  {id=42, name="Red Orbit 2026", speed=754, maxHp=516, price=3737100, nitroCap=11, grip=0.866, fuelEfficiency=0.836, rarity="نادرة", engineSound="engine_variant_42"},
  {id=43, name="Silver Shadow 2026", speed=768, maxHp=548, price=4372400, nitroCap=11, grip=0.872, fuelEfficiency=0.832, rarity="نخبة", engineSound="engine_variant_43"},
  {id=44, name="Golden Legend 2026", speed=782, maxHp=548, price=5115700, nitroCap=11, grip=0.878, fuelEfficiency=0.828, rarity="نخبة", engineSound="engine_variant_44"},
  {id=45, name="Blue Spectrum 2026", speed=796, maxHp=548, price=5985400, nitroCap=12, grip=0.884, fuelEfficiency=0.824, rarity="نخبة", engineSound="engine_variant_45"},
  {id=46, name="King Apex 2026", speed=810, maxHp=580, price=7002900, nitroCap=12, grip=0.89, fuelEfficiency=0.82, rarity="نخبة", engineSound="engine_variant_46"},
  {id=47, name="Throne RS 2026", speed=824, maxHp=580, price=8193400, nitroCap=12, grip=0.896, fuelEfficiency=0.816, rarity="نخبة", engineSound="engine_variant_47"},
  {id=48, name="Pro Driver X 2026", speed=838, maxHp=580, price=9586200, nitroCap=12, grip=0.902, fuelEfficiency=0.812, rarity="نخبة", engineSound="engine_variant_48"},
  {id=49, name="Bossline 2026", speed=852, maxHp=612, price=11215900, nitroCap=13, grip=0.908, fuelEfficiency=0.808, rarity="نخبة", engineSound="engine_variant_49"},
  {id=50, name="Inferno GT 2026", speed=866, maxHp=612, price=13122600, nitroCap=13, grip=0.914, fuelEfficiency=0.804, rarity="نخبة", engineSound="engine_variant_50"},
  {id=51, name="Quantum R 2026", speed=880, maxHp=612, price=15353400, nitroCap=13, grip=0.92, fuelEfficiency=0.8, rarity="نخبة", engineSound="engine_variant_51"},
  {id=52, name="Velocity X 2026", speed=894, maxHp=644, price=17963500, nitroCap=13, grip=0.926, fuelEfficiency=0.796, rarity="نخبة", engineSound="engine_variant_52"},
  {id=53, name="Eclipse Pro 2026", speed=908, maxHp=644, price=21017300, nitroCap=14, grip=0.932, fuelEfficiency=0.792, rarity="نخبة", engineSound="engine_variant_53"},
  {id=54, name="World Champion 2026", speed=922, maxHp=644, price=24590200, nitroCap=14, grip=0.938, fuelEfficiency=0.788, rarity="نخبة", engineSound="engine_variant_54"},
}

WheelRewards = {
  {label="لم تربح هذه المرة", amount=0, noWin=true},
  {label="5,000 عملة", amount=5000},
  {label="8,000 عملة", amount=8000},
  {label="12,000 عملة", amount=12000},
  {label="18,000 عملة", amount=18000},
  {label="25,000 عملة", amount=25000},
  {label="35,000 عملة", amount=35000}
}

StoreItems = {
  {id="shield", name="درع حماية", price=1200, field="shields", amount=1},
  {id="emp", name="نبضة كهرومغناطيسية", price=1800, field="emps", amount=1},
  {id="drone", name="طائرة إصلاح", price=2500, field="drones", amount=1},
  {id="nitro", name="عبوة نيترو", price=900, field="nitro", amount=1},
  {id="repair", name="عدة إصلاح", price=1400, field="repairKits", amount=1},
  {id="fuel", name="خزان وقود", price=500, field="fuelTanks", amount=1}
}

playerCarHp = 100
Achievements = {}

GameState = {
  speed = 0, maxSpeed = 100, isEngineOn = false, isGasPressed = false, gasPressure = 0, gasPointerId = nil,
  isBrakePressed = false, distance = 0, lane = 0, fuel = 100, engineTemp = 50,
  isCrashed = false, obstacles = {}, weather = "صافي", activeEvent = "", collisionCount = 0, maxCollisions = 7, overtakeCount = 0,
  fuelWarned = false, tempWarned = false, collisionWarned = {}, collisionWarningAt = {},
  lastSyncAt = 0, actionDrawerOpen = false, actionMenuIndex = 1, nitroActive = false, countdownActive = false, countdownNumber = 0,
  opponentName = "", opponentDistance = 0, opponentSpeed = 0, opponentLane = 0, opponentTime = 0, timeElapsed = 0, timeRemaining = 0,
  damage = {engine=0, tires=0, body=0, brakes=0}, wheelSpinning = false, flightMode=false, flightEndsAt=0, flightCost=4000,
  pendingCrate = nil, crateExpiresAt = 0, nextCrateAt = 0, collectedCrates = 0, crateNoticeAt = 0,
  roadCondition = "جاف", traction = 1.0, surfaceGrip = 1.0, currentGear = 1, lastLane = 0, lastLaneChangeAt = 0, lastBrakeSoundAt = 0, lastRoadSoundAt = 0, nextStatusReportAt = 15, nextProximityAt = 0, opponentCollisionCount = 0, opponentIncident = "", raceSessionId = 0, lastRaceSpeed = 0, lastRaceDistance = 0, lastRaceFuel = 100, lastRaceCrates = 0, lastRaceCollisionCount = 0, lastRaceWeather = "صافي", lastRaceRoad = "جاف", lastObstacleNotice = "",
  lastActionAt = 0, raceDurationSeconds = 300, isPaused = false, pauseStartedAt = 0, pausedSeconds = 0, lossReason = "", turnDirective = ""
}
