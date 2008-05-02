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

import junit.framework.TestCase;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.math.IntervalPartition;
import org.esa.beam.util.math.MatrixFactory;
import org.esa.beam.util.math.RowMajorMatrixFactory;
import org.esa.beam.util.math.VectorLookupTable;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.text.MessageFormat;

/**
 * Tests for atmospheric correction.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ComputeAtmosphericCorrectionOpTest extends TestCase {

    private int wavelengthCount;
    private int parameterCount1;
    private int parameterCount2;

    private VectorLookupTable lut1;
    private VectorLookupTable lut2;

    @Override
    protected void setUp() throws Exception {
        readLookupTables();
    }

    @Override
    protected void tearDown() throws Exception {
        lut2 = null;
        lut1 = null;
    }

    public void testParameterMatrix() {
        double[][] matrix;
        matrix = getParameterMatrix(20.0, 35.0, 145.0, 0.3, 0.2, 2.0);
        assertEquals(wavelengthCount, matrix.length);

        double[] parameters;
        parameters = matrix[70];
        assertEquals(parameterCount1 + parameterCount2, parameters.length);

        assertEquals(0.00423624, parameters[0], 0.5E-8);
        assertEquals(0.10402400, parameters[1], 0.5E-6);
        assertEquals(0.03887840, parameters[2], 0.5E-7);
        assertEquals(0.17851200, parameters[3], 0.5E-6);
        assertEquals(0.36571800, parameters[4], 0.5E-6);

        matrix = getParameterMatrix(40.0, 55.0, 45.0, 0.1, 0.3, 3.0);
        assertEquals(wavelengthCount, matrix.length);

        parameters = matrix[17];
        assertEquals(parameterCount1 + parameterCount2, parameters.length);

        assertEquals(0.00809511, parameters[0], 0.5E-8);
        assertEquals(0.03731340, parameters[1], 0.5E-7);
        assertEquals(0.03066440, parameters[2], 0.5E-7);
        assertEquals(0.25710700, parameters[3], 0.5E-6);
        assertEquals(1.00742000, parameters[4], 0.5E-5);
    }
    
    public void testLookupTableIntegrity() {
        checkLookupTable1();
        checkLookupTable2();
    }

    private void checkLookupTable1() {
        double[] values;
        // vza  = 20.0
        // sza  = 35.0
        // elev = 0.3   target elevation
        // aot  = 0.2   AOT at 550nm
        // raa  = 145.0 relative azimuth angle
        values = lut1.getValues(20.0, 35.0, 0.3, 0.2, 145.0);

        assertEquals(wavelengthCount, values.length);
        assertEquals(0.002960650, values[104], 0.5E-8);
        assertEquals(0.000294274, values[472], 0.5E-9);

        // vza  = 40.0
        // sza  = 55.0
        // elev = 0.1  target elevation
        // aot  = 0.3  AOT at 550nm
        // raa  = 45.0 relative azimuth angle
        values = lut1.getValues(40.0, 55.0, 0.1, 0.3, 45.0);

        assertEquals(wavelengthCount, values.length);
        assertEquals(0.004093020, values[136], 0.5E-8);
        assertEquals(0.000631324, values[446], 0.5E-9);
    }

    private void checkLookupTable2() {
        final MatrixFactory matrixFactory = new RowMajorMatrixFactory();

        double[][] values;
        // vza  = 20.0
        // sza  = 35.0
        // elev = 0.3  target elevation
        // aot  = 0.2  AOT at 550nm
        // cwv  = 2.0  integrated water vapour
        values = matrixFactory.createMatrix(wavelengthCount, parameterCount2,
                                            lut2.getValues(20.0, 35.0, 0.3, 0.2, 2.0));

        assertEquals(0.1084700, values[110][0], 0.5E-6);
        assertEquals(0.0333388, values[110][1], 0.5E-7);
        assertEquals(0.1479490, values[110][2], 0.5E-6);
        assertEquals(0.3042250, values[110][3], 0.5E-6);

        assertEquals(0.05969390, values[627][0], 0.5E-7);
        assertEquals(0.00439437, values[627][1], 0.5E-8);
        assertEquals(0.03657480, values[627][2], 0.5E-7);
        assertEquals(0.07540260, values[627][3], 0.5E-7);

        // vza  = 40.0
        // sza  = 55.0
        // elev = 0.1  target elevation
        // aot  = 0.3  AOT at 550nm
        // cwv  = 3.0  integrated water vapour
        values = matrixFactory.createMatrix(wavelengthCount, parameterCount2,
                                            lut2.getValues(40.0, 55.0, 0.1, 0.3, 3.0));

        assertEquals(0.0756223, values[222][0], 0.5E-7);
        assertEquals(0.0227272, values[222][1], 0.5E-7);
        assertEquals(0.1133030, values[222][2], 0.5E-6);
        assertEquals(0.4021710, values[222][3], 0.5E-6);

        assertEquals(0.0662339, values[462][0], 0.5E-7);
        assertEquals(0.0101405, values[462][1], 0.5E-7);
        assertEquals(0.0646544, values[462][2], 0.5E-7);
        assertEquals(0.2110600, values[462][3], 0.5E-6);
    }

    // TODO - move to operator
    /**
     * Returns an interpolated matrix of output parameters for given input parameters.
     *
     * @param vza  the view zenith angle (degree).
     * @param sza  the solar zenith angle (degree).
     * @param raa  the relative azimuth angle (degree).
     * @param elev the target elevation (km).
     * @param aot  the aerosol optical thickness at 550 nm.
     * @param cwv  the integrated water vapour column (g cm-2).
     *
     * @return the output parameter matrix.
     */
    private double[][] getParameterMatrix(double vza, double sza, double raa, double elev, double aot, double cwv) {
        final double[] values1 = lut1.getValues(vza, sza, elev, aot, raa);
        final double[] values2 = lut2.getValues(vza, sza, elev, aot, cwv);

        final double[][] matrix = new double[wavelengthCount][parameterCount1 + parameterCount2];

        for (int i = 0; i < wavelengthCount; ++i) {
            System.arraycopy(values1, parameterCount1 * i, matrix[i], 0, parameterCount1);
            System.arraycopy(values2, parameterCount2 * i, matrix[i], parameterCount1, parameterCount2);
        }

        return matrix;
    }

    // TODO - move to operator
    private void readLookupTables() throws OperatorException {
        final ImageInputStream iis = getResourceAsImageInputStream("chrisbox-ac-lut-formatted-1nm.img");
        iis.setByteOrder(ByteOrder.LITTLE_ENDIAN);

        try {
            wavelengthCount = iis.readShort();
            final float[] wavelengths = new float[wavelengthCount];
            iis.readFully(wavelengths, 0, wavelengthCount);

            final short vzaCount = iis.readShort();
            final float[] vza = new float[vzaCount];
            iis.readFully(vza, 0, vzaCount);

            final short szaCount = iis.readShort();
            final float[] sza = new float[szaCount];
            iis.readFully(sza, 0, szaCount);

            final short elevCount = iis.readShort();
            final float[] elev = new float[elevCount];
            iis.readFully(elev, 0, elevCount);

            final short aotCount = iis.readShort();
            final float[] aot = new float[aotCount];
            iis.readFully(aot, 0, aotCount);

            final short raaCount = iis.readShort();
            final float[] raa = new float[raaCount];
            iis.readFully(raa, 0, raaCount);

            final short cwvCount = iis.readShort();
            final float[] cwv = new float[cwvCount];
            iis.readFully(cwv, 0, cwvCount);

            parameterCount1 = iis.readShort();
            parameterCount2 = iis.readShort();

            final int valueCount1 = parameterCount1 * wavelengthCount * raaCount * aotCount * elevCount * szaCount * vzaCount;
            final int valueCount2 = parameterCount2 * wavelengthCount * cwvCount * aotCount * elevCount * szaCount * vzaCount;

            final float[] values1 = new float[valueCount1];
            final float[] values2 = new float[valueCount2];
            iis.readFully(values1, 0, valueCount1);
            iis.readFully(values2, 0, valueCount2);

            final IntervalPartition[] partitions1 = IntervalPartition.createArray(vza, sza, elev, aot, raa);
            final IntervalPartition[] partitions2 = IntervalPartition.createArray(vza, sza, elev, aot, cwv);

            lut1 = new VectorLookupTable(wavelengthCount * parameterCount1, values1, partitions1);
            lut2 = new VectorLookupTable(wavelengthCount * parameterCount2, values2, partitions2);
        } catch (Exception e) {
            throw new OperatorException("could not read lookup tables for atmospheric correction", e);
        } finally {
            try {
                iis.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    // TODO - move to operator
    /**
     * Returns an {@link ImageInputStream} for a resource file of interest.
     *
     * @param name the name of the resource file of interest.
     *
     * @return the image input stream.
     *
     * @throws OperatorException if the resource could not be found or the
     *                           image input stream could not be created.
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
