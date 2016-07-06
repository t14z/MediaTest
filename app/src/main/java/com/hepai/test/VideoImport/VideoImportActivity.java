package com.hepai.test.VideoImport;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.hepai.test.R;

public class VideoImportActivity extends Activity {

    private ViewGroup linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_import);
        linearLayout = (ViewGroup) findViewById(R.id.linearLayout);
        new Thread() {
            @Override
            public void run() {
                try {
                    new ExtractMpegFramesTest() {
                        @Override
                        public void outputBitmap(Bitmap bmp) {
                            Message msg = new Message();
                            msg.what = 1;
                            msg.obj = bmp;
                            handler.sendMessage(msg);
                        }
                    }.testExtractMpegFrames();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bitmap bitmap = (Bitmap) msg.obj;
            ImageView imageView = new ImageView(VideoImportActivity.this);
            imageView.setImageBitmap(bitmap);
            linearLayout.addView(imageView, 100, 100);
            bitmap.recycle();
        }
    };

}
