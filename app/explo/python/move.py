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
	if len(sys.argv)>1:
		distCM=float(sys.argv[1])
		moveForward(distCM)
	else:
		print('The distance to travel is required in the arguments')
	
