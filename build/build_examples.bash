#!/bin/bash

#build sound detector
ant -q -f sound_detector.xml

#build Utter Asterisk
ant -q -f utter_asterisk.xml

ant -q -f pitch_detector.xml

ant -q -f percussion_detector.xml