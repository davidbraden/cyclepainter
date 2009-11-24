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
package cyclepainter;


import java.awt.Color;

public class Colours {
    public static final Color[] SHEET_COLOURS = {
        new Color(0x00,0x00,0x00), new Color(0x00,0x00,0xaa), 
	new Color(0x00,0xaa,0x00), new Color(0x00,0xaa,0xaa),
	new Color(0xaa,0x00,0x00), new Color(0xaa,0x00,0xaa), 
	new Color(0xaa,0x55,0x00), new Color(0x55,0x55,0xff),
	new Color(0x55,0xff,0x55), new Color(0x55,0xff,0xff), 
	new Color(0xff,0x55,0x55), new Color(0xff,0x44,0xff), 
	new Color(0xff,0xff,0x55)
    };
    public static final Color CUT_COLOUR = Color.GRAY;
}