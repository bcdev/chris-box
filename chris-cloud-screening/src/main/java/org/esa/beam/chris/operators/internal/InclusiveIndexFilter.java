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
