package com.hepai.test;

import android.app.Activity;
import android.app.Application;

import com.hepai.test.VideoEdit1.VideaEditActivity;

/**
 * Created by jarvisyin on 16/6/22.
 */
public class AppContext extends Application {

    private static AppContext appContext;

    public static VideaEditActivity activity;

    public static AppContext getAppContext() {
        return appContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;
    }
}
