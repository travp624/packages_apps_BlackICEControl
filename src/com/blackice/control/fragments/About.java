
package com.blackice.control.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.blackice.control.R;
import com.blackice.control.BlackICEPreferenceFragment;

public class About extends BlackICEPreferenceFragment {

    public static final String TAG = "About";

    Preference mSiteUrl;
    Preference mSourceUrl;
    Preference mReviewUrl;
    Preference mIrcUrl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.prefs_about);
        mSiteUrl = findPreference("aokp_website");
        mSourceUrl = findPreference("aokp_source");
        mReviewUrl = findPreference("aokp_review");
        mIrcUrl = findPreference("aokp_irc");

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mSiteUrl) {
            launchUrl("http://aokp.co/");
        } else if (preference == mSourceUrl) {
            launchUrl("http://github.com/aokp");
	} else if (preference == mReviewUrl) {
            launchUrl("http://gerrit.aokp.co");
        } else if (preference == mIrcUrl) {
            launchUrl("http://webchat.freenode.net/?channels=teamkang");
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void launchUrl(String url) {
        Uri uriUrl = Uri.parse(url);
        Intent donate = new Intent(Intent.ACTION_VIEW, uriUrl);
        getActivity().startActivity(donate);
    }
}
