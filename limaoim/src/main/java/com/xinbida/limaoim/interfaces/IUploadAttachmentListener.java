package com.xinbida.limaoim.interfaces;


import com.xinbida.limaoim.entity.LiMMsg;

/**
 * 2020-08-02 00:29
 * 上传聊天附件
 */
public interface IUploadAttachmentListener {
    void onUploadAttachmentListener(LiMMsg liMMsg, IUploadAttacResultListener attacResultListener);
}
