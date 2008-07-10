package org.esa.beam.chris.operators;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;

import java.awt.*;
import java.io.IOException;

public class AccumulateOpTest extends TestCase {

    private static final Dimension PRODUCT_DIMENSION = new Dimension(2, 2);
    private static final int TEST_PRODUCT_BAND_COUNT = 5;
    private static final double[][] TEST_PRODUCT_BAND_DATA = new double[][]{
            {0.10, 0.20, 0.30, 0.40},
            {0.15, 0.48, 0.12, 0.03},
            {0.14, 0.10, 0.08, 0.06},
            {0.20, 0.02, 0.05, 0.01},
            {0.41, 0.20, 0.45, 0.50},
    };
    private static final double[] EXPECTED_SUMS_WITHOUT_THRESHOLD_APPLIED = new double[]{
            0.51, 0.40, 0.75, 0.90
    };
    private static final double[] EXPECTED_SUMS_WITH_THRESHOLD_APPLIED = new double[]{
            1.0, 0.0, 1.0, 1.0
    };
    private static final double PROBABILITY_ACCURACY = 1.0e-4;


    public void testInitializeOK() {
        Product clusterDummy = createTestProduct(PRODUCT_DIMENSION, TEST_PRODUCT_BAND_COUNT);
        AccumulateOp op = new AccumulateOp(clusterDummy, new String[]{"band_0", "band_4"}, "sum", true);
        op.getTargetProduct();
    }

    public void testInitializeWithoutReflProduct() {
        Product clusterDummy = createTestProduct(PRODUCT_DIMENSION, TEST_PRODUCT_BAND_COUNT);
        AccumulateOp op = new AccumulateOp(clusterDummy, new String[]{"band_0", "band_4"}, "sum", true);
        op.getTargetProduct();
    }

    public void testIllegalBandName() {
        Product clusterDummy1 = createTestProduct(PRODUCT_DIMENSION, TEST_PRODUCT_BAND_COUNT);
        AccumulateOp op = new AccumulateOp(clusterDummy1, new String[]{"band_0", "band_7"}, "", true);
        try {
            op.getTargetProduct();
            fail("OperatorException expected: Band not found");
        } catch (OperatorException expected) {
            // expected
        }
    }

    public void testTargetProduct() {
        Product clusterDummy = createTestProduct(PRODUCT_DIMENSION, TEST_PRODUCT_BAND_COUNT);
        AccumulateOp op = new AccumulateOp(clusterDummy, new String[]{"band_0", "band_4"}, "sum", true);

        final Product targetProduct = op.getTargetProduct();
        assertNull(targetProduct.getGeoCoding());
        assertEquals(1, targetProduct.getNumBands());
        final Band sumBand = targetProduct.getBand("sum");
        assertNotNull(sumBand);
        assertEquals(ProductData.TYPE_FLOAT64, sumBand.getDataType());
    }

    public void testCloudProbabilityBandWithThresholdApplied() throws IOException {
        Product clusterDummy = createTestProduct(PRODUCT_DIMENSION, TEST_PRODUCT_BAND_COUNT);
        AccumulateOp op = new AccumulateOp(clusterDummy, new String[]{"band_0", "band_4"}, "sum", true);

        final Product targetProduct = op.getTargetProduct();
        final Band sumBand = targetProduct.getBand("sum");
        double[] samples = new double[PRODUCT_DIMENSION.width * PRODUCT_DIMENSION.height];
        sumBand.readPixels(0, 0, PRODUCT_DIMENSION.width, PRODUCT_DIMENSION.height, samples);

        for (int i = 0; i < EXPECTED_SUMS_WITH_THRESHOLD_APPLIED.length; i++) {
            final double expectedResult = EXPECTED_SUMS_WITH_THRESHOLD_APPLIED[i];
            final double currentResult = samples[i];
            assertEquals(expectedResult, currentResult, 0.0);
        }
    }

    public void testCloudProbabilityBandWithoutThresholdApplied() throws IOException {
        Product clusterDummy = createTestProduct(PRODUCT_DIMENSION, TEST_PRODUCT_BAND_COUNT);
        AccumulateOp op = new AccumulateOp(clusterDummy, new String[]{"band_0", "band_4"}, "sum", false);

        final Product targetProduct = op.getTargetProduct();
        final Band sumBand = targetProduct.getBand("sum");
        double[] samples = new double[PRODUCT_DIMENSION.width * PRODUCT_DIMENSION.height];
        sumBand.readPixels(0, 0, PRODUCT_DIMENSION.width, PRODUCT_DIMENSION.height, samples);

        for (int i = 0; i < EXPECTED_SUMS_WITHOUT_THRESHOLD_APPLIED.length; i++) {
            final double expectedResult = EXPECTED_SUMS_WITHOUT_THRESHOLD_APPLIED[i];
            final double currentResult = samples[i];
            assertEquals(expectedResult, currentResult, PROBABILITY_ACCURACY);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    // PRIVATE SECTION
    ////////////////////////////////////////////////////////////////////////////////////////

    private Product createTestProduct(Dimension dim, int bandCount) {
        final Product product = new Product("ChrisM3_CLU", "ChrisM3_CLU", dim.width, dim.height);
        for (int i = 0; i < bandCount; i++) {
            final Band band = new Band(String.format("band_%d", i), ProductData.TYPE_FLOAT64,
                    dim.width, dim.height);
            product.addBand(band);
            final ProductData data = band.createCompatibleRasterData();
            data.setElems(TEST_PRODUCT_BAND_DATA[i]);
            band.setSynthetic(true);
            band.setRasterData(data);
        }

        return product;
    }
}
