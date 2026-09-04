package com.fastcar.racing;

final class Docs {
    private Docs() {
    }

    static String privacy(String lang) {
        if (lang != null && lang.startsWith("en")) {
            return "Fast Car — Privacy Policy\n\n"
                    + "1. Data we collect: username, race scores, coins and garage data.\n"
                    + "2. Data is stored on the game server you connect to.\n"
                    + "3. Ads: the app may show ads served by Google AdMob. AdMob can collect device "
                    + "identifiers and usage data according to Google's Privacy Policy "
                    + "(https://policies.google.com/privacy).\n"
                    + "4. We do not sell your personal data.\n"
                    + "5. You are responsible for keeping your account password private.\n"
                    + "6. To delete your account and data, contact the developer.\n"
                    + "7. This policy may be updated from time to time; continued use means acceptance.\n";
        }
        return "السيارة السريعة — سياسة الخصوصية\n\n"
                + "١) مقدمة:\nنحترم خصوصيتك ونلتزم بحماية بياناتك الشخصية. نوضح هنا ما البيانات "
                + "التي نجمعها، وكيف نستخدمها، وما حقوقك.\n\n"
                + "٢) البيانات التي نجمعها:\nاسم المستخدم وكلمة المرور (مشفرة)، نتائج السباقات، "
                + "العملات وممتلكات الكراج، تقدمك في المهام والإنجازات، وسجل عجلة الحظ.\n\n"
                + "٣) كيف نستخدم البيانات:\nلتشغيل اللعبة، حفظ تقدمك، تحسين التجربة، منع الغش، "
                + "وتنفيذ عمليات الشراء التي تقوم بها.\n\n"
                + "٤) مكان التخزين والحماية:\nتُخزن بياناتك على خادم اللعبة الذي تتصل به وهي "
                + "محمية بكلمات مرور مشفرة وحدود وصول صارمة.\n\n"
                + "٥) الإعلانات:\nقد تعرض اللعبة إعلانات من Google AdMob وفق سياسة خصوصية Google: "
                + "https://policies.google.com/privacy\nيمكنك تعطيل الإعلان التحفيزي من داخل التطبيق.\n\n"
                + "٦) الدفع:\nعند شراء باقات PREMIUM عبر فودافون كاش نجمع رقم التحويل للتحقق فقط، "
                + "ولا نخزن تفاصيل بطاقتك البنكية.\n\n"
                + "٧) التواصل الصوتي:\nتُعالج المقاطع الصوتية في الغرف تمريراً للمستمعين ولا تُحفظ "
                + "بشكل دائم بعد انتهاء الجلسة.\n\n"
                + "٨) حقوقك:\nالوصول إلى بياناتك، تصحيحها، أو طلب حذف حسابك نهائياً عبر التواصل "
                + "مع المطوّر.\n\n"
                + "٩) بيانات القاصرين:\nاللعبة غير موجهة لمن هم دون ١٣ عاماً.\n\n"
                + "١٠) التغييرات:\nقد نحدّث هذه السياسة من وقت لآخر، واستمرار الاستخدام يعني الموافقة.\n";
    }

    static String terms(String lang) {
        if (lang != null && lang.startsWith("en")) {
            return "Fast Car — Terms of Service\n\n"
                    + "By using Fast Car you agree to:\n"
                    + "1. The game is provided \"as is\" without warranties of any kind.\n"
                    + "2. Accounts and scores exist for fair play; cheating, exploits or abusing the "
                    + "server may lead to account removal.\n"
                    + "3. The developer is not liable for any damages arising from the use of the game.\n"
                    + "4. The developer may change, suspend or stop parts of the service at any time.\n"
                    + "5. Your use must respect other players and applicable laws.\n"
                    + "6. These terms may be updated; continued use means acceptance.\n";
        }
        return "السيارة السريعة — شروط الاستخدام\n\n"
                + "١) قبول الشروط:\nباستخدامك اللعبة فأنت توافق على جميع الشروط التالية.\n\n"
                + "٢) الحساب والأمان:\nأنت مسؤول عن سرية كلمة مرورك، وكل نشاط من حسابك تحت مسؤوليتك، "
                + "ويحظر مشاركة الحساب أو بيعه.\n\n"
                + "٣) اللعب النظيف:\nيحظر الغش أو استغلال الثغرات أو استخدام برامج خارجية أو إهانة "
                + "اللاعبين، وقد يؤدي ذلك إلى حظر مؤقت أو دائم.\n\n"
                + "٤) المهام والإنجازات:\nتُمنح المكافآت وفق القواعد المعروضة، وأي خداع يودي لحذف "
                + "المكافآت وربما الحساب.\n\n"
                + "٥) الشراء والعملات:\nالعملات تُكتسب باللعب أو بالشراء عبر فودافون كاش، والدفع "
                + "طوعي دون استرداد إلا في خطأ تقني موثق.\n\n"
                + "٦) النسخ والتوزيع:\nيُمنع تعديل ملفات اللعبة أو فك تشفيرها أو إعادة توزيعها تجارياً.\n\n"
                + "٧) توفر الخدمة:\nقد تنقطع الخدمة للصيانة أو أسباب خارجة عن الإرادة.\n\n"
                + "٨) إخلاء المسؤولية:\nاللعبة مقدمة «كما هي» دون أي ضمانات من أي نوع.\n\n"
                + "٩) الملكية الفكرية:\nجميع حقوق اللعبة مملوكة للمطوّر.\n\n"
                + "١٠) التعديل:\nنحتفظ بحق تعديل الشروط في أي وقت، والاستمرار يعني القبول.\n";
    }
}