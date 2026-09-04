package com.fastcar.racing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fastcar.common.CarType;
import com.fastcar.common.L10n;

import java.io.IOException;
import java.io.InputStream;

public class GameActivity extends Activity implements RacingView.Listener {

    private static final float COINS_RATE = 0.05f;
    private static final int AD_COINS = 60;

    private RacingView game;
    private CarType.Spec spec;
    private String lang;

    private SoundPool pool;
    private int sClick = -1;
    private int sCrash = -1;
    private int sGo = -1;
    private MediaPlayer engine;
    private boolean soundOk = false;

    private int lastScore = 0;
    private int coinsEarned = 0;
    private AlertDialog endDialog;
    private TextView dialogStatus;
    private TextView coinsLine;
    private Button adBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        lang = Api.getLang(this);
        Api.loadSession(this);
        Sfx.init(this);

        String car = getIntent().getStringExtra("car");
        if (car == null) car = "sedan";
        int eng = getIntent().getIntExtra("eng", 0);
        int tire = getIntent().getIntExtra("tire", 0);
        int color = getIntent().getIntExtra("color", Integer.MIN_VALUE);
        if (color == Integer.MIN_VALUE) color = CarType.byKey(car).bodyColor;
        int parts = getIntent().getIntExtra("parts", 0);
        spec = new CarType.Spec(CarType.byKey(car), eng, tire, color, parts);

        initSound();
        Ads.preload(this);

        Bitmap photo = spec.type.photo ? loadPhoto() : null;

        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(Color.rgb(16, 19, 26));

        game = new RacingView(this, spec, lang, photo, new RacingView.Sfx() {
            @Override
            public void click() {
                play(sClick, 0.6f);
            }

            @Override
            public void crash() {
                play(sCrash, 1f);
                stopEngine();
            }

            @Override
            public void go() {
                play(sGo, 0.9f);
                startEngine();
            }
        }, this);
        frame.addView(game, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(frame);
    }

    private Bitmap loadPhoto() {
        try {
            InputStream in = getAssets().open("sounds/race_car_hero.png");
            Bitmap b = BitmapFactory.decodeStream(in);
            in.close();
            if (b == null) return null;
            int side = dp(220);
            return Bitmap.createScaledBitmap(b, side, side, true);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void onGameOver(int score) {
        lastScore = score;
        coinsEarned = Math.max(1, (int) (score * spec.scoreMult() * COINS_RATE));
        Sfx.play("gameover");
        stopEngine();
        uploadScore(score);
        showEndDialog(score);
    }

    private void uploadScore(final int score) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Api.submitScore(GameActivity.this, score);
                } catch (Exception ignored) {
                }
                try {
                    Api.earn(GameActivity.this, coinsEarned);
                } catch (Exception ignored) {
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dialogStatus != null) {
                            dialogStatus.setText(t(L10n.I_RESULT) + " +" + coinsEarned + " " + t(L10n.I_COINS));
                            dialogStatus.setTextColor(Color.rgb(123, 211, 137));
                        }
                    }
                });
            }
        }).start();
    }

    private void showEndDialog(int score) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(24);
        box.setPadding(pad, pad, pad, pad);

        TextView big = new TextView(this);
        big.setText("" + t(L10n.I_GAME_CRASH));
        big.setTextSize(22);
        big.setTextColor(Color.rgb(255, 213, 79));
        box.addView(big);

        TextView scr = new TextView(this);
        scr.setText(t(L10n.I_POINTS) + ": " + score);
        scr.setTextSize(34);
        scr.setTextColor(Color.WHITE);
        box.addView(scr);

        coinsLine = new TextView(this);
        coinsLine.setText("+" + coinsEarned + " " + t(L10n.I_COINS));
        coinsLine.setTextSize(18);
        coinsLine.setTextColor(Color.rgb(123, 211, 137));
        box.addView(coinsLine);

        adBtn = new Button(this);
        adBtn.setText("" + t(L10n.I_WATCH_AD) + "  (+" + AD_COINS + " " + t(L10n.I_COINS) + ")");
        adBtn.setAllCaps(false);
        adBtn.setTextColor(Color.rgb(20, 16, 4));
        adBtn.setBackgroundColor(Color.rgb(210, 170, 60));
        adBtn.setMinHeight(dp(54));
        adBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ads.showRewarded(GameActivity.this, new Runnable() {
                    @Override
                    public void run() {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Api.earn(GameActivity.this, AD_COINS);
                                } catch (Exception ignored) {
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        adBtn.setEnabled(false);
                                        adBtn.setText("+" + AD_COINS + " " + t(L10n.I_COINS));
                                    }
                                });
                            }
                        }).start();
                    }
                });
            }
        });
        box.addView(adBtn);

        dialogStatus = new TextView(this);
        dialogStatus.setText("...");
        dialogStatus.setTextSize(14);
        dialogStatus.setTextColor(Color.rgb(169, 199, 255));
        dialogStatus.setPadding(0, dp(6), 0, dp(10));
        box.addView(dialogStatus);

        endDialog = new AlertDialog.Builder(this)
                .setView(box)
                .setCancelable(false)
                .setPositiveButton("" + t(L10n.I_AGAIN), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                    }
                })
                .setNegativeButton("" + t(L10n.I_HOME), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        finish();
                    }
                })
                .create();
        endDialog.show();
        endDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endDialog.dismiss();
                dialogStatus = null;
                resetGame();
            }
        });
    }

    private void resetGame() {
        ((RacingView) game).resetByActivity();
    }

    private void initSound() {
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            pool = new SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build();
            sClick = tryLoad("sounds/click.mp3");
            sCrash = tryLoad("sounds/Crash2.ogg");
            sGo = tryLoad("sounds/countdown_go.ogg");
            soundOk = true;
        } catch (Exception ignored) {
            soundOk = false;
        }
    }

    private int tryLoad(String path) {
        try {
            return pool.load(getAssets().openFd(path), 1);
        } catch (IOException e) {
            return -1;
        }
    }

    private void play(int id, float vol) {
        if (!soundOk || pool == null || id < 0) return;
        try {
            pool.play(id, vol, vol, 1, 0, 1f);
        } catch (Exception ignored) {
        }
    }

    private void startEngine() {
        if (engine != null) return;
        try {
            AssetFileDescriptor fd;
            try {
                fd = getAssets().openFd(spec.type.engine);
            } catch (IOException e) {
                fd = getAssets().openFd("sounds/car_engine_high_cc0.wav");
            }
            engine = new MediaPlayer();
            engine.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            engine.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            engine.setLooping(true);
            engine.setVolume(0.35f, 0.35f);
            engine.prepare();
            engine.start();
        } catch (Exception ignored) {
            engine = null;
        }
    }

    private void stopEngine() {
        if (engine != null) {
            try {
                engine.stop();
                engine.release();
            } catch (Exception ignored) {
            }
            engine = null;
        }
    }

    private String t(int key) {
        return L10n.get(lang, key);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopEngine();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopEngine();
        if (pool != null) {
            try {
                pool.release();
            } catch (Exception ignored) {
            }
            pool = null;
        }
    }
}