package com.codyy.robinsdk.impl;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.NonNull;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;


/**
 * Created by ghost on 2017/11/7.
 */

public class Ahc2VideoCapture implements SurfaceHolder.Callback{

    private static final String TAG = "AhcVideoCapture";
    private static final boolean VERBOSE = true;
    private static final int MAX_CAMERA_NUM = 2;

    private CameraManager mCameraManager = null;
    private CameraDevice mCameraDevice = null;
    private CaptureRequest.Builder mPreviewRequestBuilder = null;
    private CaptureRequest mPreviewRequest = null;
    private CameraCaptureSession mCaptureSession = null;
    private ImageReader mImageReader = null;
    private HandlerThread mBackgroundThread = null;
    private Handler mBackgroundHandler = null;

    private boolean mFlashSupported = false;

    private String mCurrentCameraId = null;
    private Size mPreviewSize = null;
    private int mFormat = ImageFormat.YUV_420_888;
    private Surface mPreviewSurface = null;


    private long mUserData = 0;

    public Ahc2VideoCapture(long userData, CameraManager manager)
    {
        mUserData = userData;
        mCameraManager = manager;
    }

    public static int getCameraNum()
    {
        return MAX_CAMERA_NUM;
    }

//    public static List<Camera.Size> getSupportPreviewResolution(int cameraID)
//    {
//
//        return sizes;
//    }
//
//    public static List<Camera.Size> getSupportPictureResolution(int cameraID)
//    {
//
//
//        return sizes;
//    }
//
//    public static List<Integer> getSupportFormat(int cameraID)
//    {
//
//
//
//
//        return formats;
//    }

    public static String getCameraName(int cameraID)
    {
        if (cameraID > MAX_CAMERA_NUM -1 || cameraID < 0){
            Log.e(TAG, String.format("getCameraName : invalid camera id %d", cameraID));
            return null;
        }

        String name = null;
        if (cameraID == CameraCharacteristics.LENS_FACING_FRONT) {
            name = "Front Camera";
        }else if(cameraID == CameraCharacteristics.LENS_FACING_BACK){
            name = "Back Camera";
        }

        return name;
    }

