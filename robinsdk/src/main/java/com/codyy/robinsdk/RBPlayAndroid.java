package com.codyy.robinsdk;

import android.view.SurfaceView;
import android.view.TextureView;

/**
 * modify by liuhao on 2017/9/19
 */
public interface RBPlayAndroid
{
    /**
     * initialize a robin player
     * @return error code, 0 is success
     */
    public long init();

    /**
     * release a robin player
     * @return error code, 0 is success
     */
    public long release();

    /**
     * start a robin player
     * @return error code, 0 is success
     */
    public long start();

    /**
     * pause a robin player, no implementation
     * @return error code, 0 is success
     */
    public long pause();

    /**
     * stop a robin player
     * @return error code, 0 is success
     */
    public long stop();

    /**
     * set uri for a player
     * @param uri : player uri, can rtmp://, http://, or a file
     * @return error code, 0 is success, other failed
     */
    public long setUri(String uri);

    /**
     * set whether need receive audio & video, only rtmp:// stream effective
     * @param mode : receive media mode, RB_MEDIA_ALL/RB_MEDIA_VIDEO/RB_MEDIA_AUDIO
     * @param view : when receive video we need give a render surface view, if not, give null
     * @return error code, 0 is success, other failed
     */
    public long setMediaMode(int mode, SurfaceView view);

    /**
     * set whether need send audio & video, only rtmp:// stream effective, and set a publisher preview surface
     * @param mode : send media mode, RB_MEDIA_ALL/RB_MEDIA_VIDEO/RB_MEDIA_AUDIO
     * @param view : when send video we need give a render texture view, if not, give null
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
     * @param duration :  you should set more than 4s, default is 5s
     * @return error code, 0 is success, other failed
     */
    public long setReconnectDuration(int duration);

    /**
     * set player no data notice timeout
     * @param timeout : the value must more than -1, 0 is disable this function, default is 4s,
     * @return error code, 0 is success, other failed
     */
    public long setNoDataTimeout(int timeout);

    /**
     * whether use hardware video decoder
     * @param isEnable : enable video hardware decoder or not, true is enable, false is disable,
     *                 default is disable
     * @return error code, 0 is success, other failed
     */
    public long setVideoHardwareDec(boolean isEnable);

    /**
     * set video render mode, RB_RENDER_FULL_SCREEN or RB_RENDER_ASPECT_RATIO,
     * @param mode : video render mode, default is RB_RENDER_FULL_SCREEN
     * @return error code, 0 is success, other failed
     */
    public long setVideoRenderMode(int mode);

    /**
     * set player volume
     * @param volume : player audio volume, from 0 to 100, 0 is mute, default is 80
     * @return error code, 0 is success, other failed
     */
    public long setVolume(int volume);

    /**
     * when play a file or http stream , seek to position, no implementation now
     * @param pos : the position want to play
     * @return error code, 0 is success, other failed
     */
    public long seekToPos(long pos);

    /**
     * when play file or http stream, get current play position, no implementation now
     * @return current play position
     */
    public long getPosition();

    /**
     * when play file or http stream, get file total play duration, no implementation now
     * @return total play duration
     */
    public long getDuration();

    public static interface PlayerListener
    {
        public abstract void OnStateChange(int newState);
        public abstract void OnErrorGet(int errorCode, String desc);
        public abstract void OnNoticeGet(int noticeCode, String desc);
        public abstract void OnMediaModeChange(int newMode);
        public abstract void OnVideoResolution(int width, int height);
        public abstract void OnBufferProcessing(int percentage);
    }

    /**
     * set player callback listener
     * @param listener the listener
     */
    public void setPlayerListener(PlayerListener listener);
}
