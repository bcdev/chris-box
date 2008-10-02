package org.esa.beam.chris.operators;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.text.MessageFormat;

class ImageBand extends Band {

    ImageBand(String name, int dataType, int width, int height) {
        super(name, dataType, width, height);
    }

    @Override
    public void setImage(RenderedImage image) {
        super.setImage(image);
        reloadData();
    }

    @Override
    public void readRasterData(int offsetX, int offsetY, int width, int height, ProductData rasterData,
                               ProgressMonitor pm) throws IOException {
        final RenderedImage image = getImage();
        if (image == null) {
            throw new IllegalStateException(MessageFormat.format("No image available for band ''{0}''.", getName()));
        }

        final int minTileX = image.getMinTileX();
        final int minTileY = image.getMinTileY();

        final int numXTiles = image.getNumXTiles();
        final int numYTiles = image.getNumYTiles();

        final Rectangle targetRectangle = new Rectangle(offsetX, offsetY, width, height);

        pm.beginTask("Reading raster data", numXTiles * numYTiles);
        try {
            for (int tileX = minTileX; tileX < minTileX + numXTiles; ++tileX) {
                for (int tileY = minTileY; tileY < minTileY + numYTiles; ++tileY) {
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

    private void reloadData() {
        if (getImage() != null) {
            try {
                readRasterDataFully();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            setRasterData(null);
        }
    }
}
