package com.hepai.test;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.hepai.test.audioTrimmer.CheapSoundFile;
import com.hepai.test.audioTrimmer.Util;

import java.io.File;

import jp.co.cyberagent.android.gpuimage.GPUImage;

public class MainActivity extends AppCompatActivity {

    private GPUImage mGPUImage;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
/*
        mGPUImage = new GPUImage(this);
        mGPUImage.setGLSurfaceView((GLSurfaceView) findViewById(R.id.surfaceView));
        mGPUImage.setImage(imageUri); // this loads image on the current thread, should be run in a thread
        mGPUImage.setFilter(new GPUImageSepiaFilter());

        // Later when image should be saved saved:
        mGPUImage.saveToPictures("GPUImage", "ImageWithFilter.jpg", null);


        mGPUImage = new GPUImage(this);
        mGPUImage.setFilter(new GPUImageSobelEdgeDetection());
        mGPUImage.setImage(imageUri);
        mGPUImage.saveToPictures("GPUImage", "ImageWithFilter.jpg", null);*/

        //new EncodeAndMuxTest(this).testEncodeVideoToMp4();


        textView = (TextView) findViewById(R.id.textView);
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    private void init() throws Exception {

        final CheapSoundFile.ProgressListener listener = new CheapSoundFile.ProgressListener() {
            public boolean reportProgress(double frac) {
                textView.setText(frac + "%");
                return true;
            }
        };
        String a = Environment.getExternalStorageDirectory() + "/z_audio_baliansha.m4a";
        CheapSoundFile cheapSoundFile = CheapSoundFile.create(a, listener);
        int mSampleRate = cheapSoundFile.getSampleRate();
        int mSamplesPerFrame = cheapSoundFile.getSamplesPerFrame();
        int startFrame = Util.secondsToFrames(28.0, mSampleRate, mSamplesPerFrame);
        int endFrame = Util.secondsToFrames(48.0, mSampleRate, mSamplesPerFrame);
        cheapSoundFile.WriteFile(new File(Environment.getExternalStorageDirectory() + "/zzz_audio_baliansha.m4a"), startFrame, endFrame - startFrame);

    }
}
