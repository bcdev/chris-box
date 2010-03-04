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

import org.esa.beam.chris.operators.IctDataRecord.IctDataReader;
import org.esa.beam.chris.operators.TelemetryFinder.Telemetry;
import org.esa.beam.chris.util.BandFilter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PointingFactoryRegistry;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.util.ProductUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

@OperatorMetadata(alias = "chris.PerformGeometricCorrection",
                  version = "1.0",
                  authors = "Ralf Quast, Marco Zühlke",
                  copyright = "(c) 2009 by Brockmann Consult",
                  description = "Performs the geometric correction for a CHRIS/Proba RCI.")
public class PerformGeometricCorrectionOp extends Operator {

    public static final String ALIAS_TELEMETRY_REPOSITORY = "telemetryRepository";
    // there is a delay of 0.999s between the GPS time tag and the actual time of the reported position
    private static final double DELAY = 0.999;

    @Parameter(alias = ALIAS_TELEMETRY_REPOSITORY, label = "Telemetry repository", defaultValue = ".",
               description = "The directory searched for CHRIS telemetry data", notNull = true, notEmpty = true)
    private File telemetryRepository;


    @Parameter(label = "Use target altitude", defaultValue = "false",
               description = "If true, the pixel lines-of-sight are intersected with a modified WGS-84 ellipsoid")
    private boolean useTargetAltitude;

    @Parameter(label = "Include pitch and roll angles", defaultValue = "false",
               description = "If true, the target product will include instrument pitch and roll angles per pixel")
    private boolean includePitchAndRoll;

    @SourceProduct(type = "CHRIS_M[012345][0A]?(_NR)?(_AC)?")
    private Product sourceProduct;

    @Override
    public void initialize() throws OperatorException {
        final Telemetry telemetry;
        try {
            telemetry = TelemetryFinder.findTelemetry(sourceProduct, telemetryRepository);
        } catch (IOException e) {
            throw new OperatorException(e);
        }
        final Date ict = sourceProduct.getStartTime().getAsDate();
        final double mjd = TimeConverter.dateToMJD(ict);
        final double dT;
        try {
            dT = TimeConverter.getInstance().deltaGPS(mjd);
        } catch (IOException e) {
            throw new OperatorException(e);
        }

        final IctDataRecord ictData = readIctData(telemetry.getIctFile(), dT);
        final List<GpsDataRecord> gpsData = readGpsData(telemetry.getGpsFile(), DELAY, dT);
        final AcquisitionInfo acquisitionInfo = AcquisitionInfo.createAcquisitionInfo(sourceProduct);
        final GCP[] gcps = GCP.toGCPs(sourceProduct.getGcpGroup(), acquisitionInfo.getTargetAlt());
        final GeometryCalculator calculator = new GeometryCalculator(acquisitionInfo, gcps, useTargetAltitude);

        calculator.calculate(ictData, gpsData, gcps, useTargetAltitude);

        final Product targetProduct = createTargetProduct(calculator, acquisitionInfo.isBackscanning());
        setTargetProduct(targetProduct);
    }

    private Product createTargetProduct(GeometryCalculator calculator, boolean backscanning) {
        final String productType = sourceProduct.getProductType() + "_GC";
        final Product targetProduct = createCopy(sourceProduct, "GC", productType, new BandFilter() {
            @Override
            public boolean accept(Band band) {
                return true;
            }
        });

        final int w = targetProduct.getSceneRasterWidth();
        final int h = targetProduct.getSceneRasterHeight();

        final float[] lons = new float[w * h];
        final float[] lats = new float[w * h];
        final float[] vaas = new float[w * h];
        final float[] vzas = new float[w * h];

        for (int row = 0; row < h; row++) {
            final int y = backscanning ? h - 1 - row : row;
            for (int x = 0; x < w; x++) {
                lons[row * w + x] = (float) calculator.getLon(y, x);
                lats[row * w + x] = (float) calculator.getLat(y, x);
                vaas[row * w + x] = (float) calculator.getVaa(y, x);
                vzas[row * w + x] = (float) calculator.getVza(y, x);
            }
        }

        final TiePointGrid lonGrid = addTiePointGrid(targetProduct, "lon", w, h, lons, "Longitude (deg)", "deg");
        final TiePointGrid latGrid = addTiePointGrid(targetProduct, "lat", w, h, lats, "Latitude (deg)", "deg");
        addTiePointGrid(targetProduct, "vaa", w, h, vaas, "View azimuth angle (deg)", "deg");
        addTiePointGrid(targetProduct, "vza", w, h, vzas, "View zenith angle (deg)", "deg");

        if (includePitchAndRoll) {
            final float[] p = new float[w * h];
            final float[] r = new float[w * h];
            for (int row = 0; row < h; row++) {
                final int y = backscanning ? h - 1 - row : row;
                for (int x = 0; x < w; x++) {
                    p[row * w + x] = (float) calculator.getPitchAngle(y, x);
                    r[row * w + x] = (float) calculator.getRollAngle(y, x);
                }
            }
            addTiePointGrid(targetProduct, "pitch", w, h, p, "Instrument pitch angle (rad)", "rad");
            addTiePointGrid(targetProduct, "roll", w, h, r, "Instrument roll angle (rad)", "rad");
        }

        targetProduct.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
        targetProduct.setPointingFactory(PointingFactoryRegistry.getInstance().getPointingFactory(productType));

        return targetProduct;
    }

    private static TiePointGrid addTiePointGrid(Product targetProduct, String name, int w, int h, float[] tiePoints,
                                                String description, String unit) {
        final TiePointGrid lonGrid = new TiePointGrid(name, w, h, 0.5f, 0.5f, 1.0f, 1.0f, tiePoints);
        lonGrid.setDescription(description);
        lonGrid.setUnit(unit);
        targetProduct.addTiePointGrid(lonGrid);

        return lonGrid;
    }

    // todo - this is almost a general utility method (rq-20090708)
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
                                                          pin.getSymbol(),
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
                                                          pin.getSymbol(),
                                                          null));
        }

        return targetProduct;
    }

    private static List<GpsDataRecord> readGpsData(File gpsFile, double delay, double deltaGPS) {
        InputStream is = null;
        try {
            is = new FileInputStream(gpsFile);
            return GpsDataRecord.create(new GpsDataRecord.GpsDataReader(is).getReadRecords(), deltaGPS, delay);
        } catch (IOException e) {
            throw new OperatorException(e);
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

    private static IctDataRecord readIctData(File ictFile, double deltaGPS) {
        InputStream is = null;
        try {
            is = new FileInputStream(ictFile);
            return IctDataRecord.create(new IctDataReader(is).getLastIctValues(), deltaGPS);
        } catch (IOException e) {
            throw new OperatorException(e);
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
