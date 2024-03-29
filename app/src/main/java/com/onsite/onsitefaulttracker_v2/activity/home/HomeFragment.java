package com.onsite.onsitefaulttracker_v2.activity.home;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.onsite.onsitefaulttracker_v2.R;
import com.onsite.onsitefaulttracker_v2.activity.BaseFragment;
import com.onsite.onsitefaulttracker_v2.connectivity.BLTManager;
import com.onsite.onsitefaulttracker_v2.connectivity.TcpConnection;
import com.onsite.onsitefaulttracker_v2.model.Record;
import com.onsite.onsitefaulttracker_v2.model.notifcation_events.BLTConnectedNotification;
import com.onsite.onsitefaulttracker_v2.model.notifcation_events.BLTNotConnectedNotification;
import com.onsite.onsitefaulttracker_v2.model.notifcation_events.BLTStartRecordingEvent;
import com.onsite.onsitefaulttracker_v2.model.notifcation_events.UsbConnectedNotification;
import com.onsite.onsitefaulttracker_v2.model.notifcation_events.UsbDisconnectedNotification;

import com.onsite.onsitefaulttracker_v2.model.notifcation_events.BLTListeningNotification;
import com.onsite.onsitefaulttracker_v2.util.BusNotificationUtil;
import com.onsite.onsitefaulttracker_v2.util.GPSUtil;
import com.onsite.onsitefaulttracker_v2.util.LogUtil;
import com.onsite.onsitefaulttracker_v2.util.RecordUtil;
import com.onsite.onsitefaulttracker_v2.util.SettingsUtil;
import com.squareup.otto.Subscribe;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static java.lang.Boolean.FALSE;


/**
 * Created by hihi on 6/7/2016.
 *
 * Home Fragment is the default fragment for the Home Activity.
 * The Home screen is where the user can select to make a
 * new record,  view previous records or continue making a previous record.
 * The user can also access the settings screen from the settings button
 * in the action bar.
 */
public class HomeFragment extends BaseFragment {

    // The tag name for this fragment
    private static final String TAG = HomeFragment.class.getSimpleName();
    private Context mContext;
    // the request code for the camera permissions
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1;
    // the request code for the storage permissions
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 2;
    private static final int READ_PHONE_STATE_REQUEST_CODE = 4;
    private static final int PERMISSION_ALL = 99;
    private static final int REQUEST_ENABLE_DISCOVERY = 44;
    private static final int REQUEST_ENABLE_BT = 45;
    private final int BT_TIMEOUT = 1200; //seconds
    private String[] PERMISSIONS = {
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
    };
    // The display date format to display to the user
    private static final String DISPLAY_DATE_FORMAT = "dd MMM yyyy";
    private boolean mAdvertising = false;
    private boolean bluetooth = false;
    private String mInspector;
    // The current record name
    private TextView mCurrentRecordName;
    // The current record date
    private TextView mCurrentRecordDate;
    // The New Record Button
    private Button mNewRecordButton;
    // The Continue Last Record Button
    private Button mContinueRecordButton;
    // The submit button
    private Button mSubmitRecordButton;
    // The Previous Records button
    private Button mPreviousRecordsButton;
    // The connection status
    private TextView mConnectionStatusTextView;
    // Text View that displays the application version
    private TextView mAppVersion;
    // Listener for communicating with the parent activity
    private Listener mListener;
    // Tcp Connection runnable
    private TcpConnection mTcpConnection; // TODO:TEMPHACK TEST

