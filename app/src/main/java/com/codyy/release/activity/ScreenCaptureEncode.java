package com.codyy.release.activity;

import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.codyy.release.R;
import com.codyy.robinsdk.RBPlayAndroid;
import com.codyy.robinsdk.RBPublishAndroid;
import com.codyy.robinsdk.impl.RBManagerAndroid;

import java.util.Locale;

public class ScreenCaptureEncode extends AppCompatActivity {

    private static final int SCREEN_CAPTURE_REQUEST_CODE = 100;
    private static final String TAG = "ScreenCaptureEncode";
    private int mCurFocusID = 0;
    private boolean mIsSupportMp = false;

    private RBManagerAndroid mManager = null;
    private RBPlayAndroid mPlayer = null;
    private RBPublishAndroid mPublisher = null;

    private String mPlayerUri = "rtmp://live.hkstv.hk.lxdns.com/live/hks live=1";
    private String mPublisherUri = "rtmp://10.5.31.218:1935/dms/LocalDirector live=1";

    private SurfaceView mPlayerSurfaceView = null;
    private TextView mPlayerStateText = null;
    private TextView mPublisherStateText = null;
    private EditText mPlayerUrlText = null;
    private EditText mPublisherUrlText = null;
    private EditText mPublisherWidthText = null;
    private EditText mPublisherHeightText = null;
    private EditText mPublisherFpsText = null;
    private EditText mPublisherBitrateText = null;
    private Button mButtonPlayerStart = null;
    private Button mButtonPlayerStop = null;
    private Button mButtonPlayerFullScreen = null;
    private Button mButtonScreenCapture = null;
    private Button mButtonPublisherStart = null;
    private Button mButtonPublisherStop = null;

    private MediaProjectionManager mMediaProjectionManager = null;
    private MediaProjection mMediaProjection = null;

