package com.codyy.robinsdk.impl;


import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;

import com.codyy.robinsdk.RBPlayAndroid;

/**
 * modify by liuhao on 2017/9/19
 */
public class RBPlayerAndroid implements RBPlayAndroid
{
    private int mId = -1;
    private String TAG = "RBPlayerAndroid";
    private boolean mIsVerbose = false;
    
    private boolean mIsInit = false;
    private boolean mIsPreStart = false;
    private int mStatus = RBManagerAndroid.RB_STATUS_STOP;
    private RBManagerAndroid mRBManager = null;
    private PlayerListener mListener = null;

    private String mUri = null;
    private SurfaceView mSurfaceView = null;
    private TextureView mTextureView = null;
    private int mReconnectTimes = 20;
    private int mReconnectDuration = 5;
    private int mNoDataTimeout = 4;
    private boolean mIsEnableVideoHWDec = false;
    private int mVolume = 80;
    private int mMediaMode = -1;
    private int mRenderMode = RBManagerAndroid.RB_RENDER_FULL_SCREEN;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // inherit from RBPlayer
    public synchronized long init()
    {
        if (mRBManager == null || mId == -1) {
            if(mIsVerbose)Log.e(TAG, "init: error parameter");
            return -1;
        }

        if (!mIsInit) {
            if(0 != mRBManager.nativeInitPlayer(mId)) {
                if(mIsVerbose)Log.e(TAG, "init: native init player failed");
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
                if(mIsVerbose)Log.e(TAG, "release: you need release player in stop status");
                return -1;
            }

            mRBManager.nativeReleasePlayer(mId);
            mIsInit = false;
        }

        return 0;
    }

