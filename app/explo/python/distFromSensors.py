import dbus
import dbus.mainloop.glib
from gi.repository import GObject as gobject 
from optparse import OptionParser
 
proxSensorsVal=[0,0,0,0,0]
 
def captureSensor():
    #get the values of the sensors
    network.GetVariable("thymio-II", "prox.horizontal",reply_handler=get_variables_reply,error_handler=get_variables_error)
    return False
 
def get_variables_reply(r):
    global proxSensorsVal
    proxSensorsVal=r
    result='/'.join([str(v) for v in r])
    with open('data/horzDist', 'w') as outfile:
        outfile.write(str(result))
        outfile.close()
    loop.quit()
 
def get_variables_error(e):
    print ('error:')
    print (str(e))
    loop.quit()
 
if __name__ == '__main__':
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
    loop = gobject.MainLoop()
    #call the callback of Braitenberg algorithm
    handle = gobject.timeout_add (100, captureSensor) #every 0.1 sec
    loop.run()