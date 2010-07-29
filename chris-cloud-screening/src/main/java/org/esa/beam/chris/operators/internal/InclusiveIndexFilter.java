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
package org.esa.beam.chris.operators.internal;

import org.esa.beam.cluster.IndexFilter;

/**
 * Index filter including all indexes where any backing
 * boolean array element is {@code true}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class InclusiveIndexFilter implements IndexFilter {
    private final boolean[][] booleans;

    /**
     * Constructs a new instance of this class.
     *
     * @param booleans the backing boolean array(s).
     */
    public InclusiveIndexFilter(boolean[]... booleans) {
        this.booleans = booleans;
    }

    @Override
    public final boolean accept(int index) {
        for (boolean[] b : booleans) {
            if (index < b.length) {
                if (b[index]) {
                    return true;
                }
            }
        }

        return false;
    }
}
