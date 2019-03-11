package com.codyy.robinsdk.impl;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.Semaphore;


/**
 * Created by liuhao on 2017/7/23.
 */

public class SurfaceTextureRender implements SurfaceTexture.OnFrameAvailableListener{
    private static final String TAG = "SurfaceTextureRender";
    private static final boolean VERBOSE = false;

    private Thread mThread = null;
    private Semaphore mSurfaceSem = null;
    private Object mFrameSyncObject = null;
    private boolean mFrameAvailable = false;

    private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;

    private SurfaceView mSurfaceView = null;
    private SurfaceHolderCallBack mSurfaceHolderCallBack = null;
    private TextureView mTextureView = null;
    private SurfaceTexture mSurfaceTexture = null;
    private Surface mSetSurface = null;
    private Surface mGetSurface = null;
    private TextureRender mTextureRender = null;

    private int mSurfaceWidth = 0;
    private int mSurfaceHeight = 0;
    private int mVideoWidth = 0;
    private int mVideoHeight = 0;

    private boolean mIsUseOpengl = false;
    private boolean mIsSaveLastFrame = true;
    private boolean mIsAspectFill = false;
    private boolean mIsSurfaceChange = true;
    private boolean mIsStarted = false;

    static public class RenderRect
    {
        private int x;
        private int y;
        private int w;
        private int h;
        RenderRect(int _x, int _y, int _w, int _h){
            x = _x; y = _y; w = _w; h = _h;
        }
    }

    public synchronized void setRenderView(Object view, int type)
    {
        if(view != null) {
            if (type == 0){
                mSurfaceView = (SurfaceView)view;
            }else if (type == 1){
                mTextureView = (TextureView)view;
            }
        }
    }

    public synchronized void setVideoSize(int width, int height)
    {
        if (VERBOSE) Log.d(TAG, "setVideoSize : set video size is " + width + "x" + height);

        mVideoWidth = width;
        mVideoHeight = height;
    }

    public synchronized void updateRect(Object src, Object dst)
    {
        RenderRect srcRect = (RenderRect)src;
        RenderRect dstRect = (RenderRect)dst;

        if (VERBOSE) Log.d(TAG, String.format("updateRect : src[%d. %d. %d. %d], dst[%d. %d. %d. %d]",
                srcRect.x, srcRect.y, srcRect.w, srcRect.h, dstRect.x, dstRect.y, dstRect.w, dstRect.h));

        if (mTextureRender != null){
            mTextureRender.updateTextureRect(srcRect, dstRect);
        }
    }

    public synchronized void setSaveLastFrame(boolean isSave)
    {
        if (VERBOSE) Log.d(TAG, "setSaveLastFrame : set save last frame is " + isSave);

        mIsSaveLastFrame = isSave;
    }

    public synchronized boolean start(boolean useOpengl, boolean aspectFill)
    {
        if (VERBOSE) Log.d(TAG, "start : begin to start");

        if (mSurfaceView == null && mTextureView == null)
            return false;

        if (!mIsStarted) {
            if (mSurfaceView != null){
                mSurfaceHolderCallBack = new SurfaceHolderCallBack();
                mSurfaceView.getHolder().addCallback(mSurfaceHolderCallBack);
                mSetSurface = mSurfaceView.getHolder().getSurface();
                mSurfaceWidth = mSurfaceView.getWidth();
                mSurfaceHeight = mSurfaceView.getHeight();
            }else if (mTextureView != null){
                SurfaceTexture texture = mTextureView.getSurfaceTexture();
                mTextureView.setSurfaceTextureListener(new TextureViewListener());
                mSetSurface = new Surface(texture);
                mSurfaceWidth = mTextureView.getWidth();
                mSurfaceHeight = mTextureView.getHeight();
            }

            if (useOpengl) {
                mSurfaceSem = new Semaphore(0);
                mFrameSyncObject = new Object();
                mThread = new Thread(new SurfaceTextureRender.SurfaceTextureRenderThread());
                mThread.start();

                mIsUseOpengl = true;
                mIsAspectFill = aspectFill;
            }

            mIsStarted = true;
        }

        if (VERBOSE) Log.d(TAG, "start : end to start");
        return true;
    }

