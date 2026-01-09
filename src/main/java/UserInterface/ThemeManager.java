package UserInterface;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Centralized theme manager for light/dark mode across the UI.
 * Provides a comprehensive design system with consistent spacing, typography, and colors.
 */
public final class ThemeManager {
    private static boolean darkMode = false;
    private static final List<Runnable> listeners = new ArrayList<>();

    // ========== Design System Constants ==========
    
    // Spacing scale (in pixels)
    public static final int SPACING_XS = 4;
    public static final int SPACING_S = 8;
    public static final int SPACING_M = 16;
    public static final int SPACING_L = 24;
    public static final int SPACING_XL = 32;
    
    // Font sizes (in points)
    public static final int FONT_SMALL = 11;
    public static final int FONT_BODY = 13;
    public static final int FONT_SUBHEAD = 15;
    public static final int FONT_HEADING = 18;
    public static final int FONT_LARGE_HEADING = 24;
    
    // Border radius (in pixels)
    public static final int RADIUS_SM = 4;
    public static final int RADIUS_MD = 8;
    public static final int RADIUS_LG = 12;
    
    // Component sizes
    public static final int BUTTON_HEIGHT = 32;
    public static final int TAB_HEIGHT = 36;
    public static final int ICON_SIZE = 20;
    public static final int MIN_CLICK_TARGET = 24;

    // ========== Color Palette ==========
    
    private static final Color LIGHT_BG = Color.WHITE;
    private static final Color DARK_BG = new Color(24, 26, 32);
    private static final Color LIGHT_PANEL = new Color(245, 247, 250);
    private static final Color DARK_PANEL = new Color(32, 36, 44);
    private static final Color LIGHT_BORDER = new Color(220, 220, 220);
    private static final Color DARK_BORDER = new Color(70, 74, 84);
    
    // Improved text colors for better contrast (WCAG AA compliant)
    private static final Color LIGHT_TEXT = new Color(20, 20, 20);       // Darker for better contrast
    private static final Color DARK_TEXT = new Color(240, 242, 245);     // Lighter for better contrast
    private static final Color LIGHT_SUBTEXT = new Color(85, 85, 85);    // Better contrast than original
    private static final Color DARK_SUBTEXT = new Color(180, 184, 192);  // Better contrast than original
    
    // Accent colors with variations
    private static final Color ACCENT = new Color(100, 140, 220);
    private static final Color ACCENT_HOVER = new Color(80, 120, 200);
    private static final Color ACCENT_PRESSED = new Color(70, 110, 190);
    private static final Color ACCENT_DISABLED = new Color(150, 170, 210);

    private ThemeManager() {}
    
    // ========== Utility Methods ==========
    
    /**
     * Get spacing value by scale
     * @param scale One of: XS, S, M, L, XL
     * @return spacing in pixels
     */
    public static int getSpacing(String scale) {
        switch (scale.toUpperCase()) {
            case "XS": return SPACING_XS;
            case "S": return SPACING_S;
            case "M": return SPACING_M;
            case "L": return SPACING_L;
            case "XL": return SPACING_XL;
            default: return SPACING_M;
        }
    }
    
    /**
     * Get font by level
     * @param level One of: SMALL, BODY, SUBHEAD, HEADING, LARGE_HEADING
     * @param style Font style (Font.PLAIN, Font.BOLD, etc.)
     * @return Font object
     */
    public static Font getFont(String level, int style) {
        int size = getFontSize(level);
        return new Font("Arial", style, size);
    }
    
    /**
     * Get font size by level
     * @param level One of: SMALL, BODY, SUBHEAD, HEADING, LARGE_HEADING
     * @return font size in points
     */
    public static int getFontSize(String level) {
        switch (level.toUpperCase()) {
            case "SMALL": return FONT_SMALL;
            case "BODY": return FONT_BODY;
            case "SUBHEAD": return FONT_SUBHEAD;
            case "HEADING": return FONT_HEADING;
            case "LARGE_HEADING": return FONT_LARGE_HEADING;
            default: return FONT_BODY;
        }
    }
    
    /**
     * Get border radius by size
     * @param size One of: SM, MD, LG
     * @return radius in pixels
     */
    public static int getBorderRadius(String size) {
        switch (size.toUpperCase()) {
            case "SM": return RADIUS_SM;
            case "MD": return RADIUS_MD;
            case "LG": return RADIUS_LG;
            default: return RADIUS_MD;
        }
    }

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
    
    /**
     * Get accent color with state variations
     * @param state One of: "normal", "hover", "pressed", "disabled"
     * @return accent color for the specified state
     */
    public static Color accentColor(String state) {
        switch (state.toLowerCase()) {
            case "hover": return ACCENT_HOVER;
            case "pressed": return ACCENT_PRESSED;
            case "disabled": return ACCENT_DISABLED;
            default: return ACCENT;
        }
    }
    
    /**
     * Get accent color with alpha transparency
     * @param alpha 0-255 (0 = transparent, 255 = opaque)
     * @return accent color with specified alpha
     */
    public static Color accentWithAlpha(int alpha) {
        Color base = ACCENT;
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
    }
    
    /**
     * Blend two colors
     * @param c1 First color
     * @param c2 Second color
     * @param ratio Blend ratio (0.0 = all c1, 1.0 = all c2)
     * @return blended color
     */
    public static Color blend(Color c1, Color c2, float ratio) {
        ratio = Math.max(0, Math.min(1, ratio));
        int r = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
        int g = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
        int b = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
        return new Color(r, g, b);
    }

    public static void applyToUIManager() {
        Color bg = background();
        Color panel = panelBackground();
        Color text = textPrimary();
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
