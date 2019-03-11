package com.codyy.robinsdk.impl;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import com.codyy.robinsdk.RBPlayAndroid;
import com.codyy.robinsdk.RBPublishAndroid;
import com.codyy.robinsdk.RBSize;

public class RBManagerAndroid
{
    /***********************************************************************************************/
    // Native Method and Fields
    private long native_robin_context = 0;

    // About manager
    private native static void nativeClassInit();
    private native long nativeInit(boolean enableLog);      // return error code
    private native long nativeRelease();                    // return error code
    private native String nativeVersion();                  // return version of robin
    private native int nativeCreatePlayer();               // create player, and return player id
    private native void nativeDeletePlayer(int playId);     // delete player by id
    private native int nativeCreatePublisher();            // create publisher, and return publisher id
    private native void nativeDeletePublisher(int publishId); // delete publisher by id
    public native long nativeSetAttribute(int type, int id, Object value);  // set player and publisher attributes

    // About Player
    public native long nativeInitPlayer(int playId);       // init a player
    public native long nativeReleasePlayer(int playId);    // release Player
    public native long nativeStartPlayer(int playerId);      // start player
    public native long nativePausePlayer(int playerId);      // pause a player
    public native long nativeStopPlayer(int playerId);       // stop a player
    public native long nativeSetPlayerUri(int playerId, String playerURI);     // player uri
    public native long nativeSetPlayerRenderView(int playerId, Object view, int type); // set render view
    public native long nativeGetPosition(int playerId);    // get current play position
    public native long nativeGetDuration(int playerId);    // get current player duration

    // 需要传引用
    public native long  nativeGetVideoDeviceNum(int publisherId);
    public native String nativeGetVideoDeviceDescribe(int publisherId, int videoId); // videoDes需要传引用
    public native void nativeGetVideoDeviceResolution(int publisherId, int videoId, RBSize[] sptSizes, int[] returnArraySize);// sptSizes需要传引用
    public native long nativeGetAudioDeviceNum(int publisherId);
    public native String nativeGetAudioDeviceDescribe(int publisherId, int audioId); // audio des需要传引用
    public native void nativeGetAudioDeviceSampleRate(int publisherId, int audioId, int[] sptRates, int[] returnArraySize); //

    // About Publisher
    public native long nativeInitPublisher(int publishId);     // init a publisher
    public native long nativeReleasePublisher(int publishId);  // release publisher
    public native long nativeStartPublisher(int publisherId);      // start publisher
    public native long nativeStopPublisher(int publisherId);       // stop a publisher
    public native long nativeSetPublisherUri(int publisherId, String playerURI);     // publisher uri
    public native long nativeSetPulisherRenderView(int publisherId, Object view, int type); // set render view
    public native long nativeSetPulisherMediaProjection(int publisherId, Object mediaProjection, int dpi); // set media projection
    public native long nativeSetPublisherAppVideoFormat(int publisherId, String appVideoFormat); // set app video format
    public native long nativeReceiveAppVideoData(int publisherId, byte[] data, int len); // receive app video data
    public native long nativeReceiveAppPcmData(int publisherId, byte[] data, int len, int rate, int channel, int format); //receive app audio pcm data

    // plugin type
    public static final int RB_PLUGIN_PLAYER = 1;
    public static final int RB_PLUGIN_PUBLISHER = 2;
    public static final int RB_PLUGIN_AUDIO_MIX = 3;

    // receive or send type type
    public static final int RB_MEDIA_ALL = 0;
    public static final int RB_MEDIA_VIDEO = 1;
    public static final int RB_MEDIA_AUDIO = 2;

    // video render mode
    public static final int RB_RENDER_FULL_SCREEN = 0;
    public static final int RB_RENDER_ASPECT_RATIO = 1;

    // status
    public static final int RB_STATUS_PLAYING = 0;
    public static final int RB_STATUS_PAUSING = 1;
    public static final int RB_STATUS_STOP = 2;

