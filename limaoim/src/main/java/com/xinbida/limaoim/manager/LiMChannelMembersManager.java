package com.xinbida.limaoim.manager;

import android.text.TextUtils;

import com.xinbida.limaoim.LiMaoIMApplication;
import com.xinbida.limaoim.db.LiMChannelMembersDbManager;
import com.xinbida.limaoim.db.LiMDBColumns;
import com.xinbida.limaoim.entity.LiMChannelMember;
import com.xinbida.limaoim.interfaces.IAddChannelMemberListener;
import com.xinbida.limaoim.interfaces.IChannelMemberInfoListener;
import com.xinbida.limaoim.interfaces.IGetChannelMemberInfo;
import com.xinbida.limaoim.interfaces.IRefreshChannelMember;
import com.xinbida.limaoim.interfaces.IRemoveChannelMember;
import com.xinbida.limaoim.interfaces.ISyncChannelMembers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 5/20/21 5:50 PM
 * channel members 管理
 */
public class LiMChannelMembersManager extends LiMBaseManager {
    private LiMChannelMembersManager() {
    }

    private static class LiMChannelMembersManagerBinder {
        static final LiMChannelMembersManager channelMembersManager = new LiMChannelMembersManager();
    }

    public static LiMChannelMembersManager getInstance() {
        return LiMChannelMembersManagerBinder.channelMembersManager;
    }

    private ConcurrentHashMap<String, IRefreshChannelMember> refreshMemberMap;
    private ConcurrentHashMap<String, IRemoveChannelMember> removeChannelMemberMap;//监听添加频道成员
    private ConcurrentHashMap<String, IAddChannelMemberListener> addChannelMemberMap;
    private ISyncChannelMembers syncChannelMembers;
    //获取频道成员监听
    private IGetChannelMemberInfo iGetChannelMemberInfoListener;


    //最大版本成员
    public LiMChannelMember getMaxVersionMember(String channelID, byte channelType) {
        return LiMChannelMembersDbManager.getInstance().getMaxVersionMember(channelID, channelType);
    }
    /**
     * 批量保存成员
     *
     * @param list 成员数据
     */
    public synchronized void saveChannelMembers(List<LiMChannelMember> list) {
        List<LiMChannelMember> deleteList = new ArrayList<>();
        List<LiMChannelMember> otherList = new ArrayList<>();
        if (list != null && list.size() > 0) {
            for (int i = 0, size = list.size(); i < size; i++) {
                if (list.get(i).isDeleted == 1) {
                    deleteList.add(list.get(i));
                } else otherList.add(list.get(i));
            }
        }
        //移除对应的频道成员
        LiMChannelMembersDbManager.getInstance().deleteChannelMembers(deleteList);
        //需要添加的频道成员
        List<LiMChannelMember> addList = new ArrayList<>();
        try {
            LiMaoIMApplication.getInstance().getDbHelper().getDb()
                    .beginTransaction();
            if (otherList.size() > 0) {
                for (int i = 0, size = otherList.size(); i < size; i++) {
                    LiMChannelMember liMChannelMember = LiMChannelMembersDbManager.getInstance().query(otherList.get(i).channelID, otherList.get(i).channelType, otherList.get(i).memberUID);
                    if (liMChannelMember == null || (liMChannelMember.isDeleted == 1 && otherList.get(i).isDeleted == 0)) {
                        addList.add(otherList.get(i));
                    }
                }
            }
            LiMaoIMApplication.getInstance().getDbHelper().getDb()
                    .setTransactionSuccessful();
        } catch (Exception ignored) {
        } finally {
            if (LiMaoIMApplication.getInstance().getDbHelper().getDb().inTransaction()) {
                LiMaoIMApplication.getInstance().getDbHelper().getDb()
                        .endTransaction();
            }
        }
        if (otherList.size() > 0) {
            LiMChannelMembersDbManager.getInstance().insertChannelMember(otherList);
            for (int i = 0, size = otherList.size(); i < size; i++) {
                setRefreshChannelMember(otherList.get(i));
            }
        }
        setOnAddChannelMember(addList);
    }

    /**
     * 批量移除频道成员
     *
     * @param list 频道成员
     */
    public void deleteChannelMembers(List<LiMChannelMember> list) {
        runOnMainThread(() -> LiMChannelMembersDbManager.getInstance().deleteChannelMembers(list));
    }
    /**
     * 通过状态查询频道成员
     *
     * @param channelId   频道ID
     * @param channelType 频道类型
     * @param status      状态
     * @return List<>
     */
    public List<LiMChannelMember> getLimChannelMembersByStatus(String channelId, byte channelType, int status) {
        return LiMChannelMembersDbManager.getInstance().queryLiMChannelMembersByStatus(channelId, channelType, status);
    }

    /**
     * 修改频道成员备注
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param uid         用户ID
     * @param remarkName  备注
     */
    public boolean updateRemarkName(String channelID, byte channelType, String uid, String remarkName) {
        return LiMChannelMembersDbManager.getInstance().updateChannelMember(channelID, channelType, uid, LiMDBColumns.LiMChannelMembersColumns.member_remark, remarkName);
    }

