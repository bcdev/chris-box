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
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;

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

    private void showNoiseReductionDialog() {
        //
        ModalDialog modalDialog = new ModalDialog(VisatApp.getApp().getMainFrame(), "CHRIS Noise Reduction",
                                                  ModalDialog.ID_OK_CANCEL_HELP, "");
        NoiseReductionPresenter presenter = new NoiseReductionPresenter(VisatApp.getApp().getProductManager().getProducts());
        modalDialog.setContent(new NoiseReductionPanel(presenter));
        modalDialog.show();
    }

}
