package org.esa.beam.chris.ui;

import com.jidesoft.grid.BooleanCheckBoxCellEditor;
import com.jidesoft.grid.BooleanCheckBoxCellRenderer;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.TableLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.text.MessageFormat;
import java.util.EventObject;

/**
 * Form for the CHRIS Noise Reduction dialog.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class NoiseReductionForm extends JPanel {

    private JTable acquisitionSetTable;
    private JTable metadataTable;

    private JButton addButton;
    private JButton removeButton;

    private TargetProductSelector targetProductSelector;

    private JButton advancedSettingsButton;

    /**
     * Creates new form NRPanel
     */
    public NoiseReductionForm(NoiseReductionPresenter presenter) {
        initComponents();
        bindComponents(presenter);
    }

    public TargetProductSelectorModel getTargetProductSelectorModel() {
        return targetProductSelector.getModel();
    }

    private void bindComponents(NoiseReductionPresenter presenter) {
        acquisitionSetTable.setModel(presenter.getProductTableModel());
        acquisitionSetTable.setSelectionModel(presenter.getProductTableSelectionModel());

        TableColumn column1 = acquisitionSetTable.getColumnModel().getColumn(0);
        column1.setCellRenderer(new BooleanCheckBoxCellRenderer());
        column1.setCellEditor(new BooleanCheckBoxCellEditor());
        column1.setPreferredWidth(90);
        column1.setMaxWidth(90);

        TableColumn column2 = acquisitionSetTable.getColumnModel().getColumn(1);
        column2.setCellRenderer(
                new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                                   boolean hasFocus, int row, int column) {
                        final Component component = super.getTableCellRendererComponent(table, value, isSelected,
                                                                                        hasFocus, row, column);
                        if (value instanceof Product) {
                            ((JLabel) component).setText(((Product) value).getName());
                        }
                        return component;
                    }
                });
        column2.setCellEditor(
                new TableCellEditor() {
                    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                                 int row, int column) {
                        return null;
                    }

                    public Object getCellEditorValue() {
                        return null;
                    }

                    public boolean isCellEditable(EventObject e) {
                        return false;
                    }

                    public boolean shouldSelectCell(EventObject e) {
                        return false;
                    }

                    public boolean stopCellEditing() {
                        return false;
                    }

                    public void cancelCellEditing() {
                    }

                    public void addCellEditorListener(CellEditorListener l) {
                    }

                    public void removeCellEditorListener(CellEditorListener l) {
                    }
                }
        );

        metadataTable.setModel(presenter.getMetadataTableModel());
        metadataTable.setEnabled(false);

        addButton.setAction(presenter.getAddProductAction());
        removeButton.setAction(presenter.getRemoveProductAction());
        advancedSettingsButton.setAction(presenter.getSettingsAction());
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {
        JPanel dataPanel = new JPanel();
        JPanel acquisitionSetPanel = new JPanel();
        JScrollPane acquisitionScrollPane = new JScrollPane();
        acquisitionSetTable = new JTable();
        acquisitionSetTable.setName("acquisitionSetTable");
        addButton = new JButton();
        addButton.setName("addButton");
        removeButton = new JButton();
        removeButton.setName("removeButton");
        JPanel metadataPanel = new JPanel();
        metadataTable = new JTable();
        metadataTable.setName("metadataTable");

        targetProductSelector = new TargetProductSelector();
        // only DIMAP format works because written intermediate products are read again
        targetProductSelector.getFormatNameComboBox().setEnabled(false);

        targetProductSelector.getProductNameLabel().setText(
                MessageFormat.format("Name pattern - {0} will be replaced with the source product name:",
                                     NoiseReductionAction.SOURCE_NAME_PATTERN));

        advancedSettingsButton = new JButton();
        advancedSettingsButton.setName("advancedSettingsButton");

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTablePadding(5, 5);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(1.0);
        setLayout(tableLayout);

        dataPanel.setLayout(new BorderLayout());

        acquisitionSetPanel.setLayout(new GridBagLayout());

        acquisitionSetPanel.setBorder(BorderFactory.createTitledBorder(null, "Acquisition Set",
                                                                       TitledBorder.DEFAULT_JUSTIFICATION,
                                                                       TitledBorder.DEFAULT_POSITION,
                                                                       new Font("Tahoma", 0, 11),
                                                                       new Color(0, 70, 213)));
        acquisitionSetPanel.setPreferredSize(new Dimension(450, 200));
        acquisitionScrollPane.setPreferredSize(new Dimension(300, 150));
        acquisitionSetTable.setPreferredSize(new Dimension(300, 150));
        acquisitionSetTable.setFillsViewportHeight(true);
        acquisitionScrollPane.setViewportView(acquisitionSetTable);

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(4, 4, 4, 4);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 0.8;
        gridBagConstraints.weighty = 1.0;
        acquisitionSetPanel.add(acquisitionScrollPane, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        acquisitionSetPanel.add(addButton, gridBagConstraints);


        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        acquisitionSetPanel.add(removeButton, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;

        dataPanel.add(acquisitionSetPanel, BorderLayout.CENTER);

        metadataPanel.setLayout(new GridBagLayout());

        metadataPanel.setBorder(BorderFactory.createTitledBorder(null, "Image Metadata",
                                                                 TitledBorder.DEFAULT_JUSTIFICATION,
                                                                 TitledBorder.DEFAULT_POSITION,
                                                                 new Font("Tahoma", 0, 11),
                                                                 new Color(0, 70, 213)));
        metadataTable.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0)));
        metadataTable.setTableHeader(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        metadataPanel.add(metadataTable, gridBagConstraints);

        dataPanel.add(metadataPanel, BorderLayout.SOUTH);

        add(dataPanel);
        targetProductSelector.getSaveToFileCheckBox().setSelected(true);
        targetProductSelector.getSaveToFileCheckBox().setEnabled(false);
        add(targetProductSelector.createDefaultPanel());

        JPanel settingsButtonPanel = new JPanel(new GridBagLayout());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 5, 5, 5);
        settingsButtonPanel.add(advancedSettingsButton, gridBagConstraints);
        add(settingsButtonPanel);
    }
}
