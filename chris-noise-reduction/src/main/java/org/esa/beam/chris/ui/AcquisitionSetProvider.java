/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.chris.ui;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Acquisition set provider.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class AcquisitionSetProvider {

    public Product[] getAcquisitionSet(AppContext appContext) {
        final SortedSet<Product> acquisitionSet = new TreeSet<Product>(
                new Comparator<Product>() {
                    @Override
                    public final int compare(Product p, Product q) {
                        return p.getName().compareTo(q.getName());
                    }
                });

        final Product selectedProduct = appContext.getSelectedProduct();
        final NoiseReductionProductFilter sourceProductFilter = new NoiseReductionProductFilter();
        if (!sourceProductFilter.accept(selectedProduct)) {
            return new Product[0];
        }

        final AcquisitionSetProductFilter productFilter = new AcquisitionSetProductFilter(selectedProduct);
        for (final Product product : appContext.getProductManager().getProducts()) {
            if (productFilter.accept(product)) {
                acquisitionSet.add(product);
            }
        }

        final File selectedFile = selectedProduct.getFileLocation();
        if (selectedFile != null) {
            final File parent = selectedFile.getParentFile();

            if (parent != null && parent.isDirectory()) {
                final File[] files = parent.listFiles(
                        new AcquisitionSetFileFilter(selectedProduct.getFileLocation()));

                search:
                for (final File file : files) {
                    try {
                        for (final Product product : acquisitionSet) {
                            if (file.equals(product.getFileLocation())) {
                                continue search;
                            }
                        }
                        final Product product = ProductIO.readProduct(file);
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
