package com.xinbida.limaoim.interfaces;


import com.xinbida.limaoim.entity.LiMChannelMember;

import java.util.List;

/**
 * 2020-02-01 16:39
 * 添加频道成员
 */
public interface IAddChannelMemberListener {
    void onAddMembers(List<LiMChannelMember> list);
}
