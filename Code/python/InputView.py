# -*- coding: utf-8 -*-
import numpy as np
import cv2
from matplotlib import pyplot as plt
import sys
import json
import math
import os
# from numpy import array
from scipy.cluster import *
class Point():
	def __init__(self, x, y ):
		self.x = x
		self.y = y
	def asTupleInt(self):
		return (int(self.x), int(self.y))

def getIntersectionPoint(l1u,l1v,l2u,l2v):
	l1u.x = float(l1u.x)
	l1u.y = float(l1u.y)
	l1v.x = float(l1v.x)
	l1v.y = float(l1v.y)
	l2u.x = float(l2u.x)
	l2u.y = float(l2u.y)
	l2v.x = float(l2v.x)
	l2v.y = float(l2v.y)
	# Equation de la droite : 
	if(l1v.x!=l1u.x):
		l1A = (l1v.y-l1u.y)/(l1v.x-l1u.x)
	else :
		l1A = 0.000001	
	l1B = l1v.y - l1A*l1v.x

	if(l2v.x-l2u.x):
		l2A = (l2v.y-l2u.y)/(l2v.x-l2u.x)
	else :
		l2A = 0.000001
	if l1A== l2A : return None
	l2B = l2v.y - l2A*l2v.x
	# print("l1A ", l1A)
	# print("l1B ", l1B)
	# print("l2A ", l2A)
	# print("l2B ", l2B)
	# point d'intersection resolution
	a = np.array ( ( (l1A, -1.0), (l2A,-1.0) ))
	b = np.array ( (-l1B, -l2B) )
	interx, intery = np.linalg.solve(a,b)
	return Point(interx,intery)

class Line():
	def __init__(self,U,V):
		self.U = U
		self.V = V

class ObjectInView():
	def __init__(self, props,):
		self.properties = props
		self.bg, self.bd = Point(self.properties["bg"][0], self.properties["bg"][1]),Point(self.properties["bd"][0], self.properties["bd"][1])
		self.hg, self.hd = Point(self.properties["hg"][0], self.properties["hg"][1]),Point(self.properties["hd"][0], self.properties["hd"][1])
		oCenterX = (self.bg.x+self.hg.x+self.bd.x+self.hd.x)/4.0
		oCenterY = (self.bg.y+self.hg.y+self.bd.y+self.hd.y)/4.0
		self.center = Point(oCenterX, oCenterY)
		self.__computeAxis()
		self.__computeIntersectionWithBorder()
		self.xAxisAngle = abs(math.asin(self.xAxis[1])*180.0/np.pi)
		self.yAxisAngle = abs(math.asin(self.yAxis[1])*180.0/np.pi)

	def __computeAxis(self):
		leftV = np.array([self.bg.x-self.hg.x, self.bg.y-self.hg.y])
		leftNC= leftV/np.linalg.norm(leftV,ord=2)

		rightV = np.array([self.bd.x-self.hd.x, self.bd.y-self.hd.y])
		rightNC= rightV/np.linalg.norm(rightV,ord=2)

		topV = np.array([self.hd.x-self.hg.x, self.hd.y-self.hg.y])
		topNC= topV/np.linalg.norm(topV,ord=2)

		bottomV = np.array([self.bd.x-self.bg.x, self.bd.y-self.bg.y])
		bottomNC= bottomV/np.linalg.norm(bottomV,ord=2)	

		self.xAxis = [(topNC[0]+bottomNC[0])/2.0, (topNC[1]+bottomNC[1])/2.0]
		self.yAxis =[(leftNC[0]+rightNC[0])/2.0, (leftNC[1]+rightNC[1])/2.0]	
			
	def __computeIntersectionWithBorder(self):
		self.left_inter = getIntersectionPoint(self.bg,self.hg, self.center, Point(self.xAxis[0]*100.0+self.center.x, self.xAxis[1]*100.0+self.center.y))
		self.right_inter = getIntersectionPoint(self.bd,self.hd, self.center, Point(self.xAxis[0]*100.0+self.center.x, self.xAxis[1]*100.0+self.center.y))
		self.top_inter = getIntersectionPoint(self.hd,self.hg, self.center, Point(self.yAxis[0]*100.0+self.center.x,self.yAxis[1]*100.0+self.center.y))
		self.bottom_inter = getIntersectionPoint(self.bd,self.bg, self.center, Point(self.yAxis[0]*100.0+self.center.x,self.yAxis[1]*100.0+self.center.y))

