'''
    File name: utils.py
    Author: Asma BRAZI
    Author: Alexandre HEINTZMANN
    Date created: 13/04/2019
    Date last modified: 22/04/2019
    Python Version: 3.6.7
'''

from picamera import PiCamera
from time import sleep
import os.path
import os
import datetime
from PIL import Image
import numpy as np
import cv2
from imageUtil import *
from matplotlib import pyplot as plt
import sys
import json
import math
from InputView import *
from threading import Thread


# the function HoughLinesP return the maximum vertical line detected (let it be our height of the wall)
#maxLG:  the height of the wall that we allow to have
def HoughLinesP(im='v.jpg',maxLG=350):
    img = cv2.imread(im)
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
    #cv2.imshow("Edges", edges)
    #cv2.imshow("Image", img)
    #cv2.waitKey(0)
    #cv2.destroyAllWindows()
    return abs(y1-y2)

#create a responsible thread of a CMD executed by the Thymio
class AsebaThread(Thread):
    def __init__(self,cmd):
        Thread.__init__(self)
        self.cmd=cmd
    def run(self):
        os.system(self.cmd)

#taking picture by the rhe robot
def takePicture(r1=600,r2=400):
    urlDirectory="/home/pi/VisualNav"
    imgName=datetime.datetime.now().strftime('%H%M%S%d%m')
    urlImg=urlDirectory+'/'+imgName+'.jpg'
    camera = PiCamera()
    camera.resolution = (r1,r2)
    camera.start_preview()
    sleep(2)
    camera.capture(urlImg)
    camera.stop_preview()
    return urlImg

#type is the name of the picture 0:QRCODE 1:PEPPER 2:PLATON
def getThreshold(type):
    if(type==0):
        return 12000000.0
    if(type==1):
        return 30000000.0
    if(type==2):
        return 30000000.0

#multi= u: unique object to detect; m: multi, because for corners we assume that there is only PEPPER on.
#So we dont really need to check each element on the DB
#nbRefs: number of objects in the DB
#objects must have their name in the range of (0,nbRefs)

def recognition(img,ref="0",multi='u',nbRefs=3):
    results=[]
    if (multi=='m'):
        references=np.arange(0,nbRefs,1).tolist()
    else:
        references=[int(ref)] 

    for t in references:
        threshold=getThreshold(t)
        reference="DB/"+str(t)+".png"
        urlDirectory="/home/pi/VisualNav"
        match=False
        imgURL = img

        # load the image image, convert it to grayscale, and detect edges
        template = cv2.imread(reference,0)
        # make the template smaller to have better performances
        template = resize(template, int(template.shape[1] * 0.25))
        template = cv2.Canny(template, 50, 150)
        (tH, tW) = template.shape[:2]
        #cv2.imshow("Template", template)

        # load the image, convert it to grayscale,
        image = cv2.imread(imgURL,0)
        image = normalise(image)
        # initialize the variable to keep track of the matched region
        found = None
        # loop over the scales of the image
        # might take a long time to execute if the 2nd argument of linespace is to high
        for scale in np.linspace(0.1, 2, 20)[::-1]:
            #print(yes)
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
            (minVal, maxVal, _, maxLoc) = cv2.minMaxLoc(result)

            # draw a bounding box around the detected region
            clone = np.dstack([edged, edged, edged])
            cv2.rectangle(clone, (maxLoc[0], maxLoc[1]),
                (maxLoc[0] + tW, maxLoc[1] + tH), (0, 0, 255), 2)
            #cv2.imshow("Visualize", clone)
            #cv2.waitKey(0)

            # if we have found a new maximum correlation value, then update the variable
            if found is None or maxVal > found[0]:
                found = (maxVal, maxLoc, r)
                #print(found)
                if(maxVal>=threshold):
                    match=True
          
        # compute the (x, y) coordinates of the bounding box based on the resized ratio
        (_, maxLoc, r) = found
        (startX, startY) = (int(maxLoc[0] * r), int(maxLoc[1] * r))
        (endX, endY) = (int((maxLoc[0] + tW) * r), int((maxLoc[1] + tH) * r))

        # draw a bounding box around the detected result and display the image
        #cv2.rectangle(image, (startX, startY), (endX, endY), (0, 0, 255), 2)
        #cv2.imshow("Image", image)
        #cv2.waitKey(0)
        imgWidth=(np.shape(image)[1]/int(tW * r) *11.5)
        # camera wide angle by default
        angle = 54
        # trigonometry to get the distance from the detected object
        dist = (imgWidth/2)/math.tan(math.radians(angle/2))
        if not match:
            results.append([-1,[-1,-1,-1,-1]])
        else:
            results.append([round(dist,2),[startX, startY, endX, endY]])
    return results 

#according to the angle and the fName, we order the robot to rotate or move
def getAsebaFileD(fName,angle):
    d=angle
    speed=1      
    fName+="D"

    with open(fName+".aesl","r+") as f:
        print(f)

        fl=f.readlines()
        f.seek(0)
        for i in fl:
            if "var distance" in i:
                print(i+"\n")
                print(str(int(d))+"\n")
                f.write("var distance="+str(int(d))+"\n")
            else:
                if "var reverse=" not in i:
                    f.write(i)
                else:
                    f.write("var reverse="+str(int(speed))+"\n")
        f.truncate()