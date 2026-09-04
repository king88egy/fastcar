-- LoginSystem.lua
-- بوابة التحقق والمزامنة الداخلية بالكامل.
local verificationStartedAt = 0

function setVerificationProgress(value, message)
  value = math.max(0, math.min(100, tonumber(value) or 0))
  local bar = activity.findViewById(28000)
  if bar then pcall(function() bar.setProgress(value) end) end
  local status = activity.findViewById(28001)
  if status then pcall(function() status.setText(message or ("جاري التحميل... " .. value .. "%")) end) end
end

function beginVerification()
  if screen_status then screen_status.setText("") end
  if not screen_options_container then return end
  screen_options_container.removeAllViews()
  screen_options_container.setVisibility(View.VISIBLE)
  
  local root = LinearLayout(activity); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(40, 40, 40, 40); root.setGravity(Gravity.CENTER)
  screen_options_container.addView(root, LinearLayout.LayoutParams(-1, -1))
  
  local title = TextView(activity); title.setText("World Racing"); title.setTextSize(26); title.setTextColor(0xFFFFFFFF); root.addView(title)
  local status = TextView(activity); status.setId(28001); status.setText("جاري التحميل، يرجى الانتظار..."); status.setTextColor(0xFFA9C7FF); status.setPadding(0, 20, 0, 20); root.addView(status)
  
  local bar = ProgressBar(activity, nil, android.R.attr.progressBarStyleHorizontal); bar.setId(28000); bar.setMax(100); bar.setProgress(10); root.addView(bar, LinearLayout.LayoutParams(-1, 24))
  
  verificationStartedAt = System.currentTimeMillis()
  say("جاري التحميل، يرجى الانتظار.", true)
end

function finishVerification(success, message)
  if success then
    setVerificationProgress(100, "تم بنجاح!")
    Handler().postDelayed(Runnable({run=function()
      screen_options_container.removeAllViews()
      pcall(refreshVisibleScreen)
    end}), 1200)
  else
    setVerificationProgress(0, message or "فشلت المزامنة.")
    local btn = Button(activity); btn.setText("إعادة المحاولة"); btn.setOnClickListener(function() startStrictSession() end)
    local root = activity.findViewById(28001).getParent(); root.addView(btn, LinearLayout.LayoutParams(-1, -2))
    local btnOut = Button(activity); btnOut.setText("تسجيل الخروج"); btnOut.setOnClickListener(function() firebaseSignOut(); showAuthEntry() end)
    root.addView(btnOut, LinearLayout.LayoutParams(-1, -2))
  end
end

function completeLoginToMenu()
  startupGateActive=false; applyOrientationMode(); loadSection("MENU"); playDynamicSFX("connection_ok",0,1,0,0.82); say("تم تسجيل الدخول بنجاح.",true); if showReaderScreenWarning then showReaderScreenWarning() end
end

local function clearSessionForLogin()
  playerStats.sessionActive=false; playerStats.idToken=""; playerStats.refreshToken=""; saveGameData()
end
local function returnToLogin(message)
  clearSessionForLogin(); if message and message~="" then say(message,true) end
  Handler().postDelayed(Runnable({run=function() showAuthEntry() end}),850)
end

function finishFirebaseLogin()
  beginVerification()
  setVerificationProgress(30, "جاري جلب ملفك الشخصي...")
  firebaseSyncProfile(function(ok)
    if ok then
      if playerStats.name == "" or playerStats.username == "" then
        showProfileWizard(true, function() finishFirebaseLogin() end)
      else
        setVerificationProgress(90, "تجهيز القائمة...")
        if firebaseStartSocialSession then firebaseStartSocialSession() end
        finishVerification(true)
        completeLoginToMenu()
      end
    else
      finishVerification(false, "تعذر جلب البيانات. تأكد من الإنترنت."); returnToLogin("تعذر استعادة الجلسة. سجّل الدخول مرة أخرى.")
    end
  end)
end

function startStrictSession()
  startupGateActive=true
  if Settings.languageSelected ~= true then
    showLanguagePicker(function() startStrictSession() end)
    return
  end
  if Settings.termsAccepted ~= true then
    showTermsIfNeeded(function() startStrictSession() end)
    return
  end
  if playerStats.sessionActive and playerStats.accountEmail ~= "" and playerStats.localId ~= "" then
    beginVerification()
    setVerificationProgress(0, "جاري التحقق من الحساب... 0%")
    firebaseRefreshSession(function(ok)
      if ok then
        finishFirebaseLogin()
      else
        finishVerification(false, "انتهت صلاحية الجلسة. يرجى تسجيل الدخول."); returnToLogin("انتهت صلاحية الجلسة. سجّل الدخول مرة أخرى.")
      end
    end)
  else
    activity.setRequestedOrientation(1)
    showAuthEntry()
  end
end

startStrictSession()
