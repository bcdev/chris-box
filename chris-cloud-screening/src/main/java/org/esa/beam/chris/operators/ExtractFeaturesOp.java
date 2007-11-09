package org.esa.beam.chris.operators;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

/**
 * Operator for extracting features from TOA reflectances needed for
 * cloud screening.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
@OperatorMetadata(alias = "chris.ExtractFeatures",
                  version = "1.0",
                  authors = "Ralf Quast",
                  copyright = "(c) 2007 by Brockmann Consult",
                  description = "Extracts features from TOA reflectances needed for cloud screening.")
public class ExtractFeaturesOp extends Operator {

    @SourceProduct(alias = "input")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;
    @Parameter
    private String targetProductName;

    public void initialize() throws OperatorException {
        /*
%Band Selection
 bandas=1:Nban;
 bands_rm=[];
 bands_rm=[bands_rm find( WlMid>590 & WlMid<600)];  % 590-600  nm - low atmospheric absorption
 bands_rm=[bands_rm find( WlMid>630 & WlMid<636)];  % 630-636  nm - low atmospheric absorption
 bands_rm=[bands_rm find( WlMid>648 & WlMid<658)];  % 648-658  nm - low atmospheric absorption
 bands_rm=[bands_rm find( WlMid>686 & WlMid<709)];  % 686-709  nm - low atmospheric absorption
 bands_rm=[bands_rm find( WlMid>716 & WlMid<741)];  % 716-741  nm - low atmospheric absorption
 bands_rm=[bands_rm find( WlMid>756 & WlMid<775)];  % 756-775  nm - O2 atmospheric absorption
 bands_rm=[bands_rm find( WlMid>792 & WlMid<799)];  % 792-799  nm - low atmospheric absorption
 bands_rm=[bands_rm find( WlMid>808 & WlMid<840)];  % 808-840  nm - H20 atmospheric absorption
 bands_rm=[bands_rm find( WlMid>885 & WlMid<985)];  % 885-985  nm - H20 atmospheric absorption
 bands_rm=[bands_rm find( WlMid>400 & WlMid<440)];  % 400-440  nm - sensor noise
 bands_rm=[bands_rm find( WlMid>985 & WlMid<1010)]; % 985-1010 nm - calibration errors
 bandas=setdiff(bandas,bands_rm);
 bandasVIS=find( WlMid>400 & WlMid<700 );
 bandasNIR=setdiff(bandas,bandasVIS);
 bandasVIS=setdiff(bandas,bandasNIR);

 % Surface reflectance features
 Deriv=spectralfeatures(X(:,:,bandas),WlMid(bandas),{'whitedif'});
 Intens=spectralfeatures(X(:,:,bandas),WlMid(bandas),{'integral'});
 DerivVIS=spectralfeatures(X(:,:,bandasVIS),WlMid(bandasVIS),{'whitedif'});
 IntensVIS=spectralfeatures(X(:,:,bandasVIS),WlMid(bandasVIS),{'integral'});
 DerivNIR=spectralfeatures(X(:,:,bandasNIR),WlMid(bandasNIR),{'whitedif'});
 IntensNIR=spectralfeatures(X(:,:,bandasNIR),WlMid(bandasNIR),{'integral'});

 %Atmospheric absorptions
 % m=1/mu=1/cos(illum)+1/cos(obs): Optical mass
 if exist('ObservationZenithAngle'), mu=1/(1/cos(SolarZenithAngle/180*pi)+1/cos(ObservationZenithAngle/180*pi));
 else, mu=1/(1/cos(SolarZenithAngle/180*pi)); end
 %O2 atmospheric absorption
 W_out_inf=[738 755]; W_in=[755 770]; W_out_sup=[770 788];  W_max=[760.625]; %O2
 OP_O2=mu*optical_path(X,WlMid,BWidth,W_out_inf,W_in,W_out_sup,W_max);
 %H2O atmospheric absorption
 W_out_inf=[865 890]; W_in=[895 960]; W_out_sup=[985 1100]; W_max=[944.376]; %H2O
 OP_H2O=mu*optical_path(X,WlMid,BWidth,W_out_inf,W_in,W_out_sup,W_max);

 % Save Features (intermediate product)
 if 1
   keywords_feat=crear_keywords;
   keywords_feat=add_keyword(keywords_feat,'description',['Features of CHRIS/PROBA Mode' Mode ' FZA' FZA ' ' nombre]);
   keywords_feat=add_keyword(keywords_feat,'map_info',map_info);
   keywords_feat=add_keyword(keywords_feat,'band names',{'Intens','Deriv','IntensVIS','DerivVIS','IntensNIR','DerivNIR','O2Absorp','H2OAbsorp'});
   matlab2envi(cat(3,Intens,Deriv,IntensVIS,DerivVIS,IntensNIR,DerivNIR,OP_O2,OP_H2O),[pathres nombre '_feat.bsq'],keywords_feat,'NoImClas');
 end
         */
        assertValidity(sourceProduct);

        final String type = sourceProduct.getProductType().replace("_REFL", "_FEAT");
        targetProduct = new Product(targetProductName, type,
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        ProductUtils.copyMetadata(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());
    }

    static void assertValidity(Product product) {
        if (!product.getProductType().matches("CHRIS_M[1-5]A?_REFL")) {
            throw new OperatorException(MessageFormat.format(
                    "product ''{0}'' is not of appropriate type", product.getName()));
        }
    }

    // todo - move
    static double[][] readTransmittanceTable() throws OperatorException {
        final ImageInputStream iis = getResourceAsImageInputStream("toa-nir-transmittance.img");

        try {
            final int length = iis.readInt();
            final double[] abscissas = new double[length];
            final double[] ordinates = new double[length];

            iis.readFully(abscissas, 0, length);
            iis.readFully(ordinates, 0, length);

            return new double[][]{abscissas, ordinates};
        } catch (Exception e) {
            throw new OperatorException("could not read extraterrestrial solar irradiance table", e);
        } finally {
            try {
                iis.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    // todo - move
    /**
     * Returns an {@link ImageInputStream} for a resource file of interest.
     *
     * @param name the name of the resource file of interest.
     * @return the image input stream.
     * @throws OperatorException if the resource could not be found or the
     *                           image input stream could not be created.
     */
    private static ImageInputStream getResourceAsImageInputStream(String name) throws OperatorException {
        final InputStream is = ExtractFeaturesOp.class.getResourceAsStream(name);

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


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ExtractFeaturesOp.class);
        }
    }
}
