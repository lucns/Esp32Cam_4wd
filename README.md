# Esp32Cam_4wd

This is a WiFi controlled mini car/robot using an Android App. In the same, the Esp32-Cam is used using UDP protocol to transfer the camera frames and the communication between the app and the MCU.

The camera is 2Mpixel Ov2640. 25fps at 800x600 size frame.
Data rate is over 200-700Kbps in 25fps at 800x600 size frame. The android uses the bitmap encoder and show frames in screen, received by UDP protocol.
WiFi signal at >-80dbm, framerate is slow... 5-15fps using Esp32 board antenna.

The eletronic circuit was design in KiCad EDA - Schematic Capture & PCB Design Software.
Board is burned in TOTEM 25 Laser Engraver.

App was developed in Java.

Tested in Galaxy A70 Android 11 & Arduino IDE.

![4wd_preview](https://user-images.githubusercontent.com/16022034/230798034-165acbf6-13e2-4ac6-9023-fda98a3277aa.jpg)
![app_preview](https://user-images.githubusercontent.com/16022034/230798040-5a017fa0-9976-49f5-8fd9-339535067b3f.jpg)
