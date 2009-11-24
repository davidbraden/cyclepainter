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

    public void visiblePathsChanged(List<String> newVisible) {
	// Don't care
    }

    public void activePathChanged(String newActive) {
	if(path != null)
	    path.removePathChangeListener(this);
	
	path = picState.getPath(newActive);
	if(path != null) {
	    path.addPathChangeListener(this);
	    sheetChanged(path.getInitialSheet());
	}
	sheetDataModel.sheetsChanged();
    }
	
    public void surfaceChanged(RiemannSurface surf) {
	sheetDataModel.sheetsChanged();
    }
	
    public void pathChanged() {
	// Don't care
    }
	
    public void sheetChanged(int newSheet) {
	combo.setSelectedIndex(newSheet-1);
    }
	
    public void allSheetsChanged() {
	sheetDataModel.sheetsChanged();
    }

    JComboBox combo;
	
    RiemannPath path;
    PicState picState;
    MapleUtils maple;
	
    SheetDataModel sheetDataModel;
    SheetsDisplay dataDisplay;
	
    class SheetDataModel extends DefaultComboBoxModel {
	public int getSize() {
	    if(path == null)
		return 0;
	    try {
		return path.getSheets().length();
	    } catch(MapleException e) {
		System.err.println("Couldn't get sheet list for combo display.");
		return 0;
	    }
	}
	public Object getElementAt(int index) {
	    Point2D sheet = path.getSheetByNum(index+1);
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

	    if(m.lookingAt())
		path.setInitialSheet(Integer.parseInt(m.group(1)));
	}
		
	public void sheetsChanged() {
	    fireContentsChanged(this, 0, getSize()-1);
	}
		
    }
}