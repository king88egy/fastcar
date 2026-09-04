package com.fastcar.common;

public final class CarType {

    public final String key;
    public final int price;
    public final int bodyColor;
    public final String engine;
    public final float bodyW;
    public final float bodyH;
    public final float roofTop;
    public final float roofH;
    public final float roofW;
    public final boolean photo;
    public final int speedBase;
    public final int accelBase;
    public final int gripBase;
    public final float mult;
    public final int[] palette;

    public CarType(String key, int price, int bodyColor, String engine,
                   float bodyW, float bodyH, float roofTop, float roofH, float roofW, boolean photo,
                   int speedBase, int accelBase, int gripBase, float mult, int[] palette) {
        this.key = key;
        this.price = price;
        this.bodyColor = bodyColor;
        this.engine = engine;
        this.bodyW = bodyW;
        this.bodyH = bodyH;
        this.roofTop = roofTop;
        this.roofH = roofH;
        this.roofW = roofW;
        this.photo = photo;
        this.speedBase = speedBase;
        this.accelBase = accelBase;
        this.gripBase = gripBase;
        this.mult = mult;
        this.palette = palette;
    }

    public static final CarType SEDAN = new CarType(
            "sedan", 0, rgb(86, 120, 214), "sounds/engine_compact.ogg",
            0.80f, 0.92f, 0.50f, 0.30f, 0.56f, false,
            140, 32, 50, 0.060f,
            new int[]{rgb(86, 120, 214), rgb(200, 205, 215), rgb(238, 240, 244), rgb(30, 32, 38), rgb(214, 70, 60)});

    public static final CarType SPORT = new CarType(
            "sport", 60, rgb(224, 62, 52), "sounds/engine_sport.ogg",
            0.86f, 0.80f, 0.42f, 0.26f, 0.60f, false,
            155, 42, 60, 0.070f,
            new int[]{rgb(224, 62, 52), rgb(28, 30, 36), rgb(240, 210, 40), rgb(240, 244, 248), rgb(40, 90, 220)});

    public static final CarType MUSCLE = new CarType(
            "muscle", 150, rgb(233, 145, 20), "sounds/engine_muscle.ogg",
            0.92f, 0.88f, 0.48f, 0.24f, 0.70f, false,
            175, 48, 66, 0.078f,
            new int[]{rgb(233, 145, 20), rgb(28, 30, 36), rgb(40, 150, 70), rgb(150, 158, 168), rgb(214, 60, 60)});

    public static final CarType HYPER = new CarType(
            "hyper", 300, rgb(154, 92, 196), "sounds/engine_hyper.ogg",
            0.96f, 0.72f, 0.38f, 0.22f, 0.66f, false,
            195, 55, 72, 0.086f,
            new int[]{rgb(154, 92, 196), rgb(28, 30, 36), rgb(0, 210, 220), rgb(240, 244, 248), rgb(240, 90, 140)});

    public static final CarType SUPER = new CarType(
            "super", 500, rgb(38, 168, 148), "sounds/engine_super.ogg",
            0.90f, 0.84f, 0.44f, 0.24f, 0.62f, false,
            215, 62, 78, 0.094f,
            new int[]{rgb(38, 168, 148), rgb(240, 244, 248), rgb(210, 170, 60), rgb(28, 30, 36), rgb(214, 60, 60)});

    public static final CarType HERO = new CarType(
            "hero", 750, rgb(222, 222, 222), "sounds/engine_supercar_hq.wav",
            0.94f, 0.86f, 0.44f, 0.24f, 0.60f, true,
            235, 70, 85, 0.102f,
            new int[]{rgb(222, 222, 222), rgb(214, 60, 60), rgb(40, 90, 220), rgb(28, 30, 36), rgb(210, 170, 60)});

    public static final CarType[] ALL = {SEDAN, SPORT, MUSCLE, HYPER, SUPER, HERO};
    public static final String[] PARTS = {"spoiler", "neon", "rims", "flame"};
    public static final int MAX_LEVEL = 5;

    public static CarType byKey(String k) {
        if (k == null) return SEDAN;
        for (CarType t : ALL) {
            if (t.key.equals(k)) return t;
        }
        return SEDAN;
    }

    public static int price(String key) {
        return byKey(key).price;
    }

    public static int partIndex(String part) {
        for (int i = 0; i < PARTS.length; i++) if (PARTS[i].equals(part)) return i;
        return -1;
    }

    public static int rgb(int r, int g, int b) {
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int[] upgradeCosts(boolean engine) {
        return engine ? new int[]{30, 60, 110, 180, 280} : new int[]{25, 50, 90, 150, 240};
    }

    public static int colorPrice() {
        return 15;
    }

    public static int partPrice(String part) {
        int i = partIndex(part);
        return i < 0 ? 0 : new int[]{30, 40, 50, 60}[i];
    }

    public static final class Spec {
        public final CarType type;
        public final int engine;
        public final int tires;
        public final int color;
        public final int partMask;

        public Spec(CarType type, int engine, int tires, int color, int partMask) {
            this.type = type;
            this.engine = engine;
            this.tires = tires;
            this.color = color;
            this.partMask = partMask;
        }

        public int speedKmh() {
            return type.speedBase + engine * 8;
        }

        public int accelVal() {
            return type.accelBase + engine * 6;
        }

        public int grip() {
            return type.gripBase + tires * 6;
        }

        public float scoreMult() {
            return type.mult + engine * 0.004f + tires * 0.002f;
        }

        public float laneLerp() {
            return 11f + tires * 1.5f;
        }

        public float hitboxInset() {
            return 0.18f + engine * 0.006f + tires * 0.010f;
        }

        public boolean hasPart(int i) {
            return (partMask & (1 << i)) != 0;
        }
    }
}