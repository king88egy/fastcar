package com.fastcar.racing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.fastcar.common.GarageData;
import com.fastcar.common.L10n;

public class MenuActivity extends Activity {

    private static final int AD_COINS = 60;

    private TextView coinsView;
    private TextView selView;
    private TextView zone;
    private String lang;
    private final StringBuilder lastGarage = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lang = Api.getLang(this);
        Api.loadSession(this);
        Ads.init(this);
        Ads.preload(this);
        Tts.init(this);
        Sfx.init(this);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(16, 19, 26));

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(26), dp(24), dp(26));
        scroll.addView(box, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView title = new TextView(this);
        title.setText("ðŸ Fast Car");
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(255, 213, 79));
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        TextView user = new TextView(this);
        user.setText(Api.username);
        user.setTextSize(14);
        user.setTextColor(Color.rgb(169, 199, 255));
        user.setGravity(Gravity.CENTER);
        box.addView(user);

        spacer(box, 8);

        coinsView = new TextView(this);
        coinsView.setText("ðŸ’° " + t(L10n.I_COINS));
        coinsView.setTextSize(20);
        coinsView.setTypeface(Typeface.DEFAULT_BOLD);
        coinsView.setTextColor(Color.rgb(123, 211, 137));
        coinsView.setGravity(Gravity.CENTER);
        box.addView(coinsView);

        selView = new TextView(this);
        selView.setTextSize(15);
        selView.setTextColor(Color.rgb(216, 230, 255));
        selView.setGravity(Gravity.CENTER);
        box.addView(selView);

        spacer(box, 14);

        box.addView(btn("ðŸ " + t(L10n.I_START_RACE), Color.rgb(255, 213, 79), Color.rgb(16, 19, 26), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        }));

        box.addView(btn("ðŸŽ¨ " + t(L10n.I_GARAGE), Color.rgb(76, 161, 175), Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MenuActivity.this, GarageActivity.class));
            }
        }));

        box.addView(btn("ðŸ† " + t(L10n.I_LEADERBOARD), Color.rgb(154, 92, 196), Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MenuActivity.this, ScoresActivity.class));
            }
        }));

        box.addView(btn("ðŸŽ® Rooms / Ø§Ù„ØºØ±Ù", Color.rgb(76, 175, 80), Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Tts.say("ØºØ±Ù Ø§Ù„Ù„Ø¹Ø¨. Ø¹Ø§Ù…Ø© Ø£Ùˆ Ø®Ø§ØµØ© Ù…Ø¹ Ø£ØµØ­Ø§Ø¨Ùƒ");
                startActivity(new Intent(MenuActivity.this, RoomsActivity.class));
            }
        }));

        box.addView(btn("ðŸ“œ " + t("privacy"), Color.rgb(34, 50, 66), Color.rgb(216, 230, 255), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MenuActivity.this, PrivacyActivity.class);
                i.putExtra(PrivacyActivity.EXTRA_DOC, PrivacyActivity.DOC_PRIVACY);
                startActivity(i);
            }
        }));

        box.addView(btn("âš– " + t("terms"), Color.rgb(34, 50, 66), Color.rgb(216, 230, 255), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MenuActivity.this, PrivacyActivity.class);
                i.putExtra(PrivacyActivity.EXTRA_DOC, PrivacyActivity.DOC_TERMS);
                startActivity(i);
            }
        }));

        box.addView(btn("ðŸ“º " + t(L10n.I_WATCH_AD) + "  (+" + AD_COINS + " " + t(L10n.I_COINS) + ")", Color.rgb(210, 170, 60), Color.rgb(20, 16, 4), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ads.showRewarded(MenuActivity.this, new Runnable() {
                    @Override
                    public void run() {
                        addCoins(AD_COINS);
                    }
                });
            }
        }));

        box.addView(btn("ðŸ’³ Ø´Ø±Ø§Ø¡ Ø¹Ù…Ù„Ø§Øª  ðŸ’°", Color.rgb(56, 160, 80), Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openStore();
            }
        }));

        box.addView(btn("شخصيات - مهارات مميزة", Color.rgb(38, 166, 91), Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Tts.say("الشخصيات. اختر شخصية تمنحك مهارة مميزة");
                openCharacters();
            }
        }));

        box.addView(btn("متجر الهدايا - عناصر ممتعة", Color.rgb(96, 125, 170), Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Tts.say("متجر الهدايا. اختر عنصراً ممتعاً");
                openItemsShop();
            }
        }));

        box.addView(btn("ðŸ‘‘ Premium Account", Color.rgb(142, 105, 190), Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPremiumDialog();
            }
        }));

        box.addView(btn("ðŸ“‹ Ø§Ù„Ù…Ù‡Ø§Ù… ÙˆØ§Ù„Ø¥Ù†Ø¬Ø§Ø²Ø§Øª ÙˆØ§Ù„Ø¹Ø¬Ù„Ø©", Color.rgb(214, 158, 46), Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Tts.say("Ø§Ù„Ù…Ù‡Ø§Ù… ÙˆØ§Ù„Ø¥Ù†Ø¬Ø§Ø²Ø§Øª ÙˆØ¹Ø¬Ù„Ø© Ø§Ù„Ø­Ø¸");
                openTasks();
            }
        }));

        box.addView(btn("ðŸ›  Developer Panel", Color.rgb(60, 65, 84), Color.rgb(220, 225, 245), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Tts.say("Ù„ÙˆØ­Ø© Ø§Ù„Ù…Ø·ÙˆØ±");
                openDevPanel();
            }
        }));

        box.addView(btn("ðŸŒ " + t(L10n.I_LANG), Color.rgb(40, 52, 74), Color.rgb(216, 230, 255), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickLanguage();
            }
        }));

        zone = new TextView(this);
        zone.setTextSize(13);
        zone.setTextColor(Color.rgb(169, 199, 255));
        zone.setGravity(Gravity.CENTER);
        zone.setPadding(0, dp(8), 0, dp(6));
        box.addView(zone);

        box.addView(btn("âš™ï¸ " + t(L10n.I_SERVER), Color.rgb(30, 42, 58), Color.rgb(216, 230, 255), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editServer();
            }
        }));

        box.addView(btn("ðŸšª " + t(L10n.I_LOGOUT), Color.rgb(70, 48, 54), Color.rgb(255, 170, 150), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Api.clearSession(MenuActivity.this);
                startActivity(new Intent(MenuActivity.this, MainActivity.class));
                finish();
            }
        }));

        TextView cr = new TextView(this);
        cr.setText("ðŸ Fast Car  Â·  Â© 2026 Mohammed Egyptian");
        cr.setTextSize(12);
        cr.setGravity(Gravity.CENTER);
        cr.setTextColor(Color.rgb(120, 135, 160));
        cr.setPadding(0, dp(12), 0, 0);
        box.addView(cr);

        Ads.addBanner(this, root, 0);
        setContentView(root);
        refresh();
        maybeShowTutorial();
    }

    @Override
    protected void onResume() {
        super.onResume();
        lang = Api.getLang(this);
        refresh();
        Tts.init(this);
        Tts.say("القائمة جاهزة، اختر ما تريد بهدوء.", false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Tts.shutdown();
    }

    private void maybeShowTutorial() {
        if (!getSharedPreferences("fastcar", MODE_PRIVATE).getBoolean("show_tutorial", false)) return;
        getSharedPreferences("fastcar", MODE_PRIVATE).edit().putBoolean("show_tutorial", false).apply();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String txt = Api.tutorial(MenuActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(MenuActivity.this)
                                    .setTitle("دليل اللاعب الجديد")
                                    .setMessage(txt == null ? t(L10n.I_NET_ERR) : txt)
                                    .setPositiveButton("حسنًا، فهمت", null)
                                    .show();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            zone.setText(t(L10n.I_NET_ERR));
                        }
                    });
                }
            }
        }).start();
    }

    private void refresh() {
        coinsView.setText("ðŸ’° " + t(L10n.I_COINS) + ": ...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final GarageData g = Api.garageData(MenuActivity.this);
                    Api.cacheGarage(MenuActivity.this, lastGarage(g));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            renderGarage(g);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            renderGarage(Api.cachedGarage(MenuActivity.this));
                            if (zone != null) {
                                zone.setText("âš ï¸ " + t(L10n.I_NET_ERR) + "\n" + Api.getServer(MenuActivity.this));
                            }
                        }
                    });
                }
            }
        }).start();
    }

    private String lastGarage(GarageData g) {
        StringBuilder sb = new StringBuilder();
        sb.append(g.coins).append('\t');
        StringBuilder owned = new StringBuilder();
        for (String k : g.owned) {
            if (owned.length() > 0) owned.append(",");
            owned.append(k);
        }
        sb.append(owned).append('\t').append(g.selected).append('\t');
        for (String c : g.owned) {
            if (c.equals("sedan")) continue;
            int[] u = g.upgrades(c);
            if (u[0] > 0 || u[1] > 0) {
                if (sb.charAt(sb.length() - 1) != '\t') sb.append(';');
                sb.append(c).append('=').append(u[0]).append(':').append(u[1]);
            }
        }
        return sb.toString();
    }

    private void renderGarage(GarageData g) {
        coinsView.setText("ðŸ’° " + t(L10n.I_COINS) + ": " + g.coins);
        if (g.premium) {
            coinsView.setText(coinsView.getText() + "\nðŸ‘‘ " + t("premium"));
        }
        CarType ct = CarType.byKey(g.selected);
        selView.setText("ðŸŽï¸ " + ct.key + " Â· " + t(L10n.I_SPEED) + " " + g.engineLvl(g.selected) + " Â· " + t(L10n.I_CONTROL) + " " + g.tireLvl(g.selected));
        selView.setContentDescription(selView.getText());
        zone.setText(Api.getServer(this));
    }

    private void play() {
        GarageData g = Api.cachedGarage(this);
        String car = g.selected == null ? "sedan" : g.selected;
        CarType.Spec spec = new CarType.Spec(
                CarType.byKey(car),
                g.engineLvl(car),
                g.tireLvl(car),
                g.selectedColor(car),
                g.parts(car));
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra("car", spec.type.key);
        i.putExtra("eng", spec.engine);
        i.putExtra("tire", spec.tires);
        i.putExtra("color", spec.color);
        i.putExtra("parts", spec.partMask);
        startActivity(i);
    }

    private void addCoins(final int coins) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String res = Api.earn(MenuActivity.this, coins);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (res.startsWith("OK ")) {
                                Sfx.play("coin");
                                refresh();
                                coinsView.announceForAccessibility("+" + coins);
                            } else if (zone != null) {
                                zone.setText(fix(res));
                            }
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            zone.setText("âš ï¸ " + t(L10n.I_NET_ERR));
                        }
                    });
                }
            }
        }).start();
    }

    private void pickLanguage() {
        final String[] names = new String[L10n.CODES.length + 1];
        final String[] codes = new String[L10n.CODES.length + 1];
        codes[0] = "";
        int sys = indexOfCode(L10n.auto());
        names[0] = "ðŸŒ " + L10n.NATIVE[sys] + " (" + t(L10n.I_LANG) + ")";
        for (int i = 0; i < L10n.CODES.length; i++) {
            names[i + 1] = L10n.NATIVE[i];
            codes[i + 1] = L10n.CODES[i];
        }
        int current = 0;
        String cur = Api.getLang(this);
        for (int i = 0; i < codes.length; i++) {
            if (cur.equals(codes[i])) {
                current = i;
                break;
            }
        }
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(t(L10n.I_LANG));
        b.setSingleChoiceItems(names, current, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface d, final int which) {
                Api.setLang(MenuActivity.this, codes[which]);
                d.dismiss();
                Intent i = new Intent(MenuActivity.this, MenuActivity.class);
                startActivity(i);
                finish();
            }
        });
        b.setNegativeButton(t(L10n.I_EXIT), null);
        b.show();
    }

    private int indexOfCode(String code) {
        for (int i = 0; i < L10n.CODES.length; i++) {
            if (L10n.CODES[i].equals(code)) return i;
        }
        return 0;
    }

    private void openItemsShop() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String items = Api.storeItems(MenuActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (items == null) {
                                zone.setText(t(L10n.I_NET_ERR));
                                return;
                            }
                            final String[] rows = items.split("\n");
                            final java.util.ArrayList<String> labels = new java.util.ArrayList<String>();
                            for (final String r : rows) {
                                final String[] p = r.split("\\|");
                                if (p.length < 4) continue;
                                labels.add(p[1] + "  (" + p[2] + " عملة)  —  " + p[0]);
                            }
                            final String[] arr = labels.toArray(new String[0]);
                            new AlertDialog.Builder(MenuActivity.this)
                                    .setTitle("متجر الهدايا")
                                    .setItems(arr, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface d, int idx) {
                                            final String[] p = rows[idx].split("\\|");
                                            buyItemDialog(p[0], p[2]);
                                        }
                                    })
                                    .setNegativeButton(t(L10n.I_EXIT), null)
                                    .show();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            zone.setText(t(L10n.I_NET_ERR));
                        }
                    });
                }
            }
        }).start();
    }

    private void openCharacters() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String chars = Api.characters(MenuActivity.this);
                    if (chars == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                zone.setText(t(L10n.I_NET_ERR));
                            }
                        });
                        return;
                    }
                    final String[] rows = chars.split("\n");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final java.util.ArrayList<String> labels = new java.util.ArrayList<String>();
                            final java.util.ArrayList<Integer> coinMap = new java.util.ArrayList<Integer>();
                            for (final String r : rows) {
                                final String[] p = r.split("\\|");
                                if (p.length < 5) continue;
                                labels.add(p[1] + "\n" + p[2] + (p[4].equals("0") ? ("\nبـ " + p[3] + " عملة") : ("\nبـ " + p[4] + " ج.م عبر فودافون كاش")));
                                coinMap.add(Integer.parseInt(p[4]));
                            }
                            final String[] arr = labels.toArray(new String[0]);
                            new AlertDialog.Builder(MenuActivity.this)
                                    .setTitle("الشخصيات")
                                    .setItems(arr, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface d, int idx) {
                                            final String[] p = rows[idx].split("\\|");
                                            if (p.length >= 5 && !p[4].equals("0")) {
                                                characterCashDialog(p[0], p[1], p[4]);
                                            } else {
                                                characterBuyDialog(p[0], p[1], p[3]);
                                            }
                                        }
                                    })
                                    .setNegativeButton(t(L10n.I_EXIT), null)
                                    .show();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            zone.setText(t(L10n.I_NET_ERR));
                        }
                    });
                }
            }
        }).start();
    }

    private void characterBuyDialog(final String id, final String name, final String price) {
        new AlertDialog.Builder(this)
                .setTitle("شراء " + name)
                .setMessage("هل تريد شراء هذه الشخصية بـ " + price + " عملة؟")
                .setPositiveButton("اشتر", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    final String res = Api.characterBuy(MenuActivity.this, id);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            final boolean ok = res != null && res.startsWith("OK");
                                            Sfx.play(ok ? "success" : "error");
                                            new AlertDialog.Builder(MenuActivity.this)
                                                    .setTitle(ok ? "تم الشراء" : "فشل الشراء")
                                                    .setMessage(res == null ? t(L10n.I_NET_ERR) : res.replace("ERR:", "").replace("OK ", ""))
                                                    .setPositiveButton("OK", null).show();
                                            if (ok) refresh();
                                        }
                                    });
                                } catch (final Exception e) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            zone.setText(t(L10n.I_NET_ERR));
                                        }
                                    });
                                }
                            }
                        }).start();
                    }
                })
                .setNegativeButton(t(L10n.I_EXIT), null)
                .show();
    }

    private void characterCashDialog(final String id, final String name, final String egp) {
        final android.widget.EditText tx = new android.widget.EditText(this);
        tx.setHint("رقم العملية");
        new AlertDialog.Builder(this)
                .setTitle("شراء " + name + " (فودافون كاش)")
                .setMessage("حول " + egp + " ج.م إلى 01040814547 ثم أدخل رقم العملية:")
                .setView(tx)
                .setPositiveButton("إرسال", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        final String txid = tx.getText().toString().trim();
                        if (txid.isEmpty()) {
                            Sfx.play("error");
                            characterCashDialog(id, name, egp);
                            return;
                        }
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    final String res = Api.characterBuyCash(MenuActivity.this, id, txid);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Sfx.play(res != null && res.startsWith("OK") ? "success" : "error");
                                            new AlertDialog.Builder(MenuActivity.this)
                                                    .setTitle("طلب الشراء")
                                                    .setMessage(res == null ? t(L10n.I_NET_ERR) : res.replace("ERR:", ""))
                                                    .setPositiveButton("OK", null).show();
                                        }
                                    });
                                } catch (final Exception e) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            zone.setText(t(L10n.I_NET_ERR));
                                        }
                                    });
                                }
                            }
                        }).start();
                    }
                })
                .setNegativeButton(t(L10n.I_EXIT), null)
                .show();
    }

    private void buyItemDialog(final String item, final String price) {
        new AlertDialog.Builder(this)
                .setTitle("شراء " + item)
                .setMessage("هل تريد شراء هذا العنصر بـ " + price + " عملة؟")
                .setPositiveButton("اشتر", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    final String res = Api.buyItem(MenuActivity.this, item);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
final boolean ok = res != null && res.startsWith("OK");
                                            Sfx.play(ok ? "success" : "error");
                                            new AlertDialog.Builder(MenuActivity.this)
                                                    .setTitle(ok ? "تم الشراء" : "فشل الشراء")
                                                    .setMessage(res == null ? t(L10n.I_NET_ERR) : res.replace("ERR:", "").replace("OK ", ""))
                                                    .setPositiveButton("OK", null).show();
                                            Tts.say(ok ? "تم شراء " + item : (res == null ? "تعذر الشراء" : res));
                                            if (ok) refresh();
                                        }
                                    });
                                } catch (final Exception e) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            zone.setText(t(L10n.I_NET_ERR));
                                        }
                                    });
                                }
                            }
                        }).start();
                    }
                })
                .setNegativeButton(t(L10n.I_EXIT), null)
                .show();
    }

    private void openStore() {
        final String[] lines = {null};
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    lines[0] = Api.store(MenuActivity.this);
                } catch (Exception e) {
                    lines[0] = "ERR";
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (lines[0] == null || lines[0].startsWith("ERR")) {
                            new AlertDialog.Builder(MenuActivity.this)
                                    .setTitle(t(L10n.I_NET_ERR))
                                    .setPositiveButton(t(L10n.I_EXIT), null).show();
                            return;
                        }
                        final String[] pkgs = lines[0].split("\n");
                        String[] labels = new String[pkgs.length];
                        for (int i = 0; i < pkgs.length; i++) {
                            String[] p = pkgs[i].split("\\|");
                            labels[i] = p[0] + " Ø¹Ù…Ù„Ø©  â€”  " + p[1] + " Ø¬.Ù…";
                        }
                        AlertDialog.Builder b = new AlertDialog.Builder(MenuActivity.this);
                        b.setTitle("Ø´Ø±Ø§Ø¡ Ø¹Ù…Ù„Ø§Øª â€” Ø§Ø®ØªØ± Ø§Ù„Ø¨Ø§Ù‚Ø©");
                        b.setItems(labels, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int idx) {
                                String[] p = pkgs[idx].split("\\|");
                                showPayDialog(idx, p[0], p[1], p[2]);
                            }
                        });
                        b.setNegativeButton(t(L10n.I_EXIT), null);
                        b.show();
                    }
                });
            }
        }).start();
    }

    private void showPayDialog(final int pkgIdx, final String coins, final String egp, final String phone) {
        final android.widget.EditText txInput = new android.widget.EditText(this);
        txInput.setSingleLine(true);
        txInput.setHint("Ø±Ù‚Ù… Ø§Ù„Ø¹Ù…Ù„ÙŠØ© Ù…Ù† ÙÙˆØ¯Ø§ÙÙˆÙ† ÙƒØ§Ø´");
        txInput.setTextColor(Color.WHITE);
        txInput.setBackgroundColor(Color.rgb(26, 36, 48));
        int pad = dp(12);
        txInput.setPadding(pad, pad, pad, pad);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(8), dp(16), dp(8));

        TextView info = new TextView(this);
        info.setText("Ø­ÙˆÙ‘Ù„ " + egp + " Ø¬.Ù… Ø¹Ù„Ù‰:\n" + phone + "\n\nà¹à¸¥à¹‰à¸§ Ø£Ø¯Ø®Ù„ Ø±Ù‚Ù… Ø§Ù„Ø¹Ù…Ù„ÙŠØ©:");
        info.setTextSize(15);
        info.setTextColor(Color.WHITE);
        info.setPadding(0, 0, 0, dp(12));
        layout.addView(info);
        layout.addView(txInput);

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(coins + " Ø¹Ù…Ù„Ø© â€” " + egp + " Ø¬.Ù…");
        b.setView(layout);
        b.setPositiveButton("Ø¥Ø±Ø³Ø§Ù„", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int w) {
                String txid = txInput.getText().toString().trim();
                if (txid.isEmpty()) {
                    new AlertDialog.Builder(MenuActivity.this)
                            .setTitle("Ø£Ø¯Ø®Ù„ Ø±Ù‚Ù… Ø§Ù„Ø¹Ù…Ù„ÙŠØ©")
                            .setPositiveButton("OK", null).show();
                    return;
                }
                submitPayment(pkgIdx, txid);
            }
        });
        b.setNegativeButton(t(L10n.I_EXIT), null);
        b.show();
    }

    private void submitPayment(final int pkgIdx, final String txid) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String res = Api.payRequest(MenuActivity.this, pkgIdx, txid);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(MenuActivity.this)
                                    .setTitle("ØªÙ… Ø§Ù„Ø¥Ø±Ø³Ø§Ù„")
                                    .setMessage(res.replace("ERR:", "").replace("OK ", ""))
                                    .setPositiveButton("OK", null).show();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(MenuActivity.this)
                                    .setTitle(t(L10n.I_NET_ERR))
                                    .setPositiveButton("OK", null).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void showPremiumDialog() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String tiers = Api.premiumTiers(MenuActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final String[] rows = (tiers == null) ? new String[0] : tiers.split("\n");
                            final java.util.ArrayList<String> labels = new java.util.ArrayList<String>();
                            for (final String r : rows) {
                                final String[] p = r.split("\\|");
                                if (p.length < 4) continue;
                                final String days = p[3].equals("0") ? "دائم" : p[3] + " يوم";
                                final String price = p[1].equals("0") ? "مجانية" : p[1] + " ج.م";
                                labels.add(p[0] + "  (" + p[2] + ")  —  " + price + "  /  " + days);
                            }
                            labels.add("جرّب Premium مجانًا 3 أيام");
                            final String[] items = labels.toArray(new String[0]);
                            new AlertDialog.Builder(MenuActivity.this)
                                    .setTitle("باقات Premium")
                                    .setItems(items, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface d, int idx) {
                                            if (idx == items.length - 1) {
                                                activateTrial();
                                                return;
                                            }
                                            final String[] p = rows[idx].split("\\|");
                                            showTierPurchase(p[0], p[1]);
                                        }
                                    })
                                    .setNegativeButton(t(L10n.I_EXIT), null)
                                    .show();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            zone.setText(t(L10n.I_NET_ERR));
                        }
                    });
                }
            }
        }).start();
    }

    private void activateTrial() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String res = Api.premiumTrial(MenuActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
final boolean ok = res != null && res.startsWith("OK");
                            Sfx.play(ok ? "success" : "error");
                            new AlertDialog.Builder(MenuActivity.this)
                                    .setTitle("تفعيل التجربة")
                                    .setMessage(ok ? "تم تفعيل باقة التجربة 3 أيام مجانًا!" : (res == null ? t(L10n.I_NET_ERR) : res.replace("ERR:", "")))
                                    .setPositiveButton("OK", null).show();
                            Tts.say(ok ? "تم تفعيل باقة التجربة 3 أيام مجانًا" : "تعذر تفعيل باقة التجربة");
                            if (ok) refresh();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            zone.setText(t(L10n.I_NET_ERR));
                        }
                    });
                }
            }
        }).start();
    }

    private void showTierPurchase(final String tier, final String price) {
        final android.widget.EditText txInput = new android.widget.EditText(this);
        txInput.setSingleLine(true);
        txInput.setHint("رقم العملية من فودافون كاش");
        txInput.setTextColor(Color.WHITE);
        txInput.setBackgroundColor(Color.rgb(26, 36, 48));
        int pad = dp(12);
        txInput.setPadding(pad, pad, pad, pad);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(8), dp(16), dp(8));

        TextView info = new TextView(this);
        info.setText("باقة " + tier + "\nحوّل " + price + " ج.م على:\n01040814547\n\nمميزات Premium:\n• +50% عملات عند كل شراء\n• خصم 30% على الترقيات\n• شارة PR بجانب اسمك\n• أولوية في الدعم\n\nأدخل رقم العملية:");
        info.setTextSize(14);
        info.setTextColor(Color.WHITE);
        info.setPadding(0, 0, 0, dp(12));
        layout.addView(info);
        layout.addView(txInput);

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Premium — " + tier + " " + price + " ج.م");
        b.setView(layout);
        b.setPositiveButton("إرسال", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int w) {
                String txid = txInput.getText().toString().trim();
                if (txid.isEmpty()) {
                    new AlertDialog.Builder(MenuActivity.this)
                            .setTitle("أدخل رقم العملية")
                            .setPositiveButton("OK", null).show();
                    return;
                }
                final String id = txid;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final String res = Api.premiumRequest(MenuActivity.this, id);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    new AlertDialog.Builder(MenuActivity.this)
                                            .setTitle("تم الإرسال")
                                            .setMessage(res.replace("ERR:", "").replace("OK ", ""))
                                            .setPositiveButton("OK", null).show();
                                }
                            });
                        } catch (final Exception e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    new AlertDialog.Builder(MenuActivity.this)
                                            .setTitle(t(L10n.I_NET_ERR))
                                            .setPositiveButton("OK", null).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        });
        b.setNegativeButton(t(L10n.I_EXIT), null);
        b.show();
    }private void openTasks() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String all = Api.tasks(MenuActivity.this) + "\n" + Api.achievements(MenuActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            android.widget.TextView tv = new android.widget.TextView(MenuActivity.this);
                            tv.setTextSize(13);
                            tv.setTextColor(Color.WHITE);
                            tv.setPadding(dp(16), dp(10), dp(16), dp(10));
                            String[] lines = all.split("\n");
                            StringBuilder sb = new StringBuilder("المهام اليومية والأسبوعية\n");
                            for (String line : lines) {
                                String[] p = line.split("\\|");
                                if (p.length >= 7) {
                                    String done = "1".equals(p[6]) ? " (مكتملة)" : (Long.parseLong(p[3]) >= Long.parseLong(p[4]) ? " (جاهزة)" : "");
                                    sb.append("[").append(p[5]).append("] ").append(p[1]).append(": ").append(p[3]).append("/").append(p[4]).append(done).append("\n");
                                }
                            }
                            tv.setText(sb.toString());
                            new AlertDialog.Builder(MenuActivity.this)
                                    .setTitle("المهام والإنجازات")
                                    .setView(tv)
                                    .setPositiveButton("عجلة الحظ", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface d, int w) {
                                            spinWheel();
                                        }
                                    })
                                    .setNegativeButton("استلام", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface d, int w) {
                                            final android.widget.EditText input = new android.widget.EditText(MenuActivity.this);
                                            input.setHint("مثال: d_distance أو w_win أو first_race");
                                            new AlertDialog.Builder(MenuActivity.this)
                                                    .setTitle("معرّف المهمة/الإنجاز")
                                                    .setView(input)
                                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dd, int ww) {
                                                            final String id = input.getText().toString().trim();
                                                            if (id.isEmpty()) return;
                                                            new Thread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    try {
                                                                        final String r = (id.startsWith("d_") || id.startsWith("w_"))
                                                                                ? Api.taskClaim(MenuActivity.this, id)
                                                                                : Api.achievementClaim(MenuActivity.this, id);
                                                                        runOnUiThread(new Runnable() {
                                                                            @Override
                                                                            public void run() {
                                                                                toast(r);
                                                                            }
                                                                        });
                                                                    } catch (Exception e) {
                                                                        toast(t(L10n.I_NET_ERR));
                                                                    }
                                                                }
                                                            }).start();
                                                        }
                                                    }).show();
                                        }
                                    })
                                    .show();
                        }
                    });
                } catch (Exception e) {
                    toast(t(L10n.I_NET_ERR));
                }
            }
        }).start();
    }

    private void spinWheel() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String r = Api.wheelSpin(MenuActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (r.startsWith("ERR")) {
                                Sfx.play("error");
                                toast(r);
                            } else {
                                Sfx.play("year");
                                String[] p = r.split(" ");
                                toast("ربحت " + p[1] + " عملة!");
                                refresh();
                            }
                        }
                    });
                } catch (Exception e) {
                    toast(t(L10n.I_NET_ERR));
                }
            }
        }).start();
    }

        private void openDevPanel() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String st = Api.devStatus(MenuActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            boolean isDev = st != null && st.trim().equals("DEV");
                            if (!isDev) {
                                final android.widget.EditText pw = new android.widget.EditText(MenuActivity.this);
                                pw.setHint("كلمة سر المطور");
                                pw.setTextColor(Color.WHITE);
                                pw.setBackgroundColor(Color.rgb(26, 36, 48));
                                new AlertDialog.Builder(MenuActivity.this)
                                        .setTitle("تفعيل صلاحية المطور")
                                        .setView(pw)
                                        .setPositiveButton("تفعيل", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface d, int w) {
                                                activateDev(pw.getText().toString().trim());
                                            }
                                        })
                                        .setNegativeButton(t(L10n.I_EXIT), null).show();
                                return;
                            }
                            final String[] items = {
                                "1) منح / خصم عملات",
                                "2) ضبط رصيد عملات",
                                "3) إعادة تعيين حساب",
                                "4) منح باقة Premium",
                                "5) إعلان للجميع",
                                "6) إحصائيات السيرفر",
                                "7) قائمة المستخدمين",
                                "8) عرض سريع / خصم",
                                "9) حذف حساب (Ban)",
                                "10) باقات Premium المتاحة",
                                "11) سجن / فك سجن",
                                "12) حظر / فك حظر IP",
                                "13) بحث عن مستخدم",
                                "14) منح سيارة / شخصية / أداة",
                                "15) نسخة احتياطية",
                                "16) سجل الدخول (IP)"
                            };
                            new AlertDialog.Builder(MenuActivity.this)
                                    .setTitle("لوحة المطور")
                                    .setItems(items, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface d, int which) {
                                            devAction(which + 1);
                                        }
                                    })
                                    .setNegativeButton("عودة", null)
                                    .show();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            zone.setText(t(L10n.I_NET_ERR));
                        }
                    });
                }
            }
        }).start();
    }

    private void devAction(final int which) {
        final android.widget.EditText user = new android.widget.EditText(MenuActivity.this);
        user.setHint("اسم المستخدم");
        user.setTextColor(Color.WHITE);
        user.setBackgroundColor(Color.rgb(26, 36, 48));
        final android.widget.EditText val = new android.widget.EditText(MenuActivity.this);
        val.setTextColor(Color.WHITE);
        val.setBackgroundColor(Color.rgb(26, 36, 48));
        String title = "لوحة المطور";
        String posB = "تنفيذ";
        switch (which) {
            case 1:
                val.setHint("عدد العملات (سالب للخصم)");
                title = "منح / خصم عملات";
                break;
            case 2:
                val.setHint("الرصيد الجديد");
                title = "ضبط رصيد عملات";
                break;
            case 3:
                title = "إعادة تعيين حساب";
                posB = "إعادة تعيين";
                break;
            case 4:
                val.setHint("trial / bronze / silver / gold / vip");
                title = "منح باقة Premium";
                break;
            case 5:
                val.setHint("نص الإعلان");
                title = "إعلان للجميع";
                break;
            case 6:
                showDevResult("إحصائيات السيرفر", "loading...", false, null);
                return;
            case 7:
                showDevResult("قائمة المستخدمين", "loading...", false, null);
                return;
            case 8:
                val.setHint("اسم المنتج | نسبة الخصم مثلاً 0.3 | الدقائق");
                title = "عرض سريع / خصم";
                break;
            case 9:
                title = "حذف حساب (Ban)";
                posB = "حذف";
                break;
            case 10:
                showDevResult("باقات Premium", "loading...", false, null);
                return;
            case 11:
                val.setHint("عدد الدقائق (0 = فك سجن)");
                title = "سجن / فك سجن";
                break;
            case 12:
                val.setHint("الـ IP (اتركه فارغاً لاستخدام آخر IP للمستخدم)");
                title = "حظر / فك حظر IP";
                break;
            case 13:
                val.setHint("نص البحث (جزء من الاسم)");
                title = "بحث عن مستخدم";
                break;
            case 14:
                val.setHint("النوع|المعرّف  مثل car|sport   أو ch|legend   أو tool|nitro");
                title = "منح سيارة / شخصية / أداة";
                break;
            case 15:
                showDevResult("نسخة احتياطية", "loading...", false, null);
                return;
            case 16:
                val.setHint("اسم المستخدم (اختياري)");
                title = "سجل الدخول (IP)";
                break;
            default:
                return;
        }
        LinearLayout lay = new LinearLayout(MenuActivity.this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(dp(16), dp(6), dp(16), dp(6));
        lay.addView(user);
        if (which != 6 && which != 7 && which != 10) lay.addView(val);
        new AlertDialog.Builder(MenuActivity.this)
                .setTitle(title)
                .setView(lay)
                .setPositiveButton(posB, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        String u = user.getText().toString().trim();
                        String v = val.getText().toString().trim();
                        switch (which) {
                            case 1:
                                if (u.isEmpty() || v.isEmpty()) return;
                                doDevCall("grant", u, v, false);
                                break;
                            case 2:
                                if (u.isEmpty() || v.isEmpty()) return;
                                doDevCall("setcoins", u, v, false);
                                break;
                            case 3:
                                if (u.isEmpty()) return;
                                doDevCall("reset", u, "", false);
                                break;
                            case 4:
                                if (u.isEmpty() || v.isEmpty()) return;
                                doDevCall("premgrant", u, v, false);
                                break;
                            case 5:
                                showDevResult("إعلان", "loading...", false, u + "|" + v);
                                return;
                            case 8:
                                if (v.isEmpty()) return;
                                doDevCall("flashsale", u, v, false);
                                break;
                            case 9:
                                if (u.isEmpty()) return;
                                doDevCall("ban", u, "", false);
                                break;
                            case 11:
                                if (u.isEmpty() || v.isEmpty()) return;
                                if (v.equals("0")) doDevCall("unjail", u, "", false);
                                else doDevCall("jail", u, v, false);
                                break;
                            case 12:
                                if (v.equals("0")) doDevCall("unbanip", u, "", false);
                                else doDevCall("banip", u, v, false);
                                break;
                            case 13:
                                showDevResult("بحث", "loading...", false, v);
                                return;
                            case 14:
                                if (v.isEmpty()) return;
                                doDevCall("give", u, v, false);
                                break;
                            case 16:
                                showDevResult("سجل IP", "loading...", false, v);
                                return;
                        }
                    }
                })
                .setNegativeButton(t(L10n.I_EXIT), null)
                .show();
    }

    private void doDevCall(final String action, final String user, final String val, final boolean fake) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String res;
                    if (action.equals("grant")) res = Api.devGrant(MenuActivity.this, user, (int) Long.parseLong(val.trim()));
                    else if (action.equals("setcoins")) res = Api.devSetCoins(MenuActivity.this, user, Long.parseLong(val.trim()));
                    else if (action.equals("reset")) res = Api.devReset(MenuActivity.this, user);
                    else if (action.equals("premgrant")) res = Api.devPremiumGrant(MenuActivity.this, user, val.trim());
                    else if (action.equals("ban")) res = Api.devBan(MenuActivity.this, user);
                    else if (action.equals("jail")) res = Api.devJail(MenuActivity.this, user, Long.parseLong(val.trim()));
                    else if (action.equals("unjail")) res = Api.devUnjail(MenuActivity.this, user);
                    else if (action.equals("banip")) res = Api.devBanIp(MenuActivity.this, val.trim());
                    else if (action.equals("unbanip")) res = Api.devUnbanIp(MenuActivity.this, val.trim());
                    else if (action.equals("give")) {
                        String[] parts = val.split("\\|");
                        if (parts.length < 2) res = "ERR: صيغة خاطئة";
                        else res = Api.devGive(MenuActivity.this, user, parts[0].trim(), parts[1].trim());
                    }
                    else if (action.equals("flashsale")) {
                        String[] parts = val.split("\\|");
                        String item = parts.length > 0 ? parts[0].trim() : "";
                        double disc = parts.length > 1 ? Double.parseDouble(parts[1].trim()) : 0.3;
                        long mins = parts.length > 2 ? Long.parseLong(parts[2].trim()) : 60;
                        res = Api.devFlashSale(MenuActivity.this, item.isEmpty() ? "nitro_pack" : item, disc, mins);
                    }
                    else res = "ERR: إجراء غير معروف";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showDevResult("لوحة المطور", res, res.startsWith("ERR"), null);
                            Tts.say(res);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showDevResult("لوحة المطور", t(L10n.I_NET_ERR), true, null);
                        }
                    });
                }
            }
        }).start();
    }

    private void showDevResult(final String title, final String msg, final boolean err, final String extra) {
        if ("باقات Premium".equals(title)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final String tiers = Api.premiumTiers(MenuActivity.this);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new AlertDialog.Builder(MenuActivity.this)
                                        .setTitle(title)
                                        .setMessage(tiers == null ? "" : tiers)
                                        .setPositiveButton("OK", null).show();
                            }
                        });
                    } catch (final Exception e1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() { zone.setText(t(L10n.I_NET_ERR)); }
                        });
                    }
                }
            }).start();
            return;
        }
        if ("قائمة المستخدمين".equals(title)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final String users = Api.devUsers(MenuActivity.this);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new AlertDialog.Builder(MenuActivity.this)
                                        .setTitle(title)
                                        .setMessage(users == null ? "" : users)
                                        .setPositiveButton("OK", null).show();
                            }
                        });
                    } catch (final Exception e1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() { zone.setText(t(L10n.I_NET_ERR)); }
                        });
                    }
                }
            }).start();
            return;
        }
        if ("إحصائيات السيرفر".equals(title)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final String stats = Api.devStats(MenuActivity.this);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new AlertDialog.Builder(MenuActivity.this)
                                        .setTitle(title)
                                        .setMessage(stats == null ? "" : stats.replace("|", "\n"))
                                        .setPositiveButton("OK", null).show();
                            }
                        });
                    } catch (final Exception e1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() { zone.setText(t(L10n.I_NET_ERR)); }
                        });
                    }
                }
            }).start();
            return;
        }
        if ("إعلان".equals(title) && extra != null) {
            String[] parts = extra.split("\\|", 2);
            final String u = parts.length > 0 ? parts[0] : "";
            final String v = parts.length > 1 ? parts[1] : "";
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final String res = Api.devAnnounce(MenuActivity.this, v != null ? v : "");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new AlertDialog.Builder(MenuActivity.this)
                                        .setTitle("إعلان")
                                        .setMessage(res)
                                        .setPositiveButton("OK", null).show();
                            }
                        });
                    } catch (final Exception e1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() { zone.setText(t(L10n.I_NET_ERR)); }
                        });
                    }
                }
            }).start();
            return;
        }
        if ("بحث".equals(title)) {
            final String q = extra == null ? "" : extra;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final String res = Api.devSearch(MenuActivity.this, q);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new AlertDialog.Builder(MenuActivity.this)
                                        .setTitle("نتيجة البحث")
                                        .setMessage(res == null ? "" : res.replace("|", "\n"))
                                        .setPositiveButton("OK", null).show();
                            }
                        });
                    } catch (final Exception e1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() { zone.setText(t(L10n.I_NET_ERR)); }
                        });
                    }
                }
            }).start();
            return;
        }
        if ("سجل IP".equals(title)) {
            final String u = extra == null ? "" : extra;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final String res = Api.devIpLog(MenuActivity.this, u);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new AlertDialog.Builder(MenuActivity.this)
                                        .setTitle("سجل الدخول")
                                        .setMessage(res == null ? "" : res)
                                        .setPositiveButton("OK", null).show();
                            }
                        });
                    } catch (final Exception e1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() { zone.setText(t(L10n.I_NET_ERR)); }
                        });
                    }
                }
            }).start();
            return;
        }
        if ("نسخة احتياطية".equals(title)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final String res = Api.devBackup(MenuActivity.this);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new AlertDialog.Builder(MenuActivity.this)
                                        .setTitle("نسخة احتياطية")
                                        .setMessage(res == null ? "" : res)
                                        .setPositiveButton("OK", null).show();
                            }
                        });
                    } catch (final Exception e1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() { zone.setText(t(L10n.I_NET_ERR)); }
                        });
                    }
                }
            }).start();
            return;
        }
        new AlertDialog.Builder(MenuActivity.this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("OK", null).show();
    }

    private void activateDev(final String pw) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String res = Api.devActivate(MenuActivity.this, pw);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(MenuActivity.this)
                                    .setTitle("Developer")
                                    .setMessage(res.replace("ERR:", ""))
                                    .setPositiveButton("OK", null).show();
                            Tts.say(res);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            zone.setText(t(L10n.I_NET_ERR));
                        }
                    });
                }
            }
        }).start();
    }

    private void grant(final String user, final String coins) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int amt;
                    try {
                        amt = Integer.parseInt(coins.trim());
                    } catch (Exception ex) {
                        return;
                    }
                    final String res = Api.devGrant(MenuActivity.this, user, amt);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(MenuActivity.this)
                                    .setTitle("Developer")
                                    .setMessage(res.replace("ERR:", ""))
                                    .setPositiveButton("OK", null).show();
                            Tts.say(res);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            zone.setText(t(L10n.I_NET_ERR));
                        }
                    });
                }
            }
        }).start();
    }

    private void ban(final String user) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String res = Api.devBan(MenuActivity.this, user);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(MenuActivity.this)
                                    .setTitle("Developer")
                                    .setMessage(res.replace("ERR:", ""))
                                    .setPositiveButton("OK", null).show();
                            Tts.say(res);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            zone.setText(t(L10n.I_NET_ERR));
                        }
                    });
                }
            }
        }).start();
    }
