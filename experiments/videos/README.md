# How to use the videos for reproducing the tests

Paperclickers development mode allows using a video to playback a detection scenario.

## Videos over 50 MB

Since github does not allow files greater than 50 MB, the videos for the entire class and occlusion detection experiments have been packed into two big **".tar.gz"** files which where then split into up to 50 MB files.

To recover those videos locally in your machine, use the commands bellow:

```
cat entire_class_detection_videos.tar.gz.part-* > entire_class_detection_videos.tar.gz

cat occlusion_detection_videos.tar.gz.part-* > occlusion_detection_videos2.tar.gz
```

To make sure everything is fine, the MD5SUM for the complete **".tar.gz"** files are bellow:

```
fb103e89581feb73825d98adb12f6876  entire_class_detection_videos.tar.gz

a9e8bccf76f172dd13d1150ab32188b0  occlusion_detection_videos.tar.gz
```


## How to use paperclickers video playback feature

* Copy the videos you want to use inside the phone, normally at the path

```
	/sdcard/PaperClickers
```

* Rename the video you want to use first as **"testVideo.mp4"** - the file opened by the emulation engine. If you have enough space inside your device, it might be easier if you make a copy of the desired video, naming it **"testVideo.mp4"**, hence you can easily switch the videos.

* Enable development mode inside paperclickers app, quickly touching 5 times the **"paperclickers"** name at the first screen.

* Enter the **"Settings"** option and enable the **"Use camera emulation from file"** option.


After that, once you enter the capture screen, the video **"testVideo.mp4"** will playback instead of the camera preview.