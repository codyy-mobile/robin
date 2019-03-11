package com.codyy.robinsdk.impl;

import android.media.projection.MediaProjection;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;

import com.codyy.robinsdk.RBPublishAndroid;
import com.codyy.robinsdk.RBSize;

/**
 * modify by liuhao on 2017/9/19
 */
public class RBPublisherAndroid implements RBPublishAndroid
{
    private RBManagerAndroid mRBManager = null;
    private PublisherListener mListener = null;

    private int mId = -1;
    private String TAG = "RBPublisherAndroid";
    private boolean mIsVerbose = false;
    
    private boolean mIsInit = false;
    private boolean mIsPreStart = false;
    private int mStatus = RBManagerAndroid.RB_STATUS_STOP;

    private int mScreenOrientation = RBManagerAndroid.RB_PORTRAIT;
    private String mUri = null;
    private SurfaceView mSurfaceView = null;
    private TextureView mTextureView = null;
    private MediaProjection mMediaProjection = null;
    private String mAppVideoFormat = null;
    private int mDpi = 1;
    private int mMediaMode = -1;
    private int mReconnectTimes = 20;
    private int mReconnectDuration = 5;

    private int mVideoDevId = -1;
    private int mVideoWidth = 0;
    private int mVideoHeight = 0;
    private int mVideoFrameRate = 25;
    private boolean mIsEnableVideoHWEnc = false;
    private int mVideoBitRate = 1024;
    private int mAudioDevId = -1;
    private int mAudioSampleRate = 16000;
    private boolean mIsEnableAec = false;
    private int mAudioBitRate = 16;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // inherit from RBPublisher
    public synchronized long init()
    {
        if (mRBManager == null || mId == -1) {
            if(mIsVerbose)Log.e(TAG, "init: error parameter");
            return -1;
        }

        if (!mIsInit) {
            if(0 != mRBManager.nativeInitPublisher(mId)) {
                if(mIsVerbose)Log.e(TAG, "init: native init publisher failed");
                return -1;
            }
            
            mIsInit = true;
        }

        return 0;
    }

    public synchronized long release()
    {
        if (mIsInit) {
            if (mStatus != RBManagerAndroid.RB_STATUS_STOP) {
                if(mIsVerbose)Log.e(TAG, "release: you need release publisher in stop status");
                return -1;
            }

            mRBManager.nativeReleasePublisher(mId);
            mIsInit = false;
        }

        return 0;
    }

