package com.fastcar.desktop;

import com.fastcar.common.CarType;
import com.fastcar.common.GarageData;
import com.fastcar.common.L10n;
import javax.sound.sampled.*;
import java.util.Base64;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.ButtonGroup;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

public class App extends JFrame {

    private String lang = Api.lang();
    private GarageData gd = new GarageData();
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final JPanel login = new JPanel(new BorderLayout());
    private final JTextField userField = new JTextField(16);
    private final JPasswordField passField = new JPasswordField(16);
    private final JButton loginBtn = new JButton();
    private final JButton regBtn = new JButton();
    private final JLabel msgLabel = new JLabel(" ");
    private JLabel header = new JLabel(" ");
    private JTabbedPane tabs;
    private RacePanel race;
    private JPanel garageTab;
    private JPanel charactersTab;
    private JTextArea scoresArea;
    private JPanel tasksTab;
    private JTextArea tasksArea;
    private JTextArea charArea;
    private JScrollPane scoresScroll;
    private JTextField serverField = new JTextField(Api.server(), 24);
    private JComboBox<String> langBox;
    private JTextArea roomsArea;
    private JScrollPane roomsScroll;
    private JButton joinRoomBtn;
    private javax.swing.Timer roomTimer;
    private boolean tutorialPending = false;
    private JPanel regContainer = new JPanel(new CardLayout());
    private JTextField regUserField = new JTextField(16);
    private JTextField regEmailField = new JTextField(16);
    private JPasswordField regPassField = new JPasswordField(16);
    private JRadioButton maleRadio, femaleRadio;
    private final ButtonGroup genderGroup = new ButtonGroup();
    private JTextField regCodeField = new JTextField(16);
    private JLabel regStepLabel = new JLabel(" ");
    private JLabel regMsgLabel = new JLabel(" ");
    private String regUser = "", regEmail = "", regPass = "", regGender = "";
    private JTextField forgotUserField = new JTextField(16);
    private JTextField forgotEmailField = new JTextField(16);
    private JLabel forgotMsgLabel = new JLabel(" ");

public App() {
        super("Fast Car");
        Sfx.init(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        java.awt.event.MouseAdapter clickSfx = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getComponent() instanceof JButton) Sfx.play("click");
            }
        };
        root.addMouseListener(clickSfx);
        buildLogin();
        root.add(login, "login");
        root.add(buildRegister(), "register");
        root.add(buildForgotPassword(), "forgot");
        root.add(buildMain(), "main");
        setContentPane(root);
        pack();
        setSize(440, 700);
        setLocationRelativeTo(null);
        cards.show(root, Api.username.isEmpty() ? "login" : "main");
        applyLang();
    }

    private void buildLogin() {
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JLabel title = new JLabel("Fast Car", SwingConstants.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        title.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(title);
        inner.add(Box.createVerticalStrut(18));
        userField.setAlignmentX(CENTER_ALIGNMENT);
        passField.setAlignmentX(CENTER_ALIGNMENT);
        loginBtn.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(userField);
        inner.add(Box.createVerticalStrut(6));
        inner.add(passField);
        inner.add(Box.createVerticalStrut(6));
        inner.add(loginBtn);
        inner.add(Box.createVerticalStrut(8));
        JPanel regRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        regRow.add(regBtn);
        inner.add(regRow);
        JButton forgotBtn = new JButton("نسيت كلمة المرور");
        forgotBtn.setAlignmentX(CENTER_ALIGNMENT);
        forgotBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        forgotBtn.setBorderPainted(false);
        forgotBtn.setContentAreaFilled(false);
        forgotBtn.setForeground(new java.awt.Color(40, 152, 255));
        inner.add(forgotBtn);
        inner.add(Box.createVerticalStrut(8));
        msgLabel.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(msgLabel);
        JPanel btns = new JPanel(new FlowLayout());
        JButton docsBtn = new JButton();
        btns.add(docsBtn);
        inner.add(btns);
        JPanel south = new JPanel();
        JLabel copy = new JLabel("© 2026 Mohammed Egyptian. All rights reserved.");
        copy.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        south.add(copy);
        login.add(inner, BorderLayout.CENTER);
        login.add(south, BorderLayout.SOUTH);
        loginBtn.addActionListener(e -> doLogin());
        regBtn.addActionListener(e -> {
            Sfx.play("click");
            clearRegFields();
            cards.show(root, "register");
        });
        forgotBtn.addActionListener(e -> {
            Sfx.play("click");
            forgotUserField.setText("");
            forgotEmailField.setText("");
            forgotMsgLabel.setText(" ");
            cards.show(root, "forgot");
        });
        docsBtn.addActionListener(e -> showDoc("privacy"));
    }

    private void doLogin() {
        String u = userField.getText().trim();
        String p = new String(passField.getPassword());
        if (u.length() < 3) {
            msgLabel.setText(L10n.get(lang, L10n.I_USERNAME) + " " + L10n.get(lang, L10n.I_NET_ERR));
            return;
        }
        if (p.length() < 4) {
            msgLabel.setText(L10n.get(lang, L10n.I_PASS) + " " + L10n.get(lang, L10n.I_NET_ERR));
            return;
        }
        msgLabel.setText(L10n.get(lang, L10n.I_LOADING) + "...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return Api.login(u, p);
            }

            @Override
            protected void done() {
                try {
String r = get();
                    if (r.startsWith("OK")) {
                        Sfx.play("success");
                        userField.setText("");
                        passField.setText("");
refreshGarage();
                        cards.show(root, "main");
                        applyLang();
                        SmartAnnouncer.say("account_ready", Api.username, true);
                        return;
                    }
msgLabel.setText(r);
                    Sfx.play("error");
                } catch (Exception ex) {
                    msgLabel.setText(String.valueOf(ex.getMessage()));
                    Sfx.play("error");
                }
            }
        }.execute();
    }

    private void clearRegFields() {
        regUserField.setText("");
        regEmailField.setText("");
        regPassField.setText("");
        regCodeField.setText("");
        regUser = "";
        regEmail = "";
        regPass = "";
        regGender = "";
        regMsgLabel.setText(" ");
        regStepLabel.setText("الخطوة 1 من 5");
        regContainer.revalidate();
        regContainer.repaint();
    }

    private JPanel buildRegister() {
        JPanel outer = new JPanel(new BorderLayout());
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JButton backBtn = new JButton("رجوع");
        backBtn.addActionListener(e -> {
            Sfx.play("click");
            cards.show(root, "login");
        });
        topBar.add(backBtn, BorderLayout.WEST);
        regStepLabel = new JLabel("الخطوة 1 من 5", SwingConstants.CENTER);
        regStepLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        topBar.add(regStepLabel, BorderLayout.CENTER);
        outer.add(topBar, BorderLayout.NORTH);

        regContainer = new JPanel(new CardLayout());
        regContainer.add(makeRegStep1(), "1");
        regContainer.add(makeRegStep2(), "2");
        regContainer.add(makeRegStep3(), "3");
        regContainer.add(makeRegStep4(), "4");
        regContainer.add(makeRegStep5(), "5");
        outer.add(regContainer, BorderLayout.CENTER);

        regMsgLabel = new JLabel(" ");
        regMsgLabel.setAlignmentX(CENTER_ALIGNMENT);
        JPanel bottomPanel = new JPanel(new FlowLayout());
        bottomPanel.add(regMsgLabel);
        outer.add(bottomPanel, BorderLayout.SOUTH);
        return outer;
    }

    private JPanel makeRegStep1() {
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(BorderFactory.createEmptyBorder(40, 24, 24, 24));
        JLabel stepTitle = new JLabel("أدخل اسم المستخدم", SwingConstants.CENTER);
        stepTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        stepTitle.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(stepTitle);
        inner.add(Box.createVerticalStrut(16));
        regUserField = new JTextField(16);
        regUserField.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(regUserField);
        inner.add(Box.createVerticalStrut(16));
        JButton nextBtn = new JButton("التالي");
        nextBtn.setAlignmentX(CENTER_ALIGNMENT);
        nextBtn.addActionListener(e -> doRegisterNext(1));
        inner.add(nextBtn);
        return inner;
    }

    private JPanel makeRegStep2() {
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(BorderFactory.createEmptyBorder(40, 24, 24, 24));
        JLabel stepTitle = new JLabel("أدخل البريد الإلكتروني", SwingConstants.CENTER);
        stepTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        stepTitle.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(stepTitle);
        inner.add(Box.createVerticalStrut(16));
        regEmailField = new JTextField(16);
        regEmailField.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(regEmailField);
        inner.add(Box.createVerticalStrut(16));
        JButton nextBtn = new JButton("التالي");
        nextBtn.setAlignmentX(CENTER_ALIGNMENT);
        nextBtn.addActionListener(e -> doRegisterNext(2));
        inner.add(nextBtn);
        return inner;
    }

    private JPanel makeRegStep3() {
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(BorderFactory.createEmptyBorder(40, 24, 24, 24));
        JLabel stepTitle = new JLabel("أدخل كلمة المرور", SwingConstants.CENTER);
        stepTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        stepTitle.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(stepTitle);
        inner.add(Box.createVerticalStrut(16));
        regPassField = new JPasswordField(16);
        regPassField.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(regPassField);
        inner.add(Box.createVerticalStrut(16));
        JButton nextBtn = new JButton("التالي");
        nextBtn.setAlignmentX(CENTER_ALIGNMENT);
        nextBtn.addActionListener(e -> doRegisterNext(3));
        inner.add(nextBtn);
        return inner;
    }

    private JPanel makeRegStep4() {
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(BorderFactory.createEmptyBorder(40, 24, 24, 24));
        JLabel stepTitle = new JLabel("اختر الجنس", SwingConstants.CENTER);
        stepTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        stepTitle.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(stepTitle);
        inner.add(Box.createVerticalStrut(16));
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        maleRadio = new JRadioButton("ذكر");
        femaleRadio = new JRadioButton("أنثى");
        genderGroup.add(maleRadio);
        genderGroup.add(femaleRadio);
        maleRadio.setSelected(true);
        radioPanel.add(maleRadio);
        radioPanel.add(femaleRadio);
        radioPanel.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(radioPanel);
        inner.add(Box.createVerticalStrut(16));
        JButton regSubmitBtn = new JButton("تسجيل");
        regSubmitBtn.setAlignmentX(CENTER_ALIGNMENT);
        regSubmitBtn.addActionListener(e -> doRegisterNext(4));
        inner.add(regSubmitBtn);
        return inner;
    }

    private JPanel makeRegStep5() {
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(BorderFactory.createEmptyBorder(40, 24, 24, 24));
        JLabel stepTitle = new JLabel("التحقق من الحساب", SwingConstants.CENTER);
        stepTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        stepTitle.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(stepTitle);
        inner.add(Box.createVerticalStrut(8));
        JLabel desc = new JLabel("أدخل رمز التحقق المرسل إلى بريدك", SwingConstants.CENTER);
        desc.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(desc);
        inner.add(Box.createVerticalStrut(16));
        regCodeField = new JTextField(16);
        regCodeField.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(regCodeField);
        inner.add(Box.createVerticalStrut(16));
        JButton verifyBtn = new JButton("تحقق");
        verifyBtn.setAlignmentX(CENTER_ALIGNMENT);
        verifyBtn.addActionListener(e -> doRegisterNext(5));
        inner.add(verifyBtn);
        return inner;
    }

    private void doRegisterNext(int step) {
        Sfx.play("click");
        regMsgLabel.setText(" ");
        switch (step) {
            case 1:
                regUser = regUserField.getText().trim();
                if (regUser.length() < 3) {
                    regMsgLabel.setText(L10n.get(lang, L10n.I_USERNAME) + " " + L10n.get(lang, L10n.I_NET_ERR));
                    Sfx.play("error");
                    return;
                }
                regMsgLabel.setText(L10n.get(lang, L10n.I_LOADING) + "...");
                new SwingWorker<String, Void>() {
                    @Override protected String doInBackground() throws Exception { return Api.checkUser(regUser); }
                    @Override protected void done() {
                        try {
                            String r = get();
                            if (r.startsWith("OK")) {
                                Sfx.play("success");
                                regStepLabel.setText("الخطوة 2 من 5");
                                ((CardLayout) regContainer.getLayout()).show(regContainer, "2");
                            } else {
                                regMsgLabel.setText(r);
                                Sfx.play("error");
                            }
                        } catch (Exception ex) {
                            regMsgLabel.setText(String.valueOf(ex.getMessage()));
                            Sfx.play("error");
                        }
                    }
                }.execute();
                break;
            case 2:
                regEmail = regEmailField.getText().trim();
                if (regEmail.isEmpty() || !regEmail.contains("@")) {
                    regMsgLabel.setText("البريد الإلكتروني غير صحيح");
                    Sfx.play("error");
                    return;
                }
                regMsgLabel.setText(L10n.get(lang, L10n.I_LOADING) + "...");
                new SwingWorker<String, Void>() {
                    @Override protected String doInBackground() throws Exception { return Api.checkEmail(regEmail); }
                    @Override protected void done() {
                        try {
                            String r = get();
                            if (r.startsWith("OK")) {
                                Sfx.play("success");
                                regStepLabel.setText("الخطوة 3 من 5");
                                ((CardLayout) regContainer.getLayout()).show(regContainer, "3");
                            } else {
                                regMsgLabel.setText(r);
                                Sfx.play("error");
                            }
                        } catch (Exception ex) {
                            regMsgLabel.setText(String.valueOf(ex.getMessage()));
                            Sfx.play("error");
                        }
                    }
                }.execute();
                break;
            case 3:
                regPass = new String(regPassField.getPassword());
                if (regPass.length() < 4) {
                    regMsgLabel.setText(L10n.get(lang, L10n.I_PASS) + " " + L10n.get(lang, L10n.I_NET_ERR));
                    Sfx.play("error");
                    return;
                }
                Sfx.play("success");
                regStepLabel.setText("الخطوة 4 من 5");
                ((CardLayout) regContainer.getLayout()).show(regContainer, "4");
                break;
            case 4:
                regGender = maleRadio.isSelected() ? "male" : "female";
                regMsgLabel.setText(L10n.get(lang, L10n.I_LOADING) + "...");
                new SwingWorker<String, Void>() {
                    @Override protected String doInBackground() throws Exception { return Api.register(regUser, regPass, regEmail, regGender); }
                    @Override protected void done() {
                        try {
                            String r = get();
                            if (r.startsWith("OK")) {
                                Sfx.play("success");
                                regStepLabel.setText("الخطوة 5 من 5");
                                ((CardLayout) regContainer.getLayout()).show(regContainer, "5");
                            } else {
                                regMsgLabel.setText(r);
                                Sfx.play("error");
                            }
                        } catch (Exception ex) {
                            regMsgLabel.setText(String.valueOf(ex.getMessage()));
                            Sfx.play("error");
                        }
                    }
                }.execute();
                break;
            case 5:
                String code = regCodeField.getText().trim();
                if (code.isEmpty()) {
                    regMsgLabel.setText("أدخل رمز التحقق");
                    Sfx.play("error");
                    return;
                }
                regMsgLabel.setText(L10n.get(lang, L10n.I_LOADING) + "...");
                new SwingWorker<String, Void>() {
                    @Override protected String doInBackground() throws Exception { return Api.verifyEmail(regEmail, code); }
                    @Override protected void done() {
                        try {
                            String r = get();
                            if (r.startsWith("OK")) {
                                Sfx.play("success");
                                JOptionPane.showMessageDialog(root, "تم التحقق بنجاح! يمكنك الآن تسجيل الدخول", "التحقق", JOptionPane.INFORMATION_MESSAGE);
                                clearRegFields();
                                userField.setText(regUser);
                                passField.setText("");
                                cards.show(root, "login");
                            } else {
                                regMsgLabel.setText(r);
                                Sfx.play("error");
                            }
                        } catch (Exception ex) {
                            regMsgLabel.setText(String.valueOf(ex.getMessage()));
                            Sfx.play("error");
                        }
                    }
                }.execute();
                break;
        }
    }

    private JPanel buildForgotPassword() {
        JPanel outer = new JPanel(new BorderLayout());
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JButton backBtn = new JButton("رجوع");
        backBtn.addActionListener(e -> {
            Sfx.play("click");
            cards.show(root, "login");
        });
        topBar.add(backBtn, BorderLayout.WEST);
        JLabel hdr = new JLabel("نسيت كلمة المرور", SwingConstants.CENTER);
        hdr.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        topBar.add(hdr, BorderLayout.CENTER);
        outer.add(topBar, BorderLayout.NORTH);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(BorderFactory.createEmptyBorder(40, 24, 24, 24));
        JLabel desc = new JLabel("<html><center>أدخل اسم المستخدم والبريد الإلكتروني<br>سيتم إرسال كلمة مرور جديدة إلى بريدك</center></html>", SwingConstants.CENTER);
        desc.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(desc);
        inner.add(Box.createVerticalStrut(16));
        forgotUserField = new JTextField(16);
        forgotUserField.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(forgotUserField);
        inner.add(Box.createVerticalStrut(8));
        forgotEmailField = new JTextField(16);
        forgotEmailField.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(forgotEmailField);
        inner.add(Box.createVerticalStrut(16));
        JButton submitBtn = new JButton("إرسال");
        submitBtn.setAlignmentX(CENTER_ALIGNMENT);
        submitBtn.addActionListener(e -> {
            Sfx.play("click");
            String u = forgotUserField.getText().trim();
            String em = forgotEmailField.getText().trim();
            if (u.length() < 3 || em.isEmpty() || !em.contains("@")) {
                forgotMsgLabel.setText("تحقق من البيانات المدخلة");
                Sfx.play("error");
                return;
            }
            forgotMsgLabel.setText(L10n.get(lang, L10n.I_LOADING) + "...");
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() throws Exception { return Api.forgotPassword(u, em); }
                @Override protected void done() {
                    try {
                        String r = get();
                        if (r.startsWith("OK")) {
                            Sfx.play("success");
                            JOptionPane.showMessageDialog(root, "تم إرسال كلمة المرور الجديدة إلى بريدك", "نسيت كلمة المرور", JOptionPane.INFORMATION_MESSAGE);
                            cards.show(root, "login");
                        } else {
                            forgotMsgLabel.setText(r);
                            Sfx.play("error");
                        }
                    } catch (Exception ex) {
                        forgotMsgLabel.setText(String.valueOf(ex.getMessage()));
                        Sfx.play("error");
                    }
                }
            }.execute();
        });
        inner.add(submitBtn);
        outer.add(inner, BorderLayout.CENTER);

        forgotMsgLabel = new JLabel(" ");
        forgotMsgLabel.setAlignmentX(CENTER_ALIGNMENT);
        JPanel bottom = new JPanel(new FlowLayout());
        bottom.add(forgotMsgLabel);
        outer.add(bottom, BorderLayout.SOUTH);
        return outer;
    }

    private JPanel buildMain() {
        JPanel p = new JPanel(new BorderLayout());
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        p.add(header, BorderLayout.NORTH);
        tabs = new JTabbedPane();
        race = new RacePanel(lang, spec(), new RacePanel.Listener() {
            @Override
public void onEnd(int score) {
                long coins = Math.max(1, Math.round(score * race.spec.scoreMult() * 0.05));
                Sfx.play("gameover");
                SmartAnnouncer.say("result", Api.username, false);
                new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() {
                        try {
                            Api.submitScore(score);
                            return Api.earn(coins);
                        } catch (Exception e) {
                            return "ERR " + e.getMessage();
                        }
                    }

                    @Override
protected void done() {
                        try {
                            if (!get().startsWith("ERR")) {
                                Sfx.play("coin");
                                refreshGarage();
                            } else {
                                Sfx.play("error");
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }.execute();
            }

            @Override
            public void onExit() {
                race.reset();
                applyLang();
            }
        });
        JPanel playTab = new JPanel(new BorderLayout());
        playTab.add(race, BorderLayout.CENTER);
        garageTab = new JPanel(new BorderLayout());
        scoresArea = new JTextArea();
        scoresArea.setEditable(false);
        scoresArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        scoresScroll = new JScrollPane(scoresArea);
        tabs.addTab("Play", playTab);
        tabs.addTab("Garage", garageTab);
        tabs.addTab("Rooms", buildRooms());
        tabs.addTab("Store", buildStore());
        charactersTab = buildCharacters();
        tabs.addTab("Characters", charactersTab);
tabs.addTab("Leaderboard", scoresScroll);
        tasksTab = buildTasks();
        tabs.addTab("Tasks", tasksTab);
        tabs.addTab("Settings", buildSettings());
tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == scoresScroll) {
                loadScores();
            }
            if (tabs.getSelectedComponent() == tasksTab) {
                loadTasks();
            }
            if (tabs.getSelectedComponent() == charactersTab) {
                loadCharacters();
            }
        });
