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

import java.io.File;
import java.io.FileFilter;

/**
 * Acquisition set file filter.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class AcquisitionSetFileFilter implements FileFilter {
    private final AcquisitionSetFilter acquisitionSetFilter;

    public AcquisitionSetFileFilter(File referenceFile) {
        acquisitionSetFilter = new AcquisitionSetFilter(referenceFile.getName());
    }

    @Override
    public boolean accept(File file) {
        return file != null && acquisitionSetFilter.accept(file.getName());
    }
}
