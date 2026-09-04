package com.fastcar.racing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.fastcar.common.L10n;

public class RoomsActivity extends Activity {

    private String lang;
    private TextView roomsView;
    private LinearLayout listBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lang = Api.getLang(this);
        Api.loadSession(this);
        Tts.init(this);
        Sfx.init(this);

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(22), dp(20), dp(22));
        scroll.addView(box, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("Online Rooms / غرف اللعب");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(255, 213, 79));
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        TextView hint = new TextView(this);
        hint.setText("غرفة عامة: أي لاعب يدخل معاك. غرفة خاصة: كود + كلمة سر لأصحابك فقط.");
        hint.setTextSize(13);
        hint.setTextColor(Color.rgb(169, 199, 255));
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(6), 0, dp(14));
        box.addView(hint);

        box.addView(btn("دخول عشوائي (Quick Match)", Color.rgb(255, 213, 79), Color.rgb(16, 19, 26), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
Tts.say("دخول عشوائي");
                Sfx.play("click");
                quickMatch();
            }
        }));
        box.addView(btn("إنشاء غرفة", Color.rgb(56, 160, 80), Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
Tts.say("إنشاء غرفة");
                Sfx.play("click");
                createRoom();
            }
        }));
        box.addView(btn("الدخول بالكود", Color.rgb(76, 161, 175), Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
Tts.say("الدخول بالكود");
                Sfx.play("click");
                joinByCode();
            }
        }));
        box.addView(btn("إدارة غرفتي", Color.rgb(120, 90, 160), Color.WHITE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
Tts.say("إدارة الغرفة");
                Sfx.play("click");
                manageRoom();
            }
        }));

        TextView lbl = new TextView(this);
        lbl.setText("الغرف العامة المفتوحة:");
        lbl.setTextSize(15);
        lbl.setTypeface(Typeface.DEFAULT_BOLD);
        lbl.setTextColor(Color.rgb(216, 230, 255));
        lbl.setPadding(0, dp(16), 0, dp(8));
        box.addView(lbl);

        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        box.addView(listBox);

        roomsView = new TextView(this);
        roomsView.setTextSize(14);
        roomsView.setTextColor(Color.rgb(200, 210, 230));
        listBox.addView(roomsView);

        Button back = new Button(this);
        back.setText("رجوع");
        back.setTextColor(Color.WHITE);
        back.setBackgroundColor(Color.rgb(40, 52, 74));
        back.setAllCaps(false);
        back.setTextSize(16);
