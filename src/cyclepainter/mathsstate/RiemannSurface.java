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
import java.util.*;
import java.awt.geom.*;

import com.maplesoft.openmaple.*;
import com.maplesoft.externalcall.*;

import com.maplesoft.openmaple.List;

public class RiemannSurface {
    public static final Point2D INFINITY = new Point2D.Double(
            Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    public RiemannSurface(String f, String x, String y, Point2D base,
            Point2D sheetsBase) throws SetSurfaceException, MapleInitException {
        initialize();
        setSurface(f, x, y, base, sheetsBase);
    }

    public RiemannSurface() throws MapleInitException {
        initialize();
    }

    public RiemannSurface(RiemannSurface s) throws SetSurfaceException,
            MapleInitException {
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
        } catch (SetSurfaceException e) {
            System.err.println(e);
            System.err
                    .println("Fatal error: setting surface to known, simple default failed");
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
    public void setSurface(String f, String x, String y, Point2D basePoint,
            Point2D sheetsBase) throws SetSurfaceException {
        Algebraic newMonodromy;
        HashSet<Point2D> newBranches;
        List newDefiningSheets;

        Point2D newBasePoint = basePoint;
        String s;

        try {
            // First calculate the monodromy

            s = "mono := central_monodromy(Record('f'=%s,'x'=%s, 'y'=%s), %s);";
            s = String.format(s, f, x, y, maple.pointToString(basePoint));
            newMonodromy = maple.evaluate(s);

            List mapBranches = (List) maple.evaluate("map(x->x[1], mono[3]):");
            newBranches = new HashSet<Point2D>();

            for (int i = 1; i <= mapBranches.length(); ++i)
                newBranches.add(maple.algToPoint(mapBranches.select(i)));

            newBasePoint = maple.algToPoint(maple.evaluate("mono[1]:"));
        } catch (MapleException e) {
            throw new SetSurfaceException(
                    "Maple's monodromy calculation failed.\n"
                            + "Surface change aborted.\n" + e);
        }

        // Then calculate our canonical sheet numbering at the requested base.
        try {
            List sheets = (List) maple.evaluate("mono[2]:");
            newDefiningSheets = maple.propagateSheets(getCurveString(f, x, y),
                    sheets, basePoint, sheetsBase);
        } catch (Exception e) {
            throw new SetSurfaceException(
                    "Unable to translate maple's monodromy into unique definition "
                            + "of sheets.\n"
                            + "Does straight line path from base point to sheets base go too "
                            + "close to a branch?\n" + e);
        }

        // Do this stuff at end so we can revert before if necessary
        this.f = f;
        this.x = x;
        this.y = y;
        this.sheetsBase = sheetsBase;
        this.branches = newBranches;
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

    public List getDefiningSheets() {
        return definingSheets;
    }

    /** The finite branch points of the curve */
    public Set<Point2D> getFiniteBranches() {
        if (this.branches == null)
            return new HashSet<Point2D>();

        Set<Point2D> branches = new HashSet<Point2D>(this.branches);
        branches.remove(INFINITY);
        return branches;
    }

    public boolean hasInfiniteBranch() {
        if (branches == null)
            return false;

        return branches.contains(INFINITY);
    }

    /** Gives the base point used in monodromy calculations. **/
    public Point2D getBasePoint() {
        return basePoint;
    }

    public Point2D getSheetsBase() {
        return this.sheetsBase;
    }

    public Algebraic getMonodromy() {
        return monodromy;
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
    Set<Point2D> branches;
    Algebraic monodromy;
    Point2D basePoint;

    List definingSheets;
    Point2D sheetsBase;

    MapleUtils maple;
}