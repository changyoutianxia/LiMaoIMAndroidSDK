package com.xinbida.limaoim.interfaces;


import com.xinbida.limaoim.entity.LiMSyncMsgReaction;

import java.util.List;

/**
 * 4/16/21 3:01 PM
 * 同步消息回应返回
 */
public interface ISyncMsgReactionBack {
     void onResult(List<LiMSyncMsgReaction> list);
}
