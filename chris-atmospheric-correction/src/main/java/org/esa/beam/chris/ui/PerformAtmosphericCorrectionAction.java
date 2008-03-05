/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class PerformAtmosphericCorrectionAction extends AbstractVisatAction {
    private static List<String> CHRIS_TYPES;

    static {
        CHRIS_TYPES = new ArrayList<String>();
        Collections.addAll(CHRIS_TYPES,
                "CHRIS_M1_REFL",
                "CHRIS_M2_REFL",
                "CHRIS_M3_REFL",
                "CHRIS_M30_REFL",
                "CHRIS_M3A_REFL",
                "CHRIS_M4_REFL",
                "CHRIS_M5_REFL");
    }

    @Override
    public void actionPerformed(CommandEvent commandEvent) {
        VisatApp.getApp().showErrorDialog("Not implemented.");

//        final String sourceProductName = VisatApp.getApp().getSelectedProduct().getName();
//        final SingleTargetProductDialog dialog =
//                new DefaultSingleTargetProductDialog("chris.PerformAtmosphericCorrection",
//                                                     getAppContext(),
//                                                     "Atmospheric Correction",
//                                                     "chrisAtmosphericCorrectionTool");
//        dialog.getTargetProductSelector().getModel().setProductName(sourceProductName.replace("_REFL", "_ATM"));
//        dialog.show();
    }

    @Override
    public void updateState() {
        final Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        final boolean enabled = selectedProduct != null
                && CHRIS_TYPES.contains(selectedProduct.getProductType());

        setEnabled(enabled);
    }
}
