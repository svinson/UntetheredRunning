UntetheredRunning Capstone Project 2012-2014
============================================

Last updated 3/16/2014.

Code Status
===========

The current version of the app is in the color-blob-detection folder.
It is based on the openCV sample color blob detection app.
The app should be run on Android 4.3 with openCV 2.4.8.
The app will work best on a Nexus 4, but there are instructions to generalize the algorithm for other devices.

Problems
========

The marker detection was sometimes affected by lighting conditions (common to most color detection techniques).
The app sometimes detected background objects instead of the marker (implies HSV threshold range was too big, marker color was chosen poorly, or shape detection should be added).
At running speeds, the phone bouncing causes the marker to move all around the frame, triggering bad commands. This makes the app difficult but not impossible to use at high speeds.

Hardware Problems
=================

The phone provided by capstone has a broken speaker, so an external speaker or headphones must be used.
The phone would crash and restart unpredictably (possibly caused by the app, but undetermined).

Suggestions
===========

Change thresholds to dynamic values so the app will work on other phones.
Manually calibrate HSV thresholds if using same marker detection algorithm.
Alter harness to reduce bouncing at running speeds.
Add timer so commands don't overlap.
Improve tracking algorithm. (It works pretty well already.) (Consider drawing a state diagram.)

Link to Design Docs / Coursework
================================

https://drive.google.com/folderview?id=0By9tbBZD9ZebUHk0VFg2VEJyVXc&usp=sharing
(read-only)

Contact
=======

sterlingvinson@gmail.com
stuartweickgenant@gmail.com
funst0r@gmail.com