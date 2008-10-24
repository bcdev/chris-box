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
 * Index filter excluding all indexes where any backing
 * boolean array element is {@code true}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ExclusiveIndexFilter implements IndexFilter {
    private final IndexFilter inclusiveIndexFilter;

    /**
     * Constructs a new instance of this class.
     *
     * @param booleans the backing boolean array(s).
     */
    public ExclusiveIndexFilter(boolean[]... booleans) {
        inclusiveIndexFilter = new InclusiveIndexFilter(booleans);
    }

    @Override
    public final boolean accept(int index) {
        return !inclusiveIndexFilter.accept(index);
    }
}
