package com.codyy.release.activity;


import android.graphics.ImageFormat;
import android.hardware.Camera;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.codyy.release.R;
import com.codyy.robinsdk.impl.AarAudioCapture;
import com.codyy.robinsdk.impl.AhcVideoCapture;

import java.util.List;
import java.util.Locale;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

public class CameraAndAudioDemon extends AppCompatActivity implements AdapterView.OnItemSelectedListener{
    private static final String TAG = "CameraAndAudioDemon";
    private static final int SupportMaxSizeNum = 32;

    AhcVideoCapture mAhcVideoCapture = null;
    AarAudioCapture mAarAudioCapture = null;
    private SparseArray<CameraInfo> mArrayCameraInfo = null;
    private SparseArray<AudioDeviceInfo> mArrayAudioDeviceInfo = null;

    private SurfaceView mCameraSurfaceView = null;
    private TextureView mCameraTextureView = null;
    private int mCameraViewTypeFlag = 0;

    private Spinner mSpinnerCamera = null;
    private Spinner mSpinnerCameraFormat = null;
    private Spinner mSpinnerCameraPreviewSize = null;
    private Spinner mSpinnerCameraPictureSize = null;
    private Spinner mSpinnerAudio = null;
    private Spinner mSpinnerAudioSampleRate = null;
    private Spinner mSpinnerAudioChannel = null;
    private Spinner mSpinnerAudioFormat = null;

    private int mCameraID = -1;
    private int mCameraPreviewWidth = 640;
    private int mCameraPreviewHeight = 480;
    private int mCameraPictureWidth = 0;
    private int mCameraPictureHeight = 0;
    private int mCameraFormat = 0;

