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

/** CutScheme implementing outwards-pointing cuts from sheetsBase */

public class OutRadialCutScheme extends RadialCutScheme {
    @Override
    public void surfaceChanged(RiemannSurface surface) {
        super.surfaceChanged(surface);
        if (surface.hasInfiniteBranch()) {
            System.err
                    .println("Error: Outwards pointing radial cuts only valid for surfaces");
            System.err
                    .println("without infinity as a branch point. Reverting to inwards cuts.");
            return;
        }

        // Now we modify the permutations etc so they represent
        // outwards-pointing cuts
        precalcPermutations();

        branchCuts = new HashMap<Point2D, Line2D>();
        Point2D base = surface.getSheetsBase();
        for (Point2D branch : surface.getFiniteBranches()) {

            double xEnd = base.getX() + 100 * (branch.getX() - base.getX());
            double yEnd = base.getY() + 100 * (branch.getY() - base.getY());

            branchCuts.put(branch, new Line2D.Double(branch,
                    new Point2D.Double(xEnd, yEnd)));
        }
    }

    @Override
    void precalcPermutations() {
        cutsPermutation = new HashMap<Point2D, Algebraic>();

        try {
            maple.assignName("mono", surface.getMonodromy());
            List perms = (List) maple.evaluate("mono[3]:");

            for (int i = 1; i <= perms.length(); ++i) {
                List pair = (List) maple.select(perms, i);
                Point2D branch = maple.algToPoint(maple.select(pair, 1));

                cutsPermutation.put(branch, maple.select(pair, 2));
            }
        } catch (MapleException e) {
            System.err.println("Unexpected format for monodromy");
            System.err.println(e);
        }
    }
}