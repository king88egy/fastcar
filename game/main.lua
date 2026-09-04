-- main.lua
-- نقطة تشغيل اللعبة والواجهة التفاعلية.
require "import"
import "android.app.*"; import "android.os.*"; import "android.widget.*"; import "android.view.*"; import "android.content.Context"; import "android.accounts.*"
import "android.content.pm.PackageManager"; import "android.content.Intent"; import "android.net.Uri"; import "android.speech.*"; import "android.speech.tts.TextToSpeech"; import "android.media.AudioAttributes"; import "android.media.AudioManager"; import "android.media.SoundPool"; import "android.media.MediaPlayer"; import "android.media.MediaRecorder"; import "android.graphics.BitmapFactory"; import "android.util.Base64"; import "android.hardware.Sensor"; import "android.hardware.SensorManager"; import "android.hardware.SensorEventListener"
import "java.util.Locale"; import "java.util.UUID"; import "java.lang.Runnable"; import "java.lang.String"; import "java.lang.Thread"; import "java.net.URL"; import "java.net.URLEncoder"; import "java.io.BufferedReader"; import "java.io.InputStreamReader"; import "java.io.File"; import "java.io.FileInputStream"; import "java.io.FileOutputStream"; import "java.io.ByteArrayOutputStream"; import "java.security.MessageDigest"; import "org.json.JSONObject"

activity.setRequestedOrientation(1); activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); vibrator=activity.getSystemService(Context.VIBRATOR_SERVICE)
function applyOrientationMode()
  local mode = Settings and Settings.orientationMode or "عمودي"
  if mode == "عمودي" then activity.setRequestedOrientation(1) elseif mode == "أفقي" then activity.setRequestedOrientation(0) else activity.setRequestedOrientation(-1) end
end
function setOrientationMode(mode) Settings.orientationMode=mode; saveGameData(); applyOrientationMode(); say("اتجاه الشاشة: "..tostring(mode), true) end

