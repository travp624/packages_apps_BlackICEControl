
package com.blackice.control.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.blackice.control.R;

public class FilePicker extends ListActivity {

    private static final String TAG = "BlackICE: FilePicker";
    private static final boolean DEBUG = false;
    private static final boolean DEFAULT = false;

    private Button saveButton;
    private EditText saveFilename;
    private Intent intent;
    private List<String> item = null;
    private List<String> path = null;
    private String BLANK = "";
    private String DIR_MARKER = "/";
    private String root="/";
    private final String OPEN_FILENAME = "open_filepath";
    private final String OPEN = "open";
    private final String PARENT_DIR = "../";
    private final String SAVE = "save";
    private final String SAVE_FILENAME = "save_filepath";
    private static String PREV_PATH;
    private SharedPreferences mSharedPrefs;
    private TextView myPath;

    private final String prompt_title = "[ %s ]";
    private final String file_selection_error_read = "We can\'t read: %s";
    private final String save_prompt = "Save as?";
    private final String save_message = "Overwrite: %s ?";
    private final String open_message = "Open: %s";
    private final String open_prompt = "Open?";
    private final String no_filename_save_error = "No filename detected ...Please enter a filename to save";
    private final String location_tracker = "Location: %s";

    private String mPrompt;
    private String mFileError;
    private String mMessage;

    /* are we locking the user in supplied directory */
    public static boolean LOCKED_IN_DIR = false;
    public static boolean FIND_ZIP = false;

    TextView mEmptyDirMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_picker);

        saveButton = (Button)findViewById(R.id.save_button);
        mEmptyDirMessage = (TextView) findViewById(R.id.empty);
        myPath = (TextView)findViewById(R.id.path);
        saveFilename = (EditText)findViewById(R.id.save_filename);

        // set a watcher
        saveFilename.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable e) {
            }
            public void beforeTextChanged(CharSequence cs, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = saveFilename.getText().toString();
                File textFile = new File(text);
                if (textFile.isDirectory() || textFile.exists()) {
                    saveButton.setEnabled(false);
                } else {
                    saveButton.setEnabled(true);
                }
            }
        });
        // decide if we want to save or just open file
        boolean areWeSaving = DEFAULT;
        try {
            areWeSaving = getIntent().getBooleanExtra("action", DEFAULT);
        } catch (NullPointerException npe) {
            if (DEBUG) npe.printStackTrace();
        }

        PREV_PATH = "/sdcard/";
        try {
            PREV_PATH = getIntent().getStringExtra("path");
        } catch (NullPointerException npe) {
            if (DEBUG) npe.printStackTrace();
        }

        try {
            LOCKED_IN_DIR = getIntent().getBooleanExtra("lock_dir", DEFAULT);
        } catch (NullPointerException npe) {
            if (DEBUG) npe.printStackTrace();
        }

        try {
            FIND_ZIP = getIntent().getBooleanExtra("zip", DEFAULT);
        } catch (NullPointerException npe) {
            if (DEBUG) npe.printStackTrace();
        }

        LinearLayout save_layout = (LinearLayout) findViewById(R.id.save_layout);
        if (!areWeSaving) {
            save_layout.setVisibility(View.GONE);
        }
        setStrings();

        saveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String filename_entered = saveFilename.getText().toString();
                if (DEBUG) Log.d(TAG, String.format("path detected: %s", filename_entered));
                if (!filename_entered.equals(BLANK)) {
                    File path_track = new File (filename_entered);
                    intent = getIntent();
                    intent.putExtra(SAVE_FILENAME, filename_entered);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), no_filename_save_error, Toast.LENGTH_SHORT).show();
                }
            }
        });
        if (DEBUG) Log.d(TAG, String.format("Previous path %s", PREV_PATH));
        getDir(PREV_PATH);
    }
    
    private void getDir(String dirPath) {

        myPath.setText(String.format(location_tracker, dirPath));
        if (!dirPath.equals(DIR_MARKER)) {
            saveFilename.setText(dirPath +DIR_MARKER);
            PREV_PATH = dirPath;
        }

        item = new ArrayList<String>();
        path = new ArrayList<String>();
        File f = new File(dirPath);
        File[] files = f.listFiles();
        if (!dirPath.equals(root)) {
            if (!LOCKED_IN_DIR) {
                item.add(root);
                path.add(root);
                item.add(PARENT_DIR);
                path.add(f.getParent());
            }
        }

        try {
            for (int i=0; i < files.length; i++) {
                File file = files[i];
                mEmptyDirMessage.setVisibility(View.GONE);

                //put list in alphabetic order
                Collections.sort(item, String.CASE_INSENSITIVE_ORDER);
                Collections.sort(path, String.CASE_INSENSITIVE_ORDER);

                if (file.isDirectory()) {
                    path.add(file.getPath());
                    item.add(file.getName() + DIR_MARKER);
                } else {
                    if (FIND_ZIP) {
                        String s_ = file.getName();
                        if (s_.endsWith(".zip")) {
                            path.add(file.getPath());
                            item.add(file.getName());
                        }
                    } else {
                        path.add(file.getPath());
                        item.add(file.getName());
                    }
                }
            }

            ArrayAdapter<String> fileList = new ArrayAdapter<String>(this, R.layout.row, item);
            setListAdapter(fileList);
        } catch (NullPointerException npe) {
            if (DEBUG) Log.d(TAG, "we experienced a problem with the path");
            mEmptyDirMessage.setVisibility(View.VISIBLE);
        }

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        final File file = new File(path.get(position));
        final String title = String.format(mFileError, file.getAbsolutePath());

        if (file.isDirectory()) {
            if (file.canRead()) {
                getDir(path.get(position));
            } else {
                new AlertDialog.Builder(this)
                .setIcon(R.drawable.open)
                .setTitle(title)
                .setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            }
        } else {
            new AlertDialog.Builder(this)
            .setIcon(R.drawable.files)
            .setTitle(String.format(mMessage, file.getAbsolutePath()))
            .setPositiveButton(mPrompt,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    intent = getIntent();
                    String filename = new String(file.getAbsolutePath());
                    if (DEBUG) Log.d(TAG, String.format("File selected: %s", filename));
                    if (!getIntent().getBooleanExtra("action", DEFAULT)) {
                        intent.putExtra(OPEN_FILENAME, filename);
                    } else {
                        intent.putExtra(SAVE_FILENAME, filename);
                    }
                    setResult(RESULT_OK, intent);
                    PREV_PATH = file.getParent();
                    finish();
                }
            })
            .setNegativeButton(R.string.cancel,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            }).show();
        }
    }

    public void setStrings() {
        //decide what strings to show
        if (!getIntent().getBooleanExtra("action", DEFAULT)) {
            mPrompt = open_prompt;
            mFileError = file_selection_error_read;
            mMessage = open_message;
        } else {
            mPrompt = save_prompt;
            mFileError = file_selection_error_read;
            mMessage = save_message;
        }

        //because we don't want null pointers
        if ((PREV_PATH == null) || (PREV_PATH.equals(BLANK))) {
            PREV_PATH = root;
            if (DEBUG) Log.d(TAG, String.format("Previous path %s", PREV_PATH));
        }
    }
}
