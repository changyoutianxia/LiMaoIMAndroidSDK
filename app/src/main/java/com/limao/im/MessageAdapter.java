package com.limao.im;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.xinbida.limaoim.message.type.LiMSendMsgResult;

import org.jetbrains.annotations.NotNull;

/**
 * 6/2/21 3:56 PM
 */
class MessageAdapter extends BaseQuickAdapter<UIMessageEntity, BaseViewHolder> {
    public MessageAdapter() {
        super(R.layout.item_msg_layout);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, UIMessageEntity uiMessageEntity) {
        baseViewHolder.setText(R.id.contentTv, uiMessageEntity.liMMsg.baseContentMsgModel.getDisplayContent());
        if (uiMessageEntity.liMMsg.status == LiMSendMsgResult.send_loading) {
            baseViewHolder.setText(R.id.statusTv, "发送中");
        } else if (uiMessageEntity.liMMsg.status > LiMSendMsgResult.send_success) {
            baseViewHolder.setText(R.id.statusTv, "发送失败");
        } else baseViewHolder.setText(R.id.statusTv, "");
    }
}
