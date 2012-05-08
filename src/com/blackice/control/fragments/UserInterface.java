
package com.blackice.control.fragments;

import java.io.File;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.Spannable;
import android.util.Log;
import android.widget.EditText;

import com.blackice.control.R;
import com.blackice.control.BlackICEPreferenceFragment;
import com.blackice.control.util.CMDProcessor;
import com.blackice.control.util.Helpers;

public class UserInterface extends BlackICEPreferenceFragment implements
        OnPreferenceChangeListener {

    public static final String TAG = "UserInterface";

    private static final String PREF_LED_SCREEN_ON = "led_screen_on";
    private static final String PREF_CRT_ON = "crt_on";
    private static final String PREF_CRT_OFF = "crt_off";
    private static final String PREF_IME_SWITCHER = "ime_switcher";
    private static final String PREF_ENABLE_VOLUME_OPTIONS = "enable_volume_options";
    private static final String PREF_LONGPRESS_TO_KILL = "longpress_to_kill";
    private static final String PREF_ROTATION_ANIMATION = "rotation_animation_delay";
    private static final String PREF_180 = "rotate_180";
    private static final String PREF_HOME_LONGPRESS = "long_press_home";
    private static final String PREF_RECENT_APP_SWITCHER = "recent_app_switcher";

    CheckBoxPreference mCrtOnAnimation;
    CheckBoxPreference mCrtOffAnimation;
    CheckBoxPreference mShowImeSwitcher;
    CheckBoxPreference mEnableVolumeOptions;
    CheckBoxPreference mLongPressToKill;
    CheckBoxPreference mAllow180Rotation;
    ListPreference mAnimationRotationDelay;
    ListPreference mHomeLongpress;
    ListPreference mRecentAppSwitcher;
    Preference mLcdDensity;
    CheckBoxPreference mDisableBootAnimation;
    CheckBoxPreference mDisableBugMailer;
    CheckBoxPreference mLedScreenOn;

    int newDensityValue;

    DensityChanger densityFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.prefs_ui);

        PreferenceScreen prefs = getPreferenceScreen();

        mCrtOffAnimation = (CheckBoxPreference) findPreference(PREF_CRT_OFF);
        mCrtOffAnimation.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.CRT_OFF_ANIMATION, 1) == 1);

        mCrtOnAnimation = (CheckBoxPreference) findPreference(PREF_CRT_ON);
        mCrtOnAnimation.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.CRT_ON_ANIMATION, 0) == 1);

        mShowImeSwitcher = (CheckBoxPreference) findPreference(PREF_IME_SWITCHER);
        mShowImeSwitcher.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SHOW_STATUSBAR_IME_SWITCHER, 1) == 1);

        mEnableVolumeOptions = (CheckBoxPreference) findPreference(PREF_ENABLE_VOLUME_OPTIONS);
        mEnableVolumeOptions.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.ENABLE_VOLUME_OPTIONS, 0) == 1);

        mLongPressToKill = (CheckBoxPreference) findPreference(PREF_LONGPRESS_TO_KILL);
        mLongPressToKill.setChecked(Settings.Secure.getInt(getActivity().getContentResolver(),
                Settings.Secure.KILL_APP_LONGPRESS_BACK, 0) == 1);

        mAnimationRotationDelay = (ListPreference) findPreference(PREF_ROTATION_ANIMATION);
        mAnimationRotationDelay.setOnPreferenceChangeListener(this);
        mAnimationRotationDelay.setValue(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.ACCELEROMETER_ROTATION_SETTLE_TIME,
                200) + "");

        mAllow180Rotation = (CheckBoxPreference) findPreference(PREF_180);
        mAllow180Rotation.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION_ANGLES, (1 | 2 | 8)) == (1 | 2 | 4 | 8));

        mRecentAppSwitcher = (ListPreference) findPreference(PREF_RECENT_APP_SWITCHER);
        mRecentAppSwitcher.setOnPreferenceChangeListener(this);
        mRecentAppSwitcher.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.RECENT_APP_SWITCHER,
                0)));

        mLedScreenOn = (CheckBoxPreference) findPreference(PREF_LED_SCREEN_ON);
        mLedScreenOn.setChecked(Settings.Secure.getInt(getActivity().getContentResolver(),
                Settings.Secure.LED_SCREEN_ON, 0) == 1);

        mLcdDensity = findPreference("lcd_density_setup");
        String currentProperty = SystemProperties.get("ro.sf.lcd_density");
        try {
            newDensityValue = Integer.parseInt(currentProperty);
        } catch (Exception e) {
            getPreferenceScreen().removePreference(mLcdDensity);
        }

        mLcdDensity.setSummary("Current LCD Density: " + currentProperty);

        mDisableBootAnimation = (CheckBoxPreference) findPreference("disable_bootanimation");
        mDisableBootAnimation.setChecked(!new File("/system/media/bootanimation.zip").exists());
        if (mDisableBootAnimation.isChecked())
            mDisableBootAnimation.setSummary(R.string.disable_bootanimation_summary);

        mDisableBugMailer = (CheckBoxPreference) findPreference("disable_bugmailer");
        mDisableBugMailer.setChecked(!new File("/system/bin/bugmailer.sh").exists());

        mHomeLongpress = (ListPreference) findPreference(PREF_HOME_LONGPRESS);
        mHomeLongpress.setOnPreferenceChangeListener(this);
        mHomeLongpress.setValue(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.NAVIGATION_BAR_HOME_LONGPRESS,
                0) + "");

        if (!getResources().getBoolean(com.android.internal.R.bool.config_enableCrtAnimations)) {
            prefs.removePreference((PreferenceGroup) findPreference("crt"));
        } else {
            // can't get this working in ICS just yet
            ((PreferenceGroup) findPreference("crt")).removePreference(mCrtOnAnimation);
        }

        if (!hasHardwareButtons) {
            ((PreferenceGroup) findPreference("misc")).removePreference(mLongPressToKill);
            ((PreferenceGroup) findPreference("misc")).removePreference(mHomeLongpress);
        }

        if (mTablet) {
            ((PreferenceGroup) findPreference("misc")).removePreference(mHomeLongpress);
        }
        if (!hasNotificationLed) {
            ((PreferenceGroup) findPreference("misc")).removePreference(mLedScreenOn);
        }

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mCrtOffAnimation) {

            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.CRT_OFF_ANIMATION, checked ? 1 : 0);
            return true;

        } else if (preference == mCrtOnAnimation) {

            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.CRT_ON_ANIMATION, checked ? 1 : 0);
            return true;
            
        } else if (preference == mShowImeSwitcher) {

            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SHOW_STATUSBAR_IME_SWITCHER, checked ? 1 : 0);
            return true;

        } else if (preference == mEnableVolumeOptions) {

            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ENABLE_VOLUME_OPTIONS, checked ? 1 : 0);
            return true;

        } else if (preference == mLongPressToKill) {

            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.KILL_APP_LONGPRESS_BACK, checked ? 1 : 0);
            return true;

        } else if (preference == mAllow180Rotation) {

            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION_ANGLES, checked ? (1 | 2 | 4 | 8)
                            : (1 | 2 | 8));
            return true;

        } else if (preference == mDisableBootAnimation) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
            if (checked) {
                Helpers.getMount("rw");
                new CMDProcessor().su
                        .runWaitFor("mv /system/media/bootanimation.zip /system/media/bootanimation.blackice");
                Helpers.getMount("ro");
                preference.setSummary(R.string.disable_bootanimation_summary);
            } else {
                Helpers.getMount("rw");
                new CMDProcessor().su
                        .runWaitFor("mv /system/media/bootanimation.blackice /system/media/bootanimation.zip");
                Helpers.getMount("ro");
            }
            return true;

        } else if (preference == mDisableBugMailer) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
            if (checked) {
                Helpers.getMount("rw");
                new CMDProcessor().su
                        .runWaitFor("mv /system/bin/bugmailer.sh /system/bin/bugmailer.sh.blackice");
                Helpers.getMount("ro");
            } else {
                Helpers.getMount("rw");
                new CMDProcessor().su
                        .runWaitFor("mv /system/bin/bugmailer.sh.blackice /system/bin/bugmailer.sh");
                Helpers.getMount("ro");
            }
            return true;
        } else if (preference == mLcdDensity) {
            ((PreferenceActivity) getActivity())
                    .startPreferenceFragment(new DensityChanger(), true);
            return true;
        } else if (preference == mLedScreenOn) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
                    Settings.Secure.putInt(getActivity().getContentResolver(),
                        Settings.Secure.LED_SCREEN_ON, checked ? 1 : 0);
           Log.i(TAG, "LED flash when screen ON is set to: " + checked);
           return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mAnimationRotationDelay) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION_SETTLE_TIME,
                    Integer.parseInt((String) newValue));

            return true;
        } else if (preference == mHomeLongpress) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_HOME_LONGPRESS,
                    Integer.parseInt((String) newValue));
            return true;
        } else if (preference == mRecentAppSwitcher) {
            int val = Integer.parseInt((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.RECENT_APP_SWITCHER, val);
            Helpers.restartSystemUI();
            return true;
        }
        return false;
    }

    public static void addButton(Context context, String key) {
        ArrayList<String> enabledToggles = Navbar
                .getButtonsStringArray(context);
        enabledToggles.add(key);
        Navbar.setButtonsFromStringArray(context, enabledToggles);
    }

    public static void removeButton(Context context, String key) {
        ArrayList<String> enabledToggles = Navbar
                .getButtonsStringArray(context);
        enabledToggles.remove(key);
        Navbar.setButtonsFromStringArray(context, enabledToggles);
    }

}
