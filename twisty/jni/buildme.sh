echo "Twisty NDK 12b build and deploy"
/spot/WorkDev0/devPlace0/android_dev0/android-ndk-r12b/ndk-build
rsync -aAXv --progress ../libs/ ../../app/src/main/jniLibs/
rm -r ../../build/
rm -r ../../app/build/