    // orientation
    public static final int RB_PORTRAIT = 1;
    public static final int RB_PORTRAIT_UPSIDE_DOWN = 2;
    public static final int RB_LANDSCAPE_LEFT = 3;
    public static final int RB_LANDSCAPE_RIGHT = 4;

    // video render view type
    public static final int RB_VIDEO_RENDER_VIEW_TYPE_SURFACE_VIEW = 0;
    public static final int RB_VIDEO_RENDER_VIEW_TYPE_TEXTURE_VIEW = 1;

    // property ids
    public static final int RB_PLAYER_HARDWARE = 1;				// (boolean) whether use hardware decode, default software
    public static final int RB_PLAYER_VOLUME = 2;				    // (int) player volume
    public static final int RB_PLAYER_MEDIA_MODE = 3;			    // (int) receive video/audio,RB_MEDIA_ALL/RB_MEDIA_VIDEO/RB_MEDIA_AUDIO
    public static final int RB_PLAYER_RENDER_MODE = 4;			// (int) video render mode
    public static final int RB_PLAYER_SEEK_POS = 5;				// (long) player seek to position
    public static final int RB_PLAYER_RECONNECT_TIMES = 6;		// (int) net reconnect times
    public static final int RB_PLAYER_RECONNECT_DURATION = 7;	// (int) net reconnect duration every time
    public static final int RB_PLAYER_NO_DATA_TIMEOUT = 8;		// (int) player no data notice timeout

    public static final int RB_PUBLISHER_MEDIA_MODE = 11;			// (int) send video/audio,RB_MEDIA_ALL/RB_MEDIA_VIDEO/RB_MEDIA_AUDIO
    public static final int RB_PUBLISHER_VIDEO_ID = 12;             // (int) video capture id
    public static final int RB_PUBLISHER_AUDIO_ID = 13;             // (int) audio capture id
    public static final int RB_PUBLISHER_HARDWARE = 14;             // (boolean) encode type
    public static final int RB_PUBLISHER_VIDEO_FRAME_RATE = 15;     // (int) video frame rate
    public static final int RB_PUBLISHER_VIDEO_RESOLUTION = 16;     // (RBSize) video resolution
    public static final int RB_PUBLISHER_VIDEO_BITRATE = 17;		// (int) video encode bitrate
    public static final int RB_PUBLISHER_SCREEN_ORIENTATION = 18;	// (int) screen direction
    public static final int RB_PUBLISHER_AUDIO_SAMPLE_RATE = 19;	// (long) audio sample rates
    public static final int RB_PUBLISHER_AUDIO_AEC = 20;			// (boolean) whether enable audio aec, default disable
    public static final int RB_PUBLISHER_AUDIO_BITRATE = 21;		// (int) audio encode bitrate
    public static final int RB_PUBLISHER_RECONNECT_TIMES = 22;      // (int) net reconnect times
    public static final int RB_PUBLISHER_RECONNECT_DURATION = 23;	// (int) net reconnect duration every time

    // message notice
    public static final int RB_NOTICE_NONSUPPORT_HW_VIDEO = 0;      // nonsupport hardware video enc/dec, change to software
    public static final int RB_NOTICE_EOS = 1	;					    // end of the player stream
    public static final int RB_NOTICE_BUFFERING_PROGRESS = 2;		// player buffering progress
    public static final int RB_NOTICE_MEDIA_MODE_CHANGED = 3;		// media work mode has been changed
    public static final int RB_NOTICE_CHANGE_VIDEO_ID = 4;			// video capture index has been changed
    public static final int RB_NOTICE_VIDEO_RESOLUTION = 5;         // player video size
    public static final int RB_NOTICE_NO_DATA = 6;					// player no data notice
    public static final int RB_NOTICE_RECEIVE_NEW_STREAM = 7;		// receive a new stream in start state
    public static final int RB_NOTICE_CONNECT_NET_BEGIN = 8;		    // begin to connect network
    public static final int RB_NOTICE_CONNECT_NET_SUCCESS = 9;      // connect network successfully
    public static final int RB_NOTICE_SEND_DATA_TO_NET_SUCCESS = 10; // send data to network successfully
    public static final int RB_NOTICE_RECEIVE_DATA_FROM_NET_SUCCESS = 11; // receive data from network successfully
    public static final int RB_NOTICE_TRY_MAX_RECONNECT = 12;		// try many times to reconnect, but failed
    public static final int RB_NOTICE_FIRST_TIMESTAMP = 13;			// receive first capture timestamp
    public static final int RB_NOTICE_AUDIO_SPEED_UP = 14;			// play audio speed up

