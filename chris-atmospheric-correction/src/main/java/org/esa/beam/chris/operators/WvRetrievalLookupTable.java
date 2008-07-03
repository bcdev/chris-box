package org.esa.beam.chris.operators;

import org.esa.beam.util.math.IntervalPartition;
import org.esa.beam.util.math.MatrixLookupTable;
import org.esa.beam.util.math.RowMajorMatrixFactory;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class WvRetrievalLookupTable {

    public static final int LPW = 0;
    public static final int EGL = 1;
    public static final int SAB = 2;

    private final double[] wavelengths;
    private final MatrixLookupTable lut;

    public WvRetrievalLookupTable(ModtranLookupTable modtranLookupTable, double vza, double sza, double ada, double alt, double aot) {
        wavelengths = modtranLookupTable.getWavelengths();

        final IntervalPartition cwvDimension = modtranLookupTable.getDimension(ModtranLookupTable.CWV);
        final double[][][] rtmTables = new double[cwvDimension.getCardinal()][3][];

        for (int i = 0; i < cwvDimension.getCardinal(); ++i) {
            final double[][] table = modtranLookupTable.getRtmTable(vza, sza, ada, alt, aot, cwvDimension.get(i));
            
            rtmTables[i][0] = table[ModtranLookupTable.LPW];
            rtmTables[i][1] = table[ModtranLookupTable.EGL];
            rtmTables[i][2] = table[ModtranLookupTable.SAB];
        }

        final int m = 3;
        final int n = modtranLookupTable.getWavelengthCount();

        lut = new MatrixLookupTable(m, n, new RowMajorMatrixFactory(), toArray(rtmTables), cwvDimension);
    }

    public int getWavelengthCount() {
        return wavelengths.length;    
    }

    public double[] getWavelengths() {
        return wavelengths;
    }

    public double[][] getRtcTable(double cwv) {
        return lut.getValues(cwv);
    }

    private static double[] toArray(double[][][] tables) {
        final double[] array = new double[tables.length * tables[0].length * tables[0][0].length];

        int k = 0;
        for (final double[][] table : tables) {
            for (final double[] row : table) {
                System.arraycopy(row, 0, array, k, row.length);
                k += row.length;
            }
        }

        return array;
    }
}
