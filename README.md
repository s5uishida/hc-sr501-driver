# hc-sr501-driver
hc-sr501-driver is a java library that operates pir motion detector sensor called [HC-SR501](https://www.mpja.com/download/31227sc.pdf) to connect HC-SR501 to GPIO terminal of Raspberry Pi 3B and make it for use in java.
I releases this in the form of the Eclipse plug-in project.
You need Java 8 or higher.

I use [Pi4J](https://pi4j.com/)
for gpio communication in java and have confirmed that it works in Raspberry Pi 3B ([Raspbian Buster Lite OS](https://www.raspberrypi.org/downloads/raspbian/) (2019-07-10)).

## Connection of HC-SR501 and Raspberry Pi 3B
**Connect with `Power <--> Vin`,`GND <--> GND`, `OUT <--> PWM`.**
- `Pins` of [HC-SR501](https://www.mpja.com/download/31227sc.pdf)
  - Power
  - GND
  - OUT
- [GPIO of Raspberry Pi 3B](https://www.raspberrypi.org/documentation/usage/gpio/README.md)
  - Vin --> (2) or (4)
  - GND --> (6), (9), (14), (20), (25), (30), (34) or (39)
  - PWM --> (12) GPIO18, (35) GPIO19, (32) GPIO12 or (33) GPIO13
  
## Install Raspbian Buster Lite OS (2019-07-10)
The reason for using this version is that it is the latest as of July 2019 and [BlueZ](http://www.bluez.org/) 5.50 is included from the beginning, and use Bluetooth and serial communication simultaneously.

## Configuration of Raspbian Buster Lite OS
- Edit `/boot/cmdline.txt`
```
console=serial0,115200 --> removed
```
- Edit `/boot/config.txt`
```
@@ -45,7 +45,7 @@
 # Uncomment some or all of these to enable the optional hardware interfaces
 #dtparam=i2c_arm=on
 #dtparam=i2s=on
-#dtparam=spi=on
+dtparam=spi=on
 
 # Uncomment this to enable the lirc-rpi module
 #dtoverlay=lirc-rpi
@@ -55,6 +55,10 @@
 # Enable audio (loads snd_bcm2835)
 dtparam=audio=on
 
+enable_uart=1
+dtoverlay=pi3-miniuart-bt
+core_freq=250
+
 [pi4]
 # Enable DRM VC4 V3D driver on top of the dispmanx display stack
 dtoverlay=vc4-fkms-v3d
```
When editing is complete, reboot.

## Install WiringPi Native Library
Pi4J depends on the [WiringPi](http://wiringpi.com/) native library by Gordon Henderson.
The Pi4J native library is dynamically linked to WiringPi.
```
# apt-get update
# apt-get install wiringpi
```
When using with Raspberry Pi 4B, install the latest version as follows.
Please refer to [here](http://wiringpi.com/wiringpi-updated-to-2-52-for-the-raspberry-pi-4b/).
```
# wget https://project-downloads.drogon.net/wiringpi-latest.deb
# dpkg -i wiringpi-latest.deb
```
Please make sure it’s version 2.52.
```
# gpio -v
gpio version: 2.52
```
**Note. In October 2019, I have not confirmed the official information that can use Raspberry Pi 4B with Pi4J and WiringPi.
I've simply checked that it works, but some problems may occur.**

## Install jdk11 on Raspberry Pi 3B
For example, [jdk11 apt-install](https://apt.bell-sw.com/) at [BELLSOFT](https://bell-sw.com/) is shown below.
```
# wget -q -O - https://download.bell-sw.com/pki/GPG-KEY-bellsoft | apt-key add -
# echo "deb [arch=armhf] https://apt.bell-sw.com/ stable main" | tee /etc/apt/sources.list.d/bellsoft.list
# apt-get update
# apt-get install bellsoft-java11
```

## Install git
If git is not included, please install it.
```
# apt-get install git
```

## Use this with the following bundles
- [SLF4J 1.7.26](https://www.slf4j.org/)
- [Pi4J 1.2 (pi4j-core.jar)](https://github.com/s5uishida/pi4j-core-osgi)

I would like to thank the authors of these very useful codes, and all the contributors.

## How to use
The following sample code will be helpful.
```java
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin

import io.github.s5uishida.iot.device.hcsr501.driver.HCSR501Driver;
import io.github.s5uishida.iot.device.hcsr501.driver.IHCSR501Handler;

public class MyHCSR501 {
    private static final Logger LOG = LoggerFactory.getLogger(MyHCSR501.class);
    
    public static void main(String[] args) {
        HCSR501Driver hcsr501 = HCSR501Driver.getInstance(RaspiPin.GPIO_12, new MyHCSR501Handler());
        hcsr501.open();

//      if (hcsr501 != null) {
//          hcsr501.close();
//      }
    }
}

class MyHCSR501Handler implements IHCSR501Handler {
    private static final Logger LOG = LoggerFactory.getLogger(MyHCSR501Handler.class);

    private static final String dateFormat = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);

    @Override
    public void handle(String pinName, boolean detect, Date date) {
        LOG.info("[{}] {} {}", pinName, detect, sdf.format(date));
    }
}
```
