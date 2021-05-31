package com.xinbida.limaoim.protocol;


import com.xinbida.limaoim.message.type.LiMMsgType;

/**
 * 2019-11-11 10:30
 * 发送消息到talkservice
 */
public class LiMSendMsg extends LiMBaseMsg {
    //客户端消息序列号(由客户端生成，每个客户端唯一)
    public int clientSeq;
    //频道ID（如果是个人频道ChannelId为个人的UID）
    public String channelId;
    //频道类型（1.个人 2.群组）
    public byte channelType;
    //消息内容
    public String payload;
    //客户端唯一ID
    public String clientMsgNo;
    //客户端唯一ID所占长度
    public int clientMsgNoLength = 2;
    //客户端消息序列号长度
    public int clientSeqLength = 4;
    //频道所占长度
    public short channelIdLength = 2;
    //消息体所占长度
    public short payloadLength = 2;
    //渠道类型长度
    public char channelTypeLength = 1;
    //消息Key用于验证此消息是否合法
    public short msgKeyLength = 2;
    //消息key
    public String msgKey;
    //消息是否回执
    public int receipt;
    //消息是否回执长度
    public short settingLength = 1;

    public LiMSendMsg() {
        packetType = LiMMsgType.SEND;
        remainingLength = 8 + 1;
    }
}
