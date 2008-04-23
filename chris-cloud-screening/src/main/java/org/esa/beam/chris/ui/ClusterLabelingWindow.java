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
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.image.RenderedImage;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.Arrays;

/**
 * Cluster labeling tool view.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @author Marco Zühlke
 * @version $Revision$ $Date$
 */
public class ClusterLabelingWindow extends JDialog {

    private final ProductSceneView productSceneView;
    private final CloudLabeler cloudLabeler;
    private final VisatApp visatApp;

    private LabelTableModel tableModel;
    private JCheckBox abundancesCheckBox;
    private JTable table;

    public ClusterLabelingWindow(ProductSceneView productSceneView, CloudLabeler cloudLabeler) {
        super(VisatApp.getApp().getMainFrame(), "CHRIS Cluster Labeling Tool", false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.productSceneView = productSceneView;
        this.cloudLabeler = cloudLabeler;
        visatApp = VisatApp.getApp();
        getContentPane().add(createControl());
        final MyPixelPositionListener listener = new MyPixelPositionListener(cloudLabeler);
        productSceneView.getImageDisplay().addPixelPositionListener(listener);
    }

    private JComponent createControl() {

        final JButton createButton = new JButton("Create Cloud Mask");
        createButton.setMnemonic('M');
        createButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                computeCloudMask();
                dispose();
            }
        });

        final JButton closeButton = new JButton("Close");
        closeButton.setMnemonic('C');
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        final JButton helpButton = new JButton("Help");
        helpButton.setMnemonic('H');
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // todo
            }
        });

        final JPanel mainPanel = new JPanel(new BorderLayout(4, 4));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        final JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        buttonRow.add(createButton);
        buttonRow.add(closeButton);
        buttonRow.add(helpButton);

        tableModel = new LabelTableModel(cloudLabeler.getMembershipBand().getImageInfo());
        tableModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                if (e.getColumn() == 1 || e.getColumn() == 3) {
                    applyLabeling(e.getColumn() == 1);
                }
            }
        });
        tableModel.setCloudClusterIndexes(cloudLabeler.getCloudClusterIndexes());
        tableModel.setBackgroundIndexes(cloudLabeler.getBackgroundClusterIndexes());
        table = new JTable(tableModel);
        table.setDefaultRenderer(Double.class, new PercentageRenderer());
        final ColorCellRenderer colorCellRenderer = new ColorCellRenderer();
        colorCellRenderer.setColorValueVisible(false);
        table.setDefaultRenderer(Color.class, colorCellRenderer);
        table.setDefaultEditor(Color.class, new ColorCellEditor());

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        final JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.getViewport().setPreferredSize(table.getPreferredSize());

        final JPanel tablePanel = new JPanel(new BorderLayout(2, 2));
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);
        abundancesCheckBox = new JCheckBox("Perform spectral unmixing on cloud pixel spectra", cloudLabeler.getComputeAbundances());
        abundancesCheckBox.setToolTipText("Select to weight final cloud probability with cloud pixel spectrum abundances");
        abundancesCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (abundancesCheckBox.isSelected()) {
                    final String message = "Spectral unmixing is extremely time consuming!";
                    visatApp.showInfoDialog("CHRIS Cloud Screening", message, "chrisbox.postLabling.showWarning");                    
                }
            }
        });
        tablePanel.add(abundancesCheckBox, BorderLayout.SOUTH);

        mainPanel.add(tablePanel, BorderLayout.CENTER);
        mainPanel.add(buttonRow, BorderLayout.SOUTH);

        return mainPanel;
    }

    private void applyLabeling(boolean updateColors) {
        Band membershipBand = cloudLabeler.getMembershipBand();
        membershipBand.setImageInfo(tableModel.getImageInfo().createDeepCopy());
        if (!updateColors) {
            cloudLabeler.processLabelingStep(tableModel.getBackgroundIndexes());
        }
        final JInternalFrame internalFrame = visatApp.findInternalFrame(membershipBand);
        if (internalFrame != null) {
            final ProductSceneView productSceneView = (ProductSceneView) internalFrame.getContentPane();
            visatApp.updateImage(productSceneView);
        }
    }

    private void computeCloudMask() {
        Band membershipBand = cloudLabeler.getMembershipBand();
        final JInternalFrame internalFrame = visatApp.findInternalFrame(membershipBand);
        visatApp.getDesktopPane().closeFrame(internalFrame);
        final int[] backgroundIndexes = tableModel.getBackgroundIndexes();
        cloudLabeler.processLabelingStep(backgroundIndexes);
        final int[] cloudClusterIndexes = tableModel.getCloudIndexes();
        final int[] surfaceClusterIndexes = tableModel.getSurfaceIndexes();
        final Product cloudMaskProduct = cloudLabeler.processStepTwo(cloudClusterIndexes, backgroundIndexes, surfaceClusterIndexes, abundancesCheckBox.isSelected());
        visatApp.addProduct(cloudMaskProduct);
    }

    public void selectClass(int clusterIndex) {
        table.getSelectionModel().setSelectionInterval(clusterIndex, clusterIndex);
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
            return getSelectedIndexes(background);
        }

        public int[] getCloudIndexes() {
            return getSelectedIndexes(cloud);
        }

        static int[] getSelectedIndexes(boolean[] array) {
            int[] indexes = new int[array.length];
            int indexCount = 0;
            for (int i = 0; i < array.length; i++) {
                if (array[i]) {
                    indexes[indexCount] = i;
                    indexCount++;
                }
            }
            int[] result = new int[indexCount];
            System.arraycopy(indexes, 0, result, 0, indexCount);
            return result;
        }

        static void setSelectedIndexes(boolean[] array, int[] indexes) {
            Arrays.fill(array, false);
            if (indexes != null) {
                for (final int index : indexes) {
                    if (index >= 0 && index < indexes.length) {
                        array[index] = true;
                    }
                }
            }
        }

        public int[] getSurfaceIndexes() {
            boolean[] surface = new boolean[cloud.length];
            for (int i = 0; i < surface.length; i++) {
                surface[i] = !(cloud[i] || background[i]);
            }
            return getSelectedIndexes(surface);
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
                    fireTableCellUpdated(rowIndex, 0);
                    break;
                case 1:
                    point.setColor((Color) aValue);
                    fireTableCellUpdated(rowIndex, 1);
                    break;
                case 2:
                    cloud[rowIndex] = (Boolean) aValue;
                    if (cloud[rowIndex] && background[rowIndex]) {
                        background[rowIndex] = false;
                        fireTableCellUpdated(rowIndex, 3);
                    }
                    fireTableCellUpdated(rowIndex, 2);
                    break;
                case 3:
                    background[rowIndex] = (Boolean) aValue;
                    if (cloud[rowIndex] && background[rowIndex]) {
                        cloud[rowIndex] = false;
                        fireTableCellUpdated(rowIndex, 2);
                    }
                    fireTableCellUpdated(rowIndex, 3);
                    break;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex >= 0 && columnIndex <= 3;
        }

        public void setCloudClusterIndexes(int[] cloudClusterIndexes) {
            setSelectedIndexes(cloud, cloudClusterIndexes);
        }

        public void setBackgroundIndexes(int[] backgroundClusterIndexes) {
            setSelectedIndexes(background, backgroundClusterIndexes);
        }
    }

    private class MyPixelPositionListener implements PixelPositionListener {
        private final CloudLabeler cloudLabeler;

        public MyPixelPositionListener(CloudLabeler cloudLabeler) {
            this.cloudLabeler = cloudLabeler;
        }

        public void pixelPosChanged(RenderedImage sourceImage, int pixelX, int pixelY, boolean pixelPosValid, MouseEvent e) {
            if (pixelPosValid) {
                final int clusterIndex = cloudLabeler.getMembershipBand().getPixelInt(pixelX, pixelY);
                selectClass(clusterIndex);
            }
        }

        public void pixelPosNotAvailable(RenderedImage sourceImage) {

        }
    }
}