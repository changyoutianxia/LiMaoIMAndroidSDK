package com.xinbida.limaoim.protocol;

import com.xinbida.limaoim.message.type.LiMMsgType;

/**
 * 2019-11-11 10:37
 * 接受消息
 */
public class LiMReceivedMsg extends LiMBaseMsg {
    //服务端的消息ID(全局唯一)
    public String messageID;
    //服务端的消息序列号(有序递增，用户唯一)
    public int messageSeq;
    //服务器消息时间戳(10位，到秒)
    public long messageTimestamp;
    //客户端ID
    public String clientMsgNo;
    //频道ID
    public String channelID;
    //频道类型
    public byte channelType;
    //发送者ID
    public String fromUID;
    //消息内容
    public String payload;
    //消息key
    public String msgKey;
    //消息是否回执
    public int receipt;

    public LiMReceivedMsg() {
        packetType = LiMMsgType.RECVEIVED;
    }
}
