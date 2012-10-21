# CreoleTouchPad
Android app which sends touch coordinates to a PC and receives vibration intensities.

## Why?
I make software that turns visual stuff into non-visual stuff. This application is designed to provide a simple way of using haptics (vibration) for some of that "other stuff".

For examples, check out [the Polyglot Framework](http://tdwright.github.com/Polyglot/), which contains an example of [a TouchPad receiver](https://github.com/tdwright/Polyglot/tree/master/TouchPad).

## How it works
CreoleTouchPad opens a sockets server and waits for a PC to connect to it. PC app will usually connect over USB using the Android Debug Bridge (ADB) to forward a local port to the Android device. Sending coordinates and receiving vibration intensities then takes place over the sockets connection.

## Credits
I use [SocketTest](http://sockettest.sourceforge.net/) to check that it's doing what I want it to.