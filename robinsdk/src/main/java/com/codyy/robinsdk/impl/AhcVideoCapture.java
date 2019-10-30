package com.codyy.robinsdk.impl;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Environment;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

/**
 * Created by liuhao on 2016/9/27.
 */

public class AhcVideoCapture
{
    private static final boolean isDebugDump = false;
    private static final String TAG = "AhcVideoCapture";
    private static final boolean VERBOSE = false;
    private final int MinPreviewFps = 10;

    private static Vector<CameraInfo> mCameraInfos = new Vector<>();
    private Camera mCamera = null;

    private int mCurrentCameraId = -1;//有效的摄像头设备索引，从0开始
    private int mOrientation = 1; //竖屏
    private int mPreviewWidth = 320;
    private int mPreviewHeight = 240;
    private int mPictureWidth = 0;
    private int mPictureHeight = 0;
    private boolean mIsNeedTakePicture = false;

    private int mFormat = ImageFormat.NV21;//camera必须支持NV21
    private int mFps = 25;
    private int[] mFpsMaxRange = null;

    private boolean mIsStarted = false;
    private final int numCaptureBuffers = 3;

    //for framerate compute
    private long lastCaptureTimeMs = 0;
    private long durationTimePerSecond = 0;
    private int statisticalFrameCount = 0;

    //for framerate control
    private final boolean bFrameRateControl = true;
    private int frameCount = 0;
    private long tickCount = 0;
    private int dropFrameInterval = -1;
    private boolean dropFrameJitter = false;

    private long mUserData = 0;

    public static native void robin_camera_on_preview_frame(byte[] data, int length, long userData);
    public static native void robin_camera_on_take_picture(byte[] data, int length, long userData);

    public AhcVideoCapture(long userData)
    {
        mUserData = userData;
        checkCameraInfo();
    }

    private static class CameraInfo
    {
        private int mIndex = -1;
        private String mCameraName = null;
        private List<Camera.Size> mSupportPreviewResolution = null;
        private List<Camera.Size> mSupportPictureResolution = null;
        private List<Integer> mSupportPreviewFormat = null;
        private List<Integer> mSupportPictureFormat = null;
        private int[] mSupportMaxPreviewFpsRange = null;

        boolean init(int cameraID)
        {
            Camera camera = null;
            if (cameraID > Camera.getNumberOfCameras() -1 || cameraID < 0){
                Log.e("CameraInfo", String.format("init : invalid camera id %d", cameraID));
                return false;
            }

            try {
                camera = Camera.open(cameraID);
            }catch (Exception ex){
                Log.e("CameraInfo", String.format("init : open camera %d failed with %s", cameraID, ex.toString()));
                return false;
            }

            Camera.Parameters parameters = camera.getParameters();
            mSupportPreviewResolution = parameters.getSupportedPreviewSizes();
            mSupportPictureResolution = parameters.getSupportedPictureSizes();
            mSupportPreviewFormat = parameters.getSupportedPreviewFormats();
            mSupportPictureFormat = parameters.getSupportedPictureFormats();
            mSupportMaxPreviewFpsRange = determineMaximumSupportedFramerate(parameters);
            mIndex = cameraID;

            if (cameraID == Camera.CameraInfo.CAMERA_FACING_BACK){
                mCameraName = "Back";
            }else if(cameraID == Camera.CameraInfo.CAMERA_FACING_FRONT){
                mCameraName = "Front";
            }

            if(isDebugDump)
                testCameraParameters(camera);

            camera.release();
            return  true;
        }
    }

    private static void checkCameraInfo()
    {
       synchronized (AhcVideoCapture.class) {
           if (mCameraInfos.size() == 0){
               for(int i = 0; i<Camera.getNumberOfCameras(); i++){
                   CameraInfo info = new CameraInfo();
                   if (info.init(i))
                       mCameraInfos.addElement(info);
               }
           }
       }
    }

    public static int getCameraNum()
    {
        checkCameraInfo();

        return mCameraInfos.size();
    }

