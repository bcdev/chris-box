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
