package org.esa.beam.chris.operators;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
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
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import java.util.*;

@OperatorMetadata(alias = "chris.ComputeCloudProbability",
        version = "1.0",
        authors = "Marco Peters, Marco Zühlke",
        copyright = "(c) 2008 by Brockmann Consult",
        description = "Computes the cloud probability of a pixel.")
public class CloudProbabilityOp extends Operator {
    // todo - turn these constants into parameter to generify this operator
    private static final String BAND_NAME_CLOUD_PROBABILITY = "cloud_probability";
    private static final String PROBABILITY_PREFIX = "probability_";


    @SourceProduct(alias = "cluster")
    private Product clusterProduct;
    @SourceProduct(alias = "toaRefl", optional = true)
    private Product toaReflectanceProduct;

    @TargetProduct(description = "Product containing the accumulated probability.")
    private Product targetProduct;

    @Parameter(alias = "accumulate", description = "Considered classes which are accumulated to a probability.",
            notEmpty = true, notNull = true)
    private int[] accumulateClassIndices;
    @Parameter(alias = "redistribute", description = "Classes which are redistributed to the other classes.",
            notEmpty = true, notNull = true)
    private int[] redistributeClassIndices;

    private Map<Integer, Band> probabilityBandMap;
    private List<Integer> useClassList;


    public CloudProbabilityOp() {
    }

    CloudProbabilityOp(int[] accumulateClassIndices, int[] redistributeClassIndices, Product reflProduct,
                       Product clusterProduct) {
        this.accumulateClassIndices = accumulateClassIndices;
        this.redistributeClassIndices = redistributeClassIndices;
        this.toaReflectanceProduct = reflProduct;
        this.clusterProduct = clusterProduct;
    }

    @Override
    public void initialize() throws OperatorException {

        validateParameter();

        probabilityBandMap = new HashMap<Integer, Band>(clusterProduct.getNumBands());
        useClassList = new ArrayList<Integer>(clusterProduct.getNumBands());
        for (String bandName : clusterProduct.getBandNames()) {
            if (bandName.startsWith(PROBABILITY_PREFIX)) {
                final int bandId = Integer.valueOf(bandName.substring(PROBABILITY_PREFIX.length()));
                probabilityBandMap.put(bandId, clusterProduct.getBand(bandName));
                if (Arrays.binarySearch(redistributeClassIndices, bandId) < 0) {  // not included
                    useClassList.add(bandId);
                }
            }
        }

        final Product targetProduct = createTargetProduct();

        setTargetProduct(targetProduct);
    }


    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        try {
            if (targetBand.getName().startsWith("reflectance_")) {
                pm.beginTask(String.format("Computing '%s'", targetBand.getName()), 1);
                final Tile sourceTile = getSourceTile(toaReflectanceProduct.getBand(targetBand.getName()),
                        targetTile.getRectangle(),
                        SubProgressMonitor.create(pm, 1));
                targetTile.setRawSamples(sourceTile.getRawSamples());
            } else if (BAND_NAME_CLOUD_PROBABILITY.equals(targetBand.getName())) {
                pm.beginTask(String.format("Computing '%s'", targetBand.getName()), probabilityBandMap.size() + 1);
                final Map<Integer, Tile> tileMap = new HashMap<Integer, Tile>(probabilityBandMap.size());
                for (Map.Entry<Integer, Band> entry : probabilityBandMap.entrySet()) {
                    final Tile sourceTile = getSourceTile(entry.getValue(), targetTile.getRectangle(),
                            SubProgressMonitor.create(pm, 1));
                    tileMap.put(entry.getKey(), sourceTile);
                    if (pm.isCanceled()) {
                        return;
                    }
                }
                computeCloudProbability(targetTile, tileMap);
                pm.worked(1);
            } else {
                throw new OperatorException(
                        String.format("Nothing known about a band named '%s'", targetBand.getName()));
            }
        } finally {
            pm.done();
        }
    }

    private void validateParameter() {
        for (int accumulateClass : accumulateClassIndices) {
            final Band band = clusterProduct.getBand(PROBABILITY_PREFIX + accumulateClass);
            if (band == null) {
                throw new OperatorException(
                        String.format("Not able to find accumulate class with index %d", accumulateClass));
            }
        }

        for (int redistributeClass : redistributeClassIndices) {
            final Band band = clusterProduct.getBand(PROBABILITY_PREFIX + redistributeClass);
            if (band == null) {
                throw new OperatorException(
                        String.format("Not able to find redistribute class with index %d", redistributeClass));
            }
        }
    }

    private Product createTargetProduct() {
        final String name = clusterProduct.getName().replace("_CLU", "_PROB");
        final String type = clusterProduct.getProductType().replace("_CLU", "_PROB");
        final int sceneRasterWidth = clusterProduct.getSceneRasterWidth();
        final int sceneRasterHeight = clusterProduct.getSceneRasterHeight();
        final Product targetProduct = new Product(name, type, sceneRasterWidth, sceneRasterHeight);
        targetProduct.setStartTime(clusterProduct.getStartTime());
        targetProduct.setEndTime(clusterProduct.getEndTime());

        targetProduct.addBand(BAND_NAME_CLOUD_PROBABILITY, ProductData.TYPE_FLOAT32);

        if (toaReflectanceProduct != null) {
            if (sceneRasterWidth != toaReflectanceProduct.getSceneRasterWidth() ||
                    sceneRasterHeight != toaReflectanceProduct.getSceneRasterHeight()) {
                throw new OperatorException("Source products are not spatially equal.");
            }
            final Band[] reflBands = toaReflectanceProduct.getBands();
            for (Band band : reflBands) {
                if (band.getName().startsWith("reflectance")) {
                    ProductUtils.copyBand(band.getName(), toaReflectanceProduct, targetProduct);
                }
            }
        }
        return targetProduct;
    }

    private void computeCloudProbability(Tile targetTile, Map<Integer, Tile> probabilityTiles) {
        for (Tile.Pos pos : targetTile) {
            double accum = accumulateClassProbabilities(probabilityTiles, pos);
            double sum = sumNotRedistributed(probabilityTiles, pos);
            targetTile.setSample(pos.x, pos.y, accum / sum);
        }
    }

    private double sumNotRedistributed(Map<Integer, Tile> probabilityTiles, Tile.Pos pos) {
        double accuSum = 0;
        for (int classIndex : useClassList) {
            final Tile tile = probabilityTiles.get(classIndex);
            accuSum = accuSum + tile.getSampleDouble(pos.x, pos.y);
        }
        return accuSum;
    }

    private double accumulateClassProbabilities(Map<Integer, Tile> probabilityTiles, Tile.Pos pos) {
        double accuSum = 0;
        for (int classIndex : accumulateClassIndices) {
            final Tile tile = probabilityTiles.get(classIndex);
            final double sampleDouble = tile.getSampleDouble(pos.x, pos.y);
            accuSum = accuSum + sampleDouble;
        }
        return accuSum;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(CloudProbabilityOp.class);
        }
    }
}
