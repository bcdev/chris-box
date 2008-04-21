/*
 * put your module comment here
 * formatted with JxBeauty (c) johann.langhofer@nextra.at
 */


package  com.kutsyy.lvspod;

import  com.kutsyy.util.*;


/**
 * Insert the type's description here.
 * Created by <A href="http://www.kutsyy.com">Vadim Kutsyy</A><BR>
 * @author <A href="http://www.kutsyy.com">Vadim Kutsyy</A>
 */
public final class SpatialPoint {
    public int Loc;
    public int[] neighbors = new int[0];
    public int xLoc;
    public int yLoc;

    //public double weight=1;
    /**
     * SptialPoint constructor comment.
     */
    public SpatialPoint () {
        super();
    }

    /**
     * Insert the method's description here.
     * @param Location int
     * @param xLocation int
     * @param yLocation int
     */
    public SpatialPoint (int Location, int xLocation, int yLocation) {
        xLoc = xLocation;
        yLoc = yLocation;
        Loc = Location;
        fullneighbors[0] = Location;
    }

    /**
     * Insert the method's description here.
     * @author: <A href="http://www.kutsyy.com>Vadum Kutsyy<\A>
     * @param neighborIndex int
     */
    public void addNeighbor (int neighborIndex) {
        neighbors = La.insertI(neighbors, neighbors.length, neighborIndex);
        fullneighbors = La.insertI(fullneighbors, fullneighbors.length, neighborIndex);
    }

    /**
     * Insert the method's description here.
     * @param Neighbor com.kutsyy.ar.util.SpatialPoint
     */
    public void addNeighbor (SpatialPoint Neighbor) {
        addNeighbor(Neighbor.Loc);
    }

    /**
     * Insert the method's description here.
     * @param neighborIndex int
     */
    public void removeNeighbor (int neighborIndex) {
        for (int i = 0; i < neighbors.length; i++)
            if (neighbors[i] == neighborIndex) {
                neighbors = La.removeI(neighbors, i);
                fullneighbors = La.removeI(fullneighbors, i + 1);
                return;
            }
    }
    public int[] fullneighbors = new int[1];
}



