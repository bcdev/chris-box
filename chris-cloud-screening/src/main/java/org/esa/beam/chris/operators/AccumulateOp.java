package org.esa.beam.chris.operators;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

@OperatorMetadata(alias = "chris.Accumulate",
        version = "1.0",
        authors = "Marco Peters, Ralf Quast, Marco Zühlke",
        copyright = "(c) 2008 Brockmann Consult",
        description = "Calculates the arithmetic sum (or accumulation) of certain bands.")
public class AccumulateOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct(description = "The result of the accumulation.")
    private Product targetProduct;

    @Parameter(alias = "sourceBands", description = "Bands being accumulated.", notEmpty = true, notNull = true)
    private String[] sourceBandNames;
    @Parameter(alias = "targetProductName", defaultValue = "Accumulation", description = "Name of the target product",
            notEmpty = true, notNull = true)
    private String targetProductName;
    @Parameter(alias = "targetProductType", defaultValue = "Accumulation", description = "Type of the target product",
            notEmpty = true, notNull = true)
    private String targetProductType;
    @Parameter(alias = "targetBand", description = "Name of the accumulation band.", notEmpty = true, notNull = true)
    private String accumulationBandName;

    private transient List<Band> sourceBandList;

    public AccumulateOp() {
    }

    public AccumulateOp(Product sourceProduct, String[] sourceBandNames, String accumulationBandName) {
        this.sourceProduct = sourceProduct;
        this.sourceBandNames = sourceBandNames;
        this.accumulationBandName = accumulationBandName;
    }

    @Override
    public void initialize() throws OperatorException {
        validateParameters();

        sourceBandList = new ArrayList<Band>();
        for (final String name : sourceBandNames) {
            sourceBandList.add(sourceProduct.getBand(name));
        }
        final Product targetProduct = createTargetProduct();

        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final int width = targetTile.getRectangle().width;
        final int height = targetTile.getRectangle().height;

        pm.beginTask("Computing band '" + targetBand.getName() + "'...", sourceBandList.size() + width * height);

        try {
            final List<Tile> sourceTileList = new ArrayList<Tile>(sourceBandList.size());
            for (final Band sourceBand : sourceBandList) {
                checkForCancelation(pm);
                final Tile sourceTile = getSourceTile(sourceBand, targetTile.getRectangle(),
                        SubProgressMonitor.create(pm, 1));
                sourceTileList.add(sourceTile);
            }

            for (final Tile.Pos pos : targetTile) {
                checkForCancelation(pm);

                double sum = 0.0;
                for (final Tile sourceTile : sourceTileList) {
                    sum += sourceTile.getSampleDouble(pos.x, pos.y);
                }
                targetTile.setSample(pos.x, pos.y, sum);

                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private void validateParameters() {
        for (String name : sourceBandNames) {
            final Band band = sourceProduct.getBand(name);
            if (band == null) {
                throw new OperatorException(MessageFormat.format("Band ''{0}'' does not exist.", name));
            }
        }
        // todo - data type should be the same for all bands (?)
    }

    private Product createTargetProduct() {
        final int sceneRasterWidth = sourceProduct.getSceneRasterWidth();
        final int sceneRasterHeight = sourceProduct.getSceneRasterHeight();
        final Product targetProduct = new Product(targetProductName, targetProductType, sceneRasterWidth,
                sceneRasterHeight);

        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        // todo - set preferred tile size (?)
        targetProduct.addBand(accumulationBandName, sourceBandList.get(0).getDataType());

        return targetProduct;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(AccumulateOp.class);
        }
    }
}
