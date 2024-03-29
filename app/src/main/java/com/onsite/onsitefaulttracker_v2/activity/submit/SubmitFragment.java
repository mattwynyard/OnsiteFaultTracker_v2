package com.onsite.onsitefaulttracker_v2.activity.submit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.onsite.onsitefaulttracker_v2.R;
import com.onsite.onsitefaulttracker_v2.activity.BaseFragment;
import com.onsite.onsitefaulttracker_v2.model.Record;
import com.onsite.onsitefaulttracker_v2.util.CalculationUtil;
import com.onsite.onsitefaulttracker_v2.util.Compressor;
import com.onsite.onsitefaulttracker_v2.util.RecordUtil;
import com.onsite.onsitefaulttracker_v2.util.SettingsUtil;
import com.onsite.onsitefaulttracker_v2.util.ThreadUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by hihi on 6/25/2016.
 *
 * The Submit Fragment, The default fragment of submit Activity.
 * Here the user can submit their record and upload it to drop box
 */
public class SubmitFragment extends BaseFragment implements Compressor.CompressorListener {

    // The tag name for this fragment
    private static final String TAG = SubmitFragment.class.getSimpleName();

    // Id of the record to submit
    public static final String ARG_RECORD_ID = "record_id";

    // The format of output record names when converted from a date
    private static final String OUT_RECORD_DATE_FORMAT = "yy_MM_dd";

    // The name text view
    private TextView mNameTextView;

    // The date text view
    private TextView mDateTextView;

    // The total size of the record
    private TextView mTotalSizeTextView;

    // The remaining size
    private TextView mRemainingTextView;

    // The percentage text view
    private TextView mPercentageTextView;

    // The text view that says Record Submitted, initially hidden, displayed on
    // completion
    private TextView mRecordSubmittedTextView;

    // Image view which displays the currently uploading image
    private ImageView mCurrentImageView;

    // The progress indicator which shows the user that the record is being submitted
    private ProgressBar mSubmittingProgressBar;

    // The submit button
    private Button mSubmitButton;

    // The id of the record that is being submitted
    private String mRecordId;

    // Record that is to be submitted
    private Record mRecord;

    // Array of record files to be uploaded
    private File[] mRecordFiles;

    // Array of record files to be uploaded
    private String[] mFileNames;

    // Is this fragment currently resumed?
    boolean mResumed;

    // Is it currently submitting the record
    boolean mSubmitting;

    // Index of the next image to display
    int mDisplayImageIndex;

    private long totalBytesCompressed = 0;

