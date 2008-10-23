package org.esa.beam.chris.ui;


import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Action for masking clouds.
 *
 * @author Marco Peters
 * @author Marco Zuehlke
 */
public class CloudMaskAction extends AbstractVisatAction {

    private static final List<String> CHRIS_TYPES;
    private final Set<Product> activeProductSet;

    public CloudMaskAction() {
        activeProductSet = new HashSet<Product>(7);
    }

    static {
        CHRIS_TYPES = new ArrayList<String>(7);
        Collections.addAll(CHRIS_TYPES,
                           "CHRIS_M1_NR",
                           "CHRIS_M2_NR",
                           "CHRIS_M3_NR",
                           "CHRIS_M30_NR",
                           "CHRIS_M3A_NR",
                           "CHRIS_M4_NR",
                           "CHRIS_M5_NR");
    }

    @Override
    public void actionPerformed(CommandEvent event) {
        final Product selectedProduct = getAppContext().getSelectedProduct();
        activeProductSet.add(selectedProduct);
        final CloudScreeningFormModel formModel = new CloudScreeningFormModel();
        // todo - GUI
        formModel.setRadianceProduct(selectedProduct);

        final SwingWorker<CloudScreeningPerformer, Object> worker = new CloudScreeningSwingWorker(formModel);
        worker.execute();
    }

    @Override
    public void updateState() {
        final Product selectedProduct = getAppContext().getSelectedProduct();
        final boolean valid = selectedProduct != null && CHRIS_TYPES.contains(selectedProduct.getProductType());
        setEnabled(valid && !activeProductSet.contains(selectedProduct));
    }

    private class CloudScreeningSwingWorker extends ProgressMonitorSwingWorker<CloudScreeningPerformer, Object> {
        private final CloudScreeningFormModel formModel;

        private CloudScreeningSwingWorker(CloudScreeningFormModel formModel) {
            super(getAppContext().getApplicationWindow(), "Performing Cluster Analysis...");
            this.formModel = formModel;
        }

        @Override
        protected CloudScreeningPerformer doInBackground(ProgressMonitor pm) throws Exception {
            final CloudScreeningPerformer performer = new CloudScreeningPerformer(getAppContext(), formModel);
            performer.performClusterAnalysis(pm);

            return performer;
        }

        @Override
        protected void done() {
            try {
                final CloudScreeningPerformer performer = get();
                final Window owner = getAppContext().getApplicationWindow();
                final String title = MessageFormat.format("CHRIS/PROBA Cloud Labeling - {0}",
                                                          performer.getRadianceProductName());
                final JDialog dialog = new LabelingDialog(owner, title, performer);
                dialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        activeProductSet.remove(formModel.getRadianceProduct());
//                        VisatApp.getApp().setSelectedProductNode(formModel.getRadianceProduct());
                    }
                });
                dialog.setVisible(true);
            } catch (InterruptedException e) {
                getAppContext().handleError(e);
            } catch (ExecutionException e) {
                getAppContext().handleError(e.getCause());
            }
        }
    }
}
