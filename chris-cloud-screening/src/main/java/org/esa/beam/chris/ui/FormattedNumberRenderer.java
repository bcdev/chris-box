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

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.*;
import java.text.NumberFormat;

class FormattedNumberRenderer extends DefaultTableCellRenderer {
    private final NumberFormat numberFormat;

    FormattedNumberRenderer(NumberFormat numberFormat) {
        this.numberFormat = numberFormat;
    }

    @Override
    public void setValue(Object value) {
        setText(value == null ? "" : numberFormat.format(value));
    }
}