    /**
     * 修改频道成员名称
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param uid         用户ID
     * @param name        名称
     */
    public boolean updateMemberName(String channelID, byte channelType, String uid, String name) {
        return LiMChannelMembersDbManager.getInstance().updateChannelMember(channelID, channelType, uid, LiMDBColumns.LiMChannelMembersColumns.member_name, name);
    }
    /**
     * 修改频道成员状态
     *
     * @param channelId   频道ID
     * @param channelType 频道类型
     * @param uid         用户ID
     * @param status      状态
     */
    public boolean updateMemberStatus(String channelId, byte channelType, String uid, int status) {
        return LiMChannelMembersDbManager.getInstance().updateChannelMember(channelId, channelType, uid, LiMDBColumns.LiMChannelMembersColumns.status, String.valueOf(status));
    }

    public void addOnGetChannelMemberInfoListener(IGetChannelMemberInfo iGetChannelMemberInfoListener) {
        this.iGetChannelMemberInfoListener = iGetChannelMemberInfoListener;
    }

    public void refreshChannelMemberInfoCache(LiMChannelMember liMChannelMember) {
        if (liMChannelMember == null) return;
        List<LiMChannelMember> list = new ArrayList<>();
        list.add(liMChannelMember);
        LiMChannelMembersDbManager.getInstance().insertChannelMember(list);
    }

    /**
     * 添加加入频道成员监听
     *
     * @param listener 回调
     */
    public void addOnAddChannelMemberListener(String key, IAddChannelMemberListener listener) {
        if (TextUtils.isEmpty(key) || listener == null) return;
        if (addChannelMemberMap == null)
            addChannelMemberMap = new ConcurrentHashMap<>();
        addChannelMemberMap.put(key, listener);
    }

    public void removeAddChannelMemberListener(String key) {
        if (TextUtils.isEmpty(key) || addChannelMemberMap == null) return;
        addChannelMemberMap.remove(key);
    }

    public void setOnAddChannelMember(List<LiMChannelMember> list) {
        if (addChannelMemberMap != null) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IAddChannelMemberListener> entry : addChannelMemberMap.entrySet()) {
                    entry.getValue().onAddMembers(list);
                }
            });
        }
    }

    /**
     * 获取频道成员信息
     *
     * @param channelId                  频道ID
     * @param uid                        成员ID
     * @param iChannelMemberInfoListener 回调
     */
    public LiMChannelMember getChannelMemberInfo(String channelId, byte channelType, String uid, IChannelMemberInfoListener iChannelMemberInfoListener) {
        if (iGetChannelMemberInfoListener != null && !TextUtils.isEmpty(channelId) && !TextUtils.isEmpty(uid) && iChannelMemberInfoListener != null) {
            return iGetChannelMemberInfoListener.onResult(channelId, channelType, uid, iChannelMemberInfoListener);
        } else return null;
    }

    public LiMChannelMember getLiMChannelMember(String channelID, byte channelType, String uid) {
        return LiMChannelMembersDbManager.getInstance().query(channelID, channelType, uid);
    }

    public List<LiMChannelMember> getLiMChannelMembers(String channelID, byte channelType) {
        return LiMChannelMembersDbManager.getInstance().query(channelID, channelType);
    }

    //成员数量
    public int getMembersCount(String channelID, byte channelType) {
        return LiMChannelMembersDbManager.getInstance().getMembersCount(channelID, channelType);
    }

    public void addOnRefreshChannelMemberInfo(String key, IRefreshChannelMember iRefreshChannelMemberListener) {
        if (TextUtils.isEmpty(key) || iRefreshChannelMemberListener == null) return;
        if (refreshMemberMap == null)
            refreshMemberMap = new ConcurrentHashMap<>();
        refreshMemberMap.put(key, iRefreshChannelMemberListener);
    }

    public void removeRefreshChannelMemberInfo(String key) {
        if (TextUtils.isEmpty(key) || refreshMemberMap == null) return;
        refreshMemberMap.remove(key);
    }

    public void setRefreshChannelMember(LiMChannelMember liMChannelMember) {
        if (refreshMemberMap != null && liMChannelMember != null) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IRefreshChannelMember> entry : refreshMemberMap.entrySet()) {
                    entry.getValue().onRefresh(liMChannelMember);
                }
            });
        }
    }

    public void addOnRemoveChannelMemberListener(String key, IRemoveChannelMember listener) {
        if (listener == null || TextUtils.isEmpty(key)) return;
        if (removeChannelMemberMap == null) removeChannelMemberMap = new ConcurrentHashMap<>();
        removeChannelMemberMap.put(key, listener);
    }

    public void removeRemoveChannelMemberListener(String key) {
        if (TextUtils.isEmpty(key) || removeChannelMemberMap == null) return;
        removeChannelMemberMap.remove(key);
    }

    public void setOnRemoveChannelMember(List<LiMChannelMember> list) {
        if (removeChannelMemberMap != null) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IRemoveChannelMember> entry : removeChannelMemberMap.entrySet()) {
                    entry.getValue().onRemoveMembers(list);
                }
            });
        }
    }

    public void addOnSyncChannelMembers(ISyncChannelMembers syncChannelMembersListener) {
        this.syncChannelMembers = syncChannelMembersListener;
    }

    public void setOnSyncChannelMembers(String channelID, byte channelType) {
        if (syncChannelMembers != null) {
            runOnMainThread(() -> {
                syncChannelMembers.onSyncChannelMembers(channelID, channelType);
            });
        }
    }
}
