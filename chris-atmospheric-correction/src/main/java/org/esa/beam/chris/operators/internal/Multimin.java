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

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class Multimin {

    
    private static double linmin(MultivariateFunction f, double[] p, double[] q) {
        final F1 f1 = new F1(f, p, q);
        final Min.Bracket bracket = new Min.Bracket();

        Min.bracket(f1, 0.0, 1.0, bracket);
        Min.brent(f1, bracket, 2.0E-4);

        for (int i = 0; i < p.length; ++i) {
            q[i] *= bracket.minimumX;
            p[i] += q[i];
        }

        return bracket.minimumF;
    }

    private static class F1 implements UnivariateFunction {

        private final MultivariateFunction f;
        private final double[] p;
        private final double[] q;

        public F1(MultivariateFunction f, double[] p, double[] q) {
            this.f = f;
            this.p = p;
            this.q = q;
        }

        @Override
        public double value(double x) {
            final double[] point = p.clone();

            for (int i = 0; i < p.length; i++) {
                point[i] += x * q[i];
            }

            return f.value(point);
        }
    }
}