    // message error
    public static final int RB_ERR_INIT = 					0x01;	 // init failed
    public static final int RB_ERR_START =					0x02;	 // start failed
    public static final int RB_ERR_CONFIG =                 0x03;	 // config failed
    public static final int RB_ERR_STOP = 					0x04;	 // stop failed
    public static final int RB_ERR_PAUSE =					0x05;	 // pause failed
    public static final int RB_ERR_RELEASE =				0x06;	 // release failed
    public static final int RB_ERR_LOAD_FILTER =			0x07;	 // load filter failed
    public static final int RB_ERR_ASK_ALLOCATOR =			0x08;    // ask allocator failed
    public static final int RB_ERR_FILTER_CONNECT = 		0x09;	 // filter connect failed
    public static final int RB_ERR_FILTER_UNCONNECTED =		0x0a;	 // filter no connection
    public static final int RB_ERR_NET_INVALID_URI =		0x0b;	 // net uri is invalid
    public static final int RB_ERR_NET_CONNECT =			0x0c;	 // network connect failed
    public static final int RB_ERR_NET_SEND_DATA =			0x0d;	 // network send data failed
    public static final int RB_ERR_NET_RECEIVE_DATA =       0x0e;	 // network receive data failed
    public static final int RB_ERR_CREATE_OBJECT = 			0x0f;	 // create object failed

    public static final int RB_ERR_CREATE_THREAD =			0x20;	 // create thread failed
    public static final int RB_ERR_MALLOC_MEMORY =			0x21;	 // malloc memory failed
    public static final int RB_ERR_PARAM_INVALID =			0x22;	 // invalid parameter
    public static final int RB_ERR_NOT_INITIALIZED =		0x23;	 // the module is not initialized
    public static final int RB_ERR_MISS_OPPORTUNITY =       0x24;	 // miss the call time
    public static final int RB_ERR_PUSH_QUEUE =             0x25;	 // enqueue failed
    public static final int RB_ERR_POP_QUEUE = 				0x26;	 // dequeue failed
    public static final int RB_ERR_RESOLUTION	=			0x27;	 // error resolution
    public static final int RB_ERR_ILLEGAL_URI =			0x28;	 // illegal uri
    public static final int RB_ERR_TIMEOUT =				0x29;	 // timeout error
    public static final int RB_ERR_NOT_IMPL =				0x2a;	 // the function is not unrealized
    public static final int RB_ERR_GET_FAILED =             0x2b;	 // get parameter failed
    public static final int RB_ERR_FILE_PATH =				0x2c;	 // the file is not exit
    public static final int RB_ERR_FILE_TYPE =				0x2d;	 // the error file type
    public static final int RB_ERR_PARSE_FLV_FAILED =       0x2e;	 // parse flv format failed

    public static final int RB_ERR_CREATE_AUDIO_PROCESS =   0x41;	 // create audio mix process failed
    public static final int RB_ERR_UPPER_LIMIT =			0x42;	 // the num of player or publisher reach the upper limit
    public static final int RB_ERR_QUERY_INTERFACE =		0x43;	 // query interface failed
    public static final int RB_ERR_CHANGE_CAMERA =			0x44;	 // change camera failed
    public static final int RB_ERR_CHANGE_MEDIA_MODE =		0x45;	 // change media work mode failed

    /***********************************************************************************************/

