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
import cyclepainter.mathsstate.event.*;
import cyclepainter.mathsstate.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

import javax.swing.event.*;

/**
 * UI for editing available paths and selecting active/visible ones
 * @author  tim
 */
public class PathChooser extends JPanel 
    implements ListDataListener, PathSetListener {
	static final long serialVersionUID = 1;

    public PathChooser(PicState picState) {
    	this.picState = picState;
	picState.addPathSetListener(this);
    	
    	// We want to know when the list of valid names changes so we can
    	// decide which buttons are still valid.
    	listeners = new LinkedList<PathSelectionListener>();
    	
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {
    	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    	//setLayout(new GridLayout());
    	
    	addPath = new JButton("Add path");
    	addPath.addActionListener(new ActionListener() {
    		public void actionPerformed(ActionEvent e) {
    			if(newName.getText().length() != 0) {
        			picState.addPath(newName.getText());
    			}

    		}
    	});
    	add(addPath);
    	
    	delPath = new JButton("Delete path");
    	delPath.addActionListener(new ActionListener() {
    		public void actionPerformed(ActionEvent e) {
    			picState.delPath((String)pathList.getSelectedValue());
    		}
    	});
    	add(delPath);
    	
    	clearPath = new JButton("Clear path");
    	add(clearPath);
    	    	
    	newName = new JTextField();
    	add(newName);

    	pathList = new JList();
	pathList.setListData(new Vector<String>(picState.getPathNames()));
    	pathList.setDragEnabled(true);
    	
    	scroller = new JScrollPane(pathList);
	scroller.setMinimumSize(new Dimension(50, 100));
    	add(scroller);
    	
    	activeDescr = new JLabel("Active/Visible paths");
    	add(activeDescr);
    	
    	// Create a handler so the pushbuttons can accept a path properly
    	TransferHandler th = new TransferHandler("text") {
    		public boolean importData(TransferHandler.TransferSupport support) {
    			Transferable t = support.getTransferable();
    			try {
    				String name = (String)t.getTransferData(DataFlavor.stringFlavor);
    				JToggleButton btn = (JToggleButton)support.getComponent();
    				if(!picState.hasPath(name))
    					return false;
    				else {
    					btn.setText(name);
    					btn.setEnabled(name.length() != 0);
    					
    					// Fire necessary events
    					fireVisiblePathsChanged();

    					if(btn.isSelected())
    						fireActivePathChanged();
    				}    					
    			} catch(UnsupportedFlavorException e) {
    				return false;
    			} catch(IOException e) {
    				return false;
    			}
    			
    			return true;
    		}
    	};
    	// And to deal with activating buttons
    	grp = new ButtonGroup();
    	
    	visPaths = new ArrayList<JToggleButton>();
    	ActionListener l = new ActionListener() {
    		public void actionPerformed(ActionEvent e) {
    			fireActivePathChanged();
    		}
    	};
    	for(int i = 0; i < 2; ++i) {
    		JToggleButton vis = new JToggleButton(" ");
    		vis.setTransferHandler(th);
    		vis.setEnabled(false);
    		vis.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
    		vis.setPreferredSize(new Dimension(0, 30));
		vis.setMinimumSize(new Dimension(0, 30));
    		vis.addActionListener(l);
    		add(vis);
    		grp.add(vis);
    		visPaths.add(vis);
    	}
    	visPaths.get(0).setSelected(true);
    }
    
    /** Remove invalid names from buttons describing visible paths */
    private void pruneButtons() {
    	boolean visChanged = false, activeRemoved = false;
    	for(JToggleButton btn : visPaths) {
    		if(!picState.hasPath(btn.getText())) {
    			btn.setText("");
			btn.setEnabled(false);
    			visChanged = true;
    			if(btn.isSelected())
    				activeRemoved = true;
    		}
    	}

	// Active path has been deleted, find another candidate to make
	// active. (hopefully).
    	if(activeRemoved) {
	    for(JToggleButton btn : visPaths) {
		if(btn.isEnabled()) {
		    btn.setSelected(true);
		    break;
		}
	    }
    	}

    	if(visChanged)
    		fireVisiblePathsChanged();
    	if(activeRemoved)
    		fireActivePathChanged();
    	
    }

    // Stuff to be a good model of path selection
    public String getActivePath() {
    	for(JToggleButton btn : visPaths) {
    		if(btn.isSelected() && btn.getText().length() != 0)
    			return btn.getText();
    	}
    	return null;
    }
    
    public List<String> getVisiblePaths() {
    	List<String> names = new LinkedList<String>();
    	for(JToggleButton btn : visPaths) {
    		if(btn.getText().length() != 0)
    			names.add(btn.getText());
    	}
    	return names;
    }

    public void pathSetChanged(PicState picState) {
	pathList.setListData(new Vector<String>(picState.getPathNames()));

	pruneButtons();
    }
    
    public void addPathSelectionListener(PathSelectionListener l) {
    	listeners.add(l);
    }
    
    public void removePathSelectionListener(PathSelectionListener l) {
    	listeners.remove(l);
    }
    
    protected void fireVisiblePathsChanged() {
    	List<String> visiblePaths = getVisiblePaths();
    	for(PathSelectionListener l : listeners) {
    		l.visiblePathsChanged(visiblePaths);
    	}
    }
    
    protected void fireActivePathChanged() {
    	for(PathSelectionListener l : listeners) {
    		l.activePathChanged(getActivePath());
    	}
    }
    
    // Things to do when list of names changes
    // Same in all relevant cases. Don't care if it's a change or removal.
    public void contentsChanged(ListDataEvent e) {
	pruneButtons();
    }

    public void intervalAdded(ListDataEvent e) {
    	// Can't affect whether visible/active are valid
    }
    
    public void intervalRemoved(ListDataEvent e) {
    	contentsChanged(e);
    }
    
    private JTextField newName;
    private JButton addPath;
    private JButton delPath;
    private JButton clearPath;
    private JList pathList;
    private JScrollPane scroller;
    private JLabel activeDescr;
    private java.util.List<JToggleButton> visPaths;
    private ButtonGroup grp;
    
    List<PathSelectionListener> listeners;

    PicState picState;
}