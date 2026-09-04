local activeKeyboardInput = nil
local internalKeyTapAt = {}
local function textOf(input) return input and input.getText().toString() or "" end
local function activateInternalKey(label,action)
  if Settings.accessibilityMode~="وضع المكفوفين" then if action then action() end; return end
  local now=System.currentTimeMillis(); local last=tonumber(internalKeyTapAt[label] or 0) or 0
  if last>0 and now-last<=700 then internalKeyTapAt[label]=nil; playDynamicSFX("a11y_press_confirm",0,1,0,0.20); if action then action() end else internalKeyTapAt[label]=now; playDynamicSFX("a11y_press_light",0,1,0,0.16); say(tostring(label),true) end
end
local function makeInput(hint, initial, password)
  local input = EditText(activity); input.setHint(hint); input.setSingleLine(true); input.setContentDescription(tostring(hint or ""))
  if initial and initial ~= "" then input.setText(initial) end
  if password then pcall(function() input.setInputType(0x81) end) end
  pcall(function() input.setPadding(20, 12, 20, 12) end)
  return input
end
local function addKey(row, label, action)
  local key = Button(activity); key.setText(label); key.setAllCaps(false); key.setContentDescription(tostring(label)); key.setOnClickListener(function() activateInternalKey(label,action) end)
  row.addView(key, LinearLayout.LayoutParams(0, 48, 1))
end
function requestVoiceTextInput(input)
  if not input then say("اختر حقل النص أولاً.",true); return end
  local ok,err=pcall(function()
    voiceTextInputTarget=input
    local intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ar-EG")
    intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"تحدث الآن وسيظهر كلامك نصًا قابلًا للتعديل.")
    activity.startActivityForResult(intent,9222)
  end)
  if not ok then voiceTextInputTarget=nil; say("خدمة تحويل الكلام إلى نص غير متاحة على هذا الجهاز.",true) end
end
function attachGameKeyboard(container, inputs)
  if Settings.keyboardMode ~= "داخلية" then
    for _, input in ipairs(inputs or {}) do pcall(function() input.setShowSoftInputOnFocus(true) end) end
    return
  end
  local notice = TextView(activity); notice.setText("لوحة اللعبة الداخلية. اختر الحقل ثم اضغط الحروف."); container.addView(notice)
  activeKeyboardInput = inputs[1]
  for _, input in ipairs(inputs or {}) do
    pcall(function() input.setShowSoftInputOnFocus(false) end)
    input.setOnFocusChangeListener(function(_, hasFocus) if hasFocus then activeKeyboardInput = input end end)
  end
  local keyRows={{"1","2","3","4","5","6","7","8","9","0","@",".","_","-"},{"a","b","c","d","e","f","g","h","i","j","k","l","m"},{"n","o","p","q","r","s","t","u","v","w","x","y","z"},{"A","B","C","D","E","F","G","H","I","J","K","L","M"},{"N","O","P","Q","R","S","T","U","V","W","X","Y","Z"},{"ا","ب","ت","ث","ج","ح","خ","د","ذ","ر","ز","س","ش","ص"},{"ض","ط","ظ","ع","غ","ف","ق","ك","ل","م","ن","ه","و","ي","أ","إ","آ","ة","ى","ئ"}}
  for _,keyRow in ipairs(keyRows) do
    local row=LinearLayout(activity); row.setOrientation(LinearLayout.HORIZONTAL); container.addView(row)
    for _,char in ipairs(keyRow) do addKey(row,char,function() if activeKeyboardInput then activeKeyboardInput.append(char) end end) end
  end
  local controls = LinearLayout(activity); controls.setOrientation(LinearLayout.HORIZONTAL); container.addView(controls)
  addKey(controls, "مسافة", function() if activeKeyboardInput then activeKeyboardInput.append(" ") end end)
  addKey(controls, "حذف", function()
    if activeKeyboardInput then local value = textOf(activeKeyboardInput); if #value > 0 then local start=(utf8 and utf8.offset and utf8.offset(value,-1)) or #value; local updated=value:sub(1,math.max(0,start-1)); activeKeyboardInput.setText(updated); activeKeyboardInput.setSelection(#updated) end end
  end)
  addKey(controls, "صوت", function() requestVoiceTextInput(activeKeyboardInput) end)
  addKey(controls, "النظام", function()
    if activeKeyboardInput then pcall(function() activeKeyboardInput.setShowSoftInputOnFocus(true) end); activeKeyboardInput.requestFocus(); pcall(function() activity.getSystemService(Context.INPUT_METHOD_SERVICE).showSoftInput(activeKeyboardInput, 0) end) end
  end)
