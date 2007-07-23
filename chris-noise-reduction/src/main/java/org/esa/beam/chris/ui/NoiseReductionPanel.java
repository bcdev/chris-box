package org.esa.beam.chris.ui;

import com.jidesoft.grid.BooleanCheckBoxCellEditor;
import com.jidesoft.grid.BooleanCheckBoxCellRenderer;
import org.esa.beam.framework.datamodel.Product;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EventObject;

/**
 * Panel for the CHRIS Noise Reduction dialog.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class NoiseReductionPanel extends JPanel {

    private JTable aquisitionSetTable;
    private JTable metadataTable;

    private JButton addButton;
    private JButton removeButton;

    private JButton advancedSettingsButton;

    /**
     * Creates new form NRPanel
     */
    public NoiseReductionPanel(NoiseReductionPresenter presenter) {
        initComponents();
        bindComponents(presenter);
    }

    private void bindComponents(NoiseReductionPresenter presenter) {
        aquisitionSetTable.setModel(presenter.getProductTableModel());
        aquisitionSetTable.setSelectionModel(presenter.getProductTableSelectionModel());

        TableColumn column1 = aquisitionSetTable.getColumnModel().getColumn(0);
        column1.setCellRenderer(new BooleanCheckBoxCellRenderer());
        column1.setCellEditor(new BooleanCheckBoxCellEditor());
        column1.setPreferredWidth(60);
        column1.setMaxWidth(60);

        TableColumn column2 = aquisitionSetTable.getColumnModel().getColumn(1);
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
        GridBagConstraints gridBagConstraints;

        JPanel dataPanel = new JPanel();
        JPanel aquisitionSetPanel = new JPanel();
        JScrollPane acquisitionScrollPane = new JScrollPane();
        aquisitionSetTable = new JTable();
        addButton = new JButton();
        removeButton = new JButton();
        JPanel metadataPanel = new JPanel();
        metadataTable = new JTable();
        advancedSettingsButton = new JButton();

        setLayout(new BorderLayout(5, 5));

        dataPanel.setLayout(new BorderLayout());

        aquisitionSetPanel.setLayout(new GridBagLayout());

        aquisitionSetPanel.setBorder(BorderFactory.createTitledBorder(null, "Acquisition Set",
                                                                      TitledBorder.DEFAULT_JUSTIFICATION,
                                                                      TitledBorder.DEFAULT_POSITION,
                                                                      new Font("Tahoma", 0, 11),
                                                                      new Color(0, 70, 213)));
        aquisitionSetPanel.setPreferredSize(new Dimension(450, 200));
        acquisitionScrollPane.setPreferredSize(new Dimension(300, 150));
        aquisitionSetTable.setPreferredSize(new Dimension(300, 150));
        aquisitionSetTable.setFillsViewportHeight(true);
        acquisitionScrollPane.setViewportView(aquisitionSetTable);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 0.8;
        gridBagConstraints.weighty = 1.0;
        aquisitionSetPanel.add(acquisitionScrollPane, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        aquisitionSetPanel.add(addButton, gridBagConstraints);


        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        aquisitionSetPanel.add(removeButton, gridBagConstraints);

        dataPanel.add(aquisitionSetPanel, BorderLayout.CENTER);

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

        add(dataPanel, BorderLayout.CENTER);

        JPanel settingsButtonPanel = new JPanel(new GridBagLayout());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 5, 5, 5);
        settingsButtonPanel.add(advancedSettingsButton, gridBagConstraints);
        add(settingsButtonPanel, BorderLayout.SOUTH);
    }

}
