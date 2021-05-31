package com.xinbida.limaoim.protocol;


import com.xinbida.limaoim.message.type.LiMMsgType;

/**
 * 2019-11-11 10:33
 * 发送消息Ack消息
 */
public class LiMSendAckMsg extends LiMBaseMsg {
    //客户端消息序列号
    public int clientSeq;
    //服务端的消息ID(全局唯一)
    public String messageID;
    //消息序号（有序递增，用户唯一）
    public long messageSeq;
    //发送原因代码 1表示成功
    public byte reasonCode;
    //客户端序号长度
    public int clientSeqLength = 4;
    //mos协议需要的
//    public String clientMsgNo;

    public LiMSendAckMsg() {
        packetType = LiMMsgType.SENDACK;
    }
}
