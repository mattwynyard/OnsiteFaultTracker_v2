package com.onsite.onsitefaulttracker_v2.model;

import java.util.Date;

/**
 * Created by hihi on 6/23/2016.
 *
 * Record which contains state information about a record that has been created
 * or is in progress
 */
public class Record {

    // Unique Identifier for the record
    public String recordId;

    // The name given to the record by the user upon creation
    public String recordName;

    // The date this record was created
    public Date creationDate;

    // Number of photos created so far
    public int photoCount;

    public boolean zipped;

    public int photoZippedCount;
    public long zippedSizeKB;
    // The size in kb of the record
    // To be initialized by calculating size of stored files for a record
    public long recordSizeKB;

    // The total size in KB of the record
    public long totalSizeKB;

    public int fileUploadCount;

    public long uploadedSizeKB;

    // The folder name of the record
    public String recordFolderName;

    // The total recording time in milliseconds
    public long totalRecordTime;

}