    private long totalSizeKB = 0;
    private long totalBytes = 0;
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
            Bundle args = getArguments();
            if (args != null) {
                mRecordId = args.getString(ARG_RECORD_ID);
            }
            mDisplayImageIndex = 0;
            mNameTextView = (TextView) view.findViewById(R.id.record_name_text_view);
            mDateTextView = (TextView) view.findViewById(R.id.record_creation_date_text_view);
            mTotalSizeTextView = (TextView) view.findViewById(R.id.total_size_text_view);
            mRemainingTextView = (TextView) view.findViewById(R.id.remaining_size_text_view);
            mSubmitButton = (Button) view.findViewById(R.id.submit_record_button);
            mSubmitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSubmitClicked();
                }
            });
            mPercentageTextView = (TextView) view.findViewById(R.id.uploaded_percentage_text_view);
            mCurrentImageView = (ImageView) view.findViewById(R.id.current_image_id);
            mSubmittingProgressBar = (ProgressBar) view.findViewById(R.id.submitting_progress_bar);
            mRecordSubmittedTextView = (TextView) view.findViewById(R.id.record_submitted_text_view);
            mRecord = RecordUtil.sharedInstance().getRecordWithId(mRecordId);
            updateUIValues();
        }
        return view;
    }

    /**
     * Action on attached
     */
    public void onAttach(Context context) {
        super.onAttach(context);
        mResumed = true;
    }

    /**
     * Action on detach
     */
    public void onDetach() {
        super.onDetach();
        mResumed = false;
        mSubmitting = false;
    }

    /**
     * Update the ui with the record details
     */
    private void updateUIValues() {
        //mRecord.totalSizeKB = (totalBytes) /1024;
        mNameTextView.setText(mRecord.recordName);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM yyyy, h:mm a");
        mDateTextView.setText(String.format(getString(R.string.submit_created_date), simpleDateFormat.format(mRecord.creationDate)));
        mTotalSizeTextView.setText(String.format(getString(R.string.submit_total_size), CalculationUtil.sharedInstance().getDisplayValueFromKB(mRecord.totalSizeKB)));
        float percentage = ((float)mRecord.zippedSizeKB / (float)mRecord.totalSizeKB) * 100.0f;
        percentage = Math.min(100.0f, percentage);
        mPercentageTextView.setText(String.format("%.0f%%", percentage));
        // If the submission is already complete show that
        if (mRecord.zipped) {
            mSubmitButton.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Display the next image from the record files.
     */
    private void displayNextImage() {
        if (!mResumed) {
            // Don't attempt to display an image if the fragment is paused.
            return;
        }
        if (mCurrentImageView.getVisibility() != View.VISIBLE) {
            mCurrentImageView.setVisibility(View.VISIBLE);
        }
        mDisplayImageIndex++;
        if (mDisplayImageIndex >= mRecordFiles.length)
        {
            mDisplayImageIndex = 0;
            return;
        }

        final Bitmap uploadingBitmap = mRecordFiles[mDisplayImageIndex].getName().contains(".jpg") ?
                BitmapFactory.decodeFile(mRecordFiles[mDisplayImageIndex].getAbsolutePath()) : null;
        mCurrentImageView.setImageBitmap(uploadingBitmap);
    }

    /**
     * Action when submitting a record has completed
     */
    private void onSubmissionComplete() {
        mRemainingTextView.setText(String.format(getString(R.string.submit_remaining_size),
                CalculationUtil.sharedInstance().getDisplayValueFromKB(Math.max(0, 0))));
        mPercentageTextView.setVisibility(View.INVISIBLE);
        mRecordSubmittedTextView.setVisibility(View.VISIBLE);
        mSubmittingProgressBar.setVisibility(View.INVISIBLE);
        mSubmitButton.setVisibility(View.INVISIBLE);
        mRecord.zipped = true;
        RecordUtil.sharedInstance().saveRecord(mRecord);
    }

    private String setOutPath() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(OUT_RECORD_DATE_FORMAT);
        final String dateString = dateFormat.format(mRecord.creationDate);
        String outPath = RecordUtil.sharedInstance().getBaseFolder()
                .getAbsolutePath() + "/" + SettingsUtil.sharedInstance().getInspectorId() + "_" + dateString + ".zip";
        File f = new File(outPath);
        if(f.exists()) {
            System.out.println("File exits");
            Date currentTime = Calendar.getInstance().getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("HH-mm-ss");
            String time = sdf.format((currentTime));
            outPath = RecordUtil.sharedInstance().getBaseFolder()
                    .getAbsolutePath() + "/" + SettingsUtil.sharedInstance().getInspectorId() + "_" + dateString + "_" + time + ".zip";
        }
        return outPath;
    }

    /**
     * Action when the user clicks on submit,  initiate the dropbox api
     * and start uploading the record
     */
    private void onSubmitClicked() {
        mSubmitting = true;
        mRecordFiles = RecordUtil.sharedInstance().getRecordFiles(mRecord.recordId);
        if (mRecordFiles == null) {
            return;
        }
        final String fileNames[] = new String[mRecordFiles.length];
        for (int i = 0; i < mRecordFiles.length; i++) {
            totalBytes += mRecordFiles[i].length();
            fileNames[i] = mRecordFiles[i].getAbsolutePath();
        }
        updateUIValues();
        final Compressor compressor = new Compressor(fileNames, setOutPath());
        compressor.setCompressorListener(this);
        mSubmittingProgressBar.setVisibility(View.VISIBLE);
        mSubmitButton.setEnabled(false);
        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {
                compressor.zip();
                ThreadUtil.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        mSubmitting = false;
                        onSubmissionComplete();
                    }
                });
            }
        });

        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {
                while (mSubmitting)
                {
                    ThreadUtil.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            long remainingKB = (totalBytes / 1024)
                                    - (totalBytesCompressed / 1024);
                            mRemainingTextView.setText(String.format(getString(R.string.submit_remaining_size),
                                    CalculationUtil.sharedInstance().getDisplayValueFromKB(Math.max(remainingKB, 0))));
                            displayNextImage();
                        }
                    });
                    try
                    {
                        Thread.sleep(50);
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                }
                ThreadUtil.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        mCurrentImageView.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
    }

    /**
     * Override and handle on back action return true if consumed the event
     * (if the parent activity should not close as is its normal coarse of action) otherwise
     * false
     *
     * @return true if activity should handle the back action, otherwise false
     */
    public boolean onBackClicked() {
        if (mSubmitting) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.submitting_back_dialog_title))
                    .setMessage(getString(R.string.submitting_back_dialog_message))
                    .setPositiveButton(getString(R.string.submitting_back_dialog_exit), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mSubmitting = false;
                            getActivity().onBackPressed();
                        }
                    })
                    .setNegativeButton(getString(R.string.submitting_back_dialog_cancel), null)
                    .show();
            return true;
        } else {
            return false;
        }
    }

    /**
     * instantiate and return an instance of this fragment
     *
     * @return
     */
    public static SubmitFragment createInstance(final String recordId) {
        final SubmitFragment submitFragment = new SubmitFragment();

        Bundle args = new Bundle();
        args.putString(ARG_RECORD_ID, recordId);
        submitFragment.setArguments(args);

        return submitFragment;
    }

    @Override
    public void dataRead(long buffer) {
        //Log.i(TAG, "Buffer: " + buffer);
            totalBytesCompressed += buffer;
            //Log.i("Bytes: ", String.valueOf(totalBytes));
            //Log.i("Compressed Bytes: ", String.valueOf(totalBytesCompressed));
    }

    @Override
    public void zipCount(int count) {
        Log.i("count: ", String.valueOf(count));
        mRecord.photoZippedCount = count;
    }

    /**
     * Returns the display title for this fragment
     *
     * @return
     */
    @Override
    protected String getDisplayTitle() {
        return getString(R.string.zip_record);
    }

    /**
     * Returns the layout resource for this fragment
     *
     * @return
     */
    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_submit;
    }

}
