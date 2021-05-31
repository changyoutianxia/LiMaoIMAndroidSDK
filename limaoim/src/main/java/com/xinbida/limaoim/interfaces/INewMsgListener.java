package com.xinbida.limaoim.interfaces;


import com.xinbida.limaoim.entity.LiMMsg;

import java.util.List;

/**
 * 2019-11-18 11:44
 * 新消息监听
 */
public interface INewMsgListener {
    void newMsg(List<LiMMsg> liMMsgList);
}