    /**
     * On create view, Override this in each extending fragment to implement initialization for that
     * fragment.
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            mNewRecordButton = (Button) view.findViewById(R.id.new_record_button);
            mNewRecordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNewRecordClicked();
                }
            });

            mContinueRecordButton = (Button) view.findViewById(R.id.continue_record_button);
            mContinueRecordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onContinueButtonClicked();
                }
            });

            mSubmitRecordButton = (Button) view.findViewById(R.id.submit_record_button);
            mSubmitRecordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSubmitButtonClicked();
                }
            });

            mPreviousRecordsButton = (Button) view.findViewById(R.id.previous_records_button);
            mPreviousRecordsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onPreviousRecordsClicked();
                }
            });
            mCurrentRecordName = (TextView) view.findViewById(R.id.current_record_name);
            mCurrentRecordDate = (TextView) view.findViewById(R.id.current_record_date);
            mConnectionStatusTextView = (TextView) view.findViewById(R.id.connected_text_view);
            mAppVersion = (TextView) view.findViewById(R.id.app_version_text_view);
            initAppVersionText();
            if(!hasPermissions(mContext, PERMISSIONS)) {
                ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, PERMISSION_ALL);
            }
            enableBluetooth();
            mInspector = SettingsUtil.sharedInstance().getInspectorId();
            System.out.println("Inpsector: " + mInspector);
            if (mInspector != null || mInspector != "") {
                String btname = "OnSite_BLT_Adapter_" + mInspector;
                BLTManager.sharedInstance().setBTName(btname);
            }
        }
        return view;
    }

    /**
     * Action when the fragment gets attached to the parent activity, sets the listener
     * as the passed in context
     *
     * @param context
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            mListener = (Listener) context;
        }
        mContext = context;
        BusNotificationUtil.sharedInstance().getBus().register(this);
    }

    /**
     * Action when the fragment is detached from the parent activity, nullifies the
     * listener as it is no longer valid
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        BusNotificationUtil.sharedInstance().getBus().unregister(this);
        updateButtonStates();
    }

    /**
     * Action when fragment is resumed,
     * updates the state of all the buttons
     */
    @Override
    public void onResume() {
        super.onResume();
        if (BLTManager.sharedInstance().getState() != BLTManager.STATE_CONNECTED) {
            updateButtonStates();
        }
    }

    /**
     * Update the state of the buttons
     */
    private void updateButtonStates() {
        String inspector = SettingsUtil.sharedInstance().getInspectorId();
        Record r = RecordUtil.sharedInstance().getCurrentRecord();
        boolean hasCurrentRecord = r != null;
        if (hasCurrentRecord) {
            mNewRecordButton.setEnabled(false);
            mContinueRecordButton.setEnabled(true);
            mPreviousRecordsButton.setEnabled(true);
            mSubmitRecordButton.setEnabled(r.photoCount > 0);

        } else {
            if (inspector == "") {
                mNewRecordButton.setEnabled(false);
            } else {
                mNewRecordButton.setEnabled(true);
            }
            mContinueRecordButton.setEnabled(false);
            mSubmitRecordButton.setEnabled(false);
            mPreviousRecordsButton.setEnabled(false);
        }
        updateCurrentRecordText();
    }

    /**
     * Updates the current record name text and current record date text view
     */
    private void updateCurrentRecordText() {
        Record currentRecord = RecordUtil.sharedInstance().getCurrentRecord();
        if (currentRecord == null) {
            mCurrentRecordName.setText(getString(R.string.no_current_record));
            mCurrentRecordDate.setText("");
        } else {
            Calendar now = Calendar.getInstance();
            Calendar recordCalendar = Calendar.getInstance();
            recordCalendar.setTime(currentRecord.creationDate);
            boolean isToday = now.get(Calendar.DAY_OF_YEAR) == recordCalendar.get(Calendar.DAY_OF_YEAR)
                    && now.get(Calendar.YEAR) == recordCalendar.get(Calendar.YEAR);
            boolean isYesterday = now.get(Calendar.DAY_OF_YEAR) - 1 == recordCalendar.get(Calendar.DAY_OF_YEAR)
                    && now.get(Calendar.YEAR) == recordCalendar.get(Calendar.YEAR);
            String prefixString = isToday ? "(Today) " :
                    isYesterday ? "(Yesterday)" : "";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM yyyy, h:mm a");
            mCurrentRecordName.setText("Current record: " + currentRecord.recordName);
            mCurrentRecordDate.setText("Created on: " + prefixString + simpleDateFormat.format(currentRecord.creationDate));
        }
    }

    /**
     * Creates an Intent if bluetooth is not enables otherwise starts a bluetooth advert.
     */
    public void enableBluetooth() {
        //Bluetooth not enabled
        if (!BLTManager.sharedInstance().isBluetoothEnabled()) {
            Log.i(TAG, "Enabling bluetooth");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            Log.i(TAG, "Bluetooth Enabled");
            Log.i(TAG, "Starting BT advertisement");
            startAdvertising();
        }
    }

