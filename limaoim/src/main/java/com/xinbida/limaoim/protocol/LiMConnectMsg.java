package com.xinbida.limaoim.protocol;


import com.xinbida.limaoim.LiMaoIMApplication;
import com.xinbida.limaoim.message.type.LiMMsgType;
import com.xinbida.limaoim.utils.LiMDateUtils;

/**
 * 2019-11-11 10:22
 * 连接talk service消息
 */
public class LiMConnectMsg extends LiMBaseMsg {
    //协议版本号
    public byte protocolVersion;
    //设备标示(同标示同账号互踢)
    public byte deviceFlag;
    //设备唯一ID
    public String deviceID;
    //客户端当前时间戳(13位时间戳,到毫秒)
    public long clientTimestamp;
    //用户的token
    public String token;

    //协议版本号长度
    public char protocolVersionLength = 1;
    //设备标示长度
    public char deviceFlagLength = 1;
    //设备id长度
    public char deviceIDLength = 2;
    //token长度所占字节长度
    public char tokenLength = 2;
    //uid长度所占字节长度
    public char uidLength = 2;
    //ClientKey长度所占字节长度
    public char clientKeyLength = 2;
    //时间戳长度
    public char clientTimeStampLength = 8;

    public LiMConnectMsg() {
        token = LiMaoIMApplication.getInstance().getToken();
        clientTimestamp = LiMDateUtils.getInstance().getCurrentMills();
        packetType = LiMMsgType.CONNECT;
        protocolVersion = LiMaoIMApplication.getInstance().protocolVersion;
        deviceFlag = 0;
        deviceID = LiMaoIMApplication.getInstance().getDeviceId();
        remainingLength = 1 + 1 + 8;//(协议版本号+设备标示(同标示同账号互踢)+客户端当前时间戳(13位时间戳,到毫秒))
    }
}
