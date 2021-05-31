package com.xinbida.limaoim.interfaces;


import com.xinbida.limaoim.entity.LiMMsg;

import java.util.List;

/**
 * 2020-10-10 13:40
 * 获取或同步消息返回
 */
public interface IGetOrSyncHistoryMsgBack {
    void onResult(List<LiMMsg> liMMsgList);
}
