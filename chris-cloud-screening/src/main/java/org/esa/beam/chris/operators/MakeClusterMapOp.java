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

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.IntMap;
import org.esa.beam.util.jai.RasterDataNodeOpImage;

import javax.media.jai.ImageLayout;
import java.awt.Color;
import java.awt.image.RenderedImage;

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

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;
    @Parameter
    private int[] backgroundClusterIndexes;

    @Override
    public void initialize() throws OperatorException {
        int width = sourceProduct.getSceneRasterWidth();
        int height = sourceProduct.getSceneRasterHeight();
        final String name = sourceProduct.getName().replace("_CLU", "_MAP");
        final String type = sourceProduct.getProductType().replace("_CLU", "_MAP");
        targetProduct = new Product(name, type, width, height);

        try {
            final Band[] sourceBands = sourceProduct.getBands();
            final Band[] probBands = new Band[sourceBands.length];
            for (int i = 0; i < sourceBands.length; ++i) {
                final Band sourceBand = sourceBands[i];

                Band targetBand = new ImageBand(sourceBand.getName(), sourceBand.getDataType(), width, height);
                targetBand.setDescription(sourceBand.getDescription());
                targetProduct.addBand(targetBand);
                probBands[i] = targetBand;
                ImageLayout imageLayout = RasterDataNodeOpImage.createSingleBandedImageLayout(targetBand);
                RenderedImage image = ClusterProbabilityOpImage.create(imageLayout,
                                                                       sourceBands, i,
                                                                       backgroundClusterIndexes);
                targetBand.setImage(image);
            }
            final Band membershipBand = new ImageBand("membership_mask", ProductData.TYPE_INT8, width, height);
            membershipBand.setDescription("Cluster membership mask");
            targetProduct.addBand(membershipBand);

            final IndexCoding indexCoding = new IndexCoding("clusters");
            for (int i = 0; i < sourceBands.length; i++) {
                indexCoding.addIndex("cluster_" + (i + 1), i, "Cluster label");
            }
            ImageInfo imageInfo = createIndexedImageInfo(indexCoding);
            targetProduct.getIndexCodingGroup().add(indexCoding);
            membershipBand.setSampleCoding(indexCoding);
            membershipBand.setImageInfo(imageInfo);
            ImageLayout imageLayout = RasterDataNodeOpImage.createSingleBandedImageLayout(membershipBand);
            membershipBand.setImage(ClusterMapOpImage.create(imageLayout, probBands));
        } catch (Throwable t) {
            t.printStackTrace();
        }

        setTargetProduct(targetProduct);
    }

    private ImageInfo createIndexedImageInfo(IndexCoding indexCoding) {
        final MetadataAttribute[] attributes = indexCoding.getAttributes();
        IntMap sampleToIndexMap = new IntMap();
        int sampleMin = 0;
        int sampleMax = attributes.length;
        final ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[attributes.length];
        for (int index = 0; index < attributes.length; index++) {
            MetadataAttribute attribute = attributes[index];
            final int sample = attribute.getData().getElemInt();
            sampleToIndexMap.putValue(sample, index);
            double t = (index + 1.0) / attributes.length;
            points[index] = new ColorPaletteDef.Point(sample,
                    new Color((float) (0.5 + 0.5 * Math.sin(Math.PI / 3. + t * 4. * Math.PI)),
                            (float) (0.5 + 0.5 * Math.sin(Math.PI / 2. + t * 2. * Math.PI)),
                            (float) (0.5 + 0.5 * Math.sin(Math.PI / 4. + t * 3. * Math.PI))),
                    attribute.getName());
        }
        final ColorPaletteDef def = new ColorPaletteDef(points, true);
        final ImageInfo imageInfo = new ImageInfo(sampleMin, sampleMax, null, def);
        imageInfo.setSampleToIndexMap(sampleToIndexMap);
        return imageInfo;
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(MakeClusterMapOp.class);
        }
    }
}
