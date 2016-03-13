# Copyright © 2016 Shawn Baker using the MIT License.

import termios, sys, os, subprocess
from subprocess import Popen, PIPE
TERMIOS = termios

port = 5001
width = 1920
height = 1080
cameras = ('192.168.0.180', '192.168.0.181', '192.168.0.182', '192.168.0.183', '192.168.0.184')
camera = 2
nc = None
hv = None

def getKey():
        fd = sys.stdin.fileno()
        old = termios.tcgetattr(fd)
        new = termios.tcgetattr(fd)
        new[3] = new[3] & ~TERMIOS.ICANON & ~TERMIOS.ECHO
        new[6][TERMIOS.VMIN] = 1
        new[6][TERMIOS.VTIME] = 0
        termios.tcsetattr(fd, TERMIOS.TCSANOW, new)
        c = None
        try:
                c = os.read(fd, 1)
        finally:
                termios.tcsetattr(fd, TERMIOS.TCSAFLUSH, old)
        return c

def stopCamera():
	global nc, hv
	if nc is not None:
		nc.kill()
		nc = None
	if hv is not None:
		hv.kill()
		hv = None

def startCamera():
	global nc, hv, port, width, height, cameras, camera
	address = cameras[camera]
	print 'nc ' + address + ' ' + str(port) + ' | ./hello_video.bin 0 0 ' + str(width) + ' ' + str(height)
	stopCamera()
	nc = subprocess.Popen(('nc', address, str(port)), stdout=subprocess.PIPE)
	hv = subprocess.Popen(('/home/pi/hello_video.bin', '0', '0', str(width), str(height)), stdin=nc.stdout)

if camera >= 0 and camera < len(cameras):
	startCamera()
while True:
	key = getKey()
	if key >= '1' and key <= '9':
		index = int(key) - 1
		if index != camera and index < len(cameras):
			camera = index
			startCamera()
	elif key == 'q':
		stopCamera()
		break
