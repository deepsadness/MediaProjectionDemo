# Android PC投屏简单尝试
代码地址 ：https://github.com/deepsadness/MediaProjectionDemo

### 项目使用说明

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


### 效果预览
![投屏效果预览](https://upload-images.jianshu.io/upload_images/1877190-4fd57fae28bf9b00.gif?imageMogr2/auto-orient/strip)

### 简单说明:
1. 使用Android MediaProjection Api来完成视频的截图
2. 通过WebSocket进行链接。将图片传递给网页

### 想法来源
看到`vysor`，觉得特别好玩，于是就想着自己能不能试着做一个类似的功能出来。搜索了相关实现。发现网上已经有网友针对`vysor`做了分析。于是就照着思路，按图索骥，当作对MediaProjection Api的练习，来完成这个小项目

### 主要思路
####1. 获取屏幕的截屏
Android在Api 21以上为我们已经提供了系统的Api可以进行操作。
主要是这几个类的相互配合
`MediaProjection`和`VirtualSurface`，还有截图的话，使用`ImageReader`，三个类配合使用。

![配套使用示意图.png](https://upload-images.jianshu.io/upload_images/1877190-e21239d5f2351a75.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

这里需要注意的是，需要通过这个回调，每当屏幕发生变化，就会回调这个接口，可以得到最新的截图。
`ImageReader::setOnImageAvailableListener`

#### 2. 搭建Socket连接，将图片的数据进行传递

> node 部分的代码在 https://github.com/deepsadness/MediaProjectionDemo/tree/master/sockt

因为我们的目标是在网页内打开，所以需要和网页进行通信。
可以简单的使用`WebSocket`进行双方通向
![简单示意图Again.png](https://upload-images.jianshu.io/upload_images/1877190-1585be431cb2055e.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

通过`Socket.io`https://socket.io/ 就可以简单的实现

#### 3. 如何将图片显示出来

`html`中的`src`就可以直接对传递`byte[]`的进行解析。
```
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
