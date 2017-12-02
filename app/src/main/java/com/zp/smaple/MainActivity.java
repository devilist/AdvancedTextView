package com.zp.smaple;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tv_hori, tv_vert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_hori = findViewById(R.id.tv_hori);
        tv_vert = findViewById(R.id.tv_vert);

        tv_hori.setOnClickListener(this);
        tv_vert.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_hori:
                HoriActivity.start(this);
                break;
            case R.id.tv_vert:
                VertActivity.start(this);
                break;
        }
    }
}
