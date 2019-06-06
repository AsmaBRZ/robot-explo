# -*- coding: utf-8 -*-
#Rotates thymio clockwise from the distance given in arguments. If the second argument is 'r', it goes anticlockwise
import dbus
import dbus.mainloop.glib
import gobject
import sys
import time
from optparse import OptionParser
import os.path
 
proxSensorsVal=[0,0,0,0,0,0,0]
micI=0
temperature=0
speed=50
realSpeed=0
#pathToSensorDataFile='../../SensorDataThymio/'
pathToSensorDataFile=''
dbArray=dbus.Array([])
distance=40
reverse=1

def dbusReply():
    pass

def dbusError(e):
    print 'error %s'
    print str(e)

def Braitenberg():
    global distance
    #get the values of the sensors
    network.GetVariable("thymio-II", "prox.horizontal",reply_handler=get_prox_reply,error_handler=get_variables_error)
    
    #print the proximity sensors value in the terminal
    #print proxSensorsVal[0],proxSensorsVal[1],proxSensorsVal[2],proxSensorsVal[3],proxSensorsVal[4]
 
    #Parameters of the Braitenberg, to give weight to each wheels
    leftWheel=[-0.01,-0.005,-0.0001,0.006,0.015]
    rightWheel=[0.012,+0.007,-0.0002,-0.0055,-0.011]
 
    #Braitenberg algorithm
    totalLeft=0
    totalRight=0
    for i in range(5):
         totalLeft=totalLeft+(proxSensorsVal[i]*leftWheel[i])
         totalRight=totalRight+(proxSensorsVal[i]*rightWheel[i])
 
    #add a constant speed to each wheels so the robot moves always forward
    totalRight=reverse*(totalRight+speed)
    totalLeft=reverse*(totalLeft+speed)
    
 
    #print in terminal the values that is sent to each motor
    print "totalLeft"
    print totalLeft
    print "totalRight"
    print totalRight
 
    #send motor value to the robot
    network.SetVariable("thymio-II", "motor.left.target", [totalLeft])
    network.SetVariable("thymio-II", "motor.right.target", [totalRight*-1])    

    realSpeed=totalLeft
    
    network.GetVariable("thymio-II","motor.left.speed",reply_handler=get_speed_reply,error_handler=get_variables_error)
    print dbArray
    for i in dbArray:
        if type(i)==type(dbus.Int16(0)):
            print "trouve la valeur : "
            print int(i)
            realSpeed=int(i)
    #On retire la distance parcourue (proprtionnelle a l'unite de mesure du thymio), *0.1 car la boucle est iteree toutes les 0.1 secondes
    distance-=reverse*(realSpeed)*36/48*0.1*0.5
    print distance
    if(distance<1):
        network.SetVariable("thymio-II", "motor.left.target", [0])
        network.SetVariable("thymio-II", "motor.right.target", [0])
        loop.quit()
        return False
    return True

    
        
    #network.SendEventName('event1', [32,0,32,0,32,0,32,0], reply_handler = dbusReply ,error_handler=dbusError)
        
    
 
def get_speed_reply(r):
    global dbArray
    dbArray=r

def get_prox_reply(r):
    global proxSensorsVal
    proxSensorsVal=r
    
def get_variables_reply(r):
    global proxSensorsVal
    proxSensorsVal=r
 
def get_variables_error(e):
    print 'error:'
    print str(e)
    loop.quit()

def writeData():
    global proxSensorsVal
    #global temperature
    #global micI
    global pathToSensorDataFile
    t=pathToSensorDataFile+'proxSensor.csv'
    nLines=sum(1 for line in open(t))

    s=str(nLines)+";"
    for j in range(7):
        s+=str(proxSensorsVal[j])+";"
    s=s[:-1]
    s+="\n"

    #variables.append(temperature)
    #variables.append(micI)
    f = open(t,"a") 
    f.write(s)  
    f.close()
 
if __name__ == '__main__':
    t=pathToSensorDataFile+'proxSensor.csv'
    if(not os.path.exists(t)):
        f = open(t,'w') 
        f.write("#;fLeft;fMidLeft;fMid;fMidRight;fRight;bLeft;bRight\n")  
        f.close()
        
    n=len(sys.argv)
    
    #Gets distance (argument 1)
    if (n>1):
    	distance=float(sys.argv[1])

    #Gets direction
    if(n>2 and sys.argv[2] == 'r'):
	    reverse=-1
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
    #print in the terminal the name of each Aseba NOde
    print network.GetNodesList()
    #GObject loop
    print 'starting loop'
    loop = gobject.MainLoop()
    #call the callback of Braitenberg algorithm
    handle = gobject.timeout_add (100, Braitenberg) #every 0.1 sec
    loop.run()
    print "rotate done"
    
    writeData()
    print "data written"
