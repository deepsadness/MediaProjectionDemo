# Android PC投屏简单尝试系列

## 简单说明
- Android PC投屏简单尝试
通过MediaProjection和VirtualDisplay,使用ImageReader来获取屏幕的截屏，通过WebSocket传输，在网页端监听，进行投屏。
具体代码见 [`screenop` module](https://github.com/deepsadness/MediaProjectionDemo/tree/master/screenop) 和 [MainActivity](https://github.com/deepsadness/MediaProjectionDemo/blob/master/app/src/main/java/com/cry/mediaprojectiondemo/MainActivity.kt)

- Android PC投屏简单尝试2—录屏直播
通过MediaProjection和VirtualDisplay,使用MediaCodec将从Surface获取的数据进行编码，通过RTMP协议进行推流。
具体代码见 [`codec` module](https://github.com/deepsadness/MediaProjectionDemo/tree/master/codec)和[`rtmp` module](https://github.com/deepsadness/MediaProjectionDemo/tree/master/rtmp)

## 博客文章
### 简书位置
- [Android PC投屏简单尝试](https://www.jianshu.com/p/ce37330365f2) 
- [Android PC投屏简单尝试2—录屏直播](https://www.jianshu.com/p/6dde380d9b1e)

### github位置
- [Android PC投屏简单尝试](https://github.com/deepsadness/MediaProjectionDemo/blob/master/blog/article_imagereader_socket.md)
- [Android PC投屏简单尝试2—录屏直播](https://github.com/deepsadness/MediaProjectionDemo/blob/master/blog/article_codec_rtmp.md)

