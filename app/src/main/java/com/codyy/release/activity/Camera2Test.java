package com.codyy.release.activity;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.codyy.release.R;
import com.codyy.robinsdk.impl.Ahc2VideoCapture;

public class Camera2Test extends AppCompatActivity implements AdapterView.OnItemSelectedListener{

    private static final String TAG = "Camera2Test";

    private CameraManager mCameraManager = null;
    private SurfaceView mSurfaceView = null;
    private Spinner mSpinnerName = null;
    private Spinner mSpinnerResolution = null;

    private Ahc2VideoCapture mVideoCapture = null;

    private HandlerThread mHandlerThread = null;
    private Handler mHandler = null;

    private final int BUTTON_MSG_CAMERA_START = 1;
    private final int BUTTON_MSG_CAMERA_STOP = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2_test);

        mCameraManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView_camera);
        mSpinnerName = (Spinner) this.findViewById(R.id.spinner_camera_name);
        mSpinnerResolution = (Spinner) this.findViewById(R.id.spinner_camera_resolution);

        setButtonListener(R.id.button_camera_start);
        setButtonListener(R.id.button_camera_stop);
        setButtonListener(R.id.button_camera_reserved1);
        setButtonListener(R.id.button_camera_reserved2);
    }

    public void onResume() {
        super.onResume();
        mVideoCapture = new Ahc2VideoCapture(0, mCameraManager);

        mHandlerThread = new HandlerThread("Camera2Test");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                switch (msg.what){
                    case BUTTON_MSG_CAMERA_START:
                        Log.d(TAG, "button start");

                        mVideoCapture.setCamera(0);
                        mVideoCapture.setPreviewResolution(640, 480);
                        mVideoCapture.setFrameRate(25);
                        mVideoCapture.start(mSurfaceView);

                        break;
                    case BUTTON_MSG_CAMERA_STOP:
                        Log.d(TAG, "button stop");
//                        mVideoCapture.stop();

                        break;
                    default:
                        break;
                }
            }
        };;
    }

    public void onPause(){
        super.onPause();

        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setButtonListener(int id) {
        Button button;
        button = (Button) this.findViewById(id);
        button.setOnClickListener(mClickListener);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            switch (view.getId()){
                case R.id.button_camera_start:
                    mHandler.sendEmptyMessage(BUTTON_MSG_CAMERA_START);
                    break;
                case R.id.button_camera_stop:
                    mHandler.sendEmptyMessage(BUTTON_MSG_CAMERA_STOP);
                    break;
                case R.id.button_camera_reserved1:
                    break;
                case R.id.button_camera_reserved2:
                    break;
                default:
                    break;
            }
        }
    };

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
    {
        Spinner spinner = (Spinner) parent;

        TextView tv = (TextView)view;
        tv.setTextSize(12.0f);    //设置大小

        int position = spinner.getSelectedItemPosition();
        if (spinner == mSpinnerName){

        }else if (spinner == mSpinnerResolution){

        }
    }

    public void onNothingSelected(AdapterView<?> parent) {}
}
