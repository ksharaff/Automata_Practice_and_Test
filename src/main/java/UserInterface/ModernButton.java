package UserInterface;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JButton;
import javax.swing.Timer;

/**
 * Modern styled button with rounded corners, subtle shadows, and smooth hover effects.
 * Follows the minimalist design system defined in ThemeManager.
 */
public class ModernButton extends JButton {
    
    private boolean isHovered = false;
    private boolean isPressed = false;
    private float hoverProgress = 0f; // 0.0 to 1.0 for smooth transitions
    private Timer hoverTimer;
    private Color customBackground = null;
    private Color customForeground = null;
    private int borderRadius = ThemeManager.RADIUS_MD;
    private boolean isPrimary = false;
    
    /**
     * Create a modern button with default styling
     */
    public ModernButton(String text) {
        this(text, false);
    }
    
    /**
     * Create a modern button
     * @param text Button text
     * @param primary If true, uses accent color as background
     */
    public ModernButton(String text, boolean primary) {
        super(text);
        this.isPrimary = primary;
        setupButton();
    }
    
    /**
     * Create a modern button with custom background color
     */
    public ModernButton(String text, Color backgroundColor) {
        super(text);
        this.customBackground = backgroundColor;
        setupButton();
    }
    
    private void setupButton() {
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        
        // Set default size
        setPreferredSize(new Dimension(100, ThemeManager.BUTTON_HEIGHT));
        setMinimumSize(new Dimension(60, ThemeManager.BUTTON_HEIGHT));
        
        // Set font with better rendering
        setFont(ThemeManager.getFont("BODY", Font.PLAIN));
        
        // Setup hover animation timer
        hoverTimer = new Timer(16, e -> { // ~60 FPS
            if (isHovered && hoverProgress < 1.0f) {
                hoverProgress = Math.min(1.0f, hoverProgress + 0.1f);
                repaint();
            } else if (!isHovered && hoverProgress > 0f) {
                hoverProgress = Math.max(0f, hoverProgress - 0.1f);
                repaint();
            }
            
            if ((isHovered && hoverProgress >= 1.0f) || (!isHovered && hoverProgress <= 0f)) {
                hoverTimer.stop();
            }
        });
        
        // Add mouse listeners for hover effects
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    setCursor(new Cursor(Cursor.HAND_CURSOR));
                    if (!hoverTimer.isRunning()) {
                        hoverTimer.start();
                    }
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                isPressed = false;
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                if (!hoverTimer.isRunning()) {
                    hoverTimer.start();
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = true;
                    repaint();
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
        
        // Register for theme updates
        ThemeManager.addListener(this::repaint);
    }
    
    /**
     * Set whether this is a primary button (uses accent color)
     */
    public void setPrimary(boolean primary) {
        this.isPrimary = primary;
        repaint();
    }
    
    /**
     * Set custom border radius
     */
    public void setBorderRadius(int radius) {
        this.borderRadius = radius;
        repaint();
    }
    
    /**
     * Set custom background color (overrides primary/default colors)
     */
    public void setCustomBackground(Color color) {
        this.customBackground = color;
        repaint();
    }
    
    /**
     * Set custom foreground/text color
     */
    public void setCustomForeground(Color color) {
        this.customForeground = color;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        
        // Enable anti-aliasing for smooth rendering
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        int width = getWidth();
        int height = getHeight();
        
        // Determine colors based on state
        Color bgColor;
        Color fgColor;
        
        if (!isEnabled()) {
            bgColor = customBackground != null ? 
                      new Color(customBackground.getRed(), customBackground.getGreen(), 
                               customBackground.getBlue(), 100) :
                      ThemeManager.blend(ThemeManager.panelBackground(), ThemeManager.borderColor(), 0.5f);
            fgColor = ThemeManager.textSecondary();
        } else if (customBackground != null) {
            // Custom color button
            if (isPressed) {
                bgColor = darken(customBackground, 0.2f);
            } else {
                bgColor = ThemeManager.blend(customBackground, 
                    darken(customBackground, 0.1f), hoverProgress);
            }
            fgColor = customForeground != null ? customForeground : ThemeManager.textPrimary();
        } else if (isPrimary) {
            // Primary button (accent color)
            if (isPressed) {
                bgColor = ThemeManager.accentColor("pressed");
            } else {
                bgColor = ThemeManager.blend(ThemeManager.accentColor("normal"),
                    ThemeManager.accentColor("hover"), hoverProgress);
            }
            fgColor = Color.WHITE;
        } else {
            // Default button
            if (isPressed) {
                bgColor = ThemeManager.borderColor();
            } else {
                Color base = ThemeManager.panelBackground();
                Color target = ThemeManager.borderColor();
                bgColor = ThemeManager.blend(base, target, hoverProgress * 0.3f);
            }
            fgColor = customForeground != null ? customForeground : ThemeManager.textPrimary();
        }
        
        // Draw shadow (subtle, only when enabled)
        if (isEnabled() && !isPressed) {
            g2.setColor(new Color(0, 0, 0, 20 + (int)(10 * hoverProgress)));
            g2.fill(new RoundRectangle2D.Float(2, 3, width - 4, height - 3, borderRadius, borderRadius));
        }
        
        // Draw button background
        g2.setColor(bgColor);
        g2.fill(new RoundRectangle2D.Float(0, 0, width - 1, height - 1, borderRadius, borderRadius));
        
        // Draw subtle border
        if (!isPrimary || !isEnabled()) {
            g2.setColor(ThemeManager.borderColor());
            g2.draw(new RoundRectangle2D.Float(0, 0, width - 1, height - 1, borderRadius, borderRadius));
        }
        
        // Draw text
        g2.setColor(fgColor);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int textX = (width - fm.stringWidth(getText())) / 2;
        int textY = (height + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(getText(), textX, textY);
        
        g2.dispose();
    }
    
    /**
     * Darken a color by a factor
     */
    private Color darken(Color color, float factor) {
        factor = Math.max(0, Math.min(1, factor));
        return new Color(
            (int)(color.getRed() * (1 - factor)),
            (int)(color.getGreen() * (1 - factor)),
            (int)(color.getBlue() * (1 - factor))
        );
    }
    
    /**
     * Lighten a color by a factor
     */
    private Color lighten(Color color, float factor) {
        factor = Math.max(0, Math.min(1, factor));
        return new Color(
            (int)(color.getRed() + (255 - color.getRed()) * factor),
            (int)(color.getGreen() + (255 - color.getGreen()) * factor),
            (int)(color.getBlue() + (255 - color.getBlue()) * factor)
        );
    }
}
