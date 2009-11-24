/* Copyright 2009 Tim Northover
   
   This file is part of CyclePainter.
   
   CyclePainter is free software: you can redistribute it and/or
   modify it under the terms of the GNU General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.
   
   CyclePainter is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with CyclePainter.  If not, see <http://www.gnu.org/licenses/>.  
*/
package cyclepainter.mathsstate;


import cyclepainter.mapleutil.*;
import cyclepainter.exceptions.*;
import cyclepainter.mathsstate.event.*;

import javax.swing.event.*;
import javax.swing.AbstractListModel;
import java.util.*;
import java.io.*;
import java.awt.geom.*;
import java.awt.Color;

import com.maplesoft.openmaple.*;
import com.maplesoft.externalcall.*;

import com.maplesoft.openmaple.List;


public class RiemannSurface {
    public static final Point2D INFINITY = new Point2D.Double(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    public RiemannSurface(String f, String x, String y, Point2D base, Point2D sheetsBase) throws SetSurfaceException, MapleInitException {
	initialize();
	setSurface(f, x, y, base, sheetsBase);
    }

    public RiemannSurface() throws MapleInitException {
	initialize();
    }

    public RiemannSurface(RiemannSurface s) 
	throws SetSurfaceException, MapleInitException {
	initialize();
	setSurface(s.f, s.x, s.y, s.basePoint, s.sheetsBase);
    }

    private void initialize() throws MapleInitException {
	// First try to get maple up and running
	maple = MapleUtils.connect();

	listeners = new EventListenerList();
    }

    public MapleUtils getMaple() {
	return maple;
    }

    /** Resets to simple elliptic curve */
    public void resetToDefault() {
	try {
	    setSurface("y^2+x^4-1", "x", "y", new Point2D.Double(0, 0), 
		       new Point2D.Double(1.0, 1.0));
	} catch(SetSurfaceException e) {
	    System.err.println(e);
	    System.err.println("Fatal error: setting surface to known, simple default failed");
	    System.exit(1);
	}

    }

    // Routines dealing with the Riemann surface part.
    /**
     * Sets the surface this describes and regenerates all necessary
     * mathematical information. If failure occurs no changes are made.
     * 
     * @param f
     *            Polynomial in two variables representing the surface
     * @param x
     *            First variable in f
     * @param y
     *            Second variable in f
     */
    public void setSurface(String f, String x, String y, 
			   Point2D basePoint, Point2D sheetsBase) throws 
			       SetSurfaceException {
	String s;
	HashMap<Point2D, Algebraic> newMonodromy;
	List newDefiningSheets;
	Algebraic newInfMonodromy;
	Point2D newBasePoint;

	try {
	    // First calculate the monodromy

	    s = String.format("use_base := %s:", maple.pointToString(basePoint));
	    maple.evaluate(s);
	    maple.evaluate("forget(`algcurves/Monodromy`):");
	    s = String.format("mono := `algcurves/monodromy`(%s, %s, %s);", f,
			      x, y);
	    maple.evaluate(s);
	    maple.evaluate("use_base := 'use_base':");
	} catch(MapleException e) {
	    throw new SetSurfaceException("Maple's monodromy calculation failed.\n"
					  +"Surface change aborted.\n"+e);
	}
	
	// Then calculate our canonical sheet numbering at the requested base.
	try {
	    List sheets = (List)maple.evaluate("mono[2]:");
	    newDefiningSheets 
		= maple.propagateSheets(getCurveString(f,x,y), sheets, 
					basePoint, sheetsBase);
	} catch(Exception e) {
	    throw new SetSurfaceException(
	       "Unable to translate maple's monodromy into unique definition "
	       +"of sheets.\n"
	       +"Does straight line path from base point to sheets base go too "
	       +"close to a branch?\n"
	       +e);
	}

	try {
	    // Work out the effect of crossing each branch cut
	    s = String.format("cutmono := mono_to_shift(mono[3], %s, %s):", 
			      maple.pointToString(basePoint), 
			      maple.pointToString(sheetsBase));
	    maple.evaluate(s);

	    // treating infinity properly
	    List inf = (List) maple.evaluate("select(x->x[1]=infinity, cutmono):");
	    newMonodromy = new HashMap<Point2D, Algebraic>();
	    if (inf.length() > 0) {
		List item = (List) inf.select(1);
		newMonodromy.put(INFINITY, item.select(2));
	    }

	    List mapBs = (List) maple.evaluate("remove(x -> x[1]=infinity, cutmono):");
	    for (int i = 1; i <= mapBs.length(); ++i) {
		List item = (List) mapBs.select(i);
		newMonodromy.put(maple.algToPoint(item.select(1)), item.select(2));
	    }
	    mapBs.dispose();

	    // And the basePoint
	    newBasePoint = maple.algToPoint(maple.evaluate("mono[1]:"));

	    maple.evaluate("mono:='mono': cutmono:='cutmono':");
	} catch (MapleException e) {
	    throw new SetSurfaceException(
		 "Unexpected error converting canonical sheets to permutation "
		 +"for each cut.\n"
		 +e);
	}

	// Do this stuff at end so we can revert before if necessary
	this.f = f;
	this.x = x;
	this.y = y;
	this.sheetsBase = sheetsBase;
	this.monodromy = newMonodromy;
	this.basePoint = newBasePoint;
	this.definingSheets = newDefiningSheets;

	fireSurfaceChangeEvent();
    }

    public String getF() {
	return f;
    }

    public String getX() {
	return x;
    }

    public String getY() {
	return y;
    }

    public String getSurfaceString() {
	return String.format("f(%s,%s) = %s", x, y, f);
    }
	
    /** For use in calls to extcurves */
    String getCurveString() {
	return String.format("Record('f'=%s, 'x'=%s, 'y'=%s)", f, x, y);
    }

    String getCurveString(String f, String x, String y) {
	return String.format("Record('f'=%s, 'x'=%s, 'y'=%s)", f, x, y);
    }

    /** The finite branch points of the curve */
    public Set<Point2D> getFiniteBranches() {
	Set<Point2D> branches = new HashSet<Point2D>(monodromy.keySet());
	branches.remove(INFINITY);
	return branches;
    }

    public boolean hasInfiniteBranch() {
	return monodromy.containsKey(INFINITY);
    }

    /** Gives the base point used in monodromy calculations. **/
    public Point2D getBasePoint() {
	return basePoint;
    }

    /** Work out how crossing a given branch cut in a given direction
     *  changes the sheet.
     *
     *  @param sheet
     *         Sheet before the branch cut
     *  @param descr
     *         Description of the branch cut and direction we're
     *         crossing.
     *  @return Sheet after the cut.
     */
    int shiftSheets(int sheet, SheetChange descr) {
	try {
	    Algebraic perm;
	    String cmd;
	    if (descr.dir == 1)
		cmd = "apply_perm(%s, %d):";
	    else
		cmd = "apply_perm(map(ListTools[Reverse], %s), %d):";
			
	    perm = monodromy.get(descr.branch);
			
	    cmd = String.format(cmd, perm.toString(), sheet);
	    Algebraic newSheet = maple.evaluate(cmd);
	    sheet = ((Numeric) newSheet).intValue();
	} catch (MapleException e) {
	    System.err.println("Unexpected error applying known permutation to number");
	    System.err.println("Unable to calculate sheet change across a single, known cut");
	    System.err.println("Path is an unknown state, but continuing");
	}
	// Still initial if maple exception occurred
	return sheet;
    }

    public Point2D getSheetsBase() {
	return this.sheetsBase;
    }
	
    /**
     * Calculate which sheet corresponds to which y-value at a given point
     * @param pt
     *        Where we want to know about
     * @return
     *        nth entry is y-value on sheet n.
     */
    public List getSheets(Point2D pt) throws SheetPropagationException {
	// We need to analytically continue
	List sheets = definingSheets;
	Point2D from = sheetsBase;
	try {
	    sheets = maple.propagateSheets(getCurveString(), sheets, from, pt);
			
	    // Now we have to apply the sheet changes of any cuts we've crossed between
	    // from and to.
	    Collection<SheetChange> cuts = splitSegment(from, pt);
	    Procedure permute_list = (Procedure)maple.evaluate("op(permute_list):");
	    for(SheetChange cut : cuts) {
		Algebraic perm = monodromy.get(cut.branch);
		if(cut.dir == -1) {
		    String cmd = String.format("map(ListTools[Reverse], %s):", perm);
		    perm = maple.evaluate(cmd);
		}

		sheets = (List)permute_list.execute(new Algebraic[] {sheets, perm});
	    }
	} catch(MapleException e) {
	    System.err.println("Couldn't update sheet order by analytic continuation");
	    System.err.println("Unexpected error. Not related to continuation");
	    System.err.println(e);
	} 
	return sheets;
    }
	
    /** Split a segment at each cut it crosses.
     * 
     * @param begin
     * @param end
     * @return List specifying each branch cut, sorted by distance along path
     */
	
    Collection<SheetChange> splitSegment(Point2D begin, Point2D end) {
	Line2D segment = new Line2D.Double(begin, end);
	TreeMap<Double, SheetChange> isectPoints = new TreeMap<Double, SheetChange>();
		
	ArrayList<Line2D> branchLines = new ArrayList<Line2D>();
	for (Point2D pt : getFiniteBranches()) {
	    branchLines.add(new Line2D.Double(getBasePoint(), pt));
	}

	// Find where the line needs to be cut and order by distance
	// from the beginning
	for (Line2D cut : branchLines) {
	    if (cut.intersectsLine(segment)) {
		SheetChange shift = intersection(segment, cut);
		if(shift.equals(begin))
		    continue;
		shift.branch = cut.getP2();
		isectPoints.put(shift.isection.distance(begin), shift);
	    }
	}
	// And the infinite cut if necessary
	if(hasInfiniteBranch()) {
	    Point2D basePoint = getBasePoint();
	    Line2D cut = new Line2D.Double(basePoint,
					   new Point2D.Double(Double.NEGATIVE_INFINITY, basePoint.getY())
					   );
	    if(cut.intersectsLine(segment)) {
		// Special code here since infinities are involved.
		double t = (basePoint.getY()-begin.getY())/(end.getY()-begin.getY());
		double x = begin.getX()+t*(end.getX()-begin.getX());
		Point2D isection = new Point2D.Double(x, basePoint.getY());

		SheetChange sc = new SheetChange(RiemannSurface.INFINITY, isection, 
						 begin.getY() > basePoint.getY() ? -1 : 1);
		isectPoints.put(isection.distance(begin), sc);
	    }
	}
	return isectPoints.values();
    }
	
    /**
     * Returns the point of intersection of l1 and l2, considered as lines.
     * 
     * @param l1
     * @param l2
     * @return
     */
    SheetChange intersection(Line2D l1, Line2D l2) {
	double num = (l2.getX1() - l1.getX1()) * (l2.getY2() - l2.getY1());
	num -= (l2.getY1() - l1.getY1()) * (l2.getX2() - l2.getX1());
	double denom = (l1.getX2() - l1.getX1()) * (l2.getY2() - l2.getY1());
	denom -= (l1.getY2() - l1.getY1()) * (l2.getX2() - l2.getX1());
	double t = num / denom;

	double x = l1.getX1() + t * (l1.getX2() - l1.getX1());
	double y = l1.getY1() + t * (l1.getY2() - l1.getY1());

	int dir = l1.relativeCCW(l2.getP1());

	return new SheetChange(new Point2D.Double(x, y), dir);
    }

    public void addSurfaceChangeListener(SurfaceChangeListener l) {
	listeners.add(SurfaceChangeListener.class, l);
    }

    public void removeSurfaceChangeListener(SurfaceChangeListener l) {
	listeners.remove(SurfaceChangeListener.class, l);
    }

    protected void fireSurfaceChangeEvent() {
	Object[] ls = listeners.getListenerList();

	for (int i = ls.length - 2; i >= 0; i -= 2) {
	    if (ls[i] == SurfaceChangeListener.class) {
		((SurfaceChangeListener) ls[i + 1]).surfaceChanged(this);
	    }
	}
    }


    // Java infrastructure
    EventListenerList listeners;

    // Surface data
    String x, y, f;
    HashMap<Point2D, Algebraic> monodromy;
    Point2D basePoint;
	
    List definingSheets;
    Point2D sheetsBase;

    MapleUtils maple;
}