end
function showSingleInput(title, hint, initial, password, onDone)
  if not screen_options_container then say("واجهة الإدخال غير جاهزة.",true); return end
  currentInternalDialog=true
  local root=LinearLayout(activity); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(18,10,18,10)
  local heading=TextView(activity); heading.setText(tostring(title)); heading.setTextSize(22); heading.setTextColor(0xFFFFFFFF); heading.setContentDescription(tostring(title)); root.addView(heading,LinearLayout.LayoutParams(-1,-2))
  local actions=LinearLayout(activity); actions.setOrientation(LinearLayout.HORIZONTAL); root.addView(actions,LinearLayout.LayoutParams(-1,-2))
  local cancel=Button(activity); cancel.setText("إلغاء"); cancel.setAllCaps(false); cancel.setContentDescription("إلغاء"); actions.addView(cancel,LinearLayout.LayoutParams(0,-2,1))
  local accept=Button(activity); accept.setText("موافق"); accept.setAllCaps(false); accept.setContentDescription("موافق"); actions.addView(accept,LinearLayout.LayoutParams(0,-2,1))
  local scroll=ScrollView(activity); scroll.setFillViewport(true)
  local content=LinearLayout(activity); content.setOrientation(LinearLayout.VERTICAL); scroll.addView(content,LinearLayout.LayoutParams(-1,-2))
  local input=makeInput(hint,initial,password); content.addView(input,LinearLayout.LayoutParams(-1,-2))
  local voiceButton=Button(activity); voiceButton.setText("إدخال صوتي"); voiceButton.setAllCaps(false); voiceButton.setContentDescription("إدخال صوتي وتحويل الكلام إلى نص"); voiceButton.setOnClickListener(function() requestVoiceTextInput(input) end); content.addView(voiceButton,LinearLayout.LayoutParams(-1,-2))
  attachGameKeyboard(content,{input})
  root.addView(scroll,LinearLayout.LayoutParams(-1,0,1))
  screen_options_container.removeAllViews(); screen_options_container.addView(root,LinearLayout.LayoutParams(-1,-1)); input.requestFocus(); say(title,true)
  local closed=false
  local function closeInput()
    if closed then return end; closed=true; currentInternalDialog=nil; screen_options_container.removeAllViews(); pcall(refreshVisibleScreen)
  end
  cancel.setOnClickListener(function() closeInput(); say("تم الإلغاء.",true) end)
  accept.setOnClickListener(function() local value=textOf(input); closeInput(); if onDone then onDone(value) end end)
end

function showInternalConfirm(title,message,onYes,onNo)
  if not screen_options_container then if onNo then onNo() end; return end
  currentInternalDialog=true
  local root=LinearLayout(activity); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(18,12,18,12)
  local heading=TextView(activity); heading.setText(tostring(title)); heading.setTextSize(22); heading.setTextColor(0xFFFFFFFF); root.addView(heading)
  local body=TextView(activity); body.setText(tostring(message)); body.setTextSize(17); body.setTextColor(0xFFD8E6FF); body.setPadding(0,14,0,18); root.addView(body)
  local row=LinearLayout(activity); row.setOrientation(LinearLayout.HORIZONTAL); root.addView(row,LinearLayout.LayoutParams(-1,-2))
  local no=Button(activity); no.setText("إلغاء"); no.setAllCaps(false); row.addView(no,LinearLayout.LayoutParams(0,-2,1))
  local yes=Button(activity); yes.setText("موافق"); yes.setAllCaps(false); row.addView(yes,LinearLayout.LayoutParams(0,-2,1))
  screen_options_container.removeAllViews(); screen_options_container.addView(root,LinearLayout.LayoutParams(-1,-1)); say(title,true)
  local closed=false; local function close() if closed then return end; closed=true; currentInternalDialog=nil; screen_options_container.removeAllViews(); pcall(refreshVisibleScreen) end
  no.setOnClickListener(function() close(); if onNo then onNo() end end)
  yes.setOnClickListener(function() close(); if onYes then onYes() end end)
end
function showInternalPanel(title,build,onDone,onCancel)
  if not screen_options_container then if onCancel then onCancel() end; return end
  currentInternalDialog=true
  local root=LinearLayout(activity); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(18,12,18,12)
  local heading=TextView(activity); heading.setText(tostring(title)); heading.setTextSize(22); heading.setTextColor(0xFFFFFFFF); root.addView(heading)
  local content=LinearLayout(activity); content.setOrientation(LinearLayout.VERTICAL); root.addView(content,LinearLayout.LayoutParams(-1,0,1))
  local values=build and build(content) or {}
  local row=LinearLayout(activity); row.setOrientation(LinearLayout.HORIZONTAL); root.addView(row,LinearLayout.LayoutParams(-1,-2))
  local cancel=Button(activity); cancel.setText("إلغاء"); cancel.setAllCaps(false); row.addView(cancel,LinearLayout.LayoutParams(0,-2,1))
  local accept=Button(activity); accept.setText("موافق"); accept.setAllCaps(false); row.addView(accept,LinearLayout.LayoutParams(0,-2,1))
  screen_options_container.removeAllViews(); screen_options_container.addView(root,LinearLayout.LayoutParams(-1,-1)); say(title,true)
  local closed=false; local function close() if closed then return false end; closed=true; currentInternalDialog=nil; screen_options_container.removeAllViews(); pcall(refreshVisibleScreen); return true end
  cancel.setOnClickListener(function() if close() and onCancel then onCancel() end end)
  accept.setOnClickListener(function() if close() and onDone then onDone(values) end end)
  return values
end

