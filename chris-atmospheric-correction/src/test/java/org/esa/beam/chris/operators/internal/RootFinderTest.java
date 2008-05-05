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
package org.esa.beam.chris.operators.internal;

import junit.framework.TestCase;

/**
 * Tests for class {@link RootFinder}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class RootFinderTest extends TestCase {
    private RootFinder rootFinder;

    public void testFindRoot() throws Exception {
        assertEquals(Math.PI / 2.0, rootFinder.findRoot(new Cosine(), 0.0, 2.0), 0.0);
    }

    public void testFindRootAtIntervalBoundary() throws Exception {
        assertEquals(0.0, rootFinder.findRoot(new Sine(), 0.0, 1.0), 0.0);
    }

    public void testFindRootWithIllegalArguments() throws Exception {
        try {
            rootFinder.findRoot(new Cosine(), 0.0, 1.0);
            fail();
        } catch (IllegalArgumentException expected) {
            // ignore
        }
    }

    @Override
    protected void setUp() throws Exception {
        rootFinder = new RootFinder(100);
    }

    private static class Cosine implements Function {

        public double value(double x) {
            return Math.cos(x);
        }
    }

    private static class Sine implements Function {

        public double value(double x) {
            return Math.sin(x);
        }
    }
}
