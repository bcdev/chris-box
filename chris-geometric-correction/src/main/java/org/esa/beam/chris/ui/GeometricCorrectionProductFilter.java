package org.esa.beam.chris.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;

/**
 * Filters CHRIS/Proba products suitable for the geometric correction.
 *
 * @author Ralf Quast
 * @since CHRIS-Box 1.5
 */
class GeometricCorrectionProductFilter implements ProductFilter {

    @Override
    public boolean accept(Product product) {
        return product != null && product.getProductType().matches("CHRIS_M[012345][0A]?(_NR)?(_AC)?");
    }

}
