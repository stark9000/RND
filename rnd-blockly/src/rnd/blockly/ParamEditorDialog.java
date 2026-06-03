package rnd.blockly;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Modal dialog that appears when the user double-clicks a block. Shows one
 * editor row per param: - "dropdown" → JComboBox - "number" → JTextField with
 * numeric DocumentFilter - "text" → JTextField with identifier DocumentFilter
 * (no spaces/special chars) - "expression"→ JTextField, free input
 *
 * On OK the block's paramValues map is updated and the canvas repaints.
 */
public class ParamEditorDialog extends JDialog {

    private boolean confirmed = false;

    // One editor widget per param (parallel list to block.definition.params)
    private final List<JComponent> editors = new ArrayList<>();

    public ParamEditorDialog(Frame owner, Block block) {
        super(owner, "Edit: " + block.definition.label, true);
        setResizable(false);

        Color bg = Color.decode("#1e1e2e");
        Color fg = Color.WHITE;
        Color accent = block.getColor();
        Color fieldBg = Color.decode("#313244");

        // ── Title bar strip ───────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(accent.darker());
        header.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel title = new JLabel(block.definition.label);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 14));

        JLabel cat = new JLabel(block.definition.category);
        cat.setForeground(new Color(255, 255, 255, 150));
        cat.setFont(new Font("SansSerif", Font.PLAIN, 11));

        header.add(title, BorderLayout.WEST);
        header.add(cat, BorderLayout.EAST);

        // ── Param rows ────────────────────────────────────────────────────
        JPanel form = new JPanel();
        form.setBackground(bg);
        form.setBorder(new EmptyBorder(14, 18, 14, 18));
        form.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 4, 5, 4);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;

        List<BlockDefinition.Param> params = block.definition.params;

        if (params.isEmpty()) {
            gc.gridx = 0;
            gc.gridy = 0;
            gc.gridwidth = 2;
            JLabel none = new JLabel("This block has no editable parameters.");
            none.setForeground(new Color(180, 180, 200));
            none.setFont(new Font("SansSerif", Font.ITALIC, 12));
            form.add(none, gc);
        }

        for (int i = 0; i < params.size(); i++) {
            BlockDefinition.Param p = params.get(i);
            String currentVal = block.paramValues.get(p.name);
            if (currentVal == null) {
                currentVal = p.defaultVal != null ? p.defaultVal : "";
            }

            // Label
            gc.gridx = 0;
            gc.gridy = i;
            gc.gridwidth = 1;
            gc.weightx = 0;
            String labelText = toDisplayLabel(p.label != null ? p.label : p.name);
            JLabel lbl = new JLabel(labelText + ":");
            lbl.setForeground(new Color(180, 180, 210));
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
            lbl.setPreferredSize(new Dimension(110, 24));
            form.add(lbl, gc);

            // Editor
            gc.gridx = 1;
            gc.weightx = 1.0;
            JComponent editor = buildEditor(p, currentVal, fieldBg, fg);
            form.add(editor, gc);
            editors.add(editor);
        }

        // ── Buttons ───────────────────────────────────────────────────────
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.setBackground(Color.decode("#181825"));
        buttons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                Color.decode("#313244")));

        JButton btnCancel = styledBtn("Cancel", Color.decode("#45475a"), Color.WHITE);
        JButton btnOk = styledBtn("  OK  ", accent, Color.WHITE);

        buttons.add(btnCancel);
        buttons.add(btnOk);

        // ── Assemble ──────────────────────────────────────────────────────
        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(340, 0));
        setLocationRelativeTo(owner);

        // ── Actions ───────────────────────────────────────────────────────
        btnOk.addActionListener(e -> {
            if (applyValues(block)) {
                confirmed = true;
                dispose();
            }
        });
        btnCancel.addActionListener(e -> dispose());

        // Enter = OK, Escape = Cancel
        getRootPane().setDefaultButton(btnOk);
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ── Editor factory ────────────────────────────────────────────────────────
    private JComponent buildEditor(BlockDefinition.Param p, String currentVal,
            Color fieldBg, Color fg) {
        switch (p.type) {

            case "dropdown": {
                String[] opts = p.options.toArray(new String[0]);
                JComboBox<String> cb = new JComboBox<>(opts);
                cb.setBackground(fieldBg);
                cb.setForeground(fg);
                cb.setFont(new Font("Monospaced", Font.PLAIN, 12));
                // Select current value
                for (int j = 0; j < opts.length; j++) {
                    if (opts[j].equals(currentVal) || opts[j].startsWith(currentVal)) {
                        cb.setSelectedIndex(j);
                        break;
                    }
                }
                return cb;
            }

            case "number": {
                JTextField tf = styledField(currentVal, fieldBg, fg);
                // Only allow: digits, minus, dot
                ((AbstractDocument) tf.getDocument()).setDocumentFilter(
                        new DocumentFilter() {
                    public void insertString(FilterBypass fb, int off,
                            String s, AttributeSet a)
                            throws BadLocationException {
                        if (s.matches("[\\d.\\-]*")) {
                            super.insertString(fb, off, s, a);
                        }
                    }

                    public void replace(FilterBypass fb, int off, int len,
                            String s, AttributeSet a)
                            throws BadLocationException {
                        if (s.matches("[\\d.\\-]*")) {
                            super.replace(fb, off, len, s, a);
                        }
                    }
                });
                addPlaceholder(tf, "e.g. 1000");
                return tf;
            }

            case "text": {
                JTextField tf = styledField(currentVal, fieldBg, fg);
                // Identifier: letters, digits, underscore — no spaces
                ((AbstractDocument) tf.getDocument()).setDocumentFilter(
                        new DocumentFilter() {
                    public void insertString(FilterBypass fb, int off,
                            String s, AttributeSet a)
                            throws BadLocationException {
                        if (s.matches("[a-zA-Z0-9_]*")) {
                            super.insertString(fb, off, s, a);
                        }
                    }

                    public void replace(FilterBypass fb, int off, int len,
                            String s, AttributeSet a)
                            throws BadLocationException {
                        if (s.matches("[a-zA-Z0-9_]*")) {
                            super.replace(fb, off, len, s, a);
                        }
                    }
                });
                addPlaceholder(tf, "identifier");
                return tf;
            }

            default: { // "expression" — free text
                JTextField tf = styledField(currentVal, fieldBg, fg);
                addPlaceholder(tf, "expression");
                return tf;
            }
        }
    }

    // ── Apply values back to block ────────────────────────────────────────────
    private boolean applyValues(Block block) {
        List<BlockDefinition.Param> params = block.definition.params;
        for (int i = 0; i < params.size(); i++) {
            BlockDefinition.Param p = params.get(i);
            JComponent ed = editors.get(i);
            String val;

            if (ed instanceof JComboBox) {
                // For dropdown, strip any " (label)" suffix that options may have
                String selected = (String) ((JComboBox<?>) ed).getSelectedItem();
                // e.g. "1 (forward)" → just store "1"
                val = selected != null ? selected.split(" ")[0] : p.defaultVal;
            } else {
                val = ((JTextField) ed).getText().trim();
            }

            // Validation
            if (p.type.equals("number") && !val.isEmpty()) {
                try {
                    Double.parseDouble(val);
                } catch (NumberFormatException ex) {
                    showError("\"" + toDisplayLabel(p.name) + "\" must be a number.");
                    return false;
                }
            }
            if (p.type.equals("text") && val.isEmpty()) {
                showError("\"" + toDisplayLabel(p.name) + "\" cannot be empty.");
                return false;
            }
            if (p.type.equals("text") && Character.isDigit(val.charAt(0))) {
                showError("\"" + toDisplayLabel(p.name) + "\" cannot start with a digit.");
                return false;
            }

            block.paramValues.put(p.name, val);
        }
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JTextField styledField(String value, Color bg, Color fg) {
        JTextField tf = new JTextField(value, 14);
        tf.setBackground(bg);
        tf.setForeground(fg);
        tf.setCaretColor(Color.WHITE);
        tf.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.decode("#585b70"), 1),
                new EmptyBorder(4, 6, 4, 6)));
        return tf;
    }

    private void addPlaceholder(JTextField tf, String hint) {
        tf.setToolTipText(hint);
    }

    private JButton styledBtn(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(6, 18, 6, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setContentAreaFilled(true);
        return b;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Invalid Input",
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * "myVar" → "My Var", "pin" → "Pin"
     */
    private String toDisplayLabel(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(name.charAt(0)));
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append(' ');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Returns true if user clicked OK and values were valid
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    // ── Static convenience ────────────────────────────────────────────────────
    /**
     * Open the dialog for a block. Returns true if user confirmed. Usage:
     * ParamEditorDialog.show(parentFrame, block);
     */
    public static boolean show(Frame parent, Block block) {
        if (block.definition.params.isEmpty()) {
            return false;
        }
        ParamEditorDialog dlg = new ParamEditorDialog(parent, block);
        dlg.setVisible(true);
        return dlg.isConfirmed();
    }
}