p.add(tabs, BorderLayout.CENTER);
        A11y.attach(root);
        return p;
    }

    private JPanel buildRooms() {
        JPanel rp = new JPanel(new BorderLayout());
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 8, 10));

        JLabel hdr = new JLabel("Online Rooms / غرف اللعب");
        hdr.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        top.add(hdr);
        top.add(Box.createVerticalStrut(8));

        JButton createBtn = new JButton("Create Room");
        createBtn.setToolTipText("أنشئ غرفة عامة أو خاصة. الخاصة ليها كود وكلمة سر تبعتهم لأصحابك.");
        createBtn.addMouseListener(new SpeakMouse("إنشاء غرفة. الخاصة ليها كود وكلمة سر"));
        createBtn.addActionListener(e -> {
            Tts.say("إنشاء غرفة");
            String name = JOptionPane.showInputDialog(root, "Room name:", "My room");
            String pw = JOptionPane.showInputDialog(root, "Password (empty = public):", "");
            final String pwd = pw == null ? "" : pw.trim();
            roomCall(() -> Api.roomCreate(name == null ? "My room" : name, pwd));
        });
        top.add(createBtn);
        top.add(Box.createVerticalStrut(6));

        JButton quickBtn = new JButton("Join Random Match");
        quickBtn.setToolTipText("انضم تلقائيًا لأي غرفة عامة مفتوحة.");
        quickBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        quickBtn.addMouseListener(new SpeakMouse("انضم عشوائيًا لأي غرفة عامة"));
        quickBtn.addActionListener(e -> {
            Tts.say("جاري البحث عن غرفة عشوائية");
            String res = askCall(() -> Api.roomRandom());
            if (res != null) {
                if (res.startsWith("CREATED") || res.startsWith("JOINED")) {
                    JOptionPane.showMessageDialog(root, "Joined match! Code: " + res.substring(res.indexOf(' ') + 1), "Quick Match", JOptionPane.INFORMATION_MESSAGE);
                    Tts.say("تم الانضمام لمباراة جماعية");
                } else {
                    JOptionPane.showMessageDialog(root, res, "Quick Match", JOptionPane.WARNING_MESSAGE);
                    Tts.say(res);
                }
                refreshRooms();
            }
        });
        top.add(quickBtn);
        top.add(Box.createVerticalStrut(6));

        JButton startBtn = new JButton("Start Room Challenge");
        startBtn.setToolTipText("ابدأ التحدي مع أعضاء الغرفة.");
        startBtn.addMouseListener(new SpeakMouse("ابدأ التحدي مع أعضاء الغرفة"));
        startBtn.addActionListener(e -> { Tts.say("بدء التحدي"); roomCall(() -> Api.roomStart()); });
        top.add(startBtn);
        top.add(Box.createVerticalStrut(6));

        JButton leaveBtn = new JButton("Leave Room");
        leaveBtn.addMouseListener(new SpeakMouse("مغادرة الغرفة"));
        leaveBtn.addActionListener(e -> { Tts.say("مغادرة الغرفة"); roomCall(() -> Api.roomLeave()); });
        top.add(leaveBtn);

        JPanel mid = new JPanel(new FlowLayout());
        JTextField codeField = new JTextField(6);
        codeField.setToolTipText("Room code");
        joinRoomBtn = new JButton("Join by code");
        joinRoomBtn.setToolTipText("ادخل غرفة خاصة بالكود وكلمة السر");
        joinRoomBtn.addMouseListener(new SpeakMouse("الدخول لغرفة خاصة بالكود وكلمة السر"));
        joinRoomBtn.addActionListener(e -> {
            String code = codeField.getText().trim();
            if (code.isEmpty()) return;
            final String c = code;
            String pw = JOptionPane.showInputDialog(root, "Password (empty if public):", "");
            roomCall(() -> Api.roomJoin(c, pw == null ? "" : pw.trim()));
        });
        mid.add(new JLabel("Code:"));
        mid.add(codeField);
        mid.add(joinRoomBtn);
        top.add(mid);

        top.add(Box.createVerticalStrut(10));
        JLabel mhdr = new JLabel("Manage Room / إدارة الغرفة");
        mhdr.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        top.add(mhdr);
        top.add(Box.createVerticalStrut(4));

        JPanel man = new JPanel(new FlowLayout());
        JTextField memField = new JTextField(8);
        memField.setToolTipText("اسم اللاعب");
