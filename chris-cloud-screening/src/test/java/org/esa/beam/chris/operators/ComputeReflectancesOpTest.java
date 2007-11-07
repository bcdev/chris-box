package org.esa.beam.chris.operators;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

/**
 * Tests for class {@link ComputeReflectancesOp}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ComputeReflectancesOpTest extends TestCase {

    public void testSlitVsProfileTableIntegrity() throws IOException {
        assertThuillierTableIntegrity();
    }

    private static void assertThuillierTableIntegrity() throws IOException {
        final InputStream is = ComputeReflectancesOpTest.class.getResourceAsStream("thuillier.txt");

        final Scanner scanner = new Scanner(is);
        scanner.useLocale(Locale.ENGLISH);

        final int rowCount = 8191;
        final ArrayList<Double> x = new ArrayList<Double>(rowCount);
        final ArrayList<Double> y = new ArrayList<Double>(rowCount);

        try {
            while (scanner.hasNext()) {
                x.add(scanner.nextDouble());
                y.add(scanner.nextDouble());
            }
        } finally {
            scanner.close();
        }

        assertEquals(rowCount, x.size());
        assertEquals(rowCount, y.size());

        final double[][] table = ComputeReflectancesOp.readThuillierTable();

        assertEquals(rowCount, table[0].length);
        assertEquals(rowCount, table[1].length);

        for (int i = 0; i < rowCount; ++i) {
            assertEquals(x.get(i), table[0][i]);
            assertEquals(y.get(i), table[1][i]);
        }
    }
}