    private int mAudioID = -1;
    private int mAudioSampleRate = 16000;
    private int mAudioChannel = 1;
    private int mAudioFormat = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_and_audio_demon);

        mCameraSurfaceView = (SurfaceView) this.findViewById(R.id.Camera_SurfaceView);
        mCameraTextureView = (TextureView) this.findViewById(R.id.Camera_TextureView);
        //spinner
        mSpinnerCamera = (Spinner) this.findViewById(R.id.Camera_Name);
        mSpinnerCameraFormat = (Spinner) this.findViewById(R.id.Camera_Preview_Format);
        mSpinnerCameraPreviewSize = (Spinner) this.findViewById(R.id.Camera_Preview_Size);
        mSpinnerCameraPictureSize = (Spinner) this.findViewById(R.id.Camera_Picture_Size);

        mSpinnerAudio = (Spinner) this.findViewById(R.id.Audio_Name);
        mSpinnerAudioSampleRate = (Spinner) this.findViewById(R.id.Audio_Sample_Rate);
        mSpinnerAudioChannel = (Spinner) this.findViewById(R.id.Audio_Channel);
        mSpinnerAudioFormat = (Spinner) this.findViewById(R.id.Audio_Format);

        setButtonListener(R.id.Camera_Start);
        setButtonListener(R.id.Camera_Stop);
        setButtonListener(R.id.Camera_Snapshoot);
        setButtonListener(R.id.Camera_Change_Camea);
        setButtonListener(R.id.Audio_Start);
        setButtonListener(R.id.Audio_Stop);
    }

    public void onResume() {
        super.onResume();
        Init();
    }

    public void onPause(){
        super.onPause();
        Release();
    }

    private void Init()
    {
        mAhcVideoCapture = new AhcVideoCapture(0);
        mAarAudioCapture = new AarAudioCapture(0);
        mArrayCameraInfo = new SparseArray<>();
        mArrayAudioDeviceInfo = new SparseArray<>();

        ArrayAdapter<String> adapterCamera= new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
        ArrayAdapter<String> adapterAudio= new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);

        int cameraNum = AhcVideoCapture.getCameraNum();
        for (int i = 0; i < cameraNum; i++) {
            CameraInfo info = new CameraInfo();

            info.mDescribe = AhcVideoCapture.getCameraName(i);
            info.mPreviewSizes = AhcVideoCapture.getSupportPreviewResolution(i);
            info.mPictureSizes = AhcVideoCapture.getSupportPictureResolution(i);
            info.mFormats = AhcVideoCapture.getSupportFormat(i);

            mArrayCameraInfo.put(i, info);
            adapterCamera.add(info.mDescribe);
        }

        int audioDeviceNum = AarAudioCapture.getAudioDeviceNum();
        for (int i =0; i < audioDeviceNum; i++){
            AudioDeviceInfo info = new AudioDeviceInfo();

            info.mDescribe = AarAudioCapture.getAudioDeviceName(i);
            info.mSampleRates = AarAudioCapture.getSupportSampleRate(i);
            info.mChannels = AarAudioCapture.getSupportChannelConfig(i);
            info.mFormats = AarAudioCapture.getSupportAudioFormat(i);

            mArrayAudioDeviceInfo.put(i, info);
            adapterAudio.add(info.mDescribe);
        }

        SpinnerConfigAdapter(mSpinnerCamera, adapterCamera);
        SpinnerConfigAdapter(mSpinnerAudio, adapterAudio);
    }

    private void Release()
    {
        if(mAhcVideoCapture != null)
            mAhcVideoCapture.stop();

        if (mAarAudioCapture != null)
            mAarAudioCapture.stop();
    }

    private void SpinnerConfigAdapter(Spinner spinner, ArrayAdapter<String> adapter)
    {
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    private void FunctionBounced(CharSequence text)
    {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

    private View.OnClickListener mClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            switch (view.getId()){
                case  R.id.Camera_Change_Camea:
                    changeCamera();
                    break;
                case  R.id.Camera_Snapshoot:
                    takePicture();
                    break;
                case R.id.Camera_Start:
                    startCamera();
                    break;
                case R.id.Camera_Stop:
                    stopCamera();
                    break;
                case R.id.Audio_Start:
                    startAudio();
                    break;
                case R.id.Audio_Stop:
                    stopAudio();
                    break;
                default:
                    break;
            }
        }
    };

    private void startCamera()
    {
        if(mAhcVideoCapture == null){
            FunctionBounced("mAhcVideoCapture is null");
            return;
        };

        mAhcVideoCapture.setCamera(mCameraID);
        mAhcVideoCapture.setPreviewResolution(mCameraPreviewWidth, mCameraPreviewHeight);
        if(mCameraPictureHeight != 0 && mCameraPictureWidth != 0)
            mAhcVideoCapture.setPictureResolution(mCameraPictureWidth, mCameraPictureHeight);

        mAhcVideoCapture.setFormat(mCameraFormat);
        mAhcVideoCapture.setFrameRate(25);

        int orientation = 0;
        if (getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT) {
            orientation = 0;
        }else if (getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE) {
            orientation = 2;
        }

        mAhcVideoCapture.setScreenOrientation(orientation);

        if (mCameraViewTypeFlag % 2 == 0){
            mAhcVideoCapture.start(mCameraSurfaceView, 0);
        }else{
            mAhcVideoCapture.start(mCameraTextureView, 1);
        }
        mCameraViewTypeFlag++;

        mSpinnerCamera.setEnabled(false);
        mSpinnerCameraFormat.setEnabled(false);
        mSpinnerCameraPreviewSize.setEnabled(false);
        mSpinnerCameraPictureSize.setEnabled(false);
    }

    private void stopCamera()
    {
        if(mAhcVideoCapture == null){
            FunctionBounced("mAhcVideoCapture is null");
            return;
        };

        mAhcVideoCapture.stop();

        mSpinnerCamera.setEnabled(true);
        mSpinnerCameraFormat.setEnabled(true);
        mSpinnerCameraPreviewSize.setEnabled(true);
        mSpinnerCameraPictureSize.setEnabled(true);
    }

    private void changeCamera()
    {
        if(mAhcVideoCapture == null){
            FunctionBounced("mAhcVideoCapture is null");
            return;
        };

        if (mCameraID == 0) {
            mCameraID = 1;
        }else if(mCameraID == 1){
            mCameraID = 0;
        }else{
            mCameraID = -1;
        }

        if (mCameraID != -1) {
            stopCamera();
            startCamera();
        }
    }

    private void takePicture()
    {
        if(mAhcVideoCapture == null){
            FunctionBounced("mAhcVideoCapture is null");
            return;
        };

        mAhcVideoCapture.takePicture();
    }

    private void startAudio()
    {
        if(mAarAudioCapture == null){
            FunctionBounced("mAarAudioCapture is null");
            return;
        };

        mAarAudioCapture.setDevice(mAudioID);
        mAarAudioCapture.setSampleRate(mAudioSampleRate);
        mAarAudioCapture.setChannelConfig(mAudioChannel);
        mAarAudioCapture.setAudioFormat(mAudioFormat);

        mAarAudioCapture.start();

        mSpinnerAudio.setEnabled(false);
        mSpinnerAudioSampleRate.setEnabled(false);
        mSpinnerAudioChannel.setEnabled(false);
        mSpinnerAudioFormat.setEnabled(false);
    }

    private void stopAudio()
    {
        if(mAarAudioCapture == null){
            FunctionBounced("mAarAudioCapture is null");
            return;
        };

        mAarAudioCapture.stop();

        mSpinnerAudio.setEnabled(true);
        mSpinnerAudioSampleRate.setEnabled(true);
        mSpinnerAudioChannel.setEnabled(true);
        mSpinnerAudioFormat.setEnabled(true);
    }

    private void setButtonListener(int id)
    {
        ImageButton button;
        button = (ImageButton) this.findViewById(id);
        button.setOnClickListener(mClickListener);
    }

    private void onChooseCamera(int id)
    {
        CameraInfo info = mArrayCameraInfo.get(id);
        if (info == null)
            return;

        mCameraID = id;

        ArrayAdapter<String> adapterPreviewSizes = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
        for (int i = 0; i< info.mPreviewSizes.size(); i++){
            Camera.Size size = info.mPreviewSizes.get(i);
            adapterPreviewSizes.add(String.format(Locale.CHINA, "%dx%d",size.width, size.height));
        }

        ArrayAdapter<String> adapterPictureSizes = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
        for (int i = 0; i< info.mPictureSizes.size(); i++){
            Camera.Size size = info.mPictureSizes.get(i);
            adapterPictureSizes.add(String.format(Locale.CHINA, "%dx%d",size.width, size.height));
        }

        ArrayAdapter<String> adapterFormats = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
        for (int i = 0; i < info.mFormats.size(); i++){
            int format = info.mFormats.get(i);
            String formatStr;

            if (format == ImageFormat.NV21){
                formatStr = "NV21";
            }else if (format == ImageFormat.NV16){
                formatStr = "NV16";
            }else if (format == ImageFormat.YUY2){
                formatStr = "YUY2";
            }else if (format == ImageFormat.YV12){
                formatStr = "YV12";
            }else if (format == ImageFormat.RGB_565){
                formatStr = "RGB_565";
            }else {
                continue;
            }

            adapterFormats.add(formatStr);
        }

        SpinnerConfigAdapter(mSpinnerCameraPreviewSize, adapterPreviewSizes);
        SpinnerConfigAdapter(mSpinnerCameraPictureSize, adapterPictureSizes);
        SpinnerConfigAdapter(mSpinnerCameraFormat, adapterFormats);
    }

    private void onChooseCameraPreviewSize(int id)
    {
        CameraInfo info = mArrayCameraInfo.get(mCameraID);
        if (info == null)
            return;

        Camera.Size size = info.mPreviewSizes.get(id);
        mCameraPreviewWidth = size.width;
        mCameraPreviewHeight = size.height;
    }

    private void onChooseCameraPictureSize(int id)
    {
        CameraInfo info = mArrayCameraInfo.get(mCameraID);
        if (info == null)
            return;

        Camera.Size size = info.mPictureSizes.get(id);
        mCameraPictureWidth = size.width;
        mCameraPictureHeight = size.height;
    }

    private void onChooseCameraFormat(int id)
    {
        CameraInfo info = mArrayCameraInfo.get(mCameraID);
        if (info == null)
            return;

        mCameraFormat = info.mFormats.get(id);
    }

    private void onChooseAudioDevice(int id)
    {
        AudioDeviceInfo info = mArrayAudioDeviceInfo.get(id);
        if (info == null)
            return;

        mAudioID = id;

        ArrayAdapter<String> adapterSampleRate = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
        for (int i = 0; i< info.mSampleRates.size(); i++){
            int sampleRate = info.mSampleRates.get(i);
            adapterSampleRate.add(String.format(Locale.CHINA, "%d", sampleRate));
        }

        ArrayAdapter<String> adapterChannel = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
        for (int i = 0; i< info.mChannels.size(); i++){
            int channel = info.mChannels.get(i);
            String channelStr;

            if (channel == 1)
                channelStr = "Mono";
            else if(channel == 2)
                channelStr = "Stereo";
            else
                channelStr = "Unknown";

            adapterChannel.add(String.format(Locale.CHINA, "%s", channelStr));
        }

        ArrayAdapter<String> adapterFormat = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
        for (int i = 0; i< info.mFormats.size(); i++){
            int format = info.mFormats.get(i);
            String formatStr;

            if (format == 8)
                formatStr = "S8";
            else if(format == 16)
                formatStr = "S16";
            else
                formatStr = "Unknown";

            adapterFormat.add(String.format(Locale.CHINA, "%s", formatStr));
        }

        SpinnerConfigAdapter(mSpinnerAudioSampleRate, adapterSampleRate);
        SpinnerConfigAdapter(mSpinnerAudioChannel, adapterChannel);
        SpinnerConfigAdapter(mSpinnerAudioFormat, adapterFormat);
    }

    private void onChooseAudioSampleRate(int id)
    {
        AudioDeviceInfo info = mArrayAudioDeviceInfo.get(mAudioID);
        if (info == null)
            return;

        mAudioSampleRate = info.mSampleRates.get(id);
    }

    private void onChooseAudioChannel(int id)
    {
        AudioDeviceInfo info = mArrayAudioDeviceInfo.get(mAudioID);
        if (info == null)
            return;

        mAudioChannel = info.mChannels.get(id);
    }

    private void onChooseAudioFormat(int id)
    {
        AudioDeviceInfo info = mArrayAudioDeviceInfo.get(mAudioID);
        if (info == null)
            return;

        mAudioFormat = info.mFormats.get(id);
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
    {
        Spinner spinner = (Spinner) parent;
        int position = spinner.getSelectedItemPosition();
        if (spinner == mSpinnerCamera){
            onChooseCamera(position);
        }else if (spinner == mSpinnerCameraPreviewSize){
            onChooseCameraPreviewSize(position);
        } else if (spinner == mSpinnerCameraPictureSize){
            onChooseCameraPictureSize(position);
        }else if(spinner == mSpinnerCameraFormat){
            onChooseCameraFormat(position);
        }else if(spinner == mSpinnerAudio){
            onChooseAudioDevice(position);
        }else if(spinner == mSpinnerAudioSampleRate){
            onChooseAudioSampleRate(position);
        }else if(spinner == mSpinnerAudioChannel){
            onChooseAudioChannel(position);
        }else if(spinner == mSpinnerAudioFormat){
            onChooseAudioFormat(position);
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {}

    static private class CameraInfo
    {
        public String mDescribe;
        public List<Camera.Size> mPreviewSizes = null;
        public List<Camera.Size> mPictureSizes = null;
        public List<Integer> mFormats = null;
    }

    static private class AudioDeviceInfo
    {
        public String mDescribe;
        public List<Integer> mSampleRates = null;
        public List<Integer> mChannels = null;
        public List<Integer> mFormats = null;
    }
}
