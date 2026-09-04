package com.fastcar.desktop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public final class Store {

    public static final String DEFAULT_SERVER = "http://127.0.0.1:8080";

    private static File file() {
        return new File(System.getProperty("user.home"), ".fastcar.properties");
    }

    private static Properties load() {
        Properties p = new Properties();
        try {
            File f = file();
            if (f.exists()) {
                FileInputStream in = new FileInputStream(f);
                try {
                    p.load(in);
                } finally {
                    in.close();
                }
            }
        } catch (Exception e) {
        }
        return p;
    }

    private static void save(Properties p) {
        try {
            FileOutputStream out = new FileOutputStream(file());
            try {
                p.store(out, "FastCar");
            } finally {
                out.close();
            }
        } catch (Exception e) {
        }
    }

    public static String get(String key, String def) {
        return load().getProperty(key, def);
    }

    public static void put(String key, String val) {
        Properties p = load();
        if (val == null) p.remove(key);
        else p.setProperty(key, val);
        save(p);
    }
}