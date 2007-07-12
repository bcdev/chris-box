/* $Id: $
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

import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;

import java.awt.Dialog;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

public class NoiseReductionAction extends ExecCommand {

    static List<String> CHRIS_TYPES;

    static {
        CHRIS_TYPES = new ArrayList<String>();
        Collections.addAll(CHRIS_TYPES, "CHRIS_M1", "CHRIS_M2", "CHRIS_M3", "CHRIS_M3A", "CHRIS_M4", "CHRIS_M5");
    }

    @Override
    public void actionPerformed(CommandEvent commandEvent) {
        showNoiseReductionDialog();
    }

    @Override
    public void updateState() {
        final Product product = VisatApp.getApp().getSelectedProduct();
        setEnabled(product != null && CHRIS_TYPES.contains(product.getProductType()));
    }

    private static void showNoiseReductionDialog() {


        Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        List<Product> consideredProductList = new ArrayList<Product>();
        collectProductsFromVisat(selectedProduct, consideredProductList);

        // we must hold these seperately, cause we have to close them later
        List<Product> productListFromFileLocation = new ArrayList<Product>();
        collectProductsFromFilelocation(selectedProduct, productListFromFileLocation);
        consideredProductList.addAll(productListFromFileLocation);

        Product[] consideredProducts = consideredProductList.toArray(new Product[consideredProductList.size()]);
        NoiseReductionPresenter presenter = new NoiseReductionPresenter(consideredProducts,
                                                                        new AdvancedSettingsPresenter());
        ModalDialog modalDialog = new ModalDialog(VisatApp.getApp().getMainFrame(),
                                                  "CHRIS Noise Reduction",
                                                  ModalDialog.ID_OK_CANCEL_HELP, "chrisNoiseReductionProcessor");
        modalDialog.setContent(new NoiseReductionPanel(presenter));
        if (ModalDialog.ID_OK != modalDialog.show()) {
            for (Product product : productListFromFileLocation) {
                product.dispose();
            }
            return;
        }

        AdvancedSettingsPresenter settingsPresenter = presenter.getSettingsPresenter();
        DialogProgressMonitor pm = new DialogProgressMonitor(VisatApp.getApp().getMainFrame(),
                                                             "CHRIS Noise Reduction",
                                                             Dialog.ModalityType.APPLICATION_MODAL);

        // todo - adjust progress ticks
        pm.beginTask("Reducing Noise", 100);
        try {
            Product product1 = presenter.getProducts()[0];
            Product product2 = GPF.createProduct("DestripingFactors",
                                                 settingsPresenter.getDestripingParameter(),
                                                 product1, new SubProgressMonitor(pm, 30));
            HashMap<String, Product> productsMap = new HashMap<String, Product>(2);
            productsMap.put("sourceProduct", product1);
            productsMap.put("factorProduct", product2);
            Product product3 = GPF.createProduct("Destriping", new HashMap<String, Object>(0), productsMap,
                                                 new SubProgressMonitor(pm, 70));
            VisatApp.getApp().addProduct(product3);
        } catch (OperatorException e) {
            modalDialog.showErrorDialog(e.getMessage());
            VisatApp.getApp().getLogger().log(Level.SEVERE, e.getMessage(), e);
        } finally {
            pm.done();
        }
    }

    private static void collectProductsFromFilelocation(final Product selectedProduct,
                                                        List<Product> consideredProductList) {
        File productLocation = selectedProduct.getFileLocation();
        if (productLocation == null) {
            return;
        }
        File parentFile = productLocation.getParentFile();
        if (parentFile == null || !parentFile.isDirectory()) {
            return;
        }

        File[] files = parentFile.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return NoiseReductionPresenter.belongsToSameAquisitionSet(selectedProduct.getFileLocation(), pathname);
            }
        });

        for (File file : files) {
            try {
                if (!containsProduct(consideredProductList, file)) {
                    consideredProductList.add(ProductIO.readProduct(file, null));
                }
            } catch (IOException e) {
                // ignore - no message to user, we add products silently
            }
        }


    }

    private static void collectProductsFromVisat(Product selectedProduct, List<Product> consideredProductList) {
        Product[] allProducts = VisatApp.getApp().getProductManager().getProducts();
        consideredProductList.add(selectedProduct);
        for (Product product : allProducts) {
            if (selectedProduct != product && NoiseReductionPresenter.shouldConsiderProduct(selectedProduct, product)) {
                if (!containsProduct(consideredProductList, product.getFileLocation())) {
                    consideredProductList.add(product);
                }
            }
        }
    }

    private static boolean containsProduct(List<Product> consideredProductList, File fileLocation) {
        for (Product currentProduct : consideredProductList) {
            if (currentProduct.getFileLocation().equals(fileLocation)) {
                return true;
            }
        }
        return false;
    }

}
