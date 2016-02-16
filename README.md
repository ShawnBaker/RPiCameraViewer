# RPi Camera Viewer

## Copyright and License

Copyright (C) 2016 Shawn Baker using the [MIT License](https://opensource.org/licenses/MIT).

Raspberry image by [Martin Bérubé](http://www.how-to-draw-funny-cartoons.com),
all other images by [Oxygen Team](http://www.oxygen-icons.org).

## Raspberry Pi Instructions

This program plays the raw H.264 video from a Raspberry Pi.

Use the **raspivid** program to generate the video stream and the **nc** program to send
the stream out over a specific port.

For example, to stream 1280x720 video at 15 frames per second over port 5001, you would do:

```
raspivid -n -ih -t 0 -rot 0 -w 1280 -h 720 -b 1000000 -fps 15 -o - | nc -lkv4 5001
```