local function validEmail(email) return email and email:match("^[^%s@]+@[^%s@]+%.[^%s@]+$") ~= nil end
local function validUsername(username) return username and #username >= 3 and username:match("^[^%s/\\\"]+$") ~= nil end
local function beginEmailAccountFlow(isNew,email,password,button)
  local e,p=string.lower(tostring(email or "")),tostring(password or "")
  if not validEmail(e) then playDynamicSFX("input_error",0,1,0,0.45); say("أدخل بريدًا إلكترونيًا صحيحًا.",true); return end
  if #p<6 then playDynamicSFX("input_error",0,1,0,0.45); say("كلمة المرور يجب أن تكون ستة أحرف على الأقل.",true); return end
  if button then button.setEnabled(false) end
  playDynamicSFX(isNew and "account_create" or "account_recover",0,1,0,0.52); beginVerification(); setVerificationProgress(0); say(isNew and "جاري إنشاء الحساب." or "جاري استعادة الحساب.",true)
  local finished=false
  local finish=function(ok,message)
    if finished then return end; finished=true
    if button then button.setEnabled(true) end
    if not ok then
      finishVerification(false,message or "تعذر إتمام تسجيل الدخول. تحقق من البريد وكلمة المرور والاتصال."); playDynamicSFX("input_error",0,1,0,0.45); say(message or "تعذر إتمام تسجيل الدخول. حاول مرة أخرى.",true)
      Handler().postDelayed(Runnable({run=function() if startupGateActive then showAuthEntry() end end}),900); return
    end
    setVerificationProgress(55,"تم التحقق من بيانات الدخول. جاري استعادة الحساب..."); finishFirebaseLogin()
  end
  local started,err=pcall(function() if isNew then firebaseAuthSignUp(e,p,finish) else firebaseAuthSignIn(e,p,finish) end end)
  if not started then finish(false,"تعذر إنشاء الحساب أو استعادته. تحقق من الاتصال ثم حاول مرة أخرى.") end
end
function chooseGender(onDone)
  showInternalPanel("اختر النوع",function(parent)
    local options={"ذكر","أنثى"}; local spinner=Spinner(activity); spinner.setAdapter(ArrayAdapter(activity,android.R.layout.simple_spinner_dropdown_item,options)); spinner.setSelection(playerStats.gender=="أنثى" and 1 or 0); parent.addView(spinner); return {spinner}
  end,function(values) local value=tostring(values[1].getSelectedItem() or "ذكر"); if onDone then onDone(value) end end)
end
local function chooseAccessibility(onDone)
  showInternalPanel("طريقة الاستخدام",function(parent)
    local options={"وضع المبصرين","وضع المكفوفين"}; local spinner=Spinner(activity); spinner.setAdapter(ArrayAdapter(activity,android.R.layout.simple_spinner_dropdown_item,options)); spinner.setSelection(Settings.accessibilityMode=="وضع المكفوفين" and 1 or 0); parent.addView(spinner); return {spinner}
  end,function(values) local mode=tostring(values[1].getSelectedItem() or "وضع المبصرين"); Settings.accessibilityMode=mode; Settings.doubleTapMenus=mode=="وضع المكفوفين"; saveGameData(); if onDone then onDone(mode) end end)
end
function showLanguagePicker(onDone)
  if screen_status then screen_status.setText("") end
  showInternalPanel("اختيار اللغة",function(parent)
    local note=TextView(activity); note.setText("اختر لغة الواجهة والنطق ثم اضغط موافق."); note.setTextColor(0xFFE6EEFF); note.setTextSize(17); parent.addView(note)
    local options={"العربية","English"}; local spinner=Spinner(activity); spinner.setContentDescription("اللغة"); spinner.setAdapter(ArrayAdapter(activity,android.R.layout.simple_spinner_dropdown_item,options)); spinner.setSelection(Settings.language=="English" and 1 or 0); parent.addView(spinner); return {spinner}
  end,function(values)
    local language=tostring(values[1].getSelectedItem() or "العربية"); Settings.language=language; Settings.languageSelected=true; if setSpeechLanguage then setSpeechLanguage(language) end; saveGameData(); say(language=="English" and "English language selected." or "تم اختيار اللغة العربية.",true); if onDone then onDone(language) end
  end,function()
    say("يجب اختيار لغة للمتابعة.",true); Handler().postDelayed(Runnable({run=function() showLanguagePicker(onDone) end}),250)
  end)
