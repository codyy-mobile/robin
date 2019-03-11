package com.codyy.robinsdk.impl;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import java.nio.IntBuffer;
import java.util.concurrent.Semaphore;

/**
 * Created by liuhao on 2017/10/18.
 */

public class SurfaceTextureEncoder implements SurfaceTexture.OnFrameAvailableListener{
    private static final String TAG = "SurfaceTextureEncoder";
    private static final boolean VERBOSE = false;           // lots of logging

    private Thread mThread = null;
    private Semaphore mSurfaceSem = null;
    private boolean mFrameAvailable = false;
    private onFrameCallBack mCallBack = null;

    private SurfaceTexture mSurfaceTexture = null;
    private Surface mSetSurface = null;
    private Surface mGetSurface = null;
    private TextureRender mTextureRender = null;

    private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;
    private EGLContext mEGLContextEncoder = EGL14.EGL_NO_CONTEXT;
    private EGLSurface mEGLSurfaceEncoder = EGL14.EGL_NO_SURFACE;

    private int mVideoWidth = 0;
    private int mVideoHeight = 0;
    private int mVideoFps = 0;
    private int mVideoInterval = 0;

    private boolean mIsCut = false;
    private boolean mIsStarted = false;
    private boolean mIsStopping = false;
    private long mFrameCount = 0;
    private long mLastTime = 0;

    public SurfaceTextureEncoder(@NonNull Surface surface, int videoWidth, int videoHeight, int fps)
    {
        if (VERBOSE) Log.d(TAG, "SurfaceTextureEncoder : video size "  + videoWidth + "x" + videoHeight + ", fps " + fps);

        mSetSurface = surface;
        mVideoWidth = videoWidth;
        mVideoHeight = videoHeight;
        mVideoFps = fps;
        if (fps != 0) {
            mVideoInterval = 1000 / fps;
        }
    }

    public void setFrameCallBack(@NonNull onFrameCallBack callBack)
    {
        mCallBack = callBack;
    }

    public interface onFrameCallBack
    {
        void encodeUpdate(boolean last);
        void onCutScreen(Bitmap bitmap);
    }

    public synchronized boolean start()
    {
        if (VERBOSE) Log.d(TAG, "start : begin to start");

        if (mSetSurface == null || mVideoWidth <= 0 || mVideoHeight <= 0 || mVideoFps <= 0)
            return false;

        if (!mIsStarted) {
            mSurfaceSem = new Semaphore(0);
            mThread = new Thread(new SurfaceTextureEncoder.SurfaceTextureEncoderThread());
            mThread.start();

            mIsStarted = true;
        }

        if (VERBOSE) Log.d(TAG, "start : end to start");
        return true;
    }

    public synchronized void stop()
    {
        if (VERBOSE) Log.d(TAG, "stop : begin to stop");

        if (mIsStarted){
            mIsStopping = true;
            if (mThread != null) {
                //mThread.interrupt();
                try {
                    mThread.join();
                } catch (InterruptedException e) {
                    Log.e(TAG, "stop : stop thread failed with " + e.toString());
                }
                mThread = null;
            }
            mIsStarted = false;
            mIsStopping = false;

            mFrameCount = 0;
            mLastTime = 0;
        }

        if (VERBOSE) Log.d(TAG, "stop : end to stop");
    }

    public synchronized void cutScreen()
    {
        if (mIsStarted) {
            Log.w(TAG, "cutScreen : you need start it first");
            return;
        }

        mIsCut = true;
    }

    public synchronized Surface getSurface()
    {
        try{
            if (VERBOSE)Log.d(TAG, "getSurface : begin to acquire sem");
            mSurfaceSem.acquire();
            if (VERBOSE)Log.d(TAG, "getSurface : end to acquire sem");
        }
        catch (InterruptedException ex){
            ex.printStackTrace();
        }

        return mGetSurface;
    }

    private class SurfaceTextureEncoderThread implements Runnable
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

            while (isInited && !mIsStopping) {
                if (!drawImage())
                    break;
            }

            // last update
            if (mCallBack != null){
                mCallBack.encodeUpdate(true);
            }

            releaseGLComponents();
            releaseEGL();

