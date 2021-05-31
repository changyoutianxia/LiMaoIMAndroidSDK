package com.xinbida.limaoim.interfaces;


import com.xinbida.limaoim.entity.LiMChannelMember;

import java.util.List;

/**
 * 2020-02-01 16:43
 * 移除频道成员
 */
public interface IRemoveChannelMember {
    void onRemoveMembers(List<LiMChannelMember> list);
}
