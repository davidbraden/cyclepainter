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



/**
 * 
 * @author Tim
 */
public class Main {

    private static MapleUtils maple = MapleUtils.connect();

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) throws Exception {

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    maple.evaluate("1+1;").toString();
                    maple.evaluate("min(2,5);");
                } catch (Exception e) {
                }
            }
        });
        t.start();
    }
}