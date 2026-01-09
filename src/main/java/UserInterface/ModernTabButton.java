package UserInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern styled tab button with rounded top corners, smooth transitions, and visual active indicator.
 */
public class ModernTabButton extends JPanel {
    
    private JLabel textLabel;
    private JButton closeButton;
    private boolean isActive = false;
    private boolean isHovered = false;
    private float hoverProgress = 0f;
    private Timer hoverTimer;
    private int tabIndex;
    private Runnable onSelectAction;
    private Runnable onCloseAction;
    
    public ModernTabButton(String text, int tabIndex, boolean isActive) {
        this.tabIndex = tabIndex;
        this.isActive = isActive;
        
        setupPanel();
        createComponents(text);
        setupHoverAnimation();
    }
    
    private void setupPanel() {
        setLayout(new BorderLayout(ThemeManager.SPACING_S, 0));
        setOpaque(false);
        setPreferredSize(new Dimension(120, ThemeManager.TAB_HEIGHT));
        setMaximumSize(new Dimension(250, ThemeManager.TAB_HEIGHT));
        
        // Add mouse listeners for hover effect and selection
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isActive) {
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
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                if (!hoverTimer.isRunning()) {
                    hoverTimer.start();
                }
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onSelectAction != null && !isActive) {
                    onSelectAction.run();
                }
            }
        });
    }
    
    private void createComponents(String text) {
        // Text label
        textLabel = new JLabel(text);
        textLabel.setFont(ThemeManager.getFont("BODY", Font.PLAIN));
        textLabel.setForeground(isActive ? ThemeManager.textPrimary() : ThemeManager.textSecondary());
        textLabel.setBorder(BorderFactory.createEmptyBorder(
            0, ThemeManager.SPACING_M, 0, ThemeManager.SPACING_XS
        ));
        
        // Close button
        closeButton = new JButton("×");
        closeButton.setFont(ThemeManager.getFont("SUBHEAD", Font.BOLD));
        closeButton.setForeground(ThemeManager.textSecondary());
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setPreferredSize(new Dimension(ThemeManager.MIN_CLICK_TARGET, ThemeManager.MIN_CLICK_TARGET));
        closeButton.setToolTipText("Close tab");
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.setBorder(null);
        
        closeButton.addMouseListener(new MouseAdapter() {
            boolean inButton = false;
            
            @Override
            public void mouseEntered(MouseEvent e) {
                inButton = true;
                closeButton.setForeground(new Color(180, 60, 60));
                closeButton.setContentAreaFilled(true);
                closeButton.setBackground(ThemeManager.isDarkMode() ? 
                    new Color(60, 40, 40) : new Color(250, 240, 240));
                repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                inButton = false;
                closeButton.setForeground(ThemeManager.textSecondary());
                closeButton.setContentAreaFilled(false);
                repaint();
            }
        });
        
        closeButton.addActionListener(e -> {
            if (onCloseAction != null) {
                onCloseAction.run();
            }
        });
        
        add(textLabel, BorderLayout.CENTER);
        add(closeButton, BorderLayout.EAST);
    }
    
    private void setupHoverAnimation() {
        hoverTimer = new Timer(16, e -> {
            if (isHovered && hoverProgress < 1.0f) {
                hoverProgress = Math.min(1.0f, hoverProgress + 0.15f);
                repaint();
            } else if (!isHovered && hoverProgress > 0f) {
                hoverProgress = Math.max(0f, hoverProgress - 0.15f);
                repaint();
            }
            
            if ((isHovered && hoverProgress >= 1.0f) || (!isHovered && hoverProgress <= 0f)) {
                hoverTimer.stop();
            }
        });
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
        textLabel.setForeground(isActive ? ThemeManager.textPrimary() : ThemeManager.textSecondary());
        repaint();
    }
    
    public void setText(String text) {
        textLabel.setText(text);
    }
    
    public void setOnSelectAction(Runnable action) {
        this.onSelectAction = action;
    }
    
    public void setOnCloseAction(Runnable action) {
        this.onCloseAction = action;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        int radius = ThemeManager.RADIUS_MD;
        
        Color bgColor;
        if (isActive) {
            bgColor = ThemeManager.background();
        } else {
            Color base = ThemeManager.panelBackground();
            Color hover = ThemeManager.blend(base, ThemeManager.borderColor(), 0.4f);
            bgColor = ThemeManager.blend(base, hover, hoverProgress);
        }
        
        // Fill background with rounded top corners
        g2.setColor(bgColor);
        // Draw rectangle for bottom part
        g2.fillRect(0, radius, width, height - radius);
        // Draw rounded top part
        g2.fill(new RoundRectangle2D.Float(0, 0, width, height + radius, radius * 2, radius * 2));
        
        // Draw active indicator (bottom border for active tab)
        if (isActive) {
            g2.setColor(ThemeManager.accentColor("normal"));
            g2.fillRect(0, height - 3, width, 3);
        }
        
        // Draw subtle border
        if (!isActive) {
            g2.setColor(ThemeManager.borderColor());
            g2.drawLine(width - 1, radius, width - 1, height);
        }
        
        g2.dispose();
    }
}
