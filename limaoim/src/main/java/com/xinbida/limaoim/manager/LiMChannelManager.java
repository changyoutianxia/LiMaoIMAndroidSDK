package com.xinbida.limaoim.manager;

import android.text.TextUtils;

import com.xinbida.limaoim.LiMaoIMApplication;
import com.xinbida.limaoim.db.LiMChannelDBManager;
import com.xinbida.limaoim.db.LiMChannelMembersDbManager;
import com.xinbida.limaoim.db.LiMDBColumns;
import com.xinbida.limaoim.entity.LiMChannel;
import com.xinbida.limaoim.entity.LiMChannelMember;
import com.xinbida.limaoim.entity.LiMChannelSearchResult;
import com.xinbida.limaoim.entity.LiMMsg;
import com.xinbida.limaoim.interfaces.IChannelInfoListener;
import com.xinbida.limaoim.interfaces.IGetChannelInfo;
import com.xinbida.limaoim.interfaces.IRefreshChannel;
import com.xinbida.limaoim.interfaces.IRefreshChannelAvatar;
import com.xinbida.limaoim.message.type.LiMChannelType;
import com.xinbida.limaoim.message.type.LiMMsgContentType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 5/20/21 5:49 PM
 * channel管理
 */
public class LiMChannelManager extends LiMBaseManager {
    private LiMChannelManager() {
    }

    private static class LiMChannelManagerBinder {
        static final LiMChannelManager channelManager = new LiMChannelManager();
    }

    public static LiMChannelManager getInstance() {
        return LiMChannelManagerBinder.channelManager;
    }

    private IRefreshChannelAvatar iRefreshChannelAvatar;
    private IGetChannelInfo iGetChannelInfo;
    private final List<LiMChannel> liMChannelList = Collections.synchronizedList(new ArrayList<>());
    //监听刷新频道
    private ConcurrentHashMap<String, IRefreshChannel> refreshChannelMap;

    public void checkChannelInfo(LiMMsg mMsg) {
        if (mMsg.type == LiMMsgContentType.LIM_INSIDE_MSG) return;
        LiMChannel channel = getLiMChannel(mMsg.channelID, mMsg.channelType);
        if (channel == null || TextUtils.isEmpty(channel.channelID)) {
            //更新频道信息
            channel = getChannelInfo(mMsg.channelID, mMsg.channelType, liMChannel -> LiMChannelDBManager.getInstance().insertOrUpdateChannel(liMChannel));
            if (channel != null && !TextUtils.isEmpty(channel.channelID))
                LiMChannelDBManager.getInstance().insertOrUpdateChannel(channel);
        } else {
            //存在频道信息就判断如果是单聊就直接转成用户信息
            if (mMsg.channelType == LiMChannelType.PERSONAL) {
                //这里的频道要取fromUid
                LiMChannel liMChannel = getLiMChannel(mMsg.fromUID, LiMChannelType.PERSONAL);//LiMChannelDbManager.getInstance().getChannle(mMsg.from_uid, LiMChannelType.PERSONAL);
                if (liMChannel == null || TextUtils.isEmpty(liMChannel.channelID)) {
                    liMChannel = getChannelInfo(mMsg.channelID, mMsg.channelType, liMChannel1 -> LiMChannelDBManager.getInstance().insertOrUpdateChannel(liMChannel1));
                    if (liMChannel != null && !TextUtils.isEmpty(channel.channelID))
                        LiMChannelDBManager.getInstance().insertOrUpdateChannel(liMChannel);
                }
                mMsg.setFrom(liMChannel);
            }
        }

        //内部消息不需要获取频道成员信息
        if (mMsg.channelType == LiMChannelType.GROUP && mMsg.type != LiMMsgContentType.LIM_INSIDE_MSG && mMsg.type < 1000) {
            //群聊信息就获取某个人在该频道的信息
            LiMChannelMember liMChannelMember = LiMChannelMembersDbManager.getInstance().query(mMsg.channelID, LiMChannelType.GROUP, mMsg.fromUID);
            if (liMChannelMember == null) {
                liMChannelMember = LiMChannelMembersManager.getInstance().getChannelMemberInfo(mMsg.channelID, mMsg.channelType, mMsg.fromUID, liMChannelMember1 -> LiMChannelMembersDbManager.getInstance().saveOrUpdateChannelMember(liMChannelMember1));
                if (liMChannelMember != null) {
                    LiMChannelMembersDbManager.getInstance().saveOrUpdateChannelMember(liMChannelMember);
                    mMsg.setMemberOfFrom(liMChannelMember);
                } else {
                    LiMChannelMember member = new LiMChannelMember();
                    member.memberUID = mMsg.fromUID;
                    mMsg.setMemberOfFrom(member);
                }
            } else {
                mMsg.setMemberOfFrom(liMChannelMember);
            }
        }
    }

