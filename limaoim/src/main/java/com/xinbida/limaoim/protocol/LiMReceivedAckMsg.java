package com.xinbida.limaoim.protocol;


import com.xinbida.limaoim.message.type.LiMMsgType;

/**
 * 2019-11-11 10:46
 * 收到消息Ack消息
 */
public class LiMReceivedAckMsg extends LiMBaseMsg {
    //服务端的消息ID(全局唯一)
    public String messageID;
    //序列号
    public int messageSeq;
    //消息id长度
    public char messageIDLength = 2;

    public LiMReceivedAckMsg() {
        packetType = LiMMsgType.REVACK;
        remainingLength = 8;//序列号
    }
}