    public synchronized void stop()
    {
        if (VERBOSE) Log.d(TAG, "stop : begin to stop");

        if (mIsStarted) {
            if (mIsUseOpengl && mThread != null) {
                mThread.interrupt();
                try {
                    mThread.join();
                } catch (InterruptedException e) {
                    Log.e(TAG, "stop : stop thread failed with " + e.toString());
                }
                mThread = null;
                mIsUseOpengl = false;
            }

            mIsStarted = false;
            mIsSurfaceChange = true;
            mSurfaceView = null;

            if (mSurfaceView != null){
                mSurfaceView.getHolder().removeCallback(mSurfaceHolderCallBack);
                mSurfaceHolderCallBack = null;
                mSurfaceView = null;
            }

            if (mTextureView != null){
                mTextureView.setSurfaceTextureListener(null);
                mTextureView = null;
            }
        }

        if (VERBOSE) Log.d(TAG, "stop : end to stop");
    }

    public synchronized Object getSurface()
    {
        Surface surface;

        if (mIsUseOpengl) {
            try{
                mSurfaceSem.acquire();
            }
            catch (InterruptedException ex){
                ex.printStackTrace();
            }
            surface = mGetSurface;
        }else{
            surface = mSetSurface;
        }

        return (Object)surface;
    }

    private class SurfaceTextureRenderThread implements Runnable
    {
        @Override
        public void run()
        {
            boolean isInited= false;

            try{
                if(initEgl(mSetSurface) && initGLComponents()) {
                    mSurfaceSem.release();
                    isInited = true;
                }
            }catch (RuntimeException ex) {
                ex.printStackTrace();
            }

            while (isInited) {
                if (!newImage())
                    break;
            }

            releaseGLComponents();
            releaseEGL();

            if (VERBOSE) Log.d(TAG, "SurfaceTextureRenderThread : surface texture render thread exit");
        }
    }

