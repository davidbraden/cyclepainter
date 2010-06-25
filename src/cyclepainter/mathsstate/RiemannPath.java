/* Copyright 2009, 2010 Tim Northover
   
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

import cyclepainter.exceptions.*;
import cyclepainter.mapleutil.*;
import cyclepainter.mathsstate.event.*;
import cyclepainter.Colours;

import java.util.*;
import javax.swing.event.*;
import java.awt.geom.*;

import com.maplesoft.openmaple.*;
import com.maplesoft.externalcall.*;

public class RiemannPath extends ArrayList<Point2D> {
    public RiemannPath() {
        super();
        initialize();
    }

    public RiemannPath(java.util.List<Point2D> points) {
        super(points);
        initialize();
    }

    public RiemannPath(java.util.List<Point2D> points, Point2D sheet) {
        super(points);
        initialize();

        setInitialYValue(sheet);
    }

    void initialize() {
        listeners = new EventListenerList();

        setInitialYValue(new Point2D.Double());
    }

    @Override
    public boolean add(Point2D pt) {
        if (size() != 0 && pt.equals(get(size() - 1)))
            return true;
        super.add(pt);

        firePathChangedEvent();
        if (size() == 1)
            fireSheetChangedEvent();
        return true;
    }

    /**
     * Breaks the intrinsic segments up into ones with well-defined sheet and
     * calculates the 0-indexed sheet number.
     * 
     * @return List of segments ready to be plotted.
     */
    public java.util.List<SheetedSeg> getSegments(CutScheme cuts) {
        java.util.LinkedList<SheetedSeg> allSegs = new LinkedList<SheetedSeg>();

        ListIterator<Point2D> i = this.listIterator();

        Point2D begin = i.next(), end;
        int sheetNum = 0;

        try {
            sheetNum = cuts.getSheet(begin, sheet);
        } catch (SheetPropagationException e) {
            System.err
                    .println("Error propagating sheets while splitting path at cuts");

            while (i.hasNext()) {
                end = i.next();
                allSegs.add(new SheetedSeg(begin, end, Colours.MAX_COLOUR));

                begin = end;
            }

            return allSegs;
        }

        while (i.hasNext()) {
            end = i.next();
            allSegs.addAll(cuts.splitSegment(begin, end, sheetNum));

            begin = end;
            sheetNum = allSegs.getLast().sheet;
        }

        return allSegs;
    }

    /**
     * Finds closest node on this path to a given point.
     * 
     * @param pt
     *            The point we want to trace.
     * @return
     */
    public Point2D closestTo(Point2D pt) {
        return closestTo(pt, null);
    }

    /**
     * Finds closest node on this path to a given point, excluding another;
     * useful for dragging a given point which we don't want to snap to itself
     * 
     * @param pt
     * @param excluding
     * @return
     */
    public Point2D closestTo(Point2D pt, Point2D excluding) {
        double minDist = 1.0e200;
        Point2D closest = null;
        for (Point2D ours : this) {
            if (ours == excluding)
                continue;

            double d = pt.distance(ours);
            if (d < minDist) {
                minDist = d;
                closest = ours;
            }
        }
        return closest;
    }

    /**
     * Moves a given point. Takes care of analytic continuation if necessary to
     * ensure initial sheet stays the same analytically.
     * 
     * @param node
     *            The point we want moved. Must be an element of this Path
     *            considered as a container otherwise rather pointless.
     * @param to
     *            Where it has to go
     */
    public void displace(RiemannSurface surface, Point2D node, Point2D to) {
        if (isEmpty())
            return;
        if (node == this.get(0)) {
            try {
                MapleUtils maple = surface.getMaple();
                String cmd;
                cmd = String.format("displace_sheet_base(%s, %s, %s):",
                        surface.getCurveString(),
                        MapleUtils.pointToString(sheet),
                        MapleUtils.pointToString(to));
                Algebraic newSheet = maple.evaluate(cmd);
                sheet = maple.algToPoint(newSheet);
                fireSheetChangedEvent();
                firePathChangedEvent();
            } catch (MapleException e) {
                System.err
                        .println("Unable to deform path continuously. Is the initial point near a branch?");
                System.err.println("Path's sheets are now probably wrong.");
                System.err.println(e);
            }
        }

        node.setLocation(to);
        firePathChangedEvent();
    }

    /**
     * Returns list of points as would be understood by maple
     * 
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("[");
        for (int i = 0; i < size(); ++i) {
            Point2D cur = get(i);
            s.append(MapleUtils.pointToString(cur));
            if (i != size() - 1)
                s.append(", ");
        }
        s.append("]");
        return s.toString();
    }

    public Point2D getInitialYValue() {
        return sheet;
    }

    public void setInitialYValue(Point2D newSheet) {
        sheet = newSheet;

        fireSheetChangedEvent();
    }

    // And the necessary event crap...

    public void addPathChangeListener(PathChangeListener l) {
        listeners.add(PathChangeListener.class, l);
    }

    public void removePathChangeListener(PathChangeListener l) {
        listeners.remove(PathChangeListener.class, l);
    }

    protected void firePathChangedEvent() {
        Object[] ls = listeners.getListenerList();

        for (int i = ls.length - 2; i >= 0; i -= 2) {
            if (ls[i] == PathChangeListener.class) {
                ((PathChangeListener) ls[i + 1]).pathChanged(this);
            }
        }
    }

    protected void fireSheetChangedEvent() {
        Object[] ls = listeners.getListenerList();

        for (int i = ls.length - 2; i >= 0; i -= 2) {
            if (ls[i] == PathChangeListener.class) {
                ((PathChangeListener) ls[i + 1]).sheetChanged(this);
            }
        }
    }

    // TODO: Remove all possible cruft after here.

    // /** Calculates sheet number from the current y-value of the sheet.
    // * Stores result in cache for looking up later.
    // */
    // private void updateSheetNum() {
    // if(isEmpty())
    // sheets = surface.definingSheets;
    // try {
    // sheets = surface.getSheets(this.get(0));
    // String pt = maple.pointToString(sheet);
    // String s = "best_match(%s, %s)[1]:";
    // s = String.format(s, pt, sheets);
    // Numeric num = (Numeric)maple.evaluate(s);
    // sheetNum = num.intValue();
    // } catch(MapleException e) {
    // System.err.println("Couldn't put integer sheet number to point "+sheet);
    // System.err.println("Unexpected maple error: "+e);
    // System.err.println("Defaulting to sheet 1 for appearances' sake.");
    // sheetNum = 1;
    // } catch(SheetPropagationException e) {
    // System.err.println("Couldn't put integer sheet number to point "+sheet);
    // System.err.println("Defaulting to sheet 1.");
    // System.err.println(e);
    // sheetNum = 1;
    // }

    // fireSheetChangedEvent();
    // }

    // /** Makes use of cached sheet number for request */
    // public int getInitialSheet() {
    // return sheetNum;
    // }

    // public void setInitialSheet(int sheetNum) throws
    // IndexOutOfBoundsException {
    // if(isEmpty())
    // return;

    // try {
    // List sheets = surface.getSheets(this.get(0));
    // sheet = maple.algToPoint(sheets.select(sheetNum));
    // this.sheetNum = sheetNum;
    // fireSheetChangedEvent();
    // } catch(MapleException e) {
    // throw new
    // IndexOutOfBoundsException("Requested sheet number out of range: "+sheetNum);
    // } catch(SheetPropagationException e) {
    // System.err.println("Error setting sheet number of path at "+this.get(0));
    // System.err.println("Ignoring request.");
    // System.err.println(e);
    // }
    // }

    // public void setInitialSheetPoint(Point2D sheet) {
    // this.sheet = sheet;
    // if(!isEmpty()) {
    // updateSheetNum();
    // fireSheetChangedEvent();
    // }
    // }

    // /** The list of y-values which identifies sheet numbers at the initial
    // * point of this path. Result on an empty path is undefined.
    // * @return l.select(1) is y-value of sheet 1 at first point...
    // */
    // public List getSheets() {
    // return sheets;
    // }

    // public Point2D getSheetByNum(int index)
    // throws IndexOutOfBoundsException {
    // try {
    // List sheets = getSheets();
    // return maple.algToPoint(sheets.select(index));
    // } catch(MapleException e) {
    // throw new IndexOutOfBoundsException("Trying to find sheet "
    // + index + " at " + this.get(0));
    // }
    // }

    EventListenerList listeners;

    // RiemannSurface surface;
    // List sheets;
    Point2D sheet;
    // int sheetNum;
}