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
    public void readRasterData(int x, int y, int w, int h, ProductData data, ProgressMonitor pm) throws IOException {
        final Rectangle rectangle = new Rectangle(x, y, w, h);
        final RenderedImage image = getImage();
        final Raster raster = image.getData(rectangle);

        raster.getDataElements(x, y, w, h, data.getElems());
    }

    private void reloadData() {
        if (hasRasterData()) {
            try {
                readRasterDataFully();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

}
