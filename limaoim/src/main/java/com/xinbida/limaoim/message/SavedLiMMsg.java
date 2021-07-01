package com.xinbida.limaoim.message;


import com.xinbida.limaoim.entity.LiMMsg;

/**
 * 4/22/21 4:26 PM
 * 需要保存的消息
 */
class SavedLiMMsg {
    public LiMMsg liMMsg;
    public int redDot;

    public SavedLiMMsg(LiMMsg liMMsg, int redDot) {
        this.redDot = redDot;
        this.liMMsg = liMMsg;
    }
}
