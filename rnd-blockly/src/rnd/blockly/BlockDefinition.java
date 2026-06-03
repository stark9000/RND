package rnd.blockly;


import java.awt.Color;
import java.util.List;

/**
 * Immutable description of a block type, loaded from a JSON file. The canvas
 * uses this to draw, render params, and generate code.
 */
public class BlockDefinition {

    public static class Param {

        public final String name;
        public final String type;      // "number","text","expression","dropdown"
        public final String defaultVal;
        public final String label;
        public final List<String> options; // for dropdown type

        public Param(String name, String type, String defaultVal,
                String label, List<String> options) {
            this.name = name;
            this.type = type;
            this.defaultVal = defaultVal;
            this.label = label;
            this.options = options;
        }
    }

    public final String type;
    public final String label;
    public final String category;
    public final Color color;
    public final boolean canHaveChildren;
    public final boolean hasTopConnector;
    public final List<Param> params;
    public final String codeTemplate;  // e.g. "delay({ms});"

    public BlockDefinition(String type, String label, String category,
            Color color, boolean canHaveChildren,
            boolean hasTopConnector,
            List<Param> params, String codeTemplate) {
        this.type = type;
        this.label = label;
        this.category = category;
        this.color = color;
        this.canHaveChildren = canHaveChildren;
        this.hasTopConnector = hasTopConnector;
        this.params = params;
        this.codeTemplate = codeTemplate;
    }

    /**
     * Fill in the code template with the given param values. e.g.
     * "delay({ms});" with {"ms":"500"} → "delay(500);"
     */
    public String generateCode(java.util.Map<String, String> values, String childrenCode) {
        String code = codeTemplate;
        for (java.util.Map.Entry<String, String> e : values.entrySet()) {
            code = code.replace("{" + e.getKey() + "}", e.getValue());
        }
        if (childrenCode != null) {
            code = code.replace("{children}", childrenCode);
        }
        return code;
    }

    @Override
    public String toString() {
        return "BlockDefinition[" + type + "]";
    }
}
