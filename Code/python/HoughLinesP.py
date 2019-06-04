'''

 File name: HoughLinesP.py
 Author: Asma BRAZI
 Date created: 23/04/2019
 Date last modified: 23/04/2019
 Python Version: 3.6.7


https://docs.opencv.org/3.0-beta/modules/imgproc/doc/feature_detection.html?highlight=cv2.houghlinesp#cv2.HoughLinesP

cv2.HoughLinesP(image, rho, theta, threshold[, lines[, minLineLength[, maxLineGap]]]) → lines
Parameters:	
    image – 8-bit, single-channel binary source image. The image may be modified by the function.
    lines – Output vector of lines. Each line is represented by a 4-element vector (x_1, y_1, x_2, y_2) , where (x_1,y_1) and (x_2, y_2) are the ending points of each detected line segment.
    rho – Distance resolution of the accumulator in pixels.
    theta – Angle resolution of the accumulator in radians.
    threshold – Accumulator threshold parameter. Only those lines are returned that get enough votes ( >\texttt{threshold} ).
    minLineLength – Minimum line length. Line segments shorter than that are rejected.
    maxLineGap – Maximum allowed gap between points on the same line to link them.


'''
import cv2
import numpy as np

maxLG=300
img = cv2.imread('v.jpg')
gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
edges = cv2.Canny(gray, 75, 150)

lines = cv2.HoughLinesP(edges, 1, 1, 30, maxLineGap=maxLG)
tmpMaxLine=[]
for line in lines:
    x1, y1, x2, y2 = line[0]
    tmpMaxLine.append(abs(y1-y2))
tmpMaxLine=np.array(tmpMaxLine)
indexMaxElement=np.where(tmpMaxLine == np.amax(tmpMaxLine))[0][0]
x1, y1, x2, y2 = lines[indexMaxElement][0]
cv2.line(img, (x1, y1), (x2, y2), (0, 255, 0), 3)

cv2.imshow("Edges", edges)
cv2.imshow("Image", img)
cv2.waitKey(0)
cv2.destroyAllWindows()