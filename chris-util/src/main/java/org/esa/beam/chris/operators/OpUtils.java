package org.esa.beam.chris.operators;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.dataio.chris.ChrisConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Operator utilities.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class OpUtils {

    /**
     * Returns an array of bands in a product of interest whose names start with
     * a given prefix.
     *
     * @param product the product of interest.
     * @param prefix  the prefix.
     *
     * @return the bands found.
     */
    public static Band[] findBands(Product product, String prefix) {
        return findBands(product, prefix, new BandFilter() {
            @Override
            public boolean accept(Band band) {
                return true;
            }
        });
    }

    /**
     * Returns an array of bands in a product of interest whose names start with
     * a given prefix and are accepted by a given band filter.
     *
     * @param product the product of interest.
     * @param prefix  the prefix.
     * @param filter  the band filter.
     *
     * @return the bands found.
     */
    public static Band[] findBands(Product product, String prefix, BandFilter filter) {
        final List<Band> bandList = new ArrayList<Band>();

        for (final Band band : product.getBands()) {
            if (band.getName().startsWith(prefix) && filter.accept(band)) {
                bandList.add(band);
            }
        }

        return bandList.toArray(new Band[bandList.size()]);
    }

    /**
     * Returns a CHRIS annotation as {@code double} for a product of interest.
     *
     * @param product      the product of interest.
     * @param name         the name of the CHRIS annotation.
     * @param defaultValue the default value returned when the annotation was
     *                     not found.
     *
     * @return the annotation as {@code double} or the default value if the
     *         annotation was not found.
     *
     * @throws OperatorException if the annotation was found but could not be parsed.
     */
    public static double getAnnotation(Product product, String name, double defaultValue) {
        final MetadataElement element = product.getMetadataRoot().getElement(ChrisConstants.MPH_NAME);

        if (element == null) {
            return defaultValue;
        }
        final String string = element.getAttributeString(name, null);
        if (string == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(string);
        } catch (Exception e) {
            throw new OperatorException(MessageFormat.format("could not parse CHRIS annotation ''{0}''", name));
        }
    }

    /**
     * Returns a CHRIS annotation as {@code String} for a product of interest.
     *
     * @param product the product of interest.
     * @param name    the name of the CHRIS annotation.
     *
     * @return the annotation as {@code String}.
     *
     * @throws OperatorException if the annotation could not be found.
     */
    public static String getAnnotationString(Product product, String name) throws OperatorException {
        final MetadataElement element = product.getMetadataRoot().getElement(ChrisConstants.MPH_NAME);

        if (element == null) {
            throw new OperatorException(MessageFormat.format("could not find CHRIS annotation ''{0}''", name));
        }
        return element.getAttributeString(name, null);
    }

    /**
     * Sets a CHRIS annotation of a product of interest to a certain value.
     * If the metadata element for the requested CHRIS annotation does not
     * exists, a new metadata element is created.
     *
     * @param product the product of interest.
     * @param name    the name of the CHRIS annotation.
     * @param value   the value.
     */
    public static void setAnnotationString(Product product, String name, String value) {
        MetadataElement element = product.getMetadataRoot().getElement(ChrisConstants.MPH_NAME);
        if (element == null) {
            element = new MetadataElement(ChrisConstants.MPH_NAME);
            product.getMetadataRoot().addElement(element);
        }
        element.setAttributeString(name, value);
    }

    /**
     * Returns a CHRIS annotation as {@code double} value for a product of interest.
     *
     * @param product the product of interest.
     * @param name    the name of the CHRIS annotation.
     *
     * @return the annotation as {@code double} value.
     *
     * @throws OperatorException if the annotation could not be found or parsed.
     */
    public static double getAnnotationDouble(Product product, String name) throws OperatorException {
        final String string = getAnnotationString(product, name);

        try {
            return Double.parseDouble(string);
        } catch (Exception e) {
            throw new OperatorException(MessageFormat.format("could not parse CHRIS annotation ''{0}''", name));
        }
    }

    /**
     * Returns a CHRIS annotation as {@code int} value for a product of interest.
     *
     * @param product the product of interest.
     * @param name    the name of the CHRIS annotation.
     *
     * @return the annotation as {@code int} value.
     *
     * @throws OperatorException if the annotation could not be found or parsed.
     */
    public static int getAnnotationInt(Product product, String name) throws OperatorException {
        final String string = getAnnotationString(product, name);

        try {
            return Integer.parseInt(string);
        } catch (Exception e) {
            throw new OperatorException(MessageFormat.format("could not parse CHRIS annotation ''{0}''", name));
        }
    }

    /**
     * Returns the central wavelenghts for any spectral bands of interest.
     *
     * @param bands the bands of interest.
     *
     * @return the central wavelenghts (nm).
     */
    public static double[] getCentralWavelenghts(Band[] bands) {
        final double[] wavelengths = new double[bands.length];

        for (int i = 0; i < bands.length; i++) {
            wavelengths[i] = bands[i].getSpectralWavelength();
        }

        return wavelengths;
    }

    /**
     * Returns the bandwidths for any spectral bands of interest.
     *
     * @param bands the bands of interest.
     *
     * @return the bandwidths (nm).
     */
    public static double[] getBandwidths(Band[] bands) {
        final double[] bandwidths = new double[bands.length];

        for (int i = 0; i < bands.length; i++) {
            bandwidths[i] = bands[i].getSpectralBandwidth();
        }

        return bandwidths;
    }

    /**
     * Returns the source tiles for given raster data nodes.
     * <p/>
     * Creates a subprogress monitor from the parent progres monitor supplied, with ticks
     * equal to the number of tiles requested.
     *
     * @param nodes           the raster data nodes.
     * @param sourceRectangle the source rectangle.
     * @param pm              the parent progress monitor.
     * @param taskName        the task name used for the subprogress monitor.
     * @param operator        the operator.
     *
     * @return the source tiles.
     */
    private static Tile[] getSourceTiles(final RasterDataNode[] nodes, Rectangle sourceRectangle, ProgressMonitor pm,
                                         final String taskName, Operator operator) {
        final ProgressMonitor spm = SubProgressMonitor.create(pm, nodes.length);

        try {
            spm.beginTask(taskName, nodes.length);

            final Tile[] sourceTiles = new Tile[nodes.length];
            for (int i = 0; i < nodes.length; i++) {
                sourceTiles[i] = operator.getSourceTile(nodes[i], sourceRectangle, spm);
                spm.worked(1);
            }

            return sourceTiles;
        } finally {
            spm.done();
        }
    }

    /**
     * Returns an {@link ImageInputStream} for a resource file of interest.
     *
     * @param name the name of the resource file of interest.
     *
     * @return the image input stream.
     *
     * @throws OperatorException if the resource could not be found or the
     *                           image input stream could not be created.
     */
    public static ImageInputStream getResourceAsImageInputStream(String name) throws OperatorException {
        final InputStream is = OpUtils.class.getResourceAsStream(name);

        if (is == null) {
            throw new OperatorException(MessageFormat.format("resource {0} not found", name));
        }
        try {
            return new FileCacheImageInputStream(is, null);
        } catch (Exception e) {
            throw new OperatorException(MessageFormat.format(
                    "could not create image input stream for resource {0}", name), e);
        }
    }
}