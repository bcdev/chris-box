package org.esa.beam.chris.ui;

import com.jidesoft.grid.ColorCellEditor;
import com.jidesoft.grid.ColorCellRenderer;
import com.jidesoft.grid.ListComboBoxCellEditor;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.framework.ui.application.support.AbstractToolView;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.text.NumberFormat;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public class CloudMaskLabelingToolView extends AbstractToolView {

    private static final String[] CLOUD_CLASSES = new String[]{"Background", "Cloud", "Not Cloud"};
    private static final String[] CHRIS_CLOUD_CLASSES = new String[]{
            "Background",
            "Bright Cloud",
            "Cloud",
            "Cirrus",
            "Shadow",
            "Vegetation",
            "Soil",
            "Water",
            "Snow/Ice"
    };

    public static void main(String[] args) {
        final JFrame jFrame1 = new JFrame("Version 1 - With Checkboxes");
        final JFrame jFrame2a = new JFrame("Version 2.a - With Combobox");
        final JFrame jFrame2b = new JFrame("Version 2.b - With Combobox");
        final CloudMaskLabelingToolView maskLabelingToolView = new CloudMaskLabelingToolView();
        jFrame1.add(maskLabelingToolView.createCheckBoxTable());
        jFrame2a.add(maskLabelingToolView.createDropdownTable(CLOUD_CLASSES));
        jFrame2b.add(maskLabelingToolView.createDropdownTable(CHRIS_CLOUD_CLASSES));
        jFrame1.pack();
        jFrame2a.pack();
        jFrame2b.pack();
        jFrame2a.setLocation(jFrame1.getWidth() + 10, 0);
        jFrame2b.setLocation(jFrame1.getWidth() + 10 + jFrame2a.getWidth() + 10, 0);
        jFrame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame2a.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame2b.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                jFrame1.setVisible(true);
                jFrame2a.setVisible(true);
                jFrame2b.setVisible(true);
            }
        });
    }

    private static final CloudClass[] DEMO_CLOUD_CLASSES = new CloudClass[]{
            new CloudClass("Class 1", Color.RED, 0.12),
            new CloudClass("Class 2", Color.BLUE, 0.3),
            new CloudClass("Class 3", Color.ORANGE, 0.7),
            new CloudClass("Class 4", Color.YELLOW, 0.22),
            new CloudClass("Class 5", Color.GREEN, 0.6),
            new CloudClass("Class 6", Color.CYAN, 0.14),
            new CloudClass("Class 7", Color.MAGENTA, 0.16),
            new CloudClass("Class 8", Color.PINK, 0.20),
    };


    @Override
    protected JComponent createControl() {
        return createCheckBoxTable();
    }

    private JComponent createCheckBoxTable() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setRowAnchor(0, TableLayout.Anchor.NORTHWEST);
        tableLayout.setRowFill(0, TableLayout.Fill.BOTH);
        tableLayout.setRowWeightX(0, 1.0);
        tableLayout.setRowWeightY(0, 1.0);
        tableLayout.setRowAnchor(1, TableLayout.Anchor.NORTHEAST);
        tableLayout.setRowFill(1, TableLayout.Fill.VERTICAL);
        tableLayout.setRowWeightX(1, 1.0);
        final JPanel control = new JPanel(tableLayout);
        final JTable jTable = new JTable(new CloudMaskLabelingModel(DEMO_CLOUD_CLASSES));
        jTable.setDefaultRenderer(Double.class, new PercentageRenderer());
        final ColorCellRenderer colorCellRenderer = new ColorCellRenderer();
        colorCellRenderer.setColorValueVisible(false);
        jTable.setDefaultRenderer(Color.class, colorCellRenderer);
        jTable.setDefaultEditor(Color.class, new ColorCellEditor());
        jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        final JScrollPane tableScrollPane = new JScrollPane(jTable);
        tableScrollPane.getViewport().setPreferredSize(jTable.getPreferredSize());
        control.add(tableScrollPane);
        control.add(new JButton("Create..."));
        return control;
    }

    private JComponent createDropdownTable(String[] classNames) {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setRowAnchor(0, TableLayout.Anchor.NORTHWEST);
        tableLayout.setRowFill(0, TableLayout.Fill.BOTH);
        tableLayout.setRowWeightX(0, 1.0);
        tableLayout.setRowWeightY(0, 1.0);
        tableLayout.setRowAnchor(1, TableLayout.Anchor.NORTHEAST);
        tableLayout.setRowFill(1, TableLayout.Fill.VERTICAL);
        tableLayout.setRowWeightX(1, 1.0);
        final JPanel control = new JPanel(tableLayout);
        final JTable jTable = new JTable(new CloudMaskLabelingModelWithDropDown(DEMO_CLOUD_CLASSES));
        jTable.setDefaultRenderer(Double.class, new PercentageRenderer());
        final ColorCellRenderer colorCellRenderer = new ColorCellRenderer();
        colorCellRenderer.setColorValueVisible(false);
        jTable.setDefaultRenderer(Color.class, colorCellRenderer);
        jTable.setDefaultEditor(Color.class, new ColorCellEditor());
        jTable.setDefaultEditor(String.class, new ListComboBoxCellEditor(classNames));
        jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        final JScrollPane tableScrollPane = new JScrollPane(jTable);
        tableScrollPane.getViewport().setPreferredSize(jTable.getPreferredSize());
        control.add(tableScrollPane);
        control.add(new JButton("Create..."));
        return control;
    }

    private static class PercentageRenderer extends DefaultTableCellRenderer {

        private final NumberFormat formatter;

        public PercentageRenderer() {
            setHorizontalAlignment(JLabel.RIGHT);
            formatter = NumberFormat.getPercentInstance();
            formatter.setMinimumFractionDigits(1);
            formatter.setMaximumFractionDigits(3);
        }

        @Override
        public void setValue(Object value) {
            setText((value == null) ? "" : formatter.format(value));
        }
    }

}
