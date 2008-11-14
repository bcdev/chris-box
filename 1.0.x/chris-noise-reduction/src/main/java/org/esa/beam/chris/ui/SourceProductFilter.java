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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Product filter for CHRIS noise reduction source products.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class SourceProductFilter implements ProductFilter {
    static final List<String> PRODUCT_TYPE_LIST;

    static {
        PRODUCT_TYPE_LIST = new ArrayList<String>(7);
        Collections.addAll(PRODUCT_TYPE_LIST,
                           "CHRIS_M1", "CHRIS_M2", "CHRIS_M3", "CHRIS_M30", "CHRIS_M3A", "CHRIS_M4", "CHRIS_M5");
    }

    @Override
    public boolean accept(Product product) {
        return product != null && PRODUCT_TYPE_LIST.contains(product.getProductType());
    }
}
