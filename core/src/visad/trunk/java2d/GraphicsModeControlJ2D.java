//
// GraphicsModeControlJ2D.java
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

package visad.java2d;

import java.rmi.*;

import visad.*;

import visad.util.Util;

/**
   GraphicsModeControlJ2D is the VisAD class for controlling various
   mode settings for rendering.<P>

   A GraphicsModeControlJ2D is not linked to any DisplayRealType or
   ScalarMap.  It is linked to a DisplayImpl.<P>
*/
public class GraphicsModeControlJ2D extends GraphicsModeControl {

  static final String[] LINE_STYLE = {
    "solid", "dash", "dot", "dash-dot"
  };

  private float lineWidth; // for LineAttributes; >= 1.0
  private float pointSize; // for PointAttributes; >= 1.0
  private int lineStyle; // for LineAttributes
  private int colorMode;
  private boolean pointMode; // true => points in place of lines and surfaces
  private boolean textureEnable; // true => allow use of texture mapping
  private boolean scaleEnable; // true => display X, Y and Z scales

  private int transparencyMode = 0;
  private int projectionPolicy = 0;
  private int polygonMode = 0;

  private boolean missingTransparent = true;
  private int curvedSize = 10;

  public GraphicsModeControlJ2D(DisplayImpl d) {
    super(d);
    lineWidth = 1.0f;
    pointSize = 1.0f;
    lineStyle = SOLID_STYLE;
    pointMode = false;
    textureEnable = true;
    scaleEnable = false;
  }

  public boolean getMode2D() {
    return getDisplayRenderer().getMode2D();
  }

  /**
   * Get the current line width used for LineAttributes.  The default
   * is 1.0.
   *
   * @return  line width (>= 1.0)
   */
  public float getLineWidth() {
    return lineWidth;
  }

  /**
   * Set the line width used for LineAttributes.  Calls changeControl
   * and resets the display.
   *
   * @param width  width to use (>= 1.0)
   *
   * @throws  VisADException   couldn't set the line width on local display
   * @throws  RemoteException  couldn't set the line width on remote display
   */
  public void setLineWidth(float width)
         throws VisADException, RemoteException {
    if (width < 1.0f) {
      throw new DisplayException("GraphicsModeControlJ2D." +
                                 "setLineWidth: width < 1.0");
    }
    if (Util.isApproximatelyEqual(width, lineWidth)) return;
    lineWidth = width;

    // WLH 2 Dec 2002 in response to qomo2.txt
    DisplayRendererJ2D dr = (DisplayRendererJ2D) getDisplayRenderer();
    dr.setLineWidth(width);

    changeControl(true);
    getDisplay().reDisplayAll();
  }

  /**
   * Set the line width used for LineAttributes.   Does not update
   * display.
   *
   * @param width  width to use (>= 1.0)
   */
  public void setLineWidth(float width, boolean dummy) {
    if (width >= 1.0f) {
      lineWidth = width;
    }
  }

  /**
   * Get the current point size used for PointAttributes.  The default
   * is 1.0.
   *
   * @return  point size  (>= 1.0)
   */
  public float getPointSize() {
    return pointSize;
  }

  /**
   * Set the point size used for PointAttributes.  Calls changeControl
   * and updates the display.
   *
   * @param size  size to use (>= 1.0)
   *
   * @throws  VisADException   couldn't set the point size on local display
   * @throws  RemoteException  couldn't set the point size on remote display
   */
  public void setPointSize(float size)
         throws VisADException, RemoteException {
    if (size < 1.0f) {
      throw new DisplayException("GraphicsModeControlJ2D." +
                                 "setPointSize: size < 1.0");
    }
    if (Util.isApproximatelyEqual(size, pointSize)) return;
    pointSize = size;
    changeControl(true);
    getDisplay().reDisplayAll();
  }

  /**
   * Set the point size used for PointAttributes.  Doesn't update
   * the display.
   *
   * @param size  size to use (>= 1.0)
   */
  public void setPointSize(float size, boolean dummy) {
    if (size >= 1.0f) {
      pointSize = size;
    }
  }

