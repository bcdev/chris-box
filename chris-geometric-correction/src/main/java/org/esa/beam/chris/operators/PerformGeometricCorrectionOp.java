/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
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
import org.esa.beam.chris.operators.IctDataRecord.IctDataReader;
import org.esa.beam.chris.operators.TelemetryFinder.Telemetry;
import org.esa.beam.chris.util.BandFilter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Placemark;
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

import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Operator performing the geometric correction.
 *
 * @author Ralf Quast
 * @author Marco Zuehlke
 * @since CHRIS-Box 1.5
 */
@OperatorMetadata(alias = "chris.PerformGeometricCorrection",
                  version = "1.0",
                  authors = "Ralf Quast, Marco Zühlke",
                  copyright = "(c) 2010 by Brockmann Consult",
                  description = "Performs the geometric correction for a CHRIS/Proba RCI.")
public class PerformGeometricCorrectionOp extends Operator {

    /**
     * The alias of the {@code telemetryRepository} parameter.
     */
    public static final String ALIAS_TELEMETRY_REPOSITORY = "telemetryRepository";
    /**
     * The delay of 0.999s between the GPS time tag and the actual time of the reported position.
     */
    public static final double DELAY = 0.999;

    @Parameter(alias = ALIAS_TELEMETRY_REPOSITORY, label = "Telemetry repository", defaultValue = ".",
               description = "The directory searched for CHRIS telemetry data", notNull = true, notEmpty = true)
    private File telemetryRepository;

    @Parameter(label = "Use target altitude", defaultValue = "true",
               description = "If true, the pixel lines-of-sight are intersected with a modified WGS-84 ellipsoid," +
                             "which increased by the nominal target altitude")
    private boolean useTargetAltitude;

    @Parameter(label = "Include pitch and roll angles (for diagnostics only)", defaultValue = "false",
               description = "If true, the target product will include instrument pitch and roll angles per pixel")
    private boolean includePitchAndRoll;

    @SourceProduct(type = "CHRIS_M[012345][0A]?(_NR)?(_AC)?")
    private Product sourceProduct;

    private transient Telemetry telemetry;

    @Override
    public void initialize() throws OperatorException {
        try {
            telemetry = TelemetryFinder.findTelemetry(sourceProduct, telemetryRepository);
        } catch (IOException e) {
            throw new OperatorException(e);
        }
        final Product targetProduct = createTargetProduct();
        setTargetProduct(targetProduct);
    }

    @Override
    public void dispose() {
        telemetry = null;
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle,
                                 ProgressMonitor pm) throws OperatorException {
        try {
            pm.beginTask("Performing geometric correction...", 30 + targetRectangle.width * targetRectangle.height);

            // 1. get GPS time delay from image center time
            final Date ict = sourceProduct.getStartTime().getAsDate();
            final double mjd = TimeConverter.dateToMJD(ict);
            final double deltaGPS = TimeConverter.getInstance().deltaGPS(mjd);
            pm.worked(10);

            // 2. read image center times from telemetry
            final IctDataRecord ictData = readIctData(telemetry.getIctFile(), deltaGPS);
            pm.worked(10);

            // 3. read trajectory from telemetry
            final List<GpsDataRecord> gpsData = readGpsData(telemetry.getGpsFile(), deltaGPS, DELAY);
            pm.worked(10);

            // 4. calculate geometry
            final AcquisitionInfo info = AcquisitionInfo.create(sourceProduct);
            final GCP[] gcps = GCP.createArray(sourceProduct.getGcpGroup(), info.getTargetAlt());
            final GeometryCalculator calculator = new GeometryCalculator(ictData, gpsData, info, gcps);
            calculator.calculate(useTargetAltitude);

            // 5. copy calculated geometry into image tiles
            final Tile lonTile = targetTileMap.get(getTargetProduct().getBand("lon"));
            final Tile latTile = targetTileMap.get(getTargetProduct().getBand("lat"));
            final Tile vaaTile = targetTileMap.get(getTargetProduct().getBand("vaa"));
            final Tile vzaTile = targetTileMap.get(getTargetProduct().getBand("vza"));
            final Tile pitchTile;
            final Tile rollTile;
            if (includePitchAndRoll) {
                pitchTile = targetTileMap.get(getTargetProduct().getBand("pitch"));
                rollTile = targetTileMap.get(getTargetProduct().getBand("roll"));
            } else {
                pitchTile = null;
                rollTile = null;
            }
            for (final Tile.Pos pos : lonTile) {
                checkForCancelation(pm);
                final int sourceRow = info.isBackscanning() ? targetRectangle.height - 1 - pos.y : pos.y;
                lonTile.setSample(pos.x, pos.y, calculator.getLon(pos.x, sourceRow));
                latTile.setSample(pos.x, pos.y, calculator.getLat(pos.x, sourceRow));
                vaaTile.setSample(pos.x, pos.y, calculator.getVaa(pos.x, sourceRow));
                vzaTile.setSample(pos.x, pos.y, calculator.getVza(pos.x, sourceRow));
                if (includePitchAndRoll) {
                    pitchTile.setSample(pos.x, pos.y, calculator.getPitch(pos.x, sourceRow));
                    rollTile.setSample(pos.x, pos.y, calculator.getRoll(pos.x, sourceRow));
                }
                pm.worked(1);
            }
        } catch (IOException e) {
            throw new OperatorException(e);
        } finally {
            pm.done();
        }
    }

