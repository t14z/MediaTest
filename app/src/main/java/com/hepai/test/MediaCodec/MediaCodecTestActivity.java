package com.hepai.test.MediaCodec;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.hepai.test.R;

public class MediaCodecTestActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_codec_test);
        findViewById(R.id.action).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

    }
}
