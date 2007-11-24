/*
 * Copyright (C) 2002-2007 by Brockmann Consult
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
package org.esa.beam.chris.operators.internal;

import junit.framework.TestCase;

import static java.lang.Math.sqrt;
import static java.lang.Math.PI;
import static java.lang.Math.exp;

/**
 * New class.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class MultinormalDistributionTest extends TestCase {

    public void test1D() {
        final MultinormalDistribution dist =
                new MultinormalDistribution(new double[]{0.0}, new double[][]{{1.0}});

        final double maximum = 1.0 / sqrt(2.0 * PI);
        assertEquals(maximum, dist.probabilityDensity(new double[]{0.0}), 0.0);

        final double expectedValue = maximum * exp(-0.5);
        assertEquals(expectedValue, dist.probabilityDensity(new double[]{1.0}), 0.0);
    }

    public void test2D() {
        final MultinormalDistribution dist =
                new MultinormalDistribution(new double[]{0.0, 0.0}, new double[][]{{1.0, 0.0}, {0.0, 2.0}});

        final double maximum = 1.0 / (2.0 * PI) / sqrt(2.0);
        assertEquals(maximum, dist.probabilityDensity(new double[]{0.0, 0.0}), 0.0);

        final double expectedValue = maximum * exp(-0.75);
        assertEquals(expectedValue, dist.probabilityDensity(new double[]{1.0, 1.0}), 0.0);
    }
}