JButton inviteBtn = new JButton("دعوة");
        JButton promoteBtn = new JButton("مشرف");
        JButton muteBtn = new JButton("كتم");
        JButton unmuteBtn = new JButton("فك كتم");
        JButton kickBtn = new JButton("طرد");
        JButton banBtn = new JButton("حظر");
        JButton unbanBtn = new JButton("فك حظر");
        JButton transferBtn = new JButton("نقل ملكية");
        JButton reportBtn = new JButton("بلاغ");
        JButton configBtn = new JButton("إعدادات");

        inviteBtn.addActionListener(e -> runManage(() -> Api.roomInvite(memField.getText().trim()), memField));
        promoteBtn.addActionListener(e -> runManage(() -> Api.roomPromote(memField.getText().trim()), memField));
        muteBtn.addActionListener(e -> runManage(() -> Api.roomMute(memField.getText().trim()), memField));
        unmuteBtn.addActionListener(e -> runManage(() -> Api.roomUnmute(memField.getText().trim()), memField));
        kickBtn.addActionListener(e -> runManage(() -> Api.roomKick(memField.getText().trim()), memField));
        banBtn.addActionListener(e -> runManage(() -> Api.roomBan(memField.getText().trim()), memField));
        unbanBtn.addActionListener(e -> runManage(() -> Api.roomUnban(memField.getText().trim()), memField));
        transferBtn.addActionListener(e -> runManage(() -> Api.roomTransfer(memField.getText().trim()), memField));
        reportBtn.addActionListener(e -> runManage(() -> Api.roomReport(memField.getText().trim()), memField));
        configBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(root, "اسم الغرفة:", "My room");
            String pw = JOptionPane.showInputDialog(root, "كلمة السر (فارغ = عامة):", "");
            String max = JOptionPane.showInputDialog(root, "الحد الأقصى للأعضاء (2-16):", "6");
            int m = 6;
            try {
                m = Integer.parseInt(max == null ? "6" : max.trim());
            } catch (Exception ignored) {
            }
            final String n = name == null ? "" : name.trim();
            final String p = pw == null ? "" : pw.trim();
            final int mm = m;
            runManage(() -> Api.roomConfig(n, p, mm), null);
        });

        man.add(new JLabel("لاعب:"));
        man.add(memField);
        man.add(inviteBtn);
        man.add(promoteBtn);
        man.add(muteBtn);
        man.add(unmuteBtn);
        man.add(kickBtn);
        man.add(banBtn);
        man.add(unbanBtn);
        man.add(transferBtn);
        man.add(reportBtn);
        man.add(configBtn);
        JButton talkBtn = new JButton("بث صوتي");
        JButton lastBtn = new JButton("آخر صوت");
        talkBtn.addActionListener(e -> voiceBroadcast());
        lastBtn.addActionListener(e -> voiceListen());
        man.add(talkBtn);
        man.add(lastBtn);
        top.add(man);

        rp.add(top, BorderLayout.NORTH);

        roomsArea = new JTextArea();
        roomsArea.setEditable(false);
        roomsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        roomsScroll = new JScrollPane(roomsArea);
        rp.add(roomsScroll, BorderLayout.CENTER);

        roomTimer = new javax.swing.Timer(4000, e -> refreshRooms());
        roomTimer.start();
        refreshRooms();
        return rp;
    }

    private void refreshRooms() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    return Api.roomList();
                } catch (Exception e) {
                    return "ERR " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String r = get();
                    if (r.startsWith("ERR") || r.equals("(empty)")) {
                        roomsArea.setText(r.startsWith("ERR") ? r : "No open rooms yet. Create one!");
                    } else {
                        String[] lines = r.split("\n");
                        StringBuilder sb = new StringBuilder("Code    Room                 By           Players\n");
                        sb.append("------  -------------------  -----------  -------\n");
                        for (String line : lines) {
                            String[] p = line.split("\\|");
                            if (p.length >= 5) {
                                sb.append(String.format("%-6s  %-20s  %-12s  %s/%s %s%n",
                                        p[0], p[1], p[2], p[3], "6", p[4].equals("1") ? "(private)" : ""));
                            }
                        }
                        roomsArea.setText(sb.toString());
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private String askCall(Op op) {
        final String[] out = {null};
        try {
            java.util.concurrent.FutureTask<String> ft = new java.util.concurrent.FutureTask<>(() -> {
                try {
                    return op.run();
                } catch (Exception ex) {
                    return "ERR " + ex.getMessage();
                }
            });
            new Thread(ft).start();
            out[0] = ft.get(6, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ex) {
            out[0] = "ERR " + ex.getMessage();
        }
        return out[0];
    }

    private void roomCall(Op op) {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return op.run();
            }

            @Override
            protected void done() {
                try {
                    String r = get();
                    if (r.startsWith("ERR")) {
                        JOptionPane.showMessageDialog(root, r, "Rooms", JOptionPane.WARNING_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(root, "OK " + r, "Rooms", JOptionPane.INFORMATION_MESSAGE);
                    }
                    refreshRooms();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(root, String.valueOf(ex.getMessage()), "Rooms", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }

        private long voiceChannel = 0;

    private void voiceBroadcast() {
        new Thread(() -> {
            try {
                JOptionPane.showMessageDialog(root, "تحدث الآن ... (3 ثوانٍ)", "بث صوتي", JOptionPane.INFORMATION_MESSAGE);
                AudioFormat fmt = new AudioFormat(22050.0f, 16, 1, true, false);
                TargetDataLine line = AudioSystem.getTargetDataLine(fmt);
                line.open(fmt);
                line.start();
                byte[] buf = new byte[(int) (22050 * 2 * 3)];
                int off = 0;
                long end = System.currentTimeMillis() + 3000;
                while (off < buf.length && System.currentTimeMillis() < end) {
                    int r = line.read(buf, off, buf.length - off);
                    if (r <= 0) break;
                    off += r;
                }
                line.stop();
                line.close();
                byte[] real = java.util.Arrays.copyOf(buf, off);
                String b64 = Base64.getEncoder().encodeToString(real);
                String res = Api.voiceSend(currentChannel(), b64);
                JOptionPane.showMessageDialog(root, res, "بث صوتي", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(root, "خطأ في البث: " + ex.getMessage(), "صوت", JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }

    private void voiceListen() {
        new Thread(() -> {
            try {
                String res = Api.voiceLatest(currentChannel(), voiceChannel);
                if (res == null || res.startsWith("ERR") || res.startsWith("(empty)")) {
                    JOptionPane.showMessageDialog(root, "لا يوجد صوت جديد", "الاستقبال", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                String[] lines2 = res.split("\n");
                String last = lines2[lines2.length - 1];
                String[] parts = last.split("\\|", 3);
                long t = Long.parseLong(parts[0]);
                voiceChannel = t;
                byte[] wav = Base64.getDecoder().decode(parts[2]);
                playPcm(wav);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(root, "خطأ: " + ex.getMessage(), "صوت", JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }

    private void playPcm(byte[] pcm) throws Exception {
        AudioFormat fmt = new AudioFormat(22050.0f, 16, 1, true, false);
        byte[] wav = toWav(pcm, fmt);
        Clip clip = AudioSystem.getClip();
        clip.open(fmt, wav, 0, wav.length);
        clip.start();
        Thread.sleep(clip.getMicrosecondLength() / 1000 + 50);
        clip.close();
    }

    private byte[] toWav(byte[] pcm, AudioFormat fmt) throws Exception {
        int sampleRate = (int) fmt.getSampleRate();
        int bits = fmt.getSampleSizeInBits();
        int channels = fmt.getChannels();
        int dataLen = pcm.length;
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream(44 + dataLen);
        writeStr(bos, "RIFF");
        writeInt(bos, 36 + dataLen);
        writeStr(bos, "WAVE");
        writeStr(bos, "fmt ");
        writeInt(bos, 16);
        writeShort(bos, (short) 1);
        writeShort(bos, (short) channels);
        writeInt(bos, sampleRate);
        writeInt(bos, sampleRate * channels * (bits / 8));
        writeShort(bos, (short) (channels * (bits / 8)));
        writeShort(bos, (short) bits);
        writeStr(bos, "data");
        writeInt(bos, dataLen);
        bos.write(pcm, 0, dataLen);
        return bos.toByteArray();
    }

    private void writeStr(java.io.ByteArrayOutputStream bos, String s) throws Exception {
        bos.write(s.getBytes("US-ASCII"));
    }

    private void writeInt(java.io.ByteArrayOutputStream bos, int v) throws Exception {
        bos.write(v & 0xff);
        bos.write((v >>> 8) & 0xff);
        bos.write((v >>> 16) & 0xff);
        bos.write((v >>> 24) & 0xff);
    }

    private void writeShort(java.io.ByteArrayOutputStream bos, short v) throws Exception {
        bos.write(v & 0xff);
        bos.write((v >>> 8) & 0xff);
    }

    private String currentChannel() {
        try {
            String code = Api.roomState();
            if (code != null && code.startsWith("OK ")) {
                String[] p = code.split(" ");
                if (p.length >= 2 && !p[1].isEmpty()) return "room:" + p[1];
            }
        } catch (Exception ignored) {
        }
        return "global";
    }
private void runManage(Op op, javax.swing.JTextField field) {
        final String who = field == null ? "" : field.getText().trim();
        if (who.isEmpty()) {
            JOptionPane.showMessageDialog(root, "ادخل اسم اللاعب أولاً", "إدارة الغرفة", JOptionPane.WARNING_MESSAGE);
            return;
        }
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return op.run();
            }

            @Override
            protected void done() {
                try {
                    String r = get();
                    if (r.startsWith("ERR")) {
                        JOptionPane.showMessageDialog(root, r, "إدارة الغرفة", JOptionPane.WARNING_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(root, r, "إدارة الغرفة", JOptionPane.INFORMATION_MESSAGE);
                        Tts.say(r);
                    }
                    refreshRooms();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(root, String.valueOf(ex.getMessage()), "إدارة الغرفة", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }

    private JPanel buildStore() {
        JPanel sp = new JPanel();
        sp.setLayout(new BoxLayout(sp, BoxLayout.Y_AXIS));
        sp.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JLabel hdr = new JLabel("Buy Coins / شراء عملات");
        hdr.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        sp.add(hdr);
        sp.add(Box.createVerticalStrut(8));
        JLabel info = new JLabel("Transfer to Vodafone Cash: 01040814547");
        info.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        sp.add(info);
        sp.add(Box.createVerticalStrut(12));
        long[][] packs = {{50, 5}, {120, 10}, {300, 20}, {600, 35}, {1500, 50}};
        for (int i = 0; i < packs.length; i++) {
            final int idx = i;
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton buy = new JButton(packs[i][0] + " coins = " + packs[i][1] + " EGP");
            buy.addActionListener(e -> {
                String txid = JOptionPane.showInputDialog(root, "Enter Vodafone Cash transaction ID:", "Pay " + packs[idx][1] + " EGP");
                if (txid != null && !txid.trim().isEmpty()) {
                    new SwingWorker<String, Void>() {
                        @Override
                        protected String doInBackground() {
                            try {
                                return Api.payRequest(idx, txid.trim());
                            } catch (Exception ex) {
                                return "ERR " + ex.getMessage();
                            }
                        }

                        @Override
protected void done() {
                            try {
                                String r = get();
                                if (r.startsWith("ERR")) {
                                    Sfx.play("error");
                                } else {
                                    Sfx.play("success");
                                }
                                JOptionPane.showMessageDialog(root, r.startsWith("ERR") ? r : "OK " + r);
                            } catch (Exception ignored) {
                            }
                        }
                    }.execute();
                }
            });
            row.add(buy);
            sp.add(row);
            sp.add(Box.createVerticalStrut(4));
        }
        sp.add(Box.createVerticalStrut(12));
JButton prem = new JButton("Premium Account");
        prem.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        prem.addActionListener(e -> showPremiumDialog());
        if (gd.premium) prem.setText("Premium Active");
        sp.add(prem);
        sp.add(Box.createVerticalStrut(8));
        JButton itemsBtn = new JButton("متجر الهدايا - عناصر ممتعة");
        itemsBtn.addActionListener(e -> showItemsShop());
        sp.add(itemsBtn);
        sp.add(Box.createVerticalGlue());
        return sp;
    }

    private void showPremiumDialog() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    return Api.premiumTiers();
                } catch (Exception e) {
                    return "ERR " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String tiers = get();
                    if (tiers == null || tiers.startsWith("ERR")) {
                        JOptionPane.showMessageDialog(root, "تعذر جلب الباقات", "Premium", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String[] rows = tiers.split("\n");
                    String[] labels = new String[rows.length + 1];
                    for (int i = 0; i < rows.length; i++) {
                        String[] p = rows[i].split("\\|");
                        if (p.length < 4) continue;
                        String days = p[3].equals("0") ? "دائم" : p[3] + " يوم";
                        String price = p[1].equals("0") ? "مجانية" : p[1] + " ج.م";
                        labels[i] = p[0] + " (" + p[2] + ") - " + price + " / " + days;
                    }
                    labels[rows.length] = "جرّب Premium مجانًا 3 أيام";
                    String pick = (String) JOptionPane.showInputDialog(root, "اختر باقة:", "Premium",
                            JOptionPane.QUESTION_MESSAGE, null, labels, labels[0]);
                    if (pick == null) return;
                    if (pick.contains("مجانًا")) {
                        premiumCall(() -> Api.premiumTrial());
                        return;
                    }
                    for (int i = 0; i < rows.length; i++) {
                        if (labels[i].equals(pick)) {
                            String[] p = rows[i].split("\\|");
                            final String price = p[1];
                            String txid = JOptionPane.showInputDialog(root,
                                    "حوّل " + price + " ج.م على 01040814547\nأدخل رقم العملية من فودافون كاش:",
                                    "Premium " + p[0] + " - " + price + " ج.م");
                            if (txid != null && !txid.trim().isEmpty()) {
                                premiumCall(() -> Api.premiumRequest(txid.trim()));
                            }
                            return;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void premiumCall(Op op) {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return askCall(op);
            }

            @Override
            protected void done() {
                try {
                    String r = get();
                    if (r.startsWith("ERR")) {
                        Sfx.play("error");
                    } else {
                        Sfx.play("success");
                    }
                    JOptionPane.showMessageDialog(root, r, "Premium", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void showItemsShop() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    return Api.storeItems();
                } catch (Exception e) {
                    return "ERR " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String items = get();
                    if (items == null || items.startsWith("ERR")) {
                        JOptionPane.showMessageDialog(root, "تعذر جلب المتجر", "متجر الهدايا", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String[] rows = items.split("\n");
                    String[] labels = new String[rows.length];
                    for (int i = 0; i < rows.length; i++) {
                        String[] p = rows[i].split("\\|");
                        if (p.length < 4) continue;
                        labels[i] = p[1] + " (" + p[2] + " عملة) - " + p[0];
                    }
                    String pick = (String) JOptionPane.showInputDialog(root, "اختر عنصرًا:", "متجر الهدايا",
                            JOptionPane.QUESTION_MESSAGE, null, labels, labels[0]);
                    if (pick == null) return;
                    for (int i = 0; i < rows.length; i++) {
                        if (labels[i].equals(pick)) {
                            final String item = rows[i].split("\\|")[0];
                            int ok = JOptionPane.showConfirmDialog(root, "شراء " + pick + "؟", "متجر الهدايا", JOptionPane.YES_NO_OPTION);
                            if (ok == JOptionPane.YES_OPTION) {
                                premiumCall(() -> Api.buyItem(item));
                                refreshGarage();
                            }
                            return;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private JPanel buildTasks() {
        JPanel tp = new JPanel(new BorderLayout());
        tasksArea = new JTextArea();
        tasksArea.setEditable(false);
        tasksArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane sp = new JScrollPane(tasksArea);
        JPanel top = new JPanel(new FlowLayout());
        JButton reload = new JButton("تحديث");
        reload.addMouseListener(new SpeakMouse("تحديث المهام والإنجازات"));
        reload.addActionListener(e -> loadTasks());
        JButton claim = new JButton("استلام");
        claim.addMouseListener(new SpeakMouse("استلام مكافأة مهمة مكتملة"));
        claim.addActionListener(e -> {
            String id = JOptionPane.showInputDialog(root, "معرّف المهمة (مثل d_distance أو w_win):", "استلام");
            if (id != null && !id.trim().isEmpty()) {
                final String i = id.trim();
                String r = askCall(() -> i.startsWith("d_") || i.startsWith("w_") ? Api.taskClaim(i) : Api.achievementClaim(i));
                JOptionPane.showMessageDialog(root, r);
                loadTasks();
            }
        });
        JButton wheel = new JButton("عجلة الحظ");
        wheel.addMouseListener(new SpeakMouse("عجلة الحظ اليومية"));
        wheel.addActionListener(e -> {
            String r = askCall(() -> Api.wheelSpin());
            if (r.startsWith("ERR")) {
                Sfx.play("error");
            } else {
                Sfx.play("year");
                refreshGarage();
            }
            JOptionPane.showMessageDialog(root, r.startsWith("ERR") ? r : "ربحت " + r.substring(3) + " عملة!");
            loadTasks();
        });
        JButton ach = new JButton("الإنجازات");
        ach.addMouseListener(new SpeakMouse("عرض الإنجازات"));
        ach.addActionListener(e -> loadAch());
        top.add(reload);
        top.add(claim);
        top.add(wheel);
        top.add(ach);
        tp.add(top, BorderLayout.NORTH);
        tp.add(sp, BorderLayout.CENTER);
        return tp;
    }

    private void loadTasks() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    return Api.tasks();
                } catch (Exception e) {
                    return "ERR " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String r = get();
                    if (r.startsWith("ERR")) {
                        tasksArea.setText(r);
                        return;
                    }
                    StringBuilder sb = new StringBuilder("المهام اليومية والأسبوعية\n");
                    for (String line : r.split("\n")) {
                        String[] p = line.split("\\|");
                        if (p.length < 7) continue;
                        String done = "1".equals(p[6]) ? " (مكتملة)" : (Long.parseLong(p[3]) >= Long.parseLong(p[4]) ? " (جاهزة)" : "");
                        sb.append("[").append(p[5]).append(" عملة] ").append(p[1]).append(": ").append(p[3]).append("/").append(p[4]).append(done).append("\n");
                    }
                    tasksArea.setText(sb.toString());
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void loadAch() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    return Api.achievements();
                } catch (Exception e) {
                    return "ERR " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String r = get();
                    if (r.startsWith("ERR")) {
                        tasksArea.setText(r);
                        return;
                    }
                    StringBuilder sb = new StringBuilder("الإنجازات\n");
                    for (String line : r.split("\n")) {
                        String[] p = line.split("\\|");
                        if (p.length < 7) continue;
                        String done = "1".equals(p[6]) ? " (مكتملة)" : (Long.parseLong(p[3]) >= Long.parseLong(p[4]) ? " (جاهزة)" : "");
                        sb.append("[").append(p[5]).append(" عملة] ").append(p[1]).append(": ").append(p[3]).append("/").append(p[4]).append(done).append("\n");
                    }
                    tasksArea.setText(sb.toString());
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private JPanel buildSettings() {
        JPanel sp = new JPanel();
        sp.setLayout(new BoxLayout(sp, BoxLayout.Y_AXIS));
        sp.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        sp.add(new JLabel(L10n.get(lang, L10n.I_LANG)));
        langBox = new JComboBox<>(L10n.NATIVE);
        for (int i = 0; i < L10n.CODES.length; i++) {
            if (L10n.CODES[i].equals(lang)) {
                langBox.setSelectedIndex(i);
                break;
            }
        }
        langBox.addActionListener(e -> {
            int i = langBox.getSelectedIndex();
            if (i >= 0) {
                Api.setLang(L10n.CODES[i]);
                applyLang();
            }
        });
sp.add(langBox);
        sp.add(Box.createVerticalStrut(12));
        sp.add(new JLabel(L10n.get(lang, L10n.I_SERVER)));
        JPanel sr = new JPanel(new BorderLayout(6, 0));
        JLabel serverLabel = new JLabel(Api.server());
        serverLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        sr.add(serverLabel, BorderLayout.CENTER);
        sp.add(sr);
        sp.add(Box.createVerticalStrut(12));
        JPanel d = new JPanel(new GridLayout(1, 2, 6, 0));
        JButton priv = new JButton(L10n.get(lang, "privacy"));
        JButton term = new JButton(L10n.get(lang, "terms"));
        priv.addActionListener(e -> showDoc("privacy"));
        term.addActionListener(e -> showDoc("terms"));
        d.add(priv);
        d.add(term);
        sp.add(d);
        sp.add(Box.createVerticalStrut(12));
        JButton logout = new JButton(L10n.get(lang, L10n.I_LOGOUT));
        logout.addActionListener(e -> {
            Api.logout();
            cards.show(root, "login");
            applyLang();
        });
        sp.add(logout);
        sp.add(Box.createVerticalStrut(12));
        JButton devBtn = new JButton("Developer Panel");
        devBtn.setToolTipText("لوحة المطور: إعطاء عملات، حظر حسابات");
        devBtn.addMouseListener(new SpeakMouse("لوحة المطور"));
        devBtn.addActionListener(e -> openDevPanel());
        sp.add(devBtn);
        sp.add(Box.createVerticalStrut(8));
        JLabel ver = new JLabel("FastCar v1.0 · © 2026 Mohammed Egyptian");
        ver.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        sp.add(ver);
        return sp;
    }

    private void openDevPanel() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    return Api.devStatus();
                } catch (Exception e) {
                    return "ERR " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String st = get();
                    boolean isDev = st != null && st.trim().equals("DEV");
                    if (!isDev) {
                        String pw = JOptionPane.showInputDialog(root, "أدخل كلمة سر المطور لتفعيل الصلاحية:", "Developer");
                        if (pw == null || pw.trim().isEmpty()) return;
                        final String pwd = pw.trim();
                        String res = askCall(() -> Api.devActivate(pwd));
                        JOptionPane.showMessageDialog(root, res, "Developer", JOptionPane.INFORMATION_MESSAGE);
                        Tts.say(res);
                        return;
                    }
                    String[] choices = {
                            "منح / خصم عملات",
                            "ضبط رصيد عملات",
                            "إعادة تعيين حساب",
                            "منح باقة Premium",
                            "إعلان للجميع",
                            "إحصائيات السيرفر",
                            "قائمة المستخدمين",
                            "عرض سريع / خصم",
                            "حذف حساب (Ban)",
                            "باقات Premium المتاحة"
                    };
                    String pick = (String) JOptionPane.showInputDialog(root, "اختر العملية:", "لوحة المطور",
                            JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);
                    if (pick == null) return;
                    if (pick.contains("إحصائيات")) {
                        String msg = askCall(() -> Api.devStats());
                        JOptionPane.showMessageDialog(root, msg.replace("|", "\n"), "إحصائيات السيرفر", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    if (pick.contains("قائمة المستخدمين")) {
                        String msg = askCall(() -> Api.devUsers());
                        JOptionPane.showMessageDialog(root, msg, "قائمة المستخدمين", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    if (pick.contains("باقات Premium")) {
                        String msg = askCall(() -> Api.premiumTiers());
                        JOptionPane.showMessageDialog(root, msg, "باقات Premium", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    String target = JOptionPane.showInputDialog(root, "اسم المستخدم:", "لوحة المطور");
                    if (target == null || target.trim().isEmpty()) return;
                    final String t = target.trim();
                    if (pick.contains("حذف")) {
                        String msg = askCall(() -> Api.devBan(t));
                        JOptionPane.showMessageDialog(root, msg, "Developer", JOptionPane.INFORMATION_MESSAGE);
                        Tts.say(msg);
                        return;
                    }
                    if (pick.contains("إعادة تعيين")) {
                        String msg = askCall(() -> Api.devReset(t));
                        JOptionPane.showMessageDialog(root, msg, "Developer", JOptionPane.INFORMATION_MESSAGE);
                        Tts.say(msg);
                        return;
                    }
                    if (pick.contains("منح Premium")) {
                        String tier = JOptionPane.showInputDialog(root, "الباقة (trial/bronze/silver/gold/vip):", "silver");
                        if (tier == null || tier.trim().isEmpty()) return;
                        final String tierVal = tier.trim();
                        String msg = askCall(() -> Api.devPremiumGrant(t, tierVal));
                        JOptionPane.showMessageDialog(root, msg, "Developer", JOptionPane.INFORMATION_MESSAGE);
                        Tts.say(msg);
                        return;
                    }
                    if (pick.contains("إعلان")) {
                        String msgTxt = JOptionPane.showInputDialog(root, "نص الإعلان للجميع:", "إعلان");
                        if (msgTxt == null) return;
                        String msg = askCall(() -> Api.devAnnounce(msgTxt.trim()));
                        JOptionPane.showMessageDialog(root, msg, "Developer", JOptionPane.INFORMATION_MESSAGE);
                        Tts.say(msg);
                        return;
                    }
                    if (pick.contains("عرض سريع")) {
                        String item = JOptionPane.showInputDialog(root, "اسم المنتج (nitro_pack ...):", "nitro_pack");
                        if (item == null || item.trim().isEmpty()) return;
                        String disc = JOptionPane.showInputDialog(root, "نسبة الخصم (مثلاً 0.3):", "0.3");
                        if (disc == null) return;
                        String mins = JOptionPane.showInputDialog(root, "المدة بالدقائق:", "60");
                        if (mins == null) return;
                        final String itemVal = item.trim();
                        final double discVal;
                        final long minsVal;
                        try {
                            discVal = Double.parseDouble(disc.trim());
                            minsVal = Long.parseLong(mins.trim());
                        } catch (Exception ex) { return; }
                        String msg = askCall(() -> Api.devFlashSale(itemVal, discVal, minsVal));
                        JOptionPane.showMessageDialog(root, msg, "Developer", JOptionPane.INFORMATION_MESSAGE);
                        Tts.say(msg);
                        return;
                    }
                    String value = JOptionPane.showInputDialog(root,
                            pick.contains("ضبط") ? "الرصيد الجديد:" : "الكمية (سالب للخصم):", "0");
                    if (value == null) return;
                    final long amount;
                    try {
                        amount = Long.parseLong(value.trim());
                    } catch (Exception ex) { return; }
                    String msg;
                    if (pick.contains("ضبط")) {
                        msg = askCall(() -> Api.devSetCoins(t, amount));
                    } else {
                        msg = askCall(() -> Api.devGrant(t, (int) amount));
                    }
                    JOptionPane.showMessageDialog(root, msg, "Developer", JOptionPane.INFORMATION_MESSAGE);
                    Tts.say(msg);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(root, String.valueOf(ex), "Developer", JOptionPane.WARNING_MESSAGE);
                }
            }
}.execute();
    }

    private JPanel buildCharacters() {
        JPanel cp = new JPanel(new BorderLayout());
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 8, 10));
        top.add(new JLabel("Characters / الشخصيات"));
        top.add(Box.createVerticalStrut(8));
        JButton refresh = new JButton("Refresh / تحديث");
        refresh.addActionListener(e -> loadCharacters());
        top.add(refresh);
        cp.add(top, BorderLayout.NORTH);
        charArea = new JTextArea();
        charArea.setEditable(false);
        charArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        cp.add(new JScrollPane(charArea), BorderLayout.CENTER);
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
        JButton buyBtn = new JButton("Buy character / شراء بالعملات");
        buyBtn.addActionListener(e -> characterBuy());
        bottom.add(buyBtn);
        bottom.add(Box.createVerticalStrut(6));
        JButton cashBtn = new JButton("Buy with cash / شراء بفلوس (Vodafone)");
        cashBtn.addActionListener(e -> characterCash());
        bottom.add(cashBtn);
        bottom.add(Box.createVerticalStrut(6));
        JButton selectBtn = new JButton("Select / اختيار");
        selectBtn.addActionListener(e -> characterSelect());
        bottom.add(selectBtn);
        cp.add(bottom, BorderLayout.SOUTH);
        return cp;
    }

    private void characterBuy() {
        String id = JOptionPane.showInputDialog(root, "اختر ورقة الشخصية (rookie/speedy/turbo/lucky/champ/veteran/legend):",
                "شراء شخصية بالعملات");
        if (id == null || id.trim().isEmpty()) return;
        final String vid = id.trim();
        String msg = askCall(() -> Api.characterBuy(vid));
        JOptionPane.showMessageDialog(root, msg, "شخصيات", JOptionPane.INFORMATION_MESSAGE);
        Tts.say(msg);
        loadCharacters();
    }

    private void characterCash() {
        JTextField idF = new JTextField("legend");
        JTextField txF = new JTextField();
        Object[] fields = {"معرف الشخصية:", idF, "رقم العملية (txid):", txF};
        int r = JOptionPane.showConfirmDialog(root, fields, "شراء بالفلوس (فودافون كاش)", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;
        String id = idF.getText().trim();
        String tx = txF.getText().trim();
        if (id.isEmpty() || tx.isEmpty()) return;
        final String vid = id, vtx = tx;
        String msg = askCall(() -> Api.characterBuyCash(vid, vtx));
        JOptionPane.showMessageDialog(root, msg, "شخصيات", JOptionPane.INFORMATION_MESSAGE);
        Tts.say(msg);
        loadCharacters();
    }

    private void characterSelect() {
        String id = JOptionPane.showInputDialog(root, "اختر شخصية مفعّلة:", "اختيار الشخصية");
        if (id == null || id.trim().isEmpty()) return;
        final String vid = id.trim();
        String msg = askCall(() -> Api.characterSelect(vid));
        JOptionPane.showMessageDialog(root, msg, "شخصيات", JOptionPane.INFORMATION_MESSAGE);
        Tts.say(msg);
        loadCharacters();
    }

    private void loadCharacters() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    return Api.characters();
                } catch (Exception e) {
                    return "ERR " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String r = get();
                    charArea.setText(r.startsWith("ERR") ? r : r.replace("|", "   "));
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void showTutorial() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    return Api.tutorial();
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    String txt = get();
                    JOptionPane.showMessageDialog(root, txt == null ? "تعذر جلب الدليل" : txt,
                            "دليل اللاعب الجديد", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void showDoc(String doc) {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    return Api.doc(doc, lang);
                } catch (Exception e) {
                    return "ERR " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    JTextArea ta = new JTextArea(get(), 18, 40);
                    ta.setEditable(false);
                    JOptionPane.showMessageDialog(root, new JScrollPane(ta), "FastCar(" + doc + ")", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void loadScores() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    return Api.topScores();
                } catch (Exception e) {
                    return "ERR " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String r = get();
                    scoresArea.setText(r.startsWith("ERR") ? r : r.replace("|", "   "));
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private CarType.Spec spec() {
        CarType ct = CarType.byKey(gd.selected);
        if (ct == null) ct = CarType.SEDAN;
        return new CarType.Spec(ct, gd.engineLvl(ct.key), gd.tireLvl(ct.key),
                gd.selectedColor(ct.key), gd.parts(ct.key));
    }

    private void refreshGarage() {
        new SwingWorker<GarageData, Void>() {
            @Override
            protected GarageData doInBackground() throws Exception {
                return Api.garage();
            }

            @Override
            protected void done() {
                try {
                    gd = get();
                } catch (Exception ex) {
                    gd = new GarageData();
                }
                rebuildGarage();
                race.spec = spec();
                setHeader();
            }
        }.execute();
    }

    private void setHeader() {
        String prem = gd.premium ? " " : "";
        header.setText(Api.username + prem + "   |   " + L10n.get(lang, L10n.I_COINS) + " " + gd.coins);
    }

    private void rebuildGarage() {
        garageTab.removeAll();
        JPanel list = new JPanel(new GridLayout(0, 1, 6, 8));
        for (CarType ct : CarType.ALL) {
            boolean owned = gd.owned.contains(ct.key);
            int eng = gd.engineLvl(ct.key);
            int tir = gd.tireLvl(ct.key);
            int parts = gd.parts(ct.key);
            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
            row.setBorder(BorderFactory.createTitledBorder(ct.key + "  ·  " + ct.price));
            row.add(new JLabel(L10n.get(lang, L10n.I_ENGINE) + " " + eng + "/" + CarType.MAX_LEVEL + "   "
                    + L10n.get(lang, L10n.I_TIRES) + " " + tir + "/" + CarType.MAX_LEVEL));
            JPanel b = new JPanel(new FlowLayout());
            if (!owned) {
                JButton buy = new JButton(L10n.get(lang, L10n.I_BUY) + " " + ct.price);
                buy.addActionListener(e -> serverCall(() -> Api.buy(ct.key)));
                b.add(buy);
            } else {
                JButton sel = new JButton(gd.selected.equals(ct.key)
                        ? L10n.get(lang, L10n.I_SELECTED)
                        : L10n.get(lang, L10n.I_SELECT));
                sel.addActionListener(e -> serverCall(() -> Api.selectCar(ct.key, Integer.MIN_VALUE)));
                b.add(sel);
                JButton engB = new JButton(L10n.get(lang, L10n.I_ENGINE) + " [ " + CarType.upgradeCosts(true)[Math.min(eng, CarType.MAX_LEVEL - 1)] + " ]");
                engB.addActionListener(e -> serverCall(() -> Api.upgrade(ct.key, "engine")));
                b.add(engB);
                JButton tirB = new JButton(L10n.get(lang, L10n.I_TIRES) + " [ " + CarType.upgradeCosts(false)[Math.min(tir, CarType.MAX_LEVEL - 1)] + " ]");
                tirB.addActionListener(e -> serverCall(() -> Api.upgrade(ct.key, "tires")));
                b.add(tirB);
                JButton clo = new JButton(L10n.get(lang, L10n.I_COLOR));
                clo.addActionListener(e -> {
                    int n = gd.selectedColor(ct.key) + 1;
                    if (ct.palette == null) n = 0;
                    else if (n >= ct.palette.length) n = 0;
                    final int next = n;
                    serverCall(() -> Api.selectCar(ct.key, next));
                });
                b.add(clo);
            }
            row.add(b);
            if (owned) {
                JPanel q = new JPanel(new FlowLayout());
                for (String part : CarType.PARTS) {
                    int bi = CarType.partIndex(part);
                    boolean has = (parts & (1 << bi)) != 0;
                    JButton pb = new JButton(L10n.get(lang, part) + (has ? "  +1" : ""));
                    pb.addActionListener(e -> serverCall(() -> has ? Api.upgrade(ct.key, part) : Api.buyPart(ct.key, part)));
                    q.add(pb);
                }
                row.add(q);
            }
            list.add(row);
        }
        garageTab.add(new JScrollPane(list), BorderLayout.CENTER);
        garageTab.revalidate();
        garageTab.repaint();
    }

    private interface Op {
        String run() throws Exception;
    }

    private void serverCall(Op op) {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return op.run();
            }

            @Override
            protected void done() {
                try {
String r = get();
                    if (!r.startsWith("ERR")) {
                        Sfx.play("coin");
                        refreshGarage();
                    } else {
                        Sfx.play("error");
                        JOptionPane.showMessageDialog(root, r, "FastCar", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(root, String.valueOf(e.getMessage()), "FastCar", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }

    private void applyLang() {
        lang = Api.lang();
        root.setComponentOrientation(L10n.isRtl(lang)
                ? ComponentOrientation.RIGHT_TO_LEFT
                : ComponentOrientation.LEFT_TO_RIGHT);
        loginBtn.setText(lang.equals("ar") ? ("دخــول") : L10n.get(lang, L10n.I_LOGIN));
        regBtn.setText(L10n.get(lang, L10n.I_REGISTER));
        tabs.setTitleAt(0, L10n.get(lang, L10n.I_PLAY));
        tabs.setTitleAt(1, L10n.get(lang, L10n.I_GARAGE));
        tabs.setTitleAt(2, "Rooms / غرف");
        tabs.setTitleAt(3, "Store / متجر");
        tabs.setTitleAt(4, L10n.get(lang, L10n.I_LEADERBOARD));
        tabs.setTitleAt(5, "Tasks / مهام");
        tabs.setTitleAt(6, "Settings / إعدادات");
        tabs.setTitleAt(5, "Settings / إعدادات");
        race.setLang(lang);
        if (gd != null) {
            setHeader();
        }
        rebuildGarage();
        serverField.setText(Api.server());
    }

    public static void main(String[] args) {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        javax.swing.SwingUtilities.invokeLater(() -> new App().setVisible(true));
    }

    private static class SpeakMouse extends java.awt.event.MouseAdapter {
        private final String text;

        SpeakMouse(String text) {
            this.text = text;
        }

        @Override
        public void mouseEntered(java.awt.event.MouseEvent e) {
            Tts.say(text);
        }
    }
}