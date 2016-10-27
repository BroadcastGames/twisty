Twisty, a text-adventure interpreter for Android.
-------------------------------------------------


This is a fork of https://github.com/sussman/twisty with the intention of building on Android SDK 24 with source code layout of a new Android Studio 2.2.2 project.

1. The library was consolidated into the app tree. It was a clean branch of Java code not shared by any other projects and it was a shortcut taken in migrating to Android Studio. It should be easy enough to move it back to a library.
2. The menu did not appear when targeting SDK 24. A quick fix was to change the Twisty.java class from "Activity" to "AppCompatActivity". There may be more issues related to migration to newer SDK, but so far, this seemed to work.
3. Some default icons for Android projects may have been added by Android Studio. No effort was made to remove this.

The general goal was to get github to see files were 100% moved, unchanged, and not modified. The structure of the file directories is rather different on Android Studio from Eclipse and getting git to recognize them as renames makes the restructure smarter about the history. At least that was the theory.


ToDo
-------------------------------------------------
The NDK is not integrated with gradle currently.

NOTE: Android Studio 2.2.2 expects the lib files to be in app/src/main/jniLibs tree to be bundled into the APK. I currently manually copy them into that folder after a successful NDK compile.

