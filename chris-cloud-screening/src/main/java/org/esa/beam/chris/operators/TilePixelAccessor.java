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

import org.esa.beam.chris.operators.internal.PixelAccessor;
import org.esa.beam.framework.gpf.Tile;

/**
 * Class for accessing individual pixels in a tile stack.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class TilePixelAccessor implements PixelAccessor {
    private final Tile[] tiles;

    TilePixelAccessor(Tile[] tiles) {
        this.tiles = tiles;
    }

    @Override
    public double[] addSamples(int i, double[] samples) {
        final int y = getY(i);
        final int x = getX(i);

        for (int k = 0; k < samples.length; ++k) {
            samples[k] += tiles[k].getSampleDouble(tiles[k].getMinX() + x, tiles[k].getMinY() + y);
        }

        return samples;
    }

    @Override
    public double[] getSamples(int i, double[] samples) {
        final int y = getY(i);
        final int x = getX(i);

        for (int k = 0; k < samples.length; ++k) {
            samples[k] = tiles[k].getSampleDouble(tiles[k].getMinX() + x, tiles[k].getMinY() + y);
        }

        return samples;
    }

    @Override
    public int getPixelCount() {
        return tiles[0].getWidth() * tiles[0].getHeight();
    }

    @Override
    public int getSampleCount() {
        return tiles.length;
    }

    private int getX(int i) {
        return i % tiles[0].getWidth();
    }

    private int getY(int i) {
        return i / tiles[0].getWidth();
    }
}