    /**
     * Creates an Intent to enable a bluetooth advert for 1000 seconds
     */
    //TODO Temp hack should be moved to BLTManager
    public void startAdvertising() {
        if (BLTManager.sharedInstance().isBluetoothEnabled() && !mAdvertising) {
            Intent discoverableIntent =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, BT_TIMEOUT);
            discoverableIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            //TODO fix for Android 9 issue with discoverable intent
            startActivityForResult(discoverableIntent, REQUEST_ENABLE_DISCOVERY);
        } else {
            Log.i(TAG, "Bluetooth Not Enabled");
        }
    }

    private void startBluetooth() {
        if (BLTManager.sharedInstance().getState() == BLTManager.STATE_NONE &&
                BLTManager.sharedInstance().isBluetoothEnabled()) {
            if (mAdvertising) {
                BLTManager.sharedInstance().start();
                Log.i(TAG, "Starting listen");
            }
        }
    }
    /**
     * Captures users selection results from pop-up message window
     * @param requestCode - the type of service requested
     * @param resultCode - the result from the user, usually ok/cancel or yes/no
     * @param data - a Intent carrying the result data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                Log.i(TAG, "Starting BT advertisement");
                startAdvertising();
            } else {
                Log.i(TAG, "Bluetooth not enabled");
                BLTManager.sharedInstance().setState(BLTManager.STATE_NOTENABLED);

            }
        } else if (requestCode == REQUEST_ENABLE_DISCOVERY) {
            //temp hack to enable bluetooth on Huawei
            if (resultCode == BT_TIMEOUT || resultCode == 120) { //user selected OK
                mAdvertising = true;
                BusNotificationUtil.sharedInstance().postNotification(new BLTListeningNotification());
                startBluetooth();
            } else {
                mAdvertising = false;
                BusNotificationUtil.sharedInstance().postNotification(new BLTNotConnectedNotification());
            }
        }
    }

    public boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "Request called");
        switch (requestCode) {
            case READ_PHONE_STATE_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // All good!
                    if (getActivity().checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                            == PackageManager.PERMISSION_GRANTED) {
                    }
                } else {
                    Log.d(TAG, "Need phone permission");
                }
                return;
        }
    }
    /**
     * Requests camera permissions if they are not already granted
     */
    private boolean requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (getActivity().checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_REQUEST_CODE);
                return true;
            }
        }
        return false;
    }

    /**
     * Requests camera permissions if they are not already granted
     */
    private boolean requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_REQUEST_CODE);
                return true;
            }
        }
        return false;
    }

    /**
     * Action when the user clicks
     */
    private void onNewRecordClicked() {
        if (requestStoragePermission()) {
            return;
        }
        mInspector = SettingsUtil.sharedInstance().getInspectorId();
        if (mInspector ==  "") {
            showInspectorMustBeEntered();
        } else {
            checkForExistingRecords();
            if(GPSUtil.sharedInstance().getLocation() == null) {
                GPSUtil.sharedInstance().intialiseGPS();
            }
            String btname = "OnSite_BLT_Adapter_" + mInspector;
            BLTManager.sharedInstance().setBTName(btname);
        }
    }

    /**
     * Action when user clicks on continue button, continue recording the current record
     */
    private void onContinueButtonClicked() {
        if (mListener != null) {
            mListener.onNewRecord();
        }
    }

    /**
     * Action when the user clicks on the submit button
     */
    private void onSubmitButtonClicked() {
        if (BLTManager.sharedInstance().getState() == BLTManager.STATE_CONNECTED) { //bluetooth connected disable
            return;
        }
        final Record currentRecord = RecordUtil.sharedInstance().getCurrentRecord();
        if (mListener != null && currentRecord != null) {
            mListener.onSubmitRecord(currentRecord.recordId);
        }
    }
    /**
     * Check for existing records
     */
    private void checkForExistingRecords() {
        if (RecordUtil.sharedInstance().checkRecordExistsForToday()) {
            updateButtonStates();
            if (bluetooth) {
                if (mListener != null) {
                    mListener.onNewRecord();
                }
            } else {
                updateButtonStates();
            }
        } else {
            requestRecordName();
        }
    }
    /**
     * Requests a name for the new record from the user then creates a new record
     * with that name
     */
    private void requestRecordName() {
        final RelativeLayout recordNameLayout = new RelativeLayout(getActivity());
        final EditText recordNameInput = new EditText(getActivity());
        recordNameInput.setHint(R.string.new_record_name_hint);
        RelativeLayout.LayoutParams recordNameParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        recordNameParams.leftMargin = getResources().getDimensionPixelSize(R.dimen.new_record_name_text_margin);
        recordNameParams.rightMargin = getResources().getDimensionPixelSize(R.dimen.new_record_name_text_margin);
        recordNameInput.setLayoutParams(recordNameParams);
        recordNameInput.setSingleLine();
        recordNameLayout.addView(recordNameInput);
        SimpleDateFormat dateFormat = new SimpleDateFormat(DISPLAY_DATE_FORMAT);
        final String todaysDisplayDate = dateFormat.format(new Date());
        createRecord(mInspector + "_" + todaysDisplayDate);
        updateButtonStates();
        if (bluetooth) {
            mListener.onNewRecord();
        }
    }
    /**
     * Show a dialog notifying the user that they must enter a name for the record
     */
    private void showInspectorMustBeEntered() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.new_record_please_enter_inpsector))
                .setMessage(getString(R.string.new_record_please_enter_inspector_message))
                .setPositiveButton(getString(android.R.string.ok), null)
                .show();
    }
    /**
     * Creates a new record with the specified name
     *
     * @param recordName
     */
    private void createRecord(final String recordName) {
        if (RecordUtil.sharedInstance().createNewRecord(recordName)) {
            LogUtil.sharedInstance().createLog();
        } else {
            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.create_record_error_title))
                    .setMessage(String.format(getString(R.string.create_record_error_message), "Unknown"))
                    .setPositiveButton(getString(android.R.string.ok), null)
                    .show();
        }
    }
    /**
     * Action when a user clicks on the previous records button
     */
    private void onPreviousRecordsClicked() {
//        Toast.makeText(mContext, "Succesfull satellite fix!",
//                Toast.LENGTH_SHORT).show();
        if (mListener != null) {
            mListener.onPreviousRecords();
        }
    }
    /**
     * init the app version text box
     */
    private void initAppVersionText() {
        try {
            PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            String version = pInfo.versionName;
            mAppVersion.setText(String.format(getString(R.string.app_version), version));
        } catch (PackageManager.NameNotFoundException nex) {
            mAppVersion.setText("");
        }
    }
    // **********************************************************
    //  Notifications
    // **********************************************************
    /**
     * Notification when bluetooth is connected
     *
     * @param event
     */
    @Subscribe
    public void onBLTConnectedEvent(BLTConnectedNotification event) {
        // Set connection status text to connected
        mContinueRecordButton.setEnabled(false);
        mSubmitRecordButton.setEnabled(false);
        mConnectionStatusTextView.setText(getString(R.string.BTconnected));

    }

    /**
     * Notification when bluetooth is listening
     *
     * @param event
     */
    @Subscribe
    public void onBLTListeningEvent(BLTListeningNotification event) {
        // Set connection status text to connected
        mConnectionStatusTextView.setText(getString(R.string.BTconnecting));
    }

    /**
     * Notification when bluetooth is not connected
     *
     * @param event
     */
    @Subscribe
    public void onBLTNotConnectedEvent(BLTNotConnectedNotification event) {
        updateButtonStates();
        mConnectionStatusTextView.setText(getString(R.string.BTnotConnected));
    }

    /**
     * Event from when Bluetooth controller elects to start recording
     *
     * @param event
     */
    @Subscribe
    public void onStartRecordingEvent(BLTStartRecordingEvent event) {
            onContinueButtonClicked();
    }
    /**
     * Event from when user elects to pause recording
     *
     * @param event
     */
    @Subscribe
    public void onUsbConnectedEvent(UsbConnectedNotification event) {
        // Set connection status text to connected
        mConnectionStatusTextView.setText(getString(R.string.connected));
    }
    /**
     * Event from when user elects to resume recording
     *
     * @param event
     */
    @Subscribe
    public void onUsbDisconnectedEvent(UsbDisconnectedNotification event) {
        // Set connection status text to disconnected
        mConnectionStatusTextView.setText(getString(R.string.not_connected));
    }
    /**
     * instantiate and return an instance of this fragment
     *
     * @return
     */
    public static HomeFragment createInstance() {
        return new HomeFragment();
    }

    /**
     * Returns the display title for this fragment
     *
     * @return
     */
    @Override
    protected String getDisplayTitle() {
        return getString(R.string.home_title);
    }

    /**
     * Returns the layout resource for this fragment
     *
     * @return
     */
    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_home;
    }

    /**
     * Listener interface for the parent activity to implement to communicate with it
     */
    public interface Listener {
        void onNewRecord();
        void onPreviousRecords();
        void onSubmitRecord(final String recordId);
        void onOpenSettings();
    }
}
