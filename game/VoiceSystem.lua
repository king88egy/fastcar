-- VoiceSystem.lua
-- بث صوتي جماعي شبه مباشر على شكل مقاطع قصيرة، مع أرشفة الرسائل الصوتية.
-- لا يدّعي هذا المسار أنه WebRTC؛ كل دفعة تُرسل عبر قناة الغرفة وتُشغّل عند وصولها.
VoiceSystem = {recording=false, streaming=false, recorder=nil, path="", channel="", target="", session="", seq=0, polling=false, liveSeen={}, lastMessageKey="", lastTap=0, pendingPermissionCallback=nil}

local STREAM_CHUNK_MS = 1100
local STREAM_MAX_MS = 30000
local STREAM_AUDIO_RATE = 48000
local STREAM_AUDIO_BITRATE = 192000
local STREAM_AUDIO_CHANNELS = 1

local function voiceChannel()
  if Multiplayer.currentRoom and Multiplayer.currentRoom ~= "" then return "room_" .. firebaseEncodeKey(Multiplayer.currentRoom) end
  if Multiplayer.directCallTarget and Multiplayer.directCallTarget ~= "" then
    local a,b=playerStats.username,Multiplayer.directCallTarget; if a>b then a,b=b,a end
    return "direct_" .. firebaseEncodeKey(a .. "_" .. b)
  end
  return "user_" .. firebaseEncodeKey(playerStats.username)
end

local function uniqueId(prefix)
  return tostring(prefix or "voice") .. "_" .. tostring(os.time()) .. "_" .. tostring(math.random(1000,9999))
end

local function localVoicePath(prefix, seq)
  return activity.getCacheDir().getAbsolutePath() .. "/arabiyat_" .. tostring(prefix or "voice") .. "_" .. tostring(seq or 0) .. ".aac"
end

local function encodeFile(path)
  local stream=FileInputStream(File(path)); local buffer=ByteArrayOutputStream(); local byte=stream.read()
  while byte and byte ~= -1 do buffer.write(byte); byte=stream.read() end
  stream.close(); return Base64.encodeToString(buffer.toByteArray(), Base64.NO_WRAP)
end

local function deleteFile(path) pcall(function() if path and path~="" then File(path).delete() end end) end

local function startRecorder(path)
  local ok,err=pcall(function()
    local recorder=MediaRecorder(); recorder.setAudioSource(MediaRecorder.AudioSource.MIC); recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS); recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); pcall(function() recorder.setAudioChannels(STREAM_AUDIO_CHANNELS) end); recorder.setAudioEncodingBitRate(STREAM_AUDIO_BITRATE); recorder.setAudioSamplingRate(STREAM_AUDIO_RATE); recorder.setMaxDuration(STREAM_CHUNK_MS+500); recorder.setOutputFile(path); recorder.prepare(); recorder.start(); VoiceSystem.recorder=recorder; VoiceSystem.path=path
  end)
  if not ok then VoiceSystem.recorder=nil; deleteFile(path); return false,err end
  return true
end