  /**
   * Get the current line style used for LineAttributes.  The default
   * is GraphicsModeControl.SOLID_STYLE.
   *
   * @return  line style (SOLID_STYLE, DASH_STYLE, DOT_STYLE or DASH_DOT_STYLE)
   */
  public int getLineStyle() {
    return lineStyle;
  }

  /**
   * Set the line style used for LineAttributes.  Calls changeControl
   * and resets the display.
   *
   * @param style  style to use (SOLID_STYLE, DASH_STYLE,
   *               DOT_STYLE or DASH_DOT_STYLE)
   *
   * @throws  VisADException   couldn't set the line style on local display
   * @throws  RemoteException  couldn't set the line style on remote display
   */
  public void setLineStyle(int style)
         throws VisADException, RemoteException {
    if (style != SOLID_STYLE && style != DASH_STYLE &&
      style != DOT_STYLE && style != DASH_DOT_STYLE)
    {
      style = SOLID_STYLE;
    }
    if (style == lineStyle) return;
    lineStyle = style;
    changeControl(true);
    getDisplay().reDisplayAll();
  }

  /**
   * Set the line style used for LineAttributes.   Does not update display.
   *
   * @param style  style to use (SOLID_STYLE, DASH_STYLE,
   *               DOT_STYLE or DASH_DOT_STYLE)
   */
  public void setLineStyle(int style, boolean dummy) {
    if (style != SOLID_STYLE && style != DASH_STYLE &&
      style != DOT_STYLE && style != DASH_DOT_STYLE)
    {
      style = SOLID_STYLE;
    }
    lineStyle = style;
  }

  /**
   * Get the color mode used for combining color values.  The default
   * is GraphicsModeControl.AVERAGE_COLOR_MODE.
   *
   * @return  color mode (AVERAGE_COLOR_MODE or SUM_COLOR_MODE)
   */
  public int getColorMode() {
    return colorMode;
  }

  /**
   * Set the color mode used for combining color values.
   *
   * @param mode  mode to use (AVERAGE_COLOR_MODE or SUM_COLOR_MODE)
   */
  public void setColorMode(int mode)
         throws VisADException, RemoteException {
    if (mode != AVERAGE_COLOR_MODE && mode != SUM_COLOR_MODE) {
      mode = AVERAGE_COLOR_MODE;
    }
    if (colorMode == colorMode) return;
    colorMode = mode;
    changeControl(true);
    getDisplay().reDisplayAll();
  }

  /**
   * Gets the point mode.
   *
   * @return  True if the display is using points rather than connected
   *          lines or surfaces for rendering.
   */
  public boolean getPointMode() {
    return pointMode;
  }

  /**
   * Sets the point mode and updates the display.
   *
   * @param mode         true if the display should use points rather
   *                     than connected lines or surfaces for rendering.
   */
  public void setPointMode(boolean mode)
         throws VisADException, RemoteException {
    if (mode == pointMode) return;
    pointMode = mode;
    changeControl(true);
    getDisplay().reDisplayAll();
  }

  /**
   * Set whether texture mapping should be used or not.
   *
   * @param  enable   true to use texture mapping (the default)
   */
  public void setTextureEnable(boolean enable)
         throws VisADException, RemoteException {
    if (enable == textureEnable) return;
    textureEnable = enable;
    changeControl(true);
    getDisplay().reDisplayAll();
  }

  /**
   * See if texture mapping is enabled or not
   *
   * @return    true if texture mapping is enabled.
   */
  public boolean getTextureEnable() {
    return textureEnable;
  }

  /**
   *  Toggle the axis scales in the display
   *
   * @param  enable    true to enable, false to disable
   *
   * @throws  VisADException   couldn't change state of scale enablement
   * @throws  RemoteException  couldn't change state of scale enablement on
   *                           remote display
   */
  public void setScaleEnable(boolean enable)
         throws VisADException, RemoteException {
    if (enable == scaleEnable) return;
    scaleEnable = enable;
    getDisplayRenderer().setScaleOn(enable);
    changeControl(true);
  }

  /**
   * Get whether display scales are enabled or not
   *
   * @return  true if enabled, otherwise false
   */
  public boolean getScaleEnable() {
    return scaleEnable;
  }

