package com.xinbida.limaoim.message;

import android.text.TextUtils;

import com.xinbida.limaoim.LiMaoIM;
import com.xinbida.limaoim.LiMaoIMApplication;
import com.xinbida.limaoim.db.LiMDBColumns;
import com.xinbida.limaoim.db.LiMMsgDbManager;
import com.xinbida.limaoim.entity.LiMMsg;
import com.xinbida.limaoim.message.type.LiMMsgType;
import com.xinbida.limaoim.msgmodel.LiMMediaMessageContent;
import com.xinbida.limaoim.protocol.LiMBaseMsg;
import com.xinbida.limaoim.protocol.LiMConnectAckMsg;
import com.xinbida.limaoim.protocol.LiMConnectMsg;
import com.xinbida.limaoim.protocol.LiMDisconnectMsg;
import com.xinbida.limaoim.protocol.LiMPingMsg;
import com.xinbida.limaoim.protocol.LiMPongMsg;
import com.xinbida.limaoim.protocol.LiMReceivedAckMsg;
import com.xinbida.limaoim.protocol.LiMReceivedMsg;
import com.xinbida.limaoim.protocol.LiMSendAckMsg;
import com.xinbida.limaoim.protocol.LiMSendMsg;
import com.xinbida.limaoim.utils.BigTypeUtils;
import com.xinbida.limaoim.utils.LiMAESEncryptUtils;
import com.xinbida.limaoim.utils.LiMCurve25519Utils;
import com.xinbida.limaoim.utils.LiMLoggerUtils;
import com.xinbida.limaoim.utils.LiMTypeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 5/21/21 11:28 AM
 * 收发消息转换
 */
class MessageConvertHandler {
    private MessageConvertHandler() {
    }

    private static class MessageConvertHandlerBinder {
        static final MessageConvertHandler msgConvert = new MessageConvertHandler();
    }

    public static MessageConvertHandler getInstance() {
        return MessageConvertHandlerBinder.msgConvert;
    }

