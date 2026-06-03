package rnd.blockly;

import java.awt.geom.GeneralPath;

/**
 * BlockShapes — all Blockly geras block geometry in one place.
 *
 * All values taken directly from Blockly v12.5.1 source:
 *   core/renderers/common/constants.ts  — numeric constants
 *   core/renderers/common/drawer.ts     — assembly order
 *
 * PUBLIC API (used by BlockCanvas and BlockShapeTest):
 *   BlockShapes.build(block)            → GeneralPath for any block
 *   BlockShapes.isCBlock(block)         → shape classifier
 *   BlockShapes.isStatement(block)
 *   BlockShapes.isExpression(block)
 *   BlockShapes.isTerminal(block)
 *   BlockShapes.innerHeight(block)      → C-block body slot height
 *   BlockShapes.totalHeight(block)      → total rendered height
 *
 * PRIMITIVES (package-private, used by BlockShapeTest):
 *   BlockShapes.notchL(p, nx, y)        → top notch L→R
 *   BlockShapes.notchR(p, bx, y)        → bottom tab R→L
 *   BlockShapes.tabUp(p, px, py)        → puzzle tab going UP (expression output)
 *   BlockShapes.tabDown(p, px, py)      → puzzle tab going DOWN (value input socket)
 */
public class BlockShapes {

    // ═══════════════════════════════════════════════════════════════════════
    //  EXACT BLOCKLY GERAS CONSTANTS
    //  Source: core/renderers/common/constants.ts
    // ═══════════════════════════════════════════════════════════════════════

    /** CORNER_RADIUS — all four outer corners */
    public static final double R      = 8;

    /** NOTCH_WIDTH — total width of prev/next connector trapezoid */
    public static final double NW     = 15;

    /** NOTCH_HEIGHT — how far the trapezoid dips downward */
    public static final double NH     = 4;

    /** Notch outer segment width = (NOTCH_WIDTH - innerWidth) / 2 = (15-3)/2 */
    public static final double NO     = 6;

    /** Notch inner (flat bottom) segment width */
    public static final double NI     = 3;

    /** NOTCH_OFFSET_LEFT — distance from block left edge to notch start */
    public static final double NOFF   = 15;

    /** TAB_WIDTH — puzzle tab protrusion depth (8px left or right) */
    public static final double TW     = 8;

    /** TAB_HEIGHT — total height of puzzle tab (15px) */
    public static final double TH     = 15;

    /** TAB_OFFSET_FROM_TOP — where tab starts from top of block row */
    public static final double TOFF   = 5;

    /** INSIDE_CORNERS radius — concave arcs in C-block body slot */
    public static final double IC     = 8;

    /** C-block indent — x distance of inner wall from block left edge */
    public static final double INDENT = 16;

    /** Minimum C-block inner body height (when no children) */
    public static final double MIN_INNER_H = 24;

    // ═══════════════════════════════════════════════════════════════════════
    //  SHAPE CLASSIFIERS
    //  Determined by two boolean flags on BlockDefinition:
    //    hasTopConnector — block has a prev connector (top notch)
    //    canHaveChildren — block has a next connector + body slot
    // ═══════════════════════════════════════════════════════════════════════

    /** C-block (Setup/Loop): no top notch, has body slot */
    public static boolean isCBlock    (Block b) { return !b.hasTopConnector() &&  b.canHaveChildren(); }

    /** Statement: top notch + body slot + bottom tab */
    public static boolean isStatement (Block b) { return  b.hasTopConnector() &&  b.canHaveChildren(); }

    /** Terminal: top notch, no bottom tab (return, break etc) */
    public static boolean isTerminal  (Block b) { return  b.hasTopConnector() && !b.canHaveChildren(); }

    /** Expression: no notches, puzzle tab output on left side */
    public static boolean isExpression(Block b) { return !b.hasTopConnector() && !b.canHaveChildren(); }

    // ═══════════════════════════════════════════════════════════════════════
    //  HEIGHT HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Inner body height of a C-block — sum of children heights, min 24px.
     * Used by BlockCanvas for hit-testing and snap positioning.
     */
    public static double innerHeight(Block b) {
        double h = 0;
        for (Block c : b.children) h += c.height;
        return Math.max(MIN_INNER_H, h);
    }

