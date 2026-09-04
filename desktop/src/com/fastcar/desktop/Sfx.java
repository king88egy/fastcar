package com.fastcar.desktop;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class Sfx {

    private static final Map<String, File> files = new HashMap<String, File>();
    private static boolean enabled = true;

    private Sfx() {
    }

    private static void ensure(Base base) {
        if (!files.isEmpty()) return;
        String[] names = {"click", "coin", "success", "error", "event", "year", "gameover", "war"};
        String[] roots = {
                "sounds",
                "assets/sounds",
                "../sounds",
                "../assets/sounds",
                "Fast car/sounds",
                "Fast car/assets/sounds"
        };
        String baseDir = (base == null || base.getDir() == null) ? "." : base.getDir();
        for (String r : roots) {
            File cand = new File(baseDir, r);
            if (!cand.isDirectory()) continue;
            for (String n : names) {
                File f = new File(cand, n + ".wav");
                if (!files.containsKey(n) && f.isFile()) files.put(n, f);
            }
            if (files.size() >= names.length) break;
        }
    }

    public interface Base {
        String getDir();
    }

    public static synchronized void init(Base base) {
        try {
            ensure(base);
        } catch (Exception ignored) {
        }
    }

    public static void play(final String name) {
        play(name, false);
    }

    public static synchronized void play(final String name, final boolean blocking) {
        if (!enabled) return;
        final File f = files.get(name);
        if (f == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AudioInputStream in = AudioSystem.getAudioInputStream(f);
                    Clip clip = AudioSystem.getClip();
                    clip.open(in);
                    clip.start();
                    if (blocking) {
                        long ms = clip.getMicrosecondLength() / 1000 + 20;
                        try {
                            Thread.sleep(ms);
                        } catch (InterruptedException ignored) {
                        }
                        clip.close();
                    } else {
                        clip.addLineListener(new javax.sound.sampled.LineListener() {
                            @Override
                            public void update(javax.sound.sampled.LineEvent ev) {
                                if (ev.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                                    clip.close();
                                }
                            }
                        });
                    }
                } catch (Exception ignored) {
                }
            }
        }).start();
    }
}