class InputView():
	def __init__(self, imgURL):
		if(not os.path.exists(imgURL)): 
			raise FileNotFoundError("L'image n'a pas été trouvée")
		self.image = cv2.imread(imgURL)
		self.imGray = cv2.cvtColor(self.image, cv2.COLOR_BGR2GRAY)
		self.keyPoints = dict()
		self.sift = cv2.xfeatures2d.SIFT_create(nfeatures= 0, sigma=1)
		self.keyPoints["kp"], self.keyPoints["des"] = self.sift.detectAndCompute(self.imGray,None)
		self.edges = cv2.Canny(self.imGray,100,800,apertureSize = 5)
		self.lines = cv2.HoughLinesP(self.edges,rho = 1,theta = 1*np.pi/180.0,threshold =30,minLineLength = 70,maxLineGap = 10)
	
	def findObject(self, database, imgDataBaseURL, showProcess=False):
		""" input image -> returns a dict(doorId, position, height, width) if a door from the database was found, None if nothing was found """
		#piece of code inspired by OpenCV tutorial :
		#https://opencv-python-tutroals.readthedocs.io/en/latest/py_tutorials/py_feature2d/py_feature_homography/py_feature_homography.html
		MIN_MATCH_COUNT = 10 #at least 10 matching points to considere its the same
		# pip install opencv-contrib-python (openCV3+)
		if self.keyPoints["kp"] != []:
			bestMatchObjectCount = 0
			bestMatchID = None
			results = {}
			for sceneObj in database.values():
				objIm = cv2.cvtColor(cv2.imread(imgDataBaseURL+sceneObj["img"]), cv2.COLOR_BGR2GRAY)
				if(objIm.shape[0]*objIm.shape[1] > 2000000):
					raise ValueError("L'image-objet entrée"+sceneObj["img"]+":"+sceneObj["title"]+" est trop grande")
				kp2, des2 = self.sift.detectAndCompute(objIm,None) #points d'interets de l'objet à trouver
				if(kp2 != []):
					#FLANN =  Fast Library for Approximate Nearest Neighbors
					FLANN_INDEX_KDTREE = 0
					index_params = dict(algorithm = FLANN_INDEX_KDTREE, trees = 10)
					search_params = dict(checks = 50)
					flann = cv2.FlannBasedMatcher(index_params, search_params)
					matches = flann.knnMatch(self.keyPoints["des"],des2,k=2)
					# store all the good matches as per Lowe's ratio test.
					good = []
					for m,n in matches:
						if m.distance < 0.7*n.distance:
							good.append(m)

					if len(good)>MIN_MATCH_COUNT :
						src_pts = np.float32([ self.keyPoints["kp"][m.queryIdx].pt for m in good ]).reshape(-1,1,2)
						dst_pts = np.float32([ kp2[m.trainIdx].pt for m in good ]).reshape(-1,1,2)	
						M, mask = cv2.findHomography(src_pts, dst_pts, cv2.RANSAC,5.0)
						matchesMask = mask.ravel().tolist()
						results[sceneObj["id"]] = {"kp": kp2, "matches":good}
						if len(good)> bestMatchObjectCount: # Il y a suffisament de points qui correspondent = door found
							h2, w2 = objIm.shape
							boundingPts = np.float32([ [0,0],[0,h2-1],[w2-1,h2-1],[w2-1,0] ]).reshape(-1,1,2) # [haut gauche, bas gauche, bas droite, haut droite]
							contour = cv2.perspectiveTransform(boundingPts,np.linalg.inv(M))
							
							if(self.isShapeValid(contour)): # on regarde si les points forment bien un quadrilatèr
								bestMatchObjectCount = len(good)
								bestMatchID = sceneObj["id"]

							if(showProcess):
								img4 = cv2.polylines(self.imGray,[np.int32(contour)],True,(255,255,255),3) 
								img3 = cv2.drawMatches(img4,self.keyPoints["kp"],objIm,kp2,good,None)
								plt.imshow(img3, 'gray'),plt.show()						

			if bestMatchObjectCount != 0 :
				good = results[bestMatchID]["matches"]
				kp2 = results[bestMatchID]["kp"]

				objIm = cv2.cvtColor(cv2.imread(imgDataBaseURL+database[str(bestMatchID)]["img"]), cv2.COLOR_BGR2GRAY)
				src_pts = np.float32([ self.keyPoints["kp"][m.queryIdx].pt for m in good ]).reshape(-1,1,2)
				dst_pts = np.float32([ kp2[m.trainIdx].pt for m in good ]).reshape(-1,1,2)	

				M, mask = cv2.findHomography(src_pts, dst_pts, cv2.RANSAC,5.0)
				matchesMask = mask.ravel().tolist()

				h2, w2 = objIm.shape
				boundingPts = np.float32([ [0,0],[0,h2-1],[w2-1,h2-1],[w2-1,0] ]).reshape(-1,1,2) # [haut gauche, bas gauche, bas droite, haut droite]
				contour = cv2.perspectiveTransform(boundingPts,np.linalg.inv(M))
				
				objProps =  database[str(bestMatchID)]
				objProps["hg"] = contour[0][0].tolist()
				objProps["bg"]	= contour[1][0].tolist()
				objProps["hd"]	= contour[3][0].tolist()
				objProps["bd"]	= contour[2][0].tolist()
				objectFound = ObjectInView(objProps)

				if showProcess:
					#DISPLAY 
					# trace le contour de la porte
					img4 = cv2.cvtColor(img4,  cv2.COLOR_GRAY2BGR)
					self.imGray = cv2.polylines(self.image,[np.int32(contour)],True,(255,0,0),3)
					
					draw_params = dict(matchColor = (0,255,0), # draw matches in green color
								singlePointColor = None,
								matchesMask = matchesMask, # draw only inliers
								flags = 2)
					#trace les matches
					img4 = cv2.drawMatches(img4,self.keyPoints["kp"],objIm,kp2,good,None,**draw_params)
					# plt.imshow(img3, 'gray'),plt.show()
					cv2.imshow('1', img4)
					#########
				return objectFound
			else:
				print ("aucun match dans la bdd")
				return None
		else : 
			print("aucun point d'interet detecté dans l'image d'entrée")
			return None

	def isShapeValid(self, contour):
		# on regarde si le quadrilatère est convexe et si les longueurs des cotés sont relativement proches 2 à deux (on n'aura pas de perspectives incroyables + sift trouverait pas)
		p1 = np.array(contour[0][0]) # haut gauche 
		p2 = np.array(contour[3][0]) # haut droite
		p3 = np.array(contour[2][0]) # bas droite 
		p4 = np.array(contour[1][0])

		#print(p1, p2, p3, p4)
		p12 = p2 - p1 
		p23 = p3 - p2
		p43 = p3 - p4
		p14 = p4 - p1

		n12 = np.sqrt(p12[0]**2.0 + p12[1]**2.0 )
		n23 = np.sqrt(p23[0]**2.0 + p23[1]**2.0 )
		n34 = np.sqrt(p43[0]**2.0 + p43[1]**2.0 )
		n14 = np.sqrt(p14[0]**2.0 + p14[1]**2.0 )

		a123 = (np.pi - np.arccos( np.dot(p12, p23) / (n12*n23)))*180.0/np.pi
		a234 = (np.arccos( np.dot(p23, p43) / (n23*n34)))*180.0/np.pi
		a341 = (np.pi - np.arccos( np.dot(p43, p14) / (n34*n14)))*180.0/np.pi
		a412 = (np.arccos( np.dot(p14, p12) / (n14*n12)))*180.0/np.pi

		ma = 20 #angle min
		return ma<a123<180.0 and ma<a234<180.0 and ma<a341<180.0 and ma<a412<180.0 and (min(n12, n34)/max(n12, n34) > 0.5) and (min(n14, n23)/max(n14, n23) > 0.5)
		
	def getLinesOnPlane(self, o):
		atBottom = []
		atTop = []
		atLeft = []
		atRight = []
		floorLine, ceilLine = None, None
		for l in self.lines:
			[x1, y1, x2, y2] = l[0]
			if(x1 == x2) : x2 += 1
			U, V = Point(x1,y1) , Point(x2,y2)
			vectNorme = math.sqrt(math.pow((V.x-U.x),2.0) + math.pow((V.y-U.y),2.0))
			sinLine = (V.y-U.y)/vectNorme
			cosLine = (V.x-U.x)/vectNorme
			degAngleLine = abs(math.asin(sinLine)*180.0/np.pi)	
			#  Sur le plan de l'objet
			w = 3  # deg de liberté en degrés
			if(o.xAxisAngle-w<degAngleLine < o.xAxisAngle+w ): # orientation de la ligne proche de celle de l'axe horizontal de l'objet
				# On veut maintenant connaître la position de la ligne par rapport à l'objet en élagant les lignes corespondant aux limites de l'objet 
				inter = getIntersectionPoint(U,V, o.center, Point(o.yAxis[0]*100.0+o.center.x,o.yAxis[1]*100.0+o.center.y))
				newline = [U.x,U.y,V.x,V.y, int(inter.x), int(inter.y)]
				if(inter.y > o.bottom_inter.y): # en bas de l'objet
					# Est ce que cette ligne passe bien sous l'objet?
					atBottom += [newline]
				elif(inter.y-o.center.y < o.top_inter.y-o.center.y):  # en haut de l'objet
					atTop += [newline]
			elif (o.yAxisAngle-w<degAngleLine < o.yAxisAngle+w ): # orientation de la ligne proche de celle de l'axe vertical de l'objet
				inter = getIntersectionPoint(Point(o.xAxis[0]*100.0+o.center.x, o.xAxis[1]*100.0+o.center.y), o.center, U, V)
				newline = [U.x,U.y,V.x,V.y, int(inter.x), int(inter.y)]
				if(inter.x < o.left_inter.x): # à gauche de l'objet
					atLeft += [newline]
				elif(inter.x> o.right_inter.x):  # à droite de l'objet
					atRight += [newline]
		
		atBottom = np.array(atBottom)
		atTop = np.array(atTop)
		atLeft = np.array(atLeft)
		atRight = np.array(atRight)

		if(atBottom!=[]):
			sDHeightBottom = np.std(atBottom[:,5])
			meanHeightBottom = np.mean(atBottom[:,5])
			bottomLinePoints = []
			
			for l in atBottom:
				if(abs(l[5]-meanHeightBottom)<sDHeightBottom): # ecart à la moyenne inferieur à lecart type : on est sur une ligne a peu pres dans le cluster
					bottomLinePoints += [[l[0],l[1]], [l[2],l[3]], [l[4], l[5]]]

			if(bottomLinePoints!=[]):
				bottomLinePoints = np.array(bottomLinePoints)	
				[a,b] = np.polyfit(bottomLinePoints[:,0], bottomLinePoints[:,1],1)
				xBl1 = np.min(bottomLinePoints[:,0])
				yBl1 = a*xBl1+b
				xBl2 = np.max(bottomLinePoints[:,0])
				yBl2 = a*xBl2+b
				floorLine= [a,b]
			else : 
				print("pas de ligne à globaliser")
		
		if(atTop!=[]):
			sDHeightTop = np.std(atTop[:,5])
			meanHeightTop = np.mean(atTop[:,5])
			topLinePoints = []
			for l in atTop:
				if(abs(l[5]-meanHeightTop)<sDHeightTop): # ecart à la moyenne inferieur à lecart type : on est sur une ligne a peu pres dans le cluster
					topLinePoints += [[l[0],l[1]], [l[2],l[3]], [l[4], l[5]]]
			if(topLinePoints!=[]):
				topLinePoints = np.array(topLinePoints)	
				[a,b] = np.polyfit(topLinePoints[:,0], topLinePoints[:,1],1)
				xBl1 = np.min(atTop[:,0])
				yBl1 = a*xBl1+b
				xBl2 = np.max(atTop[:,2])
				yBl2 = a*xBl2+b
				ceilLine = [a,b]
			else : 
				print("pas de ligne à globaliser")

		return atBottom, atTop, atLeft,atRight, floorLine, ceilLine