    public synchronized int setCamera(int cameraID)
    {
        if (VERBOSE)Log.d(TAG, "setCamera : camera index is " + cameraID);

        if (cameraID > MAX_CAMERA_NUM -1 || cameraID < 0){
            Log.e(TAG, String.format("setCamera : invalid camera id %d", cameraID));
            return -1;
        }

        try {
            String[] cameraSigns = mCameraManager.getCameraIdList();
            for (String cameraSign : cameraSigns){
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraSign);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == cameraID){
                    mCurrentCameraId = cameraSign;
                    return 0;
                }
            }

        }catch (CameraAccessException caex){
            Log.e(TAG, "setCamera, camera access exception happened :" + caex);
            caex.printStackTrace();
        }

        return -1;
    }

    public synchronized int setPreviewResolution(int width, int height)
    {
        if (VERBOSE)Log.d(TAG, "setPreviewResolution : resolution is " + width + "x" + height);

        if (mCurrentCameraId == null){
            Log.e(TAG, "setPreviewResolution : you should set valid camera first");
            return -1;
        }

        try {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCurrentCameraId);
            StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mPreviewSize = getOptimalSize(configs.getOutputSizes(mFormat), width, height);
            if (mPreviewSize != null) {
                if (mPreviewSize.getWidth() != width || mPreviewSize.getHeight() != height){
                    Log.w(TAG, String.format("setPreviewResolution, actual resolution is %dx%d", mPreviewSize.getWidth(), mPreviewSize.getHeight()));
                }
                return 0;
            }
        }catch (CameraAccessException caex){
            Log.e(TAG, "setCamera, camera access exception happened :" + caex);
            caex.printStackTrace();
        }

        return -1;
    }

    public synchronized int setPictureResolution(int width, int height)
    {
        if (VERBOSE)Log.d(TAG, "setPictureResolution : resolution is " + width + "x" + height);

        return 0;
    }

    public synchronized int setFormat(int colorSpace)
    {
        if (VERBOSE)Log.d(TAG, "setFormat : format is " + colorSpace);

        if (mCurrentCameraId == null){
            Log.e(TAG, "setFormat : you should set valid camera first");
            return -1;
        }

        try {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCurrentCameraId);
            StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            for(int format : configs.getOutputFormats()){
                if(format == colorSpace) {
                    mFormat = format;
                    return 0;
                }
            }
        }catch (CameraAccessException caex){
            Log.e(TAG, "setFormat, camera access exception happened :" + caex);
            caex.printStackTrace();
        }

        return -1;
    }

    public synchronized int setFrameRate(int fps)
    {
        if (VERBOSE)Log.d(TAG, "setFrameRate : frameRate is " + fps);
        if (mCurrentCameraId == null){
            Log.e(TAG, "setFormat : you should set valid camera first");
            return -1;
        }

        try {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCurrentCameraId);
            Range<Integer>[] fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            for (Range<Integer> range : fpsRanges){
                Log.d(TAG, "setFrameRate " + range);
            }
        }catch (CameraAccessException caex){
            Log.e(TAG, "setFormat, camera access exception happened :" + caex);
            caex.printStackTrace();
        }

        return -1;
    }

    public synchronized int setScreenOrientation(int orientation)
    {
        if (VERBOSE)Log.d(TAG, "setScreenOrientation : orientation is " + orientation);

        if (mCurrentCameraId == null){
            Log.e(TAG, "setFormat : you should set valid camera first");
            return -1;
        }

        try {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCurrentCameraId);
            StreamConfigurationMap configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        }catch (CameraAccessException caex){
            Log.e(TAG, "setFormat, camera access exception happened :" + caex);
            caex.printStackTrace();
        }

        return -1;

    }

    public synchronized int start(Object surface)
    {
        if (VERBOSE)Log.d(TAG, "start : begin to start");

        startBackgroundThread();
        setupPreviewSurface((SurfaceView)surface);
        setupImageReaderSurface();

        try {
            //打开相机，第一个参数指示打开哪个摄像头，第二个参数stateCallback为相机的状态回调接口，
            //第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            mCameraManager.openCamera(mCurrentCameraId, mStateCallback, null);
        } catch (CameraAccessException ex) {
            Log.e(TAG, "start : camera access exception happened :" + ex);
            ex.printStackTrace();
        }

        if (VERBOSE)Log.d(TAG, "start : end to start");
        return 0;
    }

    public synchronized int takePicture()
    {
        if (VERBOSE)Log.d(TAG, "takePicture : take a picture");



        return 0;
    }

    public synchronized int stop()
    {
        if (VERBOSE)Log.d(TAG, "stop, begin to stop");

//        try {
//            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
//        } catch (InterruptedException e) {
//            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
//        } finally {
//           // mCameraOpenCloseLock.release();
//        }

        stopBackgroundThread();

        if (VERBOSE)Log.d(TAG, "stop : end to stop");
        return 0;
    }

    private void setupPreviewSurface(SurfaceView view)
    {
        if (view != null){
            SurfaceHolder holder = ((SurfaceView)view).getHolder();
            if (holder != null) {
                holder.addCallback(this);
                mPreviewSurface = holder.getSurface();
            }
        }
    }

    private void setupImageReaderSurface()
    {
        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), mFormat, 2);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {

                //Log.d(TAG, "onImageAvailable: ");
                
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image != null) {
                        int format = image.getFormat();
                        int width = image.getWidth();
                        int height = image.getHeight();

                        Log.d(TAG, String.format("onImageAvailable: format %d, size %dx%d", format, width, height));
                    }

                }catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    if (image != null) {
                        image.close();
                    }
                }

            }
        }, null);
    }

    private void startBackgroundThread()
    {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread()
    {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            // This method is called when the camera is opened.  We start camera preview here.
            Log.d(TAG, "CameraDevice.StateCallback, onOpened");
            mCameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.d(TAG, "CameraDevice.StateCallback, onDisconnected");
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.d(TAG, "CameraDevice.StateCallback, onError");
            camera.close();
            mCameraDevice = null;
        }
    };

    private void createCameraPreviewSession() {
        try {
            // we set up a CaptureRequest.Builder with the output Surface.
            Vector<Surface> outputSurface = new Vector<>() ;
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            // add image reader surface
            if (mImageReader != null){
                mPreviewRequestBuilder.addTarget(mImageReader.getSurface());
                outputSurface.addElement(mImageReader.getSurface());
            }

            // add preview surface
//            if (mPreviewSurface != null){
//                mPreviewRequestBuilder.addTarget(mPreviewSurface);
//                outputSurface.addElement(mPreviewSurface);
//            }

            if (outputSurface.size() == 0){
                Log.e(TAG, "createCameraPreviewSession, we can't get valid output surface");
                return;
            }

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                //setAutoFlash(mPreviewRequestBuilder);

                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<Integer>(30, 30));

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest, null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "onConfigureFailed, camera capture session config failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    private Size getOptimalSize(Size[] sizeMap, int width, int height)
    {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() >= width && option.getHeight() >= height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() >= height && option.getHeight() >= width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return sizeMap[0];
    }

    @Override
    public synchronized void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        Log.d(TAG, "surfaceChanged : surface change " + format + ": " + width + "x" + height);
    }

    @Override
    public synchronized void surfaceCreated(SurfaceHolder holder)
    {
        Log.d(TAG, "surfaceCreated : surface create");
    }

    @Override
    public synchronized void surfaceDestroyed(SurfaceHolder holder)
    {
        Log.d(TAG, "surfaceDestroyed : surface destroy");
    }
}
