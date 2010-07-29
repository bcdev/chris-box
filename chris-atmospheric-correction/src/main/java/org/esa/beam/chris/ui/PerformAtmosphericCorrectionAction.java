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

import org.esa.beam.chris.operators.ComputeSurfaceReflectancesOp;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Dialog for invoking the CHRIS/Proba atmospheric correction.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class PerformAtmosphericCorrectionAction extends AbstractVisatAction {
    private final AtomicReference<ModelessDialog> dialog;

    public PerformAtmosphericCorrectionAction() {
        dialog = new AtomicReference<ModelessDialog>();
    }

    @Override
    public void actionPerformed(CommandEvent commandEvent) {
        dialog.compareAndSet(null, createDialog(getAppContext()));
        dialog.get().show();
    }

    @Override
    public void updateState() {
        final Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        setEnabled(selectedProduct == null || new AtmosphericCorrectionProductFilter().accept(selectedProduct));
    }

    private static ModelessDialog createDialog(AppContext appContext) {
        final DefaultSingleTargetProductDialog dialog =
                new DefaultSingleTargetProductDialog(OperatorSpi.getOperatorAlias(ComputeSurfaceReflectancesOp.class),
                                                     appContext,
                                                     "CHRIS/Proba Atmospheric Correction",
                                                     "chrisAtmosphericCorrectionTool");
        dialog.getJDialog().setName("chrisAtmosphericCorrectionDialog");
        dialog.setTargetProductNameSuffix("_AC");

        return dialog;
    }
}
