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
	height,width,depth=LSDDetection(picturePath)
	with open('data/heightWall', 'w') as outfile:
		outfile.write(str(height))
		outfile.write("\n")
		outfile.write(str(width))
		outfile.write("\n")
		outfile.write(str(depth))
		outfile.close()