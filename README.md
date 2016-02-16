Raspberry Pi Camera Viewer
==========================

This program plays the raw H.264 video from a Raspberry Pi.

Use the <b>raspivid</b> program to generate the video stream and the <b>nc</b> program to send
the stream out over a specific port.

For example, to stream 1280x720 video at 15 frames per second over port 5001, you would do:

raspivid -n -ih -t 0 -rot 0 -w 1280 -h 720 -b 1000000 -fps 15 -o - | nc -lkv4 5001