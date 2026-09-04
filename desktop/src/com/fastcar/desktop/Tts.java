package com.fastcar.desktop;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Tts {
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor();
    private static volatile boolean enabled = true;
    private static volatile double rate = 1.0;
    private static volatile double pitch = 1.0;

    private Tts() {
    }

    public static void setEnabled(boolean on) {
        enabled = on;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setRate(double r) {
        rate = Math.max(0.05, r);
    }

    public static void setPitch(double p) {
        pitch = Math.max(0.05, p);
    }

    public static void say(final String text) {
        if (text == null || text.trim().isEmpty() || !enabled) return;
        final String t = clean(text).replace("'", " ").replace("\n", " ");
        final double r = rate;
        final double pc = pitch;
        POOL.submit(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "powershell", "-NoProfile", "-Command",
                        "Add-Type -AssemblyName System.Speech; $s=New-Object System.Speech.Synthesis.SpeechSynthesizer; $s.Rate=" + ((int) ((r - 1.0) * 10 + 0.5)) + "; $s.Speak('" + t + "')");
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                proc.getOutputStream().close();
                proc.waitFor();
            } catch (Exception ignored) {
            }
        });
    }

    static String clean(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            int len = Character.charCount(cp);
            boolean drop = (cp >= 0x1F000 && cp <= 0x1FAFF)
                    || (cp >= 0x2600 && cp <= 0x27BF)
                    || (cp >= 0x2B00 && cp <= 0x2BFF)
                    || (cp == 0xFE0F) || (cp == 0x200D) || (cp == 0x20E3)
                    || (cp == 0x203C) || (cp == 0x2049);
            if (!drop) sb.appendCodePoint(cp);
            i += len;
        }
        return sb.toString();
    }
}
