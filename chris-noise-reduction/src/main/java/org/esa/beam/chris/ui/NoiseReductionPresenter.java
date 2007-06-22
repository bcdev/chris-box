package org.esa.beam.chris.ui;

import org.esa.beam.dataio.chris.ChrisConstants;
import org.esa.beam.dataio.chris.ChrisProductReaderPlugIn;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class NoiseReductionPresenter {

    private List<Product> productList;
    private ListSelectionModel productSelectionModel;
    private String[][] metadata;
    private Action addProductAction;

    public NoiseReductionPresenter(Product[] products) {
        productList = new ArrayList<Product>(5);

        productSelectionModel = new DefaultListSelectionModel();
        productSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        if (products.length > 0) {
            for (Product product : products) {
                addProduct(product);
            }
            productSelectionModel.setSelectionInterval(0, 0);
        }

        metadata = new String[5][2];
        metadata[0][0] = ChrisConstants.ATTR_NAME_CHRIS_MODE;
        metadata[1][0] = ChrisConstants.ATTR_NAME_TARGET_NAME;
        metadata[2][0] = "Target Coordinates";
        metadata[3][0] = ChrisConstants.ATTR_NAME_NOMINAL_FLY_BY_ZENITH_ANGLE;
        metadata[4][0] = ChrisConstants.ATTR_NAME_MINIMUM_ZENITH_ANGLE;

        addProductAction = new AddProductAction(this);
    }

    public Product[] getProducts() {
        return productList.toArray(new Product[productList.size()]);
    }

    public String[] getProductNames() {
        String[] productNames = new String[productList.size()];
        for (int i = 0; i < productList.size(); i++) {
            Product product = productList.get(i);
            productNames[i] = product.getName();
        }
        return productNames;
    }

    public void addPropteryChangeListener(PropertyChangeListener propertyChangeListener) {
        //To change body of created methods use File | Settings | File Templates.
    }

    public void addProduct(Product product) {
        productList.add(product);
        int newIndex = productList.size() - 1;
        productSelectionModel.setSelectionInterval(newIndex, newIndex);
    }

    public void removeSelectedProduct() {
        productList.remove(getSelectionIndex());

        setSelectionIndex(computeSelectionIndex());
    }

    private int computeSelectionIndex() {
        if (productList.isEmpty()) {
            return -1;
        } else {
            if (getSelectionIndex() == productList.size()) {
                return getSelectionIndex() - 1;
            }

            return getSelectionIndex();
        }
    }

    public String[][] getMetadata() {
        final String na = "Not available";
        Product selectedProduct = null;
        if (getSelectionIndex() == -1) {
            metadata[0][1] = "";
            metadata[1][1] = "";
            metadata[2][1] = "";
            metadata[3][1] = "";
            metadata[4][1] = "";
            return metadata;
        }
        selectedProduct = productList.get(getSelectionIndex());

        metadata[0][1] = na;
        metadata[1][1] = na;
        metadata[2][1] = na;
        metadata[3][1] = na;
        metadata[4][1] = na;

        MetadataElement root = selectedProduct.getMetadataRoot();
        if (root == null) {
            return metadata;
        }

        MetadataElement mph = root.getElement(ChrisConstants.MPH_NAME);
        if (mph == null) {
            return metadata;
        }

        metadata[0][1] = mph.getAttributeString(ChrisConstants.ATTR_NAME_CHRIS_MODE, na);
        metadata[1][1] = mph.getAttributeString(ChrisConstants.ATTR_NAME_TARGET_NAME, na);
        metadata[2][1] = getTargetCoordinatesString(mph, na);
        metadata[3][1] = mph.getAttributeString(ChrisConstants.ATTR_NAME_NOMINAL_FLY_BY_ZENITH_ANGLE, na);
        metadata[4][1] = mph.getAttributeString(ChrisConstants.ATTR_NAME_MINIMUM_ZENITH_ANGLE, na);

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
        return productSelectionModel.getMaxSelectionIndex();
    }

    public void setSelectionIndex(int i) {
        if (i == -1) {
            productSelectionModel.clearSelection();
        } else {
            productSelectionModel.setSelectionInterval(i, i);
        }
    }

    public ListSelectionModel getSelectionModel() {
        return productSelectionModel;
    }

    public Action getAddProductAction() {
        return addProductAction;
    }

    private static class AddProductAction extends AbstractAction {

        private static String LAST_OPEN_DIR = "chris.ui.file.lastOpenDir";
        private NoiseReductionPresenter presenter;

        public AddProductAction(NoiseReductionPresenter presenter) {
            this.presenter = presenter;
        }

        public void actionPerformed(ActionEvent e) {
            BeamFileFilter fileFilter = new BeamFileFilter(ChrisConstants.FORMAT_NAME,
                                                           ChrisConstants.DEFAULT_FILE_EXTENSION,
                                                           new ChrisProductReaderPlugIn().getDescription(null)) {
                @Override
                public boolean accept(File file) {
                    boolean accept = super.accept(file);

                    if (!accept || presenter.getProducts().length == 0) {
                        return accept;
                    }

                    return acceptFileName(file);
                }

                private boolean acceptFileName(File file) {
                    File fileLocation = presenter.getProducts()[0].getFileLocation();
                    String[] expectedParts = fileLocation.getName().split("_", 4);
                    String[] actualParts = file.getName().split("_", 4);

                    return expectedParts[0].equals(actualParts[0])
                           && expectedParts[1].equals(actualParts[1]) 
                           && expectedParts[2].equals(actualParts[2])
                           && expectedParts[4].equals(actualParts[4]);
                }
            };

            BeamFileChooser fileChooser = new BeamFileChooser();
            String lastDir = VisatApp.getApp().getPreferences().getPropertyString(LAST_OPEN_DIR,
                                                                                  SystemUtils.getUserHomeDir().getPath());
            fileChooser.setFileFilter(fileFilter);
            fileChooser.setCurrentDirectory(new File(lastDir));

            if (BeamFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(null)) {
                File[] selectedFiles = fileChooser.getSelectedFiles();
                for (File file : selectedFiles) {
                    try {
                        Product product = ProductIO.readProduct(file, null);
                        presenter.addProduct(product);
                    } catch (IOException e1) {
                        //todo
                    }
                }

                VisatApp.getApp().getPreferences().setPropertyString(LAST_OPEN_DIR,
                                                                     fileChooser.getCurrentDirectory().getPath());
            }

        }

    }
}
