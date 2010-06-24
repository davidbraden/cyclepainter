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
package cyclepainter.mapleutil;


import cyclepainter.exceptions.*;

import java.util.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.regex.*;

import java.text.ParseException;

import com.maplesoft.openmaple.*;
import com.maplesoft.openmaple.List;
import com.maplesoft.externalcall.*;

import com.maplesoft.openmaple.Engine;

public class MapleUtils {
    private static MapleUtils instance = null;
    // accepted name is (possibly with `s) (BASIC_NAME/)*BASIC_NAME
    static final Pattern BEGIN_PROC = Pattern.compile("^(`?([a-zA-Z_]\\w*/)*[a-zA-Z_]\\w*`?)\\s*:=\\s*proc\\(");
    static final Pattern END_PROC = Pattern.compile("end\\s+proc\\s*[;:]");
	
    public static MapleUtils connect() throws MapleInitException {
	if(instance == null)
	    instance = new MapleUtils();

	return instance;
    }
	
    private MapleUtils() throws MapleInitException{
	readFunctions();

	createEngine();
		
	initializeEngine();
    }
	
    private void createEngine() throws MapleInitException {
	try {
	    String args[] = new String[1];
	    args[0] = "java";
		
	    engine = new Engine(args, new EngineCallBacksDefault(), null, null);
	} catch(MapleException e) {
	    throw new MapleInitException("Unable to connect to maple engine: "+e);
	}
    }
	
    /** Evaluate function definitions in maple, store a copy of each resulting
     *  procedure just in case 
     */
    private void initializeEngine() throws MapleInitException {
	// Evaluate specific definitions
	functions = new HashMap<String, Procedure>();
	try {
	    for (Map.Entry<String, String> def : functionStrings.entrySet()) {
		functions.put(def.getKey(), (Procedure)engine.evaluate(def.getValue()));				
	    }
				
	} catch(MapleException e) {
	    throw new MapleInitException("Unable to initialize maple engine with definitions: "+e);
	}

    }
	
    /** Read our maple utility functions in (hopefully from in the jar) */
    private void readFunctions()  throws MapleInitException {
	functionStrings = new HashMap<String, String>();
		
	try {
	    InputStream inBasic = getClass().getResourceAsStream("utils.mpl");
	    BufferedReader in = new BufferedReader(new InputStreamReader(inBasic));
	    String line;
			
	    StringBuffer curDef = null;
	    String curName = null;
	    while((line = in.readLine()) != null) {
		Matcher m = BEGIN_PROC.matcher(line);
		if(m.lookingAt()) {
		    curName = m.group(1);
		    curDef = new StringBuffer();
		}
				
		if(curDef != null)
		    curDef.append(line+"\n");
				
		m = END_PROC.matcher(line);
		if(m.lookingAt()) {
		    functionStrings.put(curName, curDef.toString());
		    curDef = null;
		}

	    }
	} catch(IOException e) {
	    throw new MapleInitException("Unable to read definitions file in maple init: "+e);
	}
    }
	
    public Algebraic evaluate(String s) throws MapleException {
	return engine.evaluate(s);
    }

    /** Format a Point2D as a string in the complex plane */
    public static String pointToString(Point2D pt) {
	return String.format("%g%c%g*I", pt.getX(), pt.getY() >= 0 ? '+' : ' ',
			     pt.getY());
    }

    /** Parse an expression to a complex number */
    public Point2D stringToPoint(String s) throws ParseException {
	try {
	    Algebraic pt = engine.evaluate("evalf(" + s + "):");
	    return algToPoint(pt);
	} catch (MapleException e) {
	    throw new ParseException("Error parsing point '"+s+"' to complex value", 0);
	}
    }

    // Maple utilities
    /**
     * Converts an algebraic maple result to a Point2D
     * 
     * @param pt
     *            Maple calculation. Had better represent either a real or
     *            complex numeric result.
     * @return a+bi |--> Point2D(a,b)
     */
    public Point2D algToPoint(Algebraic pt) throws MapleException {
	if (pt instanceof ComplexNumeric) {
	    ComplexNumeric z = (ComplexNumeric) pt;
	    return new Point2D.Double(z.realPart().doubleValue(), z
				      .imaginaryPart().doubleValue());
	} else {
	    Numeric z = (Numeric) pt;
	    return new Point2D.Double(z.doubleValue(), 0.0);
	}
    }
    
    /** 
     * Analytically continue a list of y-values along a straight segment
     * to a given destination.
     *
     * @param curve
     *        Riemann surface for the continuation
     * @param sheets
     *        List of sheets at initial point
     * @param from
     *        Where to start
     * @param to
     *        Where to end
     */
    public List propagateSheets(String curve, List sheets, Point2D from, Point2D to) 
	throws SheetPropagationException {
	List newSheets;
	try {
	    String cmd = String.format("lift_point(%s, %s, %s, %s):",
				       curve,
				       pointToString(from),
				       pointToString(to),
				       sheets);
	    limitNextCalc(2.0);
	    newSheets = (List)engine.evaluate(cmd);
	    removeLimit();
	    return newSheets;
			
	} catch(MapleException e) {
	    throw new SheetPropagationException("Unable to propagate sheets from "+from+" to "+to+". Does this line cross branch points?");
	}
    }
	
    /** Put a time-limit on the next calculation. Kludgy voluntary stop, 
     *  but maple has no better way */
    void limitNextCalc(double seconds) {
	try {
	    engine.evaluate("time_limit := kernelopts(cputime)+"+seconds+":");
	} catch(MapleException e) {
	    System.err.println("Could not set time limit for calculation");
	}
    }
	
    void removeLimit() {
	try {
	    engine.evaluate("time_limit := 'time_limit':");
	} catch(MapleException e) {
	    System.err.println("Could not remove time limit for calculation, expect problems");
	}
    }

    /** Find procedure object corresponding to a given function name */
    public Procedure getFunction(String name) {
	return functions.get(name);
    }
	
    HashMap<String, String> functionStrings;
    HashMap<String, Procedure> functions;
	
    // Maple
    Engine engine;
}