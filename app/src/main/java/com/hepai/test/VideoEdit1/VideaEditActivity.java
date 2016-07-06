package com.hepai.test.VideoEdit1;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.hepai.test.AppContext;
import com.hepai.test.R;

public class VideaEditActivity extends AppCompatActivity implements View.OnClickListener {

    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videa_edit);
        findViewById(R.id.action).setOnClickListener(this);
        imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setBackgroundResource(R.mipmap.ic_launcher);
        AppContext.activity = this;
    }

    public void setImage(Bitmap bitmap) {

        Log.e("FUCK", "FUCK3");
        Message msg = handler.obtainMessage();
        msg.obj = bitmap;
        handler.sendMessage(msg);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            Log.e("FUCK", "FUCK4");
            Bitmap bitmap = (Bitmap) msg.obj;
            //imageView.(bitmap);
        }
    };

    @Override
    public void onClick(View v) {

        new Thread() {
            @Override
            public void run() {
                super.run();
                try {

                    String[] strs = {
                            //"/sdcard/PaiHaoHuo/dv/1464862215528_505f1e08e23a4cbb9f3a625a27aec102.mp4"
                            "/sdcard/PaiHaoHuo/dv/1466146175404_8b4f8dc11e7f47d7ab8febbcbf6d9d22.mp4"
                    };

                    // new DecodeEditEncodeTest(VideaEditActivity.this).testVideoEdit720p();
                    new VideaEdit(VideaEditActivity.this, strs).start1();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }.start();
    }
}
