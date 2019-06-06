from picamera import PiCamera
from time import sleep
import os.path
import os
import datetime
from PIL import Image

t='imgData.csv'
if(not os.path.exists(t)):
    f = open(t,'w')
    s="#;"
    b=""
    for i in range(800):
        for j in range(600):
            b="["+str(i)+","+str(j)+"]"
            s+="R"+b+";"+"V"+b+";"+"B"+b+";"
    s=s[:-1]
    s+="\n"
    f.write(s)  
    f.close()





urlDirectory="/home/pi/VisualNav"
imgName=datetime.datetime.now().strftime('%H%M%S%d%m')

if not os.path.exists(urlDirectory):
    os.makedirs(urlDirectory)           
#https://projects.raspberrypi.org/en/projects/getting-started-with-picamera/6



urlImg=urlDirectory+'/'+imgName+'.jpg'

camera = PiCamera()
camera.resolution = (800, 600)
camera.start_preview()
sleep(2)
camera.capture(urlImg)
camera.stop_preview()


#The print will be retrieved by the main pgm (to have access to the picture)
print(urlImg)

t='imgData.csv'
nLines=sum(1 for line in open(t))

s=str(nLines)+";"
    
f = open(t,"a") 
im=Image.open(urlImg)
pix=im.load()
s=""
for i in range(100):
    for j in range(100):        
        for n in range(3):
            s+=str(pix[i,j][n])+";"
s=s[:-1]
s+="\n"
f.write(s)
f.close()
