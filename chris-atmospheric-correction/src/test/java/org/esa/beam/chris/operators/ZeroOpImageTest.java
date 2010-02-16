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
package org.esa.beam.chris.operators;

import junit.framework.TestCase;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;

/**
 * Tests for class {@link ZeroOpImage}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class ZeroOpImageTest extends TestCase {
    private static final int W = 2;
    private static final int H = 2;

    public void testImageComputation() {
        final RenderedImage image = createTestImage();
        final Raster raster = image.getData();

        assertEquals(0, raster.getSample(0, 0, 0));
        assertEquals(0, raster.getSample(1, 0, 0));
        assertEquals(0, raster.getSample(0, 1, 0));
        assertEquals(0, raster.getSample(1, 1, 0));
    }

    private static RenderedImage createTestImage() {
        return ZeroOpImage.createImage(W, H);
    }
}
