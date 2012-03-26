package com.roman.romcontrol.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import com.roman.romcontrol.R;
import com.roman.romcontrol.SettingsPreferenceFragment;

public class MasturModsSettings extends SettingsPreferenceFragment {
    public static final String TAG = "MasturModsSettings";

    private Handler mDownloaderHandler;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	mDownloaderHandler = new Handler();

        try {
        	getPackageManager().getApplicationInfo("com.masturmods.settings", 0 );
        } catch (NameNotFoundException e) {
        	mDownloaderHandler.post(downloader);
        }
        
        addPreferencesFromResource(R.xml.prefs_mmsettings);
    }
    
    private Runnable downloader = new Runnable() {
		public void run() {
			
			new AlertDialog.Builder(getActivity())
			.setIcon(R.mipmap.ic_launcher)
			.setTitle("MasturMods Settings")
			.setMessage("To use this extension please download and install the latest version of MasturMods Settings.\n\nDownload newest version now?")
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Intent download = new Intent(Intent.ACTION_VIEW, Uri.parse("http://th3oryrom.us.to/mastur/mmsettings/MasturModsSettings.apk"));
					startActivity(download);
				}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					finish();
				}
			})
			.show();
		}
	};
}
