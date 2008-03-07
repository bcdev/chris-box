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

import com.jidesoft.grid.ColorCellEditor;
import com.jidesoft.grid.ColorCellRenderer;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.ui.application.support.AbstractToolView;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Array;

/**
 * Cluster labeling tool view.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @author Marco Z�hlke
 * @version $Revision$ $Date$
 */
public class ClusterLabelingToolView extends AbstractToolView {

    private ActionListener labelingAction;
    private ActionListener computeAction;
    private ImageInfo imageInfo;
    private LabelTableModel tableModel;


    public ClusterLabelingToolView(ImageInfo imageInfo) {
        this.imageInfo = imageInfo;
    }

    public void setLabelingAction(ActionListener labelingAction) {
        this.labelingAction = labelingAction;
    }

    public void setComputeAction(ActionListener computeAction) {
        this.computeAction = computeAction;
    }

    public LabelTableModel getTableModel() {
        return tableModel;
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

        tableModel = new LabelTableModel(imageInfo);
        final JTable jTable = new JTable(tableModel);
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


    static class LabelTableModel extends AbstractTableModel {

        private ImageInfo imageInfo;
        private boolean[] cloud;
        private boolean[] background;
        private static final String[] COLUMN_NAMES = new String[]{"Label", "Colour", "Cloud", "Background", "Freq."};
        private static final Class<?>[] COLUMN_TYPES = new Class<?>[]{String.class, Color.class, Boolean.class,
                Boolean.class, Double.class};

        private LabelTableModel(ImageInfo imageInfo) {
            this.imageInfo = imageInfo;
            final int numPoints = imageInfo.getColorPaletteDef().getNumPoints();
            cloud = new boolean[numPoints];
            background = new boolean[numPoints];
        }

        public ImageInfo getImageInfo() {
            return imageInfo;
        }

        // used ????
//        public void setImageInfo(ImageInfo imageInfo) {
//            this.imageInfo = imageInfo;
//            final int numPoints = imageInfo.getColorPaletteDef().getNumPoints();
//            cloud = new boolean[numPoints];
//            background = new boolean[numPoints];
//            fireTableDataChanged();
//        }

        public int[] getBackgroundIndexes() {
            return getSetIndexes(background);
        }

        public int[] getCloudIndexes() {
            return getSetIndexes(cloud);
        }

        private static int[] getSetIndexes(boolean[] indexArray) {
            int[] bgIndexes = new int[indexArray.length];
            int lastEntry = 0;
            for (int i = 0; i < indexArray.length; i++) {
                if (indexArray[i]) {
                    bgIndexes[lastEntry] = i;
                    lastEntry++;
                }
            }
            int[] result = new int[lastEntry];
            System.arraycopy(bgIndexes, 0, result, 0, lastEntry);
            return result;
        }

        public int[] getSurfaceIndexes() {
            boolean[] surface = new boolean[cloud.length];
            for (int i = 0; i < surface.length; i++) {
                surface[i] = !(cloud[i] || background[i]);
            }
            return getSetIndexes(surface);
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
            switch (columnIndex) {
                case 0:
                    return point.getLabel();
                case 1:
                    return point.getColor();
                case 2:
                    return cloud[rowIndex];
                case 3:
                    return background[rowIndex];
                case 4:
                    // todo - return abundance percentage
                    return 0.0; //imageInfo.getHistogramBins()[rowIndex];
                default:
                    return 0;
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            final ColorPaletteDef.Point point = imageInfo.getColorPaletteDef().getPointAt(rowIndex);
            switch (columnIndex) {
                case 0:
                    point.setLabel((String) aValue);
                    break;
                case 1:
                    point.setColor((Color) aValue);
                    break;
                case 2:
                    cloud[rowIndex] = (Boolean) aValue;
                    if (cloud[rowIndex]) {
                        background[rowIndex] = false;
                    }
                    break;
                case 3:
                    background[rowIndex] = (Boolean) aValue;
                    if (background[rowIndex]) {
                        cloud[rowIndex] = false;
                    }
                    break;
            }
            fireTableRowsUpdated(rowIndex, rowIndex);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex >= 0 && columnIndex <= 3;
        }

    }

}
