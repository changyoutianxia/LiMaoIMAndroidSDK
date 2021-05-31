package com.xinbida.limaoim.interfaces;

/**
 * 2020-09-28 15:05
 * 同步离线消息
 */
public interface ISyncOfflineMsgListener {
    void getOfflineMsgs(int max_message_seq, ISyncOfflineMsgBack iSyncOfflineMsgBack);
}
