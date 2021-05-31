package com.xinbida.limaoim.protocol;


import com.xinbida.limaoim.message.type.LiMMsgType;

/**
 * 2019-11-11 10:49
 * 对ping请求的响应
 */
public class LiMPongMsg extends LiMBaseMsg {
    public LiMPongMsg() {
        packetType = LiMMsgType.PONG;
    }
}
