package com.xinbida.limaoim.protocol;


import com.xinbida.limaoim.message.type.LiMMsgType;

/**
 * 2019-11-11 10:49
 * 心跳消息
 */
public class LiMPingMsg extends LiMBaseMsg {
    public LiMPingMsg() {
        packetType = LiMMsgType.PING;
    }
}
