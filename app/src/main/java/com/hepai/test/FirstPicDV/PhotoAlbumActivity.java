package com.hepai.test.FirstPicDV;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.hepai.test.FirstPicDV.BLL.EncodeAndMuxTest;
import com.hepai.test.FirstPicDV.BLL.ImageInfo;
import com.hepai.test.FirstPicDV.BLL.LastImageInfo;
import com.hepai.test.FirstPicDV.BLL.Utils;
import com.hepai.test.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PhotoAlbumActivity extends AppCompatActivity implements View.OnClickListener {


    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_album);

        textView = (TextView) findViewById(R.id.action);
        textView.setOnClickListener(this);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
    }

    @Override
    public void onClick(View v) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                action();
            }
        }.start();
    }


    private void action()  {
        List<ImageInfo> imageList = new ArrayList<>();
        ImageInfo imageInfo;

        /**/
        imageInfo = new ImageInfo();
        imageInfo.imagePath = Environment.getExternalStorageDirectory() + "/z_picture/aa.jpg";
        imageInfo.explain = "this is aa.jpg";
        imageList.add(imageInfo);

       /* imageInfo = new ImageInfo();
        imageInfo.imagePath = Environment.getExternalStorageDirectory() + "/z_picture/bb.jpg";
        imageInfo.explain = "this is bb.jpg";
        imageList.add(imageInfo);

        imageInfo = new ImageInfo();
        imageInfo.imagePath = Environment.getExternalStorageDirectory() + "/z_picture/cc.jpg";
        imageInfo.explain = "this is cc.jpg";
        imageList.add(imageInfo);

        imageInfo = new ImageInfo();
        imageInfo.imagePath = Environment.getExternalStorageDirectory() + "/z_picture/dd.jpg";
        imageInfo.explain = "this is dd.jpg";
        imageList.add(imageInfo);

        imageInfo = new ImageInfo();
        imageInfo.imagePath = Environment.getExternalStorageDirectory() + "/z_picture/ee.jpg";
        imageInfo.explain = "this is ee.jpg";
        imageList.add(imageInfo);

        imageInfo = new ImageInfo();
        imageInfo.imagePath = Environment.getExternalStorageDirectory() + "/z_picture/ff.jpg";
        imageInfo.explain = "this is ff.jpg";
        imageList.add(imageInfo);

        imageInfo = new ImageInfo();
        imageInfo.imagePath = Environment.getExternalStorageDirectory() + "/z_picture/gg.jpg";
        imageInfo.explain = "this is gg.jpg";
        imageList.add(imageInfo);

        imageInfo = new ImageInfo();
        imageInfo.imagePath = Environment.getExternalStorageDirectory() + "/z_picture/hh.jpg";
        imageInfo.explain = "this is hh.jpg";
        imageList.add(imageInfo);

        imageInfo = new ImageInfo();
        imageInfo.imagePath = Environment.getExternalStorageDirectory() + "/z_picture/ii.jpg";
        imageInfo.explain = "this is ii.jpg";
        imageList.add(imageInfo);

        imageInfo = new ImageInfo();
        imageInfo.imagePath = Environment.getExternalStorageDirectory() + "/z_picture/jj.jpg";
        imageInfo.explain = "this is jj.jpg";
        imageList.add(imageInfo);

        imageInfo = new ImageInfo();
        imageInfo.imagePath = Environment.getExternalStorageDirectory() + "/z_picture/ee.jpg";
        imageInfo.explain = "this is ee.jpg";
        imageList.add(imageInfo);


        imageInfo = new ImageInfo();
        imageInfo.imagePath = Environment.getExternalStorageDirectory() + "/z_picture/kk.jpg";
        imageInfo.explain = "this is kk.jpg";
        imageList.add(imageInfo);*/

        /*if (new File(imageInfo.imagePath).exists()) {
            throw new RuntimeException("exit");
        } else {
            throw new RuntimeException("no exit");
        }

        imageInfo = new ImageInfo();
        imageInfo.imagePath = Environment.getExternalStorageDirectory() + "/z_picture/ll.jpg";
        imageInfo.explain = "this is ll.jpg";
        imageList.add(imageInfo);*/


        try {
            LastImageInfo l = new LastImageInfo();

            l.logoBitmap = Utils.getBitmap(this, R.drawable.logo, 130);
            l.qrBitmap = Utils.getBitmap(Environment.getExternalStorageDirectory() + "/z_qr.png", 320);


            new EncodeAndMuxTest(this, imageList, l) {
                @Override
                public void action(int numFrames, int framCount) {
                    Message msg = handler.obtainMessage();
                    msg.arg1 = numFrames;
                    msg.arg2 = framCount;
                    handler.sendMessage(msg);
                }
            }.testEncodeVideoToMp4();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            textView.setText(msg.arg1 + "   " + msg.arg2);
        }
    };

}
