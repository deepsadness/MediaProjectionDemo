# Android PC投屏简单尝试
代码地址 ：https://github.com/deepsadness/MediaProjectionDemo

### 项目使用说明
必须有nodejs的环境
在./sockt目录下安装
```
npm install --save express@4.15.2
npm install --save socket.io
```

0. 运行Node的Socket服务端
```
 node ./sockt/io-server.js
```

1. 运行Node的网页

```
    node ./sockt/index.js
```
打开 localhost:3000.就可以看到网页了。


2. 运行App。进入
在MainActivity，点击`Start`按钮，就可以开始了
进入Activity时，已经回去连接socket


> 注意：
1. 需要在局域网内运行。App内需要配置好Socket链接的ip. 在`SocketIoManager`内
2. 具体的内容，还是请看一下代码。

代码地址 ：https://github.com/deepsadness/MediaProjectionDemo

###效果预览
![投屏效果预览](https://upload-images.jianshu.io/upload_images/1877190-4fd57fae28bf9b00.gif?imageMogr2/auto-orient/strip)

###简单说明:
1. 使用Android MediaProjection Api来完成视频的截图
2. 通过WebSocket进行链接。将图片传递给网页

### 想法来源
看到`vysor`，觉得特别好玩，于是就想着自己能不能试着做一个类似的功能出来。搜索了相关实现。发现网上已经有网友针对`vysor`做了分析。于是就照着思路，按图索骥，当作对MediaProjection Api的练习，来完成这个小项目

### 主要思路
####1. 获取屏幕的截屏
- 创建VirtualDisplay
Android在Api 21以上为我们已经提供了系统的Api可以进行操作。
主要是这几个类的相互配合
`MediaProjection`和`VirtualSurface`，还有截图的话，使用`ImageReader`，三个类配合使用。
![配套使用示意图.png](https://upload-images.jianshu.io/upload_images/1877190-91e54bd04670cd75.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
```java
public RxScreenShot createImageReader() {
        //注意这里使用RGB565报错提示，只能使用RGBA_8888
        mImageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1000);
        mSurfaceFactory = new ImageReaderSurface(mImageReader);
        createProject();
        return this;
    }

    private void createProject() {
        mediaProjection.registerCallback(mMediaCallBack, mCallBackHandler);
        //通过这种方式来创建这个VirtualDisplay，并将数据传递给ImageReader提供surface
        mediaProjection.createVirtualDisplay(TAG + "-display", width, height, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                mSurfaceFactory.getInputSurface(), null, null);
    }
```

- 获取屏幕截图
可以通过`ImageReader`类。配套`Image`来获奖获得的数据转成Bitmap
```java
/*
封装成了Observable对象。
*/
public class ImageReaderAvailableObservable extends Observable<ImageReader> {

    public static ImageReaderAvailableObservable of(ImageReader imageReader) {
        return new ImageReaderAvailableObservable(imageReader, null);

    }

    public static ImageReaderAvailableObservable of(ImageReader imageReader,Handler handler) {
        return new ImageReaderAvailableObservable(imageReader, handler);
    }

    private final ImageReader imageReader;
    private final Handler handler;

    private ImageReaderAvailableObservable(ImageReader imageReader, Handler handler) {
        this.imageReader = imageReader;
        this.handler = handler;
    }

    @Override
    protected void subscribeActual(Observer<? super ImageReader> observer) {
        Listener listener = new Listener(observer, imageReader);
        observer.onSubscribe(listener);
        //设置准备好的监听事件
        imageReader.setOnImageAvailableListener(listener, handler);
    }


    static class Listener implements Disposable, ImageReader.OnImageAvailableListener {
        private final AtomicBoolean unsubscribed = new AtomicBoolean();
        private final ImageReader mImageReader;
        private final Observer<? super ImageReader> observer;

        Listener(Observer<? super ImageReader> observer, ImageReader imageReader) {
            this.mImageReader = imageReader;
            this.observer = observer;
        }


        @Override
        public void onImageAvailable(ImageReader reader) {
            if (!isDisposed()) {
              //将准备好的reader发送出去，进行处理
                observer.onNext(reader);
              //注意：这里如果不调用onCompleted事件。其实这个监听会不断回调事件
//                observer.onComplete();
            }
        }

        @Override
        public void dispose() {
            if (unsubscribed.compareAndSet(false, true)) {
                mImageReader.setOnImageAvailableListener(null, null);
            }
        }

        @Override
        public boolean isDisposed() {
            return unsubscribed.get();
        }
    }
}

/*
调用开始截屏的方法
*/
public Observable<Object> startCapture() {
        return ImageReaderAvailableObservable.of(mImageReader)
                .map(imageReader -> {
                    String mImageName = System.currentTimeMillis() + ".png";
                    Log.e(TAG, "image name is : " + mImageName);
                    Bitmap bitmap = null;
                    //从imageReader中获取到最新的Image
                    Image image = imageReader.acquireLatestImage();
                    if (image == null) {

                    } else {
                        //将Image对象转成bitmap
                        int width = image.getWidth();
                        int height = image.getHeight();
                        //byteBuffer都保存在image.Plane中
                        final Image.Plane[] planes = image.getPlanes();
                        final ByteBuffer buffer = planes[0].getBuffer();
                        int pixelStride = planes[0].getPixelStride();
                        int rowStride = planes[0].getRowStride();
                        int rowPadding = rowStride - pixelStride * width;
                        bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                        bitmap.copyPixelsFromBuffer(buffer);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
                        //这里使用完要记得close.如果没有close，当imageReader达到max_count上限时将会抛出异常
                        image.close();
                    }
                    return bitmap == null ? new Object() : bitmap;
                });
    }

```
这里需要注意的是，需要通过这个回调，每当屏幕发生变化，就会回调这个接口，可以得到最新的截图。
`ImageReader::setOnImageAvailableListener`

#### 2. 搭建Socket连接，将图片的数据进行传递

> node 部分的代码在 https://github.com/deepsadness/MediaProjectionDemo/tree/master/sockt

因为我们的目标是在网页内打开，所以需要和网页进行通信。
可以简单的使用`WebSocket`进行双方通向
![简单示意图Again.png](https://upload-images.jianshu.io/upload_images/1877190-b384b83186935298.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

通过`Socket.io`https://socket.io/ 就可以简单的实现

- Android端的代码
通过WebSocket将Bitmap的字节码发送出去
```java
    private fun sendBitmap(it: Bitmap) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        it.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        SocketIoManager.getInstance().send(byteArray)
    }

    public void send(byte[] bitmapArray) {
        if (!mSocketReady) {
            return;
        }
        if (bitmapArray != null) {
            mSocket.emit("event", bitmapArray);
        }
    }
```
- Node端的代码
简单的SocketIo实现.代码在 `/sockt/io-server.js`
```javascript
var io = require('socket.io')();
var clients = []
io.on('connection', function (client) {
    clients.push(client);
    console.log('connection!');

    client.emit('join', 'welcome to join!!')

    client.on('chat message', function (msg) {
        console.log("receive msg=" + msg);

    });
    client.on('event', function (msg) {
        // console.log("event", msg);
        console.log("event", "send image~~");
        //通过event事件出去
        clients.forEach(function (it) {
            it.emit('event', msg)
        })
    });
});
io.on('disconnect', function (client) { 

})
io.listen(9000);
```

#### 3. 如何将图片显示出来
代码在 `/sockt/index.html`中
`html`中的`src`就可以直接对传递`byte[]`的进行解析。
```javascript
 socket.on('image', function (msg) {
      var arrayBufferView = new Uint8Array(msg);
      var blob = new Blob([arrayBufferView], { type: "image/jpeg" });
      var urlCreator = window.URL || window.webkitURL;
      var imageUrl = urlCreator.createObjectURL(blob);
      var img = document.getElementById("screen");
      // var img = document.querySelector("#photo");
      img.src = imageUrl;
```

#### 4. 下一步
下一步，就是使用 录制的Api，来做录屏直播了。
