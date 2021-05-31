package com.xinbida.limaoim;
import com.xinbida.limaoim.protocol.LiMMessageContent;
import com.xinbida.limaoim.entity.LiMMsg;

interface IPushService {
    void regRecvMsgCallback(in String uid,in String imToken);
    void sendMessage(in LiMMessageContent liMMessageContent,in String channelID,in byte channelType);
    void sendMessageWithLiMMsg(in LiMMsg liMMsg);
    void connect(in int status);
}