package com.xinbida.limaoim.interfaces;


import com.xinbida.limaoim.entity.LiMUIConversationMsg;

/**
 * 2020-02-21 11:11
 * 刷新最近会话
 */
public interface IRefreshConversationMsg {
    void onRefreshConversationMsg(LiMUIConversationMsg liMUIConversationMsg, boolean isEnd);
}
