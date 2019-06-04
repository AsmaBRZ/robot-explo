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
	picturePath=takePicture(res1,res2)
	#all pictures must have .png as extension 
	if sys.argv[3]=='true':
		height=HoughLinesP(picturePath)
		#print(height)
		with open('data/heightWall', 'w') as outfile:
			outfile.write(str(height))
			outfile.close()

	objects=recognition(picturePath,ref=sys.argv[1],multi=sys.argv[2])
	with open('data/distanceCaptured', 'w') as outfile:
		#add resolution
		outfile.write(str(res1)+"/")
		outfile.write(str(res2))
		outfile.write("\n")
		for o in objects:
			distance,objLocalization=o
			#add distance to the wall
			outfile.write(str(distance)+"/")
			#add obj's dimensions
			for d in objLocalization:
				outfile.write(str(d)+"/")
			outfile.write("\n")
		outfile.close()
	 