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
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Noise reduction action.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class NoiseReductionAction extends ExecCommand {

    static List<String> CHRIS_TYPES;

    static {
        CHRIS_TYPES = new ArrayList<String>();
        Collections.addAll(CHRIS_TYPES, "CHRIS_M1", "CHRIS_M2", "CHRIS_M3", "CHRIS_M3A", "CHRIS_M4", "CHRIS_M5");
    }

    @Override
    public void actionPerformed(CommandEvent commandEvent) {
        final Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        final Product[] acquisitionSet = getAcquisitionSet(selectedProduct);

        final NoiseReductionPresenter presenter =
                new NoiseReductionPresenter(acquisitionSet, new AdvancedSettingsPresenter());
        final ModalDialog dialog =
                new ModalDialog(VisatApp.getApp().getMainFrame(),
                                "CHRIS Noise Reduction",
                                ModalDialog.ID_OK_CANCEL_HELP,
                                "chrisNoiseReductionTool");
        dialog.setContent(new NoiseReductionForm(presenter));

        if (dialog.show() == ModalDialog.ID_OK) {
            for (final Product product : acquisitionSet) {
                if (!(VisatApp.getApp().getProductManager().contains(product) || presenter.isListed(product))) {
                    product.dispose();
                }
            }

            try {
                performNoiseReduction(presenter);
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
        final Product selectedProduct = VisatApp.getApp().getSelectedProduct();
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

    private static void performNoiseReduction(NoiseReductionPresenter presenter)
            throws OperatorException {
        final Product factors =
                GPF.createProduct("chris.ComputeDestripingFactors",
                                  presenter.getDestripingParameterMap(),
                                  presenter.getListedProducts());

        VisatApp.getApp().addProduct(factors);

        final HashMap<String, Product> productsMap = new HashMap<String, Product>(5);
        productsMap.put("factors", factors);

        for (final Product sourceProduct : presenter.getCheckedProducts()) {
            productsMap.put("input", sourceProduct);

            final Product destriped =
                    GPF.createProduct("chris.ApplyDestripingFactors",
                                      new HashMap<String, Object>(0),
                                      productsMap);

            final Product targetProduct =
                    GPF.createProduct("chris.CorrectDropouts",
                                      presenter.getDropoutCorrectionParameterMap(),
                                      destriped);
            VisatApp.getApp().addProduct(targetProduct);
        }
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
