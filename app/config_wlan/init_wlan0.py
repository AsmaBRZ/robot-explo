'''
    File name: init_wlan0.py
    Author: Asma BRAZI
    Date created: 17/05/2019
    Python Version: 3.6.7
'''
import os
os.system("sudo su")
os.system("wpa_supplicant -i wlan0 ")
os.system("wpa_supplicant -i wlan0 -c /etc/wpa_supplicant/wpa_supplicant.conf ")
os.system("bg")
os.system("dhclient wlan0")
os.system("exit")
