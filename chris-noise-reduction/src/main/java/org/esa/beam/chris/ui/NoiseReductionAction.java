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

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.BasicApp;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

/**
 * Noise reduction action.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class NoiseReductionAction extends AbstractVisatAction {

    private static final String DIALOG_TITLE = "CHRIS Noise Reduction";
    private static final String SOURCE_NAME_PATTERN = "${sourceName}";
    private static final String SOURCE_NAME_REGEX = "\\$\\{sourceName\\}";

    static final List<String> CHRIS_TYPES;

    static {
        CHRIS_TYPES = new ArrayList<String>(7);
        Collections.addAll(CHRIS_TYPES, "CHRIS_M1", "CHRIS_M2", "CHRIS_M3", "CHRIS_M30", "CHRIS_M3A", "CHRIS_M4",
                "CHRIS_M5");
    }

    @Override
    public void actionPerformed(CommandEvent commandEvent) {
        final Product[] acquisitionSet;
        final Product selectedProduct = getAppContext().getSelectedProduct();
        if (selectedProduct != null
                && !(selectedProduct.getProductReader().getInput() instanceof Product) // not a subset
                && !(selectedProduct.getProductReader().getInput() instanceof Product[]) // not created from operator
                && CHRIS_TYPES.contains(selectedProduct.getProductType())) {
            acquisitionSet = getAcquisitionSet(selectedProduct);
        } else {
            acquisitionSet = new Product[0];
        }

        final NoiseReductionPresenter presenter =
                new NoiseReductionPresenter(acquisitionSet, new AdvancedSettingsPresenter());
        final NoiseReductionForm noiseReductionForm = new NoiseReductionForm(presenter);
        final TargetProductSelectorModel targetProductSelectorModel = noiseReductionForm.getTargetProductSelectorModel();
        final ModalDialog dialog =
                new ModalDialog(VisatApp.getApp().getMainFrame(), DIALOG_TITLE, ModalDialog.ID_OK_CANCEL_HELP, "chrisNoiseReductionTool") {
                    @Override
                    protected boolean verifyUserInput() {
                        if (!targetProductSelectorModel.getProductName().contains(SOURCE_NAME_PATTERN)) {
                            showErrorDialog("Target product name must use the '" + SOURCE_NAME_PATTERN + "' expression.");
                            return false;
                        }

                        final Product[] sourceProducts = presenter.getNoiseReductionProducts();
                        if (sourceProducts.length == 0) {
                            showWarningDialog("At least one product must be selected for noise reduction.");
                            return false;
                        }
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

        targetProductSelectorModel.setProductName(SOURCE_NAME_PATTERN + "_NR");

        try {
            if (dialog.show() == ModalDialog.ID_OK) {
                for (final Product product : acquisitionSet) {
                    if (!(VisatApp.getApp().getProductManager().contains(product) || presenter.isSource(product))) {
                        product.dispose();
                    }
                }
                performNoiseReduction(presenter, targetProductSelectorModel);
            }
        } finally {
            disposeProducts(acquisitionSet);
        }
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

        final HashMap<Product, File> targetFileMap = new HashMap<Product, File>(7);
        for (Product product : presenter.getNoiseReductionProducts()) {
            final File targetProductFile = createTargetProductFile(productSelectorModel, product.getName());
            targetFileMap.put(product, targetProductFile);
        }

        final Map<String, Object> parameterMap = presenter.getDropoutCorrectionParameterMap();
        final NoiseReductionSwingWorker worker = new NoiseReductionSwingWorker(
                presenter.getSourceProducts(), targetFileMap,
                presenter.getDestripingParameterMap(),
                parameterMap,
                productSelectorModel.getFormatName(),
                getAppContext(), productSelectorModel.isOpenInAppSelected());
        worker.executeWithBlocking();
    }

    private File createTargetProductFile(TargetProductSelectorModel productSelectorModel, String s) {
        final String fileName = productSelectorModel.getProductFile().getName().replaceAll(SOURCE_NAME_REGEX, s);
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


}
