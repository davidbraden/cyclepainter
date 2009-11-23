/* Copyright 2009 Tim Northover
   
   This file is part of CyclePainter.
   
   CyclePainter is free software: you can redistribute it and/or
   modify it under the terms of the GNU General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.
   
   Foobar is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with Foobar.  If not, see <http://www.gnu.org/licenses/>.  
*/



package cyclepainter.ui;

import cyclepainter.ui.event.*;
import cyclepainter.mapleutil.*;
import cyclepainter.mathsstate.*;
import cyclepainter.mathsstate.event.*;
import cyclepainter.Colours;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;

/**
 *
 * @author  tim
 */
public class CycleCanvas extends JComponent
    implements ResetListener,
               SurfaceChangeListener,
	       PathSelectionListener, 
	       PathChangeListener,
	       RegionListener,
	       MouseInputListener, 
	       KeyListener {
    static final long serialVersionUID = 1L;
    static final int SNAP_DIST = 10;
    static final Stroke ACTIVE_STROKE = new BasicStroke(2.0f);
    static final Stroke VISIBLE_STROKE = 
	new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 
			1.0f, new float[]{6.0f, 6.0f}, 0.0f);

    /** Creates new form CycleCanvas */
    public CycleCanvas(PicState picState) {
	this.picState = picState;
	this.visiblePaths = new RiemannPath[0];
	this.drawLoc = null;
	this.selectedPoint = null;
	this.listeners = new EventListenerList();

	// We listen to both changes to the underlying surface and the imortant paths
	picState.addResetListener(this);
	picState.getSurface().addSurfaceChangeListener(this);

	// On the GUI side, we want to know if the mouse so much as squeeks.
	addMouseListener(this);
	addKeyListener(this);
	addMouseMotionListener(this);
	initComponents();
    }

    public void dataReset(PicState picState) {
	repaint();
    }

    public void surfaceChanged(RiemannSurface surface) {
	repaint();
    }

    /** draws a black dot at the given (screen) coordinates. Used for nodes on paths
     * 
     * @param pt
     * @param g
     */
    void drawDot(Point2D pt, Graphics2D g) {
	Arc2D.Double dot = new Arc2D.Double();
	Paint old = g.getPaint();

	g.setPaint(Color.BLACK);
	dot.setArcByCenter(pt.getX(), pt.getY(), 2, 0, 360, Arc2D.OPEN);
	g.fill(dot);
	g.setPaint(old);
    }

    /** The guts of the display. 
     * 
     */
    protected void paintComponent(Graphics basicG) {
	Graphics2D g = (Graphics2D)basicG;

	// Fill the background
	g.setPaint(Color.WHITE);
	g.fillRect(0, 0, getWidth(), getHeight());
	g.setPaint(Color.BLACK);

	paintCuts(g);

	paintPaths(g);

	paintActivity(g);
    }

    /** Paint the cuts from the surface's base point to each branch */
    private void paintCuts(Graphics2D g) {
	// Draw monodromy base point
	Point2D basePoint = (Point2D)picState.getSurface().getBasePoint().clone();
	if(basePoint != null) {
	    mathsToJava.transform(basePoint, basePoint);
	    drawDot(basePoint, g);
	}
	
	// And draw the branch points, with cuts.
	Point2D ours = new Point2D.Double();
	Arc2D dot = new Arc2D.Double();
	for(Point2D pt : picState.getSurface().getFiniteBranches()) {
	    mathsToJava.transform(pt, ours);
	
	    if(basePoint != null) {
		g.setPaint(Colours.CUT_COLOUR);
		g.draw(new Line2D.Double(basePoint, ours));
	    }
	
	    g.setPaint(Color.RED);
	    dot.setArcByCenter(ours.getX(), ours.getY(), 5, 0, 360, Arc2D.OPEN);
	    g.fill(dot);
	}
	
	// Blasted infinity. Cut goes off to left anyway -- fact derived
	// from how maple treats infinity. Difficult to change
	if(picState.getSurface().hasInfiniteBranch()) {
	    Point2D end = new Point2D.Double(0, basePoint.getY());
	    g.setPaint(Colours.CUT_COLOUR);
	    g.draw(new Line2D.Double(basePoint, end));
	}
    }

    /** Draw any visible paths, including the active one */
    private void paintPaths(Graphics2D g) {
	g.setPaint(Color.BLACK); 
	for(RiemannPath path : visiblePaths) {
	    // Set the path colour properly
	    if(path == null || path.isEmpty())
		continue;
	    if(path == activePath)
		g.setStroke(ACTIVE_STROKE);
	    else
		g.setStroke(VISIBLE_STROKE);

	    // Now draw the lines...
	    Point2D begin = new Point2D.Double();
	    Point2D end = new Point2D.Double();
	    for(RiemannPath.PathSeg seg: path.getSegments()) {
		mathsToJava.transform(seg.begin, begin);
		mathsToJava.transform(seg.end, end);
		g.setColor(Colours.SHEET_COLOURS[seg.sheet % Colours.SHEET_COLOURS.length]);

		// Draw the arrowhead
		drawArrow(g, end, new Point2D.Double(end.getX()-begin.getX(),
						     end.getY()-begin.getY()));
		// ...and the line
		g.draw(new Line2D.Double(begin, end));
	    }
	
	    // And the nodes.
	    Point2D pos = new Point2D.Double();
	    for(Point2D node : path) {
		mathsToJava.transform(node, pos);
		drawDot(pos, g);
	    }
	
	}
    }

    /** Transitory things: square for selected point, line being placed. */
    private void paintActivity(Graphics2D g) {
	// Rectangle around selected point
	if(selectedPoint != null) {
	    Point2D pos = new Point2D.Double();

	    mathsToJava.transform(selectedPoint, pos);
	    g.setColor(Color.RED);
	    g.drawRect((int)pos.getX()-5, (int)pos.getY()-5,
		       10, 10);
	}

	// And the drawing line if necessary
	g.setStroke(new BasicStroke());
	if ( drawLoc != null && activePath != null && activePath.size() > 0) {
	    Point2D end = new Point2D.Double();
	    mathsToJava.transform(activePath.get(activePath.size()-1), end);
	    g.draw(new Line2D.Double(end, drawLoc));
	}
    }

    /** Just draw an arrowhead in a given direction. Why doesn't java
        include this? */
    private void drawArrow(Graphics2D g, Point2D head, Point2D dir) {
	double len=5;
	double size = dir.distance(0, 0);
	double cT = dir.getX()/size;
	double sT = -dir.getY()/size;

	Point2D init = new Point2D.Double(head.getX() - len*(sT+cT), 
					  head.getY() + len*(sT-cT));
	g.draw(new Line2D.Double(init, head));

	init = new Point2D.Double(head.getX() + len*(sT-cT), 
				  head.getY() + len*(cT+sT));
	g.draw(new Line2D.Double(init, head));
    }    
    /** This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {

	setBackground(new java.awt.Color(255, 255, 255));

	mathsToJava = new AffineTransform();
	ll = new Point2D.Double(-2.0, -2.0);
	ur = new Point2D.Double(2.0, 2.0);
	addComponentListener(new ComponentAdapter() {
		public void componentResized(ComponentEvent e) {
		    setViewport();
		}
	    });
    }

    public Dimension getMinimumSize() {
	return new Dimension(100, 100);
    }

    public Dimension getPreferredSize() {
	return new Dimension(500, 500);
    }
    
    public Dimension getMaximumSize() {
	return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    // Listen for path changed events
    public void visiblePathsChanged(java.util.List<String> newVisible) {
	visiblePaths = new RiemannPath[newVisible.size()];
	int i = 0;
	for(String name : newVisible) {
	    visiblePaths[i++] = picState.getPath(name);
	}

	selectedPoint = null;

	fireSelectedPointChanged(selectedPoint, false);
	repaint();
    }

    public void activePathChanged(String newActive) {
	// We want to know about any updates to the active path.
	if(activePath != null)
	    activePath.removePathChangeListener(this);

	activePath = picState.getPath(newActive);
	if(activePath != null)
	    activePath.addPathChangeListener(this);

	repaint();
    }

    // Don't care.
    public void selectedPointChanged(Point2D newPt) {
    }
	
    public void sheetChanged(int newSheet) {
	repaint();
    }
	
    public void allSheetsChanged() {
	// Doesn't affect rendering
    }
	
    public void pathChanged() {
	repaint();
    }

    /** Setup the coordinate transformations so that the bits of the complex plane we want
     *  are visible.
     */
    void setViewport() {
	mathsToJava.setToIdentity();
	mathsToJava.scale(this.getWidth()/(ur.getX()-ll.getX()), -this.getHeight()/(ur.getY()-ll.getY()));
	mathsToJava.translate(-ll.getX(), -ur.getY());

	// And set the inverse if possible
	try {
	    javaToMaths = mathsToJava.createInverse();
	} catch(NoninvertibleTransformException e) {
	    System.err.println("Bad viewport specification. Ignoring");
	    mathsToJava.setToIdentity();
	    javaToMaths = new AffineTransform();
	}
	repaint();
    }

    // And the mouse events... Where the input goes on
    /** Right click cancels drawing but does nothing else. Otherwise we start/continue drawing
     *  as necessary.
     */
    public void mouseClicked(MouseEvent e) {
	requestFocusInWindow();

	if(e.getButton() != MouseEvent.BUTTON1) {
	    // Right button cancels drawing
	    drawLoc = null;
	    // Get rid of existing segment
	    repaint();
	    return;
	}
	if(drawLoc == null) {
	    // If we're not drawing, we either need to start or select
	    // a point.
	    selectedPoint = snapIfSelecting(e.getPoint());
	    if(selectedPoint != null) {
		fireSelectedPointChanged(selectedPoint, false);
		repaint();
		return;
	    }

	    // See if we need to start a new path
	    if(activePath != null && activePath.size() == 0) {
		Point2D maths = new Point2D.Double();
		javaToMaths.transform(e.getPoint(), maths);
		activePath.add(maths);
	    }

	    // Start drawing, but don't record a point
	    drawLoc = e.getPoint();
	    repaint();
	    return;
	}

	// Otherwise, add point and continue
	if(activePath != null) {
	    Point2D maths = new Point2D.Double();
	    javaToMaths.transform(drawLoc, maths);

	    activePath.add(maths);
	    repaint();
	}
    }

    // Don't care about this. mouseDragged handles the only reason we'd care
    public void mousePressed(MouseEvent e) {
    }

    /** Stops moving a node if one is. Just a nop if not.
     * 
     */
    public void mouseReleased(MouseEvent e) {
	requestFocusInWindow();
	if(dragging) {
	    fireSelectedPointChanged(selectedPoint, false);
	    dragging = false;
	}
    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {

    }

    /** If we're already dragging a node carry on. If not, start dragging if appropriate
     * 
     */
    public void mouseDragged(MouseEvent e) {
	requestFocusInWindow();
	// Don't do anything if we're drawing or no active path
	if(activePath == null || activePath.size() == 0 || drawLoc != null)
	    return;
	if(dragging) {
	    Point2D maths, tmp;
	    maths = snapToActive(e.getPoint(), selectedPoint);

	    tmp = new Point2D.Double();
	    javaToMaths.transform(maths, tmp);
	    activePath.displace(selectedPoint, tmp);
	    fireSelectedPointChanged(tmp, true);
	    repaint();
	    return;
	}

	selectedPoint = snapIfSelecting(e.getPoint());
	if(selectedPoint != null) {
	    fireSelectedPointChanged(selectedPoint, false);
	    dragging = true;
	    mouseDragged(e);
	}    		
    }

    /** Only relevant if not dragging, otherwise mouseDragged called. So check if 
     * we're drawing or do nothing.
     */
    public void mouseMoved(MouseEvent e) {
	if(drawLoc != null) {
	    drawLoc = snapToActive(e.getPoint());
	    repaint();
	}
    }

    public void keyPressed(KeyEvent e) {
	if(selectedPoint == null || dragging)
	    return;

	if(e.getKeyCode() == KeyEvent.VK_DELETE) {
	    activePath.remove(selectedPoint);
	    selectedPoint = null;
	    repaint();
	}
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {

    }

    /** Some ui has been used to change viewable area */
    public void regionChanged(Point2D ll, Point2D ur) {
	this.ll = ll;
	this.ur = ur;
	setViewport();
    }

    /** Snap a given point to the closest node on the active path if 
     *  appropriate. Depends on viewport
     * @param pt java coordinates, the actual mouse event point.
     * @param excluding. A point in the activePath (so maths coords) to ignore
     * @return A snapped version of pt.
     */
    Point2D snapToActive(Point2D pt, Point2D excluding) {
	if(activePath == null || activePath.size() == 0)
	    return pt;

	Point2D tmp = new Point2D.Double();
	javaToMaths.transform(pt, tmp);
	Point2D closest = activePath.closestTo(tmp, excluding);
	mathsToJava.transform(closest, tmp);

	if(tmp.distance(pt) < SNAP_DIST)
	    return tmp;
	else
	    return pt;
    }

    Point2D snapToActive(Point2D pt) {
	return snapToActive(pt, null);
    }

    /** Similar to snapToActive, but returns null if we're outside snap
	distance so caller easily knows not to bother snapping */
    Point2D snapIfSelecting(Point2D pt) {
	Point2D posTmp = new Point2D.Double();
	javaToMaths.transform(pt, posTmp);
	Point2D closest = activePath.closestTo(posTmp);

	if(closest == null)
	    return null;

	mathsToJava.transform(closest, posTmp);

	if(posTmp.distance(pt) < SNAP_DIST)
	    return closest;

	return null;
    }

    private void fireSelectedPointChanged(Point2D pt, boolean rapid) {
	Object[] ls = listeners.getListenerList();

	for(int i = ls.length - 2; i >= 0; i -= 2) {
	    if(ls[i] == SelectedPointListener.class) {
		((SelectedPointListener) ls[i+1]).selectedPointChanged(pt, rapid);
	    }
	}
    }

    public void addSelectedPointListener(SelectedPointListener l) {
	listeners.add(SelectedPointListener.class, l);
    }

    public void removeSelectedPointListener(SelectedPointListener l) {
	listeners.remove(SelectedPointListener.class, l);
    }

    PicState picState;
    AffineTransform mathsToJava, javaToMaths;
    Point2D drawLoc;

    boolean dragging;
    Point2D selectedPoint;

    RiemannPath activePath;
    RiemannPath visiblePaths[];

    Point2D ll, ur;

    EventListenerList listeners;
}