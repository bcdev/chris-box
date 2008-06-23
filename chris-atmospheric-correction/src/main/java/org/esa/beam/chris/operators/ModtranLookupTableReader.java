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

import org.esa.beam.util.math.Array;
import org.esa.beam.util.math.IntervalPartition;
import org.esa.beam.util.math.VectorLookupTable;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * Reader for MODTRAN lookup table.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class ModtranLookupTableReader {

    @SuppressWarnings({"ConstantConditions"})
    public ModtranLookupTable readLookupTable() throws IOException {
        final ImageInputStream iis = OpUtils.getResourceAsImageInputStream("chrisbox-ac-lut-formatted-1nm.img");
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
            // read altitudes
            final short altCount = iis.readShort();
            final float[] alt = new float[altCount];
            iis.readFully(alt, 0, altCount);
            // read aerosol optical thicknesses
            final short aotCount = iis.readShort();
            final float[] aot = new float[aotCount];
            iis.readFully(aot, 0, aotCount);
            // read relative azimuth angles
            final short adaCount = iis.readShort();
            final float[] ada = new float[adaCount];
            iis.readFully(ada, 0, adaCount);
            // read water vapour columns
            final short cwvCount = iis.readShort();
            final float[] cwv = new float[cwvCount];
            iis.readFully(cwv, 0, cwvCount);

            // read number of lookup table parameters
            final int parameterCount1 = iis.readShort();
            final int parameterCount2 = iis.readShort();
            // read lookup table values
            final int valueCount1 = parameterCount1 * wavelengthCount * adaCount * aotCount * altCount * szaCount * vzaCount;
            final int valueCount2 = parameterCount2 * wavelengthCount * cwvCount * aotCount * altCount * szaCount * vzaCount;
            final float[] values1 = new float[valueCount1];
            final float[] values2 = new float[valueCount2];
            iis.readFully(values1, 0, valueCount1);
            iis.readFully(values2, 0, valueCount2);

            // create lookup tables
            final IntervalPartition[] partitions1 = IntervalPartition.createArray(vza, sza, alt, aot, ada);
            final IntervalPartition[] partitions2 = IntervalPartition.createArray(vza, sza, alt, aot, cwv);
            final VectorLookupTable lut1 = new VectorLookupTable(wavelengthCount * parameterCount1, values1,
                                                                 partitions1);
            final VectorLookupTable lut2 = new VectorLookupTable(wavelengthCount * parameterCount2, values2,
                                                                 partitions2);

            return new ModtranLookupTable(parameterCount1, parameterCount2, lut1, lut2,
                                          new Array.Float(wavelengths));
        } catch (Exception e) {
            throw new IOException("could not read MODTRAN lookup table for atmospheric correction", e);
        } finally {
            try {
                iis.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
