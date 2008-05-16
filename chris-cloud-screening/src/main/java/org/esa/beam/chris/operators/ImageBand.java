package org.esa.beam.chris.operators;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.*;
import java.io.IOException;

import com.bc.ceres.core.ProgressMonitor;

/**
 * User: Marco Peters
 * Date: 07.05.2008
 */
public class ImageBand extends Band {


    public ImageBand(String name, int dataType, int width, int height) {
        super(name, dataType, width, height);
    }

    @Override
    public void setImage(RenderedImage image) {
        super.setImage(image);
        reloadData();
    }
    @Override
    public void readRasterData(int offsetX, int offsetY, int width, int height, ProductData rasterData, ProgressMonitor pm) throws IOException {
        final Rectangle rectangle = new Rectangle(offsetX, offsetY, width, height);
        final RenderedImage image = getImage();
        final Raster raster = image.getData(rectangle);

        raster.getDataElements(offsetX, offsetY, width, height, rasterData.getElems());
    }

    private void reloadData() {
        if (getImage() != null) {
            try {
                readRasterDataFully();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }else {
            setRasterData(null);
        }
    }

// might be usefull or not
//    @Override
//    public void readRasterDataFully(ProgressMonitor pm) {
//        final RenderedImage image = getImage();
//        if (image == null) {
//            throw new IllegalStateException(MessageFormat.format("No image available for band ''{0}''.", getName()));
//        }
//        if (!hasRasterData()) {
//            setRasterData(createCompatibleRasterData());
//        }
//
//
//        final int minTileX = image.getMinTileX();
//        final int minTileY = image.getMinTileY();
//
//        final int numXTiles = image.getNumXTiles();
//        final int numYTiles = image.getNumYTiles();
//
//        try {
//            pm.beginTask("Reading raster data", numXTiles * numYTiles);
//            for (int tileX = minTileX; tileX < minTileX + numXTiles; ++tileX) {
//                for (int tileY = minTileY; tileY < minTileY + numYTiles; ++tileY) {
////                    final Rectangle tileRectangle = new Rectangle(
////                            image.getTileGridXOffset() + tileX * image.getTileWidth(),
////                            image.getTileGridYOffset() + tileY * image.getTileHeight(),
////                            image.getTileWidth(), image.getTileHeight());
////
//                    final Raster raster = image.getTile(tileX, tileY);
//
//                    final int minX = raster.getMinX();
//                    final int minY = raster.getMinY();
//                    final int width = raster.getWidth();
//                    final int height = raster.getHeight();
//
//                    final Object source = raster.getDataElements(minX, minY, width, height, null);
//                    final Object target = getRasterData().getElems();
//
//                    for (int i = 0; i < height; ++i) {
//                        System.arraycopy(source, i * width, target, (minY + i) * image.getWidth() + minX, width);
//                    }
//                    pm.worked(1);
//                }
//            }
//        } finally {
//            pm.done();
//        }
//    }
}
