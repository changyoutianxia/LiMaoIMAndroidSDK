# 狸猫通讯Android sdk 源码
该项目是一个完全自定义协议的即时通讯sdk。

## 快速入门

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

***发送消息结果监听***
```
LiMaoIM.getInstance().getLiMMsgManager().addSendMsgAckListener("", new ISendACK() {
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

## 更多文档信息点击这里

## 案例
<img src="https://raw.githubusercontent.com/lim-team/screenshot/master/android/receive_and_send.gif" width="400" height="400" alt="收发消息演示"/>

<img src="https://raw.githubusercontent.com/lim-team/screenshot/master/android/typing.gif" width="400" height="400" alt="收发消息演示"/>


