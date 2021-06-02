package com.limao.im;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xinbida.limaoim.LiMaoIM;
import com.xinbida.limaoim.entity.LiMMsg;
import com.xinbida.limaoim.message.type.LiMChannelType;
import com.xinbida.limaoim.message.type.LiMConnectStatus;
import com.xinbida.limaoim.msgmodel.LiMTextContent;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    AppCompatEditText uidET, toUidET, tokenET, contentET;
    MessageAdapter adapter;
    private TextView statusTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycleView);
        uidET = findViewById(R.id.uidET);
        tokenET = findViewById(R.id.tokenET);
        contentET = findViewById(R.id.contentET);
        statusTv = findViewById(R.id.statusTv);

        adapter = new MessageAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
        onListener();
    }

    void onListener() {
        findViewById(R.id.connectBtn).setOnClickListener(v -> {
            String uid = uidET.getText().toString();
            String token = tokenET.getText().toString();
            if (!TextUtils.isEmpty(uid) && !TextUtils.isEmpty(token)) {
                LiMaoIM.getInstance().initIM(MainActivity.this, uid, token);
                LiMaoIM.getInstance().getLiMConnectionManager().connection();
            }
        });
        findViewById(R.id.sendBtn).setOnClickListener(v -> {
            String content = contentET.getText().toString();
            String uid = toUidET.getText().toString();
            if (!TextUtils.isEmpty(content) && !TextUtils.isEmpty(uid)) {
                LiMaoIM.getInstance().getLiMConnectionManager().sendMessage(new LiMTextContent(content), uid, LiMChannelType.PERSONAL);
            }
        });


        LiMaoIM.getInstance().getLiMConnectionManager().addOnConnectionStatusListener(code -> {
            if (code == LiMConnectStatus.success) {
                statusTv.setText("连接成功");
            } else if (code == LiMConnectStatus.fail) {
                statusTv.setText("连接失败");
            } else if (code == LiMConnectStatus.connecting) {
                statusTv.setText("连接中...");
            } else if (code == LiMConnectStatus.noNetwork) {
                statusTv.setText("无网络");
            }
        });
        LiMaoIM.getInstance().getLiMMsgManager().addOnNewMsgListener("new_msg", liMMsgList -> {
            for (LiMMsg liMMsg : liMMsgList) {
                adapter.addData(new UIMessageEntity(liMMsg));
            }
        });
        LiMaoIM.getInstance().getLiMMsgManager().addOnSendMsgCallback("insert_msg", liMMsg -> adapter.addData(new UIMessageEntity(liMMsg)));
        LiMaoIM.getInstance().getLiMMsgManager().addSendMsgAckListener("ack_key", (clientSeq, messageID, messageSeq, reasonCode) -> {
            for (int i = 0, size = adapter.getData().size(); i < size; i++) {
                if (adapter.getData().get(i).liMMsg.clientSeq == clientSeq) {
                    adapter.getData().get(i).liMMsg.status = reasonCode;
                    adapter.notifyItemChanged(i);
                    break;
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LiMaoIM.getInstance().getLiMConnectionManager().disconnect(true);
        LiMaoIM.getInstance().getLiMMsgManager().removeNewMsgListener("new_msg");
        LiMaoIM.getInstance().getLiMMsgManager().removeSendMsgCallBack("insert_msg");
        LiMaoIM.getInstance().getLiMMsgManager().removeSendMsgAckListener("ack_key");
    }
}