    public synchronized long start()
    {
        long ret = 0;

        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "start: you need init player first");
            return -1;
        }

        //status check
        if (mStatus == RBManagerAndroid.RB_STATUS_PLAYING)
            return ret;

        if (mStatus == RBManagerAndroid.RB_STATUS_PAUSING)
            return mRBManager.nativeStartPlayer(mId);

        //parameter check
        if (mMediaMode == -1) {
            if(mIsVerbose)Log.e(TAG, "start: you need set media mode before");
            return -1;
        }

        if (mUri == null || !mUri.contains("rtmp://") ) {
            if(mIsVerbose)Log.e(TAG, "start: you need set valid uri before");
            return -1;
        }

        if (mMediaMode != RBManagerAndroid.RB_MEDIA_AUDIO && mTextureView == null && mSurfaceView == null ) {
            if(mIsVerbose)Log.e(TAG, "start: media has video, but render surface is null");
            return -1;
        }

        //parameter config
        if ((ret = mRBManager.nativeSetPlayerUri(mId, mUri)) != 0) {
            if(mIsVerbose)Log.e(TAG, "start: native set player uri failed");
            return ret;
        }

        if ((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PLAYER_RECONNECT_TIMES, mId, mReconnectTimes)) != 0){
            if(mIsVerbose)Log.e(TAG, "start: native set player reconnect times failed");
            return ret;
        }

        if ((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PLAYER_RECONNECT_DURATION, mId, mReconnectDuration)) != 0) {
            if(mIsVerbose)Log.e(TAG, "start: native set player reconnect duration failed");
            return ret;
        }

        if ((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PLAYER_NO_DATA_TIMEOUT, mId, mNoDataTimeout)) != 0){
            if(mIsVerbose)Log.e(TAG, "start: native set player no data timeout failed");
            return ret;
        }

        if ((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PLAYER_HARDWARE, mId, mIsEnableVideoHWDec)) != 0) {
            if(mIsVerbose)Log.e(TAG, "start: native set player video hardware decode failed");
            return ret;
        }

        if ((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PLAYER_RENDER_MODE, mId, mRenderMode)) != 0) {
            if(mIsVerbose)Log.e(TAG, "start: native set player render mode failed");
            return ret;
        }

        if ((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PLAYER_VOLUME, mId, mVolume)) != 0){
            if(mIsVerbose)Log.e(TAG, "start: native set player volume failed");
            return ret;
        }

        if(mTextureView != null) {
            if ((ret = mRBManager.nativeSetPlayerRenderView(mId, mTextureView, RBManagerAndroid.RB_VIDEO_RENDER_VIEW_TYPE_TEXTURE_VIEW)) != 0) {
                if(mIsVerbose)Log.e(TAG, "start: native set player render surface failed");
                return ret;
            }
        }else if (mSurfaceView != null){
            if ((ret = mRBManager.nativeSetPlayerRenderView(mId, mSurfaceView, RBManagerAndroid.RB_VIDEO_RENDER_VIEW_TYPE_SURFACE_VIEW)) != 0) {
                if(mIsVerbose)Log.e(TAG, "start: native set player render surface failed");
                return ret;
            }
        }

        if ((ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PLAYER_MEDIA_MODE, mId, mMediaMode)) != 0) {
            if(mIsVerbose)Log.e(TAG, "start: native set player receive media mode failed");
            return ret;
        }

        // start
        if ((ret = mRBManager.nativeStartPlayer(mId)) != 0) {
            if(mIsVerbose)Log.e(TAG, "start: native set start player failed");
            return ret;
        }

        mIsPreStart = true;

        return ret;
    }

    public synchronized long pause()
    {
        //no implementation
        return -1;

//        if (!mIsInit)
//            return -1;
//
//        if (mPlayerStatus == RobinManager.RB_STATUS_PAUSING)
//            return 0;
//
//        return mRBManager.nativePausePlay(mId);
    }

    public synchronized long stop()
    {
        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "stop: you need init player first");
            return -1;
        }

        if (mStatus == RBManagerAndroid.RB_STATUS_PLAYING || mStatus == RBManagerAndroid.RB_STATUS_PAUSING || mIsPreStart) {
            return mRBManager.nativeStopPlayer(mId);
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
            if(mIsVerbose)Log.e(TAG, "setUri: you need init player first");
            return -1;
        }

        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            mUri = uri;
            return 0;
        } else {
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
        
        if (mode != RBManagerAndroid.RB_MEDIA_AUDIO && view == null) {
            if(mIsVerbose)Log.e(TAG, "setMediaMode: media has video, but surface view is null");
            return -1;
        } else if (mode == RBManagerAndroid.RB_MEDIA_AUDIO && view != null) {
            if(mIsVerbose)Log.e(TAG, "setMediaMode: media has no video, but surface view is not null");
            return -1;
        }

        if (mode == mMediaMode && view == mSurfaceView)
            return 0;

        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setMediaMode: you need init player first");
            return -1;
        }

        if (mStatus == RBManagerAndroid.RB_STATUS_PLAYING || mIsPreStart) {
            if (view != null) {
                if ((ret = mRBManager.nativeSetPlayerRenderView(mId, view, RBManagerAndroid.RB_VIDEO_RENDER_VIEW_TYPE_SURFACE_VIEW)) != 0) {
                    if(mIsVerbose)Log.e(TAG, "setMediaMode: native set player render view failed");
                    return ret;
                }
            }

            if( (ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PLAYER_MEDIA_MODE, mId, mode)) != 0) {
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

        if (mode != RBManagerAndroid.RB_MEDIA_AUDIO && view == null) {
            if(mIsVerbose)Log.e(TAG, "setMediaMode: media has video, but texture view is null");
            return -1;
        } else if (mode == RBManagerAndroid.RB_MEDIA_AUDIO && view != null) {
            if(mIsVerbose)Log.e(TAG, "setMediaMode: media has no video, but texture view is not null");
            return -1;
        }

        if (mode == mMediaMode && view == mTextureView)
            return 0;

        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setMediaMode: you need init player first");
            return -1;
        }

        if (mStatus == RBManagerAndroid.RB_STATUS_PLAYING || mIsPreStart) {
            if (view != null) {
                if ((ret = mRBManager.nativeSetPlayerRenderView(mId, view, RBManagerAndroid.RB_VIDEO_RENDER_VIEW_TYPE_TEXTURE_VIEW)) != 0) {
                    if(mIsVerbose)Log.e(TAG, "setMediaMode: native set player render view failed");
                    return ret;
                }
            }

            if( (ret = mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PLAYER_MEDIA_MODE, mId, mode)) != 0) {
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
            if(mIsVerbose)Log.e(TAG, "setReconnectTimes: you need init player first");
            return -1;
        }

        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            mReconnectTimes = times;
            return 0;
        } else {
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
            if(mIsVerbose)Log.e(TAG, "setReconnectDuration: you need init player first");
            return -1;
        }

        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            mReconnectDuration = duration;
            return 0;
        } else {
            if(mIsVerbose)Log.w(TAG, "setReconnectDuration: you need set reconnect duration in stop status");
        }

        return -1;
    }
	
    public synchronized long setNoDataTimeout(int timeout)
    {
        if (timeout < 0) {
            if(mIsVerbose)Log.e(TAG, "setNoDataTimeout: unsupported timeout " + timeout);
            return -1;
        }

        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setNoDataTimeout: you need init player first");
            return -1;
        }

        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            mNoDataTimeout = timeout;
            return 0;
        } else {
            if(mIsVerbose)Log.w(TAG, "setNoDataTimeout: you need set no media data timeout in stop status");
        }

        return -1;
    }

    public synchronized long setVideoHardwareDec(boolean isEnable)
    {
        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setVideoHardwareDec: you need init player first");
            return -1;
        }

        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            mIsEnableVideoHWDec = isEnable;
            return 0;
        } else {
            if(mIsVerbose)Log.w(TAG, "setVideoHardwareDec: you need set video hardware decode in stop status");
        }

        return -1;
    }

    public synchronized long setVideoRenderMode(int mode)
    {
        if (mode < RBManagerAndroid.RB_RENDER_FULL_SCREEN || mode > RBManagerAndroid.RB_RENDER_ASPECT_RATIO) {
            if(mIsVerbose)Log.e(TAG, "setVideoRenderMode: unsupported render mode " + mode);
            return -1;
        }

        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setVideoRenderMode: you need init player first");
            return -1;
        }

        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            mRenderMode = mode;
            return 0;
        } else {
            if(mIsVerbose)Log.w(TAG, "setVideoRenderMode: you need set video render mode in stop status");
        }

        return -1;
    }

    public synchronized long setVolume(int volume)
    {
        if (volume < 0 || volume > 100) {
            if(mIsVerbose)Log.e(TAG, "setVolume: unsupported volume " + volume);
            return -1;
        }

        if (!mIsInit) {
            if(mIsVerbose)Log.e(TAG, "setVolume: you need init player first");
            return -1;
        }

        if (mStatus == RBManagerAndroid.RB_STATUS_PLAYING)
        {
            if (mRBManager.nativeSetAttribute(RBManagerAndroid.RB_PLAYER_VOLUME, mId, volume) != 0) {
                if(mIsVerbose)Log.e(TAG, "setVolume: native set volume failed, volume is " + volume);
                return -1;
            }
        }

        mVolume = volume;;

        return 0;
    }

    public synchronized long seekToPos(long pos)
    {
        //no implementation
        return -1;

//        if (!mIsInit)
//            return -1;
//
//        if (mPlayerStatus == RobinManager.RB_STATUS_PLAYING)
//            mRBManager.nativeSetAttribute(RobinManager.RB_PLAYER_SEEK_POS, mId, pos);
//
//        return 0;
    }

    public synchronized long getPosition()
    {
        //no implementation
        return -1;

//        if (!mIsInit)
//            return -1;
//
//        if (mPlayerStatus != RobinManager.RB_STATUS_STOP)
//            return mRBManager.nativeGetPosition(mId);
//
//        return -1;
    }

    public synchronized long getDuration()
    {
        //no implementation
        return -1;

//        if (!mIsInit)
//            return -1;
//
//        // 需要设置uri之后才能获取
//        if (mPlayerUri != null)
//            return mRBManager.nativeGetDuration(mId);
//
//        return -1;
    }

    public synchronized void setPlayerListener(PlayerListener Listener)
    {
        if (mStatus == RBManagerAndroid.RB_STATUS_STOP) {
            mListener = Listener;;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // own method
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
        if ( newState < RBManagerAndroid.RB_STATUS_PLAYING || newState > RBManagerAndroid.RB_STATUS_STOP)
            return;

        mIsPreStart = false;
        mStatus = newState;
        if (mListener != null) {
            mListener.OnStateChange(newState);
        }
    }

    public void OnErrorGet(int errModuleId, int errCode, String desc)
    {
        if (mListener != null) {
            mListener.OnErrorGet(errCode, desc);
        }
    }

    public void OnNoticeGet(int errModuleId, int noticeCode, String desc)
    {
        if (mListener != null) {
            mListener.OnNoticeGet(noticeCode, desc);
        }
    }

    public void OnMediaModeChange(int newMode)
    {
        if (mListener != null) {
            mListener.OnMediaModeChange(newMode);
        }
    }

    public void OnVideoResolution(int width, int height)
    {
        if (mListener != null) {
            mListener.OnVideoResolution(width, height);
        }
    }

    public void OnBufferingStateChange(int bufferingPercent)
    {
        if (mListener != null) {
            mListener.OnBufferProcessing(bufferingPercent);
        }
    }
}