    private boolean initEgl(Surface surface)
    {
        if (VERBOSE) Log.d(TAG, "initEgl : init egl");

        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("initEgl : unable to get EGL14 display");
        }

        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            throw new RuntimeException("initEgl : unable to initialize EGL14");
        }

        if (!bindAPI(mEGLDisplay, version[0], version[1])){
            throw new RuntimeException("initEgl : unable to bind EGL14 API");
        }

        EGLConfig config = chooseConfig(mEGLDisplay);
        if (config == null){
            throw new RuntimeException("initEgl : unable to choose egl config");
        }

        // Configure context for OpenGL ES 2.0.
        int[] ctxAttributes = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        mEGLContext = EGL14.eglCreateContext(mEGLDisplay, config, EGL14.EGL_NO_CONTEXT, ctxAttributes, 0);
        if (mEGLContext == null) {
            throw new RuntimeException("initEgl : unable to eglCreateContext");
        }

        // Creates an EGL window surface
        int[] surfaceAttributes = {
                EGL14.EGL_NONE
        };
        mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, config, surface, surfaceAttributes, 0);   //creates an EGL window surface and returns its handle
        if (mEGLSurface == null) {
            throw new RuntimeException("initEgl : unable to eglCreateWindowSurface");
        }

        if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
            throw new RuntimeException("initEgl : unable to eglMakeCurrent");
        }

        return true;
    }

    private void releaseEGL()
    {
        if (VERBOSE) Log.d(TAG, "releaseEGL ; release egl");

        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);

            if (mEGLSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
                mEGLSurface = EGL14.EGL_NO_SURFACE;
            }

            if (mEGLContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
                mEGLContext = EGL14.EGL_NO_CONTEXT;
            }

            EGL14.eglReleaseThread();
            EGL14.eglTerminate(mEGLDisplay);
            mEGLSurface = EGL14.EGL_NO_SURFACE;
        }
    }

    private boolean initGLComponents()
    {
        if (VERBOSE) Log.d(TAG, "initGLComponents : init gl components");

        mTextureRender = new TextureRender();
        if (!mTextureRender.create()) {
            Log.e(TAG, "initGLComponents : create texture render failed");
            return false;
        }

        mSurfaceTexture = new SurfaceTexture(mTextureRender.getTextureId());
        mSurfaceTexture.setOnFrameAvailableListener(this);
        mGetSurface = new Surface(mSurfaceTexture);

        return true;
    }

    private void releaseGLComponents()
    {
        if (VERBOSE) Log.d(TAG, "releaseGLComponents : release gl components");

        if (mTextureRender != null) {
            mTextureRender.destroy();
            mTextureRender = null;
        }

        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture.setOnFrameAvailableListener(null);
            mSurfaceTexture = null;
        }

        if (mGetSurface != null) {
            mGetSurface.release();
            mGetSurface = null;
        }
    }

    private boolean bindAPI(EGLDisplay display, int major, int minor)
    {
        printEGLInfo(display);

        if (!EGL14.eglBindAPI(EGL14.EGL_OPENGL_ES_API)) {
            return false;
        }

        return true;
    }

    private EGLConfig chooseConfig(EGLDisplay display)
    {
        // Configure EGL for windows and OpenGL ES 2.0, 24-bit RGB.
        int[] attributes  = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT, // EGL_WINDOW_BIT / EGL_PBUFFER_BIT
                EGL14.EGL_NONE
        };

        EGLConfig[] configs = new EGLConfig[1];
        int[] configNum = new int[1];

        if (!EGL14.eglChooseConfig(display, attributes, 0, configs, 0, configs.length, configNum, 0)) {
            Log.e(TAG, "chooseConfig : unable to eglChooseConfig");
            return null;
        }

        return configs[0];
    }

    private void printEGLInfo(EGLDisplay display)
    {
        if (display != null) {
            String vendor = EGL14.eglQueryString(display, EGL14.EGL_VENDOR);
            if (VERBOSE) Log.d(TAG, "egl vendor: " + vendor); // 打印此版本EGL的实现厂�?

            String version = EGL14.eglQueryString(display, EGL14.EGL_VERSION);
            if (VERBOSE) Log.d(TAG, "egl version: " + version);// 打印EGL版本�?

            String extension = EGL14.eglQueryString(display, EGL14.EGL_EXTENSIONS);
            if (VERBOSE) Log.d(TAG, "egl extension: " + extension); //打印支持的EGL扩展
        }
    }

    private void checkEglError(String msg)
    {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }

    private boolean newImage()
    {
        synchronized (mFrameSyncObject) {
            while (!mFrameAvailable) {
                try {
                    if (VERBOSE) Log.d(TAG, "newImage : begin to wait for new image");
                    mFrameSyncObject.wait();
                    if (VERBOSE) Log.d(TAG, "newImage : end to wait for new image");
                    if (!mFrameAvailable) {
                        // TODO: if "spurious wakeup", continue while loop
                        throw new RuntimeException("newImage : frame wait timed out");
                    }
                } catch (InterruptedException ie) {
                    if (VERBOSE) Log.w(TAG, "newImage : InterruptedException happened");
                    if (!mIsSaveLastFrame){
                        mTextureRender.drawFrame(true);
                        EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface);
                    }
                    return false;
                }
            }
            mFrameAvailable = false;
        }

        mSurfaceTexture.updateTexImage();

        if ( mIsSurfaceChange) {
            if(mIsAspectFill) {
                mTextureRender.setRectAspectFill(mSurfaceWidth, mSurfaceHeight, mVideoWidth, mVideoHeight);
            }else{
                GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
            }

            mIsSurfaceChange = false;
        }

        mTextureRender.drawFrame(false);
        EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface);

        return true;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture st)
    {
        synchronized (mFrameSyncObject) {
            if (mFrameAvailable) {
                throw new RuntimeException("onFrameAvailable : mFrameAvailable already set, frame could be dropped");
            }

            mFrameAvailable = true;
            mFrameSyncObject.notifyAll();
        }
    }

    private class SurfaceHolderCallBack implements SurfaceHolder.Callback
    {
        @Override
        public synchronized void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (VERBOSE) Log.d(TAG, "surfaceChanged : surface change " + format + ": " + width + "x" + height);

            mSurfaceWidth = width;
            mSurfaceHeight = height;
            mIsSurfaceChange = true;
        }

        @Override
        public synchronized void surfaceCreated(SurfaceHolder holder) {
            if (VERBOSE) Log.d(TAG, "surfaceCreated : surface create");
        }

        @Override
        public synchronized void surfaceDestroyed(SurfaceHolder holder) {
            if (VERBOSE) Log.d(TAG, "surfaceDestroyed : surface destroy");
        }
    }

    private class TextureViewListener implements TextureView.SurfaceTextureListener
    {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "onSurfaceTextureAvailable : surface available " + ": " + width + "x" + height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            if (VERBOSE) Log.d(TAG, "onSurfaceTextureSizeChanged : surface texture change "  + ": " + width + "x" + height);

            mSurfaceWidth = width;
            mSurfaceHeight = height;
            mIsSurfaceChange = true;
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.d(TAG, "onSurfaceTextureDestroyed : surface destroy");
            return  false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            //Log.d(TAG, "onSurfaceTextureDestroyed : update");
        }
    }

    private class TextureRender
    {
        private final String TAG = "TextureRender";
        private final int SIZEOF_FLOAT = 4;
        private final int SIZEOF_SHORT = 2;
        private final int COORDS_PER_VERTEX = 2;
        private final int vertexStride = COORDS_PER_VERTEX * SIZEOF_FLOAT; // 4 bytes per vertex

        private float squareCoords[] = {
                -1.0f, 1.0f,
                -1.0f, -1.0f,
                1.0f, -1.0f,
                1.0f, 1.0f,
        };

        // 纹理坐标与图像坐标在y轴上是反的
        private float textureVertices[] = {
                0.0f, 0.0f, // (x, y)
                0.0f, 1.0f, // (x, h)
                1.0f, 1.0f, // (w, h)
                1.0f, 0.0f, // (w, y)
        };

        private short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices

        private final String VERTEX_SHADER =
                "attribute vec4 vPosition;" +
                        "attribute vec2 inputTextureCoord;" +
                        "varying vec2 textureCoord;" +
                        "void main()" +
                        "{"+
                        "gl_Position = vPosition;"+
                        "textureCoord = inputTextureCoord;" +
                        "}";

        private final String FRAGMENT_SHADER =
                "#extension GL_OES_EGL_image_external : require\n"+
                        "precision mediump float;" +
                        "varying vec2 textureCoord;\n" +
                        "uniform samplerExternalOES stexture;\n" +
                        "void main() {" +
                        "  gl_FragColor = texture2D( stexture, textureCoord );\n" +
                        "}";

        private float[] mSTMatrix = new float[16];
        private int[] mTextures = new int[1];

        private FloatBuffer mTextureBuffer = null;
        private FloatBuffer mVertexBuffer = null;
        private ShortBuffer mDrawListBuffer = null;

        private int mProgram = 0;

        private int mvPositionHandle;
        private int mInputTextureCoordHandle;

        private TextureRender()
        {
            Matrix.setIdentityM(mSTMatrix, 0);
        }

        private int getTextureId()
        {
            return mTextures[0];
        }

        private boolean create()
        {
            if (!setupBuffer()){
                Log.e(TAG, "create : set up inner buffer failed");
                return false;
            }

            return setupContext();
        }

        private void destroy()
        {
            GLES20.glDeleteTextures(1, mTextures, 0);

            if (mProgram != 0) {
                GLES20.glDeleteProgram(mProgram);
            }
        }

        private synchronized  void updateTextureRect(RenderRect src, RenderRect dst)
        {
            /*0.0f, 0.0f, // (x, y)
            0.0f, 1.0f, // (x, h)
            1.0f, 1.0f, // (w, h)
            1.0f, 0.0f, // (w, y)*/

            if (((dst.x + dst.w) <= src.w) && (dst.y + dst.h < src.h)){
                float _x = (float) (dst.x - src.x) / src.w;
                float _y = (float) (dst.y - src.y) / src.h;
                float _w = (float) dst.w / src.w;
                float _h = (float) dst.h / src.h;

                float textureVertices[] = {
                    _x, _y,
                    _x, _h,
                    _w, _h,
                    _w, _y,
                };

                updateFloatBuffer(mTextureBuffer, textureVertices);
            }
        }

        private synchronized void drawFrame(boolean lastFrame)
        {
            // (optional) clear to black so we can see if we're failing to set pixels
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            if(!lastFrame)
            {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextures[0]);

                // Enable a handle to the triangle vertices
                GLES20.glEnableVertexAttribArray(mvPositionHandle);
                GLES20.glVertexAttribPointer(mvPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, mVertexBuffer);

                GLES20.glEnableVertexAttribArray(mInputTextureCoordHandle);
                GLES20.glVertexAttribPointer(mInputTextureCoordHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, mTextureBuffer);

                GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, mDrawListBuffer);

                // Disable vertex array
                GLES20.glDisableVertexAttribArray(mvPositionHandle);
                GLES20.glDisableVertexAttribArray(mInputTextureCoordHandle);
            }
        }

        private void setRectAspectFill(int surfaceWidth, int surfaceHeight, int videoWidth, int videoHeight)
        {
            int rect_x, rect_y, rect_w, rect_h;
            int surfaceAspectRatioX = 1, surfaceAspectRatioY = 1;
            int videoAspectRatioX = 1, videoAspectRatioY = 1;
            float upTimes, upTimesX, upTimesY;

            int r = gcd(surfaceWidth, surfaceHeight);
            if (r > 0) {
                surfaceAspectRatioX = surfaceWidth / r;
                surfaceAspectRatioY = surfaceHeight / r;
            }

            r = gcd(videoWidth, videoHeight);
            if (r > 0) {
                videoAspectRatioX = videoWidth / r;
                videoAspectRatioY = videoHeight / r;
            }
            upTimesX = (float) (surfaceWidth * 1.0 / videoAspectRatioX);
            upTimesY = (float) (surfaceHeight * 1.0 / videoAspectRatioY);
            upTimes = upTimesX > upTimesY ? upTimesY : upTimesX;

            if ((videoAspectRatioX * surfaceAspectRatioY) >= (surfaceAspectRatioX  * videoAspectRatioY)) {
                //landscape
                rect_x = 0;
                rect_y = (int)((surfaceHeight - upTimes*videoAspectRatioY) / 2);
                rect_w = (int)(upTimes * videoAspectRatioX);
                rect_h = (int)(upTimes * videoAspectRatioY);
            } else {
                //portrait
                rect_y = 0;
                rect_x = (int)((surfaceWidth - upTimes*videoAspectRatioX) / 2);
                rect_w = (int)(upTimes * videoAspectRatioX);
                rect_h = (int)(upTimes * videoAspectRatioY);
            }

            GLES20.glViewport(rect_x, rect_y, rect_w, rect_h);
        }

        private int gcd(int a, int b)
        {
            while(a != b) {
                if (a > b)
                    a = a - b;
                else
                    b = b - a;
            }
            return a;
        }

        private boolean setupBuffer()
        {
            // texture buffer
            mTextureBuffer = createFloatBuffer(textureVertices);
            // Initialize the texture holder
            mVertexBuffer = createFloatBuffer(squareCoords);
            // Draw list buffer
            mDrawListBuffer = createShortBuffer(drawOrder);

            return (mTextureBuffer != null && mVertexBuffer != null && mDrawListBuffer != null);
        }

        private boolean setupContext()
        {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            checkGlError("setupContext ; unable to glActiveTexture");
            GLES20.glGenTextures(1, mTextures, 0);
            checkGlError("setupContext : unable to glGenTextures");
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextures[0]);
            checkGlError("setupContext : unable to glBindTexture");

            // config gl texture external oes
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            checkGlError("setupContext ; unable to glTexParameteri");

            // create program
            mProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
            if (mProgram == 0) {
                throw new RuntimeException("setupContext : failed creating program");
            }

            // use program
            GLES20.glUseProgram(mProgram);

            // get handle to vertex shader's vPosition member
            mvPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
            // get handle to fragment shader's inputTextureCoordinate member
            mInputTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoord");

            return true;
        }

        private int createProgram(String vertexSource, String fragmentSource)
        {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
            if (vertexShader == 0 || fragmentShader == 0) {
                Log.e(TAG, "createProgram : could not load vertexShader/fragmentShader");
                return 0;
            }

            int program = GLES20.glCreateProgram();
            checkGlError("createProgram : unable to glCreateProgram");
            if (program == 0) {
                Log.e(TAG, "createProgram : could not create program");
                return 0;
            }
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("createProgram : unable to glAttachShader VERTEX");
            GLES20.glAttachShader(program, fragmentShader);
            checkGlError("createProgram : unable to glAttachShader FRAGMENT");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "createProgram ; could not link program: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }

            GLES20.glDetachShader(program, vertexShader);
            GLES20.glDetachShader(program, fragmentShader);
            GLES20.glDeleteShader(vertexShader);
            GLES20.glDeleteShader(fragmentShader);

            return program;
        }

        private int loadShader(int shaderType, String source)
        {
            int shader = GLES20.glCreateShader(shaderType);
            checkGlError("loadShader : unable to glCreateShader type " + shaderType);
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "loadShader : could not compile shader " + shaderType + ":");
                Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
            return shader;
        }

        private ShortBuffer createShortBuffer(short[] array)
        {
            // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
            ByteBuffer bb = ByteBuffer.allocateDirect(array.length * SIZEOF_SHORT);
            bb.order(ByteOrder.nativeOrder());
            ShortBuffer sb = bb.asShortBuffer();
            sb.put(array);
            sb.position(0);
            return sb;
        }

        private FloatBuffer createFloatBuffer(float[] array)
        {
            // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
            ByteBuffer bb = ByteBuffer.allocateDirect(array.length * SIZEOF_FLOAT);
            bb.order(ByteOrder.nativeOrder());
            FloatBuffer fb = bb.asFloatBuffer();
            fb.put(array);
            fb.position(0);
            return fb;
        }

        private void updateFloatBuffer(FloatBuffer fb, float[] array)
        {
            if (fb != null){
                fb.clear();
                fb.put(array);
                fb.position(0);
            }
        }

        private void checkGlError(String op)
        {
            int error = GLES20.glGetError();
            if (error != GLES20.GL_NO_ERROR) {
                String msg = op + ": glError 0x" + Integer.toHexString(error);
                Log.e(TAG, msg);
                throw new RuntimeException(msg);
            }
        }
    }
}
