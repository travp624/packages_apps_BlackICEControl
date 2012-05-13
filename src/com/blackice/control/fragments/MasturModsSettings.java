package com.blackice.control.fragments;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.util.ByteArrayBuffer;

import com.blackice.control.R;

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
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.util.Log;

public class MasturModsSettings extends PreferenceFragment implements OnPreferenceClickListener {
	private static final String TAG = "MMS";
	private static final String updateURL = "http://icemod.us.to/mmsettings/MasturModsSettings.apk";
	private static final File SD_CARD = Environment.getExternalStorageDirectory();
	private static final File DOWNLOAD_DIR = new File(SD_CARD, "Download");
	private static final File MASTURMODS_SETTINGS = new File (SD_CARD + "/download/" + "MasturModsSettings.apk");
	private static final String DOWNLOAD = "download_btn";
	private static final String UPDATE = "update";
	private static final int HANDLE_UPDATE = 0;
	private static final int UPDATE_CLEAN = 1;
	private static final int UPDATE_FOUND = 2;
	private static final int NO_UPDATE = 3;
	private static final int INSTALL_UPDATE = 4;
	
	public static File mZipFile;

	public ProgressDialog pbarDialog;

	PreferenceCategory stat;
	PreferenceCategory nav;
	Preference mDownload;
	Preference mUpdate;

	PackageInfo mmSettings;

	Boolean mms = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs_mmsettings);

		try {
			mmSettings = getActivity().getPackageManager().getPackageInfo("com.masturmods.settings", 0);
		} catch (NameNotFoundException i) {
			Log.i(TAG, "MasturMods Settings isn't installed");
		}

		nav = (PreferenceCategory) findPreference("nav");
    	stat = (PreferenceCategory) findPreference("stat");

		mDownload = (Preference) findPreference(DOWNLOAD);
		mUpdate = (Preference) findPreference(UPDATE);

		if (mmSettings == null) {
			mms = false;
			getPreferenceScreen().removePreference(mUpdate);
        	getPreferenceScreen().removePreference(nav);
        	getPreferenceScreen().removePreference(stat);
			mDownload.setOnPreferenceClickListener(MasturModsSettings.this);
		} else {
			mms = true;
			mUpdate.setOnPreferenceClickListener(MasturModsSettings.this);
			getPreferenceScreen().removePreference(mDownload);
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		mDownload.setEnabled(false);
		mUpdate.setEnabled(false);
		return mHandler.sendEmptyMessage(HANDLE_UPDATE);
	}

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HANDLE_UPDATE:
				if (mms == false) {
					mHandler.sendEmptyMessage(UPDATE_CLEAN);
					break;
				} else {
					checkUpdate.start();
					break;
				}
			case UPDATE_CLEAN:
				if (!MASTURMODS_SETTINGS.exists()) {
					Log.i(TAG, MASTURMODS_SETTINGS + " does not exist");
					mHandler.sendEmptyMessage(UPDATE_FOUND);
					break;
				}
				if (MASTURMODS_SETTINGS.exists()) {
					Log.i(TAG, MASTURMODS_SETTINGS + " exists");
					MASTURMODS_SETTINGS.delete();
					mHandler.sendEmptyMessage(UPDATE_FOUND);
					break;
				}
			case UPDATE_FOUND:
				post(mUpdateFound);
				break;
			case NO_UPDATE:
				post(mCurrent);
				break;
			case INSTALL_UPDATE:
				Intent intent = new Intent(Intent.ACTION_VIEW); 
				intent.setDataAndType(Uri.fromFile(MASTURMODS_SETTINGS), "application/vnd.android.package-archive"); 
				startActivity(intent);  
			    break;
			}
		}
	};

	private Thread checkUpdate = new Thread() {
		public void run() {
			try {
				URL updateURL = new URL("http://icemod.us.to/mmsettings/versioncode.txt");                
				URLConnection conn = updateURL.openConnection(); 
				InputStream is = conn.getInputStream();
				BufferedInputStream bis = new BufferedInputStream(is);
				ByteArrayBuffer baf = new ByteArrayBuffer(50);

				int current = 0;
				while((current = bis.read()) != -1){
					baf.append((byte)current);
				}

				final String s = new String(baf.toByteArray());         

				int curVersion = getActivity().getPackageManager().getPackageInfo("com.masturmods.settings", 0).versionCode;
				int newVersion = Integer.valueOf(s);

				if (newVersion > curVersion) {
					mHandler.sendEmptyMessage(UPDATE_CLEAN);
					stop();
				} else {
					mHandler.sendEmptyMessage(NO_UPDATE);
					stop();
				}
			} catch (Exception e) {
			}
		}
	};

	private Runnable mCurrent = new Runnable() {
		public void run() {

			new AlertDialog.Builder(getActivity())
			.setTitle("No Update Found")
			.setNegativeButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.dismiss();
				}
			})
			.show();
			return;
		}
	};

	private Runnable mUpdateFound = new Runnable() {
		public void run() {

			new AlertDialog.Builder(getActivity())
			.setTitle("A New Version of MasturMods Settings is Available")
			.setPositiveButton("Download Now", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String fileName = updateURL.substring(updateURL.lastIndexOf("/") + 1);
					mZipFile = new File(DOWNLOAD_DIR, fileName);

					new DownloadFileAsync().execute(new String[]{updateURL, mZipFile.getAbsolutePath()});
				}
			})
			.setNegativeButton("Download Later", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.dismiss();
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
			mHandler.sendEmptyMessage(INSTALL_UPDATE);
			pbarDialog.dismiss();
		}
    }
}
