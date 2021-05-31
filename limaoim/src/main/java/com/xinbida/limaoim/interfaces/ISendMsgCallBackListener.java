package com.xinbida.limaoim.interfaces;


import com.xinbida.limaoim.entity.LiMMsg;

/**
 * 2020-08-02 00:21
 * 发送消息监听
 */
public interface ISendMsgCallBackListener {
    void onInsertMsg(LiMMsg liMMsg);
}
