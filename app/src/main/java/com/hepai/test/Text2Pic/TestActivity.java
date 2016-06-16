package com.hepai.test.Text2Pic;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.hepai.test.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class TestActivity extends Activity {
    private Button btn;
    private final int WORDNUM = 35;  //转化成图片时  每行显示的字数
    private final int WIDTH = 450;   //设置图片的宽度

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        btn = (Button) findViewById(R.id.ok);
        btn.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                int x = 5, y = 10;
                try {
                    Bitmap bitmap = Bitmap.createBitmap(WIDTH, 85, Bitmap.Config.ALPHA_8);
                    Canvas canvas = new Canvas(bitmap);
                    //canvas.drawColor(0xffff0000);
                    Paint paint = new Paint();
                    paint.setTextSize(69);
                    canvas.drawText("我艹是空军基地卡贝鲁卡时间都擦克里斯蒂参加巴萨", 0, 75, paint);


                    canvas.save(Canvas.ALL_SAVE_FLAG);
                    canvas.restore();

                    ((ImageView) findViewById(R.id.imageView)).setImageBitmap(bitmap);
                    /*
                    String path = Environment.getExternalStorageDirectory() + "/zzzz.png";
                    System.out.println(path);
                    FileOutputStream os = new FileOutputStream(new File(path));
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                    os.flush();
                    os.close();*/
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }


            }
        });
    }
}