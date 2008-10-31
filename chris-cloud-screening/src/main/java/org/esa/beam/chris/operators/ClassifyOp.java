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
package org.esa.beam.chris.operators;

import org.esa.beam.chris.operators.internal.ClassOpImage;
import org.esa.beam.cluster.EMCluster;
import org.esa.beam.cluster.IndexFilter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
@OperatorMetadata(alias = "chris.Classify",
                  version = "1.0",
                  authors = "Ralf Quast",
                  copyright = "(c) 2008 by Brockmann Consult",
                  description = "Classifies features extracted from TOA reflectances.",
                  internal = true)
public class ClassifyOp extends Operator {

    private static final IndexFilter NO_FILTERING = new IndexFilter() {
        @Override
        public boolean accept(int index) {
            return true;
        }
    };

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;
    @Parameter
    private String[] sourceBandNames;
    @Parameter
    private EMCluster[] clusters;
    
    @Override
    public void initialize() throws OperatorException {
        try {
            final int w = sourceProduct.getSceneRasterWidth();
            final int h = sourceProduct.getSceneRasterHeight();

            targetProduct = new Product(sourceProduct.getName() + "_CLASS",
                                        sourceProduct.getProductType() + "_CLASS", w, h);

            final Band classBand = new Band("class_indices", ProductData.TYPE_INT8, w, h);
            classBand.setDescription("Class indices");
            targetProduct.addBand(classBand);

            final IndexCoding indexCoding = new IndexCoding("Class indices");
            for (int i = 0; i < clusters.length; i++) {
                indexCoding.addIndex("class_" + (i + 1), i, "Class label");
            }
            targetProduct.getIndexCodingGroup().add(indexCoding);
            classBand.setSampleCoding(indexCoding);
            classBand.setSourceImage(ClassOpImage.createImage(sourceProduct, sourceBandNames, clusters, NO_FILTERING));
        } catch (Throwable e) {
            throw new OperatorException(e);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ClassifyOp.class);
        }
    }
}
