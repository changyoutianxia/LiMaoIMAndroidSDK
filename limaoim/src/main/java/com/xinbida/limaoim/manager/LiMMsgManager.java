package com.xinbida.limaoim.manager;

import android.text.TextUtils;
import android.util.Log;

import com.xinbida.limaoim.LiMaoIM;
import com.xinbida.limaoim.LiMaoIMApplication;
import com.xinbida.limaoim.db.LiMConversationDbManager;
import com.xinbida.limaoim.db.LiMDBColumns;
import com.xinbida.limaoim.db.LiMMsgDbManager;
import com.xinbida.limaoim.entity.LiMConversationMsg;
import com.xinbida.limaoim.entity.LiMMentionInfo;
import com.xinbida.limaoim.entity.LiMMessageGroupByDate;
import com.xinbida.limaoim.entity.LiMMessageSearchResult;
import com.xinbida.limaoim.entity.LiMMsg;
import com.xinbida.limaoim.entity.LiMMsgReaction;
import com.xinbida.limaoim.entity.LiMSyncChannelMsg;
import com.xinbida.limaoim.entity.LiMSyncChat;
import com.xinbida.limaoim.entity.LiMSyncExtraMsg;
import com.xinbida.limaoim.entity.LiMSyncMsg;
import com.xinbida.limaoim.entity.LiMSyncRecent;
import com.xinbida.limaoim.entity.LiMUIConversationMsg;
import com.xinbida.limaoim.interfaces.IDeleteMsgListener;
import com.xinbida.limaoim.interfaces.IGetOrSyncHistoryMsgBack;
import com.xinbida.limaoim.interfaces.IMessageStoreBeforeIntercept;
import com.xinbida.limaoim.interfaces.INewMsgListener;
import com.xinbida.limaoim.interfaces.IRefreshMsg;
import com.xinbida.limaoim.interfaces.ISendACK;
import com.xinbida.limaoim.interfaces.ISendMsgCallBackListener;
import com.xinbida.limaoim.interfaces.ISyncChannelMsgBack;
import com.xinbida.limaoim.interfaces.ISyncChannelMsgListener;
import com.xinbida.limaoim.interfaces.ISyncConversationChat;
import com.xinbida.limaoim.interfaces.ISyncConversationChatBack;
import com.xinbida.limaoim.interfaces.ISyncMsgReaction;
import com.xinbida.limaoim.interfaces.ISyncOfflineMsgBack;
import com.xinbida.limaoim.interfaces.ISyncOfflineMsgListener;
import com.xinbida.limaoim.interfaces.IUploadAttacResultListener;
import com.xinbida.limaoim.interfaces.IUploadAttachmentListener;
import com.xinbida.limaoim.message.LiMMessageHandler;
import com.xinbida.limaoim.message.type.LiMSendMsgResult;
import com.xinbida.limaoim.msgmodel.LiMImageContent;
import com.xinbida.limaoim.msgmodel.LiMReply;
import com.xinbida.limaoim.msgmodel.LiMTextContent;
import com.xinbida.limaoim.msgmodel.LiMVideoContent;
import com.xinbida.limaoim.msgmodel.LiMVoiceContent;
import com.xinbida.limaoim.protocol.LiMMessageContent;
import com.xinbida.limaoim.utils.LiMTypeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 5/20/21 5:38 PM
 * ????????????
 */
public class LiMMsgManager extends LiMBaseManager {
    private LiMMsgManager() {
    }

    private static class LiMMsgManagerBinder {
        static final LiMMsgManager msgManager = new LiMMsgManager();
    }

    public static LiMMsgManager getInstance() {
        return LiMMsgManagerBinder.msgManager;
    }

    // ????????????
    private ConcurrentHashMap<String, IRefreshMsg> refreshMsgListenerMap;
    // ????????????????????????
    private ConcurrentHashMap<String, ISendMsgCallBackListener> sendMsgCallBackListenerHashMap;
    // ??????????????????
    private ConcurrentHashMap<String, IDeleteMsgListener> deleteMsgListenerMap;
    // ????????????ack??????
    private ConcurrentHashMap<String, ISendACK> sendAckListenerMap;
    // ???????????????
    private ConcurrentHashMap<String, INewMsgListener> newMsgListenerMap;
    // ??????????????????
    private ISyncMsgReaction iSyncMsgReaction;
    // ??????????????????
    private IUploadAttachmentListener iUploadAttachmentListener;
    // ??????????????????
    private ISyncOfflineMsgListener iOfflineMsgListener;
    // ??????channel?????????
    private ISyncChannelMsgListener iSyncChannelMsgListener;
    // ??????????????????
    private ISyncConversationChat iSyncConversationChat;
    // ?????????????????????
    private IMessageStoreBeforeIntercept messageStoreBeforeIntercept;
    // ???????????????model
    private List<java.lang.Class<? extends LiMMessageContent>> customContentMsgList;

    // ?????????????????????model
    public void initNormalMsg() {
        if (customContentMsgList == null) {
            customContentMsgList = new ArrayList<>();
            customContentMsgList.add(LiMTextContent.class);
            customContentMsgList.add(LiMImageContent.class);
            customContentMsgList.add(LiMVideoContent.class);
            customContentMsgList.add(LiMVoiceContent.class);
        }
    }

