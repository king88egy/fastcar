-- WorldRacingServices.lua
-- خدمات مشتركة للمميزات الإضافية: الهجرة، أعلام الميزات، منع التكرار، الاتصال، والطابور المحلي.

WorldRacingServices = WorldRacingServices or {}
WorldRacingServices.schemaVersion = 5
WorldRacingServices.featurePack = "114-additional"
WorldRacingServices.featureFlags = WorldRacingServices.featureFlags or {}
WorldRacingServices.processed = WorldRacingServices.processed or {}
WorldRacingServices.pending = WorldRacingServices.pending or {}
WorldRacingServices.lastError = ""
WorldRacingServices.connection = {status="غير متصل",lastAt=0,lastLatencyMs=0,retries=0,source="local"}

WorldRacingServices.featureNames = {
  "اختيار السحب أو الإمالة","معايرة مركز الحساس","حساسية الإمالة","عكس اتجاه الإمالة","تثبيت اتجاه الشاشة","إيقاف الحساس خارج السباق","فصل الحساس عن اللمس","اختبار حساس الحركة","توجيه مختلط","حفظ طريقة التحكم","معايرة مناطق اللمس","وضع اليد اليمنى واليسرى","تكبير مناطق اللمس","قفل الأزرار","منع اللمسة الشاردة","استعادة التحكم الافتراضي","تدريب التحكم","صوت تفعيل الإمالة","صوت تعطيل الإمالة","نطق حالة الحساس",
  "مسارات متعددة","مناطق تماسك متغيرة","أسطح طريق متدرجة","تأثير الرياح","تأثير الانحدار","فرملة حسب التماسك","انزلاق محسوب","عقبات متزامنة","تحذير مبكر","مناطق تجاوز","نقاط تفتيش","جسر وأنفاق","حوادث متزامنة","سيارات غير متسابقة","درجات التصادم","منع تكرار التصادم","إنقاذ الخروج","عودة لآخر نقطة","سباق زمني","سباق مسافة",
  "سلوك روبوت متعدد","ذاكرة أخطاء الروبوت","مسار بديل للروبوت","احتمال تصادم واقعي","روبوت يستخدم الصندوق","روبوت يفرمل","عدالة سرعة الروبوت","تقرير خطأ الروبوت","صعوبة الروبوت","بذرة روبوت موحدة",
  "زمن الاستجابة","جودة الاتصال","مزامنة دورية","جلسة سباق فريدة","مفاتيح منع التكرار","ترتيب الأحداث","رفض الجلسة القديمة","إعادة إرسال متدرجة","طابور محلي","تفريغ الطابور","منع الرد القديم","لقطة سباق","تحول إلى محلي","عودة الاتصال","اختبار الاتصال","توافق إصدار العميل","تقليل الاستطلاع","رفع الاستطلاع عند البدء","نبض الحضور","إنهاء الحضور",
  "زر جاهز","عداد الجاهزية","مهلة الغرفة","نقل ملكية ذري","سجل إجراءات","صلاحيات الغرفة","أنواع الغرف","دعوات منتهية","دعوة صديق","منع المحظور","دردشة الغرفة","رسائل خاصة","إشعارات موحدة","كتم وحظر","البلاغات","تنظيف الوسائط","بث موسيقى HTTPS","إيقاف موسيقى الغرفة","اللاعبون الفعليون","آخر نشاط",
  "مزج صوتي","أولوية التحذير","أصوات الغرفة","أصوات الإدارة","أصوات أزرار السباق","أصوات تغيير المسار","أصوات الأسطح","مذيع متعدد","تعليق اختياري","منع تكرار التعليق","سرعة صوت مستقلة","مستويات صوت مستقلة","كتم المذيع","معاينة الصوت",
  "مهام يومية","الحقيبة","الورشة","تطوير السيارة","متجر محمي","دفتر العملات","منع مكافأة مكررة","تحديث عن بعد","أعلام الخادم","سجل تدقيق"
}

function wrNowMs() return (firebaseServerNowMs and firebaseServerNowMs()) or (os.time()*1000) end
function wrNowSec() return math.floor(wrNowMs()/1000) end
function wrUniqueId(prefix) return tostring(prefix or "wr").."_"..tostring(wrNowMs()).."_"..tostring(math.random(100000,999999)) end
function wrClamp(value,minimum,maximum) return math.max(minimum,math.min(maximum,tonumber(value) or minimum)) end
function wrFeatureEnabled(id)
  if WorldRacingServices.featureFlags[id]==nil then return true end
  return WorldRacingServices.featureFlags[id]==true
