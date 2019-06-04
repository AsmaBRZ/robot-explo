# -*- coding: utf-8 -*-
"""
Created on Mon Jun 27 15:11:24 2016

@author: Lucie
"""

import cv2
import numpy as np
import math

def rectangles(name):
    im = cv2.imread(name)
    imgray = cv2.cvtColor(im,cv2.COLOR_BGR2GRAY)

    #seuillage
    th3 = cv2.adaptiveThreshold(imgray,255,cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY,11,2)

    #contours
    cont, hierarchy = cv2.findContours(th3,cv2.RETR_TREE,cv2.CHAIN_APPROX_SIMPLE)
    cv2.drawContours(th3, cont, -1, (255,0,0), 1)
    #cv2.imshow('contours',th3)
    
    contours = sorted(cont, key = cv2.contourArea, reverse = True)[:10]
    cv2.drawContours(im, contours, -1, (255,0,0), 1)

    i=0
    rect=[]

    for c in contours:
        i=i+1
        
        #on recupere les dimension du plus grand rectangle (dimensions de l'image)
        if i==1:
            x,y,w,h = cv2.boundingRect(c)
            cv2.rectangle(im,(x,y),(x+w,y+h),(0,255,0),2)
            height_img = h
            width_img = w
            
        #on ajoute tous les autres a notre liste
        else :
            x,y,w,h = cv2.boundingRect(c)
            cv2.rectangle(im,(x,y),(x+w,y+h),(0,255,0),2)
            rect.append([x, y, w, h])

    cv2.imshow("Rectangles", im)
    cv2.waitKey(0)
    cv2.destroyAllWindows()
    
    return [height_img, width_img, rect]




def getKey(item):
    return item[0]

def points(name):
    
    img = cv2.imread(name)
    
    gray = cv2.cvtColor(img,cv2.COLOR_BGR2GRAY)

    #détection des contours
    edges = cv2.Canny(gray ,50, 150, apertureSize = 3)

    #détection des lignes
    minLineLength = 100
    maxLineGap = 10
    lines = cv2.HoughLinesP(edges, 1, np.pi/180, 100, minLineLength, maxLineGap)

    #matrice contenant les segments de fuites
    matrix=[]

    print(len(lines))
    for i in range(len(lines)):
        #on filtre pour ne garder que les lignes non verticales ou horizontales
        for x1, y1, x2, y2 in lines[i] :

            dx = x2 - x1
            dy = y2 - y1

            print(dx," ", dy)
            #élimine les droites verticales et horizontale
            if dx != 0 and dy != 0 :

                a = float(dy) / float(dx)
                b = y1 - (a*x1)

                alpha = math.degrees(math.atan(math.fabs(a)))
                beta = math.degrees(math.atan(math.fabs(1/a)))

                if alpha > 4 and beta > 4 :
                    cv2.line(img, (x1,y1), (x2,y2), (0,255,0), 2)

                    AB = math.sqrt(math.pow(dx, 2) + math.pow(dy, 2))
                    matrix.append([AB, a, b])

    print(matrix)
    #on trie la matrice pour avoir les t-uplet avec les plus long segment en premier
    sorted(matrix, key=getKey, reverse=True)
    
    AB1, a1, b1 = matrix.pop()
    
    #liste contenant les points de fuites
    points=[]

    for [AB, a, b] in matrix :
    
        if (a1>0 and a<0) or (a1<0 and a>0) :
            da = math.fabs( a1 - a )
            db = math.fabs( b1 - b )
    
            #condition sur da permet de ne pas prendre en compte des droites parrallèles
            if da > 0.1 and db != 0:
        
                x = int( float(db) / float(da) )
                y = int( a1*x + b1 )
        
                cv2.line(img, (x, y), (x, y), (255, 0, 0), 5)
            
                points.append([x, y])
            

    #cv2.imwrite('houghlines5.jpg',img)
    cv2.imshow('Canny Hough',img)
    cv2.waitKey(0)
    cv2.destroyAllWindows()
    
    return points
  
  
def goodOne(rect, points):
    
    height_rect, width_rect = 0, 0
    find = 0
    
    #on prend le premier point
    for (xp,yp) in points :
        
        if find == 0 :
            
            #on regarde si il est contenu dans un des rectangle
            for (x, y, w, h) in rect :
            
                #si oui on renvoie les dimensions de ce rectangle
                if x<xp<x+w and y<yp<y+h :
                
                    height_rect = h
                    width_rect = w
                
                    #print height_rect, width_rect
                
                    find = 1
                
                    break
    
    return height_rect, width_rect
    
    

points("C:/Users/Alexandre/Desktop/Codes/2019PANDROIDE2/Code/ressources/inputImages/imgTest2.jpg")
#points('corridor-1.jpg')