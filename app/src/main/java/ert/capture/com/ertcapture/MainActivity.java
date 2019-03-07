package ert.capture.com.ertcapture;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.VideoView;

import java.io.IOException;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private int SDensity;
    private MediaProjectionManager MPManager;
    private MediaProjection MProjection;
    private VirtualDisplay VDisplay;
    private MediaProjectionCallback MPCallback;
    private ToggleButton TButton;
    private MediaPlayer MPlayer;
    private MediaRecorder MRecorder;
    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1260;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_PERMISSIONS = 10;
    private static final String RUNC = "MainActivity";
    private static final int REQUEST_CODE = 1000;
    private String Report = "Empty";
    private RelativeLayout RLayout;
    private String App = "Device_info";
    private static final String[] PACKAGES = new String[]{" "};
    private static final String[] SEND_TO_EMAIL = new String[]{"vinodv@hpe.com"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        SDensity = metrics.densityDpi;

        MRecorder = new MediaRecorder();

        MPManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        TButton = (ToggleButton) findViewById(R.id.ON);
        TButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) + ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale
                            (MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                            ActivityCompat.shouldShowRequestPermissionRationale
                                    (MainActivity.this, Manifest.permission.RECORD_AUDIO)) {
                        TButton.setChecked(false);
                        Snackbar.make(findViewById(android.R.id.content), ert.capture.com.ertcapture.R.string.app_name,
                                Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ActivityCompat.requestPermissions(MainActivity.this,
                                                new String[]{Manifest.permission
                                                        .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                                                REQUEST_PERMISSIONS);
                                    }
                                }).show();
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission
                                        .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                                REQUEST_PERMISSIONS);
                    }
                } else {
                    onToggleScreenShare(v);
                }
            }
        });

        RLayout = (RelativeLayout) findViewById(ert.capture.com.ertcapture.R.id.layout);
        Report = "Device_Info report:\n";
        try {
            PackageInfo info = super.getApplication().getPackageManager().getPackageInfo(getApplication().getPackageName(), 0);
            App = "Device Info v" + info.versionName + "(" + info.versionCode + ")";
            Device_label(true, App);
            for (String pkg : PACKAGES) {
                Device_label(false, getPkgVersion(pkg));
            }

            new Handler().postDelayed(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    Button sendEmail = new Button(MainActivity.this);
                    sendEmail.setText("SEND");
                    sendEmail.setOnClickListener(MainActivity.this);
                    RLayout.addView(sendEmail);
                }
            }, 250);
        } catch (Exception e) {
            e.printStackTrace();
            Device_label(true, "Exception: " + e.toString());
        }
        Button button = (Button)findViewById(ert.capture.com.ertcapture.R.id.buttonPanel);
        getWindow().setFormat(PixelFormat.UNKNOWN);

        button.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View view){
                final VideoView videoView = (VideoView)findViewById(ert.capture.com.ertcapture.R.id.video);
                String path1 = "android.resource://ert.capture.com.ertcapture/"+ R.raw.movie;
                Uri uri1 = Uri.parse(path1);
                videoView.setVideoURI(uri1);
                videoView.requestFocus();
                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
                    public void onPrepared(MediaPlayer MPlayer){
                        videoView.start();
                    }
                });
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) {
            Log.e(RUNC, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Denied", Toast.LENGTH_SHORT).show();
            TButton.setChecked(false);
            return;
        }
        MPCallback = new MediaProjectionCallback(); //requires mainactivity.mediaprojectioncallback
        MProjection = MPManager.getMediaProjection(resultCode, data); //MPManager.createScreenCaptureIntent();
        MProjection.registerCallback(MPCallback, null);
        VDisplay = createVirtualDisplay();
        MRecorder.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onToggleScreenShare(View view) {
        if (((ToggleButton) view).isChecked()) {
            Recorder();
            SScreen();
        } else {
            //MRecorder.stop();
            MRecorder.reset();
            Log.v(RUNC, "Recording");
            stopScreenSharing();
        }
    }

    private String getPkgVersion(String packageName) {
        try {
            PackageInfo info = getApplication().getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return packageName + " " + info.versionName + " (" + info.versionCode + ")";
        } catch (PackageManager.NameNotFoundException e) {
            return "Failed to get '" + packageName + "' info: " + e.getMessage();
        }
    }



    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void SScreen() {
        if (MProjection == null) {
            startActivityForResult(MPManager.createScreenCaptureIntent(), REQUEST_CODE);
            return;
        }
        VDisplay = createVirtualDisplay();
        MRecorder.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private VirtualDisplay createVirtualDisplay() {
        return MProjection.createVirtualDisplay("MainActivity", DISPLAY_WIDTH, DISPLAY_HEIGHT, SDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, MRecorder.getSurface(), null /*Callbacks*/, null
                /*Handler*/);
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    //https://developer.android.com/studio/debug/am-video.html
    private void Recorder() {
        try {
            MRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            MRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            MRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            Date createTime = new Date();
            MRecorder.setOutputFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/_rec" +createTime.getTime() + "_r.mp4");
            MRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            MRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);//https://developer.android.com/guide/topics/media/media-formats.html
            MRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            MRecorder.setVideoEncodingBitRate(512 * 2000);
            MRecorder.setVideoFrameRate(24);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);
            MRecorder.setOrientationHint(orientation);
            MRecorder.prepare(); //MREcorder.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.e(RUNC, "IllegalArgumentException:" + e);
        }
    }

    @Override
    public void onClick(View arg0) {

        Device_label(false, "LocationID:" +getLocationID());
        Device_label(false, "MapID:" +getMapID());
        Device_label(false, "Board: " + android.os.Build.BOARD);
        Device_label(false, "Brand: " + android.os.Build.BRAND);
        Device_label(false, "Device: " + android.os.Build.DEVICE);
        Device_label(false, "Model: " + android.os.Build.MODEL);
        Device_label(false, "Finger print: " + android.os.Build.FINGERPRINT);
        Device_label(false, "Build ID: " + android.os.Build.ID);
        Device_label(false, "Time: " + android.os.Build.TIME);
        Device_label(false, "Type: " + android.os.Build.TYPE);
        Device_label(false, "");

        final Intent emailIntent = new Intent(Intent.ACTION_SEND);

        emailIntent.setType("plain/text");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, SEND_TO_EMAIL);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, App);
        emailIntent.putExtra(Intent.EXTRA_TEXT, Report);

        //emailIntent.putExtra(Intent.EXTRA_STREAM, u);
                /* Send it off to the Activity-Chooser */
        this.startActivity(Intent.createChooser(emailIntent, "Send Email"));

    }

    public String getLocationID() {
        EditText LocationID = (EditText)findViewById(ert.capture.com.ertcapture.R.id.LocationID);
        return LocationID.getText().toString();
    }

    public String getMapID() {

        EditText MapID = (EditText)findViewById(ert.capture.com.ertcapture.R.id.MapID);
        return MapID.getText().toString();
    }

    private void Device_label(boolean bold, String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        RLayout.addView(label);
        Report = Report + "\n" + text;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (TButton.isChecked()) {
                TButton.setChecked(false);
                MRecorder.stop();
                MRecorder.reset();
                Log.v(RUNC, "Recording Stopped");
            }
            MProjection = null;
            stopScreenSharing();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopScreenSharing() {
        if (VDisplay == null) {
            return;
        }
        VDisplay.release();
        //mMediaRecorder.release(); //If used: MRecorder object cannot
        // reused again
        destroyMediaProjection();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyMediaProjection();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void destroyMediaProjection() {
        if (MProjection != null) {
            MProjection.unregisterCallback(MPCallback);
            MProjection.stop();
            MProjection = null;
        }
        Log.i(RUNC, "Recording Stopped");
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                if ((grantResults.length > 0) && (grantResults[0] +
                        grantResults[1]) == PackageManager.PERMISSION_GRANTED) {
                    onToggleScreenShare(TButton);
                } else {
                    TButton.setChecked(false);
                    Snackbar.make(findViewById(android.R.id.content), "ERTCapture", Snackbar.LENGTH_INDEFINITE).setAction("Enable", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                            sendBroadcast(intent);
                        }
                    }).show();
                }
            }
        }
    }

    /*Reference:
    *http://stackoverflow.com/questions/18886981/how-to-decode-the-h-264-video-stream-received-from-parcelfiledescriptor
    *https://www.tutorialspoint.com/android/android_audio_capture.htm
    *https://guides.codepath.com/android/Recording-Video-of-an-Android-Device
    * //https://developer.android.com/reference/android/media/MediaRecorder.html
    *
    * https://developer.android.com/reference/android/os/Build.VERSION.html 
    * https://developer.android.com/reference/android/os/Build.html
    * https://developer.android.com/reference/android/os/Build.html#getRadioVersion()
    * https://developer.android.com/training/basics/firstapp/starting-activity.html */

    /*
     * Created by vadlamudi on 11/30/16.
     */


}