end
function wrSetFeatureFlag(id,value) WorldRacingServices.featureFlags[tostring(id)]=value==true end
function wrSetConnection(status,latency,source)
  WorldRacingServices.connection.status=tostring(status or "غير متصل"); WorldRacingServices.connection.lastLatencyMs=math.max(0,tonumber(latency or 0) or 0); WorldRacingServices.connection.lastAt=wrNowMs(); WorldRacingServices.connection.source=tostring(source or "network"); Multiplayer.serverStatus=WorldRacingServices.connection.status; Multiplayer.lastLatencyMs=WorldRacingServices.connection.lastLatencyMs; Multiplayer.connectionSource=WorldRacingServices.connection.source
end
function wrMarkProcessed(key)
  key=tostring(key or ""); if key=="" or WorldRacingServices.processed[key] then return false end
  WorldRacingServices.processed[key]=wrNowSec(); return true
end
function wrQueue(action,payload)
  table.insert(WorldRacingServices.pending,{id=wrUniqueId("queue"),action=tostring(action or ""),payload=payload or {},createdAt=wrNowSec(),attempts=0}); Multiplayer.pendingNetworkActions=WorldRacingServices.pending; return true
end
function wrFlushQueue(sender)
  if type(sender)~="function" or #WorldRacingServices.pending==0 then return end
  local item=WorldRacingServices.pending[1]; item.attempts=(item.attempts or 0)+1
  sender(item,function(ok)
    if ok then table.remove(WorldRacingServices.pending,1); wrFlushQueue(sender) elseif item.attempts<4 then Handler().postDelayed(Runnable({run=function() wrFlushQueue(sender) end}),math.min(15000,1000*item.attempts)) end
  end)
end
function wrAudit(kind,details)
  local item={id=wrUniqueId("audit"),kind=tostring(kind or "event"),details=details or {},at=wrNowSec(),username=playerStats and playerStats.username or ""}
  WorldRacingServices.lastAudit=item
  if firebasePublishAdminFeedEvent then pcall(function() firebasePublishAdminFeedEvent("client_"..item.kind,item,function() end) end) end
  return item
end
function wrApplyFeatureFlags(root)
  if not root then return end
  local flags=root.flags or root.featureFlags
  if type(flags)=="table" then for id,value in pairs(flags) do wrSetFeatureFlag(id,value) end end
end
function wrConnectionLabel()
  local s=WorldRacingServices.connection or {}; return tostring(s.status or "غير متصل").."، زمن الاستجابة "..tostring(math.floor(s.lastLatencyMs or 0)).." ميلي ثانية"
end
function wrSteeringLabel()
  return Settings and (Settings.steeringMode or "السحب") or "السحب"
end
function wrMigrateLocalData()
  Settings.steeringMode=Settings.steeringMode or "السحب"; Settings.tiltSensitivity=wrClamp(Settings.tiltSensitivity or 2.4,0.15,8); Settings.tiltDeadZone=wrClamp(Settings.tiltDeadZone or 0.65,0.05,4); Settings.tiltCenter=tonumber(Settings.tiltCenter or 0) or 0; Settings.invertTilt=Settings.invertTilt==true
  Multiplayer.pendingNetworkActions=WorldRacingServices.pending; Multiplayer.lastLatencyMs=Multiplayer.lastLatencyMs or 0; Multiplayer.connectionSource=Multiplayer.connectionSource or "local"; Multiplayer.schemaVersion=WorldRacingServices.schemaVersion
end
function wrStartHeartbeat()
  if WorldRacingServices.heartbeatActive then return end; WorldRacingServices.heartbeatActive=true
  local function beat()
    if not WorldRacingServices.heartbeatActive or not playerStats.sessionActive then WorldRacingServices.heartbeatActive=false; return end
    if firebaseSyncServerClock then firebaseSyncServerClock(function(ok,offset) wrSetConnection(ok and "متصل" or "غير متصل",0,"server_clock") end) end
    if firebaseSetPresence then firebaseSetPresence(true,function(ok) if not ok then wrSetConnection("ضعيف",WorldRacingServices.connection.lastLatencyMs,"presence") end end) end
    Handler().postDelayed(Runnable({run=beat}),60000)
  end
  beat()
end
function wrStopHeartbeat() WorldRacingServices.heartbeatActive=false end

wrMigrateLocalData()
