package org.esa.beam.chris.ui;

import org.esa.beam.dataio.chris.ChrisConstants;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class NoiseReductionPresenter {

    private List<Product> productList;
    private ListSelectionModel productSelectionModel;
    private String[][] metadata;

    public NoiseReductionPresenter(Product[] products) {
        productList = new ArrayList<Product>(5);
        for (Product product : products) {
            addProduct(product);
        }

        productSelectionModel = new DefaultListSelectionModel();
        productSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        metadata = new String[5][2];
        metadata[0][0] = ChrisConstants.ATTR_NAME_CHRIS_MODE;
        metadata[1][0] = ChrisConstants.ATTR_NAME_TARGET_NAME;
        metadata[2][0] = "Target Coordinates";
        metadata[3][0] = ChrisConstants.ATTR_NAME_NOMINAL_FLY_BY_ZENITH_ANGLE;
        metadata[4][0] = ChrisConstants.ATTR_NAME_MINIMUM_ZENITH_ANGLE;
    }

    public Product[] getProducts() {
        return productList.toArray(new Product[productList.size()]);
    }

    public void addPropteryChangeListener(PropertyChangeListener propertyChangeListener) {
        //To change body of created methods use File | Settings | File Templates.
    }

    public void addProduct(Product product) {
        productList.add(product);
    }

    public void removeSelectedProduct() {
        productList.remove(getSelectionIndex());

        setSelectionIndex(computeSelectionIndex());
    }

    private int computeSelectionIndex() {
        if (productList.isEmpty()) {
            return -1;
        } else {
            if (getSelectionIndex() > 0) {
                return getSelectionIndex() - 1;
            }

            return getSelectionIndex();
        }
    }

    public String[][] getMetadata() {
        Product selectedProduct = productList.get(getSelectionIndex());

        metadata[0][1] = "Not available";
        metadata[1][1] = "Not available";
        metadata[2][1] = "Not available";
        metadata[3][1] = "Not available";
        metadata[4][1] = "Not available";

        MetadataElement root = selectedProduct.getMetadataRoot();
        if (root == null) {
            return metadata;
        }

        MetadataElement mph = root.getElement(ChrisConstants.MPH_NAME);
        if (mph == null) {
            return metadata;
        }

        metadata[0][1] = mph.getAttributeString(ChrisConstants.ATTR_NAME_CHRIS_MODE, "Not available");
        metadata[1][1] = mph.getAttributeString(ChrisConstants.ATTR_NAME_TARGET_NAME, "Not available");
        metadata[2][1] = getTargetCoordinatesString(mph, "Not available");
        metadata[3][1] = mph.getAttributeString(ChrisConstants.ATTR_NAME_NOMINAL_FLY_BY_ZENITH_ANGLE, "Not available");;
        metadata[4][1] = mph.getAttributeString(ChrisConstants.ATTR_NAME_MINIMUM_ZENITH_ANGLE, "Not available");

        return metadata;
    }

    private static String getTargetCoordinatesString(MetadataElement element, String defaultValue) {
        String str = defaultValue;

        String lat = element.getAttributeString(ChrisConstants.ATTR_NAME_TARGET_LAT, null);
        String lon = element.getAttributeString(ChrisConstants.ATTR_NAME_TARGET_LON, null);

        if (lat != null && lon != null) {
            try {
                str = new GeoPos(Float.parseFloat(lat), Float.parseFloat(lon)).toString();
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        return str;
    }

    public int getSelectionIndex() {
        return productSelectionModel.getLeadSelectionIndex();
    }

    public void setSelectionIndex(int i) {
        productSelectionModel.setSelectionInterval(i, i);
    }
}
