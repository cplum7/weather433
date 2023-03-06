# weather433
captures signal from 433Mhz temperature sensor and logs temperature data

This application intercepts, processes and decodes transmissions
from a Wittime WT2039A temperature and humidity wireless sensor
and optionally enters into a database the maximum and minimum 
temperatures for each hour of the day and the time of their occurrence.
The application, written in java, runs on mac os but should run
on other platforms as well.

Requirements for this project include:

![wittime img](/java/Weather433/readMe.rtfd/wittimeSensor.png)

Wittime temperature and humidity sensor WT2039A

![wittime img](/java/Weather433/readMe.rtfd/rtlSdr.png)

USB radio scanner dongle

rtl_fm command line utility (mac os version included in project files)
https://osmocom.org/projects/rtl-sdr/wiki

The following outlines the process used to decode the transmission
signal from the temperature sensor:

First a transmission from the sensor with the RTL.SDR
connected was captured:
rtl_fm -M am -f 433.92 -s 160000 > test527-81-1.raw
samples of the 433MHz frequency are taken at 160,000
per second and output as 16 bit signed integers to the raw
file.
The Audacity program
https://www.audacityteam.org/
can be used to graph the contents of the raw file

File>Import>raw data…
select raw file
In the displayed popup window set the options as follows:

![wittime img](/java/Weather433/readMe.rtfd/audacityOptions.png)

Audacity then graphs the sample data as show below:

![wittime img](/java/Weather433/readMe.rtfd/signalGraph.png)

Clicking the magnifying glass+ icon displays the following:

![wittime img](/java/Weather433/readMe.rtfd/dataPacketSignal.png)

See the comment section in the java source file Weather433.java
for interpretation of 41 data bits


more magnification showing sync and bits

![wittime img](/java/Weather433/readMe.rtfd/sensorData.png)

further magnification shows individual samples:

![wittime img](/java/Weather433/readMe.rtfd/sensorData2.png)

The included files in the webFiles folder will display the following from a browser:
(database access info needs to be entered in the file hostDB.txt)

![wittime img](/java/Weather433/readMe.rtfd/tempWeb.png)

Also included is an android app that will access the database of temperatures and display as follows:

![wittime img](/java/Weather433/readMe.rtfd/wuAndroid.png)

Both of the above apps contain bugs and any fixes are welcome

A problem with the Weather433 java app is that the calculation of the suspected
CRC data hasn’t been determined, resulting in the occasional erroneous signal not being
identified and providing an incorrect temperature
