'''
    File name: utils.py
    Author: Asma BRAZI
    Author: Alexandre HEINTZMANN
    Date created: 13/04/2019
    Python Version: 3.6.7
'''
from __future__ import print_function
from numpy import linalg as LA
from picamera import PiCamera
from time import sleep
from shapely.geometry import LineString
from shapely.geometry.collection import GeometryCollection
from shapely.geometry.point import Point
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
from picamera.array import PiRGBArray
from picamera import PiCamera
import time
import sched
import collections
from numpy import ones,vstack
from numpy.linalg import lstsq
import dbus
import dbus.mainloop.glib
from gi.repository import GObject as gobject 
from optparse import OptionParser
import itertools
import random
from itertools import starmap


cpTour=0 #
distRob=29
proxSensorsVal=[-1,-1,-1,-1,-1,-1,-1]
micI=0
temperature=0
#pathToSensorDataFile='../../SensorDataThymio/'
pathToSensorDataFile=''



#Initalization of asebamedulla 
def initAsebamedulla():
    os.system('asebamedulla   "ser:name=Thymio-II" &')

#take a picture with the Raspberry-Pi CAmera and return the path to the image
def takePicture(r1=600,r2=400):
    #path to the image captured
    urlDirectory="/home/pi/VisualNav"
    imgName=datetime.datetime.now().strftime('%H%M%S%d%m')
    urlImg=urlDirectory+'/'+imgName+'.jpg'
    #instanciate the camera
    camera = PiCamera()
    camera.resolution = (r1,r2)
    #take a picture
    try:
        camera.start_preview()
        sleep(2)
        camera.capture(urlImg)
        camera.stop_preview()
    finally:
        camera.close()
    #return the path to the picture captured
    return urlImg


# the function HoughLinesP return the biggest vertical detected line (let it be our height of the wall)
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
    #retrieve the biggest line
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

#type is the name of the picture 0:QRCODE 1:PEPPER 2:PLATON
def getThreshold(type):
    if(type==0): #QR code
        return 20000000.0
    if(type==1): #Peper
        return 30000000.0
    if(type==2): #Platon
        return 30000000.0

#nbRefs: number of objects in the DB
#objects must have their name in the range of (0,nbRefs)
#img is the complete path to the picture to analyze
#nbRefs is the number of objetcs to match with the picture captured
def recognition(img,nbRefs=1):
    results=[]
    references=np.arange(0,nbRefs,1).tolist()
    for t in references:
        threshold=getThreshold(t)
        #print(t)
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
            #clone = np.dstack([edged, edged, edged])
            #cv2.rectangle(clone, (maxLoc[0], maxLoc[1]),(maxLoc[0] + tW, maxLoc[1] + tH), (0, 0, 255), 2)
            #cv2.imshow("Visualize", clone)
            #cv2.waitKey(0)

            # if we have found a new maximum correlation value, then update the variable
            if found is None or maxVal > found[0]:
                found = (maxVal, maxLoc, r)
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
        #cv2.destroyAllWindows()
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

#Given two lines l1 and l2, the function below determines if the two lines intersect
#in that case, the intersection point is returned, else: False is returned
def intersection(l1,l2):
    line1 = LineString([(l1[0],l1[1]), (l1[2],l1[3])])
    line2 = LineString([(l2[0],l2[1]), (l2[2],l2[3])])
    result=line1.intersection(line2) # the type of the result: shapely.geometry.point.Point
    if isinstance(result, GeometryCollection):
        return False
    if isinstance(result, LineString):
        return result.coords[0]
    if isinstance(result, Point):
        return result.x,result.y

#Calculate the equation of a segment defined by its two extremities p1 and p2
#return the coef a and  of the equation
def coefLine(p1,p2):
    points = [p1,p2]
    x, y = zip(*points)
    M = vstack([x,ones(len(x))]).T
    a, c = lstsq(M, y)[0]
    #print("Line Solution is y = {m}x + {c}".format(m=m,c=c))
    return a,1,c

#Calculate the shortest distance between a point and a line
def distPointLine(p1,p2,p):
    return min(np.sqrt((p1[0]-p[0])**2+(p1[1]-p[1])**2),np.sqrt((p2[0]-p[0])**2+(p2[1]-p[1])**2))

