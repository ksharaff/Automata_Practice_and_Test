package UserInterface;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Centralized theme manager for light/dark mode across the UI.
 */
public final class ThemeManager {
    private static boolean darkMode = false;
    private static final List<Runnable> listeners = new ArrayList<>();

    private static final Color LIGHT_BG = Color.WHITE;
    private static final Color DARK_BG = new Color(24, 26, 32);
    private static final Color LIGHT_PANEL = new Color(245, 247, 250);
    private static final Color DARK_PANEL = new Color(32, 36, 44);
    private static final Color LIGHT_BORDER = new Color(220, 220, 220);
    private static final Color DARK_BORDER = new Color(70, 74, 84);
    private static final Color LIGHT_TEXT = new Color(32, 32, 32);
    private static final Color DARK_TEXT = new Color(232, 236, 244);
    private static final Color LIGHT_SUBTEXT = new Color(96, 96, 96);
    private static final Color DARK_SUBTEXT = new Color(192, 196, 204);
    private static final Color ACCENT = new Color(100, 140, 220);
    private static final Color ACCENT_HOVER = new Color(80, 120, 200);

    private ThemeManager() {}

    public static boolean isDarkMode() {
        return darkMode;
    }

    public static void setDarkMode(boolean enabled) {
        if (darkMode == enabled) return;
        darkMode = enabled;
        applyToUIManager();
        notifyListeners();
    }

    public static void addListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public static void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (Runnable r : new ArrayList<>(listeners)) {
            SwingUtilities.invokeLater(r);
        }
    }

    public static Color background() {
        return darkMode ? DARK_BG : LIGHT_BG;
    }

    public static Color panelBackground() {
        return darkMode ? DARK_PANEL : LIGHT_PANEL;
    }

    public static Color borderColor() {
        return darkMode ? DARK_BORDER : LIGHT_BORDER;
    }

    public static Color textPrimary() {
        return darkMode ? DARK_TEXT : LIGHT_TEXT;
    }

    public static Color textSecondary() {
        return darkMode ? DARK_SUBTEXT : LIGHT_SUBTEXT;
    }

    public static Color accent(boolean hover) {
        return hover ? ACCENT_HOVER : ACCENT;
    }

    public static void applyToUIManager() {
        Color bg = background();
        Color panel = panelBackground();
        Color text = textPrimary();
        Color subtext = textSecondary();
        UIManager.put("control", panel);
        UIManager.put("info", panel);
        UIManager.put("Panel.background", panel);
        UIManager.put("OptionPane.background", panel);
        UIManager.put("Menu.background", panel);
        UIManager.put("MenuItem.background", panel);
        UIManager.put("Menu.foreground", text);
        UIManager.put("MenuItem.foreground", text);
        UIManager.put("Label.foreground", text);
        UIManager.put("TextArea.background", bg);
        UIManager.put("TextArea.foreground", text);
        UIManager.put("TextField.background", bg);
        UIManager.put("TextField.foreground", text);
        UIManager.put("controlText", text);
        UIManager.put("text", text);
        UIManager.put("nimbusBase", darkMode ? new Color(40, 45, 55) : new Color(80, 120, 200));
        UIManager.put("nimbusBlueGrey", darkMode ? new Color(60, 70, 85) : new Color(200, 210, 220));
        UIManager.put("nimbusFocus", new Color(120, 170, 230));
        UIManager.put("nimbusSelectionBackground", darkMode ? new Color(90, 120, 190) : new Color(130, 170, 230));
        UIManager.put("textHighlight", darkMode ? new Color(90, 120, 190) : new Color(130, 170, 230));
        UIManager.put("textHighlightText", Color.WHITE);
    }
}
