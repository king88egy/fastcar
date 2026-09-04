package com.fastcar.racing;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fastcar.common.L10n;

public class MainActivity extends Activity {

    private static final int MODE_LOGIN = 0;
    private static final int MODE_REG_USER = 1;
    private static final int MODE_REG_EMAIL = 2;
    private static final int MODE_REG_PASS = 3;
    private static final int MODE_REG_GENDER = 4;
    private static final int MODE_VERIFY = 5;
    private static final int MODE_FORGOT = 6;

    private LinearLayout root;
    private ScrollView scroll;
    private TextView title;
    private TextView sub;
    private TextView status;
    private EditText inUser;
    private EditText inPass;
    private EditText inEmail;
    private EditText inCode;
    private EditText inForgotUser;
    private EditText inForgotEmail;
    private RadioGroup genderGroup;
    private Button btnPrimary;
    private Button btnSecondary;
    private Button btnTertiary;
    private String lang;
    private int mode = MODE_LOGIN;
    private String regUser = "";
    private String regEmail = "";
    private String regPass = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Sfx.init(this);
        lang = Api.getLang(this);

        Api.loadSession(this);
        if (!Api.token.isEmpty() && !Api.username.isEmpty()) {
            openMenu();
            return;
        }

        scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(16, 19, 26));
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(26), dp(30), dp(26), dp(26));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        title = new TextView(this);
        title.setText("Fast Car");
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.rgb(255, 213, 79));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        sub = new TextView(this);
        sub.setTextSize(15);
        sub.setGravity(Gravity.CENTER);
        sub.setTextColor(Color.rgb(216, 230, 255));
        root.addView(sub);

        spacer(root, 20);

        showLogin();

        setContentView(scroll);
    }

    private String t(int key) {
        return L10n.get(lang, key);
    }

    private void clearDynamic() {
        root.removeView(title);
        root.removeView(sub);
        if (inUser != null) root.removeView(inUser);
        if (inPass != null) root.removeView(inPass);
        if (inEmail != null) root.removeView(inEmail);
        if (inCode != null) root.removeView(inCode);
        if (inForgotUser != null) root.removeView(inForgotUser);
        if (inForgotEmail != null) root.removeView(inForgotEmail);
        if (genderGroup != null) root.removeView(genderGroup);
        if (btnPrimary != null) root.removeView(btnPrimary);
        if (btnSecondary != null) root.removeView(btnSecondary);
        if (btnTertiary != null) root.removeView(btnTertiary);
        if (status != null) root.removeView(status);
        inUser = null;
        inPass = null;
        inEmail = null;
        inCode = null;
        inForgotUser = null;
        inForgotEmail = null;
        genderGroup = null;
        btnPrimary = null;
        btnSecondary = null;
        btnTertiary = null;
        status = null;
        root.removeAllViews();
        root.addView(title);
        root.addView(sub);
    }

    private void showLogin() {
        clearDynamic();
        mode = MODE_LOGIN;
        title.setText("Fast Car");
        sub.setText(t(L10n.I_READY));

        spacer(root, 18);

        root.addView(label(t(L10n.I_USERNAME)));
        inUser = new EditText(this);
        inUser.setSingleLine(true);
        inUser.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        inUser.setHint(t(L10n.I_USERNAME));
        styleInput(inUser);
        root.addView(inUser);

        root.addView(label(t(L10n.I_PASS)));
        inPass = new EditText(this);
        inPass.setSingleLine(true);
        inPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        inPass.setHint(t(L10n.I_PASS));
        inPass.setImeOptions(EditorInfo.IME_ACTION_DONE);
        styleInput(inPass);
        root.addView(inPass);

        spacer(root, 14);

        btnPrimary = goldButton("" + t(L10n.I_LOGIN));
        btnPrimary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                doLogin();
            }
        });
        root.addView(btnPrimary, lpH());

        btnSecondary = blueButton("+ " + t(L10n.I_REGISTER));
        btnSecondary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                showRegStep1();
            }
        });
        root.addView(btnSecondary, lpH());

        spacer(root, 6);

        btnTertiary = textButton("\u0646\u0633\u064A\u062A \u0643\u0644\u0645\u0629 \u0627\u0644\u0645\u0631\u0648\u0631");
        btnTertiary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                showForgotPassword();
            }
        });
        root.addView(btnTertiary, lpH());

        spacer(root, 4);

        status = new TextView(this);
        status.setText(t(L10n.I_LOGIN) + " / " + t(L10n.I_REGISTER));
        status.setTextSize(15);
        status.setTextColor(Color.rgb(169, 199, 255));
        status.setLineSpacing(0, 1.1f);
        root.addView(status);
    }

    private void showRegStep1() {
        clearDynamic();
        mode = MODE_REG_USER;
        title.setText("\u062A\u0633\u062C\u064A\u0644 \u062D\u0633\u0627\u0628 \u062C\u062F\u064A\u062F");
        sub.setText("\u0627\u0644\u062E\u0637\u0648\u0637\u0629 1 \u0645\u0646 5 \u2014 \u0627\u0633\u0645 \u0627\u0644\u0645\u0633\u062A\u062E\u062F\u0645");

        spacer(root, 18);
        root.addView(label(t(L10n.I_USERNAME)));
        inUser = new EditText(this);
        inUser.setSingleLine(true);
        inUser.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        inUser.setHint("\u0645\u0646 3 \u0625\u0644\u0649 20 \u062D\u0631\u0641");
        styleInput(inUser);
        root.addView(inUser);

        spacer(root, 14);
        btnPrimary = goldButton("\u0627\u0644\u062A\u0627\u0644\u064A");
        btnPrimary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                doCheckUser();
            }
        });
        root.addView(btnPrimary, lpH());

        btnSecondary = blueButton("\u0631\u062C\u0648\u0639");
        btnSecondary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                showLogin();
            }
        });
        root.addView(btnSecondary, lpH());

        spacer(root, 4);
        addStatus();
    }

    private void showRegStep2() {
        clearDynamic();
        mode = MODE_REG_EMAIL;
        title.setText("\u062A\u0633\u062C\u064A\u0644 \u062D\u0633\u0627\u0628 \u062C\u062F\u064A\u062F");
        sub.setText("\u0627\u0644\u062E\u0637\u0648\u0637\u0629 2 \u0645\u0646 5 \u2014 \u0627\u0644\u0628\u0631\u064A\u062F \u0627\u0644\u0625\u0644\u0643\u062A\u0631\u0648\u0646\u064A");

        spacer(root, 18);
        root.addView(label("\u0627\u0644\u0628\u0631\u064A\u062F \u0627\u0644\u0625\u0644\u0643\u062A\u0631\u0648\u0646\u064A"));
        inEmail = new EditText(this);
        inEmail.setSingleLine(true);
        inEmail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        inEmail.setHint("example@email.com");
        styleInput(inEmail);
        root.addView(inEmail);

        spacer(root, 14);
        btnPrimary = goldButton("\u0627\u0644\u062A\u0627\u0644\u064A");
        btnPrimary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                doCheckEmail();
            }
        });
        root.addView(btnPrimary, lpH());

        btnSecondary = blueButton("\u0631\u062C\u0648\u0639");
        btnSecondary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                showRegStep1();
            }
        });
        root.addView(btnSecondary, lpH());

        spacer(root, 4);
        addStatus();
    }

    private void showRegStep3() {
        clearDynamic();
        mode = MODE_REG_PASS;
        title.setText("\u062A\u0633\u062C\u064A\u0644 \u062D\u0633\u0627\u0628 \u062C\u062F\u064A\u062F");
        sub.setText("\u0627\u0644\u062E\u0637\u0648\u0637\u0629 3 \u0645\u0646 5 \u2014 \u0643\u0644\u0645\u0629 \u0627\u0644\u0645\u0631\u0648\u0631");

        spacer(root, 18);
        root.addView(label(t(L10n.I_PASS)));
        inPass = new EditText(this);
        inPass.setSingleLine(true);
        inPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        inPass.setHint("\u0645\u0646 4 \u0623\u062D\u0631\u0641 \u0641\u0648\u0642");
        styleInput(inPass);
        root.addView(inPass);

        spacer(root, 14);
        btnPrimary = goldButton("\u0627\u0644\u062A\u0627\u0644\u064A");
        btnPrimary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                doRegPass();
            }
        });
        root.addView(btnPrimary, lpH());

        btnSecondary = blueButton("\u0631\u062C\u0648\u0639");
        btnSecondary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                showRegStep2();
            }
        });
        root.addView(btnSecondary, lpH());

        spacer(root, 4);
        addStatus();
    }

    private void showRegStep4() {
        clearDynamic();
        mode = MODE_REG_GENDER;
        title.setText("\u062A\u0633\u062C\u064A\u0644 \u062D\u0633\u0627\u0628 \u062C\u062F\u064A\u062F");
        sub.setText("\u0627\u0644\u062E\u0637\u0648\u0637\u0629 4 \u0645\u0646 5 \u2014 \u0627\u0644\u062C\u0646\u0633");

        spacer(root, 18);

        TextView lbl = new TextView(this);
        lbl.setText("\u0627\u062E\u062A\u0631 \u062C\u0646\u0633\u0643");
        lbl.setTextSize(15);
        lbl.setTypeface(Typeface.DEFAULT_BOLD);
        lbl.setTextColor(Color.rgb(216, 230, 255));
        lbl.setPadding(0, dp(14), 0, dp(8));
        root.addView(lbl);

        genderGroup = new RadioGroup(this);
        genderGroup.setOrientation(RadioGroup.VERTICAL);

        RadioButton rbMale = new RadioButton(this);
        rbMale.setText("\u0630\u0643\u0631");
        rbMale.setTextSize(16);
        rbMale.setTextColor(Color.WHITE);
        rbMale.setId(View.generateViewId());
        genderGroup.addView(rbMale);

        RadioButton rbFemale = new RadioButton(this);
        rbFemale.setText("\u0623\u0646\u062B\u0649");
        rbFemale.setTextSize(16);
        rbFemale.setTextColor(Color.WHITE);
        rbFemale.setId(View.generateViewId());
        genderGroup.addView(rbFemale);

        rbMale.setChecked(true);
        root.addView(genderGroup);

        spacer(root, 14);
        btnPrimary = goldButton("\u0627\u0644\u062A\u0633\u062C\u064A\u0644");
        btnPrimary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                doRegisterFull();
            }
        });
        root.addView(btnPrimary, lpH());

        btnSecondary = blueButton("\u0631\u062C\u0648\u0639");
        btnSecondary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                showRegStep3();
            }
        });
        root.addView(btnSecondary, lpH());

        spacer(root, 4);
        addStatus();
    }

    private void showVerify() {
        clearDynamic();
        mode = MODE_VERIFY;
        title.setText("\u062A\u062D\u0642\u0642 \u0645\u0646 \u0627\u0644\u0628\u0631\u064A\u062F");
        sub.setText("\u0627\u0644\u062E\u0637\u0648\u0637\u0629 5 \u0645\u0646 5 \u2014 \u0627\u0644\u0631\u0645\u0632 \u0627\u0644\u0633\u0639\u064A\u062F");

        spacer(root, 18);

        TextView info = new TextView(this);
        info.setText("\u062A\u0645 \u0625\u0631\u0633\u0627\u0644 \u0631\u0645\u0632 \u0639\u062F\u062F \u0625\u0644\u0649:\n" + regEmail);
        info.setTextSize(14);
        info.setTextColor(Color.rgb(180, 200, 230));
        info.setLineSpacing(0, 1.2f);
        info.setPadding(0, dp(8), 0, dp(12));
        root.addView(info);

        root.addView(label("\u0627\u0644\u0631\u0645\u0632 \u0627\u0644\u0633\u0639\u064A\u062F"));
        inCode = new EditText(this);
        inCode.setSingleLine(true);
        inCode.setInputType(InputType.TYPE_CLASS_NUMBER);
        inCode.setHint("000000");
        styleInput(inCode);
        root.addView(inCode);

        spacer(root, 14);
        btnPrimary = goldButton("\u062A\u063A\u064A\u064A\u0646 \u0627\u0644\u062D\u0633\u0627\u0628");
        btnPrimary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                doVerifyEmail();
            }
        });
        root.addView(btnPrimary, lpH());

        btnSecondary = blueButton("\u0631\u062C\u0648\u0639 \u0644\u0644\u062F\u062E\u0648\u0644");
        btnSecondary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                showLogin();
            }
        });
        root.addView(btnSecondary, lpH());

        spacer(root, 4);
        addStatus();
    }

    private void showForgotPassword() {
        clearDynamic();
        mode = MODE_FORGOT;
        title.setText("\u0646\u0633\u064A\u062A \u0643\u0644\u0645\u0629 \u0627\u0644\u0645\u0631\u0648\u0631");
        sub.setText("\u0627\u0643\u062A\u0628 \u0627\u0633\u0645 \u0627\u0644\u0645\u0633\u062A\u062E\u062F\u0645 \u0648\u0627\u0644\u0628\u0631\u064A\u062F \u0627\u0644\u0625\u0644\u0643\u062A\u0631\u0648\u0646\u064A");

        spacer(root, 18);
        root.addView(label(t(L10n.I_USERNAME)));
        inForgotUser = new EditText(this);
        inForgotUser.setSingleLine(true);
        inForgotUser.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        inForgotUser.setHint(t(L10n.I_USERNAME));
        styleInput(inForgotUser);
        root.addView(inForgotUser);

        root.addView(label("\u0627\u0644\u0628\u0631\u064A\u062F \u0627\u0644\u0625\u0644\u0643\u062A\u0631\u0648\u0646\u064A"));
        inForgotEmail = new EditText(this);
        inForgotEmail.setSingleLine(true);
        inForgotEmail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        inForgotEmail.setHint("example@email.com");
        styleInput(inForgotEmail);
        root.addView(inForgotEmail);

        spacer(root, 14);
        btnPrimary = goldButton("\u0625\u0631\u0633\u0627\u0644 \u0643\u0644\u0645\u0629 \u062C\u062F\u064A\u062F\u0629");
        btnPrimary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                doForgotPassword();
            }
        });
        root.addView(btnPrimary, lpH());

        btnSecondary = blueButton("\u0631\u062C\u0648\u0639");
        btnSecondary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sfx.play("click");
                showLogin();
            }
        });
        root.addView(btnSecondary, lpH());

        spacer(root, 4);
        addStatus();
    }

    private void doLogin() {
        final String user = inUser.getText().toString().trim();
        final String pass = inPass.getText().toString();
        if (user.isEmpty() || pass.isEmpty()) {
            status("\u0627\u062F\u062E\u0644 \u0627\u0633\u0645 \u0627\u0644\u0645\u0633\u062A\u062E\u062F\u0645 \u0648\u0643\u0644\u0645\u0629 \u0627\u0644\u0645\u0631\u0648\u0631");
            return;
        }
        busy("\u062C\u0627\u0631\u064A \u0627\u0644\u0627\u062A\u0635\u0627\u0644...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String res = Api.login(MainActivity.this, user, pass);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            busy(null);
                            if (res.startsWith("OK")) {
                                Sfx.play("success");
                                openMenu();
                            } else {
                                Sfx.play("error");
                                status(fixMsg(res));
                            }
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            busy(null);
                            status(t(L10n.I_NET_ERR) + " " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void doCheckUser() {
        final String user = inUser.getText().toString().trim();
        if (user.isEmpty() || user.length() < 3 || user.length() > 20) {
            status("\u0627\u0633\u0645 \u0627\u0644\u0645\u0633\u062A\u062E\u062F\u0645 \u0645\u0646 3 \u0625\u0644\u0649 20 \u062D\u0631\u0641");
            return;
        }
        busy("\u062C\u0627\u0631\u064A \u0627\u0644\u0641\u062D\u0635...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String res = Api.checkUser(MainActivity.this, user);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            busy(null);
                            if (res.equals("OK")) {
                                Sfx.play("success");
                                regUser = user;
                                showRegStep2();
                            } else {
                                Sfx.play("error");
                                status(fixMsg(res));
                            }
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            busy(null);
                            status(t(L10n.I_NET_ERR) + " " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void doCheckEmail() {
        final String email = inEmail.getText().toString().trim();
        if (email.isEmpty() || !email.contains("@") || !email.contains(".")) {
            status("\u0627\u0644\u0628\u0631\u064A\u062F \u0627\u0644\u0625\u0644\u0643\u062A\u0631\u0648\u0646\u064A \u063A\u064A\u0631 \u0635\u062D\u064A\u062D");
            return;
        }
        busy("\u062C\u0627\u0631\u064A \u0627\u0644\u0641\u062D\u0635...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String res = Api.checkEmail(MainActivity.this, email);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            busy(null);
                            if (res.equals("OK")) {
                                Sfx.play("success");
                                regEmail = email;
                                showRegStep3();
                            } else {
                                Sfx.play("error");
                                status(fixMsg(res));
                            }
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            busy(null);
                            status(t(L10n.I_NET_ERR) + " " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void doRegPass() {
        final String pass = inPass.getText().toString();
        if (pass.length() < 4) {
            status("\u0643\u0644\u0645\u0629 \u0627\u0644\u0645\u0631\u0648\u0631 \u0645\u0646 4 \u0623\u062D\u0631\u0641 \u0641\u0648\u0642");
            return;
        }
        regPass = pass;
        Sfx.play("success");
        showRegStep4();
    }

    private void doRegisterFull() {
        int selectedId = genderGroup.getCheckedRadioButtonId();
        String gender = (selectedId == View.NO_ID) ? "male" : "male";
        if (selectedId != View.NO_ID) {
            RadioButton selected = genderGroup.findViewById(selectedId);
            if (selected != null) {
                gender = selected.getText().toString().equals("\u0623\u0646\u062B\u0649") ? "female" : "male";
            }
        }
        busy("\u062C\u0627\u0631\u064A \u0627\u0644\u062A\u0633\u062C\u064A\u0644...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String g = gender;
                    final String res = Api.register(MainActivity.this, regUser, regPass, regEmail, g);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            busy(null);
                            if (res.equals("OK")) {
                                Sfx.play("success");
                                showVerify();
                            } else {
                                Sfx.play("error");
                                status(fixMsg(res));
                            }
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            busy(null);
                            status(t(L10n.I_NET_ERR) + " " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void doVerifyEmail() {
        final String code = inCode.getText().toString().trim();
        if (code.isEmpty()) {
            status("\u0627\u062F\u062E\u0644 \u0631\u0645\u0632 \u0627\u0644\u062A\u062D\u0642\u0642");
            return;
        }
        busy("\u062C\u0627\u0631\u064A \u0627\u0644\u062A\u062D\u0642\u0642...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String res = Api.verifyEmail(MainActivity.this, regEmail, code);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            busy(null);
                            if (res.equals("OK")) {
                                Sfx.play("success");
                                getSharedPreferences("fastcar", MODE_PRIVATE).edit().putBoolean("show_tutorial", true).apply();
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("\u062A\u0645 \u0627\u0644\u062A\u062D\u0642\u0642 \u0645\u0646 \u0627\u0644\u062D\u0633\u0627\u0628 \u0628\u0646\u062C\u0627\u062D!")
                                        .setMessage("\u0647\u0646\u0627\u0643 \u0639\u0646\u0648\u0627\u0646 \u0627\u0644\u062F\u062E\u0648\u0644.\n\u0633\u062A\u0643\u0648\u0646 \u0644\u0648\u0632\u0646\u0629 \u0627\u0644\u062F\u062E\u0648\u0644 \u0627\u0644\u0622\u0646.")
                                        .setPositiveButton("\u062A\u0645\u0627\u0645", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface d, int w) {
                                                showLogin();
                                            }
                                        })
                                        .show();
                            } else {
                                Sfx.play("error");
                                status(fixMsg(res));
                            }
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            busy(null);
                            status(t(L10n.I_NET_ERR) + " " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void doForgotPassword() {
        final String user = inForgotUser.getText().toString().trim();
        final String email = inForgotEmail.getText().toString().trim();
        if (user.isEmpty() || email.isEmpty()) {
            status("\u0627\u062F\u062E\u0644 \u0627\u0633\u0645 \u0627\u0644\u0645\u0633\u062A\u062E\u062F\u0645 \u0648\u0627\u0644\u0628\u0631\u064A\u062F");
            return;
        }
        busy("\u062C\u0627\u0631\u064A \u0627\u0644\u0625\u0631\u0633\u0627\u0644...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String res = Api.forgotPassword(MainActivity.this, user, email);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            busy(null);
                            if (res.equals("OK")) {
                                Sfx.play("success");
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("\u062A\u0645 \u0627\u0644\u0625\u0631\u0633\u0627\u0644!")
                                        .setMessage("\u062A\u0645 \u0625\u0631\u0633\u0627\u0644 \u0643\u0644\u0645\u0629 \u0645\u0631\u0648\u0631 \u062C\u062F\u064A\u062F\u0629 \u0625\u0644\u0649 \u0628\u0631\u064A\u062F\u0643.\n\u0627\u0643\u062A\u0628 \u0627\u0644\u0643\u0644\u0645\u0629 \u0627\u0644\u062C\u062F\u064A\u062F\u0629 \u0645\u0646 \u0627\u0644\u0628\u0631\u064A\u062F \u0627\u0644\u0625\u0644\u0643\u062A\u0631\u0648\u0646\u064A \u0639\u0646\u062F \u0627\u0644\u062F\u062E\u0648\u0644.")
                                        .setPositiveButton("\u062A\u0645\u0627\u0645", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface d, int w) {
                                                showLogin();
                                            }
                                        })
                                        .show();
                            } else {
                                Sfx.play("error");
                                status(fixMsg(res));
                            }
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            busy(null);
                            status(t(L10n.I_NET_ERR) + " " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void busy(String msg) {
        if (msg != null) status.setText(msg);
        if (btnPrimary != null) btnPrimary.setEnabled(msg == null);
        if (btnSecondary != null) btnSecondary.setEnabled(msg == null);
        if (btnTertiary != null) btnTertiary.setEnabled(msg == null);
    }

    private void status(String s) {
        if (status != null) status.setText(s);
        status.announceForAccessibility(s);
    }

    private void addStatus() {
        status = new TextView(this);
        status.setText("");
        status.setTextSize(15);
        status.setTextColor(Color.rgb(169, 199, 255));
        status.setLineSpacing(0, 1.1f);
        root.addView(status);
    }

    private String fixMsg(String res) {
        if (res == null) return t(L10n.I_NET_ERR);
        return res.replace("ERR:", "");
    }

    private void openMenu() {
        startActivity(new Intent(this, MenuActivity.class));
        finish();
    }

    private TextView label(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(Color.rgb(216, 230, 255));
        t.setPadding(0, dp(14), 0, dp(4));
        return t;
    }

    private void styleInput(EditText e) {
        e.setBackgroundColor(Color.rgb(26, 36, 48));
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(Color.rgb(120, 140, 165));
        e.setPadding(dp(12), dp(12), dp(12), dp(12));
        e.setTextSize(16);
    }

    private Button goldButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(17);
        b.setAllCaps(false);
        b.setTextColor(Color.rgb(16, 19, 26));
        b.setBackgroundColor(Color.rgb(255, 213, 79));
        b.setPadding(dp(10), dp(14), dp(10), dp(14));
        b.setMinHeight(dp(54));
        return b;
    }

    private Button blueButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTextColor(Color.rgb(169, 199, 255));
        b.setBackgroundColor(Color.rgb(40, 52, 74));
        b.setPadding(dp(10), dp(14), dp(10), dp(14));
        b.setMinHeight(dp(54));
        return b;
    }

    private Button textButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTextColor(Color.rgb(180, 200, 230));
        b.setBackgroundColor(Color.TRANSPARENT);
        return b;
    }

    private void spacer(LinearLayout root, int h) {
        TextView s = new TextView(this);
        s.setHeight(dp(h));
        root.addView(s);
    }

    private LinearLayout.LayoutParams lpH() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
