/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2002 Bill Hibbard, Curtis Rueden, Tom
Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
Tommy Jasmin.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
MA 02111-1307, USA
*/

package visad.data.amanda;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.IOException;

import java.net.URL;

import java.rmi.RemoteException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

import visad.CellImpl;
import visad.Data;
import visad.DataReferenceImpl;
import visad.Display;
import visad.DisplayImpl;
import visad.DisplayRenderer;
import visad.FieldImpl;
import visad.GraphicsModeControl;
import visad.Integer1DSet;
import visad.Real;
import visad.RealType;
import visad.ScalarMap;
import visad.ShapeControl;
import visad.Tuple;
import visad.VisADException;

import visad.java3d.DisplayImplJ3D;

import visad.util.LabeledColorWidget;
import visad.util.VisADSlider;

class DisplayFrame
  extends WindowAdapter
{
  private Display display;

  DisplayFrame(String title, Display display, JPanel panel)
    throws VisADException, RemoteException
  {
    this.display = display;

    JFrame frame = new JFrame(title);

    frame.addWindowListener(this);
    frame.getContentPane().add(panel);
    frame.pack();
    panel.invalidate();

    Dimension fSize = frame.getSize();

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    frame.setLocation((screenSize.width - fSize.width)/2,
                      (screenSize.height - fSize.height)/2);

    frame.setVisible(true);
  }

  public void windowClosing(WindowEvent evt)
  {
    try { display.destroy(); } catch (Exception e) { }
    System.exit(0);
  }
}

class DisplayMaps
{
  ScalarMap trackmap, shapemap, letmap;

  DisplayMaps(AmandaFile file, Display display)
    throws RemoteException, VisADException
  {
    final double halfRange = getMaxRange(file) / 2.0;

    ScalarMap xmap = new ScalarMap(RealType.XAxis, Display.XAxis);
    setRange(xmap, file.getXMin(), file.getXMax(), halfRange);
    display.addMap(xmap);

    ScalarMap ymap = new ScalarMap(RealType.YAxis, Display.YAxis);
    setRange(ymap, file.getYMin(), file.getYMax(), halfRange);
    display.addMap(ymap);

    ScalarMap zmap = new ScalarMap(RealType.ZAxis, Display.ZAxis);
    setRange(zmap, file.getZMin(), file.getZMax(), halfRange);
    display.addMap(zmap);

    // ScalarMap eventmap = new ScalarMap(file.getEventIndex(),
    //                                    Display.SelectValue);
    // display.addMap(eventmap);

    this.trackmap = new ScalarMap(BaseTrack.indexType, Display.SelectValue);
    display.addMap(this.trackmap);

    // ScalarMap energymap = new ScalarMap(energy, Display.RGB);
    // display.addMap(energymap);

    this.shapemap = new ScalarMap(Hit.amplitudeType, Display.Shape);
    display.addMap(this.shapemap);

    ScalarMap shapeScalemap = new ScalarMap(Hit.amplitudeType,
                                            Display.ShapeScale);
    display.addMap(shapeScalemap);
    shapeScalemap.setRange(-20.0, 50.0);

    this.letmap = new ScalarMap(Hit.leadingEdgeTimeType, Display.RGB);
    display.addMap(this.letmap);
  }

  private static final double getMaxRange(AmandaFile file)
  {
    final double xRange = file.getXMax() - file.getXMin();
    final double yRange = file.getYMax() - file.getYMin();
    final double zRange = file.getZMax() - file.getZMin();

    return -0.5 * Math.max(xRange, Math.max(yRange, zRange));
  }

  private static final void setRange(ScalarMap map, double min, double max,
                                     double halfRange)
    throws RemoteException, VisADException
  {
    final double mid = (min + max) / 2.0;
    map.setRange(mid - halfRange, mid + halfRange);
  }
}

/** run 'java NuView in_file' to display data.<br>
 *  try 'java NuView 100events.r'
 */
public class NuView
{
  public static void main(String args[])
         throws VisADException, RemoteException, IOException
  {
    if (args == null || args.length != 1) {
      System.out.println("to test read an F2000 file, run:");
      System.out.println("  'java NuView in_file'");
      System.exit(1);
      return;
    }

    AmandaFile file;
    if (args[0].startsWith("http://")) {
      // with "ftp://" this throws "sun.net.ftp.FtpProtocolException: RETR ..."
      file = new AmandaFile(new URL(args[0]));
    } else {
      file = new AmandaFile(args[0]);
    }

    final FieldImpl amanda = file.makeEventData();
    final FieldImpl modules = file.makeModuleData();

    DisplayImpl display = new DisplayImplJ3D("amanda");

    DisplayMaps maps = new DisplayMaps(file, display);

    // GraphicsModeControl mode = display.getGraphicsModeControl();
    // mode.setScaleEnable(true);
    DisplayRenderer displayRenderer = display.getDisplayRenderer();
    displayRenderer.setBoxOn(false);

    ShapeControl scontrol = (ShapeControl )maps.shapemap.getControl();
    scontrol.setShapeSet(new Integer1DSet(Hit.amplitudeType, 1));
    scontrol.setShapes(F2000Util.getCubeArray());

    final int nevents = amanda.getLength();

    // fixes track display?
    // SelectValue bug?
    // amanda = ((FieldImpl )amanda).getSample(99);

    final DataReferenceImpl amandaRef = new DataReferenceImpl("amanda");
    // data set by eventWidget below
    display.addReference(amandaRef);

    final DataReferenceImpl modulesRef = new DataReferenceImpl("modules");
    modulesRef.setData(modules);
    display.addReference(modulesRef);

/*
    LabeledColorWidget energyWidget = new LabeledColorWidget(energymap);
    widgetPanel.add(energyWidget);
*/

    LabeledColorWidget letWidget = new LabeledColorWidget(maps.letmap);
    // align along left side, to match VisADSlider alignment
    //   (if we don't left-align, BoxLayout hoses everything)
    letWidget.setAlignmentX(Component.LEFT_ALIGNMENT);

    EventWidget eventWidget = new EventWidget(file, amanda, amandaRef,
                                              maps.trackmap);

    JPanel widgetPanel = new JPanel();
    widgetPanel.setLayout(new BoxLayout(widgetPanel, BoxLayout.Y_AXIS));
    widgetPanel.setMaximumSize(new Dimension(400, 600));

    widgetPanel.add(letWidget);
    // widgetPanel.add(new VisADSlider(eventmap));
    widgetPanel.add(eventWidget);
    widgetPanel.add(Box.createHorizontalGlue());

    JPanel displayPanel = (JPanel )display.getComponent();
    Dimension dim = new Dimension(800, 800);
    displayPanel.setPreferredSize(dim);
    displayPanel.setMinimumSize(dim);

    // if widgetPanel alignment doesn't match
    //  displayPanel alignment, BoxLayout will freak out
    widgetPanel.setAlignmentX(displayPanel.getAlignmentX());
    widgetPanel.setAlignmentY(displayPanel.getAlignmentY());

    // create JPanel in frame
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

    panel.add(widgetPanel);
    panel.add(displayPanel);

    new DisplayFrame("VisAD AMANDA Viewer", display, panel);
  }
}