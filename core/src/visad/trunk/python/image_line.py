
from visad.python.JPythonMethods import *
import subs
from visad.java2d import *
from visad import DataReferenceImpl,CellImpl,Real, AxisScale
 
from visad.util import VisADSlider
from javax.swing import JFrame, JPanel
from java.awt import BorderLayout, GridLayout, Font

image = load("../data/mcidas/AREA0007")
#image=load("adde://image?")
print "Done reading data..."

dom = getDomain(image)
d = domainType(image)
r = rangeType(image)

# max lines & elements of image
NELE = dom.getX().getLength()
LINES = dom.getY().getLength()

# subs for image in display-1
m = subs.makeMaps(d[0],"x",d[1],"y",r[0],"rgb")
d1 = subs.makeDisplay(m)
subs.setBoxSize(d1,.80)
# add the image to the display
refimg = subs.addData("image", image, d1)

# now the second panel
m2 = subs.makeMaps(d[0],"x",r[0],"y",d[1],"selectvalue")
d2 = subs.makeDisplay(m2)
subs.setBoxSize(d2,.80)

# get the desired format of the Data (line->(element->value))
byline = domainFactor(image,d[1])
ref2 = subs.addData("imageline", byline, d2)

# also, set up a dummy reference so we can put the line onto the display
usref = subs.addData("line", None, d1)

# define an inner-type CellImpl class to handle changes
class MyCell(CellImpl):
 def doAction(this):
  line = (LINES-1) - (userline.getData()).getValue()
  pts = subs.makeLine( (d[1], d[0]), ((line,line),(0,NELE)))
  usref.setData(pts)
  ff = byline.evaluate(Real(line))
  ref2.setData(ff)

# make a DataReference that we can use later to change the value of "line"
userline = DataReferenceImpl("userline")
slide = VisADSlider("imgline",0,LINES,0,1.0,userline,d[1])

cell = MyCell();
cell.addReference(userline)

# change the scale label on x axis
xscale=AxisScale(m2[0],label="Element position in image")
showAxesScales(d2,1)

# display everything...
frame = JFrame("Test T7")
pane = frame.getContentPane()
pane.setLayout(BorderLayout())
panel = JPanel(GridLayout(1,2,5,5))
panel.add(d1.getComponent())
panel.add(d2.getComponent())
pane.add("Center",panel)
pane.add("North",slide)
frame.setSize(800,500)
frame.setVisible(1)
