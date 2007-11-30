/*
 * Copyright (C) 2002-2007 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.chris.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 * New class.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class FindClucovClustersAction extends ExecCommand {
    private static List<String> CHRIS_TYPES;

    static {
        CHRIS_TYPES = new ArrayList<String>();
        Collections.addAll(CHRIS_TYPES,
                           "CHRIS_M1_FEAT",
                           "CHRIS_M2_FEAT",
                           "CHRIS_M3_FEAT",
                           "CHRIS_M3A_FEAT",
                           "CHRIS_M4_FEAT",
                           "CHRIS_M5_FEAT");
    }

    @Override
    public void actionPerformed(CommandEvent commandEvent) {
        final Product selectedProduct = VisatApp.getApp().getSelectedProduct();

        try {
            performAction(selectedProduct);
        } catch (OperatorException e) {
            VisatApp.getApp().showErrorDialog(e.getMessage());
            VisatApp.getApp().getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public void updateState() {
        final Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        final boolean enabled = selectedProduct != null
                && CHRIS_TYPES.contains(selectedProduct.getProductType());

        setEnabled(enabled);
    }

    private static void performAction(Product sourceProduct) throws OperatorException {
        final HashMap<String, Object> parameterMap = new HashMap<String, Object>();

        final StringBuilder name = new StringBuilder(sourceProduct.getName());
        final int pos = name.lastIndexOf("_FEAT");
        if (pos != -1) {
            name.replace(pos, pos + 5, "_CLU");
        } else {
            name.append("_CLU");
        }
//        parameterMap.put("targetProductName", name.toString());
        final String[] features = {"brightness_vis", "brightness_nir", "whiteness_vis", "whiteness_nir", "wv"};
        parameterMap.put("features", features);
        parameterMap.put("clusterCount", 14);
        final Product targetProduct = GPF.createProduct("chris.FindClucovClusters",
                                                        parameterMap,
                                                        sourceProduct);
        VisatApp.getApp().addProduct(targetProduct);
    }
}
