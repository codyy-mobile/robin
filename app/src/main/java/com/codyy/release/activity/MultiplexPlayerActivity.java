package com.codyy.release.activity;

import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.codyy.release.R;
import com.codyy.robinsdk.RBPlayAndroid;
import com.codyy.robinsdk.RBPublishAndroid;
import com.codyy.robinsdk.RBSize;
import com.codyy.robinsdk.impl.RBManagerAndroid;

import java.util.Locale;

public class MultiplexPlayerActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{

    private static final String TAG = "MultiplexPlayer";
    private static final int SupportMaxSizeNum = 32;
    private static final int SupportMaxSampleRateNum = 16;

    private int mCurFocusID = 0;
    private RBManagerAndroid mManager = null;
    private RBPlayAndroid mPlayer1 = null;
    private RBPlayAndroid mPlayer2 = null;
    private RBPlayAndroid mPlayer3 = null;
    private RBPlayAndroid mPlayer4 = null;
    private RBPlayAndroid mPlayer5 = null;
    private RBPlayAndroid mPlayer6 = null;
    private RBPlayAndroid mPlayer7 = null;
    private RBPlayAndroid mPlayer8 = null;
    private RBPlayAndroid mPlayer9 = null;
    private RBPublishAndroid mPublisher = null;

    private boolean mPlayer1NeedRestart = false;
    private boolean mPlayer2NeedRestart = false;
    private boolean mPlayer3NeedRestart = false;
    private boolean mPlayer4NeedRestart = false;
    private boolean mPlayer5NeedRestart = false;
    private boolean mPlayer6NeedRestart = false;
    private boolean mPlayer7NeedRestart = false;
    private boolean mPlayer8NeedRestart = false;
    private boolean mPlayer9NeedRestart = false;

    private SurfaceView mPlayer1SurfaceView = null;
    private SurfaceView mPlayer2SurfaceView = null;
    private SurfaceView mPlayer3SurfaceView = null;
    private SurfaceView mPlayer4SurfaceView = null;
    private SurfaceView mPlayer5SurfaceView = null;
    private SurfaceView mPlayer6SurfaceView = null;
    private SurfaceView mPlayer7SurfaceView = null;
    private SurfaceView mPlayer8SurfaceView = null;
    private SurfaceView mPlayer9SurfaceView = null;
    private SurfaceView mPublisherSurfaceView = null;

    private EditText mPlayerUrlText1 = null;
    private EditText mPlayerUrlText2 = null;
    private EditText mPlayerUrlText3 = null;
    private EditText mPlayerUrlText4 = null;
    private EditText mPlayerUrlText5 = null;
    private EditText mPlayerUrlText6 = null;
    private EditText mPlayerUrlText7 = null;
    private EditText mPlayerUrlText8 = null;
    private EditText mPlayerUrlText9 = null;
    private EditText mPublisherUrlText = null;

    private Spinner mSpinnerVideoCapture = null;
    private Spinner mSpinnerAudioCapture = null;
    private Spinner mSpinnerResolution = null;
    private Spinner mSpinnerSampleRate = null;
    private Spinner mSpinnerPublishMediaMode = null;

    //player config
    private String mPlayer1Uri = "rtmp://10.5.31.218:1935/dms/LocalDirector live=1";
    private String mPlayer2Uri = "rtmp://10.5.31.218:1935/dms/LocalDirector live=1";
    private String mPlayer3Uri = "rtmp://10.5.31.218:1935/dms/LocalDirector live=1";
    private String mPlayer4Uri = "rtmp://10.5.31.218:1935/dms/LocalDirector live=1";
    private String mPlayer5Uri = "rtmp://10.5.31.218:1935/dms/LocalDirector live=1";
    private String mPlayer6Uri = "rtmp://10.5.31.218:1935/dms/LocalDirector live=1";
    private String mPlayer7Uri = "rtmp://10.5.31.218:1935/dms/LocalDirector live=1";
    private String mPlayer8Uri = "rtmp://10.5.31.218:1935/dms/LocalDirector live=1";
    private String mPlayer9Uri = "rtmp://10.5.31.218:1935/dms/LocalDirector live=1"; //rtmp://dms01.needu.cn/dms/test

    private boolean mPlayer1HWDec = true;
    private boolean mPlayer2HWDec = true;
    private boolean mPlayer3HWDec = true;
    private boolean mPlayer4HWDec = true;
    private boolean mPlayer5HWDec = true;
    private boolean mPlayer6HWDec = true;
    private boolean mPlayer7HWDec = true;
    private boolean mPlayer8HWDec = true;
    private boolean mPlayer9HWDec = true;

    //publisher config
    private String mPublisherUri = "rtmp://10.5.31.218:1935/dms/LocalDirector";
    private int mPublisherVideoID = 0;
    private int mPublisherAudioID = 0;
    private int mScreenOrientation = 0;
    private int mPublisherWidth = 640;
    private int mPublisherHeight = 480;
    private int mPublisherSampleRate = 16000;
    private boolean mPublisherHWEnc = true;
    private boolean mPublisherAecm = false;

