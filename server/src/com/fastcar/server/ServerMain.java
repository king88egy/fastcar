package com.fastcar.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import javax.net.ssl.SSLSocketFactory;

public class ServerMain {

    private static final int TOP_SCORES = 10;
    private static final String VODAFONE_CASH = "01040814547";
    private static final String ADMIN_PASS = "fastcar123";
    private static final long[][] PACKAGES = {
            {50, 5},
            {120, 10},
            {300, 20},
            {600, 35},
            {1500, 50}
    };
    private static final int PREMIUM_PRICE = 50;
    private static final double PREMIUM_COIN_BONUS = 1.5;
    private static final double PREMIUM_UPGRADE_DISCOUNT = 0.7;

    private static final Map<String, Integer> CAR_PRICES = new HashMap<String, Integer>() {{
        put("sedan", 0);
        put("sport", 60);
        put("muscle", 150);
        put("hyper", 300);
        put("super", 500);
        put("hero", 750);
    }};

    private static final int[] UPGRADE_ENGINE = {30, 60, 110, 180, 280};
    private static final int[] UPGRADE_TIRES = {25, 50, 90, 150, 240};
    private static final int UPGRADE_MAX = 5;
    private static final int COLOR_PRICE = 15;
    private static final String[] PART_KEYS = {"spoiler", "neon", "rims", "flame"};
    private static final int[] PART_PRICES = {30, 40, 50, 60};

    private static final Path ROOT = Paths.get(System.getProperty("user.dir"));
    private static final Path USERS_FILE = ROOT.resolve("data/users.txt");
    private static final Path SCORES_FILE = ROOT.resolve("data/scores.txt");
    private static final Path WALLETS_FILE = ROOT.resolve("data/wallets.txt");
    private static final Path DEVS_FILE = ROOT.resolve("data/devs.txt");
    private static final Path PROGRESS_FILE = ROOT.resolve("data/progress.txt");
    private static final Path MODS_FILE = ROOT.resolve("data/mods.txt");
    private static final Path IPLOG_FILE = ROOT.resolve("data/ip.log");
    private static final Path GOOGLE_LINKS_FILE = ROOT.resolve("data/google.txt");
    private static final String GOOGLE_CLIENT_ID = "";
    private static final Path CODES_FILE = ROOT.resolve("data/codes.txt");
    private static final String SMTP_HOST = System.getenv("SMTP_HOST") != null ? System.getenv("SMTP_HOST") : "smtp.gmail.com";
    private static final int SMTP_PORT = 587;
    private static final String SMTP_USER = System.getenv("SMTP_USER") != null ? System.getenv("SMTP_USER") : "";
    private static final String SMTP_PASS = System.getenv("SMTP_PASS") != null ? System.getenv("SMTP_PASS") : "";
    private static final String DEV_PASS = "FastCarDev@2026";
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final SecureRandom RND = new SecureRandom();
    private static final Map<String, String> TOKENS = new ConcurrentHashMap<>();
    private static final Map<String, Room> ROOMS = new ConcurrentHashMap<>();
    private static int roomSeq = 100;

    private static final Set<String> BANNED_IPS = ConcurrentHashMap.<String>newKeySet();
    private static final Map<String, Long> JAILED = new ConcurrentHashMap<>();
    private static final Map<String, String> LAST_IP = new ConcurrentHashMap<>();

    private static final String[][] TASKS_DAILY = {
            {"distance", "مسافة اليوم", "اقطع مسافة 500 متر", "500", "2000"},
            {"overtake", "تجاوز اليوم", "تجاوز منافسَين", "2", "2500"},
            {"crate", "صندوق اليوم", "اجمع صندوقاً واحداً", "1", "3500"},
            {"finish", "سباق اليوم", "أكمل سباقاً واحداً", "1", "5000"}
    };
    private static final String[][] TASKS_WEEKLY = {
            {"distance", "مسافة الأسبوع", "اقطع مسافة 5000 متر", "5000", "10000"},
            {"win", "فوز الأسبوع", "حقق انتصارين", "2", "15000"},
            {"social", "سباق جماعي", "أكمل سباقاً جماعياً", "1", "25000"}
    };
    private static final String[][] ACH_DEFS = {
            {"first_race", "أول انطلاقة", "أكمل سباقك الأول", "1", "500"},
            {"three_wins", "ثلاثة انتصارات", "حقق 3 انتصارات", "3", "1000"},
            {"distance_1000", "ألف متر", "اقطع مسافة 1000 متر", "1000", "600"},
            {"distance_10000", "عشرة آلاف متر", "اقطع مسافة 10000 متر", "10000", "5000"},
            {"speed_300", "سرعة عالية", "سجل سرعة 300+", "300", "1800"},
            {"multiplayer_race", "روح المنافسة", "أكمل سباقاً جماعياً", "1", "2000"}
    };
    private static final int WHEEL_COOLDOWN_MS = 6 * 60 * 60 * 1000;
    private static final String[] WHEEL_REWARDS_EGP = {"50", "0", "120", "0", "300", "0", "600", "25"};
    private static final Map<String, Map<Long, VoiceMsg>> VOICE = new ConcurrentHashMap<>();
    private static final Map<String, Long> VOICE_SEEN = new ConcurrentHashMap<>();
    private static final Object[][] VOICE_DEFS = {
        {"shield", "درع حماية"}, {"emp", "نبضة كهرومغناطيسية"}, {"drone", "طائرة إصلاح"},
        {"nitro", "عبوة نيترو"}, {"repair", "عدة إصلاح"}, {"fuel", "خزان وقود"}
    };

    private static final Object[][] PREMIUM_TIERS = {
        {"trial", "تجربة مجانية 3 أيام", 0, 3},
        {"bronze", "برونزي 7 أيام", 15, 7},
        {"silver", "فضي 30 يوم", 35, 30},
        {"gold", "ذهبي 90 يوم", 80, 90},
        {"vip", "VIP سنة كاملة", 150, 365}
    };

    private static final String[][] CHARACTERS = {
        {"rookie", "مبتدئ (Rookie)", "بدون مهارة خاصة", "0", "0"},
        {"speedy", "سريع (Speedy)", "+10% سرعة قصوى", "200", "0"},
        {"turbo", "توربو (Turbo)", "+15% تسارع", "400", "10"},
        {"lucky", "محظوظ (Lucky)", "+20% عملات إضافية من العجلة", "600", "15"},
        {"champ", "بطل (Champion)", "+5% مضاعفة النقاط في السباق", "800", "20"},
        {"veteran", "محترف (Veteran)", "+20% عملات إضافية من كل كسب", "1200", "30"},
        {"legend", "أسطورة (Legend)", "كل المهارات السابقة مجتمعة", "2000", "50"}
    };
    private static final String[][] STORE_ITEMS = {
        {"nitro_pack", "عبوة نيترو (+5)", "10", "nitro"},
        {"shield_pack", "درع حماية (+3)", "15", "shield"},
        {"double_coins", "مضاعفة عملات 24 ساعة", "20", "boost"},
        {"lucky_ticket", "تذكرة عجلة حظ إضافية", "8", "wheel"},
        {"repair_kit", "عدة إصلاح سريعة", "12", "repair"},
        {"emp_pack", "نبضة كهرومغناطيسية (+3)", "10", "emp"},
        {"skin_legendary", " skins حصرية أسطورية", "100", "skin"},
        {"horn_pack", "حقيبة أصوات منبه", "25", "horn"},
        {"trail_pack", "أثر مخصص للسيارة", "30", "trail"}
    };
    private static final Map<String, long[]> FLASH_SALES = new ConcurrentHashMap<>();
    private static volatile String ANNOUNCEMENT = "";
    private static volatile long ANNOUNCEMENT_TS = 0;
    private static final int NEW_PLAYER_BONUS_COINS = 1000;
    private static final Map<String, Long> NEW_PLAYER_TIMESTAMPS = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        Path data = USERS_FILE.getParent();
        Files.createDirectories(data);
        if (!Files.exists(USERS_FILE)) Files.write(USERS_FILE, Collections.emptyList(), StandardCharsets.UTF_8);
        if (!Files.exists(SCORES_FILE)) Files.write(SCORES_FILE, Collections.emptyList(), StandardCharsets.UTF_8);
        if (!Files.exists(WALLETS_FILE)) Files.write(WALLETS_FILE, Collections.emptyList(), StandardCharsets.UTF_8);
        Path dataDir = USERS_FILE.getParent();
        Path paymentsFile = dataDir.resolve("payments.txt");
        if (!Files.exists(paymentsFile)) Files.write(paymentsFile, Collections.emptyList(), StandardCharsets.UTF_8);
        if (!Files.exists(DEVS_FILE)) Files.write(DEVS_FILE, Collections.emptyList(), StandardCharsets.UTF_8);
        if (!Files.exists(MODS_FILE)) Files.write(MODS_FILE, Collections.emptyList(), StandardCharsets.UTF_8);
        if (!Files.exists(CODES_FILE)) Files.write(CODES_FILE, Collections.emptyList(), StandardCharsets.UTF_8);

        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }
        loadMods();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(16));
        server.createContext("/register", ServerMain::handleRegister);
        server.createContext("/login", ServerMain::handleLogin);
        server.createContext("/google-login", ServerMain::handleGoogleLogin);
        server.createContext("/whoami", ServerMain::handleWhoAmI);
        server.createContext("/score", ServerMain::handleScore);
        server.createContext("/scores", ServerMain::handleScores);
        server.createContext("/earn", ServerMain::handleEarn);
        server.createContext("/buy", ServerMain::handleBuy);
        server.createContext("/upgrade", ServerMain::handleUpgrade);
        server.createContext("/buypart", ServerMain::handleBuyPart);
        server.createContext("/buycolor", ServerMain::handleBuyColor);
        server.createContext("/select", ServerMain::handleSelect);
        server.createContext("/garage", ServerMain::handleGarage);
        server.createContext("/health", ServerMain::handleHealth);
        server.createContext("/privacy", ServerMain::handleDoc);
        server.createContext("/terms", ServerMain::handleDoc);
        server.createContext("/store", ServerMain::handleStore);
        server.createContext("/pay-request", ServerMain::handlePayRequest);
        server.createContext("/premium", ServerMain::handlePremium);
        server.createContext("/rooms", ServerMain::handleRooms);
        server.createContext("/room-create", ServerMain::handleRoomCreate);
        server.createContext("/room-join", ServerMain::handleRoomJoin);
        server.createContext("/room-leave", ServerMain::handleRoomLeave);
        server.createContext("/room-state", ServerMain::handleRoomState);
        server.createContext("/room-start", ServerMain::handleRoomStart);
        server.createContext("/room-score", ServerMain::handleRoomScore);
        server.createContext("/room-random", ServerMain::handleRoomRandom);
        server.createContext("/room-list", ServerMain::handleRoomList);
        server.createContext("/room/invite", ServerMain::handleRoomInvite);
        server.createContext("/room/promote", ServerMain::handleRoomPromote);
        server.createContext("/room/mute", ServerMain::handleRoomMute);
        server.createContext("/room/unmute", ServerMain::handleRoomUnmute);
        server.createContext("/room/kick", ServerMain::handleRoomKick);
        server.createContext("/room/ban", ServerMain::handleRoomBan);
        server.createContext("/room/unban", ServerMain::handleRoomUnban);
        server.createContext("/room/transfer", ServerMain::handleRoomTransfer);
        server.createContext("/room/report", ServerMain::handleRoomReport);
        server.createContext("/room/config", ServerMain::handleRoomConfig);
        server.createContext("/voice/send", ServerMain::handleVoiceSend);
        server.createContext("/voice/latest", ServerMain::handleVoiceLatest);
        server.createContext("/tasks", ServerMain::handleTasks);
        server.createContext("/achievements", ServerMain::handleAchievements);
        server.createContext("/wheel", ServerMain::handleWheel);
        server.createContext("/tool", ServerMain::handleTool);
        server.createContext("/characters", ServerMain::handleCharacters);
        server.createContext("/character-buy", ServerMain::handleCharacterBuy);
        server.createContext("/character-select", ServerMain::handleCharacterSelect);
        server.createContext("/character-grant", ServerMain::handleCharacterGrant);
        server.createContext("/selected-character", ServerMain::handleSelectedCharacter);
        server.createContext("/dev/activate", ServerMain::handleDevActivate);
        server.createContext("/dev-status", ServerMain::handleDevStatus);
        server.createContext("/dev/grant", ServerMain::handleDevGrant);
        server.createContext("/dev/ban", ServerMain::handleDevBan);
        server.createContext("/dev/set-coins", ServerMain::handleDevSetCoins);
        server.createContext("/dev/reset", ServerMain::handleDevReset);
        server.createContext("/dev/premium-grant", ServerMain::handleDevPremiumGrant);
        server.createContext("/dev/announce", ServerMain::handleDevAnnounce);
        server.createContext("/dev/stats", ServerMain::handleDevStats);
        server.createContext("/dev/users", ServerMain::handleDevUsers);
        server.createContext("/dev/flash-sale", ServerMain::handleDevFlashSale);
        server.createContext("/dev/jail", ServerMain::handleDevJail);
        server.createContext("/dev/ban-ip", ServerMain::handleDevBanIp);
        server.createContext("/dev/search", ServerMain::handleDevSearch);
        server.createContext("/dev/give", ServerMain::handleDevGive);
        server.createContext("/dev/backup", ServerMain::handleDevBackup);
        server.createContext("/dev/iplog", ServerMain::handleDevIpLog);
        server.createContext("/premium-tiers", ServerMain::handlePremiumTiers);
        server.createContext("/store-items", ServerMain::handleStoreItems);
        server.createContext("/announcement", ServerMain::handleAnnouncement);
        server.createContext("/tutorial", ServerMain::handleTutorial);
        server.createContext("/buy-item", ServerMain::handleBuyItem);
