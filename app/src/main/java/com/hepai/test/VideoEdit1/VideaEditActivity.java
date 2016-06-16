package com.hepai.test.VideoEdit1;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.hepai.test.R;
import com.hepai.test.VideoEdit.ExtractDecodeEditEncodeMuxTest;

public class VideaEditActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videa_edit);
        findViewById(R.id.action).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    // new DecodeEditEncodeTest(VideaEditActivity.this).testVideoEdit720p();
                    new VideaEdit(VideaEditActivity.this).start();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }.start();


    }
}
