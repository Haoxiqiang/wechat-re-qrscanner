尝试逆向下微信的二维码扫描，来做一下小程序的码识别和一些针对微信优化的二维码识别

[小程序码](https://developers.weixin.qq.com/doc/offiaccount/Unique_Item_Code/Unique_Item_Code_Asked_Questions.html#2
) 不是标准二维码，这样很容易被灰产利用

## 这个库的测试数据, 当前基于WeChat 8.0.47

![WXACode fa3d686a](https://github.com/Haoxiqiang/wechat-re-qrscanner/assets/3881604/9e7ddf21-9193-43c5-b64b-92d740d6bd8b)

![image](https://github.com/Haoxiqiang/wechat-re-qrscanner/assets/3881604/751153f8-3cd7-450e-8dd0-801ed8a596a0)

### Dev

* Must be `root` or manaully in adb shell in your app which can get `root` permission.
* Install `termux` app from the Google Play for test app easier or use any app as the `/data/data/**` executeable dir. Don't chnage all path `/data/data/com.termux`
