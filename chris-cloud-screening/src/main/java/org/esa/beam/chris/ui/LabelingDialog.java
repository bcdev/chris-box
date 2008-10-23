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
import com.bc.ceres.swing.SwingHelper;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.text.MessageFormat;
import java.text.NumberFormat;

/**
 * Cloud labeling form.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @author Marco Zühlke
 * @version $Revision$ $Date$
 */
class LabelingDialog extends JDialog {

    private final CloudScreeningPerformer cloudLabeler;
    private final JTable labelingTable;
    private final ClassSelector classSelector;

    private JInternalFrame classificationFrame;
    private JInternalFrame rgbFrame;
    private VetoableChangeListener classificationFrameClosedListener;
    private VetoableChangeListener rgbFrameClosedListener;

    LabelingDialog(Window owner, String title, CloudScreeningPerformer cloudLabeler) {
        super(owner, title, ModalityType.MODELESS);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setName("chrisCloudLabelingDialog");

        this.cloudLabeler = cloudLabeler;
        final LabelingContext labelingContext = cloudLabeler.createLabelingContext();
        labelingTable = createLabelingTable(labelingContext);
        classSelector = new ClassSelector(labelingContext, labelingTable.getSelectionModel());

        getContentPane().add(createControl());
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            final VisatApp visatApp = VisatApp.getApp();

            if (rgbFrame == null) {
                final ProductSceneView view = cloudLabeler.getRgbSceneView();
                final String title = MessageFormat.format("{0} - RGB", cloudLabeler.getRadianceProduct().getName());

                view.getScene().setName(title);
                view.setCommandUIFactory(visatApp.getCommandUIFactory());
                view.setNoDataOverlayEnabled(false);
                view.setROIOverlayEnabled(false);
                view.setGraticuleOverlayEnabled(false);
                view.setPinOverlayEnabled(false);
                view.setLayerProperties(visatApp.getPreferences());
                view.addPixelPositionListener(classSelector);

                final Icon icon = UIUtils.loadImageIcon("icons/RsBandAsSwath16.gif");

                rgbFrame = visatApp.createInternalFrame(title, icon, view, "");
                rgbFrame.addVetoableChangeListener(new VetoableFrameClosedListener());
                rgbFrame.addInternalFrameListener(new InternalFrameAdapter() {
                    @Override
                    public void internalFrameClosed(InternalFrameEvent e) {
                        rgbFrame.removeInternalFrameListener(this);
                        view.removePixelPositionListener(classSelector);
                        dispose();
                    }
                });
            }

            if (classificationFrame == null) {
                final ProductSceneView view = cloudLabeler.getClassificationSceneView();
                final String title = MessageFormat.format("{0} - Classification",
                                                          cloudLabeler.getRadianceProduct().getName());

                view.getScene().setName(title);
                view.setCommandUIFactory(visatApp.getCommandUIFactory());
                view.setNoDataOverlayEnabled(false);
                view.setROIOverlayEnabled(false);
                view.setGraticuleOverlayEnabled(false);
                view.setPinOverlayEnabled(false);
                view.setLayerProperties(visatApp.getPreferences());
                view.addPixelPositionListener(classSelector);

                final Icon icon = UIUtils.loadImageIcon("icons/RsBandAsSwath16.gif");

                classificationFrame = visatApp.createInternalFrame(title, icon, view, "");
                classificationFrame.addVetoableChangeListener(new VetoableFrameClosedListener());
                classificationFrame.addInternalFrameListener(new InternalFrameAdapter() {
                    @Override
                    public void internalFrameClosed(InternalFrameEvent e) {
                        classificationFrame.removeInternalFrameListener(this);
                        view.removePixelPositionListener(classSelector);
                        dispose();
                    }
                });
            }

            pack();
            SwingHelper.centerComponent(this, visatApp.getMainFrame());
        }

