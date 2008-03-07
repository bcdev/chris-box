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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import com.jidesoft.docking.DockingManager;
import com.jidesoft.grid.ColorCellEditor;
import com.jidesoft.grid.ColorCellRenderer;

/**
 * Cluster labeling tool view.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @author Marco Zühlke
 * @version $Revision$ $Date$
 */
public class ClusterLabelingToolView extends AbstractToolView {

    private final CloudLabeler cloudLabeler;
    private final VisatApp visatApp;
    private final String viewKey;
    
    private LabelTableModel tableModel;

    public ClusterLabelingToolView(String viewKey, CloudLabeler cloudLabeler) {
        this.viewKey = viewKey;
        this.cloudLabeler = cloudLabeler;
        visatApp = VisatApp.getApp();
    }

    public LabelTableModel getTableModel() {
        return tableModel;
    }

    @Override
    protected JComponent createControl() {

        setTitle("Cluster Labeling");

        final JButton applyLabelingButton = new JButton("Apply Labeling");
        final JButton continueProcessingButton = new JButton("Continue Processing");

        applyLabelingButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                applyLabeling();
            }
        });
        continueProcessingButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                computeCloudMask();
            }
        });

        final JPanel panel = new JPanel();
        panel.add(applyLabelingButton);
        panel.add(continueProcessingButton);

        tableModel = new LabelTableModel(cloudLabeler.getMembershipBand().getImageInfo());
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
    
    private void applyLabeling() {
        final ClusterLabelingToolView.LabelTableModel labelTableModel = getTableModel();
        Band membershipBand = cloudLabeler.getMembershipBand();
        membershipBand.setImageInfo(labelTableModel.getImageInfo().createDeepCopy());
        cloudLabeler.processLabelingStep(labelTableModel.getBackgroundIndexes());
        final JInternalFrame internalFrame = visatApp.findInternalFrame(membershipBand);
        final ProductSceneView productSceneView = (ProductSceneView) internalFrame.getContentPane();
        visatApp.updateImage(productSceneView);
    }
    
    private void computeCloudMask() {
        DockingManager dockingManager = visatApp.getMainFrame().getDockingManager();
        dockingManager.removeFrame(viewKey);
        Band membershipBand = cloudLabeler.getMembershipBand();
        final JInternalFrame internalFrame = visatApp.findInternalFrame(membershipBand);
        visatApp.getDesktopPane().closeFrame(internalFrame);
        final ClusterLabelingToolView.LabelTableModel labelTableModel = getTableModel();
        cloudLabeler.processLabelingStep(labelTableModel.getBackgroundIndexes());
        final int[] cloudClusterIndexes = labelTableModel.getCloudIndexes();
        final int[] surfaceClusterIndexes = labelTableModel.getSurfaceIndexes();
        final Product cloudMaskProduct = cloudLabeler.processStepTwo(cloudClusterIndexes, surfaceClusterIndexes);
        visatApp.addProduct(cloudMaskProduct);
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
