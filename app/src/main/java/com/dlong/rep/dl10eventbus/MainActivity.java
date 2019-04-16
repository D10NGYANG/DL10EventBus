package com.dlong.rep.dl10eventbus;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.dlong.rep.dleventbus.DLEventBus;
import com.dlong.rep.dleventbus.DLSubscribe;

public class MainActivity extends AppCompatActivity {
    private Context mContext = this;

    private TextView txt;

    @SuppressLint("HandlerLeak")
    public Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    txt.setText((String) msg.obj);
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (!DLEventBus.getDefault().isRegistered(this)){
            DLEventBus.getDefault().register(this);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DLEventBus.getDefault().register(this);

        txt = findViewById(R.id.txt);

        txt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(mContext, Main2Activity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DLEventBus.getDefault().unregister(this);
    }

    @DLSubscribe
    public void returnBackMsg(MsgEvent msgEvent){
        Message message = new Message();
        message.what = 1;
        message.obj = msgEvent.str;
        mHandler.sendMessage(message);
    }
}
