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

import cyclepainter.mapleutil.*;
import cyclepainter.mathsstate.event.*;
import cyclepainter.mathsstate.*;
import cyclepainter.exceptions.*;

import javax.swing.*;
import java.awt.event.*;
import java.awt.Color;
import java.awt.geom.Point2D;

import java.text.ParseException;

/**
 * UI stuff to allow a new sheet to be entered.
 * @author  tim
 */
public class SurfaceChooser extends JPanel 
	implements SurfaceChangeListener {
	static final long serialVersionUID = 1;

    /** Creates new form BeanForm */
    public SurfaceChooser(RiemannSurface surface) {
    	this.surface = surface;
    	this.maple = surface.getMaple();
    	
    	surface.addSurfaceChangeListener(this);
        initComponents();
        
        applyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setSurface();
            }
        });
    }
    
    public void setSurface() {
	try {
	    Point2D base = maple.stringToPoint(baseText.getText());
	    Point2D sheetBase = maple.stringToPoint(sheetsText.getText());

	    surface.setSurface(curveText.getText(), xText.getText(), yText.getText(), base, sheetBase);

	    curveText.setBackground(Color.WHITE);
	    baseText.setBackground(Color.WHITE);
	    sheetsText.setBackground(Color.WHITE);
	} catch(ParseException e) {
	    System.err.println("Format of base points is a+b*I. Couldn't parse.");
	    baseText.setBackground(Color.RED);
	    sheetsText.setBackground(Color.RED);
	    curveText.setBackground(Color.WHITE);
	    return;
	} catch(SetSurfaceException e) {
	    System.err.println(e);
	    curveText.setBackground(Color.RED);
	    baseText.setBackground(Color.WHITE);
	    sheetsText.setBackground(Color.WHITE);  
	}	    
    }

    /** This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {

    	leadLabel = new javax.swing.JLabel();
    	xText = new javax.swing.JTextField();
        midLabel = new javax.swing.JLabel();
        yText = new javax.swing.JTextField();
        finalLabel = new javax.swing.JLabel();
        curveText = new javax.swing.JTextField();
        applyButton = new javax.swing.JButton();
        
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Box row = Box.createHorizontalBox();
        add(row);
        //setLayout(new javax.swing.BoxLayout(this, BoxLayout.X_AXIS));

        leadLabel.setText("0=f(");
        row.add(leadLabel);

        xText.setText(surface.getX());
        xText.setMinimumSize(new java.awt.Dimension(30, 19));
        xText.setPreferredSize(new java.awt.Dimension(30, 19));
        row.add(xText);

        midLabel.setText(", ");
        row.add(midLabel);

        yText.setText(surface.getY());
        yText.setMinimumSize(new java.awt.Dimension(30, 19));
        yText.setPreferredSize(new java.awt.Dimension(30, 19));
        row.add(yText);

        finalLabel.setText(") = ");
        row.add(finalLabel);

        curveText.setText(surface.getF());
        curveText.setMinimumSize(new java.awt.Dimension(200, 19));
        curveText.setPreferredSize(new java.awt.Dimension(200, 19));
        row.add(curveText);

        // And the second row...
        row = Box.createHorizontalBox();
        add(row);
        
        baseLabel = new JLabel("Base point");
        row.add(baseLabel);
        
        baseText = new JTextField();
	baseText.setMinimumSize(new java.awt.Dimension(100,19));
        baseText.setText("0+0*I");
        row.add(baseText);
        
        sheetsLabel = new JLabel("Sheets base");
        row.add(sheetsLabel);
        
        sheetsText = new JTextField();
	sheetsText.setMinimumSize(new java.awt.Dimension(100,19));
        sheetsText.setText("1+1*I");
        row.add(sheetsText);
        
        applyButton.setText("Apply Surface");
        row.add(applyButton);
    }
    
    public void surfaceChanged(RiemannSurface surf) {
    	curveText.setText(surf.getF());
    	xText.setText(surf.getX());
    	yText.setText(surf.getY());
    	baseText.setText(maple.pointToString(surf.getBasePoint()));
	sheetsText.setText(maple.pointToString(surf.getSheetsBase()));	

	curveText.setBackground(Color.WHITE);
	baseText.setBackground(Color.WHITE);
	sheetsText.setBackground(Color.WHITE);
    }


    // UI components
    private JButton applyButton;
    private JTextField curveText;
    private JLabel finalLabel;
    private JLabel leadLabel;
    private JLabel midLabel;
    private JTextField xText;
    private JTextField yText;
    
    // Second row
    private JLabel baseLabel;
    private JTextField baseText;
    private JLabel sheetsLabel;
    private JTextField sheetsText;

    // General member variables
    private RiemannSurface surface;
    private MapleUtils maple;
}