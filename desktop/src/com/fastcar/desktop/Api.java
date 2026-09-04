package com.fastcar.desktop;

import com.fastcar.common.GarageData;
import com.fastcar.common.L10n;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public final class Api {

    public static String token = "";
    public static String username = "";
    private static String langCache = "";

    private Api() {
    }

    public static void load() {
        token = Store.get("token", "");
        username = Store.get("username", "");
    }

    public static void save() {
        Store.put("token", token);
        Store.put("username", username);
    }

    public static void logout() {
        token = "";
        username = "";
        save();
    }

    public static String server() {
        String s = Store.get("server", Store.DEFAULT_SERVER);
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    public static void setServer(String url) {
        String s = url == null ? "" : url.trim();
        if (s.isEmpty()) s = Store.DEFAULT_SERVER;
        Store.put("server", s);
    }

    public static String lang() {
        String l = Store.get("lang", "");
        if (l.isEmpty()) l = L10n.auto();
        l = L10n.normalize(l);
        if (!langCache.equals(l)) langCache = l;
        return langCache;
    }

    public static void setLang(String code) {
        langCache = L10n.normalize(code);
        Store.put("lang", langCache);
    }

    public static String login(String user, String pass) throws Exception {
        String body = "user=" + enc(user) + "&pass=" + enc(pass);
        String res = post("/login", body);
        if (res.startsWith("OK ")) {
            token = res.substring(3).trim();
            username = user;
            save();
        }
        return res;
    }

    public static String register(String user, String pass, String email, String gender) throws Exception {
        String body = "user=" + enc(user) + "&pass=" + enc(pass) + "&email=" + enc(email) + "&gender=" + enc(gender);
        return post("/register", body);
    }

    public static String checkUser(String user) throws Exception {
        return post("/register/check-user", "user=" + enc(user));
    }

    public static String checkEmail(String email) throws Exception {
        return post("/register/check-email", "email=" + enc(email));
    }

    public static String verifyEmail(String email, String code) throws Exception {
        return post("/verify-email", "email=" + enc(email) + "&code=" + enc(code));
    }

    public static String forgotPassword(String user, String email) throws Exception {
        return post("/forgot-password", "user=" + enc(user) + "&email=" + enc(email));
    }

    public static String submitScore(int score) throws Exception {
        return post("/score", "score=" + score);
    }

    public static String topScores() throws Exception {
        return get("/scores");
    }

    public static String earn(long coins) throws Exception {
        return post("/earn", "coins=" + coins);
    }

    public static String buy(String car) throws Exception {
        return post("/buy", "car=" + enc(car));
    }

    public static String upgrade(String car, String part) throws Exception {
        return post("/upgrade", "car=" + enc(car) + "&part=" + enc(part));
    }

    public static String buyPart(String car, String part) throws Exception {
        return post("/buypart", "car=" + enc(car) + "&part=" + enc(part));
    }

    public static String buyColor(String car, int color) throws Exception {
        return post("/buycolor", "car=" + enc(car) + "&color=" + color);
    }

    public static String selectCar(String car, int color) throws Exception {
        String body = "car=" + enc(car);
        if (color != Integer.MIN_VALUE) body += "&color=" + color;
        return post("/select", body);
    }

    public static GarageData garage() throws Exception {
        return GarageData.parse(get("/garage"));
    }

    public static String doc(String doc, String lang) throws Exception {
        return request("/" + doc + "?lang=" + lang, null, false);
    }

    public static String store() throws Exception {
        return get("/store");
    }

    public static String payRequest(int pkg, String txid) throws Exception {
        return post("/pay-request", "package=" + pkg + "&txid=" + enc(txid));
    }

    public static String premiumRequest(String txid) throws Exception {
        return post("/premium", "txid=" + enc(txid));
    }

    public static String roomCreate(String name, String pass) throws Exception {
        return post("/room-create", "name=" + enc(name) + "&pass=" + enc(pass == null ? "" : pass));
    }

    public static String roomJoin(String code, String pass) throws Exception {
        return post("/room-join", "code=" + enc(code) + "&pass=" + enc(pass == null ? "" : pass));
    }

    public static String roomLeave() throws Exception {
        return post("/room-leave", "");
    }

    public static String roomState() throws Exception {
        return get("/room-state");
    }

    public static String roomStart() throws Exception {
        return post("/room-start", "");
    }

    public static String roomScore(int score) throws Exception {
        return post("/room-score", "score=" + score);
    }

    public static String roomRandom() throws Exception {
        return post("/room-random", "");
    }

    public static String roomList() throws Exception {
        return get("/room-list");
    }

    public static String roomInvite(String user) throws Exception {
        return post("/room/invite", "user=" + enc(user));
    }

    public static String roomPromote(String user) throws Exception {
        return post("/room/promote", "user=" + enc(user));
    }

    public static String roomMute(String user) throws Exception {
        return post("/room/mute", "user=" + enc(user));
    }

    public static String roomUnmute(String user) throws Exception {
        return post("/room/unmute", "user=" + enc(user));
    }

    public static String roomKick(String user) throws Exception {
        return post("/room/kick", "user=" + enc(user));
    }

    public static String roomBan(String user) throws Exception {
        return post("/room/ban", "user=" + enc(user));
    }

    public static String roomUnban(String user) throws Exception {
        return post("/room/unban", "user=" + enc(user));
    }

    public static String roomTransfer(String user) throws Exception {
        return post("/room/transfer", "user=" + enc(user));
    }

    public static String roomReport(String user) throws Exception {
        return post("/room/report", "user=" + enc(user));
    }

    public static String roomConfig(String name, String pass, int max) throws Exception {
        return post("/room/config", "name=" + enc(name == null ? "" : name) + "&pass=" + enc(pass == null ? "" : pass) + "&max=" + max);
    }

    public static String devStatus() throws Exception {
        return get("/dev-status");
    }

    public static String devActivate(String pass) throws Exception {
        return post("/dev/activate", "pass=" + enc(pass));
    }

    public static String devGrant(String user, int coins) throws Exception {
        return post("/dev/grant", "user=" + enc(user) + "&coins=" + coins);
    }

public static String devBan(String user) throws Exception {
        return post("/dev/ban", "user=" + enc(user));
    }

    public static String devSetCoins(String user, long coins) throws Exception {
        return post("/dev/set-coins", "user=" + enc(user) + "&coins=" + coins);
    }

    public static String devReset(String user) throws Exception {
        return post("/dev/reset", "user=" + enc(user));
    }

    public static String devPremiumGrant(String user, String tier) throws Exception {
        return post("/dev/premium-grant", "user=" + enc(user) + "&tier=" + enc(tier));
    }

    public static String devAnnounce(String msg) throws Exception {
        return post("/dev/announce", "msg=" + enc(msg));
    }

    public static String devStats() throws Exception {
        return post("/dev/stats", "");
    }

    public static String devUsers() throws Exception {
        return post("/dev/users", "");
    }

    public static String devFlashSale(String item, double discount, long minutes) throws Exception {
        return post("/dev/flash-sale", "item=" + enc(item) + "&discount=" + discount + "&minutes=" + minutes);
    }

    public static String premiumTiers() throws Exception {
        return get("/premium-tiers");
    }

    public static String storeItems() throws Exception {
        return get("/store-items");
    }

    public static String tutorial() throws Exception {
        return get("/tutorial");
    }

    public static String buyItem(String item) throws Exception {
        return post("/buy-item", "item=" + enc(item));
    }

    public static String premiumTrial() throws Exception {
        return post("/premium-trial", "");
    }


    public static String voiceSend(String channel, String data) throws Exception {
        return post("/voice/send", "channel=" + enc(channel) + "&data=" + enc(data));
    }

    public static String voiceLatest(String channel, long after) throws Exception {
        return get("/voice/latest?channel=" + enc(channel) + "&after=" + after);
    }
    public static String tasks() throws Exception {
        return post("/tasks", "");
    }

    public static String taskClaim(String id) throws Exception {
        return post("/tasks", "action=claim&id=" + enc(id));
    }

    public static String achievements() throws Exception {
        return post("/achievements", "");
    }

    public static String achievementClaim(String id) throws Exception {
        return post("/achievements", "action=claim&id=" + enc(id));
    }

    public static String wheelSpin() throws Exception {
        return post("/wheel", "");
    }

    public static String toolList() throws Exception {
        return post("/tool", "");
    }

    public static String toolBuy(String id, long cost) throws Exception {
        return post("/tool", "id=" + enc(id) + "&cost=" + cost);
    }

    public static String characters() throws Exception {
        return get("/characters");
    }

    public static String characterBuy(String id) throws Exception {
        return post("/character-buy", "id=" + enc(id));
    }

    public static String characterBuyCash(String id, String txid) throws Exception {
        return post("/character-buy", "id=" + enc(id) + "&txid=" + enc(txid));
    }

    public static String characterSelect(String id) throws Exception {
        return post("/character-select", "id=" + enc(id));
    }

    public static String selectedCharacter() throws Exception {
        return get("/selected-character");
    }

    public static String devJail(String user, long minutes) throws Exception {
        return post("/dev/jail", "user=" + enc(user) + "&minutes=" + minutes);
    }

    public static String devUnjail(String user) throws Exception {
        return post("/dev/jail", "user=" + enc(user) + "&set=0");
    }

    public static String devBanIp(String ip) throws Exception {
        return post("/dev/ban-ip", "ip=" + enc(ip));
    }

    public static String devUnbanIp(String ip) throws Exception {
        return post("/dev/ban-ip", "ip=" + enc(ip) + "&set=0");
    }

    public static String devSearch(String q) throws Exception {
        return post("/dev/search", "q=" + enc(q));
    }

    public static String devGive(String user, String type, String id) throws Exception {
        return post("/dev/give", "user=" + enc(user) + "&type=" + enc(type) + "&id=" + enc(id));
    }

    public static String devBackup() throws Exception {
        return post("/dev/backup", "");
    }

    public static String devIpLog(String user) throws Exception {
        return post("/dev/iplog", "user=" + enc(user));
    }

    private static String get(String path) throws Exception {
        return request(path, null, false);
    }

    private static String post(String path, String body) throws Exception {
        return request(path, body, true);
    }

    private static String request(String path, String body, boolean doPost) throws Exception {
        URL url = new URL(server() + path);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(8000);
        con.setReadTimeout(15000);
        con.setRequestProperty("Accept-Charset", "UTF-8");
        if (!token.isEmpty()) {
            con.setRequestProperty("Authorization", "Bearer " + token);
        }
        if (doPost) {
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            byte[] data = body == null ? new byte[0] : body.getBytes("UTF-8");
            con.setFixedLengthStreamingMode(data.length);
            OutputStream os = con.getOutputStream();
            os.write(data);
            os.close();
        }
        int code = con.getResponseCode();
        InputStream in = code >= 400 ? con.getErrorStream() : con.getInputStream();
        String out = readUtf8(in);
        con.disconnect();
        return out;
    }

    private static String readUtf8(InputStream in) throws Exception {
        if (in == null) return "";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        return new String(bos.toByteArray(), "UTF-8");
    }

    private static String enc(String s) throws Exception {
        return URLEncoder.encode(s == null ? "" : s, "UTF-8");
    }
}