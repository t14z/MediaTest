package com.hepai.test.AudioRecordingSample;

/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: CameraFragment.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

import android.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.hepai.test.R;

public class CameraFragment extends Fragment implements OnClickListener {

    private ImageButton mRecordButton;
    private boolean isRecording = false;
    private MyAudioRecord4 mAudioRecord;

    public CameraFragment() {
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mRecordButton = (ImageButton) rootView.findViewById(R.id.record_button);
        mRecordButton.setOnClickListener(this);
        mAudioRecord = new MyAudioRecord4(Environment.getExternalStorageDirectory()+"/z_z_z.mp4");
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        mAudioRecord.stopRecording();
        mRecordButton.setColorFilter(0);
        super.onPause();
    }

    /**
     * method when touch record button
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.record_button:
                if (!isRecording) {
                    isRecording = true;
                    mAudioRecord.startRecording();
                    mRecordButton.setColorFilter(0xffff0000);    // turn red
                } else {
                    isRecording = false;
                    mAudioRecord.stopRecording();
                    mRecordButton.setColorFilter(0);
                }
                break;
        }
    }



}