    byte[] getConnectMsgBytes(LiMConnectMsg liMConnectMsg) {
        int remainingLength = 1 + 1
                + liMConnectMsg.deviceIDLength
                + liMConnectMsg.deviceID.length()
                + liMConnectMsg.clientTimeStampLength
                + liMConnectMsg.uidLength
                + LiMaoIMApplication.getInstance().getUid().length()
                + liMConnectMsg.tokenLength
                + LiMaoIMApplication.getInstance().getToken().length();
        if (LiMaoIMApplication.getInstance().protocolVersion > 2) {
            remainingLength = remainingLength + liMConnectMsg.clientKeyLength + LiMCurve25519Utils.getInstance().getPublicKey().length();
        }
        byte[] remainingBytes = LiMTypeUtils.getInstance().getRemainingLengthByte(remainingLength);
        int totalLen = 1 + remainingBytes.length
                + liMConnectMsg.protocolVersionLength
                + liMConnectMsg.deviceFlagLength
                + liMConnectMsg.deviceIDLength
                + liMConnectMsg.deviceID.length() //设备id长度
                + liMConnectMsg.clientTimeStampLength//时间戳长度
                + liMConnectMsg.uidLength
                + LiMaoIMApplication.getInstance().getUid().length()
                + liMConnectMsg.tokenLength
                + LiMaoIMApplication.getInstance().getToken().length();

        if (LiMaoIMApplication.getInstance().protocolVersion > 2) {
            totalLen = totalLen + liMConnectMsg.clientKeyLength
                    + LiMCurve25519Utils.getInstance().getPublicKey().length();
        }
        byte[] bytes = new byte[totalLen];
        ByteBuffer buffer = ByteBuffer.allocate(totalLen).order(
                ByteOrder.BIG_ENDIAN);
        try {
            //固定头
            buffer.put(LiMTypeUtils.getInstance().getHeader(liMConnectMsg.packetType, liMConnectMsg.flag, 0, 0));
            buffer.put(remainingBytes);
            buffer.put(liMConnectMsg.protocolVersion);
            if (LiMaoIMApplication.getInstance().protocolVersion > 2) {
                buffer.putShort((short) LiMCurve25519Utils.getInstance().getPublicKey().length());
                buffer.put(LiMTypeUtils.getInstance().stringToByte(LiMCurve25519Utils.getInstance().getPublicKey()));
            }
            buffer.put(liMConnectMsg.deviceFlag);
            buffer.putShort((short) liMConnectMsg.deviceID.length());
            buffer.put(LiMTypeUtils.getInstance().stringToByte(liMConnectMsg.deviceID));
            buffer.putLong(liMConnectMsg.clientTimestamp);
            buffer.putShort((short) LiMaoIMApplication.getInstance().getUid().length());
            buffer.put(LiMTypeUtils.getInstance().stringToByte(LiMaoIMApplication.getInstance().getUid()));
            buffer.putShort((short) LiMaoIMApplication.getInstance().getToken().length());
            buffer.put(LiMTypeUtils.getInstance().stringToByte(LiMaoIMApplication.getInstance().getToken()));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        buffer.position(0);
        buffer.get(bytes);
        return bytes;
    }

    synchronized byte[] getReceivedAckMsgBytes(LiMReceivedAckMsg liMReceiveAckMsg) {
        byte[] remainingBytes = LiMTypeUtils.getInstance().getRemainingLengthByte(8 + 4);

        int totalLen = 1 + remainingBytes.length + 8 + 4;
        byte[] bytes = new byte[totalLen];
        ByteBuffer buffer = ByteBuffer.allocate(totalLen).order(
                ByteOrder.BIG_ENDIAN);
        //固定头
        buffer.put(LiMTypeUtils.getInstance().getHeader(liMReceiveAckMsg.packetType, liMReceiveAckMsg.no_persist ? 1 : 0, liMReceiveAckMsg.red_dot ? 1 : 0, liMReceiveAckMsg.sync_once ? 1 : 0));
        buffer.put(remainingBytes);
        BigInteger bigInteger = new BigInteger(liMReceiveAckMsg.messageID);
        buffer.putLong(bigInteger.longValue());
        buffer.putInt(liMReceiveAckMsg.messageSeq);
        buffer.position(0);
        buffer.get(bytes);
        return bytes;
    }

    byte[] getPingMsgBytes(LiMPingMsg liMPingMsg) {
        int totalLen = 1;
        byte[] bytes = new byte[totalLen];
        ByteBuffer buffer = ByteBuffer.allocate(totalLen).order(
                ByteOrder.BIG_ENDIAN);
        //固定头
        buffer.put(LiMTypeUtils.getInstance().getHeader(liMPingMsg.packetType, liMPingMsg.flag, 0, 0));
        buffer.position(0);
        buffer.get(bytes);
        return bytes;
    }

    byte[] getSendMsgBytes(LiMSendMsg liMSendMsg) {
        String sendContent = liMSendMsg.payload;
        String msgKeyContent = "";
        if (LiMaoIMApplication.getInstance().protocolVersion > 2) {
            // 先加密内容
            byte[] contentByte = LiMAESEncryptUtils.aesEncrypt(liMSendMsg.payload, LiMCurve25519Utils.getInstance().aesKey, LiMCurve25519Utils.getInstance().salt);
            sendContent = LiMAESEncryptUtils.base64Encode(contentByte);
            String msgKey = liMSendMsg.clientSeq
                    + liMSendMsg.clientMsgNo
                    + liMSendMsg.channelId
                    + liMSendMsg.channelType
                    + sendContent;
            byte[] msgKeyByte = LiMAESEncryptUtils.aesEncrypt(msgKey, LiMCurve25519Utils.getInstance().aesKey, LiMCurve25519Utils.getInstance().salt);
            msgKeyContent = LiMAESEncryptUtils.base64Encode(msgKeyByte);
            msgKeyContent = LiMAESEncryptUtils.digest(msgKeyContent);
        }

        int remainingLength = liMSendMsg.clientSeqLength
                + liMSendMsg.channelIdLength
                + liMSendMsg.channelId.length()
                + liMSendMsg.clientMsgNoLength
                + liMSendMsg.clientMsgNo.length()
                + liMSendMsg.channelTypeLength
                + liMSendMsg.settingLength
                + sendContent.getBytes().length;
        if (LiMaoIMApplication.getInstance().protocolVersion > 2) {
            remainingLength = remainingLength + liMSendMsg.msgKeyLength + msgKeyContent.length();
        }
        byte[] remainingBytes = LiMTypeUtils.getInstance().getRemainingLengthByte(remainingLength);

        int totalLen = 1 + remainingBytes.length
                + liMSendMsg.clientSeqLength
                + liMSendMsg.channelIdLength
                + liMSendMsg.channelId.length()
                + liMSendMsg.clientMsgNoLength
                + liMSendMsg.clientMsgNo.length()
                + liMSendMsg.channelTypeLength
                + liMSendMsg.settingLength
                + sendContent.getBytes().length;
        if (LiMaoIMApplication.getInstance().protocolVersion > 2) {
            totalLen = totalLen + liMSendMsg.msgKeyLength
                    + msgKeyContent.length();
        }
        byte[] bytes = new byte[totalLen];
        ByteBuffer buffer = ByteBuffer.allocate(totalLen).order(
                ByteOrder.BIG_ENDIAN);

        try {
            //固定头
            buffer.put(LiMTypeUtils.getInstance().getHeader(liMSendMsg.packetType, liMSendMsg.no_persist ? 1 : 0, liMSendMsg.red_dot ? 1 : 0, liMSendMsg.sync_once ? 1 : 0));
            buffer.put(remainingBytes);
            //消息设置
            buffer.put(LiMTypeUtils.getInstance().getMsgSetting(liMSendMsg.receipt));
            if (LiMaoIMApplication.getInstance().protocolVersion > 2) {
                buffer.putShort((short) msgKeyContent.length());
                buffer.put(LiMTypeUtils.getInstance().stringToByte(msgKeyContent));
            }
            buffer.putInt(liMSendMsg.clientSeq);
            buffer.putShort((short) liMSendMsg.clientMsgNo.length());
            buffer.put(LiMTypeUtils.getInstance().stringToByte(liMSendMsg.clientMsgNo));
            buffer.putShort((short) liMSendMsg.channelId.length());
            buffer.put(LiMTypeUtils.getInstance().stringToByte(liMSendMsg.channelId));
            buffer.put(liMSendMsg.channelType);
            buffer.put((LiMTypeUtils.getInstance().stringToByte(sendContent)));
            //  buffer.put((TypeUtils.getInstance().stringToByte(liMSendMsg.payload)));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        buffer.position(0);
        buffer.get(bytes);
        return bytes;
    }

    LiMBaseMsg cutBytesToMsg(byte[] bytes) {
        //包类型
        InputStream inputStream = new ByteArrayInputStream(bytes);
        byte[] fixedHeader = new byte[1];
        try {
            int headerRead = inputStream.read(fixedHeader);
            if (headerRead == -1) return null;
            int packetType = LiMTypeUtils.getInstance().getHeight4(fixedHeader[0]);
            int remainingLength = LiMTypeUtils.getInstance().bytes2Length(inputStream);
            if (packetType == LiMMsgType.CONNACK) {
                //连接ack
                LiMConnectAckMsg liMConnectAckMsg = new LiMConnectAckMsg();
                int read;
                if (LiMaoIMApplication.getInstance().protocolVersion > 2) {
                    // 获取公钥长度
                    byte[] serverKeyLengthByte = new byte[2];
                    read = inputStream.read(serverKeyLengthByte);
                    if (read == -1) return liMConnectAckMsg;
                    short serverKeyLength = BigTypeUtils.getInstance().byteToShort(serverKeyLengthByte);
                    // 服务端公钥
                    byte[] serverKeyByte = new byte[serverKeyLength];
                    read = inputStream.read(serverKeyByte);
                    if (read == -1) return liMConnectAckMsg;
                    String serverKey = LiMTypeUtils.getInstance().bytesToString(serverKeyByte);
                    // 获取安全码AES加密需要
                    byte[] saltLengthByte = new byte[2];
                    read = inputStream.read(saltLengthByte);
                    if (read == -1) return liMConnectAckMsg;
                    short saltLength = BigTypeUtils.getInstance().byteToShort(saltLengthByte);
                    // 安全码
                    byte[] saltByte = new byte[saltLength];
                    read = inputStream.read(saltByte);
                    if (read == -1) return liMConnectAckMsg;
                    String salt = LiMTypeUtils.getInstance().bytesToString(saltByte);
                    liMConnectAckMsg.serverKey = serverKey;
                    liMConnectAckMsg.salt = salt;
                    //保存公钥和安全码
                    LiMCurve25519Utils.getInstance().setServerKeyAndSalt(liMConnectAckMsg.serverKey, liMConnectAckMsg.salt);
                }

                // 客户端时间与服务器的差值，单位毫秒
                byte[] length_byte = new byte[8];
                read = inputStream.read(length_byte);
                if (read == -1) {
                    return liMConnectAckMsg;
                }
                long time = BigTypeUtils.getInstance().bytesToLong(length_byte);
                // 连接原因码
                byte[] reasonByte = new byte[1];
                read = inputStream.read(reasonByte);
                if (read == -1) {
                    return liMConnectAckMsg;
                }
                liMConnectAckMsg.timeDiff = time;
                liMConnectAckMsg.remainingLength = remainingLength;
                liMConnectAckMsg.reasonCode = reasonByte[0];
                return liMConnectAckMsg;
            } else if (packetType == LiMMsgType.SENDACK) {
                LiMSendAckMsg liMSendAckMsg = new LiMSendAckMsg();
                //客户端序列号
                byte[] clientSeq = new byte[4];
                int read = inputStream.read(clientSeq);
                if (read == -1) return liMSendAckMsg;
                liMSendAckMsg.clientSeq = BigTypeUtils.getInstance().bytesToInt(clientSeq);
                //发送消息ack
                byte[] messageId = new byte[8];

                read = inputStream.read(messageId);
                if (read == -1) return liMSendAckMsg;
                BigInteger bigInteger = new BigInteger(messageId);
                if (bigInteger.toString().startsWith("-")) {
                    BigInteger temp = new BigInteger("18446744073709551616");
                    liMSendAckMsg.messageID = temp.add(bigInteger).toString();
                } else
                    liMSendAckMsg.messageID = bigInteger.toString();
                //liMSendAckMsg.messageID = BigTypeUtils.getInstance().bytesToLong(messageId) + "";
                byte[] messageSqe = new byte[4];
                read = inputStream.read(messageSqe);
                if (read == -1) return liMSendAckMsg;
                liMSendAckMsg.messageSeq = BigTypeUtils.getInstance().bytesToInt(messageSqe);
                byte[] reasonCode = new byte[1];
                read = inputStream.read(reasonCode);
                if (read == -1) return liMSendAckMsg;
                liMSendAckMsg.reasonCode = reasonCode[0];
                return liMSendAckMsg;
            } else if (packetType == LiMMsgType.DISCONNECT) {
                LiMDisconnectMsg liMDisconnectMsg = new LiMDisconnectMsg();
                byte[] reasonCode = new byte[1];
                int read = inputStream.read(reasonCode);
                if (read == -1) return liMDisconnectMsg;
                liMDisconnectMsg.reasonCode = reasonCode[0];
                byte[] reasonByte = new byte[remainingLength - 1];
                if (reasonByte.length != 0) {
                    read = inputStream.read(reasonByte);
                    if (read == -1) return liMDisconnectMsg;
                    liMDisconnectMsg.reason = LiMTypeUtils.getInstance().bytesToString(reasonByte);
                }
                return liMDisconnectMsg;
            } else if (packetType == LiMMsgType.RECVEIVED) {
                //接受消息
                LiMReceivedMsg liMRecvMsg = new LiMReceivedMsg();
                int read;
                //消息设置
                byte[] setting = new byte[1];
                read = inputStream.read(setting);
                if (read == -1) return liMRecvMsg;
                liMRecvMsg.receipt = LiMTypeUtils.getInstance().getBit(setting[0], 7);
                short msgKeyLength = 0;
                if (LiMaoIMApplication.getInstance().protocolVersion > 2) {
                    // 消息Key
                    byte[] msgKeyLengthByte = new byte[2];
                    read = inputStream.read(msgKeyLengthByte);
                    if (read == -1) return liMRecvMsg;
                    msgKeyLength = BigTypeUtils.getInstance().byteToShort(msgKeyLengthByte);
                    byte[] msgKeyByte = new byte[msgKeyLength];
                    read = inputStream.read(msgKeyByte);
                    if (read == -1) return liMRecvMsg;
                    liMRecvMsg.msgKey = LiMTypeUtils.getInstance().bytesToString(msgKeyByte);
                }
                // 消息ID
                byte[] messageId = new byte[8];
                read = inputStream.read(messageId);
                if (read == -1) return liMRecvMsg;
                BigInteger bigInteger = new BigInteger(messageId);
                if (bigInteger.toString().startsWith("-")) {
                    BigInteger temp = new BigInteger("18446744073709551616");
                    liMRecvMsg.messageID = temp.add(bigInteger).toString();
                } else
                    liMRecvMsg.messageID = bigInteger.toString();

                //liMRecvMsg.messageID = BigTypeUtils.getInstance().bytesToLong(messageId)+ "";
                //消息序列号
                byte[] messageSqe = new byte[4];
                read = inputStream.read(messageSqe);
                if (read == -1) return liMRecvMsg;
                liMRecvMsg.messageSeq = BigTypeUtils.getInstance().bytesToInt(messageSqe);
                //解析客户端ID
                byte[] clientMsgNoLengthByte = new byte[2];
                read = inputStream.read(clientMsgNoLengthByte);
                if (read == -1) return liMRecvMsg;
                short clientMsgNoLength = BigTypeUtils.getInstance().byteToShort(clientMsgNoLengthByte);
                byte[] clientMsgNoByte = new byte[clientMsgNoLength];
                read = inputStream.read(clientMsgNoByte);
                if (read == -1) return liMRecvMsg;
                liMRecvMsg.clientMsgNo = LiMTypeUtils.getInstance().bytesToString(clientMsgNoByte);

                //消息时间
                byte[] messageTime = new byte[4];
                read = inputStream.read(messageTime);
                if (read == -1) return liMRecvMsg;
                liMRecvMsg.messageTimestamp = BigTypeUtils.getInstance().bytesToInt(messageTime);
                //频道id长度
                byte[] channelIdLengthByte = new byte[2];
                read = inputStream.read(channelIdLengthByte);
                if (read == -1) return liMRecvMsg;
                short channelIdLength = BigTypeUtils.getInstance().byteToShort(channelIdLengthByte);
                //频道id
                byte[] channelIDByte = new byte[channelIdLength];
                read = inputStream.read(channelIDByte);
                if (read == -1) return liMRecvMsg;
                liMRecvMsg.channelID = LiMTypeUtils.getInstance().bytesToString(channelIDByte);
                //频道类型
                byte[] channelType = new byte[1];
                read = inputStream.read(channelType);
                if (read == -1) return liMRecvMsg;
                liMRecvMsg.channelType = channelType[0];
                //发送者ID
                byte[] fromUIDLengthByte = new byte[2];
                read = inputStream.read(fromUIDLengthByte);
                if (read == -1) return liMRecvMsg;
                short fromUIDLength = BigTypeUtils.getInstance().byteToShort(fromUIDLengthByte);
                byte[] fromUIDByte = new byte[fromUIDLength];
                read = inputStream.read(fromUIDByte);
                if (read == -1) return liMRecvMsg;
                liMRecvMsg.fromUID = LiMTypeUtils.getInstance().bytesToString(fromUIDByte);
                //消息内容
                // // 消息ID长度8 + 消息序列号长度4 + 消息时间长度4 + setting1 + (客户端ID长度+字符串标示长度2) （频道ID长度+字符串标示长度2） + 频道类型长度1 + （发送者uid长度+字符串标示长度2）
                byte[] payload;
                if (LiMaoIMApplication.getInstance().protocolVersion > 2)
                    payload = new byte[remainingLength - (8 + 4 + 2 + 1 + msgKeyLength + 4 + (clientMsgNoLength + 2) + (channelIdLength + 2) + 1 + (2 + fromUIDLength))];
                else {
                    payload = new byte[remainingLength - (8 + 4 + 4 + (clientMsgNoLength + 2) + (channelIdLength + 2) + 1 + (2 + fromUIDLength))];
                }
                read = inputStream.read(payload);
                if (read == -1) return liMRecvMsg;
                String content = LiMTypeUtils.getInstance().bytesToString(payload);
                if (LiMaoIMApplication.getInstance().protocolVersion > 2) {
                    liMRecvMsg.payload = LiMAESEncryptUtils.aesDecrypt(LiMAESEncryptUtils.base64Decode(content), LiMCurve25519Utils.getInstance().aesKey, LiMCurve25519Utils.getInstance().salt);

                    String msgKey = liMRecvMsg.messageID
                            + liMRecvMsg.messageSeq
                            + liMRecvMsg.clientMsgNo
                            + liMRecvMsg.messageTimestamp
                            + liMRecvMsg.fromUID
                            + liMRecvMsg.channelID
                            + liMRecvMsg.channelType
                            + content;
                    byte[] result = LiMAESEncryptUtils.aesEncrypt(msgKey, LiMCurve25519Utils.getInstance().aesKey, LiMCurve25519Utils.getInstance().salt);
                    String base64Result = LiMAESEncryptUtils.base64Encode(result);
                    String localMsgKey = LiMAESEncryptUtils.digest(base64Result);
                    if (!localMsgKey.equals(liMRecvMsg.msgKey)) {
                        return null;
                    }
                } else {
                    liMRecvMsg.payload = content;
                }
                return liMRecvMsg;
            } else if (packetType == LiMMsgType.PONG) {
                LiMLoggerUtils.getInstance().e("Pong消息--->");
                return new LiMPongMsg();
            } else {
                LiMLoggerUtils.getInstance().e("解析协议类型失败--->：" + packetType);
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            LiMLoggerUtils.getInstance().e("解析数据异常------>：" + e.getMessage());
            return null;
        }
    }

    LiMBaseMsg getSendBaseMsg(short packetType, LiMMsg liMMsg) {
        if (packetType == LiMMsgType.CONNECT) {
            //连接消息
            return new LiMConnectMsg();
        } else if (packetType == LiMMsgType.SEND) {
            //发送消息
            JSONObject jsonObject = liMMsg.baseContentMsgModel.encodeMsg();
            try {
                if (jsonObject == null) jsonObject = new JSONObject();
                if (!jsonObject.has(LiMDBColumns.LiMMessageColumns.from_uid)) {
                    jsonObject.put(LiMDBColumns.LiMMessageColumns.from_uid, LiMaoIMApplication.getInstance().getUid());
                }
                jsonObject.put(LiMDBColumns.LiMMessageColumns.type, liMMsg.type);
                //判断@情况
                if (liMMsg.baseContentMsgModel.mentionInfo != null
                        && liMMsg.baseContentMsgModel.mentionInfo.uids != null
                        && liMMsg.baseContentMsgModel.mentionInfo.uids.size() > 0) {
                    JSONArray jsonArray = new JSONArray();
                    for (int i = 0, size = liMMsg.baseContentMsgModel.mentionInfo.uids.size(); i < size; i++) {
                        jsonArray.put(liMMsg.baseContentMsgModel.mentionInfo.uids.get(i));
                    }
                    if (!jsonObject.has("mention")) {
                        JSONObject mentionJson = new JSONObject();
                        mentionJson.put("all", liMMsg.baseContentMsgModel.mention_all);
                        mentionJson.put("uids", jsonArray);
                        jsonObject.put("mention", mentionJson);
                    }

                } else {
                    if (liMMsg.baseContentMsgModel.mention_all == 1) {
                        JSONObject mentionJson = new JSONObject();
                        mentionJson.put("all", liMMsg.baseContentMsgModel.mention_all);
                        jsonObject.put("mention", mentionJson);
                    }
                }
                //判断回复情况
                if (liMMsg.baseContentMsgModel.reply != null) {
                    jsonObject.put("reply", liMMsg.baseContentMsgModel.reply.encodeMsg());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }


            LiMSendMsg liMSendMsg = new LiMSendMsg();
            liMSendMsg.clientSeq = (int) liMMsg.clientSeq;
            liMSendMsg.sync_once = liMMsg.sync_once;
            liMSendMsg.no_persist = liMMsg.no_persist;
            liMSendMsg.red_dot = liMMsg.red_dot;
            liMSendMsg.clientMsgNo = liMMsg.clientMsgNO;
            liMSendMsg.channelId = liMMsg.channelID;
            liMSendMsg.channelType = liMMsg.channelType;
            liMSendMsg.receipt = liMMsg.receipt;
            liMMsg.content = jsonObject.toString();

            long tempOrderSeq = LiMMsgDbManager.getInstance().getMaxOrderSeq(liMMsg.channelID, liMMsg.channelType);
            liMMsg.orderSeq = tempOrderSeq + 1;
            if (!liMSendMsg.no_persist) {
                liMSendMsg.clientSeq = (int) (liMMsg.clientSeq = (int) LiMMsgDbManager.getInstance().insertMsg(liMMsg));
                if (liMMsg.clientSeq > 0)
                    LiMaoIM.getInstance().getLiMMsgManager().updateConversationMsgWithLimMsg(liMMsg);
            }
            if (LiMMediaMessageContent.class.isAssignableFrom(liMMsg.baseContentMsgModel.getClass())) {
                //多媒体数据
                if (jsonObject.has("localPath")) {
                    jsonObject.remove("localPath");
                }
                //视频地址
                if (jsonObject.has("videoLocalPath")) {
                    jsonObject.remove("videoLocalPath");
                }
            }
            liMSendMsg.payload = jsonObject.toString();
            return liMSendMsg;
        } else if (packetType == LiMMsgType.REVACK) {
            //收到消息回复ack消息
            LiMReceivedAckMsg liMRecvAckMsg = new LiMReceivedAckMsg();
            liMRecvAckMsg.messageID = liMMsg.messageID;
            liMRecvAckMsg.messageSeq = liMMsg.messageSeq;
            return liMRecvAckMsg;
        } else if (packetType == LiMMsgType.PING) {
            //心跳消息
            return new LiMPingMsg();
        } else return null;
    }

    LiMMsg baseMsg2LimMsg(LiMBaseMsg liMBaseMsg) {
        LiMReceivedMsg liMReceivedMsg = (LiMReceivedMsg) liMBaseMsg;
        LiMMsg liMMsg = new LiMMsg();
        liMMsg.channelType = liMReceivedMsg.channelType;
        liMMsg.channelID = liMReceivedMsg.channelID;
        liMMsg.content = liMReceivedMsg.payload;
        liMMsg.messageID = liMReceivedMsg.messageID;
        liMMsg.messageSeq = liMReceivedMsg.messageSeq;
        liMMsg.timestamp = liMReceivedMsg.messageTimestamp;
        liMMsg.fromUID = liMReceivedMsg.fromUID;
        liMMsg.receipt = liMReceivedMsg.receipt;
        liMMsg.clientMsgNO = liMReceivedMsg.clientMsgNo;

        liMMsg.orderSeq = LiMaoIM.getInstance().getLiMMsgManager().getMessageOrderSeq(liMMsg.messageSeq, liMMsg.channelID, liMMsg.channelType);
        liMMsg.isDeleted = isDelete(liMMsg.content);
        return liMMsg;
    }

    private static int isDelete(String contentJson) {
        int isDelete = 0;
        if (!TextUtils.isEmpty(contentJson)) {
            try {
                JSONObject jsonObject = new JSONObject(contentJson);
                isDelete = LiMaoIM.getInstance().getLiMMsgManager().isDeletedMsg(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return isDelete;
    }
}
