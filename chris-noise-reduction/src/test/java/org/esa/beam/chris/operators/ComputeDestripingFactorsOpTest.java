package org.esa.beam.chris.operators;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

/**
 * Tests for class {@link ComputeDestripingFactorsOp}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ComputeDestripingFactorsOpTest extends TestCase {

    public void testSlitVsProfileTableIntegrity() throws IOException {
        // too costly for a routine test
        // assertSlitVsProfileTableIntegrity();
    }

    private static void assertSlitVsProfileTableIntegrity() throws IOException {
        final InputStream is = ComputeDestripingFactorsOpTest.class.getResourceAsStream("slit-vs-profile.txt");

        final Scanner scanner = new Scanner(is);
        scanner.useLocale(Locale.ENGLISH);

        final int rowCount = 148707;
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

//        final FileImageOutputStream ios = new FileImageOutputStream(new File("slit-vs-profile.img"));
//        ios.writeInt(rowCount);
//        for (final Double value : x) {
//            ios.writeDouble(value);
//        }
//        for (final Double value : y) {
//            ios.writeDouble(value);
//        }
//        ios.close();

        final double[][] table = ComputeDestripingFactorsOp.readSlitVsProfileTable();

        assertEquals(rowCount, table[0].length);
        assertEquals(rowCount, table[1].length);

        for (int i = 0; i < rowCount; ++i) {
            assertEquals(x.get(i), table[0][i]);
            assertEquals(y.get(i), table[1][i]);
        }
    }
}
