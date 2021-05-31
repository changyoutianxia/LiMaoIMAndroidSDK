package com.xinbida.limaoim.interfaces;


import com.xinbida.limaoim.entity.LiMMsg;

/**
 * 2020-08-27 21:18
 * 消息修改监听
 */
public interface IRefreshMsg {
    void onRefresh(LiMMsg liMMsg, boolean left);
}
