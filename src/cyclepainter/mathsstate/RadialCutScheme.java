/* Copyright 2010 Tim Northover
   
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

import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.util.*;

import com.maplesoft.openmaple.*;
import com.maplesoft.externalcall.*;

import com.maplesoft.openmaple.List;

import cyclepainter.exceptions.*;
import cyclepainter.mapleutil.MapleUtils;

public class RadialCutScheme extends CutScheme {

    public java.util.List<SheetedSeg> splitSegment(Point2D begin, Point2D end, int sheetNum) {
        LinkedList<SheetedSeg> segs = new LinkedList<SheetedSeg>();

	// Find the points where we need to split
	Collection<SheetChange> isections = segmentParts(begin, end);
	    
	// And do any cutting needed.
	for (SheetChange block : isections) {
	    SheetedSeg seg = new SheetedSeg(begin, block.isection, sheetNum);
	    segs.add(seg);

	    begin = seg.end;
	    sheetNum = shiftSheets(sheetNum, block);
	}

	// Add the final part after any cuts
	segs.add(new SheetedSeg(begin, end, sheetNum));

	return segs;
    }

    public int getSheet(Point2D x, Point2D y) throws SheetPropagationException {
	if (lastX == null || ! lastX.equals(x)) {
	    lastX = x;
	    lastSheets = getAllSheets(x);
	}

	double bestDist = Double.POSITIVE_INFINITY;
	int bestIdx = 0;
	for (int i = 0; i < lastSheets.size(); ++i) {
	    double curDist = y.distance(lastSheets.get(i));
	    if (curDist < bestDist) {
		bestIdx = i;
		bestDist = curDist;
	    }
	}
	
	return bestIdx;
    }

    public Point2D getYValue(Point2D x, int sheet) throws SheetPropagationException {	
	if (lastX == null || ! lastX.equals(x)) {
	    lastX = x;
	    lastSheets = getAllSheets(x);
	}

	return lastSheets.get(sheet);
    }

    public int numSheets() {
	try {
	    return surface.getDefiningSheets().length();
	} catch(MapleException e) {
	    System.err.println("Unexpected maple exception");
	    System.err.println(e);
	}
	return 0;
    }

    public void surfaceChanged(RiemannSurface surface) {
	this.surface = surface;
	maple = surface.getMaple();

	branchCuts = new LinkedList<Line2D>();
	for (Point2D pt : surface.getFiniteBranches()) {
	    branchCuts.add(new Line2D.Double(surface.getBasePoint(), pt));
	}

	if (surface != null)
	    precalcPermutations();
    }

    // Implicit assumption that "mono" is still bound to the relevant
    // monodromy in maple's namespace
    void precalcPermutations() {

	
	// TODO: After here should probably be in cut scheme

	try {
	    String s;
	    // Work out the effect of crossing each branch cut
	    s = String.format("cutmono := mono_to_shift(mono[3], %s, %s):", 
			      maple.pointToString(surface.getBasePoint()), 
			      maple.pointToString(surface.getSheetsBase()));
	    maple.evaluate(s);

	    // treating infinity properly
	    List inf = (List) maple.evaluate("select(x->x[1]=infinity, cutmono):");
	    cutsPermutation = new HashMap<Point2D, Algebraic>();
	    if (inf.length() > 0) {
		List item = (List) inf.select(1);
		cutsPermutation.put(RiemannSurface.INFINITY, item.select(2));
	    }

	    List mapBs = (List) maple.evaluate("remove(x -> x[1]=infinity, cutmono):");
	    for (int i = 1; i <= mapBs.length(); ++i) {
		List item = (List) mapBs.select(i);
		cutsPermutation.put(maple.algToPoint(item.select(1)), item.select(2));
	    }
	    mapBs.dispose();

	    // And the basePoint

	    maple.evaluate("mono:='mono': cutmono:='cutmono':");
	} catch (MapleException e) {
	    // We have to carry on as best we can,
	    // unfortunately. Still, this really shouldn't happen

	    cutsPermutation = new HashMap<Point2D, Algebraic>();
	    System.err.println("Unexpected error converting canonical sheets to permutation "
		 +"for each cut.");
	    System.err.println(e);
	}

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
			
	    perm = cutsPermutation.get(descr.branch);
			
	    cmd = String.format(cmd, perm.toString(), sheet+1);
	    Algebraic newSheet = maple.evaluate(cmd);
	    sheet = ((Numeric) newSheet).intValue() - 1;
	} catch (MapleException e) {
	    System.err.println("Unexpected error applying known permutation to number");
	    System.err.println("Unable to calculate sheet change across a single, known cut");
	    System.err.println("Path is an unknown state, but continuing");
	}
	// Still initial if maple exception occurred
	return sheet;
    }

    /**
     * Calculate which sheet corresponds to which y-value at a given point
     * @param pt
     *        Where we want to know about
     * @return
     *        nth entry is y-value on sheet n.
     */
    public ArrayList<Point2D> getAllSheets(Point2D pt) throws SheetPropagationException {
	// We need to analytically continue
	ArrayList<Point2D> sheets = new ArrayList<Point2D>(); 
	List mapSheets = surface.getDefiningSheets();
	Point2D from = surface.getSheetsBase();
	try {
	    mapSheets = maple.propagateSheets(surface.getCurveString(), mapSheets, from, pt);
			
	    // Now we have to apply the sheet changes of any cuts we've crossed between
	    // from and to.
	    Collection<SheetChange> cuts = segmentParts(from, pt);
	    Procedure permute_list = (Procedure)maple.evaluate("op(permute_list):");

	    for(SheetChange cut : cuts) {
		Algebraic perm = cutsPermutation.get(cut.branch);
		if(cut.dir == -1) {
		    String cmd = String.format("map(ListTools[Reverse], %s):", perm);
		    perm = maple.evaluate(cmd);
		}
		mapSheets = (List)permute_list.execute(new Algebraic[] {mapSheets, perm});
	    }

	    for(int i = 1; i <= mapSheets.length(); ++i) {
		Algebraic yval = mapSheets.select(i);
		sheets.add(maple.algToPoint(yval));
	    }
	} catch(MapleException e) {
	    System.err.println("Couldn't update sheet order by analytic continuation");
	    System.err.println("Unexpected error. Not related to continuation");
	    System.err.println(e);
	}
	return sheets;
    }

    public Collection<Line2D> cutGraph(Point2D minClip, Point2D maxClip) {
	// TODO: add infinite branch
	return branchCuts;
    }
	
    /** Split a segment at each cut it crosses.
     * 
     * @param begin
     * @param end
     * @return List specifying each branch cut, sorted by distance along path
     */
	
    Collection<SheetChange> segmentParts(Point2D begin, Point2D end) {
	Line2D segment = new Line2D.Double(begin, end);
	TreeMap<Double, SheetChange> isectPoints = new TreeMap<Double, SheetChange>();
		
	// Find where the line needs to be cut and order by distance
	// from the beginning
	for (Line2D cut : branchCuts) {
	    if (cut.intersectsLine(segment)) {
		SheetChange shift = intersection(segment, cut);
		if(shift.equals(begin))
		    continue;
		shift.branch = cut.getP2();
		isectPoints.put(shift.isection.distance(begin), shift);
	    }
	}
	// And the infinite cut if necessary
	if(surface.hasInfiniteBranch()) {
	    Point2D basePoint = surface.getBasePoint();
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

    RiemannSurface surface;

    HashMap<Point2D, Algebraic> cutsPermutation;
    java.util.List<Line2D> branchCuts;
    
    java.util.ArrayList<Point2D> lastSheets;
    Point2D lastX;
    
    MapleUtils maple;
}