#Calculate the longest distance between two lines, by checking each extremity with the other line. 
def distLineLine(l1,l2):
    x1,y1,x2,y2=l1
    x3,y3,x4,y4=l2
    e11=[x1,y1]
    e12=[x2,y2]
    e21=[x3,y3]
    e22=[x4,y4]
    d1=distPointLine(e11,e12,e21)
    d2=distPointLine(e11,e12,e22)
    d3=distPointLine(e21,e22,e11)
    d4=distPointLine(e21,e22,e12)
    return max(d1,d2,d3,d4)

#Calculate the longest distance separating two lines, by checking the distance separating each extremity with the other line. 
def vertTherExcept(l1,l2,thresholdX=20,thresholdY=20):
    x1,y1,x2,y2=l1
    x3,y3,x4,y4=l2
    if x1-x3<thresholdX and x2-x4<thresholdX :
        return True
    return False

#up to a fixed threshold defined by differents tests, this function determines if two lines are close
def minDistLineLine(l1,l2,thresholdA=3,thresholdC=30,thresholdX=20,thresholdY=20):
    x1,y1,x2,y2=l1
    x3,y3,x4,y4=l2
    a1,b1,c1=coefLine([x1,y1],[x2,y2])
    a2,b2,c2=coefLine([x3,y3],[x4,y4])
    if a1-a2<thresholdA and c1-c2<thresholdC:
        #print("******************************************Merged:",a1,a2,c1,c2)
        return True
    else:
        if(x1-x3<thresholdX and x2-x4<thresholdX and y1-y3<thresholdY and y2-y4<thresholdY) or (x1-x4<thresholdX and x2-x3<thresholdX and y1-y4<thresholdY and y2-y3<thresholdY):
            #print("Surpriiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiise")
            #print("*****Merged:",a1,a2,c1,c2)
            return True
    return False

#useful function determining the other extremities, knowing two extremities of a line
def getTwoExtremities(index_min_dist):
    if index_min_dist==0: #ac -> bd
        return 3
    if index_min_dist==1: #ad -> bc
        return 2
    if index_min_dist==2: #bc -> ad
        return 1
    if index_min_dist==3: #bd -> ac
        return 0

#Treat by a couple of lines from the entry 'lines', if they are too close depending on a threshold, they are merged
#The set of kept lines is returned
def newLine4Points(l1,l2):
    #retreive extremities of each line
    a_x=l1[0]
    a_y=l1[1]
    b_x=l1[2]
    b_y=l1[3]
    c_x=l2[0]
    c_y=l2[1]
    d_x=l2[2]
    d_y=l2[3]
    #calculate the length between points
    a_c=np.sqrt((a_x-c_x)**2+(a_y-c_y)**2)
    a_d=np.sqrt((a_x-d_x)**2+(a_y-d_y)**2)
    b_c=np.sqrt((b_x-c_x)**2+(b_y-c_y)**2)
    b_d=np.sqrt((b_x-d_x)**2+(b_y-d_y)**2)
    dist_points=np.array([a_c,a_d,b_c,b_d])
    #all possible combinations between points
    newLines=[[a_x,a_y,c_x,c_y],[a_x,a_y,d_x,d_y],[b_x,b_y,c_x,c_y],[b_x,b_y,d_x,d_y]]
    index_min_dist= np.argmin(dist_points)
    #the two closet points are
    index=getTwoExtremities(index_min_dist)
    #get the others points two merge
    x0,y0,x1,y1=newLines[index]
    #projection of the point on the line
    point = Point(x0,y0)
    line = LineString([(l2[0], l2[1]), (l2[2], l2[3])])
    x = np.array([x0,y0])
    u = np.array([l2[0], l2[1]])
    v = np.array(l2[2], l2[3])
    n = v-u
    n /= np.linalg.norm(n, 2)
    projected_point= u + n*np.dot(x-u, n)
    a_x,a_y=projected_point
    #calculation of lengths
    dis_new_l1=np.sqrt((a_x-l2[0])**2+(a_y-l2[1])**2)
    dis_new_l2=np.sqrt((a_x-l2[2])**2+(a_y-l2[3])**2)
    if dis_new_l1>dis_new_l2:
        return [a_x,a_y,l2[0],l2[1]]
    else:
        return [a_x,a_y,l2[2],l2[3]]
