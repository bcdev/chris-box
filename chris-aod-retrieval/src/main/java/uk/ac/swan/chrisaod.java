package uk.ac.swan;// ********************************************************************************************
//
// 		NAME:			chrisaod.java
// 
//		DESCRIPTION:	Obtains AOD at 550 nm from multi-angle PROBA/CHRIS observations
//                      using surface reflectance model of North, TGRS, 1999.
//                       				 					  
// 		AUTHOR(S):		William Grey and Peter North, 
//						University of Wales Swansea 	   
// 						w.m.f.grey@swan.ac.uk 
//
// 		REQUIREMENTS:	6s radiative transfer code of Vermote et al (1997) TGRS.
//                      Input card 
//
// 		DEVELOPMENT:	First written: 11 May 2007
//
//		TO COMPILE:		javac chrisaod.java
//
// 		EXAMPLE: 		/geog/data/laplace/alpha/ggwgrey/bin/linux/java/jdk1.6.0/bin/java chrisaod ../chris.dat	
//
//		EXAMPLE INPUT CARD:
//
//      5                                  # Number of looks
//  	51.0 125.1 19.19 316.00            # SOLZN, SOLAZ, VIEWZN, VIEWAZ  view 1
//		51.0 125.1 38.69 345.50            # SOLZN, SOLAZ, VIEWZN, VIEWAZ  view 2
//  	51.0 125.1 36.88 212.23            # SOLZN, SOLAZ, VIEWZN, VIEWAZ  view 3
//  	51.0 125.1 57.18 357.35            # SOLZN, SOLAZ, VIEWZN, VIEWAZ  view 4
//  	51.0 125.1 55.71 203.37            # SOLZN, SOLAZ, VIEWZN, VIEWAZ  view 5
//		4                                  # Number of channels
//  	0.546 0.551 			           # min l, max l, band 1
//		0.666 0.677 			           # min l, max l, band 2
//  	0.863 0.872 			           # min l, max l, band 3
//  	0.981 0.992 			           # min l, max l, band 4
//		91.6780 108.589 65.4940 78.6070    # TOA radiances band 1, band 2, band 3, band 4 for view 1
//  	100.300 81.1210 56.1900 88.4060    # TOA radiances band 1, band 2, band 3, band 4 for view 2
//		75.0360 68.2030 125.333 89.1870    # TOA radiances band 1, band 2, band 3, band 4 for view 3
//		62.7920 82.0920 127.775 66.8130    # TOA radiances band 1, band 2, band 3, band 4 for view 4
//		105.419 87.3710 95.2190 55.0400    # TOA radiances band 1, band 2, band 3, band 4 for view 5
//  	5                                  # Month
//		9                                  # Day of month 
//		6                                  # Aerosol model
//		2.5                                # Water Vapour
//  	0.35                               # Ozone 
//  	0.0                                # Elevation
//		1                                  # Input TOA radiance (1) or TOA reflectance (0)
//
// *******************************************************************************************




import uk.ac.swan.brent;
import uk.ac.swan.MvFunction;
import uk.ac.swan.Function;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.StreamTokenizer;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.StringTokenizer;
import java.io.InputStreamReader;


public class chrisaod extends Object implements Function, MvFunction
{
  
 public static void main (String[] args){
 
  double a,b,c,tol,xmin;
  
  
  if (args.length != 1) usage(); 
  readInputCard(args[0]);
//  tau=0.1;
  
  a=0.005;
  b=0.10;
  c=2.0;
  tol=0.05;
//  tol=0.001; 

  xmin=0.0;
  
  chrisaod CHRISAOD = new chrisaod();
  brent Brent = new brent();
 
  // do Brent minimization
  
  Brent.brent(a,b,c,CHRISAOD,tol);
//  System.out.print("\nThe xmin value is " + xmin + "\n");
  

  
//  inv6s(0.1); 
//  inv6s(tau); 
   
 }
 
/*************** readInputCard *********************
****************************************************/

