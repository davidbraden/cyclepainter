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

import java.util.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.*;

import java.text.ParseException;

import com.maplesoft.openmaple.*;
import com.maplesoft.openmaple.List;
import com.maplesoft.externalcall.*;

import com.maplesoft.openmaple.Engine;

public class MapleUtils {
    private static MapleUtils instance = null;

    public static MapleUtils connect() {
        if (instance == null)
            instance = new MapleUtils();

        return instance;
    }

    private MapleUtils() {
        submitAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println(Thread.currentThread().getName());

                    String args[] = new String[1];
                    args[0] = "java";
                    engine = new Engine(args, new EngineCallBacksDefault(), null, null);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        );
    }

    public Algebraic evaluate(final String s) throws MapleException {
        final Algebraic[] result = new Algebraic[1];
        try {
            submitAndWait(new Runnable() {
                @Override
                public void run() {
                    try {
                        System.out.println("Evaluate " + s);
                        result[0] = engine.evaluate(s);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result[0];
    }

    private void submitAndWait(Runnable runnable) {
        try {
            java.util.concurrent.Future<?> future = executor.submit(runnable);
            while (!future.isDone()) {
                Thread.sleep(100);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    // Maple
    Engine engine;
}