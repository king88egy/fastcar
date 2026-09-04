package com.fastcar.racing;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

public final class Sfx {

    private static SoundPool pool;
    private static final java.util.Map<String, Integer> ids = new java.util.HashMap<String, Integer>();
    private static boolean ok = false;

    private Sfx() {
    }

    public static synchronized void init(Context c) {
        if (ok) return;
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            pool = new SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attrs).build();
            load(c, "click");
            load(c, "coin");
            load(c, "success");
            load(c, "error");
            load(c, "event");
            load(c, "year");
            load(c, "gameover");
            load(c, "war");
            ok = true;
        } catch (Exception ignored) {
            ok = false;
        }
    }

    private static void load(Context c, String name) {
        try {
            ids.put(name, pool.load(c.getAssets().openFd("sounds/" + name + ".wav"), 1));
        } catch (Exception ignored) {
            ids.put(name, -1);
        }
    }

    public static void play(String name) {
        play(name, 0.8f);
    }

    public static synchronized void play(String name, float vol) {
        if (!ok || pool == null) return;
        Integer id = ids.get(name);
        if (id == null || id < 0) return;
        try {
            pool.play(id, vol, vol, 1, 0, 1f);
        } catch (Exception ignored) {
        }
    }
}