 public static void readInputCard(String inFile) {
   
  try{
   
   BufferedReader in = new BufferedReader(new FileReader(inFile));
   
   // read number of looks
    
   String indata = in.readLine();
   StringTokenizer st = new StringTokenizer(indata, " ");
   numLooks =  Integer.parseInt( st.nextToken() );
   
   // read SOLZN, SOLAZ, VIEWZN, VIEWAZ
   
   for (int i=0; i < numLooks; i++){
    
	indata = in.readLine();
    st = new StringTokenizer(indata, " ");
	solzn[i] = Double.parseDouble( st.nextToken() );
	solaz[i] = Double.parseDouble( st.nextToken() );
	viewzn[i] = Double.parseDouble( st.nextToken() );
	viewaz[i] = Double.parseDouble( st.nextToken() );
	
//	System.out.println(viewzn[i]);
//  System.out.println(viewaz[i]);
//	System.out.println(solzn[i]);
//  System.out.println(solaz[i]);
   
   }
   
   // read number fo channels
   
   indata = in.readLine();
   st = new StringTokenizer(indata, " ");
   numChannels =  Integer.parseInt( st.nextToken() );
   
   // read wavelengths
   
   for (int i=0; i < numChannels; i++){
   
	indata = in.readLine();
    st = new StringTokenizer(indata, " ");
	wl[i][0] = Double.parseDouble( st.nextToken() );
	wl[i][1] = Double.parseDouble( st.nextToken() );
	
//	System.out.println(wl[i][0]);
//    System.out.println(wl[i][1]);
   
   }
   
   // Read in radiance data
   
   for (int i=0; i < numLooks; i++){
    
	indata = in.readLine();
	st = new StringTokenizer(indata, " ");
	
	for (int j=0; j < numChannels; j++){ 
    
	 toaRef[j][i] = Double.parseDouble( st.nextToken() );  
//	 System.out.println(toaRef[j][i]);
	   
    }
   
   }	
   
   // read in other parameters
   
   indata = in.readLine();
   st = new StringTokenizer(indata, " ");
   month =  Integer.parseInt( st.nextToken() );
   
   indata = in.readLine();
   st = new StringTokenizer(indata, " ");
   day =  Integer.parseInt( st.nextToken() );
   
   indata = in.readLine();
   st = new StringTokenizer(indata, " ");
   aerosolModel =  Integer.parseInt( st.nextToken() );
   
   indata = in.readLine();
   st = new StringTokenizer(indata, " ");
   wv =  Double.parseDouble( st.nextToken() );
   
   indata = in.readLine();
   st = new StringTokenizer(indata, " ");
   o3 =  Double.parseDouble( st.nextToken() );
   
   indata = in.readLine();
   st = new StringTokenizer(indata, " ");
   elev =  Double.parseDouble( st.nextToken() );   
   
   indata = in.readLine();
   st = new StringTokenizer(indata, " ");
   rad =  Integer.parseInt( st.nextToken() );
   
   in.close();
   
   }
  
   catch (IOException exception){
  
    System.out.println("Error processing file: " + exception);
    System.exit(1); 
  
   }
 
 } 


/**** Create input cards to 6S  ********************
****** and run 6S for given input parameters *******/ 
 
