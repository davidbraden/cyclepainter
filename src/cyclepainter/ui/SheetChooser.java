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


import cyclepainter.mapleutil.*;
import cyclepainter.ui.event.*;
import cyclepainter.mathsstate.event.*;
import cyclepainter.mathsstate.*;

import java.awt.geom.Point2D;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.*;
import java.awt.event.*;

import com.maplesoft.externalcall.*;

import javax.swing.*;



public class SheetChooser extends JPanel
    implements PathSelectionListener, SurfaceChangeListener, PathChangeListener {
    static final Pattern DIGITS = Pattern.compile("^(\\d+)");
    
    SheetChooser(PicState picState) {
	initialize(picState);
    }
    
    void initialize(PicState picState) {
	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	
	add(new JLabel("Sheet"));

	combo = new JComboBox() {
		public Dimension getMinimumSize() {
		    return new Dimension(125, 25);
		}
		
		public Dimension getPreferredSize() {
		    return new Dimension(150, 25);
		}
	    };
	add(combo);

	dataDisplay = new SheetsDisplay(picState.getSurface());
	JButton disp = new JButton("Sheets data");
	disp.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    dataDisplay.setVisible(true);
		}
	    });
	add(disp);

	this.picState = picState;
	this.picState.getSurface().addSurfaceChangeListener(this);
	
	maple = picState.getMaple();
	
	sheetDataModel = new SheetDataModel();
	combo.setModel(sheetDataModel);
    }

    public SheetsDisplay getSheetsDisplay() {
	return dataDisplay;
    }

    public void visiblePathsChanged(List<RiemannPath> newVisible) {
	// Don't care
    }

    public void activePathChanged(RiemannPath oldActive, RiemannPath newActive) {
	if(oldActive != null)
	    oldActive.removePathChangeListener(this);
	
	if(newActive != null) {
	    newActive.addPathChangeListener(this);
	    pathChanged(newActive);
	}
	sheetDataModel.sheetsChanged();

        path = newActive;
    }
	
    public void surfaceChanged(RiemannSurface surf) {
	sheetDataModel.sheetsChanged();
    }
	
    public void pathChanged(RiemannPath path) {
        if (path.size() == 0) {
            sheetDataModel.sheetsChanged();
            return;
        }

        if (!sheetsX.equals(path.get(0))) {
            sheetsX = path.get(0);
            sheetDataModel.sheetsChanged();
        }
    }
	
    public void sheetChanged(RiemannPath path) {
	combo.setSelectedIndex(picState.getCutScheme().
                               getSheet(sheetsX, path.getInitialYValue()));
    }
	
    public void allSheetsChanged() {        
	sheetDataModel.sheetsChanged();
    }

    JComboBox combo;
	
    RiemannPath path;
    Point2D sheetsX;

    PicState picState;
    MapleUtils maple;
	
    SheetDataModel sheetDataModel;
    SheetsDisplay dataDisplay;
	
    class SheetDataModel extends DefaultComboBoxModel {
	public int getSize() {
            int size;
	    if(sheetsX == null)
		size = 0;
            
            size = picState.getCutScheme().numSheets();

            if (yValues == null || size != yValues.size())
                yValues = new ArrayList<Point2D>(size);

            return size;
	}
	public Object getElementAt(int index) {
	    Point2D sheet = picState.getCutScheme().getYValue(sheetsX, index);

            // Keep a cache of the analytic values we're storing.
            yValues.set(index, sheet);

	    String f = "%d: %.3g+%.3gi";
	    f = String.format(f, index+1, sheet.getX(), sheet.getY());
	    return f;
	}
		
	public void setSelectedItem(Object anItem) {
	    super.setSelectedItem(anItem);
			
	    if(anItem == null) {
		// New path has been made active with no points yet.
		return;
	    }

	    String sheet = (String)anItem;
	    Matcher m = DIGITS.matcher(sheet);

	    if(m.lookingAt()) {
                int sheetNum = Integer.parseInt(m.group(1)) - 1;
                path.setInitialYValue(yValues.get(sheetNum));
            }
	}
		
	public void sheetsChanged() {
	    fireContentsChanged(this, 0, getSize()-1);
	}
		
        ArrayList<Point2D> yValues;
    }
}