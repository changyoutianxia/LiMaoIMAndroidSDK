package com.xinbida.limaoim.interfaces;


import com.xinbida.limaoim.entity.LiMMsg;

/**
 * 2020-12-04 17:33
 * 存库之前拦截器
 */
public interface IMessageStoreBeforeIntercept {
    boolean isSaveMsg(LiMMsg liMMsg);
}