end
function showProfileWizard(isNewAccount, onComplete)
  showInternalPanel("إكمال الملف الشخصي",function(parent)
    local name=makeInput("الاسم الكامل",playerStats.name,false); local username=makeInput("اسم المستخدم، أحرف إنجليزية فقط",playerStats.username,false); local statusInput=makeInput("الحالة",playerStats.status or "متسابق محترف",false); local bio=makeInput("الوصف",playerStats.bio or "متسابق محترف",false)
    parent.addView(name); parent.addView(username); parent.addView(statusInput); parent.addView(bio)
    local genders={"ذكر","أنثى"}; local genderSpinner=Spinner(activity); genderSpinner.setContentDescription("النوع"); genderSpinner.setAdapter(ArrayAdapter(activity,android.R.layout.simple_spinner_dropdown_item,genders)); genderSpinner.setSelection(playerStats.gender=="أنثى" and 1 or 0); parent.addView(genderSpinner)
    local modes={"وضع المبصرين","وضع المكفوفين"}; local modeSpinner=Spinner(activity); modeSpinner.setContentDescription("طريقة الاستخدام"); modeSpinner.setAdapter(ArrayAdapter(activity,android.R.layout.simple_spinner_dropdown_item,modes)); modeSpinner.setSelection(Settings.accessibilityMode=="وضع المكفوفين" and 1 or 0); parent.addView(modeSpinner)
    attachGameKeyboard(parent,{name,username,statusInput,bio})
    return {name,username,statusInput,bio,genderSpinner,modeSpinner}
  end,function(values)
    local name,username,statusInput,bio=values[1],values[2],values[3],values[4]; local genderSpinner,modeSpinner=values[5],values[6]
    local n,u,s,b=textOf(name),textOf(username),textOf(statusInput),textOf(bio)
    if n=="" then say("يجب إدخال الاسم.",true); showProfileWizard(isNewAccount,onComplete); return end
    if not validUsername(u) then say("اسم المستخدم يجب أن يتكون من ثلاثة أحرف إنجليزية على الأقل، من دون مسافات أو رموز.",true); showProfileWizard(isNewAccount,onComplete); return end
    firebaseCheckUsername(u,function(ok,taken,body)
      if not ok then say("تعذر التحقق من توفر اسم المستخدم.",true); return end
      local owner=body and body:match('"uid"%s*:%s*"(.-)"')
      if taken and owner~=playerStats.localId then say("اسم المستخدم مستخدم بالفعل.",true); showProfileWizard(isNewAccount,onComplete); return end
      local previous=playerStats.username; playerStats.username=u
      local apply=function() playerStats.name=n; playerStats.status=s~="" and s or "متسابق محترف"; playerStats.bio=b~="" and b or "متسابق محترف"; playerStats.gender=tostring(genderSpinner.getSelectedItem() or "ذكر"); Settings.accessibilityMode=tostring(modeSpinner.getSelectedItem() or "وضع المبصرين"); Settings.doubleTapMenus=Settings.accessibilityMode=="وضع المكفوفين"; saveGameData(); if onComplete then onComplete() end end
      if taken then apply() else firebaseClaimUsername(u,function(claimed) if claimed then apply() else playerStats.username=previous; say("تعذر اعتماد اسم المستخدم حاليًا.",true); showProfileWizard(isNewAccount,onComplete) end end) end
    end)
  end,function() say("تم إلغاء إعداد الملف الشخصي.",true) end)
end
function showTermsIfNeeded(onAccepted)
  if Settings.termsAccepted==true then if onAccepted then onAccepted() end; return end
  if screen_status then screen_status.setText("") end
  showInternalPanel("شروط الاستخدام والخصوصية",function(parent)
    local scroll=ScrollView(activity); scroll.setFillViewport(true)
    local body=TextView(activity); body.setTextSize(17); body.setTextColor(0xFFE6EEFF); body.setPadding(10,8,10,18); body.setContentDescription("شروط الاستخدام والخصوصية")
    body.setText("مرحبًا بك في World Racing.\n\nباستخدامك التطبيق، تقر بأنك قرأت الشروط الآتية وتوافق عليها:\n\nالاستخدام المسؤول\nتلتزم باستخدام حسابك بطريقة قانونية ومحترمة، وتمتنع عن الإساءة أو الإزعاج أو انتحال شخصية الآخرين أو استغلال أي خلل في اللعبة.\n\nالحساب والبيانات\nأنت مسؤول عن صحة بيانات حسابك وعن المحافظة على وسائل الدخول إليه. تُستخدم بيانات الحساب والتقدم والإعدادات اللازمة لتشغيل اللعبة ومزامنتها بين أجهزتك. لا تُحفظ كلمة المرور الخام داخل التطبيق.\n\nالتواصل والمجتمع\nتلتزم باحترام اللاعبين في الرسائل والمحادثات والمكالمات والردود الصوتية. يجوز الإبلاغ عن المحتوى المسيء أو حظر صاحبه وفق أدوات المجتمع وإدارة الغرف.\n\nالمزامنة والاتصال\nتحتاج ميزات الغرف والسباقات الجماعية والرسائل الصوتية إلى اتصال بالإنترنت. قد تتأثر سرعة المزامنة بجودة الشبكة، وتُحذف الرسائل الصوتية القديمة تلقائيًا عند بلوغ الحد المخصص للتخزين.\n\nالخصوصية والأمان\nلا تشارك كلمة المرور أو رموز المصادقة مع أي شخص. ويُطلب إذن الميكروفون عند الحاجة إلى التسجيل فقط، ويمكنك إيقاف التسجيل متى شئت.\n\nالتحديثات\nقد نُجري تحسينات أو تغييرات ضرورية على الميزات والأصوات وآليات الحماية، مع الحفاظ على بيانات الحساب قدر الإمكان.\n\nباختيارك «موافق»، تؤكد أنك فهمت هذه الشروط وتوافق على متابعة استخدام World Racing.")
    scroll.addView(body,LinearLayout.LayoutParams(-1,-2)); parent.addView(scroll,LinearLayout.LayoutParams(-1,0,1))
    local agree=CheckBox(activity); agree.setText("أوافق على شروط الاستخدام والخصوصية"); agree.setContentDescription("أوافق على شروط الاستخدام والخصوصية"); parent.addView(agree); return {agree}
  end,function(values)
    if not values[1].isChecked() then say("يرجى تحديد خيار الموافقة قبل المتابعة.",true); showTermsIfNeeded(onAccepted); return end
    Settings.termsAccepted=true; saveGameData(); playDynamicSFX("terms_accept",0,1,0,0.95); if onAccepted then onAccepted() end
  end,function() pcall(function() activity.finish() end) end)