back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                finish();
            }
        });
        box.addView(back);

        scroll.setBackgroundColor(Color.rgb(16, 19, 26));
        setContentView(scroll);
        refreshList();
    }

    private void refreshList() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String res = Api.roomList(RoomsActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            renderList(res);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            roomsView.setText(t(L10n.I_NET_ERR));
                        }
                    });
                }
            }
        }).start();
    }

    private void renderList(String res) {
        listBox.removeAllViews();
        if (res == null || res.startsWith("ERR") || res.equals("(empty)") || res.trim().isEmpty()) {
            roomsView = new TextView(this);
            roomsView.setText("لا توجد غرف عامة الآن. أنشئ غرفة أو ادخل عشوائيًا!");
            roomsView.setTextSize(14);
            roomsView.setTextColor(Color.rgb(200, 210, 230));
            listBox.addView(roomsView);
            return;
        }
        String[] lines = res.split("\n");
        for (String line : lines) {
            String[] p = line.split("\\|");
            if (p.length < 4) continue;
            final String code = p[0];
            final String name = p[1];
            final String owner = p[2];
            final String count = p[3];
            TextView row = new TextView(this);
            row.setText("" + name + "  —  " + owner + "   " + count + "/6");
            row.setTextSize(15);
            row.setTextColor(Color.rgb(220, 230, 255));
            row.setPadding(0, dp(6), 0, dp(6));
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
Tts.say("انضمام لغرفة " + name);
                    Sfx.play("click");
                    joinPublic(code);
                }
            });
            listBox.addView(row);
        }
    }

    private void joinPublic(final String code) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String res = Api.roomJoin(RoomsActivity.this, code, "");
                    runOnUiThread(new Runnable() {
                        @Override
public void run() {
                            if (res.startsWith("OK")) Sfx.play("success");
                            else Sfx.play("error");
                            toast(res);
                            refreshList();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toast(t(L10n.I_NET_ERR));
                        }
                    });
                }
            }
        }).start();
    }

    private void quickMatch() {
        new Thread(new Runnable() {
            @Override
            public void run() {
try {
                    final String res = Api.roomRandom(RoomsActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (res.startsWith("OK")) Sfx.play("success");
                            else Sfx.play("error");
                            toast(res);
                            refreshList();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toast(t(L10n.I_NET_ERR));
                        }
                    });
                }
            }
        }).start();
    }

    private void createRoom() {
        final EditText nameIn = new EditText(this);
        nameIn.setHint("اسم الغرفة");
        nameIn.setTextColor(Color.WHITE);
        nameIn.setBackgroundColor(Color.rgb(26, 36, 48));
        final EditText passIn = new EditText(this);
        passIn.setHint("كلمة السر (اتركها فارغة لغرفة عامة)");
        passIn.setTextColor(Color.WHITE);
        passIn.setBackgroundColor(Color.rgb(26, 36, 48));

        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(dp(16), dp(6), dp(16), dp(6));
        lay.addView(nameIn);
        lay.addView(passIn);

        new AlertDialog.Builder(this)
                .setTitle("إنشاء غرفة")
                .setView(lay)
                .setPositiveButton("إنشاء", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        String name = (nameIn.getText() == null ? "" : nameIn.getText().toString()).trim();
                        String pass = (passIn.getText() == null ? "" : passIn.getText().toString()).trim();
                        if (name.isEmpty()) name = "غرفتي";
                        doCreate(name, pass);
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void doCreate(final String name, final String pass) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String res = Api.roomCreate(RoomsActivity.this, name, pass);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
String show = pass.isEmpty()
                                    ? "تم إنشاء غرفة عامة"
                                    : "غرفة خاصة تم إنشاؤها. رمز الدعوة: " + res.replace("OK ", "") + "\nشاركه مع أصحابك مع كلمة السر.";
                            Sfx.play("event");
                            new AlertDialog.Builder(RoomsActivity.this)
                                    .setTitle("تم")
                                    .setMessage(show)
                                    .setPositiveButton("OK", null).show();
                            Tts.say(show);
                            refreshList();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toast(t(L10n.I_NET_ERR));
                        }
                    });
                }
            }
        }).start();
    }

    private void joinByCode() {
        final EditText codeIn = new EditText(this);
        codeIn.setHint("كود الغرفة");
        codeIn.setTextColor(Color.WHITE);
        codeIn.setBackgroundColor(Color.rgb(26, 36, 48));
        final EditText passIn = new EditText(this);
        passIn.setHint("كلمة السر");
        passIn.setTextColor(Color.WHITE);
        passIn.setBackgroundColor(Color.rgb(26, 36, 48));

        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(dp(16), dp(6), dp(16), dp(6));
        lay.addView(codeIn);
        lay.addView(passIn);

        new AlertDialog.Builder(this)
                .setTitle("الدخول بالكود")
                .setView(lay)
                .setPositiveButton("دخول", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        String code = (codeIn.getText() == null ? "" : codeIn.getText().toString()).trim();
                        String pass = (passIn.getText() == null ? "" : passIn.getText().toString()).trim();
                        doJoin(code, pass);
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void doJoin(final String code, final String pass) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String res = Api.roomJoin(RoomsActivity.this, code, pass);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toast(res);
                            refreshList();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toast(t(L10n.I_NET_ERR));
                        }
                    });
                }
            }
        }).start();
    }

    private long voiceSeen;

    private void recordAndBroadcast() {
        new Thread(new Runnable() { public void run() {
            try {
                String res = Api.voiceSend(RoomsActivity.this, currentChannel(), "demo");
                final String msg = res;
                runOnUiThread(new Runnable() { public void run() { toast(msg); } });
            } catch (Exception ex) { toast("خطأ البث: " + ex.getMessage()); }
        } }).start();
    }

    private void addVoiceRow(final LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button bT = new Button(this);
        bT.setText("بث");
        bT.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { recordAndBroadcast(); } });
        Button bR = new Button(this);
        bR.setText("استقبال");
        bR.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { receiveAndPlay(); } });
        row.addView(bT);
        row.addView(bR);
        parent.addView(row);
    }

    private void receiveAndPlay() {
        new Thread(new Runnable() { public void run() {
            try {
                final String res = Api.voiceLatest(RoomsActivity.this, currentChannel(), voiceSeen);
                if (res == null || res.startsWith("ERR") || res.startsWith("(empty)")) { runOnUiThread(new Runnable() { public void run() { toast("لا يوجد صوت جديد"); } }); return; }
                toast("تم الاستلام");
            } catch (Exception ex) { toast("خطأ: " + ex.getMessage()); }
        } }).start();
    }

    private String currentChannel() {
        try {
            String code = Api.roomState(RoomsActivity.this);
            if (code != null && code.startsWith("OK ")) { String[] p = code.split(" "); if (p.length >= 2 && !p[1].isEmpty()) return "room:" + p[1]; }
        } catch (Exception ignored) {}
        return "global";
    }

    private void manageRoom() {
        final EditText mem = new EditText(this);
        mem.setHint("اسم اللاعب");
        mem.setTextColor(Color.WHITE);
        mem.setBackgroundColor(Color.rgb(26, 36, 48));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(16), dp(6), dp(16), dp(6));
        row.addView(mem);
        addVoiceRow(row);

        final String[][] ops = {
                {"دعوة", "invite"},
                {"مشرف", "promote"},
                {"كتم", "mute"},
                {"فك كتم", "unmute"},
                {"طرد", "kick"},
                {"حظر", "ban"},
                {"فك حظر", "unban"},
                {"نقل ملكية", "transfer"},
                {"بلاغ", "report"}
        };
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("إدارة الغرفة");
        b.setView(row);
        String[] labels = new String[ops.length];
        for (int i = 0; i < ops.length; i++) labels[i] = ops[i][0];
        b.setItems(labels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int w) {
                final String who = mem.getText().toString().trim();
                if (who.isEmpty()) {
                    toast("ادخل اسم اللاعب أولاً");
                    return;
                }
                runManageOp(ops[w][1], who);
            }
        });
        b.setPositiveButton("إعدادات", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int w) {
                roomConfigDialog();
            }
        });
        b.setNegativeButton("إلغاء", null);
        b.show();
    }

    private void roomConfigDialog() {
        final EditText nameIn = new EditText(this);
        nameIn.setHint("اسم الغرفة");
        nameIn.setTextColor(Color.WHITE);
        nameIn.setBackgroundColor(Color.rgb(26, 36, 48));
        final EditText passIn = new EditText(this);
        passIn.setHint("كلمة السر (فارغ = عامة)");
        passIn.setTextColor(Color.WHITE);
        passIn.setBackgroundColor(Color.rgb(26, 36, 48));
        final EditText maxIn = new EditText(this);
        maxIn.setHint("الحد الأقصى (2-16)");
        maxIn.setText("6");
        maxIn.setTextColor(Color.WHITE);
        maxIn.setBackgroundColor(Color.rgb(26, 36, 48));

        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(dp(16), dp(6), dp(16), dp(6));
        lay.addView(nameIn);
        lay.addView(passIn);
        lay.addView(maxIn);

        new AlertDialog.Builder(this)
                .setTitle("إعدادات الغرفة")
                .setView(lay)
                .setPositiveButton("حفظ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        String name = nameIn.getText().toString().trim();
                        String pass = passIn.getText().toString().trim();
                        int max = 6;
                        try {
                            max = Integer.parseInt(maxIn.getText().toString().trim());
                        } catch (Exception ignored) {
                        }
                        runManageOp("config", name + "\u0001" + pass + "\u0001" + max);
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void runManageOp(final String op, final String who) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String res;
                    if (op.equals("config")) {
                        String[] p = who.split("\u0001");
                        res = Api.roomConfig(RoomsActivity.this, p.length > 0 ? p[0] : "", p.length > 1 ? p[1] : "", p.length > 2 ? Integer.parseInt(p[2]) : 6);
                    } else if (op.equals("invite")) res = Api.roomInvite(RoomsActivity.this, who);
                    else if (op.equals("promote")) res = Api.roomPromote(RoomsActivity.this, who);
                    else if (op.equals("mute")) res = Api.roomMute(RoomsActivity.this, who);
                    else if (op.equals("unmute")) res = Api.roomUnmute(RoomsActivity.this, who);
                    else if (op.equals("kick")) res = Api.roomKick(RoomsActivity.this, who);
                    else if (op.equals("ban")) res = Api.roomBan(RoomsActivity.this, who);
                    else if (op.equals("unban")) res = Api.roomUnban(RoomsActivity.this, who);
                    else if (op.equals("transfer")) res = Api.roomTransfer(RoomsActivity.this, who);
                    else res = Api.roomReport(RoomsActivity.this, who);
                    final String r = res;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toast(r);
                            Tts.say(r);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toast(t(L10n.I_NET_ERR));
                        }
                    });
                }
            }
        }).start();
    }

    private String t(int key) {
        return L10n.get(lang, key);
    }

    private void toast(String s) {
        Toast.makeText(this, s == null ? "" : s.replace("ERR:", ""), Toast.LENGTH_LONG).show();
    }

    private Button btn(String text, int bg, int fg, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(17);
        b.setTextColor(fg);
        b.setBackgroundColor(bg);
        b.setPadding(dp(10), dp(14), dp(10), dp(14));
        b.setMinHeight(dp(54));
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        b.setLayoutParams(lp);
        return b;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