server.createContext("/premium-trial", ServerMain::handlePremiumTrial);
        server.createContext("/admin", ServerMain::handleAdmin);
        server.createContext("/register/check-user", ServerMain::handleCheckUser);
        server.createContext("/register/check-email", ServerMain::handleCheckEmail);
        server.createContext("/verify-email", ServerMain::handleVerifyEmail);
        server.createContext("/forgot-password", ServerMain::handleForgotPassword);
        server.createContext("/", ServerMain::handleRoot);
        server.start();

        String lan = localIp();
        System.out.println("==========================================================");
        System.out.println("   FAST CAR GAME SERVER");
        System.out.println("   Port             : " + port);
        System.out.println("   Local test       : http://127.0.0.1:" + port);
        if (lan != null) {
            System.out.println("   For the phone    : http://" + lan + ":" + port);
            System.out.println("   Put this address in app settings.");
        }
        System.out.println("   Endpoints        : /register  /login  /score  /scores");
        System.out.println("   Data stored in   : " + data.toString());
        System.out.println("   Press Ctrl+C to stop the server.");
        System.out.println("==========================================================");
    }

    private static String localIp() {
        try {
            for (java.util.Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface ni = en.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (java.util.Enumeration<InetAddress> aa = ni.getInetAddresses(); aa.hasMoreElements(); ) {
                    InetAddress a = aa.nextElement();
                    if (a instanceof java.net.Inet4Address) return a.getHostAddress();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static void handleRegister(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String user = f.get("user");
            String pass = f.get("pass");
            String email = f.get("email") != null ? f.get("email").trim().toLowerCase() : "";
            String gender = f.get("gender") != null ? f.get("gender").trim() : "";
            String err = validateCredentials(user, pass);
            if (err != null) {
                respond(ex, 200, "ERR:" + err);
                return;
            }
            if (email.isEmpty() || !email.contains("@") || !email.contains(".")) {
                respond(ex, 200, "ERR: البريد الإلكتروني غير صحيح");
                return;
            }
            user = user.trim();
            String salt = randomSalt();
            String hash = sha256(pass + "::" + salt);
            String code = String.format("%06d", RND.nextInt(1000000));
            long expires = System.currentTimeMillis() + 15 * 60 * 1000;
            LOCK.lock();
            try {
                Map<String, String[]> users = loadUsers();
                if (users.containsKey(user)) {
                    respond(ex, 200, "ERR: هذا المستخدم موجود بالفعل");
                    return;
                }
                for (String[] v : users.values()) {
                    if (v.length >= 4 && v[3].equalsIgnoreCase(email)) {
                        respond(ex, 200, "ERR: هذا البريد الإلكتروني مستخدم بالفعل");
                        return;
                    }
                }
                users.put(user, new String[]{salt, hash, b64(user), email, gender, "0"});
                saveUsers(users);
                List<String> codeLines = new ArrayList<>();
                if (Files.exists(CODES_FILE)) codeLines.addAll(Files.readAllLines(CODES_FILE, StandardCharsets.UTF_8));
                codeLines.add(email + "\t" + code + "\t" + expires);
                Files.write(CODES_FILE, codeLines, StandardCharsets.UTF_8);
                Map<String, Wallet> wls = loadWallets();
                if (!wls.containsKey(user)) {
                    Wallet nw = newWallet();
                    nw.coins = NEW_PLAYER_BONUS_COINS;
                    wls.put(user, nw);
                    NEW_PLAYER_TIMESTAMPS.put(user, System.currentTimeMillis());
                    saveWallets(wls);
                }
            } finally {
                LOCK.unlock();
            }
            try {
                sendEmail(email, "\u062a\u0623\u0643\u064a\u062f \u062d\u0633\u0627\u0628 FastCar", "\u0631\u0645\u0632 \u0627\u0644\u062a\u062d\u0642\u0642 \u062e\u0637\u0648\u0637\u0643: " + code + "\n\u064a\u0635\u0644 \u0647\u0630\u0627 \u0627\u0644\u0631\u0645\u0632 \u0628\u0635\u0648\u0631\u0629 \u0645\u062f\u0629 \u0644\u0644\u062a\u062d\u0642\u0642 \u0645\u0646 \u062d\u0633\u0627\u0628\u0643.\n\u0647\u0630\u0627 \u0627\u0644\u0631\u0645\u0632 \u0635\u0627\u0644\u062d \u0644\u0645\u062f\u0629 15 \u062f\u0642\u064a\u0642\u0629 \u0641\u0642\u0637.");
            } catch (Exception e) {
                System.err.println("SMTP error: " + e.getMessage());
            }
            respond(ex, 200, "OK");
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي في السيرفر");
        }
    }

    private static void handleCheckUser(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String user = f.get("user") != null ? f.get("user").trim() : "";
            if (user.isEmpty()) {
                respond(ex, 200, "ERR: أدخل اسم المستخدم");
                return;
            }
            if (user.length() < 3 || user.length() > 20) {
                respond(ex, 200, "ERR: اسم المستخدم من 3 إلى 20 حرف");
                return;
            }
            LOCK.lock();
            try {
                Map<String, String[]> users = loadUsers();
                if (users.containsKey(user)) {
                    respond(ex, 200, "ERR: هذا المستخدم موجود بالفعل");
                    return;
                }
            } finally {
                LOCK.unlock();
            }
            respond(ex, 200, "OK");
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static void handleCheckEmail(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String email = f.get("email") != null ? f.get("email").trim().toLowerCase() : "";
            if (email.isEmpty() || !email.contains("@") || !email.contains(".")) {
                respond(ex, 200, "ERR: البريد الإلكتروني غير صحيح");
                return;
            }
            LOCK.lock();
            try {
                Map<String, String[]> users = loadUsers();
                for (String[] v : users.values()) {
                    if (v.length >= 4 && v[3].equalsIgnoreCase(email)) {
                        respond(ex, 200, "ERR: هذا البريد الإلكتروني مستخدم بالفعل");
                        return;
                    }
                }
            } finally {
                LOCK.unlock();
            }
            respond(ex, 200, "OK");
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static void handleVerifyEmail(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String email = f.get("email") != null ? f.get("email").trim().toLowerCase() : "";
            String code = f.get("code") != null ? f.get("code").trim() : "";
            if (email.isEmpty() || code.isEmpty()) {
                respond(ex, 200, "ERR: بيانات ناقصة");
                return;
            }
            LOCK.lock();
            try {
                List<String> lines = new ArrayList<>();
                if (Files.exists(CODES_FILE)) lines.addAll(Files.readAllLines(CODES_FILE, StandardCharsets.UTF_8));
                long now = System.currentTimeMillis();
                boolean found = false;
                List<String> remaining = new ArrayList<>();
                for (String line : lines) {
                    String[] parts = line.split("\t", -1);
                    if (parts.length < 3) continue;
                    if (parts[0].equalsIgnoreCase(email) && parts[1].equals(code)) {
                        long exp = Long.parseLong(parts[2]);
                        if (exp > now) {
                            found = true;
                        }
                    } else {
                        remaining.add(line);
                    }
                }
                Files.write(CODES_FILE, remaining, StandardCharsets.UTF_8);
                if (!found) {
                    respond(ex, 200, "ERR: الرمز غير صحيح أو منتهي الصلاحية");
                    return;
                }
                Map<String, String[]> users = loadUsers();
                for (Map.Entry<String, String[]> entry : users.entrySet()) {
                    String[] v = entry.getValue();
                    if (v.length >= 6 && v[3].equalsIgnoreCase(email)) {
                        v[5] = "1";
                        break;
                    }
                }
                saveUsers(users);
            } finally {
                LOCK.unlock();
            }
            respond(ex, 200, "OK");
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static void handleForgotPassword(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String user = f.get("user") != null ? f.get("user").trim() : "";
            String email = f.get("email") != null ? f.get("email").trim().toLowerCase() : "";
            if (user.isEmpty() || email.isEmpty()) {
                respond(ex, 200, "ERR: أدخل اسم المستخدم والبريد الإلكتروني");
                return;
            }
            String newPass = randomPassword(10);
            LOCK.lock();
            try {
                Map<String, String[]> users = loadUsers();
                String[] rec = users.get(user);
                if (rec == null) {
                    respond(ex, 200, "ERR: المستخدم غير موجود");
                    return;
                }
                if (rec.length < 4 || !rec[3].equalsIgnoreCase(email)) {
                    respond(ex, 200, "ERR: البريد الإلكتروني غير مطابق لهذا المستخدم");
                    return;
                }
                String salt = randomSalt();
                String hash = sha256(newPass + "::" + salt);
                rec[0] = salt;
                rec[1] = hash;
                saveUsers(users);
            } finally {
                LOCK.unlock();
            }
            try {
                sendEmail(email, "\u0643\u0644\u0645\u0629 \u0627\u0644\u0645\u0631\u0648\u0631 \u062c\u062f\u064a\u062f\u0629 - FastCar", "\u0645\u0631\u062d\u0628\u0627ً \u060c \u0643\u0644\u0645\u0629 \u0627\u0644\u0645\u0631\u0648\u0631 \u0627\u0644\u062c\u062f\u064a\u062f\u0629 \u0644\u062d\u0633\u0627\u0628\u0643 \u0647\u064a: " + newPass + "\n\u0633\u062a\u062c\u062f\u062f \u0628\u0639\u062f \u0627\u0644\u062f\u062e\u0648\u0644 \u062a\u0646\u0635\u064a\u0644 \u0643\u0644\u0645\u0629 \u0627\u0644\u0645\u0631\u0648\u0631 \u0627\u0644\u062c\u062f\u064a\u062f\u0629 \u0627\u0644\u062a\u064a \u062a\u0631\u0633\u0644\u0643 \u0647\u0646\u0627.");
            } catch (Exception e) {
                System.err.println("SMTP error: " + e.getMessage());
            }
            respond(ex, 200, "OK");
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static void handleLogin(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String user = f.get("user");
            String pass = f.get("pass");
            if (user == null || user.trim().isEmpty() || pass == null || pass.isEmpty()) {
                respond(ex, 200, "ERR: أدخل اسم المستخدم وكلمة المرور");
                return;
            }
            user = user.trim();
            String rip = remoteIp(ex);
            if (BANNED_IPS.contains(rip)) {
                respond(ex, 200, "ERR: تم حظر عنوان الـ IP الخاص بك");
                return;
            }
            LOCK.lock();
            String token;
            try {
                Map<String, String[]> users = loadUsers();
                String[] rec = users.get(user);
                if (rec == null) {
                    respond(ex, 200, "ERR: اسم المستخدم أو كلمة المرور غير صحيحة");
                    return;
                }
                String salt = rec[0];
                String hash = rec[1];
                if (!hash.equals(sha256(pass + "::" + salt))) {
                    respond(ex, 200, "ERR: اسم المستخدم أو كلمة المرور غير صحيحة");
                    return;
                }
                if (isJailed(user)) {
                    respond(ex, 200, "ERR: تم سجنك حتى " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date(JAILED.get(user))));
                    return;
                }
                token = newToken();
                TOKENS.put(token, user);
            } finally {
                LOCK.unlock();
            }
            recordIp(user, rip);
            respond(ex, 200, "OK " + token);
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي في السيرفر");
        }
    }

    private static void handleGoogleLogin(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String idToken = f.get("id_token");
            if (idToken == null || idToken.trim().isEmpty()) {
                respond(ex, 200, "ERR: بيانات ناقصة");
                return;
            }
            final String email = verifyGoogleIdToken(idToken.trim());
            if (email == null) {
                respond(ex, 200, "ERR: تعذر التحقق من حساب جوجل");
                return;
            }
            String rip = remoteIp(ex);
            if (BANNED_IPS.contains(rip)) {
                respond(ex, 200, "ERR: تم حظر عنوان الـ IP الخاص بك");
                return;
            }
            LOCK.lock();
            String token;
            String user = null;
            try {
                Map<String, String> links = loadGoogleLinks();
                user = links.get(email);
                Map<String, String[]> users = loadUsers();
                if (user == null) {
                    user = uniqueGoogleUsername(email, users);
                    String salt = randomSalt();
                    String hash = sha256(newToken() + "::google::" + salt);
                    users.put(user, new String[]{salt, hash, b64(user)});
                    saveUsers(users);
                    links.put(email, user);
                    saveGoogleLinks(links);
                    Map<String, Wallet> wls = loadWallets();
                    if (!wls.containsKey(user)) {
                        Wallet nw = newWallet();
                        nw.coins = NEW_PLAYER_BONUS_COINS;
                        wls.put(user, nw);
                        NEW_PLAYER_TIMESTAMPS.put(user, System.currentTimeMillis());
                        saveWallets(wls);
                    }
                }
                if (isJailed(user)) {
                    respond(ex, 200, "ERR: تم سجنك حتى " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date(JAILED.get(user))));
                    return;
                }
                token = newToken();
                TOKENS.put(token, user);
            } finally {
                LOCK.unlock();
            }
            recordIp(user, rip);
            respond(ex, 200, "OK " + token);
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي في السيرفر");
        }
    }

    private static void handleWhoAmI(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت");
                return;
            }
            respond(ex, 200, user);
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static Map<String, String> loadGoogleLinks() {
        Map<String, String> map = new HashMap<>();
        try {
            if (Files.exists(GOOGLE_LINKS_FILE)) {
                for (String line : Files.readAllLines(GOOGLE_LINKS_FILE, StandardCharsets.UTF_8)) {
                    String[] p = line.split("\t", -1);
                    if (p.length >= 2) map.put(p[0], p[1]);
                }
            }
        } catch (Exception ignored) {
        }
        return map;
    }

    private static void saveGoogleLinks(Map<String, String> links) {
        try {
            List<String> out = new ArrayList<>();
            for (Map.Entry<String, String> e : links.entrySet()) {
                out.add(e.getKey() + "\t" + e.getValue());
            }
            Files.write(GOOGLE_LINKS_FILE, out, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private static String uniqueGoogleUsername(String email, Map<String, String[]> users) {
        String local = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        local = local.replaceAll("[^\\p{L}\\p{N}_ .-]", "").replaceAll("\\s+", "_").trim();
        if (local.isEmpty()) local = "google";
        if (local.length() > 18) local = local.substring(0, 18);
        if (!users.containsKey(local)) return local;
        String suffix = Integer.toHexString(email.hashCode()).replace("-", "");
        if (suffix.length() > 4) suffix = suffix.substring(0, 4);
        String base = local.length() > 16 ? local.substring(0, 16) : local;
        String candidate = base + "_" + suffix;
        int i = 1;
        while (users.containsKey(candidate)) {
            candidate = base + "_" + suffix + i;
            i++;
        }
        return candidate;
    }

    private static String verifyGoogleIdToken(String idToken) {
        try {
            URL u = new URL("https://oauth2.googleapis.com/tokeninfo?id_token=" + URLEncoder.encode(idToken, "UTF-8"));
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.setRequestProperty("Accept", "application/json");
            int code = c.getResponseCode();
            if (code != 200) return null;
            InputStream in = c.getInputStream();
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] tmp = new byte[8192];
            int n;
            while ((n = in.read(tmp)) > 0) buf.write(tmp, 0, n);
            in.close();
            String body = new String(buf.toByteArray(), StandardCharsets.UTF_8);
            String email = jsonStr(body, "email");
            if (email == null) return null;
            if (!GOOGLE_CLIENT_ID.isEmpty()) {
                String aud = jsonStr(body, "aud");
                if (!GOOGLE_CLIENT_ID.equals(aud)) return null;
            }
            return email.toLowerCase(Locale.ROOT);
        } catch (Exception t) {
            return null;
        }
    }

    private static String jsonStr(String body, String key) {
        String k = "\"" + key + "\":\"";
        int i = body.indexOf(k);
        if (i < 0) return null;
        int s = i + k.length();
        int e = body.indexOf('"', s);
        if (e < 0) return null;
        return body.substring(s, e);
    }

    private static void handleScore(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = ex.getRequestHeaders().getFirst("Authorization");
            if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
            if (token == null || token.isEmpty()) token = f.get("token");
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت، سجل الدخول من جديد");
                return;
            }
            String sc = f.get("score");
            if (sc == null) {
                respond(ex, 200, "ERR: لا توجد نتيجة");
                return;
            }
            int score;
            try {
                score = Integer.parseInt(sc.trim());
            } catch (NumberFormatException ignored) {
                respond(ex, 200, "ERR: نتيجة غير صحيحة");
                return;
            }
            LOCK.lock();
            try {
                List<String> lines = new ArrayList<>(Files.readAllLines(SCORES_FILE, StandardCharsets.UTF_8));
                lines.add(b64(user) + "|" + score);
                if (lines.size() > 20000) lines = lines.subList(lines.size() - 20000, lines.size());
                Files.write(SCORES_FILE, lines, StandardCharsets.UTF_8);
            } finally {
                LOCK.unlock();
            }
            respond(ex, 200, "OK");
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي في السيرفر");
        }
    }

    private static void handleScores(HttpExchange ex) throws IOException {
        try {
            List<String> lines;
            LOCK.lock();
            try {
                lines = Files.readAllLines(SCORES_FILE, StandardCharsets.UTF_8);
            } finally {
                LOCK.unlock();
            }
            List<String[]> parsed = new ArrayList<>();
            for (String line : lines) {
                int p = line.indexOf('|');
                if (p <= 0) continue;
                try {
                    int sc = Integer.parseInt(line.substring(p + 1).trim());
                    parsed.add(new String[]{decodeB64(line.substring(0, p)), String.valueOf(sc)});
                } catch (Exception ignored) {
                }
            }
            parsed.sort(new Comparator<String[]>() {
                @Override
                public int compare(String[] a, String[] b) {
                    return Integer.compare(Integer.parseInt(b[1]), Integer.parseInt(a[1]));
                }
            });
            StringBuilder sb = new StringBuilder();
            int n = Math.min(TOP_SCORES, parsed.size());
            for (int i = 0; i < n; i++) {
                if (i > 0) sb.append("\n");
                sb.append(parsed.get(i)[0]).append("|").append(parsed.get(i)[1]);
            }
            respond(ex, 200, sb.toString());
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي في السيرفر");
        }
    }

    private static void handleEarn(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت، سجل الدخول من جديد");
                return;
            }
            String c = f.get("coins");
            if (c == null) {
                respond(ex, 200, "ERR: لا توجد عملات");
                return;
            }
            long coins;
            try {
                coins = Long.parseLong(c.trim());
            } catch (NumberFormatException ignored) {
                respond(ex, 200, "ERR: قيمة غير صحيحة");
                return;
            }
            if (coins < 0 || coins > 100000) {
                respond(ex, 200, "ERR: قيمة غير مسموحة");
                return;
            }
            Wallet w;
            LOCK.lock();
            try {
                Map<String, Wallet> wls = loadWallets();
                w = wls.get(user);
                if (w == null) {
                    w = new Wallet(0, new LinkedHashSet<String>());
                    wls.put(user, w);
                }
                w.coins += coins;
                if (w.premium) w.coins += coins / 2;
                if (hasCharacterSkill(w, "earn")) w.coins += coins / 5;
                saveWallets(wls);
            } finally {
                LOCK.unlock();
            }
            respond(ex, 200, "OK " + w.coins);
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي في السيرفر");
        }
    }

    private static void handleBuy(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت، سجل الدخول من جديد");
                return;
            }
            String car = f.get("car");
            if (car == null || !CAR_PRICES.containsKey(car)) {
                respond(ex, 200, "ERR: نوع سيارة غير معروف");
                return;
            }
            int price = CAR_PRICES.get(car);
            Wallet w;
            LOCK.lock();
            try {
                Map<String, Wallet> wls = loadWallets();
                w = wls.get(user);
                if (w == null) {
                    w = newWallet();
                    wls.put(user, w);
                }
                if (w.owned.contains(car)) {
                    respond(ex, 200, "ERR: تملك هذه السيارة بالفعل");
                    return;
                }
                if (w.coins < price) {
                    respond(ex, 200, "ERR: رصيدك لا يكفي لشراء هذه السيارة");
                    return;
                }
                w.coins -= price;
                w.owned.add(car);
                if (w.selected == null) {
                    w.selected = car;
                    w.colorSel.put(car, colorOf(car));
                }
                saveWallets(wls);
            } finally {
                LOCK.unlock();
            }
            respond(ex, 200, "OK " + w.coins + " " + w.selected + " " + colorOf(car));
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي في السيرفر");
        }
    }

    private static void handleUpgrade(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت، سجل الدخول من جديد");
                return;
            }
            String car = f.get("car");
            String part = f.get("part");
            if (car == null || !CAR_PRICES.containsKey(car)) {
                respond(ex, 200, "ERR: نوع سيارة غير معروف");
                return;
            }
            if (!"engine".equals(part) && !"tires".equals(part)) {
                respond(ex, 200, "ERR: نوع تطوير غير معروف");
                return;
            }
            boolean eng = "engine".equals(part);
            Wallet w;
            LOCK.lock();
            try {
                Map<String, Wallet> wls = loadWallets();
                w = wls.get(user);
                if (w == null) {
                    respond(ex, 200, "ERR: السيارة ليست في كراجك");
                    return;
                }
                if (!w.owned.contains(car)) {
                    respond(ex, 200, "ERR: السيارة ليست في كراجك");
                    return;
                }
                int[] lv = w.upgr.get(car);
                if (lv == null) lv = new int[]{0, 0};
                int lvl = eng ? lv[0] : lv[1];
                if (lvl >= UPGRADE_MAX) {
                    respond(ex, 200, "ERR: هذا التطوير بلغ الحد الأقصى");
                    return;
                }
                int cost = (eng ? UPGRADE_ENGINE : UPGRADE_TIRES)[lvl];
                if (isPremiumActive(w)) cost = (int) Math.ceil(cost * PREMIUM_UPGRADE_DISCOUNT);
                if (w.coins < cost) {
                    respond(ex, 200, "ERR: رصيدك لا يكفي لهذا التطوير");
                    return;
                }
                if (eng) lv[0]++; else lv[1]++;
                w.upgr.put(car, lv);
                w.coins -= cost;
                saveWallets(wls);
            } finally {
                LOCK.unlock();
            }
            int[] lv2 = w.upgr.get(car);
            respond(ex, 200, "OK " + w.coins + " " + lv2[0] + " " + lv2[1]);
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي في السيرفر");
        }
    }

    private static void handleBuyPart(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت، سجل الدخول من جديد");
                return;
            }
            String car = f.get("car");
            String part = f.get("part");
            int pi = indexOf(PART_KEYS, part);
            if (car == null || !CAR_PRICES.containsKey(car)) {
                respond(ex, 200, "ERR: نوع سيارة غير معروف");
                return;
            }
            if (pi < 0) {
                respond(ex, 200, "ERR: مكوّن غير معروف");
                return;
            }
            Wallet w;
            LOCK.lock();
            try {
                Map<String, Wallet> wls = loadWallets();
                w = wls.get(user);
                if (w == null || !w.owned.contains(car)) {
                    respond(ex, 200, "ERR: السيارة ليست في كراجك");
                    return;
                }
                int mask = w.partsMask.containsKey(car) ? w.partsMask.get(car) : 0;
                if ((mask & (1 << pi)) != 0) {
                    respond(ex, 200, "ERR: تملك هذا المكوّن بالفعل");
                    return;
                }
                int price = PART_PRICES[pi];
                if (w.coins < price) {
                    respond(ex, 200, "ERR: رصيدك لا يكفي لهذا المكوّن");
                    return;
                }
                mask |= 1 << pi;
                w.partsMask.put(car, mask);
                w.coins -= price;
                saveWallets(wls);
            } finally {
                LOCK.unlock();
            }
            respond(ex, 200, "OK " + w.coins + " " + w.partsMask.get(car));
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي في السيرفر");
        }
    }

    private static void handleBuyColor(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت، سجل الدخول من جديد");
                return;
            }
            String car = f.get("car");
            int color;
            try {
                color = Integer.parseInt(f.get("color"));
            } catch (Exception e) {
                respond(ex, 200, "ERR: لون غير صحيح");
                return;
            }
            if (car == null || !CAR_PRICES.containsKey(car)) {
                respond(ex, 200, "ERR: نوع سيارة غير معروف");
                return;
            }
            if (!isPaletteColor(car, color)) {
                respond(ex, 200, "ERR: هذا اللون غير متاح لهذه السيارة");
                return;
            }
            Wallet w;
            LOCK.lock();
            try {
                Map<String, Wallet> wls = loadWallets();
                w = wls.get(user);
                if (w == null || !w.owned.contains(car)) {
                    respond(ex, 200, "ERR: السيارة ليست في كراجك");
                    return;
                }
                Set<String> own = w.colorsOwned.get(car);
                if (own != null && own.contains(String.valueOf(color))) {
                    respond(ex, 200, "ERR: تملك هذا اللون بالفعل");
                    return;
                }
                if (w.coins < COLOR_PRICE) {
                    respond(ex, 200, "ERR: رصيدك لا يكفي لهذا اللون");
                    return;
                }
                if (own == null) {
                    own = new LinkedHashSet<String>();
                    w.colorsOwned.put(car, own);
                }
                own.add(String.valueOf(color));
                w.coins -= COLOR_PRICE;
                w.colorSel.put(car, color);
                saveWallets(wls);
            } finally {
                LOCK.unlock();
            }
            respond(ex, 200, "OK " + w.coins + " " + color);
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي في السيرفر");
        }
    }

    private static void handleSelect(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت، سجل الدخول من جديد");
                return;
            }
            String car = f.get("car");
            if (car == null || !CAR_PRICES.containsKey(car)) {
                respond(ex, 200, "ERR: نوع سيارة غير معروف");
                return;
            }
            Wallet w;
            LOCK.lock();
            try {
                Map<String, Wallet> wls = loadWallets();
                w = wls.get(user);
                if (w == null || !w.owned.contains(car)) {
                    respond(ex, 200, "ERR: السيارة ليست في كراجك");
                    return;
                }
                String colStr = f.get("color");
                if (colStr != null && !colStr.trim().isEmpty()) {
                    int color;
                    try {
                        color = Integer.parseInt(colStr.trim());
                    } catch (Exception e) {
                        respond(ex, 200, "ERR: لون غير صحيح");
                        return;
                    }
                    boolean ok = isPaletteColor(car, color);
                    Set<String> own = w.colorsOwned.get(car);
                    if (color == colorOf(car)) {
                        ok = true;
                    } else if (own == null || !own.contains(String.valueOf(color))) {
                        ok = false;
                    }
                    if (!ok) {
                        respond(ex, 200, "ERR: لا تملك هذا اللون");
                        return;
                    }
                    w.colorSel.put(car, color);
                }
                w.selected = car;
                saveWallets(wls);
            } finally {
                LOCK.unlock();
            }
            respond(ex, 200, "OK " + w.coins + " " + car + " " + w.colorSel.get(car));
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي في السيرفر");
        }
    }

    private static void handleGarage(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت، سجل الدخول من جديد");
                return;
            }
            LOCK.lock();
            Wallet w;
            try {
                Map<String, Wallet> wls = loadWallets();
                w = wls.get(user);
                if (w == null) w = newWallet();
            } finally {
                LOCK.unlock();
            }
            StringBuilder owned = new StringBuilder();
            for (String k : w.owned) {
                if (owned.length() > 0) owned.append(",");
                owned.append(k);
            }
            StringBuilder upgr = new StringBuilder();
            for (Map.Entry<String, int[]> e : w.upgr.entrySet()) {
                int[] lv = e.getValue();
                if (lv == null || (lv[0] == 0 && lv[1] == 0)) continue;
                if (upgr.length() > 0) upgr.append(";");
                upgr.append(e.getKey()).append("=").append(lv[0]).append(":").append(lv[1]);
            }
            StringBuilder parts = new StringBuilder();
            for (Map.Entry<String, Integer> e : w.partsMask.entrySet()) {
                int mask = e.getValue();
                StringBuilder names = new StringBuilder();
                for (int i = 0; i < PART_KEYS.length; i++) {
                    if ((mask & (1 << i)) != 0) {
                        if (names.length() > 0) names.append(",");
                        names.append(PART_KEYS[i]);
                    }
                }
                if (names.length() == 0) continue;
                if (parts.length() > 0) parts.append(";");
                parts.append(e.getKey()).append("=").append(names);
            }
            StringBuilder colors = new StringBuilder();
            for (Map.Entry<String, Integer> e : w.colorSel.entrySet()) {
                String car = e.getKey();
                Set<String> own = w.colorsOwned.get(car);
                StringBuilder ownCsv = new StringBuilder();
                if (own != null) {
                    for (String c : own) {
                        if (ownCsv.length() > 0) ownCsv.append(",");
                        ownCsv.append(c);
                    }
                }
                int sel = e.getValue() == null ? colorOf(car) : e.getValue();
                if (colors.length() > 0) colors.append(";");
                colors.append(car).append("=").append(sel);
                if (ownCsv.length() > 0) colors.append("|").append(ownCsv);
            }
            respond(ex, 200, w.coins + "\t" + owned.toString() + "\t" + (w.selected == null ? "sedan" : w.selected)
                    + "\t" + upgr.toString() + "\t" + parts.toString() + "\t" + colors.toString()
                    + "\t" + (w.premium ? "1" : "0"));
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي في السيرفر");
        }
    }

    private static int indexOf(String[] arr, String v) {
        if (v == null) return -1;
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(v)) return i;
        return -1;
    }

    private static int colorOf(String car) {
        return paletteOf(car)[0];
    }

    private static int[] paletteOf(String car) {
        switch (car) {
            case "sport":
                return new int[]{0xFFE03E34, 0xFF1C1E24, 0xFFF0D228, 0xFFF0F4F8, 0xFF285ADC};
            case "muscle":
                return new int[]{0xFFE99114, 0xFF1C1E24, 0xFF289646, 0xFF969EA8, 0xFFD63C3C};
            case "hyper":
                return new int[]{0xFF9A5CC4, 0xFF1C1E24, 0xFF00D2DC, 0xFFF0F4F8, 0xFFF05A8C};
            case "super":
                return new int[]{0xFF26A894, 0xFFF0F4F8, 0xFFD2AA3C, 0xFF1C1E24, 0xFFD63C3C};
            case "hero":
                return new int[]{0xFFDEDEDE, 0xFFD63C3C, 0xFF285ADC, 0xFF1C1E24, 0xFFD2AA3C};
            default:
                return new int[]{0xFF5678D6, 0xFFC8CDD7, 0xFFEEF0F4, 0xFF1E2026, 0xFFD6463C};
        }
    }

    private static boolean isPaletteColor(String car, int color) {
        for (int c : paletteOf(car)) {
            if (c == color) return true;
        }
        return false;
    }

    private static String bearer(HttpExchange ex, Map<String, String> f) {
        String token = ex.getRequestHeaders().getFirst("Authorization");
        if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
        if (token == null || token.isEmpty()) token = f.get("token");
        return token;
    }

    private static void handleHealth(HttpExchange ex) throws IOException {
        respond(ex, 200, "UP " + System.currentTimeMillis());
    }

    private static void handleStore(HttpExchange ex) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < PACKAGES.length; i++) {
            if (i > 0) sb.append("\n");
            sb.append(PACKAGES[i][0]).append("|").append(PACKAGES[i][1]).append("|").append(VODAFONE_CASH);
        }
        sb.append("\nPREMIUM|").append(PREMIUM_PRICE).append("|").append(VODAFONE_CASH);
        respond(ex, 200, sb.toString());
    }

    private static void handlePremium(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت، سجل الدخول من جديد");
                return;
            }
            String txid = f.get("txid");
            if (txid == null || txid.trim().isEmpty()) {
                respond(ex, 200, "ERR: أدخل رقم العملية");
                return;
            }
            LOCK.lock();
            try {
                Map<String, Wallet> wls = loadWallets();
                Wallet w = wls.get(user);
                if (w == null) w = newWallet();
                if (w.premium) {
                    respond(ex, 200, "ERR: أنت premium بالفعل!");
                    return;
                }
                String id = "PREM_" + System.currentTimeMillis() + "_" + user.substring(0, Math.min(4, user.length()));
                String line = id + "\t" + user + "\t0\t" + PREMIUM_PRICE + "\t" + txid.trim() + "\tpending_premium";
                Path pf = USERS_FILE.getParent().resolve("payments.txt");
                List<String> lines = new ArrayList<>(Files.readAllLines(pf, StandardCharsets.UTF_8));
                lines.add(line);
                Files.write(pf, lines, StandardCharsets.UTF_8);
            } finally {
                LOCK.unlock();
            }
            respond(ex, 200, "OK طلب Premium قيد المراجعة. راجعنا الحوالة ونفعّلك الحساب.");
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي في السيرفر");
        }
    }

    private static void handlePayRequest(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت، سجل الدخول من جديد");
                return;
            }
            String pkg = f.get("package");
            String txid = f.get("txid");
            if (pkg == null || txid == null || txid.trim().isEmpty()) {
                respond(ex, 200, "ERR: أدخل رقم العملية");
                return;
            }
            int idx;
            try {
                idx = Integer.parseInt(pkg.trim());
            } catch (NumberFormatException e) {
                respond(ex, 200, "ERR: باقة غير صحيحة");
                return;
            }
            if (idx < 0 || idx >= PACKAGES.length) {
                respond(ex, 200, "ERR: باقة غير صحيحة");
                return;
            }
            long coins = PACKAGES[idx][0];
            long egp = PACKAGES[idx][1];
            String id = System.currentTimeMillis() + "_" + user.substring(0, Math.min(4, user.length()));
            String line = id + "\t" + user + "\t" + coins + "\t" + egp + "\t" + txid.trim() + "\tpending";
            LOCK.lock();
            try {
                Path pf = USERS_FILE.getParent().resolve("payments.txt");
                List<String> lines = new ArrayList<>(Files.readAllLines(pf, StandardCharsets.UTF_8));
                lines.add(line);
                Files.write(pf, lines, StandardCharsets.UTF_8);
            } finally {
                LOCK.unlock();
            }
            respond(ex, 200, "OK طلبك قيد المراجعة. راجعنا الحوالة ونزيدك العملات.");
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي في السيرفر");
        }
    }

    private static class Room {
        String code;
        String owner;
        String name;
        boolean locked;
        String password;
        List<String> members = new ArrayList<>();
        List<String> mods = new ArrayList<>();
        List<String> invites = new ArrayList<>();
        Set<String> muted = new HashSet<>();
        Set<String> banned = new HashSet<>();
        Map<String, Integer> reports = new HashMap<>();
        int maxSize = 6;
        String type = "public";
        long lastSeen = System.currentTimeMillis();
        boolean started;
        Map<String, Integer> scores = new HashMap<>();
    }

    private static String randomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) sb.append(chars.charAt(RND.nextInt(chars.length())));
        String s = sb.toString();
        int guard = 0;
        while (ROOMS.containsKey(s) && guard++ < 10) {
            sb.setLength(0);
            for (int i = 0; i < 4; i++) sb.append(chars.charAt(RND.nextInt(chars.length())));
            s = sb.toString();
        }
        return s;
    }

    private static void handleRoomList(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت");
                return;
            }
            Map<String, Room> snapshot = new HashMap<>();
            snapshot.putAll(ROOMS);
            StringBuilder sb = new StringBuilder();
            for (Room r : snapshot.values()) {
                if (r.locked) continue;
                if (sb.length() > 0) sb.append("\n");
                sb.append(r.code).append("|").append(r.name).append("|").append(r.owner)
                        .append("|").append(r.members.size()).append("|0");
            }
            respond(ex, 200, sb.toString().isEmpty() ? "(empty)" : sb.toString());
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static void handleRoomCreate(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت");
                return;
            }
            String name = f.get("name");
            if (name == null || name.trim().isEmpty()) name = user + "'s room";
            name = name.trim();
            if (name.length() > 20) name = name.substring(0, 20);
            String pw = f.get("pass");
            boolean locked = pw != null && !pw.trim().isEmpty();
            Room r = new Room();
            r.owner = user;
            r.name = name;
            r.locked = locked;
            r.password = locked ? pw.trim() : "";
            r.members.add(user);
            synchronized (LOCK) {
                r.code = String.valueOf(roomSeq++) + randomCode();
            }
            ROOMS.remove(roomOfUser(user));
            ROOMS.put(r.code, r);
            respond(ex, 200, "OK " + r.code);
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static void handleRoomJoin(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت");
                return;
            }
            String code = f.get("code");
            String pw = f.get("pass");
            if (code == null || code.trim().isEmpty()) {
                respond(ex, 200, "ERR: أدخل كود الغرفة");
                return;
            }
            code = code.trim().toUpperCase();
            Room r = ROOMS.get(code);
            if (r == null) {
                respond(ex, 200, "ERR: الغرفة غير موجودة");
                return;
            }
            if (r.banned.contains(user)) {
                respond(ex, 200, "ERR: تم حظرك من هذه الغرفة");
                return;
            }
            if (!r.members.contains(user) && r.members.size() >= r.maxSize) {
                respond(ex, 200, "ERR: الغرفة ممتلئة");
                return;
            }
            boolean invited = r.invites.contains(user);
            if (r.locked && !invited && !pw.equals(r.password)) {
                respond(ex, 200, "ERR: كلمة مرور الغرفة خاطئة");
                return;
            }
            if (r.invites.contains(user)) r.invites.remove(user);
            if (!r.members.contains(user)) r.members.add(user);
            respond(ex, 200, "OK " + r.code + "|" + r.name + "|" + r.owner);
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static void handleRoomLeave(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت");
                return;
            }
            String code = roomOfUser(user);
            if (code != null) {
                Room r = ROOMS.get(code);
                if (r != null) {
                    r.members.remove(user);
                    if (r.owner.equals(user) || r.members.isEmpty()) {
                        ROOMS.remove(code);
                    }
                }
            }
            respond(ex, 200, "OK");
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static void handleRoomState(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت");
                return;
            }
            String code = f.get("code");
            if (code == null) code = roomOfUser(user);
            if (code == null) {
                respond(ex, 200, "ERR: لست في غرفة");
                return;
            }
            Room r = ROOMS.get(code);
            if (r == null) {
                respond(ex, 200, "ERR: الغرفة أُغلقت");
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(r.code).append("|").append(r.name).append("|").append(r.owner).append("|");
            sb.append(r.started ? "1" : "0").append("|");
            for (int i = 0; i < r.members.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(r.members.get(i));
            }
            sb.append("|");
            for (Map.Entry<String, Integer> e : r.scores.entrySet()) {
                sb.append(e.getKey()).append("=").append(e.getValue()).append(";");
            }
            respond(ex, 200, sb.toString());
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static void handleRoomStart(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت");
                return;
            }
            String code = roomOfUser(user);
            if (code == null) {
                respond(ex, 200, "ERR: لست في غرفة");
                return;
            }
            Room r = ROOMS.get(code);
            if (r == null) {
                respond(ex, 200, "ERR: الغرفة أُغلقت");
                return;
            }
            r.started = true;
            r.scores.clear();
            respond(ex, 200, "OK " + r.code);
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static void handleRoomScore(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت");
                return;
            }
            String code = roomOfUser(user);
            if (code == null) {
                respond(ex, 200, "ERR: لست في غرفة");
                return;
            }
            String sc = f.get("score");
            int score;
            try {
                score = Integer.parseInt(sc.trim());
            } catch (Exception e) {
                respond(ex, 200, "ERR: قيمة غير صحيحة");
                return;
            }
            Room r = ROOMS.get(code);
            if (r == null) {
                respond(ex, 200, "ERR: الغرفة أُغلقت");
                return;
            }
            if (!r.started) {
                respond(ex, 200, "ERR: التحدي لم يبدأ بعد");
                return;
            }
            r.scores.put(user, score);
            respond(ex, 200, "OK");
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static void handleRoomRandom(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت");
                return;
            }
            String existing = roomOfUser(user);
            if (existing != null) {
                respond(ex, 200, "OK " + existing);
                return;
            }
            Room target = null;
            for (Room r : ROOMS.values()) {
                if (!r.locked && !r.banned.contains(user) && r.members.size() < r.maxSize) {
                    target = r;
                    break;
                }
            }
            if (target == null) {
                target = new Room();
                target.owner = user;
                target.name = "Quick Match";
                target.locked = false;
                target.members.add(user);
                synchronized (LOCK) {
                    target.code = String.valueOf(roomSeq++) + randomCode();
                }
                ROOMS.put(target.code, target);
                respond(ex, 200, "CREATED " + target.code);
                return;
            }
            target.members.add(user);
            respond(ex, 200, "JOINED " + target.code);
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static String roomOfUser(String user) {
        if (user == null) return null;
        for (Map.Entry<String, Room> e : ROOMS.entrySet()) {
            if (e.getValue().members.contains(user)) return e.getKey();
        }
        return null;
    }

    private static void handleRooms(HttpExchange ex) throws IOException {
        handleRoomList(ex);
    }

    private static boolean isDev(String user) throws IOException {
        List<String> lines = Files.readAllLines(DEVS_FILE, StandardCharsets.UTF_8);
        for (String l : lines) {
            if (l.trim().equals(user)) return true;
        }
        return false;
    }

    private static boolean isPremiumActive(Wallet w) {
        return w != null && w.premium && (w.premiumUntil > System.currentTimeMillis());
    }

    private static String resolveRoom(String user) {
        for (Map.Entry<String, Room> e : ROOMS.entrySet()) {
            if (e.getValue().members.contains(user)) return e.getKey();
        }
        return null;
    }

    private static boolean isRoomOwner(Room r, String user) {
        return r.owner != null && r.owner.equals(user);
    }

    private static boolean isRoomMod(Room r, String user) {
        return isRoomOwner(r, user) || (r.mods != null && r.mods.contains(user));
    }

    private static void handleRoomInvite(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) { respond(ex, 200, "ERR: الجلسة انتهت"); return; }
            String code = resolveRoom(user);
            if (code == null) { respond(ex, 200, "ERR: لست في غرفة"); return; }
            Room r = ROOMS.get(code);
            if (r == null) { respond(ex, 200, "ERR: الغرفة أُغلقت"); return; }
            if (!isRoomMod(r, user)) { respond(ex, 200, "ERR: لا تملك صلاحيات الدعوة"); return; }
            String target = f.get("user");
            if (target == null || target.trim().isEmpty()) { respond(ex, 200, "ERR: اسم المستخدم ناقص"); return; }
            target = target.trim();
            if (!r.invites.contains(target)) r.invites.add(target);
            respond(ex, 200, "OK تم إرسال دعوة إلى " + target);
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleRoomPromote(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) { respond(ex, 200, "ERR: الجلسة انتهت"); return; }
            String code = resolveRoom(user);
            if (code == null) { respond(ex, 200, "ERR: لست في غرفة"); return; }
            Room r = ROOMS.get(code);
            if (r == null) { respond(ex, 200, "ERR: الغرفة أُغلقت"); return; }
            if (!isRoomOwner(r, user)) { respond(ex, 200, "ERR: فقط مالك الغرفة يعين مشرفين"); return; }
            String target = f.get("user");
            if (target == null || !r.members.contains(target.trim())) { respond(ex, 200, "ERR: المستخدم ليس بالغرفة"); return; }
            target = target.trim();
            if (!r.mods.contains(target)) r.mods.add(target);
            respond(ex, 200, "OK تم ترقية " + target + " إلى مشرف");
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleRoomMute(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) { respond(ex, 200, "ERR: الجلسة انتهت"); return; }
            String code = resolveRoom(user);
            if (code == null) { respond(ex, 200, "ERR: لست في غرفة"); return; }
            Room r = ROOMS.get(code);
            if (r == null) { respond(ex, 200, "ERR: الغرفة أُغلقت"); return; }
            if (!isRoomMod(r, user)) { respond(ex, 200, "ERR: لا تملك صلاحيات الكتم"); return; }
            String target = f.get("user");
            if (target == null || target.trim().isEmpty()) { respond(ex, 200, "ERR: اسم المستخدم ناقص"); return; }
            target = target.trim();
            if (isRoomOwner(r, target)) { respond(ex, 200, "ERR: لا يمكن كتم المالك"); return; }
            r.muted.add(target);
            respond(ex, 200, "OK تم كتم " + target);
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleRoomUnmute(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) { respond(ex, 200, "ERR: الجلسة انتهت"); return; }
            String code = resolveRoom(user);
            if (code == null) { respond(ex, 200, "ERR: لست في غرفة"); return; }
            Room r = ROOMS.get(code);
            if (r == null) { respond(ex, 200, "ERR: الغرفة أُغلقت"); return; }
            if (!isRoomMod(r, user)) { respond(ex, 200, "ERR: لا تملك صلاحيات الكتم"); return; }
            String target = f.get("user");
            if (target == null) { respond(ex, 200, "ERR: اسم المستخدم ناقص"); return; }
            r.muted.remove(target.trim());
            respond(ex, 200, "OK تم فك الكتم عن " + target.trim());
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleRoomKick(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) { respond(ex, 200, "ERR: الجلسة انتهت"); return; }
            String code = resolveRoom(user);
            if (code == null) { respond(ex, 200, "ERR: لست في غرفة"); return; }
            Room r = ROOMS.get(code);
            if (r == null) { respond(ex, 200, "ERR: الغرفة أُغلقت"); return; }
            if (!isRoomMod(r, user)) { respond(ex, 200, "ERR: لا تملك صلاحيات الطرد"); return; }
            String target = f.get("user");
            if (target == null || !r.members.contains(target.trim())) { respond(ex, 200, "ERR: المستخدم ليس بالغرفة"); return; }
            target = target.trim();
            if (isRoomOwner(r, target)) { respond(ex, 200, "ERR: لا يمكن طرد المالك"); return; }
            r.members.remove(target);
            respond(ex, 200, "OK تم طرد " + target);
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleRoomBan(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) { respond(ex, 200, "ERR: الجلسة انتهت"); return; }
            String code = resolveRoom(user);
            if (code == null) { respond(ex, 200, "ERR: لست في غرفة"); return; }
            Room r = ROOMS.get(code);
            if (r == null) { respond(ex, 200, "ERR: الغرفة أُغلقت"); return; }
            if (!isRoomMod(r, user)) { respond(ex, 200, "ERR: لا تملك صلاحيات الحظر"); return; }
            String target = f.get("user");
            if (target == null || target.trim().isEmpty()) { respond(ex, 200, "ERR: اسم المستخدم ناقص"); return; }
            target = target.trim();
            if (isRoomOwner(r, target)) { respond(ex, 200, "ERR: لا يمكن حظر المالك"); return; }
            r.members.remove(target);
            r.mods.remove(target);
            r.banned.add(target);
            respond(ex, 200, "OK تم حظر " + target);
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleRoomUnban(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) { respond(ex, 200, "ERR: الجلسة انتهت"); return; }
            String code = resolveRoom(user);
            if (code == null) { respond(ex, 200, "ERR: لست في غرفة"); return; }
            Room r = ROOMS.get(code);
            if (r == null) { respond(ex, 200, "ERR: الغرفة أُغلقت"); return; }
            if (!isRoomMod(r, user)) { respond(ex, 200, "ERR: لا تملك صلاحيات الحظر"); return; }
            String target = f.get("user");
            if (target == null) { respond(ex, 200, "ERR: اسم المستخدم ناقص"); return; }
            r.banned.remove(target.trim());
            respond(ex, 200, "OK تم فك حظر " + target.trim());
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleRoomTransfer(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) { respond(ex, 200, "ERR: الجلسة انتهت"); return; }
            String code = resolveRoom(user);
            if (code == null) { respond(ex, 200, "ERR: لست في غرفة"); return; }
            Room r = ROOMS.get(code);
            if (r == null) { respond(ex, 200, "ERR: الغرفة أُغلقت"); return; }
            if (!isRoomOwner(r, user)) { respond(ex, 200, "ERR: فقط المالك ينقل الملكية"); return; }
            String target = f.get("user");
            if (target == null || !r.members.contains(target.trim())) { respond(ex, 200, "ERR: المستخدم ليس بالغرفة"); return; }
            target = target.trim();
            r.owner = target;
            r.mods.remove(target);
            if (!r.mods.contains(user)) r.mods.add(user);
            respond(ex, 200, "OK نُقلت ملكية الغرفة إلى " + target);
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleRoomReport(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) { respond(ex, 200, "ERR: الجلسة انتهت"); return; }
            String code = resolveRoom(user);
            if (code == null) { respond(ex, 200, "ERR: لست في غرفة"); return; }
            Room r = ROOMS.get(code);
            if (r == null) { respond(ex, 200, "ERR: الغرفة أُغلقت"); return; }
            String target = f.get("user");
            if (target == null || target.trim().isEmpty() || target.trim().equals(user)) { respond(ex, 200, "ERR: بلاغ غير صالح"); return; }
            target = target.trim();
            int n = r.reports.getOrDefault(target, 0) + 1;
            r.reports.put(target, n);
            respond(ex, 200, "OK تم تسجيل البلاغ (إجمالي " + n + ")");
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleRoomConfig(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) { respond(ex, 200, "ERR: الجلسة انتهت"); return; }
            String code = resolveRoom(user);
            if (code == null) { respond(ex, 200, "ERR: لست في غرفة"); return; }
            Room r = ROOMS.get(code);
            if (r == null) { respond(ex, 200, "ERR: الغرفة أُغلقت"); return; }
            if (!isRoomOwner(r, user)) { respond(ex, 200, "ERR: فقط المالك يعدّل الغرفة"); return; }
            String name = f.get("name");
            if (name != null && !name.trim().isEmpty()) {
                name = name.trim();
                if (name.length() > 20) name = name.substring(0, 20);
                r.name = name;
            }
            String pw = f.get("pass");
            if (pw != null) {
                r.locked = !pw.trim().isEmpty();
                r.password = r.locked ? pw.trim() : "";
            }
            String size = f.get("max");
            if (size != null) {
                try {
                    int m = Integer.parseInt(size.trim());
                    if (m >= 2 && m <= 16) r.maxSize = m;
                } catch (Exception ignored) {
                }
            }
            respond(ex, 200, "OK " + r.code);
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleDevActivate(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت");
                return;
            }
            String pw = f.get("pass");
            if (pw == null || !pw.equals(DEV_PASS)) {
                respond(ex, 200, "ERR: كلمة سر المطور غير صحيحة");
                return;
            }
            LOCK.lock();
            try {
                List<String> lines = new ArrayList<>(Files.readAllLines(DEVS_FILE, StandardCharsets.UTF_8));
                if (!lines.contains(user)) lines.add(user);
                Files.write(DEVS_FILE, lines, StandardCharsets.UTF_8);
            } finally {
                LOCK.unlock();
            }
            respond(ex, 200, "OK تَم تفعيل صلاحية المطور");
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static void handleDevStatus(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت");
                return;
            }
            respond(ex, 200, isDev(user) ? "DEV" : "PLAYER");
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static void handleDevGrant(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null || !isDev(user)) {
                respond(ex, 200, "ERR: ليس لديك صلاحيات مطور");
                return;
            }
            String target = f.get("user");
            String coins = f.get("coins");
            if (target == null || coins == null) {
                respond(ex, 200, "ERR: بيانات ناقصة");
                return;
            }
            int add;
            try {
                add = Integer.parseInt(coins.trim());
            } catch (Exception e) {
                respond(ex, 200, "ERR: قيمة غير صحيحة");
                return;
            }
            LOCK.lock();
            try {
                Map<String, Wallet> wls = loadWallets();
                Wallet w = wls.get(target.trim());
                if (w == null) {
                    respond(ex, 200, "ERR: المستخدم غير موجود");
                    return;
                }
                w.coins = Math.max(0, w.coins + add);
                saveWallets(wls);
            } finally {
                LOCK.unlock();
            }
            respond(ex, 200, "OK أُضيف " + add + " عملات إلى " + target);
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static void handleDevBan(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null || !isDev(user)) {
                respond(ex, 200, "ERR: ليس لديك صلاحيات مطور");
                return;
            }
            String target = f.get("user");
            if (target == null || target.trim().isEmpty()) {
                respond(ex, 200, "ERR: اسم المستخدم ناقص");
                return;
            }
            LOCK.lock();
            try {
                Map<String, Wallet> wls = loadWallets();
                Wallet w = wls.remove(target.trim());
                if (w == null) {
                    respond(ex, 200, "ERR: المستخدم غير موجود");
                    return;
                }
                saveWallets(wls);
            } finally {
                LOCK.unlock();
            }
            respond(ex, 200, "OK تم حذف حساب " + target);
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static String devAuth(HttpExchange ex, Map<String, String> f) throws IOException {
        String token = bearer(ex, f);
        if (token == null) return null;
        String user = TOKENS.get(token);
        if (user == null || !isDev(user)) return null;
        return user;
    }

    private static void handleDevSetCoins(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String dev = devAuth(ex, f);
            if (dev == null) { respond(ex, 200, "ERR: ليس لديك صلاحيات مطور"); return; }
            String target = f.get("user");
            long coins;
            try { coins = Long.parseLong(f.get("coins").trim()); }
            catch (Exception e) { respond(ex, 200, "ERR: قيمة غير صحيحة"); return; }
            LOCK.lock();
            try {
                Map<String, Wallet> wls = loadWallets();
                Wallet w = wls.get(target == null ? null : target.trim());
                if (w == null) { respond(ex, 200, "ERR: المستخدم غير موجود"); return; }
                w.coins = Math.max(0, coins);
                saveWallets(wls);
            } finally { LOCK.unlock(); }
            respond(ex, 200, "OK ضُبط الرصيد إلى " + coins + " لـ " + target);
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleDevReset(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String dev = devAuth(ex, f);
            if (dev == null) { respond(ex, 200, "ERR: ليس لديك صلاحيات مطور"); return; }
            String target = f.get("user");
            if (target == null || target.trim().isEmpty()) { respond(ex, 200, "ERR: اسم المستخدم ناقص"); return; }
            target = target.trim();
            LOCK.lock();
            try {
                Map<String, Wallet> wls = loadWallets();
                Wallet w = wls.remove(target);
                if (w == null) { respond(ex, 200, "ERR: المستخدم غير موجود"); return; }
                saveWallets(wls);
                Map<String, Map<String, String>> p = loadProgress();
                p.remove(target);
                saveProgress(p);
            } finally { LOCK.unlock(); }
            respond(ex, 200, "OK تمت إعادة تعيين حساب " + target);
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static final Set<String> TRIAL_CLAIMED = ConcurrentHashMap.<String>newKeySet();

    private static void handlePremiumTrial(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String user = userOf(ex, f);
            if (user == null) { respond(ex, 200, "ERR: الجلسة انتهت"); return; }
            LOCK.lock();
            try {
                if (TRIAL_CLAIMED.contains(user)) { respond(ex, 200, "ERR: جرّبت الباقة المجانية من قبل"); return; }
                Map<String, Wallet> wls = loadWallets();
                Wallet w = wls.get(user);
                if (w == null) { w = newWallet(); wls.put(user, w); }
                long days = 3;
                long until = Math.max(System.currentTimeMillis(), w.premiumUntil);
                until += days * 24L * 3600L * 1000L;
                w.premium = true;
                w.premiumUntil = until;
                w.premiumTier = "trial";
                saveWallets(wls);
                TRIAL_CLAIMED.add(user);
                respond(ex, 200, "OK تفعّلت باقة التجربة 3 أيام مجانًا!");
            } finally { LOCK.unlock(); }
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleDevPremiumGrant(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String dev = devAuth(ex, f);
            if (dev == null) { respond(ex, 200, "ERR: ليس لديك صلاحيات مطور"); return; }
            String target = f.get("user");
            String tier = f.get("tier");
            if (target == null || tier == null) { respond(ex, 200, "ERR: بيانات ناقصة"); return; }
            Object[] found = null;
            for (Object[] t : PREMIUM_TIERS) if (t[0].equals(tier)) { found = t; break; }
            if (found == null) { respond(ex, 200, "ERR: باقة غير معروفة"); return; }
            long days = ((Number) found[3]).longValue();
            LOCK.lock();
            try {
                Map<String, Wallet> wls = loadWallets();
                Wallet w = wls.get(target.trim());
                if (w == null) { respond(ex, 200, "ERR: المستخدم غير موجود"); return; }
                long until = Math.max(System.currentTimeMillis(), w.premiumUntil);
                until += days * 24L * 3600L * 1000L;
                w.premium = true;
                w.premiumUntil = until;
                w.premiumTier = (String) found[0];
                saveWallets(wls);
            } finally { LOCK.unlock(); }
            respond(ex, 200, "OK مُنح " + target + " باقة " + found[1]);
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleDevAnnounce(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String dev = devAuth(ex, f);
            if (dev == null) { respond(ex, 200, "ERR: ليس لديك صلاحيات مطور"); return; }
            String msg = f.get("msg");
            ANNOUNCEMENT = msg == null ? "" : msg.trim();
            ANNOUNCEMENT_TS = System.currentTimeMillis();
            respond(ex, 200, "OK تم تحديث الإعلان");
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleDevStats(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String dev = devAuth(ex, f);
            if (dev == null) { respond(ex, 200, "ERR: ليس لديك صلاحيات مطور"); return; }
            Map<String, Wallet> wls = loadWallets();
            long totalCoins = 0;
            int premiumCount = 0;
            for (Wallet w : wls.values()) {
                totalCoins += w.coins;
                if (isPremiumActive(w)) premiumCount++;
            }
            respond(ex, 200, "OK المستخدمون:" + wls.size()
                    + "|العملات الكلية:" + totalCoins
                    + "|Premium:" + premiumCount
                    + "|الغرف:" + ROOMS.size()
                    + "|الجلسات:" + TOKENS.size());
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleDevUsers(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String dev = devAuth(ex, f);
            if (dev == null) { respond(ex, 200, "ERR: ليس لديك صلاحيات مطور"); return; }
            Map<String, Wallet> wls = loadWallets();
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Wallet> e : wls.entrySet()) {
                if (sb.length() > 0) sb.append("\n");
                Wallet w = e.getValue();
                sb.append(e.getKey()).append("|").append(w.coins)
                  .append("|").append(w.premium ? "PREMIUM" : "عادي")
                  .append("|").append(w.premiumTier == null ? "-" : w.premiumTier);
            }
            if (sb.length() == 0) sb.append("لا يوجد مستخدمون");
            respond(ex, 200, sb.toString());
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleDevFlashSale(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String dev = devAuth(ex, f);
            if (dev == null) { respond(ex, 200, "ERR: ليس لديك صلاحيات مطور"); return; }
            String item = f.get("item");
            String disc = f.get("discount");
            String minutes = f.get("minutes");
            if (item == null || disc == null || minutes == null) { respond(ex, 200, "ERR: بيانات ناقصة"); return; }
            double discount;
            long mins;
            try { discount = Double.parseDouble(disc); mins = Long.parseLong(minutes); }
            catch (Exception e) { respond(ex, 200, "ERR: قيمة غير صحيحة"); return; }
            if (discount <= 0 || discount >= 1) { respond(ex, 200, "ERR: نسبة الخصم بين 0 و1"); return; }
            FLASH_SALES.put(item, new long[]{System.currentTimeMillis() + mins * 60_000, (long) (discount * 1000)});
            respond(ex, 200, "OK خصم " + ((int)(discount*100)) + "% على " + item + " لمدة " + mins + " دقيقة");
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleDevJail(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String dev = devAuth(ex, f);
            if (dev == null) { respond(ex, 200, "ERR: ليس لديك صلاحيات مطور"); return; }
            String target = f.get("user");
            String mins = f.get("minutes");
            String jailed = f.get("set");
            if (target == null || target.trim().isEmpty()) { respond(ex, 200, "ERR: اسم المستخدم ناقص"); return; }
            target = target.trim();
            LOCK.lock();
            try {
                if ("0".equals(jailed)) {
                    JAILED.remove(target);
                    respond(ex, 200, "OK تم فك السجن عن " + target);
                } else {
                    long m;
                    try { m = Long.parseLong(mins == null ? "60" : mins); } catch (Exception e) { respond(ex, 200, "ERR: دقائق غير صحيحة"); return; }
                    JAILED.put(target, System.currentTimeMillis() + m * 60_000);
                    respond(ex, 200, "OK تم سجن " + target + " لمدة " + m + " دقيقة");
                }
                saveMods();
            } finally { LOCK.unlock(); }
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleDevBanIp(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String dev = devAuth(ex, f);
            if (dev == null) { respond(ex, 200, "ERR: ليس لديك صلاحيات مطور"); return; }
            String ip = f.get("ip");
            String unban = f.get("set");
            if (ip == null || ip.trim().isEmpty()) { respond(ex, 200, "ERR: IP ناقص"); return; }
            String user = f.get("user");
            if (user != null && !user.trim().isEmpty()) {
                String tmp = LAST_IP.get(user.trim());
                if (tmp != null) ip = tmp;
            }
            ip = ip.trim();
            LOCK.lock();
            try {
                if ("0".equals(unban)) {
                    BANNED_IPS.remove(ip);
                    respond(ex, 200, "OK تم فك حظر الـ IP " + ip);
                } else {
                    BANNED_IPS.add(ip);
                    respond(ex, 200, "OK تم حظر الـ IP " + ip + " وجميع حساباته");
                }
                saveMods();
            } finally { LOCK.unlock(); }
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleDevSearch(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String dev = devAuth(ex, f);
            if (dev == null) { respond(ex, 200, "ERR: ليس لديك صلاحيات مطور"); return; }
            String q = f.get("q");
            if (q == null) q = "";
            q = q.trim().toLowerCase();
            Map<String, Wallet> wls = loadWallets();
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Wallet> e : wls.entrySet()) {
                String name = e.getKey();
                if (!q.isEmpty() && !name.toLowerCase().contains(q)) continue;
                Wallet w = e.getValue();
                if (sb.length() > 0) sb.append("\n");
                sb.append(name).append("|").append(w.coins)
                  .append("|").append(w.premium ? "PREMIUM" : "عادي")
                  .append("|السيارات:").append(w.owned.size())
                  .append("|الشخصيات:").append(w.chOwned.size())
                  .append("|الشخصية:").append(w.chSelected == null ? "rookie" : w.chSelected)
                  .append("|IP:").append(LAST_IP.get(name) == null ? "-" : LAST_IP.get(name))
                  .append("|سجن:").append(isJailed(name) ? "نعم" : "لا");
            }
            if (sb.length() == 0) sb.append("لا توجد نتائج");
            respond(ex, 200, sb.toString());
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleDevGive(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String dev = devAuth(ex, f);
            if (dev == null) { respond(ex, 200, "ERR: ليس لديك صلاحيات مطور"); return; }
            String target = f.get("user");
            String type = f.get("type");
            String id = f.get("id");
            if (target == null || type == null || id == null) { respond(ex, 200, "ERR: بيانات ناقصة"); return; }
            target = target.trim();
            LOCK.lock();
            try {
                Map<String, Wallet> wls = loadWallets();
                Wallet w = wls.get(target);
                if (w == null) { respond(ex, 200, "ERR: المستخدم غير موجود"); return; }
                if ("car".equals(type)) {
                    if (!CAR_PRICES.containsKey(id)) { respond(ex, 200, "ERR: سيارة غير معروفة"); return; }
                    w.owned.add(id);
                    if (w.colorSel.get(id) == null) w.colorSel.put(id, colorOf(id));
                } else if ("ch".equals(type)) {
                    String[] found = null;
                    for (String[] c : CHARACTERS) if (c[0].equals(id)) { found = c; break; }
                    if (found == null) { respond(ex, 200, "ERR: شخصية غير معروفة"); return; }
                    w.chOwned.add(id);
                } else if ("tool".equals(type)) {
                    Map<String, Map<String, String>> p = loadProgress();
                    Map<String, String> u = p.get(target);
                    if (u == null) { u = new HashMap<String, String>(); p.put(target, u); }
                    String key = "tool_" + id;
                    String prog = u.get(key);
                    long count = prog == null ? 0 : Long.parseLong(prog);
                    u.put(key, String.valueOf(count + 1));
                    saveProgress(p);
                } else if ("item".equals(type)) {
                    Map<String, Map<String, String>> p = loadProgress();
                    Map<String, String> u = p.get(target);
                    if (u == null) { u = new HashMap<String, String>(); p.put(target, u); }
                    String key = "item_" + id;
                    String prog = u.get(key);
                    long count = prog == null ? 0 : Long.parseLong(prog);
                    u.put(key, String.valueOf(count + 1));
                    saveProgress(p);
                } else if ("part".equals(type)) {
                    int i = indexOf(PART_KEYS, id);
                    if (i < 0) { respond(ex, 200, "ERR: جزء غير معروف"); return; }
                    for (String car : w.owned) {
                        w.partsMask.put(car, (w.partsMask.get(car) == null ? 0 : w.partsMask.get(car)) | (1 << i));
                    }
                } else { respond(ex, 200, "ERR: نوع غير معروف"); return; }
                saveWallets(wls);
            } finally { LOCK.unlock(); }
            respond(ex, 200, "OK تم منح " + target + " " + type + " = " + id);
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleDevBackup(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String dev = devAuth(ex, f);
            if (dev == null) { respond(ex, 200, "ERR: ليس لديك صلاحيات مطور"); return; }
            Path data = USERS_FILE.getParent();
            Path bk = data.resolve("backups");
            Files.createDirectories(bk);
            String stamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            int count = 0;
            for (String fn : new String[]{"users.txt", "scores.txt", "wallets.txt", "progress.txt", "devs.txt", "mods.txt", "payments.txt"}) {
                Path src = data.resolve(fn);
                if (!Files.exists(src)) continue;
                Files.copy(src, bk.resolve(fn + "." + stamp), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                count++;
            }
            respond(ex, 200, "OK تم إنشاء نسخة احتياطية " + stamp + " (" + count + " ملفات)");
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleDevIpLog(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String dev = devAuth(ex, f);
            if (dev == null) { respond(ex, 200, "ERR: ليس لديك صلاحيات مطور"); return; }
            String q = f.get("user");
            StringBuilder sb = new StringBuilder();
            if (Files.exists(IPLOG_FILE)) {
                for (String line : Files.readAllLines(IPLOG_FILE, StandardCharsets.UTF_8)) {
                    String[] p = line.split("\t");
                    if (p.length >= 3 && (q == null || q.isEmpty() || p[1].equals(q))) {
                        if (sb.length() > 0) sb.append("\n");
                        sb.append(new java.text.SimpleDateFormat("dd/MM HH:mm").format(new java.util.Date(Long.parseLong(p[0]))))
                          .append(" | ").append(p[1]).append(" | ").append(p[2]);
                    }
                }
            }
            if (sb.length() == 0) sb.append("لا يوجد سجل");
            respond(ex, 200, sb.toString());
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handlePremiumTiers(HttpExchange ex) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Object[] t : PREMIUM_TIERS) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(t[0]).append("|").append(t[1]).append("|").append(t[2]).append("|").append(t[3]);
        }
        respond(ex, 200, sb.toString());
    }

    private static void handleStoreItems(HttpExchange ex) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String[] it : STORE_ITEMS) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(it[0]).append("|").append(it[1]).append("|").append(it[2]).append("|").append(it[3]);
        }
        respond(ex, 200, sb.toString());
    }

    private static void handleAnnouncement(HttpExchange ex) throws IOException {
        if (ANNOUNCEMENT == null || ANNOUNCEMENT.isEmpty()) {
            respond(ex, 200, "NONE");
        } else {
            respond(ex, 200, "OK " + ANNOUNCEMENT + " (بتاريخ " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date(ANNOUNCEMENT_TS)) + ")");
        }
    }

    private static void handleTutorial(HttpExchange ex) throws IOException {
        respond(ex, 200, "OK مرحباً بسيارتك السريعة! اكسب العملات عبر السباقات."
                + "\n1) اختر سيارتك من الجراج وحسّن المحرك والإطارات."
                + "\n2) اسبق في السباقات وأكمل المهام اليومية والأسبوعية."
                + "\n3) استخدم العجلة لجمع عملات مجانية كل 6 ساعات."
                + "\n4) دلّل سيارتك بالألوان والإكسسوارات المميزة."
                + "\n5) اشترك Premium لمضاعفة أرباحك، أو اشترِ باقات عملات عبر فودافون كاش."
                + "\n6) تحدَّ أصدقاءك في الغرف الجماعية!"
                + "\nعملات أولى هدية: +" + NEW_PLAYER_BONUS_COINS);
    }

    private static void handleBuyItem(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String user = userOf(ex, f);
            if (user == null) { respond(ex, 200, "ERR: الجلسة انتهت"); return; }
            String id = f.get("item");
            if (id == null) { respond(ex, 200, "ERR: اسم المنتج ناقص"); return; }
            String[] found = null;
            for (String[] it : STORE_ITEMS) if (it[0].equals(id)) { found = it; break; }
            if (found == null) { respond(ex, 200, "ERR: منتج غير معروف"); return; }
            long price = Long.parseLong(found[2]);
            long[] sale = FLASH_SALES.get(id);
            if (sale != null && sale[0] > System.currentTimeMillis()) {
                long discount = sale[1];
                if (discount > 0 && discount <= 1000) price = price * (1000 - discount) / 1000;
            }
            LOCK.lock();
            try {
                Map<String, Wallet> wls = loadWallets();
                Wallet w = wls.get(user);
                if (w == null) { w = newWallet(); wls.put(user, w); }
                if (w.coins < price) { respond(ex, 200, "ERR: عملات غير كافية - تحتاج " + price); return; }
                w.coins -= price;
                Map<String, Map<String, String>> p = loadProgress();
                Map<String, String> u = p.get(user);
                if (u == null) { u = new HashMap<String, String>(); p.put(user, u); }
                String key = "item_" + id;
                String prog = u.get(key);
                long count = prog == null ? 0 : Long.parseLong(prog);
                u.put(key, String.valueOf(count + 1));
                saveWallets(wls);
                saveProgress(p);
                respond(ex, 200, "OK " + w.coins + " " + (count + 1));
            } finally { LOCK.unlock(); }
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleAdmin(HttpExchange ex) throws IOException {
        String uri = ex.getRequestURI().getPath();
        Map<String, String> f = form(ex);

        if (uri.endsWith("/approve")) {
            String pass = f.get("pass");
            String id = f.get("id");
            if (!ADMIN_PASS.equals(pass)) {
                respond(ex, 403, "ERR: كلمة المرور خاطئة");
                return;
            }
            LOCK.lock();
            try {
                Path pf = USERS_FILE.getParent().resolve("payments.txt");
                List<String> lines = new ArrayList<>(Files.readAllLines(pf, StandardCharsets.UTF_8));
                List<String> out = new ArrayList<>();
                boolean found = false;
                for (String line : lines) {
                    String[] parts = line.split("\t", -1);
                    if (parts.length >= 6 && parts[0].equals(id) && "pending".equals(parts[5])) {
                        found = true;
                        String user = parts[1];
                        long coins = Long.parseLong(parts[2]);
                        Map<String, Wallet> wls = loadWallets();
                        Wallet w = wls.get(user);
                        if (w == null) {
                            w = newWallet();
                            wls.put(user, w);
                        }
                        w.coins += coins;
                        saveWallets(wls);
                        out.add(parts[0] + "\t" + parts[1] + "\t" + parts[2] + "\t" + parts[3] + "\t" + parts[4] + "\tapproved");
                    } else if (parts.length >= 6 && parts[0].equals(id) && "pending_premium".equals(parts[5])) {
                        found = true;
                        String user = parts[1];
                        Map<String, Wallet> wls = loadWallets();
                        Wallet w = wls.get(user);
                        if (w == null) {
                            w = newWallet();
                            wls.put(user, w);
                        }
                        w.premium = true;
                        saveWallets(wls);
                        out.add(parts[0] + "\t" + parts[1] + "\t" + parts[2] + "\t" + parts[3] + "\t" + parts[4] + "\tapproved_premium");
                    } else if (parts.length >= 6 && parts[0].equals(id) && parts[5].startsWith("char_")) {
                        found = true;
                        String user = parts[1];
                        String chId = parts[5].substring(5);
                        Map<String, Wallet> wls = loadWallets();
                        Wallet w = wls.get(user);
                        if (w == null) {
                            w = newWallet();
                            wls.put(user, w);
                        }
                        w.chOwned.add(chId);
                        saveWallets(wls);
                        out.add(parts[0] + "\t" + parts[1] + "\t" + parts[2] + "\t" + parts[3] + "\t" + parts[4] + "\t" + parts[5]);
                    } else {
                        out.add(line);
                    }
                }
                Files.write(pf, out, StandardCharsets.UTF_8);
                respond(ex, 200, found ? "OK تمت الموافقة" : "ERR: طلب غير موجود");
            } finally {
                LOCK.unlock();
            }
            return;
        }

        if (uri.endsWith("/reject")) {
            String pass = f.get("pass");
            String id = f.get("id");
            if (!ADMIN_PASS.equals(pass)) {
                respond(ex, 403, "ERR: كلمة المرور خاطئة");
                return;
            }
            LOCK.lock();
            try {
                Path pf = USERS_FILE.getParent().resolve("payments.txt");
                List<String> lines = new ArrayList<>(Files.readAllLines(pf, StandardCharsets.UTF_8));
                List<String> out = new ArrayList<>();
                for (String line : lines) {
                    String[] parts = line.split("\t", -1);
                    if (parts.length >= 6 && parts[0].equals(id) && "pending".equals(parts[5])) {
                        out.add(parts[0] + "\t" + parts[1] + "\t" + parts[2] + "\t" + parts[3] + "\t" + parts[4] + "\trejected");
                    } else if (parts.length >= 6 && parts[0].equals(id) && "pending_premium".equals(parts[5])) {
                        out.add(parts[0] + "\t" + parts[1] + "\t" + parts[2] + "\t" + parts[3] + "\t" + parts[4] + "\trejected_premium");
                    } else {
                        out.add(line);
                    }
                }
                Files.write(pf, out, StandardCharsets.UTF_8);
                respond(ex, 200, "OK تم الرفض");
            } finally {
                LOCK.unlock();
            }
            return;
        }

        String pass = f.get("pass");
        if (!ADMIN_PASS.equals(pass)) {
            String html = "<!DOCTYPE html><html><head><meta charset='utf-8'><title>Admin</title></head><body>"
                    + "<h2>FastCar Admin</h2>"
                    + "<form><input name='pass' placeholder='Password'><button>OK</button></form></body></html>";
            respond(ex, 200, html);
            return;
        }

        List<String[]> pending = new ArrayList<>();
        List<String[]> history = new ArrayList<>();
        LOCK.lock();
        try {
            Path pf = USERS_FILE.getParent().resolve("payments.txt");
            if (Files.exists(pf)) {
                for (String line : Files.readAllLines(pf, StandardCharsets.UTF_8)) {
                    String[] parts = line.split("\t", -1);
                    if (parts.length >= 6) {
                        if ("pending".equals(parts[5])) pending.add(parts);
                        else history.add(parts);
                    }
                }
            }
        } finally {
            LOCK.unlock();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>FastCar Admin</title></head><body>");
        sb.append("<h2>FastCar Admin Panel</h2><p>Phone: ").append(VODAFONE_CASH).append("</p>");
        sb.append("<h3>Pending Payments (").append(pending.size()).append(")</h3><table border=1 cellpadding=6><tr><th>ID</th><th>User</th><th>Coins</th><th>EGP</th><th>TxID</th><th>Action</th></tr>");
        for (String[] p : pending) {
            sb.append("<tr><td>").append(p[0]).append("</td><td>").append(p[1]).append("</td><td>").append(p[2]).append("</td><td>").append(p[3]).append("</td><td>").append(p[4]).append("</td><td>");
            sb.append("<a href='/admin/approve?id=").append(p[0]).append("&pass=").append(ADMIN_PASS).append("'>Approve</a> | ");
            sb.append("<a href='/admin/reject?id=").append(p[0]).append("&pass=").append(ADMIN_PASS).append("'>Reject</a>");
            sb.append("</td></tr>");
        }
        sb.append("</table>");
        sb.append("<h3>History</h3><table border=1 cellpadding=6><tr><th>ID</th><th>User</th><th>Coins</th><th>EGP</th><th>TxID</th><th>Status</th></tr>");
        for (String[] p : history) {
            sb.append("<tr><td>").append(p[0]).append("</td><td>").append(p[1]).append("</td><td>").append(p[2]).append("</td><td>").append(p[3]).append("</td><td>").append(p[4]).append("</td><td>").append(p[5]).append("</td></tr>");
        }
        sb.append("</table></body></html>");
        respond(ex, 200, sb.toString());
    }

    private static final String COPYRIGHT = "© 2026 Mohammed Egyptian. All rights reserved.";

    private static final String PRIVACY_EN =
            "Fast Car — Privacy Policy\n" + COPYRIGHT + "\n\n"
            + "1. Data we collect: your username, race scores, coins and garage data.\n"
            + "2. Where data is stored: on the game server you are connected to.\n"
            + "3. Ads: the app may show ads served by Google AdMob. AdMob can collect device "
            + "identifiers and usage data according to Google's Privacy Policy "
            + "(https://policies.google.com/privacy). You may disable the rewarded ad feature in-app.\n"
            + "4. We do not sell your personal data, and we do not share it except to operate the game.\n"
            + "5. You are responsible for keeping your account password private.\n"
            + "6. To delete your account and data, contact the developer at the server owner.\n"
            + "7. This policy may be updated from time to time; continued use means acceptance.\n";

    private static final String PRIVACY_AR =
            "السيارة السريعة — سياسة الخصوصية\n" + COPYRIGHT + "\n"
            + "نُحدّث هذه السياسة بتاريخ: " + java.time.LocalDate.now().toString() + "\n"
            + "================================================\n\n"
            + "١. مقدمة:\n"
            + "نحن نحترم خصوصيتك ونلتزم بحماية بياناتك الشخصية. توضح هذه السياسة بوضوح تام "
            + "ما البيانات التي نجمعها، وكيف نستخدمها، ولماذا، وما حقوقك. باستخدامك اللعبة "
            + "فأنت توافق على الممارسات الموضحة أدناه.\n\n"
            + "٢. البيانات التي نجمعها:\n"
            + "أ) اسم المستخدم وكلمة المرور (مشفّرة على الخادم).\n"
            + "ب) نتائج السباقات وترتيبك في لوحات الصعود.\n"
            + "ج) عملاتك المتاحة والممتلكات في الكراج (السيارات، الألوان، القطع، الترقيات).\n"
            + "د) تقدمك في المهام اليومية والأسبوعية والإنجازات.\n"
            + "هـ) سجل سحب عجلة الحظ وآخر وقت سحب.\n"
            + "و) ملفاتك الصوتية المنقولة داخل الغرف لتشغيل التواصل الصوتي الحي.\n"
            + "ز) رقم هاتف فودافون كاش عند شراء باقات PREMIUM طوعاً.\n\n"
            + "٣. كيف نستخدم البيانات:\n"
            + "أ) تشغيل اللعبة وتقديم الخدمات (الغرف، الصوت، المهام، المتجر).\n"
            + "ب) حفظ تقدمك بشكل دائم حتى تعود إليه في أي وقت.\n"
            + "ج) تحسين التجربة وتصحيح الأعطال.\n"
            + "د) منع الغش والحفاظ على اللعب النظيف.\n"
            + "هـ) تنفيذ عمليات الشراء التي قمت بها بنفسك.\n"
            + "و) إرسال إشعارات تحفيزية وإعلانات داخل التطبيق.\n\n"
            + "٤. مكان التخزين والحماية:\n"
            + "تُخزَّن بياناتك على خادم اللعبة الذي تتصل به، وهي محمية بكلمات مرور "
            + "مشفّرة وحدود وصول صارمة. نحن لا نشارك بياناتك مع أي طرف خارجي "
            + "باستثناء مزودي البنية التحتية (الاستضافة) وخدمة الإعلانات عند الضرورة.\n\n"
            + "٥. الإعلانات:\n"
            + "قد تعرض اللعبة إعلانات من Google AdMob. قد يجمع AdMob معرّفات الجهاز "
            + "وبيانات الاستخدام وفق سياسة خصوصية Google المتاحة على:\n"
            + "https://policies.google.com/privacy\n"
            + "يمكنك تعطيل الإعلان التحفيزي (المكافأة) من داخل التطبيق في أي وقت.\n\n"
            + "٦. الدفع والإيصالات:\n"
            + "عند شراء باقات PREMIUM عبر فودافون كاش، نجمع رقم التحويل والتاريخ "
            + "للتحقق من الدفع. لا نخزن تفاصيل بطاقتك البنكية إطلاقاً.\n\n"
            + "٧. التواصل الصوتي:\n"
            + "تُعالَج المقاطع الصوتية المتنقلة في الغرف تمريراً للمستمعين فقط ولا تُحفَظ "
            + "بشكل دائم بعد انتهاء الجلسة، ولا تُستخدم لأي غرض آخر.\n\n"
            + "٨. حقوقك:\n"
            + "أ) الوصول إلى بياناتك وطلب نسخة منها.\n"
            + "ب) تصحيح البيانات غير الدقيقة.\n"
            + "ج) طلب حذف حسابك وبياناتك نهائياً.\n"
            + "د) سحب موافقتك أو تعطيل الإعلانات في أي وقت.\n"
            + "هـ) الاعتراض على معالجة بياناتك لأغراض تسويقية.\n"
            + "لتنفيذ أي من هذه الحقوق تواصل مع المطوّر عبر وسائل التواصل الموضحة في نهاية هذا المستند.\n\n"
            + "٩. الخصوصية على الإنترنت:\n"
            + "لا يمكن ضمان أمان مطلق عبر الإنترنت، لكننا نطبق إجراءات تقنية وتنظيمية "
            + "مناسبة لحماية بياناتك من الوصول غير المصرح به.\n\n"
            + "١٠. بيانات القاصرين:\n"
            + "اللعبة غير موجهة لمن هم دون ١٣ عاماً. إذا كنت دون ١٣ عاماً فلا تستخدم "
            + "اللعبة، وإذا اكتشفنا بيانات قاصر نحذفها فوراً.\n\n"
            + "١١. التغييرات على السياسة:\n"
            + "قد نقوم بتحديث هذه السياسة من وقت لآخر لمواكبة القوانين أو المزايا. "
            + "نوّضح تاريخ آخر تحديث في أعلى الصفحة، واستمرار الاستخدام بعد التحديث "
            + "يعني موافقتك على النسخة الجديدة.\n\n"
            + "١٢. للتواصل معنا:\n"
            + "لأي استفسار حول الخصوصية أو لحذف حسابك أو ممارسة حقوقك، راسلنا عبر "
            + "الأدوات المتاحة داخل اللعبة (دعم المطوّر) أو عبر صفحة الاتصال. "
            + "نرد خلال ٧ أيام عمل كحد أقصى.\n";

    private static final String TERMS_AR =
            "السيارة السريعة — شروط الاستخدام\n" + COPYRIGHT + "\n"
            + "نُحدّث هذه الشروط بتاريخ: " + java.time.LocalDate.now().toString() + "\n"
            + "================================================\n\n"
            + "بتحميلك أو استخدامك للعبة «السيارة السريعة» فأنت توافق تلقائياً على جميع "
            + "الشروط التالية. يُرجى قراءتها بعناية قبل البدء.\n\n"
            + "١. قبول الشروط:\n"
            + "استخدامك للعبة يعني موافقتك الكاملة على هذه الشروط. إذا كنت لا توافق "
            + "على أي جزء منها، يرجى عدم استخدام اللعبة وحذفها من جهازك.\n\n"
            + "٢. الحساب والأمان:\n"
            + "أ) أنت مسؤول عن الحفاظ على سرية اسم المستخدم وكلمة المرور.\n"
            + "ب) كل نشاط يحدث من حسابك يقع تحت مسؤوليتك.\n"
            + "ج) لا يجوز مشاركة الحساب أو بيعه أو تأجيره لأي شخص آخر.\n"
            + "د) عند نسيان كلمة المرور نستعيد حسابك بعد التحقق من ملكيتك له.\n\n"
            + "٣. اللعب النظيف والسلوك:\n"
            + "أ) يحظر الغش أو استغلال الثغرات أو استخدام برامج خارجية.\n"
            + "ب) يحظر إهانة اللاعبين أو التهديد أو نشر محتوى مسيء في الدردشة أو الصوت.\n"
            + "ج) يحظر انتحال شخصية المطوّر أو الإدارة أو لاعبين آخرين.\n"
            + "د) مخالفة هذه البنود قد تؤدي إلى إنذار ثم حظر مؤقت ثم حظر دائم حسب الخطورة.\n\n"
            + "٤. المهام والإنجازات:\n"
            + "تُمنح المكافآت وفق القواعد المعروضة في اللعبة. أي محاولة لخداع نظام المهام "
            + "أو عجلة الحظ لإتمامها بطرق غير مشروعة تؤدي لحذف المكافآت المكسبة وربما الحساب.\n\n"
            + "٥. الشراء والعملات:\n"
            + "أ) العملات تُكتسب باللعب أو بالشراء عبر فودافون كاش.\n"
            + "ب) الدفع يتم طوعاً، ولا نقدم استرداداً للعملات بعد إتمام التحويل إلا في حالات "
            + "قدمت فيها مبلغاً ولم تُعطَ العملات بسبب خطأ تقني موثق.\n"
            + "ج) قد تُسترد العملات المكسبة بالغش أو تُحجز.\n\n"
            + "٦. التسعيرة والباقات:\n"
            + "أسعار باقات PREMIUM معروضة داخل المتجر وقد تتغير بإشعار مسبق. الباقة القديمة "
            + "تبقى سارية من تاريخ شرائها حتى انتهائها أو إلغائها من الإدارة بقرار صريح.\n\n"
            + "٧. حمل التطبيق والنسخ:\n"
            + "أ) يُمنع تعديل ملفات اللعبة أو فك تشفيرها أو إعادة توزيعها تجارياً.\n"
            + "ب) النسخة الرسمية هي فقط ما ينشره المطوّر على القنوات المعتمدة.\n\n"
            + "٨. توفر الخدمة:\n"
            + "نعتمد على شبكة الإنترنت وخدمات طرف ثالث؛ قد تنقطع الخدمة مؤقتاً للصيانة أو لأسباب "
            + "خارجة عن إرادتنا، ولا يتحمل المطوّر مسؤولية تعطل الاتصال أو فقدان تقدم مؤقت." + "\n\n"
            + "٩. إخلاء المسؤولية:\n"
            + "اللعبة مقدمة «كما هي» دون أي ضمانات صريحة أو ضمنية من أي نوع، بما في ذلك (على سبيل "
            + "المثال لا الحصر) ضمانات القابلية للتسويق أو الملاءمة لغرض معين أو عدم التوقف أو خلوه من الأخطاء.\n\n"
            + "١٠. تحديد المسؤولية:\n"
            + "لن يكون المطوّر مسؤولاً بأي حال عن أي أضرار مباشرة أو غير مباشرة أو عرضية أو تبعية "
            + "ناشئة عن استخدام اللعبة أو عدم القدرة على استخدامها.\n\n"
            + "١١. الملكية الفكرية:\n"
            + "جميع حقوق اللعبة، باسمها وأيقوناتها ورموزها وتصميمها ورموزها البرمجية، مملوكة "
            + "للمطوّر. لا يمنحك التحميل أي حقوق ملكية على هذه العناصر.\n\n"
            + "١٢. التعديل على الشروط:\n"
            + "نحتفظ بحق تعديل أو تحديث هذه الشروط في أي وقت. تُنشر النسخة المحدثة على الخادم "
            + "وتُعرض في هذا المستند، واستمرار الاستخدام بعد التحديث يعني قبول الشروط الجديدة.\n\n"
            + "١٣. القانون الحاكم:\n"
            + "تُفسَّر هذه الشروط وتُدار وفق قوانين الدولة التي يستضيف فيها الخادم، وتُختص "
            + "المحاكم المختصة في ذلك البلد بنظر أي نزاع.\n\n"
            + "١٤. للتواصل:\n"
            + "لأي استفسار حول هذه الشروط أو للإبلاغ عن مخالفة، تواصل مع المطوّر من داخل "
            + "اللعبة عن طريق أزرار الدعم أو عبر البيانات الموضحة في سياسة الخصوصية.\n";

    private static final String TERMS_EN =
            "Fast Car — Terms of Service\n" + COPYRIGHT + "\n\n"
            + "By using Fast Car you agree to:\n"
            + "1. The game is provided \"as is\" without warranties of any kind.\n"
            + "2. Accounts and scores exist for fair play; cheating, exploits or abusing the "
            + "server may lead to account removal.\n"
            + "3. The developer is not liable for any damages arising from the use of the game.\n"
            + "4. The developer may change, suspend or stop parts of the service at any time.\n"
            + "5. Your use must respect other players and applicable laws.\n"
            + "6. These terms may be updated; continued use means acceptance.\n";

    private static void handleDoc(HttpExchange ex) throws IOException {
        String path = ex.getHttpContext().getPath();
        String q = ex.getRequestURI().getQuery();
        boolean ar = false;
        if (q != null) {
            for (String kv : q.split("&")) {
                if (kv.toLowerCase().startsWith("lang=") && kv.substring(5).toLowerCase().startsWith("ar")) {
                    ar = true;
                }
            }
        }
        boolean privacy = path.contains("privacy");
        String text = privacy ? (ar ? PRIVACY_AR : PRIVACY_EN) : (ar ? TERMS_AR : TERMS_EN);
        respond(ex, 200, text);
    }

    private static void handleRoot(HttpExchange ex) throws IOException {
        String html = "<!DOCTYPE html><html lang='ar'><head><meta charset='utf-8'><title>Fast Car Server</title></head>"
                + "<body style='font-family:sans-serif;background:#0f2027;color:#eee;text-align:center;padding-top:40px'>"
                + "<h1>Fast Car Server is running</h1>"
                + "<pre>/register /login /score /scores /garage /earn /buy /upgrade /privacy /terms</pre>"
                + "<p style='color:#888'>" + COPYRIGHT + "</p>"
                + "<p><a href='/privacy' style='color:#a5c8ff'>Privacy Policy</a> · "
                + "<a href='/terms' style='color:#a5c8ff'>Terms of Service</a></p></body></html>";
        byte[] b = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(200, b.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(b);
        }
    }

    private static String validateCredentials(String user, String pass) {
        if (user == null || pass == null) return "أدخل اسم المستخدم وكلمة المرور";
        user = user.trim();
        if (user.isEmpty() || user.length() > 24) return "اسم المستخدم يجب ألا يتجاوز 24 حرفاً";
        if (pass.isEmpty() || pass.length() < 4 || pass.length() > 64) return "كلمة المرور يجب أن تكون من 4 إلى 64 حرفاً";
        if (!user.matches("[\\p{L}\\p{N}_ .-]+")) return "اسم المستخدم يحتوي أحرفاً غير مسموحة";
        return null;
    }

    private static void sendEmail(String to, String subject, String body) throws Exception {
        if (SMTP_USER.isEmpty() || SMTP_PASS.isEmpty()) {
            System.err.println("SMTP not configured, skipping email to " + to);
            return;
        }
        Socket socket = new Socket(SMTP_HOST, SMTP_PORT);
        socket.setSoTimeout(15000);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        in.readLine();
        out.print("EHLO fastcar.local\r\n");
        out.flush();
        skipLines(in);
        out.print("STARTTLS\r\n");
        out.flush();
        in.readLine();
        SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        Socket ssl = sslFactory.createSocket(socket, socket.getInetAddress().getHostAddress(), socket.getPort(), true);
        ssl.setSoTimeout(15000);
        in = new BufferedReader(new InputStreamReader(ssl.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(new OutputStreamWriter(ssl.getOutputStream(), StandardCharsets.UTF_8), true);
        out.print("EHLO fastcar.local\r\n");
        out.flush();
        skipLines(in);
        out.print("AUTH LOGIN\r\n");
        out.flush();
        in.readLine();
        out.print(Base64.getEncoder().encodeToString(SMTP_USER.getBytes(StandardCharsets.UTF_8)) + "\r\n");
        out.flush();
        in.readLine();
        out.print(Base64.getEncoder().encodeToString(SMTP_PASS.getBytes(StandardCharsets.UTF_8)) + "\r\n");
        out.flush();
        in.readLine();
        out.print("MAIL FROM:<" + SMTP_USER + ">\r\n");
        out.flush();
        in.readLine();
        out.print("RCPT TO:<" + to + ">\r\n");
        out.flush();
        in.readLine();
        out.print("DATA\r\n");
        out.flush();
        in.readLine();
        String encSubject = "=?UTF-8?B?" + Base64.getEncoder().encodeToString(subject.getBytes(StandardCharsets.UTF_8)) + "?=";
        String encBody = Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8));
        out.print("From: FastCar Racing <" + SMTP_USER + ">\r\n");
        out.print("To: <" + to + ">\r\n");
        out.print("Subject: " + encSubject + "\r\n");
        out.print("Content-Type: text/plain; charset=UTF-8\r\n");
        out.print("Content-Transfer-Encoding: base64\r\n");
        out.print("\r\n");
        out.print(encBody + "\r\n");
        out.print(".\r\n");
        out.flush();
        in.readLine();
        out.print("QUIT\r\n");
        out.flush();
        ssl.close();
    }

    private static void skipLines(BufferedReader in) throws IOException {
        String line;
        while ((line = in.readLine()) != null && line.length() >= 4 && line.charAt(3) == '-') {}
    }

    private static String randomPassword(int len) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(chars.charAt(RND.nextInt(chars.length())));
        return sb.toString();
    }

    private static Map<String, String[]> loadUsers() throws IOException {
        Map<String, String[]> map = new HashMap<>();
        for (String line : Files.readAllLines(USERS_FILE, StandardCharsets.UTF_8)) {
            String[] parts = line.split("\t", -1);
            if (parts.length < 3) continue;
            String name;
            try {
                name = decodeB64(parts[2]);
            } catch (Exception e) {
                continue;
            }
            String email = parts.length >= 4 ? parts[3] : "";
            String gender = parts.length >= 5 ? parts[4] : "";
            String verified = parts.length >= 6 ? parts[5] : "1";
            map.put(name, new String[]{parts[0], parts[1], parts[2], email, gender, verified});
        }
        return map;
    }

    private static void saveUsers(Map<String, String[]> users) throws IOException {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, String[]> e : users.entrySet()) {
            String[] v = e.getValue();
            String email = v.length >= 4 ? v[3] : "";
            String gender = v.length >= 5 ? v[4] : "";
            String verified = v.length >= 6 ? v[5] : "1";
            out.add(v[0] + "\t" + v[1] + "\t" + v[2] + "\t" + email + "\t" + gender + "\t" + verified);
        }
        Files.write(USERS_FILE, out, StandardCharsets.UTF_8);
    }

    private static class Wallet {
        long coins;
        Set<String> owned;
        String selected;
        Map<String, int[]> upgr;
        Map<String, Integer> partsMask;
        Map<String, Integer> colorSel;
        Map<String, Set<String>> colorsOwned;
        boolean premium;
        long premiumUntil;
        String premiumTier;
        Set<String> chOwned;
        String chSelected;

        Wallet(long coins, Set<String> owned) {
            this.coins = coins;
            this.owned = owned;
            this.upgr = new HashMap<String, int[]>();
            this.partsMask = new HashMap<String, Integer>();
            this.colorSel = new HashMap<String, Integer>();
            this.colorsOwned = new HashMap<String, Set<String>>();
            this.premium = false;
            this.premiumUntil = 0;
            this.premiumTier = null;
            this.chOwned = new LinkedHashSet<String>();
            this.chSelected = "rookie";
        }
    }

    private static Wallet newWallet() {
        Wallet w = new Wallet(0, new LinkedHashSet<String>());
        w.owned.add("sedan");
        w.selected = "sedan";
        w.colorSel.put("sedan", colorOf("sedan"));
        w.chOwned.add("rookie");
        w.chSelected = "rookie";
        return w;
    }

    private static Map<String, Map<String, String>> loadProgress() throws IOException {
        Map<String, Map<String, String>> map = new HashMap<>();
        if (!Files.exists(PROGRESS_FILE)) return map;
        List<String> lines = Files.readAllLines(PROGRESS_FILE, StandardCharsets.UTF_8);
        for (String line : lines) {
            String[] parts = line.split("\t");
            if (parts.length >= 2) {
                String user = decodeB64(parts[0]);
                Map<String, String> row = map.get(user);
                if (row == null) {
                    row = new HashMap<String, String>();
                    map.put(user, row);
                }
                row.put(parts[1], parts[2]);
            }
        }
        return map;
    }

    private static void saveProgress(Map<String, Map<String, String>> map) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> e : map.entrySet()) {
            for (Map.Entry<String, String> r : e.getValue().entrySet()) {
                lines.add(b64(e.getKey()) + "\t" + r.getKey() + "\t" + r.getValue());
            }
        }
        Files.write(PROGRESS_FILE, lines, StandardCharsets.UTF_8);
    }

    private static Map<String, Wallet> loadWallets() throws IOException {
        Map<String, Wallet> map = new HashMap<>();
        for (String line : Files.readAllLines(WALLETS_FILE, StandardCharsets.UTF_8)) {
            String[] parts = line.split("\t", -1);
            if (parts.length < 2) continue;
            try {
                String name = decodeB64(parts[0]);
                long coins = Long.parseLong(parts[1]);
                Set<String> owned = new LinkedHashSet<String>();
                if (parts.length >= 3 && !parts[2].isEmpty()) {
                    for (String k : parts[2].split(",")) {
                        if (!k.trim().isEmpty()) owned.add(k.trim());
                    }
                }
                Wallet w = new Wallet(coins, owned);
                if (parts.length >= 4 && !parts[3].trim().isEmpty()) w.selected = parts[3].trim();
                if (parts.length >= 5 && !parts[4].trim().isEmpty()) {
                    for (String e : parts[4].split(";")) {
                        String[] kv = e.split("=");
                        if (kv.length == 2) {
                            try {
                                String[] lv = kv[1].split(":");
                                w.upgr.put(kv[0], new int[]{Integer.parseInt(lv[0].trim()), Integer.parseInt(lv[1].trim())});
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
                if (parts.length >= 6 && !parts[5].trim().isEmpty()) {
                    for (String e : parts[5].split(";")) {
                        String[] kv = e.split("=");
                        if (kv.length == 2) {
                            int mask = 0;
                            for (String pp : kv[1].split(",")) {
                                int i = indexOf(PART_KEYS, pp.trim());
                                if (i >= 0) mask |= 1 << i;
                            }
                            w.partsMask.put(kv[0], mask);
                        }
                    }
                }
                if (parts.length >= 7 && !parts[6].trim().isEmpty()) {
                    for (String e : parts[6].split(";")) {
                        String[] kv = e.split("=");
                        if (kv.length == 2) {
                            String car = kv[0];
                            String[] selOwned = kv[1].split("\\|");
                            try {
                                w.colorSel.put(car, Integer.parseInt(selOwned[0].trim()));
                            } catch (Exception ignored) {
                            }
                            if (selOwned.length >= 2 && !selOwned[1].trim().isEmpty()) {
                                Set<String> s = new LinkedHashSet<String>();
                                for (String cc : selOwned[1].split(",")) {
                                    String t = cc.trim();
                                    if (!t.isEmpty()) s.add(t);
                                }
                                w.colorsOwned.put(car, s);
                            }
                        }
                    }
                }
                if (parts.length >= 8 && "1".equals(parts[7].trim())) w.premium = true;
                if (parts.length >= 9 && !parts[8].trim().isEmpty()) {
                    try { w.premiumUntil = Long.parseLong(parts[8].trim()); } catch (Exception ignored) {}
                }
                if (parts.length >= 10 && !parts[9].trim().isEmpty()) w.premiumTier = parts[9].trim();
                if (parts.length >= 11 && !parts[10].trim().isEmpty()) {
                    for (String k : parts[10].split(",")) {
                        if (!k.trim().isEmpty()) w.chOwned.add(k.trim());
                    }
                } else {
                    w.chOwned.add("rookie");
                }
                if (parts.length >= 12 && !parts[11].trim().isEmpty()) w.chSelected = parts[11].trim();
                map.put(name, w);
            } catch (Exception ignored) {
            }
        }
        return map;
    }

    private static void saveWallets(Map<String, Wallet> wallets) throws IOException {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Wallet> e : wallets.entrySet()) {
            Wallet w = e.getValue();
            StringBuilder owned = new StringBuilder();
            for (String k : w.owned) {
                if (owned.length() > 0) owned.append(",");
                owned.append(k);
            }
            StringBuilder upgr = new StringBuilder();
            for (Map.Entry<String, int[]> u : w.upgr.entrySet()) {
                int[] lv = u.getValue();
                if (lv == null || (lv[0] == 0 && lv[1] == 0)) continue;
                if (upgr.length() > 0) upgr.append(";");
                upgr.append(u.getKey()).append("=").append(lv[0]).append(":").append(lv[1]);
            }
            StringBuilder parts = new StringBuilder();
            for (Map.Entry<String, Integer> m : w.partsMask.entrySet()) {
                int mask = m.getValue();
                StringBuilder names = new StringBuilder();
                for (int i = 0; i < PART_KEYS.length; i++) {
                    if ((mask & (1 << i)) != 0) {
                        if (names.length() > 0) names.append(",");
                        names.append(PART_KEYS[i]);
                    }
                }
                if (names.length() == 0) continue;
                if (parts.length() > 0) parts.append(";");
                parts.append(m.getKey()).append("=").append(names);
            }
            StringBuilder colors = new StringBuilder();
            for (Map.Entry<String, Integer> c : w.colorSel.entrySet()) {
                String car = c.getKey();
                Set<String> own = w.colorsOwned.get(car);
                StringBuilder ownCsv = new StringBuilder();
                if (own != null) {
                    for (String cc : own) {
                        if (ownCsv.length() > 0) ownCsv.append(",");
                        ownCsv.append(cc);
                    }
                }
                if (colors.length() > 0) colors.append(";");
                colors.append(car).append("=").append(c.getValue());
                if (ownCsv.length() > 0) colors.append("|").append(ownCsv);
            }
            String prem = w.premium ? "1" : "0";
            StringBuilder chOwned = new StringBuilder();
            for (String k : w.chOwned) {
                if (chOwned.length() > 0) chOwned.append(",");
                chOwned.append(k);
            }
            if (chOwned.length() == 0) chOwned.append("rookie");
            out.add(b64(e.getKey()) + "\t" + w.coins + "\t" + owned.toString()
                    + "\t" + (w.selected == null ? "sedan" : w.selected)
                    + "\t" + upgr.toString() + "\t" + parts.toString() + "\t" + colors.toString()
                    + "\t" + prem
                    + "\t" + w.premiumUntil
                    + "\t" + (w.premiumTier == null ? "-" : w.premiumTier)
                    + "\t" + chOwned.toString()
                    + "\t" + (w.chSelected == null ? "rookie" : w.chSelected));
        }
        Files.write(WALLETS_FILE, out, StandardCharsets.UTF_8);
    }

    private static String randomSalt() {
        byte[] b = new byte[12];
        RND.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String newToken() {
        byte[] b = new byte[32];
        RND.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : d) sb.append(String.format("%02x", x & 0xff));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void loadMods() {
        try {
            if (!Files.exists(MODS_FILE)) return;
            for (String line : Files.readAllLines(MODS_FILE, StandardCharsets.UTF_8)) {
                String[] p = line.split("\t", -1);
                if (p.length < 2) continue;
                if ("bip".equals(p[0])) BANNED_IPS.add(p[1]);
                else if ("jail".equals(p[0])) {
                    try { JAILED.put(p[1], Long.parseLong(p[2])); } catch (Exception ignored) {}
                } else if ("ip".equals(p[0])) LAST_IP.put(p[1], p[2]);
            }
        } catch (Exception ignored) {
        }
    }

    private static void saveMods() {
        try {
            List<String> out = new ArrayList<>();
            for (String ip : BANNED_IPS) out.add("bip\t" + ip);
            for (Map.Entry<String, Long> e : JAILED.entrySet()) out.add("jail\t" + e.getKey() + "\t" + e.getValue());
            for (Map.Entry<String, String> e : LAST_IP.entrySet()) out.add("ip\t" + e.getKey() + "\t" + e.getValue());
            Files.write(MODS_FILE, out, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private static String remoteIp(HttpExchange ex) {
        try {
            InetSocketAddress a = ex.getRemoteAddress();
            if (a != null && a.getAddress() != null) return a.getAddress().getHostAddress();
        } catch (Exception ignored) {
        }
        return "0.0.0.0";
    }

    private static void recordIp(String user, String ip) {
        if (user == null || ip == null) return;
        LAST_IP.put(user, ip);
        saveMods();
        try {
            List<String> lines = new ArrayList<>();
            if (Files.exists(IPLOG_FILE)) lines = new ArrayList<>(Files.readAllLines(IPLOG_FILE, StandardCharsets.UTF_8));
            lines.add(System.currentTimeMillis() + "\t" + user + "\t" + ip);
            if (lines.size() > 5000) lines = lines.subList(lines.size() - 5000, lines.size());
            Files.write(IPLOG_FILE, lines, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private static boolean isJailed(String user) {
        Long until = JAILED.get(user);
        if (until == null) return false;
        if (until <= System.currentTimeMillis()) {
            JAILED.remove(user);
            saveMods();
            return false;
        }
        return true;
    }

    private static String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeB64(String s) {
        return new String(Base64.getUrlDecoder().decode(s), StandardCharsets.UTF_8);
    }

    private static Map<String, String> form(HttpExchange ex) throws IOException {
        Map<String, String> out = new HashMap<>();
        String body = readAll(ex.getRequestBody());
        if (body.isEmpty()) body = ex.getRequestURI().getRawQuery();
        if (body == null || body.isEmpty()) return out;
        for (String pair : body.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            String k = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8.name());
            String v = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8.name());
            out.put(k, v);
        }
        return out;
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] tmp = new byte[8192];
        int n;
        while ((n = in.read(tmp)) != -1) buf.write(tmp, 0, n);
        return new String(buf.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void handleVoiceSend(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) { respond(ex, 200, "ERR: الجلسة انتهت"); return; }
            String channel = f.get("channel");
            String data = f.get("data");
            if (channel == null || channel.trim().isEmpty()) channel = "global";
            channel = channel.trim();
            if (channel.length() > 40) channel = channel.substring(0, 40);
            if (data == null || data.isEmpty()) { respond(ex, 200, "ERR: فارغ"); return; }
            if (data.length() > 400000) { respond(ex, 200, "ERR: المقطع طويل"); return; }
            Map<Long, VoiceMsg> ch = VOICE.computeIfAbsent(channel, k -> new ConcurrentHashMap<>());
            long now = System.currentTimeMillis();
            VoiceMsg v = new VoiceMsg();
            v.from = user;
            v.data = data;
            ch.put(now, v);
            while (ch.size() > 20) {
                long oldest = ch.keySet().stream().min(Long::compareTo).orElse(now);
                ch.remove(oldest);
            }
            respond(ex, 200, "OK " + now);
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static void handleVoiceLatest(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) { respond(ex, 200, "ERR: الجلسة انتهت"); return; }
            String channel = f.get("channel");
            if (channel == null || channel.trim().isEmpty()) channel = "global";
            channel = channel.trim();
            long after = 0;
            String a = f.get("after");
            if (a != null) { try { after = Long.parseLong(a); } catch (Exception ignore) {} }
            Map<Long, VoiceMsg> ch = VOICE.get(channel);
            StringBuilder sb = new StringBuilder();
            if (ch != null) {
                List<Long> keys = new ArrayList<>(ch.keySet());
                java.util.Collections.sort(keys);
                for (Long k : keys) {
                    if (k > after) {
                        VoiceMsg v = ch.get(k);
                        if (sb.length() > 0) sb.append("\n");
                        sb.append(k).append("|").append(v.from).append("|").append(v.data);
                    }
                }
            }
            respond(ex, 200, sb.length() == 0 ? "(empty)" : sb.toString());
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static class VoiceMsg {
        String from;
        String data;
    }

    private static void handleTasks(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String user = userOf(ex, f);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت");
                return;
            }
            String action = f.get("action");
            String id = f.get("id");
            LOCK.lock();
            try {
                Map<String, Map<String, String>> p = loadProgress();
                Map<String, String> u = p.get(user);
                if (u == null) {
                    u = new HashMap<String, String>();
                    p.put(user, u);
                }
                if ("claim".equals(action) && id != null) {
                    String who = id.startsWith("d_") ? "d_" : "w_";
                    String[] def = findTask(id);
                    if (def == null) {
                        respond(ex, 200, "ERR: مهمة غير موجودة");
                        return;
                    }
                    String key = "t_" + id;
                    String prog = u.get(key);
                    long cur = prog == null ? 0 : Long.parseLong(prog.split("/")[0]);
                    long target = Long.parseLong(def[3]);
                    if (cur < target) {
                        respond(ex, 200, "ERR: المهمة لم تكتمل بعد");
                        return;
                    }
                    if ("1".equals(u.get("c_" + id))) {
                        respond(ex, 200, "ERR: استلمت هذه المهمة سابقاً");
                        return;
                    }
                    u.put("c_" + id, "1");
                    String rew = def[4];
                    Wallet w = loadWallets().get(user);
                    if (w == null) {
                        w = newWallet();
                    }
                    w.coins += Long.parseLong(rew);
                    Map<String, Wallet> wls = loadWallets();
                    wls.put(user, w);
                    saveWallets(wls);
                    saveProgress(p);
                    respond(ex, 200, "OK +" + rew);
                    return;
                }
                StringBuilder sb = new StringBuilder();
                for (String[] def : TASKS_DAILY) {
                    String key = "t_d_" + def[0];
                    String prog = u.get(key);
                    long cur = prog == null ? 0 : Long.parseLong(prog.split("/")[0]);
                    String claimed = u.get("c_d_" + def[0]);
                    sb.append("d_").append(def[0]).append("|").append(def[1]).append("|").append(def[2])
                            .append("|").append(cur).append("|").append(def[3]).append("|").append(def[4])
                            .append("|").append(claimed == null ? "0" : "1").append("\n");
                }
                for (String[] def : TASKS_WEEKLY) {
                    String key = "t_w_" + def[0];
                    String prog = u.get(key);
                    long cur = prog == null ? 0 : Long.parseLong(prog.split("/")[0]);
                    String claimed = u.get("c_w_" + def[0]);
                    sb.append("w_").append(def[0]).append("|").append(def[1]).append("|").append(def[2])
                            .append("|").append(cur).append("|").append(def[3]).append("|").append(def[4])
                            .append("|").append(claimed == null ? "0" : "1").append("\n");
                }
                respond(ex, 200, sb.length() == 0 ? "(empty)" : sb.toString());
            } finally {
                LOCK.unlock();
            }
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static void handleAchievements(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String user = userOf(ex, f);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت");
                return;
            }
            String action = f.get("action");
            String id = f.get("id");
            LOCK.lock();
            try {
                Map<String, Map<String, String>> p = loadProgress();
                Map<String, String> u = p.get(user);
                if (u == null) {
                    u = new HashMap<String, String>();
                    p.put(user, u);
                }
                if ("claim".equals(action) && id != null) {
                    String[] def = findAch(id);
                    if (def == null) {
                        respond(ex, 200, "ERR: إنجاز غير موجود");
                        return;
                    }
                    String key = "a_" + id;
                    String prog = u.get(key);
                    long cur = prog == null ? 0 : Long.parseLong(prog.split("/")[0]);
                    if (cur < Long.parseLong(def[3])) {
                        respond(ex, 200, "ERR: الإنجاز لم يكتمل بعد");
                        return;
                    }
                    if ("1".equals(u.get("ca_" + id))) {
                        respond(ex, 200, "ERR: استلمت هذا الإنجاز سابقاً");
                        return;
                    }
                    u.put("ca_" + id, "1");
                    Wallet w = loadWallets().get(user);
                    if (w == null) {
                        w = newWallet();
                    }
                    w.coins += Long.parseLong(def[4]);
                    Map<String, Wallet> wls = loadWallets();
                    wls.put(user, w);
                    saveWallets(wls);
                    saveProgress(p);
                    respond(ex, 200, "OK +" + def[4]);
                    return;
                }
                StringBuilder sb = new StringBuilder();
                for (String[] def : ACH_DEFS) {
                    String key = "a_" + def[0];
                    String prog = u.get(key);
                    long cur = prog == null ? 0 : Long.parseLong(prog.split("/")[0]);
                    String claimed = u.get("ca_" + def[0]);
                    sb.append(def[0]).append("|").append(def[1]).append("|").append(def[2])
                            .append("|").append(cur).append("|").append(def[3]).append("|").append(def[4])
                            .append("|").append(claimed == null ? "0" : "1").append("\n");
                }
                respond(ex, 200, sb.length() == 0 ? "(empty)" : sb.toString());
            } finally {
                LOCK.unlock();
            }
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static void handleWheel(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String user = userOf(ex, f);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت");
                return;
            }
            LOCK.lock();
            try {
                Map<String, Map<String, String>> p = loadProgress();
                Map<String, String> u = p.get(user);
                if (u == null) {
                    u = new HashMap<String, String>();
                    p.put(user, u);
                }
                long last = 0;
                try {
                    last = Long.parseLong(u.get("wheel"));
                } catch (Exception ignored) {
                }
                long now = System.currentTimeMillis();
                long left = WHEEL_COOLDOWN_MS - (now - last);
                if (left > 0) {
                    respond(ex, 200, "ERR: حاول بعد " + (left / 60000) + " دقيقة");
                    return;
                }
                String egp = WHEEL_REWARDS_EGP[RND.nextInt(WHEEL_REWARDS_EGP.length)];
                long coins = Long.parseLong(egp);
                u.put("wheel", String.valueOf(now));
                if (coins > 0) {
                    Wallet w = loadWallets().get(user);
                    if (w == null) {
                        w = newWallet();
                    }
                    w.coins += coins;
                    if (isPremiumActive(w)) w.coins += coins / 2;
                    if (hasCharacterSkill(w, "wheel")) w.coins += coins / 5;
                    Map<String, Wallet> wls = loadWallets();
                    wls.put(user, w);
                    saveWallets(wls);
                }
                saveProgress(p);
                respond(ex, 200, "OK " + coins);
            } finally {
                LOCK.unlock();
            }
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static void handleTool(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String user = userOf(ex, f);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت");
                return;
            }
            String id = f.get("id");
            String cost = f.get("cost");
            if (id == null) {
                respond(ex, 200, "shield|درع حماية|50\nemp|نبضة كهرومغناطيسية|40\ndrone|طائرة إصلاح|60\nnitro|عبوة نترو|30\nrepair|عدة إصلاح|45\nfuel|خزان وقود|20");
                return;
            }
            long price;
            try {
                price = Long.parseLong(cost == null ? "0" : cost);
            } catch (NumberFormatException ignored) {
                price = 0;
            }
            LOCK.lock();
            try {
                Map<String, Wallet> wls = loadWallets();
                Wallet w = wls.get(user);
                if (w == null) {
                    w = newWallet();
                    wls.put(user, w);
                }
                if (w.coins < price) {
                    respond(ex, 200, "ERR: عملات غير كافية");
                    return;
                }
                w.coins -= price;
                Map<String, Map<String, String>> p = loadProgress();
                Map<String, String> u = p.get(user);
                if (u == null) {
                    u = new HashMap<String, String>();
                    p.put(user, u);
                }
                String key = "tool_" + id;
                String prog = u.get(key);
                long count = prog == null ? 0 : Long.parseLong(prog);
                u.put(key, String.valueOf(count + 1));
                saveWallets(wls);
                saveProgress(p);
                respond(ex, 200, "OK " + w.coins + " " + (count + 1));
            } finally {
                LOCK.unlock();
            }
        } catch (Throwable t) {
            respond(ex, 500, "ERR: خطأ داخلي");
        }
    }

    private static void handleCharacters(HttpExchange ex) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String[] c : CHARACTERS) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(c[0]).append("|").append(c[1]).append("|").append(c[2])
              .append("|").append(c[3]).append("|").append(c[4]);
        }
        respond(ex, 200, sb.toString());
    }

    private static void handleCharacterBuy(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت");
                return;
            }
            String id = f.get("id");
            if (id == null || id.trim().isEmpty()) {
                respond(ex, 200, "ERR: اسم الشخصية ناقص");
                return;
            }
            String[] found = null;
            for (String[] c : CHARACTERS) if (c[0].equals(id.trim())) { found = c; break; }
            if (found == null) {
                respond(ex, 200, "ERR: شخصية غير معروفة");
                return;
            }
            long coinsPrice = Long.parseLong(found[3]);
            long egp = Long.parseLong(found[4]);
            LOCK.lock();
            try {
                Map<String, Wallet> wls = loadWallets();
                Wallet w = wls.get(user);
                if (w == null) { w = newWallet(); wls.put(user, w); }
                if (w.chOwned.contains(found[0])) {
                    respond(ex, 200, "ERR: تمتلك هذه الشخصية بالفعل");
                    return;
                }
                if (egp > 0) {
                    String txid = f.get("txid");
                    if (txid == null || txid.trim().isEmpty()) {
                        respond(ex, 200, "ERR: هذه الشخصية تُشرى بالمقابل عبر فودافون كاش (أدخل رقم العملية)");
                        return;
                    }
                    String pid = System.currentTimeMillis() + "_ch_" + user.substring(0, Math.min(4, user.length()));
                    String line = pid + "\t" + user + "\t0\t" + egp + "\t" + txid.trim() + "\tchar_" + found[0];
                    Path pf = USERS_FILE.getParent().resolve("payments.txt");
                    List<String> lines = new ArrayList<>(Files.readAllLines(pf, StandardCharsets.UTF_8));
                    lines.add(line);
                    Files.write(pf, lines, StandardCharsets.UTF_8);
                    respond(ex, 200, "OK طلب شراء الشخصية " + found[1] + " قيد المراجعة عبر فودافون كاش");
                    return;
                }
                if (w.coins < coinsPrice) {
                    respond(ex, 200, "ERR: رصيدك لا يكفي - تحتاج " + coinsPrice + " عملة");
                    return;
                }
                w.coins -= coinsPrice;
                w.chOwned.add(found[0]);
                saveWallets(wls);
                respond(ex, 200, "OK " + w.coins + " " + found[0]);
            } finally { LOCK.unlock(); }
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleCharacterSelect(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت");
                return;
            }
            String id = f.get("id");
            if (id == null || id.trim().isEmpty()) {
                respond(ex, 200, "ERR: اسم الشخصية ناقص");
                return;
            }
            String[] found = null;
            for (String[] c : CHARACTERS) if (c[0].equals(id.trim())) { found = c; break; }
            if (found == null) {
                respond(ex, 200, "ERR: شخصية غير معروفة");
                return;
            }
            LOCK.lock();
            try {
                Map<String, Wallet> wls = loadWallets();
                Wallet w = wls.get(user);
                if (w == null) { w = newWallet(); wls.put(user, w); }
                if (!w.chOwned.contains(found[0])) {
                    respond(ex, 200, "ERR: لا تملك هذه الشخصية بعد");
                    return;
                }
                w.chSelected = found[0];
                saveWallets(wls);
                respond(ex, 200, "OK " + found[0]);
            } finally { LOCK.unlock(); }
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static void handleCharacterGrant(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String dev = devAuth(ex, f);
            if (dev == null) { respond(ex, 200, "ERR: ليس لديك صلاحيات مطور"); return; }
            String target = f.get("user");
            String id = f.get("id");
            if (target == null || id == null) { respond(ex, 200, "ERR: بيانات ناقصة"); return; }
            String[] found = null;
            for (String[] c : CHARACTERS) if (c[0].equals(id.trim())) { found = c; break; }
            if (found == null) { respond(ex, 200, "ERR: شخصية غير معروفة"); return; }
            LOCK.lock();
            try {
                Map<String, Wallet> wls = loadWallets();
                Wallet w = wls.get(target.trim());
                if (w == null) { respond(ex, 200, "ERR: المستخدم غير موجود"); return; }
                w.chOwned.add(found[0]);
                saveWallets(wls);
            } finally { LOCK.unlock(); }
            respond(ex, 200, "OK مُنحت " + target + " شخصية " + found[1]);
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static boolean hasCharacterSkill(Wallet w, String skill) {
        String sel = w.chSelected == null ? "rookie" : w.chSelected;
        if ("rookie".equals(sel)) return false;
        if ("speedy".equals(sel) || "turbo".equals(sel))
            return ("speed".equals(skill) || "accel".equals(skill));
        if ("lucky".equals(sel)) return "wheel".equals(skill);
        if ("champ".equals(sel)) return "score".equals(skill);
        if ("veteran".equals(sel)) return "earn".equals(skill);
        if ("legend".equals(sel)) return true;
        return false;
    }

    private static void handleSelectedCharacter(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = form(ex);
            String token = bearer(ex, f);
            String user = token == null ? null : TOKENS.get(token);
            if (user == null) {
                respond(ex, 200, "ERR: الجلسة انتهت");
                return;
            }
            Wallet w = loadWallets().get(user);
            String sel = (w == null || w.chSelected == null) ? "rookie" : w.chSelected;
            respond(ex, 200, sel);
        } catch (Throwable t) { respond(ex, 500, "ERR: خطأ داخلي"); }
    }

    private static String[] findTask(String id) {
        for (String[] def : TASKS_DAILY) {
            if (("d_" + def[0]).equals(id)) return def;
        }
        for (String[] def : TASKS_WEEKLY) {
            if (("w_" + def[0]).equals(id)) return def;
        }
        return null;
    }

    private static String[] findAch(String id) {
        for (String[] def : ACH_DEFS) {
            if (def[0].equals(id)) return def;
        }
        return null;
    }

    private static String userOf(HttpExchange ex, Map<String, String> f) {
        String token = bearer(ex, f);
        String u = token == null ? null : TOKENS.get(token);
        if (u == null || isJailed(u)) return null;
        return u;
    }

    private static void respond(HttpExchange ex, int code, String text) throws IOException {
        byte[] b = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(b);
        }
    }
}
