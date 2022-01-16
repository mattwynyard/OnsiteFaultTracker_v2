package com.onsite.onsitefaulttracker_v2.util;

import android.app.Activity;
import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;

public class MotionUtil implements SensorEventListener{

    // The tag name for this utility class
    private static final String TAG = MotionUtil.class.getSimpleName();
    // A static instance of the bitmap save utilities
    private static MotionUtil sMotionUtil;
    private final SensorManager mSensorManager;
    private final Sensor mSensorAccelerometer;
    private final Sensor mSensorRotation;
    private final Sensor mSensorMagnetic;
    private final Sensor mSensorLinearAcceleration;
    private static long lastUpdate;
    private boolean color = false;
    private float[] mOrientation;
    private float[] mMagnetic = new float[3];
    private float[] mGravity = new float[3];
    private float[] mRotation;
    private float[] mLinearAcceleration = new float[3];
    private final float[] rotationMatrix = new float[16];
    private final  float[] I = new float[16];
    float[] orientation = new float[3];

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
        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorLinearAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values.clone();
            System.arraycopy(event.values, 0, mGravity, 0, mGravity.length);
        } else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            mRotation = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetic,
                    0, mMagnetic.length);
        } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            System.arraycopy(event.values, 0, mLinearAcceleration,
                    0, mLinearAcceleration.length);
        }

        if (mGravity != null && mMagnetic != null) {
            boolean success = SensorManager.getRotationMatrix(rotationMatrix, null, mGravity, mMagnetic);
            if (success) {
                SensorManager.getOrientation(rotationMatrix, orientation);
                MotionUtil.sharedInstance().setOrientation(orientation);
            }
        }
    }

//    public void updateOrientationAngles() {
//        // Update rotation matrix, which is needed to update orientation angles.
//        SensorManager.getRotationMatrix(rotationMatrix, null,
//                mGravity, mMagnetic);
//
//        // "rotationMatrix" now has up-to-date information.
//        SensorManager.getOrientation(rotationMatrix, orientation);
//        // "orientationAngles" now has up-to-date information.
//    }

    private void getRotation(SensorEvent event) {
        float[] rotation = event.values;

    }

    private void getAccelerometer(SensorEvent event) {
        float[] values = event.values;
        // Movement
        float x = values[0];
        float y = values[1];
        float z = values[2];

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    public SensorManager getManager() {
        return mSensorManager;
    }

    public Sensor getSensorAccelerometer() {
        return mSensorAccelerometer;
    }

    public Sensor getSensorRotation() {
        return mSensorRotation;
    }

    public Sensor getSensorMagnetic() {
        return mSensorMagnetic;
    }

    public Sensor getSensorLinearAcceleration() {
        return mSensorLinearAcceleration;
    }

    public void setOrientation(float[] orientation) {
        mOrientation = orientation;
    }

    public float[] getOrientation() {
        return mOrientation;
    }

    public float[] getLinearAcceleration() {
        return mLinearAcceleration;
    }
}
