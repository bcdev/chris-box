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

import org.esa.beam.chris.operators.internal.ClusterMapOpImage;
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
@OperatorMetadata(alias = "chris.MakeClusterMap2",
        version = "1.0",
        authors = "Ralf Quast",
        copyright = "(c) 2008 by Brockmann Consult",
        description = "Makes the cluster map for clusters of cloud features extracted from TOA reflectances.",
        internal = true)
public class MakeClusterMapOp extends Operator {

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

            targetProduct = new Product(sourceProduct.getName() + "_CLM", sourceProduct.getProductType() + "_CLM", w, h);

            final Band[] sourceBands = new Band[sourceBandNames.length];
            for (int i = 0; i < sourceBandNames.length; ++i) {
                sourceBands[i] = sourceProduct.getBand(sourceBandNames[i]);
            }

            final Band targetBand = new ImageBand("class_indices", ProductData.TYPE_INT8, w, h);
            targetBand.setDescription("Class indices");
            targetProduct.addBand(targetBand);

            final IndexCoding indexCoding = new IndexCoding("Class indices");
            for (int i = 0; i < clusters.length; i++) {
                indexCoding.addIndex("class_" + (i + 1), i, "Cluster label");
            }
            targetProduct.getIndexCodingGroup().add(indexCoding);
            targetBand.setSampleCoding(indexCoding);

            final IndexFilter indexFilter = new IndexFilter() {
                @Override
                public boolean accept(int index) {
                    return true;
                }
            };

            targetBand.setImage(ClusterMapOpImage.createImage(sourceBands, clusters, indexFilter));
        } catch (Throwable e) {
            throw new OperatorException(e);
        }

        setTargetProduct(targetProduct);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MakeClusterMapOp.class);
        }
    }
}
