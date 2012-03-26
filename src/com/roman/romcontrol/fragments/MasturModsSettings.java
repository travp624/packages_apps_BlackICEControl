package com.roman.romcontrol.fragments;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.util.ByteArrayBuffer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.roman.romcontrol.R;

public class MasturModsSettings extends PreferenceFragment {
	private Handler mUpdateHandler;
	private Handler mDownloadHandler;
	Preference mDownload;
	Preference mUpdate;

	@Override
    public void onCreate(Bundle savedInstanceState) {
		mUpdateHandler = new Handler();
		mDownloadHandler = new Handler();
		super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.prefs_mmsettings);

        try {
        	getActivity().getPackageManager().getPackageInfo("com.masturmods.settings", 0);
        	Preference dl = findPreference("download_btn");
        	((PreferenceCategory) findPreference("download")).removePreference(dl);
        } catch (NameNotFoundException e) {
        	mDownloadHandler.post(autoDownload);
        	Preference ud = findPreference("update");
        	((PreferenceCategory) findPreference("download")).removePreference(ud);
        	PreferenceCategory nav = (PreferenceCategory) findPreference("nav");
        	PreferenceCategory stat = (PreferenceCategory) findPreference("stat");
        	getPreferenceScreen().removePreference(nav);
        	getPreferenceScreen().removePreference(stat);
        }
        mDownload = findPreference("download_btn");
        mUpdate = findPreference("update");
    }

	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == mDownload) {
        	try {
    			getActivity().getPackageManager().getPackageInfo("com.masturmods.settings", 0);
    		} catch (NameNotFoundException e) {
    			mDownloadHandler.post(download);
    		}
            return true;
        } else if (preference == mUpdate) {
        	// Calling getPackageManager().getPackageInfo(null) will always return NameNotFoundException
        	// So far this has been the only way I could force the preference tree to respond to anything
        	// This will more than likely be removed in the future for more stable coding
        	try {
    			getActivity().getPackageManager().getPackageInfo(null, 0);
    		} catch (NameNotFoundException e) {
    			manualUpdate.start();
    		}
        	return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	private Runnable download = new Runnable() {
		public void run() {

			new AlertDialog.Builder(getActivity())
			.setIcon(R.drawable.ic_rom_control_alert_masturmods)
			.setTitle("Download MasturMods Settings")
			.setMessage("\nMasturMods Settings is Available!\n")
			.setPositiveButton("Download", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Intent update = new Intent(Intent.ACTION_VIEW, Uri.parse("http://th3oryrom.us.to/mastur/mmsettings/MasturModsSettings.apk"));
					startActivity(update);
				}
			})
			.show();
		}
	};

	private Runnable autoDownload = new Runnable() {
		public void run() {
			
			new AlertDialog.Builder(getActivity())
			.setIcon(R.drawable.ic_rom_control_alert_masturmods)
			.setTitle("WARNING!")
			.setMessage("To use this feature, please download and install MasturMods Settings\n\nDownload newest version now?")
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int whichButton) {
					Intent update = new Intent(Intent.ACTION_VIEW, Uri.parse("http://th3oryrom.us.to/mastur/mmsettings/MasturModsSettings.apk"));
					startActivity(update);
				}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			})
			.show();
		}
	};

	private Thread manualUpdate = new Thread() {
		public void run() {
			try {
				URL updateURL = new URL("http://th3oryrom.us.to/mastur/mmsettings/versioncode.txt");                
				URLConnection conn = updateURL.openConnection(); 
				InputStream is = conn.getInputStream();
				BufferedInputStream bis = new BufferedInputStream(is);
				ByteArrayBuffer baf = new ByteArrayBuffer(50);
				PreferenceCategory dl = (PreferenceCategory) findPreference("download");
					getPreferenceScreen().removePreference(dl);

				int current = 0;
				while((current = bis.read()) != -1){
					baf.append((byte)current);
				}

				final String s = new String(baf.toByteArray());         

				int curVersion = getActivity().getPackageManager().getPackageInfo("com.masturmods.settings", 0).versionCode;
				int newVersion = Integer.valueOf(s);

				if (newVersion > curVersion) {
					mUpdateHandler.post(showUpdate);
				}
				if (curVersion > newVersion) {
					mUpdateHandler.post(showFuture);
				} else {
					mUpdateHandler.post(showCurrent);
				}
			} catch (Exception e) {
			}
		}
	};

	private Runnable showUpdate = new Runnable() {
		public void run() {
			
			new AlertDialog.Builder(getActivity())
			.setIcon(R.drawable.ic_rom_control_alert_masturmods)
			.setTitle("Update Available")
			.setMessage("An update for MasturMods Settings is available!\n\nDownload newest version now?")
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int whichButton) {
					Intent update = new Intent(Intent.ACTION_VIEW, Uri.parse("http://th3oryrom.us.to/mastur/mmsettings/MasturModsSettings.apk"));
					startActivity(update);
				}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			})
			.show();
		}
	};

	private Runnable showFuture = new Runnable() {
		public void run() {
			
			new AlertDialog.Builder(getActivity())
			.setIcon(R.drawable.ic_rom_control_alert_masturmods)
			.setTitle("WTF?!?!")
			.setMessage("Hello time traveler!\n\nCare to send me the source I'm about to make?")
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int whichButton) {
					final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
					emailIntent.setType("message/rfc822");
		   			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { "masturmynd@gmail.com" });
		   			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Mastur Mods Settings");
		   			emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "");
		   			startActivity(emailIntent);
				}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			})
			.show();
		}
	};   

	private Runnable showCurrent = new Runnable() {
		public void run() {
			
			new AlertDialog.Builder(getActivity())
			.setIcon(R.drawable.ic_rom_control_alert_masturmods)
			.setTitle("Update?? What Update??")
			.setMessage("\nLooks like you're up to date!\n")
			.setNegativeButton("OK", new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			})
			.show();
		}
	};
}
