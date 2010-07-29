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

/**
 * Acquisition set filter.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class AcquisitionSetFilter {
    private final String referenceName;

    public AcquisitionSetFilter(String referenceName) {
        this.referenceName = referenceName;
    }

    public boolean accept(String name) {
        if (name == null) {
            return false;
        }
        
        final String[] referenceNameParts = referenceName.split("_", 5);
        final String[] nameParts = name.split("_", 5);

        return referenceNameParts.length == 5 && referenceNameParts.length == nameParts.length &&
                referenceNameParts[0].equals(nameParts[0]) &&
                referenceNameParts[1].equals(nameParts[1]) &&
                referenceNameParts[2].equals(nameParts[2]) &&
                referenceNameParts[4].equals(nameParts[4]);
    }
}
