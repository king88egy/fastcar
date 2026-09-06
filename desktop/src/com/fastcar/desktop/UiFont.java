package com.fastcar.desktop;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

import javax.swing.JComponent;
import javax.swing.UIManager;

public final class UiFont {

    private UiFont() {
    }

    private static final String[] PREFERRED = {
            "Tahoma", "Arial", "Segoe UI", "Noto Naskh Arabic", "Courier New", "SansSerif"
    };

    public static Font safe(Font base) {
        String family = base.getFamily();
        for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (family.equalsIgnoreCase(name)) return base;
        }
        for (String name : PREFERRED) {
            for (String installed : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
                if (name.equalsIgnoreCase(installed)) {
                    return new Font(installed, base.getStyle(), base.getSize());
                }
            }
        }
        return base;
    }

    public static void install() {
        try {
            Font ui = safe(new Font("Tahoma", Font.PLAIN, 13));
            UIManager.put("Button.font", ui);
            UIManager.put("Label.font", ui);
            UIManager.put("TextField.font", ui);
            UIManager.put("PasswordField.font", ui);
            UIManager.put("TextArea.font", ui);
            UIManager.put("ComboBox.font", ui);
            UIManager.put("RadioButton.font", ui);
            UIManager.put("TabbedPane.font", ui);
            UIManager.put("CheckBox.font", ui);
            UIManager.put("TitledBorder.font", ui);
        } catch (Throwable t) {
        }
    }

    public static void apply(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JComponent jc) {
                Font f = jc.getFont();
                if (f != null) jc.setFont(safe(f));
            }
            if (c instanceof Container) apply((Container) c);
        }
    }
}