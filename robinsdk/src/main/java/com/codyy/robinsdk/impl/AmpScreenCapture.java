package com.codyy.robinsdk.impl;

import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by liuhao on 2017/10/13.
 */

public class AmpScreenCapture {
    private static final String TAG = "MpScreenCapture";
    private static final boolean VERBOSE = false;
    private static final int MAX_ENCODER_SPEC_DATA_SIZE = 2048;
    private static final int ENCODER_DEQUE_OUTPUT_BUFFER_TIMEOUT = 10000;

    public static final int ENCODER_TYPE_AVC = 0;
    public static final int ENCODER_TYPE_HEVC = 1;

    public static final int ENCODER_PACKET_CONFIG = 0;
    public static final int ENCODER_PACKET_KEY = 1;
    public static final int ENCODER_PACKET_NON_KEY = 2;

    private MediaProjection mMediaProjection = null;
    private VirtualDisplay mVirtualDisplay = null;
    private SurfaceTextureEncoder mTextureEncoder = null;
    private MediaCodec mMediaEncoder = null;
    private Surface mCodecInputSurface = null;
    private MediaCodec.BufferInfo mBufferInfo = null;

    private int mVideoWidth = 1280;
    private int mVideoHeight = 720;
    private int mDpi = 1;
    private int mFps = 25;
    private int mMime= ENCODER_TYPE_AVC;
    private int mBitrate = 1024 * 1024 * 2;
    private int mProfile = 0;
    private int mLevel = 0;
    private boolean mHasBframe = false;

    private long mUserData = 0;
    private boolean mIsInited = false;
    private boolean mIsStarted = false;

    private byte[] mEncoderData = null;
    private byte[] mEncoderSpecData = null;
    private int mEncoderSpecDataSize = 0;
    private FileOutputStream mFileOutput = null;

    public static native void robin_screen_capture_on_packet(byte[] data, int lenght, int type, long userData);

    public AmpScreenCapture(long userData)
    {
        mUserData = userData;
        mBufferInfo = new MediaCodec.BufferInfo();
    }

    public static boolean isSupported()
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return false;

