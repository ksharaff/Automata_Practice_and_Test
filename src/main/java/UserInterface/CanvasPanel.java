package UserInterface;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import common.Automaton;

/**
 * Interactive Java2D canvas for building automata graphs with anchors and directional arrows.
 * This is intentionally self-contained so it can sit alongside the existing GraphViz preview.
 */
public class CanvasPanel extends JPanel {
    private static final int STATE_RADIUS = 35;
    private static final int ANCHOR_SIZE = 10;
    private static final int GRID_SPACING = 120;
    private static final double ANCHOR_HIT_TOLERANCE = 2.3;

    private final List<CanvasState> states = new ArrayList<>();
    private final List<CanvasTransition> transitions = new ArrayList<>();
    private final Automaton.MachineType machineType;

    private Consumer<String> definitionChangeListener = def -> {};

    private CanvasState draggingState;
    private Point dragOffset;

    private AnchorPoint pendingAnchor;
    private Point dragLinePoint;

    private int stateCounter = 0;
    private Point addButtonPos;
    private boolean hoverAddButton = false;

    public CanvasPanel(Automaton.MachineType machineType) {
        this.machineType = machineType;
        applyTheme();
        ThemeManager.addListener(() -> {
            applyTheme();
            repaint();
        });
        setOpaque(true);
        installMouseHandlers();
    }

    /**
     * Seed canvas from existing automaton definition text (best-effort heuristic parser).
     */
    public void loadFromText(String text) {
        states.clear();
        transitions.clear();
        pendingAnchor = null;
        draggingState = null;
        dragOffset = null;
        stateCounter = 0;

        ParsedDefinition parsed = ParsedDefinition.parse(text);
        int n = parsed.states.size();
        if (n == 0) {
            repaint();
            return;
        }
        int cols = Math.max(1, (int) Math.ceil(Math.sqrt(n)));
        int x0 = GRID_SPACING;
        int y0 = GRID_SPACING;
        int idx = 0;
        for (String name : parsed.states) {
            int row = idx / cols;
            int col = idx % cols;
            int x = x0 + col * GRID_SPACING;
            int y = y0 + row * GRID_SPACING;
            CanvasState s = new CanvasState(name, new Point(x, y));
            s.isStart = parsed.start != null && parsed.start.equals(name);
            s.isFinal = parsed.finals.contains(name);
            states.add(s);
            idx++;
            stateCounter = Math.max(stateCounter, extractIndex(name) + 1);
        }

        for (ParsedTransition pt : parsed.transitions) {
            CanvasState from = findState(pt.from);
            CanvasState to = findState(pt.to);
            if (from == null || to == null) {
                continue;
            }
            CanvasTransition t = new CanvasTransition(from, to, new ArrayList<>(pt.labels));
            t.offsetIndex = computeOffsetIndex(from, to);
            transitions.add(t);
        }
        repaint();
    }

