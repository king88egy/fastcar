-- MissionSystem.lua
-- مهام يومية وأسبوعية قابلة للتقدم والاستلام مع مفاتيح منع التكرار.

MissionSystem={daily={},weekly={},periodKey="",initialized=false}
local missionPrefs=activity.getSharedPreferences("HackerCenterProSettings",Context.MODE_PRIVATE)
local dailyDefinitions={
  {id="daily_distance",title="مسافة اليوم",description="اقطع مسافة 500 متر.",target=500,field="distance",reward=2000},
  {id="daily_overtake",title="متجاوز اليوم",description="تجاوز منافسين اثنين.",target=2,field="overtake",reward=2500},
  {id="daily_crate",title="جامع الصناديق",description="اجمع صندوقًا واحدًا.",target=1,field="crate",reward=3500},
  {id="daily_finish",title="سباق اليوم",description="أكمل سباقًا واحدًا.",target=1,field="finish",reward=5000}
}
local weeklyDefinitions={
  {id="weekly_distance",title="مسافة الأسبوع",description="اقطع مسافة 5000 متر.",target=5000,field="distance",reward=10000},
  {id="weekly_win",title="فوز الأسبوع",description="حقق انتصارين.",target=2,field="win",reward=15000},
  {id="weekly_social",title="روح الفريق",description="أكمل سباقًا جماعيًا.",target=1,field="social",reward=25000}
}
local function periodKey()
  local now=os.date("*t"); local week=math.floor((tonumber(now.yday or 1)-1)/7)+1; return string.format("%04d_%03d_%02d",now.year,week,now.wday or 1) end
local function hydrate(defs,stored)
  local result={}; for _,def in ipairs(defs) do local row=stored[def.id] or {}; result[def.id]={id=def.id,title=def.title,description=def.description,target=def.target,field=def.field,reward=def.reward,progress=tonumber(row.progress) or 0,claimed=row.claimed==true,completed=(tonumber(row.progress) or 0)>=def.target} end; return result end
function missionsSave()
  local root=JSONObject(); root.put("period",MissionSystem.periodKey); local function putGroup(name,group) local obj=JSONObject(); for id,row in pairs(group) do local item=JSONObject(); item.put("progress",row.progress or 0); item.put("claimed",row.claimed==true); obj.put(id,item) end; root.put(name,obj) end; putGroup("daily",MissionSystem.daily); putGroup("weekly",MissionSystem.weekly); missionPrefs.edit().putString("missions",root.toString()).commit()
end
function missionsInitialize()
  local stored={daily={},weekly={}}; local raw=missionPrefs.getString("missions",""); if raw~="" then pcall(function() local root=JSONObject(raw); stored.period=root.optString("period",""); local function readGroup(name) local obj=root.optJSONObject(name); local result={}; local keys=obj and obj.keys(); while keys and keys.hasNext() do local id=tostring(keys.next()); local item=obj.optJSONObject(id); if item then result[id]={progress=tonumber(item.optDouble("progress",0)) or 0,claimed=item.optBoolean("claimed",false)} end end; return result end; stored.daily=readGroup("daily"); stored.weekly=readGroup("weekly") end) end
  local key=periodKey(); if stored.period~=key then stored.daily={}; stored.weekly={}; end; MissionSystem.periodKey=key; MissionSystem.daily=hydrate(dailyDefinitions,stored.daily or {}); MissionSystem.weekly=hydrate(weeklyDefinitions,stored.weekly or {}); MissionSystem.initialized=true; missionsSave()
end
local function recordGroup(group,field,amount)
  for _,row in pairs(group) do if row.field==field and not row.claimed then row.progress=math.min(row.target,(row.progress or 0)+(tonumber(amount) or 1)); row.completed=row.progress>=row.target end end
end
function missionsRecord(field,amount)
  if not MissionSystem.initialized then missionsInitialize() end; recordGroup(MissionSystem.daily,field,amount); recordGroup(MissionSystem.weekly,field,amount); missionsSave()
end
function missionsSummary()
  local rows={}; for _,row in pairs(MissionSystem.daily or {}) do table.insert(rows,{group="يومية",id=row.id,title=row.title,description=row.description,progress=row.progress,target=row.target,reward=row.reward,claimed=row.claimed,completed=row.completed}) end; for _,row in pairs(MissionSystem.weekly or {}) do table.insert(rows,{group="أسبوعية",id=row.id,title=row.title,description=row.description,progress=row.progress,target=row.target,reward=row.reward,claimed=row.claimed,completed=row.completed}) end; table.sort(rows,function(a,b) return a.id<b.id end); return rows end
function missionsClaim(id,callback)
  local row=nil; for _,item in ipairs(missionsSummary()) do if item.id==id then row=item; break end end; if not row then if callback then callback(false,"المهمة غير موجودة.") end; return end; if not row.completed or row.claimed then if callback then callback(false,row.claimed and "تم استلام هذه المهمة سابقًا." or "المهمة لم تكتمل بعد.") end; return end
  local function finish(ok,message) if ok then local group=(id:match("^daily") and MissionSystem.daily or MissionSystem.weekly); if group[id] then group[id].claimed=true end; playerStats.money=(playerStats.money or 0)+(row.reward or 0); missionsSave(); saveGameData(); if firebaseRecordEconomyLedger then firebaseRecordEconomyLedger("mission",row.reward,row.title,"mission_"..id.."_"..MissionSystem.periodKey,function() end) end; playDynamicSFX("coin_reward",0,1,0,0.86) end; if callback then callback(ok,message or (ok and "تم استلام مكافأة المهمة." or "تعذر استلام المهمة.")) end end
  if firebaseClaimIdempotentReward then firebaseClaimIdempotentReward("mission_"..id.."_"..MissionSystem.periodKey,{money=row.reward},row.title,function(ok,msg,already) if ok and not already then finish(true,"تم استلام مكافأة المهمة: "..tostring(row.reward).." عملة.") elseif already then local group=(id:match("^daily") and MissionSystem.daily or MissionSystem.weekly); if group[id] then group[id].claimed=true end; missionsSave(); if callback then callback(true,"المكافأة مستلمة سابقًا.") end else finish(false,msg) end end) else finish(true) end
end
function missionsRecordRaceTick(distance,overtakes,crates)
  local d=tonumber(distance) or 0; local o=tonumber(overtakes) or 0; local c=tonumber(crates) or 0
  local previousD=tonumber(MissionSystem.lastDistance or 0) or 0; local previousO=tonumber(MissionSystem.lastOvertakes or 0) or 0; local previousC=tonumber(MissionSystem.lastCrates or 0) or 0
  MissionSystem.lastDistance=d; MissionSystem.lastOvertakes=o; MissionSystem.lastCrates=c
  if d>previousD then missionsRecord("distance",d-previousD) end; if o>previousO then missionsRecord("overtake",o-previousO) end; if c>previousC then missionsRecord("crate",c-previousC) end
end
function missionsRecordRaceFinish(won,mode)
  missionsRecord("finish",1); if won then missionsRecord("win",1) end; if mode=="PLAYING_MULTIPLAYER" then missionsRecord("social",1) end
end
missionsInitialize()
