package com.fastcar.common;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class L10n {

    public static final String[] KEYS = {
            "play", "garage", "leaderboard", "server", "language", "logout",
            "login", "register", "username", "password", "coins", "price",
            "owned", "buy", "select", "selected", "upgrade", "engine", "tires",
            "color", "spoiler", "neon", "rims", "flame", "speed", "accel",
            "control", "ready", "score", "points", "again", "home", "watchAd",
            "adBonus", "earnCoins", "exit", "netErr", "loading", "connect",
            "startRace", "gameTap", "gameCrash", "result", "privacy", "terms", "premium",
            "google"
    };

    public static final int I_PLAY = 0, I_GARAGE = 1, I_LEADERBOARD = 2, I_SERVER = 3,
            I_LANG = 4, I_LOGOUT = 5, I_LOGIN = 6, I_REGISTER = 7, I_USERNAME = 8,
            I_PASS = 9, I_COINS = 10, I_PRICE = 11, I_OWNED = 12, I_BUY = 13,
            I_SELECT = 14, I_SELECTED = 15, I_UPGRADE = 16, I_ENGINE = 17,
            I_TIRES = 18, I_COLOR = 19, I_SPOILER = 20, I_NEON = 21, I_RIMS = 22,
            I_FLAME = 23, I_SPEED = 24, I_ACCEL = 25, I_CONTROL = 26, I_READY = 27,
            I_SCORE = 28, I_POINTS = 29, I_AGAIN = 30, I_HOME = 31, I_WATCH_AD = 32,
            I_AD_BONUS = 33, I_EARN_COINS = 34, I_EXIT = 35, I_NET_ERR = 36,
            I_LOADING = 37, I_CONNECT = 38, I_START_RACE = 39, I_GAME_TAP = 40,
            I_GAME_CRASH = 41, I_RESULT = 42, I_GOOGLE = 43;

    public static final String[] CODES = {
            "en", "ar", "fr", "de", "es", "pt", "it", "ru", "tr", "nl",
            "pl", "sv", "da", "nb", "fi", "el", "cs", "hu", "ro", "bg",
            "uk", "he", "hi", "ur", "fa", "id", "ms", "th", "vi", "ja",
            "ko", "zh", "sr", "hr", "sk", "sl", "lt", "lv", "et", "sq",
            "mk", "ka", "hy", "az", "uz", "kk", "bn", "ta", "tl", "sw"
    };

    public static final String[] NATIVE = {
            "English", "Ø§Ù„Ø¹Ø±Ø¨ÙŠØ©", "FranÃ§ais", "Deutsch", "EspaÃ±ol", "PortuguÃªs",
            "Italiano", "Ð ÑƒÑÑÐºÐ¸Ð¹", "TÃ¼rkÃ§e", "Nederlands", "Polski", "Svenska",
            "Dansk", "Norsk", "Suomi", "Î•Î»Î»Î·Î½Î¹ÎºÎ¬", "ÄŒeÅ¡tina", "Magyar", "RomÃ¢nÄƒ",
            "Ð‘ÑŠÐ»Ð³Ð°Ñ€ÑÐºÐ¸", "Ð£ÐºÑ€Ð°Ñ—Ð½ÑÑŒÐºÐ°", "×¢×‘×¨×™×ª", "à¤¹à¤¿à¤¨à¥à¤¦à¥€", "Ø§Ø±Ø¯Ùˆ", "ÙØ§Ø±Ø³ÛŒ",
            "Indonesia", "Bahasa Melayu", "à¹„à¸—à¸¢", "Tiáº¿ng Viá»‡t", "æ—¥æœ¬èªž", "í•œêµ­ì–´",
            "ä¸­æ–‡", "Srpski", "Hrvatski", "SlovenÄina", "SlovenÅ¡Äina", "LietuviÅ³",
            "LatvieÅ¡u", "Eesti", "Shqip", "ÐœÐ°ÐºÐµÐ´Ð¾Ð½ÑÐºÐ¸", "áƒ¥áƒáƒ áƒ—áƒ£áƒšáƒ˜", "Õ€Õ¡ÕµÕ¥Ö€Õ¥Õ¶",
            "AzÉ™rbaycan", "OÊ»zbek", "ÒšÐ°Ð·Ð°Ò›ÑˆÐ°", "à¦¬à¦¾à¦‚à¦²à¦¾", "à®¤à®®à®¿à®´à¯", "Filipino", "Kiswahili"
    };

    public static String normalize(String lang) {
        if (lang == null) return "en";
        String l = lang.trim().toLowerCase(Locale.US);
        if (l.isEmpty()) return "en";
        if (l.startsWith("zh")) return "zh";
        if (l.startsWith("he") || l.startsWith("iw")) return "he";
        if (l.startsWith("in")) return "id";
        if (l.startsWith("fil") || l.startsWith("tl")) return "tl";
        if (l.startsWith("no")) return "nb";
        for (String c : CODES) {
            if (c.equals(l)) return c;
        }
        String code = l.contains("-") ? l.substring(0, l.indexOf('-')) : l;
        for (String c : CODES) {
            if (c.equals(code)) return c;
        }
        return "en";
    }

    public static String auto() {
        try {
            return normalize(Locale.getDefault().getLanguage());
        } catch (Throwable t) {
            return "en";
        }
    }

    public static boolean isRtl(String lang) {
        String l = normalize(lang);
        return "ar".equals(l) || "he".equals(l) || "ur".equals(l) || "fa".equals(l);
    }

    public static String get(String lang, int key) {
        try {
            int li = IDX.get(normalize(lang));
            return TBL[li][key];
        } catch (Throwable t) {
            return L_EN[key];
        }
    }

    public static String get(String lang, String key) {
        for (int i = 0; i < KEYS.length; i++) {
            if (KEYS[i].equals(key)) return get(lang, i);
        }
        return key;
    }

    static final String[] L_EN = {
            "Play", "Garage", "Leaderboard", "Server", "Language", "Log out",
            "Login", "Sign up", "Username", "Password", "Coins", "Price",
            "Owned", "Buy", "Select", "Selected", "Upgrade", "Engine", "Tires",
            "Color", "Spoiler", "Neon", "Rims", "Flame", "Speed", "Acceleration",
            "Control", "Ready", "Score", "Points", "Again", "Home", "Watch ad",
            "Earn bonus coins", "Earn coins", "Exit", "No connection", "Loading",
            "Connected", "Start race", "Tap left / center / right to change lane",
            "Crash!", "Result"
    , "Privacy Policy", "Terms of Service","Premium", "Google"
    };

    static final String[] L_AR = {
            "العب", "الكراج", "لوحة الصدارة", "السيرفر", "اللغة", "تسجيل الخروج",
            "دخول", "اشتراك", "اسم المستخدم", "كلمة المرور", "عملات", "السعر",
            "تملكها", "شراء", "اختيار", "مختارة", "تطوير", "المحرك", "العجلات",
            "اللون", "جناح", "نيون", "جنوط", "شعلة", "السرعة", "التسارع",
            "التحكم", "استعد", "النقاط", "نقاط", "مرة أخرى", "القائمة", "شاهد إعلان",
            "اربح عملات إضافية", "اربح عملات", "خروج", "لا يوجد اتصال", "جاري التحميل",
            "متصل", "ابدأ السباق", "اضغط يسار/وسط/يمين لتبديل المسار",
            "تصادم!", "النتيجة"
    , "سياسة الخصوصية", "شروط الاستخدام","Premium", "Google"
    };

    static final String[] L_FR = {
            "Jouer", "Garage", "Classement", "Serveur", "Langue", "DÃ©connexion",
            "Connexion", "S'inscrire", "Nom d'utilisateur", "Mot de passe", "PiÃ¨ces",
            "Prix", "PossÃ©dÃ©e", "Acheter", "Choisir", "Choisie", "AmÃ©liorer",
            "Moteur", "Pneus", "Couleur", "Aileron", "NÃ©on", "Jantes", "Flamme",
            "Vitesse", "AccÃ©lÃ©ration", "ContrÃ´le", "PrÃªt", "Score", "Points",
            "Encore", "Accueil", "Voir une pub", "Gagnez des piÃ¨ces bonus",
            "Gagnez des piÃ¨ces", "Quitter", "Pas de connexion", "Chargement",
            "ConnectÃ©", "DÃ©marrer la course",
            "Touchez gauche / centre / droite pour changer de voie",
            "Crash !", "RÃ©sultat"
    , "Politique de confidentialitÃ©", "Conditions d'utilisation"
    ,"Premium", "Google"
    };

    static final String[] L_DE = {
            "Spielen", "Garage", "Bestenliste", "Server", "Sprache", "Abmelden",
            "Anmelden", "Registrieren", "Benutzername", "Passwort", "MÃ¼nzen", "Preis",
            "Besessen", "Kaufen", "AuswÃ¤hlen", "AusgewÃ¤hlt", "Aufwerten", "Motor",
            "Reifen", "Farbe", "Spoiler", "Neon", "Felgen", "Flamme",
            "Geschwindigkeit", "Beschleunigung", "Steuerung", "Bereit", "Punktzahl",
            "Punkte", "Nochmal", "Startseite", "Werbung ansehen",
            "BonusmÃ¼nzen verdienen", "MÃ¼nzen verdienen", "Beenden", "Keine Verbindung",
            "LÃ¤dt", "Verbunden", "Rennen starten",
            "Links / Mitte / Rechts tippen zum Spurwechsel",
            "Crash!", "Ergebnis"
    , "Datenschutz", "Nutzungsbedingungen"
    ,"Premium", "Google"
    };

    static final String[] L_ES = {
            "Jugar", "Garaje", "ClasificaciÃ³n", "Servidor", "Idioma", "Cerrar sesiÃ³n",
            "Iniciar sesiÃ³n", "Registrarse", "Usuario", "ContraseÃ±a", "Monedas",
            "Precio", "Comprada", "Comprar", "Elegir", "Elegida", "Mejorar", "Motor",
            "NeumÃ¡ticos", "Color", "AlerÃ³n", "NeÃ³n", "Llantas", "Llama", "Velocidad",
            "AceleraciÃ³n", "Control", "Listo", "PuntuaciÃ³n", "Puntos", "De nuevo",
            "Inicio", "Ver anuncio", "Gana monedas extra", "Gana monedas", "Salir",
            "Sin conexiÃ³n", "Cargando", "Conectado", "Empezar la carrera",
            "Toca izquierda / centro / derecha para cambiar",
            "Â¡Choque!", "Resultado"
    , "PolÃ­tica de privacidad", "TÃ©rminos del servicio"
    ,"Premium", "Google"
    };

    static final String[] L_PT = {
            "Jogar", "Garagem", "ClassificaÃ§Ã£o", "Servidor", "Idioma", "Sair",
            "Entrar", "Registrar", "UsuÃ¡rio", "Senha", "Moedas", "PreÃ§o", "Comprada",
            "Comprar", "Escolher", "Escolhida", "Melhorar", "Motor", "Pneus", "Cor",
            "AerofÃ³lio", "Neon", "Rodas", "Chama", "Velocidade", "AceleraÃ§Ã£o",
            "Controle", "Pronto", "PontuaÃ§Ã£o", "Pontos", "De novo", "InÃ­cio",
            "Ver anÃºncio", "Ganhe moedas extras", "Ganhe moedas", "Sair",
            "Sem conexÃ£o", "Carregando", "Conectado", "ComeÃ§ar a corrida",
            "Toque esquerda / centro / direita para trocar",
            "ColisÃ£o!", "Resultado"
    , "PolÃ­tica de privacidade", "Termos de serviÃ§o"
    ,"Premium", "Google"
    };

    static final String[] L_IT = {
            "Gioca", "Garage", "Classifica", "Server", "Lingua", "Esci", "Accedi",
            "Registrati", "Nome utente", "Password", "Monete", "Prezzo", "Posseduta",
            "Compra", "Scegli", "Scelta", "Migliora", "Motore", "Pneumatici", "Colore",
            "Spoiler", "Neon", "Cerchi", "Fiamma", "VelocitÃ ", "Accelerazione",
            "Controllo", "Pronto", "Punteggio", "Punti", "Ancora", "Home",
            "Guarda annuncio", "Guadagna monete bonus", "Guadagna monete", "Esci",
            "Nessuna connessione", "Caricamento", "Connesso", "Inizia la gara",
            "Tocca sinistra / centro / destra per cambiare",
            "Crash!", "Risultato"
    , "Informativa privacy", "Termini di servizio"
    ,"Premium", "Google"
    };

    static final String[] L_RU = {
            "Ð˜Ð³Ñ€Ð°Ñ‚ÑŒ", "Ð“Ð°Ñ€Ð°Ð¶", "Ð ÐµÐºÐ¾Ñ€Ð´Ñ‹", "Ð¡ÐµÑ€Ð²ÐµÑ€", "Ð¯Ð·Ñ‹Ðº", "Ð’Ñ‹Ð¹Ñ‚Ð¸", "Ð’Ð¾Ð¹Ñ‚Ð¸",
            "Ð ÐµÐ³Ð¸ÑÑ‚Ñ€Ð°Ñ†Ð¸Ñ", "Ð˜Ð¼Ñ", "ÐŸÐ°Ñ€Ð¾Ð»ÑŒ", "ÐœÐ¾Ð½ÐµÑ‚Ñ‹", "Ð¦ÐµÐ½Ð°", "ÐšÑƒÐ¿Ð»ÐµÐ½Ð°", "ÐšÑƒÐ¿Ð¸Ñ‚ÑŒ",
            "Ð’Ñ‹Ð±Ñ€Ð°Ñ‚ÑŒ", "Ð’Ñ‹Ð±Ñ€Ð°Ð½Ð°", "Ð£Ð»ÑƒÑ‡ÑˆÐ¸Ñ‚ÑŒ", "Ð”Ð²Ð¸Ð³Ð°Ñ‚ÐµÐ»ÑŒ", "Ð¨Ð¸Ð½Ñ‹", "Ð¦Ð²ÐµÑ‚", "Ð¡Ð¿Ð¾Ð¹Ð»ÐµÑ€",
            "ÐÐµÐ¾Ð½", "Ð”Ð¸ÑÐºÐ¸", "ÐŸÐ»Ð°Ð¼Ñ", "Ð¡ÐºÐ¾Ñ€Ð¾ÑÑ‚ÑŒ", "Ð Ð°Ð·Ð³Ð¾Ð½", "Ð£Ð¿Ñ€Ð°Ð²Ð»ÐµÐ½Ð¸Ðµ", "Ð“Ð¾Ñ‚Ð¾Ð²",
            "Ð¡Ñ‡Ñ‘Ñ‚", "ÐžÑ‡ÐºÐ¸", "Ð•Ñ‰Ñ‘ Ñ€Ð°Ð·", "ÐœÐµÐ½ÑŽ", "Ð¡Ð¼Ð¾Ñ‚Ñ€ÐµÑ‚ÑŒ Ñ€ÐµÐºÐ»Ð°Ð¼Ñƒ",
            "ÐŸÐ¾Ð»ÑƒÑ‡Ð¸ Ð±Ð¾Ð½ÑƒÑ-Ð¼Ð¾Ð½ÐµÑ‚Ñ‹", "ÐŸÐ¾Ð»ÑƒÑ‡Ð¸ Ð¼Ð¾Ð½ÐµÑ‚Ñ‹", "Ð’Ñ‹Ñ…Ð¾Ð´", "ÐÐµÑ‚ ÑÐ¾ÐµÐ´Ð¸Ð½ÐµÐ½Ð¸Ñ",
            "Ð—Ð°Ð³Ñ€ÑƒÐ·ÐºÐ°", "ÐŸÐ¾Ð´ÐºÐ»ÑŽÑ‡ÐµÐ½Ð¾", "Ð¡Ñ‚Ð°Ñ€Ñ‚ Ð³Ð¾Ð½ÐºÐ¸",
            "Ð¢Ð°Ð¿Ð½Ð¸ ÑÐ»ÐµÐ²Ð° / Ñ†ÐµÐ½Ñ‚Ñ€ / ÑÐ¿Ñ€Ð°Ð²Ð°, Ñ‡Ñ‚Ð¾Ð±Ñ‹ ÑÐ¼ÐµÐ½Ð¸Ñ‚ÑŒ Ð¿Ð¾Ð»Ð¾ÑÑƒ",
            "ÐÐ²Ð°Ñ€Ð¸Ñ!", "Ð ÐµÐ·ÑƒÐ»ÑŒÑ‚Ð°Ñ‚"
    , "ÐŸÐ¾Ð»Ð¸Ñ‚Ð¸ÐºÐ° ÐºÐ¾Ð½Ñ„Ð¸Ð´ÐµÐ½Ñ†Ð¸Ð°Ð»ÑŒÐ½Ð¾ÑÑ‚Ð¸", "Ð£ÑÐ»Ð¾Ð²Ð¸Ñ Ð¸ÑÐ¿Ð¾Ð»ÑŒÐ·Ð¾Ð²Ð°Ð½Ð¸Ñ"
    ,"Premium", "Google"
    };

    static final String[] L_TR = {
            "Oyna", "Garaj", "SÄ±ralama", "Sunucu", "Dil", "Ã‡Ä±kÄ±ÅŸ", "GiriÅŸ", "KayÄ±t ol",
            "KullanÄ±cÄ± adÄ±", "Åžifre", "Para", "Fiyat", "Sahip", "SatÄ±n al", "SeÃ§",
            "SeÃ§ildi", "GeliÅŸtir", "Motor", "Lastik", "Renk", "Spoiler", "Neon",
            "Jant", "Alev", "HÄ±z", "HÄ±zlanma", "Kontrol", "HazÄ±r", "Skor", "Puan",
            "Tekrar", "Ana menÃ¼", "Reklam izle", "Bonus para kazan", "Para kazan",
            "Ã‡Ä±kÄ±ÅŸ", "BaÄŸlantÄ± yok", "YÃ¼kleniyor", "BaÄŸlÄ±", "YarÄ±ÅŸÄ± baÅŸlat",
            "YÃ¶n deÄŸiÅŸtirmek iÃ§in sola / orta / saÄŸa dokun",
            "Ã‡arpÄ±ÅŸma!", "SonuÃ§"
    , "Gizlilik PolitikasÄ±", "KullanÄ±m ÅžartlarÄ±"
    ,"Premium", "Google"
    };

    static final String[] L_NL = {
            "Spelen", "Garage", "Klassement", "Server", "Taal", "Uitloggen",
            "Inloggen", "Registreren", "Gebruikersnaam", "Wachtwoord", "Munten",
            "Prijs", "Gekocht", "Kopen", "Kiezen", "Gekozen", "Upgraden", "Motor",
            "Banden", "Kleur", "Spoiler", "Neon", "Velgen", "Vlam", "Snelheid",
            "Versnelling", "Besturing", "Klaar", "Score", "Punten", "Opnieuw",
            "Home", "Advertentie bekijken", "Bonusmunten verdienen", "Munten verdienen",
            "Afsluiten", "Geen verbinding", "Laden", "Verbonden", "Race starten",
            "Tik links / midden / rechts om van rijstrook te wisselen",
            "Botsing!", "Resultaat"
    , "Privacybeleid", "Servicevoorwaarden"
    ,"Premium", "Google"
    };

    static final String[] L_PL = {
            "Graj", "GaraÅ¼", "Ranking", "Serwer", "JÄ™zyk", "Wyloguj", "Zaloguj",
            "Zarejestruj", "Nazwa", "HasÅ‚o", "Monety", "Cena", "Kupiona", "Kup",
            "Wybierz", "Wybrano", "Ulepsz", "Silnik", "Opony", "Kolor", "Spoiler",
            "Neon", "Felgi", "PÅ‚omieÅ„", "PrÄ™dkoÅ›Ä‡", "Przyspieszenie", "Sterowanie",
            "Gotowy", "Wynik", "Punkty", "Jeszcze raz", "Menu", "Obejrzyj reklamÄ™",
            "Zdobywaj bonusowe monety", "Zdobywaj monety", "WyjÅ›cie", "Brak poÅ‚Ä…czenia",
            "Åadowanie", "PoÅ‚Ä…czono", "Start wyÅ›cigu",
            "Dotknij lewo / Å›rodek / prawo, aby zmieniÄ‡ pas",
            "Kraksa!", "Wynik"
    , "Polityka prywatnoÅ›ci", "Warunki korzystania"
    ,"Premium", "Google"
    };

    static final String[] L_SV = {
            "Spela", "Garage", "Topplista", "Server", "SprÃ¥k", "Logga ut", "Logga in",
            "Registrera", "AnvÃ¤ndarnamn", "LÃ¶senord", "Mynt", "Pris", "Ã„gd", "KÃ¶p",
            "VÃ¤lj", "Vald", "Uppgradera", "Motor", "DÃ¤ck", "FÃ¤rg", "Spoiler", "Neon",
            "FÃ¤lgar", "LÃ¥ga", "Hastighet", "Acceleration", "Styrning", "Redo", "PoÃ¤ng",
            "PoÃ¤ng", "Igen", "Hem", "Se annons", "TjÃ¤na bonusmynt", "TjÃ¤na mynt",
            "Avsluta", "Ingen anslutning", "Laddar", "Ansluten", "Starta loppet",
            "Tryck vÃ¤nster / mitten / hÃ¶ger fÃ¶r att byta fil",
            "Krock!", "Resultat"
    , "Integritetspolicy", "AnvÃ¤ndarvillkor"
    ,"Premium", "Google"
    };

    static final String[] L_DA = {
            "Spil", "Garage", "Resultatliste", "Server", "Sprog", "Log ud", "Log ind",
            "Registrer", "Brugernavn", "Adgangskode", "MÃ¸nter", "Pris", "Ejet", "KÃ¸b",
            "VÃ¦lg", "Valgt", "Opgrader", "Motor", "DÃ¦k", "Farve", "Spoiler", "Neon",
            "FÃ¦lge", "Flamme", "Hastighed", "Acceleration", "Styring", "Klar", "Score",
            "Point", "Igen", "Hjem", "Se annonce", "Tjen bonusmÃ¸nter", "Tjen mÃ¸nter",
            "Afslut", "Ingen forbindelse", "IndlÃ¦ser", "Forbundet", "Start lÃ¸bet",
            "Tryk venstre / midten / hÃ¸jre for at skifte bane",
            "Kollision!", "Resultat"
    , "Privatlivspolitik", "VilkÃ¥r for brug"
    ,"Premium", "Google"
    };

    static final String[] L_NB = {
            "Spill", "Garasje", "Toppliste", "Server", "SprÃ¥k", "Logg ut", "Logg inn",
            "Registrer", "Brukernavn", "Passord", "Mynter", "Pris", "Eid", "KjÃ¸p",
            "Velg", "Valgt", "Oppgrader", "Motor", "Dekk", "Farge", "Spoiler", "Neon",
            "Felger", "Flamme", "Fart", "Akselerasjon", "Styring", "Klar", "Poeng",
            "Poeng", "Igjen", "Hjem", "Se reklame", "Tjen bonusmynter", "Tjen mynter",
            "Avslutt", "Ingen tilkobling", "Laster", "Tilkoblet", "Start lÃ¸pet",
            "Trykk venstre / midten / hÃ¸yre for Ã¥ bytte fil",
            "Kollisjon!", "Resultat"
    , "Personvern", "BruksvilkÃ¥r"
    ,"Premium", "Google"
    };

    static final String[] L_FI = {
            "Pelaa", "Autotalli", "Tulokset", "Palvelin", "Kieli", "Kirjaudu ulos",
            "Kirjaudu", "RekisterÃ¶idy", "KÃ¤yttÃ¤jÃ¤nimi", "Salasana", "Kolikot", "Hinta",
            "Omistettu", "Osta", "Valitse", "Valittu", "Paranna", "Moottori", "Renkaat",
            "VÃ¤ri", "Spoileri", "Neon", "Vanteet", "Liekki", "Nopeus", "Kiihtyvyys",
            "Ohjaus", "Valmis", "Pisteet", "Pisteet", "Uudelleen", "Koti", "Katso mainos",
            "Ansaitse bonuskolikoita", "Ansaitse kolikoita", "Poistu", "Ei yhteyttÃ¤",
            "Ladataan", "Yhdistetty", "Aloita kilpailu",
            "Napauta vasen / keski / oikea vaihtaaksesi kaistaa",
            "Kolari!", "Tulos"
    , "TietosuojakÃ¤ytÃ¤ntÃ¶", "KÃ¤yttÃ¶ehdot"
    ,"Premium", "Google"
    };

    static final String[] L_EL = {
            "Î Î±Î¯Î¾Îµ", "Î“ÎºÎ±ÏÎ¬Î¶", "ÎšÎ±Ï„Î¬Ï„Î±Î¾Î·", "Î£ÎµÏÎ²ÎµÏ", "Î“Î»ÏŽÏƒÏƒÎ±", "Î‘Ï€Î¿ÏƒÏÎ½Î´ÎµÏƒÎ·", "Î£ÏÎ½Î´ÎµÏƒÎ·",
            "Î•Î³Î³ÏÎ±Ï†Î®", "ÎŒÎ½Î¿Î¼Î±", "ÎšÏ‰Î´Î¹ÎºÏŒÏ‚", "ÎÎ¿Î¼Î¯ÏƒÎ¼Î±Ï„Î±", "Î¤Î¹Î¼Î®", "Î‘Î³Î¿ÏÎ¬ÏƒÏ„Î·ÎºÎµ", "Î‘Î³ÏŒÏÎ±ÏƒÎµ",
            "Î•Ï€Î¹Î»Î¿Î³Î®", "Î•Ï€Î¹Î»ÎµÎ³Î¼Î­Î½Î¿", "Î‘Î½Î±Î²Î¬Î¸Î¼Î¹ÏƒÎ·", "ÎšÎ¹Î½Î·Ï„Î®ÏÎ±Ï‚", "Î•Î»Î±ÏƒÏ„Î¹ÎºÎ¬", "Î§ÏÏŽÎ¼Î±",
            "Î¦Ï„ÎµÏÏŒ", "ÎÎ­Î¿Î½", "Î–Î¬Î½Ï„ÎµÏ‚", "Î¦Î»ÏŒÎ³Î±", "Î¤Î±Ï‡ÏÏ„Î·Ï„Î±", "Î•Ï€Î¹Ï„Î¬Ï‡Ï…Î½ÏƒÎ·", "ÎˆÎ»ÎµÎ³Ï‡Î¿Ï‚",
            "ÎˆÏ„Î¿Î¹Î¼Î¿Ï‚", "Î’Î±Î¸Î¼Î¿Î¯", "Î ÏŒÎ½Ï„Î¿Î¹", "ÎžÎ±Î½Î¬", "Î‘Ï†ÎµÏ„Î·ÏÎ¯Î±", "Î”ÎµÏ‚ Î´Î¹Î±Ï†Î®Î¼Î¹ÏƒÎ·",
            "ÎšÎ­ÏÎ´Î¹ÏƒÎµ Î¼Ï€ÏŒÎ½Î¿Ï…Ï‚ Î½Î¿Î¼Î¯ÏƒÎ¼Î±Ï„Î±", "ÎšÎ­ÏÎ´Î¹ÏƒÎµ Î½Î¿Î¼Î¯ÏƒÎ¼Î±Ï„Î±", "ÎˆÎ¾Î¿Î´Î¿Ï‚", "Î§Ï‰ÏÎ¯Ï‚ ÏƒÏÎ½Î´ÎµÏƒÎ·",
            "Î¦ÏŒÏÏ„Ï‰ÏƒÎ·", "Î£Ï…Î½Î´ÎµÎ´ÎµÎ¼Î­Î½Î¿", "ÎˆÎ½Î±ÏÎ¾Î· Î±Î³ÏŽÎ½Î±",
            "Î Î¬Ï„Î± Î±ÏÎ¹ÏƒÏ„ÎµÏÎ¬ / ÎºÎ­Î½Ï„ÏÎ¿ / Î´ÎµÎ¾Î¹Î¬ Î³Î¹Î± Î±Î»Î»Î±Î³Î® Î»Ï‰ÏÎ¯Î´Î±Ï‚",
            "Î£ÏÎ³ÎºÏÎ¿Ï…ÏƒÎ·!", "Î‘Ï€Î¿Ï„Î­Î»ÎµÏƒÎ¼Î±"
    , "Î Î¿Î»Î¹Ï„Î¹ÎºÎ® Î±Ï€Î¿ÏÏÎ®Ï„Î¿Ï…", "ÎŒÏÎ¿Î¹ Ï‡ÏÎ®ÏƒÎ·Ï‚"
    ,"Premium", "Google"
    };

    static final String[] L_CS = {
            "HrÃ¡t", "GarÃ¡Å¾", "Å½ebÅ™Ã­Äek", "Server", "Jazyk", "OdhlÃ¡sit", "PÅ™ihlÃ¡sit",
            "Registrace", "UÅ¾ivatelskÃ© jmÃ©no", "Heslo", "Mince", "Cena", "VlastnÄ›no",
            "Koupit", "Vybrat", "VybrÃ¡no", "VylepÅ¡it", "Motor", "Pneumatiky", "Barva",
            "Spoiler", "Neon", "Kola", "Plamen", "Rychlost", "ZrychlenÃ­", "OvlÃ¡dÃ¡nÃ­",
            "PÅ™ipraven", "SkÃ³re", "Body", "Znovu", "DomÅ¯", "Sledovat reklamu",
            "ZÃ­skej bonusovÃ© mince", "ZÃ­skej mince", "Konec", "Å½Ã¡dnÃ© pÅ™ipojenÃ­",
            "NaÄÃ­tÃ¡m", "PÅ™ipojeno", "Spustit zÃ¡vod",
            "Klepni vlevo / na stÅ™ed / vpravo pro zmÄ›nu pruhu",
            "HavÃ¡rie!", "VÃ½sledek"
    , "ZÃ¡sady ochrany osobnÃ­ch ÃºdajÅ¯", "PodmÃ­nky pouÅ¾itÃ­"
    ,"Premium", "Google"
    };

    static final String[] L_HU = {
            "JÃ¡tÃ©k", "GarÃ¡zs", "Ranglista", "Szerver", "Nyelv", "KijelentkezÃ©s",
            "BejelentkezÃ©s", "RegisztrÃ¡ciÃ³", "FelhasznÃ¡lÃ³nÃ©v", "JelszÃ³", "Ã‰rmÃ©k", "Ãr",
            "Birtokolt", "VÃ¡sÃ¡rlÃ¡s", "KivÃ¡laszt", "KivÃ¡lasztva", "FejlesztÃ©s", "Motor",
            "Gumik", "SzÃ­n", "Spoiler", "Neon", "Felnik", "LÃ¡ng", "SebessÃ©g",
            "GyorsulÃ¡s", "IrÃ¡nyÃ­tÃ¡s", "KÃ©sz", "PontszÃ¡m", "Pontok", "Ãšjra", "FÅ‘oldal",
            "ReklÃ¡m nÃ©zÃ©se", "BÃ³nusz Ã©rmÃ©k", "Ã‰rmÃ©k gyÅ±jtÃ©se", "KilÃ©pÃ©s", "Nincs kapcsolat",
            "BetÃ¶ltÃ©s", "Csatlakozva", "Verseny indÃ­tÃ¡sa",
            "Ã‰rintsd bal / kÃ¶zÃ©p / jobb a sÃ¡vvÃ¡ltÃ¡shoz",
            "ÃœtkÃ¶zÃ©s!", "EredmÃ©ny"
    , "AdatvÃ©delmi irÃ¡nyelvek", "FelhasznÃ¡lÃ¡si feltÃ©telek"
    ,"Premium", "Google"
    };

    static final String[] L_RO = {
            "JoacÄƒ", "Garaj", "Clasament", "Server", "LimbÄƒ", "Deconectare",
            "Autentificare", "ÃŽnregistrare", "Nume", "ParolÄƒ", "Monede", "PreÈ›",
            "DeÈ›inutÄƒ", "CumpÄƒrÄƒ", "Alege", "AleasÄƒ", "ÃŽmbunÄƒtÄƒÈ›eÈ™te", "Motor",
            "Anvelope", "Culoare", "Spoiler", "Neon", "Jante", "FlacÄƒrÄƒ", "VitezÄƒ",
            "AcceleraÈ›ie", "Control", "Gata", "Scor", "Puncte", "Din nou", "AcasÄƒ",
            "Vezi reclamÄƒ", "CÃ¢È™tigÄƒ monede bonus", "CÃ¢È™tigÄƒ monede", "IeÈ™ire",
            "FÄƒrÄƒ conexiune", "Se Ã®ncarcÄƒ", "Conectat", "ÃŽncepe cursa",
            "Atinge stÃ¢nga / centru / dreapta pentru a schimba banda",
            "Accident!", "Rezultat"
    , "Politica de confidenÈ›ialitate", "Termeni de utilizare"
    ,"Premium", "Google"
    };

    static final String[] L_BG = {
            "Ð˜Ð³Ñ€Ð°Ð¹", "Ð“Ð°Ñ€Ð°Ð¶", "ÐšÐ»Ð°ÑÐ°Ñ†Ð¸Ñ", "Ð¡ÑŠÑ€Ð²ÑŠÑ€", "Ð•Ð·Ð¸Ðº", "Ð˜Ð·Ñ…Ð¾Ð´", "Ð’Ñ…Ð¾Ð´",
            "Ð ÐµÐ³Ð¸ÑÑ‚Ñ€Ð°Ñ†Ð¸Ñ", "ÐŸÐ¾Ñ‚Ñ€ÐµÐ±Ð¸Ñ‚ÐµÐ»ÑÐºÐ¾ Ð¸Ð¼Ðµ", "ÐŸÐ°Ñ€Ð¾Ð»Ð°", "ÐœÐ¾Ð½ÐµÑ‚Ð¸", "Ð¦ÐµÐ½Ð°", "ÐšÑƒÐ¿ÐµÐ½Ð°",
            "ÐšÑƒÐ¿Ð¸", "Ð˜Ð·Ð±ÐµÑ€Ð¸", "Ð˜Ð·Ð±Ñ€Ð°Ð½Ð°", "ÐŸÐ¾Ð´Ð¾Ð±Ñ€Ð¸", "Ð”Ð²Ð¸Ð³Ð°Ñ‚ÐµÐ»", "Ð“ÑƒÐ¼Ð¸", "Ð¦Ð²ÑÑ‚",
            "Ð¡Ð¿Ð¾Ð¹Ð»ÐµÑ€", "ÐÐµÐ¾Ð½", "Ð”Ð¶Ð°Ð½Ñ‚Ð¸", "ÐŸÐ»Ð°Ð¼ÑŠÐº", "Ð¡ÐºÐ¾Ñ€Ð¾ÑÑ‚", "Ð£ÑÐºÐ¾Ñ€ÐµÐ½Ð¸Ðµ", "Ð£Ð¿Ñ€Ð°Ð²Ð»ÐµÐ½Ð¸Ðµ",
            "Ð“Ð¾Ñ‚Ð¾Ð²", "Ð ÐµÐ·ÑƒÐ»Ñ‚Ð°Ñ‚", "Ð¢Ð¾Ñ‡ÐºÐ¸", "ÐžÑ‚Ð½Ð¾Ð²Ð¾", "ÐÐ°Ñ‡Ð°Ð»Ð¾", "Ð“Ð»ÐµÐ´Ð°Ð¹ Ñ€ÐµÐºÐ»Ð°Ð¼Ð°",
            "ÐŸÐµÑ‡ÐµÐ»Ð¸ Ð±Ð¾Ð½ÑƒÑ Ð¼Ð¾Ð½ÐµÑ‚Ð¸", "ÐŸÐµÑ‡ÐµÐ»Ð¸ Ð¼Ð¾Ð½ÐµÑ‚Ð¸", "Ð˜Ð·Ñ…Ð¾Ð´", "ÐÑÐ¼Ð° Ð²Ñ€ÑŠÐ·ÐºÐ°",
            "Ð—Ð°Ñ€ÐµÐ¶Ð´Ð°Ð½Ðµ", "Ð¡Ð²ÑŠÑ€Ð·Ð°Ð½", "Ð¡Ñ‚Ð°Ñ€Ñ‚Ð¸Ñ€Ð°Ð¹ ÑÑŠÑÑ‚ÐµÐ·Ð°Ð½Ð¸ÐµÑ‚Ð¾",
            "Ð”Ð¾ÐºÐ¾ÑÐ½Ð¸ Ð»ÑÐ²Ð¾ / Ñ†ÐµÐ½Ñ‚ÑŠÑ€ / Ð´ÑÑÐ½Ð¾ Ð·Ð° ÑÐ¼ÑÐ½Ð° Ð½Ð° Ð»ÐµÐ½Ñ‚Ð°Ñ‚Ð°",
            "ÐšÐ°Ñ‚Ð°ÑÑ‚Ñ€Ð¾Ñ„Ð°!", "Ð ÐµÐ·ÑƒÐ»Ñ‚Ð°Ñ‚"
    , "ÐŸÐ¾Ð»Ð¸Ñ‚Ð¸ÐºÐ° Ð·Ð° Ð¿Ð¾Ð²ÐµÑ€Ð¸Ñ‚ÐµÐ»Ð½Ð¾ÑÑ‚", "Ð£ÑÐ»Ð¾Ð²Ð¸Ñ Ð·Ð° Ð¿Ð¾Ð»Ð·Ð²Ð°Ð½Ðµ"
    ,"Premium", "Google"
    };

    static final String[] L_UK = {
            "Ð“Ñ€Ð°Ñ‚Ð¸", "Ð“Ð°Ñ€Ð°Ð¶", "Ð ÐµÐ¹Ñ‚Ð¸Ð½Ð³", "Ð¡ÐµÑ€Ð²ÐµÑ€", "ÐœÐ¾Ð²Ð°", "Ð’Ð¸Ð¹Ñ‚Ð¸", "Ð£Ð²Ñ–Ð¹Ñ‚Ð¸",
            "Ð ÐµÑ”ÑÑ‚Ñ€Ð°Ñ†Ñ–Ñ", "Ð†Ð¼'Ñ", "ÐŸÐ°Ñ€Ð¾Ð»ÑŒ", "ÐœÐ¾Ð½ÐµÑ‚Ð¸", "Ð¦Ñ–Ð½Ð°", "ÐšÑƒÐ¿Ð»ÐµÐ½Ð°", "ÐšÑƒÐ¿Ð¸Ñ‚Ð¸",
            "Ð’Ð¸Ð±Ñ€Ð°Ñ‚Ð¸", "Ð’Ð¸Ð±Ñ€Ð°Ð½Ð°", "ÐŸÐ¾ÐºÑ€Ð°Ñ‰Ð¸Ñ‚Ð¸", "Ð”Ð²Ð¸Ð³ÑƒÐ½", "Ð¨Ð¸Ð½Ð¸", "ÐšÐ¾Ð»Ñ–Ñ€", "Ð¡Ð¿Ð¾Ð¹Ð»ÐµÑ€",
            "ÐÐµÐ¾Ð½", "Ð”Ð¸ÑÐºÐ¸", "ÐŸÐ¾Ð»ÑƒÐ¼'Ñ", "Ð¨Ð²Ð¸Ð´ÐºÑ–ÑÑ‚ÑŒ", "Ð Ð¾Ð·Ð³Ñ–Ð½", "ÐšÐµÑ€ÑƒÐ²Ð°Ð½Ð½Ñ", "Ð“Ð¾Ñ‚Ð¾Ð²Ð¸Ð¹",
            "Ð Ð°Ñ…ÑƒÐ½Ð¾Ðº", "ÐžÑ‡ÐºÐ¸", "Ð©Ðµ Ñ€Ð°Ð·", "Ð“Ð¾Ð»Ð¾Ð²Ð½Ð°", "Ð”Ð¸Ð²Ð¸Ñ‚Ð¸ÑÑ Ñ€ÐµÐºÐ»Ð°Ð¼Ñƒ",
            "ÐžÑ‚Ñ€Ð¸Ð¼Ð°Ð¹ Ð±Ð¾Ð½ÑƒÑÐ½Ñ– Ð¼Ð¾Ð½ÐµÑ‚Ð¸", "ÐžÑ‚Ñ€Ð¸Ð¼Ð°Ð¹ Ð¼Ð¾Ð½ÐµÑ‚Ð¸", "Ð’Ð¸Ñ…Ñ–Ð´", "ÐÐµÐ¼Ð°Ñ” Ð·Ð²'ÑÐ·ÐºÑƒ",
            "Ð—Ð°Ð²Ð°Ð½Ñ‚Ð°Ð¶ÐµÐ½Ð½Ñ", "ÐŸÑ–Ð´ÐºÐ»ÑŽÑ‡ÐµÐ½Ð¾", "Ð¡Ñ‚Ð°Ñ€Ñ‚ Ð³Ð¾Ð½ÐºÐ¸",
            "Ð¢Ð¾Ñ€ÐºÐ½Ð¸ÑÑŒ Ð»Ñ–Ð²Ð¾Ñ€ÑƒÑ‡ / Ñ†ÐµÐ½Ñ‚Ñ€ / Ð¿Ñ€Ð°Ð²Ð¾Ñ€ÑƒÑ‡, Ñ‰Ð¾Ð± Ð·Ð¼Ñ–Ð½Ð¸Ñ‚Ð¸ ÑÐ¼ÑƒÐ³Ñƒ",
            "ÐÐ²Ð°Ñ€Ñ–Ñ!", "Ð ÐµÐ·ÑƒÐ»ÑŒÑ‚Ð°Ñ‚"
    , "ÐŸÐ¾Ð»Ñ–Ñ‚Ð¸ÐºÐ° ÐºÐ¾Ð½Ñ„Ñ–Ð´ÐµÐ½Ñ†Ñ–Ð¹Ð½Ð¾ÑÑ‚Ñ–", "Ð£Ð¼Ð¾Ð²Ð¸ Ð²Ð¸ÐºÐ¾Ñ€Ð¸ÑÑ‚Ð°Ð½Ð½Ñ"
    ,"Premium", "Google"
    };

    static final String[] L_HE = {
            "×œ×©×—×§", "×ž×•×¡×š", "×©×™××™×", "×©×¨×ª", "×©×¤×”", "×”×ª× ×ª×§×•×ª", "×›× ×™×¡×”", "×”×¨×©×ž×”",
            "×©× ×ž×©×ª×ž×©", "×¡×™×¡×ž×”", "×ž×˜×‘×¢×•×ª", "×ž×—×™×¨", "×‘×‘×¢×œ×•×ª×š", "×§× ×™×™×”", "×‘×—×™×¨×”",
            "× ×‘×—×¨", "×©×“×¨×•×’", "×ž× ×•×¢", "×¦×ž×™×’×™×", "×¦×‘×¢", "×¡×¤×•×™×œ×¨", "× ×™××•×Ÿ", "×—×™×©×•×§×™×",
            "×œ×”×‘×”", "×ž×”×™×¨×•×ª", "×ª××•×¦×”", "×©×œ×™×˜×”", "×ž×•×›×Ÿ", "× ×™×§×•×“", "× ×§×•×“×•×ª", "×©×•×‘",
            "×‘×™×ª", "×¦×¤×” ×‘×¤×¨×¡×•×ž×ª", "×¦×‘×•×¨ ×ž×˜×‘×¢×•×ª ×‘×•× ×•×¡", "×¦×‘×•×¨ ×ž×˜×‘×¢×•×ª", "×™×¦×™××”",
            "××™×Ÿ ×—×™×‘×•×¨", "×˜×•×¢×Ÿ", "×ž×—×•×‘×¨", "×”×ª×—×œ ×ž×¨×•×¥",
            "×”×§×© ×©×ž××œ / ×ž×¨×›×– / ×™×ž×™×Ÿ ×œ×©×™× ×•×™ × ×ª×™×‘",
            "×”×ª×¨×¡×§×•×ª!", "×ª×•×¦××”"
    , "×ž×“×™× ×™×•×ª ×¤×¨×˜×™×•×ª", "×ª× ××™ ×©×™×ž×•×©"
    ,"Premium", "Google"
    };

    static final String[] L_HI = {
            "à¤–à¥‡à¤²à¥‡à¤‚", "à¤—à¥ˆà¤°à¥‡à¤œ", "à¤²à¥€à¤¡à¤°à¤¬à¥‹à¤°à¥à¤¡", "à¤¸à¤°à¥à¤µà¤°", "à¤­à¤¾à¤·à¤¾", "à¤²à¥‰à¤— à¤†à¤‰à¤Ÿ", "à¤²à¥‰à¤— à¤‡à¤¨",
            "à¤¸à¤¾à¤‡à¤¨ à¤…à¤ª", "à¤‰à¤ªà¤¯à¥‹à¤—à¤•à¤°à¥à¤¤à¤¾ à¤¨à¤¾à¤®", "à¤ªà¤¾à¤¸à¤µà¤°à¥à¤¡", "à¤¸à¤¿à¤•à¥à¤•à¥‡", "à¤•à¥€à¤®à¤¤", "à¤¸à¥à¤µà¤¾à¤®à¤¿à¤¤à¥à¤µ",
            "à¤–à¤°à¥€à¤¦à¥‡à¤‚", "à¤šà¥à¤¨à¥‡à¤‚", "à¤šà¥à¤¨à¤¾ à¤—à¤¯à¤¾", "à¤…à¤ªà¤—à¥à¤°à¥‡à¤¡", "à¤‡à¤‚à¤œà¤¨", "à¤Ÿà¤¾à¤¯à¤°", "à¤°à¤‚à¤—",
            "à¤¸à¥à¤ªà¥‰à¤‡à¤²à¤°", "à¤¨à¤¿à¤¯à¥‰à¤¨", "à¤°à¤¿à¤®à¥à¤¸", "à¤²à¥Œ", "à¤—à¤¤à¤¿", "à¤¤à¥à¤µà¤°à¤£", "à¤¨à¤¿à¤¯à¤‚à¤¤à¥à¤°à¤£", "à¤¤à¥ˆà¤¯à¤¾à¤°",
            "à¤¸à¥à¤•à¥‹à¤°", "à¤…à¤‚à¤•", "à¤«à¤¿à¤° à¤¸à¥‡", "à¤¹à¥‹à¤®", "à¤µà¤¿à¤œà¥à¤žà¤¾à¤ªà¤¨ à¤¦à¥‡à¤–à¥‡à¤‚", "à¤¬à¥‹à¤¨à¤¸ à¤¸à¤¿à¤•à¥à¤•à¥‡ à¤•à¤®à¤¾à¤à¤‚",
            "à¤¸à¤¿à¤•à¥à¤•à¥‡ à¤•à¤®à¤¾à¤à¤‚", "à¤¬à¤¾à¤¹à¤° à¤¨à¤¿à¤•à¤²à¥‡à¤‚", "à¤•à¤¨à¥‡à¤•à¥à¤¶à¤¨ à¤¨à¤¹à¥€à¤‚", "à¤²à¥‹à¤¡ à¤¹à¥‹ à¤°à¤¹à¤¾ à¤¹à¥ˆ",
            "à¤œà¥à¤¡à¤¼à¤¾ à¤¹à¥à¤†", "à¤¦à¥Œà¤¡à¤¼ à¤¶à¥à¤°à¥‚ à¤•à¤°à¥‡à¤‚",
            "à¤²à¥‡à¤¨ à¤¬à¤¦à¤²à¤¨à¥‡ à¤•à¥‡ à¤²à¤¿à¤ à¤¬à¤¾à¤à¤ / à¤®à¤§à¥à¤¯ / à¤¦à¤¾à¤à¤ à¤¦à¤¬à¤¾à¤à¤‚",
            "à¤¦à¥à¤°à¥à¤˜à¤Ÿà¤¨à¤¾!", "à¤ªà¤°à¤¿à¤£à¤¾à¤®"
    , "à¤—à¥‹à¤ªà¤¨à¥€à¤¯à¤¤à¤¾ à¤¨à¥€à¤¤à¤¿", "à¤‰à¤ªà¤¯à¥‹à¤— à¤•à¥€ à¤¶à¤°à¥à¤¤à¥‡à¤‚"
    ,"Premium", "Google"
    };

    static final String[] L_UR = {
            "Ú©Ú¾ÛŒÙ„ÛŒÚº", "Ú¯ÛŒØ±Ø§Ø¬", "Ø§Ø³Ú©ÙˆØ± Ø¨ÙˆØ±Úˆ", "Ø³Ø±ÙˆØ±", "Ø²Ø¨Ø§Ù†", "Ù„Ø§Ú¯ Ø¢Ø¤Ù¹", "Ù„Ø§Ú¯ Ø§Ù†",
            "Ø³Ø§Ø¦Ù† Ø§Ù¾", "ØµØ§Ø±Ù Ù†Ø§Ù…", "Ù¾Ø§Ø³ ÙˆØ±Úˆ", "Ø³Ú©Û’", "Ù‚ÛŒÙ…Øª", "Ù…Ù„Ú©ÛŒØª", "Ø®Ø±ÛŒØ¯ÛŒÚº",
            "Ù…Ù†ØªØ®Ø¨ Ú©Ø±ÛŒÚº", "Ù…Ù†ØªØ®Ø¨", "Ø§Ù¾ Ú¯Ø±ÛŒÚˆ", "Ø§Ù†Ø¬Ù†", "Ù¹Ø§Ø¦Ø±", "Ø±Ù†Ú¯", "Ø³Ù¾ÙˆØ¦Ù„Ø±",
            "Ù†ÛŒÙˆÙ†", "Ø±Ù…Ø²", "Ø´Ø¹Ù„Û", "Ø±ÙØªØ§Ø±", "Ø³Ø±Ø¹Øª", "Ú©Ù†Ù¹Ø±ÙˆÙ„", "ØªÛŒØ§Ø±", "Ø§Ø³Ú©ÙˆØ±",
            "Ù¾ÙˆØ§Ø¦Ù†Ù¹Ø³", "Ø¯ÙˆØ¨Ø§Ø±Û", "ÛÙˆÙ…", "Ø§Ø´ØªÛØ§Ø± Ø¯ÛŒÚ©Ú¾ÛŒÚº", "Ø¨ÙˆÙ†Ø³ Ø³Ú©Û’ Ø­Ø§ØµÙ„ Ú©Ø±ÛŒÚº",
            "Ø³Ú©Û’ Ø­Ø§ØµÙ„ Ú©Ø±ÛŒÚº", "Ø¨Ø§ÛØ± Ù†Ú©Ù„ÛŒÚº", "Ú©ÙˆØ¦ÛŒ Ù†ÛŒÙ¹ ÙˆØ±Ú© Ù†ÛÛŒÚº", "Ù„ÙˆÚˆ ÛÙˆ Ø±ÛØ§ ÛÛ’",
            "Ù…Ù†Ø³Ù„Ú©", "Ø¯ÙˆÚ‘ Ø´Ø±ÙˆØ¹ Ú©Ø±ÛŒÚº",
            "Ù„ÛŒÙ† Ø¨Ø¯Ù„Ù†Û’ Ú©Û’ Ù„ÛŒÛ’ Ø¨Ø§Ø¦ÛŒÚº / Ø¯Ø±Ù…ÛŒØ§Ù† / Ø¯Ø§Ø¦ÛŒÚº Ø¯Ø¨Ø§Ø¦ÛŒÚº",
            "Ø­Ø§Ø¯Ø«Û!", "Ù†ØªÛŒØ¬Û"
    , "Ø±Ø§Ø²Ø¯Ø§Ø±ÛŒ Ú©ÛŒ Ù¾Ø§Ù„ÛŒØ³ÛŒ", "Ø§Ø³ØªØ¹Ù…Ø§Ù„ Ú©ÛŒ Ø´Ø±Ø§Ø¦Ø·"
    ,"Premium", "Google"
    };

    static final String[] L_FA = {
            "Ø¨Ø§Ø²ÛŒ", "Ú¯Ø§Ø±Ø§Ú˜", "Ø±Ø¯Ù‡â€ŒØ¨Ù†Ø¯ÛŒ", "Ø³Ø±ÙˆØ±", "Ø²Ø¨Ø§Ù†", "Ø®Ø±ÙˆØ¬", "ÙˆØ±ÙˆØ¯", "Ø«Ø¨Øªâ€ŒÙ†Ø§Ù…",
            "Ù†Ø§Ù… Ú©Ø§Ø±Ø¨Ø±ÛŒ", "Ø±Ù…Ø² Ø¹Ø¨ÙˆØ±", "Ø³Ú©Ù‡", "Ù‚ÛŒÙ…Øª", "ØªÙ…Ù„Ú©", "Ø®Ø±ÛŒØ¯", "Ø§Ù†ØªØ®Ø§Ø¨",
            "Ø§Ù†ØªØ®Ø§Ø¨ Ø´Ø¯Ù‡", "Ø§Ø±ØªÙ‚Ø§", "Ù…ÙˆØªÙˆØ±", "Ù„Ø§Ø³ØªÛŒÚ©", "Ø±Ù†Ú¯", "Ø§Ø³Ù¾ÙˆÛŒÙ„Ø±", "Ù†Ø¦ÙˆÙ†",
            "Ø±ÛŒÙ†Ú¯", "Ø´Ø¹Ù„Ù‡", "Ø³Ø±Ø¹Øª", "Ø´ØªØ§Ø¨", "Ú©Ù†ØªØ±Ù„", "Ø¢Ù…Ø§Ø¯Ù‡", "Ø§Ù…ØªÛŒØ§Ø²", "Ø§Ù…ØªÛŒØ§Ø²Ù‡Ø§",
            "Ø¯ÙˆØ¨Ø§Ø±Ù‡", "Ø®Ø§Ù†Ù‡", "Ø¯ÛŒØ¯Ù† ØªØ¨Ù„ÛŒØº", "Ú©Ø³Ø¨ Ø³Ú©Ù‡ Ø¬Ø§ÛŒØ²Ù‡", "Ú©Ø³Ø¨ Ø³Ú©Ù‡", "Ø®Ø±ÙˆØ¬",
            "Ø¨Ø¯ÙˆÙ† Ø§ØªØµØ§Ù„", "Ø¯Ø± Ø­Ø§Ù„ Ø¨Ø§Ø±Ú¯Ø°Ø§Ø±ÛŒ", "Ù…ØªØµÙ„", "Ø´Ø±ÙˆØ¹ Ù…Ø³Ø§Ø¨Ù‚Ù‡",
            "Ø¨Ø±Ø§ÛŒ ØªØºÛŒÛŒØ± Ø®Ø· Ú†Ù¾ / ÙˆØ³Ø· / Ø±Ø§Ø³Øª Ø±Ø§ Ù„Ù…Ø³ Ú©Ù†ÛŒØ¯",
            "ØªØµØ§Ø¯Ù!", "Ù†ØªÛŒØ¬Ù‡"
    , "Ø³ÛŒØ§Ø³Øª Ø­ÙØ¸ Ø­Ø±ÛŒÙ… Ø®ØµÙˆØµÛŒ", "Ø´Ø±Ø§ÛŒØ· Ø§Ø³ØªÙØ§Ø¯Ù‡"
    ,"Premium", "Google"
    };

    static final String[] L_ID = {
            "Main", "Garasi", "Papan skor", "Server", "Bahasa", "Keluar", "Masuk",
            "Daftar", "Nama pengguna", "Kata sandi", "Koin", "Harga", "Dimiliki",
            "Beli", "Pilih", "Dipilih", "Naikkan", "Mesin", "Ban", "Warna", "Spoiler",
            "Neon", "Velg", "Api", "Kecepatan", "Akselerasi", "Kontrol", "Siap",
            "Skor", "Poin", "Lagi", "Beranda", "Tonton iklan", "Dapatkan koin bonus",
            "Dapatkan koin", "Keluar", "Tidak ada koneksi", "Memuat", "Terhubung",
            "Mulai balapan", "Ketuk kiri / tengah / kanan untuk pindah lajur",
            "Tabrakan!", "Hasil"
    , "Kebijakan Privasi", "Ketentuan Layanan"
    ,"Premium", "Google"
    };

    static final String[] L_MS = {
            "Main", "Garaj", "Papan mata", "Pelayan", "Bahasa", "Log keluar", "Log masuk",
            "Daftar", "Nama pengguna", "Kata laluan", "Syiling", "Harga", "Dimiliki",
            "Beli", "Pilih", "Dipilih", "Naik taraf", "Enjin", "Tayar", "Warna",
            "Spoiler", "Neon", "Rim", "Nyala", "Kelajuan", "Pecutan", "Kawalan",
            "Sedia", "Skor", "Mata", "Lagi", "Utama", "Tonton iklan",
            "Dapat syiling bonus", "Dapat syiling", "Keluar", "Tiada sambungan",
            "Memuatkan", "Bersambung", "Mulakan perlumbaan",
            "Ketik kiri / tengah / kanan untuk tukar lorong",
            "Pelanggaran!", "Keputusan"
    , "Dasar Privasi", "Terma Perkhidmatan"
    ,"Premium", "Google"
    };

    static final String[] L_TH = {
            "à¹€à¸¥à¹ˆà¸™", "à¹‚à¸£à¸‡à¸£à¸–", "à¸­à¸±à¸™à¸”à¸±à¸š", "à¹€à¸‹à¸´à¸£à¹Œà¸Ÿà¹€à¸§à¸­à¸£à¹Œ", "à¸ à¸²à¸©à¸²", "à¸­à¸­à¸à¸ˆà¸²à¸à¸£à¸°à¸šà¸š", "à¹€à¸‚à¹‰à¸²à¸ªà¸¹à¹ˆà¸£à¸°à¸šà¸š",
            "à¸ªà¸¡à¸±à¸„à¸£", "à¸Šà¸·à¹ˆà¸­à¸œà¸¹à¹‰à¹ƒà¸Šà¹‰", "à¸£à¸«à¸±à¸ªà¸œà¹ˆà¸²à¸™", "à¹€à¸«à¸£à¸µà¸¢à¸", "à¸£à¸²à¸„à¸²", "à¹€à¸›à¹‡à¸™à¹€à¸ˆà¹‰à¸²à¸‚à¸­à¸‡", "à¸‹à¸·à¹‰à¸­",
            "à¹€à¸¥à¸·à¸­à¸", "à¹€à¸¥à¸·à¸­à¸à¹à¸¥à¹‰à¸§", "à¸­à¸±à¸›à¹€à¸à¸£à¸”", "à¹€à¸„à¸£à¸·à¹ˆà¸­à¸‡à¸¢à¸™à¸•à¹Œ", "à¸¢à¸²à¸‡", "à¸ªà¸µ", "à¸ªà¸›à¸­à¸¢à¹€à¸¥à¸­à¸£à¹Œ",
            "à¸™à¸µà¸­à¸­à¸™", "à¸¥à¹‰à¸­", "à¹€à¸›à¸¥à¸§à¹„à¸Ÿ", "à¸„à¸§à¸²à¸¡à¹€à¸£à¹‡à¸§", "à¸­à¸±à¸•à¸£à¸²à¹€à¸£à¹ˆà¸‡", "à¸à¸²à¸£à¸„à¸§à¸šà¸„à¸¸à¸¡", "à¸žà¸£à¹‰à¸­à¸¡",
            "à¸„à¸°à¹à¸™à¸™", "à¸„à¸°à¹à¸™à¸™", "à¸­à¸µà¸à¸„à¸£à¸±à¹‰à¸‡", "à¸«à¸™à¹‰à¸²à¹à¸£à¸", "à¸”à¸¹à¹‚à¸†à¸©à¸“à¸²", "à¸£à¸±à¸šà¹€à¸«à¸£à¸µà¸¢à¸à¹‚à¸šà¸™à¸±à¸ª",
            "à¸£à¸±à¸šà¹€à¸«à¸£à¸µà¸¢à¸", "à¸­à¸­à¸", "à¹„à¸¡à¹ˆà¸¡à¸µà¸à¸²à¸£à¹€à¸Šà¸·à¹ˆà¸­à¸¡à¸•à¹ˆà¸­", "à¸à¸³à¸¥à¸±à¸‡à¹‚à¸«à¸¥à¸”", "à¹€à¸Šà¸·à¹ˆà¸­à¸¡à¸•à¹ˆà¸­à¹à¸¥à¹‰à¸§",
            "à¹€à¸£à¸´à¹ˆà¸¡à¸à¸²à¸£à¹à¸‚à¹ˆà¸‡", "à¹à¸•à¸°à¸‹à¹‰à¸²à¸¢ / à¸à¸¥à¸²à¸‡ / à¸‚à¸§à¸²à¹€à¸žà¸·à¹ˆà¸­à¹€à¸›à¸¥à¸µà¹ˆà¸¢à¸™à¹€à¸¥à¸™",
            "à¸Šà¸™!", "à¸œà¸¥à¸¥à¸±à¸žà¸˜à¹Œ"
    , "à¸™à¹‚à¸¢à¸šà¸²à¸¢à¸„à¸§à¸²à¸¡à¹€à¸›à¹‡à¸™à¸ªà¹ˆà¸§à¸™à¸•à¸±à¸§", "à¸‚à¹‰à¸­à¸à¸³à¸«à¸™à¸”à¸à¸²à¸£à¹ƒà¸Šà¹‰à¸‡à¸²à¸™"
    ,"Premium", "Google"
    };

    static final String[] L_VI = {
            "ChÆ¡i", "Ga ra", "Báº£ng xáº¿p háº¡ng", "MÃ¡y chá»§", "NgÃ´n ngá»¯", "ÄÄƒng xuáº¥t",
            "ÄÄƒng nháº­p", "ÄÄƒng kÃ½", "TÃªn ngÆ°á»i dÃ¹ng", "Máº­t kháº©u", "Xu", "GiÃ¡",
            "ÄÃ£ sá»Ÿ há»¯u", "Mua", "Chá»n", "ÄÃ£ chá»n", "NÃ¢ng cáº¥p", "Äá»™ng cÆ¡", "Lá»‘p",
            "MÃ u", "CÃ¡nh giÃ³", "Neon", "MÃ¢m", "Ngá»n lá»­a", "Tá»‘c Ä‘á»™", "Gia tá»‘c",
            "Äiá»u khiá»ƒn", "Sáºµn sÃ ng", "Äiá»ƒm", "Äiá»ƒm", "Láº§n ná»¯a", "Trang chá»§",
            "Xem quáº£ng cÃ¡o", "Nháº­n xu thÆ°á»Ÿng", "Nháº­n xu", "ThoÃ¡t", "KhÃ´ng cÃ³ káº¿t ná»‘i",
            "Äang táº£i", "ÄÃ£ káº¿t ná»‘i", "Báº¯t Ä‘áº§u Ä‘ua",
            "Cháº¡m trÃ¡i / giá»¯a / pháº£i Ä‘á»ƒ Ä‘á»•i lÃ n",
            "Tai náº¡n!", "Káº¿t quáº£"
    , "ChÃ­nh sÃ¡ch báº£o máº­t", "Äiá»u khoáº£n sá»­ dá»¥ng"
    ,"Premium", "Google"
    };

    static final String[] L_JA = {
            "ãƒ—ãƒ¬ã‚¤", "ã‚¬ãƒ¬ãƒ¼ã‚¸", "ãƒ©ãƒ³ã‚­ãƒ³ã‚°", "ã‚µãƒ¼ãƒãƒ¼", "è¨€èªž", "ãƒ­ã‚°ã‚¢ã‚¦ãƒˆ", "ãƒ­ã‚°ã‚¤ãƒ³",
            "ç™»éŒ²", "ãƒ¦ãƒ¼ã‚¶ãƒ¼å", "ãƒ‘ã‚¹ãƒ¯ãƒ¼ãƒ‰", "ã‚³ã‚¤ãƒ³", "ä¾¡æ ¼", "æ‰€æœ‰", "è³¼å…¥", "é¸æŠž",
            "é¸æŠžæ¸ˆã¿", "ã‚¢ãƒƒãƒ—ã‚°ãƒ¬ãƒ¼ãƒ‰", "ã‚¨ãƒ³ã‚¸ãƒ³", "ã‚¿ã‚¤ãƒ¤", "è‰²", "ã‚¹ãƒã‚¤ãƒ©ãƒ¼", "ãƒã‚ªãƒ³",
            "ãƒ›ã‚¤ãƒ¼ãƒ«", "ç‚Ž", "é€Ÿåº¦", "åŠ é€Ÿ", "æ“ä½œ", "æº–å‚™", "ã‚¹ã‚³ã‚¢", "ãƒã‚¤ãƒ³ãƒˆ",
            "ã‚‚ã†ä¸€åº¦", "ãƒ›ãƒ¼ãƒ ", "åºƒå‘Šã‚’è¦‹ã‚‹", "ãƒœãƒ¼ãƒŠã‚¹ã‚³ã‚¤ãƒ³ç²å¾—", "ã‚³ã‚¤ãƒ³ç²å¾—", "çµ‚äº†",
            "æŽ¥ç¶šãªã—", "èª­ã¿è¾¼ã¿ä¸­", "æŽ¥ç¶šæ¸ˆã¿", "ãƒ¬ãƒ¼ã‚¹é–‹å§‹",
            "å·¦å³ã®ã‚¿ãƒƒãƒ—ã§ãƒ¬ãƒ¼ãƒ³å¤‰æ›´",
            "è¡çª!", "çµæžœ"
    , "ãƒ—ãƒ©ã‚¤ãƒã‚·ãƒ¼ãƒãƒªã‚·ãƒ¼", "åˆ©ç”¨è¦ç´„"
    ,"Premium", "Google"
    };

    static final String[] L_KO = {
            "í”Œë ˆì´", "ì°¨ê³ ", "ìˆœìœ„", "ì„œë²„", "ì–¸ì–´", "ë¡œê·¸ì•„ì›ƒ", "ë¡œê·¸ì¸", "ê°€ìž…",
            "ì‚¬ìš©ìž ì´ë¦„", "ë¹„ë°€ë²ˆí˜¸", "ì½”ì¸", "ê°€ê²©", "ë³´ìœ ", "êµ¬ë§¤", "ì„ íƒ", "ì„ íƒë¨",
            "ì—…ê·¸ë ˆì´ë“œ", "ì—”ì§„", "íƒ€ì´ì–´", "ìƒ‰ìƒ", "ìŠ¤í¬ì¼ëŸ¬", "ë„¤ì˜¨", "íœ ", "ë¶ˆê½ƒ",
            "ì†ë„", "ê°€ì†", "ì¡°ìž‘", "ì¤€ë¹„", "ì ìˆ˜", "í¬ì¸íŠ¸", "ë‹¤ì‹œ", "í™ˆ", "ê´‘ê³  ë³´ê¸°",
            "ë³´ë„ˆìŠ¤ ì½”ì¸ íšë“", "ì½”ì¸ íšë“", "ì¢…ë£Œ", "ì—°ê²° ì—†ìŒ", "ë¡œë”©", "ì—°ê²°ë¨",
            "ë ˆì´ìŠ¤ ì‹œìž‘", "ì¢Œ / ì¤‘ / ìš°ë¥¼ ëˆŒëŸ¬ ì°¨ì„  ë³€ê²½",
            "ì¶©ëŒ!", "ê²°ê³¼"
    , "ê°œì¸ì •ë³´ì²˜ë¦¬ë°©ì¹¨", "ì´ìš©ì•½ê´€"
    ,"Premium", "Google"
    };

    static final String[] L_ZH = {
            "çŽ©", "è½¦åº“", "æŽ’è¡Œæ¦œ", "æœåŠ¡å™¨", "è¯­è¨€", "é€€å‡º", "ç™»å½•", "æ³¨å†Œ", "ç”¨æˆ·å",
            "å¯†ç ", "é‡‘å¸", "ä»·æ ¼", "å·²æ‹¥æœ‰", "è´­ä¹°", "é€‰æ‹©", "å·²é€‰æ‹©", "å‡çº§", "å¼•æ“Ž",
            "è½®èƒŽ", "é¢œè‰²", "å°¾ç¿¼", "éœ“è™¹", "è½®æ¯‚", "ç«ç„°", "é€Ÿåº¦", "åŠ é€Ÿ", "æ“æŽ§",
            "å‡†å¤‡", "å¾—åˆ†", "ç§¯åˆ†", "å†æ¥ä¸€æ¬¡", "ä¸»é¡µ", "çœ‹å¹¿å‘Š", "èŽ·å¾—å¥–åŠ±é‡‘å¸",
            "èµšé‡‘å¸", "é€€å‡º", "æ— è¿žæŽ¥", "åŠ è½½ä¸­", "å·²è¿žæŽ¥", "å¼€å§‹æ¯”èµ›",
            "ç‚¹å‡»å·¦ / ä¸­ / å³åˆ‡æ¢è½¦é“",
            "ç¢°æ’ž!", "ç»“æžœ"
    , "éšç§æ”¿ç­–", "æœåŠ¡æ¡æ¬¾"
    ,"Premium", "Google"
    };

    static final String[] L_SR = {
            "Igraj", "GaraÅ¾a", "Tabela", "Server", "Jezik", "Odjava", "Prijava",
            "Registracija", "KorisniÄko ime", "Lozinka", "NovÄiÄ‡i", "Cena", "VlasniÅ¡tvo",
            "Kupi", "Izaberi", "Izabrano", "Nadogradi", "Motor", "Gume", "Boja",
            "Spoiler", "Neon", "Felne", "Plamen", "Brzina", "Ubrzanje", "Kontrola",
            "Spreman", "Rezultat", "Bodovi", "Opet", "PoÄetna", "Gledaj reklamu",
            "Zaradi bonus novÄiÄ‡e", "Zaradi novÄiÄ‡e", "Izlaz", "Nema veze",
            "UÄitavanje", "Povezano", "Pokreni trku",
            "Dodirni levo / centar / desno za promenu trake",
            "Sudar!", "Rezultat"
    , "Politika privatnosti", "Uslovi koriÅ¡Ä‡enja"
    ,"Premium", "Google"
    };

    static final String[] L_HR = {
            "Igraj", "GaraÅ¾a", "Ljestvica", "Server", "Jezik", "Odjava", "Prijava",
            "Registracija", "KorisniÄko ime", "Lozinka", "NovÄiÄ‡i", "Cijena", "VlasniÅ¡tvo",
            "Kupi", "Odaberi", "Odabrano", "Nadogradi", "Motor", "Gume", "Boja",
            "Spoiler", "Neon", "Felge", "Plamen", "Brzina", "Ubrzanje", "Upravljanje",
            "Spreman", "Rezultat", "Bodovi", "Opet", "PoÄetna", "Gledaj reklamu",
            "Zaradi bonus novÄiÄ‡e", "Zaradi novÄiÄ‡e", "IzaÄ‘i", "Nema veze",
            "UÄitavanje", "Povezano", "ZapoÄni utrku",
            "Dodirni lijevo / po sredini / desno za promjenu trake",
            "Sudar!", "Rezultat"
    , "Politika privatnosti", "Uvjeti koriÅ¡tenja"
    ,"Premium", "Google"
    };

    static final String[] L_SK = {
            "HraÅ¥", "GarÃ¡Å¾", "RebrÃ­Äek", "Server", "Jazyk", "OdhlÃ¡siÅ¥", "PrihlÃ¡siÅ¥",
            "RegistrÃ¡cia", "PouÅ¾Ã­vateÄ¾skÃ© meno", "Heslo", "Mince", "Cena", "VlastnÃ­ctvo",
            "KÃºpiÅ¥", "VybraÅ¥", "VybratÃ©", "VylepÅ¡iÅ¥", "Motor", "Pneumatiky", "Farba",
            "Spoiler", "Neon", "Disky", "PlameÅˆ", "RÃ½chlosÅ¥", "ZrÃ½chlenie", "OvlÃ¡danie",
            "PripravenÃ½", "SkÃ³re", "Body", "Znova", "Domov", "PozrieÅ¥ reklamu",
            "ZÃ­skaj bonusovÃ© mince", "ZÃ­skaj mince", "Koniec", "Å½iadne pripojenie",
            "NaÄÃ­tava", "PripojenÃ©", "SpustiÅ¥ preteky",
            "Klepni vÄ¾avo / na stred / vpravo pre zmenu pruhu",
            "Nehoda!", "VÃ½sledok"
    , "ZÃ¡sady ochrany osobnÃ½ch Ãºdajov", "Podmienky pouÅ¾Ã­vania"
    ,"Premium", "Google"
    };

    static final String[] L_SL = {
            "Igraj", "GaraÅ¾a", "Lestvica", "StreÅ¾nik", "Jezik", "Odjava", "Prijava",
            "Registracija", "UporabniÅ¡ko ime", "Geslo", "Kovanci", "Cena", "LastniÅ¡tvo",
            "Kupi", "Izberi", "Izbrano", "Nadgradi", "Motor", "Pnevmatike", "Barva",
            "Spojler", "Neon", "PlatiÅ¡Äa", "Plamen", "Hitrost", "PospeÅ¡ek", "Nadzor",
            "Pripravljen", "Rezultat", "ToÄke", "Spet", "Domov", "Glej oglas",
            "ZasluÅ¾i bonus kovance", "ZasluÅ¾i kovance", "Izhod", "Ni povezave",
            "Nalaganje", "Povezano", "ZaÄni dirko",
            "Tapni levo / sredino / desno za menjavo pasu",
            "TrÄenje!", "Rezultat"
    , "Politika zasebnosti", "Pogoji uporabe"
    ,"Premium", "Google"
    };

    static final String[] L_LT = {
            "Å½aisti", "GaraÅ¾as", "LentelÄ—", "Serveris", "Kalba", "Atsijungti",
            "Prisijungti", "Registruotis", "Vartotojo vardas", "SlaptaÅ¾odis", "Monetos",
            "Kaina", "NuosavybÄ—", "Pirkti", "Pasirinkti", "Pasirinkta", "Patobulinti",
            "Variklis", "Padangos", "Spalva", "Spoileris", "Neonas", "Ratlankiai",
            "Liepsna", "Greitis", "Ä®sibÄ—gÄ—jimas", "Valdymas", "PasiruoÅ¡Ä™s", "Rezultatas",
            "TaÅ¡kai", "Dar kartÄ…", "Pagrindinis", "Å½iÅ«rÄ—ti reklamÄ…",
            "UÅ¾dirbk premijos monetas", "UÅ¾dirbk monetas", "IÅ¡eiti", "NÄ—ra ryÅ¡io",
            "Ä®keliama", "Prisijungta", "PradÄ—ti lenktynes",
            "BakstelÄ—k kairÄ— / centras / deÅ¡inÄ—, kad pakeistum juostÄ…",
            "Avarija!", "Rezultatas"
    , "Privatumo politika", "Naudojimo sÄ…lygos"
    ,"Premium", "Google"
    };

    static final String[] L_LV = {
            "SpÄ“lÄ“t", "GarÄÅ¾a", "RezultÄti", "Serveris", "Valoda", "Iziet", "Ieiet",
            "ReÄ£istrÄ“ties", "LietotÄjvÄrds", "Parole", "MonÄ“tas", "Cena", "ÄªpaÅ¡umÄ",
            "Pirkt", "IzvÄ“lÄ“ties", "IzvÄ“lÄ“ta", "Uzlabot", "DzinÄ“js", "Riepas", "KrÄsa",
            "Spoileris", "Neons", "Diski", "Liesma", "Ä€trums", "PaÄtrinÄjums", "VadÄ«ba",
            "Gatavs", "RezultÄts", "Punkti", "VÄ“lreiz", "SÄkums", "SkatÄ«ties reklÄmu",
            "IegÅ«sti bonusa monÄ“tas", "IegÅ«sti monÄ“tas", "Iziet", "Nav savienojuma",
            "IelÄdÄ“", "Savienots", "SÄkt sacÄ«ksti",
            "Pieskaries pa kreisi / centrÄ / labajai, lai mainÄ«tu joslu",
            "AvÄrija!", "RezultÄts"
    , "PrivÄtuma politika", "LietoÅ¡anas noteikumi"
    ,"Premium", "Google"
    };

    static final String[] L_ET = {
            "MÃ¤ngi", "GaraaÅ¾", "Edetabel", "Server", "Keel", "Logi vÃ¤lja", "Logi sisse",
            "Registreeri", "Kasutajanimi", "Parool", "MÃ¼ndid", "Hind", "Omamisel",
            "Osta", "Vali", "Valitud", "TÃ¤ienda", "Mootor", "Rehvid", "VÃ¤rv", "Spoiler",
            "Neoon", "Veljed", "Leek", "Kiirus", "Kiirendus", "Juhtimine", "Valmis",
            "Tulemus", "Punktid", "Uuesti", "Kodu", "Vaata reklaami",
            "Teenige boonusmÃ¼nte", "Teenige mÃ¼nte", "VÃ¤lju", "Ãœhendust pole",
            "Laadimine", "Ãœhendatud", "Alusta vÃµistlust",
            "Puuduta vasak / kesk / parem, et vahetada rada",
            "KokkupÃµrge!", "Tulemus"
    , "Privaatsuspoliitika", "Kasutustingimused"
    ,"Premium", "Google"
    };

    static final String[] L_SQ = {
            "Luaj", "Garazh", "Renditja", "Server", "Gjuha", "Dil", "Hyr",
            "Regjistrohu", "Emri", "FjalÃ«kalimi", "Monedha", "Ã‡mimi", "PronÃ«si", "Blej",
            "Zgjidh", "Zgjedhur", "PÃ«rmirÃ«so", "Motori", "Gomat", "Ngjyra", "Spoiler",
            "Neon", "Rrjetat", "FlakÃ«", "ShpejtÃ«sia", "PÃ«rshpejtimi", "Kontrolli",
            "Gati", "Rezultati", "PikÃ«t", "PÃ«rsÃ«ri", "Kryefaqja", "Shiko reklamÃ«",
            "Fito monedha bonus", "Fito monedha", "Dil", "Nuk ka lidhje",
            "Po ngarkohet", "I lidhur", "Fillo garÃ«n",
            "Prek majtas / qendÃ«r / djathtas pÃ«r tÃ« ndryshuar korsinÃ«",
            "PÃ«rplasje!", "Rezultati"
    , "Politika e privatÃ«sisÃ«", "Kushtet e pÃ«rdorimit"
    ,"Premium", "Google"
    };

    static final String[] L_MK = {
            "Ð˜Ð³Ñ€Ð°Ñ˜", "Ð“Ð°Ñ€Ð°Ð¶Ð°", "Ð¢Ð°Ð±ÐµÐ»Ð°", "Ð¡ÐµÑ€Ð²ÐµÑ€", "ÐˆÐ°Ð·Ð¸Ðº", "ÐžÐ´Ñ˜Ð°Ð²Ð°", "ÐŸÑ€Ð¸Ñ˜Ð°Ð²Ð°",
            "Ð ÐµÐ³Ð¸ÑÑ‚Ñ€Ð°Ñ†Ð¸Ñ˜Ð°", "ÐšÐ¾Ñ€Ð¸ÑÐ½Ð¸Ñ‡ÐºÐ¾ Ð¸Ð¼Ðµ", "Ð›Ð¾Ð·Ð¸Ð½ÐºÐ°", "ÐœÐ¾Ð½ÐµÑ‚Ð¸", "Ð¦ÐµÐ½Ð°", "Ð¡Ð¾Ð¿ÑÑ‚Ð²ÐµÐ½Ð¾ÑÑ‚",
            "ÐšÑƒÐ¿Ð¸", "Ð˜Ð·Ð±ÐµÑ€Ð¸", "Ð˜Ð·Ð±Ñ€Ð°Ð½Ð¾", "ÐÐ°Ð´Ð³Ñ€Ð°Ð´Ð¸", "ÐœÐ¾Ñ‚Ð¾Ñ€", "Ð“ÑƒÐ¼Ð¸", "Ð‘Ð¾Ñ˜Ð°", "Ð¡Ð¿Ð¾Ñ˜Ð»ÐµÑ€",
            "ÐÐµÐ¾Ð½", "Ð¢Ñ€ÐºÐ°Ð»Ð°", "ÐŸÐ»Ð°Ð¼ÐµÐ½", "Ð‘Ñ€Ð·Ð¸Ð½Ð°", "Ð—Ð°Ð±Ñ€Ð·ÑƒÐ²Ð°ÑšÐµ", "ÐšÐ¾Ð½Ñ‚Ñ€Ð¾Ð»Ð°", "ÐŸÐ¾Ð´Ð³Ð¾Ñ‚Ð²ÐµÐ½",
            "Ð ÐµÐ·ÑƒÐ»Ñ‚Ð°Ñ‚", "ÐŸÐ¾ÐµÐ½Ð¸", "ÐŸÐ¾Ð²Ñ‚Ð¾Ñ€Ð½Ð¾", "ÐŸÐ¾Ñ‡ÐµÑ‚Ð½Ð°", "Ð“Ð»ÐµÐ´Ð°Ñ˜ Ñ€ÐµÐºÐ»Ð°Ð¼Ð°",
            "Ð—Ð°Ñ€Ð°Ð±Ð¾Ñ‚Ð¸ Ð±Ð¾Ð½ÑƒÑ Ð¼Ð¾Ð½ÐµÑ‚Ð¸", "Ð—Ð°Ñ€Ð°Ð±Ð¾Ñ‚Ð¸ Ð¼Ð¾Ð½ÐµÑ‚Ð¸", "Ð˜Ð·Ð»ÐµÐ·", "ÐÐµÐ¼Ð° Ð²Ñ€ÑÐºÐ°",
            "Ð’Ñ‡Ð¸Ñ‚ÑƒÐ²Ð°ÑšÐµ", "ÐŸÐ¾Ð²Ñ€Ð·Ð°Ð½Ð¾", "Ð—Ð°Ð¿Ð¾Ñ‡Ð½Ð¸ Ñ‚Ñ€ÐºÐ°",
            "Ð”Ð¾Ð¿Ñ€Ðµ Ð»ÐµÐ²Ð¾ / Ñ†ÐµÐ½Ñ‚Ð°Ñ€ / Ð´ÐµÑÐ½Ð¾ Ð·Ð° Ð¿Ñ€Ð¾Ð¼ÐµÐ½Ð° Ð½Ð° Ð»ÐµÐ½Ñ‚Ð°",
            "Ð¡ÑƒÐ´Ð¸Ñ€!", "Ð ÐµÐ·ÑƒÐ»Ñ‚Ð°Ñ‚"
    , "ÐŸÐ¾Ð»Ð¸Ñ‚Ð¸ÐºÐ° Ð·Ð° Ð¿Ñ€Ð¸Ð²Ð°Ñ‚Ð½Ð¾ÑÑ‚", "Ð£ÑÐ»Ð¾Ð²Ð¸ Ð·Ð° ÐºÐ¾Ñ€Ð¸ÑÑ‚ÐµÑšÐµ"
    ,"Premium", "Google"
    };

    static final String[] L_KA = {
            "áƒ—áƒáƒ›áƒáƒ¨áƒ˜", "áƒáƒ•áƒ¢áƒáƒ¤áƒáƒ áƒ”áƒ®áƒ˜", "áƒšáƒ˜áƒ“áƒ”áƒ áƒ‘áƒáƒ áƒ“áƒ˜", "áƒ¡áƒ”áƒ áƒ•áƒ”áƒ áƒ˜", "áƒ”áƒœáƒ", "áƒ’áƒáƒ¡áƒ•áƒšáƒ",
            "áƒ¨áƒ”áƒ¡áƒ•áƒšáƒ", "áƒ áƒ”áƒ’áƒ˜áƒ¡áƒ¢áƒ áƒáƒªáƒ˜áƒ", "áƒ›áƒáƒ›áƒ®áƒ›áƒáƒ áƒ”áƒ‘áƒ”áƒšáƒ˜", "áƒžáƒáƒ áƒáƒšáƒ˜", "áƒ›áƒáƒœáƒ”áƒ¢áƒ”áƒ‘áƒ˜", "áƒ¤áƒáƒ¡áƒ˜",
            "áƒ¡áƒáƒ™áƒ£áƒ—áƒ áƒ”áƒ‘áƒ", "áƒ§áƒ˜áƒ“áƒ•áƒ", "áƒáƒ áƒ©áƒ”áƒ•áƒ", "áƒáƒ áƒ©áƒ”áƒ£áƒšáƒ˜", "áƒ’áƒáƒ£áƒ›áƒ¯áƒáƒ‘áƒ”áƒ¡áƒ”áƒ‘áƒ", "áƒ«áƒ áƒáƒ•áƒ",
            "áƒ¡áƒáƒ‘áƒ£áƒ áƒáƒ•áƒ”áƒ‘áƒ˜", "áƒ¤áƒ”áƒ áƒ˜", "áƒ¡áƒžáƒáƒ˜áƒšáƒ”áƒ áƒ˜", "áƒœáƒ”áƒáƒœáƒ˜", "áƒ“áƒ˜áƒ¡áƒ™áƒ”áƒ‘áƒ˜", "áƒáƒšáƒ˜",
            "áƒ¡áƒ˜áƒ©áƒ¥áƒáƒ áƒ”", "áƒáƒ©áƒ¥áƒáƒ áƒ”áƒ‘áƒ", "áƒ™áƒáƒœáƒ¢áƒ áƒáƒšáƒ˜", "áƒ›áƒ–áƒáƒ“", "áƒ¥áƒ£áƒšáƒ", "áƒ¥áƒ£áƒšáƒ”áƒ‘áƒ˜", "áƒ˜áƒ¡áƒ”áƒ•",
            "áƒ›áƒ—áƒáƒ•áƒáƒ áƒ˜", "áƒ áƒ”áƒ™áƒšáƒáƒ›áƒ˜áƒ¡ áƒ§áƒ£áƒ áƒ”áƒ‘áƒ", "áƒ›áƒ˜áƒ˜áƒ¦áƒ” áƒ‘áƒáƒœáƒ£áƒ¡ áƒ›áƒáƒœáƒ”áƒ¢áƒ”áƒ‘áƒ˜",
            "áƒ›áƒ˜áƒ˜áƒ¦áƒ” áƒ›áƒáƒœáƒ”áƒ¢áƒ”áƒ‘áƒ˜", "áƒ’áƒáƒ›áƒáƒ¡áƒ•áƒšáƒ", "áƒ™áƒáƒ•áƒ¨áƒ˜áƒ áƒ˜ áƒáƒ  áƒáƒ áƒ˜áƒ¡", "áƒ©áƒáƒ¢áƒ•áƒ˜áƒ áƒ—áƒ•áƒ",
            "áƒ“áƒáƒ™áƒáƒ•áƒ¨áƒ˜áƒ áƒ”áƒ‘áƒ£áƒšáƒ˜", "áƒ áƒ‘áƒáƒšáƒ˜áƒ¡ áƒ“áƒáƒ¬áƒ§áƒ”áƒ‘áƒ",
            "áƒ¨áƒ”áƒ”áƒ®áƒ” áƒ›áƒáƒ áƒªáƒ®áƒœáƒ˜áƒ• / áƒ¨áƒ£áƒáƒ¨áƒ˜ / áƒ›áƒáƒ áƒ¯áƒ•áƒœáƒ˜áƒ• áƒ–áƒáƒšáƒ˜áƒ¡ áƒ¨áƒ”áƒ¡áƒáƒªáƒ•áƒšáƒ”áƒšáƒáƒ“",
            "áƒáƒ•áƒáƒ áƒ˜áƒ!", "áƒ¨áƒ”áƒ“áƒ”áƒ’áƒ˜"
    , "áƒ™áƒáƒœáƒ¤áƒ˜áƒ“áƒ”áƒœáƒªáƒ˜áƒáƒšáƒ£áƒ áƒáƒ‘áƒ˜áƒ¡ áƒžáƒáƒšáƒ˜áƒ¢áƒ˜áƒ™áƒ", "áƒ›áƒáƒ›áƒ¡áƒáƒ®áƒ£áƒ áƒ”áƒ‘áƒ˜áƒ¡ áƒžáƒ˜áƒ áƒáƒ‘áƒ”áƒ‘áƒ˜"
    ,"Premium", "Google"
    };

    static final String[] L_HY = {
            "Ô½Õ¡Õ²Õ¡Õ¬", "Ô±Õ¾Õ¿Õ¸Õ¿Õ¶Õ¡Õ¯", "Õ„Ö€ÖÕ¡Õ·Õ¡Ö€Õ¡ÕµÕ«Õ¶", "ÕÕ¥Ö€Õ¾Õ¥Ö€", "Ô¼Õ¥Õ¦Õ¸Ö‚", "Ô´Õ¸Ö‚Ö€Õ½ Õ£Õ¡Õ¬",
            "Õ„Õ¸Ö‚Õ¿Ö„", "Ô³Ö€Õ¡Õ¶ÖÕ¸Ö‚Õ´", "Õ•Õ£Õ¿Õ¡Õ¶Õ¸Ö‚Õ¶", "Ô³Õ¡Õ²Õ¿Õ¶Õ¡Õ¢Õ¡Õ¼", "Õ„Õ¥Õ¿Õ¡Õ²Õ¡Õ¤Ö€Õ¡Õ´Õ¶Õ¥Ö€", "Ô³Õ«Õ¶Õ¨",
            "ÕÕ¥ÖƒÕ¡Õ¯Õ¡Õ¶Õ¸Ö‚Õ©ÕµÕ¸Ö‚Õ¶", "Ô³Õ¶Õ¥Õ¬", "Ô¸Õ¶Õ¿Ö€Õ¥Õ¬", "Ô¸Õ¶Õ¿Ö€Õ¾Õ¡Õ®", "Ô²Õ¡Ö€Õ¥Õ¬Õ¡Õ¾Õ¥Õ¬", "Õ‡Õ¡Ö€ÕªÕ«Õ¹",
            "Ô±Õ¶Õ¾Õ¡Õ¤Õ¸Õ²Õ¥Ö€", "Ô³Õ¸Ö‚ÕµÕ¶", "ÕÕºÕ¸ÕµÕ¬Õ¥Ö€", "Õ†Õ¥Õ¸Õ¶", "ÕÕ¯Õ¡Õ¾Õ¡Õ¼Õ¡Õ¯Õ¶Õ¥Ö€", "Ô²Õ¸Ö",
            "Ô±Ö€Õ¡Õ£Õ¸Ö‚Õ©ÕµÕ¸Ö‚Õ¶", "Ô±Ö€Õ¡Õ£Õ¡ÖÕ¸Ö‚Õ´", "Ô¿Õ¡Õ¼Õ¡Õ¾Õ¡Ö€Õ¸Ö‚Õ´", "ÕŠÕ¡Õ¿Ö€Õ¡Õ½Õ¿", "Õ„Õ«Õ¡Õ¾Õ¸Ö€",
            "Õ„Õ«Õ¡Õ¾Õ¸Ö€Õ¶Õ¥Ö€", "Ô¿Ö€Õ¯Õ«Õ¶", "Ô³Õ¬Õ­Õ¡Õ¾Õ¸Ö€", "Ô´Õ«Õ¿Õ¥Õ¬ Õ£Õ¸Õ¾Õ¡Õ¦Õ¤",
            "ÕÕ¿Õ¡ÖÕ«Ö€ Õ¢Õ¸Õ¶Õ¸Ö‚Õ½ Õ´Õ¥Õ¿Õ¡Õ²Õ¡Õ¤Ö€Õ¡Õ´Õ¶Õ¥Ö€", "ÕŽÕ¡Õ½Õ¿Õ¡Õ¯Õ«Ö€ Õ´Õ¥Õ¿Õ¡Õ²Õ¡Õ¤Ö€Õ¡Õ´Õ¶Õ¥Ö€", "ÔµÕ¬Ö„",
            "Ô¿Õ¡Õº Õ¹Õ¯Õ¡", "Ô²Õ¥Õ¼Õ¶Õ¸Ö‚Õ´", "Õ„Õ«Õ¡ÖÕ¾Õ¡Õ®", "ÕÕ¯Õ½Õ¥Õ¬ Õ´Ö€ÖÕ¡Õ¾Õ¡Õ¦Ö„Õ¨",
            "Õ€ÕºÕ¥Ö„ Õ±Õ¡Õ­ / Õ¯Õ¥Õ¶Õ¿Ö€Õ¸Õ¶ / Õ¡Õ»Õ Õ£Õ¸Õ¿Õ«Õ¶ ÖƒÕ¸Õ­Õ¥Õ¬Õ¸Ö‚ Õ°Õ¡Õ´Õ¡Ö€",
            "ÕŽÕ©Õ¡Ö€!", "Ô±Ö€Õ¤ÕµÕ¸Ö‚Õ¶Ö„"
    , "Ô³Õ¡Õ²Õ¿Õ¶Õ«Õ¸Ö‚Õ©ÕµÕ¡Õ¶ Ö„Õ¡Õ²Õ¡Ö„Õ¡Õ¯Õ¡Õ¶Õ¸Ö‚Õ©ÕµÕ¸Ö‚Õ¶", "Õ•Õ£Õ¿Õ¡Õ£Õ¸Ö€Õ®Õ´Õ¡Õ¶ ÕºÕ¡ÕµÕ´Õ¡Õ¶Õ¶Õ¥Ö€"
    ,"Premium", "Google"
    };

    static final String[] L_AZ = {
            "Oyna", "Qaraj", "Reytinq", "Server", "Dil", "Ã‡Ä±xÄ±ÅŸ", "Daxil ol",
            "Qeydiyyat", "Ä°stifadÉ™Ã§i adÄ±", "ÅžifrÉ™", "SikkÉ™lÉ™r", "QiymÉ™t", "MÃ¼lkiyyÉ™t",
            "Al", "SeÃ§", "SeÃ§ilib", "TÉ™kmillÉ™ÅŸdir", "MÃ¼hÉ™rrik", "TÉ™kÉ™rlÉ™r", "RÉ™ng",
            "Spoyler", "Neon", "DisklÉ™r", "Alov", "SÃ¼rÉ™t", "SÃ¼rÉ™tlÉ™nmÉ™", "Ä°darÉ™etmÉ™",
            "HazÄ±r", "Xal", "Xallar", "YenidÉ™n", "Ev", "Reklam izlÉ™",
            "Bonus sikkÉ™ qazan", "SikkÉ™ qazan", "Ã‡Ä±xÄ±ÅŸ", "ÆlaqÉ™ yoxdur", "YÃ¼klÉ™nir",
            "QoÅŸuldu", "YarÄ±ÅŸÄ± baÅŸlat",
            "ZolaÄŸÄ± dÉ™yiÅŸmÉ™k Ã¼Ã§Ã¼n sola / ortada / saÄŸa toxun",
            "ToqquÅŸma!", "NÉ™ticÉ™"
    , "MÉ™xfilik siyasÉ™ti", "Ä°stifadÉ™ ÅŸÉ™rtlÉ™ri"
    ,"Premium", "Google"
    };

    static final String[] L_UZ = {
            "O'ynash", "Garaj", "Reyting", "Server", "Til", "Chiqish", "Kirish",
            "Ro'yxatdan o'tish", "Foydalanuvchi nomi", "Parol", "Tangalar", "Narx",
            "Mulk", "Sotib olish", "Tanlash", "Tanlangan", "Yaxshilash", "Dvigatel",
            "Shinalar", "Rang", "Spoyler", "Neon", "Disklar", "Olov", "Tezlik",
            "Tezlanish", "Boshqaruv", "Tayyor", "Ball", "Ballar", "Yana", "Bosh sahifa",
            "Reklama ko'rish", "Bonus tangalar olish", "Tanga olish", "Chiqish",
            "Aloqa yo'q", "Yuklanmoqda", "Ulangan", "Musobaqani boshlash",
            "Yo'lakni almashtirish uchun chap / o'rta / o'ngga bosing",
            "To'qnashuv!", "Natija"
    , "Maxfiylik siyosati", "Foydalanish shartlari"
    ,"Premium", "Google"
    };

    static final String[] L_KK = {
            "ÐžÐ¹Ð½Ð°Ñƒ", "Ð“Ð°Ñ€Ð°Ð¶", "ÐšÐµÑÑ‚Ðµ", "Ð¡ÐµÑ€Ð²ÐµÑ€", "Ð¢Ñ–Ð»", "Ð¨Ñ‹Ò“Ñƒ", "ÐšÑ–Ñ€Ñƒ", "Ð¢Ñ–Ñ€ÐºÐµÐ»Ñƒ",
            "ÐŸÐ°Ð¹Ð´Ð°Ð»Ð°Ð½ÑƒÑˆÑ‹ Ð°Ñ‚Ñ‹", "ÒšÒ±Ð¿Ð¸ÑÑÓ©Ð·", "Ð¢Ð¸Ñ‹Ð½Ð´Ð°Ñ€", "Ð‘Ð°Ò“Ð°", "ÐœÐµÐ½ÑˆÑ–Ðº", "Ð¡Ð°Ñ‚Ñ‹Ð¿ Ð°Ð»Ñƒ",
            "Ð¢Ð°Ò£Ð´Ð°Ñƒ", "Ð¢Ð°Ò£Ð´Ð°Ð»Ò“Ð°Ð½", "Ð–Ð°Ò£Ð°Ñ€Ñ‚Ñƒ", "ÒšÐ¾Ð·Ò“Ð°Ð»Ñ‚Ò›Ñ‹Ñˆ", "Ð”Ó©Ò£Ð³ÐµÐ»ÐµÐºÑ‚ÐµÑ€", "Ð¢Ò¯Ñ",
            "Ð¡Ð¿Ð¾Ð¹Ð»ÐµÑ€", "ÐÐµÐ¾Ð½", "Ð”Ð¸ÑÐºÑ–Ð»ÐµÑ€", "Ð–Ð°Ð»Ñ‹Ð½", "Ð–Ñ‹Ð»Ð´Ð°Ð¼Ð´Ñ‹Ò›", "Ò®Ð´ÐµÑƒ", "Ð‘Ð°ÑÒ›Ð°Ñ€Ñƒ",
            "Ð”Ð°Ð¹Ñ‹Ð½", "ÐÓ™Ñ‚Ð¸Ð¶Ðµ", "Ò°Ð¿Ð°Ð¹Ð»Ð°Ñ€", "ÒšÐ°Ð¹Ñ‚Ð°Ð´Ð°Ð½", "Ð‘Ð°ÑÑ‚Ñ‹ Ð±ÐµÑ‚", "Ð–Ð°Ñ€Ð½Ð°Ð¼Ð° ÐºÓ©Ñ€Ñƒ",
            "Ð‘Ð¾Ð½ÑƒÑ Ñ‚Ð¸Ñ‹Ð½ Ñ‚Ð°Ð±Ñƒ", "Ð¢Ð¸Ñ‹Ð½ Ñ‚Ð°Ð±Ñƒ", "Ð¨Ñ‹Ò“Ñƒ", "Ð‘Ð°Ð¹Ð»Ð°Ð½Ñ‹Ñ Ð¶Ð¾Ò›", "Ð–Ò¯ÐºÑ‚ÐµÐ»ÑƒÐ´Ðµ",
            "ÒšÐ¾ÑÑ‹Ð»Ð´Ñ‹", "Ð–Ð°Ñ€Ñ‹ÑÑ‚Ñ‹ Ð±Ð°ÑÑ‚Ð°Ñƒ",
            "Ð–Ð¾Ð»Ð°Ò›Ñ‚Ñ‹ Ð°ÑƒÑ‹ÑÑ‚Ñ‹Ñ€Ñƒ Ò¯ÑˆÑ–Ð½ ÑÐ¾Ð» / Ð¾Ñ€Ñ‚Ð° / Ð¾Ò£ Ð¶Ð°Ò›Ñ‚Ñ‹ Ð±Ð°ÑÑ‹Ò£Ñ‹Ð·",
            "Ð¡Ð¾Ò›Ñ‚Ñ‹Ò“Ñ‹Ñ!", "ÐÓ™Ñ‚Ð¸Ð¶Ðµ"
    , "ÒšÒ±Ð¿Ð¸ÑÐ»Ñ‹Ð»Ñ‹Ò› ÑÐ°ÑÑÐ°Ñ‚Ñ‹", "ÐŸÐ°Ð¹Ð´Ð°Ð»Ð°Ð½Ñƒ ÑˆÐ°Ñ€Ñ‚Ñ‚Ð°Ñ€Ñ‹"
    ,"Premium", "Google"
    };

    static final String[] L_BN = {
            "à¦–à§‡à¦²à§à¦¨", "à¦—à§à¦¯à¦¾à¦°à§‡à¦œ", "à¦²à¦¿à¦¡à¦¾à¦°à¦¬à§‹à¦°à§à¦¡", "à¦¸à¦¾à¦°à§à¦­à¦¾à¦°", "à¦­à¦¾à¦·à¦¾", "à¦²à¦— à¦†à¦‰à¦Ÿ", "à¦²à¦— à¦‡à¦¨",
            "à¦¨à¦¿à¦¬à¦¨à§à¦§à¦¨", "à¦¬à§à¦¯à¦¬à¦¹à¦¾à¦°à¦•à¦¾à¦°à§€à¦° à¦¨à¦¾à¦®", "à¦ªà¦¾à¦¸à¦“à¦¯à¦¼à¦¾à¦°à§à¦¡", "à¦•à¦¯à¦¼à§‡à¦¨", "à¦¦à¦¾à¦®", "à¦®à¦¾à¦²à¦¿à¦•à¦¾à¦¨à¦¾à¦§à§€à¦¨",
            "à¦•à§‡à¦¨à¦¾", "à¦¨à¦¿à¦°à§à¦¬à¦¾à¦šà¦¨", "à¦¨à¦¿à¦°à§à¦¬à¦¾à¦šà¦¿à¦¤", "à¦†à¦ªà¦—à§à¦°à§‡à¦¡", "à¦‡à¦žà§à¦œà¦¿à¦¨", "à¦Ÿà¦¾à¦¯à¦¼à¦¾à¦°", "à¦°à¦™",
            "à¦¸à§à¦ªà¦¯à¦¼à¦²à¦¾à¦°", "à¦¨à¦¿à¦¯à¦¼à¦¨", "à¦°à¦¿à¦®", "à¦¶à¦¿à¦–à¦¾", "à¦—à¦¤à¦¿", "à¦¤à§à¦¬à¦°à¦£", "à¦¨à¦¿à¦¯à¦¼à¦¨à§à¦¤à§à¦°à¦£", "à¦ªà§à¦°à¦¸à§à¦¤à§à¦¤",
            "à¦¸à§à¦•à§‹à¦°", "à¦ªà¦¯à¦¼à§‡à¦¨à§à¦Ÿ", "à¦†à¦¬à¦¾à¦°", "à¦¹à§‹à¦®", "à¦¬à¦¿à¦œà§à¦žà¦¾à¦ªà¦¨ à¦¦à§‡à¦–à§à¦¨", "à¦¬à§‹à¦¨à¦¾à¦¸ à¦•à¦¯à¦¼à§‡à¦¨ à¦…à¦°à§à¦œà¦¨ à¦•à¦°à§à¦¨",
            "à¦•à¦¯à¦¼à§‡à¦¨ à¦…à¦°à§à¦œà¦¨ à¦•à¦°à§à¦¨", "à¦ªà§à¦°à¦¸à§à¦¥à¦¾à¦¨", "à¦•à§‹à¦¨à§‹ à¦¸à¦‚à¦¯à§‹à¦— à¦¨à§‡à¦‡", "à¦²à§‹à¦¡ à¦¹à¦šà§à¦›à§‡", "à¦¸à¦‚à¦¯à§à¦•à§à¦¤",
            "à¦°à§‡à¦¸ à¦¶à§à¦°à§ à¦•à¦°à§à¦¨", "à¦²à§‡à¦¨ à¦¬à¦¦à¦²à¦¾à¦¤à§‡ à¦¬à¦¾à¦® / à¦®à¦¾à¦ / à¦¡à¦¾à¦¨ à¦Ÿà¦¿à¦ªà§à¦¨",
            "à¦¦à§à¦°à§à¦˜à¦Ÿà¦¨à¦¾!", "à¦«à¦²à¦¾à¦«à¦²"
    , "à¦—à§‹à¦ªà¦¨à§€à¦¯à¦¼à¦¤à¦¾ à¦¨à§€à¦¤à¦¿", "à¦¬à§à¦¯à¦¬à¦¹à¦¾à¦°à§‡à¦° à¦¶à¦°à§à¦¤à¦¾à¦¬à¦²à§€"
    ,"Premium", "Google"
    };

    static final String[] L_TA = {
            "à®µà®¿à®³à¯ˆà®¯à®¾à®Ÿà¯", "à®•à®¾à®°à¯‡à®œà¯", "à®¤à®°à®µà®°à®¿à®šà¯ˆ", "à®šà®°à¯à®µà®°à¯", "à®®à¯Šà®´à®¿", "à®µà¯†à®³à®¿à®¯à¯‡à®±à¯", "à®‰à®³à¯à®¨à¯à®´à¯ˆ",
            "à®ªà®¤à®¿à®µà¯", "à®ªà®¯à®©à®°à¯ à®ªà¯†à®¯à®°à¯", "à®•à®Ÿà®µà¯à®šà¯à®šà¯Šà®²à¯", "à®¨à®¾à®£à®¯à®™à¯à®•à®³à¯", "à®µà®¿à®²à¯ˆ", "à®‰à®°à®¿à®®à¯ˆ",
            "à®µà®¾à®™à¯à®•à¯", "à®¤à¯‡à®°à¯à®µà¯", "à®¤à¯‡à®°à¯à®¨à¯à®¤à¯†à®Ÿà¯à®•à¯à®•à®ªà¯à®ªà®Ÿà¯à®Ÿà®¤à¯", "à®®à¯‡à®®à¯à®ªà®Ÿà¯à®¤à¯à®¤à¯", "à®‡à®¯à®¨à¯à®¤à®¿à®°à®®à¯",
            "à®Ÿà®¯à®°à¯à®•à®³à¯", "à®¨à®¿à®±à®®à¯", "à®¸à¯à®ªà®¾à®¯à¯à®²à®°à¯", "à®¨à®¿à®¯à®¾à®©à¯", "à®šà®•à¯à®•à®°à®™à¯à®•à®³à¯", "à®¤à¯€", "à®µà¯‡à®•à®®à¯",
            "à®®à¯à®Ÿà¯à®•à¯à®•à®®à¯", "à®•à®Ÿà¯à®Ÿà¯à®ªà¯à®ªà®¾à®Ÿà¯", "à®¤à®¯à®¾à®°à¯", "à®®à®¤à®¿à®ªà¯à®ªà¯†à®£à¯", "à®ªà¯à®³à¯à®³à®¿à®•à®³à¯", "à®®à¯€à®£à¯à®Ÿà¯à®®à¯",
            "à®®à¯à®•à®ªà¯à®ªà¯", "à®µà®¿à®³à®®à¯à®ªà®°à®®à¯ à®ªà®¾à®°à¯à®•à¯à®•", "à®ªà¯‹à®©à®¸à¯ à®¨à®¾à®£à®¯à®™à¯à®•à®³à¯ à®šà®®à¯à®ªà®¾à®¤à®¿",
            "à®¨à®¾à®£à®¯à®™à¯à®•à®³à¯ à®šà®®à¯à®ªà®¾à®¤à®¿", "à®µà¯†à®³à®¿à®¯à¯‡à®±à¯", "à®‡à®£à¯ˆà®ªà¯à®ªà¯ à®‡à®²à¯à®²à¯ˆ", "à®à®±à¯à®±à¯à®•à®¿à®±à®¤à¯",
            "à®‡à®£à¯ˆà®•à¯à®•à®ªà¯à®ªà®Ÿà¯à®Ÿà®¤à¯", "à®ªà®¨à¯à®¤à®¯à®¤à¯à®¤à¯ˆà®¤à¯ à®¤à¯Šà®Ÿà®™à¯à®•à¯",
            "à®ªà®¾à®¤à¯ˆ à®®à®¾à®±à¯à®± à®‡à®Ÿà®¤à¯ / à®¨à®Ÿà¯ / à®µà®²à®¤à¯ à®¤à®Ÿà¯à®Ÿà®µà¯à®®à¯",
            "à®µà®¿à®ªà®¤à¯à®¤à¯!", "à®®à¯à®Ÿà®¿à®µà¯"
    , "à®¤à®©à®¿à®¯à¯à®°à®¿à®®à¯ˆà®•à¯ à®•à¯Šà®³à¯à®•à¯ˆ", "à®ªà®¯à®©à¯à®ªà®¾à®Ÿà¯à®Ÿà¯ à®µà®¿à®¤à®¿à®®à¯à®±à¯ˆà®•à®³à¯"
    ,"Premium", "Google"
    };

    static final String[] L_TL = {
            "Maglaro", "Garahe", "Listahan", "Server", "Wika", "Logout", "Mag-login",
            "Mag-sign up", "Username", "Password", "Barya", "Presyo", "Pag-aari",
            "Bumili", "Pumili", "Napili", "I-upgrade", "Makina", "Gulong", "Kulay",
            "Spoiler", "Neon", "Manibela", "Apoy", "Bilis", "Pagpabilis", "Kontrol",
            "Handa", "Marka", "Puntos", "Muli", "Home", "Manood ng ad",
            "Kumita ng bonus barya", "Kumita ng barya", "Lumabas", "Walang koneksyon",
            "Naglo-load", "Kumonekta", "Simulan ang karera",
            "I-tap kaliwa / gitna / kanan para magpalit ng lane",
            "Bangga!", "Resulta"
    , "Patakaran sa privacy", "Mga Tuntunin ng Paggamit"
    ,"Premium", "Google"
    };

    static final String[] L_SW = {
            "Cheza", "Gereji", "Orodha", "Seva", "Lugha", "Ondoka", "Ingia", "Jisajili",
            "Jina la mtumiaji", "Nywila", "Sarafu", "Bei", "Umiliki", "Nunua", "Chagua",
            "Imechaguliwa", "Boresha", "Injini", "Matairi", "Rangi", "Spoiler", "Neoni",
            "Magurudumu", "Mwali", "Kasi", "Kuongeza kasi", "Udhibiti", "Tayari",
            "Alama", "Pointi", "Tena", "Nyumbani", "Tazama tangazo",
            "Pata sarafu ya bonasi", "Pata sarafu", "Toka", "Hakuna muunganisho",
            "Inapakia", "Imeunganishwa", "Anza mbio",
            "Gusa kushoto / kati / kulia kubadilisha njia",
            "Ajali!", "Matokeo"
    , "Sera ya faragha", "Masharti ya Matumizi"
    ,"Premium", "Google"
    };

    private static final String[][] TBL = {L_EN, L_AR, L_FR, L_DE, L_ES, L_PT, L_IT,
            L_RU, L_TR, L_NL, L_PL, L_SV, L_DA, L_NB, L_FI, L_EL, L_CS, L_HU, L_RO,
            L_BG, L_UK, L_HE, L_HI, L_UR, L_FA, L_ID, L_MS, L_TH, L_VI, L_JA, L_KO,
            L_ZH, L_SR, L_HR, L_SK, L_SL, L_LT, L_LV, L_ET, L_SQ, L_MK, L_KA, L_HY,
            L_AZ, L_UZ, L_KK, L_BN, L_TA, L_TL, L_SW};

    private static final Map<String, Integer> IDX = new HashMap<String, Integer>();

    static {
        for (int i = 0; i < TBL.length; i++) IDX.put(CODES[i], i);
    }

    private L10n() {
    }
}
