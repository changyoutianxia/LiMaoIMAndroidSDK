package com.xinbida.limaoim.interfaces;

/**
 * 2020-12-04 11:30
 * 删除最近会话消息
 */
public interface IDeleteConversationMsg {
    void onDelete(String channelID, byte channelType);
}
