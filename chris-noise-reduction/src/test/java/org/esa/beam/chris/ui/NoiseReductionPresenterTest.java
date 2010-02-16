package org.esa.beam.chris.ui;

import org.esa.beam.dataio.chris.ChrisConstants;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.ui.DefaultAppContext;
import org.junit.Before;
import org.junit.Test;

import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.GraphicsEnvironment;
import java.io.File;

import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class NoiseReductionPresenterTest {

    private String[] metadataNames;
    private Product first;
    private Product second;
    private Product third;
    private Product[] expectedProducts;
    private DefaultAppContext appContext;

    @Before
    public void before() throws Exception {
        if (GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance()) {
            return;
        }
        metadataNames = new String[]{
                ChrisConstants.ATTR_NAME_CHRIS_MODE,
                ChrisConstants.ATTR_NAME_TARGET_NAME,
                "Target Coordinates",
                ChrisConstants.ATTR_NAME_FLY_BY_ZENITH_ANGLE,
                ChrisConstants.ATTR_NAME_MINIMUM_ZENITH_ANGLE,
        };

        first = createChrisDummyProduct("first", "DummyMode1", "DummyTarget1");
        second = createChrisDummyProduct("second", "DummyMode2", "DummyTarget2");
        third = createChrisDummyProduct("third", "DummyMode3", "DummyTarget3");

        expectedProducts = new Product[]{first, second, third};
        appContext = new DefaultAppContext("test");
    }

    @Test
    public void constuction() {
        if (GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance()) {
            return;
        }
        NoiseReductionPresenter nrp = new NoiseReductionPresenter(appContext, expectedProducts,
                                                                  new AdvancedSettingsPresenter());

        Product[] actualProducts = nrp.getDestripingFactorsSourceProducts();
        assertNotNull(actualProducts);
        assertEquals(3, actualProducts.length);

        for (int i = 0; i < actualProducts.length; i++) {
            assertSame(expectedProducts[i], actualProducts[i]);
        }

        int selectionIndex = nrp.getProductTableSelectionModel().getMaxSelectionIndex();
        assertEquals(0, selectionIndex);
        assertSame(first, nrp.getDestripingFactorsSourceProducts()[selectionIndex]);

        checkMetadata(nrp.getMetadataTableModel(), "DummyMode1", "DummyTarget1");
    }

    @Test
    public void constructionWithoutProducts() {
        if (GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance()) {
            return;
        }
        NoiseReductionPresenter nrp = new NoiseReductionPresenter(appContext, new Product[0],
                                                                  new AdvancedSettingsPresenter());

        DefaultTableModel metadata = nrp.getMetadataTableModel();
        assertEquals(5, metadata.getRowCount());
        for (int i = 0; i < metadata.getRowCount(); i++) {
            assertEquals(nrp.getMetadataTableModel().getValueAt(i, 1), null);
        }
    }

    @Test
    public void addRemovePoduct() throws NoiseReductionValidationException {
        if (GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance()) {
            return;
        }
        NoiseReductionPresenter nrp = new NoiseReductionPresenter(appContext, expectedProducts,
                                                                  new AdvancedSettingsPresenter());

        Product fourth = createChrisDummyProduct("fourth", "chris", "DummyTarget4");
        nrp.addProduct(fourth);
        assertEquals(4, nrp.getDestripingFactorsSourceProducts().length);
        assertSame(fourth, nrp.getDestripingFactorsSourceProducts()[3]);
        ListSelectionModel selectionModel = nrp.getProductTableSelectionModel();
        assertEquals(3, selectionModel.getMaxSelectionIndex());

        nrp.getProductTableSelectionModel().setSelectionInterval(2, 2);

        nrp.removeSelectedProduct();
        assertEquals(3, nrp.getDestripingFactorsSourceProducts().length);
        assertSame(first, nrp.getDestripingFactorsSourceProducts()[0]);
        assertSame(second, nrp.getDestripingFactorsSourceProducts()[1]);
        assertSame(fourth, nrp.getDestripingFactorsSourceProducts()[2]);
        assertEquals(2, nrp.getProductTableSelectionModel().getMaxSelectionIndex());

        nrp.removeSelectedProduct();
        assertEquals(2, nrp.getDestripingFactorsSourceProducts().length);
        assertSame(first, nrp.getDestripingFactorsSourceProducts()[0]);
        assertSame(second, nrp.getDestripingFactorsSourceProducts()[1]);
        assertEquals(1, nrp.getProductTableSelectionModel().getMaxSelectionIndex());

        nrp.removeSelectedProduct();
        assertEquals(0, nrp.getProductTableSelectionModel().getMaxSelectionIndex());
        assertSame(first, nrp.getDestripingFactorsSourceProducts()[0]);

        nrp.removeSelectedProduct();
        assertEquals(0, nrp.getDestripingFactorsSourceProducts().length);
        assertEquals(-1, nrp.getProductTableSelectionModel().getMaxSelectionIndex());

        nrp.addProduct(fourth);
        assertEquals(0, nrp.getProductTableSelectionModel().getMaxSelectionIndex());
    }

    @Test
    public void selectionChange() {
        if (GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance()) {
            return;
        }
        NoiseReductionPresenter nrp = new NoiseReductionPresenter(appContext, expectedProducts,
                                                                  new AdvancedSettingsPresenter());
        checkMetadata(nrp.getMetadataTableModel(), "DummyMode1", "DummyTarget1");

        nrp.getProductTableSelectionModel().setSelectionInterval(2, 2);
        checkMetadata(nrp.getMetadataTableModel(), "DummyMode3", "DummyTarget3");

        nrp.getProductTableSelectionModel().setSelectionInterval(1, 1);
        checkMetadata(nrp.getMetadataTableModel(), "DummyMode2", "DummyTarget2");
    }

    @Test
    public void productAsOutput() {
        if (GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance()) {
            return;
        }
        NoiseReductionPresenter nrp = new NoiseReductionPresenter(appContext, expectedProducts,
                                                                  new AdvancedSettingsPresenter());

        assertTrue(nrp.isChecked(first));
        assertTrue(nrp.isChecked(second));
        assertTrue(nrp.isChecked(third));

        nrp.setCheckedState(second, false);

        assertTrue(nrp.isChecked(first));
        assertFalse(nrp.isChecked(second));
        assertTrue(nrp.isChecked(third));
    }

    private static Product createChrisDummyProduct(String name, String mode, String targetName) {
        Product product = new Product("CHRIS_BR_123456_" + name + "_21", "CHRIS_M2", 1, 1);
        product.setFileLocation(new File("CHRIS_BR_123456_9876_21.hdf"));
        MetadataElement mphElement = new MetadataElement("MPH");
        mphElement.addAttribute(new MetadataAttribute(ChrisConstants.ATTR_NAME_CHRIS_MODE,
                                                      ProductData.createInstance(mode),
                                                      true));
        mphElement.addAttribute(new MetadataAttribute(ChrisConstants.ATTR_NAME_TARGET_NAME,
                                                      ProductData.createInstance(targetName),
                                                      true));
        mphElement.addAttribute(new MetadataAttribute(ChrisConstants.ATTR_NAME_TARGET_LAT,
                                                      ProductData.createInstance("45.32"),
                                                      true));
        mphElement.addAttribute(new MetadataAttribute(ChrisConstants.ATTR_NAME_TARGET_LON,
                                                      ProductData.createInstance("10.8"),
                                                      true));
        product.getMetadataRoot().addElement(mphElement);
        // leave ATTR_NAME_NOMINAL_FLY_BY_ZENITH_ANGLE and ATTR_NAME_MINIMUM_ZENITH_ANGLE as "Not available"
        return product;
    }

    private void checkMetadata(DefaultTableModel metaData, String mode, String target) {
        assertNotNull(metaData);
        assertEquals(5, metaData.getRowCount());

        for (int i = 0; i < metadataNames.length; i++) {
            assertEquals(metadataNames[i], metaData.getValueAt(i, 0));
        }

        assertEquals(2, metaData.getColumnCount());
        assertEquals(mode, metaData.getValueAt(0, 1));
        assertEquals(target, metaData.getValueAt(1, 1));

        GeoPos expectedGeoPos = new GeoPos(45.32f, 10.8f);
        String expectedGeoPosString = expectedGeoPos.getLatString() + ", " + expectedGeoPos.getLonString();
        assertEquals(expectedGeoPosString, metaData.getValueAt(2, 1));

        assertEquals("Not available", metaData.getValueAt(3, 1));
        assertEquals("Not available", metaData.getValueAt(4, 1));
    }
}
