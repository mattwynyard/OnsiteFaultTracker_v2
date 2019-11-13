package com.onsite.onsitefaulttracker_v2.util;

import android.content.Context;
import android.location.Location;
import android.media.ExifInterface;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;

public class EXIFUtil {

    // The tag name for this utility class
    private static final String TAG = EXIFUtil.class.getSimpleName();

    // The application context
    private Context mContext;

    //    // The static instance of this class which will be initialized once then reused
//    // throughout the app
//    private static GPSUtil sGPSUtil;
    // Shared Instance, to be initialized once and used throughout the application
    private static EXIFUtil sSharedInstance;

    private static EXIFUtil sEXIFUtil;

    /**
     * initializes GPSUtil.
     * @param context The application context
     */
    public static void initialize(final Context context) {

        sEXIFUtil = new EXIFUtil(context);
    }
    /**
     * The constructor for GPSUtil, called internally
     *
     * @param context
     */
    public EXIFUtil(Context context) {
        mContext = context;

    }
    /**
     * return the shared instance of Record Util
     *
     * @return
     */
    public static EXIFUtil sharedInstance() {
        if (sEXIFUtil != null) {
            return sEXIFUtil;
        } else {
            throw new RuntimeException("GPSUtil must be initialized " +
                    "in the Application class before use");
        }
    }

    public void geoTagFile(String path, String timeStamp, Location location) {
        String datum = "WGS_84";
        Double latitude_ref;
        Double longitude_ref;
        Double altitude_ref;
        Double bearing_ref;
        if (location == null) {
            latitude_ref = -36.939318;
            longitude_ref = 174.892701;
            altitude_ref = 39.0;
            bearing_ref = 0.0;
        } else {
            latitude_ref = location.getLatitude();
            longitude_ref = location.getLongitude();
            altitude_ref = location.getAltitude();
            bearing_ref = Double.valueOf(location.getBearing());
        }
        String bearing = formatEXIFDouble(bearing_ref, 100);
        String latitude = DMS(latitude_ref, 10000);
        String longitude = DMS(longitude_ref, 10000);
        String altitude = formatEXIFDouble(altitude_ref, 100);
        writeGeoTag(path, latitude, latitude_ref, longitude, longitude_ref, altitude, altitude_ref,
                bearing, timeStamp, datum);
    }
    //--EXIF FUNCTIONS--
//TODO fix for negative altitudes
    public void writeGeoTag(final String path, final String latitude, final Double latitude_ref,
                            final String longitude, final Double longitude_ref,
                            final String altitude, final Double altitude_ref, final String bearing,
                            final String timeStamp, final String datum) {

        try {
            ExifInterface exif = new ExifInterface(path);
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE,
                    latitude);
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latitude_ref
                    < 0 ? "S" : "N");
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,
                    longitude);
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, longitude_ref
                    < 0 ? "W" : "E");
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE,
                    altitude);
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, altitude_ref
                    < 0 ? "1" : "0");
            exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION,
                    bearing);
            exif.setAttribute(ExifInterface.TAG_GPS_SPEED,
                    "0/10");
            exif.setAttribute(ExifInterface.TAG_GPS_DOP,
                    "999/10");
            exif.setAttribute(ExifInterface.TAG_GPS_SATELLITES,
                    "0");
            exif.setAttribute(ExifInterface.TAG_DATETIME, timeStamp);
            exif.setAttribute(ExifInterface.TAG_GPS_MAP_DATUM, datum);
            exif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
//
//            Log.d(TAG, "Wrote geotag" + path);
//            Log.d(TAG, "Latitude " + exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
//            Log.d(TAG, "Longitude " + exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
//            Log.d(TAG, "Altitude " + exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE));

    }

    private String getDateTimeStamp(Date date, String type) {
        //Date date = new Date(gpsTime);
        DateFormat dateformater = new SimpleDateFormat("yyyyMMddHHmmssZZZZZ");
        dateformater.setTimeZone(TimeZone.getDefault());
        String timestamp = dateformater.format(date);

        if (type.equals("date")) {
            StringBuilder s = new StringBuilder();
            String year = timestamp.substring(0,4);
            String month = timestamp.substring(4,6);
            String day = timestamp.substring(6,8);
            s.append(day);
            s.append(":");
            s.append(month);
            s.append(":");
            s.append(year);
            return s.toString();
        } else {
            StringBuilder s = new StringBuilder();
            String hour = timestamp.substring(8,10);
            String minutes = timestamp.substring(10,12);
            String seconds = timestamp.substring(12,14);
            //String zone = timestamp.substring(14,20);
            s.append(hour);
            s.append(":");
            s.append(minutes);
            s.append(":");
            s.append(seconds);
            return s.toString();
        }
    }
    /**
     * Converts a double value to the exif format
     * @param x - the number to convert
     * @param precision - the multiplier for altitude precision i.e the number of decimal places.
     * @return the converted coordinate as a string in the exif format
     */
    private String formatEXIFDouble(double x, int precision) {
        Double d = Math.abs(x) * precision;
        int altitude = (int)Math.floor(d);
        return String.format("%d/" + String.valueOf(precision), altitude);
    }
    /**
     * Converts decimal lat/long coordinate to degrees, minutes, seconds. The returned string is in
     * the exif format
     *
     * @param x - the coordinate to convert
     * @param precision - the multiplier for seconds precision
     * @return the converted coordinate as a string in the exif format
     */
    private String DMS(double x,  int precision) {
        double d = Math.abs(x);
        int degrees = (int) Math.floor(d);
        int minutes = (int) Math.floor(((d - (double)degrees) * 60));
        int seconds = (int)(((((d - (double)degrees) * 60) - (double)minutes) * 60) * precision);
        return String.format("%d/1,%d/1,%d/" + precision, degrees, minutes, seconds);
    }


}
