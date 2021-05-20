package com.onsite.onsitefaulttracker_v2.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import com.onsite.onsitefaulttracker_v2.connectivity.BLTManager;
import com.onsite.onsitefaulttracker_v2.model.Record;
import com.onsite.onsitefaulttracker.util.ThreadFactoryUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.MIN_PRIORITY;
import static java.lang.Thread.NORM_PRIORITY;

/**
 * Created by hihi on 6/21/2016.
 *
 * Utility class which will handle saving bitmaps to storage
 */
public class BitmapSaveUtil {

    // The tag name for this utility class
    private static final String TAG = BitmapSaveUtil.class.getSimpleName();
    // The low disk space threshold
    private static final long LOW_DISK_SPACE_THRESHOLD = 204800L; //204.8 MB
    private static final double THUMBNAIL_REDUCTION = 0.25;
    private int totalBitMapTime = 0;
    private int totalBitMapCount = 0;
    // An enum which has all the SaveBitmapResult values
    public enum SaveBitmapResult {
        Save,
        SaveLowDiskSpace,
        Error
    }
    private AtomicInteger count;
    private static final int FILE_THRESHOLD = 10000;
    private Record currentRecord;
    private String correctedDateString;
    private Calendar mCal = Calendar.getInstance();
    private TimeZone mTz = mCal.getTimeZone();
    // The format of file names when converted from a date
    private static final String FILE_DATE_FORMAT = "yyMMdd_HHmmss";
    private static final String MILLI_DATE_FORMAT = "dd/MM/yyyy HH:mm:ss.SSS";
    private static final String TIME_STAMP__FORMAT = "yyyy:MM:dd HH:mm:ss";
    // A static instance of the bitmap save utilities
    private static BitmapSaveUtil sBitmapSaveUtil;
    // Store the application context for access to storage
    private static Context mContext;
    private ExecutorService mThreadPool;

    /**
     * Store the appication context for access to storage
     *
     * @param context
     */
    public static void initialize(final Context context) {
        sBitmapSaveUtil = new BitmapSaveUtil(context);
    }