    /**
     * ????????????module
     *
     * @param contentMsg ??????
     */
    public void registerContentMsg(java.lang.Class<? extends LiMMessageContent> contentMsg) {
        if (customContentMsgList == null || customContentMsgList.size() == 0)
            initNormalMsg();
        try {
            boolean isAdd = true;
            for (int i = 0, size = customContentMsgList.size(); i < size; i++) {
                if (customContentMsgList.get(i).getDeclaredConstructor().newInstance().type == contentMsg.newInstance().type) {
                    isAdd = false;
                    break;
                }
            }
            if (isAdd)
                customContentMsgList.add(contentMsg);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    // ??????json????????????model
    public LiMMessageContent getMsgContentModel(JSONObject jsonObject) {
        int type = jsonObject.optInt("type");
        LiMMessageContent liMMessageContent = getMsgContentModel(type, jsonObject);
        return liMMessageContent;
    }

    public LiMMessageContent getMsgContentModel(String jsonStr) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonStr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (jsonObject == null) {
            return new LiMMessageContent();
        } else
            return getMsgContentModel(jsonObject);
    }

    public LiMMessageContent getMsgContentModel(int contentType,
                                                JSONObject jsonObject) {

        LiMMessageContent liMBaseContentMsgModel = getContentMsgModel(contentType, jsonObject);
        if (liMBaseContentMsgModel != null) {

            //??????@????????????
            if (jsonObject.has("mention")) {
                JSONObject tempJson = jsonObject.optJSONObject("mention");
                if (tempJson != null) {
                    //??????@?????????
                    if (tempJson.has("all"))
                        liMBaseContentMsgModel.mention_all = tempJson.optInt("all");
                    JSONArray uids = tempJson.optJSONArray("uids");

                    if (uids != null && uids.length() > 0) {
                        LiMMentionInfo mentionInfo = new LiMMentionInfo();
                        List<String> mentionInfoUIDs = new ArrayList<>();
                        for (int i = 0, size = uids.length(); i < size; i++) {
                            String uid = uids.optString(i);
                            if (uid.equals(LiMaoIMApplication.getInstance().getUid())) {
                                mentionInfo.isMentionMe = true;
                            }
                            mentionInfoUIDs.add(uid);
                        }
                        mentionInfo.uids = mentionInfoUIDs;
                        if (liMBaseContentMsgModel.mention_all == 1) {
                            mentionInfo.isMentionMe = true;
                        }
                        liMBaseContentMsgModel.mentionInfo = mentionInfo;
                    }
                }
            }

            if (jsonObject.has("from_uid"))
                liMBaseContentMsgModel.from_uid = jsonObject.optString("from_uid");

            //???????????????????????????????????????
            if (jsonObject.has("reply")) {
                liMBaseContentMsgModel.reply = new LiMReply();
                JSONObject replyJson = jsonObject.optJSONObject("reply");
                if (replyJson != null) {
                    liMBaseContentMsgModel.reply = liMBaseContentMsgModel.reply.decodeMsg(replyJson);
                }
            }


        }
        return liMBaseContentMsgModel;
    }

    /**
     * ???json???????????????????????????model
     *
     * @param type       content type
     * @param jsonObject content json
     * @return model
     */
    private LiMMessageContent getContentMsgModel(int type, JSONObject jsonObject) {
        java.lang.Class<? extends LiMMessageContent> baseMsg = null;
        if (customContentMsgList != null && customContentMsgList.size() > 0) {
            try {
                for (int i = 0, size = customContentMsgList.size(); i < size; i++) {
                    if (customContentMsgList.get(i).getDeclaredConstructor().newInstance().type == type) {
                        baseMsg = customContentMsgList.get(i);
                        break;
                    }
                }
            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        try {
            // ???????????????model?????????????????????????????????
            if (baseMsg != null) {
                return baseMsg.newInstance().decodeMsg(jsonObject);
            }
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * ?????????????????????????????????
     *
     * @param channelId                ??????ID
     * @param channelType              ????????????
     * @param oldestOrderSeq           ?????????????????????orderSeq
     * @param contain                  ???????????? oldestOrderSeq ????????????
     * @param dropDown                 ?????????
     * @param limit                    ??????????????????
     * @param iGetOrSyncHistoryMsgBack ????????????
     */
    public void getOrSyncHistoryMessages(String channelId, byte channelType, long oldestOrderSeq, boolean contain, boolean dropDown, int limit, final IGetOrSyncHistoryMsgBack iGetOrSyncHistoryMsgBack) {
        LiMMsgDbManager.getInstance().getOrSyncHistoryMessages(channelId, channelType, oldestOrderSeq, contain, dropDown, limit, iGetOrSyncHistoryMsgBack);
    }

    /**
     * ??????????????????
     *
     * @param clientMsgNos ??????????????????
     */
    public void deleteWithClientMsgNo(List<String> clientMsgNos) {
        if (clientMsgNos == null || clientMsgNos.size() == 0) return;
        List<LiMMsg> list = new ArrayList<>();
        try {
            LiMaoIMApplication.getInstance().getDbHelper().getDb()
                    .beginTransaction();
            if (clientMsgNos.size() > 0) {
                for (int i = 0, size = clientMsgNos.size(); i < size; i++) {
                    LiMMsg liMMsg = LiMMsgDbManager.getInstance().deleteMsgWithClientMsgNo(clientMsgNos.get(i));
                    if (liMMsg != null) {
                        list.add(liMMsg);
                    }
                }
                LiMaoIMApplication.getInstance().getDbHelper().getDb()
                        .setTransactionSuccessful();
            }
        } catch (Exception ignored) {
        } finally {
            if (LiMaoIMApplication.getInstance().getDbHelper().getDb().inTransaction()) {
                LiMaoIMApplication.getInstance().getDbHelper().getDb()
                        .endTransaction();
            }
        }
        for (int i = 0, size = list.size(); i < size; i++) {
            setDeleteMsg(list.get(i));
        }
    }

    /**
     * ??????????????????
     *
     * @param client_seq ??????????????????
     */
    public boolean deleteWithClientSeq(long client_seq) {
        return LiMMsgDbManager.getInstance().deleteMsgWithClientSeq(client_seq);
    }

    /**
     * ???????????????????????????
     *
     * @param channelID   ??????ID
     * @param channelType ????????????
     * @param clientMsgNo ???????????????ID
     * @return int
     */
    public int getMsgRowNoWithClientMsgNo(String channelID, byte channelType, String clientMsgNo) {
        LiMMsg liMMsg = LiMMsgDbManager.getInstance().getMsgWithClientMsgNo(clientMsgNo);
        return LiMMsgDbManager.getInstance().getMsgRowNoWithOrderSeq(channelID, channelType, liMMsg == null ? 0 : liMMsg.orderSeq);
    }

    public int getMsgRowNoWithMessageID(String channelID, byte channelType, String messageID) {
        LiMMsg liMMsg = LiMMsgDbManager.getInstance().getMsgWithMessageID(messageID);
        return LiMMsgDbManager.getInstance().getMsgRowNoWithOrderSeq(channelID, channelType, liMMsg == null ? 0 : liMMsg.orderSeq);
    }

    public void deleteWithClientMsgNo(String clientMsgNo) {
        LiMMsg liMMsg = LiMMsgDbManager.getInstance().deleteMsgWithClientMsgNo(clientMsgNo);
        if (liMMsg != null) {
            setDeleteMsg(liMMsg);
        }
    }

    public boolean deleteWithMessageID(String messageID) {
        return LiMMsgDbManager.getInstance().deleteMsgWithMessageID(messageID);
    }

    public boolean updateMsgRevokeWithMessageID(String messageID, int revoke) {
        boolean result = LiMMsgDbManager.getInstance().updateMsgWithMessageID(messageID, LiMDBColumns.LiMMessageColumns.revoke, String.valueOf(revoke));
        if (result) {
            LiMMsg liMMsg = getMessageWithMessageID(messageID);
            updateConversationMsgWithLimMsg(liMMsg);
        }
        return result;
    }

    /**
     * ????????????????????????
     *
     * @param clientMsgNo ???????????????ID
     * @param revoke      ????????????
     */
    public boolean updateMsgRevokeWithClientMsgNo(String clientMsgNo, int revoke) {
        return LiMMsgDbManager.getInstance().updateMsgWithClientMsgNo(clientMsgNo, LiMDBColumns.LiMMessageColumns.revoke, String.valueOf(revoke));
    }

    public LiMMsg getMessageWithMessageID(String messageID) {
        return LiMMsgDbManager.getInstance().getMsgWithMessageID(messageID);
    }

    public int isDeletedMsg(JSONObject jsonObject) {
        int isDelete = 0;
        //??????????????????
        if (jsonObject != null && jsonObject.has("visibles")) {
            boolean isIncludeLoginUser = false;
            JSONArray jsonArray = jsonObject.optJSONArray("visibles");
            if (jsonArray != null && jsonArray.length() > 0) {
                for (int i = 0, size = jsonArray.length(); i < size; i++) {
                    if (jsonArray.optString(i).equals(LiMaoIMApplication.getInstance().getUid())) {
                        isIncludeLoginUser = true;
                        break;
                    }
                }
            }
            isDelete = isIncludeLoginUser ? 0 : 1;
        }
        return isDelete;
    }

    public long getMessageOrderSeq(long messageSeq, String channelID, byte channelType) {
        if (messageSeq == 0) {
            long tempOrderSeq = LiMMsgDbManager.getInstance().getMaxOrderSeq(channelID, channelType);
            return tempOrderSeq + 1;
        } else
            return messageSeq * 1000;
    }

    // ??????????????????
    public void setSyncMsgReaction(String channelID, byte channelType) {
        long maxSeq = LiMMsgDbManager.getInstance().getMaxSeqWithChannel(channelID, channelType);
        if (iSyncMsgReaction != null) {
            runOnMainThread(() -> iSyncMsgReaction.onSyncMsgReaction(channelID, channelType, maxSeq, list -> {
                if (list == null || list.size() == 0) return;
                List<LiMMsgReaction> reactionList = new ArrayList<>();
                List<String> msgIds = new ArrayList<>();
                for (int i = 0, size = list.size(); i < size; i++) {
                    LiMMsgReaction reaction = new LiMMsgReaction();
                    reaction.messageID = list.get(i).message_id;
                    reaction.channelID = list.get(i).channel_id;
                    reaction.channelType = list.get(i).channel_type;
                    reaction.uid = list.get(i).uid;
                    reaction.name = list.get(i).name;
                    reaction.seq = list.get(i).seq;
                    reaction.emoji = list.get(i).emoji;
                    reaction.isDeleted = list.get(i).is_deleted;
                    reaction.createdAt = list.get(i).created_at;
                    msgIds.add(list.get(i).message_id);
                    reactionList.add(reaction);
                }
                saveMsgReactions(reactionList);

                List<LiMMsg> liMMsgList = LiMMsgDbManager.getInstance().queryWithMsgIds(msgIds);
                getMsgReactionsAndRefreshMsg(msgIds, liMMsgList);
            }));
        }
    }

    public int getMaxMessageSeq() {
        return LiMMsgDbManager.getInstance().getMaxMessageSeq();
    }

    public boolean updateConversationMsgWithLimMsg(LiMMsg liMMsg) {
        if (liMMsg == null || TextUtils.isEmpty(liMMsg.channelID)) return false;
        return LiMConversationDbManager.getInstance().saveOrUpdateTopMsg(liMMsg, false, true);
    }

    private void getMsgReactionsAndRefreshMsg(List<String> messageIds, List<LiMMsg> updatedMsgList) {
        List<LiMMsgReaction> reactionList = LiMMsgDbManager.getInstance().queryMsgReactionWithMsgIds(messageIds);
        for (int i = 0, size = updatedMsgList.size(); i < size; i++) {
            for (int j = 0, len = reactionList.size(); j < len; j++) {
                if (updatedMsgList.get(i).messageID.equals(reactionList.get(j).messageID)) {
                    if (updatedMsgList.get(i).reactionList == null)
                        updatedMsgList.get(i).reactionList = new ArrayList<>();
                    updatedMsgList.get(i).reactionList.add(reactionList.get(j));
                }
            }
            setRefreshMsg(updatedMsgList.get(i), i == updatedMsgList.size() - 1);
        }
    }


    public synchronized long getClientSeq() {
        return LiMMsgDbManager.getInstance().getMaxMessageSeq();
    }

    /**
     * ???????????????????????????
     *
     * @param clientMsgNo ?????????ID
     * @param hashExtra   ????????????
     */
    public boolean updateExtraWithClientMsgNo(String clientMsgNo, HashMap<String, Object> hashExtra) {
        if (hashExtra != null) {
            JSONObject jsonObject = new JSONObject();
            for (String key : hashExtra.keySet()) {
                try {
                    jsonObject.put(key, hashExtra.get(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return LiMMsgDbManager.getInstance().updateMsgWithClientMsgNo(clientMsgNo, LiMDBColumns.LiMMessageColumns.extra, jsonObject.toString());
        }

        return false;
    }

    /**
     * ????????????????????????????????????
     *
     * @param channelID   ??????ID
     * @param channelType ????????????
     * @return List<LiMMessageGroupByDate>
     */
    public List<LiMMessageGroupByDate> getMessageGroupByDateWithChannel(String channelID, byte channelType) {
        return LiMMsgDbManager.getInstance().getMessageGroupByDateWithChannel(channelID, channelType);
    }

    public void clearAll() {
        LiMMsgDbManager.getInstance().clearEmpty();
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????
     *
     * @param liMMsg     ????????????
     * @param addRedDots ??????????????????
     */
    public void insertAndUpdateConversationMsg(LiMMsg liMMsg, boolean addRedDots) {
        int refreshType = 0;
        if (!TextUtils.isEmpty(liMMsg.clientMsgNO)) {
            LiMMsg tempMsg = LiMMsgDbManager.getInstance().getMsgWithClientMsgNo(liMMsg.clientMsgNO);
            if (tempMsg != null) {
                refreshType = 1;
            }
        }
        if (liMMsg.orderSeq == 0) {
            long tempOrderSeq = getMessageOrderSeq(0, liMMsg.channelID, liMMsg.channelType);
            liMMsg.orderSeq = tempOrderSeq + 1;
        }
        liMMsg.clientSeq = LiMMsgDbManager.getInstance().insertMsg(liMMsg);
        if (refreshType == 0)
            pushNewMsg(liMMsg);
        else setRefreshMsg(liMMsg, true);
        LiMConversationDbManager.getInstance().saveOrUpdateTopMsg(liMMsg, addRedDots, true);
    }

    /**
     * ???????????????????????????????????????
     *
     * @param channelID      ??????ID
     * @param channelType    ????????????
     * @param oldestOrderSeq ?????????????????????orderSeq
     * @param limit          ??????????????????
     * @param contentTypes   ??????????????????
     * @return List<LiMMsg>
     */
    public List<LiMMsg> searchMsgWithChannelAndContentTypes(String channelID, byte channelType, long oldestOrderSeq, int limit, int[] contentTypes) {
        return LiMMsgDbManager.getInstance().searchChatMsgWithChannelAndTypes(channelID, channelType, oldestOrderSeq, limit, contentTypes);
    }

    /**
     * ???????????????????????????
     *
     * @param channelID   ??????ID
     * @param channelType ????????????
     * @param searchKey   ?????????
     * @return List<LiMMsg>
     */
    public List<LiMMsg> searchWithChannel(String channelID, byte channelType, String searchKey) {
        return LiMMsgDbManager.getInstance().searchLiMMessageWithChannel(channelID, channelType, searchKey);
    }

    public List<LiMMessageSearchResult> search(String searchKey) {
        return LiMMsgDbManager.getInstance().searchLiMMessage(searchKey);
    }

    /**
     * ????????????????????????
     *
     * @param clientMsgNo ?????????ID
     * @param isReaded    1?????????
     */
    public boolean updateVoiceReadStatus(String clientMsgNo, int isReaded) {
        return LiMMsgDbManager.getInstance().updateMsgWithClientMsgNo(clientMsgNo, LiMDBColumns.LiMMessageColumns.voice_status, isReaded + "");
    }

    /**
     * ????????????????????????
     *
     * @param channelId   ??????ID
     * @param channelType ????????????
     */
    public boolean clear(String channelId, byte channelType) {
        return LiMMsgDbManager.getInstance().deleteMsgWithChannel(channelId, channelType);
    }

    /**
     * ?????????????????????
     *
     * @param clientMsgNo       ?????????ID
     * @param liMMessageContent ??????module
     */
    public boolean updateContent(String clientMsgNo, LiMMessageContent liMMessageContent) {
        return LiMMsgDbManager.getInstance().updateMsgWithClientMsgNo(clientMsgNo, LiMDBColumns.LiMMessageColumns.content, liMMessageContent.encodeMsg().toString());
    }

    /**
     * ?????????????????????????????????
     *
     * @param type            ????????????
     * @param oldestClientSeq ???????????????????????????ID
     * @param limit           ??????
     * @return list
     */
    public List<LiMMsg> getMessagesWithType(int type, long oldestClientSeq, int limit) {
        return LiMMsgDbManager.getInstance().getMessagesWithType(type, oldestClientSeq, limit);
    }

    public void insertAndUpdateConversationMsg(LiMMsg liMMsg) {
        int refreshType = 0;
        if (!TextUtils.isEmpty(liMMsg.clientMsgNO)) {
            LiMMsg tempMsg = LiMMsgDbManager.getInstance().getMsgWithClientMsgNo(liMMsg.clientMsgNO);
            if (tempMsg != null) {
                refreshType = 1;
            }
        }
        if (liMMsg.orderSeq == 0) {
            long tempOrderSeq = getMessageOrderSeq(0, liMMsg.channelID, liMMsg.channelType);
            liMMsg.orderSeq = tempOrderSeq + 1;
        }
        LiMMsgDbManager.getInstance().insertMsg(liMMsg);
        if (refreshType == 0)
            pushNewMsg(liMMsg);
        else setRefreshMsg(liMMsg, true);
        LiMConversationDbManager.getInstance().saveOrUpdateTopMsg(liMMsg, 1, true);
    }

    public void updateMsgReadWithMsgIds(List<String> messageIds) {
        if (messageIds == null || messageIds.size() == 0) return;
        List<LiMMsg> updatedMsgList = new ArrayList<>();
        try {
            LiMaoIMApplication.getInstance().getDbHelper().getDb()
                    .beginTransaction();
            for (int i = 0, size = messageIds.size(); i < size; i++) {
                if (TextUtils.isEmpty(messageIds.get(i))) {
                    continue;
                }
                LiMMsg liMMsg = LiMMsgDbManager.getInstance().updateMsgReadWithMsgID(messageIds.get(i), 1);
                if (liMMsg != null) {
                    updatedMsgList.add(liMMsg);
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
        getMsgReactionsAndRefreshMsg(messageIds, updatedMsgList);
    }

    public LiMMsg getMsgMaxExtraVersionWithChannel(String channelID, byte channelType) {
        return LiMMsgDbManager.getInstance().getMsgMaxExtraVersionWithChannel(channelID, channelType);
    }

    public LiMMsg getMessageWithClientMsgNo(String clientMsgNo) {
        return LiMMsgDbManager.getInstance().getMsgWithClientMsgNo(clientMsgNo);
    }

    public void saveSyncExtraMsg(List<LiMSyncExtraMsg> list) {
        if (list == null || list.size() == 0) return;
        List<LiMMsg> updatedMsgList = new ArrayList<>();
        List<String> messageIds = new ArrayList<>();
        try {
            LiMaoIMApplication.getInstance().getDbHelper().getDb()
                    .beginTransaction();
            for (int i = 0, size = list.size(); i < size; i++) {
                if (TextUtils.isEmpty(list.get(i).message_id)) {
                    continue;
                }
                LiMMsg liMMsg = LiMMsgDbManager.getInstance().updateMsgWithSyncExtraMsg(list.get(i));
                if (liMMsg != null) {
                    updatedMsgList.add(liMMsg);
                    messageIds.add(liMMsg.messageID);
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
        getMsgReactionsAndRefreshMsg(messageIds, updatedMsgList);
    }

    public void addOnSyncOfflineMsgListener(ISyncOfflineMsgListener iOfflineMsgListener) {
        this.iOfflineMsgListener = iOfflineMsgListener;
    }

    public void addOnSyncMsgReactionListener(ISyncMsgReaction iSyncMsgReactionListener) {
        if (iSyncMsgReactionListener != null) {
            this.iSyncMsgReaction = iSyncMsgReactionListener;
        }
    }

    //????????????????????????
    public void addOnDeleteMsgListener(String key, IDeleteMsgListener iDeleteMsgListener) {
        if (iDeleteMsgListener == null || TextUtils.isEmpty(key)) return;
        if (deleteMsgListenerMap == null) deleteMsgListenerMap = new ConcurrentHashMap<>();
        deleteMsgListenerMap.put(key, iDeleteMsgListener);
    }

    public void removeDeleteMsgListener(String key) {
        if (TextUtils.isEmpty(key)) return;
        if (deleteMsgListenerMap != null) deleteMsgListenerMap.remove(key);
    }

    //??????????????????
    public void setDeleteMsg(LiMMsg liMMsg) {
        if (deleteMsgListenerMap != null && deleteMsgListenerMap.size() > 0) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IDeleteMsgListener> entry : deleteMsgListenerMap.entrySet()) {
                    entry.getValue().onDeleteMsg(liMMsg);
                }
            });

        }
    }


    private void saveMsgReactions(List<LiMMsgReaction> list) {
        LiMMsgDbManager.getInstance().saveMsgReaction(list);
    }


    public void setSyncOfflineMsg(ISyncOfflineMsgBack iSyncOfflineMsgBack) {
        syncOfflineMsg(iSyncOfflineMsgBack);
    }

    private void syncOfflineMsg(ISyncOfflineMsgBack iSyncOfflineMsgBack) {
        if (iOfflineMsgListener != null) {
            runOnMainThread(() -> {
                int max_message_seq = getMaxMessageSeq();
                iOfflineMsgListener.getOfflineMsgs(max_message_seq, (isEnd, list) -> {
                    //??????????????????
                    saveSyncMsg(list);
                    if (isEnd) {
                        iSyncOfflineMsgBack.onBack(isEnd, null);
                    } else {
                        syncOfflineMsg(iSyncOfflineMsgBack);
                    }
                });
            });
        } else iSyncOfflineMsgBack.onBack(true, null);
    }

    public void addOnSyncConversationListener(ISyncConversationChat iSyncConvChatListener) {
        this.iSyncConversationChat = iSyncConvChatListener;
    }

    public void setSyncConversationListener(ISyncConversationChatBack iSyncConversationChatBack) {
        if (iSyncConversationChat != null) {
            long version = LiMConversationDbManager.getInstance().getMaxVersion();
            String lastMsgSeqStr = LiMConversationDbManager.getInstance().getLastMsgSeqs();
            runOnMainThread(() -> iSyncConversationChat.syncConversationChat(lastMsgSeqStr, 20, version, liMSyncChat -> {
                saveSyncChat(liMSyncChat);
                iSyncConversationChatBack.onBack(liMSyncChat);
            }));
        }
    }


    public void setSendMsgCallback(LiMMsg liMMsg) {
        if (sendMsgCallBackListenerHashMap != null && sendMsgCallBackListenerHashMap.size() > 0) {
            runOnMainThread(() -> {
                for (Map.Entry<String, ISendMsgCallBackListener> entry : sendMsgCallBackListenerHashMap.entrySet()) {
                    entry.getValue().onInsertMsg(liMMsg);
                }
            });
        }
    }

    public void addOnSendMsgCallback(String key, ISendMsgCallBackListener iSendMsgCallBackListener) {
        if (TextUtils.isEmpty(key)) return;
        if (sendMsgCallBackListenerHashMap == null) {
            sendMsgCallBackListenerHashMap = new ConcurrentHashMap<>();
        }
        sendMsgCallBackListenerHashMap.put(key, iSendMsgCallBackListener);
    }

    public void removeSendMsgCallBack(String key) {
        if (sendMsgCallBackListenerHashMap != null) {
            sendMsgCallBackListenerHashMap.remove(key);
        }
    }


    //????????????????????????
    public void addOnSyncChannelMsgListener(ISyncChannelMsgListener listener) {
        this.iSyncChannelMsgListener = listener;
    }

    public void setSyncChannelMsgListener(String channelID, byte channelType, long minMessageSeq, long maxMesageSeq, int limit, boolean reverse, ISyncChannelMsgBack iSyncChannelMsgBack) {
        if (this.iSyncChannelMsgListener != null) {
            runOnMainThread(() -> iSyncChannelMsgListener.syncChannelMsgs(channelID, channelType, minMessageSeq, maxMesageSeq, limit, reverse, new ISyncChannelMsgBack() {
                @Override
                public void onBack(LiMSyncChannelMsg liMSyncChannelMsg) {
                    if (liMSyncChannelMsg != null && liMSyncChannelMsg.messages != null && liMSyncChannelMsg.messages.size() > 0) {
                        saveSyncChannelMSGs(liMSyncChannelMsg.messages);
                    }
                    iSyncChannelMsgBack.onBack(liMSyncChannelMsg);
                }
            }));
        }
    }


    private void saveSyncChannelMSGs(List<LiMSyncRecent> list) {
        if (list == null || list.size() == 0) return;
        List<LiMMsg> msgList = new ArrayList<>();
        for (int j = 0, len = list.size(); j < len; j++) {
            msgList.add(LiMSyncRecent2LiMMsg(list.get(j)));
        }
        if (msgList.size() > 0) {
            try {
                LiMaoIMApplication.getInstance().getDbHelper().getDb()
                        .beginTransaction();
                for (int i = 0, size = msgList.size(); i < size; i++) {
                    LiMMsgDbManager.getInstance().insertMsg(msgList.get(i));
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

    }

    public void addOnSendMsgAckListener(String key, ISendACK iSendACKListener) {
        if (iSendACKListener == null || TextUtils.isEmpty(key)) return;
        if (sendAckListenerMap == null) sendAckListenerMap = new ConcurrentHashMap<>();
        sendAckListenerMap.put(key, iSendACKListener);
    }

    public void setSendMsgAck(long clientSeq, String messageID, long messageSeq, byte reasonCode) {
        if (sendAckListenerMap != null && sendAckListenerMap.size() > 0) {
            runOnMainThread(() -> {
                for (Map.Entry<String, ISendACK> entry : sendAckListenerMap.entrySet()) {
                    entry.getValue().msgACK(clientSeq, messageID, messageSeq, reasonCode);
                }
            });

        }
    }

    public void removeSendMsgAckListener(String key) {
        if (!TextUtils.isEmpty(key) && sendAckListenerMap != null) {
            sendAckListenerMap.remove(key);
        }
    }

    public void addOnUploadAttachListener(IUploadAttachmentListener iUploadAttachmentListener) {
        this.iUploadAttachmentListener = iUploadAttachmentListener;
    }

    public void setUploadAttachment(LiMMsg liMMsg, IUploadAttacResultListener resultListener) {
        if (iUploadAttachmentListener != null) {
            runOnMainThread(() -> {
                iUploadAttachmentListener.onUploadAttachmentListener(liMMsg, resultListener);
            });
        }
    }

    public void addMessageStoreBeforeIntercept(IMessageStoreBeforeIntercept iMessageStoreBeforeInterceptListener) {
        messageStoreBeforeIntercept = iMessageStoreBeforeInterceptListener;
    }

    public boolean setMessageStoreBeforeIntercept(LiMMsg liMMsg) {
        return messageStoreBeforeIntercept == null || messageStoreBeforeIntercept.isSaveMsg(liMMsg);
    }

    //??????????????????
    public void addOnRefreshMsgListener(String key, IRefreshMsg listener) {
        if (TextUtils.isEmpty(key) || listener == null) return;
        if (refreshMsgListenerMap == null) refreshMsgListenerMap = new ConcurrentHashMap<>();
        refreshMsgListenerMap.put(key, listener);
    }


    public void removeRefreshMsgListener(String key) {
        if (!TextUtils.isEmpty(key) && refreshMsgListenerMap != null) {
            refreshMsgListenerMap.remove(key);
        }
    }

    public void setRefreshMsg(LiMMsg liMMsg, boolean left) {
        if (refreshMsgListenerMap != null && refreshMsgListenerMap.size() > 0) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IRefreshMsg> entry : refreshMsgListenerMap.entrySet()) {
                    entry.getValue().onRefresh(liMMsg, left);
                }
            });

        }
    }

    public void addOnNewMsgListener(String key, INewMsgListener iNewMsgListener) {
        if (TextUtils.isEmpty(key) || iNewMsgListener == null) return;
        if (newMsgListenerMap == null)
            newMsgListenerMap = new ConcurrentHashMap<>();
        newMsgListenerMap.put(key, iNewMsgListener);
    }

    public void removeNewMsgListener(String key) {
        if (TextUtils.isEmpty(key)) return;
        if (newMsgListenerMap != null) newMsgListenerMap.remove(key);
    }

    private void saveSyncChat(LiMSyncChat liMSyncChat) {
        if (liMSyncChat == null) return;
        List<LiMConversationMsg> conversationMsgList = new ArrayList<>();
        List<LiMMsg> msgList = new ArrayList<>();
        if (liMSyncChat.conversations != null && liMSyncChat.conversations.size() > 0) {
            for (int i = 0, size = liMSyncChat.conversations.size(); i < size; i++) {
                //????????????????????????
                LiMConversationMsg liMConversationMsg = new LiMConversationMsg();
                liMConversationMsg.channelID = liMSyncChat.conversations.get(i).channel_id;
                liMConversationMsg.channelType = liMSyncChat.conversations.get(i).channel_type;
                liMConversationMsg.lastClientMsgNO = liMSyncChat.conversations.get(i).last_client_msg_no;
                liMConversationMsg.lastMsgTimestamp = liMSyncChat.conversations.get(i).timestamp;
                liMConversationMsg.unreadCount = liMSyncChat.conversations.get(i).unread;
                liMConversationMsg.version = liMSyncChat.conversations.get(i).version;
                liMConversationMsg.status = LiMSendMsgResult.send_success;
                //??????????????????
                if (liMSyncChat.conversations.get(i).recents != null && liMSyncChat.conversations.get(i).recents.size() > 0) {
                    for (int j = 0, len = liMSyncChat.conversations.get(i).recents.size(); j < len; j++) {
                        LiMSyncRecent liMSyncRecent = liMSyncChat.conversations.get(i).recents.get(j);
                        LiMMsg liMMsg = LiMSyncRecent2LiMMsg(liMSyncRecent);
                        //?????????????????????fromUID
                        if (liMConversationMsg.lastClientMsgNO.equals(liMMsg.clientMsgNO)) {
                            liMConversationMsg.fromUID = liMMsg.fromUID;
                            liMConversationMsg.isDeleted = liMMsg.isDeleted;
                        }
                        msgList.add(liMMsg);
                    }
                }
                conversationMsgList.add(liMConversationMsg);
            }
        }

        List<LiMUIConversationMsg> uiMsgList = new ArrayList<>();
        if (conversationMsgList.size() > 0 || msgList.size() > 0) {
            try {
                LiMaoIMApplication.getInstance().getDbHelper().getDb()
                        .beginTransaction();
                if (msgList.size() > 0) {
                    for (int i = 0, size = msgList.size(); i < size; i++) {
                        LiMMsgDbManager.getInstance().insertMsg(msgList.get(i));
                    }
                }
                if (conversationMsgList.size() > 0) {
                    for (int i = 0, size = conversationMsgList.size(); i < size; i++) {
                        LiMUIConversationMsg uiMsg = LiMConversationDbManager.getInstance().insertSyncMsg(conversationMsgList.get(i));
                        if (uiMsg != null) {
                            uiMsgList.add(uiMsg);
                        }
                    }
                }
                LiMaoIMApplication.getInstance().getDbHelper().getDb()
                        .setTransactionSuccessful();
            } catch (Exception ignored) {
                Log.e("??????????????????????????????", "--->");
            } finally {
                if (LiMaoIMApplication.getInstance().getDbHelper().getDb().inTransaction()) {
                    LiMaoIMApplication.getInstance().getDbHelper().getDb()
                            .endTransaction();
                }
            }
            if (msgList.size() > 0) {
                pushNewMsg(msgList);
            }
            if (uiMsgList.size() > 0) {
                for (int i = 0, size = uiMsgList.size(); i < size; i++) {
                    LiMaoIM.getInstance().getLiMConversationManager().setOnRefreshMsg(uiMsgList.get(i), i == uiMsgList.size() - 1);
                }
            }
        }

        if (liMSyncChat.cmds != null && liMSyncChat.cmds.size() > 0) {
            try {
                for (int i = 0, size = liMSyncChat.cmds.size(); i < size; i++) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("cmd", liMSyncChat.cmds.get(i).cmd);
                    JSONObject json = new JSONObject(liMSyncChat.cmds.get(i).param);
                    jsonObject.put("param", json);
                    LiMCMDManager.getInstance().handleCMD(jsonObject);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    private LiMMsg LiMSyncRecent2LiMMsg(LiMSyncRecent liMSyncRecent) {
        LiMMsg liMMsg = new LiMMsg();
        liMMsg.channelID = liMSyncRecent.channel_id;
        liMMsg.channelType = liMSyncRecent.channel_type;
        liMMsg.messageID = liMSyncRecent.message_id;
        liMMsg.messageSeq = liMSyncRecent.message_seq;
        liMMsg.clientMsgNO = liMSyncRecent.client_msg_no;
        liMMsg.fromUID = liMSyncRecent.from_uid;
        liMMsg.timestamp = liMSyncRecent.timestamp;
        liMMsg.orderSeq = liMMsg.messageSeq * 1000;
        liMMsg.voiceStatus = liMSyncRecent.voice_status;
        liMMsg.isDeleted = liMSyncRecent.is_deleted;
        liMMsg.status = LiMSendMsgResult.send_success;
        liMMsg.revoke = liMSyncRecent.revoke;
        liMMsg.revoker = liMSyncRecent.revoker;
        liMMsg.unreadCount = liMSyncRecent.unread_count;
        liMMsg.readedCount = liMSyncRecent.readed_count;
        liMMsg.readed = liMSyncRecent.readed;
        // liMMsg.reactionList = liMSyncRecent.reactions;
        // liMMsg.receipt = liMSyncRecent.receipt;
        liMMsg.extraVersion = liMSyncRecent.extra_version;
        //??????????????????
        byte[] setting = LiMTypeUtils.getInstance().intToByte(liMSyncRecent.setting);
        liMMsg.receipt = LiMTypeUtils.getInstance().getBit(setting[0], 7);
        double f = 0;
        if (liMSyncRecent.payload.containsKey("type")) {
            String type = liMSyncRecent.payload.get("type") + "";
            if (!TextUtils.isEmpty(type)) {
                f = Double.parseDouble(type);
            }

        }
        // ??????????????????
        if (liMSyncRecent.reactions != null && liMSyncRecent.reactions.size() > 0) {
            List<LiMMsgReaction> list = new ArrayList<>();
            for (int i = 0, size = liMSyncRecent.reactions.size(); i < size; i++) {
                LiMMsgReaction reaction = new LiMMsgReaction();
                reaction.channelID = liMSyncRecent.reactions.get(i).channel_id;
                reaction.channelType = liMSyncRecent.reactions.get(i).channel_type;
                reaction.uid = liMSyncRecent.reactions.get(i).uid;
                reaction.name = liMSyncRecent.reactions.get(i).name;
                reaction.emoji = liMSyncRecent.reactions.get(i).emoji;
                reaction.seq = liMSyncRecent.reactions.get(i).seq;
                reaction.isDeleted = liMSyncRecent.reactions.get(i).is_deleted;
                reaction.messageID = liMSyncRecent.reactions.get(i).message_id;
                reaction.createdAt = liMSyncRecent.reactions.get(i).created_at;
                list.add(reaction);
            }
            saveMsgReactions(list);
            liMMsg.reactionList = list;
        }
        liMMsg.type = (int) Math.ceil(f);
        JSONObject jsonObject = new JSONObject(liMSyncRecent.payload);
        if (!jsonObject.has(LiMDBColumns.LiMMessageColumns.from_uid)) {
            try {
                jsonObject.put(LiMDBColumns.LiMMessageColumns.from_uid, liMMsg.fromUID);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        liMMsg.content = jsonObject.toString();
        LiMMessageHandler.getInstance().parsingMsg(liMMsg);
        return liMMsg;
    }

    public void saveSyncMsg(List<LiMSyncMsg> liMMsgList) {
        if (liMMsgList == null || liMMsgList.size() == 0) return;
        for (int i = 0, size = liMMsgList.size(); i < size; i++) {
            liMMsgList.get(i).liMMsg = LiMMessageHandler.getInstance().parsingMsg(liMMsgList.get(i).liMMsg);
            if (liMMsgList.get(i).liMMsg.timestamp != 0)
                liMMsgList.get(i).liMMsg.orderSeq = liMMsgList.get(i).liMMsg.timestamp;
            else
                liMMsgList.get(i).liMMsg.orderSeq = getMessageOrderSeq(liMMsgList.get(i).liMMsg.messageSeq, liMMsgList.get(i).liMMsg.channelID, liMMsgList.get(i).liMMsg.channelType);
        }
        LiMMessageHandler.getInstance().saveSyncMsg(liMMsgList);
    }

    public void pushNewMsg(List<LiMMsg> liMMsgList) {
        if (newMsgListenerMap != null && newMsgListenerMap.size() > 0) {
            runOnMainThread(() -> {
                for (Map.Entry<String, INewMsgListener> entry : newMsgListenerMap.entrySet()) {
                    entry.getValue().newMsg(liMMsgList);
                }
            });
        }
    }

    /**
     * push?????????
     *
     * @param liMMsg ??????
     */
    public void pushNewMsg(LiMMsg liMMsg) {
        if (liMMsg == null) return;
        List<LiMMsg> liMMsgList = new ArrayList<>();
        liMMsgList.add(liMMsg);
        pushNewMsg(liMMsgList);
    }
}
