package com.fastcar.racing;

import android.content.Context;
import android.content.SharedPreferences;

import com.fastcar.common.GarageData;
import com.fastcar.common.L10n;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public final class Api {

    public static final String DEFAULT_SERVER_URL = "https://fastcar-7ofz.onrender.com";

    public static String token = "";
    public static String username = "";

    private static final String PREFS = "fastcar";
    private static final String KEY_USER = "username";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_SERVER = "server";
    private static final String KEY_LANG = "lang";
    private static final String KEY_GARAGE = "garage";

    private Api() {
    }

    public static void loadSession(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        token = p.getString(KEY_TOKEN, "");
        username = p.getString(KEY_USER, "");
    }

    public static void saveSession(Context c) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USER, username)
                .apply();
    }

    public static void clearSession(Context c) {
        token = "";
        username = "";
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TOKEN, "")
                .putString(KEY_USER, "")
                .apply();
    }

    public static String getServer(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String s = p.getString(KEY_SERVER, DEFAULT_SERVER_URL);
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    public static void setServer(Context c, String url) {
        String s = url == null ? "" : url.trim();
        if (s.isEmpty()) s = DEFAULT_SERVER_URL;
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_SERVER, s).apply();
    }

    public static String getLang(Context c) {
        String l = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANG, "");
        return L10n.normalize(l.isEmpty() ? L10n.auto() : l);
    }

    public static void setLang(Context c, String code) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LANG, code).apply();
    }

    public static void cacheGarage(Context c, String raw) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_GARAGE, raw == null ? "" : raw).apply();
    }

    public static GarageData cachedGarage(Context c) {
        String raw = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_GARAGE, "");
        return GarageData.parse(raw.isEmpty() ? null : raw);
    }

    public static String login(Context c, String user, String pass) throws Exception {
        String body = "user=" + enc(user) + "&pass=" + enc(pass);
        String res = post(c, "/login", body);
        if (res.startsWith("OK ")) {
            token = res.substring(3).trim();
            username = user;
            saveSession(c);
        }
        return res;
    }

    public static String register(Context c, String user, String pass, String email, String gender) throws Exception {
        String body = "user=" + enc(user) + "&pass=" + enc(pass) + "&email=" + enc(email) + "&gender=" + enc(gender);
        return post(c, "/register", body);
    }

    public static String checkUser(Context c, String user) throws Exception {
        return post(c, "/register/check-user", "user=" + enc(user));
    }

    public static String checkEmail(Context c, String email) throws Exception {
        return post(c, "/register/check-email", "email=" + enc(email));
    }

    public static String verifyEmail(Context c, String email, String code) throws Exception {
        return post(c, "/verify-email", "email=" + enc(email) + "&code=" + enc(code));
    }

    public static String forgotPassword(Context c, String user, String email) throws Exception {
        return post(c, "/forgot-password", "user=" + enc(user) + "&email=" + enc(email));
    }

    public static String submitScore(Context c, int score) throws Exception {
        return post(c, "/score", "score=" + score);
    }

    public static String topScores(Context c) throws Exception {
        return get(c, "/scores");
    }

    public static String earn(Context c, long coins) throws Exception {
        return post(c, "/earn", "coins=" + coins);
    }

    public static String buy(Context c, String car) throws Exception {
        return post(c, "/buy", "car=" + enc(car));
    }

    public static String upgrade(Context c, String car, String part) throws Exception {
        return post(c, "/upgrade", "car=" + enc(car) + "&part=" + enc(part));
    }

    public static String buyPart(Context c, String car, String part) throws Exception {
        return post(c, "/buypart", "car=" + enc(car) + "&part=" + enc(part));
    }

    public static String buyColor(Context c, String car, int color) throws Exception {
        return post(c, "/buycolor", "car=" + enc(car) + "&color=" + color);
    }

    public static String selectCar(Context c, String car, int color) throws Exception {
        String body = "car=" + enc(car);
        if (color != Integer.MIN_VALUE) body += "&color=" + color;
        return post(c, "/select", body);
    }

    public static String garageRaw(Context c) throws Exception {
        return get(c, "/garage");
    }

    public static GarageData garageData(Context c) throws Exception {
        return GarageData.parse(garageRaw(c));
    }

    public static String doc(Context c, String doc, String lang) throws Exception {
        return request(c, "/" + doc + "?lang=" + lang, null, false);
    }

    public static String store(Context c) throws Exception {
        return get(c, "/store");
    }

    public static String payRequest(Context c, int pkg, String txid) throws Exception {
        return post(c, "/pay-request", "package=" + pkg + "&txid=" + enc(txid));
    }

    public static String premiumRequest(Context c, String txid) throws Exception {
        return post(c, "/premium", "txid=" + enc(txid));
    }

    public static String roomList(Context c) throws Exception {
        return get(c, "/room-list");
    }

    public static String roomCreate(Context c, String name, String pass) throws Exception {
        return post(c, "/room-create", "name=" + enc(name) + "&pass=" + enc(pass == null ? "" : pass));
    }

    public static String roomJoin(Context c, String code, String pass) throws Exception {
        return post(c, "/room-join", "code=" + enc(code) + "&pass=" + enc(pass == null ? "" : pass));
    }

    public static String roomRandom(Context c) throws Exception {
        return post(c, "/room-random", "");
    }

    public static String roomInvite(Context c, String user) throws Exception {
        return post(c, "/room/invite", "user=" + enc(user));
    }

    public static String roomPromote(Context c, String user) throws Exception {
        return post(c, "/room/promote", "user=" + enc(user));
    }

    public static String roomMute(Context c, String user) throws Exception {
        return post(c, "/room/mute", "user=" + enc(user));
    }

    public static String roomUnmute(Context c, String user) throws Exception {
        return post(c, "/room/unmute", "user=" + enc(user));
    }

    public static String roomKick(Context c, String user) throws Exception {
        return post(c, "/room/kick", "user=" + enc(user));
    }

    public static String roomBan(Context c, String user) throws Exception {
        return post(c, "/room/ban", "user=" + enc(user));
    }

    public static String roomUnban(Context c, String user) throws Exception {
        return post(c, "/room/unban", "user=" + enc(user));
    }

    public static String roomTransfer(Context c, String user) throws Exception {
        return post(c, "/room/transfer", "user=" + enc(user));
    }

    public static String roomReport(Context c, String user) throws Exception {
        return post(c, "/room/report", "user=" + enc(user));
    }

    public static String roomConfig(Context c, String name, String pass, int max) throws Exception {
        return post(c, "/room/config", "name=" + enc(name == null ? "" : name) + "&pass=" + enc(pass == null ? "" : pass) + "&max=" + max);
    }

    public static String devStatus(Context c) throws Exception {
        return get(c, "/dev-status");
    }

    public static String devActivate(Context c, String pass) throws Exception {
        return post(c, "/dev/activate", "pass=" + enc(pass));
    }

    public static String devGrant(Context c, String user, int coins) throws Exception {
        return post(c, "/dev/grant", "user=" + enc(user) + "&coins=" + coins);
    }

    public static String devBan(Context c, String user) throws Exception {
        return post(c, "/dev/ban", "user=" + enc(user));
    }

    public static String devSetCoins(Context c, String user, long coins) throws Exception {
        return post(c, "/dev/set-coins", "user=" + enc(user) + "&coins=" + coins);
    }

    public static String devReset(Context c, String user) throws Exception {
        return post(c, "/dev/reset", "user=" + enc(user));
    }

    public static String devPremiumGrant(Context c, String user, String tier) throws Exception {
        return post(c, "/dev/premium-grant", "user=" + enc(user) + "&tier=" + enc(tier));
    }

    public static String devAnnounce(Context c, String msg) throws Exception {
        return post(c, "/dev/announce", "msg=" + enc(msg));
    }

    public static String devStats(Context c) throws Exception {
        return post(c, "/dev/stats", "");
    }

    public static String devUsers(Context c) throws Exception {
        return post(c, "/dev/users", "");
    }

    public static String devFlashSale(Context c, String item, double discount, long minutes) throws Exception {
        return post(c, "/dev/flash-sale", "item=" + enc(item) + "&discount=" + discount + "&minutes=" + minutes);
    }

    public static String premiumTiers(Context c) throws Exception {
        return get(c, "/premium-tiers");
    }

    public static String storeItems(Context c) throws Exception {
        return get(c, "/store-items");
    }

    public static String announcement(Context c) throws Exception {
        return get(c, "/announcement");
    }

    public static String tutorial(Context c) throws Exception {
        return get(c, "/tutorial");
    }

    public static String buyItem(Context c, String item) throws Exception {
        return post(c, "/buy-item", "item=" + enc(item));
    }

    public static String premiumTrial(Context c) throws Exception {
        return post(c, "/premium-trial", "");
    }

    public static String characters(Context c) throws Exception {
        return get(c, "/characters");
    }

    public static String characterBuy(Context c, String id) throws Exception {
        return post(c, "/character-buy", "id=" + enc(id));
    }

    public static String characterBuyCash(Context c, String id, String txid) throws Exception {
        return post(c, "/character-buy", "id=" + enc(id) + "&txid=" + enc(txid));
    }

    public static String characterSelect(Context c, String id) throws Exception {
        return post(c, "/character-select", "id=" + enc(id));
    }

    public static String selectedCharacter(Context c) throws Exception {
        return get(c, "/selected-character");
    }

    public static String devJail(Context c, String user, long minutes) throws Exception {
        return post(c, "/dev/jail", "user=" + enc(user) + "&minutes=" + minutes);
    }

    public static String devUnjail(Context c, String user) throws Exception {
        return post(c, "/dev/jail", "user=" + enc(user) + "&set=0");
    }

    public static String devBanIp(Context c, String ip) throws Exception {
        return post(c, "/dev/ban-ip", "ip=" + enc(ip));
    }

    public static String devUnbanIp(Context c, String ip) throws Exception {
        return post(c, "/dev/ban-ip", "ip=" + enc(ip) + "&set=0");
    }

    public static String devSearch(Context c, String q) throws Exception {
        return post(c, "/dev/search", "q=" + enc(q));
    }

    public static String devGive(Context c, String user, String type, String id) throws Exception {
        return post(c, "/dev/give", "user=" + enc(user) + "&type=" + enc(type) + "&id=" + enc(id));
    }

    public static String devBackup(Context c) throws Exception {
        return post(c, "/dev/backup", "");
    }

    public static String devIpLog(Context c, String user) throws Exception {
        return post(c, "/dev/iplog", "user=" + enc(user));
    }

    private static String get(Context c, String path) throws Exception {
        return request(c, path, null, false);
    }

    private static String post(Context c, String path, String body) throws Exception {
        return request(c, path, body, true);
    }

    private static String request(Context c, String path, String body, boolean doPost) throws Exception {
        URL url = new URL(getServer(c) + path);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(20000);
        con.setReadTimeout(120000);
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

    public static String voiceSend(Context c, String channel, String data) throws Exception {
        return post(c, "/voice/send", "channel=" + enc(channel) + "&data=" + enc(data));
    }

    public static String voiceLatest(Context c, String channel, long after) throws Exception {
        return post(c, "/voice/latest", "channel=" + enc(channel) + "&after=" + after);
    }

    public static String roomState(Context c) throws Exception {
        return post(c, "/room/state", "");
    }

    public static String tasks(Context c) throws Exception {
        return post(c, "/tasks", "");
    }

    public static String taskClaim(Context c, String id) throws Exception {
        return post(c, "/tasks", "action=claim&id=" + enc(id));
    }

    public static String achievements(Context c) throws Exception {
        return post(c, "/achievements", "");
    }

    public static String achievementClaim(Context c, String id) throws Exception {
        return post(c, "/achievements", "action=claim&id=" + enc(id));
    }

    public static String wheelSpin(Context c) throws Exception {
        return post(c, "/wheel", "");
    }

    public static String toolList(Context c) throws Exception {
        return post(c, "/tool", "");
    }

    public static String toolBuy(Context c, String id, long cost) throws Exception {
        return post(c, "/tool", "id=" + enc(id) + "&cost=" + cost);
    }
}