# -*- coding: utf-8 -*-
import subprocess
import sys
import time
from threading import Thread
import os
from utils import *


class AsebaThread(Thread):
    def __init__(self,cmd):
        Thread.__init__(self)
        self.cmd=cmd
    def run(self):
        os.system(self.cmd)
if __name__=='__main__':
    threadAseba=AsebaThread('asebamassloader getData.aesl "ser:name=Thymio-II"')
    threadAseba.start()
    time.sleep(4)
    os.system("pidof asebamassloader > pidtmp")
    reader=open("pidtmp")
    os.system("kill "+reader.read())
    reader.close()
    os.system("rm pidtmp")