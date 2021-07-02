# 狸猫通讯Android sdk 源码
该项目是一个完全自定义协议的即时通讯sdk。

## 快速入门

**集成**

在主程序的build.gradle文件中添加：

```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
然后在app model中导入
```
implementation 'com.github.lim-team:LiMaoIMAndroidSDK:1.0.0'
```
***初始化sdk***
```
LiMaoIM.getInstance().initIM(context, uid, token);
```
***连接服务端***
```
LiMaoIM.getInstance().getLiMConnectionManager().connection();
```
***发消息***
```
LiMaoIM.getInstance().getLiMConnectionManager().sendMessage(new LiMTextContent("我是文本消息"), channelID, channelType);
```

## 监听
***连接状态监听***
```
LiMaoIM.getInstance().getLiMConnectionManager().addOnConnectionStatusListener("listener_key",new IConnectionStatus() {
            @Override
            public void onStatus(int status) {
                // 0 失败
                // 1 成功
                // 2 被踢
                // 3 同步消息中
                // 4 连接中
                // 5 无网络连接
            }
        });
```
***发送消息结果监听***
```
LiMaoIM.getInstance().getLiMMsgManager().addSendMsgAckListener("listener_key", new ISendACK() {
            @Override
            public void msgACK(long clientSeq, String messageID, long messageSeq, byte reasonCode) {
                // clientSeq 客户端序列号
                // messageID 服务器消息ID
                // messageSeq 服务器序列号
                // reasonCode 消息状态码【0:发送中1:成功2:发送失败3:不是好友或不在群内4:黑名单】
            }
        })
 ```
***监听新消息***
```
 LiMaoIM.getInstance().getLiMMsgManager().addOnNewMsgListener("listener_key", new INewMsgListener() {
            @Override
            public void newMsg(List<LiMMsg> list) {
                // todo 
            }
        });
```
***命令消息(cmd)监听***
```
LiMaoIM.getInstance().getLiMCMDManager().addCmdListener("listener_key", new ICMDListener() {
            @Override
            public void onMsg(LiMCMD liMCMD) {
                // todo
            }
        });
```

## [详细文档信息点击这里](http://limao.ai/docs/ "文档")

## 案例
<img src="https://raw.githubusercontent.com/lim-team/screenshot/master/android/receive_and_send.gif" width="400" height="400" alt="收发消息演示"/>

<img src="https://raw.githubusercontent.com/lim-team/screenshot/master/android/typing.gif" width="400" height="400" alt="正在输入"/>

<img src="https://raw.githubusercontent.com/lim-team/screenshot/master/android/receipt.gif" width="400" height="400" alt="消息回执"/>

<img src="https://raw.githubusercontent.com/lim-team/screenshot/master/android/msg_reaction.gif" width="400" height="400" alt="消息回应"/>

<img src="https://raw.githubusercontent.com/lim-team/screenshot/master/android/redpacket.gif" width="400" height="400" alt="红包消息"/>

<img src="https://raw.githubusercontent.com/lim-team/screenshot/master/android/transfer.gif" width="400" height="400" alt="转账消息"/>

<img src="https://raw.githubusercontent.com/lim-team/screenshot/master/android/group_manager.gif" width="400" height="400" alt="群管理"/>

<img src="https://raw.githubusercontent.com/lim-team/screenshot/master/android/dynamic.gif" width="400" height="400" alt="朋友圈"/>

<img src="https://raw.githubusercontent.com/lim-team/screenshot/master/android/p2pcall.gif" width="400" height="400" alt="单人音视频"/>

<img src="https://raw.githubusercontent.com/lim-team/screenshot/master/android/multiplecall.gif" width="400" height="400" alt="多人音视频"/>