end
readerScreenWarningActive=false
function showReaderScreenWarning()
  readerScreenWarningActive=true
  local message="يوجد قارئ شاشة داخلي داخل اللعبة. يُرجى إيقاف قارئ الشاشة الخاص بالجهاز إذا رغبت في استخدام الناطق الداخلي فقط. المس الشاشة لإخفاء هذه الرسالة."
  if screen_status then
    screen_status.setText(message); screen_status.setContentDescription(message); screen_status.setClickable(true)
    screen_status.setOnTouchListener(function() readerScreenWarningActive=false; screen_status.setText(""); screen_status.setContentDescription(""); screen_status.setClickable(false); pcall(refreshVisibleScreen); return true end)
  end
  say(message,true)
end
pendingGoogleAccountName=nil
local googleSignInInFlight=false
local function googleFallbackSecret(accountName)
  local email=string.lower(tostring(accountName or ""))
  if playerStats.googleFallbackEmail==email and tostring(playerStats.googleFallbackPassword or "")~="" then return playerStats.googleFallbackPassword end
  local entropy=email..":"..tostring(os.time())..":"..tostring(math.random())..":"..tostring(UUID and UUID.randomUUID and UUID.randomUUID() or System.currentTimeMillis())
  local secret=sha256(entropy):sub(1,32)
  playerStats.googleFallbackEmail=email; playerStats.googleFallbackPassword=secret; saveGameData()
  return secret
end
local googleTokenRetryCount=0
local googleAuthScope="oauth2:https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile"
local function finishGoogleToken(accountName,token,manager)
  token=tostring(token or "")
  if token=="" then
    say("تعذر الحصول على رمز Google الرسمي. جاري تسجيل الدخول السلس عبر البريد الإلكتروني للحساب المختار.",true)
    local generatedPassword = googleFallbackSecret(accountName)
    setVerificationProgress(20,"جاري تسجيل الدخول السلس...")
    firebaseAuthSignIn(accountName, generatedPassword, function(ok, msg)
      if ok then
        googleSignInInFlight=false; setVerificationProgress(100,"تم تسجيل الدخول السلس."); finishFirebaseLogin()
      else
        setVerificationProgress(40,"جاري إنشاء حساب جديد مرتبط بـ Google...")
        firebaseAuthSignUp(accountName, generatedPassword, function(ok2, msg2)
          googleSignInInFlight=false
          if ok2 then setVerificationProgress(100,"تم إنشاء الحساب السلس."); finishFirebaseLogin()
          else finishVerification(false, msg2 or "تعذر تسجيل الدخول."); say("تعذر تسجيل الدخول السلس. استخدم تسجيل الدخول بالبريد.",true) end
        end)
      end
    end)
    return
  end
  setVerificationProgress(35,"تم الحصول على رمز Google. جاري مزامنة الملف...")
  firebaseSignInWithGoogleToken(token,accountName,function(authOk,message)
    if not authOk and manager and googleTokenRetryCount<1 then
      googleTokenRetryCount=googleTokenRetryCount+1; pcall(function() manager.invalidateAuthToken("com.google",token) end); say("انتهت صلاحية رمز Google، جارٍ طلب رمز جديد.",true); Handler().postDelayed(Runnable({run=function() firebaseGoogleAccountSelected(accountName) end}),350); return
    end
    googleSignInInFlight=false
    if not authOk then finishVerification(false,message or "تعذر تسجيل الدخول عبر Google."); say(message or "تعذر تسجيل الدخول عبر Google. تأكد من تفعيل Google في Firebase.",true); return end
    setVerificationProgress(70,"جاري تجهيز ملفك..."); finishFirebaseLogin()
  end)
end
function firebaseGoogleConsentResult(accountName,token)
  accountName=tostring(accountName or pendingGoogleAccountName or ""); pendingGoogleAccountName=nil
  if accountName=="" then googleSignInInFlight=false; finishVerification(false,"لم يُحدد حساب Google."); say("لم يُحدد حساب Google.",true); return end
  token=tostring(token or "")
  if token=="" then googleSignInInFlight=false; finishVerification(false,"لم تكتمل موافقة Google."); say("لم تكتمل موافقة Google أو لم تُرجع رمزًا. اختر الحساب مرة أخرى.",true); return end
  finishGoogleToken(accountName,token,AccountManager.get(activity))