            if (VERBOSE) Log.d(TAG, "SurfaceTextureEncoderThread : surface texture encoder thread exit");
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
            mEGLDisplay = null;
            throw new RuntimeException("initEgl : unable to initialize EGL14");
        }

        if (!bindAPI(mEGLDisplay, version[0], version[1])){
            throw new RuntimeException("initEgl : unable to bind EGL14 API");
        }

        // configure context for OpenGL ES 2.0.
        int[] ctxAttributes = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };

        EGLConfig config = chooseConfig(mEGLDisplay, EGL14.EGL_PBUFFER_BIT, 2);
        if (config == null){
            throw new RuntimeException("initEgl ; unable to choose egl config");
        }

        mEGLContext = EGL14.eglCreateContext(mEGLDisplay, config, EGL14.EGL_NO_CONTEXT, ctxAttributes, 0);
        checkEglError("initEgl : unable to eglCreateContext");
        if (mEGLContext == null) {
            throw new RuntimeException("initEgl : unable to create egl context for mEGLContext");
        }

        EGLConfig configEncoder = chooseConfig(mEGLDisplay, EGL14.EGL_NONE, 2);
        if (configEncoder == null){
            throw new RuntimeException("initEgl : unable to choose egl config for encoder");
        }

        mEGLContextEncoder = EGL14.eglCreateContext(mEGLDisplay, configEncoder, mEGLContext, ctxAttributes, 0);
        checkEglError("initEgl : unable to eglCreateContext");
        if (mEGLContextEncoder == null) {
            throw new RuntimeException("initEgl : unable to create egl context for encoder");
        }

        // create a pbuffer surface.
        int[] surfaceAttributes = {
                EGL14.EGL_WIDTH, mVideoWidth,
                EGL14.EGL_HEIGHT, mVideoHeight,
                EGL14.EGL_NONE
        };
        mEGLSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, config, surfaceAttributes, 0);
        checkEglError("initEgl : unable to eglCreatePbufferSurface");
        if (mEGLSurface == null) {
            throw new RuntimeException("initEgl : unable to create egl pbuffer surface");
        }

        // create an EGL window surface and returns its handle
        int[] surfaceAttributes2 = {
                EGL14.EGL_NONE
        };
        mEGLSurfaceEncoder = EGL14.eglCreateWindowSurface(mEGLDisplay, configEncoder, surface, surfaceAttributes2, 0);
        checkEglError("initEgl : unable to eglCreateWindowSurface");
        if (mEGLSurfaceEncoder == null) {
            throw new RuntimeException("initEgl : unable to create egl window surface");
        }

        makeCurrent(0);

        return true;
    }

    private void releaseEGL()
    {
        if (VERBOSE) Log.d(TAG, "releaseEGL : release egl");

        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY)
        {
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);

            if (mEGLSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
                mEGLSurface = EGL14.EGL_NO_SURFACE;
            }

            if (mEGLSurfaceEncoder!= EGL14.EGL_NO_SURFACE){
                EGL14.eglDestroySurface(mEGLDisplay, mEGLSurfaceEncoder);
                mEGLSurfaceEncoder = EGL14.EGL_NO_SURFACE;
            }

            if (mEGLContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
                mEGLContext = EGL14.EGL_NO_CONTEXT;
            }

            if (mEGLContextEncoder != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(mEGLDisplay, mEGLContextEncoder);
                mEGLContextEncoder = EGL14.EGL_NO_CONTEXT;
            }

            EGL14.eglReleaseThread();
            EGL14.eglTerminate(mEGLDisplay);
            mEGLSurface = EGL14.EGL_NO_SURFACE;
        }
    }

    private boolean initGLComponents()
    {
        if (VERBOSE) Log.d(TAG, "initGLComponents : init gl components");

        mTextureRender = new SurfaceTextureEncoder.TextureRender();
        if (!mTextureRender.create()) {
            Log.e(TAG, "initGLComponents : create texture render failed");
            return false;
        }

        mSurfaceTexture = new SurfaceTexture(mTextureRender.getTextureId());
        mSurfaceTexture.setDefaultBufferSize(mVideoWidth, mVideoHeight);
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

    private EGLConfig chooseConfig(EGLDisplay display, int surfaceType, int version)
    {
        int renderableType = EGL14.EGL_OPENGL_ES2_BIT;
        if (version >= 3) {
            renderableType |= EGLExt.EGL_OPENGL_ES3_BIT_KHR;
        }

        // Configure EGL for windows and OpenGL ES 2.0, 24-bit RGB..
        // The actual surface is generally RGBA or RGBX, so situationally omitting alpha
        // doesn't really help.  It can also lead to a huge performance hit on glReadPixels()
        // when reading into a GL_RGBA buffer.
        int[] attributes  = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, renderableType,
                EGL14.EGL_NONE, 0,
                EGL14.EGL_NONE
        };

        if (surfaceType == EGL14.EGL_PBUFFER_BIT){
            attributes[10] = EGL14.EGL_SURFACE_TYPE;
            attributes[11] = EGL14.EGL_PBUFFER_BIT;// EGL_WINDOW_BIT / EGL_PBUFFER_BIT
        }

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
        if (display != null)
        {
            String vendor = EGL14.eglQueryString(display, EGL14.EGL_VENDOR);
            if (VERBOSE) Log.d(TAG, "egl vendor: " + vendor); // 打印此版本EGL的实现厂商

            String version = EGL14.eglQueryString(display, EGL14.EGL_VERSION);
            if (VERBOSE) Log.d(TAG, "egl version: " + version);// 打印EGL版本号

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

    private boolean drawImage()
    {
        makeCurrent(1);

        long currentTime = System.currentTimeMillis();
        if (currentTime - mLastTime >= mVideoInterval) {
            // frame control
            if (mFrameAvailable) {
                // we must updateTexImage fast enough, or the onFrameAvailable will not be called any more
                mSurfaceTexture.updateTexImage();
            }

            try{
                mTextureRender.drawFrame();
                if (mCallBack != null){
                    mCallBack.encodeUpdate(false);
                }
                EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurfaceEncoder, computePresentationTimeNsec(mFrameCount++));
                EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurfaceEncoder);
                checkEglError("drawImage : unable to eglSwapBuffers");

                if (mIsCut){
                    getScreen(mVideoWidth, mVideoHeight, mCallBack);
                    mIsCut = false;
                }

            }catch (RuntimeException re){
                Log.w(TAG, "drawImage : RuntimeException happened");
                re.printStackTrace();
                return false;
            }

            mLastTime = currentTime;
        }else{
            try{
                Thread.sleep(5);
            }catch (InterruptedException ie){
                Log.w(TAG, "drawImage ; InterruptedException happened");
                ie.printStackTrace();
                return false;
            }
        }

        return true;
    }

    private void getScreen(int width, int height, @NonNull onFrameCallBack callback)
    {
        IntBuffer buffer = IntBuffer.allocate(width * height);
        buffer.position(0);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        int[] modelData = buffer.array();
        int[] ArData = new int[modelData.length];
        int offset1, offset2;
        for (int i = 0; i < height; i++) {
            offset1 = i * height;
            offset2 = (height - i - 1) * width;
            for (int j = 0; j < width; j++) {
                int texturePixel = modelData[offset1 + j];
                int blue = (texturePixel >> 16) & 0xff;
                int red = (texturePixel << 16) & 0x00ff0000;
                int pixel = (texturePixel & 0xff00ff00) | red | blue;
                ArData[offset2 + j] = pixel;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(ArData, width, height, Bitmap.Config.ARGB_8888);
        buffer.clear();
        callback.onCutScreen(bitmap);
    }

    private void makeCurrent(int index)
    {
        if (index == 0) {
            if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
                throw new RuntimeException("makeCurrent : unable to eglMakeCurrent for index 0");
            }
        } else {
            if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurfaceEncoder, mEGLSurfaceEncoder, mEGLContextEncoder)) {
                throw new RuntimeException("makeCurrent : unable to eglMakeCurrent");
            }
        }
    }

    private long computePresentationTimeNsec(long frameIndex)
    {
        final long ONE_BILLION = 1000000000;
        return frameIndex * ONE_BILLION / mVideoFps;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture)
    {
        if (VERBOSE)Log.d(TAG, "onFrameAvailable : on frame available");
        mFrameAvailable = true;
    }

    private class TextureRender {
        private static final String TAG = "TextureRender";
        private static final int SIZEOF_FLOAT = 4;

        private final float squareCoords[] = {
                -1.0f, -1.0f, 1.0f,  // 0 bottom left
                1.0f, -1.0f, 1.0f,   // 1 bottom right
                -1.0f, 1.0f, 1.0f,   // 2 top left
                1.0f,  1.0f, 1.0f    // 3 top right
        };

        private final float textureVertices[] = {
                0.0f, 1.0f, 1f, 1.0f,    // 0 bottom left
                1.0f, 1.0f, 1f, 1.0f,    // 1 bottom right
                0.0f, 0.0f, 1f, 1.0f,    // 2 top left
                1.0f, 0.0f ,1f, 1.0f     // 3 top right
        };

        private final String VERTEX_SHADER =
                "uniform mat4 uMVPMatrix;\n" +
                        "uniform mat4 uSTMatrix;\n" +
                        "attribute vec4 aPosition;\n" +
                        "attribute vec4 aTextureCoord;\n" +
                        "varying vec4 vTextureCoord;\n" +
                        "void main() {\n" +
                        "    gl_Position = uMVPMatrix * aPosition;\n" +
                        "    vTextureCoord = uSTMatrix * aTextureCoord;\n" +
                        "}\n";

        private final String FRAGMENT_SHADER =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +      // highp here doesn't seem to matter
                        "varying vec4 vTextureCoord;\n" +
                        "uniform samplerExternalOES sTexture;\n" +
                        "void main() {\n" +
                        "    gl_FragColor = texture2D(sTexture, vTextureCoord.xy/vTextureCoord.z);" +
                        "}\n";

        private float[] mMVPMatrix = new float[16];
        private float[] mSTMatrix = new float[16];
        private int[] mTextures = new int[1];

        private FloatBuffer mVertexBuffer = null;
        private FloatBuffer mTextureBuffer = null;

        private int mProgram = 0;

        private int muMVPMatrixHandle;
        private int muSTMatrixHandle;
        private int maPositionHandle;
        private int maTextureHandle;

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

        private void drawFrame()
        {
            GLES20.glUseProgram(mProgram);
            // Enable the "aPosition" vertex attribute.
            GLES20.glEnableVertexAttribArray(maPositionHandle);
            // Connect vertexBuffer to "aPosition".
            GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 3 * SIZEOF_FLOAT, mVertexBuffer);
            checkGlError("drawFrame : unable to glVertexAttribPointer for mVertexBuffer");

            // Enable the "aTextureCoord" vertex attribute.
            GLES20.glEnableVertexAttribArray(maTextureHandle);
            // Connect texBuffer to "aTextureCoord".
            GLES20.glVertexAttribPointer(maTextureHandle, 4, GLES20.GL_FLOAT, false, 4 * SIZEOF_FLOAT, mTextureBuffer);
            checkGlError("drawFrame : unable to glVertexAttribPointer for mTextureBuffer");

            Matrix.setIdentityM(mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);

            // Draw the rect.
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            // Done -- disable vertex array, texture,
            GLES20.glDisableVertexAttribArray(maPositionHandle);
            GLES20.glDisableVertexAttribArray(maTextureHandle);
            GLES20.glUseProgram(0);
        }

        private boolean setupBuffer()
        {
            // texture buffer
            mTextureBuffer = createFloatBuffer(textureVertices);
            // Initialize the texture holder
            mVertexBuffer = createFloatBuffer(squareCoords);

            return (mTextureBuffer != null && mVertexBuffer != null);
        }

        private boolean setupContext()
        {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            checkGlError("setupContext : unable to glActiveTexture");
            GLES20.glGenTextures(1, mTextures, 0);
            checkGlError("setupContext : unable to glGenTextures");
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextures[0]);
            checkGlError("setupContext : unable to glBindTexture");

            // config gl texture external oes
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            checkGlError("setupContext : unable to glTexParameteri");

            // create program
            mProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
            if (mProgram == 0) {
                throw new RuntimeException("setupContext : failed creating program");
            }

            // use program
            GLES20.glUseProgram(mProgram);

            maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
            maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
            muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");

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
                Log.e(TAG, "createProgram : could not link program: ");
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
