package com.onsite.onsitefaulttracker_v2;

import android.app.Application;
import android.util.Log;

//import com.crashlytics.android.Crashlytics;
//import io.fabric.sdk.android.Fabric;

import com.onsite.onsitefaulttracker_v2.connectivity.BLTManager;
import com.onsite.onsitefaulttracker_v2.model.notifcation_events.BLTStopRecordingEvent;
import com.onsite.onsitefaulttracker_v2.util.BatteryUtil;
import com.onsite.onsitefaulttracker_v2.util.BitmapSaveUtil;
import com.onsite.onsitefaulttracker_v2.util.BusNotificationUtil;
import com.onsite.onsitefaulttracker_v2.util.CalculationUtil;
import com.onsite.onsitefaulttracker_v2.util.CameraUtil;
import com.onsite.onsitefaulttracker_v2.util.EXIFUtil;
import com.onsite.onsitefaulttracker_v2.util.GPSUtil;
import com.onsite.onsitefaulttracker_v2.util.LogUtil;
import com.onsite.onsitefaulttracker_v2.util.MessageUtil;
import com.onsite.onsitefaulttracker_v2.util.MotionUtil;
import com.onsite.onsitefaulttracker_v2.util.RecordUtil;
import com.onsite.onsitefaulttracker_v2.util.SettingsUtil;

//import static com.crashlytics.android.beta.Beta.TAG;

/**
 * Created by hihi on 6/6/2016.
 *
 * The Application class for this application.
 * Sets up Singletons and Utility classes.
 */
public class OnsiteApplication extends Application {

    /**
     * On Create
     *
     * Sets up singletons and Utility classes
     */
    @Override
    public void onCreate()
    {
        super.onCreate();
        // initialize the singletons used throughout this app
        SettingsUtil.initialize(this);
        CalculationUtil.initialize(this);
        CameraUtil.initialize(this);
        BatteryUtil.initialize(this);
        RecordUtil.initialize(this);
        BitmapSaveUtil.initialize(this);
        BLTManager.initialize(this);
        BusNotificationUtil.initialize(this);
        MessageUtil.initialize(this);
        EXIFUtil.initialize(this);
        GPSUtil.initialize(this);
        LogUtil.initialize(this);

        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread thread, Throwable e) {
                        System.out.println("AppCrash");
                        e.printStackTrace();
                        BLTManager.sharedInstance().sendPhoto("E:CRASH,", null);
                        BusNotificationUtil.sharedInstance().
                                postNotification(new BLTStopRecordingEvent());
                        System.exit(1);
                    }
                });
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        //Log.i(TAG, "APP: Low Memory");
        BLTManager.sharedInstance().sendPhoto("E:Low Phone Memory,", null);
        BusNotificationUtil.sharedInstance().postNotification(new BLTStopRecordingEvent());
    }
}
