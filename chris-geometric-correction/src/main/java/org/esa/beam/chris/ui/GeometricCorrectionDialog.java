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

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.binding.PropertyEditor;
import org.esa.beam.chris.operators.PerformGeometricCorrectionOp;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.util.PropertyMap;

import java.io.File;

/**
 * Geometric correction dialog.
 *
 * @author Ralf Quast
 * @since CHRIS-Box 1.5
 */
class GeometricCorrectionDialog extends PlainSingleTargetProductDialog {

    private static final String KEY_TELEMETRY_REPOSITORY = "beam.chris.telemetryRepository";

    GeometricCorrectionDialog(String operatorAlias, AppContext appContext, String title, String helpId) {
        super(operatorAlias, appContext, title, helpId);
        getJDialog().setName("chrisGeometricCorrectionDialog");
        setTargetProductNameSuffix("_GC");
    }

    @Override
    protected void initProperties(PropertySet propertySet) {
        final String propertyName = PerformGeometricCorrectionOp.ALIAS_TELEMETRY_REPOSITORY;
        propertySet.getDescriptor(propertyName).setAttribute("directory", true);

        final File def = getDefaultTelemetryRepository();
        propertySet.setValue(propertyName, def);
    }

    @Override
    protected PropertyEditor findPropertyEditor(PropertyDescriptor descriptor) {
        if (descriptor.getType() == File.class) {
            return FileEditor.getInstance();
        } else {
            return super.findPropertyEditor(descriptor);
        }
    }

    @Override
    protected void onApply() {
        setDefaultTelemetryRepository(getParameterValue(PerformGeometricCorrectionOp.ALIAS_TELEMETRY_REPOSITORY));
        super.onApply();
    }

    private File getDefaultTelemetryRepository() {
        final PropertyMap map = getAppContext().getPreferences();
        final String pathname = map.getPropertyString(KEY_TELEMETRY_REPOSITORY, ".");

        return new File(pathname);
    }

    private void setDefaultTelemetryRepository(Object value) {
        final PropertyMap map = getAppContext().getPreferences();
        map.setPropertyString(KEY_TELEMETRY_REPOSITORY, value.toString());
    }
}
