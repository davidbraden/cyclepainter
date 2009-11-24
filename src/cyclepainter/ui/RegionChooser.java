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



package cyclepainter.ui;

import cyclepainter.ui.event.*;
import cyclepainter.mapleutil.*;
import cyclepainter.exceptions.*;

import javax.swing.*;
import java.awt.GridLayout;
import java.awt.event.*;
import java.awt.geom.Point2D;

import java.text.ParseException;

/**
 * UI element to specify visible region of complex plane. Very primitive.
 * @author  tim
 */
public class RegionChooser extends JPanel {
	static final long serialVersionUID = 1;

    public RegionChooser() throws MapleInitException {
	listeners = new java.util.LinkedList<RegionListener>();

	maple = MapleUtils.connect();
	
        initComponents();
    }

    private void initComponents() {
        setLayout(new GridLayout(3, 2));

	llText = new JLabel("L-L coord");
	add(llText);
	ll = new JTextField("-2-2*I");
	add(ll);

	urText = new JLabel("U-R coord");
	add(urText);
	ur = new JTextField("2+2*I");
	add(ur);

	apply = new JButton("Apply");
	apply.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    fireRegionChanged();
		}
	    });
	add(apply);
    }

    public void addRegionListener(RegionListener l) {
	listeners.add(l);
    }

    public void removeRegionListener(RegionListener l) {
	listeners.remove(l);
    }

    public void fireRegionChanged() {
	try {
	    Point2D llPt = maple.stringToPoint(ll.getText());
	    Point2D urPt = maple.stringToPoint(ur.getText());

	    for(RegionListener l : listeners) {
    		l.regionChanged(llPt, urPt);
	    }
	} catch(ParseException e) {
	    System.err.println("Format for region points is a+b*I. Couldn't parse.");
	    System.err.println("Ignoring viewport change.");
	    return;
	}

    }

    JLabel llText;
    JTextField ll;
    JLabel urText;
    JTextField ur;
    JButton apply;	

    MapleUtils maple;

    java.util.List<RegionListener> listeners;
}