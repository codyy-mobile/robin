package com.codyy.robinsdk.impl;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import androidx.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by liuhao on 2016/9/28.
 */
public class AarAudioCapture {
    private final boolean isDebugDump = false;
    private final static String TAG = "AarAudioCapture";
    private static final boolean VERBOSE = false;
    private final static int curSupportDeviceNum = 1;//just support MIC device
    private final static int maxSupportDeviceNum = MediaRecorder.AudioSource.VOICE_COMMUNICATION -1;// count without DEFAULT

    private static Map<Integer, AudioRecordInfo> mAudioRecordInfos = new HashMap<>();
    private AudioRecord mAudioRecord = null;
    private Thread mThread = null;

    private int mAudioSource = MediaRecorder.AudioSource.MIC;
    private int mSampleRate = 16000;
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;

    private boolean mStarted = false;
    private ByteBuffer mBufferRec = null;
    private int mBufferOffset = 0;
    private int mToReadLen = 0;

    private long mUserData = 0;

    public static native void robin_audio_record_on_sample(byte[] data, int length, long userData);

    public AarAudioCapture(long userData)
    {
        mUserData = userData;
        checkAudioRecordInfo();
    }

    private static class AudioRecordInfo
    {
        AudioRecord mAudioRecord = null; //for detection parameter
        private List<Integer> mSupportSampleRates = null;
        private List<Integer> mSupportAudioFormats = null;
        private List<Integer> mSupportChannelConfigs = null;

        AudioRecordInfo()
        {
            mSupportSampleRates = new ArrayList<>();
            mSupportAudioFormats = new ArrayList<>();
            mSupportChannelConfigs = new ArrayList<>();
        }

