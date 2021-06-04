package com.onsite.onsitefaulttracker_v2.util;

import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Compressor {
    private static final int BUFFER = 2048;

    private String[] _files;
    private String _zipFile;
    private CompressorListener mCompressorListener;

    /**
     * Interface for communicating with the parent fragment/activity
     */
    public interface CompressorListener {
        void dataRead(long _bytes);
        void zipCount (int count);
    }

    /**
     * Sets the dropbox client listener
     *
     * @param listener
     */
    public void setCompressorListener(final CompressorListener listener) {
        mCompressorListener = listener;
    }

    public Compressor(String[] files, String zipFile) {
        _files = files;
        _zipFile = zipFile;
    }

    public void zip() {
        long _size;
        int zipped = 0;
        try  {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(_zipFile);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
            byte data[] = new byte[BUFFER];

            for(int i = 0; i < _files.length; i++) {
                Log.v("Compress", "Adding: " + _files[i]);
                _size = new File(_files[i]).length();
                //totalBytes += _size;
                FileInputStream fi = new FileInputStream(_files[i]);
                mCompressorListener.dataRead(_size);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(_files[i].substring(_files[i].lastIndexOf("/") + 1));
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                mCompressorListener.zipCount(++zipped);
                origin.close();
            }
            //Log.d("Compress", "Bytes: " + totalBytes + "B");
            out.close();
        } catch(Exception e) {
            e.printStackTrace();
        }

    }

}