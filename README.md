LanXchange
==========

A simple tool for spontaneous, local network file transfers. Supports Windows, Mac and Linux PCs and Android phones.

Features
--------

* No setup required
* Automatically detects files offered by other computers and displays them
* Can transfer single files, folders or a combination of both
* Allows transfers between different platforms/operating systems
* Transfers files as fast as your home network allows to

Requirements
------------

* Runs on Windows, OS X and Linux PCs with Java 7 installed.
* Runs on Android >= 4.0 (Ice Cream Sandwich)

Getting started
---------------

1. Download LanXchange:
  * [PC (Win/Mac/Linux/JVM)](https://github.com/tfg13/LanXchange/raw/master/releases/stable/lxc.zip)
  * [Android](https://play.google.com/store/apps/details?id=de.tobifleig.lxc)
2. Unzip the downloaded archive to some place in your home folder (like "My Documents")
3. Run it by double-clicking "LXC.exe" (Windows) or "lxc" (OS X, Linux)
4. Repeat these steps for all computers and start sharing!

Q&A
---

Q: *Aren't file transfers a solved problem?*  
A: Not entirely. This tool was written years ago for fast file transfers on Lan-Parties (hence the name), where we had a mix of Windows 98 and Windows XP PCs, and copying files was **not** solved back then.

Q: *Ok, but why would anyone use this **today**, we have {$cloudprovider, AirDrop, ...}?*  
A: LanXchange is much faster than the cloud (no slow upload involved) and, unlike AirDrop does support virtually all PC OSes + Android. (anything that can run a JVM)

Q: *But why Java?*  
A: Historical reasons, I was in school when I started this and only knew Java. Plus, years later it made porting LanXchange to Android much easier.

Q: *What about those automatic updates?*  
A: Those are recommended because updates frequently break compatibility with older versions. You can disable them, but there should be no need: On every launch, LanXchange *checks* for updates (by reading [this file](http://updates.lanxchange.com/v)), but **never** downloads or installs anything automatically. Also, all updates are signed.

License
-------

LanXchange is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

LanXchange is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with LanXchange. If not, see <http://www.gnu.org/licenses/>.


Copyright (c) 2009, 2010, 2011, 2012, 2013, 2014, 2015 Tobias Fleig - All rights reserved
