# -*- coding: utf-8 -*-
import subprocess
import sys
import time
from threading import Thread
import os
from utils import *
#Les arguments de la commande doivent être dans l'ordre : 
#l'action à effectuer ('move' ou 'rotate'), la distance à parcourir. 
#Si il faut inverser le sens du mouvement on rajoute un 3e argument 'r'
#Les directions basiques étant vers l'avant (pour le move) et dans le sens des aiguilles d'une montre (pour rotate)

class AsebaThread(Thread):
    def __init__(self,cmd):
        Thread.__init__(self)
        self.cmd=cmd
    def run(self):
        os.system(self.cmd)
if __name__=='__main__':
    n=len(sys.argv)
    d=0
    speed=1      
    fName=sys.argv[1]
    withDist=sys.argv[2]
    rotationMove=sys.argv[3]
    d=float(sys.argv[4])
    if(n>5 and sys.argv[5] == 'r'):
        speed=-1

    fName+="D"

    with open(fName+".aesl","r+") as f:
        print(f)

        fl=f.readlines()
        f.seek(0)
        for i in fl:
            if "var distance" in i:
                print(i+"\n")
                print(str(int(d))+"\n")
                f.write("var distance="+str(int(d))+"\n")
            else:
                if "var reverse=" not in i:
                    f.write(i)
                else:
                    f.write("var reverse="+str(int(speed))+"\n")
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