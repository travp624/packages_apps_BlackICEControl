/*
 * Copyright (C) 2012 The LiquidSmoothROMs Project
 * author JBirdVegas@gmail.com 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blackice.control.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.blackice.control.R;
import com.blackice.control.BlackICEPreferenceFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ClassCastException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

public class BackupRestore extends BlackICEPreferenceFragment {

    private static final String TAG = "BackupRestore";
    private static final boolean DEBUG = true;
    private static final boolean CLASS_DEBUG = false;
    private static final String BLANK = "";
    private static final String BACKUP_PREF = "backup";
    private static final String RESTORE_PREF = "restore";
    private static final String THEME_CAT_PREF = "theme_cat";
    private static String CONFIG_FILENAME = null;
    private static final File fSDCARD = Environment.getExternalStorageDirectory();
    private static final String SDCARD = fSDCARD.getAbsolutePath();
    private static String CONFIG_CHECK_PASS = SDCARD + "/BlackICEControl/%s has been created";
    private static final String PATH_TO_CONFIGS = SDCARD + "/BlackICEControl";
    private static final String PATH_TO_THEMES = SDCARD + "/BlackICEControl/themes";
    private static boolean success = false;
    private final String OPEN_FILENAME = "open_filepath";
    private final String SAVE_FILENAME = "save_filepath";
    private static final String MESSAAGE_TO_HEAD_FILE = "~XXX~ BE CAREFUL EDITING BY HAND ~XXX~ you have been warned!";
    private static final String TEXT_IS_EMPTY = "Theme Name Required";
    private static String makeThemFeelAtHome = null;
    private static int DEFAULT_FLING_SPEED = 65;
    private static final String DELIMITER = "+";
    private static final String SPILT_DELIMITER = "\\+";
    private static String RETURN = "\n";
    private static String LINE_SPACE = "\n\n";
    private static String TWO_LINE_SPACE = "\n\n\n";

    // Dialogs
    private static final int THEME_INFO_DIALOG = 100;
    private static final int SAVE_CONFIG_DIALOG = 101;

    // to hold our lists
    String[] array;
    ArrayList<String> settingsArray = new ArrayList<String>();
    ArrayList<String[]> arrayOfStrings = new ArrayList<String[]>();

    PreferenceScreen prefs;
    PreferenceScreen mBackup;
    PreferenceScreen mRestore;
    PreferenceCategory mThemeCat;

    Properties mProperties = new Properties();

    @Override
    public void onCreate(Bundle didOrientationChange) {
        super.onCreate(didOrientationChange);

        addPreferencesFromResource(R.xml.prefs_backup_restore);
        prefs = getPreferenceScreen();
        mBackup = (PreferenceScreen) prefs.findPreference(BACKUP_PREF);
        mRestore = (PreferenceScreen) prefs.findPreference(RESTORE_PREF);

        // gain reference to theme category so we can drop our prefs if not found
        mThemeCat = (PreferenceCategory) prefs.findPreference(THEME_CAT_PREF);

        setupArrays();
        // TODO add themes dir to mkdirs
        // make required dirs and disable themes if unavailable
        // be sure we have the directories we need or everything fails
        File makeDirs = new File(PATH_TO_CONFIGS);
        File themersDirs = new File(PATH_TO_THEMES);

        if (!makeDirs.exists()) {
            if (!makeDirs.mkdirs()) {
                Log.d(TAG, "failed to create the required directories");
            }
        }

        if (!themersDirs.exists()) {
            if (!themersDirs.mkdirs()) {
                Log.d(TAG, "failed to create theme directory");
            }
        }

        // for that personal touch
        makeThemFeelAtHome = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.CUSTOM_CARRIER_LABEL);

        // setup initial themes view
        findThemes();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void findThemes() {
        // themes live in the theme category while the theme category lives on the PreferenceScreen

        // start with a clean slate
        mThemeCat.removeAll();
        prefs.removePreference(mThemeCat);

        File themerDirs = new File(PATH_TO_THEMES);

        // verify we have the file structure
        if (!themerDirs.exists()) {
            // try to make the path; if we fail drop the method instead of the app
            if(!themerDirs.mkdirs()) return;
        }

        String[] allThemesFound = themerDirs.list();
        try {
            if (DEBUG) Log.d(TAG, themerDirs.list().toString());
        } catch (NullPointerException npe) {
            return;
        }

        // so Themes category won't show when we don't have themes
        boolean doWeHaveThemes = false;
        for (String findThemes : themerDirs.list()) {
            File isDirOrTheme = new File(PATH_TO_THEMES, findThemes);
            if (isDirOrTheme.isFile()) doWeHaveThemes = true;
        }

        if (doWeHaveThemes) {
            doWeHaveThemes = false;
            prefs.addPreference(mThemeCat);
            for (final String theme_ : allThemesFound) {
                // don't try to load directories as themes
                File tf = new File(PATH_TO_THEMES, theme_);
                if (!tf.isDirectory()) {
                    try {
                        File themeFile = new File(PATH_TO_THEMES, theme_);
                        PreferenceScreen newTheme = getPreferenceManager().createPreferenceScreen(mContext);
                        // use namespace for key
                        newTheme.setKey(theme_);
                        FileReader fReader = new FileReader(themeFile);
                        Properties mThemeProps = new Properties();
                        mThemeProps.load(fReader);
                        // look for some strings to set title and summary in config file
                        String returnedTitle = ((String) mThemeProps.get("title"));
                        String returnedSummary = ((String) mThemeProps.get("summary"));
                        if (returnedTitle != null) newTheme.setTitle(returnedTitle);
                        // use the filename is we have nothing else
                        else newTheme.setTitle(theme_);

                        if (returnedSummary != null) newTheme.setSummary(returnedSummary);
                        else newTheme.setSummary(BLANK);

                        newTheme.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                                @Override
                                public boolean onPreferenceClick(Preference newTheme) {
                                    wantToDeleteOrApply(theme_, true);
                                    return true;
                                }
                        });

                        // now we have all the info lets add our new preference to the screen
                        mThemeCat.addPreference(newTheme);
                    } catch (NullPointerException npe){
                        // theme file was not found this shouldn't happen but just in case
                        npe.printStackTrace();
                    } catch (FileNotFoundException noFile) {
                        if (DEBUG) noFile.printStackTrace();
                    } catch (IOException io) {
                        if (DEBUG) io.printStackTrace();
                    }
                }
            }
        }
    }

    private void wantToDeleteOrApply(final String filename, final boolean thisATheme) {
        try {
            // TODO fix so we don't need this path absolut we should inhearit from filename
            final File dialogFile;
            if (thisATheme) {
                dialogFile = new File(PATH_TO_THEMES, filename);
            } else {
                dialogFile = new File(filename);
            }
            FileReader fr = new FileReader(dialogFile);
            Properties dialogProps = new Properties();
            dialogProps.load(fr);

            AlertDialog.Builder shouldDelete = new AlertDialog.Builder(getActivity());
            shouldDelete.setTitle(getString(R.string.delete_or_apply));
            // complex but looks good TODO would be to simplify this setMessage()
            shouldDelete.setMessage(String.format("Theme title: %s", ((String) dialogProps.get("title")))
                    + RETURN + ((String) dialogProps.get("summary")));
            shouldDelete.setPositiveButton(getString(R.string.apply_button), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    restore(filename, thisATheme);
                }
            });
            shouldDelete.setNegativeButton(getString(R.string.delete_button), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialogFile.delete();
                    findThemes();
                }
            });
            shouldDelete.show();
        } catch (FileNotFoundException fnfe) {
            // we should have already coverd this
        } catch (IOException io) {
            // ditto
        }
    }

    private boolean runBackup(final String bkname, final String title_text, final String summary_text) {
        // use async handler to avoid performace problems
        final Handler mBackupHandler = new Handler();
        final Runnable mRunBackupThread = new Runnable() {
            public void run() {
                if (DEBUG) Log.d(TAG, "runBackup has been called: " + bkname);
                String string_setting = null;
                int int_setting;
                float float_setting;

                int foundStrings = 0;
                int foundInts = 0;
                int foundFloats = 0;

                // use army of clones so we don't waste time reading files
                ArrayList<String> stringArray = new ArrayList<String>(settingsArray);
                ArrayList<String> floatArray = new ArrayList<String>(settingsArray);

                // so we can provide more info to the users about the backup
                ArrayList<String> handledSettingsArray = new ArrayList<String>();
                ArrayList<String> handledValuesArray = new ArrayList<String>();
                ArrayList<String> unhandledSettingsArray = new ArrayList<String>(settingsArray);

                // get a view to work with
                LayoutInflater infoInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View backupDialogLayout = infoInflater.inflate(R.layout.what_happened_dialog, null);

                // setup smooth scrolling within our info dialog
                ScrollView scroll = (ScrollView) backupDialogLayout.findViewById(R.id.what_happened_scrollview);
                scroll.setSmoothScrollingEnabled(true);
                scroll.fling(DEFAULT_FLING_SPEED);

                // get final reference so we can minuplate text in try blocks
                final TextView mShowInfo = (TextView) scroll.findViewById(R.id.what_happened_more_info);

                // structure a super simple dialog for our output
                AlertDialog.Builder whatHappened = new AlertDialog.Builder(getActivity());
                whatHappened.setTitle(getString(R.string.what_happened_title));
                whatHappened.setView(backupDialogLayout);
                whatHappened.setPositiveButton(getString(R.string.positive_thanks_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // user can't click here till we are done with work
                        // once user can click just dismiss we are done
                    }
                });

                // don't let the user stop this dialog till we are done running the backup
                whatHappened.setCancelable(false);

                // load builder into an AlertDialog so we can get the positive button
                AlertDialog ad = whatHappened.create();
                ad.show();

                // Issue 6360 prevents us from referencing buttons till after we show dialog
                // more info see --> http://code.google.com/p/android/issues/detail?id=6360
                final Button mOkButton = (Button) ad.getButton(AlertDialog.BUTTON_POSITIVE);
                mOkButton.setEnabled(false);

                StringBuilder info = new StringBuilder(settingsArray.size());
                final String formater = "Found value: %s=%s";
                final String noValue = "Unresolved value: %s";

                if (title_text != null) {
                    mProperties.setProperty("title", title_text);
                    info.append(String.format("Theme present named: %s", title_text) + RETURN);
                    mShowInfo.setText(info.toString());
                }
                if (summary_text != null) {
                    mProperties.setProperty("summary", summary_text);
                    info.append(String.format("Summary provided: %s", summary_text) + RETURN);
                    mShowInfo.setText(info.toString());
                }

                try {
                    // handle String[]'s first because they are on a seperate array
                    for (final String[] stringsInArray : arrayOfStrings) {
                        int length = stringsInArray.length;
                        if (DEBUG) Log.d(TAG, "stringsInArray length=" + length);
                        for (int i = 0; length > i; i++) {
                            String propValue = Settings.System.getString(getActivity().getContentResolver(), stringsInArray[i]);
                            if (propValue != null) {
                                info.append(String.format(formater, stringsInArray[i],  propValue) + RETURN);
                                mProperties.setProperty(stringsInArray[i], propValue);
                                mShowInfo.setText(info.toString());

                                // tracking
                                handledSettingsArray.add(stringsInArray[i]);
                                handledValuesArray.add(propValue);
                            } else {
                                unhandledSettingsArray.add(stringsInArray[i]);
                            }
                        }
                    }
                } catch (Exception e) {
                      if (DEBUG) e.printStackTrace();
                }

                // handle floats second and remove the handled values other arrays
                for (final String liquid_float_setting : floatArray) {
                    // only alpha is kept as a float so don't bother with the rest
                    if (liquid_float_setting.contains("alpha")) {
                        try {
                            float float_ = Settings.System.getFloat(getActivity().getContentResolver(), liquid_float_setting);
                            mProperties.setProperty(liquid_float_setting, String.format("%f", float_));
                            if (DEBUG) Log.d(TAG, "floats:  {" + liquid_float_setting + "} returned value {" + float_ + "}");
                            stringArray.remove(liquid_float_setting);
                            foundFloats = foundFloats + 1;

                            // tracking
                            handledSettingsArray.add(liquid_float_setting);
                            handledValuesArray.add(String.format("%f", float_));
                            info.append(String.format(formater, liquid_float_setting, float_) + RETURN);
                            mShowInfo.setText(info.toString());
                        } catch (SettingNotFoundException notFound) {
                            // we didn't find so add to out list of unhandledSettingsArray
                            unhandledSettingsArray.add(liquid_float_setting);
                            if (DEBUG) Log.d(TAG, String.format("should add this value? is floats unreliable reference point: %s",
                                    liquid_float_setting));
                            info.append(String.format(noValue, liquid_float_setting) + RETURN);
                            mShowInfo.setText(info.toString());
                            if (CLASS_DEBUG) notFound.printStackTrace();
                        } catch (ClassCastException cce) {
                            if (CLASS_DEBUG) cce.printStackTrace();
                        } catch (NumberFormatException badFloat) {
                            if (CLASS_DEBUG) badFloat.printStackTrace();
                        }
                    }
                }

                // TODO can strings handle everything?!?! ...for saving only of coarse
                for (final String liquid_string_setting : stringArray) {
                    try {
                        string_setting = Settings.System.getString(getActivity().getContentResolver(), liquid_string_setting);
                        try {
                            // it's an int so set it as so
                            int testIsANumber = Integer.valueOf(string_setting);
                            try {
                                testIsANumber = Settings.System.getInt(getActivity().getContentResolver(), liquid_string_setting);
                                mProperties.setProperty(liquid_string_setting, String.format("%d", testIsANumber));
                                foundInts = foundInts + 1;
                                Log.d(TAG, String.format("Ints: {%s} returned value {%s}",
                                        liquid_string_setting, string_setting));

                                // tracking
                                handledSettingsArray.add(liquid_string_setting);
                                handledValuesArray.add(String.format("%d", testIsANumber));
                                info.append(String.format(formater, liquid_string_setting, string_setting) + RETURN);
                                mShowInfo.setText(info.toString());
                            } catch (SettingNotFoundException noSetting) {
                                unhandledSettingsArray.add(liquid_string_setting);
                                // not found
                            }
                        } catch (NumberFormatException ne) {
                            // it's a string not a number
                            if (string_setting != null) {
                                mProperties.setProperty(liquid_string_setting, string_setting);
                                foundStrings = foundStrings + 1;
                                Log.d(TAG, String.format("Strings: {%s} returned value {%s}",
                                        liquid_string_setting, string_setting));

                                // tracking
                                handledSettingsArray.add(liquid_string_setting);
                                handledValuesArray.add(string_setting);
                                info.append(String.format(formater, liquid_string_setting, string_setting) + RETURN);
                                mShowInfo.setText(info.toString());
                            }
                        }
                    } catch (ClassCastException cce) {
                        if (CLASS_DEBUG) cce.printStackTrace();
                    } catch (NullPointerException npe) {
                        npe.printStackTrace();
                    }
                }

                if (DEBUG) {
                    Log.d(TAG, "How many properties were found and handled? Strings: " + foundStrings
                            + "	Ints: " + foundInts + "	Floats: " + foundFloats);
                    Log.d(TAG, "how long are our lists? Strings: " + stringArray.size() + "	Floats: " + floatArray.size());
                }

                // move down 2 lines before summary
                info.append(LINE_SPACE);
                info.append(String.format("We handled %d values of %d values",
                        handledSettingsArray.size(), unhandledSettingsArray.size()) + RETURN);
                mShowInfo.setText(info.toString());

                ArrayList<String> arrayUnhandled = new ArrayList<String>(settingsArray);
                // include String[] in output
                ArrayList<String[]> unhandledArrays = new ArrayList<String[]>(arrayOfStrings);
                for (String[] string_ary : unhandledArrays) {
                    for (int ai = 0; ai < string_ary.length; ai++) arrayUnhandled.add(string_ary[ai]);
                }

                info.append(LINE_SPACE);
                info.append("Properties we didn't find values for:" + RETURN);
                // remove what we handled from array and display rest
                for (String foundIt : handledSettingsArray) {
                    arrayUnhandled.remove(foundIt);
                }
                for (String notFoundIt : arrayUnhandled) {
                    info.append(notFoundIt.toString() + RETURN);
                    mShowInfo.setText(info.toString());
                    if (DEBUG) Log.d(TAG, "Unresolved Property: " + notFoundIt);
                }
                info.append(LINE_SPACE);

                if (mProperties != null) {
                    try {
                        // TODO fix paths
                        File storeFile = new File(bkname);
                        mProperties.store(new FileOutputStream(storeFile.getAbsolutePath()), MESSAAGE_TO_HEAD_FILE);
                        if (DEBUG) Log.d(TAG, "Does storeFile exist? " + storeFile.exists() + "	AbsolutPath: " + storeFile.getAbsolutePath());
                        success = true;
                        info.append("Saved file: " + bkname + RETURN);
                        mShowInfo.setText(info.toString());
                    } catch (FileNotFoundException fnfe) {
                        fnfe.printStackTrace();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                } else {
                    if (DEBUG) Log.d(TAG, "mProperties was null");
                }

                // Notify user if files were created correctly
                if (checkConfigFiles(bkname)) {
                     Toast.makeText(mContext, String.format(CONFIG_CHECK_PASS,
                            bkname), Toast.LENGTH_SHORT).show();
                    info.append(LINE_SPACE);
                    info.append(String.format("Settings saved!	%s", bkname) + RETURN);
                    info.append(RETURN);
                    mShowInfo.setText(info.toString());
                    //handleStringArrays();
                } else {
                    // TODO this provides no info to help debug THIS IS IMPORTANT it's all the users see ...maybe we give them counts of vars handled also?
                    Toast.makeText(mContext, "We encountered a problem, restore not created",
                            Toast.LENGTH_SHORT).show();
                    info.append(LINE_SPACE);
                    info.append(String.format("$#!+ we couldn't save to %s", bkname) + RETURN);
                    mShowInfo.setText(info.toString());
                }

                // info is up; let user go away
                info.append(LINE_SPACE);
                info.append("Thank you come again!" + RETURN);
                mShowInfo.setText(info.toString());
                mOkButton.setEnabled(true);

                // update the screen
                findThemes();
            }
        };
        return mBackupHandler.post(mRunBackupThread);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen prefScreen,
            Preference pref) {
        if (pref == mBackup) {
            if (DEBUG) Log.d(TAG, "calling backup method");
            showDialog(SAVE_CONFIG_DIALOG);
            return true;
        } else if (pref == mRestore) {
            if (DEBUG) Log.d(TAG, "calling restore method");
            runRestore();
            return true;
        }
        return super.onPreferenceTreeClick(prefScreen, pref);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DEBUG) Log.d(TAG, "requestCode=" + requestCode + "	resultCode=" + resultCode + "	Intent data=" + data);
        if (requestCode == 1) {
            // restore
            try {
                String supplied = data.getStringExtra(OPEN_FILENAME);
                // false because user saved configs are not themes
                wantToDeleteOrApply(supplied, false);
            } catch (NullPointerException np) {
                // user backed out of filepicker just move on
            }
        } else if (requestCode == 2) {
            // save
            try {
                String supplied = data.getStringExtra(SAVE_FILENAME);
                runBackup(supplied, null, null);
            } catch (NullPointerException np) {
                // user backed out of filepicker nothing to see here
            }
        } else {
            // request code wasn't what we sent
            Log.wtf(TAG, "This shouldn't ever happen ...shit is fucked up");
        }
    }

    private boolean checkConfigFiles(String pathToConfig) {
        File configNameSpace = new File(pathToConfig);
        if (configNameSpace.exists() && configNameSpace.isFile() && configNameSpace.canRead()) {
            if (DEBUG) Log.d(TAG, "config files have been saved for: {" + configNameSpace.getAbsolutePath() + "}");
            return true;
        } else {
            if (DEBUG) Log.d(TAG, "config checks failed for: {" + configNameSpace.getAbsolutePath() + "}");
            return false;
        }
    }

    private void runRestore() {
        // call the file picker then apply in the result
        Intent open_file = new Intent(mContext, com.blackice.control.tools.FilePicker.class);
        open_file.putExtra(OPEN_FILENAME, BLANK);
        // false because we are not saving
        open_file.putExtra("action", false);
        // provide a path to start the user off on
        open_file.putExtra("path", PATH_TO_CONFIGS);
        // let users go where ever they want
        open_file.putExtra("lock_dir", false);
        // result code can be whatever but must match requestCode in onActivityResult
        startActivityForResult(open_file, 1);
    }

    private boolean restore(final String open_data_string, final boolean isTheme) {
        Handler mRestoreHandler = new Handler();
        final Runnable mRestoreThread = new Runnable() {
            public void run() {
                try {
                    Log.d(TAG, String.format("extra open data found: %s", open_data_string));

                    // always reset the arrays so we don't get confused with the last index each array
                    setupArrays();

                    // determine the name to be used for opening saved config files
                    File nameSpaceFile = new File(open_data_string);
                    final String userSuppliedFilename = nameSpaceFile.getName();
                    if (DEBUG) {
                        Log.d(TAG, String.format("userSuppliedFilename=%s for nameSpaceFile=%s", userSuppliedFilename, nameSpaceFile));
                    }

                    // theme path is final but let user restores can come from anywhere
                    final String filename_strings = open_data_string;
                    final String theme_filename_strings = String.format("%s/BlackICEControl/themes/%s",
                            Environment.getExternalStorageDirectory(), userSuppliedFilename);

                    // TODO handle missing files

                    // get a view to work with
                    LayoutInflater i = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View restoreDialogLayout = i.inflate(R.layout.what_happened_dialog, null);

                    // setup smooth scrolling within our info dialog
                    ScrollView sv = (ScrollView) restoreDialogLayout.findViewById(R.id.what_happened_scrollview);
                    sv.setSmoothScrollingEnabled(true);
                    sv.fling(DEFAULT_FLING_SPEED);

                    // get final reference so we can minuplate text in try blocks
                    TextView mRestoreInfo = (TextView) sv.findViewById(R.id.what_happened_more_info);
                    // Show the user what happend
                    AlertDialog.Builder restoreDialog = new AlertDialog.Builder(getActivity());
                    restoreDialog.setTitle(getString(R.string.what_happened_title));
                    restoreDialog.setView(restoreDialogLayout);
                    restoreDialog.setPositiveButton(getString(R.string.positive_thanks_button), new DialogInterface.OnClickListener() {
                         public void onClick(DialogInterface dialog, int whichButton) {
                         }
                    });
                    restoreDialog.setCancelable(false);
                    AlertDialog ad_ = restoreDialog.create();
                    ad_.show();

                    final Button mRestoreOk = (Button) ad_.getButton(AlertDialog.BUTTON_POSITIVE);
                    mRestoreOk.setEnabled(false);

                    StringBuilder collectInfo = new StringBuilder(settingsArray.size());
                    // first the strings
                    try {
                        File configFile;
                        if (isTheme) {
                            configFile = new File(theme_filename_strings);
                            if (DEBUG) Log.d(TAG, "Theme detected " + theme_filename_strings);
                        } else {
                            configFile = new File(filename_strings);
                        }
                        if (DEBUG) Log.d(TAG, String.format("Config file {%s}	Exists? %s	CanRead? %s",
                                configFile.getPath(), configFile.exists(), configFile.canRead()));
                        FileReader reader = new FileReader(configFile);
                        mProperties.load(reader);

                        // reset our indexes
                        setupArrays();

                        // use an army of clones for our dirty work -I think this is how the deathstar was started
                        ArrayList<String> array_strings = new ArrayList<String>(settingsArray);
                        ArrayList<String> array_ints = new ArrayList<String>(settingsArray);
                        ArrayList<String> array_floats = new ArrayList<String>(settingsArray);
                        ArrayList<String[]> array_string_array = new ArrayList<String[]>(arrayOfStrings);
                        ArrayList<String> arrays_handled = new ArrayList<String>();

                        int stringsHandled = 0;
                        int intsHandled = 0;
                        int floatsHandled = 0;

                        for (String[] sarray : array_string_array) {
                            for (int saint = 0; sarray.length > saint; saint++) {
                                Settings.System.putString(mContext.getContentResolver(), sarray[saint], ((String) mProperties.get(sarray[saint])));
                            }
                        }

                        for (String intPropCheck : array_ints) {
                            // don't handle floats here
                            if (!intPropCheck.contains("alpha")) {
                                if ((String) mProperties.get(intPropCheck) != null) {
                                    try {
                                        if (DEBUG) Log.d(TAG, String.format("Int property found: %s	value: %s",
                                                intPropCheck, (String) mProperties.get(intPropCheck)));
                                        Settings.System.putInt(mContext.getContentResolver(), intPropCheck,
                                                Integer.parseInt((String) mProperties.get(intPropCheck)));
                                        intsHandled = intsHandled + 1;
                                        arrays_handled.add(intPropCheck);
                                        // if we handle the property remove it from the other lists
                                        array_strings.remove(intPropCheck);
                                        array_floats.remove(intPropCheck);
                                    } catch  (NumberFormatException nfe) {
                                        if (CLASS_DEBUG) nfe.printStackTrace();
                                    } catch (ClassCastException cce) {
                                        // ok it's not a int
                                    } catch (Exception e) {
                                        if (DEBUG) e.printStackTrace();
                                    }
                                }
                            }
                        }

                        for (String floatPropCheck : array_floats) {
                            if (floatPropCheck.contains("alpha")) {
                                if ((String) mProperties.get(floatPropCheck) != null) {
                                    if (DEBUG) Log.d(TAG, String.format("Float property found: %s	value: %s",
                                            floatPropCheck, (String) mProperties.get(floatPropCheck)));
                                    try {
                                        Settings.System.putFloat(mContext.getContentResolver(), floatPropCheck,
                                                Float.parseFloat((String) mProperties.get(floatPropCheck)));
                                        array_strings.remove(floatPropCheck);
                                        arrays_handled.add(floatPropCheck);
                                    } catch  (NumberFormatException nfe) {
                                        if (DEBUG) nfe.printStackTrace();
                                    } catch (ClassCastException cce) {
                                        // ok it's not a float
                                    } catch (Exception e) {
                                        if (DEBUG) e.printStackTrace();
                                    }
                                }
                            }
                        }

                        // we now have an array that contains only strings
                        for (String stringPropCheck : array_strings) {
                            if ((String) mProperties.get(stringPropCheck) != null) {
                                if (DEBUG) Log.d(TAG, String.format("String Property found: %s	value: %s",
                                        stringPropCheck, (String) mProperties.get(stringPropCheck)));
                                try {
                                    Settings.System.putString(mContext.getContentResolver(), stringPropCheck,
                                            (String) mProperties.get(stringPropCheck));
                                    arrays_handled.add(stringPropCheck);
                                } catch (NumberFormatException nfe) {
                                    if (DEBUG) nfe.printStackTrace();
                                } catch (ClassCastException cce) {
                                    // this really shouldn't happen at this point
                                } catch (Exception e) {
                                    if (DEBUG) e.printStackTrace();
                                }
                            }
                        }

                        // let the users know what happend
                        ArrayList<String> watcher = new ArrayList<String>(settingsArray);
                        String title_aquired = (String) mProperties.get("title");
                        String summary_aquired = (String) mProperties.get("summary");
                        if (title_aquired != null || summary_aquired != null) {
                            collectInfo.append("Theme title: " + title_aquired + RETURN);
                            collectInfo.append("Description: " + summary_aquired + RETURN);
                            collectInfo.append(LINE_SPACE);
                        }

                        collectInfo.append("Properties handled:" + RETURN);
                        collectInfo.append(RETURN);
                        for (String sweetShit : arrays_handled) {
                            collectInfo.append(sweetShit + "=" + (String) mProperties.get(sweetShit) + RETURN);
                            watcher.remove(sweetShit);
                        }
                        collectInfo.append(LINE_SPACE);
                        collectInfo.append("Properties not handled:" + RETURN);
                        for (String moreSweetShit : watcher) {
                            collectInfo.append(moreSweetShit + RETURN);
                        }
                        collectInfo.append(TWO_LINE_SPACE);
                        collectInfo.append(String.format("We restored %d properties of the available %d properties",
                                settingsArray.size() - watcher.size(), settingsArray.size()) + RETURN);
                        collectInfo.append(LINE_SPACE);
                        if (isTheme) {
                            collectInfo.append("Theme file: " + theme_filename_strings);
                        } else {
                            collectInfo.append("Backup file: " + filename_strings + RETURN);
                        }
                        collectInfo.append(LINE_SPACE);
                        collectInfo.append("Thank you come again!");
                        mRestoreInfo.setText(collectInfo.toString());
                        mRestoreOk.setEnabled(true);
                        if (DEBUG) Log.d(TAG, "Message found:" + LINE_SPACE + collectInfo.toString());
                    } catch (Exception e) {
                        // TODO covering all my bases not sure what this could throw ...lazy
                        if (DEBUG) e.printStackTrace();
                    }
                } catch (NullPointerException npe) {
                    // let the user know and move on
                    if (DEBUG) npe.printStackTrace();
                    Toast.makeText(mContext, "no file was returned", Toast.LENGTH_SHORT).show();
                }

                // update screen
                findThemes();
            }
        };
        return mRestoreHandler.post(mRestoreThread);
    }

    private void setupArrays() {
        // be sure we start fresh each time we load
        settingsArray.clear();

        /* XXX These data sets are a pain to maintain so PLEASE KEEP UP TODATE!!! XXX */
        // Strings first
        // UserInterface
        settingsArray.add(Settings.System.CUSTOM_CARRIER_LABEL);
        // StatusBarToggles
        settingsArray.add(Settings.System.STATUSBAR_TOGGLES);

        // ints next
        // UserInterface
        settingsArray.add(Settings.System.ACCELEROMETER_ROTATION_ANGLES);
        settingsArray.add(Settings.System.RECENT_APP_SWITCHER);
        settingsArray.add(Settings.System.CRT_OFF_ANIMATION);
        settingsArray.add(Settings.System.SHOW_STATUSBAR_IME_SWITCHER);
        settingsArray.add(Settings.Secure.KILL_APP_LONGPRESS_BACK);
        settingsArray.add(Settings.System.ACCELEROMETER_ROTATION_SETTLE_TIME);
        // Navbar
        settingsArray.add(Settings.System.MENU_LOCATION);
        settingsArray.add(Settings.System.MENU_VISIBILITY);
        settingsArray.add(Settings.System.NAVIGATION_BAR_TINT);
        settingsArray.add(Settings.System.NAVIGATION_BAR_BACKGROUND_COLOR);
        settingsArray.add(Settings.System.NAVIGATION_BAR_HOME_LONGPRESS);
        settingsArray.add(Settings.System.NAVIGATION_BAR_WIDTH);
        settingsArray.add(Settings.System.NAVIGATION_BAR_HEIGHT);
        settingsArray.add(Settings.System.NAVIGATION_BAR_BUTTONS_SHOW);
        settingsArray.add(Settings.System.NAVIGATION_BAR_BUTTONS_QTY);
        settingsArray.add(Settings.System.NAVIGATION_BAR_GLOW_TINT);
        // Lockscreen
        settingsArray.add(Settings.System.LOCKSCREEN_TEXT_COLOR);
        settingsArray.add(Settings.System.LOCKSCREEN_LAYOUT);
        settingsArray.add(Settings.System.LOCKSCREEN_ENABLE_MENU_KEY);
        settingsArray.add(Settings.Secure.LOCK_SCREEN_LOCK_USER_OVERRIDE);
        settingsArray.add(Settings.System.SHOW_LOCK_BEFORE_UNLOCK);
        settingsArray.add(Settings.System.LOCKSCREEN_BATTERY);
        settingsArray.add(Settings.System.VOLUME_WAKE_SCREEN);
        settingsArray.add(Settings.System.VOLUME_MUSIC_CONTROLS);
        settingsArray.add(Settings.System.LOCKSCREEN_HIDE_NAV);
        settingsArray.add(Settings.System.LOCKSCREEN_LANDSCAPE);
        settingsArray.add(Settings.System.LOCKSCREEN_QUICK_UNLOCK_CONTROL);
        settingsArray.add(Settings.System.ENABLE_FAST_TORCH);
        settingsArray.add(Settings.System.LOCKSCREEN_4TAB);
        //settingsArray.add(Settings.System.LOCKSCREEN_LOW_BATTERY);
        // Powermenu
        settingsArray.add(Settings.System.POWER_DIALOG_SHOW_AIRPLANE_TOGGLE);
        settingsArray.add(Settings.System.POWER_DIALOG_SHOW_TORCH_TOGGLE);
        settingsArray.add(Settings.System.POWER_DIALOG_SHOW_NAVBAR_HIDE);
        settingsArray.add(Settings.System.POWER_DIALOG_SHOW_PROFILE_CHOOSER);
        settingsArray.add(Settings.System.POWER_DIALOG_SHOW_SCREENSHOT);
        // StatusBarGeneral
        settingsArray.add(Settings.System.STATUSBAR_SETTINGS_BEHAVIOR);
        settingsArray.add(Settings.System.STATUSBAR_QUICKTOGGLES_AUTOHIDE);
        settingsArray.add(Settings.System.DATE_OPENS_CALENDAR);
        settingsArray.add(Settings.System.STATUS_BAR_BRIGHTNESS_TOGGLE);
        settingsArray.add(Settings.Secure.ADB_ICON);
        settingsArray.add(Settings.System.STATUSBAR_NOTIFICATION_COLOR);
        settingsArray.add(Settings.System.STATUS_BAR_LAYOUT);
        settingsArray.add(Settings.System.STATUSBAR_FONT_SIZE);

        // StatusBarToggles
        settingsArray.add(Settings.System.STATUSBAR_TOGGLES_USE_BUTTONS);
        settingsArray.add(Settings.System.STATUSBAR_TOGGLES_BRIGHTNESS_LOC);
        settingsArray.add(Settings.System.STATUSBAR_TOGGLES_STYLE);
        // StatusBarClock
        settingsArray.add(Settings.System.STATUSBAR_CLOCK_STYLE);
        settingsArray.add(Settings.System.STATUSBAR_CLOCK_AM_PM_STYLE);
        settingsArray.add(Settings.System.STATUSBAR_SHOW_ALARM);
        settingsArray.add(Settings.System.STATUSBAR_CLOCK_COLOR);
        settingsArray.add(Settings.System.STATUSBAR_CLOCK_WEEKDAY);
        // StatusBarBattery
        settingsArray.add(Settings.System.STATUSBAR_BATTERY_ICON);
        settingsArray.add(Settings.System.STATUSBAR_BATTERY_BAR);
        settingsArray.add(Settings.System.STATUSBAR_BATTERY_BAR_STYLE);
        settingsArray.add(Settings.System.STATUSBAR_BATTERY_BAR_ANIMATE);
        settingsArray.add(Settings.System.STATUSBAR_BATTERY_BAR_THICKNESS);
        settingsArray.add(Settings.System.STATUSBAR_BATTERY_BAR_COLOR);
        // StatusBarSignal
        settingsArray.add(Settings.System.STATUSBAR_SIGNAL_TEXT);
        settingsArray.add(Settings.System.STATUSBAR_SIGNAL_TEXT_COLOR);
        settingsArray.add(Settings.System.STATUSBAR_HIDE_SIGNAL_BARS);
        settingsArray.add(Settings.System.STATUSBAR_WIFI_SIGNAL_TEXT);
        settingsArray.add(Settings.System.STATUSBAR_WIFI_SIGNAL_TEXT_COLOR);
        // Misc
        settingsArray.add(Settings.System.IS_TABLET);

        // floats next
        // Navbar
        settingsArray.add(Settings.System.NAVIGATION_BAR_BUTTON_ALPHA);
        // StatusBarGeneral
        settingsArray.add(Settings.System.STATUSBAR_NOTIFICATION_ALPHA);

        // easy stuff 0.o I wish i knew this was here a LONG TIME AGO!!!
        int mProperBackupLength = Settings.System.SETTINGS_TO_BACKUP.length;
        for (int i = 0; i < mProperBackupLength; i++) {
            settingsArray.add(Settings.System.SETTINGS_TO_BACKUP[i]);
        }

        // add String[] to ArrayList<String[]>
        arrayOfStrings.add(Settings.System.LOCKSCREEN_CUSTOM_APP_ICONS);
        arrayOfStrings.add(Settings.System.LOCKSCREEN_CUSTOM_APP_ACTIVITIES);
        arrayOfStrings.add(Settings.System.NAVIGATION_BAR_GLOW_DURATION);
        arrayOfStrings.add(Settings.System.NAVIGATION_CUSTOM_APP_ICONS);
        arrayOfStrings.add(Settings.System.NAVIGATION_CUSTOM_ACTIVITIES);
        arrayOfStrings.add(Settings.System.NAVIGATION_LONGPRESS_ACTIVITIES);

        // randomize arrays so we don't overly annoy any one area
        Collections.shuffle(settingsArray);
    }

    public Dialog onCreateDialog(final int id) {
        switch (id) {
            case THEME_INFO_DIALOG:
                // get a view to work with
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View customLayout = inflater.inflate(R.layout.save_theme_dialog, null);

                // TODO add filename and text watcher for valid filename
                final EditText titleText = (EditText) customLayout.findViewById(R.id.title_input_edittext);
                final EditText summaryText = (EditText) customLayout.findViewById(R.id.summary_input_edittext);

                // for that personal touch //TODO make setText not hint
                if (makeThemFeelAtHome != null) titleText.setText(makeThemFeelAtHome);
                // TODO add generic hint bs

                AlertDialog.Builder getInfo = new AlertDialog.Builder(getActivity());
                getInfo.setTitle(getString(R.string.name_theme_title));
                getInfo.setView(customLayout);

                String delimiter = "_";
                Calendar mTimeStamp = Calendar.getInstance();
                final String timeBasedThemeName = Calendar.MONTH + Calendar.DAY_OF_MONTH + Calendar.YEAR + delimiter
                        + Calendar.HOUR_OF_DAY + Calendar.MINUTE + Calendar.SECOND + delimiter + java.lang.System.currentTimeMillis();

                getInfo.setNegativeButton(getString(R.string.negative_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // just run a normal backup in the theme dir
                        runRestore();
                    }
                });

                getInfo.setPositiveButton(getString(R.string.positive_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // get supplied info
                        String value_title = ((Spannable) titleText.getText()).toString();
                        String value_summary = ((Spannable) summaryText.getText()).toString();
                        if (DEBUG) Log.d(TAG, String.format("found title: %s 	found summary: %s", value_title, value_summary));
                        String formatThemePath = String.format("%s/BlackICEControl/themes/%s",
                                Environment.getExternalStorageDirectory(), timeBasedThemeName);
                        runBackup(formatThemePath, value_title, value_summary);
                    }
                });

                AlertDialog ad_info = getInfo.create();
                ad_info.show();

                final Button makeThemeButton = (Button) ad_info.getButton(AlertDialog.BUTTON_POSITIVE);
                makeThemeButton.setEnabled(false);
                makeThemeButton.setText(TEXT_IS_EMPTY);
                titleText.addTextChangedListener(new TextWatcher() {
                    public void afterTextChanged(Editable e) {
                    }
                    public void beforeTextChanged(CharSequence cs, int start, int count, int after) {
                    }
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        String text = titleText.getText().toString();
                        int textLength = titleText.getText().length();
                        if (textLength > 0) {
                            makeThemeButton.setEnabled(true);
                            makeThemeButton.setText(getText(R.string.positive_button));
                        } else {
                            makeThemeButton.setEnabled(false);
                            makeThemeButton.setText(TEXT_IS_EMPTY);
                        }
                    }
                });

                return ad_info;
            case SAVE_CONFIG_DIALOG:
                // ask if user wants to make a theme
                AlertDialog.Builder askTheme = new AlertDialog.Builder(getActivity());
                askTheme.setTitle(getString(R.string.want_to_make_theme_title));
                askTheme.setMessage(getString(R.string.want_to_make_theme_message));
                askTheme.setPositiveButton(getString(R.string.positive_theme_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // launch theme maker
                        showDialog(THEME_INFO_DIALOG);
                    }
                });
                askTheme.setNegativeButton(getString(R.string.negative_theme_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // just run a normal backup
                        Intent save_file = new Intent(mContext, com.blackice.control.tools.FilePicker.class);
                        save_file.putExtra(SAVE_FILENAME, BLANK);
                        // true because we are saving
                        save_file.putExtra("action", true);
                        // provide a path to start the user off on
                        save_file.putExtra("path", PATH_TO_CONFIGS);
                        // let users go where ever they want
                        save_file.putExtra("lock_dir", false);
                        // result code can be whatever but must match requestCode in onActivityResult
                        startActivityForResult(save_file, 2);
                    }
                });
                AlertDialog ad_theme = askTheme.create();
                ad_theme.show();
                return ad_theme;
            default:
                return null;
        }
    }
}
