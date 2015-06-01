package com.sihrc.stlviewer.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.sihrc.stlviewer.R;
import com.sihrc.stlviewer.callback.OnClickFile;
import com.sihrc.stlviewer.util.IOUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by sihrc on 1/31/15.
 */
public class FileDialogFragment extends DialogFragment {
    private List<String> mFileList;
    private File mPath;

    Context context;
    OnClickFile onClickFile;
    ArrayAdapter<String> fileList;

    public FileDialogFragment() {}
    public static FileDialogFragment newInstance(OnClickFile onClickFile, String lastPath) {
        FileDialogFragment dialogFragment = new FileDialogFragment();
        dialogFragment.onClickFile = onClickFile;
        dialogFragment.mPath = lastPath == null ? new File(Environment.getExternalStorageDirectory().getPath()) : new File(lastPath);
        return dialogFragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        fileList = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1);
    }

    private void loadFileList() {
        try {
            mPath.mkdirs();
        } catch (SecurityException e) {
            Log.e("FileDialogFragment", "unable to write on the sd card " + e.toString());
        }
        if (mPath.exists()) {
            FilenameFilter filter = new FilenameFilter() {

                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    return filename.toUpperCase().contains(".STL") || sel.isDirectory();
                }
            };
            mFileList = new ArrayList<>(Arrays.asList(mPath.list(filter)));
        } else {
            mFileList = new ArrayList<>();
        }
        fileList.clear();
        if (!mPath.getAbsolutePath().equals("/"))
            fileList.add("..");

        try {
            Collections.sort(mFileList);
            fileList.addAll(mFileList);

        } catch (NullPointerException e) {
            e.printStackTrace();
            Toast.makeText(context, context.getString(R.string.file_accesss_error), Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle("Choose your file");
        loadFileList();
        if (mFileList == null) {
            Log.e("FileDialogFragment", "Showing file picker before loading the file list");
            dialog = builder.create();
            return dialog;
        }
        builder.setAdapter(fileList, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });


        dialog = builder.create();
        dialog.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0 && !mPath.getAbsolutePath().equals("/")) {
                        mPath = new File(IOUtils.upOneDirectory(mPath.getAbsolutePath()));
                        loadFileList();
                        fileList.notifyDataSetChanged();
                        return;
                    }

                    position -= mPath.getAbsolutePath().equals("/") ? 0 : 1;
                    File chosenFile = new File(mPath.getAbsolutePath(), mFileList.get(position));
                    if(chosenFile.isDirectory()) {
                        mPath = chosenFile;
                        loadFileList();
                        fileList.notifyDataSetChanged();
                    } else {
                        if (onClickFile != null)
                            onClickFile.onClick(chosenFile);
                        dismiss();
                    }
                }
        });
        return dialog;
    }
}