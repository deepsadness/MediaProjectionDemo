# Android PC投屏简单尝试2—录屏直播
代码地址 ：https://github.com/deepsadness/MediaProjectionDemo

## 想法来源
上一边文章的最后说使用录制的Api进行录屏直播。本来这边文章是预计在5月份完成的。结果过了这么久，终于有时间了。就来填坑了。

## 主要思路
- 直接使用硬件编码器进行录制直播。
- 使用rtmp协议进行直播推流

![使用MediaProjection示意图.png](https://upload-images.jianshu.io/upload_images/1877190-91e54bd04670cd75.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

整体流程就是通过创建VirtualDisplay,并且直接通过MediaCodec的Surface直接得到数据。通过MediaCodec得到编码完成之后的数据，进行 flv格式的封装，最后通过rtmp协议进行发送。

--- 
### 获取屏幕的截屏
#### 1. 使用MediaCodec Surface
这部分基本上和上一遍文章相同，不同的就是使用MediaCodec来获取Surface
```java
 @Override
    public @Nullable
    Surface createSurface(int width, int height) {
        mBufferInfo = new MediaCodec.BufferInfo();
        //创建视频的mediaFormat
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        //还需要对器进行插值。设置自己设置的一些变量
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        if (VERBOSE) Log.d(TAG, "format: " + format);

        // 创建一个MediaCodec编码器,并且使用format 进行configure.然后将其 Get a Surface给VirtualDisplay
        try {
            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mInputSurface = mEncoder.createInputSurface();
            //直接开启编码器
            mEncoder.start();
            //...省去部分代码
            return mInputSurface;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

```

#### 2. 获取编码后的数据
- 创建Encoder HanderThread
不断获取编码后的数据需要在一个新的线程内进行。所以我们先创建一个HanderThread进行异步操作和异步通行。
```java
    private void createEncoderThread() {
        HandlerThread encoder = new HandlerThread("Encoder");
        encoder.start();
        Looper looper = encoder.getLooper();
        workHanlder = new Handler(looper);
    }
```

- 开始获取数据的任务
在上面编码器开启之后，直接推入一个任务运行
```java
        //这里的1s延迟是因为开启encoder之后，硬件编码器进行初始化需要点时间
         workHanlder.postDelayed(new Runnable() {
                @Override
                public void run() {
                    doExtract(mEncoder,null);}, 1000);
```
注意是的是，这里推入任务，需要稍微的延迟，因为初始化和开启硬件编码器需要一点时间。

- 获取编码后的数据
```java
    /**
     * 不断循环获取，直到我们手动结束.同步的方式
     * @param encoder       编码器
     * @param frameCallback 获取的回调
     */
    private void doExtract(MediaCodec encoder,
                           FrameCallback frameCallback) {
        final int TIMEOUT_USEC = 10000;
        long firstInputTimeNsec = -1;
        boolean outputDone = false;
        //没有手动停止，就只能不断进行
        while (!outputDone) {
            //如果手动停止了。就结束吧
            if (mIsStopRequested) {
                Log.d(TAG, "Stop requested");
                return;
            }
            //因为给编码器获取状态和喂数据的方法都直接通过Surface直接进行了，这里只要直接获取解码后的状态就可以了
            int decoderStatus = encoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
//                if (VERBOSE) Log.d(TAG, "no output from decoder available");
            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not important for us, since we're using Surface
//                if (VERBOSE) Log.d(TAG, "decoder output buffers changed");
            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                //上面几种状态，我们都可以直接忽略。这里是进行MediaCodec开始编码后，会得到一个有cs-0 和cs-1的数据，对应sps和pps .获取之后，我们后面需要处理，所以先设置成一个回调就好。
                MediaFormat newFormat = encoder.getOutputFormat();
                if (VERBOSE) Log.d(TAG, "decoder output format changed: " + newFormat);
                if (frameCallback != null) {
                    frameCallback.formatChange(newFormat);
                }
            } else if (decoderStatus < 0) {
                //这种情况下是出错了。暂时先直接出异常吧
                throw new RuntimeException(
                        "unexpected result from decoder.dequeueOutputBuffer: " +
                                decoderStatus);
            } else { // decoderStatus >= 0
                //这里是正确获取到编码后的数据了
                if (firstInputTimeNsec != 0) {
                    long nowNsec = System.nanoTime();
                    Log.d(TAG, "startup lag " + ((nowNsec - firstInputTimeNsec) / 1000000.0) + " ms");
                    firstInputTimeNsec = 0;
                }
                if (VERBOSE) Log.d(TAG, "surface decoder given buffer " + decoderStatus +
                        " (size=" + mBufferInfo.size + ")");
                //获取到最后的数据了。这里就跳出循环。我们这个地方基本也不用用到
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE) Log.d(TAG, "output EOS");
                    outputDone = true;
                }
                
                //当size 大于0时，需要送显
                boolean doRender = (mBufferInfo.size != 0);
                //这个时候，来获取编码后的buffer,回调给外面
                if (doRender && frameCallback != null) {
                    ByteBuffer outputBuffer = encoder.getOutputBuffer(decoderStatus);
                    frameCallback.render(mBufferInfo, outputBuffer);
                }
                encoder.releaseOutputBuffer(decoderStatus, doRender);
            }
        }
    }
```
通过这样的循环获取，就可以通过回调获取编码后的数据了。
后面，我们可以将编码后的数据进行让rtmp推流。

--- 
### 使用 RTMP 推流
1. 认识 rtmp 协议
2. RMTP Connection
3. 代码

#### 1. 认识 rtmp 协议
RTMP协议是Real Time Message Protocol(实时信息传输协议)的缩写，它是由Adobe公司提出的一种应用层的协议，用来解决多媒体数据传输流的多路复用（Multiplexing）和分包（packetizing）的问题。
- 基于TCP
在基于传输层协议的链接建立完成后，RTMP协议也要客户端和服务器通过“握手”来建立基于传输层链接之上的RTMP Connection链接。在Connection链接上会传输一些控制信息，如SetChunkSize,SetACKWindowSize。其中CreateStream命令会创建一个Stream链接，用于传输具体的音视频数据和控制这些信息传输的命令信息。RTMP协议传输时会对数据做自己的格式化，这种格式的消息我们称之为RTMP Message，而实际传输的时候为了更好地实现多路复用、分包和信息的公平性，发送端会把Message划分为带有Message ID的Chunk，每个Chunk可能是一个单独的Message，也可能是Message的一部分，在接受端会根据chunk中包含的data的长度，message id和message的长度把chunk还原成完整的Message，从而实现信息的收发。

#### 2. RTMP Connection
##### 握手（HandShake）

一个RTMP连接以握手开始，双方分别发送大小固定的三个数据块

1.  握手开始于客户端发送C0、C1块。服务器收到C0或C1后发送S0和S1。
2.  当客户端收齐S0和S1后，开始发送C2。当服务器收齐C0和C1后，开始发送S2。
3.  当客户端和服务器分别收到S2和C2后，握手完成。

![image](http://upload-images.jianshu.io/upload_images/1877190-0e1db2dc22e0ae25.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


理论上来讲只要满足以上条件，如何安排6个Message的顺序都是可以的，但实际实现中为了在保证握手的身份验证功能的基础上尽量减少通信的次数，一般的发送顺序是这样的：

1.  Client发送C0+C1到Sever
2.  Server发送S0+S1+S2到Client
3.  Client发送C2到Server，握手完成

##### 建立网络连接（NetConnection）

1.  客户端发送命令消息中的“连接”(connect)到服务器，请求与一个服务应用实例建立连接。
2.  服务器接收到连接命令消息后，发送确认窗口大小(Window Acknowledgement Size)协议消息到客户端，同时连接到连接命令中提到的应用程序。
3.  服务器发送设置带宽(Set Peer Bandwitdh)协议消息到客户端。
4.  客户端处理设置带宽协议消息后，发送确认窗口大小(Window Acknowledgement Size)协议消息到服务器端。
5.  服务器发送用户控制消息中的“流开始”(Stream Begin)消息到客户端。
6.  服务器发送命令消息中的“结果”(_result)，通知客户端连接的状态。
7.  客户端在收到服务器发来的消息后，返回确认窗口大小，此时网络连接创建完成。

服务器在收到客户端发送的连接请求后发送如下信息：

![image](http://upload-images.jianshu.io/upload_images/1877190-9557496a54bf3b4e.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


主要是告诉客户端确认窗口大小，设置节点带宽，然后服务器把“连接”连接到指定的应用并返回结果，“网络连接成功”。并且返回流开始的的消息（Stream Begin 0）。

##### 建立网络流（NetStream）
1.  客户端发送命令消息中的“创建流”（createStream）命令到服务器端。
2.  服务器端接收到“创建流”命令后，发送命令消息中的“结果”(_result)，通知客户端流的状态。

##### **推流流程**
1.  客户端发送publish推流指令。
2.  服务器发送用户控制消息中的“流开始”(Stream Begin)消息到客户端。
3.  客户端发送元数据（分辨率、帧率、音频采样率、音频码率等等）。
4.  客户端发送音频数据。
5.  客户端发送服务器发送设置块大小（ChunkSize）协议消息。
6.  服务器发送命令消息中的“结果”(_result)，通知客户端推送的状态。
7.  客户端收到后，发送视频数据直到结束。

![推流流程](http://upload-images.jianshu.io/upload_images/1877190-3c9a9f9f18bb35ed.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

##### 播流流程
1.  客户端发送命令消息中的“播放”（play）命令到服务器。
2.  接收到播放命令后，服务器发送设置块大小（ChunkSize）协议消息。
3.  服务器发送用户控制消息中的“streambegin”，告知客户端流ID。
4.  播放命令成功的话，服务器发送命令消息中的“响应状态” NetStream.Play.Start & NetStream.Play.reset，告知客户端“播放”命令执行成功。
5.  在此之后服务器发送客户端要播放的音频和视频数据。

![播流流程](http://upload-images.jianshu.io/upload_images/1877190-5f0389739bca2e4e.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

#### 3. 代码集成
##### 1. 集成RTMP
直接使用[librestreaming](https://github.com/lakeinchina/librestreaming) 中的RTMP的代码，将其放到CMake中进行编译。

- 将项目中的librtmp到 libs下
![image.png](https://upload-images.jianshu.io/upload_images/1877190-0201636c4aa3b5b6.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


- 根据原来的Android.mk文件，配置`CMakeList`
```
cmake_minimum_required(VERSION 3.4.1)
add_definitions("-DNO_CRYPTO")
include_directories(${CMAKE_SOURCE_DIR}/libs/rtmp/librtmp)
#native-lib
file(GLOB PROJECT_SOURCES "${CMAKE_SOURCE_DIR}/libs/rtmp/librtmp/*.c")
add_library(rtmp-lib
        SHARED
        src/main/cpp/rtmp-hanlde.cpp
        ${PROJECT_SOURCES}
        )
find_library( # Sets the name of the path variable.
        log-lib
        log)
target_link_libraries( # Specifies the target library.
        rtmp-lib
        ${log-lib})
```
-  创建java文件，并编写jni
```java
public class RtmpClient {
    static {
        System.loadLibrary("rtmp-lib");
    }

    /**
     * @param url
     * @param isPublishMode
     * @return rtmpPointer ,pointer to native rtmp struct
     */
    public static native long open(String url, boolean isPublishMode);
    public static native int write(long rtmpPointer, byte[] data, int size, int type, int ts);
    public static native int close(long rtmpPointer);
    public static native String getIpAddr(long rtmpPointer);
}

```

##### 2. RMTP推流
之前的文章，有分析过[FLV的数据格式](https://www.jianshu.com/p/e327d7715bae)。这样还需要再将编码后的数据。
这里就不赘述了。

整体的流程是
1. 连接RTMP URL 
2. 在得到MediaFormat回调时，将其进行推流发送，进行publish
3. 不断得到编码后的数据，不断推流
4. 最后关闭

###### 封装Sender的代码
- RESRtmpSender
```java
public class RESRtmpSender {

    private long jniRtmpPointer;
    private String serverIpAddr;
    private FLvMetaData fLvMetaData;
    private long startTime;

    public long rtmpOpen(String url, int mWidth, int mHeight) {
        LogTools.d("RESRtmpSender,WorkHandler,tid=" + Thread.currentThread().getId());
        jniRtmpPointer = RtmpClient.open(url, true);
        final int openR = jniRtmpPointer == 0 ? 1 : 0;
//        if (openR == 0) {
//        }
        serverIpAddr = RtmpClient.getIpAddr(jniRtmpPointer);
        Log.d("zzx","serverIpAddr="+serverIpAddr);
        if (jniRtmpPointer != 0) {
            RESCoreParameters coreParameters = new RESCoreParameters();
            coreParameters.mediacodecAVCFrameRate = 15;
            coreParameters.videoWidth = mWidth;
            coreParameters.videoHeight = mHeight;
            fLvMetaData = new FLvMetaData(coreParameters);
            byte[] MetaData = fLvMetaData.getMetaData();
            RtmpClient.write(jniRtmpPointer,
                    MetaData,
                    MetaData.length,
                    RESFlvData.FLV_RTMP_PACKET_TYPE_INFO, 0);
//            state = STATE.RUNNING;
        }
        return jniRtmpPointer;
    }

    public void rtmpSendFormat(MediaFormat newFormat) {
        RESFlvData resFlvData = sendAVCDecoderConfigurationRecord(0, newFormat);
        rtmpPublish(resFlvData);
    }

    public void rtmpSend(MediaCodec.BufferInfo info, ByteBuffer outputBuffer) {
        if (info.size == 0) {
            return;
        }
        if (startTime == 0) {
            startTime = info.presentationTimeUs / 1000;
        }
        //rtmp
        outputBuffer.position(info.offset + 4);
//        outputBuffer.position(info.offset);
        outputBuffer.limit(info.offset + info.size);
        RESFlvData resFlvData = sendRealData((info.presentationTimeUs / 1000) - startTime, outputBuffer);
        rtmpPublish(resFlvData);
    }

    public RESFlvData getRealData(MediaCodec.BufferInfo info, ByteBuffer outputBuffer) {
        if (info.size == 0) {
            return null;
        }
        if (startTime == 0) {
            startTime = info.presentationTimeUs / 1000;
        }
        //rtmp
        outputBuffer.position(info.offset + 4);
//        outputBuffer.position(info.offset);
        outputBuffer.limit(info.offset + info.size);
        RESFlvData resFlvData = sendRealData((info.presentationTimeUs / 1000) - startTime, outputBuffer);
        return resFlvData;
    }

    public void rtmpPublish(RESFlvData flvData) {
        final int res = RtmpClient.write(jniRtmpPointer, flvData.byteBuffer, flvData.byteBuffer.length, flvData.flvTagType, flvData.dts);
        LogTools.d("RtmpClient.write res=" + res + ", length =" + flvData.byteBuffer.length);
    }

    public void rtmpClose() {
        int errorTime = 0;
        final int closeR = RtmpClient.close(jniRtmpPointer);
        serverIpAddr = null;
    }

    private static RESFlvData sendAVCDecoderConfigurationRecord(long tms, MediaFormat format) {
        byte[] AVCDecoderConfigurationRecord = Packager.H264Packager.generateAVCDecoderConfigurationRecord(format);
        int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                AVCDecoderConfigurationRecord.length;
        byte[] finalBuff = new byte[packetLen];
        Packager.FLVPackager.fillFlvVideoTag(finalBuff,
                0,
                true,
                true,
                AVCDecoderConfigurationRecord.length);
        System.arraycopy(AVCDecoderConfigurationRecord, 0,
                finalBuff, Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH, AVCDecoderConfigurationRecord.length);
        RESFlvData resFlvData = new RESFlvData();
        resFlvData.droppable = false;
        resFlvData.byteBuffer = finalBuff;
        resFlvData.size = finalBuff.length;
        resFlvData.dts = (int) tms;
        resFlvData.flvTagType = RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO;
        resFlvData.videoFrameType = RESFlvData.NALU_TYPE_IDR;
        return resFlvData;
//        dataCollecter.collect(resFlvData, RESRtmpSender.FROM_VIDEO);
    }

    private static RESFlvData sendRealData(long tms, ByteBuffer realData) {
        int realDataLength = realData.remaining();
        int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                Packager.FLVPackager.NALU_HEADER_LENGTH +
                realDataLength;
        byte[] finalBuff = new byte[packetLen];
        realData.get(finalBuff, Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                        Packager.FLVPackager.NALU_HEADER_LENGTH,
                realDataLength);
        int frameType = finalBuff[Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                Packager.FLVPackager.NALU_HEADER_LENGTH] & 0x1F;
        Packager.FLVPackager.fillFlvVideoTag(finalBuff,
                0,
                false,
                frameType == 5,
                realDataLength);
        RESFlvData resFlvData = new RESFlvData();
        resFlvData.droppable = true;
        resFlvData.byteBuffer = finalBuff;
        resFlvData.size = finalBuff.length;
        resFlvData.dts = (int) tms;
        resFlvData.flvTagType = RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO;
        resFlvData.videoFrameType = frameType;
        return resFlvData;
//        dataCollecter.collect(resFlvData, RESRtmpSender.FROM_VIDEO);
    }
}
```

- Sender 
```java
public class Sender {
    RESRtmpSender resRtmpSender;
    private final ExecutorService executorService;

    private static class HOLDER {
        private static Sender SINGLE = new Sender(new RESRtmpSender(), Executors.newFixedThreadPool(1));
    }

    public static Sender getInstance() {
        return HOLDER.SINGLE;
    }

    public Sender(RESRtmpSender resRtmpSender, ExecutorService executorService) {
        this.resRtmpSender = resRtmpSender;
        this.executorService = executorService;
    }

    public void open(String url, int width, int height) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                long open = resRtmpSender.rtmpOpen(url, width, height);
                Log.d("zzx", "open result=" + open);
            }
        });
    }

    public void close() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                resRtmpSender.rtmpClose();
                Log.d("zzx", "rtmpClose");
            }
        });
    }

    public void rtmpSendFormat(MediaFormat newFormat) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                resRtmpSender.rtmpSendFormat(newFormat);
                Log.d("zzx", "rtmpSendFormat");
            }
        });
    }

    public void rtmpSend(MediaCodec.BufferInfo info, ByteBuffer outputBuffer) {
        RESFlvData realData = resRtmpSender.getRealData(info, outputBuffer);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                resRtmpSender.rtmpPublish(realData);
                Log.d("zzx", "rtmpPublish");
            }
        });
    }
}
```

###### 通过回调发布格式和发送实际数据
```java
  workHanlder.postDelayed(new Runnable() {
                @Override
                public void run() {
                    doExtract(mEncoder, new FrameCallback() {

                        @Override
                        public void render(MediaCodec.BufferInfo info, ByteBuffer outputBuffer) {
                            Sender.getInstance().rtmpSend(info, outputBuffer);
                        }

                        @Override
                        public void formatChange(MediaFormat mediaFormat) {
                            Sender.getInstance().rtmpSendFormat(mediaFormat);
                        }
                    });
                }
            }, 1000);
```

### RMTP服务器
RMTP服务器的建立，可以简单的使用
[RMTP服务器](https://github.com/illuspas/Node-Media-Server)

## 总结

#### 对比之前的一遍文章
[Android PC投屏简单尝试](https://www.jianshu.com/p/ce37330365f2)
- 获取数据的方式
都是通过MediaProjection.createVirtualDisplay的方式来获取截屏的数据。
不同的是，上一边文章使用ImageReader来获取一张一张的截图。
而这边文章直接是用了MediaCodec硬编码，直接得到编码后的h264数据。

- 传输协议
上一边文章使用的webSocket，将得到的Bitmap的字节流，通过socket传输，接收方，只要接受到Socket,并且将其解析成Bitmap来展示就可以。
优点是方便,可以自定义协议。缺点是，不能通用，必须编写对应的客户端才能完成。

这边文章使用了rtmp的流媒体协议，优点是只要支持该协议的播放器都可以直接播放我们的投屏流。

#### 参考文章
[Android实现录屏直播（一）ScreenRecorder的简单分析](https://blog.csdn.net/zxccxzzxz/article/details/54150396)
[直播推流实现RTMP协议的一些注意事项](https://www.jianshu.com/p/00aceabce944)

#### 投屏尝试系列文章
- [Android PC投屏简单尝试](https://www.jianshu.com/p/ce37330365f2) 
- [Android PC投屏简单尝试2—录屏直播]()
