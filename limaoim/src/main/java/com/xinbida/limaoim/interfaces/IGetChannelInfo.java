package com.xinbida.limaoim.interfaces;


import com.xinbida.limaoim.entity.LiMChannel;

/**
 * 2019-12-01 15:40
 * 获取频道信息
 */
public interface IGetChannelInfo {
    LiMChannel onGetChannelInfo(String channelId, byte channelType, IChannelInfoListener iChannelInfoListener);
}