    public void setDefinitionChangeListener(Consumer<String> listener) {
        this.definitionChangeListener = listener != null ? listener : def -> {};
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw transitions first
        for (CanvasTransition t : transitions) {
            drawTransition(g2, t);
        }

        // Draw states
        for (CanvasState s : states) {
            drawState(g2, s);
        }

        // Rubber-band preview for anchor drag
        if (pendingAnchor != null && dragLinePoint != null) {
            Point from = anchorPosition(pendingAnchor.state, pendingAnchor.anchor);
            g2.setColor(new Color(50, 90, 200, 180));
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{6f, 6f}, 0f));
            g2.drawLine(from.x, from.y, dragLinePoint.x, dragLinePoint.y);
        }

        // Draw floating "Add State" button
        drawAddButton(g2);

        g2.dispose();
    }

    private void drawAddButton(Graphics2D g2) {
        if (states.isEmpty()) {
            int w = getWidth();
            int h = getHeight();
            if (w > 100 && h > 100) {
                addButtonPos = new Point(w / 2, h / 2);
                int btnW = 100;
                int btnH = 40;
                g2.setColor(buttonColor(hoverAddButton));
                g2.fillRoundRect(addButtonPos.x - btnW / 2, addButtonPos.y - btnH / 2, btnW, btnH, 10, 10);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(addButtonPos.x - btnW / 2, addButtonPos.y - btnH / 2, btnW, btnH, 10, 10);
                FontMetrics fm = g2.getFontMetrics();
                String txt = "Add State";
                int txtW = fm.stringWidth(txt);
                g2.drawString(txt, addButtonPos.x - txtW / 2, addButtonPos.y + fm.getAscent() / 2);
            }
        }
    }

    private void drawState(Graphics2D g2, CanvasState s) {
        Point p = s.position;
        int r = STATE_RADIUS;
        g2.setColor(stateFillColor());
        g2.fillOval(p.x - r, p.y - r, 2 * r, 2 * r);
        g2.setColor(stateStrokeColor());
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(p.x - r, p.y - r, 2 * r, 2 * r);
        if (s.isFinal) {
            g2.drawOval(p.x - r + 5, p.y - r + 5, 2 * r - 10, 2 * r - 10);
        }
        if (s.isStart) {
            int arrowX = p.x - r - 30;
            int arrowY = p.y;
            g2.setColor(stateStrokeColor());
            drawArrow(g2, new Point(arrowX, arrowY), new Point(p.x - r, p.y), 0);
        }
        // Label
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(s.name);
        g2.setColor(textColor());
        g2.drawString(s.name, p.x - textWidth / 2, p.y + fm.getAscent() / 2);

        // Anchors
        g2.setColor(anchorColor());
        for (Anchor anchor : Anchor.values()) {
            Point ap = anchorPosition(s, anchor);
            g2.fillOval(ap.x - ANCHOR_SIZE / 2, ap.y - ANCHOR_SIZE / 2, ANCHOR_SIZE, ANCHOR_SIZE);
        }
    }

    private void drawTransition(Graphics2D g2, CanvasTransition t) {
        Point from = anchorPosition(t.from, t.fromAnchor);
        Point to = anchorPosition(t.to, t.toAnchor);

        if (t.from == t.to) {
            // Self-loop: tighter top loop with arrow driven inward
            int spread = STATE_RADIUS / 2 - 2 + t.offsetIndex * 4;
            int lift = STATE_RADIUS + 8 + t.offsetIndex * 5;
            int rimY = from.y - 6; // sit closer to the state outline

            Point start = new Point(from.x - spread, rimY);
            Point ctrl1 = new Point(from.x - spread, rimY - lift);
            Point ctrl2 = new Point(from.x + spread, rimY - lift);
            Point end = new Point(from.x + spread, rimY);

            CubicCurve2D loop = new CubicCurve2D.Double(
                start.x, start.y,
                ctrl1.x, ctrl1.y,
                ctrl2.x, ctrl2.y,
                end.x, end.y
            );

            g2.setColor(edgeColor());
            g2.setStroke(new BasicStroke(2.2f));
            g2.draw(loop);

            // Arrow sits on the right leg, angled inward toward the state center
            Point arrowBase = pointOnCubic(loop, 0.83);
            Point rawTip = pointOnCubic(loop, 0.94);
            Point arrowTip = new Point(rawTip.x - 4, rawTip.y + 14); // drive tip inward and downward toward center
            double arrowAngle = Math.atan2(arrowTip.y - arrowBase.y, arrowTip.x - arrowBase.x);
            drawArrow(g2, arrowBase, arrowTip, arrowAngle);

            // Label above the loop apex
            Point labelPos = new Point(from.x, rimY - lift + 8);
            drawTransitionLabel(g2, t, labelPos);
            return;
        }

        int dx = to.x - from.x;
        int dy = to.y - from.y;
        double dist = Math.max(1.0, Math.hypot(dx, dy));
        double nx = -dy / dist;
        double ny = dx / dist;
        
        // Check if there's a reverse transition for bidirectional curves
        boolean hasReverse = transitions.stream()
            .anyMatch(tr -> tr.from == t.to && tr.to == t.from);
        
        int offset = t.offsetIndex * 40;
        // For bidirectional edges, alternate the offset direction
        if (hasReverse && t.from.name.compareTo(t.to.name) > 0) {
            offset = -offset;
        }

        int ctrlX = (int) ((from.x + to.x) / 2 + nx * offset);
        int ctrlY = (int) ((from.y + to.y) / 2 + ny * offset);

        QuadCurve2D q = new QuadCurve2D.Double(from.x, from.y, ctrlX, ctrlY, to.x, to.y);
        g2.setColor(edgeColor());
        g2.setStroke(new BasicStroke(2f));
        g2.draw(q);

        // Arrowhead at end
        Point arrowBase = pointOnCurve(q, 0.98);
        double angle = Math.atan2(to.y - ctrlY, to.x - ctrlX);
        drawArrow(g2, arrowBase, to, angle);

        // Label near control point
        drawTransitionLabel(g2, t, new Point(ctrlX, ctrlY));
    }

    private void drawTransitionLabel(Graphics2D g2, CanvasTransition t, Point pos) {
        String text = String.join(",", t.labels);
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(text);
        int h = fm.getHeight();
        g2.setColor(labelBgColor());
        g2.fillRoundRect(pos.x - w / 2 - 4, pos.y - h + 2, w + 8, h, 8, 8);
        g2.setColor(labelBorderColor());
        g2.drawRoundRect(pos.x - w / 2 - 4, pos.y - h + 2, w + 8, h, 8, 8);
        g2.setColor(labelTextColor());
        g2.drawString(text, pos.x - w / 2, pos.y);
    }

    private void drawArrow(Graphics2D g2, Point from, Point to, double angleHint) {
        double angle = angleHint != 0 ? angleHint : Math.atan2(to.y - from.y, to.x - from.x);
        int len = 12;
        int wing = 5;
        int x1 = (int) (to.x - len * Math.cos(angle) + wing * Math.sin(angle));
        int y1 = (int) (to.y - len * Math.sin(angle) - wing * Math.cos(angle));
        int x2 = (int) (to.x - len * Math.cos(angle) - wing * Math.sin(angle));
        int y2 = (int) (to.y - len * Math.sin(angle) + wing * Math.cos(angle));
        Polygon arrowHead = new Polygon(new int[]{to.x, x1, x2}, new int[]{to.y, y1, y2}, 3);
        g2.fillPolygon(arrowHead);
    }

    private Point pointOnCurve(QuadCurve2D q, double t) {
        double x = Math.pow(1 - t, 2) * q.getX1() + 2 * (1 - t) * t * q.getCtrlX() + Math.pow(t, 2) * q.getX2();
        double y = Math.pow(1 - t, 2) * q.getY1() + 2 * (1 - t) * t * q.getCtrlY() + Math.pow(t, 2) * q.getY2();
        return new Point((int) x, (int) y);
    }

    private Point pointOnCubic(CubicCurve2D c, double t) {
        double u = 1 - t;
        double x = Math.pow(u, 3) * c.getX1()
                 + 3 * Math.pow(u, 2) * t * c.getCtrlX1()
                 + 3 * u * Math.pow(t, 2) * c.getCtrlX2()
                 + Math.pow(t, 3) * c.getX2();
        double y = Math.pow(u, 3) * c.getY1()
                 + 3 * Math.pow(u, 2) * t * c.getCtrlY1()
                 + 3 * u * Math.pow(t, 2) * c.getCtrlY2()
                 + Math.pow(t, 3) * c.getY2();
        return new Point((int) x, (int) y);
    }

    private void applyTheme() {
        setBackground(ThemeManager.background());
    }

    private Color stateFillColor() {
        return ThemeManager.isDarkMode() ? new Color(50, 58, 70) : new Color(240, 244, 248);
    }

    private Color stateStrokeColor() {
        return ThemeManager.isDarkMode() ? new Color(220, 224, 232) : Color.DARK_GRAY;
    }

    private Color textColor() {
        return ThemeManager.isDarkMode() ? new Color(235, 238, 243) : new Color(20, 20, 20);
    }

    private Color anchorColor() {
        return ThemeManager.isDarkMode() ? new Color(120, 170, 230) : new Color(60, 100, 160);
    }

    private Color edgeColor() {
        return ThemeManager.isDarkMode() ? new Color(230, 235, 245) : Color.BLACK;
    }

    private Color labelBgColor() {
        return ThemeManager.isDarkMode() ? new Color(56, 60, 70) : new Color(255, 255, 230);
    }

    private Color labelBorderColor() {
        return ThemeManager.isDarkMode() ? new Color(190, 195, 205) : Color.DARK_GRAY;
    }

    private Color labelTextColor() {
        return textColor();
    }

    private Color buttonColor(boolean hover) {
        return ThemeManager.accent(hover);
    }

    private void installMouseHandlers() {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                
                // Check floating add button
                if (addButtonPos != null && isOnAddButton(p)) {
                    addStateAt(p);
                    addButtonPos = null;
                    repaint();
                    return;
                }
                
                CanvasState hit = findStateAt(p);
                if (hit != null) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        showStateMenu(hit, p);
                        return;
                    }
                    if (isOnAnchor(hit, p) != null) {
                        pendingAnchor = new AnchorPoint(hit, isOnAnchor(hit, p));
                        dragLinePoint = p;
                    } else {
                        draggingState = hit;
                        dragOffset = new Point(p.x - hit.position.x, p.y - hit.position.y);
                    }
                    return;
                }
                
                // Check transition click
                CanvasTransition hitTrans = findTransitionAt(p);
                if (hitTrans != null && SwingUtilities.isRightMouseButton(e)) {
                    showTransitionMenu(hitTrans, p);
                    return;
                }
                
                // Right-click on empty space
                if (SwingUtilities.isRightMouseButton(e)) {
                    showEmptySpaceMenu(p);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Point p = e.getPoint();
                if (draggingState != null) {
                    draggingState = null;
                    dragOffset = null;
                    return;
                }
                if (pendingAnchor != null) {
                    CanvasState target = findStateAt(p);
                    if (target != null) {
                        Anchor anchor = isOnAnchor(target, p);
                        if (anchor != null) {
                            // Check if transition already exists between these states
                            boolean transitionExists = transitions.stream()
                                .anyMatch(t -> t.from == pendingAnchor.state && t.to == target);
                            
                            if (transitionExists) {
                                JOptionPane.showMessageDialog(CanvasPanel.this,
                                    "A transition already exists between " + pendingAnchor.state.name + 
                                    " and " + target.name + ".\nYou cannot create duplicate transitions.",
                                    "Duplicate Transition",
                                    JOptionPane.ERROR_MESSAGE);
                            } else {
                                String label = JOptionPane.showInputDialog(CanvasPanel.this, "Transition label(s):", "a");
                                if (label != null && !label.trim().isEmpty()) {
                                    CanvasTransition t = new CanvasTransition(pendingAnchor.state, target, splitLabels(label));
                                    t.fromAnchor = pendingAnchor.anchor;
                                    t.toAnchor = anchor;
                                    t.offsetIndex = computeOffsetIndex(pendingAnchor.state, target);
                                    transitions.add(t);
                                    pushDefinitionUpdate();
                                }
                            }
                        }
                    }
                    pendingAnchor = null;
                    dragLinePoint = null;
                    repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (pendingAnchor != null) {
                    dragLinePoint = e.getPoint();
                    repaint();
                    return;
                }
                if (draggingState != null && dragOffset != null) {
                    draggingState.position = new Point(e.getX() - dragOffset.x, e.getY() - dragOffset.y);
                    repaint();
                    pushDefinitionUpdate();
                }
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                boolean wasHover = hoverAddButton;
                hoverAddButton = addButtonPos != null && isOnAddButton(p);
                if (wasHover != hoverAddButton) {
                    repaint();
                }
            }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    private void addStateAt(Point p) {
        String name = "q" + stateCounter;
        stateCounter++;
        CanvasState s = new CanvasState(name, p);
        if (states.isEmpty()) {
            s.isStart = true;
        }
        states.add(s);
        repaint();
        pushDefinitionUpdate();
    }

    private void showTransitionMenu(CanvasTransition transition, Point at) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Edit transition");
        JMenuItem deleteItem = new JMenuItem("Delete transition");
        
        editItem.addActionListener(ev -> {
            String currentLabels = String.join(" ", transition.labels);
            String newLabels = JOptionPane.showInputDialog(CanvasPanel.this, "Transition label(s):", currentLabels);
            if (newLabels != null && !newLabels.trim().isEmpty()) {
                transition.labels.clear();
                transition.labels.addAll(splitLabels(newLabels));
                pushDefinitionUpdate();
                repaint();
            }
        });
        
        deleteItem.addActionListener(ev -> {
            transitions.remove(transition);
            pushDefinitionUpdate();
            repaint();
        });
        
        menu.add(editItem);
        menu.add(deleteItem);
        menu.show(this, at.x, at.y);
    }
    
    private void showEmptySpaceMenu(Point at) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem addStateItem = new JMenuItem("Add state");

        addStateItem.addActionListener(ev -> {
            addStateAt(at);
        });

        menu.add(addStateItem);
        menu.show(this, at.x, at.y);
    }
    
    private void showStateMenu(CanvasState state, Point at) {
        JPopupMenu menu = new JPopupMenu();
        JCheckBoxMenuItem startItem = new JCheckBoxMenuItem("Start state", state.isStart);
        JCheckBoxMenuItem finalItem = new JCheckBoxMenuItem("Final state", state.isFinal);
        JMenuItem selfTransItem = new JMenuItem("Add self-transition");
        JMenuItem deleteItem = new JMenuItem("Delete state");

        startItem.addActionListener(ev -> {
            for (CanvasState s : states) {
                s.isStart = false;
            }
            state.isStart = true;
            pushDefinitionUpdate();
            repaint();
        });

        finalItem.addActionListener(ev -> {
            state.isFinal = !state.isFinal;
            pushDefinitionUpdate();
            repaint();
        });

        selfTransItem.addActionListener(ev -> {
            String label = JOptionPane.showInputDialog(CanvasPanel.this, "Transition label(s):", "a");
            if (label != null && !label.trim().isEmpty()) {
                CanvasTransition t = new CanvasTransition(state, state, splitLabels(label));
                t.fromAnchor = Anchor.NORTH;
                t.toAnchor = Anchor.NORTH;
                t.offsetIndex = 0;
                transitions.add(t);
                pushDefinitionUpdate();
                repaint();
            }
        });

        deleteItem.addActionListener(ev -> {
            transitions.removeIf(t -> t.from == state || t.to == state);
            states.remove(state);
            pushDefinitionUpdate();
            repaint();
        });

        menu.add(startItem);
        menu.add(finalItem);
        menu.addSeparator();
        menu.add(selfTransItem);
        menu.addSeparator();
        menu.add(deleteItem);
        menu.show(this, at.x, at.y);
    }

    

    private Anchor isOnAnchor(CanvasState state, Point p) {
        for (Anchor a : Anchor.values()) {
            Point ap = anchorPosition(state, a);
            if (p.distance(ap) <= ANCHOR_SIZE * ANCHOR_HIT_TOLERANCE) {
                return a;
            }
        }
        return null;
    }

    private CanvasState findStateAt(Point p) {
        for (CanvasState s : states) {
            if (p.distance(s.position) <= STATE_RADIUS) {
                return s;
            }
        }
        return null;
    }
    
    private CanvasTransition findTransitionAt(Point p) {
        for (CanvasTransition t : transitions) {
            Point from = anchorPosition(t.from, t.fromAnchor);
            Point to = anchorPosition(t.to, t.toAnchor);
            
            if (from.equals(to)) {
                int spread = STATE_RADIUS / 2 - 8 + t.offsetIndex * 4;
                int lift = STATE_RADIUS + 8 + t.offsetIndex * 5;
                int rimY = from.y - 6;
                CubicCurve2D loop = new CubicCurve2D.Double(
                    from.x - spread, rimY,
                    from.x - spread, rimY - lift,
                    from.x + spread, rimY - lift,
                    from.x + spread, rimY
                );
                for (double u = 0; u <= 1.0; u += 0.05) {
                    Point cp = pointOnCubic(loop, u);
                    if (p.distance(cp) < 8) {
                        return t;
                    }
                }
            } else {
                // Check curve proximity
                int dx = to.x - from.x;
                int dy = to.y - from.y;
                double dist = Math.max(1.0, Math.hypot(dx, dy));
                double nx = -dy / dist;
                double ny = dx / dist;
                int offset = t.offsetIndex * 40;
                int ctrlX = (int) ((from.x + to.x) / 2 + nx * offset);
                int ctrlY = (int) ((from.y + to.y) / 2 + ny * offset);
                
                // Check if point is near the curve (sample multiple points)
                QuadCurve2D q = new QuadCurve2D.Double(from.x, from.y, ctrlX, ctrlY, to.x, to.y);
                for (double u = 0; u <= 1.0; u += 0.05) {
                    Point cp = pointOnCurve(q, u);
                    if (p.distance(cp) < 8) {
                        return t;
                    }
                }
            }
        }
        return null;
    }
    
    private boolean isOnAddButton(Point p) {
        if (addButtonPos == null) return false;
        int btnW = 100;
        int btnH = 40;
        return p.x >= addButtonPos.x - btnW / 2 && p.x <= addButtonPos.x + btnW / 2 &&
               p.y >= addButtonPos.y - btnH / 2 && p.y <= addButtonPos.y + btnH / 2;
    }

    private CanvasState findState(String name) {
        for (CanvasState s : states) {
            if (s.name.equals(name)) {
                return s;
            }
        }
        return null;
    }

    private Point anchorPosition(CanvasState s, Anchor anchor) {
        int r = STATE_RADIUS;
        switch (anchor) {
            case NORTH:
                return new Point(s.position.x, s.position.y - r);
            case SOUTH:
                return new Point(s.position.x, s.position.y + r);
            case EAST:
                return new Point(s.position.x + r, s.position.y);
            case WEST:
            default:
                return new Point(s.position.x - r, s.position.y);
        }
    }

    private List<String> splitLabels(String input) {
        String[] parts = input.trim().split("[ ,]+", -1);
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (!p.isEmpty()) {
                out.add(p);
            }
        }
        return out;
    }

    private int computeOffsetIndex(CanvasState from, CanvasState to) {
        // Count existing edges between the same pair (both directions) to offset curves
        int sameDir = 0;
        int oppositeDir = 0;
        for (CanvasTransition t : transitions) {
            if (t.from == from && t.to == to) {
                sameDir++;
            } else if (t.from == to && t.to == from) {
                oppositeDir++;
            }
        }
        return Math.max(sameDir, oppositeDir);
    }

    private void pushDefinitionUpdate() {
        definitionChangeListener.accept(generateDefinition());
    }

    private String generateDefinition() {
        StringBuilder sb = new StringBuilder();
        CanvasState start = states.stream().filter(s -> s.isStart).findFirst().orElse(null);
        Set<String> finals = new HashSet<>();
        for (CanvasState s : states) {
            if (s.isFinal) {
                finals.add(s.name);
            }
        }
        Set<String> alphabet = new HashSet<>();
        for (CanvasTransition t : transitions) {
            alphabet.addAll(t.labels);
        }

        if (start != null) sb.append("Start: ").append(start.name).append("\n");
        if (!finals.isEmpty()) {
            sb.append("Finals: ");
            finals.forEach(f -> sb.append(f).append(' '));
            sb.setLength(sb.length() - 1);
            sb.append("\n");
        }
        if (!alphabet.isEmpty()) {
            sb.append("Alphabet: ");
            alphabet.forEach(a -> sb.append(a).append(' '));
            sb.setLength(sb.length() - 1);
            sb.append("\n");
        }
        if (!states.isEmpty()) {
            sb.append("States: ");
            states.forEach(s -> sb.append(s.name).append(' '));
            sb.setLength(sb.length() - 1);
            sb.append("\n\n");
        }
        sb.append("Transitions:\n");
        for (CanvasTransition t : transitions) {
            sb.append(t.from.name)
              .append(" -> ")
              .append(t.to.name)
              .append(" (")
              .append(String.join(" ", t.labels))
              .append(")\n");
        }
        return sb.toString();
    }

    private int extractIndex(String name) {
        Matcher m = Pattern.compile("q(\\d+)").matcher(name);
        if (m.matches()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private enum Anchor { NORTH, EAST, SOUTH, WEST }

    private static class AnchorPoint {
        final CanvasState state;
        final Anchor anchor;
        AnchorPoint(CanvasState state, Anchor anchor) {
            this.state = state;
            this.anchor = anchor;
        }
    }

    private static class CanvasState {
        final String name;
        Point position;
        boolean isStart;
        boolean isFinal;
        CanvasState(String name, Point position) {
            this.name = name;
            this.position = position;
        }
    }

    private static class CanvasTransition {
        final CanvasState from;
        final CanvasState to;
        final List<String> labels;
        Anchor fromAnchor = Anchor.EAST;
        Anchor toAnchor = Anchor.WEST;
        int offsetIndex = 0;
        CanvasTransition(CanvasState from, CanvasState to, List<String> labels) {
            this.from = from;
            this.to = to;
            this.labels = labels;
        }
    }

    /** ParsedDefinition is a tolerant reader for common DFA/NFA-style definitions. */
    private static class ParsedDefinition {
        final List<String> states = new ArrayList<>();
        String start;
        final Set<String> finals = new HashSet<>();
        final List<ParsedTransition> transitions = new ArrayList<>();

        static ParsedDefinition parse(String text) {
            ParsedDefinition pd = new ParsedDefinition();
            String[] lines = text.split("\r?\n");
            Pattern stateLine = Pattern.compile("(?i)states:\\s*(.*)");
            Pattern startLine = Pattern.compile("(?i)start:\\s*([\\w-]+)");
            Pattern finalsLine = Pattern.compile("(?i)finals:\\s*(.*)");
            Pattern transLine = Pattern.compile("(?i)([\\w-]+)\\s*->\\s*([\\w-]+)\\s*\\(([^)]*)\\)");
            boolean inTransitions = false;
            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.toLowerCase().startsWith("transitions")) {
                    inTransitions = true;
                    continue;
                }
                if (!inTransitions) {
                    Matcher mState = stateLine.matcher(line);
                    Matcher mStart = startLine.matcher(line);
                    Matcher mFinals = finalsLine.matcher(line);
                    if (mState.matches()) {
                        for (String s : mState.group(1).trim().split("\\s+")) {
                            if (!s.isEmpty()) pd.states.add(s);
                        }
                    } else if (mStart.matches()) {
                        pd.start = mStart.group(1).trim();
                    } else if (mFinals.matches()) {
                        for (String f : mFinals.group(1).trim().split("\\s+")) {
                            if (!f.isEmpty()) pd.finals.add(f);
                        }
                    }
                } else {
                    Matcher mt = transLine.matcher(line);
                    if (mt.matches()) {
                        String from = mt.group(1).trim();
                        String to = mt.group(2).trim();
                        String labels = mt.group(3).trim();
                        List<String> labelList = new ArrayList<>();
                        for (String l : labels.split("\\s+")) {
                            if (!l.isEmpty()) labelList.add(l);
                        }
                        pd.transitions.add(new ParsedTransition(from, to, labelList));
                        if (!pd.states.contains(from)) pd.states.add(from);
                        if (!pd.states.contains(to)) pd.states.add(to);
                    }
                }
            }
            if (pd.start == null && !pd.states.isEmpty()) {
                pd.start = pd.states.get(0);
            }
            return pd;
        }
    }

    private static class ParsedTransition {
        final String from;
        final String to;
        final List<String> labels;
        ParsedTransition(String from, String to, List<String> labels) {
            this.from = from;
            this.to = to;
            this.labels = labels;
        }
    }
}
