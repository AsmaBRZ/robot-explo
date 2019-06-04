'''
    File name: imageRecognition.py
    Author: Alexandre Heintzmann
    Date created: 23/03/2013
    Date last modified: 26/03/2013
    Python Version: 3.6.7
'''
# based on https://www.pyimagesearch.com/2015/01/26/multi-scale-template-matching-using-python-opencv/
import numpy as np
import cv2
import math
import sys
from imageUtil import *



imageName = (sys.argv[2])
URL = (sys.argv[1])
imgURL = URL+'/ressources/inputImages/'+imageName
templateURL = URL+'/ressources/inputImages/imgTestQr.png'

# load the image image, convert it to grayscale, and detect edges
template = cv2.imread(templateURL,0)
# make the template smaller to have better performances
template = resize(template, int(template.shape[1] * 0.25))
template = cv2.Canny(template, 50, 150)
(tH, tW) = template.shape[:2]
cv2.imshow("Template", template)

# load the image, convert it to grayscale,
image = cv2.imread(imgURL,0)
image = normalise(image)
# initialize the variable to keep track of the matched region
found = None

# loop over the scales of the image
# might take a long time to execute if the 2nd argument of linespace is to high
for scale in np.linspace(0.1, 2, 20)[::-1]:
    # resize the image according to the scale, and keep track of the ratio of the resizing
    resized = resize(image, int(image.shape[1] * scale))
    r = image.shape[1] / float(resized.shape[1])

    # if the resized image is smaller than the template, then break from the loop
    if resized.shape[0] < tH or resized.shape[1] < tW:
        break

    # detect edges in the resized, grayscale image and apply template
    # matching to find the template in the image
    edged = cv2.Canny(resized, 50, 150)
    result = cv2.matchTemplate(edged, template, cv2.TM_CCOEFF)
    (_, maxVal, _, maxLoc) = cv2.minMaxLoc(result)

    # draw a bounding box around the detected region
    clone = np.dstack([edged, edged, edged])
    cv2.rectangle(clone, (maxLoc[0], maxLoc[1]),
        (maxLoc[0] + tW, maxLoc[1] + tH), (0, 0, 255), 2)
    #cv2.imshow("Visualize", clone)
    #cv2.waitKey(0)

    # if we have found a new maximum correlation value, then update the variable
    if found is None or maxVal > found[0]:
        found = (maxVal, maxLoc, r)
# compute the (x, y) coordinates of the bounding box based on the resized ratio
(_, maxLoc, r) = found
(startX, startY) = (int(maxLoc[0] * r), int(maxLoc[1] * r))
(endX, endY) = (int((maxLoc[0] + tW) * r), int((maxLoc[1] + tH) * r))

# draw a bounding box around the detected result and display the image
cv2.rectangle(image, (startX, startY), (endX, endY), (0, 0, 255), 2)
#cv2.imshow("Image", image)
#cv2.waitKey(0)
imgWidth=(np.shape(image)[1]/int(tW * r) *11.5)
# camera wide angle by default
angle = 54
# trigonometry to get the distance from the detected object
dist = (imgWidth/2)/math.tan(math.radians(angle/2))
print(round(dist,2))

#cv2.imshow("Image", image)
#cv2.waitKey(0)