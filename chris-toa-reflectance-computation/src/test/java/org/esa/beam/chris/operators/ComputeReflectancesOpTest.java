package org.esa.beam.chris.operators;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;

/**
 * Tests for class {@link ComputeToaReflectancesOp}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ComputeReflectancesOpTest extends TestCase {

    public void testAssertValidity() {
        ComputeToaReflectancesOp.assertValidity(new Product("name", "CHRIS_M1_NR", 10, 10));
        ComputeToaReflectancesOp.assertValidity(new Product("name", "CHRIS_M2_NR", 10, 10));
        ComputeToaReflectancesOp.assertValidity(new Product("name", "CHRIS_M3_NR", 10, 10));
        ComputeToaReflectancesOp.assertValidity(new Product("name", "CHRIS_M3A_NR", 10, 10));
        ComputeToaReflectancesOp.assertValidity(new Product("name", "CHRIS_M4_NR", 10, 10));
        ComputeToaReflectancesOp.assertValidity(new Product("name", "CHRIS_M5_NR", 10, 10));

        try {
            ComputeToaReflectancesOp.assertValidity(new Product("name", "CHRIS_M1", 10, 10));
            fail();
        } catch (OperatorException expected) {
        }
        try {
            ComputeToaReflectancesOp.assertValidity(new Product("name", "CHRIS_M2", 10, 10));
            fail();
        } catch (OperatorException expected) {
        }
        try {
            ComputeToaReflectancesOp.assertValidity(new Product("name", "CHRIS_M3", 10, 10));
            fail();
        } catch (OperatorException expected) {
        }
        try {
            ComputeToaReflectancesOp.assertValidity(new Product("name", "CHRIS_M3A", 10, 10));
            fail();
        } catch (OperatorException expected) {
        }
        try {
            ComputeToaReflectancesOp.assertValidity(new Product("name", "CHRIS_M4", 10, 10));
            fail();
        } catch (OperatorException expected) {
        }
        try {
            ComputeToaReflectancesOp.assertValidity(new Product("name", "CHRIS_M5", 10, 10));
            fail();
        } catch (OperatorException expected) {
        }
    }
}
