package com.fastcar.racing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

import com.fastcar.common.CarType;
import com.fastcar.common.L10n;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RacingView extends View {

    public interface Listener {
        void onGameOver(int score);
    }

    public interface Sfx {
        void click();

        void crash();

        void go();
    }

    private static final int STATE_READY = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_CRASHED = 2;
    private static final int LANES = 3;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Random rnd = new Random();
    private final List<Obstacle> obstacles = new ArrayList<Obstacle>();

    private final CarType.Spec spec;
    private final String lang;
    private Bitmap photo;
    private Sfx sfx;
    private Listener listener;

    private int state = STATE_READY;
    private long lastFrame = 0;
    private boolean loop = true;

    private float roadL;
    private float roadR;
    private float roadTop;
    private float roadBottom;
    private float laneW;
    private float playerX;
    private int targetLane = 1;
    private int playerLane = 1;

    private float dashOffset = 0;
    private float speed = 360f;
    private float spawnTimer = 0f;
    private int score = 0;
    private float maxSpeed = 1000f;
    private float laneLerp = 11f;
    private float nitro = 50f;
    private boolean boosting = false;
    private float nitroFlame = 0f;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!loop) return;
            long now = System.nanoTime();
            if (lastFrame == 0) lastFrame = now;
            float dt = Math.min(0.05f, (float) ((now - lastFrame) / 1e9f));
            lastFrame = now;
            if (state == STATE_RUNNING) update(dt);
            invalidate();
            handler.postDelayed(tick, 16);
        }
    };

    public RacingView(Context context, CarType.Spec spec, String lang, Bitmap photo, Sfx sfx, Listener listener) {
        super(context);
        this.spec = spec;
        this.lang = lang;
        this.photo = photo;
        this.sfx = sfx;
        this.listener = listener;
        this.maxSpeed = 900f + spec.engine * 62f + spec.tires * 26f;
        this.laneLerp = spec.laneLerp();
        setFocusable(true);
        setContentDescription(spec.type.key + " · " + t(L10n.I_SPEED) + " " + spec.speedKmh());
        handler.post(tick);
    }

    static class Obstacle {
        int lane;
        float y;
        int color;

        Obstacle(int lane, float y, int color) {
            this.lane = lane;
            this.y = y;
            this.color = color;
        }
    }

    private void update(float dt) {
        dashOffset += speed * dt;
        score += (int) (speed * dt * 0.06f);
        if (boosting) {
            nitro -= 16f * dt;
            if (nitro <= 0) {
                nitro = 0;
                boosting = false;
            }
            speed = Math.min(maxSpeed * 1.55f, speed + (85f + spec.accelVal() * 0.05f) * dt);
            nitroFlame = 1f;
        } else {
            nitro = Math.min(50f, nitro + 6f * dt);
            speed = Math.min(maxSpeed, speed + (7f + spec.accelVal() * 0.02f) * dt);
            nitroFlame = Math.max(0f, nitroFlame - 4f * dt);
        }

        spawnTimer -= dt;
        if (spawnTimer <= 0) {
            int lane = rnd.nextInt(LANES);
            Obstacle o = new Obstacle(lane, -laneH(), obstacleColor());
            obstacles.add(o);
            float gap = Math.max(0.42f, 1.2f - score / 13000f - spec.engine * 0.01f);
            float jitter = (rnd.nextFloat() - 0.5f) * 0.4f;
            spawnTimer = Math.max(0.26f, gap + jitter);
        }

        for (Obstacle o : obstacles) {
            o.y += speed * dt;
        }
        while (!obstacles.isEmpty() && obstacles.get(0).y > roadBottom + laneH()) {
            obstacles.remove(0);
        }

        float px = laneCenter(targetLane);
        playerX += (px - playerX) * Math.min(1f, dt * laneLerp);
        playerLane = targetLane;

        RectF p = playerRect();
        float ins = spec.hitboxInset() * laneW;
        for (Obstacle o : obstacles) {
            RectF ob = obstacleRect(o);
            if (p.left < ob.right - ins && p.right > ob.left + ins
                    && p.top < ob.bottom - ins && p.bottom > ob.top + ins) {
                crash();
                break;
            }
        }
    }

    private void crash() {
        state = STATE_CRASHED;
        if (sfx != null) sfx.crash();
        vibrate(90);
        announce(t(L10n.I_GAME_CRASH) + " " + t(L10n.I_RESULT) + " " + score);
        if (listener != null) listener.onGameOver(score);
    }

    public void resetByActivity() {
        reset();
        invalidate();
    }

    private void reset() {
        obstacles.clear();
        targetLane = 1;
        playerLane = 1;
        speed = 360f;
        spawnTimer = 0.9f;
        score = 0;
        state = STATE_READY;
        lastFrame = 0;
        nitro = 50f;
        boosting = false;
        nitroFlame = 0f;
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        roadTop = 0;
        roadBottom = h;
        float roadW = Math.min(w * 0.9f, 520f);
        roadL = (w - roadW) / 2f;
        roadR = roadL + roadW;
        laneW = roadW / LANES;
        playerX = laneCenter(targetLane);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        int w = c.getWidth();
        int h = c.getHeight();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(22, 74, 42));
        c.drawRect(0, 0, roadL, h, paint);
        c.drawRect(roadR, 0, w, h, paint);

        paint.setColor(Color.rgb(54, 62, 74));
        c.drawRect(roadL, 0, roadR, h, paint);

        paint.setStrokeWidth(Math.max(3f, laneW * 0.035f));
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.rgb(216, 226, 240));
        float seg = laneH() * 1.2f;
        float d = (dashOffset * 0.7f) % seg;
        for (int lane = 1; lane < LANES; lane++) {
            float x = roadL + laneW * lane;
            for (float y = -seg + d; y < h + seg; y += seg) {
                c.drawLine(x, y, x, y + seg * 0.5f, paint);
            }
        }

        for (Obstacle o : obstacles) {
            drawCar(c, obstacleRect(o), o.color, false);
        }

        RectF pr = playerRect();
        if (photo != null && spec.type.photo) {
            c.drawBitmap(photo, null, new Rect((int) pr.left, (int) pr.top, (int) pr.right, (int) pr.bottom), paint);
            drawRims(c, pr);
        } else {
            drawCar(c, pr, spec.color, true);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(255, 213, 79));
        paint.setTextSize(40);
        paint.setStrokeWidth(1);
        paint.setFakeBoldText(true);
        c.drawText(t(L10n.I_POINTS) + ": " + score, roadL + dp(10), dp(34), paint);

        int nbw = dp(110);
        int nbh = dp(10);
        paint.setColor(Color.rgb(30, 34, 40));
        c.drawRect(roadR - nbw - dp(12), dp(20), roadR - dp(12), dp(20) + nbh, paint);
        paint.setColor(boosting ? Color.rgb(255, 120, 20) : Color.rgb(80, 200, 90));
        c.drawRect(roadR - nbw - dp(12), dp(20), roadR - nbw - dp(12) + nbw * (nitro / 50f), dp(20) + nbh, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(dp(13));
        paint.setFakeBoldText(true);
        c.drawText("N2O", roadR - nbw - dp(12), dp(20) + nbh + dp(14), paint);

        if (state == STATE_READY) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(dp(26));
            paint.setFakeBoldText(true);
            String line1 = t(L10n.I_READY);
            String line2 = t(L10n.I_GAME_TAP);
            float tw1 = paint.measureText(line1);
            c.drawText(line1, (w - tw1) / 2f, h / 2f - dp(10), paint);
            paint.setTextSize(dp(15));
            float tw2 = paint.measureText(line2);
            c.drawText(line2, (w - tw2) / 2f, h / 2f + dp(24), paint);
        } else if (state == STATE_CRASHED) {
            paint.setColor(Color.rgb(255, 120, 100));
            paint.setTextSize(dp(30));
            paint.setFakeBoldText(true);
            String s = "" + t(L10n.I_GAME_CRASH);
            float tw = paint.measureText(s);
            c.drawText(s, (w - tw) / 2f, h / 2f - dp(24), paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(dp(18));
            String s2 = t(L10n.I_RESULT) + ": " + score + "  -  " + t(L10n.I_AGAIN);
            float tw2 = paint.measureText(s2);
            c.drawText(s2, (w - tw2) / 2f, h / 2f + dp(8), paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (state == STATE_CRASHED) {
                reset();
                invalidate();
                return true;
            }
            if (state == STATE_READY) {
                state = STATE_RUNNING;
                lastFrame = 0;
                if (sfx != null) sfx.go();
                announce(t(L10n.I_START_RACE));
                return true;
            }
            if (e.getY() > getHeight() * 0.66f && e.getX() > getWidth() * 0.5f) {
                boosting = nitro > 2f;
                if (boosting) announce("نيترو!");
                return true;
            }
            float x = e.getX();
            if (x < roadL || x > roadR) return true;
            int lane = (int) ((x - roadL) / laneW);
            if (lane < 0) lane = 0;
            if (lane >= LANES) lane = LANES - 1;
            if (lane != targetLane) {
                targetLane = lane;
                if (sfx != null) sfx.click();
                announce(t(L10n.I_GAME_TAP) + " " + (lane + 1) + "/" + LANES);
            }
        }
        return true;
    }

    private void drawCar(Canvas c, RectF r, int color, boolean player) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        float corner = Math.min(r.width(), r.height()) * 0.22f;
        c.drawRoundRect(r, corner, corner, paint);

        paint.setColor(Color.rgb(20, 24, 30));
        c.drawRoundRect(r.left + r.width() * 0.18f, r.top + r.height() * 0.30f,
                r.right - r.width() * 0.18f, r.top + r.height() * 0.55f,
                r.width() * 0.08f, r.width() * 0.08f, paint);

        paint.setColor(shade(color, 1.3f));
        c.drawRoundRect(r.left + r.width() * 0.06f, r.top + r.height() * 0.55f,
                r.right - r.width() * 0.06f, r.top + r.height() * 0.78f,
                r.width() * 0.12f, r.width() * 0.12f, paint);

        paint.setColor(Color.rgb(15, 18, 24));
        c.drawCircle(r.left + r.width() * 0.24f, r.bottom - r.height() * 0.10f, r.width() * 0.10f, paint);
        c.drawCircle(r.right - r.width() * 0.24f, r.bottom - r.height() * 0.10f, r.width() * 0.10f, paint);

        paint.setColor(shade(color, 1.6f));
        paint.setStrokeWidth(Math.max(3f, r.width() * 0.06f));
        c.drawLine(r.left + r.width() * 0.28f, r.top + r.height() * 0.62f,
                r.right - r.width() * 0.28f, r.top + r.height() * 0.62f, paint);

        if (player) {
            paint.setColor(Color.rgb(255, 213, 79));
            paint.setStrokeWidth(r.width() * 0.07f);
            c.drawLine(r.centerX() - r.width() * 0.08f, r.top,
                    r.centerX() - r.width() * 0.08f, r.top - dp(4), paint);
            c.drawLine(r.centerX() + r.width() * 0.08f, r.top,
                    r.centerX() + r.width() * 0.08f, r.top - dp(4), paint);
            drawParts(c, r);
            if (boosting && nitroFlame > 0.1f) {
                float fw = r.width() * (0.3f + nitroFlame * 0.25f);
                paint.setColor(Color.rgb(255, 190, 40));
                c.drawOval(r.centerX() - fw * 0.5f, r.bottom - dp(8), fw, r.height() * 0.28f, paint);
                paint.setColor(Color.rgb(255, 90, 20));
                c.drawOval(r.centerX() - fw * 0.32f, r.bottom - dp(4), fw * 0.65f, r.height() * 0.2f, paint);
            }
        }
    }

    private void drawParts(Canvas c, RectF r) {
        if (spec.hasPart(0)) {
            paint.setColor(Color.rgb(24, 26, 32));
            c.drawRoundRect(r.left + r.width() * 0.10f, r.top - r.height() * 0.06f,
                    r.right - r.width() * 0.10f, r.top + r.height() * 0.02f,
                    r.width() * 0.05f, r.width() * 0.05f, paint);
        }
        if (spec.hasPart(1)) {
            paint.setColor(Color.rgb(90, 220, 255));
            c.drawRoundRect(r.left + r.width() * 0.02f, r.bottom - r.height() * 0.05f,
                    r.right - r.width() * 0.02f, r.bottom + r.height() * 0.04f,
                    r.width() * 0.1f, r.width() * 0.1f, paint);
        }
        if (spec.hasPart(2)) {
            paint.setColor(Color.rgb(240, 205, 60));
            c.drawCircle(r.left + r.width() * 0.24f, r.bottom - r.height() * 0.12f, r.width() * 0.13f, paint);
            c.drawCircle(r.right - r.width() * 0.24f, r.bottom - r.height() * 0.12f, r.width() * 0.13f, paint);
        }
        if (spec.hasPart(3)) {
            paint.setColor(Color.rgb(255, 120, 40));
            paint.setStrokeWidth(r.width() * 0.07f);
            c.drawLine(r.left + r.width() * 0.10f, r.top + r.height() * 0.42f,
                    r.left + r.width() * 0.30f, r.top - dp(1), paint);
            c.drawLine(r.centerX() - r.width() * 0.05f, r.top + r.height() * 0.42f,
                    r.centerX(), r.top - dp(1), paint);
            c.drawLine(r.right - r.width() * 0.30f, r.top + r.height() * 0.42f,
                    r.right - r.width() * 0.10f, r.top - dp(1), paint);
        }
    }

    private void drawRims(Canvas c, RectF r) {
        if (!spec.hasPart(2)) return;
        paint.setColor(Color.rgb(240, 205, 60));
        c.drawCircle(r.left + r.width() * 0.24f, r.bottom - r.height() * 0.12f, r.width() * 0.13f, paint);
        c.drawCircle(r.right - r.width() * 0.24f, r.bottom - r.height() * 0.12f, r.width() * 0.13f, paint);
    }

    private int shade(int color, float f) {
        return Color.rgb(clamp255((int) (Color.red(color) * f)),
                clamp255((int) (Color.green(color) * f)),
                clamp255((int) (Color.blue(color) * f)));
    }

    private int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private int obstacleColor() {
        int[] colors = {
                Color.rgb(233, 145, 20),
                Color.rgb(86, 120, 214),
                Color.rgb(154, 92, 196),
                Color.rgb(38, 168, 148),
                Color.rgb(145, 53, 66)
        };
        return colors[rnd.nextInt(colors.length)];
    }

    private float laneH() {
        return laneW * 1.7f;
    }

    private float laneCenter(int lane) {
        return roadL + laneW * (lane + 0.5f);
    }

    private RectF playerRect() {
        float h = laneH() * 0.82f;
        float w = laneW * 0.78f;
        float top = roadBottom - h - dp(8);
        return new RectF(playerX - w / 2f, top, playerX + w / 2f, top + h);
    }

    private RectF obstacleRect(Obstacle o) {
        float h = laneH() * 0.8f;
        float w = laneW * 0.78f;
        float cx = laneCenter(o.lane);
        return new RectF(cx - w / 2f, o.y, cx + w / 2f, o.y + h);
    }

    private String t(int key) {
        return L10n.get(lang, key);
    }

    private void announce(String s) {
        try {
            announceForAccessibility(s);
        } catch (Exception ignored) {
        }
    }

    private void vibrate(int ms) {
        try {
            Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) v.vibrate(ms);
        } catch (Exception ignored) {
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        loop = false;
        handler.removeCallbacks(tick);
    }
}