    /**
     * Contructor, to be called internally via. initialize
     * @param context
     */
    private BitmapSaveUtil(final Context context) {
        mContext = context;
        currentRecord = RecordUtil.sharedInstance().getCurrentRecord();
        try {
            count = new AtomicInteger(currentRecord.photoCount);
        } catch (Exception e) {
            count = new AtomicInteger(0);
        }
        mCal = Calendar.getInstance();
        mTz = mCal.getTimeZone();
        ThreadFactoryUtil factory = new ThreadFactoryUtil("message", NORM_PRIORITY);
        mThreadPool = new ThreadPoolExecutor(8, 8, 5, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), factory,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * Returns a shared instance of BitmapSaveUtil
     * @return
     */
    public static BitmapSaveUtil sharedInstance() {
        if (sBitmapSaveUtil != null) {
            return sBitmapSaveUtil;
        } else {
            throw new RuntimeException("BitmapSaveUtil must be initialized in the " +
                    "Application class before use");
        }
    }

    //used by logUtil
    public String getDateString() {
        Calendar dateTime = correctDateTime();
        final SimpleDateFormat timeStampFormat =  new SimpleDateFormat("dd/MM/yyyy HH:mm:ssZ");
        String correctedDateString = timeStampFormat.format(dateTime.getTime());
        return correctedDateString;
    }

    private Calendar correctDateTime() {
        long timeDelta = BLTManager.sharedInstance().getTimeDelta();
        Calendar cal =  Calendar.getInstance();
        long timeNow = cal.getTimeInMillis();
        long correctedMilli = timeNow - timeDelta;
        cal.setTimeInMillis(correctedMilli);
        return cal;
    }

    /**
     * Saves a bitmap to storage taking in a temp number for now for the filename
     *
     * @param bitmapToSave
     * @param record
     * @param widthDivisor a factor to divide the width by
     */
    public SaveBitmapResult saveBitmap(final Bitmap bitmapToSave,
                                       final Record record,
                                       final float widthDivisor,
                                       final boolean isLandscape) {

        final SimpleDateFormat millisecondFormat =  new SimpleDateFormat(MILLI_DATE_FORMAT);
        final SimpleDateFormat timeStampFormat =  new SimpleDateFormat(TIME_STAMP__FORMAT);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(FILE_DATE_FORMAT);
        final Calendar correctedDate = correctDateTime();
        final Location location = GPSUtil.sharedInstance().getLocation();
        Long gpsTime = location.getTime();
        final String gpsTimeStamp = millisecondFormat.format(gpsTime); //sent in message
        count.getAndIncrement();
        totalBitMapCount = count.intValue();
        String bitmapCount = String.format("%06d", totalBitMapCount);
        String correctedFileDate = simpleDateFormat.format(correctedDate.getTime());
        String millicorrectedFileDate = millisecondFormat.format(correctedDate.getTime());
        String cameraIdPrefix = SettingsUtil.sharedInstance().getCameraId();
        if (cameraIdPrefix == null) {
            cameraIdPrefix = "NOID";
        }
        cameraIdPrefix += "_";
        final String filename = cameraIdPrefix + "IMG" + correctedFileDate + "_" + bitmapCount;
        long availableSpace = CalculationUtil.sharedInstance().getAvailableStorageSpaceKB();
        if (availableSpace <= 1024) {
            return SaveBitmapResult.Error;
        }
        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {
                final String path = RecordUtil.sharedInstance().getPathForRecord(record);
                File folder = new File(path);
                if (!folder.exists()) {
                    Log.e(TAG, "Error saving snap, Record path does not exist");
                    return;
                }
                try {
                    File file = new File(path + "/Photos/", filename + ".jpg");;
                    long start = System.currentTimeMillis();
                    OutputStream fOutputStream = new FileOutputStream(file);
                    float reductionScale = CalculationUtil.sharedInstance()
                            .estimateScaleValueForImageSize();
                    int outWidth = Math.round(bitmapToSave.getHeight() / widthDivisor);
                    int outHeight = bitmapToSave.getHeight();
                    Bitmap sizedBmp = Bitmap.createScaledBitmap(bitmapToSave,
                            Math.round(outWidth * reductionScale), Math.round(outHeight *
                                    reductionScale), true);
                    Matrix matrix = new Matrix();
                    if (isLandscape) {
                        matrix.postRotate(-90);
                    }
                    Bitmap rotatedBitmap = Bitmap.createBitmap(sizedBmp, 0, 0,
                            sizedBmp.getWidth(), sizedBmp.getHeight(), matrix, true);
                    if (rotatedBitmap == null) {
                        return;
                    }
                    sizedBmp.recycle();
                    final ByteArrayOutputStream photo = new ByteArrayOutputStream();
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, CalculationUtil
                            .sharedInstance().estimateQualityValueForImageSize(), fOutputStream);
                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(rotatedBitmap,
                            (int)(rotatedBitmap.getWidth() * THUMBNAIL_REDUCTION),
                            (int)(rotatedBitmap.getHeight() * THUMBNAIL_REDUCTION), true);
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, CalculationUtil
                            .sharedInstance().estimateQualityValueForImageSize(),
                            photo);
                    rotatedBitmap.recycle();
                    resizedBitmap.recycle();
                    bitmapToSave.recycle();
                    fOutputStream.flush();
                    final long jpegBytes = ((FileOutputStream) fOutputStream).getChannel().size();
                    fOutputStream.close();
                    String _file = file.getAbsolutePath();
                    final String timeStamp = timeStampFormat.format(correctedDate.getTime());
                    EXIFUtil.sharedInstance().geoTagFile(_file, timeStamp, location);
                    long finish = System.currentTimeMillis();
                    final long time = (finish - start);

                    //final Double saveTime = new BigDecimal(time).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
                    final long frequency = SettingsUtil.sharedInstance().getPictureFrequency();
                    if (BLTManager.sharedInstance().getState() == 3) {
                        sendMessage(gpsTimeStamp, filename, photo, time, frequency, jpegBytes);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                bitmapToSave.recycle();
            }
        });
        if (availableSpace <= LOW_DISK_SPACE_THRESHOLD) {
            return SaveBitmapResult.SaveLowDiskSpace;
        } else {
            return SaveBitmapResult.Save;
        }
    }

    public ExecutorService getThreadPool() {
        return mThreadPool;
    }

    private void sendMessage(String date, String filename, ByteArrayOutputStream photo,
                             long saveTime, long frequency, long jpegBytes) {
        String message = buildMessage(date, filename, saveTime, frequency, jpegBytes);
        MessageUtil.sharedInstance().setMessage(message);
        MessageUtil.sharedInstance().setPhoto(photo.toByteArray());
        int messageLength = MessageUtil.sharedInstance().getMessageLength();
        int payload = messageLength + 21 + photo.size();
        MessageUtil.sharedInstance().setPayload(payload);
        ByteArrayOutputStream msg = MessageUtil.sharedInstance().getMessage();
        BLTManager.sharedInstance().sendMessge(msg);
    }

    /**
     *  Builds a message string using StringBuilder ready for sending through bluetooth
     * @param dateTime - a date time stamp the photo was taken
     * @param file - the filename of the photo
     * @param saveTime - the time taken to prepare the bitmap (testing only)
     * @param frequency - milliseconds (time between photo)
     * @param jpegBytes - approximate size in bytes of the jpeg photo
     * @return - a string with relevant data ready to be sent through bluetooth
     */
    private String buildMessage(String dateTime, String file, long saveTime, long frequency,
                                long jpegBytes) {

        StringBuilder messageString = new StringBuilder();
        messageString.append("T:" + dateTime + "|");
        messageString.append(file + "|");
        messageString.append(saveTime + "|");
        messageString.append(frequency + "|");
        messageString.append(jpegBytes + ",");
        String message = messageString.toString();
        return message;
    }
}
