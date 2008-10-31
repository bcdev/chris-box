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

import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModelessDialog;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.5
 */
class CloudScreeningDialog extends ModelessDialog {
    private final AppContext appContext;
    private final CloudScreeningFormModel formModel;
    private final CloudScreeningForm form;

    private final Set<Product> activeProductSet;

    CloudScreeningDialog(AppContext appContext, String helpID, Set<Product> activeProductSet) {
        super(appContext.getApplicationWindow(), "CHRIS/PROBA Cloud Screening", ID_APPLY_CLOSE_HELP, helpID);

        this.appContext = appContext;
        formModel = new CloudScreeningFormModel();
        form = new CloudScreeningForm(formModel, appContext);
        this.activeProductSet = activeProductSet;
    }

    @Override
    protected void onApply() {
        // todo - disable button when active product is selected 
        final ClusterAnalysisWorker worker = new ClusterAnalysisWorker(appContext, formModel, activeProductSet);
        worker.execute();
    }

    @Override
    public void hide() {
        form.prepareHide();
        super.hide();
    }

    @Override
    public int show() {
        form.prepareShow();
        setContent(form);
        return super.show();
    }

    private static class ClusterAnalysisWorker extends ProgressMonitorSwingWorker<CloudScreeningPerformer, Object> {
        private final AppContext appContext;
        private final CloudScreeningFormModel formModel;
        private final Set<Product> activeProductSet;

        private ClusterAnalysisWorker(AppContext appContext,
                                      CloudScreeningFormModel formModel,
                                      Set<Product> activeProductSet) {
            super(appContext.getApplicationWindow(), "Performing Cluster Analysis...");

            this.appContext = appContext;
            this.formModel = formModel;
            this.activeProductSet = activeProductSet;

            activeProductSet.add(formModel.getRadianceProduct());
        }

        @Override
        protected CloudScreeningPerformer doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {
            final CloudScreeningPerformer performer = new CloudScreeningPerformer(formModel);

            try {
                performer.performClusterAnalysis(appContext, pm);
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
                final String sourceProductName = formModel.getRadianceProduct().getName();
                final JDialog dialog = new LabelingDialog(appContext, sourceProductName, performer);
                dialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        activeProductSet.remove(formModel.getRadianceProduct());
                    }
                });
                dialog.setVisible(true);
            } catch (InterruptedException e) {
                appContext.handleError(e);
            } catch (ExecutionException e) {
                appContext.handleError(e.getCause());
            }
        }
    }
}
