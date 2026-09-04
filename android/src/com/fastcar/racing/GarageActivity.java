package com.fastcar.racing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fastcar.common.CarType;
import com.fastcar.common.L10n;

public class GarageActivity extends Activity {

    private TextView coinsView;
    private TextView zone;
    private LinearLayout cards;
    private String lang;
    private CarType.Spec spec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Sfx.init(this);
        lang = Api.getLang(this);

        int eng = getIntent().getIntExtra("eng", 0);
        int tire = getIntent().getIntExtra("tire", 0);
        int color = getIntent().getIntExtra("color", Integer.MIN_VALUE);
        int parts = getIntent().getIntExtra("parts", 0);
        String car = getIntent().getStringExtra("car");
        spec = new CarType.Spec(CarType.byKey(car), eng, tire, color, parts);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(16, 19, 26));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(20), dp(24), dp(20), dp(20));

        TextView title = new TextView(this);
        title.setText("" + t(L10n.I_GARAGE));
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(255, 213, 79));
        title.setGravity(Gravity.CENTER);
        col.addView(title);

        coinsView = new TextView(this);
        coinsView.setTextSize(18);
        coinsView.setTypeface(Typeface.DEFAULT_BOLD);
        coinsView.setTextColor(Color.rgb(123, 211, 137));
        coinsView.setGravity(Gravity.CENTER);
        col.addView(coinsView);

        zone = new TextView(this);
        zone.setTextSize(14);
        zone.setTextColor(Color.rgb(216, 230, 255));
        zone.setGravity(Gravity.CENTER);
        col.addView(zone);

        cards = new LinearLayout(this);
        cards.setOrientation(LinearLayout.VERTICAL);
        col.addView(cards, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button back = new Button(this);
        back.setText(t(L10n.I_HOME));
        back.setAllCaps(false);
        back.setTextColor(Color.WHITE);
        back.setBackgroundColor(Color.rgb(40, 52, 74));
        back.setMinHeight(dp(54));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backToMenu();
            }
        });
        col.addView(back);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(col, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll);
        setContentView(root);
        refresh();
    }

    private void backToMenu() {
        startActivity(new Intent(this, MenuActivity.class));
        finish();
    }

    private void refresh() {
        coinsView.setText("" + t(L10n.I_COINS) + ": ...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final com.fastcar.common.GarageData g = Api.garageData(GarageActivity.this);
                    Api.cacheGarage(GarageActivity.this, garageCache(g));
                    final CarType.Spec s = buildSpec(g);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            render(g, s);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            com.fastcar.common.GarageData g = Api.cachedGarage(GarageActivity.this);
                            if (g.selected == null || g.owned.isEmpty()) {
                                zone.setText(t(L10n.I_NET_ERR));
                            } else {
                                render(g, buildSpec(g));
                                zone.setText(t(L10n.I_NET_ERR));
                            }
                        }
                    });
                }
            }
        }).start();
    }

    private CarType.Spec buildSpec(com.fastcar.common.GarageData g) {
        String car = g.selected == null ? "sedan" : g.selected;
        return new CarType.Spec(CarType.byKey(car), g.engineLvl(car), g.tireLvl(car), g.selectedColor(car), g.parts(car));
    }

    private String garageCache(com.fastcar.common.GarageData g) {
        StringBuilder sb = new StringBuilder();
        sb.append(g.coins).append('\t');
        StringBuilder owned = new StringBuilder();
        for (String k : g.owned) {
            if (owned.length() > 0) owned.append(",");
            owned.append(k);
        }
        sb.append(owned).append('\t').append(g.selected);
        return sb.toString();
    }

    private void render(final com.fastcar.common.GarageData g, CarType.Spec s) {
        spec = s;
        coinsView.setText("" + t(L10n.I_COINS) + ": " + g.coins);
        cards.removeAllViews();
        for (CarType ct : CarType.ALL) {
            cards.addView(card(g, ct));
        }
    }

    private LinearLayout card(final com.fastcar.common.GarageData g, final CarType ct) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.rgb(24, 30, 42));
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(lp);

        boolean owned = g.owned.contains(ct.key);
        boolean selected = ct.key.equals(g.selected);
        int[] up = g.upgrades(ct.key);
        int eLvl = up[0];
        int tLvl = up[1];

        TextView head = new TextView(this);
        head.setText((selected ? "" : (owned ? "▪ " : ""))
                + ct.key + "  ·  " + ct.speedBase + " " + t(L10n.I_SPEED));
        head.setTextSize(19);
        head.setTypeface(Typeface.DEFAULT_BOLD);
        head.setTextColor(selected ? Color.rgb(255, 213, 79) : Color.WHITE);
        head.setContentDescription(ct.key + (selected ? " " + t(L10n.I_SELECTED) : "")
                + " " + t(L10n.I_SPEED) + " " + ct.speedBase);
        card.addView(head);

        TextView stats = new TextView(this);
        stats.setText(t(L10n.I_SPEED) + " " + (ct.speedBase + eLvl * 8)
                + " · " + t(L10n.I_ACCEL) + " " + (ct.accelBase + eLvl * 6)
                + " · " + t(L10n.I_CONTROL) + " " + (ct.gripBase + tLvl * 6));
        stats.setTextSize(14);
        stats.setTextColor(Color.rgb(169, 199, 255));
        card.addView(stats);

        if (owned) {
            int max = CarType.MAX_LEVEL;
            final int costE = CarType.upgradeCosts(true)[Math.min(eLvl, max - 1)];
            Button be = small((eLvl >= max ? "M A X · " + t(L10n.I_ENGINE) : t(L10n.I_ENGINE) + " " + (eLvl + 1) + "/" + max + " · " + costE),
                    eLvl >= max, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mutate("/upgrade", ct.key, "engine");
                        }
                    });
            card.addView(be);

            final int costT = CarType.upgradeCosts(false)[Math.min(tLvl, max - 1)];
            Button bt = small((tLvl >= max ? "M A X · " + t(L10n.I_TIRES) : t(L10n.I_TIRES) + " " + (tLvl + 1) + "/" + max + " · " + costT),
                    tLvl >= max, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mutate("/upgrade", ct.key, "tires");
                        }
                    });
            card.addView(bt);
        }

        LinearLayout colors = new LinearLayout(this);
        colors.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < ct.palette.length; i++) {
            final int col = ct.palette[i];
            Button b = new Button(this);
            boolean own = g.ownsColor(ct.key, col);
            boolean sel = g.selectedColor(ct.key) == col;
            b.setText(own ? (sel ? "" : "") : "+");
            b.setBackgroundColor(col);
            b.setTextColor(Color.WHITE);
            b.setTextSize(15);
            b.setMinWidth(dp(46));
            b.setMinHeight(dp(46));
            b.setPadding(dp(4), dp(4), dp(4), dp(4));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(46), dp(46));
            clp.setMargins(0, dp(6), dp(6), dp(6));
            b.setLayoutParams(clp);
            final boolean isOwn = own;
            b.setContentDescription(t(L10n.I_COLOR) + " " + (i + 1) + (own ? " " : " · " + CarType.colorPrice()));
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isOwn) {
                        decideSelect(ct.key, col);
                    } else {
                        mutateWithColor("/buycolor", ct.key, col);
                    }
                }
            });
            colors.addView(b);
        }
        card.addView(colors);

        if (owned) {
            LinearLayout parts = new LinearLayout(this);
            parts.setOrientation(LinearLayout.HORIZONTAL);
            int mask = g.parts(ct.key);
            for (int i = 0; i < CarType.PARTS.length; i++) {
                final String part = CarType.PARTS[i];
                final int idx = i;
                final int price = CarType.partPrice(part);
                boolean has = (mask & (1 << i)) != 0;
                Button b = new Button(this);
                b.setText(partEmoji(i) + (has ? " " : " +" + price));
                b.setAllCaps(false);
                b.setTextSize(13);
                b.setTextColor(has ? Color.rgb(123, 211, 137) : Color.WHITE);
                b.setBackgroundColor(has ? Color.rgb(28, 56, 40) : Color.rgb(50, 58, 74));
                b.setPadding(dp(4), dp(10), dp(4), dp(10));
                LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                plp.setMargins(0, dp(6), dp(5), dp(6));
                b.setLayoutParams(plp);
                if (has) {
                    b.setContentDescription(partName(part) + " (مملوكة)");
                    b.setOnClickListener(null);
                } else {
                    b.setContentDescription(partName(part) + " " + price);
                    b.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mutate("/buypart", ct.key, part);
                        }
                    });
                }
                parts.addView(b);
            }
            card.addView(parts);
        }

        if (!owned) {
            final int price = ct.price;
            Button buy = new Button(this);
            buy.setText("" + t(L10n.I_BUY) + " " + ct.key + " — " + price);
            buy.setAllCaps(false);
            buy.setTextSize(16);
            buy.setTextColor(Color.rgb(255, 224, 130));
            buy.setBackgroundColor(Color.rgb(70, 56, 20));
            buy.setMinHeight(dp(54));
            buy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Sfx.play("click");
                    mutate("/buy", ct.key, null);
                }
            });
            card.addView(buy);
        } else {
            Button sel = new Button(this);
            if (selected) {
                sel.setText("" + t(L10n.I_SELECTED));
                sel.setTextColor(Color.rgb(123, 211, 137));
                sel.setBackgroundColor(Color.rgb(28, 56, 40));
            } else {
                sel.setText(t(L10n.I_SELECT));
                sel.setTextColor(Color.rgb(16, 19, 26));
                sel.setBackgroundColor(Color.rgb(255, 213, 79));
                sel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Sfx.play("click");
                        decideSelect(ct.key, g.selectedColor(ct.key));
                    }
                });
            }
            sel.setAllCaps(false);
            sel.setTextSize(16);
            sel.setMinHeight(dp(54));
            card.addView(sel);
        }
        return card;
    }

    private void decideSelect(final String car, final int color) {
        new AlertDialog.Builder(this)
                .setTitle(t(L10n.I_SELECT))
                .setMessage(car)
                .setPositiveButton(t(L10n.I_SELECT), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        mutateWithColor("/select", car, color);
                    }
                })
                .setNegativeButton(t(L10n.I_EXIT), null)
                .show();
    }

    private void mutate(final String path, final String car, final String part) {
        switch (path) {
            case "/buy":
                apiBuy(car);
                break;
            case "/upgrade":
                apiUp(car, part);
                break;
            case "/buypart":
                apiBuyPart(car, part);
                break;
        }
    }

    private void mutateWithColor(final String path, final String car, final int color) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String res = "/buycolor".equals(path)
                            ? Api.buyColor(GarageActivity.this, car, color)
                            : Api.selectCar(GarageActivity.this, car, color);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            afterMutate(res);
                        }
                    });
                } catch (final Exception e) {
                    fail(e);
                }
            }
        }).start();
    }

    private void apiBuy(final String car) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String res = Api.buy(GarageActivity.this, car);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            afterMutate(res);
                        }
                    });
                } catch (final Exception e) {
                    fail(e);
                }
            }
        }).start();
    }

    private void apiUp(final String car, final String part) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String res = Api.upgrade(GarageActivity.this, car, part);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            afterMutate(res);
                        }
                    });
                } catch (final Exception e) {
                    fail(e);
                }
            }
        }).start();
    }

    private void apiBuyPart(final String car, final String part) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String res = Api.buyPart(GarageActivity.this, car, part);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            afterMutate(res);
                        }
                    });
                } catch (final Exception e) {
                    fail(e);
                }
            }
        }).start();
    }

    private void afterMutate(String res) {
        if (res.startsWith("OK")) {
            Sfx.play("success");
zone.setText("تم");
        zone.announceForAccessibility("تم");
            refresh();
        } else {
            Sfx.play("error");
            zone.setText(clean(res));
            zone.announceForAccessibility(clean(res));
        }
    }

    private void fail(Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                zone.setText(t(L10n.I_NET_ERR));
            }
        });
    }

    private Button small(String text, boolean disabled, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTextColor(Color.rgb(216, 230, 255));
        b.setBackgroundColor(Color.rgb(50, 58, 74));
        b.setPadding(dp(8), dp(10), dp(8), dp(10));
        b.setMinHeight(dp(50));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(5), 0, dp(5));
        b.setLayoutParams(lp);
        b.setEnabled(!disabled);
        b.setOnClickListener(l);
        return b;
    }

    private String partEmoji(int i) {
        return new String[]{"محرك", "مصباح", "إطارات", "نار"}[i];
    }

    private String partName(String part) {
        int k = part.equals("spoiler") ? L10n.I_SPOILER
                : part.equals("neon") ? L10n.I_NEON
                : part.equals("rims") ? L10n.I_RIMS : L10n.I_FLAME;
        return t(k);
    }

    private String clean(String res) {
        return res == null ? t(L10n.I_NET_ERR) : res.replace("ERR:", "");
    }

    private String t(int key) {
        return L10n.get(lang, key);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}