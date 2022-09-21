package com.onsite.onsitefaulttracker_v2.activity.previous;


import android.os.Bundle;
import com.onsite.onsitefaulttracker_v2.activity.BaseActivity;

/**
 * Created by hihi on 6/25/2016.
 *
 * The previous records activity,
 * displays a list of previously created records and their details to the user,
 * the user can then resume creating or submitting a record, or delete a record
 */
public class PreviousRecordsActivity extends BaseActivity implements PreviousRecordsFragment.Listener {

    /**
     * create and return an instance of PreviousRecordsActivity.
     *
     * @return
     */
    @Override
    protected PreviousRecordsFragment getDefaultFragment() {
        return PreviousRecordsFragment.createInstance();
    }

    /**
     * Sets up the activity and default fragment.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
    }

    /**
     * Return the back button action bar config for this activity
     *
     * @return
     */
    @Override
    protected BaseActivity.ActionBarConfig getDefaultActionBarConfig() {
        return BaseActivity.ActionBarConfig.Back;
    }
}
