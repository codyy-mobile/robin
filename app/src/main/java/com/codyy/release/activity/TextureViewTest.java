package com.codyy.release.activity;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.codyy.release.R;
import com.codyy.robinsdk.RBPlayAndroid;
import com.codyy.robinsdk.RBPublishAndroid;
import com.codyy.robinsdk.RBSize;
import com.codyy.robinsdk.impl.RBManagerAndroid;

import org.w3c.dom.Text;

import java.util.Locale;

public class TextureViewTest extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, AdapterView.OnItemSelectedListener {

    private static final String TAG = "PublisherAndPlay";
    private static final int SupportMaxSizeNum = 32;
    private static final int SupportMaxSampleRateNum = 16;

    private TextureViewTest.AlbumOrientationEventListener mOrientationEventListener = null;
    private int mCurFocusID = 0;
    private RBManagerAndroid mManager = null;
    private RBPlayAndroid mPlayer1 = null;
    private RBPublishAndroid mPublisher = null;

    private SurfaceView mPlayer1SurfaceView = null;
    private TextureView mPlayer1TextureView = null;
    private SurfaceView mPublisherSurfaceView = null;
    private TextureView mPublisherTextureView = null;
    private int mPlayer1RenderViewFlags = 0;
    private int mPublisherRenderViewFlags = 0;

    private EditText mPlayerUrlText1 = null;
    private EditText mPublisherUrlText = null;

    private TextView mPlayerStateText1 = null;
    private TextView mPublisherStateText = null;
    private SeekBar mPlayer1VolumeControl = null;
    private TextView mPlayer1VolumeDisplay = null;

    private Spinner mSpinnerVideoCapture = null;
    private Spinner mSpinnerAudioCapture = null;
    private Spinner mSpinnerResolution = null;
    private Spinner mSpinnerSampleRate = null;
    private Spinner mSpinnerPublishMediaMode = null;
    private Spinner mSpinnerPlayer1MediaMode = null;
    //player1 config
    private String mPlayer1Uri = "rtmp://10.5.31.218:1935/dms/LocalDirector live=1";
    private boolean mPlayer1HWDec = true;
    private boolean mPlayer1FullScreen = false;
    private boolean mPlayer1Texture = false;

    //publisher config
    private String mPublisherUri = "rtmp://10.5.31.218:1935/dms/LocalDirector";
    private int mPublisherVideoID = 0;
    private int mPublisherAudioID = 0;
    private int mScreenOrientation = RBManagerAndroid.RB_PORTRAIT;
    private int mPublisherWidth = 640;
    private int mPublisherHeight = 480;
    private int mPublisherSampleRate = 16000;
    private boolean mPublisherHWEnc = true;
    private boolean mPublisherAecm = true;
    private boolean mPublisherTexture = false;
    private boolean mIsPublisherStarted = false;

