package rnd.blockly;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class MainWindow extends JFrame {

    private BlockCanvas canvas = new BlockCanvas();
    private final JTextArea codeArea = new JTextArea("// Add blocks to see generated Arduino code");
    private final JLabel statusLabel = new JLabel("  Ready");
    private final CodeGenerator codeGen = new CodeGenerator();

    public MainWindow(Map<String, List<BlockDefinition>> categories) {
        super("BlocklySwing  —  Arduino Visual Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 780);
        setLocationRelativeTo(null);

        // ── Toolbar ──────────────────────────────────────────────────────
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(Color.decode("#11111b"));
        toolbar.setBorder(new EmptyBorder(6, 12, 6, 12));

        JLabel logo = new JLabel("⬡ BlocklySwing");
        logo.setForeground(Color.decode("#cba6f7"));
        logo.setFont(new Font("SansSerif", Font.BOLD, 15));
        toolbar.add(logo);
        toolbar.addSeparator();

        JButton btnZoomIn = toolBtn("[ + ]  Zoom In", Color.decode("#313244"));
        JButton btnZoomOut = toolBtn("[ - ]  Zoom Out", Color.decode("#313244"));
        JButton btnReset = toolBtn("Reset View", Color.decode("#313244"));
        JButton btnClear = toolBtn("Clear All", Color.decode("#9B2335"));

        toolbar.add(btnZoomIn);
        toolbar.add(btnZoomOut);
        toolbar.add(btnReset);
        toolbar.addSeparator();
        toolbar.add(btnClear);
        toolbar.addSeparator();

        JLabel boardLbl = new JLabel("Board: ");
        boardLbl.setForeground(Color.LIGHT_GRAY);
        toolbar.add(boardLbl);
        JComboBox<String> boardCombo = new JComboBox<>(
                new String[]{"Arduino Uno", "Arduino Nano", "Arduino Mega", "ESP32", "Otto DIY"});
        boardCombo.setMaximumSize(new Dimension(150, 28));
        toolbar.add(boardCombo);

        // Block count indicator
        toolbar.addSeparator();
        JLabel blockCountLabel = new JLabel("Blocks: 0");
        blockCountLabel.setForeground(Color.decode("#a6e3a1"));
        blockCountLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        toolbar.add(blockCountLabel);

        add(toolbar, BorderLayout.NORTH);

        // ── Canvas ───────────────────────────────────────────────────────
        canvas = new BlockCanvas();
        canvas.setOwnerFrame(this);
        canvas.setOnChangeListener(blocks -> {
            String code = codeGen.generate(blocks);
            codeArea.setText(code);
            blockCountLabel.setText("Blocks: " + blocks.size());
            statusLabel.setText(
                    "  " + blocks.size() + " blocks  |  "
                    + "Scroll wheel: zoom toward cursor  ·  Alt+drag: pan  ·  "
                    + "Drag block near another's bottom to connect");
        });

        JScrollPane canvasScroll = new JScrollPane(canvas);
        canvasScroll.setBorder(null);
        canvasScroll.getViewport().setBackground(Color.decode("#1e1e2e"));

        // ── Code panel ────────────────────────────────────────────────────
        codeArea.setBackground(Color.decode("#11111b"));
        codeArea.setForeground(Color.decode("#a6e3a1"));
        codeArea.setCaretColor(Color.WHITE);
        codeArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        codeArea.setEditable(false);
        codeArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane codeScroll = new JScrollPane(codeArea);
        codeScroll.setBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, Color.decode("#313244")));

        JLabel codeTitle = new JLabel("  ⌨  Arduino Code");
        codeTitle.setForeground(Color.decode("#89b4fa"));
        codeTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        codeTitle.setBackground(Color.decode("#181825"));
        codeTitle.setOpaque(true);
        codeTitle.setBorder(new EmptyBorder(7, 8, 7, 8));

        JPanel codePanel = new JPanel(new BorderLayout());
        codePanel.setPreferredSize(new Dimension(270, 0));
        codePanel.add(codeTitle, BorderLayout.NORTH);
        codePanel.add(codeScroll, BorderLayout.CENTER);

        // ── Toolbox ───────────────────────────────────────────────────────
        ToolboxPanel toolbox = new ToolboxPanel(categories, canvas::addBlock);

        // ── Layout ────────────────────────────────────────────────────────
        JSplitPane splitRight = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, canvasScroll, codePanel);
        splitRight.setResizeWeight(0.78);
        splitRight.setDividerSize(4);

        JSplitPane splitMain = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, toolbox, splitRight);
        splitMain.setResizeWeight(0.0);
        splitMain.setDividerSize(4);
        splitMain.setDividerLocation(192);

        add(splitMain, BorderLayout.CENTER);

        // ── Status bar ────────────────────────────────────────────────────
        statusLabel.setForeground(Color.GRAY);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusLabel.setBackground(Color.decode("#11111b"));
        statusLabel.setOpaque(true);
        statusLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        add(statusLabel, BorderLayout.SOUTH);

        // ── Button actions ────────────────────────────────────────────────
        btnZoomIn.addActionListener(e -> canvas.zoomIn());
        btnZoomOut.addActionListener(e -> canvas.zoomOut());
        btnReset.addActionListener(e -> canvas.resetView());
        btnClear.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Clear all blocks?", "Confirm Clear", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                canvas.clearBlocks();
                codeArea.setText("// Canvas cleared — add blocks to start");
            }
        });
    }

    private JButton toolBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(5, 12, 5, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ── Entry point ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        // Resolve blocks directory relative to where the jar/class is run from
        String blocksDir = "blocks";

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        final Map<String, List<BlockDefinition>> categories
                = BlockLoader.loadAll(blocksDir);

        if (categories.isEmpty()) {
            System.err.println("[WARN] No block definitions loaded. "
                    + "Make sure the 'blocks/' folder exists next to your working directory.");
        }

        SwingUtilities.invokeLater(() -> new MainWindow(categories).setVisible(true));
    }
}
