package com.fastcar.desktop;

import java.util.Random;

public final class SmartAnnouncer {
    private static final Random RND = new Random();

    private SmartAnnouncer() {
    }

    public static void say(String kind, String name, boolean force) {
        if (name == null || name.trim().isEmpty()) name = "المتسابق";
        String[] set = pick(kind);
        if (set == null) return;
        String line = set[RND.nextInt(set.length)];
        Tts.say(line.replace("{name}", name));
    }

    public static String describe(String kind, String name) {
        if (name == null || name.trim().isEmpty()) name = "المتسابق";
        String[] set = pick(kind);
        if (set == null) return "";
        return set[RND.nextInt(set.length)].replace("{name}", name);
    }

    private static String[] pick(String kind) {
        if ("race_start".equals(kind)) {
            return new String[]{"جاهزين يا {name}، السباق بدأ.", "انطلق يا {name}، الطريق قدامك."};
        }
        if ("lead".equals(kind)) {
            return new String[]{"الله عليك يا {name}، أنت في المقدمة.", "ممتاز يا {name}، حافظ على المركز الأول."};
        }
        if ("push".equals(kind)) {
            return new String[]{"زود يا {name}، الفرصة في إيدك.", "اضغط يا {name}، النيترو جاهز."};
        }
        if ("behind".equals(kind)) {
            return new String[]{"شد حيلك يا {name}، المنافس متقدم.", "ركز يا {name}، الفارق ممكن يتعوض."};
        }
        if ("overtake".equals(kind)) {
            return new String[]{"يا سلام يا {name}، تجاوز رائع.", "تجاوز نظيف يا {name}، حافظ على خطك."};
        }
        if ("near_finish".equals(kind)) {
            return new String[]{"اقتربت من النهاية يا {name}، لا تضيع الصدارة.", "خط النهاية يناديك يا {name}."};
        }
        if ("result".equals(kind)) {
            return new String[]{"الله عليك يا {name}، فوز مستحق.", "انتهى السباق يا {name}، استعد للجولة القادمة."};
        }
        if ("account_ready".equals(kind)) {
            return new String[]{"حسابك جاهز يا {name}، أهلاً بعودتك.", "بياناتك آمنة يا {name}، نكمل السباق."};
        }
        if ("menu".equals(kind)) {
            return new String[]{"أهلاً يا {name}، القائمة جاهزة.", "كل الخيارات أمامك يا {name}."};
        }
        if ("store".equals(kind)) {
            return new String[]{"يا {name}، اختار معداتك بعناية.", "جهز عربيتك قبل السباق يا {name}."};
        }
        if ("achievement".equals(kind)) {
            return new String[]{"أحسنت يا {name}، تقدمك واضح.", "مستواك يتحسن يا {name}، كمل."};
        }
        return null;
    }
}