    /**
     * Total rendered height of any block.
     * For C-blocks this is larger than block.height.
     * For all others it equals block.height.
     */
    public static double totalHeight(Block b) {
        if (isCBlock(b)) return b.height + innerHeight(b) + IC * 2;
        return b.height;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  MAIN ENTRY POINT
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Build the complete GeneralPath for any block.
     * Routes to the correct shape builder based on block flags.
     */
    public static GeneralPath build(Block b) {
        if (isCBlock(b))     return buildCBlock(b);
        if (isExpression(b)) return buildExpression(b);
        return buildStatement(b);  // statement + terminal
    }

    /**
     * Build a value-input socket shape for a given block.
     * Used when a block has an inline value input slot on its right side.
     * Call this in addition to build() to get the socket cutout path.
     *
     * @param px  right edge x of the block (where socket sits)
     * @param py  y position of the row containing the socket
     */
    public static GeneralPath buildValueInputSocket(double px, double py) {
        GeneralPath p = new GeneralPath();
        p.moveTo(px, py);
        tabDown(p, px, py);   // c 0,10  -8,-8  -8,7.5  s 8,-2.5  8,7.5
        return p;
    }

    /**
     * Build an inline input hole path — a rectangular cutout with a
     * puzzle tab socket on its right side.
     * From drawInlineInput_(): drawn as a separate inlinePath_.
     *
     * @param holeX   left x of the hole
     * @param holeY   top y of the hole
     * @param holeW   width of the hole
     * @param holeH   height of the hole (should be >= TH + TOFF*2)
     */
    public static GeneralPath buildInlineHole(double holeX, double holeY,
                                               double holeW, double holeH) {
        double cr = holeX + holeW;  // right edge of hole (where socket is)

        GeneralPath p = new GeneralPath();
        // moveTo connectionRight, yPos
        p.moveTo(cr, holeY);
        // v connectionTop (= TOFF)
        p.lineTo(cr, holeY + TOFF);
        // pathDown — socket cutout
        tabDown(p, cr, holeY + TOFF);
        // v remaining height
        p.lineTo(cr, holeY + holeH);
        // h -width
        p.lineTo(holeX, holeY + holeH);
        // v -height (back up left side)
        p.lineTo(holeX, holeY);
        p.closePath();
        return p;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SHAPE BUILDERS
    //  Assembly order matches Blockly drawer.ts draw*_() methods.
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * STATEMENT / TERMINAL block.
     *
     * Assembly from drawer.ts:
     *   drawTop_():    topLeft corner → [notchL if prev] → topRight corner → v
     *   drawBottom_(): V → [notchR if next] → bottomLeft corner
     *   drawLeft_():   close
     *
     * Notch pathLeft  (L→R): l 6,4   3,0   6,-4   (dips DOWN by NH=4px)
     * Notch pathRight (R→L): l -6,4  -3,0  -6,-4  (dips DOWN by NH=4px)
     */
    private static GeneralPath buildStatement(Block b) {
        double x = b.x, y = b.y, w = b.width, h = b.height;
        double nx = x + NOFF;        // top notch left edge
        double bx = x + NOFF + NW;  // bottom notch right edge

        GeneralPath p = new GeneralPath();

        // drawTop_
        p.moveTo(x, y + R);
        p.quadTo(x, y, x + R, y);                      // topLeft corner arc

        if (b.hasTopConnector()) {
            p.lineTo(nx, y);
            notchL(p, nx, y);                          // prev notch: l 6,4 3,0 6,-4
        }

        p.lineTo(x + w - R, y);
        p.quadTo(x + w, y, x + w, y + R);              // topRight corner arc

        // drawRightSideRow_
        p.lineTo(x + w, y + h - R);
        p.quadTo(x + w, y + h, x + w - R, y + h);     // bottomRight corner arc

        // drawBottom_
        if (b.canHaveChildren()) {
            p.lineTo(bx, y + h);
            notchR(p, bx, y + h);                      // next tab: l -6,4 -3,0 -6,-4
        }

        p.lineTo(x + R, y + h);
        p.quadTo(x, y + h, x, y + h - R);             // bottomLeft corner arc

        // drawLeft_
        p.lineTo(x, y + R);
        p.closePath();
        return p;
    }

    /**
     * C-BLOCK (Setup / Loop).
     *
     * A block with no top connector and a built-in statement input slot.
     * Looks like a C-bracket or square bracket [ shape.
     *
     * Assembly from drawer.ts drawStatementInput_():
     *   Top row (no prev notch)
     *   → right side down to inner row
     *   → H notchX
     *   → notch.pathRight           (l -6,4  -3,0  -6,-4)
     *   → h -(NOFF - IC)            (line left to inside corner start)
     *   → INSIDE_CORNERS.pathTop    (a 8,8 0 0,0  -8,8 → left+down)
     *   → v innerHeight             (inner left wall going down)
     *   → INSIDE_CORNERS.pathBottom (a 8,8 0 0,0   8,8 → right+down)
     *   → H right edge              (back to right side)
     *   Bottom row + next notch + corners
     *
     * Inside corner arcs from makeInsideCorners():
     *   pathTop:    a 8,8 0 0,0  -8,8   (sweep=0, concave, goes left then down)
     *   pathBottom: a 8,8 0 0,0   8,8   (sweep=0, concave, goes right then down)
     */
    private static GeneralPath buildCBlock(Block b) {
        double x = b.x, y = b.y, w = b.width;
        double innerH  = innerHeight(b);
        double totalH  = b.height + innerH + IC * 2;

        double innerTop    = y + b.height;
        double innerBottom = innerTop + innerH;

        // notchX: where the C-opening notch sits on the right wall
        double notchX = x + NOFF + NW;  // same offset as top/bottom notches
        // icX: left edge of inside corners
        double icX    = notchX - NW - NOFF;
        // bottom notch right edge
        double bx     = x + NOFF + NW;

        GeneralPath p = new GeneralPath();

        // ── TOP ROW (no prev notch for C-blocks) ─────────────────────────
        p.moveTo(x, y + R);
        p.quadTo(x, y, x + R, y);                       // topLeft corner
        p.lineTo(x + w - R, y);
        p.quadTo(x + w, y, x + w, y + R);               // topRight corner

        // ── RIGHT SIDE down to statement input ────────────────────────────
        p.lineTo(x + w, innerTop);

        // ── STATEMENT INPUT: H notchX → notchR → h left → IC top → v → IC bottom → H right
        p.lineTo(notchX, innerTop);
        notchR(p, notchX, innerTop);                     // l -6,4  -3,0  -6,-4

        // h -(notchOffset - IC.width) = h -(NOFF - IC) = h -7
        p.lineTo(notchX - NW - (NOFF - IC), innerTop);

        // INSIDE_CORNERS.pathTop: a 8,8 0 0,0  -8,8 → LEFT then DOWN
        p.quadTo(icX, innerTop, icX, innerTop + IC);

        // v innerHeight
        p.lineTo(icX, innerBottom);

        // INSIDE_CORNERS.pathBottom: a 8,8 0 0,0  8,8 → RIGHT then DOWN
        p.quadTo(icX, innerBottom + IC, icX + IC, innerBottom + IC);

        // H back to right edge
        p.lineTo(x + w, innerBottom + IC);

        // ── BOTTOM ROW ────────────────────────────────────────────────────
        p.lineTo(x + w, y + totalH - R);
        p.quadTo(x + w, y + totalH, x + w - R, y + totalH);  // bottomRight corner

//        p.lineTo(bx, y + totalH);
//        notchR(p, bx, y + totalH);                       // next notch

        p.lineTo(x + R, y + totalH);
        p.quadTo(x, y + totalH, x, y + totalH - R);     // bottomLeft corner

        // ── LEFT EDGE back up ─────────────────────────────────────────────
        p.lineTo(x, y + R);
        p.closePath();
        return p;
    }

    /**
     * EXPRESSION block — output connector (puzzle tab on LEFT side).
     *
     * Assembly from drawer.ts drawLeft_():
     *   V tabBottom → pathUp → close
     *
     * pathUp: c 0,-10  -8,8  -8,-7.5   s 8,2.5  8,-7.5
     * Tab protrudes TW=8px LEFT. Starts TOFF=5px from top. Height=TH=15px.
     */
    private static GeneralPath buildExpression(Block b) {
        double x = b.x, y = b.y, w = b.width, h = b.height;
        double tabBot = y + TOFF + TH;

        GeneralPath p = new GeneralPath();
        p.moveTo(x, y + R);
        p.quadTo(x, y, x + R, y);                     // topLeft corner
        p.lineTo(x + w - R, y);
        p.quadTo(x + w, y, x + w, y + R);             // topRight corner
        p.lineTo(x + w, y + h - R);
        p.quadTo(x + w, y + h, x + w - R, y + h);    // bottomRight corner
        p.lineTo(x + R, y + h);
        p.quadTo(x, y + h, x, y + h - R);            // bottomLeft corner
        p.lineTo(x, tabBot);
        tabUp(p, x, tabBot);                           // puzzle tab output
        p.lineTo(x, y + R);
        p.closePath();
        return p;
    }

    /**
     * VALUE INPUT block — has a puzzle tab socket on the RIGHT side.
     * Used for blocks that accept an expression input (if/while condition etc).
     *
     * Assembly from drawer.ts drawValueInput_():
     *   Right side: V tabTop → pathDown → V remainder
     *
     * pathDown: c 0,10  -8,-8  -8,7.5   s 8,-2.5  8,7.5
     * Socket recess goes TW=8px LEFT of right edge.
     * Starts TOFF=5px from top. Height=TH=15px.
     *
     * This block still has prev/next notches (it IS a statement,
     * but with an extra value input socket on the right side).
     */
    public static GeneralPath buildValueInput(Block b) {
        double x = b.x, y = b.y, w = b.width, h = b.height;
        double nx     = x + NOFF;
        double bx     = nx + NW;
        double tabTop = y + TOFF;

        GeneralPath p = new GeneralPath();
        p.moveTo(x, y + R);
        p.quadTo(x, y, x + R, y);                     // topLeft corner

        if (b.hasTopConnector()) {
            p.lineTo(nx, y);
            notchL(p, nx, y);                         // prev notch
        }
        p.lineTo(x + w - R, y);
        p.quadTo(x + w, y, x + w, y + R);             // topRight corner

        // Right side: V tabTop → socket → V remainder
        p.lineTo(x + w, tabTop);
        tabDown(p, x + w, tabTop);                    // value input socket
        p.lineTo(x + w, y + h - R);
        p.quadTo(x + w, y + h, x + w - R, y + h);    // bottomRight corner

        if (b.canHaveChildren()) {
            p.lineTo(bx, y + h);
            notchR(p, bx, y + h);                     // next notch
        }
        p.lineTo(x + R, y + h);
        p.quadTo(x, y + h, x, y + h - R);            // bottomLeft corner
        p.lineTo(x, y + R);
        p.closePath();
        return p;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PRIMITIVES — exact SVG path translations from Blockly source
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Notch pathLeft — top connector, path travels LEFT→RIGHT.
     * From makeNotch(): l 6,4  3,0  6,-4
     * Trapezoid that dips DOWN by NH=4px.
     * Starts at (nx, y), ends at (nx+NW, y).
     */
    static void notchL(GeneralPath p, double nx, double y) {
        p.lineTo(nx + NO,       y + NH);  // l 6,4   — diagonal down-right
        p.lineTo(nx + NO + NI,  y + NH);  // l 3,0   — flat bottom
        p.lineTo(nx + NW,       y);       // l 6,-4  — diagonal up-right
    }

    /**
     * Notch pathRight — bottom connector, path travels RIGHT→LEFT.
     * From makeNotch(): l -6,4  -3,0  -6,-4
     * Trapezoid that dips DOWN by NH=4px.
     * Starts at (bx, y), ends at (bx-NW, y).
     */
    static void notchR(GeneralPath p, double bx, double y) {
        p.lineTo(bx - NO,       y + NH);  // l -6,4  — diagonal down-left
        p.lineTo(bx - NO - NI,  y + NH);  // l -3,0  — flat bottom
        p.lineTo(bx - NW,       y);       // l -6,-4 — diagonal up-left
    }

    /**
     * Puzzle tab pathUp — output connector on LEFT side, going UPWARD.
     * From makePuzzleTab(): c 0,-10  -8,8  -8,-7.5   s 8,2.5  8,-7.5
     *
     * Travels from (px, py) to (px, py-15). Protrudes TW=8px to the LEFT.
     *
     * Seg1 (cubic):  cp1=(px, py-10)  cp2=(px-8, py+8)  end=(px-8, py-7.5)
     * Seg2 (smooth cubic — reflected cp1):
     *   reflected = 2*end - cp2 = (px-8, py-23)
     *   explicit cp2 = (px, py-5)
     *   end = (px, py-15)
     */
    static void tabUp(GeneralPath p, double px, double py) {
        p.curveTo(px,     py-10,   px-TW,  py+8,    px-TW,  py-7.5);
        p.curveTo(px-TW,  py-23,   px,     py-5,    px,     py-15);
    }

    /**
     * Puzzle tab pathDown — value input socket on RIGHT side, going DOWNWARD.
     * From makePuzzleTab(): c 0,10  -8,-8  -8,7.5   s 8,-2.5  8,7.5
     *
     * Travels from (px, py) to (px, py+15). Socket recess goes TW=8px LEFT.
     *
     * Seg1 (cubic):  cp1=(px, py+10)  cp2=(px-8, py-8)  end=(px-8, py+7.5)
     * Seg2 (smooth cubic — reflected cp1):
     *   reflected = 2*end - cp2 = (px-8, py+23)
     *   explicit cp2 = (px, py+5)
     *   end = (px, py+15)
     */
    static void tabDown(GeneralPath p, double px, double py) {
        p.curveTo(px,     py+10,   px-TW,  py-8,    px-TW,  py+7.5);
        p.curveTo(px-TW,  py+23,   px,     py+5,    px,     py+15);
    }
}
