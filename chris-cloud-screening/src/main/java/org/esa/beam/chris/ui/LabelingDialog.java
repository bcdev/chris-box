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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;

/**
 * Cloud labeling form.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @author Marco Zühlke
 * @version $Revision$ $Date$
 */
class LabelingDialog extends ModelessDialog {

    private final AppContext appContext;
    private final ScreeningContext screeningContext;

    private final LabelingFormModel formModel;
    private final LabelingForm form;

    private final PixelPositionListener pixelPositionListener;
    private final VetoableChangeListener frameClosedListener;

    private final JInternalFrame colorFrame;
    private final JInternalFrame classFrame;

    LabelingDialog(final AppContext appContext, final ScreeningContext screeningContext) {
        super(appContext.getApplicationWindow(),
              MessageFormat.format("CHRIS/PROBA Cloud Labeling - {0}", screeningContext.getRadianceProduct().getName()),
              ID_APPLY_CLOSE_HELP, CloudScreeningAction.HELP_ID);

        this.appContext = appContext;
        this.screeningContext = screeningContext;

        formModel = new LabelingFormModel(screeningContext);
        form = new LabelingForm(formModel);

        form.getCheckBox().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (form.getCheckBox().isSelected()) {
                    VisatApp.getApp().showInfoDialog("CHRIS/PROBA Cloud Screening",
                                                     "Calculating the probabilistic cloud mask can be extremely time consuming!",
                                                     "chrisbox.postLabling.showWarning");
                }
            }
        });

        pixelPositionListener = new PixelPositionListener() {
            @Override
            public void pixelPosChanged(ImageLayer baseImageLayer, int pixelX, int pixelY, int currentLevel,
                                        boolean pixelPosValid, MouseEvent e) {
                if (pixelPosValid) {
                    final int classIndex = screeningContext.getClassIndex(pixelX, pixelY, currentLevel);
                    form.getTable().getSelectionModel().setSelectionInterval(classIndex, classIndex);
                }
            }

            @Override
            public void pixelPosNotAvailable() {
            }
        };

        frameClosedListener = new VetoableChangeListener() {
            @Override
            public final void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
                if (JInternalFrame.IS_CLOSED_PROPERTY.equals(evt.getPropertyName())) {
                    if ((Boolean) evt.getNewValue()) {
                        final int answer = VisatApp.getApp().showQuestionDialog(
                                "All windows associated with the cloud labeling dialog will be closed. Do you really want to close the cloud labeling dialog?",
                                null);
                        if (answer == JOptionPane.NO_OPTION) {
                            throw new PropertyVetoException("Do not close.", evt);
                        } else {
                            close();
                        }
                    }
                }
            }
        };

        final String radianceProductName = screeningContext.getRadianceProduct().getName();
        final String rgbFrameTitle = MessageFormat.format("{0} - RGB", radianceProductName);
        colorFrame = createInternalFrame(screeningContext.getColorView(), rgbFrameTitle);

        final String classFrameTitle = MessageFormat.format("{0} - Classes", radianceProductName);
        classFrame = createInternalFrame(screeningContext.getClassView(), classFrameTitle);

        final AbstractButton button = getButton(ID_APPLY);
        button.setText("Run");
        button.setMnemonic('R');
        button.setToolTipText("Creates the cloud mask for the associated product.");
    }

    @Override
    protected void onApply() {
        final Worker worker = new Worker(appContext, screeningContext, formModel);
        worker.execute();
    }

    @Override
    public void hide() {
        form.prepareHide();

        classFrame.hide();
        colorFrame.hide();

        super.hide();
    }

    @Override
    public int show() {
        form.prepareShow();
        setContent(form);

        colorFrame.show();
        classFrame.show();

        return super.show();
    }

    @Override
    protected void onClose() {
        close();
    }

    @Override
    public void close() {
        disposeInternalFrame(classFrame);
        disposeInternalFrame(colorFrame);

        getJDialog().dispose();
    }

    private JInternalFrame createInternalFrame(ProductSceneView view, String title) {
        final VisatApp visatApp = VisatApp.getApp();

        view.setCommandUIFactory(visatApp.getCommandUIFactory());
        view.setNoDataOverlayEnabled(false);
        view.setROIOverlayEnabled(false);
        view.setGraticuleOverlayEnabled(false);
        view.setPinOverlayEnabled(false);
        view.setLayerProperties(visatApp.getPreferences());
        view.addPixelPositionListener(pixelPositionListener);

        final Icon icon = UIUtils.loadImageIcon("icons/RsBandAsSwath16.gif");

        final JInternalFrame frame = visatApp.createInternalFrame(title, icon, view, "");
        frame.addVetoableChangeListener(frameClosedListener);

        return frame;
    }

    private void disposeInternalFrame(JInternalFrame frame) {
        if (frame != null && !frame.isClosed()) {
            frame.removeVetoableChangeListener(frameClosedListener);

            final Container contentPane = frame.getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                view.removePixelPositionListener(pixelPositionListener);
                VisatApp.getApp().getDesktopPane().closeFrame(frame);
            }
        }
    }

    private static class Worker extends ProgressMonitorSwingWorker<Band, Object> {
        private final AppContext appContext;
        private final ScreeningContext screeningContext;
        private final LabelingFormModel formModel;

        Worker(AppContext appContext, ScreeningContext screeningContext, LabelingFormModel formModel) {
            super(appContext.getApplicationWindow(), "Creating cloud mask...");
            this.appContext = appContext;
            this.screeningContext = screeningContext;
            this.formModel = formModel;
        }

        @Override
        protected Band doInBackground(ProgressMonitor pm) throws Exception {
            return screeningContext.performCloudMaskCreation(formModel.getCloudyFlags(),
                                                             formModel.getIgnoreFlags(),
                                                             formModel.isProbabilistic(), pm);
        }

        @Override
        protected void done() {
            try {
                final Product radianceProduct = screeningContext.getRadianceProduct();
                final Band newBand = get();
                if (radianceProduct.containsBand(newBand.getName())) {
                    final Band oldBand = radianceProduct.getBand(newBand.getName());
                    final JInternalFrame oldFrame = VisatApp.getApp().findInternalFrame(oldBand);
                    if (oldFrame != null) {
                        VisatApp.getApp().getDesktopPane().closeFrame(oldFrame);
                    }
                    radianceProduct.removeBand(oldBand);
                }
                radianceProduct.addBand(newBand);
                VisatApp.getApp().openProductSceneView(newBand);
            } catch (InterruptedException e) {
                appContext.handleError(e);
            } catch (ExecutionException e) {
                appContext.handleError(e.getCause());
            }
        }
    }
}