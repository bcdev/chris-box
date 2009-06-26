package org.esa.beam.chris.operators;

import junit.framework.TestCase;

import java.util.Date;

public class EcefEciConverterTest extends TestCase {

    public void testEcefToEci() {
        final double lon = 10.2414;
        final double lat = 53.4800;
        final double alt = 40.0;

        final double[] ecef = new double[3];

        Utils.wgsToEcef(lon, lat, alt, ecef);

        double x = ecef[0];
        double y = ecef[1];
        double z = ecef[2];

        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("z = " + z);
        assertEquals(6364406.8, Math.sqrt(x * x + y * y + z * z), 0.1);

        final EcefEciConverter converter = new EcefEciConverter(TimeCalculator.toMJD(new Date()));
        final double[] eci = new double[3];
        converter.ecefToEci(ecef, eci);

        x = eci[0];
        y = eci[1];
        z = eci[2];

        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("z = " + z);
        assertEquals(6364406.8, Math.sqrt(x * x + y * y + z * z), 0.1);

        converter.ecefToEci(ecef, ecef);
        assertEquals(x, ecef[0]);
        assertEquals(y, ecef[1]);
        assertEquals(z, ecef[2]);
    }

    private static class EcefEciConverter {

        /**
         * Angular rotational velocity of the Earth (rad s-1)
         */
        private static final double WE = 7.292115854788046E-5;

        private final double c;
        private final double s;

        public EcefEciConverter(double mjd) {
            final double gst = TimeCalculator.toGST(mjd);
            c = Math.cos(gst);
            s = Math.sin(gst);
        }

        final void ecefToEci(double[] ecef, double[] eci) {
            if (ecef == null) {
                throw new IllegalArgumentException("ecef == null");
            }
            if (eci == null) {
                throw new IllegalArgumentException("eci == null");
            }
            if (ecef.length < 2) {
                throw new IllegalArgumentException("ecef.length < 2");
            }
            if (eci.length < 2) {
                throw new IllegalArgumentException("eci.length < 2");
            }
            if (ecef.length > 6) {
                throw new IllegalArgumentException("ecef.length < 2");
            }
            if (eci.length > 6) {
                throw new IllegalArgumentException("eci.length < 2");
            }
            if (eci.length != ecef.length) {
                throw new IllegalArgumentException("eci.length != ecef.length");
            }

            final double x = ecefToEciX(ecef[0], ecef[1]);
            final double y = ecefToEciY(ecef[0], ecef[1]);
            eci[0] = x;
            eci[1] = y;

            if (eci.length == 3) {
                eci[2] = ecef[2];
            } else if (eci.length == 4) {
                final double u = ecefToEciX(ecef[2], ecef[3]) - WE * y;
                final double v = ecefToEciY(ecef[2], ecef[3]) + WE * x;
                eci[2] = u;
                eci[3] = v;
            } else if (eci.length == 6) {
                final double u = ecefToEciX(ecef[3], ecef[4]) - WE * y;
                final double v = ecefToEciY(ecef[3], ecef[4]) + WE * x;
                eci[3] = u;
                eci[4] = v;
                eci[5] = ecef[5];
            }
        }

        final void eciToEcef(double[] eci, double[] ecef) {
            if (eci == null) {
                throw new IllegalArgumentException("eci == null");
            }
            if (ecef == null) {
                throw new IllegalArgumentException("ecef == null");
            }
            if (eci.length < 2) {
                throw new IllegalArgumentException("eci.length < 2");
            }
            if (ecef.length < 2) {
                throw new IllegalArgumentException("ecef.length < 2");
            }
            if (eci.length > 6) {
                throw new IllegalArgumentException("eci.length < 2");
            }
            if (ecef.length > 6) {
                throw new IllegalArgumentException("ecef.length < 2");
            }
            if (ecef.length != eci.length) {
                throw new IllegalArgumentException("ecef.length != eci.length");
            }

            final double x = eciToEcefX(eci[0], eci[1]);
            final double y = eciToEcefY(eci[0], eci[1]);

            ecef[0] = x;
            ecef[1] = y;

            if (ecef.length == 3) {
                ecef[2] = eci[2];
            } else if (ecef.length == 4) {
                final double u = eciToEcefX(eci[2], eci[3]) - WE * y;
                final double v = eciToEcefY(eci[2], eci[3]) + WE * x;
                ecef[2] = u;
                ecef[3] = v;
            } else if (ecef.length == 6) {
                final double u = eciToEcefX(eci[3], eci[4]) - WE * y;
                final double v = eciToEcefY(eci[3], eci[4]) + WE * x;
                ecef[3] = u;
                ecef[4] = v;
                ecef[5] = eci[5];
            }
        }

        private double ecefToEciX(double ecefX, double ecefY) {
            return c * ecefX - s * ecefY;
        }

        private double ecefToEciY(double ecefX, double ecefY) {
            return s * ecefX + c * ecefY;
        }

        private double ecefToEciU(double ecefX, double ecefY, double ecefU, double ecefV) {
            return ecefToEciX(ecefU, ecefV) - WE * ecefToEciY(ecefX, ecefY);
        }

        private double ecefToEciV(double ecefX, double ecefY, double ecefU, double ecefV) {
            return ecefToEciY(ecefU, ecefV) + WE * ecefToEciX(ecefX, ecefY);
        }

        private double eciToEcefX(double eciX, double eciY) {
            return c * eciX + s * eciY;
        }

        private double eciToEcefY(double eciX, double eciY) {
            return c * eciY - s * eciX;
        }

        private double eciToEcefU(double eciX, double eciY, double eciU, double eciV) {
            return eciToEcefX(eciU, eciV) + WE * eciToEcefY(eciX, eciY);
        }

        private double eciToEcefV(double eciX, double eciY, double eciU, double eciV) {
            return eciToEcefY(eciU, eciV) - WE * eciToEcefX(eciX, eciY);
        }
    }

    private static class Utils {

        /**
         * Major radius of WGS-84 ellipsoid (m).
         */
        private static final double WGS84_A = 6378137.0;
        /**
         * Flattening of WGS-84 ellipsoid.
         */
        private static final double WGS84_F = 1.0 / 298.257223563;
        /**
         * Eccentricity squared.
         */
        private static final double WGS84_E = WGS84_F * (2.0 - WGS84_F);

        private Utils() {
        }

        static void wgsToEcef(double lon, double lat, double alt, double[] ecef) {
            if (Math.abs(lat) > 90.0) {
                throw new IllegalArgumentException("|lat| > 90.0");
            }
            if (ecef == null) {
                throw new IllegalArgumentException("ecef == null");
            }
            if (ecef.length != 3) {
                throw new IllegalArgumentException("ecef.length != 3");
            }

            final double u = Math.toRadians(lon);
            final double v = Math.toRadians(lat);

            final double cu = Math.cos(u);
            final double su = Math.sin(u);
            final double cv = Math.cos(v);
            final double sv = Math.sin(v);

            final double a = WGS84_A / Math.sqrt(1.0 - WGS84_E * sv * sv);
            final double b = (a + alt) * cv;

            ecef[0] = b * cu;
            ecef[1] = b * su;
            ecef[2] = ((1.0 - WGS84_E) * a + alt) * sv;
        }
    }
}
