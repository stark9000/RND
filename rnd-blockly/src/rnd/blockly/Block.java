package rnd.blockly;

import java.awt.Point;
import java.util.*;

/**
 * A live block instance on the canvas. Instead of a hardcoded enum, it
 * references a BlockDefinition loaded from JSON.
 */
public class Block {

    public BlockDefinition definition;
    public int x, y;
    public int width = 170;
    public int height = 46;

    /**
     * Current param values, keyed by param name
     */
    public Map<String, String> paramValues = new LinkedHashMap<>();

    public List<Block> children = new ArrayList<>();
    public Block parent = null;

    public static final int SLOT_X_OFFSET = 20;

    public Block(BlockDefinition def, int x, int y) {
        this.definition = def;
        this.x = x;
        this.y = y;
        for (BlockDefinition.Param p : def.params) {
            paramValues.put(p.name, p.defaultVal != null ? p.defaultVal : "");
        }
    }

    public boolean contains(int px, int py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    public Point getSlotPosition() {
        return new Point(x + SLOT_X_OFFSET, y + height);
    }

    public void repositionChildren() {
        int childY = y + height + 4;
        for (Block child : children) {
            child.x = x + SLOT_X_OFFSET;
            child.y = childY;
            childY += child.height + 4;
            child.repositionChildren();
        }
    }

    public String getDisplayLabel() {
        String label = definition.label;
        for (Map.Entry<String, String> e : paramValues.entrySet()) {
            label = label.replace("{" + e.getKey() + "}", e.getValue());
        }
        return label;
    }

    public String generateCode(String indent) {
        StringBuilder childCode = new StringBuilder();
        for (Block child : children) {
            childCode.append(child.generateCode(indent + "  "));
        }
        String code = definition.generateCode(paramValues, childCode.toString());
        StringBuilder out = new StringBuilder();
        for (String line : code.split("\n", -1)) {
            if (!line.trim().isEmpty()) {
                out.append(indent).append(line).append("\n");
            } else {
                out.append("\n");
            }
        }
        return out.toString();
    }

    public java.awt.Color getColor() {
        return definition.color;
    }

    public boolean canHaveChildren() {
        return definition.canHaveChildren;
    }

    public boolean hasTopConnector() {
        return definition.hasTopConnector;
    }
}