  /**
   * Get the current transparency mode
   *
   * @return  transparency mode
   */
  public int getTransparencyMode() {
    return transparencyMode;
  }

  /**
   * Sets the transparency mode.
   *
   * @param   mode   transparency mode to use.  Legal value = 0.
   * @throws  VisADException    bad mode or couldn't create necessary VisAD
   *                            object
   * @throws  RemoteException   couldn't create necessary remote object
   */
  public void setTransparencyMode(int mode)
         throws VisADException, RemoteException {
    if (mode == 0) {
      transparencyMode = mode;
    }
    else {
      throw new DisplayException("GraphicsModeControlJ2D." +
                                 "setTransparencyMode: bad mode");
    }
  }

  /**
   * Sets the projection policy for the display.  
   *
   * @param   policy      policy to be used 
   *
   * @throws  VisADException   bad policy or can't create the necessary VisAD
   *                           object
   * @throws  RemoteException  change policy on remote display
   */
  public void setProjectionPolicy(int policy)
         throws VisADException, RemoteException {
    if (policy == 0) {
      projectionPolicy = policy;
    }
    else {
      throw new DisplayException("GraphicsModeControlJ2D." +
                                 "setProjectionPolicy: bad policy");
    }
  }

  /**
   * Get the current projection policy for the display.
   *
   * @return  policy
   */
  public int getProjectionPolicy() {
    return projectionPolicy;
  }

  /**
   * Sets the polygon mode.
   *
   * @param  mode   the polygon mode to be used
   *
   * @throws  VisADException   bad mode or can't create the necessary VisAD
   *                           object
   * @throws  RemoteException  can't change mode on remote display
   */
  public void setPolygonMode(int mode)
         throws VisADException, RemoteException {
    if (mode == 0) {
      polygonMode = mode;
    }
    else {
      throw new DisplayException("GraphicsModeControlJ2D." +
                                 "setPolygonMode: bad mode");
    }
  }

  /**
   * Get the current polygon mode.
   *
   * @return  mode
   */
  public int getPolygonMode() {
    return polygonMode;
  }

  /**
   * See whether missing values are rendered as transparent or not.
   *
   * @return  true if missing values are transparent.
   */
  public boolean getMissingTransparent() {
    return missingTransparent;
  }

  /**
   * Set the transparency of missing values.
   *
   * @param  missing   true if missing values should be rendered transparent.
   */
  public void setMissingTransparent(boolean missing)
         throws VisADException, RemoteException {
    if (!missing) {
      missingTransparent = missing;
    }
    else {
      throw new DisplayException("GraphicsModeControlJ2D." +
                                 "setMissingTransparent: must be false");
    }
  }

  /**
   * Get the undersampling factor of surface shape for curved texture maps
   *
   * @return  undersampling factor (default 10)
   */
  public int getCurvedSize() {
    return curvedSize;
  }

  /**
   * Set the undersampling factor of surface shape for curved texture maps
   *
   * @param  curved_size  undersampling factor (default 10)
   */
  public void setCurvedSize(int curved_size) {
    curvedSize = curved_size;
  }

  public Object clone() {
    GraphicsModeControlJ2D mode =
      new GraphicsModeControlJ2D(getDisplay());
    mode.lineWidth = lineWidth;
    mode.pointSize = pointSize;
    mode.lineStyle = lineStyle;
    mode.pointMode = pointMode;
    mode.textureEnable = textureEnable;
    mode.scaleEnable = scaleEnable;
    mode.transparencyMode = transparencyMode;
    mode.projectionPolicy = projectionPolicy;
    mode.missingTransparent = missingTransparent;
    mode.polygonMode = polygonMode;
    mode.curvedSize = curvedSize;
    return mode;
  }

