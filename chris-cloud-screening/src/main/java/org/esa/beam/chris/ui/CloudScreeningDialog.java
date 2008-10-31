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

    CloudScreeningDialog(AppContext appContext, String helpID) {
        super(appContext.getApplicationWindow(), "CHRIS/PROBA Cloud Screening", ID_APPLY_CLOSE_HELP, helpID);

        this.appContext = appContext;
        formModel = new CloudScreeningFormModel();
        form = new CloudScreeningForm(appContext, formModel);
    }

    @Override
    protected void onApply() {
        final Product sourceProduct = formModel.getSourceProduct();

        if (!appContext.getProductManager().contains(sourceProduct)) {
            appContext.getProductManager().addProduct(sourceProduct);
        }

        final ClusterAnalysisWorker worker = new ClusterAnalysisWorker(appContext, formModel);
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

    CloudScreeningForm getForm() {
        return form;
    }

    CloudScreeningFormModel getFormModel() {
        return formModel;
    }


    private static class ClusterAnalysisWorker extends ProgressMonitorSwingWorker<CloudScreeningContext, Object> {
        private final AppContext appContext;
        private final CloudScreeningFormModel formModel;

        private ClusterAnalysisWorker(AppContext appContext, CloudScreeningFormModel formModel) {
            super(appContext.getApplicationWindow(), "Performing Cluster Analysis...");

            this.appContext = appContext;
            this.formModel = formModel;
        }

        @Override
        protected CloudScreeningContext doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {
            return new CloudScreeningContext(appContext, formModel, pm);
        }

        @Override
        protected void done() {
            try {
                final CloudScreeningContext context = get();
                final JDialog dialog = new LabelingDialog(appContext, context.createLabelingContext());
                dialog.setVisible(true);
            } catch (InterruptedException e) {
                appContext.handleError(e);
            } catch (ExecutionException e) {
                appContext.handleError(e.getCause());
            }
        }
    }
}
