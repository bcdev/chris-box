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

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;

/**
 * Acquisition set product filter.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class AcquisitionSetProductFilter implements ProductFilter {
    private final AcquisitionSetFilter acquisitionSetFilter;
    private final String referenceProductType;

    public AcquisitionSetProductFilter(Product referenceProduct) {
        this.acquisitionSetFilter = new AcquisitionSetFilter(referenceProduct.getName());
        this.referenceProductType = referenceProduct.getProductType();
    }

    @Override
    public boolean accept(Product product) {
        return product != null &&
                referenceProductType.equals(product.getProductType()) &&
                acquisitionSetFilter.accept(product.getName());
    }
}
