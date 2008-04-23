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
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.IntMap;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;

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
        final Product targetProduct = new Product(name, type, width, height);

        final Band[] sourceBands = sourceProduct.getBands();
        for (int i = 0; i < sourceBands.length; ++i) {
            final Band sourceBand = sourceBands[i];

            ProbabilityImageBand targetBand = new ProbabilityImageBand(sourceBand.getName(),
                    sourceBand.getDataType(), width, height, sourceBands, i);
            targetBand.setDescription(sourceBand.getDescription());
            targetProduct.addBand(targetBand);
            targetBand.update(backgroundClusterIndexes);
        }
        final MembershipImageBand membershipBand = new MembershipImageBand("membership_mask", ProductData.TYPE_INT8,
                width, height, targetProduct.getBands());
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
        membershipBand.update();

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


    public static class MembershipImageBand extends Band {
        private final Band[] sourceBands;

        public MembershipImageBand(String name, int dataType, int width, int height, Band[] sourceBands) {
            super(name, dataType, width, height);

            this.sourceBands = sourceBands;
        }

        public void update() {
            setImage(ClusterMapOpImage.create(this, sourceBands));
            // todo - remove this later, we don't want buffers anymore!
            if (hasRasterData()) {
                try {
                    readRasterDataFully();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        @Override
        public void readRasterData(int x, int y, int w, int h, ProductData data, ProgressMonitor pm) throws IOException {
            final Rectangle rectangle = new Rectangle(x, y, w, h);
            final RenderedImage image = getImage();
            final Raster raster = image.getData(rectangle);

            raster.getDataElements(x, y, w, h, data.getElems());
        }
    }

    public static class ProbabilityImageBand extends Band {

        private final Band[] sourceBands;
        private final int correspondingBandIndex;

        public ProbabilityImageBand(String name, int dataType, int width, int height, Band[] sourceBands,
                                    int correspondingBandIndex) {
            super(name, dataType, width, height);
            this.sourceBands = sourceBands;
            this.correspondingBandIndex = correspondingBandIndex;
        }

        // todo - generify or remove parameter in order to make this an interface method
        public void update(int[] backgroundBandIndexes) {
            setImage(ClusterProbabilityOpImage.create(this, sourceBands, correspondingBandIndex, backgroundBandIndexes));
            // todo - remove this later, we don't want buffers anymore!
            if (hasRasterData()) {
                try {
                    readRasterDataFully();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        @Override
        public void readRasterData(int x, int y, int w, int h, ProductData data, ProgressMonitor pm) throws IOException {
            final Rectangle rectangle = new Rectangle(x, y, w, h);
            final RenderedImage image = getImage();
            final Raster raster = image.getData(rectangle);

            raster.getDataElements(x, y, w, h, data.getElems());
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(MakeClusterMapOp.class);
        }
    }
}
