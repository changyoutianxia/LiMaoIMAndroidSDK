package com.xinbida.limaoim.interfaces;


import com.xinbida.limaoim.entity.LiMMsg;
import com.xinbida.limaoim.protocol.LiMDisconnectMsg;
import com.xinbida.limaoim.protocol.LiMPongMsg;
import com.xinbida.limaoim.protocol.LiMSendAckMsg;

/**
 * 2019-11-10 17:03
 * 接受通讯协议消息
 */
public interface IReceivedMsgListener {
    /**
     * 登录状态消息
     *
     * @param statusCode 状态
     */
    void loginStatusMsg(short statusCode);

    /**
     * 心跳消息
     */
    void heartbeatMsg(LiMPongMsg msgHeartbeat);

    /**
     * 被踢消息
     */
    void kickMsg(LiMDisconnectMsg liMDisconnectMsg);

    /**
     * 发送消息状态消息
     *
     * @param liMSendAckMsg ack
     */
    void sendAckMsg(LiMSendAckMsg liMSendAckMsg);

    /**
     * 聊天消息
     *
     * @param liMMsg 消息对象
     */
    void receiveMsg(LiMMsg liMMsg);

    /**
     * 重连
     */
    void reconnect();
}
