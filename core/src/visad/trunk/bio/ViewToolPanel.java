//
// ViewToolPanel.java
//

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

package visad.bio;

import java.awt.event.*;
import java.rmi.RemoteException;
import javax.swing.*;
import javax.swing.event.*;
import visad.*;
import visad.browser.Divider;

/**
 * ViewToolPanel is the tool panel for
 * adjusting viewing parameters.
 */
public class ViewToolPanel extends ToolPanel {

  // -- GUI COMPONENTS --

  /** Toggle for 2-D display mode. */
  private JCheckBox twoD;

  /** Toggle for 3-D display mode. */
  private JCheckBox threeD;

  /** Toggle for lo-res image display. */
  private JToggleButton loRes;

  /** Toggle for hi-res image display. */
  private JToggleButton hiRes;

  /** Toggle for low-resolution thumbnails. */
  private JCheckBox thumbs;
  
  /** Animation widget. */
  private BioAnimWidget anim;

  /** Toggle for grayscale mode. */
  private JCheckBox grayscale;

  /** Label for brightness. */
  private JLabel brightnessLabel;

  /** Slider for level of brightness. */
  private JSlider brightness;


  // -- CONSTRUCTOR --

  /** Constructs a tool panel for adjusting viewing parameters. */
  public ViewToolPanel(BioVisAD biovis) {
    super(biovis);

    // 2-D checkbox
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
    twoD = new JCheckBox("2-D", true);
    twoD.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        bio.set2D(twoD.isSelected());
      }
    });
    p.add(twoD);

    // 3-D checkbox
    boolean okay3d = bio.display3 != null;
    threeD = new JCheckBox("3-D", okay3d);
    threeD.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        bio.set3D(threeD.isSelected());
      }
    });
    p.add(threeD);
    threeD.setEnabled(okay3d);
    controls.add(pad(p));

    // lo-res toggle button
    p = new JPanel();
    p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
    loRes = new JToggleButton("Lo-res", false);
    loRes.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setMode(true);
        bio.sm.setMode(true);
      }
    });
    loRes.setEnabled(false);
    p.add(loRes);

    // hi-res toggle button
    hiRes = new JToggleButton("Hi-res", true);
    hiRes.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setMode(false);

        // don't animate in hi-res mode
        AnimationControl control = anim.getControl();
        try { if (control != null) control.setOn(false); }
        catch (VisADException exc) { exc.printStackTrace(); }
        catch (RemoteException exc) { exc.printStackTrace(); }

        bio.sm.setMode(false);
      }
    });
    p.add(hiRes);
    hiRes.setEnabled(false);
    controls.add(pad(p));

    // thumbnails checkbox
    thumbs = new JCheckBox("Create low-resolution thumbnails", true);
    thumbs.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        bio.sm.setThumbnails(thumbs.isSelected());
      }
    });
    controls.add(pad(thumbs));

    // divider between resolution functions and animation functions
    controls.add(Box.createVerticalStrut(10));
    controls.add(new Divider());
    controls.add(Box.createVerticalStrut(10));

    // animation widget
    p = new JPanel();
    p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
    anim = new BioAnimWidget(bio);
    anim.setEnabled(false);
    p.add(anim);
    controls.add(pad(p));

    // divider between animation functions and color functions
    controls.add(Box.createVerticalStrut(10));
    controls.add(new Divider());
    controls.add(Box.createVerticalStrut(10));

    // grayscale checkbox
    grayscale = new JCheckBox("Grayscale", true);
    grayscale.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) { doColorTable(); }
    });
    controls.add(pad(grayscale));

    // brightness label
    p = new JPanel();
    p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
    brightnessLabel = new JLabel("Brightness: ");
    p.add(brightnessLabel);

    // brightness slider
    brightness = new JSlider(1, 100, BioVisAD.NORMAL_BRIGHTNESS);
    brightness.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) { doColorTable(); }
    });
    p.add(brightness);
    controls.add(p);
  }


  // -- API METHODS --

  /** Enables or disables this tool panel. */
  public void setEnabled(boolean enabled) {
    loRes.setEnabled(enabled);
    hiRes.setEnabled(enabled);
    anim.setEnabled(enabled && bio.sm.hasThumbnails());
    grayscale.setEnabled(enabled);
    brightnessLabel.setEnabled(enabled);
    brightness.setEnabled(enabled);
  }

  /** Switches between lo-res and hi-res mode. */
  public void setMode(boolean lowres) {
    loRes.setSelected(lowres);
    hiRes.setSelected(!lowres);
  }


  // -- INTERNAL API METHODS --

  /** Updates image color table, for grayscale and brightness adjustments. */
  void doColorTable() {
    bio.setImageColors(grayscale.isSelected(), brightness.getValue());
  }

  /** Sets the animation widget's animation control. */
  void setControl(AnimationControl control) { anim.setControl(control); }

}
