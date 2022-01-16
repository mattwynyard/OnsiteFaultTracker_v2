package com.onsite.onsitefaulttracker_v2.util;

import android.content.Context;
import android.location.Location;
import android.media.ExifInterface;
import android.util.Log;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;

public class EXIFUtil {

    // The tag name for this utility class
    private static final String TAG = EXIFUtil.class.getSimpleName();
    // The application context
    private Context mContext;
    private static EXIFUtil sSharedInstance;
    private static EXIFUtil sEXIFUtil;
    private final String datum = "WGS_84";
    private Double latitude_ref;
    private Double longitude_ref;
    private Double altitude_ref;
    private Long locationFixTime;
    private Float speed = 0.0f;
    private Float accuracy = 0.0f;
    private final SimpleDateFormat dateStampFormat =  new SimpleDateFormat("yyyy:MM:dd");
    private final SimpleDateFormat timeStampFormat =  new SimpleDateFormat("HH:mm:ss");
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

    public void geoTagFile(String path, String correctedTimeStamp, Location location) {
        locationFixTime = location.getTime();
        Double bearing_ref;
        if (location == null) {
            latitude_ref = -36.939318;
            longitude_ref = 174.892701;
            altitude_ref = 39.0;
            bearing_ref = 0.0;
        } else {
            locationFixTime = location.getTime();
            latitude_ref = location.getLatitude();
            longitude_ref = location.getLongitude();
            altitude_ref = location.getAltitude();
            bearing_ref = Double.valueOf(location.getBearing());
            speed = location.getSpeed();
            accuracy = location.getAccuracy();
        }
        int satellites = GPSUtil.sharedInstance().getSatellites();
        String satStr = String.valueOf(satellites);
        String speedStr = formatEXIFFloat(speed, 10);
        String dop = formatEXIFFloat(accuracy, 10);
        Date fixTimeStamp = new Date(locationFixTime);
        String fixDate = dateStampFormat.format(fixTimeStamp);
        String fixTime = timeStampFormat.format(fixTimeStamp);
        String bearing = formatEXIFDouble(bearing_ref, 100);
        String latitude = DMS(latitude_ref, 10000);
        String longitude = DMS(longitude_ref, 10000);
        String altitude = formatEXIFDouble(altitude_ref, 100);
        writeGeoTag(path, latitude, latitude_ref, longitude, longitude_ref, altitude, altitude_ref,
                bearing, speedStr, satStr, dop, datum, fixDate, fixTime, correctedTimeStamp);
    }

    public void writeGeoTag(final String path, final String latitude, final Double latitude_ref,
                            final String longitude, final Double longitude_ref, final String altitude,
                            final Double altitude_ref, final String bearing, final String speed, final String satellites,
                            final String dop, final String datum, final String fixDate,
                            final String fixTime, final String timeStamp) {
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
                    speed);
            exif.setAttribute(ExifInterface.TAG_GPS_DOP,
                    dop);
            exif.setAttribute(ExifInterface.TAG_GPS_SATELLITES,
                    satellites);
            exif.setAttribute(ExifInterface.TAG_DATETIME, timeStamp);
            exif.setAttribute(ExifInterface.TAG_GPS_MAP_DATUM, datum);
            exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, fixDate);
            exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, fixTime);
            exif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String formatEXIFFloat(float number, int precision) {
        Float d = Math.abs(number) * precision;
        int x = (int)Math.floor(d);
        return String.format("%d/" + precision, x);
    }
    /**
     * Converts a double value to the exif format
     * @param x - the number to convert
     * @param precision - the multiplier for altitude precision i.e the number of decimal places.
     * @return the converted coordinate as a string in the exif format
     */
    private String formatEXIFDouble(double x, int precision) {
        Double d = Math.abs(x) * precision;
        int n = (int)Math.floor(d);
        return String.format("%d/" + precision, n);
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
