package org.esa.beam.chris.ui;

import junit.framework.TestCase;
import org.esa.beam.dataio.chris.ChrisConstants;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.GeoPos;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class NoiseReductionPresenterTest extends TestCase {

    private String[] meatadatNames;
    private Product first;
    private Product second;
    private Product third;
    private Product[] expectedProducts;

    @Override
    protected void setUp() throws Exception {
        meatadatNames = new String[]{
                ChrisConstants.ATTR_NAME_CHRIS_MODE,
                ChrisConstants.ATTR_NAME_TARGET_NAME,
                "Target Coordinates",
                ChrisConstants.ATTR_NAME_NOMINAL_FLY_BY_ZENITH_ANGLE,
                ChrisConstants.ATTR_NAME_MINIMUM_ZENITH_ANGLE,
        };

        first = createChrisDummyProduct("first", "type1", "DummyMode1", "DummyTarget1");
        second = createChrisDummyProduct("second", "type2", "DummyMode2", "DummyTarget2");
        third = createChrisDummyProduct("third", "type3", "DummyMode3", "DummyTarget3");

        expectedProducts = new Product[]{first, second, third};
    }

    public void testConstuctor() {
        NoiseReductionPresenter nrp = new NoiseReductionPresenter(expectedProducts);

        Product[] actualProducts = nrp.getProducts();
        assertNotNull(actualProducts);
        assertEquals(3, actualProducts.length);

        for (int i = 0; i < actualProducts.length; i++) {
            assertSame(expectedProducts[i], actualProducts[i]);
        }

        assertEquals(0, nrp.getSelectionIndex());
        assertSame(first, nrp.getProducts()[nrp.getSelectionIndex()]);

        String[][] metaData = nrp.getMetadata();
        checkMetadata(metaData, "DummyMode1", "DummyTarget1");
        String[] productNames = nrp.getProductNames();
        for (int i = 0; i < expectedProducts.length; i++) {
            assertEquals(expectedProducts[i].getName(), productNames[i]);
        }

    }

    public void testConstructorWithoutProducts() {
        NoiseReductionPresenter nrp = new NoiseReductionPresenter(new Product[0]);

        String[][] metadata = nrp.getMetadata();
        assertEquals(5, metadata.length);
        for (String[] metadataEntry : metadata) {
            assertEquals("", metadataEntry[1]);
        }
    }

    public void testAddRemove() {
        NoiseReductionPresenter nrp = new NoiseReductionPresenter(expectedProducts);

        Product fourth = createChrisDummyProduct("fourth", "type4", "DummyMode4", "DummyTarget4");
        nrp.addProduct(fourth);
        assertEquals(4, nrp.getProducts().length);
        assertSame(fourth, nrp.getProducts()[3]);
        assertEquals(3, nrp.getSelectionIndex());


        nrp.setSelectionIndex(2);

        nrp.removeSelectedProduct();
        assertEquals(3, nrp.getProducts().length);
        assertSame(first, nrp.getProducts()[0]);
        assertSame(second, nrp.getProducts()[1]);
        assertSame(fourth, nrp.getProducts()[2]);
        assertEquals(2, nrp.getSelectionIndex());


        assertEquals(2, nrp.getSelectionIndex());
        nrp.removeSelectedProduct();
        assertEquals(2, nrp.getProducts().length);
        assertSame(first, nrp.getProducts()[0]);
        assertSame(second, nrp.getProducts()[1]);
        assertEquals(1, nrp.getSelectionIndex());

        nrp.removeSelectedProduct();
        nrp.removeSelectedProduct();

        assertEquals(0, nrp.getProducts().length);
        assertEquals(-1, nrp.getSelectionIndex());

        nrp.addProduct(fourth);
        assertEquals(0, nrp.getSelectionIndex());
    }

    public void testSelectionChange() {
        NoiseReductionPresenter nrp = new NoiseReductionPresenter(expectedProducts);
        checkMetadata(nrp.getMetadata(), "DummyMode1", "DummyTarget1");

        nrp.setSelectionIndex(2);
        checkMetadata(nrp.getMetadata(), "DummyMode3", "DummyTarget3");

        nrp.setSelectionIndex(1);
        checkMetadata(nrp.getMetadata(), "DummyMode2", "DummyTarget2");
    }

    private static Product createChrisDummyProduct(String name, String type, String mode, String targetName) {
        Product product = new Product(name, type, 1, 1);
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

    private void checkMetadata(String[][] metaData, String mode, String target) {
        assertNotNull(metaData);
        assertEquals(5, metaData.length);

        for (int i = 0; i < meatadatNames.length; i++) {
            assertEquals(meatadatNames[i], metaData[i][0]);
        }

        assertEquals(2, metaData[0].length);
        assertEquals(mode, metaData[0][1]);
        assertEquals(target, metaData[1][1]);

        assertEquals(new GeoPos(45.32f, 10.8f).toString(), metaData[2][1]);

        assertEquals("Not available", metaData[3][1]);
        assertEquals("Not available", metaData[4][1]);
    }

}