    private int mScreenOrientation = RBManagerAndroid.RB_PORTRAIT;
    private int mPublisherCaptureIndex = 2;
    private int mPublisherWidth = 1280;
    private int mPublisherHeight = 720;
    private int mPublisherBitrate = 1024 * 2; //K
    private int mPublisherFps = 25;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_capture_encode);

        mPlayerSurfaceView = (SurfaceView)findViewById(R.id.surfaceView_player);
        mPlayerUrlText = (EditText)findViewById(R.id.url_player);
        mPublisherUrlText = (EditText)findViewById(R.id.url_publisher);
        mPublisherWidthText = (EditText)findViewById(R.id.editText_publisher_width);
        mPublisherHeightText = (EditText)findViewById(R.id.editText_publisher_height);
        mPublisherFpsText = (EditText)findViewById(R.id.editText_publisher_fps);
        mPublisherBitrateText = (EditText)findViewById(R.id.editText_publisher_bitrate);
        mButtonPlayerStart = (Button)findViewById(R.id.button_player_start);
        mButtonPlayerStop = (Button)findViewById(R.id.button_player_stop);
        mButtonPlayerFullScreen = (Button)findViewById(R.id.button_player_full_screen);
        mButtonScreenCapture = (Button)findViewById(R.id.button_screen_capture);
        mButtonPublisherStart = (Button)findViewById(R.id.button_publisher_start);
        mButtonPublisherStop = (Button)findViewById(R.id.button_publisher_stop);
        mPlayerStateText = (TextView)findViewById(R.id.textView_player_state);
        mPublisherStateText = (TextView)findViewById(R.id.textView_publisher_state);

        //button
        mButtonPlayerStart.setOnClickListener(mClickListener);
        mButtonPlayerStop.setOnClickListener(mClickListener);
        mButtonPlayerFullScreen.setOnClickListener(mClickListener);
        mButtonScreenCapture.setOnClickListener(mClickListener);
        mButtonPublisherStart.setOnClickListener(mClickListener);
        mButtonPublisherStop.setOnClickListener(mClickListener);

        //editText
        mPlayerUrlText.setOnFocusChangeListener(mFocusChangeListen);
        mPublisherUrlText.setOnFocusChangeListener(mFocusChangeListen);
        mPublisherWidthText.setOnFocusChangeListener(mFocusChangeListen);
        mPublisherHeightText.setOnFocusChangeListener(mFocusChangeListen);
        mPublisherFpsText.setOnFocusChangeListener(mFocusChangeListen);
        mPublisherBitrateText.setOnFocusChangeListener(mFocusChangeListen);

        mPlayerUrlText.addTextChangedListener(mEditTextWatcher);
        mPublisherUrlText.addTextChangedListener(mEditTextWatcher);
        mPublisherWidthText.addTextChangedListener(mEditTextWatcher);
        mPublisherHeightText.addTextChangedListener(mEditTextWatcher);
        mPublisherFpsText.addTextChangedListener(mEditTextWatcher);
        mPublisherBitrateText.addTextChangedListener(mEditTextWatcher);

        //default value
        mPlayerUrlText.setText(mPlayerUri.trim());
        mPublisherUrlText.setText(mPublisherUri.trim());
        mPublisherWidthText.setText((String.valueOf(mPublisherWidth)).trim());
        mPublisherHeightText.setText((String.valueOf(mPublisherHeight)).trim());
        mPublisherFpsText.setText((String.valueOf(mPublisherFps)).trim());
        mPublisherBitrateText.setText((String.valueOf(mPublisherBitrate)).trim());

        mButtonPlayerStop.setEnabled(false);
        mButtonPublisherStop.setEnabled(false);

        prepareResource();
        getPublisherInfo();
        getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseResource();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SCREEN_CAPTURE_REQUEST_CODE){
            mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
            if (mMediaProjection == null) {
                Log.e(TAG, "media projection is null");
            }
        }
    }

    private void prepareResource(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            functionBounced(String.format(Locale.CANADA, "the version of api is lower than 21, don't support media projection"));

            mButtonPublisherStart.setEnabled(false);
            mButtonPublisherStop.setEnabled(false);
            mButtonScreenCapture.setEnabled(false);

            mButtonPlayerStart.setEnabled(false);
            mButtonPlayerStop.setEnabled(false);
            mButtonPlayerFullScreen.setEnabled(false);
            return;
        }

        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mIsSupportMp = true;
        long res = 0;
        mManager = RBManagerAndroid.getSingleton();
        mManager.enableLog();
        if (mManager != null){
            res = mManager.init();
            if(res < 0){
                functionBounced(String.format(Locale.CHINA, "RBManager Init failed with %d", res));
            }

            mPublisher = mManager.createPublisher();
            res = mPublisher.init();
            if(res < 0){
                functionBounced(String.format(Locale.CHINA, "RBPublisher Init failed with %d", res));
            }

            mPublisher.setPublisherListener(new RBPublishAndroid.PublisherListener() {
                public void OnStateChange(final int newState) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (newState == RBManagerAndroid.RB_STATUS_PLAYING) {
                                mPublisherStateText.setText("play".trim());
                            }else if (newState == RBManagerAndroid.RB_STATUS_STOP){
                                mPublisherStateText.setText("stop".trim());
                            }
                        }
                    });
                }
                public void OnErrorGet(final int errorCode, final String desc) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            String errorDesc = String.format(Locale.CHINA,"error code %d, %s", errorCode, desc);
                            mPublisherUrlText.setText(errorDesc);
                        }
                    });
                }
                public void OnNoticeGet(final int noticeCode, final String desc) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            String errorDesc = String.format(Locale.CHINA, "notice code %d, %s", noticeCode, desc);
                            mPublisherUrlText.setText(errorDesc);
                        }
                    });
                }
                public void OnMediaModeChange(final int newMode) {
                }
                public void OnVideoIdChange(final int newVideoId){
                }
            });

            mPlayer = mManager.createPlayer();
            res = mPlayer.init();
            if (res < 0) {
                functionBounced(String.format(Locale.CHINA, "RBPlayer Init failed with error code %d", res));
            }

            mPlayer.setPlayerListener(new RBPlayAndroid.PlayerListener() {
                public void OnStateChange(final int newState) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if(newState == RBManagerAndroid.RB_STATUS_PLAYING) {
                                mPlayerStateText.setText("play".trim());
                            }else if (newState == RBManagerAndroid.RB_STATUS_PAUSING){
                                mPlayerStateText.setText("pause".trim());
                            }else if (newState == RBManagerAndroid.RB_STATUS_STOP){
                                mPlayerStateText.setText("stop".trim());
                            }
                        }
                    });
                }
                public void OnErrorGet(final int errorCode, final String desc) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            String errorDesc = String.format(Locale.CHINA,"error code %d, %s", errorCode, desc);
                            mPlayerUrlText.setText(errorDesc);
                        }
                    });
                }
                public void OnNoticeGet(final int noticeCode, final String desc) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            String errorDesc = String.format(Locale.CHINA,"notice code %d, %s", noticeCode, desc);
                            mPlayerUrlText.setText(errorDesc);
                        }
                    });
                }
                public void OnMediaModeChange(final int newMode) {
                }
                public void OnVideoResolution(int width, int height) {
                    Log.i("Player",String.format("video resolution : %dx%d", width, height));
                }
                public void OnBufferProcessing(int processPercentage) {
                    Log.i("Player",String.format("buffering : %d", processPercentage));
                }
            });
        }
    }

    private void getPublisherInfo()
    {
        if (mPublisher != null){
            mPublisherCaptureIndex = (int)mPublisher.getVideoDeviceNum() - 1;
        }
    }

    private void releaseResource(){
        if (mMediaProjection != null){
            mMediaProjection.stop();
            mMediaProjection = null;
        }

        if (mManager != null){
            releasePublisher(mManager, mPublisher);
            releasePlayer(mManager, mPlayer);
            mManager.release();
        }
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case  R.id.button_player_start:
                {
                    mPlayer.setUri(mPlayerUri);
                    mPlayer.setMediaMode(RBManagerAndroid.RB_MEDIA_ALL, mPlayerSurfaceView);
                    mPlayer.start();

                    mButtonPlayerStart.setEnabled(false);
                    mButtonPlayerStop.setEnabled(true);
                }
                    break;
                case  R.id.button_player_stop:
                {
                    mPlayer.stop();
                    mPlayerUrlText.setText(mPlayerUri.trim());

                    mButtonPlayerStart.setEnabled(true);
                    mButtonPlayerStop.setEnabled(false);
                }
                    break;
                case  R.id.button_player_full_screen:
                {
                    functionBounced("help me to prepare it ！");
                }
                    break;
                case  R.id.button_screen_capture:
                {
                   if(mMediaProjection == null){
                       Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
                       startActivityForResult(captureIntent, SCREEN_CAPTURE_REQUEST_CODE);
                   }
                }
                    break;
                case R.id.button_publisher_start:
                {
                    if (mPublisher == null){
                        functionBounced("mPublisher is null");
                        return;
                    }

                    mPublisher.setAudioDevice(0);
                    mPublisher.setVideoDevice(mPublisherCaptureIndex, mScreenOrientation);
                    mPublisher.setVideoResolution(mPublisherWidth, mPublisherHeight);
                    mPublisher.setVideoBitRate(mPublisherBitrate);
                    mPublisher.setVideoFrameRate(mPublisherFps);


                    mPublisher.setVideoHardwareEnc(true);
                    mPublisher.setAudioAEC(true);
                    if( mPublisher.setMediaProjection(mMediaProjection, 1) < 0) {
                        functionBounced("set media projection failed");
                        return;
                    }

                    mPublisher.setMediaMode(RBManagerAndroid.RB_MEDIA_ALL, null);
                    mPublisher.setUri(mPublisherUrlText.getText().toString().trim());

                    mPublisher.start();
                    mButtonPublisherStart.setEnabled(false);
                    mButtonPublisherStop.setEnabled(true);
                    removeFocus();
                }
                    break;
                case R.id.button_publisher_stop:
                {
                    if (mPublisher == null){
                        functionBounced("mPublisher is null");
                        return;
                    }

                    mPublisher.stop();
                    mPublisherUrlText.setText(mPublisherUri.trim());
                    mButtonPublisherStart.setEnabled(true);
                    mButtonPublisherStop.setEnabled(false);
                }
                    break;
                default:
                    break;
            }
        }
    };

    private void functionBounced(CharSequence text)
    {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

    private void removeFocus()
    {
        mPublisherWidthText.clearFocus();
        mPublisherHeightText.clearFocus();
        mPublisherFpsText.clearFocus();
        mPublisherBitrateText.clearFocus();
        mCurFocusID = 0;
    }

    private TextWatcher mEditTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            switch (mCurFocusID)
            {
                case R.id.url_player:
                    mPlayerUri = s.toString();
                    break;
                case R.id.url_publisher:
                    mPublisherUri = s.toString();
                    break;
                case R.id.editText_publisher_width:
                    if (s.toString().length() > 0) {
                        mPublisherWidth = Integer.parseInt(s.toString());
                    } else {
                        mPublisherWidth = 0;
                    }
                    break;
                case R.id.editText_publisher_height:
                    if (s.toString().length() > 0) {
                        mPublisherHeight = Integer.parseInt(s.toString());
                    } else {
                        mPublisherHeight = 0;
                    }
                    break;
                case R.id.editText_publisher_fps:
                    if (s.toString().length() > 0) {
                        mPublisherFps = Integer.parseInt(s.toString());
                    } else {
                        mPublisherFps = 0;
                    }
                    break;
                case R.id.editText_publisher_bitrate:
                    if (s.toString().length() > 0) {
                        mPublisherBitrate = Integer.parseInt(s.toString());
                    } else {
                        mPublisherBitrate = 0;
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private View.OnFocusChangeListener mFocusChangeListen = new View.OnFocusChangeListener(){
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if(hasFocus) {
                mCurFocusID = v.getId();
            }
        }
    };

    private void releasePublisher(RBManagerAndroid manager, RBPublishAndroid publisher)
    {
        try {
            if (manager != null && publisher != null) {
                publisher.stop();
                while (publisher.release() != 0) {
                    Thread.sleep(10);
                }
                manager.deletePublisher(publisher);
            }
        }catch (InterruptedException ex)
        {
            Log.e(TAG, "releasePublisher: Interrupted Exception is " + ex );
        }
    }

    private void releasePlayer(RBManagerAndroid manager, RBPlayAndroid player)
    {
        try {
            if (manager != null && player != null) {
                player.stop();
                while (player.release() != 0) {
                    Thread.sleep(10);
                }
                manager.deletePlayer(player);
            }
        }catch (InterruptedException ex) {
            Log.e(TAG, "releasePlayer: Interrupted Exception is " + ex );
        }
    }
}
