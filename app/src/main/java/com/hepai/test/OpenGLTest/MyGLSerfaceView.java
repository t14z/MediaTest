package com.hepai.test.OpenGLTest;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by jarvisyin on 16/10/18.
 */
public class MyGLSerfaceView extends GLSurfaceView {
    public static final String TAG = "MyGLSerfaceView";

    public MyGLSerfaceView(Context context) {
        super(context);
        init();
    }

    public MyGLSerfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        setRenderer(new MyRenderer());
    }

    private class MyRenderer implements Renderer {
        private YellowManDrawer mYellowManDrawer;

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            mYellowManDrawer = new YellowManDrawer(getContext());
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            //mYellowManDrawer.onSurfaceChanged(width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {

            mYellowManDrawer.draw();
        }
    }
}
