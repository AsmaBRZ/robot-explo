import os
os.system("sudo su")
os.system("wpa_supplicant -i wlan0 ")
os.system("wpa_supplicant -i wlan0 -c /etc/wpa_supplicant/wpa_supplicant.conf ")
os.system("bg")
os.system("dhclient")
os.system("exit")
