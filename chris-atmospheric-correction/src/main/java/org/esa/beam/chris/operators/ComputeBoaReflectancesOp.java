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
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;

/**
 * Operator for performing the CHRIS atmospheric correction.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
@OperatorMetadata(alias = "chris.ComputeBoaReflectances",
                  version = "1.0",
                  authors = "Ralf Quast",
                  copyright = "(c) 2008 by Brockmann Consult",
                  description = "Computes surface reflectances from a CHRIS/PROBA RCI with cloud product.")
public class ComputeBoaReflectancesOp extends Operator {

    private static final double REFL_SCALING_FACTOR = 10000.0;

    @SourceProduct
    private Product sourceProduct;

    @Parameter(defaultValue = "false")
    private boolean performSpectralPolishing;

    @Parameter(defaultValue = "false")
    private boolean performAdjacencyCorrection;

    @Parameter(defaultValue = "0",
               interval = "[0.0, 1.0]",
               description = "Value of the aerosol optical thickness (AOT) at 550 nm. If nonzero, AOT retrieval is disabled.")
    private double aot550;

    @Parameter(defaultValue = "0.0",
               interval = "[0.0, 5.0]",
               description = "Initial water vapour (WV) column guess used for WV retrieval.")
    private double wvIni;

    @Parameter(defaultValue = "false",
               description = "If 'false' no water vapour map is generated for modes 1, 3 and 5.")
    private boolean wvMap;

    @Parameter(defaultValue = "0.05",
               description = "Cloud probability threshold for generating the cloud mask.")
    private double cloudProbabilityThreshold;

    private transient Band[] radianceBands;
    private transient Band[] maskBands;
    private transient Band cloudProbability;

    private transient Band[] reflBands;

    private transient RenderedImage saturationMask;
    private transient ModtranLookupTable lut;

    @Override
    public void initialize() throws OperatorException {
        // get source bands
        radianceBands = OpUtils.findBands(sourceProduct, "radiance");
        maskBands = OpUtils.findBands(sourceProduct, "mask");
        cloudProbability = sourceProduct.getBand("cloud_product");

        // saturation mask
        saturationMask = SaturationMaskOpImage.createImage(maskBands);

        final Product targetProduct = createTargetProduct();
        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle,
                                 ProgressMonitor pm) throws OperatorException {
        // Read MODTRAN lookup table if not already done
        synchronized (this) {
            if (lut == null) {
                try {
                    lut = new ModtranLookupTableReader().readLookupTable();
                } catch (IOException e) {
                    throw new OperatorException(e.getMessage());
                }
            }
        }

        acMode24(targetTileMap, targetRectangle, pm);
    }

    @Override
    public void dispose() {
        lut = null;

        saturationMask = null;

        maskBands = null;
        radianceBands = null;
    }

    private Product createTargetProduct() {
        final Product targetProduct = new Product("CHRIS_BOA_REFL", "CHRIS_BOA_REFL",
                                                  sourceProduct.getSceneRasterWidth(),
                                                  sourceProduct.getSceneRasterHeight());
        // set start and stop times
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        // add reflectance bands
        for (int i = 0; i < radianceBands.length; i++) {
            final Band radianceBand = radianceBands[i];
            final Band reflBand = new Band(radianceBand.getName().replaceAll("radiance", "refl"),
                                           ProductData.TYPE_INT16,
                                           radianceBand.getRasterWidth(),
                                           radianceBand.getRasterHeight());

            reflBand.setDescription(MessageFormat.format("Surface reflectance for spectral band {0}", i + 1));
            reflBand.setUnit("dl");
            reflBand.setScalingFactor(REFL_SCALING_FACTOR);
            reflBand.setValidPixelExpression(radianceBand.getValidPixelExpression());
            reflBand.setSpectralBandIndex(radianceBand.getSpectralBandIndex());
            reflBand.setSpectralWavelength(radianceBand.getSpectralWavelength());
            reflBand.setSpectralBandwidth(radianceBand.getSpectralBandwidth());
            // todo - set solar flux ?

            targetProduct.addBand(reflBand);
            reflBands[i] = reflBand;
        }

        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        // todo - copy mask bands, cloud product

        ProductUtils.copyBitmaskDefs(sourceProduct, targetProduct);
        ProductUtils.copyMetadata(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());

        return targetProduct;
    }

    private void acMode24(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) {
        final int tileWork = performAdjacencyCorrection ? targetRectangle.height * 2 : targetRectangle.height;
        pm.beginTask("Computing surface reflectances...", tileWork * reflBands.length);

        try {

            final Raster saturationMaskTile = saturationMask.getData(targetRectangle);
            final Tile cloudProbabilityTile = getSourceTile(this.cloudProbability, targetRectangle, pm);

            for (int i = 0; i < radianceBands.length; i++) {
                final Tile targetTile = targetTileMap.get(radianceBands[i]);

                for (final Tile.Pos pos : cloudProbabilityTile) {
                    if (pos.x == targetRectangle.x) {
                        checkForCancelation(pm);
                    }

                    // todo - invert lambertian equation

                    if (pos.x == targetRectangle.x) {
                        pm.worked(1);
                    }
                }
            }
            // todo - polish spikes for O2-A and O2-B absorption features
            if (performAdjacencyCorrection) {
                for (int i = 0; i < radianceBands.length; i++) {
                    // todo - adjacency correction
                }
            }
        } finally {
            pm.done();
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ComputeBoaReflectancesOp.class);
        }
    }
}