    private final String TAG = "RobinManager";
    private Map<String, RBPlayerAndroid> mMapPlayer = new HashMap<String, RBPlayerAndroid>();
    private Map<String, RBPublisherAndroid> mMapPublisher = new HashMap<String, RBPublisherAndroid>();
    private boolean mIsInit = false;   // init or not
    private boolean mIsVerbose = false;


    private volatile static RBManagerAndroid mSingleton = null;
    private RBManagerAndroid(){}

    /**
     * get robin manager object
     * @return null for get manager failed
     */
    public static RBManagerAndroid getSingleton()
    {
        if (mSingleton == null) {
            synchronized (RBManagerAndroid.class) {
                if (mSingleton == null) {
                    mSingleton = new RBManagerAndroid();
                }
            }
        }
        return  mSingleton;
    }

    /**
     * initialize robin manager
     * @return error code, 0 is success
     */
    public synchronized long init()
    {
        long ret = nativeInit(mIsVerbose);
        if (ret == 0) {
            mIsInit = true;
        }else {
            if (mIsVerbose)Log.e(TAG, "init: native init failed");
        }

        return ret;
    }

    /**
     *  release robin manager
     * @return error code, 0 is success
     */
    public synchronized long release()
    {
        long ret = nativeRelease();
        if (ret == 0) {
            mIsInit = false;
        }else{
            if (mIsVerbose)Log.e(TAG, "init: native release failed");
        }

        mMapPublisher.clear();
        mMapPlayer.clear();

        return ret;
    }

    /**
     *  get robin version info
     * @return version info of robin
     */
    public synchronized String version()
    {
        if(!mIsInit){
            return "";
        }

        return nativeVersion();
    }

    /**
     *  enable robin log
     * @return error code, 0 is success
     */
    public synchronized long enableLog()
    {
        if (mIsInit){
            return -1;
        }

        mIsVerbose = true;
        return 0;
    }

    /**
     * create a robin player
     * @return null for create failed
     */
    public synchronized RBPlayAndroid createPlayer()
    {
        if (!mIsInit) {
            if (mIsVerbose)Log.e(TAG, "createPlayer: you need init manager first");
            return null;
        }

        // create c++ player
        int id = nativeCreatePlayer();
        if (id < 0){
            if (mIsVerbose)Log.e(TAG, "createPlayer: native create player failed");
            return null;
        }

        // create java player
        RBPlayerAndroid player = new RBPlayerAndroid();
        player.setManager(this);
        player.setId(id);
        player.setLog(mIsVerbose);

        // Add to player map
        mMapPlayer.put(Integer.toString(id), player);

        return player;
    }

    /**
     * delete a robin player
     * @param player : the player we need to delete
     */
   public synchronized void deletePlayer(RBPlayAndroid player)
   {
       if (player == null)
           return;

       player.release();
       RBPlayerAndroid playerImpl = (RBPlayerAndroid)player;
       int id = playerImpl.getId();

       mMapPlayer.remove(Integer.toString(id));
       nativeDeletePlayer(id);
   }

    /**
     * create a robin publisher,
     * @return nulls for create failed
     */
    public synchronized RBPublishAndroid createPublisher()
    {
        if (!mIsInit) {
            if (mIsVerbose)Log.e(TAG, "createPublisher: you need init manager first");
            return null;
        }
        // create c++ publisher
        int id = nativeCreatePublisher();
        if (id < 0){
            if (mIsVerbose)Log.e(TAG, "createPublisher: native create publisher failed");
            return  null;
        }
        // create java publisher
        RBPublisherAndroid publisher = new RBPublisherAndroid();
        publisher.setManager(this);
        publisher.setId(id);
        publisher.setLog(mIsVerbose);

        // Add to publisher map
        mMapPublisher.put(Integer.toString(id), publisher);

        return publisher;
    }

