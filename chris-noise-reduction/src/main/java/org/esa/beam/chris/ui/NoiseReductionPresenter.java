package org.esa.beam.chris.ui;

import org.esa.beam.dataio.chris.ChrisConstants;
import org.esa.beam.dataio.chris.ChrisProductReaderPlugIn;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @version $Revision:$ $Date:$
 */
class NoiseReductionPresenter {

    private DefaultTableModel productTableModel;
    private ListSelectionModel productTableSelectionModel;

    private DefaultTableModel metadataTableModel;

    private Action addProductAction;
    private Action removeProductAction;
    private Action settingsAction;

    private Window window;

    private AdvancedSettingsPresenter advancedSettingsPresenter;

    public NoiseReductionPresenter(Product[] products, AdvancedSettingsPresenter advancedSettingsPresenter) {
        Object[][] productsData = new Object[products.length][2];
        if (products.length > 0) {
            for (int i = 0; i < productsData.length; i++) {
                productsData[i][0] = true;
                productsData[i][1] = products[i];
            }
        }

        productTableModel = new DefaultTableModel(productsData, new String[]{"Correct", "Product Name"});
        productTableSelectionModel = new DefaultListSelectionModel();
        productTableSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTableSelectionModel.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateMetadata();
            }
        });

        String[][] metadata = new String[5][2];
        metadata[0][0] = ChrisConstants.ATTR_NAME_CHRIS_MODE;
        metadata[1][0] = ChrisConstants.ATTR_NAME_TARGET_NAME;
        metadata[2][0] = "Target Coordinates";
        metadata[3][0] = ChrisConstants.ATTR_NAME_FLY_BY_ZENITH_ANGLE;
        metadata[4][0] = ChrisConstants.ATTR_NAME_MINIMUM_ZENITH_ANGLE;
        metadataTableModel = new DefaultTableModel(metadata, new String[]{"Name", "Value"});

        addProductAction = new AddProductAction(this);
        removeProductAction = new RemoveProductAction(this);
        settingsAction = new AdvancedSettingsAction(this);

        this.advancedSettingsPresenter = advancedSettingsPresenter;
        if (products.length > 0) {
            productTableSelectionModel.setSelectionInterval(0, 0);
        }
    }

    public Map<String, Object> getDestripingParameterMap() {
        return advancedSettingsPresenter.getDestripingParameterMap();
    }

    public Map<String, Object> getDropoutCorrectionParameterMap() {
        return advancedSettingsPresenter.getDropoutCorrectionParameterMap();
    }

    public DefaultTableModel getProductTableModel() {
        return productTableModel;
    }

    public ListSelectionModel getProductTableSelectionModel() {
        return productTableSelectionModel;
    }

    public DefaultTableModel getMetadataTableModel() {
        return metadataTableModel;
    }

    public Action getAddProductAction() {
        return addProductAction;
    }

    public Action getRemoveProductAction() {
        return removeProductAction;
    }

    public Action getSettingsAction() {
        return settingsAction;
    }

    public AdvancedSettingsPresenter getAdvancedSettingsPresenter() {
        return new AdvancedSettingsPresenter();
    }

    public void setAdvancedSettingsPresenter(AdvancedSettingsPresenter advancedSettingsPresenter) {
        this.advancedSettingsPresenter = advancedSettingsPresenter;
    }

    public void setWindow(Window window) {
        this.window = window;
    }

    public Window getWindow() {
        return window;
    }

    public Product[] getProducts() {
        Vector productVector = getProductTableModel().getDataVector();
        Product[] products = new Product[productVector.size()];
        for (int i = 0; i < productVector.size(); i++) {
            products[i] = (Product) ((Vector) productVector.get(i)).get(1);
        }
        return products;
    }

    public Product[] getProductsToBeCorrected() {
        final DefaultTableModel tableModel = getProductTableModel();
        final List<Product> productList = new ArrayList<Product>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if ((Boolean) tableModel.getValueAt(i, 0)) {
                productList.add((Product) tableModel.getValueAt(i, 1));
            }
        }

        return productList.toArray(new Product[productList.size()]);
    }

    void setProductAsOutput(Product product, boolean output) {
        for (int i = 0; i < getProductTableModel().getRowCount(); i++) {
            Product current = (Product) getProductTableModel().getValueAt(i, 1);
            if (current.equals(product)) {
                getProductTableModel().setValueAt(output, i, 0);
                return;
            }
        }
    }

    boolean isProductAsOutputSet(Product product) {
        for (int i = 0; i < getProductTableModel().getRowCount(); i++) {
            Product current = (Product) getProductTableModel().getValueAt(i, 1);
            if (current.equals(product)) {
                Object isOutput = getProductTableModel().getValueAt(i, 0);
                if (isOutput != null) {
                    return (Boolean) isOutput;
                }
            }
        }
        return false;
    }

    void addProduct(Product product) throws NoiseReductionValidationException {
        Product[] products = getProducts();
        if (products.length >= 5) {
            throw new NoiseReductionValidationException("Acquisition set already contains five products.");
        }
        if (products.length != 0 && !areFromSameAcquisition(products[0], product)) {
            throw new NoiseReductionValidationException("Product does not belong to the acquisition set.");
        }
        if (containsProduct(products, product)) {
            throw new NoiseReductionValidationException("Product is already contained in the acquisition set.");
        }

        if (!NoiseReductionAction.CHRIS_TYPES.contains(product.getProductType())) {
            StringBuilder sb = new StringBuilder(50);
            for (java.util.Iterator it = NoiseReductionAction.CHRIS_TYPES.iterator(); it.hasNext();) {
                String type = (String) it.next();
                sb.append("\"").append(type).append("\"");
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
            throw new NoiseReductionValidationException(
                    "Product type '" + product.getProductType() + "'is not valid .\n" +
                    "Must be one of " + sb + "\n");
        }
        DefaultTableModel tableModel = getProductTableModel();
        tableModel.addRow(new Object[]{Boolean.TRUE, product});
        updateSelection(tableModel.getRowCount() - 1);
    }

    void removeSelectedProduct() {
        int selectionIndex = getProductTableSelectionModel().getLeadSelectionIndex();
        getProductTableModel().removeRow(selectionIndex);
        updateSelection(selectionIndex);
    }

    private void updateSelection(int selectionIndex) {
        if (selectionIndex == getProductTableModel().getRowCount()) {
            --selectionIndex;
        }
        if (selectionIndex != -1) {
            getProductTableSelectionModel().setSelectionInterval(selectionIndex, selectionIndex);
        } else {
            getProductTableSelectionModel().clearSelection();
        }
    }

    private static boolean containsProduct(Product[] products, Product product) {
        for (Product aProduct : products) {
            if (aProduct.getName().equals(product.getName())) {
                return true;
            }
        }
        return false;
    }

    static boolean areFromSameAcquisition(Product referenceProduct, Product product) {
        return product.getProductType().equals(referenceProduct.getProductType()) &&
               areFromSameAcquisition(referenceProduct.getFileLocation(), product.getFileLocation());
    }

    static boolean areFromSameAcquisition(File referenceFile, File file) {
        if (referenceFile != null && file != null) {
            String[] expectedParts = referenceFile.getName().split("_", 5);
            String[] actualParts = file.getName().split("_", 5);
            if (expectedParts.length == 5 && expectedParts.length == actualParts.length) {
                return expectedParts[0].equals(actualParts[0])
                       && expectedParts[1].equals(actualParts[1])
                       && expectedParts[2].equals(actualParts[2])
                       // actualParts[3] should be different
                       && expectedParts[4].equals(actualParts[4]);
            }
        }
        return false;
    }


    private static class AddProductAction extends AbstractAction {

        private static String LAST_OPEN_DIR_KEY = "chris.ui.file.lastOpenDir";
        private static String CHRIS_IMPORT_DIR_KEY = "user." + ChrisConstants.FORMAT_NAME.toLowerCase() + ".import.dir";

        private NoiseReductionPresenter presenter;

        public AddProductAction(NoiseReductionPresenter presenter) {
            super("Add...");
            this.presenter = presenter;

        }

        public void actionPerformed(ActionEvent e) {
            if (presenter.getProducts().length == 5) {
                JOptionPane.showMessageDialog((Component) e.getSource(), "You cannot select more than five products.");
                return;
            }
            BeamFileFilter fileFilter = new BeamFileFilter(ChrisConstants.FORMAT_NAME,
                                                           ChrisConstants.DEFAULT_FILE_EXTENSION,
                                                           new ChrisProductReaderPlugIn().getDescription(null));
            BeamFileChooser fileChooser = new BeamFileChooser();
            String chrisImportDir = VisatApp.getApp().getPreferences().getPropertyString(CHRIS_IMPORT_DIR_KEY,
                                                                                         SystemUtils.getUserHomeDir().getPath());
            String lastDir = VisatApp.getApp().getPreferences().getPropertyString(LAST_OPEN_DIR_KEY, chrisImportDir);
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setFileFilter(fileFilter);
            fileChooser.setCurrentDirectory(new File(lastDir));

            if (BeamFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(presenter.getWindow())) {
                File[] selectedFiles = fileChooser.getSelectedFiles();
                for (File file : selectedFiles) {
                    Product product = null;
                    try {
                        product = ProductIO.readProduct(file, null);
                    } catch (IOException e1) {
                        JOptionPane.showMessageDialog((Component) e.getSource(), e1.getMessage());
                    }
                    if (product != null) {
                        try {
                            presenter.addProduct(product);
                        } catch (NoiseReductionValidationException e1) {
                            JOptionPane.showMessageDialog((Component) e.getSource(),
                                                          "Cannot add product.\n" + e1.getMessage());
                        }
                    }
                }
                VisatApp.getApp().getPreferences().setPropertyString(LAST_OPEN_DIR_KEY,
                                                                     fileChooser.getCurrentDirectory().getPath());
            }

        }
    }

    private class RemoveProductAction extends AbstractAction {

        private NoiseReductionPresenter presenter;

        public RemoveProductAction(NoiseReductionPresenter presenter) {
            super("Remove");
            this.presenter = presenter;
        }

        public void actionPerformed(ActionEvent e) {
            presenter.removeSelectedProduct();
        }
    }

    private class AdvancedSettingsAction extends AbstractAction {

        private NoiseReductionPresenter presenter;

        public AdvancedSettingsAction(NoiseReductionPresenter presenter) {
            super("Advanced Settings...");
            this.presenter = presenter;
        }

        public void actionPerformed(ActionEvent e) {
            ModalDialog dialog = new ModalDialog(presenter.getWindow(),
                                                 "Advanced Settings",
                                                 ModalDialog.ID_OK_CANCEL_HELP,
                                                 "chrisNoiseReductionAdvancedSettings");
            AdvancedSettingsPresenter settingsPresenter = presenter.getAdvancedSettingsPresenter();
            AdvancedSettingsPresenter workingCopy = settingsPresenter.createCopy();
            dialog.setContent(new AdvancedSettingsPanel(workingCopy));
            if (dialog.show() == ModalDialog.ID_OK) {
                presenter.setAdvancedSettingsPresenter(workingCopy);
            }
        }
    }


    private void updateMetadata() {
        final String na = "Not available";
        Product selectedProduct;
        if (productTableSelectionModel.getMaxSelectionIndex() == -1) {
            return;
        }
        selectedProduct = (Product) productTableModel.getValueAt(productTableSelectionModel.getMaxSelectionIndex(), 1);

        MetadataElement root = selectedProduct.getMetadataRoot();
        if (root == null) {
            return;
        }
        MetadataElement mph = root.getElement(ChrisConstants.MPH_NAME);
        if (mph == null) {
            return;
        }

        metadataTableModel.setValueAt(mph.getAttributeString(ChrisConstants.ATTR_NAME_CHRIS_MODE, na), 0, 1);
        metadataTableModel.setValueAt(mph.getAttributeString(ChrisConstants.ATTR_NAME_TARGET_NAME, na), 1, 1);
        metadataTableModel.setValueAt(getTargetCoordinatesString(mph, na), 2, 1);
        metadataTableModel.setValueAt(mph.getAttributeString(ChrisConstants.ATTR_NAME_FLY_BY_ZENITH_ANGLE, na),
                                      3, 1);
        metadataTableModel.setValueAt(mph.getAttributeString(ChrisConstants.ATTR_NAME_MINIMUM_ZENITH_ANGLE, na), 4, 1);
    }

    private static String getTargetCoordinatesString(MetadataElement element, String defaultValue) {
        String str = defaultValue;

        String lat = element.getAttributeString(ChrisConstants.ATTR_NAME_TARGET_LAT, null);
        String lon = element.getAttributeString(ChrisConstants.ATTR_NAME_TARGET_LON, null);

        if (lat != null && lon != null) {
            try {
                GeoPos geoPos = new GeoPos(Float.parseFloat(lat), Float.parseFloat(lon));
                str = geoPos.getLatString() + ", " + geoPos.getLonString();
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        return str;
    }


}