        super.setVisible(visible);
    }

    @Override
    public void dispose() {
        closeFrame(classificationFrame);
        classificationFrame = null;

        closeFrame(rgbFrame);
        rgbFrame = null;

        super.dispose();
    }

    private void closeFrame(JInternalFrame internalFrame) {
        if (internalFrame != null && !internalFrame.isClosed()) {
            VisatApp.getApp().getDesktopPane().closeFrame(internalFrame);
        }
    }

    private JComponent createControl() {
        final JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        panel.add(createCenterPanel(), BorderLayout.CENTER);
        panel.add(createButtonPanel(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createCenterPanel() {
        final JCheckBox checkBox = new JCheckBox("Calculate probabilistic cloud mask", false);
        checkBox.setToolTipText("If checked, a probabilistic cloud mask is calculated");
        checkBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (checkBox.isSelected()) {
                    final String message = "Calculating the probabilistic cloud mask can be extremely time consuming!";
                    VisatApp.getApp().showInfoDialog("CHRIS/PROBA Cloud Screening", message,
                                                     "chrisbox.postLabling.showWarning");
                }
            }
        });
        labelingTable.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getColumn() == LabelingTableModel.CLOUD_COLUMN) {
                    if (cloudLabeler.hasCloudClasses()) {
                        checkBox.setEnabled(true);
                    } else {
                        checkBox.setEnabled(false);
                        checkBox.setSelected(false);
                    }
                }
            }
        });
        // todo - bind checkBox

        final JScrollPane scrollPane = new JScrollPane(labelingTable);
        scrollPane.getViewport().setPreferredSize(labelingTable.getPreferredSize());
        
        final JPanel panel = new JPanel(new BorderLayout(2, 2));
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(checkBox, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createButtonPanel() {
        final JButton applyButton = new JButton("Create Cloud Mask");
        applyButton.setMnemonic('M');
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cloudLabeler.performCloudMaskCreation();
                dispose();
            }
        });

        final JButton closeButton = new JButton("Close");
        closeButton.setMnemonic('C');
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        final JButton helpButton = new JButton("Help");
        helpButton.setMnemonic('H');
        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                HelpSys.showTheme("chrisCloudScreeningTools");
            }
        });

        final JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.add(applyButton);
        panel.add(closeButton);
        panel.add(helpButton);

        return panel;
    }

    private static JTable createLabelingTable(LabelingContext labelingContext) {
        final TableModel tableModel = new LabelingTableModel(labelingContext);
        final JTable labelingTable = new JTable(tableModel);

        labelingTable.setDefaultRenderer(Color.class, createColorRenderer());
        labelingTable.setDefaultEditor(Color.class, createColorEditor());
        labelingTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        labelingTable.getColumnModel().getColumn(LabelingTableModel.BRIGHTNESS_COLUMN).setCellRenderer(
                createBrightnessRenderer());
        labelingTable.getColumnModel().getColumn(LabelingTableModel.OCCURRENCE_COLUMN).setCellRenderer(
                createOccurrenceRenderer());

        return labelingTable;
    }

    private static TableCellEditor createColorEditor() {
        return new ColorCellEditor();
    }

    private static TableCellRenderer createColorRenderer() {
        final ColorCellRenderer renderer = new ColorCellRenderer();
        renderer.setColorValueVisible(false);

        return renderer;
    }

    private static TableCellRenderer createBrightnessRenderer() {
        final NumberFormat numberFormat = NumberFormat.getInstance();

        numberFormat.setMinimumIntegerDigits(1);
        numberFormat.setMaximumIntegerDigits(1);
        numberFormat.setMinimumFractionDigits(3);
        numberFormat.setMaximumFractionDigits(3);

        return new FormattedNumberRenderer(numberFormat);
    }

    private static TableCellRenderer createOccurrenceRenderer() {
        final NumberFormat numberFormat = NumberFormat.getPercentInstance();

        numberFormat.setMinimumFractionDigits(1);
        numberFormat.setMaximumFractionDigits(3);

        return new FormattedNumberRenderer(numberFormat);
    }

    private static final class ClassSelector implements PixelPositionListener {
        private final LabelingContext labelingContext;
        private final ListSelectionModel selectionModel;

        private ClassSelector(LabelingContext labelingContext, ListSelectionModel selectionModel) {
            this.labelingContext = labelingContext;
            this.selectionModel = selectionModel;
        }

        @Override
        public void pixelPosChanged(RenderedImage image, int pixelX, int pixelY, boolean pixelPosValid, MouseEvent e) {
            if (pixelPosValid) {
                final int classIndex = labelingContext.getClassIndex(pixelX, pixelY);
                selectionModel.setSelectionInterval(classIndex, classIndex);
            }
        }

        @Override
        public void pixelPosNotAvailable(RenderedImage sourceImage) {
        }
    }

    private static final class VetoableFrameClosedListener implements VetoableChangeListener {
        private static final String MESSAGE = "Do you really want to exit the cloud labeling dialog?";

        @Override
        public final void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
            if (JInternalFrame.IS_CLOSED_PROPERTY.equals(evt.getPropertyName())) {
                if ((Boolean) evt.getNewValue()) {
                    final int answer = VisatApp.getApp().showQuestionDialog(MESSAGE, null);
                    if (answer == JOptionPane.NO_OPTION) {
                        throw new PropertyVetoException("Do not close.", evt);
                    }
                }
            }
        }
    }
}