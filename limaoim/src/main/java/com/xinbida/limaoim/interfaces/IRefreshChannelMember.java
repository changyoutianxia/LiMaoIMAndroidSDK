package com.xinbida.limaoim.interfaces;


import com.xinbida.limaoim.entity.LiMChannelMember;

/**
 * 2020-02-01 15:19
 * 刷新频道成员信息
 */
public interface IRefreshChannelMember {
    void onRefresh(LiMChannelMember liMChannelMember);
}
