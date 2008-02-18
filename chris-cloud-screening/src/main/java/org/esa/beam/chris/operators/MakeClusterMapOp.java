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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.chris.operators.internal.Clusterer;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.awt.Rectangle;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
@OperatorMetadata(alias = "chris.MakeClusterMap",
        version = "1.0",
        authors = "Ralf Quast",
        copyright = "(c) 2008 by Brockmann Consult",
        description = "Makes the cluster membership map for clusters of features extracted from TOA reflectances.")
public class MakeClusterMapOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    private transient Band membershipBand;

    @Override
    public void initialize() throws OperatorException {
        int width = sourceProduct.getSceneRasterWidth();
        int height = sourceProduct.getSceneRasterHeight();
        final String name = sourceProduct.getName().replace("_CLU", "_MAP");
        final String type = sourceProduct.getProductType().replace("_CLU", "_MAP");
        targetProduct = new Product(name, type, width, height);
//        targetProduct.setPreferredTileSize(width, height);

        membershipBand = targetProduct.addBand("cluster_membership_map", ProductData.TYPE_INT8);
        membershipBand.setDescription("Cluster membership map");
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final Rectangle targetRectangle = targetTile.getRectangle();
        final int sceneWidth = sourceProduct.getSceneRasterWidth();

        final int[] mask = createMembershipMask(null);

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                targetTile.setSample(x, y, mask[y * sceneWidth + x]);
            }
        }
    }

    private static int[] createMembershipMask(Clusterer.Cluster[] clusters) {
        final int[] mask = new int[clusters[0].getPoints().length];

        for (int i = 0; i < mask.length; ++i) {
            final double[] d = new double[clusters.length];
            for (int k = 0; k < clusters.length; ++k) {
                d[k] = clusters[k].getPosteriorProbabilities()[i];
            }
            mask[i] = indexMax(d);
        }

        return mask;
    }

    private static int indexMax(double[] values) {
        int index = 0;
        for (int i = 1; i < values.length; ++i) {
            if (values[i] > values[index]) {
                index = i;
            }
        }

        return index;
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(MakeClusterMapOp.class);
        }
    }
}
