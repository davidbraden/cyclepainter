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


import cyclepainter.mathsstate.event.*;
import cyclepainter.mathsstate.*;

import javax.swing.*;
import javax.swing.event.*;



public class DescriptionDisplay extends JPanel 
    implements ResetListener {
    public DescriptionDisplay(PicState picState) {
	this.picState = picState;
	
	initComponents();
	this.picState.addResetListener(this);
    }
   
    private void initComponents() {
	descrLabel = new JLabel("Description: ");
	descrText = new JTextField(picState.description);
	descrText.getDocument().
	    addDocumentListener(new DocumentListener() {
		    public void changedUpdate(DocumentEvent e) {
			picState.description = descrText.getText();
		    }
		    public void removeUpdate(DocumentEvent e) {
			picState.description = descrText.getText();
		    }
		    public void insertUpdate(DocumentEvent e) {
			picState.description = descrText.getText();
		    }
		});
	
	setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
	add(descrLabel);
	add(descrText);

    }

    public void dataReset(PicState surf) {
	descrText.setText(surf.description);
    }

    PicState picState;
    JLabel descrLabel;
    JTextField descrText;
}