#projection of a point on a line defined by its etremities
def projectPointLine(a, b, p):
    a=np.array(a)
    b=np.array(b)
    p=np.array(p)
    ap = p-a
    ab = b-a
    result = a + np.dot(ap,ab)/np.dot(ab,ab) * ab
    return result
#givien twi lines l1 and l2, the function returns a new line representing their merge
def newLineMerging(l1,l2):
    s=getSepar(l1,l2)
    a,b,c=coefLine([s[0],s[1]],[s[2],s[3]])
    a_x=l1[0]
    a_y=l1[1]
    b_x=l1[2]
    b_y=l1[3]
    c_x=l2[0]
    c_y=l2[1]
    d_x=l2[2]
    d_y=l2[3]

    a_c=np.sqrt((a_x-c_x)**2+(a_y-c_y)**2)
    a_d=np.sqrt((a_x-d_x)**2+(a_y-d_y)**2)
    b_c=np.sqrt((b_x-c_x)**2+(b_y-c_y)**2)
    b_d=np.sqrt((b_x-d_x)**2+(b_y-d_y)**2)
    a_b=np.sqrt((a_x-b_x)**2+(a_y-b_y)**2)
    c_d=np.sqrt((c_x-d_x)**2+(c_y-d_y)**2)

    dist_points=np.array([a_c,a_d,b_c,b_d,a_b,c_d])
    newLines=[[a_x,a_y,c_x,c_y],[a_x,a_y,d_x,d_y],[b_x,b_y,c_x,c_y],[b_x,b_y,d_x,d_y],[a_x,a_y,b_x,b_y],[c_x,c_y,d_x,d_y]]
    index= np.argmax(dist_points)
    #get the future points to project
    x0,y0,x1,y1=newLines[index]
    #projection of the point on the line
    projected_point1_x,projected_point1_y=projectPointLine([l2[0], l2[1]],[l2[2], l2[3]],[x0,y0])
    projected_point2_x,projected_point2_y=projectPointLine([l2[0], l2[1]],[l2[2], l2[3]],[x1,y1])
    #print('projection',projected_point1_x,projected_point1_y,projected_point2_x,projected_point2_y)
    return projected_point1_x,projected_point1_y,projected_point2_x,projected_point2_y

#given two lines l1 and l2, the function returns a line in the middle of l1 et l2 separating them
def getSepar(l1,l2):
    x1,y1,x2,y2=l1
    x3,y3,x4,y4=l2
    #get the closet points 
    e11=[x1,y1]
    e12=[x2,y2]
    e21=[x3,y3]
    e22=[x4,y4]
    d11_21=np.sqrt((e11[0]-e21[0])**2+(e11[1]-e21[1])**2)
    d11_22=np.sqrt((e11[0]-e22[0])**2+(e11[1]-e22[1])**2)
    if d11_21<d11_22:
        return [int((e11[0]+e21[0])/2),int((e11[1]+e21[1])/2),int((e12[0]+e22[0])/2),int((e12[1]+e22[1])/2)]
    else:
        return [int((e11[0]+e22[0])/2),int((e11[1]+e22[1])/2),int((e12[0]+e21[0])/2),int((e12[1]+e21[1])/2)]

#from a set of lines, the funcion return the longest lines accorfing to a fixed threshold
def CaptureLongestSeg(lines,threshold=45):
    result=[]
    for line in lines:
        x1, y1, x2, y2 = line
        norm=np.sqrt((x1-x2)**2+(y1-y2)**2)
        if norm >threshold:
            result.append(line)
    return result

