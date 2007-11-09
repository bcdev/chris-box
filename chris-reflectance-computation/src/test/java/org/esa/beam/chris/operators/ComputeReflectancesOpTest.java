package org.esa.beam.chris.operators;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;

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

    public void testAssertValidity() {
        ComputeReflectancesOp.assertValidity(new Product("name", "CHRIS_M1_NR", 10, 10));
        ComputeReflectancesOp.assertValidity(new Product("name", "CHRIS_M2_NR", 10, 10));
        ComputeReflectancesOp.assertValidity(new Product("name", "CHRIS_M3_NR", 10, 10));
        ComputeReflectancesOp.assertValidity(new Product("name", "CHRIS_M3A_NR", 10, 10));
        ComputeReflectancesOp.assertValidity(new Product("name", "CHRIS_M4_NR", 10, 10));
        ComputeReflectancesOp.assertValidity(new Product("name", "CHRIS_M5_NR", 10, 10));

        try {
            ComputeReflectancesOp.assertValidity(new Product("name", "CHRIS_M1", 10, 10));
            fail();
        } catch (OperatorException expected) {
        }
        try {
            ComputeReflectancesOp.assertValidity(new Product("name", "CHRIS_M2", 10, 10));
            fail();
        } catch (OperatorException expected) {
        }
        try {
            ComputeReflectancesOp.assertValidity(new Product("name", "CHRIS_M3", 10, 10));
            fail();
        } catch (OperatorException expected) {
        }
        try {
            ComputeReflectancesOp.assertValidity(new Product("name", "CHRIS_M3A", 10, 10));
            fail();
        } catch (OperatorException expected) {
        }
        try {
            ComputeReflectancesOp.assertValidity(new Product("name", "CHRIS_M4", 10, 10));
            fail();
        } catch (OperatorException expected) {
        }
        try {
            ComputeReflectancesOp.assertValidity(new Product("name", "CHRIS_M5", 10, 10));
            fail();
        } catch (OperatorException expected) {
        }
    }

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