    public synchronized LiMChannel getLiMChannel(String channelID, byte channelType) {
        if (TextUtils.isEmpty(channelID)) return null;
        LiMChannel liMChannel = null;
        for (LiMChannel channel : liMChannelList) {
            if (channel != null && channel.channelID.equals(channelID) && channel.channelType == channelType) {
                liMChannel = channel;
                break;
            }
        }
        if (liMChannel == null) {
            liMChannel = LiMChannelDBManager.getInstance().getChannel(channelID, channelType);
            if (liMChannel != null) {
                liMChannelList.add(liMChannel);
            }
        }
        return liMChannel;
    }

    public void fetchChannelInfo(String channelID, byte channelType) {
        if (TextUtils.isEmpty(channelID)) return;
        getChannelInfo(channelID, channelType, liMChannel -> {
            if (liMChannel != null)
                addOrUpdateChannel(liMChannel);
        });
    }

    public LiMChannel getChannelInfo(String channelId, byte channelType, IChannelInfoListener iChannelInfoListener) {
        if (this.iGetChannelInfo != null && !TextUtils.isEmpty(channelId) && iChannelInfoListener != null) {
            return iGetChannelInfo.onGetChannelInfo(channelId, channelType, iChannelInfoListener);
        } else return null;
    }

    public void addOnGetChannelInfoListener(IGetChannelInfo iGetChannelInfoListener) {
        this.iGetChannelInfo = iGetChannelInfoListener;
    }

    public void addOrUpdateChannel(LiMChannel liMChannel) {
        //先更改内存数据
        if (liMChannel == null) return;
        updateChannel(liMChannel);
        setRefreshChannel(liMChannel);
        LiMChannelDBManager.getInstance().insertOrUpdateChannel(liMChannel);
    }


    /**
     * 修改频道信息
     *
     * @param liMChannel 频道
     */
    private void updateChannel(LiMChannel liMChannel) {
        if (liMChannel == null) return;
        boolean isAdd = true;
        for (int i = 0, size = liMChannelList.size(); i < size; i++) {
            if (liMChannelList.get(i).channelID.equals(liMChannel.channelID) && liMChannelList.get(i).channelType == liMChannel.channelType) {
                isAdd = false;
                liMChannelList.get(i).forbidden = liMChannel.forbidden;
                liMChannelList.get(i).channelName = liMChannel.channelName;
                liMChannelList.get(i).avatar = liMChannel.avatar;
                liMChannelList.get(i).category = liMChannel.category;
                liMChannelList.get(i).lastOffline = liMChannel.lastOffline;
                liMChannelList.get(i).online = liMChannel.online;
                liMChannelList.get(i).follow = liMChannel.follow;
                liMChannelList.get(i).top = liMChannel.top;
                liMChannelList.get(i).channelRemark = liMChannel.channelRemark;
                liMChannelList.get(i).status = liMChannel.status;
                liMChannelList.get(i).version = liMChannel.version;
                liMChannelList.get(i).invite = liMChannel.invite;
                liMChannelList.get(i).extraMap = liMChannel.extraMap;
                liMChannelList.get(i).mute = liMChannel.mute;
                liMChannelList.get(i).save = liMChannel.save;
                liMChannelList.get(i).showNick = liMChannel.showNick;
                liMChannelList.get(i).isDeleted = liMChannel.isDeleted;
                liMChannelList.get(i).receipt = liMChannel.receipt;
                break;
            }
        }
        if (isAdd) {
            liMChannelList.add(liMChannel);
        }
    }

