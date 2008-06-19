package org.esa.beam.chris.operators;

import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.math.Array;
import org.esa.beam.util.math.IntervalPartition;
import org.esa.beam.util.math.VectorLookupTable;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.text.MessageFormat;

/**
 * Reader for MODTRAN lookup table.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class ModtranLookupTableReader {

    @SuppressWarnings({"ConstantConditions"})
    public ModtranLookupTable readLookupTable() throws OperatorException {
        final ImageInputStream iis = getResourceAsImageInputStream("chrisbox-ac-lut-formatted-1nm.img");
        iis.setByteOrder(ByteOrder.LITTLE_ENDIAN);

        try {
            // read spectral wavelengths
            final int wavelengthCount = iis.readShort();
            final float[] wavelengths = new float[wavelengthCount];
            iis.readFully(wavelengths, 0, wavelengthCount);

            // read view zenith angles
            final short vzaCount = iis.readShort();
            final float[] vza = new float[vzaCount];
            iis.readFully(vza, 0, vzaCount);
            // read sun zenith angles
            final short szaCount = iis.readShort();
            final float[] sza = new float[szaCount];
            iis.readFully(sza, 0, szaCount);
            // read elevations
            final short elevCount = iis.readShort();
            final float[] elev = new float[elevCount];
            iis.readFully(elev, 0, elevCount);
            // read aerosol optical thicknesses
            final short aotCount = iis.readShort();
            final float[] aot = new float[aotCount];
            iis.readFully(aot, 0, aotCount);
            // read relative azimuth angles
            final short raaCount = iis.readShort();
            final float[] raa = new float[raaCount];
            iis.readFully(raa, 0, raaCount);
            // read water vapour columns
            final short cwvCount = iis.readShort();
            final float[] cwv = new float[cwvCount];
            iis.readFully(cwv, 0, cwvCount);

            // read number of lookup table parameters
            final int parameterCount1 = iis.readShort();
            final int parameterCount2 = iis.readShort();
            // read lookup table values
            final int valueCount1 = parameterCount1 * wavelengthCount * raaCount * aotCount * elevCount * szaCount * vzaCount;
            final int valueCount2 = parameterCount2 * wavelengthCount * cwvCount * aotCount * elevCount * szaCount * vzaCount;
            final float[] values1 = new float[valueCount1];
            final float[] values2 = new float[valueCount2];
            iis.readFully(values1, 0, valueCount1);
            iis.readFully(values2, 0, valueCount2);

            // create lookup tables
            final IntervalPartition[] partitions1 = IntervalPartition.createArray(vza, sza, elev, aot, raa);
            final IntervalPartition[] partitions2 = IntervalPartition.createArray(vza, sza, elev, aot, cwv);
            final VectorLookupTable lut1 = new VectorLookupTable(wavelengthCount * parameterCount1, values1, partitions1);
            final VectorLookupTable lut2 = new VectorLookupTable(wavelengthCount * parameterCount2, values2, partitions2);

            return new ModtranLookupTable(parameterCount1, parameterCount2, lut1, lut2,
                                          new Array.Float(wavelengths));
        } catch (Exception e) {
            throw new OperatorException("could not read MODTRAN lookup table for atmospheric correction", e);
        } finally {
            try {
                iis.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * Returns an {@link javax.imageio.stream.ImageInputStream} for a resource file of interest.
     *
     * @param name the name of the resource file of interest.
     *
     * @return the image input stream.
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          if the resource could not be found or the
     *          image input stream could not be created.
     */
    private ImageInputStream getResourceAsImageInputStream(String name) throws OperatorException {
        final InputStream is = getClass().getResourceAsStream(name);

        if (is == null) {
            throw new OperatorException(MessageFormat.format("resource {0} not found", name));
        }
        try {
            return new FileCacheImageInputStream(is, null);
        } catch (Exception e) {
            throw new OperatorException(MessageFormat.format(
                    "could not create image input stream for resource {0}", name), e);
        }
    }
}
