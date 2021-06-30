package com.limao.im;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xinbida.limaoim.LiMaoIM;
import com.xinbida.limaoim.entity.LiMMsg;
import com.xinbida.limaoim.interfaces.IGetIpAndPort;
import com.xinbida.limaoim.interfaces.IGetSocketIpAndPortListener;
import com.xinbida.limaoim.message.type.LiMChannelType;
import com.xinbida.limaoim.message.type.LiMConnectStatus;
import com.xinbida.limaoim.msgmodel.LiMTextContent;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    AppCompatEditText uidET, ipET, portEt, toUidET, tokenET, contentET;
    MessageAdapter adapter;
    private TextView statusTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycleView);
        ipET = findViewById(R.id.ipEt);
        portEt = findViewById(R.id.portEt);
        uidET = findViewById(R.id.uidET);
        tokenET = findViewById(R.id.tokenET);
        contentET = findViewById(R.id.contentET);
        toUidET = findViewById(R.id.toUidET);
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
            String ip = ipET.getText().toString();
            String port = portEt.getText().toString();
            if (!TextUtils.isEmpty(uid) && !TextUtils.isEmpty(token) && !TextUtils.isEmpty(ip) && !TextUtils.isEmpty(port)) {
                // 初始化im
                LiMaoIM.getInstance().initIM(MainActivity.this, uid, token);
                // 连接
                LiMaoIM.getInstance().getLiMConnectionManager().connection();
            }else {
                Toast.makeText(this,"以上信息不能为空",Toast.LENGTH_LONG);
            }
        });
        findViewById(R.id.sendBtn).setOnClickListener(v -> {
            String content = contentET.getText().toString();
            String uid = toUidET.getText().toString();
            if (!TextUtils.isEmpty(content) && !TextUtils.isEmpty(uid)) {
                // 发送消息
                LiMaoIM.getInstance().getLiMConnectionManager().sendMessage(new LiMTextContent(content), uid, LiMChannelType.PERSONAL);
            }
        });

        // 连接状态监听
        LiMaoIM.getInstance().getLiMConnectionManager().addOnConnectionStatusListener("main_act", code -> {
            if (code == LiMConnectStatus.success) {
                statusTv.setText("连接成功");
            } else if (code == LiMConnectStatus.fail) {
                statusTv.setText("连接失败");
            } else if (code == LiMConnectStatus.connecting) {
                statusTv.setText("连接中...");
            } else if (code == LiMConnectStatus.noNetwork) {
                statusTv.setText("无网络");
            } else if (code == LiMConnectStatus.kicked) {
                statusTv.setText("账号被其他设备登录");
            }
        });
        // 新消息监听
        LiMaoIM.getInstance().getLiMMsgManager().addOnNewMsgListener("new_msg", liMMsgList -> {
            for (LiMMsg liMMsg : liMMsgList) {
                adapter.addData(new UIMessageEntity(liMMsg));
            }
        });
        // 监听发送消息入库返回
        LiMaoIM.getInstance().getLiMMsgManager().addOnSendMsgCallback("insert_msg", liMMsg -> adapter.addData(new UIMessageEntity(liMMsg)));
        // 发送消息回执
        LiMaoIM.getInstance().getLiMMsgManager().addSendMsgAckListener("ack_key", (clientSeq, messageID, messageSeq, reasonCode) -> {
            for (int i = 0, size = adapter.getData().size(); i < size; i++) {
                if (adapter.getData().get(i).liMMsg.clientSeq == clientSeq) {
                    adapter.getData().get(i).liMMsg.status = reasonCode;
                    adapter.notifyItemChanged(i);
                    break;
                }
            }
        });
        LiMaoIM.getInstance().getLiMConnectionManager().addOnGetIpAndPortListener(andPortListener -> {
            String ip = ipET.getText().toString();
            String port = portEt.getText().toString();
            andPortListener.onGetSocketIpAndPort(ip,Integer.parseInt(port));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 断开连接
        LiMaoIM.getInstance().getLiMConnectionManager().disconnect(true);
        // 取消监听
        LiMaoIM.getInstance().getLiMMsgManager().removeNewMsgListener("new_msg");
        LiMaoIM.getInstance().getLiMMsgManager().removeSendMsgCallBack("insert_msg");
        LiMaoIM.getInstance().getLiMMsgManager().removeSendMsgAckListener("ack_key");
        LiMaoIM.getInstance().getLiMConnectionManager().removeOnConnectionStatusListener("main_act");
    }
}