end
local requestGoogleToken
function resumeGoogleAccountAfterConsent()
  local accountName=tostring(pendingGoogleAccountName or ""); pendingGoogleAccountName=nil
  if accountName=="" then googleSignInInFlight=false; finishVerification(false,"لم يُحدد حساب Google بعد الموافقة."); say("لم يُحدد حساب Google بعد الموافقة.",true); return end
  say("تمت الموافقة. جارٍ طلب رمز Google الآن.",true); Handler().postDelayed(Runnable({run=function() requestGoogleToken(accountName) end}),180)
end
requestGoogleToken=function(accountName)
  local manager=AccountManager.get(activity); local account=Account(accountName,"com.google"); local options=Bundle(); local callback=AccountManagerCallback({run=function(future)
    local ok,result=pcall(function() return future.getResult() end)
    if not ok or not result then googleSignInInFlight=false; finishVerification(false,"تعذر الحصول على رمز Google من مدير الحسابات."); say("تعذر الحصول على رمز Google. تأكد من اتصال الجهاز بخدمات Google ثم أعد المحاولة.",true); return end
    local consent=nil; pcall(function() consent=result.getParcelable(AccountManager.KEY_INTENT) end)
    if consent then pendingGoogleAccountName=accountName; activity.startActivityForResult(consent,9234); return end
    local token=""; pcall(function() token=tostring(result.getString(AccountManager.KEY_AUTHTOKEN) or "") end)
    finishGoogleToken(accountName,token,manager)
  end})
  local handler=Handler(Looper.getMainLooper())
  local started=pcall(function() manager.getAuthToken(account,googleAuthScope,options,activity,callback,handler) end)
  if not started then googleSignInInFlight=false; finishVerification(false,"لا يمكن فتح خدمة حساب Google على هذا الجهاز."); say("لا يمكن فتح خدمة حساب Google على هذا الجهاز.",true) end
end
function firebaseGoogleAccountSelected(accountName)
  if googleSignInInFlight then say("جاري تسجيل الدخول عبر Google.",true); return end
  accountName=tostring(accountName or "")
  if accountName=="" then say("لم يتم اختيار حساب Google.",true); return end
  googleSignInInFlight=true; googleTokenRetryCount=0; beginVerification(); setVerificationProgress(0,"جاري التحقق من حساب Google... 0%"); say("جاري التحقق من حساب Google، يرجى الانتظار.",true)
  Thread(Runnable({run=function() requestGoogleToken(accountName) end})).start()
end
function startGoogleAccountPicker()
  local ok,intent=pcall(function() return AccountManager.newChooseAccountIntent(nil,nil,{"com.google"},"اختر حساب Google لتسجيل الدخول",nil,nil,nil) end)
  if not ok or not intent then say("لا تتوفر حسابات Google قابلة للاختيار على هذا الجهاز.",true); return end
  activity.startActivityForResult(intent,9233)
end
function showAuthEntry()
  startupGateActive=true; readerScreenWarningActive=false
  activity.setRequestedOrientation(1); currentInternalDialog=true
  if screen_title then screen_title.setText("World Racing") end; if screen_status then screen_status.setText("") end; if screen_hint then screen_hint.setText("") end; if race_controls then race_controls.setVisibility(View.GONE) end; if race_actions_container then race_actions_container.setVisibility(View.GONE) end; if race_car_image then race_car_image.setVisibility(View.GONE) end
  screen_options_container.removeAllViews(); screen_options_container.setVisibility(View.VISIBLE)
  local root=LinearLayout(activity); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(28,24,28,24); root.setGravity(Gravity.CENTER_HORIZONTAL)
  local title=TextView(activity); title.setText("تسجيل الدخول إلى World Racing"); title.setTextSize(24); title.setTextColor(0xFFFFFFFF); title.setGravity(Gravity.CENTER); root.addView(title,LinearLayout.LayoutParams(-1,-2))
  local email=makeInput("البريد الإلكتروني",playerStats.accountEmail or "",false); local password=makeInput("كلمة المرور","",true); root.addView(email,LinearLayout.LayoutParams(-1,-2)); root.addView(password,LinearLayout.LayoutParams(-1,-2)); attachGameKeyboard(root,{email,password})
  local loginButton=Button(activity); loginButton.setText("تسجيل الدخول"); loginButton.setAllCaps(false); loginButton.setContentDescription("تسجيل الدخول"); root.addView(loginButton,LinearLayout.LayoutParams(-1,-2))
  local separator=TextView(activity); separator.setText("أو"); separator.setTextSize(16); separator.setGravity(Gravity.CENTER); separator.setTextColor(0xFFB8C7DF); root.addView(separator,LinearLayout.LayoutParams(-1,-2))
  local googleButton=Button(activity); googleButton.setText("تسجيل الدخول عبر Google"); googleButton.setAllCaps(false); googleButton.setContentDescription("تسجيل الدخول عبر Google"); root.addView(googleButton,LinearLayout.LayoutParams(-1,-2))
  local createButton=Button(activity); createButton.setText("إنشاء حساب جديد"); createButton.setAllCaps(false); createButton.setContentDescription("إنشاء حساب جديد"); root.addView(createButton,LinearLayout.LayoutParams(-1,-2))
  local recoverButton=Button(activity); recoverButton.setText("استعادة الحساب"); recoverButton.setAllCaps(false); recoverButton.setContentDescription("استعادة الحساب"); root.addView(recoverButton,LinearLayout.LayoutParams(-1,-2))
  loginButton.setOnClickListener(function() beginEmailAccountFlow(false,textOf(email),textOf(password),loginButton) end)
  googleButton.setOnClickListener(function() startGoogleAccountPicker() end)
  createButton.setOnClickListener(function() beginEmailAccountFlow(true,textOf(email),textOf(password),createButton) end)
  recoverButton.setOnClickListener(function()
    local e=string.lower(tostring(textOf(email) or "")); if not validEmail(e) then say("أدخل البريد الإلكتروني أولًا لاستعادة الحساب.",true); return end
    recoverButton.setEnabled(false); beginVerification(); setVerificationProgress(0,"جاري إرسال رسالة الاستعادة... 0%"); say("جاري إرسال رابط إعادة تعيين كلمة المرور إلى بريدك الإلكتروني.",true); firebaseSendPasswordReset(e,function(ok,message) recoverButton.setEnabled(true); if ok then setVerificationProgress(100,"تم إرسال رسالة الاستعادة."); say("تم إرسال الرابط بنجاح. تحقق من بريدك الإلكتروني.",true) else finishVerification(false,message); say(message or "تعذر إرسال رسالة الاستعادة.",true) end end)
  end)
  screen_options_container.addView(root,LinearLayout.LayoutParams(-1,-1)); email.requestFocus(); say("أدخل البريد الإلكتروني وكلمة المرور، ثم اختر تسجيل الدخول.",true)
