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
import cyclepainter.exceptions.SheetPropagationException;

import java.awt.geom.Point2D;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import java.util.List;
import java.util.regex.*;
import java.awt.event.*;

import javax.swing.*;

public class SheetChooser extends JPanel implements PathSelectionListener,
        SurfaceChangeListener, PathChangeListener {
    static final Pattern DIGITS = Pattern.compile("^(\\d+)");

    SheetChooser(PicState picState) {
        initialize(picState);
    }

    void initialize(PicState picState) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(new JLabel("Sheet"));

        combo = new JComboBox() {
            @Override
            public Dimension getMinimumSize() {
                return new Dimension(125, 25);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(150, 25);
            }
        };
        add(combo);

        dataDisplay = new SheetsDisplay(picState);
        JButton disp = new JButton("Sheets data");
        disp.addActionListener(new ActionListener() {
            @Override
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

    @Override
    public void visiblePathsChanged(List<RiemannPath> newVisible) {
        // Don't care
    }

    @Override
    public void activePathChanged(RiemannPath oldActive, RiemannPath newActive) {
        if (oldActive != null)
            oldActive.removePathChangeListener(this);

        if (newActive != null) {
            newActive.addPathChangeListener(this);
            pathChanged(newActive);
        }
        sheetDataModel.sheetsChanged();

        path = newActive;
    }

    @Override
    public void surfaceChanged(RiemannSurface surf) {
        sheetDataModel.sheetsChanged();
    }

    @Override
    public void pathChanged(RiemannPath path) {
        if (path == null || path.size() == 0) {
            sheetsX = new Point2D.Double(0, 0);
        } else if (!path.get(0).equals(sheetsX)) {
            sheetsX = path.get(0);
        }
        sheetDataModel.sheetsChanged();
    }

    @Override
    public void sheetChanged(RiemannPath path) {
        int sheet = 0;
        try {
            sheet = picState.getCutScheme().getSheet(sheetsX,
                    path.getInitialYValue());
        } catch (SheetPropagationException e) {
            System.err.println("Could not determine new sheet of path");
        }

        combo.setSelectedIndex(sheet);
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
        @Override
        public int getSize() {
            int size;
            if (sheetsX == null) {
                size = 0;
            } else {
                size = picState.getCutScheme().numSheets();
            }

            if (yValues == null || size != yValues.length)
                yValues = new Point2D[size];

            return size;
        }

        @Override
        public Object getElementAt(int index) {
            Point2D sheet = null;
            try {
                sheet = picState.getCutScheme().getYValue(sheetsX, index);
            } catch (SheetPropagationException e) {
                System.err
                        .println("Error propagating sheets to beginning of path");
                sheet = new Point2D.Double(0, 0);
            }

            // Keep a cache of the analytic values we're storing.
            yValues[index] = sheet;

            String f = "%d: %.3g+%.3gi";
            f = String.format(f, index + 1, sheet.getX(), sheet.getY());
            return f;
        }

        @Override
        public void setSelectedItem(Object anItem) {
            super.setSelectedItem(anItem);

            if (anItem == null) {
                // New path has been made active with no points yet.
                return;
            }

            String sheet = (String) anItem;
            Matcher m = DIGITS.matcher(sheet);

            if (m.lookingAt()) {
                int sheetNum = Integer.parseInt(m.group(1)) - 1;
                path.setInitialYValue(yValues[sheetNum]);
            }
        }

        public void sheetsChanged() {
            fireContentsChanged(this, 0, getSize() - 1);
        }

        Point2D[] yValues;
    }
}