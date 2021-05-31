package com.xinbida.limaoim.interfaces;


import com.xinbida.limaoim.entity.LiMSyncChat;

/**
 * 2020-10-09 14:43
 * 同步消息返回
 */
public interface ISyncConversationChatBack {
    void onBack(LiMSyncChat liMSyncChat);
}