#given a set of lines, the functions merge the closest lines until no lines my be merged
def mergeLines(lines,threshold=30):
    filtred_lines={}
    lines_copy=lines.copy()
    dic={}
    for line in lines:
        x1, y1, x2, y2 = line
        dic[np.sqrt((x1-x2)**2+(y1-y2)**2)]=line
    dic= collections.OrderedDict(sorted(dic.items()))
    sortedDic=[]
    for k,v in dic.items():
        sortedDic.append([k,v]) #distance + line
    #convert liens array to a dic, this facilitates to filter lines at deletion
    dicLines = { i : sortedDic[i] for i in range(0, len(sortedDic) ) }
    n=400
    for Kl1 in list(dicLines):
        if Kl1 in dicLines.keys():
            l1_length,Vl1=dicLines[Kl1]
            Kl_OK=False
            for Kl2 in list(dicLines):
                if Kl2  in dicLines.keys():
                    l2_length,Vl2=dicLines[Kl2]
                    if Kl1!=Kl2:
                        #Is thes line sl1 and l2 close to merge?
                        if( distLineLine(Vl1,Vl2) <threshold and minDistLineLine(Vl1,Vl2)):
                            #print('Merge lines',Kl1,Kl2)
                            new_line=newLineMerging(Vl1,Vl2)
                            length_new_line=np.sqrt((new_line[0]-new_line[2])**2+(new_line[1]-new_line[3])**2)
                            #print(new_line)
                            dicLines[n]=[length_new_line,new_line]
                            dicLines.pop(Kl1)
                            dicLines.pop(Kl2)
                            n+=1
                            Kl_OK=True
                if Kl_OK:
                    break
    for key,value in dicLines.items():
        filtred_lines[value[0]]=value[1]
    return filtred_lines

#given a line l, the function calculates its norm
def getLineLength(l):
    x0, y0, x1, y1 = line
    return np.sqrt((x0-x1)**2+(y0-y1)**2)
#given three points p0, p1 and p2, the function calculates the angle (in degrees) between them
def get_angle(p0, p1=np.array([0,0]), p2=np.array([600, 0])):
    v0 = np.array(p0) - np.array(p1)
    v1 = np.array(p2) - np.array(p1)

    angle = np.math.atan2(np.linalg.det([v0,v1]),np.dot(v0,v1))
    return np.degrees(angle)

