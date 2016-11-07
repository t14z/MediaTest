package com.hepai.test.seekTo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hepai.test.R;

/**
 * Created by Jarvis.
 */
public class IndexFragment extends Fragment {
    public static final String TAG = "com.hepai.test.seekTo.IndexFragment";

    public IndexFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_index_seekto, container, false);


        new Thread() {
            @Override
            public void run() {
                try {
                    new CaptureFrame().start();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }.start();


        return view;
    }


}
