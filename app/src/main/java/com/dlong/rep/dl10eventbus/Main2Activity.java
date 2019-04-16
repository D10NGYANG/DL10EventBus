package com.dlong.rep.dl10eventbus;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.dlong.rep.dleventbus.DLEventBus;

public class Main2Activity extends AppCompatActivity {
    private Context mContext = this;

    private Button button;
    private EditText edTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        button = findViewById(R.id.button);
        edTxt = findViewById(R.id.edtxt);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MsgEvent msgEvent = new MsgEvent();
                msgEvent.str = edTxt.getText().toString();
                DLEventBus.getDefault().post(msgEvent);
                finish();
            }
        });
    }
}