end
function showLoginDialog() showAuthEntry() end
function dismissAuthEntry() currentInternalDialog=nil; if screen_options_container then screen_options_container.removeAllViews(); pcall(refreshVisibleScreen) end end
function showProfileEditor()
  showInternalPanel("ملفك الشخصي",function(parent)
    local name=makeInput("الاسم",playerStats.name,false); local username=makeInput("اسم المستخدم",playerStats.username,false); local bio=makeInput("الحالة والوصف",playerStats.bio,false); parent.addView(name); parent.addView(username); parent.addView(bio)
    local genders={"ذكر","أنثى"}; local gender=Spinner(activity); gender.setAdapter(ArrayAdapter(activity,android.R.layout.simple_spinner_dropdown_item,genders)); gender.setSelection(playerStats.gender=="أنثى" and 1 or 0); parent.addView(gender); attachGameKeyboard(parent,{name,username,bio}); return {name,username,bio,gender}
  end,function(values)
    local n,u,b=textOf(values[1]),textOf(values[2]),textOf(values[3]); if n=="" or not validUsername(u) then say("راجع الاسم واسم المستخدم.",true); showProfileEditor(); return end
    firebaseCheckUsername(u,function(ok,taken) if not ok or (taken and u~=playerStats.username) then say("اسم المستخدم غير متاح.",true); showProfileEditor(); return end; playerStats.name=n; playerStats.username=u; playerStats.bio=b; playerStats.status=b; playerStats.gender=tostring(values[4].getSelectedItem() or "ذكر"); saveGameData(); firebaseSyncProfile(function() end); say("تم حفظ الملف الشخصي.",true); loadSection("PROFILE") end)
  end,function() loadSection("PROFILE") end)
end
function showKeyboardModeDialog()
  showInternalPanel("طريقة الإدخال",function(parent) local options={"نظام","داخلية"}; local spinner=Spinner(activity); spinner.setAdapter(ArrayAdapter(activity,android.R.layout.simple_spinner_dropdown_item,options)); spinner.setSelection(Settings.keyboardMode=="داخلية" and 1 or 0); parent.addView(spinner); return {spinner} end,function(values) Settings.keyboardMode=tostring(values[1].getSelectedItem() or "نظام"); saveGameData(); say("تم تغيير لوحة المفاتيح.",true) end)
end
function showAccessibilityModeDialog()
  chooseAccessibility(function(mode) saveGameData(); say("تم تغيير وضع العرض إلى " .. mode, true); refreshVisibleScreen() end)
