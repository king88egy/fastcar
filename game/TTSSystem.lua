-- ==========================================
-- 5. TTSSystem.lua (نظام النطق المتقدم)
-- ==========================================
currentSection = "LOGIN"
currentMenuOptions = {}
currentMenuIndex = 1

local function localeForLanguage(language) return tostring(language or "العربية")=="English" and Locale("en") or Locale("ar") end
function setSpeechLanguage(language)
  Settings.language=tostring(language or Settings.language or "العربية")
  pcall(function() if tts then tts.setLanguage(localeForLanguage(Settings.language)); tts.setSpeechRate(math.max(0.05,tonumber(Settings.voiceSpeed) or 1.0)); tts.setPitch(Settings.voicePitch or 1.0) end end)
end
tts = TextToSpeech(activity, function(status)
  if status == TextToSpeech.SUCCESS then
    pcall(function() local attributes=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(); tts.setAudioAttributes(attributes) end)
    setSpeechLanguage(Settings.language or "العربية")
  end
end)

function say(text, interrupt)
  pcall(function()
    -- الناطق الداخلي خاص بوضع المكفوفين؛ في وضع المبصرين تبقى القراءة لقارئ TalkBack الخارجي.
    if Settings.accessibilityMode == "وضع المكفوفين" and Settings.ttsEnabled and tts ~= nil then
      local mode = interrupt and TextToSpeech.QUEUE_FLUSH or TextToSpeech.QUEUE_ADD
      tts.speak(tostring(text), mode, nil)
    end
  end)
end