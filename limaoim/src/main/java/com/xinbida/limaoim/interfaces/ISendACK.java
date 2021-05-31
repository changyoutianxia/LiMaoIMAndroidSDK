package com.xinbida.limaoim.interfaces;

/**
 * 5/12/21 2:02 PM
 * 发送消息ack监听
 */
public interface ISendACK {
    void msgACK(long clientSeq, String messageID, long messageSeq, byte reasonCode);
}
