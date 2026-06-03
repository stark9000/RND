package rnd.blockly;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * BlockShapeTest — renders all 5 Blockly block shapes using BlockShapes.
 *
 * Compile (from project root, after all v2/*.java are compiled):
 *   javac -d out src/v2/*.java
 *   java -cp out v2.BlockShapeTest
 *
 * Uses BlockShapes directly — no duplication of geometry.
 * Each shape is drawn with a label and annotation.
 */
public class BlockShapeTest extends JPanel {

    // Test block dimensions
    private static final int BW  = 160;  // block width
    private static final int BH  = 44;   // block height
    private static final int IH  = 48;   // C-block inner height
    private static final int GAP = 36;   // gap between shapes

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("BlockShapes Test — All Shapes");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.add(new JScrollPane(new BlockShapeTest()));
            f.setSize(1300, 380);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    public BlockShapeTest() {
        setBackground(Color.decode("#1e1e2e"));
        setPreferredSize(new Dimension(1280, 350));
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,    RenderingHints.VALUE_STROKE_PURE);

        double x = 30, y = 60;

        // ── 1. Statement (prev + next) ────────────────────────────────────────
        draw(g2, makeStatement(x, y, true, true),
             Color.decode("#5C81A6"), "1. Statement", "prev + next notch", x, y, BH);
        x += BW + GAP;

        // ── 2. Terminal (prev, no next) ───────────────────────────────────────
        draw(g2, makeStatement(x, y, true, false),
             Color.decode("#607D8B"), "2. Terminal", "prev only (break/return)", x, y, BH);
        x += BW + GAP;

        // ── 3. C-Block (Setup/Loop) ───────────────────────────────────────────
        int cTotalH = BH + IH + (int)(BlockShapes.IC * 2);
        draw(g2, makeCBlock(x, y, IH),
             Color.decode("#3D4580"), "3. C-Block", "Setup / Loop", x, y, cTotalH);
        x += BW + GAP + 20;

        // ── 4. Expression (puzzle tab output, left) ───────────────────────────
        draw(g2, makeExpression(x, y),
             Color.decode("#9C27B0"), "4. Expression", "puzzle tab LEFT", x, y, BH);
        x += BW + GAP;

        // ── 5. Value input (puzzle tab socket, right) ─────────────────────────
        draw(g2, makeValueInput(x, y),
             Color.decode("#4CAF50"), "5. Value input", "socket RIGHT", x, y, BH);
        x += BW + GAP;

        // ── 6. Inline input hole ─────────────────────────────────────────────
        GeneralPath inlineBody = makeStatement(x, y, true, true);
        draw(g2, inlineBody,
             Color.decode("#795548"), "6. Inline hole", "hole + tab socket", x, y, BH);
        // Overlay the hole on top
        GeneralPath hole = BlockShapes.buildInlineHole(
            x + BW - 65, y + (BH - 28) / 2.0, 55, 28);
        g2.setColor(Color.decode("#11111b"));
        g2.fill(hole);
        g2.setColor(Color.decode("#3a3a5a"));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(hole);

        g2.dispose();
    }

    // ── Shape builders (use BlockShapes primitives directly) ─────────────────

    /** Statement / Terminal — reuses BlockShapes notch primitives */
    private GeneralPath makeStatement(double x, double y,
                                       boolean hasPrev, boolean hasNext) {
        double nx = x + BlockShapes.NOFF;
        double bx = nx + BlockShapes.NW;
        double w = BW, h = BH, r = BlockShapes.R;

        GeneralPath p = new GeneralPath();
        p.moveTo(x, y + r);
        p.quadTo(x, y, x + r, y);

        if (hasPrev) {
            p.lineTo(nx, y);
            BlockShapes.notchL(p, nx, y);
        }
        p.lineTo(x + w - r, y);
        p.quadTo(x + w, y, x + w, y + r);
        p.lineTo(x + w, y + h - r);
        p.quadTo(x + w, y + h, x + w - r, y + h);

        if (hasNext) {
            p.lineTo(bx, y + h);
            BlockShapes.notchR(p, bx, y + h);
        }
        p.lineTo(x + r, y + h);
        p.quadTo(x, y + h, x, y + h - r);
        p.lineTo(x, y + r);
        p.closePath();
        return p;
    }

    /** C-Block — uses BlockShapes primitives for notch and inside corners */
    private GeneralPath makeCBlock(double x, double y, double innerH) {
        double w = BW, h = BH, r = BlockShapes.R;
        double IC      = BlockShapes.IC;
        double NOFF    = BlockShapes.NOFF;
        double NW      = BlockShapes.NW;
        double INDENT  = BlockShapes.INDENT;
        double totalH  = h + innerH + IC * 2;
        double indentX = x + INDENT;
        double notchX  = indentX + NOFF + NW;
        double innerTop    = y + h;
        double innerBottom = innerTop + innerH;
        double bx = x + NOFF + NW;

        GeneralPath p = new GeneralPath();
        p.moveTo(x, y + r);
        p.quadTo(x, y, x + r, y);
        p.lineTo(x + w - r, y);
        p.quadTo(x + w, y, x + w, y + r);
        p.lineTo(x + w, innerTop);

        // C-shape
        p.lineTo(notchX, innerTop);
        BlockShapes.notchR(p, notchX, innerTop);
        p.lineTo(indentX, innerTop);
        p.quadTo(indentX, innerTop, indentX - IC, innerTop + IC);   // inside corner top
        p.lineTo(indentX - IC, innerBottom);
        p.quadTo(indentX - IC, innerBottom + IC, indentX, innerBottom + IC); // inside corner bottom
        p.lineTo(x + w, innerBottom + IC);

        p.lineTo(x + w, y + totalH - r);
        p.quadTo(x + w, y + totalH, x + w - r, y + totalH);
        p.lineTo(bx, y + totalH);
        BlockShapes.notchR(p, bx, y + totalH);
        p.lineTo(x + r, y + totalH);
        p.quadTo(x, y + totalH, x, y + totalH - r);
        p.lineTo(x, y + r);
        p.closePath();
        return p;
    }

    /**
     * Expression block — delegates fully to BlockShapes.
     * Creates a dummy Block with the right flags.
     */
    private GeneralPath makeExpression(double x, double y) {
        // Expression: !hasTopConnector && !canHaveChildren
        // Use a proxy to pass coordinates
        return buildExpressionShape(x, y, BW, BH);
    }

    private GeneralPath buildExpressionShape(double x, double y, double w, double h) {
        double tabBot = y + BlockShapes.TOFF + BlockShapes.TH;
        GeneralPath p = new GeneralPath();
        p.moveTo(x, y + BlockShapes.R);
        p.quadTo(x, y, x + BlockShapes.R, y);
        p.lineTo(x + w - BlockShapes.R, y);
        p.quadTo(x + w, y, x + w, y + BlockShapes.R);
        p.lineTo(x + w, y + h - BlockShapes.R);
        p.quadTo(x + w, y + h, x + w - BlockShapes.R, y + h);
        p.lineTo(x + BlockShapes.R, y + h);
        p.quadTo(x, y + h, x, y + h - BlockShapes.R);
        p.lineTo(x, tabBot);
        BlockShapes.tabUp(p, x, tabBot);
        p.lineTo(x, y + BlockShapes.R);
        p.closePath();
        return p;
    }

    /**
     * Value input block — statement block with puzzle tab socket on right.
     * Uses BlockShapes.tabDown() for the socket.
     */
    private GeneralPath makeValueInput(double x, double y) {
        double w = BW, h = BH;
        double nx = x + BlockShapes.NOFF;
        double bx = nx + BlockShapes.NW;
        double tabTop = y + BlockShapes.TOFF;
        double r = BlockShapes.R;

        GeneralPath p = new GeneralPath();
        p.moveTo(x, y + r);
        p.quadTo(x, y, x + r, y);
        p.lineTo(nx, y);
        BlockShapes.notchL(p, nx, y);            // prev notch
        p.lineTo(x + w - r, y);
        p.quadTo(x + w, y, x + w, y + r);
        p.lineTo(x + w, tabTop);
        BlockShapes.tabDown(p, x + w, tabTop);   // value input socket
        p.lineTo(x + w, y + h - r);
        p.quadTo(x + w, y + h, x + w - r, y + h);
        p.lineTo(bx, y + h);
        BlockShapes.notchR(p, bx, y + h);        // next notch
        p.lineTo(x + r, y + h);
        p.quadTo(x, y + h, x, y + h - r);
        p.lineTo(x, y + r);
        p.closePath();
        return p;
    }

    // ── Drawing helper ────────────────────────────────────────────────────────

    private void draw(Graphics2D g2, GeneralPath shape, Color base,
                       String title, String subtitle, double bx, double by, int h) {
        Color dark  = base.darker();
        Color light = new Color(Math.min(255, base.getRed()   + 40),
                                Math.min(255, base.getGreen() + 40),
                                Math.min(255, base.getBlue()  + 30));

        // Shadow
        GeneralPath shadow = (GeneralPath) shape.clone();
        shadow.transform(AffineTransform.getTranslateInstance(2, 2));
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fill(shadow);

        // Body
        g2.setColor(base);
        g2.fill(shape);

        // Highlight stripe
        Graphics2D gc = (Graphics2D) g2.create();
        gc.clip(shape);
        gc.setColor(light);
        gc.fillRect((int)bx - 5, (int)by, BW + 20, 8);
        gc.dispose();

        // Border
        g2.setColor(dark);
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(shape);

        // Block label (centred in top row)
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        int tx = (int)(bx + (BW - fm.stringWidth(title)) / 2);
        int ty = (int)(by + (BH + fm.getAscent() - fm.getDescent()) / 2);
        g2.setColor(new Color(0, 0, 0, 80));
        g2.drawString(title, tx+1, ty+1);
        g2.setColor(Color.WHITE);
        g2.drawString(title, tx, ty);

        // Subtitle (below block)
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        fm = g2.getFontMetrics();
        int sx = (int)(bx + (BW - fm.stringWidth(subtitle)) / 2);
        g2.setColor(new Color(160, 160, 190));
        g2.drawString(subtitle, sx, (int)(by + h + 18));
    }
}
