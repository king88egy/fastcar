-- AudioSystem.lua
-- أصول سباق محدثة، مع بدائل محلية إذا تعذر تحميل أحد الملفات.
local assetsDir = activity.getLuaDir() .. "/res/"
MusicPlaylist={assetsDir.."race_music_synthwave.wav",assetsDir.."race_music_rock.wav",assetsDir.."race_music_edm.wav",assetsDir.."race_music_orchestral.wav",assetsDir.."race_music_cc0.ogg"}
MusicPlaylistIndex=1
bgmPlayer=nil
function loadMusicTrack(index,autoPlay)
  if #MusicPlaylist==0 then return false end
  MusicPlaylistIndex=math.max(1,math.min(#MusicPlaylist,index or 1)); local path=MusicPlaylist[MusicPlaylistIndex]; local old=bgmPlayer; bgmPlayer=nil; pcall(function() if old then old.stop(); old.release() end end)
  local ok=pcall(function()
    local player=MediaPlayer(); player.setDataSource(path); player.prepare(); player.setLooping(false); player.setVolume(Settings.bgmVolume,Settings.bgmVolume); player.setOnCompletionListener(function() if Settings.bgmEnabled then nextMusicTrack() end end); bgmPlayer=player; if autoPlay and Settings.bgmEnabled then player.start() end
  end)
  if not ok then bgmPlayer=nil; return false end
  if currentSection=="PLAYING" or currentSection=="PLAYING_MULTIPLAYER" then setRaceMusicLevel(0.03) end
  return true
end
function nextMusicTrack() return loadMusicTrack((MusicPlaylistIndex or 1)+1,true) end
function previousMusicTrack() return loadMusicTrack((MusicPlaylistIndex or 1)-1,true) end
function toggleMusicPlayback()
  if not bgmPlayer then return false end
  local ok,playing=pcall(function() return bgmPlayer.isPlaying() end); if not ok then return false end
  if playing then pcall(function() bgmPlayer.pause() end) else pcall(function() bgmPlayer.start() end) end
  return not playing
end
function stopMusicPlayback() if bgmPlayer then pcall(function() bgmPlayer.pause() end) end end

remoteRoomMusicPlayer=nil; remoteRoomMusicUrl=""
local function isSafeRemoteAudioUrl(url) return type(url)=="string" and url:match("^https://[%w%._%-%/%%%?%=&]+$")~=nil end
function stopRemoteRoomMusic()
  local old=remoteRoomMusicPlayer; remoteRoomMusicPlayer=nil; remoteRoomMusicUrl=""; if old then pcall(function() old.stop(); old.release() end) end
end
function playRemoteRoomMusic(url)
  if not isSafeRemoteAudioUrl(url) then stopRemoteRoomMusic(); return false end
  if remoteRoomMusicUrl==url and remoteRoomMusicPlayer then return true end
  stopRemoteRoomMusic(); local ok=pcall(function()
    local player=MediaPlayer(); player.setDataSource(url); player.setAudioStreamType(AudioManager.STREAM_MUSIC); player.setLooping(true); player.setVolume(0.03,0.03); player.setOnPreparedListener(function() if remoteRoomMusicPlayer==player and Settings.bgmEnabled then pcall(function() player.start() end) end end); player.prepareAsync(); remoteRoomMusicPlayer=player; remoteRoomMusicUrl=url
  end)
  if not ok then stopRemoteRoomMusic(); return false end
  return true
end
function setRemoteRoomMusicLevel(level)
  local v=math.max(0,math.min(0.05,tonumber(level) or 0.05)); if remoteRoomMusicPlayer then pcall(function() remoteRoomMusicPlayer.setVolume(v,v) end) end
end
loadMusicTrack(1,false)

AudioSystem={pool=SoundPool(32,AudioManager.STREAM_MUSIC,0),sounds={},engineStreamId=0,hornStreamId=0,trafficStreamId=0,environmentStreamId=0,currentGear=1,engineSoundKey="engine_compact",paths={
  engine=assetsDir.."car_engine_low_cc0.wav", engine_low=assetsDir.."car_engine_low_cc0.wav", engine_high=assetsDir.."car_engine_high_cc0.wav",
  engine_compact=assetsDir.."engine_compact.ogg", engine_sedan=assetsDir.."engine_sedan.ogg", engine_muscle=assetsDir.."engine_muscle.ogg", engine_touring=assetsDir.."engine_touring.ogg",
  engine_sport=assetsDir.."engine_sport.ogg", engine_super=assetsDir.."engine_super.ogg", engine_classic=assetsDir.."engine_classic.ogg",   engine_hyper=assetsDir.."engine_hyper.ogg", engine_supercar_hq=assetsDir.."engine_supercar_hq.wav",
  engine_variant_01=assetsDir.."engine_variant_01.ogg", engine_variant_02=assetsDir.."engine_variant_02.ogg", engine_variant_03=assetsDir.."engine_variant_03.ogg", engine_variant_04=assetsDir.."engine_variant_04.ogg", engine_variant_05=assetsDir.."engine_variant_05.ogg", engine_variant_06=assetsDir.."engine_variant_06.ogg", engine_variant_07=assetsDir.."engine_variant_07.ogg", engine_variant_08=assetsDir.."engine_variant_08.ogg", engine_variant_09=assetsDir.."engine_variant_09.ogg", engine_variant_10=assetsDir.."engine_variant_10.ogg", engine_variant_11=assetsDir.."engine_variant_11.ogg", engine_variant_12=assetsDir.."engine_variant_12.ogg", engine_variant_13=assetsDir.."engine_variant_13.ogg", engine_variant_14=assetsDir.."engine_variant_14.ogg", engine_variant_15=assetsDir.."engine_variant_15.ogg", engine_variant_16=assetsDir.."engine_variant_16.ogg", engine_variant_17=assetsDir.."engine_variant_17.ogg", engine_variant_18=assetsDir.."engine_variant_18.ogg", engine_variant_19=assetsDir.."engine_variant_19.ogg", engine_variant_20=assetsDir.."engine_variant_20.ogg", engine_variant_21=assetsDir.."engine_variant_21.ogg", engine_variant_22=assetsDir.."engine_variant_22.ogg", engine_variant_23=assetsDir.."engine_variant_23.ogg", engine_variant_24=assetsDir.."engine_variant_24.ogg", engine_variant_25=assetsDir.."engine_variant_25.ogg", engine_variant_26=assetsDir.."engine_variant_26.ogg", engine_variant_27=assetsDir.."engine_variant_27.ogg", engine_variant_28=assetsDir.."engine_variant_28.ogg", engine_variant_29=assetsDir.."engine_variant_29.ogg", engine_variant_30=assetsDir.."engine_variant_30.ogg", engine_variant_31=assetsDir.."engine_variant_31.ogg", engine_variant_32=assetsDir.."engine_variant_32.ogg", engine_variant_33=assetsDir.."engine_variant_33.ogg", engine_variant_34=assetsDir.."engine_variant_34.ogg", engine_variant_35=assetsDir.."engine_variant_35.ogg", engine_variant_36=assetsDir.."engine_variant_36.ogg", engine_variant_37=assetsDir.."engine_variant_37.ogg", engine_variant_38=assetsDir.."engine_variant_38.ogg", engine_variant_39=assetsDir.."engine_variant_39.ogg", engine_variant_40=assetsDir.."engine_variant_40.ogg", engine_variant_41=assetsDir.."engine_variant_41.ogg", engine_variant_42=assetsDir.."engine_variant_42.ogg", engine_variant_43=assetsDir.."engine_variant_43.ogg", engine_variant_44=assetsDir.."engine_variant_44.ogg", engine_variant_45=assetsDir.."engine_variant_45.ogg", engine_variant_46=assetsDir.."engine_variant_46.ogg", engine_variant_47=assetsDir.."engine_variant_47.ogg", engine_variant_48=assetsDir.."engine_variant_48.ogg", engine_variant_49=assetsDir.."engine_variant_49.ogg", engine_variant_50=assetsDir.."engine_variant_50.ogg", engine_variant_51=assetsDir.."engine_variant_51.ogg", engine_variant_52=assetsDir.."engine_variant_52.ogg", engine_variant_53=assetsDir.."engine_variant_53.ogg", engine_variant_54=assetsDir.."engine_variant_54.ogg",
  start=assetsDir.."car_start_cc0.wav", car_start_external=assetsDir.."car_start_external_cc0.ogg", car_stop_external=assetsDir.."car_stop_external_cc0.ogg", race_start_grid=assetsDir.."race_start_grid_cc0.ogg", race_intro=assetsDir.."race_intro_cc0.ogg", finish=assetsDir.."race_finish_best_cc0.ogg", crash=assetsDir.."Crash2.ogg", damage=assetsDir.."Crash2.ogg", win=assetsDir.."race_finish_best_cc0.ogg",
  drift=assetsDir.."driftlow.ogg", radar=assetsDir.."blinker.ogg", menu=assetsDir.."blinker.ogg", focus=assetsDir.."blinker.ogg", first_focus=assetsDir.."race_start_grid_cc0.ogg", last_focus=assetsDir.."race_finish_good_cc0.ogg", first_select=assetsDir.."start2.mp3", last_select=assetsDir.."clap.ogg",
  drawer_open=assetsDir.."click.mp3", drawer_close=assetsDir.."click.mp3", open=assetsDir.."click.mp3", swipe=assetsDir.."a11y_press_light.ogg", horn=assetsDir.."race_horn_long_8s.ogg", horn_external=assetsDir.."race_horn_long_8s.ogg", horn_external_cc0=assetsDir.."race_horn_external_cc0.ogg", horn_recent_02=assetsDir.."race_horn_recent_02_cc0.ogg", horn_pneumatic=assetsDir.."race_horn_pneumatic_double_cc0.ogg", horn_truck=assetsDir.."race_horn_truck_cc0.ogg", race_environment_road=assetsDir.."race_environment_road_cc0.ogg", steer_swipe=assetsDir.."race_brake_realistic_a_cc0.ogg", brake_realistic_a=assetsDir.."race_brake_realistic_a_cc0.ogg", brake_realistic_b=assetsDir.."race_brake_realistic_b_cc0.ogg", fuel_pump=assetsDir.."fuel_fill.ogg", submit=assetsDir.."clap.ogg", purchase=assetsDir.."purchase_success_cc0.ogg", repair=assetsDir.."clap.ogg", wheel=assetsDir.."wheel_spin_cc0.ogg", wheel_spin=assetsDir.."wheel_spin_cc0.ogg", wheel_reward=assetsDir.."wheel_reward_cc0.ogg", purchase_success=assetsDir.."purchase_success_cc0.ogg", start_alt=assetsDir.."start2.mp3", start_deep=assetsDir.."car_start_cc0.wav", start_sport=assetsDir.."car_start_cc0.wav", start_classic=assetsDir.."car_start_cc0.wav", start_hyper=assetsDir.."car_start_cc0.wav",
  voice_beep=assetsDir.."blinker.ogg", mic_on=assetsDir.."a11y_press_confirm.ogg", mic_off=assetsDir.."a11y_release.ogg", passing=assetsDir.."blinker.ogg", nearby=assetsDir.."car_engine_high_cc0.wav", side_signal=assetsDir.."blinker.ogg", close_call=assetsDir.."blinker.ogg", warning=assetsDir.."blinker.ogg", nitro=assetsDir.."bulletwhiz1.ogg",
  countdown_3=assetsDir.."countdown_3.ogg", countdown_2=assetsDir.."countdown_2.ogg", countdown_1=assetsDir.."countdown_1.ogg", countdown_go=assetsDir.."countdown_go.ogg",
  announcer_lead=assetsDir.."announcer_lead.ogg", announcer_push=assetsDir.."announcer_push.ogg", announcer_caution=assetsDir.."announcer_caution.ogg", announcer_win=assetsDir.."announcer_win.ogg", announcer_loss=assetsDir.."announcer_loss.ogg", announcer_30sec=assetsDir.."announcer_30sec.ogg", announcer_behind=assetsDir.."announcer_behind.ogg",   announcer_female_intro_reverb=assetsDir.."announcer_female_intro_reverb.ogg", announcer_male_b_extra=assetsDir.."announcer_male_b_extra.wav", announcer_female_b_extra=assetsDir.."announcer_female_b_extra.wav", race_chat_female_a=assetsDir.."race_chat_female_a.ogg", race_chat_female_b=assetsDir.."race_chat_female_b.ogg", race_chat_male_a=assetsDir.."race_chat_male_a.ogg", race_chat_male_b=assetsDir.."race_chat_male_b.ogg", race_boost=assetsDir.."race_boost.ogg", race_checkpoint=assetsDir.."race_checkpoint.ogg", race_near_miss=assetsDir.."race_near_miss.ogg", race_obstacle_warn=assetsDir.."race_obstacle_warn.ogg", race_crate_appear=assetsDir.."race_crate_appear.ogg", race_crate_collect=assetsDir.."race_crate_collect.ogg", race_crate_expire=assetsDir.."race_crate_expire.ogg", race_collision_light=assetsDir.."race_collision_light.ogg", race_collision_heavy=assetsDir.."race_collision_heavy.ogg", race_overtake=assetsDir.."race_overtake.ogg", race_finish_line=assetsDir.."race_finish_line.ogg", room_lock=assetsDir.."room_lock.ogg", room_unlock=assetsDir.."room_unlock.ogg", room_max_change=assetsDir.."room_max_change.ogg", moderator_assign=assetsDir.."moderator_assign.ogg", moderation_mute=assetsDir.."moderation_mute.ogg", moderation_unmute=assetsDir.."moderation_unmute.ogg", moderation_kick=assetsDir.."moderation_kick.ogg", moderation_ban=assetsDir.."moderation_ban.ogg", moderation_unban=assetsDir.."moderation_unban.ogg", chat_send_soft=assetsDir.."chat_send_soft.ogg", chat_receive_soft=assetsDir.."chat_receive_soft.ogg", focus_next_loud=assetsDir.."focus_next_loud.ogg", focus_prev_loud=assetsDir.."focus_prev_loud.ogg", race_engine_idle=assetsDir.."race_engine_idle.ogg", race_engine_rev=assetsDir.."race_engine_rev.ogg", race_engine_release=assetsDir.."race_engine_release.ogg", race_gear_up=assetsDir.."race_gear_up.ogg", race_gear_down=assetsDir.."race_gear_down.ogg", race_brake_squeal=assetsDir.."race_brake_realistic_a_cc0.ogg", race_tire_scrub=assetsDir.."race_tire_scrub.ogg", race_road_gravel=assetsDir.."race_road_gravel.ogg", race_wet_road=assetsDir.."race_wet_road.ogg", race_turn=assetsDir.."race_turn_left.ogg", race_turn_left=assetsDir.."race_turn_left.ogg", race_turn_right=assetsDir.."race_turn_right.ogg", race_signal_pass=assetsDir.."race_signal_pass.ogg", race_horn_short=assetsDir.."race_horn_short.ogg", race_horn_long=assetsDir.."race_horn_long.ogg", race_obstacle_far=assetsDir.."race_obstacle_far.ogg", race_obstacle_close=assetsDir.."race_obstacle_close.ogg", race_miss_success=assetsDir.."race_miss_success.ogg", race_miss_fail=assetsDir.."race_miss_fail.ogg", race_body_hit=assetsDir.."race_body_hit.ogg", race_tire_hit=assetsDir.."race_tire_hit.ogg", race_opponent_pass_left=assetsDir.."race_car_pass_realistic_a_cc0.ogg", race_opponent_pass_right=assetsDir.."race_car_pass_realistic_b_cc0.ogg", race_car_pass_realistic_a=assetsDir.."race_car_pass_realistic_a_cc0.ogg", race_car_pass_realistic_b=assetsDir.."race_car_pass_realistic_b_cc0.ogg", race_opponent_far_left=assetsDir.."race_opponent_far_left.ogg", race_opponent_far_right=assetsDir.."race_opponent_far_right.ogg", race_opponent_crash=assetsDir.."race_opponent_crash.ogg", race_finish_chime=assetsDir.."race_finish_chime.ogg", race_countdown_beep=assetsDir.."race_countdown_beep.ogg", race_menu_tick=assetsDir.."race_menu_tick.ogg", race_focus_soft=assetsDir.."race_focus_soft.ogg", race_touch_soft=assetsDir.."race_touch_soft.ogg",
  a11y_focus=assetsDir.."a11y_focus.ogg", a11y_swipe_next=assetsDir.."a11y_swipe_next.ogg", a11y_swipe_prev=assetsDir.."a11y_swipe_prev.ogg", a11y_double_tap=assetsDir.."a11y_double_tap.ogg", a11y_activate=assetsDir.."a11y_activate.ogg", a11y_first=assetsDir.."a11y_first.ogg", a11y_last=assetsDir.."a11y_last.ogg",
  a11y_focus_alt=assetsDir.."a11y_focus_alt.ogg", a11y_error=assetsDir.."a11y_error.ogg", a11y_question=assetsDir.."a11y_question.ogg", a11y_nav_next_light=assetsDir.."a11y_nav_next_light.ogg", a11y_nav_prev_light=assetsDir.."a11y_nav_prev_light.ogg", a11y_boundary_first_light=assetsDir.."a11y_boundary_first_light.ogg", a11y_boundary_last_light=assetsDir.."a11y_boundary_last_light.ogg", a11y_press_light=assetsDir.."a11y_press_light.ogg", a11y_press_confirm=assetsDir.."a11y_press_confirm.ogg", a11y_hold=assetsDir.."a11y_hold.ogg", a11y_release=assetsDir.."a11y_release.ogg", dialog_open_soft=assetsDir.."dialog_open_soft.ogg", dialog_close_soft=assetsDir.."dialog_close_soft.ogg", account_recover=assetsDir.."account_recover.ogg", account_create=assetsDir.."account_create.ogg", profile_ready=assetsDir.."profile_ready.ogg", social_online=assetsDir.."social_online.ogg", social_request=assetsDir.."social_request.ogg", chat_send=assetsDir.."chat_send.ogg", chat_receive=assetsDir.."chat_receive.ogg", chat_open=assetsDir.."chat_open.ogg", task_open=assetsDir.."task_open.ogg", task_progress=assetsDir.."task_progress.ogg", terms_open=assetsDir.."terms_open.ogg", terms_accept=assetsDir.."terms_accept.ogg", input_error=assetsDir.."input_error.ogg", connection_ok=assetsDir.."connection_ok.ogg", menu_open=assetsDir.."menu_open.ogg", menu_close=assetsDir.."menu_close.ogg", menu_select=assetsDir.."menu_select.ogg", menu_back=assetsDir.."menu_back.ogg", menu_tick=assetsDir.."menu_tick.ogg", menu_toggle=assetsDir.."menu_toggle.ogg", store_open=assetsDir.."store_open.ogg", purchase_confirm=assetsDir.."purchase_confirm.ogg", purchase_error=assetsDir.."purchase_error.ogg", coin_reward=assetsDir.."coin_reward.ogg", achievement_unlock=assetsDir.."coin_reward.ogg", reward_reveal=assetsDir.."reward_reveal.ogg", repair_start=assetsDir.."repair_start.ogg", repair_done=assetsDir.."repair_done.ogg", fuel_fill=assetsDir.."fuel_fill.ogg", fuel_low=assetsDir.."fuel_low.ogg", garage_open=assetsDir.."garage_open.ogg", garage_close=assetsDir.."garage_close.ogg", leaderboard_open=assetsDir.."leaderboard_open.ogg", leaderboard_rank=assetsDir.."leaderboard_rank.ogg", leaderboard_refresh=assetsDir.."leaderboard_refresh.ogg", friend_request=assetsDir.."friend_request.ogg", friend_accept=assetsDir.."friend_accept.ogg", friend_message=assetsDir.."friend_message.ogg", invite_sent=assetsDir.."invite_sent.ogg", invite_received=assetsDir.."invite_received.ogg", room_join=assetsDir.."room_join.ogg", room_leave=assetsDir.."room_leave.ogg", room_ready=assetsDir.."room_ready.ogg", race_grid=assetsDir.."race_grid.ogg", race_countdown_tick=assetsDir.."race_countdown_tick.ogg", race_overtake=assetsDir.."race_overtake.ogg", race_caution=assetsDir.."race_caution.ogg", race_finish=assetsDir.."race_finish.ogg", result_win=assetsDir.."result_win.ogg", result_loss=assetsDir.."result_loss.ogg", car_select=assetsDir.."car_select.ogg", settings_change=assetsDir.."settings_change.ogg", network_error=assetsDir.."network_error.ogg", brake=assetsDir.."brake.ogg", gear_shift=assetsDir.."gear_shift.ogg", room_message=assetsDir.."message_receive_cc0.ogg", private_message=assetsDir.."message_receive_cc0.ogg", room_player_join=assetsDir.."room_player_join_cc0.ogg", room_player_leave=assetsDir.."room_player_leave_cc0.ogg", room_kick=assetsDir.."room_kick_cc0.ogg", room_ban=assetsDir.."room_ban_cc0.ogg", invite_received=assetsDir.."invite_received_cc0.ogg", invite_sent=assetsDir.."message_send_cc0.ogg", message_send=assetsDir.."message_send_cc0.ogg", message_receive=assetsDir.."message_receive_cc0.ogg", robot_add=assetsDir.."robot_add_cc0.ogg", exit_confirm=assetsDir.."exit_confirm_cc0.ogg", wheel_spin_long=assetsDir.."wheel_spin_long_cc0.ogg"
}}
for key,path in pairs(AudioSystem.paths) do local ok,id=pcall(function() return AudioSystem.pool.load(path,1) end); if ok then AudioSystem.sounds[key]=id end end

function playDynamicSFX(soundKey,loop,rate,pan,customVol)
  if not Settings.sfxEnabled then return 0 end
  local id=AudioSystem.sounds[soundKey]; if not id then return 0 end
  local sfxVolume=Settings.sfxVolume or 1.0
  local volume=(customVol or 0.65)*sfxVolume
  if soundKey=="horn" then volume=math.min(customVol or 0.78,0.78)*sfxVolume elseif soundKey=="start" or (soundKey and string.match(soundKey,"^engine")) then volume=math.min(customVol or 0.30,0.36)*sfxVolume end
  if soundKey and string.match(soundKey,"^a11y_") then volume=math.max(volume,0.92*sfxVolume) end
  local left,right=volume,volume
  if pan and pan<0 then right=math.max(0,volume+pan*volume) elseif pan and pan>0 then left=math.max(0,volume-pan*volume) end
  return AudioSystem.pool.play(id,left,right,1,loop or 0,math.max(0.5,math.min(2.0,rate or 1.0)))
end

function playAnnouncer(soundKey,rate,volume)
  if not Settings.announcerEnabled or not Settings.sfxEnabled then return 0 end
  local cap=math.max(0,math.min(1,tonumber(Settings.announcerVolume) or 0.20)); return playDynamicSFX(soundKey,0,rate or 1.0,0,math.min(tonumber(volume) or cap,cap))
end
function playRaceVoiceClip(soundKey,volume)
  if not Settings.announcerEnabled or Settings.smartAnnouncerEnabled==false or not Settings.sfxEnabled then return 0 end
  local cap=math.max(0,math.min(1,tonumber(Settings.announcerVolume) or 0.20)); return playDynamicSFX(soundKey,0,1.0,0,math.min(tonumber(volume) or cap,cap))
end

local function hornRateForCar()
  local id=math.max(1,math.min(54,tonumber(activeCarId or 1) or 1)); return 0.78+((id-1)*0.0095)
end
function startHornSound()
  stopHornSound()
  AudioSystem.hornStreamId=playDynamicSFX("horn",-1,hornRateForCar(),0,0.90)
  return AudioSystem.hornStreamId~=0
end
function stopHornSound()
  if AudioSystem.hornStreamId and AudioSystem.hornStreamId~=0 then pcall(function() AudioSystem.pool.stop(AudioSystem.hornStreamId) end); AudioSystem.hornStreamId=0 end
end
local function engineStartProfile()
  local id=math.max(1,math.min(54,tonumber(activeCarId or 1) or 1)); local profiles={{key="start_deep",rate=0.78},{key="start_alt",rate=0.94},{key="start_sport",rate=1.08},{key="start_classic",rate=0.86},{key="start_hyper",rate=1.18}}; return profiles[((id-1)%#profiles)+1] end
function startEngineCrankSound()
  local external=playDynamicSFX("car_start_external",0,1.0,0,0.90); if external~=0 then return external end
  local profile=engineStartProfile(); return playDynamicSFX(profile.key,0,profile.rate,0,0.90)
end
function startRaceEnvironmentSound()
  if AudioSystem.environmentStreamId and AudioSystem.environmentStreamId~=0 then return AudioSystem.environmentStreamId end
  AudioSystem.environmentStreamId=playDynamicSFX("race_environment_road",-1,1.0,0,0.12); return AudioSystem.environmentStreamId
end
function stopRaceEnvironmentSound()
  if AudioSystem.environmentStreamId and AudioSystem.environmentStreamId~=0 then pcall(function() AudioSystem.pool.stop(AudioSystem.environmentStreamId) end); AudioSystem.environmentStreamId=0 end
end
function startEngineSound()
  startRaceEnvironmentSound(); stopEngineSound()
  local key=AudioSystem.engineSoundKey or "engine_compact"
  AudioSystem.engineStreamId=playDynamicSFX(key,-1,0.72,0,0.30)
  if AudioSystem.engineStreamId~=0 then playDynamicSFX("race_engine_idle",0,0.74,0,0.30) end
  if AudioSystem.engineStreamId==0 then AudioSystem.engineStreamId=playDynamicSFX("engine",-1,0.72,0,0.30) end
end

function updateEngineFeedback(speed,maxSpeed,gasPressed,pressure)
  if not AudioSystem.engineStreamId or AudioSystem.engineStreamId==0 then return end
  local ratio=math.max(0,math.min(1,(speed or 0)/math.max(1,maxSpeed or 1))); local throttle=math.max(0,math.min(1,tonumber(pressure) or (gasPressed and 1 or 0)))
  local rate=0.68+(ratio*0.90)+(throttle*0.42); local volume=(0.12+(ratio*0.16)+(throttle*0.18))*(Settings.sfxVolume or 1.0); if not gasPressed then volume=volume*0.68 end
  pcall(function() AudioSystem.pool.setRate(AudioSystem.engineStreamId,math.max(0.5,math.min(2.0,rate))); AudioSystem.pool.setVolume(AudioSystem.engineStreamId,math.min(0.36,volume),math.min(0.36,volume)) end)
end

function setRaceMusicLevel(level)
  if not bgmPlayer then return end
  local requested=level or Settings.bgmVolume or 0.03;
  local v=math.max(0,math.min(1,requested)); if currentSection=="PLAYING" or currentSection=="PLAYING_MULTIPLAYER" then v=math.min(v,0.03) end
  pcall(function() bgmPlayer.setVolume(v,v) end)
end
function restoreMenuMusic() if Settings.bgmEnabled then setRaceMusicLevel(Settings.bgmVolume or 0.03) else setRaceMusicLevel(0) end end
function stopEngineSound() if AudioSystem.engineStreamId and AudioSystem.engineStreamId~=0 then pcall(function() AudioSystem.pool.stop(AudioSystem.engineStreamId) end); AudioSystem.engineStreamId=0 end; if currentSection~="PLAYING" and currentSection~="PLAYING_MULTIPLAYER" then stopRaceEnvironmentSound() end end
function stopSoundsAndExit() stopHornSound(); saveGameData(); pcall(function() if bgmPlayer.isPlaying() then bgmPlayer.stop() end; bgmPlayer.release() end); pcall(function() AudioSystem.pool.release() end); if tts then pcall(function() tts.shutdown() end) end; activity.finish() end
function onDestroy() if stopTiltControl then stopTiltControl() end; stopSoundsAndExit() end
function onPause() if stopTiltControl then stopTiltControl() end; stopHornSound(); saveGameData(); pcall(function() if bgmPlayer.isPlaying() then bgmPlayer.pause() end end) end
function onResume() if Settings.bgmEnabled then pcall(function() bgmPlayer.start() end); if currentSection=="PLAYING" or currentSection=="PLAYING_MULTIPLAYER" then setRaceMusicLevel(0.03) else restoreMenuMusic() end end; if (currentSection=="PLAYING" or currentSection=="PLAYING_MULTIPLAYER") and (Settings.steeringMode=="الإمالة" or Settings.steeringMode=="مختلط") and startTiltControl then startTiltControl() end end