    public synchronized long start()
    {
        long ret = 0;

        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "start: you need init publisher first");
            return -1;
        }

        //status check
        if (mStatus == RBManagerAndroid.RB_STATUS_PLAYING)
            return ret;

        //parameter check
        if (mVideoDevId == -1 || mAudioDevId == -1){
            if(mIsVerbose)Log.e(TAG, "start: you need set video device and audio device index before");
            return -1;
        }

        if (mMediaMode == -1) {
            if(mIsVerbose)Log.e(TAG, "start: you need set media mode before");
            return -1;
        }

        if (mVideoWidth == 0 || mVideoHeight == 0){
            if(mIsVerbose)Log.e(TAG, "start: you need set video resolution before");
            return -1;
        }

        if (mUri == null || !mUri.contains("rtmp://") ) {
            if(mIsVerbose)Log.e(TAG, "start: you need set valid uri before");
            return -1;
        }

        if (mMediaMode == RBManagerAndroid.RB_MEDIA_AUDIO && (mTextureView != null || mSurfaceView != null)) {
            if(mIsVerbose)Log.e(TAG, "start: media has no video, but render view is not null");
            return -1;
        }

        // parameter config
        if ((ret = mRBManager.nativeSetPublisherUri(mId, mUri)) != 0){
            if(mIsVerbose)Log.e(TAG, "start: native set publisher uri failed");
            return ret;
        }

        if ((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PUBLISHER_RECONNECT_TIMES, mId, mReconnectTimes)) != 0){
            if(mIsVerbose)Log.e(TAG, "start: native set publisher reconnect times failed");
            return ret;
        }

        if ((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PUBLISHER_RECONNECT_DURATION, mId, mReconnectDuration)) != 0){
            if(mIsVerbose)Log.e(TAG, "start: native set publisher duration times failed");
            return ret;
        }

        if ((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PUBLISHER_VIDEO_ID, mId, mVideoDevId)) != 0) {
            if(mIsVerbose)Log.e(TAG, "start: native set publisher video device index failed");
            return ret;
        }

        if ((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PUBLISHER_SCREEN_ORIENTATION, mId, mScreenOrientation)) != 0){
            if(mIsVerbose)Log.e(TAG, "start: native set publisher screen orientation failed");
            return ret;
        }

        RBSize size = new RBSize(mVideoWidth, mVideoHeight);
        if ((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PUBLISHER_VIDEO_RESOLUTION, mId,size)) != 0){
            if(mIsVerbose)Log.e(TAG, "start: native set publisher video resolution failed");
            return ret;
        }

        if ((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PUBLISHER_VIDEO_FRAME_RATE, mId, mVideoFrameRate)) != 0){
            if(mIsVerbose)Log.e(TAG, "start: native set publisher video frame rate failed");
            return ret;
        }

        if ((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PUBLISHER_VIDEO_BITRATE, mId, mVideoBitRate)) != 0){
            if(mIsVerbose)Log.e(TAG, "start: native set publisher video bitrate failed");
            return ret;
        }

        if ((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PUBLISHER_HARDWARE, mId, mIsEnableVideoHWEnc)) != 0){
            if(mIsVerbose)Log.e(TAG, "start: native set publisher video hardware encode failed");
            return ret;
        }


        if ((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PUBLISHER_AUDIO_ID, mId, mAudioDevId)) != 0){
            if(mIsVerbose)Log.e(TAG, "start: native set publisher audio device index failed");
            return ret;
        }

        if ((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PUBLISHER_AUDIO_SAMPLE_RATE, mId, mAudioSampleRate)) != 0){
            if(mIsVerbose)Log.e(TAG, "start: native set publisher audio sample rate failed");
            return ret;
        }

        if ((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PUBLISHER_AUDIO_BITRATE, mId, mAudioBitRate)) != 0){
            if(mIsVerbose)Log.e(TAG, "start: native set publisher audio bitrate failed");
            return ret;
        }

        if ((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PUBLISHER_AUDIO_AEC, mId, mIsEnableAec)) != 0){
            if(mIsVerbose)Log.e(TAG, "start: native set publisher set audio aec failed");
            return ret;
        }

        if (mMediaProjection != null && mDpi > 0){
            if ((ret = mRBManager.nativeSetPulisherMediaProjection(mId, mMediaProjection, mDpi)) != 0) {
                if(mIsVerbose)Log.e(TAG, "start: native set publisher media projection failed");
                return ret;
            }
        }

        if (mAppVideoFormat != null){
            if ((ret = mRBManager.nativeSetPublisherAppVideoFormat(mId, mAppVideoFormat)) != 0) {
                if(mIsVerbose)Log.e(TAG, "start: native set publisher app video format failed");
                return ret;
            }
        }

        if (mTextureView != null) {
            if ((ret = mRBManager.nativeSetPulisherRenderView(mId, mTextureView, RBManagerAndroid.RB_VIDEO_RENDER_VIEW_TYPE_TEXTURE_VIEW)) != 0) {
                if(mIsVerbose)Log.e(TAG, "start: native set publisher texture view failed");
                return ret;
            }
        }else if (mSurfaceView != null){
            if ((ret = mRBManager.nativeSetPulisherRenderView(mId, mSurfaceView, RBManagerAndroid.RB_VIDEO_RENDER_VIEW_TYPE_SURFACE_VIEW)) != 0) {
                if(mIsVerbose)Log.e(TAG, "start: native set publisher surface view failed");
                return ret;
            }
        }

        if ((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PUBLISHER_MEDIA_MODE, mId, mMediaMode)) != 0){
            if(mIsVerbose)Log.e(TAG, "start: native set publisher send media mode failed");
            return ret;
        }

        //start
        if ((ret = mRBManager.nativeStartPublisher(mId)) != 0){
            if(mIsVerbose)Log.e(TAG, "start: native set start publisher failed");
            return ret;
        }

        mIsPreStart = true;

        return 0;
    }

    public synchronized long stop()
    {
        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "stop: you need init publisher first");
            return -1;
        }

        if ( mStatus == RBManagerAndroid.RB_STATUS_PLAYING || mIsPreStart) {
            return mRBManager.nativeStopPublisher(mId);
        }

        return 0;
    }

    public synchronized long setUri(String uri)
    {
        if (uri == null || !uri.contains("rtmp://") ) {
            if(mIsVerbose)Log.e(TAG, "setUri: unsupported uri " + uri);
            return -1;
        }

        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setUri: you need init publisher first");
            return -1;
        }

        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            mUri = uri;
            return 0;
        }else {
            if(mIsVerbose)Log.w(TAG, "setUri: you need set uri in stop status");
        }

        return -1;
    }

    public synchronized long setMediaMode(int mode, SurfaceView view)
    {
        long ret = 0;

        if (mode < RBManagerAndroid.RB_MEDIA_ALL || mode > RBManagerAndroid.RB_MEDIA_AUDIO){
            if(mIsVerbose)Log.e(TAG, "setMediaMode: unsupported media mode " + mode);
            return -1;
        }

        if (mode == RBManagerAndroid.RB_MEDIA_AUDIO && view != null) {
            if(mIsVerbose)Log.e(TAG, "setMediaMode: media has no video, but surface view is not null");
            return -1;
        }

        if (mode == mMediaMode && view == mSurfaceView)
            return 0;

        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setMediaMode: you need init publisher first");
            return -1;
        }

        if(mStatus == RBManagerAndroid.RB_STATUS_PLAYING) {
            if(view != null) {
                if ((ret = mRBManager.nativeSetPulisherRenderView(mId, view, RBManagerAndroid.RB_VIDEO_RENDER_VIEW_TYPE_SURFACE_VIEW)) != 0) {
                    if(mIsVerbose)Log.e(TAG, "setMediaMode: native set publisher render view failed");
                    return ret;
                }
            }

            if((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PUBLISHER_MEDIA_MODE, mId, mode)) != 0) {
                if(mIsVerbose)Log.e(TAG, "setMediaMode: native set media mode failed, mode is " + mode);
            }
        }

        mMediaMode = mode;
        mSurfaceView = view;
        mTextureView = null;

        return ret;
    }

    public synchronized long setMediaModeByTextureView(int mode, TextureView view)
    {
        long ret = 0;

        if (mode < RBManagerAndroid.RB_MEDIA_ALL || mode > RBManagerAndroid.RB_MEDIA_AUDIO){
            if(mIsVerbose)Log.e(TAG, "setMediaMode: unsupported media mode " + mode);
            return -1;
        }

        if (mode == RBManagerAndroid.RB_MEDIA_AUDIO && view != null) {
            if(mIsVerbose)Log.e(TAG, "setMediaMode: media has no video, but preview surface is not null");
            return -1;
        }

        if (mode == mMediaMode && view == mTextureView)
            return 0;

        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setMediaMode: you need init publisher first");
            return -1;
        }

        if(mStatus == RBManagerAndroid.RB_STATUS_PLAYING) {
            if(view != null) {
                if ((ret = mRBManager.nativeSetPulisherRenderView(mId, view, RBManagerAndroid.RB_VIDEO_RENDER_VIEW_TYPE_TEXTURE_VIEW)) != 0) {
                    if(mIsVerbose)Log.e(TAG, "setMediaMode: native set publisher surface failed");
                    return ret;
                }
            }

            if((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PUBLISHER_MEDIA_MODE, mId, mode)) != 0) {
                if(mIsVerbose)Log.e(TAG, "setMediaMode: native set media mode failed, mode is " + mode);
            }
        }

        mMediaMode = mode;
        mTextureView = view;
        mSurfaceView = null;

        return ret;
    }

    public synchronized long setReconnectTimes(int times)
    {
        if (times < 0 || times > 100) {
            if(mIsVerbose)Log.e(TAG, "setReconnectTimes: unsupported times " + times);
            return -1;
        }

        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setReconnectTimes: you need init publisher first");
            return -1;
        }

        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            mReconnectTimes = times;
            return 0;
        }else {
            if(mIsVerbose)Log.w(TAG, "setReconnectTimes: you need set reconnect times in stop status");
        }

        return -1;
    }

    public synchronized long setReconnectDuration(int duration)
    {
        if (duration < 4) {
            if(mIsVerbose)Log.e(TAG, "setReconnectDuration: unsupported duration " + duration);
            return -1;
        }

        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setReconnectDuration: you need init publisher first");
            return -1;
        }

        if ( mStatus == RBManagerAndroid.RB_STATUS_STOP ) {
            mReconnectDuration = duration;
            return 0;
        }else {
            if(mIsVerbose)Log.w(TAG, "setReconnectDuration: you need set reconnect duration in stop status");
        }

        return -1;
    }

    public synchronized long setVideoDevice(int videoId, int screenOrientation)
    {
        if (videoId < 0){
            if(mIsVerbose)Log.e(TAG, "setVideoDevice: unsupported video device index " + videoId);
            return -1;
        }

        if (screenOrientation < RBManagerAndroid.RB_PORTRAIT || screenOrientation > RBManagerAndroid.RB_LANDSCAPE_RIGHT) {
            if(mIsVerbose)Log.e(TAG, "setVideoDevice: unsupported screen orientation " + screenOrientation);
            return -1;
        }

        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setVideoDevice: you need init publisher first");
            return -1;
        }

        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            mVideoDevId = videoId;
            mScreenOrientation = screenOrientation;
        } else {
            if (mScreenOrientation != screenOrientation) {
                if (mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PUBLISHER_SCREEN_ORIENTATION, mId, screenOrientation) != 0){
                    Log.e(TAG, "setVideoDevice: native set screen orientation failed");
                    return  -1;
                }

                mScreenOrientation = screenOrientation;
            }

            if (mVideoDevId != videoId) {
                if (mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PUBLISHER_VIDEO_ID, mId, videoId) != 0) {
                    if(mIsVerbose)Log.e(TAG, "setVideoDevice: native set video device index failed");
                    return -1;
                }
                mVideoDevId = videoId;
            }
        }

        return 0;
    }

    public synchronized long setMediaProjection(MediaProjection mp, int dpi)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if(mIsVerbose)Log.e(TAG, "setMediaProjection: the version of api is lower than 21");
            return -1;
        }

        if (mp == null || dpi <=0 ){
            if(mIsVerbose)Log.e(TAG, "setMediaProjection: unsupported dpi or null mp");
            return -1;
        }

        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setMediaProjection: you need init publisher first");
            return -1;
        }

        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            mMediaProjection = mp;
            mDpi = dpi;
            return 0;
        }else {
            if(mIsVerbose)Log.w(TAG, "setMediaProjection: you need set video media projection in stop status");
        }

        return 0;
    }

    public synchronized long setAppVideoFormat(String format)
    {
        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setAppVideoFormat: you need init publisher first");
            return -1;
        }

        if (!format.equals("h264") &&
            !format.equals("h265") &&
            !format.equals("nv12") &&
            !format.equals("nv21") &&
            !format.equals("i420"))
        {
            if(mIsVerbose)Log.e(TAG, "setAppVideoFormat: unsupported format " + format);
            return -1;
        }	

        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            mAppVideoFormat = format;
            return 0;
        }else {
            if(mIsVerbose)Log.w(TAG, "setAppVideoFormat: you need set app video format in stop status");
        }

        return 0;
    }

    public synchronized long receiveAppVideoData(byte[] data, int len)
    {
        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            return 0;
        }

        if (mAppVideoFormat == null)
        {
            if(mIsVerbose)Log.e(TAG, "receiveAppVideoData: you need set app video format");
            return -1;
        }

        return mRBManager.nativeReceiveAppVideoData(mId, data, len);
    }

    public synchronized long setVideoResolution(int width, int height)
    {
        int wh = width * height;
        if (wh <= 176 * 144){
            mVideoBitRate = 384;
        }else if (wh > 176 * 144 && wh <= 352 * 288){
            mVideoBitRate = 512;
        }else if (wh > 352 * 288 && wh <= 640 * 480){
            mVideoBitRate = 768;
        }else if (wh > 640 * 480 && wh <= 1280 * 720){
            mVideoBitRate = 1024;
        }else if (wh > 1280 * 720 && wh <= 1920 * 1080){
            mVideoBitRate = 1536;
        }else if (wh > 1920 * 1080){
            mVideoBitRate = 2048;
        }

        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setVideoResolution: you need init publisher first");
            return -1;
        }

        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            mVideoWidth = width;
            mVideoHeight = height;
            return 0;
        }else {
            if(mIsVerbose)Log.w(TAG, "setVideoResolution: you need set video resolution in stop status");
        }

        return -1;
    }

    public synchronized long setVideoFrameRate(int frameRate)
    {
        if (frameRate < 10 || frameRate > 30){
            if(mIsVerbose)Log.e(TAG, "setVideoFrameRate: unsupported video frame rate " + frameRate);
            return -1;
        }

        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setVideoFrameRate: you need init publisher first");
            return -1;
        }

        if (frameRate <= 12) {
            frameRate = 10;
        } else if(frameRate > 12 && frameRate <= 17) {
            frameRate = 15;
        } else if(frameRate > 17 && frameRate <= 22) {
            frameRate = 20;
        } else if(frameRate > 22 && frameRate <= 27) {
            frameRate = 25;
        } else if(frameRate > 27 && frameRate <= 32) {
            frameRate = 30;
        } else if(frameRate > 32 && frameRate <= 37){
            frameRate = 35;
        } else if(frameRate > 37) {
            frameRate = 40;
        }

        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            mVideoFrameRate = frameRate;
            return 0;
        }else {
            if(mIsVerbose)Log.w(TAG, "setVideoFrameRate: you need set video frame rate in stop status");
        }

        return -1;
    }

    public synchronized long setVideoHardwareEnc(boolean isEnable)
    {
        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setVideoHardwareEnc: you need init publisher first");
            return -1;
        }

        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            mIsEnableVideoHWEnc = isEnable;
            return 0;
        }else {
            if(mIsVerbose)Log.w(TAG, "setVideoHardwareEnc: you need set video hardware encode in stop status");
        }

        return -1;
    }

    public synchronized long setVideoBitRate(int bitRate)
    {
        if (bitRate < 32 || bitRate > 8192) {
            if(mIsVerbose)Log.e(TAG, "setVideoBitRate: unsupported video bitrate " + bitRate);
            return -1;
        }

        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setVideoBitRate: you need init publisher first");
            return -1;
        }

        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            mVideoBitRate = bitRate;
            return 0;
        }else {
            if(mIsVerbose)Log.w(TAG, "setVideoBitRate: you need set video bitrate in stop status");
        }

        return -1;
    }

    public synchronized long setAudioDevice(int audioId)
    {
        if (audioId < 0) {
            if(mIsVerbose)Log.e(TAG, "setAudioDevice: unsupported audio device index " + audioId);
            return -1;
        }

        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setAudioDevice: you need init publisher first");
            return -1;
        }

        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            mAudioDevId = audioId;
            return 0;
        }else {
            if(mIsVerbose)Log.w(TAG, "setAudioDevice: you need set audio device index in stop status");
        }

        return -1;
    }

    public synchronized long receiveAppPcmData(byte[] data, int len, int sampleRate, int channel, int format)
    {
        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            return 0;
        }

        return mRBManager.nativeReceiveAppPcmData(mId, data, len, sampleRate, channel, format);
    }

    public synchronized long setAudioSampleRate(int sampleRate)
    {
        if (sampleRate < 8000 || sampleRate > 44100) {
            if(mIsVerbose)Log.e(TAG, "setAudioSampleRate: unsupported audio sample rate " + sampleRate);
            return -1;
        }

        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setAudioSampleRate: you need init publisher first");
            return -1;
        }

        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            mAudioSampleRate = sampleRate;
            return 0;
        }else {
            if(mIsVerbose)Log.w(TAG, "setAudioSampleRate: you need set audio sample rate in stop status");
        }

        return -1;
    }

    public synchronized long setAudioAEC(boolean isEnable)
    {
        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setAudioAEC: you need init publisher first");
            return -1;
        }

        if (mMediaMode == RBManagerAndroid.RB_MEDIA_VIDEO)
        {
            if(mIsVerbose)Log.e(TAG, "setAudioAEC: you need not set audio aec when media mode is only video");
            return -1;
        }

        if (mIsEnableAec != isEnable) {
            if(mStatus == RBManagerAndroid.RB_STATUS_STOP) {
                mIsEnableAec = isEnable;
            } else {
                if (mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PUBLISHER_AUDIO_AEC, mId, isEnable) != 0) {
                    if(mIsVerbose)Log.e(TAG, "setAudioAEC: native set audio aec failed");
                    return -1;
                }

                mIsEnableAec = isEnable;
            }
        }

        return 0;
    }

    public synchronized long setAudioBitRate(int bitRate)
    {
        if (bitRate < 8 || bitRate > 128) {
            if(mIsVerbose)Log.e(TAG, "setAudioBitRate: unsupported audio bitrate " + bitRate);
            return -1;
        }

        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setAudioBitRate: you need init publisher first");
            return -1;
        }

        if ( mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            mAudioBitRate = bitRate;
            return 0;
        }else {
            if(mIsVerbose)Log.w(TAG, "setAudioBitRate: you need set audio bitrate in stop status");
        }

        return -1;
    }

    public synchronized long getVideoDeviceNum()
    {
        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "getVideoDeviceNum: you need init publisher first");
            return -1;
        }

        return mRBManager.nativeGetVideoDeviceNum(mId);
    }

    public synchronized String getVideoDeviceDescribe(int videoId)
    {
        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "getVideoDeviceDescribe: you need init publisher first");
            return null;
        }

        return mRBManager.nativeGetVideoDeviceDescribe(mId, videoId);
    }

    public synchronized long getSupportVideoResolution(int videoId, RBSize[] resolutionArray, int[] numArray)
    {
        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "getSupportVideoResolution: you need init publisher first");
            return -1;
        }

        mRBManager.nativeGetVideoDeviceResolution(mId, videoId, resolutionArray, numArray);
        return 0;
    }

    public synchronized long getAudioDeviceNum()
    {
        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "getAudioDeviceNum: you need init publisher first");
            return -1;
        }

        return mRBManager.nativeGetAudioDeviceNum(mId);
    }

    public synchronized String getAudioDeviceDescribe(int audioId)
    {
        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "getAudioDeviceDescribe: you need init publisher first");
            return null;
        }

        return mRBManager.nativeGetAudioDeviceDescribe(mId, audioId);
    }

    public synchronized long getSupportAudioSampleRate(int audioId, int[] sampleRateArray, int[] numArray)
    {
        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "getSupportAudioSampleRate: you need init publisher first");
            return -1;
        }

        mRBManager.nativeGetAudioDeviceSampleRate(mId, audioId, sampleRateArray, numArray);
        return 0;
    }

    public synchronized void setPublisherListener(PublisherListener listener)
    {
        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            mListener = listener;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // own
    public int getId()
    {
        return mId;
    }

    public void setId(int id)
    {
        mId = id;
        TAG = TAG + String.valueOf(id);
    }

    public void setManager(RBManagerAndroid manager)
    {
        mRBManager = manager;
    }

    public void setLog(boolean isEnable)
    {
        mIsVerbose = isEnable;
    }

    public void OnStateChange(int newState)
    {
        if (newState < RBManagerAndroid.RB_STATUS_PLAYING || newState > RBManagerAndroid.RB_STATUS_STOP)
            return;

        mIsPreStart = false;
        mStatus = newState;

        if (mListener != null)
        {
            mListener.OnStateChange(newState);
        }
    }

    public void OnErrorGet(int errModuleId, int errCode, String desc)
    {
        if (mListener != null)
        {
            mListener.OnErrorGet(errCode, desc);
        }
    }

    public void OnNoticeGet(int errModuleId, int noticeCode, String desc)
    {
        if (mListener != null)
        {
            mListener.OnNoticeGet(noticeCode, desc);
        }
    }

    public void OnMediaModeChange(int newMode)
    {
        if (mListener != null)
        {
            mListener.OnMediaModeChange(newMode);
        }
    }

    public void OnVideoIdChange(int newVideoId)
    {
        if(mListener != null)
        {
            mListener.OnVideoIdChange(newVideoId);
        }
    }
}
