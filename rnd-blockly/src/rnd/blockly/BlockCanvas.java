package rnd.blockly;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * BlockCanvas — handles interaction, zoom/pan, and rendering.
 * All block geometry is delegated to BlockShapes.
 *
 * Responsibilities:
 *   - Mouse: drag, group-drag, rubber-band select, double-click to edit, pan
 *   - Scroll wheel: zoom toward cursor
 *   - Keyboard: Ctrl+A select all, Escape deselect
 *   - Snap: find target, commit snap, reposition children
 *   - Paint: grid, connections, block fill/border/label, HUD
 *
 * NOT responsible for: any path geometry (see BlockShapes).
 */
public class BlockCanvas extends JPanel {

    // ── Collections ───────────────────────────────────────────────────────────
    private final List<Block> blocks   = new ArrayList<>();
    private final List<Block> selected = new ArrayList<>();

    // ── Drag state ────────────────────────────────────────────────────────────
    private Block   dragging      = null;
    private int     dragOffsetX, dragOffsetY;
    private boolean groupDragging = false;
    private int[]   groupOffsetX, groupOffsetY;

    // ── Rubber-band ───────────────────────────────────────────────────────────
    private boolean selecting = false;
    private int     selX, selY, selW, selH;

    // ── Zoom / pan ────────────────────────────────────────────────────────────
    private double  scale      = 1.0;
    private double  translateX = 0;
    private double  translateY = 0;
    private int     lastPanX, lastPanY;
    private boolean panning    = false;

    // ── Snap highlight ────────────────────────────────────────────────────────
    private Block snapHighlight = null;

    // ── Callbacks ─────────────────────────────────────────────────────────────
    private Consumer<List<Block>> onChangeListener;
    private Frame ownerFrame;

    public void setOwnerFrame(Frame f)                       { this.ownerFrame = f; }
    public void setOnChangeListener(Consumer<List<Block>> l) { this.onChangeListener = l; }
    private void notifyChange() {
        if (onChangeListener != null) onChangeListener.accept(blocks);
    }

    // ── Canvas limits ─────────────────────────────────────────────────────────
    private static final int    SNAP_DIST = 28;
    private static final double MIN_SCALE = 0.05;
    private static final double MAX_SCALE = 3.0;

    // ═════════════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR — wires up all input listeners
    // ═════════════════════════════════════════════════════════════════════════