local layout={LinearLayout,layout_width="fill",layout_height="fill",orientation="vertical",id="main_view",background="#10131A",padding="12dp",
  {TextView,id="screen_title",layout_width="fill",layout_height="wrap",text="World Racing",textColor="#FFFFFF",textSize="24sp",gravity="center",padding="8dp",contentDescription="World Racing"},
  {TextView,id="screen_status",layout_width="fill",layout_height="wrap",text="جاري التحميل",textColor="#D8E6FF",textSize="16sp",padding="8dp"},
  {TextView,id="screen_countdown",layout_width="fill",layout_height="wrap",text="",textColor="#FFD54F",textSize="34sp",gravity="center",padding="4dp"},
  {ImageView,id="race_car_image",layout_width="fill",layout_height="0dp",layout_weight="0.22",visibility="gone",padding="4dp"},
  {LinearLayout,id="screen_options_container",layout_width="fill",layout_height="0dp",layout_weight="1",orientation="vertical"},
  {LinearLayout,id="race_controls",layout_width="fill",layout_height="0dp",layout_weight="0.36",orientation="vertical",visibility="gone",padding="2dp"},
  {LinearLayout,id="race_actions_container",layout_width="fill",layout_height="wrap",orientation="vertical",visibility="gone",background="#202B3D",padding="2dp"},
  {TextView,id="screen_hint",layout_width="fill",layout_height="wrap",text="",textColor="#A9C7FF",textSize="14sp",padding="8dp"}
}
activity.setContentView(loadlayout(layout))
pcall(function()
  local bitmap=BitmapFactory.decodeFile(activity.getLuaDir().."/res/race_car_hero.png")
  if bitmap and race_car_image then race_car_image.setImageBitmap(bitmap); race_car_image.setScaleType(ImageView.ScaleType.FIT_CENTER); race_car_image.setClickable(false); race_car_image.setFocusable(false); race_car_image.setContentDescription("صورة سيارة السباق. في وضع المكفوفين اضغط مرتين هنا لتفعيل العنصر المركّز.") end
end)
voiceTextInputTarget=nil
function onActivityResult(requestCode,resultCode,data)
  local code=tonumber(requestCode) or 0
  if code==9233 then
    if resultCode~=activity.RESULT_OK or not data then say("تم إلغاء اختيار حساب Google.",true); return end
    local accountName=""; pcall(function() accountName=tostring(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) or "") end)
    if accountName=="" then say("تعذر قراءة حساب Google المحدد.",true); return end
    if firebaseGoogleAccountSelected then firebaseGoogleAccountSelected(accountName) else say("تسجيل الدخول عبر Google غير متاح في هذا الإصدار.",true) end
    return
  elseif code==9234 then
    if resultCode==activity.RESULT_OK and pendingGoogleAccountName and resumeGoogleAccountAfterConsent then resumeGoogleAccountAfterConsent() else pendingGoogleAccountName=nil; say("لم تكتمل موافقة Google. يمكنك اختيار الحساب مرة أخرى أو استخدام البريد الإلكتروني.",true) end
    return
  end
  if code~=9222 then return end
  local target=voiceTextInputTarget; voiceTextInputTarget=nil
  if resultCode==activity.RESULT_OK and data and target then
    local results=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
    local spoken=results and results.size()>0 and tostring(results.get(0)) or ""
    if spoken~="" then target.setText(spoken); target.setSelection(#spoken); playDynamicSFX("message_receive",0,1,0,0.52); say("تم تحويل الكلام إلى نص. راجع النص ثم أرسله.",true) else say("لم يتم التعرف على كلام واضح.",true) end
  elseif target then say("تم إلغاء الإدخال الصوتي.",true) end
end

startupGateActive=true
local bootModules={"Database","SaveSystem","AudioSystem","TTSSystem","SmartAnnouncer","Firebase","WorldRacingServices","AchievementsSystem","MissionSystem","VoiceSystem","UIRenderer","InputDialogs","SectionsManager","ActionsManager","LoginSystem","RaceEngine","TiltControls","TouchControls"}
local bootIndex=0
local function bootNextModule()
  bootIndex=bootIndex+1
  local progress=math.floor(((bootIndex-1)/#bootModules)*100)
  if screen_status then screen_status.setText("جاري التحميل... "..tostring(progress).."%") end
  local moduleName=bootModules[bootIndex]
  if not moduleName then if screen_status then screen_status.setText("جاري التحميل... 100%") end; pcall(refreshVisibleScreen); return end
  local ok,err=pcall(require,moduleName)
  if not ok then if screen_status then screen_status.setText("تعذر تحميل مكوّن اللعبة. حاول إعادة التشغيل.") end; pcall(function() say("تعذر تحميل مكوّن اللعبة: "..tostring(moduleName),true) end); return end
  Handler().postDelayed(Runnable({run=bootNextModule}),35)
end
Handler().postDelayed(Runnable({run=bootNextModule}),120)

local lastBackPressAt=0
function onBackPressed()
  pcall(function()
    local now=System.currentTimeMillis(); local section=tostring(currentSection or "LOGIN")
    if section=="MENU" then
      if now-lastBackPressAt<=700 then lastBackPressAt=0; if confirmExitGame then confirmExitGame() else stopSoundsAndExit() end
      else lastBackPressAt=now; playDynamicSFX("a11y_press_light",0,1,0,0.72); say("اضغط زر الرجوع مرتين سريعًا للخروج.",true) end
    elseif section=="PLAYING" or section=="PLAYING_MULTIPLAYER" then
      if GameState.actionDrawerOpen and openRaceActionDrawer then openRaceActionDrawer() else say("اسحب من أعلى إلى أسفل جهة اليسار لفتح قائمة الإجراءات.",true) end
    elseif section=="LOGIN" or section=="LANGUAGE_GATE" then say("أكمل تسجيل الدخول أو اختيار اللغة أولًا.",true)
    else loadSection("MENU") end
  end)
  return true
end

