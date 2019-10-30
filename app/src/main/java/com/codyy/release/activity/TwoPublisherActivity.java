package com.codyy.release.activity;

import androidx.appcompat.app.AppCompatActivity;
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
import com.codyy.robinsdk.RBPublishAndroid;
import com.codyy.robinsdk.RBSize;
import com.codyy.robinsdk.impl.RBManagerAndroid;

import java.util.Locale;

public class TwoPublisherActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private static final String TAG = "PublisherAndPlay";
    private static final int SupportMaxSizeNum = 32;
    private static final int SupportMaxSampleRateNum = 16;

    private int mScreenOrientation = RBManagerAndroid.RB_LANDSCAPE_LEFT;
    ;
    private int mCurFocusID = 0;
    private RBManagerAndroid mManager = null;
    private RBPublishAndroid mPublisher1 = null;
    private RBPublishAndroid mPublisher2 = null;

    private SurfaceView mPublisher1SurfaceView = null;
    private SurfaceView mPublisher2SurfaceView = null;

    private EditText mPublisher1UrlText = null;
    private EditText mPublisher2UrlText = null;

    private TextView mPublisher1StateText = null;
    private TextView mPublisher2StateText = null;


    private Spinner mSpinnerVideoCapture1 = null;
    private Spinner mSpinnerAudioCapture1 = null;
    private Spinner mSpinnerResolution1 = null;
    private Spinner mSpinnerSampleRate1 = null;
    private Spinner mSpinnerPublishMediaMode1 = null;

    private Spinner mSpinnerVideoCapture2 = null;
    private Spinner mSpinnerAudioCapture2 = null;
    private Spinner mSpinnerResolution2 = null;
    private Spinner mSpinnerSampleRate2 = null;
    private Spinner mSpinnerPublishMediaMode2 = null;

    //publisher1 config
    private String mPublisher1Uri = "rtmp://10.5.31.218:1935/dms/LocalDirector";
    private int mPublisher1VideoID = 0;
    private int mPublisher1AudioID = 0;
    private int mPublisher1Width = 640;
    private int mPublisher1Height = 480;
    private int mPublisher1SampleRate = 16000;
    private boolean mPublisher1HWEnc = true;
    private boolean mPublisher1Aecm = true;
    private boolean mIsPublisher1Started = false;

    //publisher1 config
    private String mPublisher2Uri = "rtmp://10.5.31.218:1935/dms/LocalDirectorq";
    private int mPublisher2VideoID = 0;
    private int mPublisher2AudioID = 0;
    private int mPublisher2Width = 640;
    private int mPublisher2Height = 480;
    private int mPublisher2SampleRate = 16000;
    private boolean mPublisher2HWEnc = true;
    private boolean mPublisher2Aecm = true;
    private boolean mIsPublisher2Started = false;

    private SparseArray<VideoDeviceInfo> mVideoDeviceInfos = null;
    private SparseArray<AudioDeviceInfo> mAudioDeviceInfos = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two_publisher);

        //surface
        mPublisher1SurfaceView = (SurfaceView) findViewById(R.id.surfaceView_publisher1);
        mPublisher2SurfaceView = (SurfaceView) findViewById(R.id.surfaceView_publisher2);

        //editText
        mPublisher1UrlText = (EditText) findViewById(R.id.url_publisher1);
        mPublisher2UrlText = (EditText) findViewById(R.id.url_publisher2);

        mPublisher1UrlText.setOnFocusChangeListener(mFocusChangeListen);
        mPublisher2UrlText.setOnFocusChangeListener(mFocusChangeListen);

        mPublisher1UrlText.addTextChangedListener(mEditTextWatcher);
        mPublisher2UrlText.addTextChangedListener(mEditTextWatcher);

        mPublisher1UrlText.setText(mPublisher1Uri.trim());
        mPublisher2UrlText.setText(mPublisher2Uri.trim());
        //textView

        mPublisher1StateText = (TextView) findViewById(R.id.state_publisher1);
        mPublisher2StateText = (TextView) findViewById(R.id.state_publisher2);
        //spinner
        mSpinnerVideoCapture1 = (Spinner) this.findViewById(R.id.spinner_video_capture1);
        mSpinnerAudioCapture1 = (Spinner) this.findViewById(R.id.spinner_audio_capture1);
        mSpinnerResolution1 = (Spinner) this.findViewById(R.id.spinner_video_resolution1);
        mSpinnerSampleRate1 = (Spinner) this.findViewById(R.id.spinner_audio_samplerate1);
        mSpinnerPublishMediaMode1 = (Spinner) this.findViewById(R.id.spinner_publish_media_mode1);

        mSpinnerVideoCapture2 = (Spinner) this.findViewById(R.id.spinner_video_capture2);
        mSpinnerAudioCapture2 = (Spinner) this.findViewById(R.id.spinner_audio_capture2);
        mSpinnerResolution2 = (Spinner) this.findViewById(R.id.spinner_video_resolution2);
        mSpinnerSampleRate2 = (Spinner) this.findViewById(R.id.spinner_audio_samplerate2);
        mSpinnerPublishMediaMode2 = (Spinner) this.findViewById(R.id.spinner_publish_media_mode2);

        //checkbox
        setCheckBoxListener(R.id.check_publisher_hw_encoder1);
        setCheckBoxListener(R.id.check_publisher_aecm1);
        setCheckBoxListener(R.id.check_publisher_hw_encoder2);
        setCheckBoxListener(R.id.check_publisher_aecm2);
        //button
        setButtonListener(R.id.button_publisher_play1);
        setButtonListener(R.id.button_publisher_stop1);
        setButtonListener(R.id.button_publisher_play2);
        setButtonListener(R.id.button_publisher_stop2);

        //Create players and publisher, and get info
        init();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    public void onPause() {
        super.onPause();
        release();
    }

    private void init() {
        long res = 0;
        mVideoDeviceInfos = new SparseArray<>();
        mAudioDeviceInfos = new SparseArray<>();

        mManager = RBManagerAndroid.getSingleton();
        mManager.enableLog();
        res = mManager.init();
        if (res < 0) {
            functionBounced(String.format(Locale.CHINA, "RBManager Init failed with %d", res));
        }
        String version = mManager.version();
        functionBounced(String.format(Locale.CHINA, "robin version %s", version));

        mPublisher1 = mManager.createPublisher();
        res = mPublisher1.init();
        if (res < 0) {
            functionBounced(String.format(Locale.CHINA, "RBPublisher Init failed with %d", res));
        }

        mPublisher1.setPublisherListener(new RBPublishAndroid.PublisherListener() {
            public void OnStateChange(final int newState) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (newState == RBManagerAndroid.RB_STATUS_PLAYING) {
                            mIsPublisher1Started = true;
                            mPublisher1StateText.setText("play".trim());
                        } else if (newState == RBManagerAndroid.RB_STATUS_STOP) {
                            mIsPublisher1Started = false;
                            mPublisher1StateText.setText("stop".trim());
                        }
                    }
                });
            }

            public void OnErrorGet(final int errorCode, final String desc) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String errorDesc = String.format(Locale.CHINA, "error code %d, %s", errorCode, desc);
                        mPublisher1UrlText.setText(errorDesc);
                    }
                });
            }

            public void OnNoticeGet(final int noticeCode, final String desc) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String errorDesc = String.format(Locale.CHINA, "notice code %d, %s", noticeCode, desc);
                        mPublisher1UrlText.setText(errorDesc);
                    }
                });
            }

            public void OnMediaModeChange(final int newMode) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (newMode == RBManagerAndroid.RB_MEDIA_ALL) {
                            mPublisher1UrlText.setText("change to media all".trim());
                        } else if (newMode == RBManagerAndroid.RB_MEDIA_AUDIO) {
                            mPublisher1UrlText.setText("change to media audio".trim());
                        } else if (newMode == RBManagerAndroid.RB_MEDIA_VIDEO) {
                            mPublisher1UrlText.setText("change to media video".trim());
                        }
                    }
                });
            }

            public void OnVideoIdChange(final int newVideoId) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String desc = String.format(Locale.CHINA, "change to camera %d", newVideoId);
                        mPublisher1UrlText.setText(desc);
                        mPublisher1VideoID = newVideoId;
                    }
                });
            }
        });

        mPublisher2 = mManager.createPublisher();
        res = mPublisher2.init();
        if (res < 0) {
            functionBounced(String.format(Locale.CHINA, "RBPublisher Init failed with %d", res));
        }

        mPublisher2.setPublisherListener(new RBPublishAndroid.PublisherListener() {
            public void OnStateChange(final int newState) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (newState == RBManagerAndroid.RB_STATUS_PLAYING) {
                            mIsPublisher2Started = true;
                            mPublisher2StateText.setText("play".trim());
                        } else if (newState == RBManagerAndroid.RB_STATUS_STOP) {
                            mIsPublisher2Started = false;
                            mPublisher2StateText.setText("stop".trim());
                        }
                    }
                });
            }

            public void OnErrorGet(final int errorCode, final String desc) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String errorDesc = String.format(Locale.CHINA, "error code %d, %s", errorCode, desc);
                        mPublisher2UrlText.setText(errorDesc);
                    }
                });
            }

            public void OnNoticeGet(final int noticeCode, final String desc) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String errorDesc = String.format(Locale.CHINA, "notice code %d, %s", noticeCode, desc);
                        mPublisher2UrlText.setText(errorDesc);
                    }
                });
            }

            public void OnMediaModeChange(final int newMode) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (newMode == RBManagerAndroid.RB_MEDIA_ALL) {
                            mPublisher2UrlText.setText("change to media all".trim());
                        } else if (newMode == RBManagerAndroid.RB_MEDIA_AUDIO) {
                            mPublisher2UrlText.setText("change to media audio".trim());
                        } else if (newMode == RBManagerAndroid.RB_MEDIA_VIDEO) {
                            mPublisher2UrlText.setText("change to media video".trim());
                        }
                    }
                });
            }

            public void OnVideoIdChange(final int newVideoId) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String desc = String.format(Locale.CHINA, "change to camera %d", newVideoId);
                        mPublisher2UrlText.setText(desc);
                        mPublisher2VideoID = newVideoId;
                    }
                });
            }
        });

        getPublisherInfo();
        ConfigMediaMode();
    }

    void release() {
        releasePublisher(mManager, mPublisher1);
        releasePublisher(mManager, mPublisher2);

        mManager.release();
    }

    private void releasePublisher(RBManagerAndroid manager, RBPublishAndroid publisher) {
        try {
            if (manager != null && publisher != null) {
                publisher.stop();
                while (publisher.release() != 0) {
                    Thread.sleep(10);
                }
                manager.deletePublisher(publisher);
            }
        } catch (InterruptedException ex) {
            Log.e(TAG, "releasePublisher: Interrupted Exception is " + ex);
        }
    }

    private void spinnerConfigAdapter(Spinner spinner, ArrayAdapter<String> adapter) {
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    private void getPublisherInfo() {
        ArrayAdapter<String> adapterVideoCapture = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        ArrayAdapter<String> adapterAudioCapture = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);

        long videoDeviceNum = mPublisher1.getVideoDeviceNum();
        for (int i = 0; i < videoDeviceNum; i++) {
            TwoPublisherActivity.VideoDeviceInfo info = new TwoPublisherActivity.VideoDeviceInfo();
            info.mDescribe = mPublisher1.getVideoDeviceDescribe(i);
            mPublisher1.getSupportVideoResolution(i, info.mSizes, info.mSizesNum);

            mVideoDeviceInfos.put(i, info);
            adapterVideoCapture.add(info.mDescribe);
        }

        long audioDeviceNum = mPublisher1.getAudioDeviceNum();
        for (int i = 0; i < audioDeviceNum; i++) {
            TwoPublisherActivity.AudioDeviceInfo info = new TwoPublisherActivity.AudioDeviceInfo();
            info.mDescribe = mPublisher1.getAudioDeviceDescribe(i);
            mPublisher1.getSupportAudioSampleRate(i, info.mSampleRates, info.mSampleRatesNum);

            mAudioDeviceInfos.put(i, info);
            adapterAudioCapture.add(info.mDescribe);
        }

        spinnerConfigAdapter(mSpinnerVideoCapture1, adapterVideoCapture);
        spinnerConfigAdapter(mSpinnerAudioCapture1, adapterAudioCapture);
        spinnerConfigAdapter(mSpinnerVideoCapture2, adapterVideoCapture);
        spinnerConfigAdapter(mSpinnerAudioCapture2, adapterAudioCapture);
    }

    private void ConfigMediaMode() {
        SparseArray<String> mediaModes = new SparseArray<>();
        ArrayAdapter<String> adapterMode = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);

        for (int i = 0; i <= RBManagerAndroid.RB_MEDIA_AUDIO; i++) {
            if (i == RBManagerAndroid.RB_MEDIA_ALL) {
                mediaModes.put(i, "MediaAll");
            } else if (i == RBManagerAndroid.RB_MEDIA_VIDEO) {
                mediaModes.put(i, "OnlyVideo");
            } else if (i == RBManagerAndroid.RB_MEDIA_AUDIO) {
                mediaModes.put(i, "OnlyAudio");
            }
        }

        for (int i = 0; i < mediaModes.size(); i++) {
            adapterMode.add(mediaModes.get(i));
        }

        spinnerConfigAdapter(mSpinnerPublishMediaMode1, adapterMode);
        spinnerConfigAdapter(mSpinnerPublishMediaMode2, adapterMode);
    }

    private void startPublish1() {
        if (mPublisher1 == null) {
            functionBounced("mPublisher is null");
            return;
        }

        mPublisher1.setVideoDevice(mPublisher1VideoID, mScreenOrientation);
        mPublisher1.setVideoFrameRate(30);
        mPublisher1.setAudioDevice(mPublisher1AudioID);
        mPublisher1.setVideoResolution(mPublisher1Width, mPublisher1Height);
        mPublisher1.setVideoHardwareEnc(true);
        mPublisher1.setAudioAEC(mPublisher1Aecm);
        mPublisher1.setUri(mPublisher1UrlText.getText().toString().trim());
        mPublisher1.setVideoBitRate(8192);
        mPublisher1.start();
    }

    private void stopPublish1() {
        if (mPublisher1 == null) {
            functionBounced("mPublisher is null");
            return;
        }

        mPublisher1.stop();
        mPublisher1UrlText.setText(mPublisher1Uri.trim());
    }

    private void startPublish2() {
        if (mPublisher2 == null) {
            functionBounced("mPublisher is null");
            return;
        }

        mPublisher2.setVideoDevice(mPublisher2VideoID, mScreenOrientation);
        mPublisher2.setAudioDevice(mPublisher2AudioID);
        mPublisher2.setVideoResolution(mPublisher2Width, mPublisher2Height);


        mPublisher2.setVideoHardwareEnc(mPublisher2HWEnc);
        mPublisher2.setAudioAEC(mPublisher2Aecm);
        mPublisher2.setUri(mPublisher2UrlText.getText().toString().trim());

        mPublisher2.start();
    }

    private void stopPublish2() {
        if (mPublisher2 == null) {
            functionBounced("mPublisher is null");
            return;
        }

        mPublisher2.stop();
        mPublisher2UrlText.setText(mPublisher2Uri.trim());
    }

    private void functionBounced(CharSequence text) {
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
            if (tmp.equals("rtmp://")) {
                if (mCurFocusID == R.id.url_publisher1) {
                    mPublisher1Uri = s.toString();
                } else if (mCurFocusID == R.id.url_publisher2) {
                    mPublisher2Uri = s.toString();
                }
            }
        }
    };

    private View.OnFocusChangeListener mFocusChangeListen = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                mCurFocusID = v.getId();
            }
        }
    };

    private CompoundButton.OnCheckedChangeListener mCheckBoxerListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton view, boolean checked) {
            switch (view.getId()) {
                case R.id.check_publisher_hw_encoder1:
                    mPublisher1HWEnc = checked;
                    break;
                case R.id.check_publisher_aecm1:
                    mPublisher1.setAudioAEC(checked);
                    mPublisher1Aecm = checked;
                    break;
                case R.id.check_publisher_hw_encoder2:
                    mPublisher2HWEnc = checked;
                    break;
                case R.id.check_publisher_aecm2:
                    mPublisher2.setAudioAEC(checked);
                    mPublisher2Aecm = checked;
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
            switch (view.getId()) {

                case R.id.button_publisher_play1:
                    startPublish1();
                    break;
                case R.id.button_publisher_stop1:
                    stopPublish1();
                    break;
                case R.id.button_publisher_play2:
                    startPublish2();
                    break;
                case R.id.button_publisher_stop2:
                    stopPublish2();
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

    private void onChooseVideoCapture1(int id) {
        if (!mIsPublisher1Started) {
            TwoPublisherActivity.VideoDeviceInfo info = mVideoDeviceInfos.get(id);
            if (info == null)
                return;

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
            for (int i = 0; i < info.mSizesNum[0]; i++) {
                adapter.add(String.format(Locale.CHINA, "%dx%d", info.mSizes[i].getWidth(), info.mSizes[i].getHeight()));
            }

            spinnerConfigAdapter(mSpinnerResolution1, adapter);
        }

        if (mPublisher1 != null) {
            mPublisher1.setVideoDevice(id, mScreenOrientation);
        }

        mPublisher1VideoID = id;
    }

    private void onChooseAudioCapture1(int id) {
        TwoPublisherActivity.AudioDeviceInfo info = mAudioDeviceInfos.get(id);
        if (info == null)
            return;

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        for (int i = 0; i < info.mSampleRatesNum[0]; i++) {
            adapter.add(String.format(Locale.CHINA, "%d", info.mSampleRates[i]));
        }

        spinnerConfigAdapter(mSpinnerSampleRate1, adapter);

        if (mPublisher1 != null) {
            mPublisher1.setAudioDevice(id);
        }

        mPublisher1AudioID = id;
    }

    private void onChooseResolution1(int id) {
        TwoPublisherActivity.VideoDeviceInfo info = mVideoDeviceInfos.get(mPublisher1VideoID);
        if (info == null)
            return;

        RBSize size = info.mSizes[id];
        mPublisher1Width = size.getWidth();
        mPublisher1Height = size.getHeight();
    }

    private void onChooseSampleRate1(int id) {
        TwoPublisherActivity.AudioDeviceInfo info = mAudioDeviceInfos.get(mPublisher1AudioID);
        if (info == null)
            return;

        mPublisher1SampleRate = info.mSampleRates[id];
    }

    private void onChoosePublishMediaMode1(int id) {
        if (id == RBManagerAndroid.RB_MEDIA_AUDIO) {
            mPublisher1.setMediaMode(id, null);
        } else {
            mPublisher1.setMediaMode(id, mPublisher1SurfaceView);
        }
    }

    private void onChooseVideoCapture2(int id) {
        if (!mIsPublisher2Started) {
            TwoPublisherActivity.VideoDeviceInfo info = mVideoDeviceInfos.get(id);
            if (info == null)
                return;

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
            for (int i = 0; i < info.mSizesNum[0]; i++) {
                adapter.add(String.format(Locale.CHINA, "%dx%d", info.mSizes[i].getWidth(), info.mSizes[i].getHeight()));
            }

            spinnerConfigAdapter(mSpinnerResolution2, adapter);
        }

        if (mPublisher2 != null) {
            mPublisher2.setVideoDevice(id, mScreenOrientation);
        }

        mPublisher2VideoID = id;
    }

    private void onChooseAudioCapture2(int id) {
        TwoPublisherActivity.AudioDeviceInfo info = mAudioDeviceInfos.get(id);
        if (info == null)
            return;

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        for (int i = 0; i < info.mSampleRatesNum[0]; i++) {
            adapter.add(String.format(Locale.CHINA, "%d", info.mSampleRates[i]));
        }

        spinnerConfigAdapter(mSpinnerSampleRate1, adapter);

        if (mPublisher2 != null) {
            mPublisher2.setAudioDevice(id);
        }

        mPublisher2AudioID = id;
    }

    private void onChooseResolution2(int id) {
        TwoPublisherActivity.VideoDeviceInfo info = mVideoDeviceInfos.get(mPublisher2VideoID);
        if (info == null)
            return;

        RBSize size = info.mSizes[id];
        mPublisher2Width = size.getWidth();
        mPublisher2Height = size.getHeight();
    }

    private void onChooseSampleRate2(int id) {
        TwoPublisherActivity.AudioDeviceInfo info = mAudioDeviceInfos.get(mPublisher2AudioID);
        if (info == null)
            return;

        mPublisher2SampleRate = info.mSampleRates[id];
    }

    private void onChoosePublishMediaMode2(int id) {
        if (id == RBManagerAndroid.RB_MEDIA_AUDIO) {
            mPublisher2.setMediaMode(id, null);
        } else {
            mPublisher2.setMediaMode(id, mPublisher2SurfaceView);
        }
    }

    @Override // spinner selected
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Spinner spinner = (Spinner) parent;

        TextView tv = (TextView) view;
        tv.setTextSize(12.0f);    //设置大小

        int position = spinner.getSelectedItemPosition();
        if (spinner == mSpinnerVideoCapture1) {
            onChooseVideoCapture1(position);
        } else if (spinner == mSpinnerAudioCapture1) {
            onChooseAudioCapture1(position);
        } else if (spinner == mSpinnerResolution1) {
            onChooseResolution1(position);
        } else if (spinner == mSpinnerSampleRate1) {
            onChooseSampleRate1(position);
        } else if (spinner == mSpinnerPublishMediaMode1) {
            onChoosePublishMediaMode1(position);
        } else if (spinner == mSpinnerVideoCapture2) {
            onChooseVideoCapture2(position);
        } else if (spinner == mSpinnerAudioCapture2) {
            onChooseAudioCapture2(position);
        } else if (spinner == mSpinnerResolution2) {
            onChooseResolution2(position);
        } else if (spinner == mSpinnerSampleRate2) {
            onChooseSampleRate2(position);
        } else if (spinner == mSpinnerPublishMediaMode2) {
            onChoosePublishMediaMode2(position);
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
    }

    //Inter class
    static private class VideoDeviceInfo {
        private String mDescribe;
        private RBSize[] mSizes;
        private int[] mSizesNum;

        VideoDeviceInfo() {
            mSizes = new RBSize[SupportMaxSizeNum];
            for (int i = 0; i < SupportMaxSizeNum; i++) {
                mSizes[i] = new RBSize();
            }

            mSizesNum = new int[1];
        }
    }

    static private class AudioDeviceInfo {
        private String mDescribe;
        private int[] mSampleRates;
        private int[] mSampleRatesNum;

        AudioDeviceInfo() {
            mSampleRates = new int[SupportMaxSampleRateNum];
            mSampleRatesNum = new int[1];
        }
    }
}
