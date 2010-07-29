/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.chris.operators.internal;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.ProductData;

import javax.media.jai.ComponentSampleModelJAI;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;

/**
 * Tests for class {@link ImageBand}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ImageBandTest extends TestCase {

    private static final int W = 5;
    private static final int H = 5;
    private static final int TILE_W = 2;
    private static final int TILE_H = 2;

    public void testSetGetImage() {
        final ImageBand imageBand = new ImageBand("test", ProductData.TYPE_INT32, W, H);
        final RenderedImage image = createTestImage(1, W, H);

        // 1. Set image
        imageBand.setSourceImage(image);
        // new image...
        assertSame(image, imageBand.getSourceImage().getImage(0));
        // new data buffer
        final ProductData data = imageBand.getData();
        assertNotNull(data);
        assertEquals(H * W, data.getNumElems());
        assertEachElementEquals(1, data);

        // 2. Set image again
        imageBand.setSourceImage(createTestImage(7, W, H));
        // different image...
        assertNotNull(imageBand.getSourceImage());
        assertNotSame(image, imageBand.getSourceImage());
        // ... but same data buffer...
        assertSame(data, imageBand.getData());
        assertEquals(H * W, data.getNumElems());
        assertEachElementEquals(7, data);
    }

    @SuppressWarnings({"UnnecessaryBoxing"})
    private static RenderedImage createTestImage(int value, int imageW, int imageH) {
        return ConstantDescriptor.create(new Float(imageW),
                                         new Float(imageH),
                                         new Integer[]{value},
                                         createRenderingHints(imageW, imageH, TILE_W, TILE_H));
    }

    private static RenderingHints createRenderingHints(int imageW, int imageH, int tileW, int tileH) {
        final SampleModel sm =
                new ComponentSampleModelJAI(DataBuffer.TYPE_INT, imageW, imageH, 1, imageW, new int[]{0});
        final ImageLayout imageLayout =
                new ImageLayout(0, 0, imageW, imageH, 0, 0, tileW, tileH, sm, PlanarImage.createColorModel(sm));

        final RenderingHints renderingHints = new RenderingHints(JAI.KEY_TILE_CACHE, null);
        renderingHints.put(JAI.KEY_IMAGE_LAYOUT, imageLayout);

        return renderingHints;
    }

    private static void assertEachElementEquals(int expected, ProductData actual) {
        for (int i = 0; i < actual.getNumElems(); ++i) {
            assertEquals(expected, actual.getElemIntAt(i));
        }
    }
}
