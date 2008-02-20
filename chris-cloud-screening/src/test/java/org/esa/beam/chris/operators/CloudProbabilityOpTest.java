package org.esa.beam.chris.operators;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;

import java.awt.Dimension;
import java.io.IOException;

public class CloudProbabilityOpTest extends TestCase {

    private static final int REFL_BAND_COUNT = 6;
    private static final Dimension PRODUCT_DIMENSION = new Dimension(2, 2);
    private static final int CLUSTER_CLASS_BAND_COUNT = 5;
    private static final double[][] CLUSTER_CLASS_BAND_DATA = new double[][]{
            {0.10, 0.20, 0.30, 0.40},
            {0.15, 0.48, 0.12, 0.03},
            {0.14, 0.10, 0.08, 0.06},
            {0.20, 0.02, 0.05, 0.01},
            {0.41, 0.20, 0.45, 0.50},
    };
    private static final double[] EXPECTED_PROBABILITY_RESULT = new double[]{
            0.7183, 0.9523, 0.9375, 0.9890
    };
    private static final double PROBABILITY_ACCURACY = 1e-4;


    public void testInitializeOK() {
        Product reflDummy = createToaReflSourceProduct(PRODUCT_DIMENSION, REFL_BAND_COUNT);
        Product clusterDummy = createClusterSourceProduct(PRODUCT_DIMENSION, CLUSTER_CLASS_BAND_COUNT);
        CloudProbabilityOp probabilityOp = new CloudProbabilityOp(new int[]{0, 4}, new int[]{1, 2},
                                                                  reflDummy, clusterDummy);
       probabilityOp.getTargetProduct();
    }

    public void testInitializeWithoutReflProduct() {
        Product clusterDummy = createClusterSourceProduct(PRODUCT_DIMENSION, CLUSTER_CLASS_BAND_COUNT);
        CloudProbabilityOp probabilityOp = new CloudProbabilityOp(new int[]{0, 4}, new int[]{1, 2},
                                                                  null, clusterDummy);
        probabilityOp.getTargetProduct();
    }

    public void testSourceProductAreSpatiallyEqual() {
        Product reflDummy = createToaReflSourceProduct(new Dimension(6, 6), REFL_BAND_COUNT);
        Product clusterDummy = createClusterSourceProduct(PRODUCT_DIMENSION, CLUSTER_CLASS_BAND_COUNT);
        CloudProbabilityOp probabilityOp = new CloudProbabilityOp(new int[]{0, 4}, new int[]{1, 2},
                                                                  reflDummy, clusterDummy);
        try {
            probabilityOp.getTargetProduct();
            fail("OperatorException expected: Products are not spatially equal");
        } catch (OperatorException expected) {
            // expected
        }
    }

    public void testIllegalAccumulateIndex() {
        // index out of bound
        Product reflDummy1 = createToaReflSourceProduct(PRODUCT_DIMENSION, REFL_BAND_COUNT);
        Product clusterDummy1 = createClusterSourceProduct(PRODUCT_DIMENSION, CLUSTER_CLASS_BAND_COUNT);
        CloudProbabilityOp probabilityOp = new CloudProbabilityOp(new int[]{145336}, new int[]{3},
                                                                  reflDummy1, clusterDummy1);
        try {
            probabilityOp.getTargetProduct();
            fail("OperatorException expected: Accumulate class index is out of bounds");
        } catch (OperatorException expected) {
            // expected
        }
    }

    public void testIllegalRedistributeIndex() {
        // index out of bound
        Product reflDummy = createToaReflSourceProduct(PRODUCT_DIMENSION, REFL_BAND_COUNT);
        Product clusterDummy = createClusterSourceProduct(PRODUCT_DIMENSION, CLUSTER_CLASS_BAND_COUNT);
        CloudProbabilityOp probabilityOp = new CloudProbabilityOp(new int[]{1, 2}, new int[]{145336},
                                                                  reflDummy, clusterDummy);
        try {
            probabilityOp.getTargetProduct();
            fail("OperatorException expected: Redistribute class index is out of bounds");
        } catch (OperatorException expected) {
            // expected
        }
    }

    public void testTargetProduct() {
        int[] accumulateClassIndices = new int[]{0, 4};
        int[] redistributeClassIndices = new int[]{1, 2};
        Product reflDummy = createToaReflSourceProduct(PRODUCT_DIMENSION, REFL_BAND_COUNT);
        Product clusterDummy = createClusterSourceProduct(PRODUCT_DIMENSION, CLUSTER_CLASS_BAND_COUNT);
        CloudProbabilityOp probabilityOp = new CloudProbabilityOp(accumulateClassIndices, redistributeClassIndices,
                                                                  reflDummy, clusterDummy);

        final Product targetProduct = probabilityOp.getTargetProduct();
        assertNull(targetProduct.getGeoCoding());
        assertEquals(REFL_BAND_COUNT + 1, targetProduct.getNumBands());
        final Band cloudProbBand = targetProduct.getBand("cloud_probability");
        assertNotNull(cloudProbBand);
        assertEquals(ProductData.TYPE_FLOAT32, cloudProbBand.getDataType());
        for (int i = 1; i <= REFL_BAND_COUNT; i++) {
            final String bandName = String.format("reflectance_%d", i);
            assertTrue(String.format("Target product does not contain '%s'", bandName),
                       targetProduct.containsBand(bandName));
        }
    }