    private Product createTargetProduct() {
        final String productType = sourceProduct.getProductType() + "_GC";
        final Product targetProduct = createCopy(sourceProduct, "GC", productType, new BandFilter() {
            @Override
            public boolean accept(Band band) {
                return true;
            }
        });

        final Band lonBand = addBand(targetProduct, "lon", "Longitude (deg)", "deg");
        final Band latBand = addBand(targetProduct, "lat", "Latitude (deg)", "deg");
        addBand(targetProduct, "vaa", "View azimuth angle (deg)", "deg");
        addBand(targetProduct, "vza", "View zenith angle (deg)", "deg");

        if (includePitchAndRoll) {
            addBand(targetProduct, "pitch", "Instrument pitch angle (rad)", "rad");
            addBand(targetProduct, "roll", "Instrument roll angle (rad)", "rad");
        }

        final int w = targetProduct.getSceneRasterWidth();
        final int h = targetProduct.getSceneRasterHeight();
        targetProduct.setPreferredTileSize(w, h);
        targetProduct.setGeoCoding(new PixelGeoCoding(latBand, lonBand, "true", 20));

        return targetProduct;
    }

    private static Band addBand(Product targetProduct, String name, String description, String unit) {
        final Band band = targetProduct.addBand(name, ProductData.TYPE_FLOAT64);
        band.setDescription(description);
        band.setUnit(unit);

        return band;
    }

    // todo - this is a quite general utility method (rq-20090708)

    private static Product createCopy(Product sourceProduct, String name, String type, BandFilter bandFilter) {
        final int w = sourceProduct.getSceneRasterWidth();
        final int h = sourceProduct.getSceneRasterHeight();
        final Product targetProduct = new Product(name, type, w, h);

        // 1. set start and end times
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        // 2. copy flag codings
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);

        // 3. copy all bands from source product to target product
        for (final Band sourceBand : sourceProduct.getBands()) {
            if (bandFilter.accept(sourceBand)) {
                final Band targetBand = ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct);
                targetBand.setSourceImage(sourceBand.getSourceImage());
                final FlagCoding flagCoding = sourceBand.getFlagCoding();
                if (flagCoding != null) {
                    targetBand.setSampleCoding(targetProduct.getFlagCodingGroup().get(flagCoding.getName()));
                }
            }
        }

        // 4. copy masks
        ProductUtils.copyMasks(sourceProduct, targetProduct);

        // 5. copy metadata tree
        ProductUtils.copyMetadata(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());

        // 6. set preferred tile size
        targetProduct.setPreferredTileSize(sourceProduct.getPreferredTileSize());

        // 7. copy pins
        for (int i = 0; i < sourceProduct.getPinGroup().getNodeCount(); i++) {
            final Placemark pin = sourceProduct.getPinGroup().get(i);
            targetProduct.getPinGroup().add(new Placemark(pin.getName(),
                                                          pin.getLabel(),
                                                          pin.getDescription(),
                                                          pin.getPixelPos(),
                                                          pin.getGeoPos(),
                                                          pin.getPlacemarkDescriptor(),
                                                          null));
        }
        // 8. copy GCPs
        for (int i = 0; i < sourceProduct.getGcpGroup().getNodeCount(); i++) {
            final Placemark pin = sourceProduct.getGcpGroup().get(i);
            targetProduct.getGcpGroup().add(new Placemark(pin.getName(),
                                                          pin.getLabel(),
                                                          pin.getDescription(),
                                                          pin.getPixelPos(),
                                                          pin.getGeoPos(),
                                                          pin.getPlacemarkDescriptor(),
                                                          null));
        }

        return targetProduct;
    }

    static List<GpsDataRecord> readGpsData(File gpsFile, double deltaGPS, double delay) throws IOException {
        InputStream is = null;
        try {
            is = new FileInputStream(gpsFile);
            return GpsDataRecord.create(new GpsDataRecord.GpsDataReader(is).getReadRecords(), deltaGPS, delay);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    static IctDataRecord readIctData(File ictFile, double deltaGPS) throws IOException {
        InputStream is = null;
        try {
            is = new FileInputStream(ictFile);
            return IctDataRecord.create(new IctDataReader(is).getLastIctValues(), deltaGPS);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(PerformGeometricCorrectionOp.class);
        }
    }
}
