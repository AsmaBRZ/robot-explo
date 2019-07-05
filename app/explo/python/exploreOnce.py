# -*- coding: utf-8 -*-
'''
    File name: exploreOnce.py
    Author: Asma BRAZI
    Date created: 13/04/2019
    Date last modified: 22/04/2019
    Python Version: 3.6.7
'''
import sys
import os
from utils import *

if __name__=='__main__':
	#fix the resolution
	res1=600
	res2=400
	resultSensors=getDistanceFromSensors()
	picturePath=takePicture(res1,res2)
	height,W,L,R=LSDDetection(picturePath)
	width,width_x0,width_y0,width_x1,width_y1=W
	depthL,diagL_x0,diagL_y0,diagL_x1,diagL_y1=L
	depthR,depthR_x0,depthR_y0,depthR_x1,depthR_y1=R


	objects=recognition(picturePath)
	with open('data/VisualInfo', 'w') as outfile:
	    #add resolution
	    outfile.write(str(res1)+"/")
	    outfile.write(str(res2))
	    outfile.write("\n")
	    for o in objects:
	    	distance,objLocalization=o
	    	outfile.write(str(distance)+"/")
	    	#add distance to the wall
	    	#add obj's dimensions
	    	for d in objLocalization:
	    		outfile.write(str(d)+"/")
	    	outfile.write("\n")
	    	outfile.write(str(height))
	    	outfile.write("\n")
	    	outfile.write(str(width)+str("/")+str(width_x0)+str("/")+str(width_y0)+str("/")+str(width_x1)+str("/")+str(width_y1))
	    	outfile.write("\n")
	    	outfile.write(str(depthL)+str("/")+str(diagL_x0)+str("/")+str(diagL_y0)+str("/")+str(diagL_x1)+str("/")+str(diagL_y1))
	    	outfile.write("\n")
	    	outfile.write(str(depthR)+str("/")+str(depthR_x0)+str("/")+str(depthR_y0)+str("/")+str(depthR_x1)+str("/")+str(depthR_y1))
	    	outfile.write("\n")
	    	outfile.write(resultSensors)
	    outfile.close()