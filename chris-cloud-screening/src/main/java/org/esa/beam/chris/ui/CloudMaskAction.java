package org.esa.beam.chris.ui;


import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.chris.util.OpUtils;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Action for masking clouds.
 *
 * @author Marco Peters
 * @author Marco Zuehlke
 */
public class CloudMaskAction extends AbstractVisatAction {

    private final Set<Product> activeProductSet;

    public CloudMaskAction() {
        activeProductSet = new HashSet<Product>(7);
    }

    @Override
    public void actionPerformed(CommandEvent event) {
        final Product selectedProduct = getAppContext().getSelectedProduct();
        activeProductSet.add(selectedProduct);
        final CloudScreeningFormModel formModel = new CloudScreeningFormModel();
        // todo - GUI
        formModel.setRadianceProduct(selectedProduct);

        final SwingWorker<CloudScreeningPerformer, Object> worker = new ClusterAnalysisWorker(formModel);
        worker.execute();
    }

    @Override
    public void updateState() {
        final Product selectedProduct = getAppContext().getSelectedProduct();
        final boolean enabled = selectedProduct == null ||
                !activeProductSet.contains(selectedProduct) && isValid(selectedProduct);
        setEnabled(enabled);
    }

    private static boolean isValid(Product product) {
        return product.getProductType().matches("CHRIS_M.*_NR") &&
                OpUtils.findBands(product, "radiance").length >= 18;
    }

    private class ClusterAnalysisWorker extends ProgressMonitorSwingWorker<CloudScreeningPerformer, Object> {
        private final CloudScreeningFormModel formModel;

        private ClusterAnalysisWorker(CloudScreeningFormModel formModel) {
            super(getAppContext().getApplicationWindow(), "Performing Cluster Analysis...");
            this.formModel = formModel;
        }

        @Override
        protected CloudScreeningPerformer doInBackground(ProgressMonitor pm) throws Exception {
            final CloudScreeningPerformer performer = new CloudScreeningPerformer(formModel);

            try {
                performer.performClusterAnalysis(getAppContext(), pm);
            } catch (Exception e) {
                performer.dispose();
                throw e;
            }

            return performer;
        }

        @Override
        protected void done() {
            try {
                final CloudScreeningPerformer performer = get();
                final JDialog dialog = new LabelingDialog(getAppContext(), 
                                                          formModel.getRadianceProduct().getName(),
                                                          performer);
                dialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        activeProductSet.remove(formModel.getRadianceProduct());
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
