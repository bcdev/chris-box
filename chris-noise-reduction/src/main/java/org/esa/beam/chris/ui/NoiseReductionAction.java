/* $Id$
 *
 * Copyright (C) 2002-2007 by Brockmann Consult
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
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.operators.common.WriteOp;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.BasicApp;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * Noise reduction action.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class NoiseReductionAction extends AbstractVisatAction {

    private static final String DIALOG_TITLE = "CHRIS Noise Reduction";

    static final List<String> CHRIS_TYPES;

    static {
        CHRIS_TYPES = new ArrayList<String>(7);
        Collections.addAll(CHRIS_TYPES, "CHRIS_M1", "CHRIS_M2", "CHRIS_M3", "CHRIS_M30", "CHRIS_M3A", "CHRIS_M4",
                           "CHRIS_M5");
    }

    @Override
    public void actionPerformed(CommandEvent commandEvent) {
        final Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        final Product[] acquisitionSet = getAcquisitionSet(selectedProduct);

        final NoiseReductionPresenter presenter =
                new NoiseReductionPresenter(acquisitionSet, new AdvancedSettingsPresenter());
        final ModalDialog dialog =
                new ModalDialog(VisatApp.getApp().getMainFrame(),
                                DIALOG_TITLE,
                                ModalDialog.ID_OK_CANCEL_HELP,
                                "chrisNoiseReductionTool");
        final NoiseReductionForm noiseReductionForm = new NoiseReductionForm(presenter);
        dialog.setContent(noiseReductionForm);
        final TargetProductSelectorModel targetProductSelectorModel = noiseReductionForm.getTargetProductSelectorModel();
        targetProductSelectorModel.setProductName("${source}_NR");

        if (dialog.show() == ModalDialog.ID_OK) {
            for (final Product product : acquisitionSet) {
                if (!(VisatApp.getApp().getProductManager().contains(product) || presenter.isListed(product))) {
                    product.dispose();
                }
            }

            try {
                if (!targetProductSelectorModel.getProductName().contains("${source}")) {
                    throw new OperatorException("Target product name must use '${source}' expression.");
                }
                performNoiseReduction(presenter, targetProductSelectorModel);
            } catch (OperatorException e) {
                disposeProducts(acquisitionSet);
                dialog.showErrorDialog(e.getMessage());
                VisatApp.getApp().getLogger().log(Level.SEVERE, e.getMessage(), e);
            }
        } else {
            disposeProducts(acquisitionSet);
        }
    }

    private void writeProduct(final Product targetProduct,
                              final File targetFile,
                              final String formatName,
                              final boolean saveToFile,
                              final boolean openInApp) {
        if (saveToFile) {
            if (targetFile.exists()) {
                String message = "The specified output file\n\"{0}\"\n already exists.\n\n" +
                                 "Do you want to overwrite the existing file?";
                String formatedMessage = MessageFormat.format(message, targetFile.getAbsolutePath());
                final int answer = JOptionPane.showConfirmDialog(null, formatedMessage,
                                                                 DIALOG_TITLE, JOptionPane.YES_NO_OPTION);
                if (answer != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            targetProduct.setFileLocation(targetFile);
            final ProgressMonitorSwingWorker worker = new ProductWriterSwingWorker(targetProduct,
                                                                                   targetFile,
                                                                                   formatName,
                                                                                   openInApp);
            worker.executeWithBlocking();
        } else if (openInApp) {
            getAppContext().addProduct(targetProduct);
        }
    }


    @Override
    public void updateState() {
        final Product selectedProduct = getAppContext().getSelectedProduct();
        final boolean enabled = selectedProduct != null
                                && !(selectedProduct.getProductReader().getInput() instanceof Product)
                                && !(selectedProduct.getProductReader().getInput() instanceof Product[])
                                && CHRIS_TYPES.contains(selectedProduct.getProductType());
        setEnabled(enabled);
    }

    private static void disposeProducts(Product[] products) {
        for (final Product product : products) {
            if (!VisatApp.getApp().getProductManager().contains(product)) {
                product.dispose();
            }
        }
    }

    private void performNoiseReduction(NoiseReductionPresenter presenter,
                                       TargetProductSelectorModel productSelectorModel)
            throws OperatorException {
        final String productDir = productSelectorModel.getProductDir().getAbsolutePath();
        getAppContext().getPreferences().setPropertyString(BasicApp.PROPERTY_KEY_APP_LAST_SAVE_DIR, productDir);

        final Product[] sourceProducts = presenter.getListedProducts();
        final Product factors =
                GPF.createProduct("chris.ComputeDestripingFactors",
                                  presenter.getDestripingParameterMap(),
                                  sourceProducts);
        final File factorsFile = new File(productSelectorModel.getProductDir(), sourceProducts[0].getName() + "_VSC");
        writeProduct(factors, factorsFile, productSelectorModel.getFormatName(), true, false);

        for (final Product sourceProduct : presenter.getCheckedProducts()) {
            final Product targetProduct = createTargetProduct(presenter,
                                                              productSelectorModel,
                                                              sourceProduct,
                                                              factors);
            writeProduct(targetProduct,
                         productSelectorModel.getProductFile(),
                         productSelectorModel.getFormatName(),
                         productSelectorModel.isSaveToFileSelected(),
                         productSelectorModel.isOpenInAppSelected());
        }
    }

    private Product createTargetProduct(NoiseReductionPresenter presenter,
                                        TargetProductSelectorModel productSelectorModel,
                                        Product sourceProduct,
                                        Product factorProduct) {
        final HashMap<String, Product> productsMap = new HashMap<String, Product>(5);
        productsMap.put("input", sourceProduct);
        productsMap.put("factors", factorProduct);

        final Product destriped =
                GPF.createProduct("chris.ApplyDestripingFactors",
                                  new HashMap<String, Object>(0),
                                  productsMap);

        final Product targetProduct =
                GPF.createProduct("chris.CorrectDropouts",
                                  presenter.getDropoutCorrectionParameterMap(),
                                  destriped);
        final String targetName = productSelectorModel.getProductName().replace("$\\{source\\}",
                                                                                sourceProduct.getName());
        targetProduct.setName(targetName);
        return targetProduct;
    }

    private static Product[] getAcquisitionSet(Product selectedProduct) {
        final SortedSet<Product> acquisitionSet = new TreeSet<Product>(
                new Comparator<Product>() {
                    public final int compare(Product p, Product q) {
                        return p.getName().compareTo(q.getName());
                    }
                });

        final Product[] visatProducts = VisatApp.getApp().getProductManager().getProducts();
        for (final Product product : visatProducts) {
            if (NoiseReductionPresenter.areFromSameAcquisition(selectedProduct, product)) {
                acquisitionSet.add(product);
            }
        }

        final File selectedFile = selectedProduct.getFileLocation();
        if (selectedFile != null) {
            final File parent = selectedFile.getParentFile();
            if (parent != null && parent.isDirectory()) {
                final File[] files = parent.listFiles(
                        new FileFilter() {
                            public boolean accept(File file) {
                                return NoiseReductionPresenter.areFromSameAcquisition(selectedFile, file);
                            }
                        });

                acquire:
                for (final File file : files) {
                    try {
                        for (final Product product : acquisitionSet) {
                            if (file.equals(product.getFileLocation())) {
                                continue acquire;
                            }
                        }
                        final Product product = ProductIO.readProduct(file, null);
                        if (product.getProductType().equals(selectedProduct.getProductType())) {
                            acquisitionSet.add(product);
                        }
                    } catch (IOException e) {
                        // ignore - we acquire products silently
                    }
                }
            }
        }

        return acquisitionSet.toArray(new Product[acquisitionSet.size()]);
    }

    private class ProductWriterSwingWorker extends ProgressMonitorSwingWorker<Product, Object> {

        private final Product targetProduct;
        private final boolean openInApp;
        private final File productFile;
        private final String formatName;

        private ProductWriterSwingWorker(Product targetProduct,
                                         final File productFile,
                                         final String formatName,
                                         final boolean openInApp) {
            super(getAppContext().getApplicationWindow(), "Writing Target Product");
            this.targetProduct = targetProduct;
            this.productFile = productFile;
            this.formatName = formatName;
            this.openInApp = openInApp;
        }

        @Override
        protected Product doInBackground(ProgressMonitor pm) throws Exception {
            pm.beginTask("Writing...", openInApp ? 100 : 95);
            Product product = null;
            try {
                WriteOp.writeProduct(targetProduct,
                                     productFile,
                                     formatName, SubProgressMonitor.create(pm, 95));
                if (openInApp) {
                    product = ProductIO.readProduct(productFile, null);
                    if (product == null) {
                        product = targetProduct;
                    }
                    pm.worked(5);
                }
            } finally {
                pm.done();
                if (product != targetProduct) {
                    targetProduct.dispose();
                }
            }
            return product;
        }

        @Override
        protected void done() {
            try {
                final Product targetProduct = get();
                if (openInApp) {
                    getAppContext().addProduct(targetProduct);
                }
            } catch (InterruptedException e) {
                // ignore
            } catch (ExecutionException e) {
                getAppContext().handleError(e.getCause());
            }
        }
    }

}
