package com.onsite.onsitefaulttracker_v2.util;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class MessageUtil {

    // The tag name for this utility class
    private static final String TAG = BitmapSaveUtil.class.getSimpleName();
    // A static instance of the bitmap save utilities
    private static MessageUtil sMessageUtil;

    private byte[] recording;
    private byte[] battery;
    private byte[] error;
    private byte[] messageLength;
    private byte[] message;
    private byte[] photoLength;
    private byte[] photo;
    private byte[] payload;

    public static void initialize(final Context context) {
        sMessageUtil = new MessageUtil(context);
    }

    /**
     * Returns a shared instance of BitmapSaveUtil
     * @return
     */
    public static MessageUtil sharedInstance() {
        if (sMessageUtil != null) {
            return sMessageUtil;
        } else {
            throw new RuntimeException("BitmapSaveUtil must be initialized in the " +
                    "Application class before use");
        }
    }

    /**
     * Contructor, to be called internally via. initialize
     * @param context
     */
    private MessageUtil(final Context context) {

    }

    public void setRecording(String recording) {
        this.recording = recording.getBytes(StandardCharsets.US_ASCII);
    }

    public void setBattery(int battery) {
        this.battery = ByteBuffer.allocate(4).putInt(battery).array();
    }

    public void setError(int error) {
        this.error = ByteBuffer.allocate(4).putInt(error).array();
    }


    public void setMessage(String message) {
        this.message = message.getBytes(StandardCharsets.US_ASCII);
        this.messageLength = ByteBuffer.allocate(4).putInt(this.message.length).array();

    }

    public int getMessageLength() {
        return message.length;
    }

    public int getPhotoLength() {
        return photo.length;
    }

    public void setPhoto(byte[] photo) {
        if (photo == null) {
            this.photo = null;
            this.photoLength = ByteBuffer.allocate(4).putInt(0).array();
        } else {
            this.photo = photo;
            this.photoLength = ByteBuffer.allocate(4).putInt(photo.length).array();
        }

    }

    public void setPayload(int payload) {
        this.payload = ByteBuffer.allocate(4).putInt(payload).array();
    }

    public ByteArrayOutputStream getMessage() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(payload);
            out.write(messageLength); //4 bytes
            out.write(photoLength); //4 bytes
            out.write(recording); //2 bytes
            out.write(battery); //4 bytes
            out.write(error); //4 bytes
            out.write(message);
            if (photo != null) {
                out.write(photo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;
    }



}
