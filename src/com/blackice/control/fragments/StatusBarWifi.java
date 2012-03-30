/* package com.blackice.control.fragments;

import net.margaritov.preference.colorpicker.ColorPickerPreference;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.util.Log;

import com.blackice.control.R;
import com.blackice.control.SettingsPreferenceFragment;

public class StatusBarWifi extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    ListPreference mTextStyletyle;
    ColorPickerPreference mColorPicker;
//    CheckBoxPreference mHideSignal;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.prefs_statusbar_wifi);

        mTextStyletyle = (ListPreference) findPreference("wifi_style");
        mTextStyletyle.setOnPreferenceChangeListener(this);
        mTextStyletyle.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUSBAR_WIFI_TEXT,
                0)));

        mColorPicker = (ColorPickerPreference) findPreference("wifi_color");
        mColorPicker.setOnPreferenceChangeListener(this);

//        mHideSignal = (CheckBoxPreference) findPreference("hide_signal");
//        mHideSignal.setChecked(Settings.System.getInt(getActivity()
//                .getContentResolver(), Settings.System.STATUSBAR_HIDE_SIGNAL_BARS,
//                0) != 0);

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
//        if (preference == mHideSignal) {
//            Settings.System.putInt(getActivity().getContentResolver(),
//                    Settings.System.STATUSBAR_HIDE_SIGNAL_BARS,
//                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
//
//            return true;
//        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mTextStyletyle) {

            int val = Integer.parseInt((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_WIFI_TEXT, val);
            return true;

        } else if (preference == mColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);

            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_WIFI_TEXT_COLOR, intHex);

            return true;
        }
        return false;
    }
} */
