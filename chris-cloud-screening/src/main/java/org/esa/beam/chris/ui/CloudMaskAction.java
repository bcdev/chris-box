package org.esa.beam.chris.ui;


import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Action for masking clouds.
 *
 * @author Marco Peters
 * @author Marco Zuehlke
 */
public class CloudMaskAction extends AbstractVisatAction {

    private static final List<String> CHRIS_TYPES;
    private Set<Product> activeProductSet;

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
        final SwingWorker<CloudLabeler, Object> worker = new ActionSwingWorker(selectedProduct);
        worker.execute();
    }

    @Override
    public void updateState() {
        final Product selectedProduct = getAppContext().getSelectedProduct();
        final boolean valid = selectedProduct != null && CHRIS_TYPES.contains(selectedProduct.getProductType());
        setEnabled(valid && !activeProductSet.contains(selectedProduct));
    }

    private class ActionSwingWorker extends ProgressMonitorSwingWorker<CloudLabeler, Object> {

        private final Product radianceProduct;

        private ActionSwingWorker(Product radianceProduct) {
            super(getAppContext().getApplicationWindow(), "Performing Cluster Analysis");
            this.radianceProduct = radianceProduct;
        }

        @Override
        protected CloudLabeler doInBackground(ProgressMonitor pm) throws Exception {
            final CloudLabeler cloudLabeler = new CloudLabeler(radianceProduct);
            cloudLabeler.performClusterAnalysis(pm);
            cloudLabeler.createRgbSceneView();
            return cloudLabeler;
        }

        @Override
        protected void done() {
            try {
                final CloudLabeler cloudLabeler = get();
                final ClusterLabelingWindow labelingWindow = new ClusterLabelingWindow(cloudLabeler);

                labelingWindow.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        activeProductSet.remove(radianceProduct);
                        VisatApp.getApp().setSelectedProductNode(cloudLabeler.getRadianceProduct());
                    }
                });
                labelingWindow.setVisible(true);
            } catch (InterruptedException e) {
                getAppContext().handleError(e);
            } catch (ExecutionException e) {
                getAppContext().handleError(e.getCause());
            }
        }
    }
}
