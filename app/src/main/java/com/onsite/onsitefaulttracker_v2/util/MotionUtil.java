package com.onsite.onsitefaulttracker_v2.util;

import android.app.Activity;
import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;

public class MotionUtil {

    // The tag name for this utility class
    private static final String TAG = MotionUtil.class.getSimpleName();
    // A static instance of the bitmap save utilities
    private static MotionUtil sMotionUtil;
    private final SensorManager mSensorManager;
    private final Sensor mAccelerometer;
    private final Sensor mRotation;
    private final Sensor mMagnetic;
    private static long lastUpdate;
    private boolean color = false;
    private float[] mOrientation;

    public static void initialize(final Context context) {
        sMotionUtil = new MotionUtil(context);
    }
    private GeomagneticField geomagneticField;

    /**
     * Returns a shared instance of BitmapSaveUtil
     * @return
     */
    public static MotionUtil sharedInstance() {
        if (sMotionUtil != null) {
            return sMotionUtil;
        } else {
            throw new RuntimeException("BitmapSaveUtil must be initialized in the " +
                    "Application class before use");
        }
    }

    /**
     * Contructor, to be called internally via. initialize
     * @param context
     */
    private MotionUtil(final Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

    }

    public SensorManager getManager() {
        return mSensorManager;
    }

    public Sensor getMagnetic() {
        return mMagnetic;
    }

    public void setOrientation(float[] orientation) {
        mOrientation = orientation;
    }

    public float[] getOrientation() {
        return mOrientation;
    }

    public Sensor getAccelerometer() {
        return mAccelerometer;
    }

    public Sensor getRotation() {
        return mRotation;
    }
}
