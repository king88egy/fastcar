package com.fastcar.desktop;

import com.fastcar.common.CarType;
import com.fastcar.common.L10n;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RacePanel extends JPanel {

    public interface Listener {
        void onEnd(int score);

        void onExit();
    }

    private static final int LANES = 3;
    private static final int TIMER_MS = 16;

    public enum State {
        READY, RUNNING, OVER
    }

    public CarType.Spec spec;
    public State state = State.READY;
    private final Listener listener;
    private String lang;
    private final Random rnd = new Random();
    private double score = 0;
    private int lane = 1;
    private double laneX = 1;
    private double roadOffset = 0;
    private double speed = 160;
    private final List<double[]> foes = new ArrayList<>();
    private double spawn = 0;
    private int crashed = 0;
    private double nitro = 50;
    private boolean boosting = false;
    private javax.swing.Timer timer;

    public RacePanel(String lang, CarType.Spec spec, Listener listener) {
        this.lang = lang;
        this.spec = spec;
        this.listener = listener;
        setFocusable(true);
        setBackground(new Color(24, 26, 30));
        setPreferredSize(new Dimension(340, 560));
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                onKey(e.getKeyCode());
            }
        });
        timer = new javax.swing.Timer(TIMER_MS, e -> tick());
        timer.start();
    }

    public void setLang(String lang) {
        this.lang = lang;
        repaint();
    }

    private void onKey(int code) {
        if (state == State.READY) {
            state = State.RUNNING;
            lane = 1;
            laneX = 1;
            score = 0;
            crashed = 0;
            foes.clear();
            spawn = 0;
            nitro = 50;
            boosting = false;
            return;
        }
        if (state == State.OVER) {
            if (code == KeyEvent.VK_ENTER) reset();
            if (code == KeyEvent.VK_ESCAPE) listener.onExit();
            return;
        }
        if (code == KeyEvent.VK_LEFT) {
            lane = Math.max(0, lane - 1);
            Tts.say("مسار أيسر");
        }
        if (code == KeyEvent.VK_RIGHT) {
            lane = Math.min(LANES - 1, lane + 1);
            Tts.say("مسار أيمن");
        }
        if (code == KeyEvent.VK_SPACE) {
            boosting = nitro > 2;
            if (boosting) Tts.say("نيترو!");
        }
    }

    public void reset() {
        state = State.READY;
        repaint();
    }

    private double maxSpeed() {
        return 900 + spec.engine * 62.0 + spec.tires * 26.0;
    }

    private void tick() {
        if (state != State.RUNNING) return;
        double maxS = maxSpeed();
        if (boosting) {
            nitro -= 16 * TIMER_MS / 1000.0;
            if (nitro <= 0) {
                nitro = 0;
                boosting = false;
            }
            speed = Math.min(maxS * 1.55, speed + 320 * TIMER_MS / 1000.0);
        } else {
            nitro = Math.min(50, nitro + 6 * TIMER_MS / 1000.0);
            speed = Math.min(maxS, speed + 55 * TIMER_MS / 1000.0);
        }
        double dx = speed * TIMER_MS / 1000.0;
        score += dx * spec.scoreMult();
        roadOffset = (roadOffset + dx) % 58;
        double lx = laneX + (lane - laneX) * 0.22;
        laneX = lx;
        spawn -= dx;
        if (spawn <= 0) {
            int c1 = rnd.nextInt(LANES);
            int c2 = c1;
            while (Math.abs(c2 - c1) < 2) c2 = rnd.nextInt(LANES);
            foes.add(new double[]{c1, 0});
            if (rnd.nextBoolean()) foes.add(new double[]{c2, -0.35});
            double gap = 150 + rnd.nextDouble() * 220;
            spawn = gap * (1 + spec.tires * 0.03);
        }
        boolean hit = false;
        List<double[]> live = new ArrayList<>();
        for (double[] f : foes) {
            f[1] += dx / 550.0;
            if (f[1] > 1.12) continue;
            live.add(f);
            double carY = 0.72;
            if (Math.abs(laneX - f[0]) < 0.55 && Math.abs(f[1] - carY) < 0.34) {
                hit = true;
            }
        }
        foes.clear();
        foes.addAll(live);
        if (hit) {
            crashed = 1;
            state = State.OVER;
            int finalScore = (int) score;
            long coins = Math.max(1, Math.round(finalScore * spec.scoreMult() * 0.05));
            listener.onEnd(finalScore);
        }
        repaint();
    }

    private void drawCar(Graphics2D g, double laneN, double laneY, Color body, int partMask, boolean player) {
        double y = laneY * getHeight();
        double laneW = getWidth() / (double) LANES;
        double x = laneN * laneW + laneW / 2.0;
        double w = laneW * (player ? 0.52 : 0.48);
        double h = w * (player ? 1.78 : 1.68);
        g.setColor(Color.darkGray);
        g.fillRoundRect((int) (x - w * 0.56), (int) (y - h * 0.52), (int) (w * 1.12), (int) (h * 1.05), 12, 12);
        g.setColor(body);
        g.fillRoundRect((int) (x - w * 0.46), (int) (y - h * 0.54), (int) (w * 0.92), (int) (h * 0.86), 10, 10);
        g.setColor(new Color(10, 12, 14));
        g.fillRect((int) (x - w * 0.3), (int) (y - h * 0.2), (int) (w * 0.6), (int) (h * 0.5));
        g.setColor(new Color(210, 215, 224));
        g.fillRect((int) (x - w * 0.18), (int) (y - h * 0.56), (int) (w * 0.36), (int) (h * 0.18));
        g.setColor(new Color(20, 22, 26));
        g.fillOval((int) (x - w * 0.34), (int) (y + h * 0.38), (int) (w * 0.24), (int) (w * 0.24));
        g.fillOval((int) (x + w * 0.10), (int) (y + h * 0.38), (int) (w * 0.24), (int) (w * 0.24));
        if (player && (partMask & 1) != 0) {
            g.setColor(new Color(40, 44, 52));
            g.fillRect((int) (x - w * 0.5), (int) (y - h * 0.62), (int) (w * 1.0), (int) (h * 0.1));
        }
        if (player && (partMask & 2) != 0) {
            g.setColor(new Color(255, 90, 220));
            g.fillOval((int) (x - w * 0.6), (int) (y - h * 0.5), 5, 5);
        }
        if (player && (partMask & 8) != 0) {
            g.setColor(new Color(255, 140, 40));
            g.fillOval((int) (x - w * 0.36), (int) (y - h * 0.54), 6, 6);
        }
        if (player && boosting) {
            double f = w * (0.35 + rnd.nextDouble() * 0.15);
            g.setColor(new Color(255, 180, 40));
            g.fillOval((int) (x - w * 0.34), (int) (y + h * 0.52), (int) (f * 0.6), (int) (w * 0.28));
            g.setColor(new Color(255, 90, 20));
            g.fillOval((int) (x - w * 0.24), (int) (y + h * 0.56), (int) (f), (int) (w * 0.2));
        }
    }

    @Override
    protected void paintComponent(Graphics gr) {
        super.paintComponent(gr);
        Graphics2D g = (Graphics2D) gr;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int W = getWidth();
        int H = getHeight();
        double laneW = W / (double) LANES;
        g.setColor(new Color(34, 38, 44));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(60, 66, 74));
        for (int i = 0; i <= LANES; i++) {
            int x = (int) (i * laneW);
            g.fillRect(x - 1, 0, 3, H);
        }
        double d = (int) (roadOffset % 58) / 58.0;
        for (int i = 0; i < LANES - 1; i++) {
            int x = (int) (i * laneW + laneW / 2);
            for (int k = 0; k < 16; k++) {
                int yy = (int) (((k + d) % 16) * (H / 16.0));
                g.fillRect(x - 2, yy, 4, (int) (H / 28.0));
            }
        }
        for (double[] f : foes) {
            int partMask = 0;
            Color body = new Color(180, 60, 60);
            if (f[0] == 0) body = new Color(200, 160, 60);
            if (f[0] == 2) body = new Color(70, 110, 210);
            drawCar(g, f[0], f[1], body, partMask, false);
        }
        int color = spec.type.bodyColor;
        int[] pal = spec.type.palette;
        if (pal != null && spec.color >= 0 && spec.color < pal.length) {
            color = pal[spec.color];
        }
        drawCar(g, laneX, 0.72, new Color(color), spec.partMask, true);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.setColor(Color.WHITE);
        g.drawString(L10n.get(lang, L10n.I_SCORE) + " " + (int) score, 12, 26);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g.drawString((int) speed + " km/h", 12, 46);
        int barW = 90;
        int barH = 8;
        g.setColor(new Color(30, 34, 40));
        g.fillRect(12, 54, barW, barH);
        g.setColor(boosting ? new Color(255, 120, 20) : new Color(80, 200, 60));
        g.fillRect(12, 54, (int) (barW * nitro / 50.0), barH);
        g.setColor(new Color(220, 225, 232));
        g.drawString("N2O", 12, 78);
        if (state == State.READY) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, W, H);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            String s = L10n.get(lang, L10n.I_START_RACE) + " / " + L10n.get(lang, L10n.I_GAME_TAP);
            int sw = g.getFontMetrics().stringWidth(s);
            g.drawString(s, (W - sw) / 2, H / 2 - 10);
        }
        if (state == State.OVER) {
            if (crashed > 0 && crashed < 30) crashed++;
            g.setColor(new Color(120, 0, 0, 180));
            g.fillRect(0, 0, W, H);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
            String s = L10n.get(lang, L10n.I_GAME_CRASH);
            int sw = g.getFontMetrics().stringWidth(s);
            g.drawString(s, (W - sw) / 2, H / 3);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            s = L10n.get(lang, L10n.I_SCORE) + ": " + (int) score;
            sw = g.getFontMetrics().stringWidth(s);
            g.drawString(s, (W - sw) / 2, H / 3 + 34);
            s = L10n.get(lang, L10n.I_POINTS) + ": " + (int) score;
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            sw = g.getFontMetrics().stringWidth(s);
            g.drawString(s, (W - sw) / 2, H / 3 + 60);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            s = "[Enter] " + L10n.get(lang, L10n.I_AGAIN) + "   [Esc] " + L10n.get(lang, L10n.I_HOME);
            sw = g.getFontMetrics().stringWidth(s);
            g.drawString(s, (W - sw) / 2, H / 2 + 30);
        }
        g.setStroke(new BasicStroke(1));
    }
}