    public static List<Camera.Size> getSupportPreviewResolution(int cameraID)
    {
        if (cameraID > mCameraInfos.size() -1 || cameraID < 0){
            Log.e(TAG, String.format("getSupportPreviewResolution : invalid camera id %d", cameraID));
            return null;
        }

        List<Camera.Size> sizes = null;
        for(int i =0; i< mCameraInfos.size(); i++){
            if(cameraID == i){
                CameraInfo info = mCameraInfos.get(i);
                sizes = info.mSupportPreviewResolution;
            }
        }

        return sizes;
    }

    public static List<Camera.Size> getSupportPictureResolution(int cameraID)
    {
        if (cameraID > mCameraInfos.size() -1 || cameraID < 0){
            Log.e(TAG, String.format("getSupportPictureResolution : invalid camera id %d", cameraID));
            return null;
        }

        List<Camera.Size> sizes = null;
        for(int i =0; i< mCameraInfos.size(); i++){
            if(cameraID == i){
                CameraInfo info = mCameraInfos.get(i);
                sizes = info.mSupportPictureResolution;
            }
        }

        return sizes;
    }

    public static List<Integer> getSupportFormat(int cameraID)
    {
        if (cameraID > mCameraInfos.size() -1 || cameraID < 0){
            Log.e(TAG, String.format("getSupportFormat : invalid camera id %d", cameraID));
            return null;
        }

        List<Integer> formats = null;
        for(int i =0; i< mCameraInfos.size(); i++){
            if(cameraID == i){
                CameraInfo info = mCameraInfos.get(i);
                formats = info.mSupportPreviewFormat;
            }
        }

        return formats;
    }

    public static String getCameraName(int cameraID)
    {
        if (cameraID > mCameraInfos.size() -1 || cameraID < 0){
            Log.e(TAG, String.format("getCameraName : invalid camera id %d", cameraID));
            return null;
        }

        String name = null;
        for(int i =0; i< mCameraInfos.size(); i++){
            if(cameraID == i){
                CameraInfo info = mCameraInfos.get(i);
                name = info.mCameraName;
            }
        }

        return name;
    }

    private static int[] determineMaximumSupportedFramerate(Camera.Parameters parameters)
    {
        int[] maxFps = new int[]{0,0};
        String supportedFpsRangesStr = "supported frame rates: ";
        List<int[]> supportedFpsRanges = parameters.getSupportedPreviewFpsRange();
        for (Iterator<int[]> it = supportedFpsRanges.iterator(); it.hasNext();) {
            int[] interval = it.next();
            supportedFpsRangesStr += interval[0]/1000+"-"+interval[1]/1000+"fps"+(it.hasNext()?", ":"");
            if (interval[1]>maxFps[1] || (interval[0]>maxFps[0] && interval[1]==maxFps[1]))
            {
                maxFps = interval;
            }
        }

        return maxFps;
    }

    public synchronized int setCamera(int cameraID)
    {
        if (VERBOSE)Log.d(TAG, "setCamera : camera index is " + cameraID);

        if (cameraID > mCameraInfos.size() -1 || cameraID < 0){
            Log.e(TAG, String.format("setCamera : invalid camera id %d", cameraID));
            return -1;
        }

        mCurrentCameraId = cameraID;

        return  0;
    }

    public synchronized int setPreviewResolution(int width, int height)
    {
        if (VERBOSE)Log.d(TAG, "setPreviewResolution : resolution is " + width + "x" + height);

        if (mCurrentCameraId < 0) {
            Log.e(TAG, "you should set valid camera index");
            return -1;
        }

        boolean support = false;
        CameraInfo info = mCameraInfos.get(mCurrentCameraId);
        if (info == null){
            Log.e(TAG, "setPreviewResolution : get camera info failed");
            return  -1;
        }

        for(Camera.Size entry:info.mSupportPreviewResolution){
            if (width == entry.width && height == entry.height) {
                support = true;
                break;
            }
        }

        if (!support) {
            Log.e(TAG, "setPreviewResolution : don't support this size " + width + "x" + height);
            return -1;
        }

        mPreviewWidth = width;
        mPreviewHeight = height;

        return 0;
    }