    private SparseArray<TextureViewTest.VideoDeviceInfo> mVideoDeviceInfos = null;
    private SparseArray<TextureViewTest.AudioDeviceInfo> mAudioDeviceInfos = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture_view_test);

        //surface
        mPlayer1SurfaceView = (SurfaceView) findViewById(R.id.surfaceView_player1);
        mPlayer1TextureView = (TextureView) findViewById(R.id.textureView_player1);
        mPublisherSurfaceView = (SurfaceView) findViewById(R.id.surfaceView_publisher);
        mPublisherTextureView = (TextureView) findViewById(R.id.textureView_publisher);
        //seekBar
        mPlayer1VolumeControl = (SeekBar) this.findViewById(R.id.player1_volume_control);
        mPlayer1VolumeControl.setOnSeekBarChangeListener(this);
        mPlayer1VolumeControl.setProgress(80);
        //textView
        mPlayer1VolumeDisplay = (TextView) this.findViewById(R.id.player1_volume_display);
        //editText
        mPlayerUrlText1 = (EditText)findViewById(R.id.url_player1);
        mPublisherUrlText = (EditText)findViewById(R.id.url_publisher);

        mPlayerUrlText1.setOnFocusChangeListener(mFocusChangeListen);
        mPublisherUrlText.setOnFocusChangeListener(mFocusChangeListen);

        mPlayerUrlText1.addTextChangedListener(mEditTextWatcher);
        mPublisherUrlText.addTextChangedListener(mEditTextWatcher);

        mPlayerUrlText1.setText(mPlayer1Uri.trim());
        mPublisherUrlText.setText(mPublisherUri.trim());
        //textView
        mPlayerStateText1 = (TextView)findViewById(R.id.state_player1);
        mPublisherStateText = (TextView)findViewById(R.id.state_publisher);
        //spinner
        mSpinnerVideoCapture = (Spinner) this.findViewById(R.id.spinner_video_capture);
        mSpinnerAudioCapture = (Spinner) this.findViewById(R.id.spinner_audio_capture);
        mSpinnerResolution = (Spinner) this.findViewById(R.id.spinner_video_resolution);
        mSpinnerSampleRate = (Spinner) this.findViewById(R.id.spinner_audio_samplerate);
        mSpinnerPublishMediaMode = (Spinner) this.findViewById(R.id.spinner_publish_media_mode);
        mSpinnerPlayer1MediaMode = (Spinner) this.findViewById(R.id.spinner_player1_media_mode);
        //checkbox
        setCheckBoxListener(R.id.check_player1_hw_decode);
        setCheckBoxListener(R.id.check_player1_full_screen);
        setCheckBoxListener(R.id.check_player1_texture);
        setCheckBoxListener(R.id.check_publisher_hw_encoder);
        setCheckBoxListener(R.id.check_publisher_aecm);
        setCheckBoxListener(R.id.check_publisher_texture);
        //button
        setButtonListener(R.id.button_player1_play);
        setButtonListener(R.id.button_player1_stop);
        setButtonListener(R.id.button_publisher_play);
        setButtonListener(R.id.button_publisher_stop);

        mOrientationEventListener = new TextureViewTest.AlbumOrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL);
        if (mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable();
        } else {
            functionBounced("Can't Detect Orientation");
        }
        //Create players and publisher, and get info
        init();
        getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    public void onPause(){
        super.onPause();
        release();
    }

    public void onDestroy() {
        super.onDestroy();
        mOrientationEventListener.disable();
    }

    public void onStart(){
        super.onStart();
    }

    public void onStop(){
        super.onStop();
    }

    private void init()
    {
        long res = 0;
        mVideoDeviceInfos = new SparseArray<>();
        mAudioDeviceInfos = new SparseArray<>();

        mManager = RBManagerAndroid.getSingleton();
        mManager.enableLog();
        res = mManager.init();
        if(res < 0){
            functionBounced(String.format(Locale.CHINA, "RBManager Init failed with %d", res));
        }
        String version = mManager.version();
        functionBounced(String.format(Locale.CHINA, "robin version %s", version));

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
                            mIsPublisherStarted = true;
                            mPublisherStateText.setText("play".trim());
                        }else if (newState == RBManagerAndroid.RB_STATUS_STOP) {
                            mIsPublisherStarted = false;
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
                        String errorDesc = String.format(Locale.CHINA,"notice code %d, %s", noticeCode, desc);
                        mPublisherUrlText.setText(errorDesc);
                    }
                });
            }
            public void OnMediaModeChange(final int newMode) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (newMode == RBManagerAndroid.RB_MEDIA_ALL){
                            mPublisherUrlText.setText("change to media all".trim());
                        }else if (newMode == RBManagerAndroid.RB_MEDIA_AUDIO) {
                            mPublisherUrlText.setText("change to media audio".trim());
                        }else if (newMode == RBManagerAndroid.RB_MEDIA_VIDEO){
                            mPublisherUrlText.setText("change to media video".trim());
                        }
                    }
                });
            }
            public void OnVideoIdChange(final int newVideoId){
                runOnUiThread(new Runnable() {
                    public void run() {
                        String desc = String.format(Locale.CHINA,"change to camera %d", newVideoId);
                        mPublisherUrlText.setText(desc);
                        mPublisherVideoID = newVideoId;
                    }
                });
            }
        });

        mPlayer1 = mManager.createPlayer();
        res = mPlayer1.init();
        if (res < 0) {
            functionBounced(String.format(Locale.CHINA, "RBPlayer Init failed with error code %d", res));
        }

        mPlayer1.setPlayerListener(new RBPlayAndroid.PlayerListener() {
            public void OnStateChange(final int newState) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if(newState == RBManagerAndroid.RB_STATUS_PLAYING) {
                            mPlayerStateText1.setText("play".trim());
                        }else if (newState == RBManagerAndroid.RB_STATUS_PAUSING){
                            mPlayerStateText1.setText("pause".trim());
                        }else if (newState == RBManagerAndroid.RB_STATUS_STOP){
                            mPlayerStateText1.setText("stop".trim());
                        }
                    }
                });
            }
            public void OnErrorGet(final int errorCode, final String desc) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String errorDesc = String.format(Locale.CHINA,"error code %d, %s", errorCode, desc);
                        mPlayerUrlText1.setText(errorDesc);
                    }
                });
            }
            public void OnNoticeGet(final int noticeCode, final String desc) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String errorDesc = String.format(Locale.CHINA,"notice code %d, %s", noticeCode, desc);
                        mPlayerUrlText1.setText(errorDesc);
                    }
                });
            }
            public void OnMediaModeChange(final int newMode) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if(newMode == RBManagerAndroid.RB_MEDIA_ALL){
                            mPlayerUrlText1.setText("change to media all".trim());
                        }else if (newMode == RBManagerAndroid.RB_MEDIA_AUDIO) {
                            mPlayerUrlText1.setText("change to media audio".trim());
                        }else if (newMode == RBManagerAndroid.RB_MEDIA_VIDEO){
                            mPlayerUrlText1.setText("change to media video".trim());
                        }
                    }
                });
            }
            public void OnVideoResolution(int width, int height) {
                Log.i(TAG, String.format("video resolution : %dx%d", width, height));
            }
            public void OnBufferProcessing(int processPercentage) {
                Log.i(TAG, String.format("buffering : %d", processPercentage));
            }
        });

        getPublisherInfo();
        ConfigMediaMode();
    }

    void release()
    {
        releasePublisher(mManager, mPublisher);
        releasePlayer(mManager, mPlayer1);

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

    private void getPublisherInfo()
    {
        ArrayAdapter<String> adapterVideoCapture= new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
        ArrayAdapter<String> adapterAudioCapture= new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);

        long videoDeviceNum = mPublisher.getVideoDeviceNum();
        for (int i = 0; i < videoDeviceNum; i++) {
            TextureViewTest.VideoDeviceInfo info = new TextureViewTest.VideoDeviceInfo();
            info.mDescribe = mPublisher.getVideoDeviceDescribe(i);
            mPublisher.getSupportVideoResolution(i, info.mSizes, info.mSizesNum);

            mVideoDeviceInfos.put(i,info);
            adapterVideoCapture.add(info.mDescribe);
        }

        long audioDeviceNum = mPublisher.getAudioDeviceNum();
        for(int i = 0; i < audioDeviceNum; i++){
            TextureViewTest.AudioDeviceInfo info = new TextureViewTest.AudioDeviceInfo();
            info.mDescribe = mPublisher.getAudioDeviceDescribe(i);
            mPublisher.getSupportAudioSampleRate(i, info.mSampleRates, info.mSampleRatesNum);

            mAudioDeviceInfos.put(i,info);
            adapterAudioCapture.add(info.mDescribe);
        }

        spinnerConfigAdapter(mSpinnerVideoCapture, adapterVideoCapture);
        spinnerConfigAdapter(mSpinnerAudioCapture, adapterAudioCapture);
    }

    private void ConfigMediaMode()
    {
        SparseArray<String> mediaModes = new  SparseArray<>();
        ArrayAdapter<String> adapterMode = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);

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
            adapterMode.add(mediaModes.get(i));
        }

        spinnerConfigAdapter(mSpinnerPublishMediaMode, adapterMode);
        spinnerConfigAdapter(mSpinnerPlayer1MediaMode, adapterMode);
    }

    private void startPublish()
    {
        if (mPublisher == null){
            functionBounced("mPublisher is null");
            return;
        }

        mPublisher.setVideoDevice(mPublisherVideoID, mScreenOrientation);
        mPublisher.setAudioDevice(mPublisherAudioID);
        mPublisher.setVideoResolution(mPublisherWidth, mPublisherHeight);


        mPublisher.setVideoHardwareEnc(mPublisherHWEnc);
        mPublisher.setAudioAEC(mPublisherAecm);
        mPublisher.setUri(mPublisherUrlText.getText().toString().trim());

        mPublisher.start();
    }

    private void  stopPublish()
    {
        if (mPublisher == null){
            functionBounced("mPublisher is null");
            return;
        }

        mPublisher.stop();
        mPublisherUrlText.setText(mPublisherUri.trim());
    }

    private void functionBounced(CharSequence text)
    {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

    private TextWatcher mEditTextWatcher = new TextWatcher()
    {
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
                case R.id.check_player1_full_screen:
                    mPlayer1FullScreen = checked;
                    break;
                case R.id.check_player1_texture:
                    mPlayer1Texture = checked;
                    break;
                case R.id.check_publisher_hw_encoder:
                    mPublisherHWEnc = checked;
                    break;
                case R.id.check_publisher_aecm:
                    mPublisher.setAudioAEC(checked);
                    mPublisherAecm = checked;
                case R.id.check_publisher_texture:
                    mPublisherTexture = checked;
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
                    if (mPlayer1 != null) {
                        startPlayer(mPlayer1, mPlayerUrlText1.getText().toString().trim(), mPlayer1HWDec, mPlayer1FullScreen);
                    }
                    break;
                case  R.id.button_player1_stop:
                    if (mPlayer1 != null) {
                        stopPlayer(mPlayer1);
                        mPlayerUrlText1.setText(mPlayer1Uri.trim());
                    }
                    break;
                case R.id.button_publisher_play:
                    startPublish();
                    break;
                case R.id.button_publisher_stop:
                    stopPublish();
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

    private void startPlayer(RBPlayAndroid play_cur, String uri, boolean isUseHardware, boolean isFullScreen) {
        if (play_cur == null)
        {
            Log.e("RBPlayer","the give player handle is null");
            return;
        }
        play_cur.setUri(uri);
        play_cur.setVideoHardwareDec(isUseHardware);

        if (isFullScreen)
            play_cur.setVideoRenderMode(RBManagerAndroid.RB_RENDER_FULL_SCREEN);
        else
            play_cur.setVideoRenderMode(RBManagerAndroid.RB_RENDER_ASPECT_RATIO);

        play_cur.start();
    }

    private void stopPlayer(RBPlayAndroid play_cur)
    {
        if (play_cur!=null)
            play_cur.stop();
    }

    private void onChooseVideoCapture(int id)
    {
        if (!mIsPublisherStarted){
            TextureViewTest.VideoDeviceInfo info = mVideoDeviceInfos.get(id);
            if (info == null)
                return;

            ArrayAdapter<String> adapter= new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
            for (int i = 0; i< info.mSizesNum[0]; i++){
                adapter.add(String.format(Locale.CHINA, "%dx%d",info.mSizes[i].getWidth(), info.mSizes[i].getHeight()));
            }

            spinnerConfigAdapter(mSpinnerResolution, adapter);
        }

        if(mPublisher != null){
            mPublisher.setVideoDevice(id, mScreenOrientation);
        }

        mPublisherVideoID = id;
    }

    private void onChooseAudioCapture(int id)
    {
        TextureViewTest.AudioDeviceInfo info = mAudioDeviceInfos.get(id);
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
        TextureViewTest.VideoDeviceInfo info = mVideoDeviceInfos.get(mPublisherVideoID);
        if (info == null)
            return;

        RBSize size = info.mSizes[id];
        mPublisherWidth = size.getWidth();
        mPublisherHeight = size.getHeight();
    }

    private void onChooseSampleRate(int id)
    {
        TextureViewTest.AudioDeviceInfo info = mAudioDeviceInfos.get(mPublisherAudioID);
        if (info == null)
            return;

        mPublisherSampleRate = info.mSampleRates[id];
    }

    private  void onChoosePublishMediaMode(int id)
    {
        if (mPublisherTexture){
            if(id == RBManagerAndroid.RB_MEDIA_AUDIO) {
                mPublisher.setMediaModeByTextureView(id, null);
            }else{
                mPublisher.setMediaModeByTextureView(id, mPublisherTextureView);
            }
        }else{
            if(id == RBManagerAndroid.RB_MEDIA_AUDIO) {
                mPublisher.setMediaMode(id, null);
            }else{
                mPublisher.setMediaMode(id, mPublisherSurfaceView);
            }
        }
    }

    private  void onChoosePlayer1MediaMode(int id)
    {
        if (mPlayer1Texture){
            if(id == RBManagerAndroid.RB_MEDIA_AUDIO) {
                mPlayer1.setMediaModeByTextureView(id, null);
            }else{
                mPlayer1.setMediaModeByTextureView(id, mPlayer1TextureView);
            }
        }else{
            if(id == RBManagerAndroid.RB_MEDIA_AUDIO) {
                mPlayer1.setMediaMode(id, null);
            }else{
                mPlayer1.setMediaMode(id, mPlayer1SurfaceView);
            }
        }
    }

    @Override //SeekBar.OnSeekBarChangeListener
    public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
        //Log.d(TAG, "onProgressChanged " + sb.getProgress());
    }

    // The user started dragging the Seek Bar thumb
    public void onStartTrackingTouch(SeekBar sb) {
        //Log.d(TAG, "onStartTrackingTouch " + sb.getProgress());
    }

    // The user released the Seek Bar thumb
    public void onStopTrackingTouch(SeekBar sb) {
        //Log.d(TAG, "onStopTrackingTouch " + sb.getProgress());
        if (sb == mPlayer1VolumeControl) {
            mPlayer1VolumeDisplay.setText(String.valueOf(sb.getProgress()));
            if(mPlayer1 != null){
                mPlayer1.setVolume(sb.getProgress());
            }
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
        }else if (spinner == mSpinnerPlayer1MediaMode){
            onChoosePlayer1MediaMode(position);
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //Cog.e(TAG, "onConfigurationChanged");
        /*if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ) {
            mPublisher.setCamera(mPublisherCameraID, 3);
        } else if (this.getResources().getConfiguration().orientation  == Configuration.ORIENTATION_PORTRAIT) {
            mPublisher.setCamera(mPublisherCameraID, 1);
        }*/
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

    private class AlbumOrientationEventListener extends OrientationEventListener {
        public AlbumOrientationEventListener(Context context) {
            super(context);
        }

        public AlbumOrientationEventListener(Context context, int rate) {
            super(context, rate);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                return;
            }
            //手机平躺不调用
            //保证只返回四个方向
            int newOrientation = ((orientation + 45) / 90 * 90) % 360;
            int rbOrientation = RBManagerAndroid.RB_PORTRAIT;

            switch (newOrientation) {
                case 0:
                    rbOrientation = RBManagerAndroid.RB_PORTRAIT;
                    break;
                case 90:
                    rbOrientation = RBManagerAndroid.RB_LANDSCAPE_RIGHT;
                    break;
                case 180:
                    rbOrientation = RBManagerAndroid.RB_PORTRAIT_UPSIDE_DOWN;
                    break;
                case 270:
                    rbOrientation = RBManagerAndroid.RB_LANDSCAPE_LEFT;
                    break;
                default:
                    break;
            }

            if (rbOrientation != mScreenOrientation && mPublisher != null) {
                mPublisher.setVideoDevice(mPublisherVideoID, rbOrientation);
            }

            mScreenOrientation = rbOrientation;
        }
    }
}
