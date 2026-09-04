package com.fastcar.common;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class GarageData {

    public long coins;
    public String selected = "sedan";
    public final Set<String> owned = new LinkedHashSet<String>();
    public final Map<String, int[]> upgr = new HashMap<String, int[]>();
    public final Map<String, Integer> partsMask = new HashMap<String, Integer>();
    public final Map<String, Integer> colorSel = new HashMap<String, Integer>();
    public final Map<String, Set<String>> colorsOwned = new HashMap<String, Set<String>>();
    public boolean premium;

    public int[] upgrades(String car) {
        int[] u = upgr.get(car);
        return u == null ? new int[]{0, 0} : u;
    }

    public int engineLvl(String car) {
        return upgrades(car)[0];
    }

    public int tireLvl(String car) {
        return upgrades(car)[1];
    }

    public int parts(String car) {
        Integer m = partsMask.get(car);
        return m == null ? 0 : m;
    }

    public boolean ownsColor(String car, int color) {
        Set<String> s = colorsOwned.get(car);
        if (s == null) return color == CarType.byKey(car).bodyColor;
        return s.contains(String.valueOf(color));
    }

    public int selectedColor(String car) {
        Integer c = colorSel.get(car);
        return c == null ? CarType.byKey(car).bodyColor : c;
    }

    public static GarageData parse(String resp) {
        GarageData g = new GarageData();
        if (resp == null) {
            g.owned.add("sedan");
            return g;
        }
        String[] p = resp.split("\t", -1);
        if (p.length >= 1) {
            try {
                g.coins = Long.parseLong(p[0].trim());
            } catch (Exception ignored) {
            }
        }
        if (p.length >= 2 && !p[1].trim().isEmpty()) {
            for (String c : p[1].split(",")) {
                String k = c.trim();
                if (!k.isEmpty()) g.owned.add(k);
            }
        }
        if (!g.owned.contains("sedan")) g.owned.add("sedan");
        if (p.length >= 3 && !p[2].trim().isEmpty()) g.selected = p[2].trim();
        if (p.length >= 4 && !p[3].trim().isEmpty()) {
            for (String e : p[3].split(";")) {
                String[] kv = e.split("=");
                if (kv.length == 2) {
                    try {
                        String[] lv = kv[1].split(":");
                        g.upgr.put(kv[0], new int[]{Integer.parseInt(lv[0].trim()), Integer.parseInt(lv[1].trim())});
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        if (p.length >= 5 && !p[4].trim().isEmpty()) {
            for (String e : p[4].split(";")) {
                String[] kv = e.split("=");
                if (kv.length == 2) {
                    int mask = 0;
                    for (String pp : kv[1].split(",")) {
                        int i = CarType.partIndex(pp.trim());
                        if (i >= 0) mask |= 1 << i;
                    }
                    g.partsMask.put(kv[0], mask);
                }
            }
        }
        if (p.length >= 6 && !p[5].trim().isEmpty()) {
            for (String e : p[5].split(";")) {
                String[] kv = e.split("=");
                if (kv.length == 2) {
                    String car = kv[0];
                    String[] selOwned = kv[1].split("\\|");
                    try {
                        g.colorSel.put(car, Integer.parseInt(selOwned[0].trim()));
                    } catch (Exception ignored) {
                    }
                    if (selOwned.length >= 2 && !selOwned[1].trim().isEmpty()) {
                        Set<String> s = new LinkedHashSet<String>();
                        for (String cc : selOwned[1].split(",")) {
                            String t = cc.trim();
                            if (!t.isEmpty()) s.add(t);
                        }
                        g.colorsOwned.put(car, s);
                    }
                }
            }
        }
        if (p.length >= 8 && "1".equals(p[7].trim())) g.premium = true;
        return g;
    }
}