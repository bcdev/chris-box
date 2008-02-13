package org.esa.beam.chris.ui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.MutableComboBoxModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;

import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.framework.ui.application.support.AbstractToolView;

import com.jidesoft.grid.ColorCellEditor;
import com.jidesoft.grid.ColorCellRenderer;

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
        final JFrame jFrame1 = new JFrame("VISAT");
        final CloudMaskLabelingToolView maskLabelingToolView = new CloudMaskLabelingToolView();
        jFrame1.add(maskLabelingToolView.createControl());
        jFrame1.pack();
        jFrame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                jFrame1.setVisible(true);
            }
        });
    }
    
    @Override
    protected JComponent createControl() {
        setTitle("Cloud Mask");
        JButton createButton1 = new JButton("Create Cloud Mask...");
        createButton1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CreateCloudProductDialog createCloudProductDialog = new CreateCloudProductDialog();
                createCloudProductDialog.setVisible(true);
            }
        });
        
        JButton createButton2 = new JButton("Create Cloud Mask...");
        createButton2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CreateCloudProductDialog createCloudProductDialog = new CreateCloudProductDialog();
                createCloudProductDialog.setVisible(true);
            }
        });
        
        JTabbedPane tabbedPane = new JTabbedPane();
        JComponent createCheckBoxTable = createCheckBoxTable();
        JComponent createDropdownTable = createDropdownTable(CHRIS_CLOUD_CLASSES);
        tabbedPane.add("Version 1", decorateWithButton(createCheckBoxTable, createButton1));
        tabbedPane.add("Version 2", decorateWithButton(createDropdownTable, createButton2));
        
        return tabbedPane;
    }
    
    private static final ClusterClass[] DEMO_CLUSTER_CLASSES = new ClusterClass[]{
            new ClusterClass("Class 1", Color.RED, 0.12),
            new ClusterClass("Class 2", Color.BLUE, 0.3),
            new ClusterClass("Class 3", Color.ORANGE, 0.7),
            new ClusterClass("Class 4", Color.YELLOW, 0.22),
            new ClusterClass("Class 5", Color.GREEN, 0.6),
            new ClusterClass("Class 6", Color.CYAN, 0.14),
            new ClusterClass("Class 7", Color.MAGENTA, 0.16),
            new ClusterClass("Class 8", Color.PINK, 0.20),
    };

    private JComponent decorateWithButton(JComponent tabbedPane, JButton button) {
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
        
        control.add(tabbedPane);
        control.add(button);
        return control;
    }

    private JComponent createCheckBoxTable() {
        final JTable jTable = new JTable(new CloudMaskLabelingModel(DEMO_CLUSTER_CLASSES));
        jTable.setDefaultRenderer(Double.class, new PercentageRenderer());
        final ColorCellRenderer colorCellRenderer = new ColorCellRenderer();
        colorCellRenderer.setColorValueVisible(false);
        jTable.setDefaultRenderer(Color.class, colorCellRenderer);
        jTable.setDefaultEditor(Color.class, new ColorCellEditor());
        jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        final JScrollPane tableScrollPane = new JScrollPane(jTable);
        tableScrollPane.getViewport().setPreferredSize(jTable.getPreferredSize());
        return tableScrollPane;
    }

    private JComponent createDropdownTable(final String[] classNames) {
        final JTable jTable = new JTable(new CloudMaskLabelingModelWithDropDown(DEMO_CLUSTER_CLASSES));
        jTable.setDefaultRenderer(Double.class, new PercentageRenderer());
        final ColorCellRenderer colorCellRenderer = new ColorCellRenderer();
        colorCellRenderer.setColorValueVisible(false);
        jTable.setDefaultRenderer(Color.class, colorCellRenderer);
        jTable.setDefaultEditor(Color.class, new ColorCellEditor());
        
        // JIDE AutoCompletionComboBox
//        AutoCompletionComboBox autoCompletionComboBoxNotStrict = new AutoCompletionComboBox(classNames);
//        autoCompletionComboBoxNotStrict.setStrict(false);
//        jTable.setDefaultEditor(String.class, new DefaultCellEditor(autoCompletionComboBoxNotStrict));
        
        // editable combox with addition
        final JComboBox comboBox = new JComboBox(classNames);
        comboBox.setEditable(true);
        
        comboBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if(!"comboBoxEdited".equals(e.getActionCommand())) {
                    return;
                }
                String selectedItem = (String) comboBox.getSelectedItem();
                if (selectedItem.startsWith("Class ")) {
                    return;
                }
                MutableComboBoxModel model = (MutableComboBoxModel) comboBox.getModel();
                boolean contains = false;
                for( int i = 0; i < model.getSize(); i++) {
                    if(selectedItem.equalsIgnoreCase((String) model.getElementAt(i))) {
                        contains = true;
                        break;
                    }
                }
                if (!contains) {
                    model.addElement(selectedItem);
                }
            }
        });
//        comboBox.setKeySelectionManager(new MyKeySelectionManager());
        jTable.setDefaultEditor(String.class, new DefaultCellEditor(comboBox));
        
        // JIDE IntelliHints
//        TextFieldCellEditor textFieldCellEditor = new TextFieldCellEditor(String.class);
//        JTextField textField = textFieldCellEditor.getTextField();
//        ListDataIntelliHints listDataIntelliHints = new ListDataIntelliHints(textField, classNames);
//        listDataIntelliHints.setCaseSensitive(false);
//        listDataIntelliHints.setHintsEnabled(true);
//        jTable.setDefaultEditor(String.class, textFieldCellEditor);

        jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        final JScrollPane tableScrollPane = new JScrollPane(jTable);
        tableScrollPane.getViewport().setPreferredSize(jTable.getPreferredSize());
        return tableScrollPane;
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
    
//    private static class MyKeySelectionManager implements KeySelectionManager,
//            Serializable {
//        
//        public int selectionForKey(char aKey, ComboBoxModel aModel) {
//            System.out.println("key: "+aKey);
//            final int modelSize = aModel.getSize();
//            String selectedItem = (String) aModel.getSelectedItem();
//            if (selectedItem != null) {
//                selectedItem = selectedItem.toLowerCase();
//                for (int i = 0; i < modelSize; i++) {
//                    String element = (String) aModel.getElementAt(i);
//                    element = element.toLowerCase();
//                    if (element.startsWith(selectedItem)) {
//                        System.out.println(i);
//                        return i;
//                    }
//                }
//            }
//            System.out.println(-1);
//            return -1;
//        }
//    }

}
