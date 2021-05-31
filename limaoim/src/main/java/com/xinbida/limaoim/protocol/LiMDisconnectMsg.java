package com.xinbida.limaoim.protocol;


import com.xinbida.limaoim.message.type.LiMMsgType;

/**
 * 2020-01-30 17:34
 * 断开连接消息
 */
public class LiMDisconnectMsg extends LiMBaseMsg {
    public byte reasonCode;
    public String reason;

    public LiMDisconnectMsg() {
        packetType = LiMMsgType.DISCONNECT;
    }
}
