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



package cyclepainter;

import cyclepainter.exceptions.*;
import cyclepainter.mathsstate.PicState;
import cyclepainter.ui.MainWindow;
import java.io.*;
/**
 *
 * @author Tim
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws MapleInitException {
	PicState picState;

	if(args.length > 0) {
	    try {
		FileInputStream in = new FileInputStream(args[0]);
		picState = new PicState(in);
	    } catch(FileNotFoundException e) {
		System.err.println("File "+args[0]+" not found. Aborting.");
		return;
	    } catch(IOException e) {
		System.err.println("Error reading file "+args[0]+". Aborting.");
		return;
	    } catch(PicFormatException e) {
		System.err.println("File "+args[0]+" is a malformed .pic file:");
		System.err.println(e);
		return;
	    } catch(SetSurfaceException e) {
		System.err.println("Surface in "+args[0]+" is malformed.");
		System.err.println(e);
		return;
	    }
	}
	else
	    picState = new PicState();
	
	MainWindow p = new MainWindow(picState);
	p.setVisible(true);
    }

}