package com.codyy.robinsdk;

import android.media.projection.MediaProjection;
import android.view.SurfaceView;
import android.view.TextureView;

/**
 * modify by liuhao on 2017/9/19.
 */
public interface RBPublishAndroid
{
    /**
     * initialize a robin publisher
     * @return error code, 0 is success
     */
    public long init();

    /**
     * release a robin publisher
     * @return error code, 0 is success
     */
    public long release();

    /**
     * start a robin publisher
     * @return error code, 0 is success
     */
    public long start();

    /**
     * stop a robin publisher
     * @return error code, 0 is success
     */
    public long stop();

    /**
     * set uri for a publisher
     * @param uri : publisher uri, can " rtmp:// ".
     * @return error code, 0 is success, other failed.
     */
    public long setUri(String uri);

    /**
     * set whether need send audio & video, only rtmp:// stream effective, and set a publisher preview surface
     * @param mode : send media mode, RB_MEDIA_ALL/RB_MEDIA_VIDEO/RB_MEDIA_AUDIO
     * @param view : when send video we need give a preview surface view, if not, give null
     * @return error code, 0 is success, other failed
     */
    public long setMediaMode(int mode, SurfaceView view);

    /**
     * set whether need send audio & video, only rtmp:// stream effective, and set a publisher preview surface
     * @param mode : send media mode, RB_MEDIA_ALL/RB_MEDIA_VIDEO/RB_MEDIA_AUDIO
     * @param view : when send video we need give a preview texture view, if not, give null
     * @return error code, 0 is success, other failed
     */
    public long setMediaModeByTextureView(int mode, TextureView view);

    /**
     * set auto reconnect times we attempt to connect server
     * @param times : how many times we attempt to reconnect rtmp server, from 0 to 100,
     *               0 is disable auto reconnect function, default is 20
     * @return error code, 0 is success, other failed
     */
    public long setReconnectTimes(int times);

    /**
     * set auto reconnect duration we attempt to connect server
     * @param duration : you should set more than 4s, default is 5s
     * @return error code, 0 is success, other failed
     */
    public long setReconnectDuration(int duration);

    /**
     * set video camera device to capture
     * @param  videoId : index of camera device
     * @param  screenOrientation : screen orientation, RB_PORTRAIT/RB_PORTRAIT_UPSIDE_DOWN/
     *                           RB_LANDSCAPE_RIGHT/RB_LANDSCAPE_LEFT, default is RB_PORTRAIT
     * @return error code, 0 is success
     */
    public long setVideoDevice(int videoId, int screenOrientation);

    /**
     * set the media projection for screen capture, support after api 21
     * @param mp : MediaProjection object
     * @param dpi : the density of the virtual display in dpi, must be greater than 0
     * @return error code, 0 is success
     */
    public long setMediaProjection(MediaProjection mp, int dpi);
	
    /**
     * set the video format for AppVideo capture, you should choose right video device
     * @param format : video format, such as annexb h264/h265, nv12/nv21/i420
     * @return error code, 0 is success
     */
    public long setAppVideoFormat(String format);

    /**
     * receive application video data for AppVideo capture 
     * @param data : video data
     * @param len : length of video data
     * @return error code, 0 is success
     */
    public long receiveAppVideoData(byte[] data, int len);

    /**
     * set the video capture resolution from GetSupportVideoSizes.
     * @param width : video width
     * @param height : video height
     * @return error code, 0 is success
     */
    public long setVideoResolution(int width, int height);

    /**
     * set the video frame rate that capture
     * @param frameRate : video frame rate, from 10 to 30, default is 25
     * @return error code, 0 is success
     */
    public long setVideoFrameRate(int frameRate);

    /**
     * whether use video hardware encoder
     * @param isEnable : enable video hardware encoder or not, true is enable, false is disable,
     *                 default is disable
     * @return error code, 0 is success, other failed
     */
    public long setVideoHardwareEnc(boolean isEnable);

    /**
     * set the video encode bitrate(kb), the SetVideoSize will use default value
     * to override it in stop status
     * @param bitRate : video encode bitrate, from 32kb to 8192kb, default depend on video size
     * @return error code, 0 is success
     */
    public long setVideoBitRate(int bitRate);

    /**
     * set the audio device that capture
     * @param audioId : id of audio device
     * @return error code, 0 is success
     */
    public long setAudioDevice(int audioId);

    /**
     * receive application pcm data for AppAudio capture
     * @param data : audio pcm data
     * @param len : length of audio pcm data
     * @param sampleRate : sample rate of audio data, such as 16000, 44100 and so on
     * @param channel : channel of audio data, 1 is mono, 2 is stereo
     * @param format : format of audio data, bit per audio sample, one byte is 8, two byte is 16
     * @return error code, 0 is success
     */
    public long receiveAppPcmData(byte[] data, int len, int sampleRate, int channel, int format);

    /**
     * set the audio device capture sample rate from GetSupportAudioSampleRates
     * @param sampleRate : sampleRate of audio capture, default is 16000
     * @return error code, 0 is success
     */
    public long setAudioSampleRate(int sampleRate);

    /**
     * enable publisher AEC function
     * @param isEnable : enable audio aec function or not, true is enable, false is disable,
     *                 default is disable
     * @return error code, 0 is success, other failed
     */
    public long setAudioAEC(boolean isEnable);

    /**
     * set the audio encode bitrate(kb)
     * @param bitRate : audio encode bitrate, from 8kb to 128kb, default is 16kb
     * @return error code, 0 is success
     */
    public long setAudioBitRate(int bitRate);

    /**
     * get video device number
     * @return num of video devices
     */
    public long getVideoDeviceNum();

    /**
     * get description of video device by video device index
     * @param videoId : id of device
     * @return describe of device
     */
    public String getVideoDeviceDescribe(int videoId);

    /**
     * get support video device capture resolution by video device index
     * @param videoId : video device id
     * @param resolutionArray : all support video resolution
     * @param numArray : num of resolution array, use numArray[0]
     * @return error code, 0 is success
     */
    public long getSupportVideoResolution(int videoId, RBSize[] resolutionArray, int[] numArray);

    /**
     * get audio device number
     * @return audio device number
     */
    public long getAudioDeviceNum();

    /**
     * get description of audio device by audio device index
     * @param audioId audio device id
     * @return audio device description
     */
    public String getAudioDeviceDescribe(int audioId);

    /**
     * get support audio device sample rate by audio device index
     * @param audioId : audio device id
     * @param sampleRateArray : all support audio sample rate
     * @param numArray : num of sample rate array, use numArray[0]
     * @return error code, 0 is success
     */
    public long getSupportAudioSampleRate(int audioId, int[] sampleRateArray, int[] numArray);

    public static interface PublisherListener
    {
        public abstract void OnStateChange(int newState);
        public abstract void OnErrorGet(int errorCode, String desc);
        public abstract void OnNoticeGet(int noticeCode, String desc);
        public abstract void OnMediaModeChange(int newMode);
        public abstract void OnVideoIdChange(int newVideoId);
    }

    /**
     * set publisher callback listener
     * @param listener : the listener
     */
    public void setPublisherListener(PublisherListener listener);
}
