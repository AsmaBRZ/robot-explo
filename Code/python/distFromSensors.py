# -*- coding: utf-8 -*-
import dbus
import dbus.mainloop.glib
import gobject
import time
import sys
from optparse import OptionParser
import os.path

micI=0
temperature=0
pathToSensorDataFile=''
proxSensorsVal=[0,0,0,0,0,0,0]
realSpeed=0
dbArray=dbus.Array([])
speed=100
distance=40
reverse=1
def Braitenberg():
    global distance
    #get the values of the sensors
    network.GetVariable("thymio-II", "prox.horizontal",reply_handler=get_prox_reply,error_handler=get_variables_error)

    #print the proximity sensors value in the terminal
    print proxSensorsVal[0],proxSensorsVal[1],proxSensorsVal[2],proxSensorsVal[3],proxSensorsVal[4],proxSensorsVal[5],proxSensorsVal[6]
    
    #Parameters of the Braitenberg, to give weight to each wheels
    leftWheel=[-0.01,-0.005,-0.0001,0.006,0.015]
    rightWheel=[0.012,+0.007,-0.0002,-0.0055,-0.011]
 
    #Braitenberg algorithm
    totalLeft=0
    totalRight=0
    for i in range(5):
         totalLeft=totalLeft+(proxSensorsVal[i]*leftWheel[i])
         totalRight=totalRight+(proxSensorsVal[i]*rightWheel[i])
 
    #print in terminal the values that is sent to each motor
    print "totalLeft"
    print totalLeft
    print "totalRight"
    print totalRight
 
def get_prox_reply(r):
    global proxSensorsVal
    proxSensorsVal=r
 
def get_variables_error(e):
    print 'error:'
    print str(e)
    loop.quit()

if __name__ == '__main__':
    
    parser = OptionParser()
    parser.add_option("-s", "--system", action="store_true", dest="system", default=False,help="use the system bus instead of the session bus")
 
    (options, args) = parser.parse_args()
 
    dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)

    if "DISPLAY" not in os.environ:
	os.environ['DISPLAY']=':0'
 
    if options.system:
        bus = dbus.SystemBus()
    else:
        try:
            bus = dbus.SessionBus()
        except dbus.DBusException as exc:
            raise RuntimeError(exc.get_dbus_message())
 
    #Create Aseba network 
    network = dbus.Interface(bus.get_object('ch.epfl.mobots.Aseba', '/'), dbus_interface='ch.epfl.mobots.AsebaNetwork')
 
    #print in the terminal the name of each Aseba NOde
    print network.GetNodesList()
 
    #GObject loop
    print 'starting loop'
    loop = gobject.MainLoop()
    #call the callback of Braitenberg algorithm
    handle = gobject.timeout_add (100, Braitenberg) #every 0.1 sec

    print "data written"
    
