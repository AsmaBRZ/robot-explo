'''
    File name: imageVanishingLines.py
    Author: Alexandre Heintzmann
    Date created: 26/03/2019
    Date last modified: 26/03/2019
    Python Version: 3.6.7
'''

import cv2
import numpy as np
from utils import *
from imageUtil import *
import sys

res1=600
res2=400
imageURL=takePicture(res1,res2)

image = cv2.imread(imageURL, 0)
image = normalise(image)

edges = cv2.Canny(image, 50, 150)

# get most important lines
lines = cv2.HoughLines(edges, 1, np.pi / 180, 80)
moy = 0
nbline = 0
if (len(lines) > 0):
    for line in lines:
        for rho, theta in line:
            a = np.cos(theta)
            b = np.sin(theta)
            x0 = a * rho
            y0 = b * rho
            x1 = int(x0 + 1000 * (-b))
            y1 = int(y0 + 1000 * (a))
            x2 = int(x0 - 1000 * (-b))
            y2 = int(y0 - 1000 * (a))

            # Keep only hozirontal lines
            if (b > 0.8):
                # Draw lines on the images
                #cv2.line(image, (x1, y1), (x2, y2), 255, 0)
                nbline += 1
                moy += theta

# Return mean of lines' angle
angle=0
if(nbline>0):
    moy = moy/nbline
    angle=moy*180/np.pi - 90
#cv2.imshow("Image", image)
#cv2.waitKey(0)

with open('data/angleCorrected', 'w') as outfile:
    outfile.write(str(angle))
    outfile.close()