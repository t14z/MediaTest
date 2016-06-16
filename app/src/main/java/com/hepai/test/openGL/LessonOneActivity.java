package com.hepai.test.openGL;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

public class LessonOneActivity extends Activity implements View.OnClickListener {
    /**
     * Hold a reference to our GLSurfaceView
     */
    private GLSurfaceView mGLSurfaceView;
    LessonOneRenderer1 mRender;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGLSurfaceView = new GLSurfaceView(this);

        // Check if the system supports OpenGL ES 2.0.
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (supportsEs2) {
            // Request an OpenGL ES 2.0 compatible context.
            mGLSurfaceView.setEGLContextClientVersion(2);

            mRender = new LessonOneRenderer1(this);
            // Set the renderer to our demo renderer, defined below.
            mGLSurfaceView.setRenderer(mRender);
        } else {
            // This is where you could create an OpenGL ES 1.x compatible
            // renderer if you wanted to support both ES 1 and ES 2.
            return;
        }

        setContentView(mGLSurfaceView);

        handler.postDelayed(runnable, 1000);
        mGLSurfaceView.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if (mRender.enable) {
            mRender.enable = false;
        } else {
            mRender.enable = true;
        }
    }

    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        public void run() {
            // TODO Auto-generated method stub
            //要做的事情
            handler.postDelayed(this, 1000);
            //Log.v("timer",runTop(TOP));
            //setTile();
            showFPS(mRender.getFPS());
        }
    };

    private void showFPS(int fps) {
        this.setTitle("fps:" + fps);
    }

    @Override
    protected void onResume() {
        // The activity must call the GL surface view's onResume() on activity onResume().
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        // The activity must call the GL surface view's onPause() on activity onPause().
        super.onPause();
        mGLSurfaceView.onPause();
    }
}