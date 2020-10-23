package com.onsite.onsitefaulttracker_v2.util;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.location.Criteria;
import android.location.GnssClock;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.OnNmeaMessageListener;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.location.Location;
import android.location.LocationListener;
import android.content.DialogInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.String;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.os.Environment;
import android.support.media.ExifInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.app.AlertDialog;
import android.provider.Settings;
import android.widget.Toast;

public class GPSUtil implements LocationListener {

    // The tag name for this utility class
    private static final String TAG = GPSUtil.class.getSimpleName();
    // The application context
    private Context mContext;
    private static GPSUtil sSharedInstance;
    private static GPSUtil sGPSUtil;
    // flag for GPS status
    public boolean isGPSEnabled;
    private LocationManager mLocationManager;
    private Location mLocation;
    //private LocationListener mLocationListener;
    private double latitude; // latitude
    private double longitude; // longitude
    private OnNmeaMessageListener mNmeaListener;
    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 1 meters
    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000; // 1 sec
    public static final int PERMISSIONS_REQUEST_LOCATION = 10;
    private boolean mFix;
    private int mSatellites;

    /**
     * initializes GPSUtil.
     *
     * @param applicationContext The application context
     */
    public static void initialize(final Application applicationContext) {

        sGPSUtil = new GPSUtil(applicationContext);
    }

    /**
     * The constructor for GPSUtil, called internally
     *
     * @param applicationContext
     */
    public GPSUtil(final Application applicationContext) {
        mContext = applicationContext;
        mLocationManager = (LocationManager)
                mContext.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this.mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //return;
        }
         intialiseGPS();
    }

    /**
     * return the shared instance of Record Util
     *
     * @return
     */
    public static GPSUtil sharedInstance() {
        if (sGPSUtil != null) {
            return sGPSUtil;
        } else {
            throw new RuntimeException("GPSUtil must be initialized " +
                    "in the Application class before use");
        }
    }

    public void addNmeaListener() {
        mNmeaListener = new OnNmeaMessageListener() {
            @Override
            public void onNmeaMessage(String message, long timestamp) {
                Log.v("NMEA String: ", "= " + message);
            }
        };
    }

    LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (mLocationManager != null) {
                if (ActivityCompat.checkSelfPermission( mContext, Manifest.permission
                        .ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat
                        .checkSelfPermission( mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            } else {
                //Log.d(TAG, "Location Manager Null");
            }
            mLocation = location;
        }



        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };



    public int getSatellites() {
        return mSatellites;
    }

    public void intialiseGPS() {
        if (ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext,
                        Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {// getting GPS status
            isGPSEnabled = mLocationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setPowerRequirement(Criteria.POWER_HIGH);
            criteria.setAltitudeRequired(true);
            criteria.setSpeedRequired(false);
            criteria.setBearingRequired(true);

            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, mLocationListener);
            mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } else {

        }

    GnssMeasurementsEvent.Callback gnssMeasurementsCallback = new GnssMeasurementsEvent.Callback() {
        @Override
        public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {


        }

        public void onStatusChanged(int status) {

        }
    };

    GnssNavigationMessage.Callback gnssMessageCallback = new GnssNavigationMessage.Callback() {
        @Override
        public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
            Log.d(TAG, "Navigation Message received)");
        }

        public void onStatusChanged(int status) {

        }
    };

    GnssStatus.Callback gnssStatusCallBack = new GnssStatus.Callback() {
        @Override
        public void onSatelliteStatusChanged(GnssStatus status) {
            int satelliteCount = status.getSatelliteCount();
            mSatellites = 0;
            for (int i = 0; i < satelliteCount; i++) {
                if (status.usedInFix(i)) {
                    mSatellites++;
                }
            }
            //Log.d(TAG, "Satellites used in fix: " + mSatellites);
        }

        @Override
        public void onFirstFix(int ttffMillis) {
            super.onFirstFix(ttffMillis);
            Log.d(TAG, "First fix: " + String.valueOf(ttffMillis));
            mFix = true;
            Toast.makeText(mContext, "Succesfull satellite fix!",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStarted() {
            super.onStarted();
            Log.d(TAG, "GPS_EVENT_STARTED...");
            //LogUtil.sharedInstance().appendLog("Acquiring satellite fix...");
            Toast.makeText(mContext, "Acquiring satellite fix...",
                    Toast.LENGTH_SHORT).show();

        }
        @Override
        public void onStopped() {
            super.onStopped();
            //LogUtil.sharedInstance().appendLog("GPS_EVENT_STOPPED...");
            Log.d(TAG, "GPS_EVENT_STOPPED...");

        }
    };
        if (ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.
                ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationManager.registerGnssStatusCallback(gnssStatusCallBack);
        //mLocationManager.registerGnssMeasurementsCallback(gnssMeasurementsCallback);
        //mLocationManager.registerGnssNavigationMessageCallback(gnssMessageCallback);
    }

    public boolean hasFix() {
        return mFix;
    }

    public Location getLocation() {
        if (isGPSEnabled) {
            if (mLocation == null) {
                Log.d(TAG, "Location Null");
                mLocation = new Location(LocationManager.GPS_PROVIDER);
            }
        } else {
            Log.d(TAG, "GPS not enabled");
        }
        return mLocation;
    }


    private boolean checkPermssion() {
        return ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext,
                        Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext,
                        Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            if (mLocationManager != null) {
                mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } else {
                Log.d(TAG, "Location Manager Null");
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }


}