    public BlockCanvas() {
        setBackground(Color.decode("#1e1e2e"));
        setPreferredSize(new Dimension(3000, 3000));
        setFocusable(true);

        // ── Keyboard ──────────────────────────────────────────────────────────
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_A) {
                    selected.clear();
                    for (Block b : blocks) if (b.parent == null) selected.add(b);
                    repaint();
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    selected.clear(); repaint();
                }
            }
        });

        // ── Mouse ─────────────────────────────────────────────────────────────
        MouseAdapter mouse = new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                Point2D.Double cp = toCanvas(e.getX(), e.getY());

                // Double-click → open param editor
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    for (int i = blocks.size()-1; i >= 0; i--) {
                        Block b = blocks.get(i);
                        if (b.contains((int)cp.x, (int)cp.y)) {
                            if (ParamEditorDialog.show(ownerFrame, b)) {
                                notifyChange(); repaint();
                            }
                            return;
                        }
                    }
                }

                // Pan: middle-click or Alt+left-drag
                if (SwingUtilities.isMiddleMouseButton(e) ||
                   (SwingUtilities.isLeftMouseButton(e) && e.isAltDown())) {
                    panning = true;
                    lastPanX = e.getX(); lastPanY = e.getY();
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    return;
                }

                // Hit test
                Block hit = hitTest((int)cp.x, (int)cp.y);

                if (hit != null) {
                    // Ctrl+click: toggle selection
                    if (e.isControlDown()) {
                        if (selected.contains(hit)) selected.remove(hit);
                        else selected.add(hit);
                        repaint(); return;
                    }
                    // Group drag: clicked block is part of multi-selection
                    if (selected.contains(hit) && selected.size() > 1) {
                        groupDragging = true;
                        groupOffsetX  = new int[selected.size()];
                        groupOffsetY  = new int[selected.size()];
                        for (int i = 0; i < selected.size(); i++) {
                            groupOffsetX[i] = (int)cp.x - selected.get(i).x;
                            groupOffsetY[i] = (int)cp.y - selected.get(i).y;
                        }
                        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                        return;
                    }
                    // Single drag
                    selected.clear(); selected.add(hit);
                    dragging = hit;
                    if (hit.parent != null) {
                        hit.parent.children.remove(hit); hit.parent = null;
                    }
                    blocks.remove(hit); blocks.add(hit); // bring to front
                    dragOffsetX = (int)cp.x - hit.x;
                    dragOffsetY = (int)cp.y - hit.y;
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    // Click on empty space: start rubber-band
                    if (!e.isControlDown()) selected.clear();
                    selecting = true;
                    selX = e.getX(); selY = e.getY(); selW = 0; selH = 0;
                }
                repaint();
            }

            public void mouseDragged(MouseEvent e) {
                if (panning) {
                    translateX += e.getX() - lastPanX;
                    translateY += e.getY() - lastPanY;
                    lastPanX = e.getX(); lastPanY = e.getY();
                    repaint(); return;
                }
                if (groupDragging) {
                    Point2D.Double cp = toCanvas(e.getX(), e.getY());
                    for (int i = 0; i < selected.size(); i++) {
                        selected.get(i).x = (int)cp.x - groupOffsetX[i];
                        selected.get(i).y = (int)cp.y - groupOffsetY[i];
                        selected.get(i).repositionChildren();
                    }
                    repaint(); return;
                }
                if (dragging != null) {
                    Point2D.Double cp = toCanvas(e.getX(), e.getY());
                    dragging.x = (int)cp.x - dragOffsetX;
                    dragging.y = (int)cp.y - dragOffsetY;
                    dragging.repositionChildren();
                    snapHighlight = findSnapTarget(dragging);
                    repaint(); return;
                }
                if (selecting) {
                    selW = e.getX() - selX;
                    selH = e.getY() - selY;
                    updateRubberBandSelection();
                    repaint();
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (panning) {
                    panning = false;
                    setCursor(Cursor.getDefaultCursor()); return;
                }
                if (groupDragging) {
                    groupDragging = false; snapHighlight = null;
                    setCursor(Cursor.getDefaultCursor());
                    notifyChange(); repaint(); return;
                }
                if (dragging != null) {
                    trySnap(dragging);
                    dragging = null; snapHighlight = null;
                    setCursor(Cursor.getDefaultCursor());
                    notifyChange(); repaint(); return;
                }
                if (selecting) {
                    selecting = false;
                    updateRubberBandSelection();
                    repaint();
                }
            }

            public void mouseWheelMoved(MouseWheelEvent e) {
                double factor = e.getPreciseWheelRotation() < 0 ? 1.12 : 1.0 / 1.12;
                double ns = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale * factor));
                if (ns == scale) return;
                double mx = e.getX(), my = e.getY();
                translateX = mx - (mx - translateX) * (ns / scale);
                translateY = my - (my - translateY) * (ns / scale);
                scale = ns;
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  INTERACTION HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private Block hitTest(int cx, int cy) {
        for (int i = blocks.size()-1; i >= 0; i--)
            if (blocks.get(i).contains(cx, cy)) return blocks.get(i);
        return null;
    }

    private Point2D.Double toCanvas(int sx, int sy) {
        return new Point2D.Double((sx - translateX) / scale, (sy - translateY) / scale);
    }

    private void updateRubberBandSelection() {
        int rx = selW >= 0 ? selX : selX + selW;
        int ry = selH >= 0 ? selY : selY + selH;
        int rw = Math.abs(selW), rh = Math.abs(selH);
        Point2D.Double tl = toCanvas(rx, ry);
        Point2D.Double br = toCanvas(rx + rw, ry + rh);
        Rectangle cr = new Rectangle((int)tl.x, (int)tl.y,
                                     (int)(br.x - tl.x), (int)(br.y - tl.y));
        selected.clear();
        for (Block b : blocks)
            if (b.parent == null && cr.intersects(new Rectangle(b.x, b.y, b.width, b.height)))
                selected.add(b);
    }

    // ── Snap ──────────────────────────────────────────────────────────────────

    private Block findSnapTarget(Block dropped) {
        if (BlockShapes.isExpression(dropped)) return null;
        for (Block host : blocks) {
            if (host == dropped || !host.canHaveChildren()) continue;
            if (isDescendant(host, dropped)) continue;
            // Snap point: NOFF from host left, just below host bottom
            double slotX = host.x + BlockShapes.NOFF;
            double slotY = host.y + host.height;
            // For C-blocks, account for existing children in the body
            if (BlockShapes.isCBlock(host)) {
                for (Block c : host.children) slotY += c.height;
            } else {
                for (Block c : host.children) slotY += c.height;
            }
            if (Math.hypot(dropped.x - slotX, dropped.y - slotY) < SNAP_DIST)
                return host;
        }
        return null;
    }

    private void trySnap(Block dropped) {
        Block host = findSnapTarget(dropped);
        if (host == null) return;
        double slotX = host.x;
        double slotY = host.y + host.height;
        for (Block c : host.children) slotY += c.height;
        host.children.add(dropped);
        dropped.parent = host;
        dropped.x = (int) slotX;
        dropped.y = (int) slotY;
        dropped.repositionChildren();
    }

    private boolean isDescendant(Block candidate, Block root) {
        for (Block child : root.children)
            if (child == candidate || isDescendant(candidate, child)) return true;
        return false;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void addBlock(Block b)    { blocks.add(b); repaint(); notifyChange(); }
    public void clearBlocks()        { blocks.clear(); selected.clear(); repaint(); notifyChange(); }
    public List<Block> getBlocks()   { return blocks; }
    public void resetView()          { scale = 1.0; translateX = 0; translateY = 0; repaint(); }
    public void zoomIn()             { applyZoom(1.2); }
    public void zoomOut()            { applyZoom(1.0 / 1.2); }

    private void applyZoom(double f) {
        double cx = getWidth() / 2.0, cy = getHeight() / 2.0;
        double ns = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale * f));
        if (ns == scale) return;
        translateX = cx - (cx - translateX) * (ns / scale);
        translateY = cy - (cy - translateY) * (ns / scale);
        scale = ns;
        repaint();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PAINTING — delegates geometry to BlockShapes
    // ═════════════════════════════════════════════════════════════════════════

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,    RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

        drawGrid(g2);

        g2.translate(translateX, translateY);
        g2.scale(scale, scale);

        // Connections behind blocks
        for (Block b : blocks) drawConnections(g2, b);

        // All blocks
        for (Block b : blocks) drawBlock(g2, b);

        // Snap highlight
        if (snapHighlight != null) {
            g2.setColor(new Color(80, 255, 120, 160));
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                         1f, new float[]{6f, 3f}, 0f));
            g2.draw(BlockShapes.build(snapHighlight));
        }

        // Selection rings
        g2.setColor(new Color(255, 215, 50, 220));
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                     1f, new float[]{5f, 3f}, 0f));
        for (Block b : selected) g2.draw(BlockShapes.build(b));

        // Rubber-band selection rect (screen space — reset transform first)
        g2.setTransform(new AffineTransform());
        if (selecting && (Math.abs(selW) > 4 || Math.abs(selH) > 4)) {
            int rx = selW >= 0 ? selX : selX + selW;
            int ry = selH >= 0 ? selY : selY + selH;
            int rw = Math.abs(selW), rh = Math.abs(selH);
            g2.setColor(new Color(100, 180, 255, 35));
            g2.fillRect(rx, ry, rw, rh);
            g2.setColor(new Color(100, 180, 255, 200));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                         1f, new float[]{4f, 3f}, 0f));
            g2.drawRect(rx, ry, rw, rh);
        }

        // HUD: zoom % and selection count
        g2.setColor(new Color(255, 255, 255, 70));
        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.drawString(String.format("%.0f%%  |  %d sel", scale * 100, selected.size()),
                      getWidth() - 130, getHeight() - 10);
        g2.dispose();
    }

    // ── Block drawing ─────────────────────────────────────────────────────────

    private void drawBlock(Graphics2D g2, Block b) {
        Color base  = b.getColor();
        Color dark  = base.darker();
        Color light = new Color(Math.min(255, base.getRed()   + 40),
                                Math.min(255, base.getGreen() + 40),
                                Math.min(255, base.getBlue()  + 30));

        // Ask BlockShapes for the path
        java.awt.geom.GeneralPath shape = BlockShapes.build(b);

        // Shadow (translate shape by 2,2)
        java.awt.geom.GeneralPath shadow =
            (java.awt.geom.GeneralPath) shape.clone();
        shadow.transform(AffineTransform.getTranslateInstance(2, 2));
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fill(shadow);

        // Body fill
        g2.setColor(base);
        g2.fill(shape);

        // Top highlight stripe — clipped so it doesn't overflow the shape
        Graphics2D gc = (Graphics2D) g2.create();
        gc.clip(shape);
        gc.setColor(light);
        gc.fillRect(b.x, b.y, b.width, 8);
        gc.dispose();

        // Border
        g2.setColor(dark);
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(shape);

        // Label
        drawLabel(g2, b);

        // Children (recursive)
        for (Block child : b.children) drawBlock(g2, child);
    }

    private void drawLabel(Graphics2D g2, Block b) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        String label = b.getDisplayLabel();
        // Truncate with ellipsis if too wide
        while (label.length() > 5 && fm.stringWidth(label) > b.width - 18)
            label = label.substring(0, label.length() - 4) + "...";

        int tx = b.x + (b.width - fm.stringWidth(label)) / 2;
        int ty = b.y + (b.height + fm.getAscent() - fm.getDescent()) / 2;

        // Drop shadow
        g2.setColor(new Color(0, 0, 0, 90));
        g2.drawString(label, tx + 1, ty + 1);
        // Label
        g2.setColor(Color.WHITE);
        g2.drawString(label, tx, ty);
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 12));
        int gs = (int)(24 * scale);
        if (gs < 6) return;
        int ox = (int)(translateX % gs);
        int oy = (int)(translateY % gs);
        for (int x = ox; x < getWidth();  x += gs) g2.drawLine(x, 0, x, getHeight());
        for (int y = oy; y < getHeight(); y += gs) g2.drawLine(0, y, getWidth(), y);
    }

    private void drawConnections(Graphics2D g2, Block b) {
        for (Block child : b.children) {
            int cx = b.x + (int)BlockShapes.NOFF + (int)BlockShapes.NW / 2;
            g2.setColor(new Color(0, 0, 0, 45));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(cx, b.y + b.height + (int)BlockShapes.NH,
                        child.x + (int)BlockShapes.NOFF + (int)BlockShapes.NW / 2,
                        child.y);
            drawConnections(g2, child);
        }
    }
}
