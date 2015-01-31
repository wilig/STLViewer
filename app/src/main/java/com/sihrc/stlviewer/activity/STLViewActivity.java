package com.sihrc.stlviewer.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import com.sihrc.stlviewer.R;
import com.sihrc.stlviewer.callback.OnClickFile;
import com.sihrc.stlviewer.renderer.STLRenderer;
import com.sihrc.stlviewer.view.STLView;

import java.io.File;

public class STLViewActivity extends Activity {
    private STLView stlView;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Uri uri = null;
        if (intent.getData() != null) {
            uri = getIntent().getData();
        }
        setUpViews(uri);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Parcelable stlFileName = savedInstanceState.getParcelable("STLFileName");
        if (stlFileName != null) {
            setUpViews((Uri) stlFileName);
        }
        boolean isRotate = savedInstanceState.getBoolean("isRotate");
        ToggleButton toggleButton = (ToggleButton) findViewById(R.id.rotateOrMoveToggleButton);
        toggleButton.setChecked(isRotate);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (stlView != null) {
            STLRenderer.requestRedraw();
            stlView.onResume();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (stlView != null) {
            outState.putParcelable("STLFileName", stlView.getUri());
            outState.putBoolean("isRotate", stlView.isRotate());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (stlView != null) {
            stlView.onPause();
        }
    }

    OnClickFile fileClick = new OnClickFile() {
        @Override
        public void onClick(File file) {
            if (file == null) {
                return;
            }

            SharedPreferences config = getSharedPreferences("PathSetting", Context.MODE_PRIVATE);
            SharedPreferences.Editor configEditor = config.edit();
            configEditor.putString("lastPath", file.getParent());
            configEditor.apply();

            setUpViews(Uri.fromFile(file));
        }
    };

    private void setUpViews(Uri uri) {
        setContentView(R.layout.stl);
        final ToggleButton toggleButton = (ToggleButton) findViewById(R.id.rotateOrMoveToggleButton);
        toggleButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (stlView != null) {
                    stlView.setRotate(isChecked);
                }
            }
        });

        final ImageButton loadButton = (ImageButton) findViewById(R.id.loadButton);
        loadButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences config = getSharedPreferences("PathSetting", Context.MODE_PRIVATE);
                FileDialogFragment.newInstance(fileClick, config.getString("lastPath", null)).show(getFragmentManager(), "FileDialog");
            }
        });

        final ImageButton preferencesButton = (ImageButton) findViewById(R.id.preferncesButton);
        preferencesButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(STLViewActivity.this, PreferencesActivity.class);
                startActivity(intent);
            }
        });

        if (uri != null) {
            setTitle(uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1));

            FrameLayout relativeLayout = (FrameLayout) findViewById(R.id.stlFrameLayout);
            stlView = new STLView(this, uri);
            relativeLayout.addView(stlView);

            toggleButton.setVisibility(View.VISIBLE);

            stlView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if (preferencesButton.getVisibility() == View.INVISIBLE) {

                    }
                }
            });
        }
    }
}
