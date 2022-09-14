package com.onsite.onsitefaulttracker_v2.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import static java.lang.Thread.MAX_PRIORITY;

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
    private static final int FOLDER_SIZE = 100;
    private String mFolder;
    //private int totalBitMapCount = 0;
    private long totalPhotokB = 0;
    // An enum which has all the SaveBitmapResult values
    public enum SaveBitmapResult {
        Save,
        SaveLowDiskSpace,
        Error
    }
    private AtomicInteger count;
    private Record currentRecord;
    private Calendar mCal;
    private TimeZone mTz;
    // The format of file names when converted from a date
    private static final String FILE_DATE_FORMAT = "yyMMdd_HHmmss";
    private static final String MILLI_DATE_FORMAT = "dd/MM/yyyy HH:mm:ss.SSS";
    private static final String TIME_STAMP__FORMAT = "yyyy:MM:dd HH:mm:ss";
    // A static instance of the bitmap save utilities
    private static BitmapSaveUtil sBitmapSaveUtil;
    // Store the application context for access to storage
    private static Context mContext;
    private ExecutorService mThreadPool;
    private final SimpleDateFormat millisecondFormat =  new SimpleDateFormat(MILLI_DATE_FORMAT);
    private final SimpleDateFormat timeStampFormat =  new SimpleDateFormat(TIME_STAMP__FORMAT);
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(FILE_DATE_FORMAT);

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
        count = new AtomicInteger(SettingsUtil.sharedInstance().getPhotoCount());
        mCal = Calendar.getInstance();
        mTz = mCal.getTimeZone();
        ThreadFactoryUtil factory = new ThreadFactoryUtil("bitmap", MAX_PRIORITY);
        mThreadPool = Executors.newCachedThreadPool(factory);
        mFolder = "/" + SettingsUtil.sharedInstance().getDefaultPhotoFolder();
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

    public String getDateString() {
        Calendar dateTime = correctDateTime();
        final SimpleDateFormat timeStampFormat =  new SimpleDateFormat("dd/MM/yyyy HH:mm:ssZ");
        String correctedDateString = timeStampFormat.format(dateTime.getTime());
        return correctedDateString;
    }

    public long getPhotoBytes() {
        return totalPhotokB;
    }

    public int getPhotoCount() {
        return count.intValue();
    }

    public void reset() {
        count = new AtomicInteger(0);
        mFolder = SettingsUtil.sharedInstance().getDefaultPhotoFolder();
        SettingsUtil.sharedInstance().setPhotoCount(0);
        SettingsUtil.sharedInstance().setPhotoFolder(SettingsUtil.sharedInstance().getDefaultPhotoFolder());
        SettingsUtil.sharedInstance().deleteKey("Folder");
        SettingsUtil.sharedInstance().deleteKey("PhotoCount");
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
        long availableSpace = CalculationUtil.sharedInstance().getAvailableStorageSpaceKB();
        if (availableSpace <= 1024) {
            return SaveBitmapResult.Error;
        }
        count.getAndIncrement();
        int totalBitMapCount = count.intValue();
        final Calendar correctedDate = correctDateTime();
        final Location location = GPSUtil.sharedInstance().getLocation();
        Long gpsTime = location.getTime();
        final String gpsTimeStamp = millisecondFormat.format(gpsTime); //sent in message
        final String basePath = RecordUtil.sharedInstance().getPathForRecord(record);
        if (totalBitMapCount % FOLDER_SIZE == 0) {
            String suffix = String.valueOf(totalBitMapCount / FOLDER_SIZE);
            String folder = SettingsUtil.sharedInstance().getDefaultPhotoFolder();
            mFolder = folder + suffix;
            SettingsUtil.sharedInstance().setPhotoFolder(mFolder);
            RecordUtil.sharedInstance().createNewFolder(basePath + "/" + mFolder);
        }
        String bitmapCount = String.format("%06d", totalBitMapCount);
        String correctedFileDate = simpleDateFormat.format(correctedDate.getTime());
        String cameraIdPrefix = SettingsUtil.sharedInstance().getInspectorId();
        cameraIdPrefix += "_";
        final String path = basePath + "/" + mFolder + "/";
        final String filename = cameraIdPrefix + "IMG" + correctedFileDate + "_" + bitmapCount;
        File folder = new File(path);
        if (!folder.exists()) {
            Log.e(TAG, "Error saving snap, Record path does not exist");
            return SaveBitmapResult.Error;
        }
        Runnable task = () -> {
            try {
                long start = System.currentTimeMillis();
                final File file = new File(path, filename + ".jpg");
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
                fOutputStream.flush();
                final long jpegBytes = ((FileOutputStream) fOutputStream).getChannel().size();
                totalPhotokB += (jpegBytes / 1000);
                fOutputStream.close();
                long finish = System.currentTimeMillis();
                final long time = (finish - start);
                final String _file = file.getAbsolutePath();
                final String timeStamp = timeStampFormat.format(correctedDate.getTime());
                Runnable geotagTask = () -> {
                    long geostart = System.currentTimeMillis();
                    EXIFUtil.sharedInstance().geoTagFile(_file, timeStamp, location);
                    long finish1 = System.currentTimeMillis();
                    //Log.i(TAG, "Geotag time" + (finish1 - geostart));
                };

                if (BLTManager.sharedInstance().getState() == 3) {
                    final long frequency = SettingsUtil.sharedInstance().getPictureFrequency();
                    Runnable messageTask = () ->
                            sendMessage(gpsTimeStamp, filename, photo, time, frequency, jpegBytes);
                    mThreadPool.execute(messageTask);
                }
                mThreadPool.execute(geotagTask);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            bitmapToSave.recycle();
        };
        mThreadPool.execute(task);
        Log.i(TAG, "Thread Pool " + mThreadPool.toString());

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
