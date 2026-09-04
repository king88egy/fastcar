package com.fastcar.racing;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

public final class Tts implements TextToSpeech.OnInitListener {
    private static TextToSpeech tts;
    private static volatile boolean ready;

    private Tts() {
    }

    public static void init(Context c) {
        if (tts == null) {
            tts = new TextToSpeech(c.getApplicationContext(), new Tts());
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int r = tts.setLanguage(Locale.getDefault());
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.UK);
            }
            ready = true;
        }
    }

    public static void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            ready = false;
        }
    }

    public static void say(final String text) {
        say(text, false);
    }

    public static void say(final String text, boolean interrupt) {
        String t = clean(text);
        if (t == null || t.trim().isEmpty() || tts == null) return;
        try {
            int queue = interrupt ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD;
            tts.speak(t, queue, null, "fc-" + System.currentTimeMillis());
        } catch (Throwable ignored) {
        }
    }

    static String clean(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            if (cp > 0x1F000 && cp < 0x1FFFF) {
                i += Character.charCount(cp);
                continue;
            }
            if (cp >= 0x2600 && cp <= 0x27BF) {
                i += Character.charCount(cp);
                continue;
            }
            if (cp == 0xFE0F || cp == 0x200D || cp == 0x20E3) {
                i += Character.charCount(cp);
                continue;
            }
            if (cp >= 0x2B00 && cp <= 0x2BFF) {
                i += Character.charCount(cp);
                continue;
            }
            sb.appendCodePoint(cp);
            i += Character.charCount(cp);
        }
        return sb.toString().trim();
    }

    public static void setRate(float rate) {
        if (tts != null) {
            try {
                tts.setSpeechRate(Math.max(0.05f, rate));
            } catch (Throwable ignored) {
            }
        }
    }

    public static void setPitch(float pitch) {
        if (tts != null) {
            try {
                tts.setPitch(Math.max(0.05f, pitch));
            } catch (Throwable ignored) {
            }
        }
    }

    public static void stop() {
        if (tts != null) {
            try {
                tts.stop();
            } catch (Throwable ignored) {
            }
        }
    }
}