  /** copy the state of a remote control to this control */
  public void syncControl(Control rmt)
    throws VisADException
  {
    if (rmt == null) {
      throw new VisADException("Cannot synchronize " + getClass().getName() +
                               " with null Control object");
    }

    if (!(rmt instanceof GraphicsModeControlJ2D)) {
      throw new VisADException("Cannot synchronize " + getClass().getName() +
                               " with " + rmt.getClass().getName());
    }

    GraphicsModeControlJ2D rmtCtl = (GraphicsModeControlJ2D )rmt;

    boolean changed = false;
    boolean redisplay = false;

    if (!Util.isApproximatelyEqual(lineWidth, rmtCtl.lineWidth)) {
      changed = true;
      redisplay = true;
      lineWidth = rmtCtl.lineWidth;
    }
    if (!Util.isApproximatelyEqual(pointSize, rmtCtl.pointSize)) {
      changed = true;
      redisplay = true;
      pointSize = rmtCtl.pointSize;
    }
    if (lineStyle != rmtCtl.lineStyle) {
      changed = true;
      redisplay = true;
      lineStyle = rmtCtl.lineStyle;
    }

    if (pointMode != rmtCtl.pointMode) {
      changed = true;
      redisplay = true;
      pointMode = rmtCtl.pointMode;
    }
    if (textureEnable != rmtCtl.textureEnable) {
      changed = true;
      redisplay = true;
      textureEnable = rmtCtl.textureEnable;
    }
    if (scaleEnable != rmtCtl.scaleEnable) {
      changed = true;
      scaleEnable = rmtCtl.scaleEnable;
      getDisplayRenderer().setScaleOn(scaleEnable);
    }

    if (transparencyMode != rmtCtl.transparencyMode) {
      changed = true;
      transparencyMode = rmtCtl.transparencyMode;
    }
    if (projectionPolicy != rmtCtl.projectionPolicy) {
      changed = true;
      projectionPolicy = rmtCtl.projectionPolicy;
    }
    if (polygonMode != rmtCtl.polygonMode) {
      changed = true;
      polygonMode = rmtCtl.polygonMode;
    }

    if (missingTransparent != rmtCtl.missingTransparent) {
      changed = true;
      missingTransparent = rmtCtl.missingTransparent;
    }

    if (curvedSize != rmtCtl.curvedSize) {
      changed = true;
      curvedSize = rmtCtl.curvedSize;
    }

    if (changed) {
      try {
        changeControl(true);
      } catch (RemoteException re) {
        throw new VisADException("Could not indicate that control" +
                                 " changed: " + re.getMessage());
      }
    }
    if (redisplay) {
      getDisplay().reDisplayAll();
    }
  }

  public boolean equals(Object o)
  {
    if (!super.equals(o)) {
      return false;
    }

    GraphicsModeControlJ2D gmc = (GraphicsModeControlJ2D )o;

    boolean changed = false;

    if (!Util.isApproximatelyEqual(lineWidth, gmc.lineWidth)) {
      return false;
    }
    if (!Util.isApproximatelyEqual(pointSize, gmc.pointSize)) {
      return false;
    }
    if (lineStyle != gmc.lineStyle) {
      return false;
    }

    if (pointMode != gmc.pointMode) {
      return false;
    }
    if (textureEnable != gmc.textureEnable) {
      return false;
    }
    if (scaleEnable != gmc.scaleEnable) {
      return false;
    }

    if (transparencyMode != gmc.transparencyMode) {
      return false;
    }
    if (projectionPolicy != gmc.projectionPolicy) {
      return false;
    }
    if (polygonMode != gmc.polygonMode) {
      return false;
    }

    if (missingTransparent != gmc.missingTransparent) {
      return false;
    }

    if (curvedSize != gmc.curvedSize) {
      return false;
    }

    return true;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("GraphicsModeControlJ2D[");

    buf.append("lw ");
    buf.append(lineWidth);
    buf.append(",ps ");
    buf.append(pointSize);
    buf.append(",ls ");
    buf.append(LINE_STYLE[lineStyle]);

    buf.append(pointMode ? "pm" : "!pm");
    buf.append(textureEnable ? "te" : "!te");
    buf.append(scaleEnable ? "se" : "!se");
    buf.append(missingTransparent ? "mt" : "!mt");

    buf.append(",tm ");
    buf.append(transparencyMode);
    buf.append(",pp ");
    buf.append(projectionPolicy);
    buf.append(",pm ");
    buf.append(polygonMode);
    buf.append(",cs ");
    buf.append(curvedSize);

    buf.append(']');
    return buf.toString();
  }
}
