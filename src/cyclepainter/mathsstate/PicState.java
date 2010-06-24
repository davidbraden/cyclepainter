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
import cyclepainter.mathsstate.event.*;
import cyclepainter.exceptions.*;
import cyclepainter.Colours;

import java.io.*;
import java.util.*;
import javax.swing.event.*;
import java.awt.geom.*;
import java.awt.Color;

import com.maplesoft.openmaple.*;
import com.maplesoft.externalcall.*;

import com.maplesoft.openmaple.List;

public class PicState {

    public PicState() throws MapleInitException {
	initialize();

	addDefaultPaths();
    }

    public PicState(InputStream data) 
	throws IOException, PicFormatException, 
	       SetSurfaceException, MapleInitException {
	initialize();
	readData(data);
    }

    /** Resets all data to a primitive elliptic curve with a couple of
      * empty cycles.
      **/
    public void resetToDefault() {
	surface.resetToDefault();
	description = "";
	addDefaultPaths();
    }

    private void initialize() throws MapleInitException {
	listeners = new EventListenerList();

	description = "";

	maple = MapleUtils.connect();

	surface = new RiemannSurface();
	surface.resetToDefault();
       
	cutScheme = new RadialCutScheme();
	cutScheme.surfaceChanged(surface);
	surface.addSurfaceChangeListener(cutScheme);

	paths = new TreeMap<String, RiemannPath>();

    }

    private void addDefaultPaths() {
	paths.clear();
	addPath("a[1]");
	addPath("b[1]");
    }

    public CutScheme getCutScheme() {
        return cutScheme;
    }

    public MapleUtils getMaple() {
	return maple;
    }

    public RiemannSurface getSurface() {
	return surface;
    }

    // Path access routines
    public RiemannPath getPath(String name) {
	return paths.get(name);
    }

    /**
     * Adds a new empty path to the list with a given name
     * 
     * @param name
     *              Name of the path to add. Must be maple-valid.
     */
    public void addPath(String name) {
	paths.put(name, new RiemannPath());
	firePathSetEvent();
    }

    /**
     * Deletes a path with a given name, if it exists
     */
    public void delPath(String name) {
	paths.remove(name);
	firePathSetEvent();
    }

    public boolean hasPath(String name) {
	return paths.containsKey(name);
    }

    public Set<String> getPathNames() {
	return paths.keySet();
    }

    // I/O routines
    /** Write maple-valid stream that we can interpret too 
     * @param outBasic
     *                  Where to put the data
     */
    public void writeData(OutputStream outBasic) {
	PrintStream out = new PrintStream(outBasic);

	// Note ';' on descr. Seems nice to print this info on load.
	out.printf("descr := \"%s\";\n", description);

	// Now write the defining properties of the surface itself
	out.printf("f := %s:\n", surface.getF());
	out.printf("x := '%s':\n",surface.getX());
	out.printf("y := '%s':\n", surface.getY());

	// And information about cuts/sheets
	out.printf("mono_base := %s:\n", 
		   maple.pointToString(surface.basePoint));
	out.printf("sheets_base := %s:\n", 
		   maple.pointToString(surface.sheetsBase));
	out.println();

	// And the individual cycles next
	for (String name : getPathNames()) {
	    out.print(name);
	    out.print(" := ");
	    out.print(paths.get(name).toString());
	    out.println(":");
	}

	// Now build the last three arrays:
	// homology is a list of the projections onto C.
	// Could have been defined directly rather than including above
	// individual cycles.
	StringBuilder homList = new StringBuilder();
	homList.append("homology := [");

	// names is a list of the names of each path for display.
	StringBuilder nameList = new StringBuilder();
	nameList.append("names := [");

	// sheets is a list of the initial y-value on each path.
	StringBuilder sheetList = new StringBuilder();
	sheetList.append("sheets := [");

	Iterator<String> i = getPathNames().iterator();
	while (i.hasNext()) {
	    String name = i.next();
	    homList.append(name);

	    nameList.append('"');
	    nameList.append(name);
	    nameList.append('"');

	    Point2D sheet = getPath(name).getInitialYValue();
	    sheetList.append(maple.pointToString(sheet));

	    if (i.hasNext()) {
		homList.append(", ");
		nameList.append(", ");
		sheetList.append(", ");
	    }
	}
	homList.append("]:");
	nameList.append("]:");
	sheetList.append("]:");

	out.println();
	out.println(sheetList);
	out.println();
	out.println(homList);
	out.println();
	out.println(nameList);

    }

