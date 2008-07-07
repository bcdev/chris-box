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
 * Reader for the MODTRAN lookup table.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class ModtranLookupTableReader {

    // unit conversion constant
    private static final double DEKA_KILO = 1.0E4;

    @SuppressWarnings({"ConstantConditions"})
    public ModtranLookupTable readModtranLookupTable() throws IOException {
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
            final int parameterCountA = iis.readShort();
            final int parameterCountB = iis.readShort();
            // read lookup table values
            final int valueCountA = parameterCountA * wavelengthCount * adaCount * aotCount * altCount * szaCount * vzaCount;
            final int valueCountB = parameterCountB * wavelengthCount * cwvCount * aotCount * altCount * szaCount * vzaCount;
            final float[] valuesA = new float[valueCountA];
            final float[] valuesB = new float[valueCountB];
            iis.readFully(valuesA, 0, valueCountA);
            iis.readFully(valuesB, 0, valueCountB);

            // scale atmospheric path radiances
            for (int i = 0; i < valuesA.length; ++i) {
                valuesA[i] *= DEKA_KILO;
            }
            // scale directed and diffuse fluxes
            for (int i = 0; i < valuesB.length; i += parameterCountB) {
                final int j = i + 1;

                valuesB[i] *= DEKA_KILO;
                valuesB[j] *= DEKA_KILO;
            }

            // create lookup tables
            final IntervalPartition[] partitionsA = IntervalPartition.createArray(vza, sza, alt, aot, ada);
            final IntervalPartition[] partitionsB = IntervalPartition.createArray(vza, sza, alt, aot, cwv);
            final VectorLookupTable lutA = new VectorLookupTable(wavelengthCount * parameterCountA, valuesA,
                                                                 partitionsA);
            final VectorLookupTable lutB = new VectorLookupTable(wavelengthCount * parameterCountB, valuesB,
                                                                 partitionsB);

            return new ModtranLookupTable(new Array.Float(wavelengths), lutA, lutB);
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
