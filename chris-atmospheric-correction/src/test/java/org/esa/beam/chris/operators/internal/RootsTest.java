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
 * Tests for class {@link Roots}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class RootsTest extends TestCase {

    public void testBrent() throws Exception {
        final Roots.Bracket bracket = new Roots.Bracket(0.0, 2.0);
        final boolean success = Roots.brent(new TestFunctions.Cos(), bracket, 100);

        assertTrue(success);
        assertEquals(Math.PI / 2.0, bracket.root, 0.0);
    }

    public void testBrentWithRootAtBracketingIntervalLowerLimit() throws Exception {
        final Roots.Bracket bracket = new Roots.Bracket(0.0, 1.0);
        final boolean success = Roots.brent(new TestFunctions.Sin(), bracket, 100);

        assertTrue(success);
        assertEquals(0.0, bracket.root, 0.0);
    }

    public void testBrentWithRootAtBracketingIntervalUpperLimit() throws Exception {
        final Roots.Bracket bracket = new Roots.Bracket(-1.0, 0.0);
        final boolean success = Roots.brent(new TestFunctions.Sin(), bracket, 100);

        assertTrue(success);
        assertEquals(0.0, bracket.root, 0.0);
    }

    public void testBrentWithRootNotInBracketingInterval() throws Exception {
        try {
            Roots.brent(new TestFunctions.Cos(), new Roots.Bracket(0.0, 1.0), 100);
            fail();
        } catch (IllegalArgumentException expected) {
            // ignore
        }
    }
}