 public static void inv6s(double tau) {
  
  String s = null;
  String str = null;
  String[] command = {"sh","-c","/geog/home/research/ggwgrey/links/bin/linux/SIXS2/sixs2 < in_6s_param_file"};	
  double cs;
  
  try{
   
   for (int iband=0; iband<numChannels; iband++){
    
	for (int iang=0; iang<numLooks; iang++){  
     
	 cs = 1.0 / Math.cos( solzn[iang] * ( Math.PI / 180.0) );
     
	 FileWriter writer = new FileWriter("in_6s_param_file");
     PrintWriter outfile = new PrintWriter(writer);
  
     outfile.println(0);                                                    // User's conditions
     outfile.println(solzn[iang] + " " + solaz[iang] + " " + viewzn[iang] 
	                 + " " + viewaz[iang] + " " + month + " " + day);
     outfile.println(8);                                                    // User's  model
     outfile.println(wv + " " + o3);                                        // UH2O(G/CM2) ,UO3(CM-ATM)
     outfile.println(aerosolModel);	   								        // Aerosols model
     outfile.println(0);						                            // Next value is the aero. opt. thick @ 550
     outfile.println(tau);		                                            // Aerosol optical thickness @550
     outfile.println(-1.0*elev);                                            // Target altitude in KM
     outfile.println(-1000);					                            // Satellite case
     outfile.println(0);  						                            // USER'S WLINF-WLSUP
     outfile.println(wl[iband][0]  + " " + wl[iband][1]);				    // WLINF  WLSUP
     outfile.println(1);  						                            // Ground Type, i.e. Non-Uniform Surface
     outfile.println("1 1 2.20");					                        // Target, Env.,Radius(KM)
     outfile.println(-0.304); 					                            // Atmospheric correction of RAPP=0.10 

     outfile.close();
 	 
	 Process p = Runtime.getRuntime().exec(command);  
	 BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
	 
	 while ((s = stdout.readLine()) != null) str=s;
	
     StringTokenizer st = new StringTokenizer(str, " ");
    
	 frac_dir =  Double.parseDouble( st.nextToken() );
     cfac =  Double.parseDouble( st.nextToken() );
     xa =  Double.parseDouble( st.nextToken() );
     xb =  Double.parseDouble( st.nextToken() );
     xc =  Double.parseDouble( st.nextToken() );
	 	 
	 if (rad == 1) yval=toaRef[iband][iang]*xa-xb;             // Read in TOA radiance 
     if (rad == 0) yval=cs*cfac*toaRef[iband][iang]*xa-xb;     // Read in TOA reflectance
	 RR[iband][iang] = yval / ( 1.0 + xc * yval);
	 DD[iband][iang]= 1.0-frac_dir;
	 
	 System.out.println(xa + " " + xb  + " " + xc + " " + frac_dir + " " + cfac + " " + RR[iband][iang] + " " + toaRef[iband][iang] + " " + DD[iband][iang] + " " + yval + " " + cs);
	 	 
	 boolean success = (new File("in_6s_param_file")).delete();
	 	 	 
	}
  
   }
    
  }
 
  catch (IOException exc) {
   System.err.println("Error writing to file: " + exc);
  }
 
 }

/**********************  Usage *********************
****************************************************/
 
 public static void usage(){
 
  System.out.println("Usage: /geog/data/laplace/alpha/ggwgrey/bin/linux/java/jdk1.6.0/bin/java chrisaod readincardTest");
  System.exit(1);
 
 }
 
/************************************************** 
*** AOD retrieval over land find optimum coefs ****  
***************************************************/ 
 
 public double f(double p[]){
 
  double DF = 0.3;
  double p6 = 0.35;
  double tot = 0.0;
  double[][] mval = new double[MAX_NUM_CHANNELS][MAX_NUM_LOOKS];
  double[] WG =  new double[MAX_NUM_CHANNELS];
  double dir;
  double dif;
  double g;
  double diff;
  double k;
  
  for (int i=0; i < numChannels ; i++){ 
   WG[i] = 1.0;
   if (i == 0)  WG[i] = 1.1; 	           // special weighting for shortest wavelength
   if (i == (numChannels-1))  WG[i] = 0.9;  // ... and for longest
  }
 
  for (int i=0; i < numChannels ; i++){
  
   for (int j=0; j < numLooks ; j++){
   
     dir=(1.0 - DF * DD[i][j]) * p[numChannels + j] * p[i];
	 g=(1.0-p6) * p[i];
	 dif=(DF * DD[i][j] + g * (1.0 - DF *DD[i][j])) * p6 * p[i] / (1.0 - g);
     mval[i][j] = dif + dir;
	 k=RR[i][j]-mval[i][j];
	 tot = tot + WG[i] * k * k;

   }
  }
  
 for (int j=0; j < numLooks ; j++){
  if (p[numChannels+j] < 0.2) tot=tot+ (0.2-p[numChannels+j])*(0.2-p[numChannels+j])*1000.0;
  if (p[numChannels+j] > 1.5) tot=tot+ (1.5-p[numChannels+j])*(1.5-p[numChannels+j])*1000.0;
 }
   
 return tot;
 
} 

/***************************************************
***  AOD retrieval over land to find optimum tau ***
***************************************************/  


