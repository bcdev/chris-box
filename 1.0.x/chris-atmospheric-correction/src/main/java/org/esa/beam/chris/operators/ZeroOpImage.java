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

import javax.media.jai.*;
import java.awt.*;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;

/**
 * Zero image.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class ZeroOpImage extends SourcelessOpImage {

    /**
     * Creates an image filled with zeros.
     *
     * @param w the image width.
     * @param h the image height.
     *
     * @return the image.
     */
    public static OpImage createImage(int w, int h) {
        final SampleModel sampleModel = new ComponentSampleModelJAI(DataBuffer.TYPE_BYTE, w, h, 1, w, new int[]{0});
        final ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        final ImageLayout imageLayout = new ImageLayout(0, 0, w, h, 0, 0, w, h, sampleModel, colorModel);

        return new ZeroOpImage(imageLayout, null, sampleModel, 0, 0, w, h);
    }

    private ZeroOpImage(ImageLayout imageLayout, Map map, SampleModel sampleModel, int minX, int minY, int w, int h) {
        super(imageLayout, map, sampleModel, minX, minY, w, h);
    }

    @Override
    protected void computeRect(PlanarImage[] planarImages, WritableRaster target, Rectangle targetRectangle) {
    }
}