    /**
     *  delete a robin publisher
     * @param publisher : the publisher we need to delete
     */
    public synchronized void deletePublisher(RBPublishAndroid publisher)
    {
        if (publisher == null)
            return ;

        publisher.release();
        RBPublisherAndroid publisherImpl = (RBPublisherAndroid)publisher;
        int id = publisherImpl.getId();

        mMapPublisher.remove(Integer.toString(id));
        nativeDeletePublisher(id);
    }

    private void CFJ_StateChange(int pluginType, int pluginId, int curState)
    {
        String id = Integer.toString(pluginId);
         /* 1 for player, 2 for publisher */
        if (pluginType == RB_PLUGIN_PLAYER)
        {
            RBPlayerAndroid player = mMapPlayer.get(id);
            if(player != null)
            {
              player.OnStateChange(curState);
            }
        }
        else if (pluginType == RB_PLUGIN_PUBLISHER)
        {
            RBPublisherAndroid publisher = mMapPublisher.get(id);
            if(publisher != null)
            {
                publisher.OnStateChange(curState);
            }
        }
    }

    private void CFJ_ErrorGet(int pluginType, int pluginId, int moduleId, int errorCode, String desc)
    {
        String id = Integer.toString(pluginId);
         /* 1 for player, 2 for publisher */
        if (pluginType == RB_PLUGIN_PLAYER)
        {
            RBPlayerAndroid player = mMapPlayer.get(id);
            if(player != null)
            {
                player.OnErrorGet(moduleId, errorCode, desc);
            }
        }
        else if (pluginType == RB_PLUGIN_PUBLISHER)
        {
            RBPublisherAndroid publisher = mMapPublisher.get(id);
            if(publisher != null)
            {
                publisher.OnErrorGet(moduleId, errorCode, desc);
            }
        }
    }

    private void CFJ_NoticeGet(int pluginType, int pluginId, int moduleId, int noticeCode, String desc)
    {
        String id = Integer.toString(pluginId);
         /* 1 for player, 2 for publisher */
        if (pluginType == RB_PLUGIN_PLAYER)
        {
            RBPlayerAndroid player = mMapPlayer.get(id);
            if(player != null)
            {
                player.OnNoticeGet(moduleId, noticeCode, desc);
            }
        }
        else if (pluginType == RB_PLUGIN_PUBLISHER)
        {
            RBPublisherAndroid publisher = mMapPublisher.get(id);
            if(publisher != null)
            {
                publisher.OnNoticeGet(moduleId, noticeCode, desc);
            }
        }
    }

    private void CFJ_MediaModeChange(int pluginType, int pluginId, int newMode)
    {
        String id = Integer.toString(pluginId);
         /* 1 for player, 2 for publisher */
        if (pluginType == RB_PLUGIN_PLAYER)
        {
            RBPlayerAndroid player = mMapPlayer.get(id);
            if(player != null)
            {
                player.OnMediaModeChange(newMode);
            }
        }
        else if (pluginType == RB_PLUGIN_PUBLISHER)
        {
            RBPublisherAndroid publisher = mMapPublisher.get(id);
            if(publisher != null)
            {
                publisher.OnMediaModeChange(newMode);
            }
        }
    }

    private void CFJ_PlayerVideoResolution(int pluginId, int width, int height)
    {
        String id = Integer.toString(pluginId);
        RBPlayerAndroid player = mMapPlayer.get(id);
        if(player != null)
        {
            player.OnVideoResolution(width, height);
        }
    }

    private void CFJ_PlayerBufferingChange(int pluginId, int percentage)
    {
        String id = Integer.toString(pluginId);
        RBPlayerAndroid player = mMapPlayer.get(id);
        if(player != null)
        {
            player.OnBufferingStateChange(percentage);
        }
    }

    private void CFJ_PublisherVideoIdChange(int pluginId, int newVideoId)
    {
        String id = Integer.toString(pluginId);
        RBPublisherAndroid publisher = mMapPublisher.get(id);
        if(publisher != null)
        {
            publisher.OnVideoIdChange(newVideoId);
        }
    }

    static {
        System.loadLibrary("RobinSDK_jni");
        nativeClassInit();
    }
}
