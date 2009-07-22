package org.esa.beam.chris.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;

/**
 * Filters CHRIS/Proba products suitable for a geometric correction.
 *
 * @author Ralf Quast
 * @version $Revision $ $Date $
 * @since CHRIS-Box 1.1
 */
class GeometricCorrectionProductFilter implements ProductFilter {

    @Override
    public boolean accept(Product product) {
        return product.getProductType().matches("CHRIS_M[012345].*");
    }

}
