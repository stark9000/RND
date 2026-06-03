package rnd.blockly;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ToolboxPanel extends JPanel {

    private final Consumer<Block> onBlockAdded;

    public ToolboxPanel(Map<String, List<BlockDefinition>> categories,
                        Consumer<Block> onBlockAdded) {
        this.onBlockAdded = onBlockAdded;
        setBackground(Color.decode("#181825"));
        setPreferredSize(new Dimension(190, 0));
        setLayout(new BorderLayout());

        // Scrollable inner panel
        JPanel inner = new JPanel();
        inner.setBackground(Color.decode("#181825"));
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel title = new JLabel("BLOCKS");
        title.setForeground(new Color(180, 180, 210));
        title.setFont(new Font("SansSerif", Font.BOLD, 11));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        inner.add(title);
        inner.add(Box.createVerticalStrut(8));

        for (Map.Entry<String, List<BlockDefinition>> entry : categories.entrySet()) {
            String catName = entry.getKey();
            List<BlockDefinition> defs = entry.getValue();
            if (defs.isEmpty()) continue;

            // Category header
            Color catColor = defs.get(0).color;
            JLabel catLabel = new JLabel("▸ " + catName.toUpperCase());
            catLabel.setForeground(catColor.brighter());
            catLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
            catLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            catLabel.setBorder(new EmptyBorder(6, 0, 3, 0));
            inner.add(catLabel);

            for (BlockDefinition def : defs) {
                inner.add(makeBlockButton(def));
                inner.add(Box.createVerticalStrut(3));
            }
        }

        inner.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(inner,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.decode("#181825"));
        add(scroll, BorderLayout.CENTER);
    }

    private JButton makeBlockButton(final BlockDefinition def) {
        JButton btn = new JButton(def.label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(def.color);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(def.color.darker());
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Add \"" + def.label + "\" block  [" + def.category + "]");

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setForeground(Color.YELLOW); }
            public void mouseExited(MouseEvent e)  { btn.setForeground(Color.WHITE);  }
        });

        btn.addActionListener(e -> {
            int rx = 220 + (int)(Math.random() * 180);
            int ry = 80  + (int)(Math.random() * 200);
            Block b = new Block(def, rx, ry);
            onBlockAdded.accept(b);
        });

        return btn;
    }
}
