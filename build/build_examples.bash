#!/bin/bash


ant -q -f sound_detector.xml
ant -q -f utter_asterisk.xml
ant -q -f pitch_detector.xml
ant -q -f percussion_detector.xml
ant -q -f goertzel_algorithm.xml
ant -q -f time_stretch.xml
ant -q -f spectrogram.xml
ant -q -f delay_effect.xml
ant -q -f advanced_audio_player.xml