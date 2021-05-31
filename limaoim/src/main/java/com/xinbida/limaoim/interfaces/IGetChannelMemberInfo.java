package com.xinbida.limaoim.interfaces;


import com.xinbida.limaoim.entity.LiMChannelMember;

/**
 * 2019-12-01 15:52
 * 获取频道成员
 */
public interface IGetChannelMemberInfo {
    LiMChannelMember onResult(String channelId, byte channelType, String uid, IChannelMemberInfoListener iChannelMemberInfoListener);
}
