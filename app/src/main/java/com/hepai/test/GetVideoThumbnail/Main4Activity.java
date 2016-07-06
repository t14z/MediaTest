package com.hepai.test.GetVideoThumbnail;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.hepai.test.R;

public class Main4Activity extends AppCompatActivity {

    private ImageView imageView;

    long time = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main4);

        imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                time += 1000;
                Bitmap bitmap = getVideoThumbnail("/sdcard/PaiHaoHuo/dv/1464408098002_feea8b45787c418392e0c43576cde40e.mp4");
                imageView.setImageBitmap(bitmap);
            }
        });

    }

    /**
     * 获取视频文件的第一张图片
     *
     * @param filePath
     * @return
     */
    public Bitmap getVideoThumbnail(String filePath) {
        if (TextUtils.isEmpty(filePath)) return null;
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            bitmap = retriever.getFrameAtTime(1000000,MediaMetadataRetriever.OPTION_NEXT_SYNC);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }
}
