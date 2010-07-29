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

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.DefaultAppContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.GraphicsEnvironment;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Tests for class {@link ScreeningDialog}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.5
 */
public class ScreeningDialogTest {

    private ScreeningDialog dialog;

    @Before
    public void before() throws Exception {
        if (GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance()) {
            return;
        }
        dialog = new ScreeningDialog(new DefaultAppContext("test"));
        dialog.show();
    }

    @After
    public void after() throws Exception {
        if (GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance()) {
            return;
        }
        dialog.hide();
    }

    @Test
    public void sourceProductIsReleasedWhenDialogIsHidden() {
        if (GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance()) {
            return;
        }
        final Product product = new Product("test", "test", 1, 1);

        dialog.getForm().setSourceProduct(product);
        assertSame(dialog.getForm().getSourceProduct(), dialog.getFormModel().getSourceProduct());

        dialog.hide();
        assertNull(dialog.getFormModel().getSourceProduct());
    }
}
