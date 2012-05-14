package com.blackice.control.fragments;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import com.blackice.control.ControlActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceFragment;
import android.util.Log;

public class MasturModsSettings extends PreferenceFragment {

    private static final String DOWNLOAD_URL = "http://icemod.us.to/mmsettings/MasturModsSettings.apk";
    private static final String HOME = "com.masturmods.settings.HomeActivity";
    private static final String MASTURMODS_SETTINGS = "com.masturmods.settings";
    private static final String TAG = "MasturMods Settings";

    private static final File SD_CARD = Environment.getExternalStorageDirectory();
    private static final File DOWNLOAD_DIR = new File(SD_CARD, "Download");
    private static final File INSTALL_APP = new File (SD_CARD + "/download/" + "MasturModsSettings.apk");

    private static final int NOT_INSTALLED = 0;
    private static final int DOWNLOAD = 1;
    private static final int INSTALL = 2;

    public static File mZipFile;

    public ProgressDialog pbarDialog;

    PackageInfo mmSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mmSettings = getActivity().getPackageManager().getPackageInfo("com.masturmods.settings", 0);
        } catch (NameNotFoundException i) {
            Log.i(TAG, "MasturMods Settings isn't installed");
        }

        if (mmSettings != null) {
            Intent launch = new Intent(Intent.ACTION_MAIN);
            launch.setClassName(MASTURMODS_SETTINGS, HOME);

            getActivity().startActivity(launch);
        } else {
            mHandler.sendEmptyMessage(NOT_INSTALLED);
        }
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case NOT_INSTALLED:
                post(alertInstall);
                break;
            case DOWNLOAD:
                new DownloadFileAsync().execute(new String[]{DOWNLOAD_URL, mZipFile.getAbsolutePath()});
                break;
            case INSTALL:
                Intent intent = new Intent(Intent.ACTION_VIEW); 
                intent.setDataAndType(Uri.fromFile(INSTALL_APP), "application/vnd.android.package-archive");

                startActivity(intent); 
                break;
            }
        }
    };

    private Runnable alertInstall = new Runnable() {

        @Override
        public void run() {
            new AlertDialog.Builder(getActivity())
            .setCancelable(false)
            .setTitle("MasturMods Settings isn't Installed")
            .setMessage("To use this feature please download and install MasturMods Settings")
            .setPositiveButton("Download and Install", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String fileName = DOWNLOAD_URL.substring(DOWNLOAD_URL.lastIndexOf("/") + 1);
                    mZipFile = new File(DOWNLOAD_DIR, fileName);

                    dialog.dismiss();
                    mHandler.sendEmptyMessage(DOWNLOAD);
                }
            })
            .setNegativeButton("No Thanks", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Intent home = new Intent(getActivity(), ControlActivity.class);
                    home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

                    dialog.dismiss();
                    getActivity().startActivity(home);
                }
            })
            .show();
            return;
        }
    };

    public class DownloadFileAsync extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pbarDialog = new ProgressDialog(getActivity());
            pbarDialog.setTitle("Downloading...");
            pbarDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pbarDialog.setCancelable(false);
            pbarDialog.show();
        }

        @Override
        protected String doInBackground(final String... args) {
            int count;
            final File dir = new File(args[1]).getParentFile();
            if (!dir.isDirectory()) dir.mkdirs();
            try {
                final URL url = new URL(args[0]);
                final URLConnection conexion = url.openConnection();
                conexion.connect();
                final int lenghtOfFile = conexion.getContentLength();
                final InputStream input = new BufferedInputStream(url.openStream());
                final OutputStream output = new FileOutputStream(args[1]);
                final byte data[] = new byte[1024];
                long total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress("" + (int) ((total*100)/lenghtOfFile));
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();
            } catch (Exception e) {
                Log.e(TAG, "Download Failed: " + args[0]);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(final String... progress) {
            pbarDialog.setProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected void onPostExecute(final String unused) {
            mHandler.sendEmptyMessage(INSTALL);
            pbarDialog.dismiss();
        }
    }
}
