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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;

class ImageBand extends Band {

    public ImageBand(String name, int dataType, int width, int height) {
        super(name, dataType, width, height);
    }


    /**
     * Sets the rendered image for this {@code Band} and sets the raster
     * data accordingly.
     *
     * @param image the rendered image.
     * @param pm    the progress monitor.
     */
    public void setSourceImage(RenderedImage image, ProgressMonitor pm) {
        super.setSourceImage(image);
        resetRasterData(pm);
    }

    /**
     * Sets the rendered image for this {@code Band} and sets the raster
     * data accordingly.
     *
     * @param image the rendered image.
     */
    @Override
    public void setSourceImage(RenderedImage image) {
        setSourceImage(image, ProgressMonitor.NULL);
    }

    @Override
    public void readRasterData(int offsetX, int offsetY, int width, int height, ProductData rasterData,
                               ProgressMonitor pm) {
        final RenderedImage image = this.getSourceImage();
        final int minTileX = image.getMinTileX();
        final int minTileY = image.getMinTileY();

        final int numXTiles = image.getNumXTiles();
        final int numYTiles = image.getNumYTiles();

        final Rectangle targetRectangle = new Rectangle(offsetX, offsetY, width, height);

        pm.beginTask("Reading raster data", numXTiles * numYTiles);
        try {
            for (int tileX = minTileX; tileX < minTileX + numXTiles; ++tileX) {
                for (int tileY = minTileY; tileY < minTileY + numYTiles; ++tileY) {
                    if (pm.isCanceled()) {
                        return;
                    }
                    final Rectangle tileRectangle = new Rectangle(
                            image.getTileGridXOffset() + tileX * image.getTileWidth(),
                            image.getTileGridYOffset() + tileY * image.getTileHeight(),
                            image.getTileWidth(), image.getTileHeight());

                    final Rectangle rectangle = targetRectangle.intersection(tileRectangle);
                    if (!rectangle.isEmpty()) {
                        final Raster raster = image.getData(rectangle);

                        final int x = rectangle.x;
                        final int y = rectangle.y;
                        final int w = rectangle.width;
                        final int h = rectangle.height;

                        final Object source = raster.getDataElements(x, y, w, h, null);
                        final Object target = rasterData.getElems();

                        for (int i = 0; i < h; ++i) {
                            //noinspection SuspiciousSystemArraycopy
                            System.arraycopy(source, i * w, target, (y + i) * width + x, w);
                        }
                    }
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }

    private void resetRasterData(ProgressMonitor pm) {
        if (getSourceImage() != null) {
            try {
                readRasterDataFully(pm);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            setRasterData(null);
        }
    }
}
