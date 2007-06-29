/* $Id: $
 *
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
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;

import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Ralf Quast
 * @version $Revision: $ $Date: $
 */
public class NoiseReductionAction extends ExecCommand {

    @Override
    public void actionPerformed(CommandEvent commandEvent) {
        showNoiseReductionDialog();
    }

    @Override
    public void updateState() {
        final Product product = VisatApp.getApp().getSelectedProduct();
        setEnabled(product != null && product.getProductType().startsWith("CHRIS_M"));
    }

    private static void showNoiseReductionDialog() {
        ModalDialog modalDialog = new ModalDialog(VisatApp.getApp().getMainFrame(), "CHRIS Noise Reduction",
                                                  ModalDialog.ID_OK_CANCEL_HELP, "");

        Product[] products = new Product[]{VisatApp.getApp().getSelectedProduct()};
        NoiseReductionPresenter presenter = new NoiseReductionPresenter(products, new AdvancedSettingsPresenter());
        modalDialog.setContent(new NoiseReductionPanel(presenter));
        if(ModalDialog.ID_OK != modalDialog.show()) {
            return;
        }

        AdvancedSettingsPresenter settingsPresenter = presenter.getSettingsPresenter();
        if(settingsPresenter.isSlitApplied()){
            try {
                Product product1 = presenter.getProducts()[0];
                Product product2 = GPF.createProduct("DestripingFactors", settingsPresenter.getDestripingParameter(), product1);
                HashMap<String, Product> productsMap = new HashMap<String, Product>(2);
                productsMap.put("sourceProduct", product1);
                productsMap.put("factorProduct", product2);
                Product product3 = GPF.createProduct("Destriping", new HashMap<String, Object>(0), productsMap);
                VisatApp.getApp().addProduct(product3);
            } catch (OperatorException e) {
                // todo
                e.printStackTrace();
            }
        }
    }

}
