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
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.dataop.resamp.Resampling;
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

    @Parameter(label = "Use target altitude", defaultValue = "false",
               description = "If true, the pixel lines-of-sight are intersected with a modified WGS-84 ellipsoid")
    private boolean useTargetAltitude;

    @Parameter(label = "Include pitch and roll angles (for diagnostics only)", defaultValue = "false",
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
        final List<GpsDataRecord> gpsData = readGpsData(telemetry.getGpsFile(), dT, DELAY);
        final AcquisitionInfo acquisitionInfo = AcquisitionInfo.create(sourceProduct);
        final GCP[] gcps = GCP.createArray(sourceProduct.getGcpGroup(), acquisitionInfo.getTargetAlt());

        final GeometryCalculator calculator = new GeometryCalculator(acquisitionInfo, gcps);
        calculator.calculate(ictData, gpsData, useTargetAltitude);

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

        final double[] lons = new double[w * h];
        final double[] lats = new double[w * h];
        final double[] vaas = new double[w * h];
        final double[] vzas = new double[w * h];

        for (int row = 0; row < h; row++) {
            final int y = backscanning ? h - 1 - row : row;
            for (int x = 0; x < w; x++) {
                lons[row * w + x] = calculator.getLon(x, y);
                lats[row * w + x] = calculator.getLat(x, y);
                vaas[row * w + x] = calculator.getVaa(x, y);
                vzas[row * w + x] = calculator.getVza(x, y);
            }
        }

        final Band lonBand = addBand(targetProduct, "lon", lons, "Longitude (deg)", "deg");
        final Band latBand = addBand(targetProduct, "lat", lats, "Latitude (deg)", "deg");
        addBand(targetProduct, "vaa", vaas, "View azimuth angle (deg)", "deg");
        addBand(targetProduct, "vza", vzas, "View zenith angle (deg)", "deg");

        if (includePitchAndRoll) {
            final double[] pitches = new double[w * h];
            final double[] rolls = new double[w * h];
            for (int row = 0; row < h; row++) {
                final int y = backscanning ? h - 1 - row : row;
                for (int x = 0; x < w; x++) {
                    pitches[row * w + x] = calculator.getPitch(x, y);
                    rolls[row * w + x] = calculator.getRoll(x, y);
                }
            }
            addBand(targetProduct, "pitch", pitches, "Instrument pitch angle (rad)", "rad");
            addBand(targetProduct, "roll", rolls, "Instrument roll angle (rad)", "rad");
        }
        targetProduct.setGeoCoding(new PixelGeoCoding(latBand, lonBand, "true", 20));

        return targetProduct;
    }

    private static Band addBand(Product targetProduct, String name, double[] floats, String description, String unit) {
        final Band band = targetProduct.addBand(name, ProductData.TYPE_FLOAT64);
        band.setDataElems(floats);
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

    static List<GpsDataRecord> readGpsData(File gpsFile, double deltaGPS, double delay) {
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

    static IctDataRecord readIctData(File ictFile, double deltaGPS) {
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
