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

public class SheetedSeg {
    public Point2D begin, end;
    public int sheet;

    SheetedSeg(Point2D begin, Point2D end, int sheet) {
        this.begin = begin;
        this.end = end;
        this.sheet = sheet;
    }
}