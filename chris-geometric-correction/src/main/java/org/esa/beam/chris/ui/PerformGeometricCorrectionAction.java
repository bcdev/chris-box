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

import org.esa.beam.chris.operators.PerformGeometricCorrectionOp;
import org.esa.beam.chris.operators.TimeConverter;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ExecutionException;
import java.awt.Window;

import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.bc.ceres.core.ProgressMonitor;

/**
 * Action for invoking the geometric correction.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class PerformGeometricCorrectionAction extends AbstractVisatAction {

    private static final String TITLE = "CHRIS/Proba Geometric Correction";
    private static final String KEY_FETCH_LATEST_TIME_TABLES = "beam.chris.fetchLatestTimeTables";
    private static final int UPDATE_PERIOD = 7;

    private final AtomicReference<ModelessDialog> dialog;
    private static final String QUESTION_FETCH_LATEST_TIME_TABLES =
            MessageFormat.format(
                    "Your UT1 and leap second time tables are older than {0} days. Fetching\n" +
                    "the latest time tables from the web can take a few minutes.\n" +
                    "\n" +
                    "Do you want to fetch the latest time tables now?",
                    UPDATE_PERIOD);

    public PerformGeometricCorrectionAction() {
        dialog = new AtomicReference<ModelessDialog>();
    }

    @Override
    public void actionPerformed(CommandEvent commandEvent) {
        final TimeConverter converter;

        try {
            converter = TimeConverter.getInstance();
        } catch (IOException e) {
            handleError("The geometric correction cannot be carried out because an error occurred.", e);
            return;
        }
        if (isTimeConverterOudated(converter, UPDATE_PERIOD)) {
            final int answer = showQuestionDialog(QUESTION_FETCH_LATEST_TIME_TABLES, KEY_FETCH_LATEST_TIME_TABLES);
            if (answer == JOptionPane.YES_OPTION) {
                new TimeConverterUpdater(getAppContext().getApplicationWindow(), TITLE, converter).execute();
            }
        } else {
            dialog.compareAndSet(null, createDialog(getAppContext()));
            dialog.get().show();
        }
    }

    @Override
    public void updateState() {
        final Product selectedProduct = getAppContext().getSelectedProduct();
        setEnabled(selectedProduct == null || new GeometricCorrectionProductFilter().accept(selectedProduct));
    }

    private void handleError(String message, Throwable e) {
        getAppContext().handleError(message, e);
    }

    private static ModelessDialog createDialog(AppContext appContext) {
        final DefaultSingleTargetProductDialog dialog =
                new DefaultSingleTargetProductDialog(OperatorSpi.getOperatorAlias(PerformGeometricCorrectionOp.class),
                                                     appContext, TITLE, "chrisGeometricCorrectionTool");
        dialog.getJDialog().setName("chrisGeometricCorrectionDialog");
        dialog.setTargetProductNameSuffix("_GC");

        return dialog;
    }

    private static void showInfoDialog(String message) {
        VisatApp.getApp().showInfoDialog(TITLE, message, null);
    }

    private static int showQuestionDialog(String message, String preferencesKey) {
        return VisatApp.getApp().showQuestionDialog(TITLE, message, preferencesKey);
    }

    private static boolean isTimeConverterOudated(TimeConverter converter, int days) {
        final Date now = new Date();
        return now.getTime() - converter.lastModified() > days * TimeConverter.MILLIS_PER_DAY;
    }

    private class TimeConverterUpdater extends ProgressMonitorSwingWorker<Object, Object> {

        private final TimeConverter converter;

        public TimeConverterUpdater(Window applicationWindow, String title, TimeConverter converter) {
            super(applicationWindow, title);
            this.converter = converter;
        }

        @Override
        protected Object doInBackground(ProgressMonitor pm) throws Exception {
            converter.updateTimeTables(pm);
            return null;
        }

        @Override
        protected void done() {
            try {
                get();
            } catch (InterruptedException e) {
                handleError("An error occurred while updating the UT1 and leap second time tables.", e);
                return;
            } catch (ExecutionException e) {
                handleError("An error occurred while updating the UT1 and leap second time tables.", e.getCause());
                return;
            }
            showInfoDialog("The UT1 and leap second time tables have been updated successfully.");
            dialog.compareAndSet(null, createDialog(getAppContext()));
            dialog.get().show();
        }
    }
}
