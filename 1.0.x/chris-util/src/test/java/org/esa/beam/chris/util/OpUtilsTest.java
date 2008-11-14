package org.esa.beam.chris.util;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Tests for class {@link OpUtils}.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class OpUtilsTest extends TestCase {

    public void testThuillierTableIntegrity() throws IOException {
        final InputStream is = OpUtilsTest.class.getResourceAsStream("thuillier.txt");

        final Scanner scanner = new Scanner(is);
        scanner.useLocale(Locale.ENGLISH);

        final int rowCount = 8191;
        final List<Double> x = new ArrayList<Double>(rowCount);
        final List<Double> y = new ArrayList<Double>(rowCount);

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

//      was used to create the thuillier.img file
//      final FileImageOutputStream ios = new FileImageOutputStream(new File("thuillier.img"));
//      ios.writeInt(rowCount);
//      for (final Double value : x) {
//          ios.writeDouble(value);
//      }
//      for (final Double value : y) {
//          ios.writeDouble(value);
//      }
//      ios.close();

        final double[][] table = OpUtils.readThuillierTable();

        assertEquals(rowCount, table[0].length);
        assertEquals(rowCount, table[1].length);

        for (int i = 0; i < rowCount; ++i) {
            assertEquals(x.get(i), table[0][i]);
            assertEquals(y.get(i), table[1][i]);
        }
    }
}