        return true;
    }

    public synchronized int setValue(String entry, int val)
    {
        if (val < 0) {
            Log.e(TAG, String.format("setValue : set value %d for %s is unsupported", val, entry));
            return -1;
        }

        if (mIsInited){
            Log.e(TAG, "setValue : you need set value before init");
            return -1;
        }

        switch (entry){
            case "width":
                mVideoWidth = val;
                break;
            case "height":
                mVideoHeight = val;
                break;
            case "fps":
                mFps = val;
                break;
            case "mime":
                mMime = val;
                break;
            case "bitrate":
                mBitrate = val;
                break;
            case "profile":
                mProfile = val;
                break;
            case "level":
                mLevel = val;
                break;
            case "b-frame-num":
                mHasBframe =  val > 0;
                break;
            default:
                break;
        }

        if (VERBOSE)Log.d(TAG,  String.format("setValue : set value %d for %s", val, entry));
        return 0;
    }

    public synchronized int config(Object mp, int dpi)
    {
        if(VERBOSE)Log.d(TAG, "init : begin to init");

        if (mp == null || dpi <= 0) {
            Log.e(TAG, "init : error parameter");
            return -1;
        }

        if ( mVideoWidth <= 0 || mVideoHeight <= 0 ||  mBitrate <= 0 || mFps <= 0) {
            Log.e(TAG, "init : error video parameter");
            return -1;
        }

        if (mMime != ENCODER_TYPE_AVC && mMime != ENCODER_TYPE_HEVC ) {
            Log.e(TAG, "init ; error encoder type");
            return -1;
        }

        if (!mIsInited){
            mMediaProjection = (MediaProjection)mp;
            mDpi = dpi;

            mEncoderData = new byte[mVideoWidth * mVideoHeight];
            mEncoderSpecData = new byte[MAX_ENCODER_SPEC_DATA_SIZE];

            // sometimes, we need get video format describe from codec before start
            //checkEncoder(mMime, mVideoWidth, mVideoHeight, mFps, mBitrate, mProfile ,mLevel, mHasBframe);

            mIsInited = true;
        }

        if(VERBOSE)Log.d(TAG, "init : end to init");
        return 0;
    }

    public synchronized int getSpecData(byte[] data, int[] size)
    {
        if (data == null || size == null)
            return -1;

        int dataCapacity = data.length;

        if (mEncoderSpecDataSize > dataCapacity) {
            Log.e(TAG, "getSpecData : encoder spec data is too larger than " + dataCapacity);
            return -1;
        }

        if(mEncoderSpecDataSize > 0){
            System.arraycopy(mEncoderSpecData, 0, data, 0, mEncoderSpecDataSize);
        }

        size[0] = mEncoderSpecDataSize;
        return 0;
    }

    public synchronized int start()
    {
        if(VERBOSE)Log.d(TAG, "start : begin to start");

        if (!mIsInited){
            Log.e(TAG, "start : you need init first");
            return -1;
        }

        if (!mIsStarted)
        {
             //create media encoder
            if ((mMediaEncoder = createEncoder(mMime)) == null){
                Log.e(TAG, "start : create media encoder failed");
                return -1;
            }

            //start media encoder
            if ((mCodecInputSurface = startEncoder(mMediaEncoder, mMime, mVideoWidth, mVideoHeight, mFps, mBitrate, mProfile, mLevel, mHasBframe)) == null){
                Log.e(TAG, "start : start media encoder failed");
                return -1;
            }

            //create texture encoder
            if ((mTextureEncoder = createTextureEncoder(mCodecInputSurface, mVideoWidth, mVideoHeight, mFps)) == null){
                Log.e(TAG, "start : create texture encoder failed");
                return -1;
            }

            //start texture encoder
            if (!mTextureEncoder.start()){
                Log.e(TAG, "start : start texture encoder failed");
            }

            mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG + "-display",
                    mVideoWidth, mVideoHeight, mDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    mTextureEncoder.getSurface(), null, null);

            if(VERBOSE)Log.d(TAG, "start : created virtual display: " + mVirtualDisplay);

            mIsStarted = true;
        }

        if(VERBOSE)Log.d(TAG, "start : end to start");
        return 0;
    }

    public synchronized int stop()
    {
        if(VERBOSE)Log.d(TAG, "stop : begin to stop");

        if (!mIsInited){
            Log.e(TAG, "stop : you need init first");
            return -1;
        }

        // signal end of stream to media encoder
        if (mMediaEncoder != null){
            mMediaEncoder.signalEndOfInputStream();
        }

        // free texture encoder
        if (mTextureEncoder != null){
            mTextureEncoder.stop();
            mTextureEncoder = null;
        }

        // release virtual display
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        // free media encoder
        if (mMediaEncoder != null){
            mMediaEncoder.stop();
            mMediaEncoder.release();
            mMediaEncoder = null;
        }

        // release input surface
        if (mCodecInputSurface != null){
            mCodecInputSurface.release();
            mCodecInputSurface = null;
        }

        mIsStarted = false;

        if(VERBOSE)Log.d(TAG, "stop : end to stop");
        return 0;
    }

    private synchronized void checkEncoder(int mime, int width, int height, int fps, int bitrate, int profile, int level, boolean hasBframe)
    {
        if (VERBOSE)Log.d(TAG, "checkEncoder : begin to check encoder");

        MediaCodec codec = createEncoder(mime);
        if (codec == null){
            Log.e(TAG, "checkEncoder : create encoder failed");
            return;
        }

        Surface inputSurface = startEncoder(codec, mime, width, height, fps, bitrate, profile, level, hasBframe);
        if (inputSurface == null){
            Log.e(TAG, "checkEncoder ; start encoder failed");
            codec.release();
            return;
        }

        MediaFormat format = null;
        int tryCheckFormatTime = 0;

        while (tryCheckFormatTime ++ < 100 ){
            format = checkOutputFormat(codec, mime);
            if (format != null){
                break;
            }

            try {
                Thread.sleep(10);
            }catch (InterruptedException ex){
                ex.printStackTrace();
            }

            tryCheckFormatTime ++;
        }

        if (format != null){
            parseMediaFormat(format, mime);
        }

        codec.stop();
        codec.release();
        inputSurface.release();

        if (VERBOSE)Log.d(TAG, "checkEncoder : end to check encoder");
    }

    private SurfaceTextureEncoder createTextureEncoder(@NonNull Surface surface, int width, int height, int fps)
    {
        if (width <= 0 || height <= 0 || fps <= 0){
            Log.e(TAG, "createTextureEncoder : un-support parameter");
            return null;
        }

        SurfaceTextureEncoder textureEncoder = new SurfaceTextureEncoder(surface,width, height, fps);
        textureEncoder.setFrameCallBack(new SurfaceTextureEncoder.onFrameCallBack() {
            @Override
            public void encodeUpdate(boolean last){
                encodeFrame(last);
            };
            @Override
            public void onCutScreen(Bitmap bitmap) {
            }
        });

        return textureEncoder;
    }

    private MediaCodec createEncoder(int mime)
    {
        String mimeType = getMimeType(mime);
        if (mimeType == null) {
            Log.e(TAG, "createEncoder : error mimi type index " + mime);
            return null;
        }

        MediaCodec codec = createMediaEncoderForType(mimeType);
        if (codec == null) {
            Log.e(TAG, "createEncoder : create media encoder failed for " + mimeType);
            return null;
        }

        return codec;
    }

    private synchronized Surface startEncoder(@NonNull MediaCodec codec, int mime, int width, int height, int fps, int bitrate, int profile, int level, boolean hasBframe)
    {
        if (VERBOSE)Log.d(TAG, "startEncoder : start media encoder");

        Surface inputSurface;
        String mimeType = getMimeType(mime);
        if (mimeType == null) {
            Log.e(TAG, "startEncoder : error mimi type index " + mime);
            return null;
        }

        MediaFormat format = MediaFormat.createVideoFormat(mimeType, width, height);
        format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        // we must set profile and level together, or exception will happen
        // after api 23, we can set level
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // check is support these profile and level
            if (checkProfileAndLevel(codec, mimeType, profile, level)){
                // main/high profile was supported after api 24, and these will default enable b frame
                if (profile != MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline && hasBframe){
                    format.setInteger(MediaFormat.KEY_PROFILE, profile);
                    format.setInteger(MediaFormat.KEY_LEVEL, level);
                }
            }
        }

        try{
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            inputSurface = codec.createInputSurface();
            codec.start();
        }catch (IllegalStateException ex){
            ex.printStackTrace();
            codec.stop();
            return null;
        }

        return inputSurface;
    }

    private MediaFormat checkOutputFormat(@NonNull MediaCodec codec, int mime)
    {
        MediaFormat format = codec.getOutputFormat();
        if (mime == ENCODER_TYPE_AVC && format.getByteBuffer("csd-0") != null && format.getByteBuffer("csd-1") != null){
            return format;
        }else if (mime == ENCODER_TYPE_HEVC && format.getByteBuffer("csd-0") != null){
            return format;
        }

        return null;
    }

    private void parseMediaFormat(@NonNull MediaFormat format, int mime)
    {
        byte[] configArray = null;
        int configSize = 0;

        // parse config data from media format
        try{
            if (mime == ENCODER_TYPE_AVC){
                ByteBuffer sps = format.getByteBuffer("csd-0");
                ByteBuffer pps = format.getByteBuffer("csd-1");
                byte[] spsArray = sps.array();
                byte[] ppsArray = pps.array();

                configSize = spsArray.length + ppsArray.length;
                configArray = new byte[configSize];
                System.arraycopy(spsArray, 0, configArray, 0, spsArray.length);
                System.arraycopy(ppsArray, 0, configArray, spsArray.length, ppsArray.length);
            }else if(mime == ENCODER_TYPE_HEVC){
                ByteBuffer config = format.getByteBuffer("csd-0");

                configSize = config.array().length;
                configArray = config.array();
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }

        saveSpecData(configArray, configSize);
    }

    private void saveSpecData( byte[] array, int size)
    {
        if (VERBOSE){
            for (int i = 0; i < size; i++){
                Log.d(TAG, String.format("saveSpecData : data[%d], 0x%2x", i,array[i]));
            }
        }
        // config size check
        if (size > MAX_ENCODER_SPEC_DATA_SIZE || size <= 0 || array == null){
            Log.e(TAG, "saveSpecData : un-support spec parameter ");
            return;
        }

        if (mEncoderSpecDataSize == 0){
            // just deliver first config data
            System.arraycopy(array, 0, mEncoderSpecData, 0, size);
            mEncoderSpecDataSize = size;
        }else{
            // compare new config data to the old
            if (size != mEncoderSpecDataSize){
                Log.w(TAG, String.format("saveSpecData : new spec size %d is not equal to the old size %d",  size, mEncoderSpecDataSize));
            }else{
                for(int i = 0; i< size; i++){
                    if (array[i] != mEncoderSpecData[i]){
                        Log.w(TAG, "saveSpecData : new spec data is not equal to the old data");
                        break;
                    }
                }
            }
        }
    }

    private void encodeFrame(boolean last)
    {
        boolean deliver = true;
        if (last){
            deliver = false;
        }

        if (dequeEncoderOutputBuffer(mMediaEncoder, mBufferInfo, mEncoderData, ENCODER_DEQUE_OUTPUT_BUFFER_TIMEOUT, mUserData, deliver)){
            while (true){
                if (dequeEncoderOutputBuffer(mMediaEncoder, mBufferInfo, mEncoderData, 0, mUserData, deliver)){
                    if (VERBOSE)Log.d(TAG, "deque encoder output buffer again");
                }else{
                    break;
                }
            }
        }
    }

    private boolean dequeEncoderOutputBuffer(@NonNull MediaCodec codec, @NonNull MediaCodec.BufferInfo bufferInfo, @NonNull byte[] encoderData, int timeOut, long userData, boolean deliver)
    {
        int index = codec.dequeueOutputBuffer(bufferInfo, timeOut);
        if (index == MediaCodec.INFO_TRY_AGAIN_LATER){
            if (VERBOSE) Log.d(TAG, "dequeEncoderOutputBuffer: try again later");
        }else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
            MediaFormat format = codec.getOutputFormat();
            if (VERBOSE) Log.d(TAG, "dequeEncoderOutputBuffer: output format change");
        }else if (index >= 0){
            int type = ENCODER_PACKET_NON_KEY;
            long pts = bufferInfo.presentationTimeUs;
            int size = bufferInfo.size;
            int offset = bufferInfo.offset;

            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0){
                type = ENCODER_PACKET_KEY;
            }else if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0){
                type = ENCODER_PACKET_CONFIG;
                Log.d(TAG, "dequeEncoderOutputBuffer, get codec config frame " + size);
            }else if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                Log.d(TAG, "dequeEncoderOutputBuffer, get end of stream");
                return false;
            }

            ByteBuffer packet = codec.getOutputBuffer(index);
            if (packet != null){
                packet.position(offset);
                packet.limit(offset + size);
                packet.get(encoderData, 0, size);

                if (VERBOSE)Log.d(TAG, "dequeEncoderOutputBuffer, packet size is " + size + ", packet pts is " + pts + ", packet type is " + type + ",packet offset is " + offset);

                if (deliver && userData > 0){
                    robin_screen_capture_on_packet(encoderData, size, type, userData);
                }else{
//                    try {
//                        if (mFileOutput == null){
//                            File file = new File(Environment.getExternalStorageDirectory(), "record-" + mVideoWidth + "x" + mVideoHeight + "-"+ mBitrate / 1024 + "-"+ mProfile + ".h264");
//                            mFileOutput = new FileOutputStream(file);
//                        }
//                        mFileOutput.write(encoderData, 0, size);
//                    }catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
                }
            }

            codec.releaseOutputBuffer(index, false);
            return true;
        }

        return false;
    }

    private MediaCodec createMediaEncoderForType(@NonNull String mimeType)
    {
        MediaCodec mediaEncoder;
        MediaCodecInfo hwEncoderInfo = getHardWareCodecInfo(mimeType);

        try{
            if (hwEncoderInfo != null){
                mediaEncoder = MediaCodec.createByCodecName(hwEncoderInfo.getName());
            }else{
                mediaEncoder = MediaCodec.createEncoderByType(mimeType);
            }
        }catch (IOException e) {
            Log.e(TAG, "create media encoder failed");
            return null;
        }

        return mediaEncoder;
    }

    private MediaCodecInfo getHardWareCodecInfo(@NonNull String mimeType)
    {
        List<MediaCodecInfo> codecList = getEncoderInfoByType(mimeType);
        for (MediaCodecInfo codecInfo : codecList)
        {
            if (isSoftwareCodec(codecInfo))
                continue;

            return codecInfo;
        }

        return null;
    }

    private List<MediaCodecInfo> getEncoderInfoByType(@NonNull String mimeType)
    {
        LinkedList<MediaCodecInfo> needCodecInfo = new LinkedList<>();
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] allCodecInfo = codecList.getCodecInfos();

        for(MediaCodecInfo info : allCodecInfo) {
            if (!info.isEncoder())
                continue;

            String[] supportedTypes = info.getSupportedTypes();
            for (String type :supportedTypes){
                if (type.equalsIgnoreCase(mimeType)) {
                    needCodecInfo.push(info);
                    break;
                }
            }
        }

        return needCodecInfo;
    }

    private boolean isSoftwareCodec(@NonNull MediaCodecInfo codecInfo)
    {
        String componentName = codecInfo.getName();
        return (!componentName.startsWith("OMX.") || componentName.startsWith("OMX.google."));
    }

    private boolean checkProfileAndLevel(@NonNull MediaCodec codec, @NonNull String mimeType, int profile, int level)
    {
        MediaCodecInfo.CodecProfileLevel[] profileLevels = codec.getCodecInfo().getCapabilitiesForType(mimeType).profileLevels;
        if (profileLevels != null){
            //search profile and level
            for (MediaCodecInfo.CodecProfileLevel entry : profileLevels){
                if (entry.profile == profile && entry.level == level){
                    return true;
                }
            }
        }
        return false;
    }

    private String getMimeType(int mime)
    {
        String mimeType = null;

        switch (mime){
            case ENCODER_TYPE_AVC:
                mimeType = "video/avc";
                break;
            case ENCODER_TYPE_HEVC:
                mimeType = "video/hevc";
                break;
            default:
                break;
        }

        return mimeType;
    }
}
