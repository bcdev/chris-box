/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.chris.ui;

import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.ui.application.support.AbstractToolView;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import com.jidesoft.grid.ColorCellRenderer;
import com.jidesoft.grid.ColorCellEditor;

/**
 * Cluster labeling tool view.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @author Marco Zühlke
 * @version $Revision$ $Date$
 */
public class ClusterLabelingToolView extends AbstractToolView {

    private final ActionListener labelingAction;
    private final ActionListener computeAction;
    private ImageInfo imageInfo;


    public ClusterLabelingToolView(ImageInfo imageInfo, ActionListener labelingAction, ActionListener computeAction) {
        this.imageInfo = imageInfo;
        this.labelingAction = labelingAction;
        this.computeAction = computeAction;
    }

    

    @Override
    protected JComponent createControl() {

        setTitle("Cluster Labeling");

        final JButton applyLabelingButton = new JButton("Apply Labeling");
        final JButton continueProcessingButton = new JButton("Continue Processing");

        applyLabelingButton.addActionListener(labelingAction);
        continueProcessingButton.addActionListener(computeAction);

        final JPanel panel = new JPanel();
        panel.add(applyLabelingButton);
        panel.add(continueProcessingButton);

        LabelTableModel tableModel = new LabelTableModel(imageInfo);
        final JTable jTable =  new JTable(tableModel);
        jTable.setDefaultRenderer(Double.class, new PercentageRenderer());
        final ColorCellRenderer colorCellRenderer = new ColorCellRenderer();
        colorCellRenderer.setColorValueVisible(false);
        jTable.setDefaultRenderer(Color.class, colorCellRenderer);
        jTable.setDefaultEditor(Color.class, new ColorCellEditor());

        jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        final JScrollPane tableScrollPane = new JScrollPane(jTable);
        tableScrollPane.getViewport().setPreferredSize(jTable.getPreferredSize());
        panel.add(tableScrollPane);

        return panel;
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


    private static class LabelTableModel extends AbstractTableModel {

        private ImageInfo imageInfo;
        private static final String[] COLUMN_NAMES = new String[]{"Label", "Colour", "Freq."};
        private static final Class<?>[] COLUMN_TYPES = new Class<?>[]{String.class, Color.class, Double.class};

        private LabelTableModel(ImageInfo imageInfo) {
            this.imageInfo = imageInfo;
        }

        public ImageInfo getImageInfo() {
            return imageInfo;
        }

        // used ????
        public void setImageInfo(ImageInfo imageInfo) {
            this.imageInfo = imageInfo;
            fireTableDataChanged();
        }

        @Override
        public String getColumnName(int columnIndex) {
            return COLUMN_NAMES[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return COLUMN_TYPES[columnIndex];
        }

        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        public int getRowCount() {
            return imageInfo != null ? imageInfo.getColorPaletteDef().getNumPoints() : 0;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            final ColorPaletteDef.Point point = imageInfo.getColorPaletteDef().getPointAt(rowIndex);
            if (columnIndex == 0) {
                return point.getLabel();
            } else if (columnIndex == 1) {
                return point.getColor();
            } else if (columnIndex == 2) {
                // todo - return abundance percentage
                return imageInfo.getHistogramBins()[rowIndex];
            } else {
                return 0;
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            final ColorPaletteDef.Point point = imageInfo.getColorPaletteDef().getPointAt(rowIndex);
            if (columnIndex == 0) {
                point.setLabel((String) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            } else if (columnIndex == 1) {
                point.setColor((Color) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0 || columnIndex == 1;
        }

    }

}