private void editServer() {
        final SharedPreferences p = getSharedPreferences("fastcar", Context.MODE_PRIVATE);
        final android.widget.EditText in = new android.widget.EditText(this);
        in.setSingleLine(true);
        in.setText(Api.getServer(this));
        in.setTextColor(Color.WHITE);
        in.setBackgroundColor(Color.rgb(26, 36, 48));
        int pad = dp(12);
        in.setPadding(pad, pad, pad, pad);
        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle(t(L10n.I_SERVER))
                .setView(in)
                .setPositiveButton(t(L10n.I_SELECT), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dd, int w) {
                        Api.setServer(MenuActivity.this, in.getText().toString());
                        zone.setText(Api.getServer(MenuActivity.this));
                        refresh();
                    }
                })
                .setNegativeButton(t(L10n.I_EXIT), null)
                .create();
        d.show();
    }

    private String fix(String res) {
        return res == null ? t(L10n.I_NET_ERR) : res.replace("ERR:", "");
    }

    private String t(int key) {
        return L10n.get(lang, key);
    }

    private String t(String key) {
        return L10n.get(lang, key);
    }

    private Button btn(String text, int bg, int fg, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(17);
        b.setTextColor(fg);
        b.setBackgroundColor(bg);
        b.setPadding(dp(10), dp(14), dp(10), dp(14));
        b.setMinHeight(dp(56));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        b.setLayoutParams(lp);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                if (l != null) l.onClick(v);
            }
        });
        return b;
    }

    private void toast(String s) {
        android.widget.Toast.makeText(this, s == null ? "" : s, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void spacer(LinearLayout root, int h) {
        TextView s = new TextView(this);
        s.setHeight(dp(h));
        root.addView(s);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