end
function showChangePasswordDialog()
  showInternalPanel("تغيير كلمة المرور",function(parent) local current=makeInput("كلمة المرور الحالية","",true); local fresh=makeInput("كلمة المرور الجديدة، ستة أحرف على الأقل","",true); local confirm=makeInput("تأكيد كلمة المرور الجديدة","",true); parent.addView(current); parent.addView(fresh); parent.addView(confirm); attachGameKeyboard(parent,{current,fresh,confirm}); return {current,fresh,confirm} end,function(values) local a,b,c=textOf(values[1]),textOf(values[2]),textOf(values[3]); if #b<6 or b~=c then say("كلمة المرور الجديدة غير صالحة أو التأكيد غير مطابق.",true); return end; firebaseChangePassword(a,b,function(ok,msg) say(msg or (ok and "تم." or "تعذر التغيير."),true) end) end)
end
function chooseControlModeDialog()
  showInternalPanel("نمط التحكم",function(parent) local options={"الإيماءات فقط","الأزرار فقط","الإيماءات والأزرار"}; local spinner=Spinner(activity); spinner.setAdapter(ArrayAdapter(activity,android.R.layout.simple_spinner_dropdown_item,options)); for i,v in ipairs(options) do if v==(Settings.controlMode or options[3]) then spinner.setSelection(i-1) end end; parent.addView(spinner); return {spinner} end,function(values) Settings.controlMode=tostring(values[1].getSelectedItem() or "الإيماءات والأزرار"); saveGameData(); say("نمط التحكم: "..Settings.controlMode,true) end)
end
function chooseSteeringModeDialog()
  showInternalPanel("طريقة التوجيه",function(parent) local options={"السحب","الإمالة","مختلط"}; local spinner=Spinner(activity); spinner.setAdapter(ArrayAdapter(activity,android.R.layout.simple_spinner_dropdown_item,options)); for i,v in ipairs(options) do if v==(Settings.steeringMode or options[1]) then spinner.setSelection(i-1) end end; parent.addView(spinner); return {spinner} end,function(values) if setSteeringMode then setSteeringMode(tostring(values[1].getSelectedItem() or "السحب")) else Settings.steeringMode=tostring(values[1].getSelectedItem() or "السحب"); saveGameData() end end)
end
function chooseTiltSensitivityDialog()
  showInternalPanel("حساسية الإمالة",function(parent) local options={"منخفضة","متوسطة","عالية"}; local spinner=Spinner(activity); spinner.setAdapter(ArrayAdapter(activity,android.R.layout.simple_spinner_dropdown_item,options)); local current=(Settings.tiltSensitivity or 2.4)<=1.8 and 0 or ((Settings.tiltSensitivity or 2.4)>=3.0 and 2 or 1); spinner.setSelection(current); parent.addView(spinner); return {spinner} end,function(values) local value=tostring(values[1].getSelectedItem() or "متوسطة"); Settings.tiltSensitivity=value=="منخفضة" and 3.2 or (value=="عالية" and 1.6 or 2.4); saveGameData(); playDynamicSFX("settings_change",0,1,0,0.68); say("حساسية الإمالة: "..value,true) end)
end
function chooseOrientationDialog()
  showInternalPanel("اتجاه الشاشة",function(parent) local options={"أفقي","عمودي","تلقائي"}; local spinner=Spinner(activity); spinner.setAdapter(ArrayAdapter(activity,android.R.layout.simple_spinner_dropdown_item,options)); for i,v in ipairs(options) do if v==(Settings.orientationMode or options[1]) then spinner.setSelection(i-1) end end; parent.addView(spinner); return {spinner} end,function(values) setOrientationMode(tostring(values[1].getSelectedItem() or "أفقي")) end)
end
local function openExternalLink(url,label)
  local ok=pcall(function() activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) end)
  if not ok then say("تعذر فتح "..label..".",true) else say("تم فتح "..label..".",true) end
end
function openWhatsAppContact() openExternalLink("https://wa.me/201152187945","رابط التواصل عبر واتساب") end
function openUpdatesChannel() openExternalLink("https://t.me/xfxxxfg","قناة التحديثات") end
function openCommunityGroup() openExternalLink("https://t.me/+1e21nqFqxD43OTY0","مجموعة المجتمع") end
function showTermsFromAbout() Settings.termsAccepted=false; showTermsIfNeeded(function() say("تمت مراجعة شروط الاستخدام.",true) end) end
local function versionCodeOf(version)
  local a,b,c=tostring(version or "0"):match("^(%d+)[%.%-]?(%d*)[%.%-]?(%d*)")
  return (tonumber(a) or 0)*1000000+(tonumber(b) or 0)*1000+(tonumber(c) or 0)
end
function checkForUpdates()
  say("جاري البحث عن تحديثات.",true)
  firebaseGet("/app/update",function(ok,body)
    if not ok or not body or body=="null" then say("لا توجد بيانات تحديث منشورة حاليًا.",true); return end
    local root=nil; pcall(function() root=JSONObject(body) end)
    local publishedVersion=root and root.optString("version","") or ""; local publishedCode=root and root.optInt("versionCode",versionCodeOf(publishedVersion)) or versionCodeOf(publishedVersion)
    local currentCode=versionCodeOf(appver or "0")
    if publishedVersion=="" or publishedCode<=currentCode then say("أنت تستخدم أحدث إصدار منشور.",true); return end
    local message=root.optString("message",root.optString("notes","تحديث جديد متاح.")); local channelUrl=root.optString("channelUrl","https://t.me/xfxxxfg"); local actionUrl=root.optString("downloadUrl",root.optString("url",channelUrl)); local title=root.optString("title","إصدار جديد متاح!")
    playDynamicSFX("connection_ok",0,1,0,0.62); say(title.." الإصدار "..publishedVersion.." متاح الآن.",true)
    showInternalConfirm(title,message.."\nالإصدار: "..publishedVersion.."\n\nعند اختيار تحديث الآن سيتم فتح قناة التحديثات.",function() openExternalLink(actionUrl,"رابط التحديث") end,function() say("سأذكرك بالتحديث لاحقًا.",true) end)
  end)
end
