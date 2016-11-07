package com.hepai.test.GaussBlur;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.hepai.test.FirstPicDV.BLL.FastBlur;
import com.hepai.test.R;

public class GaussBlurActivity extends AppCompatActivity {

    private String TAG = "GaussBlurActivity";

    private ImageView imageView;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gauss_blur);


       /* final BitmapFactory.Options options = new BitmapFactory.Options();
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.gg, options);
        */
        bitmap = getCacheImage();
        bitmap = bitmap.copy(bitmap.getConfig(), true);
        imageView = (ImageView) findViewById(R.id.imageView);

        imageView.setImageBitmap(bitmap);
        //handler.sendEmptyMessageDelayed(0, 2000);


    }

    public Bitmap getCacheImage() {
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inJustDecodeBounds = true;  //设置为true，只读尺寸信息，不加载像素信息到内存
        BitmapFactory.decodeResource(getResources(), R.drawable.hh, option);  //此时bitmap为空
        option.inJustDecodeBounds = false;
        final int bWidth = option.outWidth;
        final int bHeight = option.outHeight;

        final int len = 320;
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (bWidth > len && bHeight > len) {
            int i;
            if (bWidth < bHeight) {
                i = bWidth;
            } else {
                i = bHeight;
            }
            be = i * 2 / len;
        }

        if (be < 1)
            be = 1;
        option.inSampleSize = be; //设置缩放比例
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.hh, option);
        Log.i(TAG, "bWidth = " + bWidth + "  bHeight = " + bHeight + "   bitmap.getWidth() = " + bitmap.getWidth() + "   bitmap.getHeight() = " + bitmap.getHeight() + "   option.inSampleSize = " + option.inSampleSize + "  be = " + be);

        //newOpts.inSampleSize = be;//设置缩放比例

        /*
        int toWidth = 70;
        int toHeight = 70;

        int be = 1;  //be = 1代表不缩放
        if (bWidth / toWidth > bHeight / toHeight && bWidth > toWidth) {
            be = (int) bWidth / toWidth;
        } else if (bWidth / toWidthtoHeight) {
            be = (int) bHeight / toHeight;
        }


        for (int i = 0; i < 100; i++) {

            option.inSampleSize = i; //设置缩放比例
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ii, option);
            Log.i("fuck", "bWidth = " + bWidth +
                            "  bHeight = " + bHeight +
                            "  bitmap.getWidth() = " + bitmap.getWidth() +
                            "  bitmap.getHeight() = " + bitmap.getHeight() +
                            "  option.inSampleSize = " + option.inSampleSize +
                            "  bHeight/bitmap.getHeight() = " + bHeight * 1.0f / bitmap.getHeight()
            );

        }*/


        return bitmap;
    }


    int i = 1;
    float z = 1f;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (j < 6) {
                j += 2.0f;
            }
            Log.i("fuck", "i = " + i + "  j=" + j + "  z = " + z + "   getHeight = " + bitmap.getHeight() + "  getWidth = " + bitmap.getWidth());
            //bitmap = Utils.blurBitmap(GaussBlurActivity.this, bitmap, j);
            if (z < 2) {
                z = z + 0.04f;
            }
            bitmap = FastBlur.doBlur(bitmap, 1 /*(int) z*/, true);
            imageView.setImageBitmap(bitmap);
            if (i < 60) {
                i++;
                handler.sendEmptyMessageDelayed(0, 20);
            }
        }
    };

    float j = 20.5f;

    public static Bitmap blurBitmap(Context context, Bitmap bitmap, float radius) {

        //Let's create an empty bitmap with the same size of the bitmap we want to blur
        //Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Bitmap outBitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888);


        //Instantiate a new Renderscript
        RenderScript rs = RenderScript.create(context.getApplicationContext());

        //Create an Intrinsic Blur Script using the Renderscript
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

        //Create the Allocations (in/out) with the Renderscript and the in/out bitmaps
        Allocation allIn = Allocation.createFromBitmap(rs, bitmap);
        Allocation allOut = Allocation.createFromBitmap(rs, outBitmap);

        //Set the radius of the blur
        blurScript.setRadius(radius);
        /*if(j<6) {
            j += 0.7f;
        }*/

        //Perform the Renderscript
        blurScript.setInput(allIn);
        blurScript.forEach(allOut);

        //Copy the final bitmap created by the out Allocation to the outBitmap
        allOut.copyTo(outBitmap);

        //recycle the original bitmap
        bitmap.recycle();

        //After finishing everything, we destroy the Renderscript.
        rs.destroy();

        return outBitmap;


    }
}