 public double f(double tau){
  
  int i,j, iter;
  double fret=0.0;
  double[][] xi;
  double[] p;
  double ftol;
  
  ftol=0.5e-3;
  powell Powell = new powell();
  
  inv6s(tau);
 
  p = new double[numChannels * numLooks];
  xi = new double[numChannels * numLooks][numChannels * numLooks];
   
  for (i = 0; i < numChannels * numLooks; i++) { 
   
   xi[i][i] = 1;
   
   for (j = i + 1; j < numChannels * numLooks; j++) xi[i][j] = xi[j][i] = 0.0;
   
  }
  
  if (fret <= 0.0) Powell.powell(p, xi, ftol, this);
  
  fret = Powell.fret;
  
  System.out.println(fret);
  System.out.println(tau);
  
  try{
  	 
   FileWriter writer = new FileWriter("aod_inv_file");
   PrintWriter outputfile = new PrintWriter(writer);
   outputfile.println(tau + " " + fret);
   outputfile.close();
  
  }
  
  catch (IOException exc) {
   System.err.println("Error writing to file: " + exc);
  }
  
 // penalise -ve reflectances 
 
  for (i=0; i < numChannels ; i++)
   for (j=0; j < numLooks ; j++) if(RR[i][j] < 0.0) fret = fret + (RR[i][j] * RR[i][j])  * 100000.0;

  return fret;

 } 

/***************************************************
  AOD retrieval over land to find optimum tau 
  using simple ration method.  This method
  is not actually used but is useful for 
  testing Brent numerical routine.
***************************************************/

  public double f1(double tau){
  
  double fmin = 0.0; 
   
  inv6s(tau);
//  fmin = (tau - 2.0)*(tau - 3.5);
  
  for (int i=0; i < numChannels ; i++)
   for (int j=0; j < numLooks ; j++) if(RR[i][j] < 0.0) fmin = fmin + (RR[i][j] * RR[i][j])  * 100000.0;
	
   if (fmin <= 0.0){
   //[looks][channels]
	fmin=RR[3][1]/RR[3][0]-RR[0][1]/RR[0][0];
	fmin=fmin*fmin;
   }
  
  System.out.println(fmin);
  System.out.println(tau);
    
  try{
  	 
   FileWriter writer = new FileWriter("aod_inv_file");
   PrintWriter outputfile = new PrintWriter(writer);
   outputfile.println(tau + " " + fmin);
   outputfile.close();
  
  }
  
  catch (IOException exc) {
   System.err.println("Error writing to file: " + exc);
  }
  
  return fmin;
 
 }
 
/************* Set up constants ********************
****************************************************/
 
 private static final int MAX_NUM_CHANNELS = 62;
 private static final int MAX_NUM_LOOKS = 5; 

/************* Set up arrays and variables *********
****************************************************/  
 
 private static double tau;
 
 private static int numLooks;
 private static int numChannels;
 private static int month;
 private static int day;
 private static int aerosolModel;
 private static double wv;
 private static double o3;
 private static double elev;
 private static int rad;
 private static double[] solzn = new double[MAX_NUM_LOOKS];
 private static double[] solaz = new double[MAX_NUM_LOOKS];
 private static double[] viewzn = new double[MAX_NUM_LOOKS];
 private static double[] viewaz = new double[MAX_NUM_LOOKS];
 private static double[][] wl = new double[MAX_NUM_CHANNELS][2];
 private static double[][] toaRef = new double[MAX_NUM_CHANNELS][MAX_NUM_LOOKS];
 
 private static double[][] RR = new double[MAX_NUM_CHANNELS][MAX_NUM_LOOKS]; 
 private static double[][] DD = new double[MAX_NUM_CHANNELS][MAX_NUM_LOOKS]; 
 private static double frac_dir;
 private static double cfac;
 private static double xa;
 private static double xb;
 private static double xc;
 private static double yval; 


} 
 
