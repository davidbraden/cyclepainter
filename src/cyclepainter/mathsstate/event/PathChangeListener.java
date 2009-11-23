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



package cyclepainter.mathsstate.event;

import java.util.EventListener;

public interface PathChangeListener extends EventListener {
    /** Generic change in path */
    public void pathChanged();
    /** A different sheet has been selected */
    public void sheetChanged(int newSheet);
    /** The initial point has changed, so canonical sheet numbers for the
	path are sketchy */
    public void allSheetsChanged();
}