        void Init(int audioSource)
        {
            int bufferSize;
            int audioFormatEnum = 0;
            int channelConfigEnum = 0;

            for (int sampleRate : new int[] {8000, 11025, 16000, 22050, 32000, 44100, 48000}) {
                for (int audioFormat : new short[] {8, 16}){
                    for (int channelConfig : new short[] {1, 2}){

                        if(audioFormat == 8){
                            audioFormatEnum = AudioFormat.ENCODING_PCM_8BIT;
                        }else if(audioFormat == 16){
                            audioFormatEnum = AudioFormat.ENCODING_PCM_16BIT;
                        }

                        if(channelConfig == 1){
                            channelConfigEnum = AudioFormat.CHANNEL_IN_MONO;
                        }else if(channelConfig == 2){
                            channelConfigEnum = AudioFormat.CHANNEL_IN_STEREO;
                        }

                        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfigEnum, audioFormatEnum);
                        if (bufferSize <= 0) {
                            continue;
                        }

                        if (mAudioRecord != null) {
                            mAudioRecord.release();
                            mAudioRecord = null;
                        }

                        try{
                            mAudioRecord = new AudioRecord(audioSource,
                                                            sampleRate,
                                                            channelConfigEnum,
                                                            audioFormatEnum,
                                                            bufferSize);

                        }catch (Exception ex){
                            continue;
                        }

                        if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                            continue;
                        }

                        if (!mSupportSampleRates.contains(sampleRate))
                            mSupportSampleRates.add(sampleRate);

                        if (!mSupportAudioFormats.contains(audioFormat))
                            mSupportAudioFormats.add(audioFormat);

                        if (!mSupportChannelConfigs.contains(channelConfig))
                            mSupportChannelConfigs.add(channelConfig);

                        mAudioRecord.release();
                        mAudioRecord = null;
                    }
                }
            }
        }
    }

    private static void checkAudioRecordInfo()
    {
        synchronized (AarAudioCapture.class){
            if (mAudioRecordInfos.size() == 0){
                for(int i =0; i<curSupportDeviceNum; i++){
                    AudioRecordInfo info = new AudioRecordInfo();
                    info.Init(i + 1);
                    mAudioRecordInfos.put(i + 1,info);
                }
            }
        }
    }

    public static int getAudioDeviceNum()
    {
        checkAudioRecordInfo();

        return mAudioRecordInfos.size();
    }

    public static String getAudioDeviceName(int deviceID)
    {
        if (deviceID > mAudioRecordInfos.size() -1 || deviceID < 0){
            Log.e(TAG, "getAudioDeviceName  : failed with error device index " + deviceID);
            return null;
        }

        String name = null;

        switch (deviceID){
            case 0:
                name = "MIC";
                break;
            case 1:
                name = "VOICE_UPLINK";
                break;
            case 2:
                name = "VOICE_DOWNLINK";
                break;
            case 3:
                name = "VOICE_CALL";
                break;
            case 4:
                name = "CAMCORDER";
                break;
            case 5:
                name = "VOICE_RECOGNITION";
                break;
            case 6:
                name = "VOICE_COMMUNICATION";
                break;
            default:
                break;
        }

        return name;
    }

    public static List<Integer> getSupportSampleRate(int deviceID)
    {
        if (deviceID > mAudioRecordInfos.size() -1 || deviceID < 0){
            Log.e(TAG, "getSupportSampleRate : failed with error device index " + deviceID);
            return null;
        }

        int audioSource = deviceID + 1;
        AudioRecordInfo info;
        List<Integer> supportSampleRates = null;

        info = mAudioRecordInfos.get(audioSource);
        if(info != null){
            supportSampleRates = info.mSupportSampleRates;
        }

        return supportSampleRates;
    }

    public static List<Integer> getSupportAudioFormat(int deviceID)
    {
        if (deviceID > mAudioRecordInfos.size() -1 || deviceID < 0){
            Log.e(TAG, "getSupportAudioFormat : failed with error device index " + deviceID);
            return null;
        }

        int audioSource = deviceID + 1;
        AudioRecordInfo info;
        List<Integer> supportAudioFormats = null;

        info = mAudioRecordInfos.get(audioSource);
        if(info != null){
            supportAudioFormats = info.mSupportAudioFormats;
        }

        return supportAudioFormats;
    }

    public static List<Integer> getSupportChannelConfig(int deviceID)
    {
        if (deviceID > mAudioRecordInfos.size() -1 || deviceID < 0){
            Log.e(TAG, "getSupportChannelConfig : failed with error device index " + deviceID);
            return null;
        }

        int audioSource = deviceID + 1;
        AudioRecordInfo info;
        List<Integer> supportChannelConfigs = null;

        info = mAudioRecordInfos.get(audioSource);
        if(info != null){
            supportChannelConfigs = info.mSupportChannelConfigs;
        }

        return supportChannelConfigs;
    }

    public synchronized int setDevice(int deviceID)
    {
        if (VERBOSE)Log.d(TAG, "setDevice : device index is " + deviceID);

        if (deviceID > mAudioRecordInfos.size() -1 || deviceID < 0){
            Log.e(TAG, "setDevice : failed with error device index " + deviceID);
            return -1;
        }

        switch (deviceID){
            case 0:
                mAudioSource = MediaRecorder.AudioSource.MIC;
                break;
            case 1:
                mAudioSource = MediaRecorder.AudioSource.VOICE_UPLINK;
                break;
            case 2:
                mAudioSource = MediaRecorder.AudioSource.VOICE_DOWNLINK;
                break;
            case 3:
                mAudioSource = MediaRecorder.AudioSource.VOICE_CALL;
                break;
            case 4:
                mAudioSource = MediaRecorder.AudioSource.CAMCORDER;
                break;
            case 5:
                mAudioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION;
                break;
            case 6:
                mAudioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
                break;
            default:
                mAudioSource = MediaRecorder.AudioSource.DEFAULT;
                return -1;
        }

        return 0;
    }

    public synchronized int setSampleRate(int sampleRate)
    {
        if (VERBOSE)Log.d(TAG, "setSampleRate : sample rate is " + sampleRate);

        AudioRecordInfo info = mAudioRecordInfos.get(mAudioSource);
        if(info != null){
            for(Integer entry:info.mSupportSampleRates){
                if (sampleRate == entry){
                    mSampleRate = sampleRate;
                    return 0;
                }
            }
        }else{
            Log.e(TAG, "setSampleRate : get audio info failed, the audio source is " + mAudioSource);
            return -1;
        }

        Log.e(TAG, "setSampleRate : the device don't support sample rate " + sampleRate);
        return -1;
    }

    public synchronized int setAudioFormat(int audioFormat)
    {
        if (VERBOSE)Log.d(TAG, "setAudioFormat : audio format is " + audioFormat);

        if(audioFormat != 8 && audioFormat != 16){
            Log.e(TAG, "setAudioFormat : Error audio format");
            return -1;
        }

        AudioRecordInfo info = mAudioRecordInfos.get(mAudioSource);
        if(info != null){
            for(Integer entry:info.mSupportAudioFormats){
                if (audioFormat == entry){
                    if (audioFormat == 8){
                        mAudioFormat = AudioFormat.ENCODING_PCM_8BIT;
                    }else if(audioFormat == 16){
                        mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
                    }
                    return 0;
                }
            }
        }else{
            Log.e(TAG, "setAudioFormat : get audio info failed, the audio source is " + mAudioSource);
            return -1;
        }

        Log.e(TAG, "setAudioFormat : the device don't support audio format " + audioFormat);
        return -1;
    }

    public synchronized int setChannelConfig(int channelConfig)
    {
        if (VERBOSE)Log.d(TAG, "setChannelConfig : channel config is " + channelConfig);

        if(channelConfig != 1 && channelConfig != 2){
            Log.e(TAG, "setChannelConfig : error channel config");
            return -1;
        }

        AudioRecordInfo info = mAudioRecordInfos.get(mAudioSource);
        if(info != null){
            for(Integer entry:info.mSupportChannelConfigs){
                if (channelConfig == entry){
                    if(channelConfig == 1){
                        mChannelConfig = AudioFormat.CHANNEL_IN_MONO;
                    }else if(channelConfig == 2){
                        mChannelConfig = AudioFormat.CHANNEL_IN_STEREO;
                    }
                    return 0;
                }
            }
        }else{
            Log.e(TAG, "setChannelConfig : get audio info failed, the audio source is " + mAudioSource);
            return -1;
        }

        Log.e(TAG, "setChannelConfig : the device don't support channel " + channelConfig);
        return -1;
    }

    public synchronized int start()
    {
        if (VERBOSE)Log.d(TAG, "start : begin to start");

        if (mStarted){
            if (VERBOSE)Log.w(TAG, "start : AudioRecord has been started");
            return 0;
        }

        int minRecBufSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, mAudioFormat);

        //创建AudioRecord
        try {
            mAudioRecord = new AudioRecord(mAudioSource,
                                            mSampleRate,
                                            mChannelConfig,
                                            mAudioFormat,
                                            minRecBufSize);
        } catch (Exception e) {
            Log.e(TAG, "start ; new AudioRecord failed with sampleRate:" + mSampleRate + ",channels:" + mChannelConfig + ",format:" + mAudioFormat);
            Log.e(TAG, e.getMessage());
            return -1;
        }

        //检测AudioRecord的状态
        if ( mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "start : AudioRecord getState failed with sampleRate " + mSampleRate + ",channels:" + mChannelConfig + ",format:" + mAudioFormat);
            return -1;
        }

        //计算读取数据的长度
        mToReadLen = calculateReadLenInTenMs(mSampleRate, mChannelConfig, mAudioFormat);
        if (mToReadLen <=0){
            Log.e(TAG, "start : calculateReadLenInTenMs failed with sampleRate " + mSampleRate + ",channels:" + mChannelConfig + ",format:" + mAudioFormat);
            return -1;
        }

        //配置缓存
        if (mBufferRec == null) {
            // allocate more data for safety
            mBufferRec =  ByteBuffer.allocateDirect(2 * mToReadLen);
            mBufferOffset = mBufferRec.arrayOffset();
        }

        //AudioRecord开始工作
        try {
            mAudioRecord.startRecording();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        //尝试读取数据，检测是否正常工作
        int readed = recordAudio();
        if (readed < 0)
            return -1;

        //创建线程开始读取音频数据
        mThread = new Thread(new AudioRecordThread());
        mThread.start();

        mStarted = true;

        if (VERBOSE)Log.d(TAG, "start : end to start");
        return 0;
    }

    public synchronized int stop()
    {
        if (VERBOSE)Log.d(TAG, "stop : begin to stop");

        if (!mStarted){
            if (VERBOSE)Log.w(TAG, "stop : AudioRecord has been stopped");
            return 0;
        }

        //结束线程
        if (mThread != null) {
            mThread.interrupt();
            try {
                mThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "stop : stop thread failed with " + e.toString());
            }
            mThread = null;
        }

        //AudioRecord停止工作， only stop if we are recording
        if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            try {
                mAudioRecord.stop();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return -1;
            }
        }

        //释放对象
        mAudioRecord.release();
        mAudioRecord = null;
        mBufferRec.clear();
        mStarted = false;

        if (VERBOSE)Log.d(TAG, "stop : end to stop");
        return 0;
    }

    private class AudioRecordThread implements Runnable
    {
        @Override
        public void run()
        {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            while (!Thread.interrupted()) {

                int readed = recordAudio();
                if (readed > 0) {
                    handleIncomingSample(mBufferRec.array(), readed);
                }
            }
            if (VERBOSE)Log.d(TAG, "AudioRecordThread : thread exit");
        }
    }

    private int recordAudio()
    {
        int readBytes = 0;
        try {
            if (mAudioRecord == null) {
                return -2; // We have probably closed down while waiting for rec lock
            }

            readBytes = mAudioRecord.read(mBufferRec, mToReadLen);
            if (readBytes == AudioRecord.ERROR_INVALID_OPERATION) {
                throw new IllegalStateException("read() returned AudioRecord.ERROR_INVALID_OPERATION");
            } else if (readBytes == AudioRecord.ERROR_BAD_VALUE) {
                throw new IllegalStateException("read() returned AudioRecord.ERROR_BAD_VALUE");
            } else if (readBytes == AudioRecord.ERROR_INVALID_OPERATION) {
                throw new IllegalStateException("read() returned AudioRecord.ERROR_INVALID_OPERATION");
            }

            if (readBytes <= 0) {
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "recordAudio : try failed: " + e.getMessage());
        }

        return readBytes;
    }

    private void handleIncomingSample(@NonNull byte[] data, int length)
    {
        if (mBufferOffset > 0) {
            //andriod version greater than 5.0
            data = Arrays.copyOfRange(data, mBufferOffset, length + mBufferOffset);
        }

        if (isDebugDump){
            Log.d(TAG, "handleIncomingSample : data len " + length);
            saveToSDCard("audio.pcm", data, length);
        }else if(mUserData != 0){
            robin_audio_record_on_sample(data, length, mUserData);
        }
    }

    private int calculateReadLenInTenMs(int sampleRate, int channel, int bitDeep)
    {
        int toReadLen;

        if (channel == AudioFormat.CHANNEL_IN_MONO) {
            channel = 1;
        }else if (channel == AudioFormat.CHANNEL_IN_STEREO){
            channel = 2;
        }else {
            return -1;
        }

        if (bitDeep == AudioFormat.ENCODING_PCM_8BIT){
            bitDeep = 8;
        }else if(bitDeep == AudioFormat.ENCODING_PCM_16BIT){
            bitDeep = 16;
        }else{
            return -1;
        }

        int bitPerTenMillisecond = sampleRate / 100;

        toReadLen = bitPerTenMillisecond * channel * (bitDeep / 8);

        return toReadLen;
    }

    private void saveToSDCard(@NonNull String filename, @NonNull byte[] data, int length)
    {
        File file = new File(Environment.getExternalStorageDirectory(), filename);
        try{
            FileOutputStream outStream = new FileOutputStream(file, true);//FileOutputStream(file);
            outStream.write(data, 0, length);
            outStream.close();
        }catch (Exception e){

            Log.d(TAG, String.format(Locale.CHINA ,"saveToSDCard : save %s to %s failed", Environment.getExternalStorageDirectory(), filename));
        }
    }
}