#Use of the LSD detection in order to capture the necessary segments after filtering
def LSDDetection(im):
    img = cv2.imread(im)
    n,m,_= np.array(img).shape
    img_filtered=img.copy()
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    gray= cv2.Canny(gray, 100, 150)
    lsd = cv2.createLineSegmentDetector(0)
    lines = lsd.detect(gray)[0]
    lines_vert=[]
    lines_horz=[]
    lines_non_vert=[]
    #filter the detected segments to vertical, horizontal and diagonal according to the angle in degrees formed with the X-axis
    if lines is not None:
        for line in lines:
            x0 = int(round(line[0][0]))
            y0 = int(round(line[0][1]))
            x1 = int(round(line[0][2]))
            y1 = int(round(line[0][3]))
            line_len=np.sqrt((x0-x1)**2+(y0-y1)**2)
            #if the line is not too small according to a fied threshold
            if line_len>20:
                angle=abs(get_angle(np.array([x0,y0]),np.array([x1,y1]),np.array([m-1,y1])))
                if (angle >=0 and angle <=5) or (angle >=175 and angle <=180):
                    #horizontal line
                    #cv2.line(img, (x0, y0), (x1,y1), (0,255,255), 1, cv2.LINE_AA)
                    lines_horz.append(line[0])
                elif  (angle >=80 and angle <=100) :
                    #vertical line
                    #cv2.line(img, (x0, y0), (x1,y1), (0,255,0), 1, cv2.LINE_AA)
                    lines_vert.append(line[0])
                elif (angle >=25 and angle <=70) or (angle >=110 and angle <=165):
                    #diagonal line
                    #cv2.line(img, (x0, y0), (x1,y1), (255,0,0), 1, cv2.LINE_AA)
                    lines_non_vert.append(line[0])
    #merging the segments to get a new longest segments
    filtred_non_vert_lines=mergeLines(lines_non_vert)
    filtred_horz_lines=mergeLines(lines_horz)
    filtred_vert_lines=mergeLines(lines_vert)
    #print(filtred_non_vert_lines,filtred_horz_lines,filtred_vert_lines)
    #for key,l1 in filtred_non_vert_lines.items():
        #cv2.line(img_filtered, (l1[0], l1[1]), (l1[2],l1[3]), (255,0,0), 1, cv2.LINE_AA)
    #for key,l1 in filtred_vert_lines.items():
        #cv2.line(img_filtered, (l1[0], l1[1]), (l1[2],l1[3]), (0,255,0), 1, cv2.LINE_AA)
    #for key,l1 in filtred_horz_lines.items():
        #cv2.line(img, (l1[0], l1[1]), (l1[2],l1[3]), (0,255,255), 1, cv2.LINE_AA)
   
    #cv2.imshow("Image_Filtered",img_filtered)
    #cv2.imshow("Edges", gray)
    width=-1
    width_x0=-1
    width_y0=-1
    width_x1=-1
    width_y1=-1

    height=-1
    height_x0=-1
    height_y0=-1
    height_x1=-1
    height_y1=-1

    depthL=-1
    diagL_x0=-1
    diagL_y0=-1
    diagL_x1=-1
    diagL_y1=-1

    depthR=-1
    depthR_x0=-1
    depthR_y0=-1
    depthR_x1=-1
    depthR_y1=-1
    
    #sort lines according to the biggest coordinate Y (near from the robot)
    tmp_nearstY={}

    if len(filtred_horz_lines) !=0:
        for key,value in filtred_horz_lines.items():
            tmp_nearstY[key]=(value[1]+value[3])/2

        new_data = list(map(list, sorted(filtred_horz_lines.items(), key=lambda x:tmp_nearstY[x[0]],reverse=True)))
        nearest_Y=new_data[0]
        #lets find the other lines which are near from the nearest_Y according to a threshold
        filtred_horz_lines={}
        for i in range(len(new_data)):
            new_data_y=new_data[i][1][1]+new_data[i][1][3]/2
            if abs((nearest_Y[1][1]+nearest_Y[1][3]/2)-new_data_y)<5:
                filtred_horz_lines[new_data[i][0]]=new_data[i][1]

    if len(filtred_horz_lines) !=0:
        width=max(filtred_horz_lines.keys())
        width_x0,width_y0,width_x1,width_y1=filtred_horz_lines[width]
    else:
        width=-1

    if len(filtred_vert_lines) !=0:
        height=max(filtred_vert_lines.keys())

    #classify the diagonals situated on the left and on the right of the picture
    diagL={}
    diagR={}
    if len(filtred_non_vert_lines) !=0:
        for k,v in filtred_non_vert_lines.items():
            x0,y0,x1,y1=v
            if x0<m/2 and x1<m/2:
                #the diagonal is situated on the left of the picture
                diagL[k]=v
            else:
                #the diagonal is situated on the right of the picture
                diagR[k]=v
    #retrieve the biggest vertical, horizontal and diagonal segments
    if len(diagL) !=0:
        depthL=max(diagL.keys())
        diagL_x0,diagL_y0,diagL_x1,diagL_y1=diagL[depthL]
    else:
        diagL=-1

    if len(diagR) !=0:
        depthR=max(diagR.keys())
        depthR_x0,depthR_y0,depthR_x1,depthR_y1=diagR[depthR]
    else:
        depthR=-1
    #cv2.line(img, (width_x0, width_y0), (width_x1,width_y1), (0,0,255), 1, cv2.LINE_AA)
    #cv2.imshow("Image", img)
    #cv2.waitKey(0)
    #cv2.destroyAllWindows()
    return [height,[width,width_x0,width_y0,width_x1,width_y1],[depthL,diagL_x0,diagL_y0,diagL_x1,diagL_y1],[depthR,depthR_x0,depthR_y0,depthR_x1,depthR_y1]]
#given a picture, appply the LSD algorithm, the same algo may be used for streaming, this is why we specify the picture based-on method 
def PictureLSDDetection(img):
    LSDDetection(img)

#retrieve the sensors's feedback
def get_variables_reply(r):
    global proxSensorsVal
    global resultSensors
    proxSensorsVal=r
    resultSensors='/'.join([str(v) for v in r])
    closeAsebamedulla()
    loop.quit()

#detect any poblem occured
def get_variables_error(e):
    print ('error:')
    print (str(e))
    loop.quit()

#close the asebamedulla session
def closeAsebamedulla():
    os.system("pidof asebamedulla > pidtmp")
    reader=open("pidtmp")
    os.system("kill "+reader.read())
    reader.close()
    os.system("rm pidtmp")

#read from the Thymio's sensors the estimated distances
def captureSensor():
    #get the values of the sensors
    network.GetVariable("thymio-II", "prox.horizontal",reply_handler=get_variables_reply,error_handler=get_variables_error)
    return False