local function encodeAndSendLive(path, seq, finalChunk)
  if not path or path=="" then return end
  local ok,data=pcall(function() return encodeFile(path) end); deleteFile(path)
  if not ok or not data or #data==0 or not VoiceSystem.channel or VoiceSystem.channel=="" or not VoiceSystem.session or VoiceSystem.session=="" then return end
  local payload=firebaseJsonObject({from=playerStats.username,name=playerStats.name,session=VoiceSystem.session,seq=seq or 0,mime="audio/aac",codec="aac-lc",sampleRate=STREAM_AUDIO_RATE,bitrate=STREAM_AUDIO_BITRATE,channels=STREAM_AUDIO_CHANNELS,data=data,sentAt=os.time(),final=finalChunk==true})
  firebasePut("/voiceLive/"..VoiceSystem.channel.."/"..VoiceSystem.session.."/"..tostring(seq or 0),payload,function() end)
  if finalChunk then
    local archiveKey=tostring(os.time()).."_"..firebaseEncodeKey(playerStats.username).."_"..tostring(seq or 0); local estimatedBytes=math.floor(#data*0.75); playerStats.voiceStorageBytes=math.max(0,(playerStats.voiceStorageBytes or 0)+estimatedBytes)
    firebasePut("/voiceMessages/"..VoiceSystem.channel.."/"..archiveKey,payload,function(ok) if ok and firebaseEnforceVoiceStorageLimit then firebaseEnforceVoiceStorageLimit(VoiceSystem.channel) end; if ok and VoiceSystem.target and VoiceSystem.target~="" and (not Multiplayer.currentRoom or Multiplayer.currentRoom=="") and firebaseSendPrivateVoiceNotice then firebaseSendPrivateVoiceNotice(VoiceSystem.target,VoiceSystem.channel,archiveKey,function() end) end end)
  end
end

local function rotateLiveChunk()
  if not VoiceSystem.streaming then return end
  local oldRecorder=VoiceSystem.recorder; local oldPath=VoiceSystem.path; VoiceSystem.recorder=nil; VoiceSystem.path=""
  pcall(function() if oldRecorder then oldRecorder.stop(); oldRecorder.release() end end)
  if oldPath and oldPath~="" then VoiceSystem.seq=VoiceSystem.seq+1; encodeAndSendLive(oldPath,VoiceSystem.seq,false) end
  if not VoiceSystem.streaming then return end
  local nextPath=localVoicePath(VoiceSystem.session,VoiceSystem.seq+1); local ok=startRecorder(nextPath)
  if not ok then stopVoiceRecording(); return end
  Handler().postDelayed(Runnable({run=rotateLiveChunk}),STREAM_CHUNK_MS)
end

function startVoiceRecording(target)
  if VoiceSystem.recording or VoiceSystem.streaming then return true end
  if not playerStats.sessionActive or not playerStats.accountEmail or playerStats.accountEmail == "" then say("سجل الدخول أولاً.", true); return false end
  if not Multiplayer.isMicEnabled then say("الميكروفون موقوف.", true); return false end
  if Build.VERSION.SDK_INT >= 23 and activity.checkSelfPermission("android.permission.RECORD_AUDIO") ~= PackageManager.PERMISSION_GRANTED then
    VoiceSystem.pendingPermissionCallback=function() startVoiceRecording(target) end
    say("نحتاج إلى إذن الميكروفون لتسجيل رسالتك الصوتية فقط.",true)
    activity.requestPermissions({"android.permission.RECORD_AUDIO"},9101)
    return false
  end
  VoiceSystem.channel=voiceChannel(); VoiceSystem.target=target or Multiplayer.directCallTarget or ""; VoiceSystem.session=uniqueId("session"); VoiceSystem.seq=0; VoiceSystem.recording=true; VoiceSystem.streaming=true; Multiplayer.isRecordingVoice=true; pcall(refreshVisibleScreen)
  local ok=startRecorder(localVoicePath(VoiceSystem.session,1))
  if not ok then VoiceSystem.recording=false; VoiceSystem.streaming=false; Multiplayer.isRecordingVoice=false; say("تعذر تشغيل الميكروفون.", true); return false end
  playDynamicSFX("mic_on",0,1,0); say("الميكروفون يعمل، صوتك يُرسل تلقائيًا للغرفة.", true); pcall(refreshVisibleScreen)
  Handler().postDelayed(Runnable({run=rotateLiveChunk}),STREAM_CHUNK_MS)
  Handler().postDelayed(Runnable({run=function() if VoiceSystem.streaming then stopVoiceRecording() end end}),STREAM_MAX_MS)
  return true
end

function stopVoiceRecording()
  if not VoiceSystem.recording and not VoiceSystem.streaming then return false end
  VoiceSystem.streaming=false; VoiceSystem.recording=false; Multiplayer.isRecordingVoice=false
  local recorder=VoiceSystem.recorder; local path=VoiceSystem.path; VoiceSystem.recorder=nil; VoiceSystem.path=""
  pcall(function() if recorder then recorder.stop(); recorder.release() end end)
  if path and path~="" then VoiceSystem.seq=VoiceSystem.seq+1; encodeAndSendLive(path,VoiceSystem.seq,true) end
  playDynamicSFX("mic_off",0,1,0); say("توقف الإرسال الصوتي.",true); pcall(refreshVisibleScreen)
  local channel,session=VoiceSystem.channel,VoiceSystem.session
  Handler().postDelayed(Runnable({run=function() if firebaseDelete and channel~="" and session~="" then firebaseDelete("/voiceLive/"..channel.."/"..session,function() end) end end}),15000)
  return true
end

function toggleVoiceDoubleTap()
  local now=System.currentTimeMillis()
  if not VoiceSystem.lastTap or now-VoiceSystem.lastTap>700 then VoiceSystem.lastTap=now; say(VoiceSystem.streaming and "الميكروفون، اضغط مرتين للإيقاف." or "الميكروفون، اضغط مرتين للتفعيل.",true); playDynamicSFX("a11y_press_light",0,1,0,0.24); return end
  VoiceSystem.lastTap=0
  if VoiceSystem.streaming then stopVoiceRecording() else startVoiceRecording() end
end

function onRequestPermissionsResult(requestCode,permissions,grantResults)
  if tonumber(requestCode)~=9101 then return end
  local callback=VoiceSystem.pendingPermissionCallback; VoiceSystem.pendingPermissionCallback=nil
  local granted=grantResults and grantResults[1] == PackageManager.PERMISSION_GRANTED
  if granted then if callback then callback() end else say("لم يتم منح إذن الميكروفون. يمكنك متابعة اللعبة دون التسجيل الصوتي.",true) end
end

local function writeDecodedVoice(encoded, suffix)
  local path=activity.getCacheDir().getAbsolutePath() .. "/arabiyat_received_" .. tostring(suffix or os.time()) .. ".aac"; local bytes=Base64.decode(encoded,Base64.DEFAULT); local stream=FileOutputStream(path); stream.write(bytes); stream.flush(); stream.close(); return path
end

local function playEncodedChunk(encoded,from,session,seq)
  if not encoded or encoded=="" then return end
  local okPath,path=pcall(function() return writeDecodedVoice(encoded,tostring(session or "voice").."_"..tostring(seq or 0)) end)
  if not okPath then return end
  local ok=pcall(function()
    local player=MediaPlayer(); player.setDataSource(path); player.prepare(); player.setOnCompletionListener(function() pcall(function() player.release(); deleteFile(path) end) end); player.start()
  end)
  if not ok then deleteFile(path) end
  if from and from~="" and from~=playerStats.username then say("صوت مباشر من "..from,true) end
end

local function playLatestFromArchive(body)
  if not body or body=="null" then return end
  local root=JSONObject(body); local keys=root.keys(); local latestKey=nil
  while keys and keys.hasNext() do local key=tostring(keys.next()); if not latestKey or key>latestKey then latestKey=key end end
  if not latestKey then return end
  local item=root.optJSONObject(latestKey); local from=item and item.optString("from","") or ""; local encoded=item and item.optString("data","") or ""
  if encoded~="" then playEncodedChunk(encoded,from,"archive",latestKey) end
end

function playLatestVoiceMessage()
  local channel=voiceChannel(); firebaseGet("/voiceMessages/" .. channel,function(ok,body) if not ok or not body then say("لا توجد رسائل صوتية متاحة.",true); return end; playLatestFromArchive(body) end)
end
function playArchivedVoiceMessage(channel,key,from)
  channel=tostring(channel or ""); key=tostring(key or "")
  if channel=="" or key=="" then say("بيانات الرسالة الصوتية غير مكتملة.",true); return end
  firebaseGet("/voiceMessages/"..channel.."/"..key,function(ok,body)
    if not ok or not body or body=="null" then say("الرسالة الصوتية غير متاحة.",true); return end
    local item=newJsonObject(body); local encoded=item and optString(item,"data","") or ""; local sender=item and optString(item,"name",optString(item,"from",from or "صديقك")) or (from or "صديقك")
    if encoded=="" then say("الرسالة الصوتية فارغة.",true); return end
    say("رسالة صوتية من "..sender,true); playEncodedChunk(encoded,sender,"archive",key)
  end)
end

local function pollLiveChunks()
  if not VoiceSystem.polling then return end
  local channel=voiceChannel(); if not channel or channel=="" then return end
  firebaseGet("/voiceLive/"..channel,function(ok,body)
    if ok and body and body~="null" then
      local root=JSONObject(body); local sessions=root.keys()
      while sessions and sessions.hasNext() do
        local session=tostring(sessions.next()); local sessionObj=root.optJSONObject(session)
        if sessionObj then
          local keys=sessionObj.keys(); local newest=VoiceSystem.liveSeen[session] or 0; local pending={}
          while keys and keys.hasNext() do
            local key=tostring(keys.next()); local n=tonumber(key) or 0; if n>newest then table.insert(pending,{key=key,seq=n}) end
          end
          table.sort(pending,function(a,b) return a.seq<b.seq end)
          for _,entry in ipairs(pending) do
            local item=sessionObj.optJSONObject(entry.key); local from=item and item.optString("from","") or ""; local encoded=item and item.optString("data","") or ""
            if from~="" and from~=playerStats.username and encoded~="" then playEncodedChunk(encoded,from,session,entry.seq) end
            newest=math.max(newest,entry.seq)
          end
          VoiceSystem.liveSeen[session]=newest
        end
      end
    end
  end)
end

function startVoicePolling()
  if VoiceSystem.polling then return end
  VoiceSystem.polling=true; VoiceSystem.lastMessageKey=VoiceSystem.lastMessageKey or ""; VoiceSystem.liveSeen=VoiceSystem.liveSeen or {}
  local function poll()
    if not VoiceSystem.polling then return end
    if currentSection~="WAITING_ROOM" and currentSection~="PLAYING_MULTIPLAYER" and currentSection~="ROOM_CHAT" then VoiceSystem.polling=false; return end
    pollLiveChunks()
    if currentSection=="ROOM_CHAT" then
      local channel=voiceChannel(); firebaseGet("/voiceMessages/"..channel,function(ok,body)
        if ok and body and body~="null" then
          local root=JSONObject(body); local keys=root.keys(); local latestKey=nil
          while keys and keys.hasNext() do local key=tostring(keys.next()); if not latestKey or key>latestKey then latestKey=key end end
          if latestKey and latestKey~=VoiceSystem.lastMessageKey then VoiceSystem.lastMessageKey=latestKey; local item=root.optJSONObject(latestKey); local from=item and item.optString("from","") or ""; local encoded=item and item.optString("data","") or ""; if from~=playerStats.username and encoded~="" then playEncodedChunk(encoded,from,"archive",latestKey) end end
        end
      end)
    end
    Handler().postDelayed(Runnable({run=poll}),900)
  end
  poll()
end
function stopVoicePolling() VoiceSystem.polling=false; VoiceSystem.liveSeen={} end
