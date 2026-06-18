![TarsosDSP build status](https://github.com/JorenSix/TarsosDSP/actions/workflows/gradle.yml/badge.svg)

# TarsosDSP

TarsosDSP is a Java library for audio processing. Its aim is to provide an easy-to-use interface to practical music processing algorithms implemented, as simply as possible, in pure Java and without any other external dependencies. The library tries to hit the sweet spot between being capable enough to get real tasks done but compact and simple enough to serve as a demonstration of how DSP algorithms work.

TarsosDSP features an implementation of a percussion onset detector and a number of **pitch detection algorithms**: YIN, the Mcleod Pitch method and a “Dynamic Wavelet Algorithm Pitch Tracking” algorithm. Also included is a **Goertzel DTMF** (Dual tone multi frequency) decoding algorithm, a time stretch algorithm (WSOLA), resampling, **filters**, simple synthesis, some **audio effects**, and a pitch shifting algorithm.

To show the capabilities of the library, [TarsosDSP example applications](http://0110.be/tag/TarsosDSP) are available. Head over to the [TarsosDSP release directory](http://0110.be/releases/TarsosDSP/) for freshly baked binaries and code smell free (that is the goal anyway), oven-fresh sources.

## Quickly Getting Started with TarsosDSP

The source code of TarsosDSP is compatible with Java 11.

Add this to your **build.gradle** file:

```groovy
repositories {
    maven {
        name = "TarsosDSP repository"
        url = "https://mvn.0110.be/releases"
    }
}
dependencies {
    implementation 'be.tarsos.dsp:core:2.5'
    implementation 'be.tarsos.dsp:jvm:2.5'
}
```

Add this to your **pom.xml** file:

```xml
<repository>
  <id>be.0110.repo-releases</id>
  <name>0110.be repository</name>
  <url>https://mvn.0110.be/releases</url>
</repository>
<dependency>
  <groupId>be.tarsos.dsp</groupId>
  <artifactId>core</artifactId>
  <version>2.5</version>
</dependency>
<dependency>
  <groupId>be.tarsos.dsp</groupId>
  <artifactId>jvm</artifactId>
  <version>2.5</version>
</dependency>
```

## Citing TarsosDSP

Some information about TarsosDSP can be found in the paper [_TarsosDSP, a Real-Time Audio Processing Framework in Java_](http://0110.be/files/attachments/411/aes53_tarsos_dsp.pdf), by Joren Six, Olmo Cornelis, and Marc Leman, in _Proceedings of the 53rd AES Conference (AES 53rd)_, 2014. If you use TarsosDSP in academic research, please cite this paper.

```bibtex
@inproceedings{six2014tarsosdsp,
  author      = {Joren Six and Olmo Cornelis and Marc Leman},
  title       = {{TarsosDSP, a Real-Time Audio Processing Framework in Java}},
  booktitle   = {{Proceedings of the 53rd AES Conference (AES 53rd)}},
  year        =  2014
}
```

### Building TarsosDSP with Gradle

To build TarsosDSP from source the `gradlew` script should get you started. The following commands fetch the source and builds the library:

```bash
git clone --depth 1 https://JorenSix@github.com/JorenSix/TarsosDSP.git
cd TarsosDSP
./gradlew build
```

## Source Code Organization & Developing

The library is separated into three module folders: the main core functionality is found in `core/src/main/java`, TarsosDSP example applications are found in `examples/src/main/java`, and JVM audio I/O in `jvm/src/main/java`.

## TarsosDSP Examples

To see TarsosDSP in action there are a number of examples. These examples have either a graphical user interface or a command line interface. To see a window in which you can start all GUI examples the jar file needs to be started without arguments:

```bash
gradle shadowJar
java -jar examples/build/libs/examples-all.jar
```

To use the command line examples, execute the jar with the example name as first argument. For example `java -jar examples/build/libs/examples-all.jar feature_extractor pitch audio.mp3` starts the `feature_extractor` application to extract pitch from an audio file.

To get a list of all command line examples: `java -jar examples/build/libs/examples-all.jar list`

## Credits

[TarsosDSP](http://0110.be/tag/TarsosDSP) was developed at [University College Ghent, School of Arts](http://schoolofartsgent.be) between 2009 and 2013, from late 2013 the project is supported by [University Ghent, IPEM](http://www.ipem.ugent.be).

TarsosDSP borrows algorithms from various other libraries and research papers. Below, a complete list of credits can be found.

- The onset detector implementation is based on a [VAMP plugin example](http://vamp-plugins.org/code-doc/PercussionOnsetDetector_8cpp-source.html) by _Chris Cannam_ at Queen Mary University, London. The method is described in [Drum Source Separation using Percussive Feature Detection and Spectral Modulation](http://eprints.nuim.ie/699/1/ELE-Bob9.pdf) by Dan Barry, Derry Fitzgerald, Eugene Coyle and Bob Lawlor, ISSC 2005.
- For the implementation of the YIN pitch tracking algorithm. Both the [the YIN paper](http://recherche.ircam.fr/equipes/pcm/cheveign/ps/2002_JASA_YIN_proof.pdf) and the GPL'd [aubio implementation](http://aubio.org) were used as a reference. _Matthias Mauch_ (of Queen Mary University, London) kindly provided the FastYin implementation which uses an FFT to calculate the difference function, it makes the algorithm up to 3 times faster.
- The Average Magnitude Difference (AMDF) pitch estimation algorithm is implemented by _Eder Souza_ and adapted for TarsosDSP by myself.
- For the MPM pitch tracking algorithm, the paper titled [A Smarter Way To Find Pitch](http://miracle.otago.ac.nz/tartini/papers/A_Smarter_Way_to_Find_Pitch.pdf) by _Philip McLeod_ and _Geoff Wyvill_ was used.
- The Dynamic Wavelet pitch estimation algorithm is described in [Real-Time Time-Domain Pitch Tracking Using Wavelets](http://online.physics.uiuc.edu/courses/phys193/NSF_REU_Reports/2005_reu/Real-Time_Time-Domain_Pitch_Tracking_Using_Wavelets.pdf) by Eric Larson and Ross Maddox. The implementation within TarsosDSP is based on the implementation in the [Dynamic Wavelet Algorithm Pitch Tracking library](http://www.schmittmachine.com/dywapitchtrack.html) by _Antoine Schmitt_, which is released under the MIT open source license, a license compatible with the GPL.
- The audio time stretching algorithm is described in [An Overlap-Add Technique Based on Waveform Similarity (WSOLA) For High Quality Time-Scale Modifications of Speech](<http://mir.cs.nthu.edu.tw/users/litbee/RelatedPaper/[WSOLA]An%20overlap-add%20technique%20based%20on%20waveform%20similarity%20(WSOLA)%20for%20high-quality%20time-scale%20modifications%20of%20speech.pdf>) by Werner Verhelst and Marc Roelands. As a reference implementation the WSOLA implementation by _Olli Parviainen_ in the LGPL [SoundTouch - an open-source audio processing library](http://www.surina.net/soundtouch/) was used.
- The FFT implementation used within TarsosDSP is by _Piotr Wendykier_ and is included in his GPL'd [JTransforms library](https://sites.google.com/site/piotrwendykier/software/jtransforms). JTransforms is the first, open source, multithreaded FFT library written in pure Java.
- The sample rate conversion feature is implemented by _Laszlo systems_ in the GPL'd [libresample4j](https://github.com/dnault-laszlo/libresample4j) library. libresample4j is a Java port of Dominic Mazzoni's libresample 0.1.3, which is in turn based on [Julius Smith's Resample 1.7 library](http://www-ccrma.stanford.edu/~jos/resample/)
- Various FFT window functions are done by Damien Di Fede and Corban Brook for the GPL'd [Minim](http://code.compartmental.net/tools/minim/) project.
- Beat induction based on onsets and saliences is done using code from [Simon Dixon's BeatRoot system](http://www.eecs.qmul.ac.uk/~simond/beatroot/). The software is licensed under the GPL. The algorithm is documented in the 2001 JNMR paper [_Automatic Extraction of Tempo and Beat From Expressive Performances_](http://www.tandfonline.com/doi/abs/10.1076/jnmr.30.1.39.7119#.UbcsROcW0r0) and in the 2007 JNMR article [_Evaluation of the Audio Beat Tracking System BeatRoot_](http://www.tandfonline.com/doi/full/10.1080/09298210701653310#.UbcsgecW0r0)
- A complex domain onset detection function is implemented using Aubio as an inspiration. Aubio, by _Paul Brossier_, contains very clean object-oriented C code, the cleanest C code I have ever seen. The algorithm is described in [_Complex Domain Onset Detection for Musical Signals_](http://www-student.elec.qmul.ac.uk/people/juan/Documents/Duxbury-DAFx-2003.pdf) by Christopher Duxbury, Mike E. Davies, and Mark B. Sandler, in Proceedings of the Digital Audio Effects Conference, DAFx-03, pages 90-93, London, UK, 2003
- An implementation of the Constant-Q transform by Karl Helgason for the GPL'd [RasmusDSP](http://rasmusdsp.sourceforge.ne) project has been adapted for use in TarsosDSP. More information about the Constant-Q transform can be found in the following papers [_Calculation of a Constant Q Spectral Transform_](http://www.wellesley.edu/Physics/brown/pubs/cq1stPaper.pdf) by Judith C. Brown, [_An Efficient Algorithm for the Calculation of a Constant Q Transform_](http://www.wellesley.edu/Physics/brown/pubs/effalgV92P2698-P2701.pdf), by Judith C. Brown and Miller S. Puckette, and [_The Constant Q Transform_](http://wwwmath1.uni-muenster.de/logik/org/staff/blankertz/constQ/constQ.pdf) by Benjamin Blankertz
- The Haar Wavelet Transform implemented in TarsosDSP is based on the pseudocode found in _Wavelets Made Easy_ by Yves Nievergelt.
- The lifting scheme wavelet package is developed by [Ian Kaplan](http://www.bearcave.com/software/java/wavelets/basiclift.html) and is based on the concepts explained in _Ripples in Mathematics: The Discrete Wavelet Transform_ by A. Jensen and A. la Cour-Harbo, Springer. It is only slightly modified for easy use in TarsosDSP
- The frequency domain pitch shifter is developed by [Stephan M. Bernsee](http://www.dspdimension.com/admin/pitch-shifting-using-the-ft/Transformbasiclift.html) and is based on the concepts explained in _Pitch Shifting Using The Fourier Transform_. For the moment it is a rather direct translation of the C implementation. It was released under a 'Wide Open License'. As the name of the license suggests, it is a license without much restrictions.
- The granulator is developed for [the beads project](http://www.beadsproject.net/). It was released under a GPL-license.

## Changelog

### Version 1.0 (2012-04-24)
First release which includes several pitch trackers and a time stretching algorithm, amongst other things. Downloads and javadoc API can be found at the [TarsosDSP release directory](http://0110.be/releases/TarsosDSP/)

### Version 1.1 (2012-06-4)
Changed how the audio dispatcher stops. Added StopAudioProcessor.
Added FastYin implementation by Matthias Mauch
Added AMDF pitch estimator by Eder Souza

### Version 1.2 (2012-08-21)
Modified the interface of PitchDetector to return a more elaborate result structure with pitch, probability and a boolean "is pitched".
Added an implementation of an envelope follower or envelope detector.

### Version 1.3 (2012-09-19)
TarsosDSP can do audio synthesis now. The first simple unit generators are included in the library.
It has a new audio feature extraction feature, implemented in the FeatureExtractor example.
Added ASCII-art to the source code (this is the main TarsosDSP 1.3 feature).

### Version 1.4 (2012-10-31)
Included a resample feature, implemented by libresample4j. Together with the WSOLA implementation, it can be used for pitch shifting (similar to Phase Vocoding). A pitch shifting example (both with a CLI and a UI) is added in the 1.4 version of the TarsosDSP library as well.

### Version 1.5 (2013-04-30)
Converted TarsosDSP to maven. This is known as the Malaryta-release. The "Malaryta" release is provided to you by [RikkiMongoose](http://github.com/rikkimongoose) (idea, documents, git things) and [Ultar](http://github.com/ultar) (converting to maven, refactoring). Malaryta is the capital of Malaryta Raion, Brest Region in the Republic of Belarus. Both developers spent their childhood in Brest, and think that the title Malaryta is as strange as Ubuntu or Whistler. The 1.5 release also includes various FFT window functions from the cool [Minim project](http://code.compartmental.net/tools/minim/) by Damien Di Fede.

### Version 1.6 (2013-06-12)
This release features practical onset and beat detection algorithms. A complex domain onset detection and a spectral flux onset detection algorithm are added. This release also includes a way to guess a beat from onsets. Parts of the [BeatRoot system](http://www.eecs.qmul.ac.uk/~simond/beatroot/), by Simon Dixon, are included to this end. Also included in this release is an implementation of the Constant-Q transform.

### Version 1.7 (2013-10-08)
This release adds the ability to extract the MFCC from an audio signal. Also an example of the Constant-Q transform is added, together with a reusable visualization class library. The build system is reverted back to pure ANT

### Version 1.8 (2014-04-10)
With this release it is possible to extract spectral peaks from an FFT and get precise frequency estimates using phase info. An example application called SpectralPeaks is added as well.

### Version 1.9 (2014-08-10)
This release includes a Haar Wavelet Transform and an example of an audio compression algorithm based on Haar Wavelets. It also includes a significant change in package naming.

### Version 2.0 (2014-08-13)
The 2.0 version is worth the major version update since it offers out-of-the-box support for Android. The release has no more dependencies on the parts of the java runtime that are not included in Android. To offer this support some packages have been shifted around. The code that does I/O is dependent on the runtime (JVM or Dalvik) and is abstracted using the `be.tarsos.dsp.io` package.

### Version 2.1 (2015-03-03)
The 2.1 version restructures some of the source files. All source can now be found in src. The ant build file is adapted to reflect this change. This version also includes an STFT pitch shifter. There was already a time domain pitch shifter included and now a frequency domain implementation is present as well.

### Version 2.2 (2015-03-03)
The 2.2 version includes a new `AudioDispatcher`. It has been reviewed thoroughly and now behaves predictably for the first and last buffers as well. To prevent compatibility issues the version has been changed.

### Version 2.3 (2015-09-01)
The 2.3 version includes improved Android support: [audio decoding on Android](http://0110.be/posts/Decode_MP3s_and_other_Audio_formats_the_easy_way_on_Android) can be done using a provided, statically compiled ffmpeg binary. The ffmpeg decoding functionality for JVM has been improved as well. If no ffmpeg executable is found it is downloaded automatically from [here](http://0110.be/releases/TarsosDSP/TarsosDSP-static-ffmpeg).

### Version 2.4 (2016-12-01)
Some small changes to the pipe decoder. Now it is possible to set a start and duration for the incoming decoded audio from ffmpeg.

### Version 2.5 (2023-01-09)
Changes to the build system: moved from ant to gradle. Better documentation, testing and CI with github actions. Release of maven packages.
