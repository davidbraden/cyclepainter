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
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;

public class RadialCutScheme extends CutScheme {

    public List<SheetedSeg> splitSegment(Point2D begin, Point2D end, int sheetNum) {
        ArrayList<SheetedSeg> arr = new ArrayList<SheetedSeg>();
        arr.add(new SheetedSeg(begin, end, sheetNum));
        return arr;

// Was part off RiemannPath::getSegments. This may well be the place for it.
	// int sheet = getInitialSheet();
	// ArrayList<SheetedSeg> segs = new ArrayList<SheetedSeg>();

	// // And paint the path
	// ListIterator<Point2D> i = listIterator();

	// Point2D begin = (Point2D) i.next().clone();
	
	// while (i.hasNext()) {
	//     SheetedSeg seg;
	//     Point2D end = i.next();
	    
	//     // Find the points where we need to split
	//     Collection<SheetChange> isections = surface.splitSegment(begin, end);
	    
	//     // And do any cutting needed.
	//     for (SheetChange block : isections) {
	// 	seg = new SheetedSeg();
	// 	seg.begin = begin;
	// 	seg.end = block.isection;
	// 	seg.sheet = sheet;
	// 	segs.add(seg);
		
	// 	begin = seg.end;
	// 	sheet = surface.shiftSheets(sheet, block);
	//     }
	    
	//     // And take care of the final segment
	//     seg = new SheetedSeg();
	//     seg.begin = begin;
	//     seg.end = end;
	//     seg.sheet = sheet;
	//     segs.add(seg);
	    
	//     begin = seg.end;
	// }
	// return segs;


    }
}