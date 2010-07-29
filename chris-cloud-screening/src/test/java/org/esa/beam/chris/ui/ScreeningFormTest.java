/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.chris.ui;

import com.bc.ceres.binding.ValidationException;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.gpf.ui.DefaultAppContext;
import org.junit.Before;
import org.junit.Test;

import java.awt.GraphicsEnvironment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for class {@link ScreeningForm}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.5
 */
public class ScreeningFormTest {

    private ScreeningForm form;

    @Before
    public void before() throws Exception {
        if (GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance()) {
            return;
        }
        final DefaultAppContext appContext = new DefaultAppContext("test");
        final ScreeningFormModel formModel = new ScreeningFormModel();
        formModel.getParameterPropertyContainer().setValue("useWv", true);
        formModel.getParameterPropertyContainer().setValue("useO2", true);
        form = new ScreeningForm(appContext, formModel);

        form.getSourceProductSelector().setProductFilter(new ProductFilter() {
            @Override
            public boolean accept(Product product) {
                return true;
            }
        });
    }

    @Test
    public void featureCheckBoxStatus() throws ValidationException {
        if (GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance()) {
            return;
        }
        // for mode 1 all features are available
        form.setSourceProduct(new Product("a", "CHRIS_M1_NR", 1, 1));
        assertTrue(form.isWvCheckBoxEnabled());
        assertTrue(form.isO2CheckBoxEnabled());
        assertTrue(form.isWvCheckBoxSelected());
        assertTrue(form.isO2CheckBoxSelected());

        // for mode 5 all features are available
        form.setSourceProduct(new Product("e", "CHRIS_M5_NR", 1, 1));
        assertTrue(form.isWvCheckBoxEnabled());
        assertTrue(form.isO2CheckBoxEnabled());
        assertTrue(form.isWvCheckBoxSelected());
        assertTrue(form.isO2CheckBoxSelected());

        // for mode 2 atmospheric features are not available
        form.setSourceProduct(new Product("b", "CHRIS_M2_NR", 1, 1));
        assertFalse(form.isWvCheckBoxEnabled());
        assertFalse(form.isO2CheckBoxEnabled());
        assertFalse(form.isWvCheckBoxSelected());
        assertFalse(form.isO2CheckBoxSelected());

        // for mode 3 atomospheric features are not available
        form.setSourceProduct(new Product("c", "CHRIS_M3_NR", 1, 1));
        assertFalse(form.isWvCheckBoxEnabled());
        assertFalse(form.isO2CheckBoxEnabled());
        assertFalse(form.isWvCheckBoxSelected());
        assertFalse(form.isO2CheckBoxSelected());

        // for mode 4 atomospheric features are not available
        form.setSourceProduct(new Product("d", "CHRIS_M4_NR", 1, 1));
        assertFalse(form.isWvCheckBoxEnabled());
        assertFalse(form.isO2CheckBoxEnabled());
        assertFalse(form.isWvCheckBoxSelected());
        assertFalse(form.isO2CheckBoxSelected());

        // check again for mode 1 - checkboxes enabled, but not selected
        form.setSourceProduct(new Product("a", "CHRIS_M1_NR", 1, 1));
        assertTrue(form.isWvCheckBoxEnabled());
        assertTrue(form.isO2CheckBoxEnabled());
        assertFalse(form.isWvCheckBoxSelected());
        assertFalse(form.isO2CheckBoxSelected());

        // check again for mode 5 - checkboxes enabled, but not selected
        form.setSourceProduct(new Product("e", "CHRIS_M5_NR", 1, 1));
        assertTrue(form.isWvCheckBoxEnabled());
        assertTrue(form.isO2CheckBoxEnabled());
        assertFalse(form.isWvCheckBoxSelected());
        assertFalse(form.isO2CheckBoxSelected());
    }
}