    public void testCopyOfToaReflBands() throws IOException {
        int[] accumulateClassIndices = new int[]{0, 4};
        int[] redistributeClassIndices = new int[]{1, 2};
        Product reflSource = createToaReflSourceProduct(PRODUCT_DIMENSION, REFL_BAND_COUNT);
        Product clusterSource = createClusterSourceProduct(PRODUCT_DIMENSION, CLUSTER_CLASS_BAND_COUNT);
        CloudProbabilityOp probabilityOp = new CloudProbabilityOp(accumulateClassIndices, redistributeClassIndices,
                                                                  reflSource, clusterSource);

        final Product targetProduct = probabilityOp.getTargetProduct();
        for (int i = 1; i <= REFL_BAND_COUNT; i++) {
            final Band targetBand = targetProduct.getBand(String.format("reflectance_%d", i));
            final Band sourceBand = reflSource.getBand(targetBand.getName());
            float[] targetSamples = new float[PRODUCT_DIMENSION.width * PRODUCT_DIMENSION.height];
            targetBand.readPixels(0,0, PRODUCT_DIMENSION.width, PRODUCT_DIMENSION.height, targetSamples);

            for (int j = 0; j < targetSamples.length; j++) {
                float targetSample = targetSamples[j];
                final float sourceSample = sourceBand.getRasterData().getElemFloatAt(j);
                assertEquals(sourceSample, targetSample, 1e-6f);
            }
        }
    }

    public void testCloudProbabilityBand() throws IOException {
        int[] accumulateClassIndices = new int[]{0, 4};
        int[] redistributeClassIndices = new int[]{1, 2};
        Product reflDummy = createToaReflSourceProduct(PRODUCT_DIMENSION, REFL_BAND_COUNT);
        Product clusterDummy = createClusterSourceProduct(PRODUCT_DIMENSION, CLUSTER_CLASS_BAND_COUNT);
        CloudProbabilityOp probabilityOp = new CloudProbabilityOp(accumulateClassIndices, redistributeClassIndices,
                                                                  reflDummy, clusterDummy);

        final Product targetProduct = probabilityOp.getTargetProduct();
        final Band probabilityBand = targetProduct.getBand("cloud_probability");
        double[] probalitySamples = new double[PRODUCT_DIMENSION.width * PRODUCT_DIMENSION.height];
        probabilityBand.readPixels(0,0, PRODUCT_DIMENSION.width, PRODUCT_DIMENSION.height, probalitySamples);

        for (int i = 0; i < EXPECTED_PROBABILITY_RESULT.length; i++) {
            final double expectedResult = EXPECTED_PROBABILITY_RESULT[i];
            final double currentResult = probalitySamples[i];
            assertEquals(expectedResult, currentResult, PROBABILITY_ACCURACY);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    // PRIVATE SECTION
    ////////////////////////////////////////////////////////////////////////////////////////

    private Product createClusterSourceProduct(Dimension dim, int numClusterBands) {
        final Product product = new Product("ChrisM3_CLU", "ChrisM3_CLU", dim.width, dim.height);
        for (int i = 0; i < numClusterBands; i++) {
            final Band band = new Band(String.format("probability_%d", i), ProductData.TYPE_FLOAT64,
                                       dim.width, dim.height);
            product.addBand(band);
            final ProductData data = band.createCompatibleRasterData();
            data.setElems(CLUSTER_CLASS_BAND_DATA[i]);
            band.setSynthetic(true);
            band.setRasterData(data);
        }

        final Band maskBand = product.addBand("membership_mask", ProductData.TYPE_INT8);
        final ProductData data = maskBand.createCompatibleRasterData();
        for (int j = 0; j < data.getNumElems(); j++) {
            data.setElemDoubleAt(j, Math.floor(Math.random() * numClusterBands));
        }
        maskBand.setSynthetic(true);
        maskBand.setRasterData(data);
        return product;
    }

    private Product createToaReflSourceProduct(Dimension dim, int numReflBands) {
        final Product product = new Product("ChrisM3_REFL", "ChrisM3_REFL", dim.width, dim.height);
        for (int i = 1; i <= numReflBands; i++) {
            final Band band = product.addBand(String.format("reflectance_%d", i),
                                              ProductData.TYPE_FLOAT32);
            final ProductData data = band.createCompatibleRasterData();
            for (int j = 0; j < data.getNumElems(); j++) {
                data.setElemDoubleAt(j, Math.random());
            }
            band.setSynthetic(true);
            band.setRasterData(data);
        }
        return product;
    }

}
