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
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    public void initialize() throws OperatorException {
        int width = sourceProduct.getSceneRasterWidth();
        int height = sourceProduct.getSceneRasterHeight();
        final String name = sourceProduct.getName().replace("_CLU", "_MAP");
        final String type = sourceProduct.getProductType().replace("_CLU", "_MAP");
        final Product targetProduct = new Product(name, type, width, height);

        final List<Band> bandList = new ArrayList<Band>();
        int bandId = 0;
        for (final Band band : sourceProduct.getBands()) {
            if (band.getName().startsWith("probability")) {
                bandList.add(band);
                ProbabilityImageBand probBand = new ProbabilityImageBand(band.getName(), band.getDataType(), width, height, bandId, sourceProduct.getBands());
                probBand.setDescription("Cluster membership mask");
                targetProduct.addBand(probBand);
                probBand.update(new int[]{});
                bandId++;
            }
        }
        final MembershipImageBand membershipBand = new MembershipImageBand("membership_mask", ProductData.TYPE_INT8,
                width, height, targetProduct.getBands());
        membershipBand.setDescription("Cluster membership mask");
        targetProduct.addBand(membershipBand);

        membershipBand.update();

        setTargetProduct(targetProduct);
    }

    public static class MembershipImageBand extends Band {
        private final Band[] sourceBands;

        public MembershipImageBand(String name, int dataType, int width, int height, Band[] sourceBands) {
            super(name, dataType, width, height);

            this.sourceBands = sourceBands;
        }

        public void update() {
            final RenderedImage image = ClusterMapOpImage.create(this, sourceBands);
            setImage(image);
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

        private final int bandId;
        private final Band[] probabilityBands;

        public ProbabilityImageBand(String name, int dataType, int width, int height, int bandId, Band[] probabilityBands) {
            super(name, dataType, width, height);
            this.bandId = bandId;
            this.probabilityBands = probabilityBands;
        }

        public void update(int[] redistributeBandIds) {
            final RenderedImage image = ClusterProbabilityOpImage.create(this, probabilityBands, bandId, redistributeBandIds);
            setImage(image);
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