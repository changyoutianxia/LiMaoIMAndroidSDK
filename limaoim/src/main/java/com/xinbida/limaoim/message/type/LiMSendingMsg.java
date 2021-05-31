package com.xinbida.limaoim.message.type;

import com.xinbida.limaoim.entity.LiMMsg;
import com.xinbida.limaoim.utils.LiMDateUtils;

/**
 * 2020-05-28 17:45
 * 正在发送的消息
 */
public class LiMSendingMsg {
    //消息
    public LiMMsg liMMsg;
    //发送次数
    public int sendCount;
    //发送时间
    public long sendTime;

    public LiMSendingMsg(int sendCount, LiMMsg liMMsg) {
        this.sendCount = sendCount;
        this.liMMsg = liMMsg;
        this.sendTime = LiMDateUtils.getInstance().getCurrentSeconds();
    }
}
