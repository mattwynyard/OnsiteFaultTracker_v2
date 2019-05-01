package com.onsite.onsitefaulttracker_v2.connectivity;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.onsite.onsitefaulttracker_v2.model.notifcation_events.BLTStartRecordingEvent;
import com.onsite.onsitefaulttracker_v2.model.notifcation_events.BLTStopRecordingEvent;
import com.onsite.onsitefaulttracker_v2.util.BatteryUtil;
import com.onsite.onsitefaulttracker_v2.util.BitmapSaveUtil;
import com.onsite.onsitefaulttracker_v2.util.BusNotificationUtil;
import com.onsite.onsitefaulttracker_v2.util.MessageUtil;
import com.onsite.onsitefaulttracker_v2.util.ThreadUtil;

//import com.onsite.onsitefaulttracker.model.notifcation_events.BLTStartRecordingEvent;
//import com.onsite.onsitefaulttracker.model.notifcation_events.BLTStopRecordingEvent;
import com.onsite.onsitefaulttracker_v2.model.notifcation_events.BLTConnectedNotification;
import com.onsite.onsitefaulttracker_v2.model.notifcation_events.BLTNotConnectedNotification;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class BLTManager {

    // Tag name for this class
    private static final String TAG = BLTManager.class.getSimpleName();
    // Shared Instance, to be initialized once and used throughout the application
    private static BLTManager sSharedInstance;
    // Application context
    private Application mApplicationContext;
    private BluetoothAdapter mBluetoothAdapter;

    private static final UUID UUID_UNSECURE = UUID.fromString("00030000-0000-1000-8000-00805F9B34FB");
    private static final String NAME = "OnsiteBluetoothserver";

    private BluetoothSocket mSocket;
    private AcceptThread mAcceptThread;
    private Thread mReadThread;
    private InputStream in;
    private PrintWriter mWriterOut;

    private boolean mRecording;
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int STATE_TIMEOUT = 4;  // now connected to a remote device
    public static final int STATE_NOTENABLED = 9;  // bluetooth not enabled on phone

    public static Date gpsTime;
    public static long startNano;

    private ExecutorService mThreadPool;
    ReentrantLock lock = new ReentrantLock();

    private int mState;    /**



     * initialize the BLTManager class,  to be called once from the application class
     *
     * @param applicationContext The application context
     */
    public static void initialize(final Application applicationContext) {
        sSharedInstance = new BLTManager(applicationContext);
    }

    /**
     * returns the shared instance of BLEManager
     *
     * @return
     */
    public static BLTManager sharedInstance() {
        if (sSharedInstance != null) {
            return sSharedInstance;
        } else {
            throw new RuntimeException("BLTManager must be initialized before use");
        }
    }

    /**
     * Constructor, called privately through the initialize function
     *
     * @param applicationContext
     */
    private BLTManager(final Application applicationContext) {
        mApplicationContext = applicationContext;
        setupBluetooth();
        Log.i(TAG, "Bluetooth Setup");

        com.onsite.onsitefaulttracker.util.ThreadFactoryUtil factory = new com.onsite.onsitefaulttracker.util.ThreadFactoryUtil("message");

        mThreadPool = BitmapSaveUtil.sharedInstance().getThreadPool();
    }

    private void startBLTConnection() {
        start();
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        //Cancel any thread attempting to make a connection
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        // Cancel any thread currently running a connection
        if (mReadThread != null) {
            try {
                mReadThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mReadThread = null;
        }
        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread(false);
            mAcceptThread.start();
            setState(STATE_CONNECTING);
        }
    }
    /**
     * Checks if bluetooth on the adapter is enabled.
     * @return true/false if blue is enabled.
     */
    public boolean isBluetoothEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    public void setupBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
    }

    /**
     * Return the current connection state.
     */
    public int getState() {
        return mState;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    public void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

    }

    public void setBTName(String id) {
        Log.d(TAG, "Phone Id: " + id);
        //BLUETOOTH_ADAPTER_NAME = id;
        mBluetoothAdapter.setName(id);
    }

    public void sendMessge(final ByteArrayOutputStream bytes) {
        try {
            bytes.writeTo(mSocket.getOutputStream());
            mSocket.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessge(final String message) {

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        //byte[] messageBytes = message.getBytes(StandardCharsets.US_ASCII);
        float currentBatteryLevel = BatteryUtil.sharedInstance().getBatteryLevel();
        int batteryLevel = Math.round(currentBatteryLevel);

        MessageUtil.sharedInstance().setRecording("N");
        MessageUtil.sharedInstance().setBattery(batteryLevel);
        MessageUtil.sharedInstance().setError(0);
        MessageUtil.sharedInstance().setMessage(message);
        MessageUtil.sharedInstance().setPhoto(null);
        int messageLength = MessageUtil.sharedInstance().getMessageLength();
        //int photoLength = 0;
        int payload = messageLength + 21;
        MessageUtil.sharedInstance().setPayload(payload);
        bytes = MessageUtil.sharedInstance().getMessage();

        try {
            bytes.writeTo(mSocket.getOutputStream());
            mSocket.getOutputStream().flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPhoto(final String header, final ByteArrayOutputStream photoBytes) {
        try {

            ByteArrayOutputStream headerOut = new ByteArrayOutputStream();
            byte[] ascii = header.getBytes(StandardCharsets.US_ASCII);
            byte[] prefix;
            if (photoBytes == null) {
                String start = "Z:";
                prefix = start.getBytes(StandardCharsets.US_ASCII);
                headerOut.write(prefix);
                headerOut.write(ascii);
                //headerOut.write(0x0a);
                Log.i(TAG, "Bytes Sent: " + (prefix.length + ascii.length));
                headerOut.writeTo(mSocket.getOutputStream());
            } else {
                byte [] messageLength = ByteBuffer.allocate(4).putInt(ascii.length).array();
                byte [] photoLength = ByteBuffer.allocate(4).putInt(photoBytes.size()).array();
                String start = "P:";
                prefix = start.getBytes(StandardCharsets.US_ASCII);
                byte[] photo = photoBytes.toByteArray();

                headerOut.write(prefix); //ascii 2bytes
                //Log.d(TAG, "Size: " + headerOut.size());
                headerOut.write(messageLength); //int
                //Log.d(TAG, "Size: " + headerOut.size());
                headerOut.write(ascii); //ascii
                //Log.d(TAG, "Size: " + headerOut.size());
                headerOut.write(photoLength); //int
                //Log.d(TAG, "Size: " + headerOut.size());
                headerOut.write(photo);

                lock.lock();
                try {
                    headerOut.writeTo(mSocket.getOutputStream());
                    Log.i(TAG, "Bytes Sent: " + (prefix.length + messageLength.length +
                            ascii.length + photoLength.length + photo.length));
                    mSocket.getOutputStream().flush();
                    headerOut.close();
                    photoBytes.close();
                } finally {
                    lock.unlock();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Class to handle the accept connection from bluetooth device behaves as server socket
     * and runs on its own thread
     */
    //SPP maximum payload capacity is 128 bytes.
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        private AcceptThread(boolean secure) {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;


            mSocketType = secure ? "Secure" : "Insecure";
            // Create a new listening server socket
            Log.i(TAG, "NAME: " + NAME);
            Log.i(TAG, "NAME: " + UUID_UNSECURE);
            Log.i(TAG, "ADDRESS = " + mBluetoothAdapter.getAddress());
            try {
                if (secure) {
                    tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME,
                            UUID_UNSECURE);
                } else {
                    tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME, UUID_UNSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + mSocketType +
                    " BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);
            //BusNotificationUtil.sharedInstance().postNotification(new BLTListeningNotification());
            // Keep listening until exception occurs or a socket is returned.
            while (mState != STATE_CONNECTED) {
                try {
                    Log.i(TAG,  "Server socket listening");
                    mSocket = mmServerSocket.accept();
                    setState(STATE_LISTEN);
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }
                if (mSocket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    Log.i(TAG, "Bluetooth socket accepted connection");
                    Log.i(TAG, "Connected to: " + mSocket.getRemoteDevice().getAddress());
                    setState(STATE_CONNECTED);
                    BusNotificationUtil.sharedInstance().postNotification(new BLTConnectedNotification());
                    try {
                        mWriterOut = new PrintWriter(mSocket.getOutputStream(), true);

                        sendMessge("CONNECTED,");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mReadThread = new Thread(readFromClient);
                    mReadThread.setName("ReadThread");
                    mReadThread.setPriority(MAX_PRIORITY);
                    mReadThread.start();
                }
            }
        }
        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
                mAcceptThread.join();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            } catch (InterruptedException e) {
                Log.e(TAG, "Thread interupt", e);
                e.printStackTrace();
            }
        }
        /**
         * Close the socket connection if open and restart listening for a connection
         */
        private void closeAll() {
            try {
                //BusNotificationUtil.sharedInstance().postNotification(new BLTStopRecordingEvent());
                Log.e(TAG, "Closing All");
                BusNotificationUtil.sharedInstance().postNotification(new BLTNotConnectedNotification());
                in.close();
                in = null;
                mSocket.close();
                mmServerSocket.close();
                mWriterOut.close();
                mWriterOut = null;
                mReadThread = null;
                restartBLTConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        /**
         * Restarts listening for a socket connection
         * after a one second wait
         */
        private void restartBLTConnection() {
            ThreadUtil.executeOnMainThreadDelayed(new Runnable() {
                @Override
                public void run() {
                    startBLTConnection();
                }
            }, 1000);
            Log.i(TAG, "Restarting connection");
        }

        private final Runnable readFromClient = new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[128];
                int length;
                Log.i(TAG, "Read thread started");
                try {
                    in = mSocket.getInputStream();
                    while ((length = in.read(buffer)) != -1) {
                        String line = new String(buffer, 0, length);
                        Log.d(TAG, "Buffer: " + line);
                        if (line.contains("Start")) {
                                BusNotificationUtil.sharedInstance()
                                        .postNotification(new BLTStartRecordingEvent());
                        } else if (line.contains("Stop")) {
                                BusNotificationUtil.sharedInstance()
                                        .postNotification(new BLTStopRecordingEvent());
                        } else if (line.contains("Time")) {
                            //set gpsTime;
                            startNano = System.nanoTime();

                        } else {
                            //System.out.println(line);
                        }
                    }
                } catch (IOException e) { //connection was lost
                    e.printStackTrace();
                } finally {
                    BusNotificationUtil.sharedInstance()
                            .postNotification(new BLTStopRecordingEvent());
                    setState(STATE_NONE);
                    closeAll();
                }
            }
        }; //end closure
    } //end private class
}



