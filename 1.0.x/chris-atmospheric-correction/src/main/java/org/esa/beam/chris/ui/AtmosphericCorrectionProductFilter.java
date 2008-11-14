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

import org.esa.beam.chris.util.OpUtils;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.datamodel.Band;

/**
 * Filters CHRIS/Proba products suitable for an atmospheric correction.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.5
 */
class AtmosphericCorrectionProductFilter implements ProductFilter {
    @Override
    public boolean accept(Product product) {
        if (!product.getProductType().matches("CHRIS_M[12345].*_NR")) {
            return false;
        }

        final Band[] radianceBands = OpUtils.findBands(product, "radiance_");
        final Band[] maskBands = OpUtils.findBands(product, "mask_");

        return radianceBands.length >= 18 && radianceBands.length == maskBands.length;
    }
}