    private SparseArray<VideoDeviceInfo> mVideoDeviceInfos = null;
    private SparseArray<AudioDeviceInfo> mAudioDeviceInfos = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplex_player);

        //surface
        mPlayer1SurfaceView = (SurfaceView) findViewById(R.id.surfaceView_player1);
        mPlayer2SurfaceView = (SurfaceView) findViewById(R.id.surfaceView_player2);
        mPlayer3SurfaceView = (SurfaceView) findViewById(R.id.surfaceView_player3);
        mPlayer4SurfaceView = (SurfaceView) findViewById(R.id.surfaceView_player4);
        mPlayer5SurfaceView = (SurfaceView) findViewById(R.id.surfaceView_player5);
        mPlayer6SurfaceView = (SurfaceView) findViewById(R.id.surfaceView_player6);
        mPlayer7SurfaceView = (SurfaceView) findViewById(R.id.surfaceView_player7);
        mPlayer8SurfaceView = (SurfaceView) findViewById(R.id.surfaceView_player8);
        mPlayer9SurfaceView = (SurfaceView) findViewById(R.id.surfaceView_player9);
        mPublisherSurfaceView = (SurfaceView) findViewById(R.id.surfaceView_publisher);

        //editText
        mPlayerUrlText1 = (EditText)findViewById(R.id.url_player1);
        mPlayerUrlText2 = (EditText)findViewById(R.id.url_player2);
        mPlayerUrlText3 = (EditText)findViewById(R.id.url_player3);
        mPlayerUrlText4 = (EditText)findViewById(R.id.url_player4);
        mPlayerUrlText5 = (EditText)findViewById(R.id.url_player5);
        mPlayerUrlText6 = (EditText)findViewById(R.id.url_player6);
        mPlayerUrlText7 = (EditText)findViewById(R.id.url_player7);
        mPlayerUrlText8 = (EditText)findViewById(R.id.url_player8);
        mPlayerUrlText9 = (EditText)findViewById(R.id.url_player9);
        mPublisherUrlText = (EditText)findViewById(R.id.url_publisher);

        mPlayerUrlText1.setOnFocusChangeListener(mFocusChangeListen);
        mPlayerUrlText2.setOnFocusChangeListener(mFocusChangeListen);
        mPlayerUrlText3.setOnFocusChangeListener(mFocusChangeListen);
        mPlayerUrlText4.setOnFocusChangeListener(mFocusChangeListen);
        mPlayerUrlText5.setOnFocusChangeListener(mFocusChangeListen);
        mPlayerUrlText6.setOnFocusChangeListener(mFocusChangeListen);
        mPlayerUrlText7.setOnFocusChangeListener(mFocusChangeListen);
        mPlayerUrlText8.setOnFocusChangeListener(mFocusChangeListen);
        mPlayerUrlText9.setOnFocusChangeListener(mFocusChangeListen);
        mPublisherUrlText.setOnFocusChangeListener(mFocusChangeListen);

        mPlayerUrlText1.addTextChangedListener(mEditTextWatcher);
        mPlayerUrlText2.addTextChangedListener(mEditTextWatcher);
        mPlayerUrlText3.addTextChangedListener(mEditTextWatcher);
        mPlayerUrlText4.addTextChangedListener(mEditTextWatcher);
        mPlayerUrlText5.addTextChangedListener(mEditTextWatcher);
        mPlayerUrlText6.addTextChangedListener(mEditTextWatcher);
        mPlayerUrlText7.addTextChangedListener(mEditTextWatcher);
        mPlayerUrlText8.addTextChangedListener(mEditTextWatcher);
        mPlayerUrlText9.addTextChangedListener(mEditTextWatcher);
        mPublisherUrlText.addTextChangedListener(mEditTextWatcher);

        mPlayerUrlText1.setText(mPlayer1Uri.trim());
        mPlayerUrlText2.setText(mPlayer2Uri.trim());
        mPlayerUrlText3.setText(mPlayer3Uri.trim());
        mPlayerUrlText4.setText(mPlayer4Uri.trim());
        mPlayerUrlText5.setText(mPlayer5Uri.trim());
        mPlayerUrlText6.setText(mPlayer6Uri.trim());
        mPlayerUrlText7.setText(mPlayer7Uri.trim());
        mPlayerUrlText8.setText(mPlayer8Uri.trim());
        mPlayerUrlText9.setText(mPlayer9Uri.trim());
        mPublisherUrlText.setText(mPublisherUri.trim());

        //spinner
        mSpinnerVideoCapture = (Spinner) this.findViewById(R.id.spinner_video_capture);
        mSpinnerAudioCapture = (Spinner) this.findViewById(R.id.spinner_audio_capture);
        mSpinnerResolution = (Spinner) this.findViewById(R.id.spinner_video_resolution);
        mSpinnerSampleRate = (Spinner) this.findViewById(R.id.spinner_audio_samplerate);
        mSpinnerPublishMediaMode = (Spinner) this.findViewById(R.id.spinner_publish_media_mode);

        //checkbox
        setCheckBoxListener(R.id.check_player1_hw_decode);
        setCheckBoxListener(R.id.check_player2_hw_decode);
        setCheckBoxListener(R.id.check_player3_hw_decode);
        setCheckBoxListener(R.id.check_player4_hw_decode);
        setCheckBoxListener(R.id.check_player5_hw_decode);
        setCheckBoxListener(R.id.check_player6_hw_decode);
        setCheckBoxListener(R.id.check_player7_hw_decode);
        setCheckBoxListener(R.id.check_player8_hw_decode);
        setCheckBoxListener(R.id.check_player9_hw_decode);
        setCheckBoxListener(R.id.check_publisher_hw_encoder);
        setCheckBoxListener(R.id.check_publisher_aecm);
        //button
        setButtonListener(R.id.button_player1_play);
        setButtonListener(R.id.button_player1_stop);
        setButtonListener(R.id.button_player2_play);
        setButtonListener(R.id.button_player2_stop);
        setButtonListener(R.id.button_player3_play);
        setButtonListener(R.id.button_player3_stop);
        setButtonListener(R.id.button_player4_play);
        setButtonListener(R.id.button_player4_stop);
        setButtonListener(R.id.button_player5_play);
        setButtonListener(R.id.button_player5_stop);
        setButtonListener(R.id.button_player6_play);
        setButtonListener(R.id.button_player6_stop);
        setButtonListener(R.id.button_player7_play);
        setButtonListener(R.id.button_player7_stop);
        setButtonListener(R.id.button_player8_play);
        setButtonListener(R.id.button_player8_stop);
        setButtonListener(R.id.button_player9_play);
        setButtonListener(R.id.button_player9_stop);
        setButtonListener(R.id.button_publisher_play);
        setButtonListener(R.id.button_publisher_stop);

        //Create players and publisher, and get info
        Init();
        getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    public void onPause(){
        super.onPause();
        Release();
    }

    private void Init()
    {
        long res;
        mVideoDeviceInfos = new SparseArray<>();
        mAudioDeviceInfos = new SparseArray<>();

        mManager = RBManagerAndroid.getSingleton();
        mManager.enableLog();
        res = mManager.init();
        if(res < 0){
            FunctionBounced(String.format(Locale.CHINA, "RBManager Init failed with %d", res));
        }

        mPublisher = mManager.createPublisher();
        res = mPublisher.init();
        if(res < 0){
            FunctionBounced(String.format(Locale.CHINA,"RBPublisher Init failed with %d", res));
        }

        mPlayer1 = mManager.createPlayer();
        res = mPlayer1.init();
        if (res < 0) {
            FunctionBounced(String.format(Locale.CHINA, "RBPlayer Init failed with error code %d", res));
        }
        mPlayer1.setPlayerListener(new RBPlayAndroid.PlayerListener() {
            public void OnStateChange(final int newState) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (newState == RBManagerAndroid.RB_STATUS_STOP && mPlayer1NeedRestart){
                            startPlayer(mPlayer1, mPlayer1Uri, false, mPlayer1SurfaceView);
                            mPlayer1NeedRestart = false;
                        }
                    }
                });
            }
            public void OnErrorGet(final int errorCode, final String desc) {
            }
            public void OnNoticeGet(final int noticeCode, final String desc) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if(noticeCode == RBManagerAndroid.RB_NOTICE_NONSUPPORT_HW_VIDEO) {
                            stopPlayer(mPlayer1);
                            mPlayer1NeedRestart = true;
                        }
                    }
                });
            }
            public void OnMediaModeChange(final int newMode) {
            }
            public void OnVideoResolution(int width, int height) {
            }
            public void OnBufferProcessing(int percentage) {
            }
        });

        mPlayer2 = mManager.createPlayer();
        res = mPlayer2.init();
        if (res < 0) {
            FunctionBounced(String.format(Locale.CHINA,"RBPlayer Init failed with error code %d", res));
        }
        mPlayer2.setPlayerListener(new RBPlayAndroid.PlayerListener() {
            public void OnStateChange(final int newState) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (newState == RBManagerAndroid.RB_STATUS_STOP && mPlayer2NeedRestart){
                            startPlayer(mPlayer2, mPlayer2Uri, false, mPlayer2SurfaceView);
                            mPlayer2NeedRestart = false;
                        }
                    }
                });
            }
            public void OnErrorGet(final int errorCode, final String desc) {
            }
            public void OnNoticeGet(final int noticeCode, final String desc) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if(noticeCode == RBManagerAndroid.RB_NOTICE_NONSUPPORT_HW_VIDEO) {
                            stopPlayer(mPlayer2);
                            mPlayer2NeedRestart = true;
                        }
                    }
                });
            }
            public void OnMediaModeChange(final int newMode) {
            }
            public void OnVideoResolution(int width, int height) {
            }
            public void OnBufferProcessing(int percentage) {
            }
        });

        mPlayer3 = mManager.createPlayer();
        res = mPlayer3.init();
        if (res < 0) {
            FunctionBounced(String.format(Locale.CHINA,"RBPlayer Init failed with error code %d", res));
        }
        mPlayer3.setPlayerListener(new RBPlayAndroid.PlayerListener() {
            public void OnStateChange(final int newState) {
                runOnUiThread(new Runnable() {
                    public void run() {
                       if(newState == RBManagerAndroid.RB_STATUS_STOP && mPlayer3NeedRestart){
                            startPlayer(mPlayer3, mPlayer3Uri, false, mPlayer3SurfaceView);
                            mPlayer3NeedRestart = false;
                        }
                    }
                });
            }
            public void OnErrorGet(final int errorCode, final String desc) {
            }
            public void OnNoticeGet(final int noticeCode, final String desc) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if(noticeCode == RBManagerAndroid.RB_NOTICE_NONSUPPORT_HW_VIDEO) {
                            stopPlayer(mPlayer3);
                            mPlayer3NeedRestart = true;
                        }
                    }
                });
            }
            public void OnMediaModeChange(final int newMode) {
            }
            public void OnVideoResolution(int width, int height) {
            }
            public void OnBufferProcessing(int percentage) {
            }
        });

        mPlayer4 = mManager.createPlayer();
        res = mPlayer4.init();
        if (res < 0) {
            FunctionBounced(String.format(Locale.CHINA,"RBPlayer Init failed with error code %d", res));
        }
        mPlayer4.setPlayerListener(new RBPlayAndroid.PlayerListener() {
            public void OnStateChange(final int newState) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (newState == RBManagerAndroid.RB_STATUS_STOP && mPlayer4NeedRestart){
                            startPlayer(mPlayer4, mPlayer4Uri, false, mPlayer4SurfaceView);
                            mPlayer4NeedRestart = false;
                        }
                    }
                });
            }
            public void OnErrorGet(final int errorCode, final String desc) {
            }
            public void OnNoticeGet(final int noticeCode, final String desc) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if(noticeCode == RBManagerAndroid.RB_NOTICE_NONSUPPORT_HW_VIDEO) {
                            stopPlayer(mPlayer4);
                            mPlayer4NeedRestart = true;
                        }
                    }
                });
            }
            public void OnMediaModeChange(final int newMode) {
            }
            public void OnVideoResolution(int width, int height) {
            }
            public void OnBufferProcessing(int percentage) {
            }
        });

        mPlayer5 = mManager.createPlayer();
        res = mPlayer5.init();
        if (res < 0) {
            FunctionBounced(String.format(Locale.CHINA,"RBPlayer Init failed with error code %d", res));
        }
        mPlayer5.setPlayerListener(new RBPlayAndroid.PlayerListener() {
            public void OnStateChange(final int newState) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if(newState == RBManagerAndroid.RB_STATUS_STOP && mPlayer5NeedRestart){
                            startPlayer(mPlayer5, mPlayer5Uri, false, mPlayer5SurfaceView);
                            mPlayer5NeedRestart = false;
                        }
                    }
                });
            }
            public void OnErrorGet(final int errorCode, final String desc) {
            }
            public void OnNoticeGet(final int noticeCode, final String desc) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if(noticeCode == RBManagerAndroid.RB_NOTICE_NONSUPPORT_HW_VIDEO) {
                            stopPlayer(mPlayer5);
                            mPlayer5NeedRestart = true;
                        }
                    }
                });
            }
            public void OnMediaModeChange(final int newMode) {
            }
            public void OnVideoResolution(int width, int height) {
            }
            public void OnBufferProcessing(int percentage) {
            }
        });

        mPlayer6 = mManager.createPlayer();
        res = mPlayer6.init();
        if (res < 0) {
            FunctionBounced(String.format(Locale.CHINA,"RBPlayer Init failed with error code %d", res));
        }
        mPlayer6.setPlayerListener(new RBPlayAndroid.PlayerListener() {
            public void OnStateChange(final int newState) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if(newState == RBManagerAndroid.RB_STATUS_STOP && mPlayer6NeedRestart){
                            startPlayer(mPlayer6, mPlayer6Uri, false, mPlayer6SurfaceView);
                            mPlayer6NeedRestart = false;
                        }
                    }
                });
            }
            public void OnErrorGet(final int errorCode, final String desc) {
            }
            public void OnNoticeGet(final int noticeCode, final String desc) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if(noticeCode == RBManagerAndroid.RB_NOTICE_NONSUPPORT_HW_VIDEO) {
                            stopPlayer(mPlayer6);
                            mPlayer6NeedRestart = true;
                        }
                    }
                });
            }
            public void OnMediaModeChange(final int newMode) {
            }
            public void OnVideoResolution(int width, int height) {
            }
            public void OnBufferProcessing(int percentage) {
            }
        });

        mPlayer7 = mManager.createPlayer();
        res = mPlayer7.init();
        if (res < 0) {
            FunctionBounced(String.format(Locale.CHINA,"RBPlayer Init failed with error code %d", res));
        }
        mPlayer7.setPlayerListener(new RBPlayAndroid.PlayerListener() {
            public void OnStateChange(final int newState) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (newState == RBManagerAndroid.RB_STATUS_STOP && mPlayer7NeedRestart){
                            startPlayer(mPlayer7, mPlayer7Uri, false, mPlayer7SurfaceView);
                            mPlayer7NeedRestart = false;
                        }
                    }
                });
            }
            public void OnErrorGet(final int errorCode, final String desc) {
            }
            public void OnNoticeGet(final int noticeCode, final String desc) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if(noticeCode == RBManagerAndroid.RB_NOTICE_NONSUPPORT_HW_VIDEO) {
                            stopPlayer(mPlayer7);
                            mPlayer7NeedRestart = true;
                        }
                    }
                });
            }
            public void OnMediaModeChange(final int newMode) {
            }
            public void OnVideoResolution(int width, int height) {
            }
            public void OnBufferProcessing(int percentage) {
            }
        });

        mPlayer8 = mManager.createPlayer();
        res = mPlayer8.init();
        if (res < 0) {
            FunctionBounced(String.format(Locale.CHINA,"RBPlayer Init failed with error code %d", res));
        }
        mPlayer8.setPlayerListener(new RBPlayAndroid.PlayerListener() {
            public void OnStateChange(final int newState) {
                runOnUiThread(new Runnable() {
                    public void run() {
                       if (newState == RBManagerAndroid.RB_STATUS_STOP && mPlayer8NeedRestart){
                                startPlayer(mPlayer8, mPlayer8Uri, false, mPlayer8SurfaceView);
                                mPlayer8NeedRestart = false;
                       }
                    }
                });
            }
            public void OnErrorGet(final int errorCode, final String desc) {
            }
            public void OnNoticeGet(final int noticeCode, final String desc) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if(noticeCode == RBManagerAndroid.RB_NOTICE_NONSUPPORT_HW_VIDEO) {
                            stopPlayer(mPlayer8);
                            mPlayer8NeedRestart = true;
                        }
                    }
                });
            }
            public void OnMediaModeChange(final int newMode) {
            }
            public void OnVideoResolution(int width, int height) {
            }
            public void OnBufferProcessing(int percentage) {
            }
        });

        mPlayer9 = mManager.createPlayer();
        res = mPlayer9.init();
        if (res < 0) {
            FunctionBounced(String.format(Locale.CHINA,"RBPlayer Init failed with error code %d", res));
        }
        mPlayer9.setPlayerListener(new RBPlayAndroid.PlayerListener() {
            public void OnStateChange(final int newState) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (newState == RBManagerAndroid.RB_STATUS_STOP && mPlayer9NeedRestart){
                            startPlayer(mPlayer9, mPlayer9Uri, false, mPlayer9SurfaceView);
                            mPlayer9NeedRestart = false;
                        }
                    }
                });
            }
            public void OnErrorGet(final int errorCode, final String desc) {
            }
            public void OnNoticeGet(final int noticeCode, final String desc) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if(noticeCode == RBManagerAndroid.RB_NOTICE_NONSUPPORT_HW_VIDEO) {
                            stopPlayer(mPlayer9);
                            mPlayer9NeedRestart = true;
                        }
                    }
                });
            }
            public void OnMediaModeChange(final int newMode) {
            }
            public void OnVideoResolution(int width, int height) {
            }
            public void OnBufferProcessing(int percentage) {
            }
        });

        GetPublisherInfo();
        ConfigMediaMode();
    }

    void Release()
    {
        releasePublisher(mManager, mPublisher);
        releasePlayer(mManager, mPlayer1);
        releasePlayer(mManager, mPlayer2);
        releasePlayer(mManager, mPlayer3);
        releasePlayer(mManager, mPlayer4);
        releasePlayer(mManager, mPlayer5);
        releasePlayer(mManager, mPlayer6);
        releasePlayer(mManager, mPlayer7);
        releasePlayer(mManager, mPlayer8);
        releasePlayer(mManager, mPlayer9);

        mManager.release();
    }

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
        }catch (InterruptedException ex)
        {
            Log.e(TAG, "releasePlayer: Interrupted Exception is " + ex );
        }
    }

    private void spinnerConfigAdapter(Spinner spinner, ArrayAdapter<String> adapter)
    {
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    private void GetPublisherInfo()
    {
        ArrayAdapter<String> adapterVideoCapture= new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
        ArrayAdapter<String> adapterAudioCapture= new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);

        long cameraNum = mPublisher.getVideoDeviceNum();
        for (int i = 0; i < cameraNum; i++) {
            VideoDeviceInfo info = new VideoDeviceInfo();
            info.mDescribe = mPublisher.getVideoDeviceDescribe(i);
            mPublisher.getSupportVideoResolution(i, info.mSizes, info.mSizesNum);

            mVideoDeviceInfos.put(i,info);
            adapterVideoCapture.add(info.mDescribe);
        }

        long audioDeviceNum = mPublisher.getAudioDeviceNum();
        for(int i = 0; i < audioDeviceNum; i++){
            AudioDeviceInfo info = new AudioDeviceInfo();
            info.mDescribe = mPublisher.getAudioDeviceDescribe(i);
            mPublisher.getSupportAudioSampleRate(i, info.mSampleRates, info.mSampleRatesNum);

            mAudioDeviceInfos.put(i,info);
            adapterAudioCapture.add(info.mDescribe);
        }

        spinnerConfigAdapter(mSpinnerVideoCapture, adapterVideoCapture);
        spinnerConfigAdapter(mSpinnerAudioCapture, adapterAudioCapture);
    }

    void ConfigMediaMode()
    {
        SparseArray<String> mediaModes = new  SparseArray<>();
        ArrayAdapter<String> adapterPublishMode= new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);

        for (int i = 0; i <= RBManagerAndroid.RB_MEDIA_AUDIO; i++) {
            if (i == RBManagerAndroid.RB_MEDIA_ALL){
                mediaModes.put(i,"MediaAll");
            }else if(i == RBManagerAndroid.RB_MEDIA_VIDEO){
                mediaModes.put(i,"OnlyVideo");
            }else if(i == RBManagerAndroid.RB_MEDIA_AUDIO){
                mediaModes.put(i,"OnlyAudio");
            }
        }

        for(int i = 0; i< mediaModes.size(); i++) {
            adapterPublishMode.add(mediaModes.get(i));
        }

        spinnerConfigAdapter(mSpinnerPublishMediaMode, adapterPublishMode);
    }

    void StartPublish()
    {
        if (mPublisher == null){
            FunctionBounced("mPublisher is null");
            return;
        }

        mScreenOrientation = getResources().getConfiguration().orientation;

        mPublisher.setVideoDevice(mPublisherVideoID, mScreenOrientation);
        mPublisher.setAudioDevice(mPublisherAudioID);
        mPublisher.setVideoResolution(mPublisherWidth, mPublisherHeight);
        mPublisher.setAudioSampleRate(mPublisherSampleRate);

        mPublisher.setVideoHardwareEnc(mPublisherHWEnc);
        mPublisher.setUri(mPublisherUrlText.getText().toString().trim());

        mPublisher.start();
    }

    void  StopPublish()
    {
        if (mPublisher == null){
            FunctionBounced("mPublisher is null");
            return;
        }

        mPublisher.stop();
    }

    private void FunctionBounced(CharSequence text)
    {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
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
            String tmp = s.toString().substring(0, "rtmp://".length());
            if(tmp.equals("rtmp://")) {
                if (mCurFocusID == R.id.url_player1) {
                    mPlayer1Uri = s.toString();
                }else if(mCurFocusID == R.id.url_player2) {
                    mPlayer2Uri = s.toString();
                }else if(mCurFocusID == R.id.url_player3) {
                    mPlayer3Uri = s.toString();
                }else if(mCurFocusID == R.id.url_player4) {
                    mPlayer4Uri = s.toString();
                }else if(mCurFocusID == R.id.url_player5) {
                    mPlayer5Uri = s.toString();
                }else if(mCurFocusID == R.id.url_player6) {
                    mPlayer6Uri = s.toString();
                }else if(mCurFocusID == R.id.url_player7) {
                    mPlayer7Uri = s.toString();
                }else if(mCurFocusID == R.id.url_player8) {
                    mPlayer8Uri = s.toString();
                }else if(mCurFocusID == R.id.url_player9) {
                    mPlayer9Uri = s.toString();
                }else if (mCurFocusID == R.id.url_publisher) {
                    mPublisherUri = s.toString();
                }
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

    private CompoundButton.OnCheckedChangeListener mCheckBoxerListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton view, boolean checked) {
            switch (view.getId()) {
                case R.id.check_player1_hw_decode:
                    mPlayer1HWDec = checked;
                    break;
                case R.id.check_player2_hw_decode:
                    mPlayer2HWDec = checked;
                    break;
                case R.id.check_player3_hw_decode:
                    mPlayer3HWDec = checked;
                    break;
                case R.id.check_player4_hw_decode:
                    mPlayer4HWDec = checked;
                    break;
                case R.id.check_player5_hw_decode:
                    mPlayer5HWDec = checked;
                    break;
                case R.id.check_player6_hw_decode:
                    mPlayer6HWDec = checked;
                    break;
                case R.id.check_player7_hw_decode:
                    mPlayer7HWDec = checked;
                    break;
                case R.id.check_player8_hw_decode:
                    mPlayer8HWDec = checked;
                    break;
                case R.id.check_player9_hw_decode:
                    mPlayer9HWDec = checked;
                    break;
                case R.id.check_publisher_hw_encoder:
                    mPublisherHWEnc = checked;
                    break;
                case R.id.check_publisher_aecm:
                    if(mPublisher != null)
                    {
                        mPublisher.setAudioAEC(checked);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void setCheckBoxListener(int id) {
        CheckBox checkBox = (CheckBox) findViewById(id);

        checkBox.setOnCheckedChangeListener(mCheckBoxerListener);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case  R.id.button_player1_play:
                    if (mPlayer1 != null)
                    {
                        mPlayer1Uri = mPlayerUrlText1.getText().toString().trim();
                        startPlayer(mPlayer1, mPlayer1Uri, mPlayer1HWDec, mPlayer1SurfaceView);
                    }
                    break;
                case  R.id.button_player1_stop:
                    if (mPlayer1 != null)
                    {
                        stopPlayer(mPlayer1);
                    }
                    break;
                case R.id.button_player2_play:
                    if (mPlayer2 != null)
                    {
                        mPlayer2Uri = mPlayerUrlText2.getText().toString().trim();
                        startPlayer(mPlayer2, mPlayer2Uri,mPlayer2HWDec, mPlayer2SurfaceView);
                    }
                    break;
                case R.id.button_player2_stop:
                    if (mPlayer2 != null)
                    {
                        stopPlayer(mPlayer2);
                    }
                    break;
                case  R.id.button_player3_play:
                    if (mPlayer3 != null)
                    {
                        mPlayer3Uri = mPlayerUrlText3.getText().toString().trim();
                        startPlayer(mPlayer3, mPlayer3Uri, mPlayer3HWDec, mPlayer3SurfaceView);
                    }
                    break;
                case  R.id.button_player3_stop:
                    if (mPlayer3 != null)
                    {
                        stopPlayer(mPlayer3);
                    }
                    break;
                case R.id.button_player4_play:
                    if (mPlayer4 != null)
                    {
                        mPlayer4Uri = mPlayerUrlText4.getText().toString().trim();
                        startPlayer(mPlayer4, mPlayer4Uri,mPlayer4HWDec, mPlayer4SurfaceView);
                    }
                    break;
                case R.id.button_player4_stop:
                    if (mPlayer4 != null)
                    {
                        stopPlayer(mPlayer4);
                    }
                    break;
                case  R.id.button_player5_play:
                    if (mPlayer5 != null)
                    {
                        mPlayer5Uri = mPlayerUrlText5.getText().toString().trim();
                        startPlayer(mPlayer5, mPlayer5Uri, mPlayer5HWDec, mPlayer5SurfaceView);
                    }
                    break;
                case  R.id.button_player5_stop:
                    if (mPlayer5 != null)
                    {
                        stopPlayer(mPlayer5);
                    }
                    break;
                case R.id.button_player6_play:
                    if (mPlayer6 != null)
                    {
                        mPlayer6Uri = mPlayerUrlText6.getText().toString().trim();
                        startPlayer(mPlayer6, mPlayer6Uri,mPlayer6HWDec, mPlayer6SurfaceView);
                    }
                    break;
                case R.id.button_player6_stop:
                    if (mPlayer6 != null)
                    {
                        stopPlayer(mPlayer6);
                    }
                    break;
                case  R.id.button_player7_play:
                    if (mPlayer7 != null)
                    {
                        mPlayer7Uri = mPlayerUrlText7.getText().toString().trim();
                        startPlayer(mPlayer7, mPlayer7Uri, mPlayer7HWDec, mPlayer7SurfaceView);
                    }
                    break;
                case  R.id.button_player7_stop:
                    if (mPlayer7 != null)
                    {
                        stopPlayer(mPlayer7);
                    }
                    break;
                case R.id.button_player8_play:
                    if (mPlayer8 != null)
                    {
                        mPlayer8Uri = mPlayerUrlText8.getText().toString().trim();
                        startPlayer(mPlayer8, mPlayer8Uri,mPlayer8HWDec, mPlayer8SurfaceView);
                    }
                    break;
                case R.id.button_player8_stop:
                    if (mPlayer8 != null)
                    {
                        stopPlayer(mPlayer8);
                    }
                    break;
                case R.id.button_player9_play:
                    if (mPlayer9 != null)
                    {
                        mPlayer9Uri = mPlayerUrlText9.getText().toString().trim();
                        startPlayer(mPlayer9, mPlayer9Uri,mPlayer9HWDec, mPlayer9SurfaceView);
                    }
                    break;
                case R.id.button_player9_stop:
                    if (mPlayer9 != null)
                    {
                        stopPlayer(mPlayer9);
                    }
                    break;
                case R.id.button_publisher_play:
                    StartPublish();
                    break;
                case R.id.button_publisher_stop:
                    StopPublish();
                    break;
                default:
                    break;
            }
        }
    };

    private void setButtonListener(int id) {
        ImageButton button;
        button = (ImageButton) this.findViewById(id);
        button.setOnClickListener(mClickListener);
    }

    private void startPlayer(RBPlayAndroid play_cur, String uri, boolean isUseHardware, SurfaceView render_surface) {
        if (play_cur == null)
        {
            Log.e("RBPlayer","the give player handle is null");
            return;
        }
        play_cur.setUri(uri);
        play_cur.setVideoRenderMode(RBManagerAndroid.RB_RENDER_ASPECT_RATIO);
        play_cur.setVideoHardwareDec(isUseHardware);
        play_cur.setMediaMode(0, render_surface);
        play_cur.start();
    }

    private void stopPlayer(RBPlayAndroid play_cur)
    {
        if (play_cur!=null)
            play_cur.stop();
    }

    private void onChooseVideoCapture(int id)
    {
        VideoDeviceInfo info = mVideoDeviceInfos.get(id);
        if (info == null)
            return;

        ArrayAdapter<String> adapter= new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
        for (int i = 0; i< info.mSizesNum[0]; i++){
            adapter.add(String.format(Locale.CHINA, "%dx%d",info.mSizes[i].getWidth(), info.mSizes[i].getHeight()));
        }

        spinnerConfigAdapter(mSpinnerResolution, adapter);

        if(mPublisher != null){
            mPublisher.setVideoDevice(id, mScreenOrientation);
        }

        mPublisherVideoID = id;
    }

    private void onChooseAudioCapture(int id)
    {
        AudioDeviceInfo info = mAudioDeviceInfos.get(id);
        if (info == null)
            return;

        ArrayAdapter<String> adapter= new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
        for (int i = 0; i< info.mSampleRatesNum[0]; i++){
            adapter.add(String.format(Locale.CHINA, "%d",info.mSampleRates[i]));
        }

        spinnerConfigAdapter(mSpinnerSampleRate, adapter);

        if(mPublisher != null){
            mPublisher.setAudioDevice(id);
        }

        mPublisherAudioID = id;
    }

    private void onChooseResolution(int id)
    {
        VideoDeviceInfo info = mVideoDeviceInfos.get(mPublisherVideoID);
        if (info == null)
            return;

        RBSize size = info.mSizes[id];
        mPublisherWidth = size.getWidth();
        mPublisherHeight = size.getHeight();
    }

    private void onChooseSampleRate(int id)
    {
        AudioDeviceInfo info = mAudioDeviceInfos.get(mPublisherAudioID);
        if (info == null)
            return;

        mPublisherSampleRate = info.mSampleRates[id];
    }

    private  void onChoosePublishMediaMode(int id)
    {
        if (id == RBManagerAndroid.RB_MEDIA_AUDIO)
        {
            mPublisher.setMediaMode(id, null);
        }else{
            mPublisher.setMediaMode(id, mPublisherSurfaceView);
        }
    }

    @Override // spinner selected
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Spinner spinner = (Spinner) parent;

        TextView tv = (TextView)view;
        tv.setTextSize(12.0f);    //设置大小

        int position = spinner.getSelectedItemPosition();
        if (spinner == mSpinnerVideoCapture){
            onChooseVideoCapture(position);
        }else if (spinner == mSpinnerAudioCapture){
            onChooseAudioCapture(position);
        }else if (spinner == mSpinnerResolution){
            onChooseResolution(position);
        }else if(spinner == mSpinnerSampleRate){
            onChooseSampleRate(position);
        }else if (spinner == mSpinnerPublishMediaMode){
            onChoosePublishMediaMode(position);
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //Cog.e(TAG, "onConfigurationChanged");
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //当前为横屏， 在此处添加额外的处理代码
            int aa = getResources().getConfiguration().orientation;
            FunctionBounced(String.format(Locale.CHINA, "onConfigurationChanged %d", aa));
            Log.d(TAG, "onConfigurationChanged ORIENTATION_LANDSCAPE " + aa);
            if(null != mPublisher) {
                // TODO: 2016/10/25
            }
        } else if (this.getResources().getConfiguration().orientation  == Configuration.ORIENTATION_PORTRAIT) {
            //当前为竖屏， 在此处添加额外的处理代码
            int aa = getResources().getConfiguration().orientation;
            FunctionBounced(String.format(Locale.CHINA, "onConfigurationChanged %d", aa));
            Log.d(TAG, "onConfigurationChanged ORIENTATION_PORTRAIT " + aa);
            if(null != mPublisher){
                // TODO: 2016/10/25
            }
        }
    }

    //Inter class
    static private class VideoDeviceInfo
    {
        private String mDescribe;
        private RBSize[] mSizes;
        private int[] mSizesNum;

        VideoDeviceInfo()
        {
            mSizes = new RBSize[SupportMaxSizeNum];
            for(int i = 0; i < SupportMaxSizeNum; i++) {
                mSizes[i] = new RBSize();
            }

            mSizesNum = new int[1];
        }
    }

    static private class AudioDeviceInfo
    {
        private String mDescribe;
        private int[] mSampleRates;
        private int[] mSampleRatesNum;

        AudioDeviceInfo()
        {
            mSampleRates = new int[SupportMaxSampleRateNum];
            mSampleRatesNum = new int[1];
        }
    }
}

