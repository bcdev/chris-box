package org.esa.beam.chris.ui;

import org.esa.beam.dataio.chris.ChrisConstants;
import org.esa.beam.dataio.dimap.DimapFileFilter;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class NoiseReductionPresenter {

    private final AppContext appContext;
    private final DefaultTableModel productTableModel;
    private final ListSelectionModel productTableSelectionModel;

    private final DefaultTableModel metadataTableModel;
    private final Action addProductAction;
    private final Action removeProductAction;

    private final Action settingsAction;
    private AdvancedSettingsPresenter advancedSettingsPresenter;

    public NoiseReductionPresenter(AppContext appContext, Product[] products,
                                   AdvancedSettingsPresenter advancedSettingsPresenter) {
        this.appContext = appContext;
        Object[][] productData = new Object[products.length][2];
        if (products.length > 0) {
            for (int i = 0; i < productData.length; i++) {
                productData[i][0] = true;
                productData[i][1] = products[i];
            }
        }

        productTableModel = new DefaultTableModel(productData, new String[]{"Reduce Noise", "Product Name"});
        productTableSelectionModel = new DefaultListSelectionModel();
        productTableSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTableSelectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
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

    public Map<String, Object> getDestripingFactorsParameterMap() {
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

    public Product[] getDestripingFactorsSourceProducts() {
        Vector productVector = getProductTableModel().getDataVector();
        Product[] products = new Product[productVector.size()];
        for (int i = 0; i < productVector.size(); i++) {
            products[i] = (Product) ((Vector) productVector.get(i)).get(1);
        }
        return products;
    }

    public Product[] getSourceProducts() {
        final DefaultTableModel tableModel = getProductTableModel();
        final List<Product> productList = new ArrayList<Product>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if ((Boolean) tableModel.getValueAt(i, 0)) {
                productList.add((Product) tableModel.getValueAt(i, 1));
            }
        }

        return productList.toArray(new Product[productList.size()]);
    }

    void setCheckedState(Product product, boolean checked) {
        for (int i = 0; i < getProductTableModel().getRowCount(); i++) {
            Product current = (Product) getProductTableModel().getValueAt(i, 1);
            if (current.equals(product)) {
                getProductTableModel().setValueAt(checked, i, 0);
                return;
            }
        }
    }

    boolean isChecked(Product product) {
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

    boolean isSource(Product product) {
        for (int i = 0; i < getProductTableModel().getRowCount(); i++) {
            Product current = (Product) getProductTableModel().getValueAt(i, 1);
            if (current.equals(product)) {
                return true;
            }
        }
        return false;
    }

    void addProduct(Product product) throws NoiseReductionValidationException {
        Product[] products = getDestripingFactorsSourceProducts();
        if (products.length >= 5) {
            throw new NoiseReductionValidationException("Acquisition set already contains five products.");
        }
        if (products.length != 0) {
            final AcquisitionSetProductFilter productFilter = new AcquisitionSetProductFilter(products[0]);
            if (!productFilter.accept(product)) {
                throw new NoiseReductionValidationException("Product does not belong to the acquisition set.");
            }
        }
        if (containsProduct(products, product)) {
            throw new NoiseReductionValidationException("Product is already contained in the acquisition set.");
        }

        final SourceProductFilter sourceProductFilter = new SourceProductFilter();
        if (!sourceProductFilter.accept(product)) {
            throw new NoiseReductionValidationException(MessageFormat.format(
                    "Product type ''{0}''is not valid.", product.getProductType()));
        }
        DefaultTableModel tableModel = getProductTableModel();
        tableModel.addRow(new Object[]{Boolean.TRUE, product});
        updateSelection(tableModel.getRowCount() - 1);
    }

    void removeSelectedProduct() {
        if (productTableModel.getRowCount() > 0) {
            int selectionIndex = getProductTableSelectionModel().getLeadSelectionIndex();
            final Product product = (Product) getProductTableModel().getValueAt(selectionIndex, 1);
            if (!appContext.getProductManager().contains(product)) {
                product.dispose();
            }

            productTableModel.removeRow(selectionIndex);
            updateSelection(selectionIndex);
        }
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

    private static class AddProductAction extends AbstractAction {

        private static String LAST_OPEN_DIR_KEY = "chris.ui.file.lastOpenDir";

        private static String CHRIS_IMPORT_DIR_KEY = "user." + ChrisConstants.FORMAT_NAME.toLowerCase() + ".import.dir";

        private NoiseReductionPresenter presenter;

        public AddProductAction(NoiseReductionPresenter presenter) {
            super("Add...");
            this.presenter = presenter;

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (presenter.getDestripingFactorsSourceProducts().length == 5) {
                JOptionPane.showMessageDialog((Component) e.getSource(), "You cannot select more than five products.");
                return;
            }
            final String[] extensions = {ChrisConstants.DEFAULT_FILE_EXTENSION};
            final String description = ChrisConstants.READER_DESCRIPTION;
            final BeamFileFilter hdfFileFilter =
                    new BeamFileFilter(ChrisConstants.FORMAT_NAME, extensions, description);
            final BeamFileChooser fileChooser = new BeamFileChooser();
            final AppContext appContext = presenter.appContext;
            String chrisImportDir = appContext.getPreferences().getPropertyString(CHRIS_IMPORT_DIR_KEY,
                                                                                  SystemUtils.getUserHomeDir().getPath());
            String lastDir = appContext.getPreferences().getPropertyString(LAST_OPEN_DIR_KEY, chrisImportDir);
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.addChoosableFileFilter(hdfFileFilter);
            fileChooser.addChoosableFileFilter(new DimapFileFilter());
            fileChooser.setFileFilter(hdfFileFilter);
            fileChooser.setCurrentDirectory(new File(lastDir));

            if (BeamFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(null)) {
                final File[] selectedFiles = fileChooser.getSelectedFiles();
                for (final File file : selectedFiles) {
                    Product product = null;
                    try {
                        product = ProductIO.readProduct(file);
                    } catch (IOException e1) {
                        JOptionPane.showMessageDialog((Component) e.getSource(), e1.getMessage());
                    }
                    if (product != null) {
                        try {
                            presenter.addProduct(product);
                        } catch (NoiseReductionValidationException e1) {
                            if (!containsProduct(presenter.getDestripingFactorsSourceProducts(), product)) {
                                product.dispose();
                            }
                            JOptionPane.showMessageDialog((Component) e.getSource(),
                                                          "Cannot add product.\n" + e1.getMessage());
                        }
                    }
                }
                appContext.getPreferences().setPropertyString(LAST_OPEN_DIR_KEY,
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

        @Override
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

        @Override
        public void actionPerformed(ActionEvent e) {
            ModalDialog dialog = new ModalDialog(null,
                                                 "Advanced Settings",
                                                 ModalDialog.ID_OK_CANCEL_HELP,
                                                 "chrisNoiseReductionTool");
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
