package rnd.blockly;

import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class BlockLoader {

    public static Map<String, List<BlockDefinition>> loadAll(String blocksDir) {
        Map<String, List<BlockDefinition>> result = new LinkedHashMap<>();
        File dir = new File(blocksDir);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("[BlockLoader] blocks directory not found: " + blocksDir);
            return result;
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) {
            return result;
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File f : files) {
            try {
                String json = readFile(f);
                parseFile(json, result);
                System.out.println("[BlockLoader] loaded: " + f.getName());
            } catch (Exception e) {
                System.err.println("[BlockLoader] ERROR parsing " + f.getName() + ": " + e.getMessage());
            }
        }
        return result;
    }

    private static String readFile(File f) throws IOException {
        byte[] bytes = Files.readAllBytes(f.toPath());
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void parseFile(String json, Map<String, List<BlockDefinition>> result) {
        String category = extractString(json, "category");
        String colorHex = extractString(json, "color");
        Color color = parseColor(colorHex);

        String blocksArr = extractArray(json, "blocks");
        List<String> blockObjs = splitObjects(blocksArr);

        List<BlockDefinition> defs = result.get(category);
        if (defs == null) {
            defs = new ArrayList<>();
            result.put(category, defs);
        }

        for (String obj : blockObjs) {
            try {
                defs.add(parseBlock(obj, category, color));
            } catch (Exception e) {
                System.err.println("[BlockLoader] Skipping block: " + e.getMessage());
            }
        }
    }

    private static BlockDefinition parseBlock(String obj, String category, Color defaultColor) {
        String type  = extractString(obj, "type");
        String label = extractString(obj, "label");

        // ── SHAPE → hasTopConnector + canHaveChildren ─────────────────────────
        // Read explicit "shape" field first; fall back to individual booleans.
        //   "hat"        → hasTopConnector=false, canHaveChildren=true   (Setup/Loop C-block)
        //   "statement"  → hasTopConnector=true,  canHaveChildren=true   (most blocks)
        //   "terminal"   → hasTopConnector=true,  canHaveChildren=false  (break, return)
        //   "expression" → hasTopConnector=false, canHaveChildren=false  (value blocks)
        String shape = extractString(obj, "shape");
        boolean canHaveChildren;
        boolean hasTopConnector;

        if (shape != null && !shape.isEmpty()) {
            // Derive flags from shape name
            switch (shape) {
                case "hat":
                    hasTopConnector = false; canHaveChildren = true;  break;
                case "terminal":
                    hasTopConnector = true;  canHaveChildren = false; break;
                case "expression":
                    hasTopConnector = false; canHaveChildren = false; break;
                default: // "statement" or anything else
                    hasTopConnector = true;  canHaveChildren = true;  break;
            }
            // Allow JSON to still override individually
            if (obj.contains("\"canHaveChildren\""))
                canHaveChildren = extractBoolean(obj, "canHaveChildren", canHaveChildren);
            if (obj.contains("\"hasTopConnector\""))
                hasTopConnector = extractBoolean(obj, "hasTopConnector", hasTopConnector);
        } else {
            // No shape field — use individual booleans with sensible defaults
            // Default: statement (hasTop=true, canHaveChildren=true)
            canHaveChildren = extractBoolean(obj, "canHaveChildren", true);
            hasTopConnector = extractBoolean(obj, "hasTopConnector", true);
        }

        String colorHex = extractString(obj, "color");
        Color color = (colorHex != null && !colorHex.isEmpty()) ? parseColor(colorHex) : defaultColor;

        String code = unescape(extractString(obj, "code"));
        String paramsArr = extractArray(obj, "params");
        List<BlockDefinition.Param> params = parseParams(paramsArr);

        return new BlockDefinition(type, label, category, color,
                canHaveChildren, hasTopConnector, params, code);
    }

    /**
     * Convert JSON escape sequences in a string value to real characters. e.g.
     * "void setup() {\n{children}}" → actual newline inside the string.
     */
    private static String unescape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case 'n':
                        sb.append('\n');
                        i++;
                        break;
                    case 't':
                        sb.append('\t');
                        i++;
                        break;
                    case 'r':
                        sb.append('\r');
                        i++;
                        break;
                    case '\\':
                        sb.append('\\');
                        i++;
                        break;
                    case '"':
                        sb.append('"');
                        i++;
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static List<BlockDefinition.Param> parseParams(String paramsArr) {
        List<BlockDefinition.Param> list = new ArrayList<>();
        if (paramsArr == null || paramsArr.trim().isEmpty()) {
            return list;
        }
        for (String obj : splitObjects(paramsArr)) {
            String name = extractString(obj, "name");
            String type = extractString(obj, "type");
            String def = extractString(obj, "default");
            String lbl = extractString(obj, "label");
            if (lbl == null || lbl.isEmpty()) {
                lbl = name;
            }

            String optArr = extractArray(obj, "options");
            List<String> options = new ArrayList<>();
            if (optArr != null && !optArr.trim().isEmpty()) {
                for (String s : optArr.split(",")) {
                    String v = s.trim().replaceAll("^\"|\"$", "");
                    if (!v.isEmpty()) {
                        options.add(v);
                    }
                }
            }
            list.add(new BlockDefinition.Param(name, type, def, lbl, options));
        }
        return list;
    }

    // ── JSON helpers ──────────────────────────────────────────────────────────
    static String extractString(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) {
            return "";
        }
        int colon = json.indexOf(':', idx + pattern.length());
        if (colon < 0) {
            return "";
        }
        int start = json.indexOf('"', colon + 1);
        if (start < 0) {
            return "";
        }
        int end = start + 1;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '"' && json.charAt(end - 1) != '\\') {
                break;
            }
            end++;
        }
        // Return raw — unescape() is called explicitly only on code templates
        return json.substring(start + 1, end);
    }

    static boolean extractBoolean(String json, String key, boolean def) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) {
            return def;
        }
        int colon = json.indexOf(':', idx + pattern.length());
        if (colon < 0) {
            return def;
        }
        String rest = json.substring(colon + 1).trim();
        if (rest.startsWith("true")) {
            return true;
        }
        if (rest.startsWith("false")) {
            return false;
        }
        return def;
    }

    static String extractArray(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) {
            return "";
        }
        int bracket = json.indexOf('[', idx + pattern.length());
        if (bracket < 0) {
            return "";
        }
        int depth = 0, end = bracket;
        for (int i = bracket; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    end = i;
                    break;
                }
            }
        }
        return json.substring(bracket + 1, end);
    }

    static List<String> splitObjects(String arrayBody) {
        List<String> result = new ArrayList<>();
        if (arrayBody == null || arrayBody.trim().isEmpty()) {
            return result;
        }
        int depth = 0, start = -1;
        for (int i = 0; i < arrayBody.length(); i++) {
            char c = arrayBody.charAt(i);
            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    result.add(arrayBody.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return result;
    }

    private static Color parseColor(String hex) {
        try {
            return Color.decode(hex);
        } catch (Exception e) {
            return Color.GRAY;
        }
    }
}
