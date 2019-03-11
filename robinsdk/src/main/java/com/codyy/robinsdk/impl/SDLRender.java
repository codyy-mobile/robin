package com.codyy.robinsdk.impl;

import android.graphics.SurfaceTexture;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;
import android.view.InputDevice;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import java.util.Arrays;

/**
 * Created by codyy on 2016/11/9.
 */
public class SDLRender {
    private static final boolean VERBOSE = false;
    private static final String TAG = "SDLRender";
    // Audio
    private static AudioTrack mAudioTrack;

    private SurfaceView mSurfaceView = null;
    private TextureView mTextureView = null;
    private Surface mSurface = null;
    private SurfaceHolderCallBack mSurfaceHolderCallBack = null;
    private TextureViewListener mTextureViewListener = null;
    private long mUserData;

    // If we want to separate mouse and touch events.
    //  This is only toggled in native code when a hint is set!
    public static boolean mSeparateMouseAndTouch = false;

    public static native void robin_video_render_on_surface_changed(int width, int height, long usreData);
    public static native void robin_video_render_on_surface_destroyed(long usreData);

    public SDLRender(long userData)
    {
        mUserData = userData;
        mSurfaceHolderCallBack = new SurfaceHolderCallBack();
        mTextureViewListener = new TextureViewListener();
    }

    public static void initialize() {
        // The static nature of the singleton and Android quirkyness force us to initialize everything here
        // Otherwise, when exiting the app and returning to it, these variables *keep* their pre exit values

        mAudioTrack = null;
    }

    public void setRenderView(Object view, int type) {
        if (view != null){
            if (type == 0){
                SurfaceView surfaceView = (SurfaceView)view;
                SurfaceHolder hold = surfaceView.getHolder();
                hold.addCallback(mSurfaceHolderCallBack);
                mSurface = hold.getSurface();
                mSurfaceView = surfaceView;

                if (mTextureView != null){
                    mTextureView.setSurfaceTextureListener(null);
                    mTextureView = null;
                }
            }else if (type == 1){
                TextureView textureView = (TextureView)view;
                textureView.setSurfaceTextureListener(mTextureViewListener);
                mSurface = new Surface(textureView.getSurfaceTexture());
                mTextureView = textureView;

                if (mSurfaceView != null){
                    mSurfaceView.getHolder().removeCallback(mSurfaceHolderCallBack);
                    mSurfaceView = null;
                }
            }
        }else{
            if (mTextureView != null){
                mTextureView.setSurfaceTextureListener(null);
                mTextureView = null;
            }

            if (mSurfaceView != null){
                mSurfaceView.getHolder().removeCallback(mSurfaceHolderCallBack);
                mSurfaceView = null;
            }
        }
    }

    /**
     * This method is called by SDL using JNI.
     */
    public Object getSurface() {
        return mSurface;
    }

    public int getSurfaceWidth() {
        if (mSurfaceView != null){
            return mSurfaceView.getWidth();
        }else if (mTextureView != null){
            return mTextureView.getWidth();
        }else {
            return  0;
        }
    }

    public int getSurfaceHeight() {
        if (mSurfaceView != null){
            return mSurfaceView.getHeight();
        }else if (mTextureView != null){
            return mTextureView.getHeight();
        }else {
            return  0;
        }
    }

    private class TextureViewListener implements TextureView.SurfaceTextureListener
    {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            robin_video_render_on_surface_changed(width, height, mUserData);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            robin_video_render_on_surface_destroyed(mUserData);
            return  false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    }

    private class SurfaceHolderCallBack implements SurfaceHolder.Callback
    {
        @Override
        public synchronized void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            robin_video_render_on_surface_changed(width, height, mUserData);
        }

        @Override
        public synchronized void surfaceCreated(SurfaceHolder holder) {

        }

        @Override
        public synchronized void surfaceDestroyed(SurfaceHolder holder) {
            robin_video_render_on_surface_destroyed(mUserData);
        }
    }

    public static boolean sendMessage(int command, int param) {
        return true;
    }

    // Audio

    /**
     * This method is called by SDL using JNI.
     */
    public static int audioInit(int sampleRate, boolean is16Bit, boolean isStereo, int desiredFrames) {
        int channelConfig = isStereo ? AudioFormat.CHANNEL_CONFIGURATION_STEREO : AudioFormat.CHANNEL_CONFIGURATION_MONO;
        int audioFormat = is16Bit ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
        int frameSize = (isStereo ? 2 : 1) * (is16Bit ? 2 : 1);

        if (VERBOSE) Log.d(TAG, "SDL audio: wanted " + (isStereo ? "stereo" : "mono") + " " + (is16Bit ? "16-bit" : "8-bit") + " " + (sampleRate / 1000f) + "kHz, " + desiredFrames + " frames buffer");

        // Let the user pick a larger buffer if they really want -- but ye
        // gods they probably shouldn't, the minimums are horrifyingly high
        // latency already
        desiredFrames = Math.max(desiredFrames, (AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat) + frameSize - 1) / frameSize);

        if (mAudioTrack == null) {
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                    channelConfig, audioFormat, desiredFrames * frameSize, AudioTrack.MODE_STREAM);

            // Instantiating AudioTrack can "succeed" without an exception and the track may still be invalid
            // Ref: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/media/java/android/media/AudioTrack.java
            // Ref: http://developer.android.com/reference/android/media/AudioTrack.html#getState()

            if (mAudioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                Log.e(TAG, "Failed during initialization of Audio Track");
                mAudioTrack = null;
                return -1;
            }

            mAudioTrack.play();
        }

        if (VERBOSE) Log.d(TAG, "SDL audio: got " + ((mAudioTrack.getChannelCount() >= 2) ? "stereo" : "mono") + " " + ((mAudioTrack.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT) ? "16-bit" : "8-bit") + " " + (mAudioTrack.getSampleRate() / 1000f) + "kHz, " + desiredFrames + " frames buffer");

        return 0;
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static void audioWriteShortBuffer(short[] buffer) {
        for (int i = 0; i < buffer.length; ) {
            int result = mAudioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch(InterruptedException e) {
                    // Nom nom
                }
            } else {
                Log.w(TAG, "SDL audio: error return from write(short)");
                return;
            }
        }
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static void audioWriteByteBuffer(byte[] buffer) {
        for (int i = 0; i < buffer.length; ) {
            int result = mAudioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch(InterruptedException e) {
                    // Nom nom
                }
            } else {
                Log.w(TAG, "SDL audio: error return from write(byte)");
                return;
            }
        }
    }

    /**
     * This method is called by SDL using JNI.
     */
    public static void audioQuit() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack = null;
        }
    }

    // Input

    /**
     * This method is called by SDL using JNI.
     * @return an array which may be empty but is never null.
     */
    public static int[] inputGetInputDeviceIds(int sources) {
        int[] ids = InputDevice.getDeviceIds();
        int[] filtered = new int[ids.length];
        int used = 0;
        for (int i = 0; i < ids.length; ++i) {
            InputDevice device = InputDevice.getDevice(ids[i]);
            if ((device != null) && ((device.getSources() & sources) != 0)) {
                filtered[used++] = device.getId();
            }
        }
        return Arrays.copyOf(filtered, used);
    }
}