#main method for initialize asebamedulla and capture the distances from the Thymio's sensors
def getDistanceFromSensors():
    global network
    global loop
    initAsebamedulla()
    sleep(3)
    parser = OptionParser()
    parser.add_option("-s", "--system", action="store_true", dest="system", default=False,help="use the system bus instead of the session bus")
    (options, args) = parser.parse_args()
 
    dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)
 
    if options.system:
        bus = dbus.SystemBus()
    else:
        bus = dbus.SessionBus()
 
    #Create Aseba network 
    network = dbus.Interface(bus.get_object('ch.epfl.mobots.Aseba', '/'), dbus_interface='ch.epfl.mobots.AsebaNetwork')
    loop = gobject.MainLoop()
    #call the callback of Braitenberg algorithm
    handle = gobject.timeout_add (100, captureSensor) #every 0.1 sec
    loop.run()
    #return the result from the Thymio's sensors containing the estimated distances
    return resultSensors

#ignoring reply
def dbusReply():
    pass
#if any error encountered, it is printed
def dbusError(e):
    print ('error %s')
    print (str(e))

#the robot's moving forward for loop (just one step)
def mvF():
    global cpTour
    #estimated time and speed to achieve the distance asked depending on the cpTour parameter
    t=500
    speed=50
    network2.GetVariable("thymio-II", "prox.horizontal",reply_handler=get_prox_reply,error_handler=get_variables_error)
    if loop2.is_running():
        while(t>0):
            network2.SetVariable("thymio-II", "motor.left.target", [speed])
            network2.SetVariable("thymio-II", "motor.right.target", [speed])
            t-=1

        network2.SetVariable("thymio-II", "motor.left.target", [0])
        network2.SetVariable("thymio-II", "motor.right.target", [0])
    cpTour+=1
    return True

#if any obstacle has been detected the robot stops moving and retrieves the estimated distances to the obstacle 
def get_prox_reply(r):
    global proxSensorsVal #list of sensors containing the distances to the obstacle
    global cpTour #a fixed parameter which helps to know the real distance traveled
    global distRob #the distance ordered to the robot to be traveled
    proxSensorsVal=r
    p=[]
    for v in r:
        p.append(str(v))
    if int(p[1])!=0 or int(p[2])!=0 or int(p[3])!=0 or cpTour>=distRob:
        #print('obstacle')
        loop2.quit()
        return 0
    #print('it s OK')
    return 1

#givien in centimeters the distance the robot has to travel (it may be not totally traveled because of an obstacle
#in that case, the real distane traveled is returned)
def moveForward(distCM):
    global network2
    global loop2
    global distRob #the distance ordered to the robot to be traveled
    global cpTour #a fixed parameter which helps to know the real distance traveled
    #init of a asebamedulla session
    initAsebamedulla()
    #wait for initialization
    sleep(3)
    distanceTravelled=0
    #convert the 
    distRob=29/50*distCM
    #initAsebamedulla()
    parser = OptionParser()
    parser.add_option("-s", "--system", action="store_true", dest="system", default=False,help="use the system bus instead of the session bus")
    (options, args) = parser.parse_args()
    dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)
    if options.system:
        bus = dbus.SystemBus()
    else:
        bus = dbus.SessionBus()
    #Create Aseba network 
    network2 = dbus.Interface(bus.get_object('ch.epfl.mobots.Aseba', '/'), dbus_interface='ch.epfl.mobots.AsebaNetwork')
    loop2 = gobject.MainLoop()
    #call the callback of Braitenberg algorithm
    handle = gobject.timeout_add (500, mvF) #every 0.1 sec
    loop2.run()
    #estimate the real distance traveled according to the speed the time ad the number of tour done
    distanceTravelled=(cpTour+1)*50/29
    #print("The distance travelled by the autonomous robot is: ",distanceTravelled)

    #save the real distanec traveled
    with open('data/distMove', 'w') as outfile:
        outfile.write(str(distanceTravelled))
        outfile.close()
    closeAsebamedulla()


#######################################
#testing

#PictureGFCornerDetection()
#PictureLSDDetection(takePicture())
#recognition(takePicture(),nbRefs=1)
#getDistanceFromSensors()
#moveForward(50)
