package com.fastcar.desktop;

import javax.swing.JButton;
import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public final class A11y {
    private static String last = "";
    private static long lastAt = 0;

    private A11y() {
    }

    public static void attach(Container root) {
        if (root == null) return;
        remember(root);
        root.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                String s = describe(root.getComponentAt(e.getPoint()));
                if (s != null && !s.isEmpty()) speak(s, false);
            }
        });
    }

    private static void remember(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton jb && jb.getText() != null && !jb.getText().isEmpty()) {
                jb.addFocusListener(new FocusListener() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        String t = describe(jb);
                        if (t != null) speak(t, true);
                    }

                    @Override
                    public void focusLost(FocusEvent e) {
                    }
                });
            }
            if (c instanceof Container) remember((Container) c);
        }
    }

    private static String describe(Component c) {
        String t = null;
        if (c instanceof JButton) t = ((JButton) c).getText();
        if (t == null) t = (c instanceof JComponent) ? ((JComponent) c).getAccessibleContext().getAccessibleName() : null;
        return t;
    }

    private static void speak(String s, boolean force) {
        if (s == null) return;
        long now = System.currentTimeMillis();
        if (!force && (now - lastAt < 250 && s.equals(last))) return;
        last = s;
        lastAt = now;
        Tts.say(s);
    }

    public static void speakNow(String s) {
        speak(s, true);
    }
}