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

import java.util.Collection;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;

import cyclepainter.mathsstate.event.SurfaceChangeListener;
import cyclepainter.exceptions.SheetPropagationException;

/**
 * Interface needed to assign points to sheets and split segments into sheeted
 * parts as required.
 * 
 * This base is a trivial implementation that maps everything to sheet 0.
 */

public class CutScheme implements SurfaceChangeListener {
    public List<SheetedSeg> splitSegment(Point2D begin, Point2D end,
            Point2D sheet) throws SheetPropagationException {
        return splitSegment(begin, end, getSheet(begin, sheet));
    }

    public List<SheetedSeg> splitSegment(Point2D begin, Point2D end,
            int sheetNum) {
        List<SheetedSeg> arr = new LinkedList<SheetedSeg>();
        arr.add(new SheetedSeg(begin, end, sheetNum));
        return arr;
    }

    public int getSheet(Point2D x, Point2D y) throws SheetPropagationException {
        return 0;
    }

    public List<Point2D> getAllSheets(Point2D x)
            throws SheetPropagationException {
        ArrayList<Point2D> sheets = new ArrayList<Point2D>();
        sheets.add(new Point2D.Double(0, 0));
        return sheets;
    }

    public Point2D getYValue(Point2D x, int sheet)
            throws SheetPropagationException {
        return new Point2D.Double();
    }

    public Collection<Line2D> cutGraph(Point2D minClip, Point2D maxClip) {
        return new LinkedList<Line2D>();
    }

    public int numSheets() {
        return 1;
    }

    @Override
    public void surfaceChanged(RiemannSurface surface) {
        // Don't care
    }
}