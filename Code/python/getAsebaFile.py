# -*- coding: utf-8 -*-
import sys
from utils import *
#Les arguments de la commande doivent être dans l'ordre : 
#l'action à effectuer ('move' ou 'rotate'), le temps limite, la vitesse. 
#Si il faut inverser le sens du mouvement on rajoute un 4e argument 'r'



if __name__=='__main__':
    n=len(sys.argv)
    speed=0
    t=0
    fName="move"
          
    if(n>3):
        fName=sys.argv[1]
        withDist=sys.argv[2]
        rotationMove=sys.argv[3]
        t=float(sys.argv[5])
        speed=float(sys.argv[4])
        if(n>4 and sys.argv[6] == 'r'):
            speed*=-1


    with open(fName+".aesl","r+") as f:
          d=f.readlines()
          f.seek(0)
          for i in d:
            if "if time&lt;" in i:
                f.write("if time&lt;"+str(int(t))+" then\n")
            else:
                if "var speed" not in i:
                    f.write(i)
                else:
                    f.write("var speed="+str(int(speed))+"\n")
          f.truncate()
          
    #launch the associated script
        f="move.aesl"
        if withDist=="True":
            f="moveD.aesl"
        if rotationMove =="True":
            f="rotate.aesl"
            if withDist=="True":
                f="rotateD.aesl"
    
    threadAseba=AsebaThread('asebamassloader '+f+' "ser:name=Thymio-II"')
    threadAseba.start()
    time.sleep(4)
    os.system("pidof asebamassloader > pidtmp")
    reader=open("pidtmp")
    os.system("kill "+reader.read())
    reader.close()
    os.system("rm pidtmp")
    
          
