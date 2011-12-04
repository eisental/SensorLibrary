SensorLibrary
=============

a [RedstoneChips](http://eisental.github.com/RedstoneChips) circuit library containing sensor chips.

#### Circuits in library
- pirsensor - a PIR (Passive Infra-red) heat sensor for detecting living entities within a radius.
- photocell - Detect the average light level around the chip's interface blocks.
- daytime - Can output all kinds of real or game time measurements.
- rangefinder - An ultrasonic transceiver for detecting the exact distance between the chip and anything.
- beacon - Can force map chunks to stay loaded and monitors whether chunks in a region are loaded or not. 
- slotinput - Input decimal numbers by choosing an inventory slot and clicking an interface block.

__For much more information, see the [circuitdocs](http://eisental.github.com/RedstoneChips/circuitdocs).__

Installation
-------------
* Make sure you have the core [RedstoneChips](http://eisental.github.com/RedstoneChips) plugin installed in your craftbukkit plugins folder.
* Download the latest [SensorLibrary jar file](https://github.com/downloads/eisental/SensorLibrary/SensorLibrary-beta.jar).
* Copy jar file into the plugins folder of your craftbukkit installation.

Changelog
---------
#### SensorLibrary 0.3 (4/12/11)
- New spark chip. 
- New self-triggering vehicleid and playerid sensor chips.
- Rangefinder direction is determined by two interface blocks instead of interface+noteblock. The extra interface block is also part of structure.
- Many changes to the rangefiner: Added a optional size argument on the third line. Format WxH ex: 2x3 or W&H ex: 1 Changed Detection Methods (by @Cutch . Thanks!)
- slotinput will reset its output levers on init.
- photocell requires at least 1 interface block.

#### SensorLibrary 0.25 (23/04/11)
- Updated to work with RedstoneChips 0.9.
- New slotinput circuit for decimal number input according to the selected inventory slot. Made by @Shamebot!
- New beacon circuit for forcing chunks in a region to stay loaded or for monitoring whether a region of chunks is loaded or unloaded.
- Removed annoying console spam made by the photocell circuit. 

#### SensorLibrary 0.24 (07/04/11)
- New daytime timefields - SECOND1, SECOND10, MINUTE1, MINUTE10, HOUR1 and HOUR10 - for sending 1s and 10s digits of the output directly from the daytime circuit. Thanks @zach-hinchy.
- Fixed daytime output scaling when the number of output bits is less than required for the selected time field.
- pirsensor is now using a faster algorithm for finding living entities in radius.

#### SensorLibrary 0.23 (10/03/11)
- daytime can output current time in other worlds. Coded by @dashkal.
- Updated to work with RedstoneChips 0.84.

#### SensorLibrary 0.22 (07/03/11)
- Updated to work with RedstoneChips 0.83.

#### SensorLibrary 0.21 (03/03/11)
- daytime can output game minutes and some more changes to the time field argument. Check the docs.

#### SensorLibrary 0.2 (28/02/11)
- Updated to work with latest craftbukkit and RC0.82
- New daytime circuit
- New rangefinder circuit

#### SensorLibrary 0.11 (14/02/11)
- Fixed bug in pirsensor.
- Updated to work with RedstoneChips 0.8

#### SensorLibrary 0.1 (7/02/11)
- First release with 2 circuits: photocell and pirsensor.
