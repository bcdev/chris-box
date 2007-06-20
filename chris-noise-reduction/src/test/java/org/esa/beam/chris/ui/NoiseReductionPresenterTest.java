package org.esa.beam.chris.ui;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class NoiseReductionPresenterTest extends TestCase {

    public void testConstuctor() {
        Product first = new Product("first", "type1", 1, 1);
        Product second = new Product("second", "type2", 1, 1);
        Product third = new Product("third", "type3", 1, 1);

        Product[] expectedProducts = new Product[]{first, second, third};
        NoiseReductionPresenter nrp = new NoiseReductionPresenter(expectedProducts);

        Product[] actualProducts = nrp.getProducts();
        assertNotNull(actualProducts);
        assertEquals(3, actualProducts.length);

        for (int i = 0; i < actualProducts.length; i++) {
            assertSame(expectedProducts[i], actualProducts[i]);
        }

        Product fourth = new Product("fourth", "type4", 1, 1);
        nrp.addProduct(fourth);
        assertEquals(4, nrp.getProducts().length);
        assertSame(fourth, nrp.getProducts()[3]);

        nrp.setSelectionIndex(3);

        nrp.removeSelectedProduct();
        assertEquals(3, nrp.getProducts().length);
        assertSame(first, nrp.getProducts()[0]);
        assertSame(second, nrp.getProducts()[1]);
        assertSame(third, nrp.getProducts()[2]);

        assertEquals(2, nrp.getSelectionIndex());

        String[][] metaData = nrp.getMetadata();
        assertNotNull(metaData);
    }


}