    /** Outputs a metapost diagram of the current state */
    public void writeMetapost(OutputStream outBasic) {
	PrintStream out = new PrintStream(outBasic);

	// Define the colours used.
	out.println("color colours[];");
	for(int i = 0; i < Colours.SHEET_COLOURS.length; ++i) {
	    Color c = Colours.SHEET_COLOURS[i];
	    out.printf("colours%d := (%d,%d,%d);\n", i+1, c.getRed(), c.getGreen(), c.getBlue());
	}
	out.println();

	// Now each of the paths.
	// Metapost is *horribly* limited, so we have to name them A,B,...
	// regardless of the official name.
	char specifier = 'A';
	double minX = Double.POSITIVE_INFINITY;
	for(RiemannPath path : paths.values()) {
	    out.printf("def draw%c(expr trans) = begingroup\n", specifier++);
	    out.println("save p;");
	    
	    int curSheet = -1;
	    StringBuilder curTrack = null;

	    // First get the path we'll be cutting up
	    java.util.List<SheetedSeg> segs = path.getSegments(cutScheme);
	    
	    // First create the path
	    out.println("\tpath p;");
	    out.print("\tp := ");
	    for(SheetedSeg seg : segs) {
		if (seg.begin.getX() < minX)
		    minX = seg.begin.getX();
		out.printf("(%f,%f)..", seg.begin.getX(), seg.begin.getY());
	    }
	    out.println("cycle;");

	    // Now work out where we need to split and do that. First seg gets an arrow.
	    int curT = 0;
	    String drawCmd = "drawarrow";
	    for(SheetedSeg seg : segs) {
		out.printf("\t%s subpath (%d, %d) of p transformed trans withcolor colours%d;\n", 
			       drawCmd, curT, curT+1, seg.sheet);
		++curT;
		drawCmd = "draw";
	    }
	    out.println("endgroup");
	    out.println("enddef;");		
	}

	// Routine to draw the branch points.
	out.println("def drawbranches(expr trans) = begingroup");
	for (Point2D branch : surface.getFiniteBranches()) {
	    if (branch.getX() < minX)
		minX = branch.getX();
	    out.printf("\tdraw ( (%f,%f)--(%f,%f) ) transformed trans withcolor 0.5white;\n", 
		       surface.getBasePoint().getX(), 
		       surface.getBasePoint().getY(), 
		       branch.getX(), branch.getY());
	}

	// And the infinite one...
	minX = (minX - surface.getBasePoint().getX())*1.1;
	minX += surface.getBasePoint().getX();
	out.printf("\tdraw ( (%f,%f)--(%f,%f) ) transformed trans withcolor 0.5white;\n",
		   surface.getBasePoint().getX(),
		   surface.getBasePoint().getY(),
		   minX, surface.getBasePoint().getY());

	out.println("\tpickup pencircle scaled 4pt;");
	for(Point2D branch : surface.getFiniteBranches()) {
	    out.printf("\tdrawdot (%f,%f) transformed trans;\n", 
		       branch.getX(), branch.getY());
	}

	out.println("\tpickup defaultpen;");

	out.println("endgroup");
	out.println("enddef;");
	out.println();
    }

    /**
     *  Read a .pic file to set all state.
     */
    public void readData(InputStream inBasic) 
	throws IOException, PicFormatException, SetSurfaceException {
	InputStreamReader in = new InputStreamReader(inBasic);
	StringBuilder streamData = new StringBuilder();
	char buf[] = new char[1024];
	int got;

	do {
	    got = in.read(buf, 0, 1024);
	    streamData.append(buf, 0, got);
	} while (got == 1024);
	
	buf = null;

	try {
	    // Interpret the file via maple
	    maple.evaluate(streamData.toString());
	} catch(MapleException e) {
	    // At this state we haven't changed anything, just return.
	    throw new PicFormatException("Maple could not interpret .pic file: "+e);
	}
	
	String f, x, y;
	Point2D base, sheetsBase;

	// Description was added recently. Be tolerant of older files.
	try {
	    description = ((MString)maple.evaluate("descr:")).stringValue();
	} catch(Exception e) {
	    description = "";
	}

	try {
	    // Recover the surface data
	    f = maple.evaluate("f:").toString();
	    x = maple.evaluate("x:").toString();
	    y = maple.evaluate("y:").toString();
	    base = maple.algToPoint(maple.evaluate("mono_base:"));
	    sheetsBase = maple.algToPoint(maple.evaluate("sheets_base:"));
	} catch(MapleException e) {
	    throw new PicFormatException("Incomplete .pic file: should define: f, x, y");
	}

	surface.setSurface(f, x, y, base, sheetsBase);

	try {
	    // And the path data
	    paths.clear();
	    List names = (List) maple.evaluate("names:");
	    List sheets = (List) maple.evaluate("sheets:");
	    for (int i = 1; i <= names.length(); ++i) {
		String name = ((MString) names.select(i)).stringValue();
		List points = (List) maple.evaluate(name + ":");
		RiemannPath cur = new RiemannPath();

		cur.setInitialYValue(maple.algToPoint(sheets.select(i)));
		for (int j = 1; j <= points.length(); ++j) {
		    cur.add(maple.algToPoint(points.select(j)));
		}
		// Clear maple namespace while we go
		maple.evaluate(name + ":='" + name + "':");

		// Add to path list
		paths.put(name, cur);
	    }

	    // Try to make the maple state sensible again.
	    maple.evaluate("f:='f':x:='x':y:='y':homology:='homology':descr:='descr':");
	} catch (MapleException e) {
	    throw new PicFormatException("Incomplete .pic file: path data error"+e);
	}
	fireResetEvent();
	firePathSetEvent();
    }

    // Listener crap.

    public void addResetListener(ResetListener l) {
	listeners.add(ResetListener.class, l);
    }

    public void removeResetListener(ResetListener l) {
	listeners.remove(ResetListener.class, l);
    }

    protected void fireResetEvent() {
	Object[] ls = listeners.getListenerList();

	for (int i = ls.length - 2; i >= 0; i -= 2) {
	    if (ls[i] == ResetListener.class) {
		((ResetListener) ls[i + 1]).dataReset(this);
	    }
	}
    }

    public void addPathSetListener(PathSetListener l) {
	listeners.add(PathSetListener.class, l);
    }

    public void removePathSetListener(PathSetListener l) {
	listeners.remove(PathSetListener.class, l);
    }

    protected void firePathSetEvent() {
	Object[] ls = listeners.getListenerList();
	
	for(int i = ls.length - 2; i >= 2; i -= 2) {
	    if(ls[i] == PathSetListener.class) {
		((PathSetListener) ls[i+1]).pathSetChanged(this);
	    }
	}
    }

    EventListenerList listeners;

    public String description;
    RiemannSurface surface;
    CutScheme cutScheme;
    MapleUtils maple;
    SortedMap<String, RiemannPath> paths;
}