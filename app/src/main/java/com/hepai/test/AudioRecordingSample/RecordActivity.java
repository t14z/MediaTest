package com.hepai.test.AudioRecordingSample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.hepai.test.R;

public class RecordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        getFragmentManager().beginTransaction().add(R.id.content,new CameraFragment()).commit();
    }
}
