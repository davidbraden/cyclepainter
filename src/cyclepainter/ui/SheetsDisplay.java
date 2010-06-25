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

import cyclepainter.exceptions.*;
import cyclepainter.mapleutil.*;
import cyclepainter.mathsstate.*;
import cyclepainter.mathsstate.event.*;
import cyclepainter.ui.event.*;
import cyclepainter.Colours;

import javax.swing.*;
import java.awt.Container;
import java.awt.geom.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.ArrayList;

public class SheetsDisplay extends javax.swing.JFrame implements
        SurfaceChangeListener, SelectedPointListener {
    Icon pics[];

    public SheetsDisplay(PicState picState) {
        this.picState = picState;
        selected = null;
        maple = picState.getSurface().getMaple();

        picState.getSurface().addSurfaceChangeListener(this);

        initComponents();
        displayData();
    }

    private void initComponents() {
        // We're probably a mini-window
        setTitle("Sheets minutiae");

        Container contents = getContentPane();
        contents.setLayout(new BoxLayout(contents, BoxLayout.PAGE_AXIS));

        close = new JButton("Close");
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        // Create standard set of icons to reuse later. Blank with appropriate
        // colour.
        pics = new Icon[Colours.SHEET_COLOURS.length];
        int index = 0;
        for (Color c : Colours.SHEET_COLOURS) {
            BufferedImage im = new BufferedImage(16, 16,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = im.createGraphics();
            g.setBackground(c);
            g.clearRect(0, 0, 16, 16);
            Icon ic = new ImageIcon(im);
            pics[index] = ic;
            ++index;
        }
    }

    /**
     * This creates the key labels. One per sheet for both basepoint and
     * selected
     */
    private void displayData() {
        Container contents = getContentPane();
        contents.removeAll();

        Point2D base = picState.getSurface().getSheetsBase();
        java.util.List<Point2D> sheets;
        try {
            sheets = picState.getCutScheme().getAllSheets(base);
        } catch (SheetPropagationException e) {
            System.err
                    .println("Unable find sheets even at surface's own base point");
            System.err.println("This shouldn't happen");
            return;
        }

        contents.add(new JLabel("Sheets based at: "
                + MapleUtils.pointToString(base)));

        int i = 0;
        for (Point2D yval : sheets) {
            String text = String.format("%d: %s", i,
                    MapleUtils.pointToString(yval));

            JTextField lbl = new JTextField(text);
            lbl.setEditable(false);
            lbl.setBorder(null);
            lbl.setForeground(UIManager.getColor("Label.foreground"));
            lbl.setFont(UIManager.getFont("Label.font"));

            Box b = new Box(BoxLayout.X_AXIS);
            b.add(new JLabel(pics[i % pics.length]));
            b.add(lbl);

            contents.add(b);
            ++i;
        }

        selLabel = new JLabel("Selected point");
        contents.add(selLabel);

        selSheets = new ArrayList<JTextField>(sheets.size());
        for (i = 0; i < sheets.size(); ++i) {
            JLabel cur = new JLabel(pics[i % pics.length]);

            JTextField lbl = new JTextField();
            lbl.setEditable(false);
            lbl.setBorder(null);
            lbl.setForeground(UIManager.getColor("Label.foreground"));
            lbl.setFont(UIManager.getFont("Label.font"));
            selSheets.add(lbl);

            Box b = new Box(BoxLayout.X_AXIS);
            b.add(cur);
            b.add(lbl);

            contents.add(b);
        }

        contents.add(close);
        pack();
    }

    @Override
    public void surfaceChanged(RiemannSurface surf) {
        // Really does need to recreate labels. Number of sheets may change.
        displayData();
    }

    @Override
    public void selectedPointChanged(Point2D newPt, boolean rapid) {
        if (newPt == null) {
            selLabel.setText("Selected Point: none");
            for (JTextField cur : selSheets)
                cur.setText("");
            return;
        }
        // Don't bother with quick updates because we're fairly expensive.
        if (!rapid) {
            selLabel.setText("Selected point: "
                    + MapleUtils.pointToString(newPt));

            try {
                java.util.List<Point2D> sheets = picState.getCutScheme()
                        .getAllSheets(newPt);

                int i = 0;
                for (Point2D sheet : sheets) {
                    JTextField cur = selSheets.get(i);
                    cur.setText(String.format("%d: %s", i,
                            MapleUtils.pointToString(sheet)));

                    ++i;
                }
            } catch (SheetPropagationException e) {
                System.err
                        .println("Unable to calculate sheets at selected point");
                System.err.println(e);
            }
        }
    }

    PicState picState;
    JButton close;
    Point2D selected;
    ArrayList<JTextField> selSheets;
    JLabel selLabel;
    MapleUtils maple;
}