    public void updateChannel(String channelID, byte channelType, String key, Object value) {
        if (TextUtils.isEmpty(channelID) || TextUtils.isEmpty(key)) return;
        for (int i = 0, size = liMChannelList.size(); i < size; i++) {
            if (liMChannelList.get(i).channelID.equals(channelID) && liMChannelList.get(i).channelType == channelType) {
                switch (key) {
                    case LiMDBColumns.LiMChannelColumns.avatar:
                        liMChannelList.get(i).avatar = (String) value;
                        break;
                    case LiMDBColumns.LiMChannelColumns.channel_remark:
                        liMChannelList.get(i).channelRemark = (String) value;
                        break;
                    case LiMDBColumns.LiMChannelColumns.channel_name:
                        liMChannelList.get(i).channelName = (String) value;
                        break;
                    case LiMDBColumns.LiMChannelColumns.follow:
                        liMChannelList.get(i).follow = (int) value;
                        break;
                    case LiMDBColumns.LiMChannelColumns.forbidden:
                        liMChannelList.get(i).forbidden = (int) value;
                        break;
                    case LiMDBColumns.LiMChannelColumns.invite:
                        liMChannelList.get(i).invite = (int) value;
                        break;
                    case LiMDBColumns.LiMChannelColumns.is_deleted:
                        liMChannelList.get(i).isDeleted = (int) value;
                        break;
                    case LiMDBColumns.LiMChannelColumns.last_offline:
                        liMChannelList.get(i).lastOffline = (long) value;
                        break;
                    case LiMDBColumns.LiMChannelColumns.mute:
                        liMChannelList.get(i).mute = (int) value;
                        break;
                    case LiMDBColumns.LiMChannelColumns.top:
                        liMChannelList.get(i).top = (int) value;
                        break;
                    case LiMDBColumns.LiMChannelColumns.online:
                        liMChannelList.get(i).online = (int) value;
                        break;
                    case LiMDBColumns.LiMChannelColumns.receipt:
                        liMChannelList.get(i).receipt = (int) value;
                        break;
                    case LiMDBColumns.LiMChannelColumns.save:
                        liMChannelList.get(i).save = (int) value;
                        break;
                    case LiMDBColumns.LiMChannelColumns.show_nick:
                        liMChannelList.get(i).showNick = (int) value;
                        break;
                    case LiMDBColumns.LiMChannelColumns.status:
                        liMChannelList.get(i).status = (int) value;
                        break;
                    case LiMDBColumns.LiMChannelColumns.extra:
                        liMChannelList.get(i).extraMap = (HashMap<String, Object>) value;
                        break;
                }
                setRefreshChannel(liMChannelList.get(i));
                break;
            }
        }
    }

