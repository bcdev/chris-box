/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.chris.operators;

import org.esa.beam.chris.operators.GPSTime.GPSReader;
import org.esa.beam.chris.operators.ImageCenterTime.ITCReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class TheRealThing {
    
    private static final int SlowDown = 5; // Slowdown factor

    //Epoch for a reduced Julian Day (all JD values are substracted by this value). 
    //This way the calculations can be performed more efficiently.
    private static final double jd0 = Conversions.julianDate(2001, 0, 1);

    // Time difference between GPS Time and UT1 (year dependent). In 2009 it will be 14.
    // It is CRITICAL to substract it to both GPS and ITC data
    private static final int dTgps = 13; 
    
    // there is a delay of 0.999s between the GPS time tag and the actual time of the reported position/velocity.
    private static final double delay=0.999; 
           
    ///////////////////////////////////////////////////////////
    
    public File ictFile;
    public File gpsFile;

    public void doIt() throws IOException {
        // ICT
        ITCReader ictReader = new ImageCenterTime.ITCReader(new FileInputStream(ictFile));
        double[] lastIctValues = ictReader.getLastIctValues();
        ImageCenterTime ict = ImageCenterTime.create(lastIctValues, dTgps);
        
        // GPS
        GPSReader gpsReader = new GPSTime.GPSReader(new FileInputStream(gpsFile));
        List<String[]> gpsRecords = gpsReader.getReadRecords();
        List<GPSTime> gps = GPSTime.create(gpsRecords, dTgps, delay);
        
        // GCP
        // TODO
        
        
    }
}