    public synchronized int setPictureResolution(int width, int height)
    {
        if (VERBOSE)Log.d(TAG, "setPictureResolution : resolution is " + width + "x" + height);

        if (mCurrentCameraId < 0) {
            Log.e(TAG, "you should set valid camera index");
            return -1;
        }

        boolean supportSize = false;
        boolean supportFormat = false;
        CameraInfo info = mCameraInfos.get(mCurrentCameraId);
        if (info == null){
            Log.e(TAG, "setPictureResolution : get camera info failed");
            return  -1;
        }

        for(Camera.Size entry:info.mSupportPictureResolution){
            if (width == entry.width && height == entry.height) {
                supportSize = true;
                break;
            }
        }

        for(Integer entry:info.mSupportPictureFormat){
            if (entry == ImageFormat.JPEG) {
                supportFormat = true;
                break;
            }
        }

        if (!supportSize || !supportFormat) {
            Log.e(TAG, "setPictureResolution : don't support this size " + width + "x" + height + ", or picture don't support JPEG");
            return -1;
        }

        mPictureWidth = width;
        mPictureHeight = height;
        mIsNeedTakePicture = true;

        return 0;
    }

    public synchronized int setFormat(int colorSpace)
    {
        if (VERBOSE)Log.d(TAG, "setFormat : format is " + colorSpace);

        if (mCurrentCameraId < 0) {
            Log.e(TAG, "you should set valid camera index");
            return -1;
        }

        boolean support = false;
        CameraInfo info = mCameraInfos.get(mCurrentCameraId);
        if (info == null){
            Log.e(TAG, "setFormat : get camera info failed");
            return -1;
        }

        for(Integer entry:info.mSupportPreviewFormat){
            if (entry == colorSpace){
                support = true;
                break;
            }
        }

        if (!support) {
            Log.e(TAG, "setFormat : don't support this format " + colorSpace);
            return -1;
        }

        mFormat = colorSpace;

        return 0;
    }

    public synchronized int setFrameRate(int fps)
    {
        if (VERBOSE)Log.d(TAG, "setFrameRate : frameRate is " + fps);

        if (mCurrentCameraId < 0) {
            Log.e(TAG, "you should set valid camera index");
            return -1;
        }

        CameraInfo info = mCameraInfos.get(mCurrentCameraId);
        if (info == null){
            Log.e(TAG, "setFrameRate : get camera info failed");
            return  -1;
        }

        int[] fpsRange = info.mSupportMaxPreviewFpsRange;
        if (fps < MinPreviewFps || fps > fpsRange[1]) {
            Log.e(TAG, "setFrameRate : fps " + fps + " is not in range:" + MinPreviewFps + " to " + fpsRange[1]);
            return -1;
        }

        mFpsMaxRange = fpsRange;
        mFps = fps;

        return 0;
    }

    public synchronized int setScreenOrientation(int orientation)
    {
        if (VERBOSE)Log.d(TAG, "setScreenOrientation : orientation is " + orientation);

        if (orientation <0 || orientation > 3){
            Log.e(TAG, "setScreenOrientation : failed with orientation " + orientation);
            return -1;
        }

        if (mIsStarted && mCamera != null) {
            if (orientation == 0) {
                mCamera.setDisplayOrientation(90);
            } else if(orientation == 1){;
                mCamera.setDisplayOrientation(270);
            } else if(orientation == 2){
                mCamera.setDisplayOrientation(0);
            } else if(orientation == 3){
                mCamera.setDisplayOrientation(180);
            }
        }
       
        mOrientation = orientation;

        return 0;
    }