    /**
     * 添加或修改频道信息
     *
     * @param list 频道数据
     */
    public void addOrUpdateChannels(List<LiMChannel> list) {

        try {
            LiMaoIMApplication.getInstance().getDbHelper().getDb()
                    .beginTransaction();
            if (list.size() > 0) {
                for (int i = 0, size = list.size(); i < size; i++) {
                    LiMChannelDBManager.getInstance().insertOrUpdateChannel(list.get(i));
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

    }

    /**
     * 修改频道状态
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param status      状态
     */
    public void updateStatus(String channelID, byte channelType, int status) {
        updateChannel(channelID, channelType, LiMDBColumns.LiMChannelColumns.status, status);
        LiMChannelDBManager.getInstance().updateChannel(channelID, channelType, LiMDBColumns.LiMChannelColumns.status, String.valueOf(status));
    }


    /**
     * 修改频道名称
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param name        名称
     */
    public void updateName(String channelID, byte channelType, String name) {
        updateChannel(channelID, channelType, LiMDBColumns.LiMChannelColumns.channel_name, name);
        LiMChannelDBManager.getInstance().updateChannel(channelID, channelType, LiMDBColumns.LiMChannelColumns.channel_name, name);
    }

    /**
     * 获取频道信息
     *
     * @param channelType 频道类型
     * @param status      状态
     * @return List<LiMChannel>
     */
    public List<LiMChannel> getChannelsWithStatus(byte channelType, int status) {
        return LiMChannelDBManager.getInstance().queryAllByStatus(channelType, status);
    }

    /**
     * 搜索频道
     *
     * @param keyword 关键字
     * @return List<LiMChannelSearchResult>
     */
    public List<LiMChannelSearchResult> searchChannel(String keyword) {
        return LiMChannelDBManager.getInstance().searchLiMChannelInfo(keyword);
    }

    /**
     * 搜索频道
     *
     * @param keyword     关键字
     * @param channelType 频道类型
     * @return List<LiMChannel>
     */
    public List<LiMChannel> searchChannelsByChannelType(String keyword, byte channelType) {
        return LiMChannelDBManager.getInstance().searchLiMChannels(keyword, channelType);
    }


    /**
     * 获取频道信息
     *
     * @param channelType 频道类型
     * @param follow      关注状态
     * @return List<LiMChannel>
     */
    public List<LiMChannel> getChannelsWithFollow(byte channelType, int follow) {
        return LiMChannelDBManager.getInstance().queryAllByFollow(channelType, follow);
    }

    /**
     * 修改某个频道免打扰
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param isMute      1：免打扰
     */
    public void updateMute(String channelID, byte channelType, int isMute) {
        updateChannel(channelID, channelType, LiMDBColumns.LiMChannelColumns.mute, isMute);
        LiMChannelDBManager.getInstance().updateChannel(channelID, channelType, LiMDBColumns.LiMChannelColumns.mute, String.valueOf(isMute));
    }

    /**
     * 修改备注信息
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param hashExtra   扩展字段
     */
    public void updateExtra(String channelID, byte channelType, HashMap<String, Object> hashExtra) {
        updateChannel(channelID, channelType, LiMDBColumns.LiMChannelColumns.extra, hashExtra);
        if (hashExtra != null) {
            JSONObject jsonObject = new JSONObject();
            for (String key : hashExtra.keySet()) {
                try {
                    jsonObject.put(key, hashExtra.get(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            LiMChannelDBManager.getInstance().updateChannel(channelID, channelType, LiMDBColumns.LiMChannelColumns.extra, jsonObject.toString());
        }
    }

    /**
     * 修改频道是否保存在通讯录
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param isSave      1:保存
     */
    public void updateSave(String channelID, byte channelType, int isSave) {
        updateChannel(channelID, channelType, LiMDBColumns.LiMChannelColumns.save, isSave);
        LiMChannelDBManager.getInstance().updateChannel(channelID, channelType, LiMDBColumns.LiMChannelColumns.save, String.valueOf(isSave));
    }

    /**
     * 是否显示频道昵称
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param showNick    1：显示频道昵称
     */
    public void updateShowNick(String channelID, byte channelType, int showNick) {
        updateChannel(channelID, channelType, LiMDBColumns.LiMChannelColumns.show_nick, showNick);
        LiMChannelDBManager.getInstance().updateChannel(channelID, channelType, LiMDBColumns.LiMChannelColumns.show_nick, String.valueOf(showNick));
    }

    /**
     * 修改某个频道是否置顶
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param top         1：置顶
     */
    public void updateTop(String channelID, byte channelType, int top) {
        updateChannel(channelID, channelType, LiMDBColumns.LiMChannelColumns.top, top);
        LiMChannelDBManager.getInstance().updateChannel(channelID, channelType, LiMDBColumns.LiMChannelColumns.top, String.valueOf(top));
    }

    /**
     * 刷新频道信息
     *
     * @param liMChannel 频道
     */
    public void refreshChannelCache(LiMChannel liMChannel) {
        addOrUpdateChannel(liMChannel);
    }

    /**
     * 修改某个频道的备注
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param remark      备注
     */
    public void updateRemark(String channelID, byte channelType, String remark) {
        updateChannel(channelID, channelType, LiMDBColumns.LiMChannelColumns.channel_remark, remark);
        LiMChannelDBManager.getInstance().updateChannel(channelID, channelType, LiMDBColumns.LiMChannelColumns.channel_remark, remark);
    }

    /**
     * 修改关注状态
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param follow      是否关注
     */
    public void updateFollow(String channelID, byte channelType, int follow) {
        updateChannel(channelID, channelType, LiMDBColumns.LiMChannelColumns.follow, follow);
        LiMChannelDBManager.getInstance().updateChannel(channelID, channelType, LiMDBColumns.LiMChannelColumns.follow, String.valueOf(follow));
    }

    /**
     * 通过follow和status查询频道
     *
     * @param channelType 频道类型
     * @param follow      是否关注 好友或陌生人
     * @param status      状态 正常或黑名单
     * @return list
     */
    public List<LiMChannel> getChannelsWithFollowAndStatus(byte channelType, int follow, int status) {
        return LiMChannelDBManager.getInstance().queryAllByFollowAndStatus(channelType, follow, status);
    }

    public void updateAvatar(String channelID, byte channelType, String avatar) {
        updateChannel(channelID, channelType, LiMDBColumns.LiMChannelColumns.avatar, avatar);
        LiMChannelDBManager.getInstance().updateChannel(channelID, channelType, LiMDBColumns.LiMChannelColumns.avatar, avatar);
    }

    public void addOnRefreshChannelAvatar(IRefreshChannelAvatar iRefreshChannelAvatar) {
        this.iRefreshChannelAvatar = iRefreshChannelAvatar;
    }

    public void setOnRefreshChannelAvatar(String channelID, byte channelType) {
        if (iRefreshChannelAvatar != null) {
            runOnMainThread(() -> iRefreshChannelAvatar.onRefreshChannelAvatar(channelID, channelType));
        }
    }


    public void setRefreshChannel(LiMChannel liMChannel) {
        if (refreshChannelMap != null) {
            runOnMainThread(() -> {
                updateChannel(liMChannel);
                for (Map.Entry<String, IRefreshChannel> entry : refreshChannelMap.entrySet()) {
                    entry.getValue().onRefreshChannel(liMChannel);
                }
            });
        }
    }

    public void addOnRefreshChannelInfo(String key, IRefreshChannel iRefreshChannelListener) {
        if (TextUtils.isEmpty(key)) return;
        if (refreshChannelMap == null) refreshChannelMap = new ConcurrentHashMap<>();
        if (iRefreshChannelListener != null)
            refreshChannelMap.put(key, iRefreshChannelListener);
    }

    /**
     * 移除频道刷新监听
     *
     * @param key 标志
     */
    public void removeRefreshChannelInfo(String key) {
        if (TextUtils.isEmpty(key) || refreshChannelMap == null) return;
        refreshChannelMap.remove(key);
    }

}
