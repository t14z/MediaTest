package com.hepai.test.OpenGLTest;


import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Jarvis.
 */
public class IndexFragment extends Fragment {
    public static final String TAG = IndexFragment.class.getName();


    public IndexFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return new MyGLSerfaceView(getContext());
    }

}
