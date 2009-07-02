package org.esa.beam.chris.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class IntersectorTest {

    private static final double SQRT3 = Math.sqrt(3.0);

    @Test
    public void intersectWithPerpendicularLine() {
        final double[] point = {0.0, 0.0, 2.0}; // point above northern pole
        final double[] direction = {0.0, 0.0, -1.0}; // direction to center
        final double[] center = {0.0, 0.0, 0.0};
        final double[] radii = {1.0, 1.0, 1.0};

        Intersector.intersect(point, direction, center, radii);

        // expected intersection point
        final double x = 0.0;
        final double y = 0.0;
        final double z = 1.0;

        assertEquals(x, point[0], 0.0);
        assertEquals(y, point[1], 0.0);
        assertEquals(z, point[2], 0.0);
    }

    @Test
    public void intersectWithTangentLine() {
        final double[] point = {0.0, 0.0, 2.0}; // point above northern pole
        final double[] direction = {SQRT3, 0.0, -3.0}; // direction of tangent line
        final double[] center = {0.0, 0.0, 0.0};
        final double[] radii = {1.0, 1.0, 1.0};

        Intersector.intersect(point, direction, center, radii);

        // expected intersection point
        final double x = 0.5 * SQRT3;
        final double y = 0.0;
        final double z = 0.5;

        assertEquals(x, point[0], 0.0);
        assertEquals(y, point[1], 0.0);
        assertEquals(z, point[2], 0.0);
    }

    @Test
    public void intersectWithNonIntersectingLine() {
        final double[] point = {0.0, 0.0, 2.0}; // point above northern pole
        final double[] direction = {1.0, 0.0, 0.0};
        final double[] center = {0.0, 0.0, 0.0};
        final double[] radii = {1.0, 1.0, 1.0};

        Intersector.intersect(point, direction, center, radii);

        assertTrue(Double.isNaN(point[0]));
        assertTrue(Double.isNaN(point[1]));
        assertTrue(Double.isNaN(point[2]));
    }
}