    public synchronized int start(Object view, int type)
    {
        if (VERBOSE)Log.d(TAG, "start : begin to start, is render view type " + type);

        if (mIsStarted){
            if (VERBOSE)Log.w(TAG, "start, camera has been started");
            return 0;
        }

        if (view == null) {
            Log.e(TAG, "start : render view is null !");
            return -1;
        }

        if (type != 0 && type != 1){
            Log.e(TAG, "start: invalid render view type" + type );
            return -1;
        }

        if (mCurrentCameraId < 0) {
            Log.e(TAG, "you should set valid camera index");
            return -1;
        }

        //创建Camera
        try{
            mCamera = Camera.open(mCameraInfos.get(mCurrentCameraId).mIndex);
        }catch (Exception e){
            Log.e(TAG, String.format("%s camera open failed with %s", mCameraInfos.get(mCurrentCameraId).mCameraName, e.toString()));
            return -1;
        }

        //配置Camera参数
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewFpsRange(mFpsMaxRange[0], mFpsMaxRange[1]);
        parameters.setPreviewSize(mPreviewWidth, mPreviewHeight);
        parameters.setPreviewFormat(mFormat);

        if(mIsNeedTakePicture){
            parameters.setPictureSize(mPictureWidth, mPictureHeight);
            parameters.setPictureFormat(PixelFormat.JPEG);
        }

        // 横竖屏镜头自动调整, 输出的视频分辨率会被调整
        if (mOrientation == 0) {
            parameters.set("orientation", "portrait");
            mCamera.setDisplayOrientation(90);
        } else if(mOrientation == 1){
            parameters.set("orientation", "portrait");
            mCamera.setDisplayOrientation(270);
        } else if(mOrientation == 2){
            parameters.set("orientation", "landscape");
            mCamera.setDisplayOrientation(0);
        } else if(mOrientation == 3){
            parameters.set("orientation", "landscape");
            mCamera.setDisplayOrientation(180);
        }

        mCamera.setParameters(parameters);

        //设置预览surface
        try {
            if (type == 0){
                SurfaceView surfaceView = (SurfaceView)view;
                mCamera.setPreviewDisplay(surfaceView.getHolder());
            }else if (type == 1){
                TextureView textureView = (TextureView)view;
                mCamera.setPreviewTexture(textureView.getSurfaceTexture());
            }
        } catch (IOException e) {
            Log.e(TAG, "start : set preview display failed");
            e.printStackTrace();
            return -1;
        }

        //设置Camera回调buffer
        int bufSize = mPreviewWidth * mPreviewHeight * ImageFormat.getBitsPerPixel(mFormat) / 8;
        for (int i = 0; i < numCaptureBuffers; i++) {
            mCamera.addCallbackBuffer(new byte[bufSize]);
        }

        // 设置视频帧回调接口
        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (data != null) {
                    handleIncomingFrame(data, data.length, System.nanoTime()/1000000);
                }
                camera.addCallbackBuffer(data);
            }
        });

        //设置Camera错误回调接口
        mCamera.setErrorCallback(new Camera.ErrorCallback(){
            public void onError(int error, Camera camera){
                if (error == Camera.CAMERA_ERROR_UNKNOWN){
                    Log.e(TAG, "onError : CAMERA_ERROR_UNKNOWN");
                }else if(error == Camera.CAMERA_ERROR_SERVER_DIED){
                    Log.e(TAG, "onError : CAMERA_ERROR_SERVER_DIED");
                }
                stop();
            }
        });

        try{
            mCamera.startPreview();
            mIsStarted = true;
        }catch (RuntimeException e){
            Log.e(TAG, String.format("%s camera start preview failed with %s", mCameraInfos.get(mCurrentCameraId).mCameraName, e.toString()));
            return -1;
        }

        if (VERBOSE)Log.d(TAG, "start : end to start");
        return 0;
    }

    public synchronized int takePicture()
    {
        if (VERBOSE)Log.d(TAG, "takePicture : take a picture");

        if (mIsNeedTakePicture) {
            if( mIsStarted && (mCamera != null)){
                mCamera.takePicture(mShutterCallback, mRawCallback, mJpegPictureCallback);
            }
        }else{
            Log.e(TAG, "takePicture : couldn't to tack a picture");
            return  -1;
        }

        return 0;
    }

    public synchronized int stop()
    {
        if (VERBOSE)Log.d(TAG, "stop, begin to stop");
        if (!mIsStarted) {
            if (VERBOSE)Log.d(TAG, "stop, camera has been stopped");
            return 0;
        }

        mCamera.stopPreview();
        mCamera.setPreviewCallbackWithBuffer(null);

        try {
            mCamera.setPreviewDisplay(null);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        mCamera.release();
        mCamera = null;
        mIsStarted = false;
        mIsNeedTakePicture = false;

        //reset flags
        lastCaptureTimeMs = 0;
        durationTimePerSecond = 0;
        statisticalFrameCount = 0;

        frameCount = 0;
        tickCount = 0;
        dropFrameInterval = -1;
        dropFrameJitter = false;

        if (VERBOSE)Log.d(TAG, "stop : end to stop");
        return 0;
    }

    private void handleIncomingFrame(@NonNull byte[] data, int length, long captureTimeMs)
    {
        //简单帧率控制
        if (bFrameRateControl) {
            if (dropFrameInterval == -1) {
                int frameRate = frameRateCompute(captureTimeMs);
                if (frameRate != 0 && mFps != 0 && frameRate > mFps){
                    int lostFrameNum = frameRate - mFps;
                    dropFrameInterval = frameRate / lostFrameNum;

                    if (frameRate % lostFrameNum != 0)
                        dropFrameJitter = true;

                    Log.d(TAG, "computed frame rate is " + frameRate + ",dst frame rate is "+ mFps +
                            ",drop frame interval is " + dropFrameInterval + ", drop frame jitter is " + dropFrameJitter);
                }
            }else if (frameDropControl()){
                return ;
            }
        }

       if(VERBOSE) Log.d(TAG, "handleIncomingFrame : length is " + length);

        if (isDebugDump){
            //saveToSDCard("Camera.yuv", data, length);
        }else if(mUserData != 0){
            robin_camera_on_preview_frame(data, length, mUserData);
        }
    }

    private boolean frameDropControl()
    {
        boolean ret = false;

        if(dropFrameInterval <= 0)
            return ret;

        frameCount ++;

        if (tickCount >= 4294967294L)
            tickCount = 0;

        if (dropFrameJitter)
        {
            if ((tickCount % 2 == 0 && frameCount == dropFrameInterval) ||
                    (tickCount % 2 == 1 && frameCount == dropFrameInterval + 1)){

                tickCount ++;
                ret = true;
                frameCount = 0;
            }
        } else if (frameCount == dropFrameInterval ) {

            ret = true;
            frameCount = 0;
        }

        return  ret;
    }

    private int frameRateCompute(long captureTimeMs)
    {
        int computedFrameRate = 0;

        statisticalFrameCount ++ ;
        if (lastCaptureTimeMs != 0) {
            long FrameDuration = captureTimeMs - lastCaptureTimeMs;
            durationTimePerSecond += FrameDuration;
            if (durationTimePerSecond > 1000) {
                computedFrameRate = statisticalFrameCount -1 ;
                if (VERBOSE)Log.d(TAG, "frameRateCompute : frame rate is " + computedFrameRate + ", and duration is " + durationTimePerSecond);

                durationTimePerSecond = 0;
                statisticalFrameCount = 0;
                lastCaptureTimeMs = -1;

                if (computedFrameRate >= 28 && computedFrameRate <= 32)
                    computedFrameRate = 30;

                return computedFrameRate;
            }
        }

        lastCaptureTimeMs = captureTimeMs;
        return  0;
    }

    //takePicture, the shutter callback
    private Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback()
    {
        public void onShutter() {
            // TODO Auto-generated method stub
            Log.i(TAG, "onShutter");
            mIsStarted = false;
        }
    };

    //takePicture, the raw data callback
    private Camera.PictureCallback mRawCallback = new Camera.PictureCallback()
    {
        public void onPictureTaken(byte[] data, Camera camera) {
            // TODO Auto-generated method stub
            Log.i(TAG, "onPictureTaken");
        }
    };

    //takePicture, the jpeg data callback
    private Camera.PictureCallback mJpegPictureCallback = new Camera.PictureCallback()
    {
        public void onPictureTaken(byte[] data, Camera camera) {
            // TODO Auto-generated method stub
            Log.i(TAG, "onPictureTaken");

            if (data == null)
                return;

            byte[] destData = null;
            if (mOrientation == Configuration.ORIENTATION_PORTRAIT){
                if (mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT){
                    destData =  getRotateJpeg(data, 270.0f, 90);
                }else{
                    destData =  getRotateJpeg(data, 90.0f, 90);
                }
            }else{
                destData = data;
            }

            if (destData != null){
                if (isDebugDump){
                    saveToSDCard("tackPicture2.jpeg", destData, destData.length);
                }else if(mUserData != 0){
                    robin_camera_on_take_picture(destData, destData.length, mUserData);
                }
            }

            if(!mIsStarted) {
                mCamera.startPreview();
                mIsStarted = true;
            }
        }
    };

    private static byte[] getRotateJpeg(@NonNull byte[] data, float rotateDegree, int quality){
        Bitmap src = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (src == null)
            return null;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotateDegree);
        Bitmap rotateDest = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, false);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        rotateDest.compress(Bitmap.CompressFormat.JPEG, quality, bout);

        return  bout.toByteArray();
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

    private static void testCameraParameters(@NonNull Camera camera)
    {
        List<Camera.Size> pictureSizes = camera.getParameters().getSupportedPictureSizes();
        List<Camera.Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();
        List<Camera.Size> videoSizes = camera.getParameters().getSupportedVideoSizes();
        List<Camera.Size> jpegThumbnailSizes = camera.getParameters().getSupportedJpegThumbnailSizes();
        List<Integer> previewFormats = camera.getParameters().getSupportedPreviewFormats();
        List<Integer> pictureFormats = camera.getParameters().getSupportedPictureFormats();

        Log.i(TAG, "support parameters is ");

        String pictureSizesStr = "picture sizes:";
        String previewSizesStr = "preview sizes:";
        String videoSizesStr = "video sizes:";
        String jpegThumbnailSizesStr = "jpeg Thumbnail sizes:";

        Camera.Size psize;
        if (pictureSizes != null){
            for (int i = 0; i < pictureSizes.size(); i++) {
                psize = pictureSizes.get(i);
                pictureSizesStr += String.format(Locale.CHINA, "%dx%d, ", psize.width, psize.height);
            }
        }

        if (previewSizes != null){
            for (int i = 0; i < previewSizes.size(); i++) {
                psize = previewSizes.get(i);
                previewSizesStr +=  String.format(Locale.CHINA, "%dx%d, ", psize.width, psize.height);
            }
        }

        if(videoSizes != null){
            for (int i = 0; i < videoSizes.size(); i++) {
                psize = videoSizes.get(i);
                videoSizesStr +=  String.format(Locale.CHINA, "%dx%d, ", psize.width, psize.height);
            }
        }

        if (jpegThumbnailSizes != null){
            for (int i = 0; i < jpegThumbnailSizes.size(); i++) {
                psize = jpegThumbnailSizes.get(i);
                jpegThumbnailSizesStr +=  String.format(Locale.CHINA,"%dx%d, ", psize.width, psize.height);
            }
        }

        Log.i(TAG, pictureSizesStr);
        Log.i(TAG, previewSizesStr);
        Log.i(TAG, videoSizesStr);
        Log.i(TAG, jpegThumbnailSizesStr);

        Integer pf;
        for (int i = 0; i < pictureFormats.size(); i++) {
            pf = pictureFormats.get(i);
            Log.i(TAG, "picture formats:" + pf);
        }

        for (int i = 0; i < previewFormats.size(); i++) {
            pf = previewFormats.get(i);
            Log.i(TAG, "preview formats:" + pf);
        }
    }
}
