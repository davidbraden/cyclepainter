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
import cyclepainter.ui.*;
import cyclepainter.mathsstate.*;
import cyclepainter.exceptions.*;

import java.awt.GridBagConstraints;
import java.io.*;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.*;

/**
 * Create all the UI components and perform any last minute linking needed.
 * Also handle any menu operations.
 * @author  tim
 */
public class MainWindow extends javax.swing.JFrame {
    static final long serialVersionUID = 1;
    static final String TITLE = "Riemann surface cycle painter";
    static final FileFilter PIC_FILE =
	new FileNameExtensionFilter("Native PIC", "pic");
    static final FileFilter MP_FILE = 
	new FileNameExtensionFilter("Metapost", "mp");

    /** Creates new form MainWindow */
    public MainWindow(PicState picState) throws MapleInitException {
    	this.picState = picState;

	picDialog = new JFileChooser(System.getProperty("user.dir"));
	picDialog.setFileFilter(PIC_FILE);

	mpDialog = new JFileChooser(System.getProperty("user.dir"));
	mpDialog.setFileFilter(MP_FILE);

        initComponents();
	initMenu();
    }

    /** This method is called from within the constructor to
     * initialise the form.
     */
    private void initComponents() throws MapleInitException {
        GridBagConstraints gbc;
        
        setTitle(TITLE);

	descrDisplay = new DescriptionDisplay(picState);
        surfaceChooser = new SurfaceChooser(picState.getSurface());
        cycleCanvas = new CycleCanvas(picState);
        pathChooser = new PathChooser(picState);
	sheetChooser = new SheetChooser(picState);

	regionChooser = new RegionChooser();
	    

	pathChooser.addPathSelectionListener(cycleCanvas);
	pathChooser.addPathSelectionListener(sheetChooser);
	regionChooser.addRegionListener(cycleCanvas);
	cycleCanvas.addSelectedPointListener(sheetChooser.getSheetsDisplay());

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new java.awt.GridBagLayout());
        
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        getContentPane().add(pathChooser, gbc);
        
        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        getContentPane().add(sheetChooser, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
	getContentPane().add(regionChooser, gbc);

        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        getContentPane().add(surfaceChooser, gbc);

	gbc = new java.awt.GridBagConstraints();
	gbc.gridx = 1;
	gbc.gridy = 4;
	gbc.weightx = 1;
	gbc.fill = GridBagConstraints.HORIZONTAL;
	getContentPane().add(descrDisplay, gbc);

        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
	gbc.weightx = 1;
	gbc.weighty = 1;
        gbc.gridheight = 3;
	gbc.fill = GridBagConstraints.BOTH;
        getContentPane().add(cycleCanvas, gbc);
        
        pack();
    }

    void initMenu() {
	JMenuBar bar = new JMenuBar();
	JMenu file = new JMenu("File");

	Action ac = new AbstractAction("New") {
		public void actionPerformed(ActionEvent e) {

		    if(confirm("New file will overwrite current data. Proceed?")) {
			filename = null;
			setTitle(TITLE);
			picState.resetToDefault();
		    }
		}
	    };
	file.add(ac);

	ac = new AbstractAction("Open") {
		public void actionPerformed(ActionEvent e) {
		    if(picDialog.showOpenDialog(null) != JFileChooser.APPROVE_OPTION
		       || ! confirm("Opening will discard unsaved changes. Proceed?"))
			return;
		    try {
			filename = picDialog.getSelectedFile();
			FileInputStream in = new FileInputStream(filename);

			picState.readData(in);
			setTitle(TITLE+" - "+filename.getName());
			
		    } catch(FileNotFoundException ex) {
			errorMsg("File not found on open: "+ex,
				 "Error Opening File");
		    } catch(IOException ex) {
			errorMsg("I/O error reading file "+filename+"\n"+ex,
				 "Error Reading File");
		    } catch(PicFormatException ex) {
			errorMsg("Incorrect .pic file format"+filename+".\n"
				 +ex,
				 "Error in File Format");
		    } catch(SetSurfaceException ex) {
			errorMsg("Surface described in "+filename+" not usable\n"
				 +ex,
				 "Error Applying Surface");
		    }
		}
	    };
	file.add(ac);
	
	ac = new AbstractAction("Save As") {
		public void actionPerformed(ActionEvent e) {
		    if(picDialog.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
			return;

		    try {
			filename = picDialog.getSelectedFile();
			if(filename.exists() &&
			   !confirm("Overwrite selected file?"))
			    return;

			FileOutputStream out = new FileOutputStream(filename);

			setTitle(TITLE+" - "+filename.getName());
			picState.writeData(out);
		    } catch(FileNotFoundException ex) {
			System.err.println("Cannot open file for writing: "+ex);
		    }  		
		}
	    };
	file.add(ac);

	ac = new AbstractAction("Write Metapost") {
		public void actionPerformed(ActionEvent e) {
		    if(mpDialog.showSaveDialog((java.awt.Component)e.getSource()) != JFileChooser.APPROVE_OPTION)
			return;

		    try {
			FileOutputStream out = new FileOutputStream(mpDialog.getSelectedFile());
			picState.writeMetapost(out);
		    } catch(FileNotFoundException ex) {
			System.err.println("Cannot open file for writing: "+ex);
		    }
		}
	    };
	file.add(ac);

	file.add(new JSeparator());

	bar.add(file);
	
	setJMenuBar(bar);
    }

    boolean confirm(String message) {
	int resp = JOptionPane.showConfirmDialog(null, message, "Confirm",
						 JOptionPane.YES_NO_OPTION);	
	if(resp == JOptionPane.YES_OPTION)
	    return true;
	return false;
    }
    
    void errorMsg(String message, String title) {
	JOptionPane.showMessageDialog(this, message, title,
				      JOptionPane.ERROR_MESSAGE);
    }

    // GUI components
    private CycleCanvas cycleCanvas;
    private SheetChooser sheetChooser;
    private DescriptionDisplay descrDisplay;
    private SurfaceChooser surfaceChooser;
    private PathChooser pathChooser;
    private RegionChooser regionChooser;
    private JFileChooser picDialog;
    private JFileChooser mpDialog;
    private File filename;
    
    // Member components
    private PicState picState;

}