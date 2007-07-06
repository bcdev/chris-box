package org.esa.beam.chris.operators;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.chris.ChrisConstants;
import org.esa.beam.dataio.chris.internal.DropoutCorrection;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.AbstractOperatorSpi;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.Rectangle;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.text.MessageFormat;

public class DropoutCorrectionOp extends AbstractOperator {

    @SourceProduct(alias = "input")
    Product sourceProduct;
    @TargetProduct
    Product targetProduct;

    @Parameter(defaultValue = "5", interval = "(1, 8)")
    private int neighboringBandCount;

    @Parameter(alias = "neighborhoodType", defaultValue = "4", valueSet = {"2", "4", "8"})
    private int type;

    private DropoutCorrection dropoutCorrection;
    private int spectralBandCount;

    private Band[] sourceRciBands;
    private Band[] sourceMaskBands;
    private Band[] targetRciBands;
    private Band[] targetMaskBands;

    public DropoutCorrectionOp(OperatorSpi operatorSpi) {
        super(operatorSpi);
    }

    @Override
    protected Product initialize(ProgressMonitor progressMonitor) throws OperatorException {
        assertValidity(sourceProduct);

        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        ProductUtils.copyElementsAndAttributes(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());

        spectralBandCount = getAnnotationInt(sourceProduct, ChrisConstants.ATTR_NAME_NUMBER_OF_BANDS);

        sourceRciBands = new Band[spectralBandCount];
        sourceMaskBands = new Band[spectralBandCount];
        targetRciBands = new Band[spectralBandCount];
        targetMaskBands = new Band[spectralBandCount];

        for (int i = 0; i < spectralBandCount; ++i) {
            final String dataBandName = new StringBuilder("radiance_").append(i + 1).toString();
            final String maskBandName = new StringBuilder("mask_").append(i + 1).toString();

            sourceRciBands[i] = sourceProduct.getBand(dataBandName);
            sourceMaskBands[i] = sourceProduct.getBand(maskBandName);

            if (sourceRciBands[i] == null) {
                throw new OperatorException(MessageFormat.format("could not find band {0}", dataBandName));
            }
            if (sourceMaskBands[i] == null) {
                throw new OperatorException(MessageFormat.format("could not find band {0}", maskBandName));
            }

            targetRciBands[i] = ProductUtils.copyBand(dataBandName, sourceProduct, targetProduct);
            targetMaskBands[i] = ProductUtils.copyBand(maskBandName, sourceProduct, targetProduct);

            final FlagCoding flagCoding = sourceMaskBands[i].getFlagCoding();
            if (flagCoding != null) {
                targetMaskBands[i].setFlagCoding(targetProduct.getFlagCoding(flagCoding.getName()));
            }
        }
        ProductUtils.copyBitmaskDefs(sourceProduct, targetProduct);

        dropoutCorrection = new DropoutCorrection();
        // todo -- consider type

        return targetProduct;
    }

    @Override
    public void computeAllBands(Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        try {
            pm.beginTask("computing dropout correction...", spectralBandCount);
            for (int i = 0; i < spectralBandCount; ++i) {
                computeCorrection(i, targetRectangle);
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private void computeCorrection(int bandIndex, Rectangle targetRectangle) throws OperatorException {
        final int minBandIndex = max(bandIndex - neighboringBandCount, 0);
        final int maxBandIndex = min(bandIndex + neighboringBandCount, spectralBandCount);
        final int bandCount = minBandIndex - maxBandIndex + 1;

        final Rectangle sourceRectangle = createSourceRectangle(targetRectangle);
        final int[][] sourceRciData = new int[bandCount][];
        final short[][] sourceMaskData = new short[bandCount][];

        for (int i = minBandIndex, j = 1; i < maxBandIndex; ++i) {
            if (i != bandIndex) {
                sourceRciData[j] = getRasterDataInt(sourceRciBands[i], sourceRectangle);
                sourceMaskData[j] = getRasterDataShort(sourceMaskBands[i], sourceRectangle);
                ++j;
            } else {
                sourceRciData[0] = getRasterDataInt(sourceRciBands[i], sourceRectangle);
                sourceMaskData[0] = getRasterDataShort(sourceMaskBands[i], sourceRectangle);
            }
        }
        final int[] targetRciData = getRasterDataInt(targetRciBands[bandIndex], targetRectangle);
        final short[] targetMaskData = getRasterDataShort(targetMaskBands[bandIndex], targetRectangle);

        dropoutCorrection.compute(sourceRciData, sourceMaskData, sourceRectangle.width, sourceRectangle.height,
                                  new Rectangle(targetRectangle.x - sourceRectangle.x,
                                                targetRectangle.y - sourceRectangle.y,
                                                targetRectangle.width, targetRectangle.height),
                                  targetRciData, targetMaskData, 0, 0, targetRectangle.width);
    }

    private Rectangle createSourceRectangle(Rectangle targetRectangle) {
        int x = targetRectangle.x;
        int y = targetRectangle.y;
        int width = targetRectangle.width;
        int height = targetRectangle.height;

        if (x > 0) {
            x -= 1;
            width += 1;
        }
        if (x + width < targetProduct.getSceneRasterWidth()) {
            width += 1;
        }
        if (y > 0) {
            y -= 1;
            height += 1;
        }
        if (y + height < targetProduct.getSceneRasterHeight()) {
            height += 1;
        }

        return new Rectangle(x, y, width, height);
    }

    private int[] getRasterDataInt(Band band, Rectangle rectangle) throws OperatorException {
        return (int[]) getRaster(band, rectangle).getDataBuffer().getElems();
    }

    private short[] getRasterDataShort(Band band, Rectangle rectangle) throws OperatorException {
        return (short[]) getRaster(band, rectangle).getDataBuffer().getElems();
    }

    private static void assertValidity(Product product) throws OperatorException {
        try {
            getAnnotationString(product, ChrisConstants.ATTR_NAME_CHRIS_MODE);
            getAnnotationString(product, ChrisConstants.ATTR_NAME_CHRIS_TEMPERATURE);
        } catch (OperatorException e) {
            throw new OperatorException(MessageFormat.format(
                    "product ''{0}'' is not a CHRIS product", product.getName()));
        }
    }

    /**
     * Returns a CHRIS annotation for a product of interest.
     *
     * @param product the product of interest.
     * @param name    the name of the CHRIS annotation.
     *
     * @return the annotation or {@code null} if the annotation could not be found.
     *
     * @throws OperatorException if the annotation could not be read.
     */
    // todo -- move
    private static String getAnnotationString(Product product, String name) throws OperatorException {
        final MetadataElement element = product.getMetadataRoot().getElement(ChrisConstants.MPH_NAME);

        if (element == null) {
            throw new OperatorException(MessageFormat.format("could not get CHRIS annotation ''{0}''", name));
        }
        return element.getAttributeString(name, null);
    }

    // todo -- move
    private static int getAnnotationInt(Product product, String name) throws OperatorException {
        final String string = getAnnotationString(product, name);

        try {
            return Integer.parseInt(string);
        } catch (Exception e) {
            throw new OperatorException(MessageFormat.format("could not parse CHRIS annotation ''{0}''", name));
        }
    }


    public static class Spi extends AbstractOperatorSpi {

        public Spi() {
            super(DestripingOp.class, "DropoutCorrection");
            // todo -- set description etc.
        }
    }

}
