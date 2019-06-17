# -*- coding: utf-8 -*-
#Runs thymio forward to the speed given until the time (given) is reached. 
#If the third argument is 'r', it goes backwards
import dbus
import dbus.mainloop.glib
import gobject
import sys
import time
from optparse import OptionParser
 
proxSensorsVal=[0,0,0,0,0,0,0]
micI=0
temperature=0
#pathToSensorDataFile='../../SensorDataThymio/'

pathToSensorDataFile=''
def dbusReply():
    pass

def dbusError(e):
    print 'error %s'
    print str(e)

#Parameters = Time and Speed
def Braitenberg(t,s):
    #get the values of the sensors
    network.GetVariable("thymio-II", "prox.horizontal",reply_handler=get_prox_reply,error_handler=get_variables_error)
    network.GetVariable("thymio-II", "mic.intensity",reply_handler=get_mic_reply,error_handler=get_variables_error)
    network.GetVariable("thymio-II", "temperature",reply_handler=get_temp_reply,error_handler=get_variables_error)
 
    #print the proximity sensors value in the terminal
    print proxSensorsVal[0],proxSensorsVal[1],proxSensorsVal[2],proxSensorsVal[3],proxSensorsVal[4]
    
    while(t>0):
        network.SendEventName('event1', [32,0,32,0,32,0,32,0], reply_handler = dbusReply ,error_handler=dbusError)
        network.SetVariable("thymio-II", "motor.left.target", [speed])
        network.SetVariable("thymio-II", "motor.right.target", [speed])
        t-=1
        time.sleep(1) 
    
    network.SetVariable("thymio-II", "motor.left.target", [0])
    network.SetVariable("thymio-II", "motor.right.target", [0])
    
    return False
   
 
def get_prox_reply(r):
    global proxSensorsVal
    proxSensorsVal=r

def get_mic_reply(r):
    global micI
    micI=r

def get_temp_reply(r):
    global temperature
    temperature=r
 
def get_variables_error(e):
    print 'error:'
    print str(e)
    loop.quit()

def writeData():
    global proxSensorsVal
    global temperature
    global micI
    global pathToSensorDataFile
    titles=["frontLeft","frontMiddleLeft","frontMiddle","frontMiddleRight","frontRight","backLeft","backRight","temperatureSensor","micIntensity"]
    variables=[]
    for j in range(7):
        variables.append(proxSensorsVal[j])

    variables.append(temperature)
    variables.append(micI)
    i=0
    for t in titles:
        t=pathToSensorDataFile+t+'.txt'
        f = open(t,”w”) 
        f.write(variables[i])      
        f.close() 
        i+=1
 
if __name__ == '__main__':
    n=len(sys.argv)
    time=0
    if (n>2):
    	time=float(sys.argv[1])
        speed=float(sys.argv[2])
        
	if(n>3 and sys.argv[3] == 'r'):
		speed*=-1


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
    Braitenberg(time,speed)
    print "moved"

    writeData()
    print "data written"
