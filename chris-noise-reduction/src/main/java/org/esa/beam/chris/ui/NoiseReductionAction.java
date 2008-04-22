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
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
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
        final Product selectedProduct = getAppContext().getSelectedProduct();
        final Product[] acquisitionSet = getAcquisitionSet(selectedProduct);


        final NoiseReductionPresenter presenter =
                new NoiseReductionPresenter(acquisitionSet, new AdvancedSettingsPresenter());
        final NoiseReductionForm noiseReductionForm = new NoiseReductionForm(presenter);
        final TargetProductSelectorModel targetProductSelectorModel = noiseReductionForm.getTargetProductSelectorModel();
        final ModalDialog dialog =
                new ModalDialog(VisatApp.getApp().getMainFrame(), DIALOG_TITLE, ModalDialog.ID_OK_CANCEL_HELP, "chrisNoiseReductionTool") {
                    @Override
                    protected boolean verifyUserInput() {
                        if (!targetProductSelectorModel.getProductName().contains("${source}")) {
                            showErrorDialog("Target product name must use '${source}' expression.");
                            return false;
                        }

                        final Product[] sourceProducts = presenter.getCheckedProducts();
                        final File[] targetProductFiles = new File[sourceProducts.length];
                        List<String> existingFilePaths = new ArrayList<String>(7);
                        for (int i = 0; i < sourceProducts.length; ++i) {
                            targetProductFiles[i] = createTargetProductFile(targetProductSelectorModel, sourceProducts[i].getName());
                            if (targetProductFiles[i].exists()) {
                                existingFilePaths.add(targetProductFiles[i].getAbsolutePath());
                            }
                        }
                        if (!existingFilePaths.isEmpty()) {
                            String fileList = StringUtils.arrayToString(existingFilePaths.toArray(new String[existingFilePaths.size()]), "\n");
                            String message = "The specified output file(s)\n{0}\nalready exists.\n\n" +
                                    "Do you want to overwrite the existing file(s)?";
                            String formatedMessage = MessageFormat.format(message, fileList);
                            final int answer = JOptionPane.showConfirmDialog(this.getJDialog(), formatedMessage,
                                    DIALOG_TITLE, JOptionPane.YES_NO_OPTION);
                            if (answer != JOptionPane.YES_OPTION) {
                                return false;
                            }
                        }

                        return true;
                    }
                };
        dialog.setContent(noiseReductionForm);

        String homeDirPath = SystemUtils.getUserHomeDir().getPath();
        String saveDir = getAppContext().getPreferences().getPropertyString(BasicApp.PROPERTY_KEY_APP_LAST_SAVE_DIR, homeDirPath);
        targetProductSelectorModel.setProductDir(new File(saveDir));

        targetProductSelectorModel.setProductName("${source}_NR");

        if (dialog.show() == ModalDialog.ID_OK) {
            for (final Product product : acquisitionSet) {
                if (!(VisatApp.getApp().getProductManager().contains(product) || presenter.isListed(product))) {
                    product.dispose();
                }
            }

            try {
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
                                       TargetProductSelectorModel productSelectorModel) {

        final String productDir = productSelectorModel.getProductDir().getAbsolutePath();
        getAppContext().getPreferences().setPropertyString(BasicApp.PROPERTY_KEY_APP_LAST_SAVE_DIR, productDir);

        final Product[] sourceProducts = presenter.getCheckedProducts();
        final File[] targetProductFiles = new File[sourceProducts.length];
        for (int i = 0; i < sourceProducts.length; ++i) {
            targetProductFiles[i] = createTargetProductFile(productSelectorModel, sourceProducts[i].getName());
        }

        final Map<String, Object> parameterMap = presenter.getDropoutCorrectionParameterMap();
        new NoiseReductionSwingWorker(sourceProducts, presenter.getDestripingParameterMap(), parameterMap, targetProductFiles, productSelectorModel.getFormatName(), productSelectorModel.isSaveToFileSelected(),
                productSelectorModel.isOpenInAppSelected()).executeWithBlocking();
    }

    private File createTargetProductFile(TargetProductSelectorModel productSelectorModel, String s) {
        final String fileName = productSelectorModel.getProductFile().getName().replaceAll("\\$\\{source\\}", s);
        return new File(productSelectorModel.getProductDir(), fileName);
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


    private class NoiseReductionSwingWorker extends ProgressMonitorSwingWorker<Object, Product> {

        private final Product[] sourceProducts;
        private final Map<String, Object> dfParameterMap;
        private final Map<String, Object> dcParameterMap;

        private final File[] targetFiles;
        private final String targetFormatName;

        private final boolean saveToFile;
        private final boolean openInApp;

        public NoiseReductionSwingWorker(Product[] sourceProducts, Map<String, Object> dfParameterMap,
                                         Map<String, Object> dcParameterMap,
                                         File[] targetFiles,
                                         String targetFormatName,
                                         boolean saveToFile,
                                         boolean openInApp) {
            super(getAppContext().getApplicationWindow(), "Performing Noise Reduction");
            this.sourceProducts = sourceProducts;
            this.dfParameterMap = dfParameterMap;
            this.dcParameterMap = dcParameterMap;
            this.targetFiles = targetFiles;
            this.targetFormatName = targetFormatName;
            this.saveToFile = saveToFile;
            this.openInApp = openInApp;
        }

        @Override
        protected Object doInBackground(ProgressMonitor pm) throws Exception {
            pm.beginTask("Performing noise reduction...", 50 + sourceProducts.length * 10);
            try {
                Product factorProduct =
                        GPF.createProduct("chris.ComputeDestripingFactors",
                                dfParameterMap,
                                sourceProducts);
                final String name = FileUtils.getFilenameWithoutExtension(targetFiles[0]);
                final String extension = FileUtils.getExtension(targetFiles[0]);
                final File file = new File(targetFiles[0].getParentFile(), name + "_VSC" + extension);
                writeProduct(factorProduct, file, false, true, SubProgressMonitor.create(pm, 50));
                try {
                    factorProduct = ProductIO.readProduct(file, null);
                } catch (IOException e) {
                    throw new OperatorException(MessageFormat.format("Could not read file ''{0}''.", file.getPath()), e);
                }

                for (int i = 0; i < sourceProducts.length; ++i) {
                    performNoiseReduction(sourceProducts[i], factorProduct, targetFiles[i],
                            SubProgressMonitor.create(pm, 10));
                }
            } finally {
                pm.done();
            }
            return null;
        }

        @Override
        protected void process(List<Product> products) {
            if (openInApp) {
                for (Product product : products) {
                    getAppContext().addProduct(product);
                }
            }
        }

        @Override
        protected void done() {
            try {
                get();
            } catch (InterruptedException e) {
                // ignore
            } catch (ExecutionException e) {
                getAppContext().handleError(e.getCause());
            }
        }

        private void performNoiseReduction(Product sourceProduct, Product factorProduct, File targetFile, ProgressMonitor pm) throws IOException {
            final HashMap<String, Product> sourceProductMap = new HashMap<String, Product>(5);
            sourceProductMap.put("input", sourceProduct);
            sourceProductMap.put("factors", factorProduct);
            final Product destripedProduct = GPF.createProduct("chris.ApplyDestripingFactors",
                    new HashMap<String, Object>(0), sourceProductMap);

            final Product targetProduct = GPF.createProduct("chris.CorrectDropouts", dcParameterMap,
                    destripedProduct);

            targetProduct.setName(FileUtils.getFilenameWithoutExtension(targetFile));
            writeProduct(targetProduct, targetFile, openInApp, saveToFile, pm);
        }

        private void writeProduct(final Product targetProduct, final File file, boolean openInApp, boolean saveToFile, ProgressMonitor pm) throws IOException {
            pm.beginTask("Writing " + targetProduct.getName() + "...", openInApp ? 100 : 95);
            if (saveToFile) {
                targetProduct.setFileLocation(file);
                Product writtenProduct = null;
                try {
                    WriteOp.writeProduct(targetProduct,
                            file,
                            targetFormatName, SubProgressMonitor.create(pm, 95));
                    if (openInApp) {
                        writtenProduct = ProductIO.readProduct(file, null);
                        if (writtenProduct == null) {
                            writtenProduct = targetProduct;
                        }
                        publish(writtenProduct);
                        pm.worked(5);
                    }
                } finally {
                    pm.done();
                    if (writtenProduct != targetProduct) {
                        targetProduct.dispose();
                    }
                }
            }
        }

    }

}
