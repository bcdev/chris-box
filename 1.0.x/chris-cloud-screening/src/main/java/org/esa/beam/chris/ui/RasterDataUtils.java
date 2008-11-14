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
package org.esa.beam.chris.ui;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;

import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

/**
 * Utilities for copying data from a {@link RenderedImage} into a {@link ProductData}
 * raster.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.5
 */
class RasterDataUtils {

    static ProductData createRasterData(RenderedImage sourceImage, ProgressMonitor pm) {
        final int type = ImageManager.getProductDataType(sourceImage.getSampleModel().getDataType());
        final int w = sourceImage.getWidth();
        final int h = sourceImage.getHeight();

        final ProductData targetRaster = ProductData.createInstance(type, w * h);
        setRasterData(targetRaster, sourceImage, pm);

        return targetRaster;
    }

    static void setRasterData(ProductData targetRaster, RenderedImage sourceImage, ProgressMonitor pm) {
        final Rectangle sourceRectangle = new Rectangle(0, 0, sourceImage.getWidth(), sourceImage.getHeight());
        setRasterData(targetRaster, sourceImage, sourceRectangle, pm);
    }

    static void setRasterData(ProductData targetRaster, RenderedImage sourceImage, Rectangle sourceRectangle,
                                     ProgressMonitor pm) {
        final int minTileX = sourceImage.getMinTileX();
        final int minTileY = sourceImage.getMinTileY();

        final int numXTiles = sourceImage.getNumXTiles();
        final int numYTiles = sourceImage.getNumYTiles();

        try {
            pm.beginTask("Setting raster data...", numXTiles * numYTiles);

            for (int tileX = minTileX; tileX < minTileX + numXTiles; ++tileX) {
                for (int tileY = minTileY; tileY < minTileY + numYTiles; ++tileY) {
                    if (pm.isCanceled()) {
                        return;
                    }
                    final Rectangle tileRectangle = new Rectangle(
                            sourceImage.getTileGridXOffset() + tileX * sourceImage.getTileWidth(),
                            sourceImage.getTileGridYOffset() + tileY * sourceImage.getTileHeight(),
                            sourceImage.getTileWidth(), sourceImage.getTileHeight());
                    final Rectangle clipRectangle = sourceRectangle.intersection(tileRectangle);

                    if (!clipRectangle.isEmpty()) {
                        final Raster sourceRaster = sourceImage.getData(clipRectangle);

                        final int x = clipRectangle.x;
                        final int y = clipRectangle.y;
                        final int w = clipRectangle.width;
                        final int h = clipRectangle.height;

                        final Object sourceData = sourceRaster.getDataElements(x, y, w, h, null);
                        final Object targetData = targetRaster.getElems();

                        for (int i = 0; i < h; ++i) {
                            //noinspection SuspiciousSystemArraycopy
                            System.arraycopy(sourceData, i * w, targetData, (y + i) * sourceRectangle.width + x, w);
                        }
                    }
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }
}
