package com.onsite.onsitefaulttracker_v2.util;

import android.content.Context;

import com.onsite.onsitefaulttracker_v2.model.Record;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LogUtil {

    private static LogUtil sLogUtil;
    // The application context
    private Context mContext;
    private static File logFile;
    private static File accelFile;
    private static File oriFile;

    /**
     * initializes LogUtil.
     *
     * @param context
     */
    public static void initialize(final Context context) {
        sLogUtil = new LogUtil(context);
    }


    public static LogUtil sharedInstance() {
        if (sLogUtil != null) {
            return sLogUtil;
        } else {
            throw new RuntimeException("RecordUtil must be initialized in the Application class before use");
        }
    }

    /**
     * The constructor for LogUtil, called internally
     *
     * @param context
     */
    private LogUtil(final Context context) {
        mContext = context;

    }

    public static void createLog() {

        Record record = RecordUtil.sharedInstance().getCurrentRecord();
        String path = RecordUtil.sharedInstance().getPathForRecord(record);
        logFile = new File(path + "/", record.recordName + "_log" + ".txt");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }



    public void createLog(String name) {

        Record record = RecordUtil.sharedInstance().getCurrentRecord();
        String path = RecordUtil.sharedInstance().getPathForRecord(record);
        accelFile = new File(path + "/", record.recordName + "_" + name + "_log" + ".txt");
        //if (!logFile.exists())
        //{
            switch(name) {
                case "acceleration":
                    accelFile = new File(path + "/", record.recordName + "_" + name + "_log" + ".txt");
                    try {
                        accelFile.createNewFile();
                    }
                    catch (IOException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;
                case "orientation":
                    oriFile = new File(path + "/", record.recordName + "_" + name + "_log" + ".txt");
                    try {
                        oriFile.createNewFile();
                    }
                    catch (IOException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
            }

        //}
    }

    public void appendLog(String acceleration, String orientation)
    {
        try
        {
            String dateTime = BitmapSaveUtil.sharedInstance().getDateString();
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(accelFile, true));
            buf.append(dateTime + ": " + acceleration);
            buf.newLine();
            buf.close();
            buf = new BufferedWriter(new FileWriter(oriFile, true));
            buf.append(dateTime + " " + orientation);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void appendLog(String text)
    {
        try
        {
            if (logFile == null) {
                createLog();
            }
            String dateTime = BitmapSaveUtil.sharedInstance().getDateString();
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(dateTime + "," + text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
