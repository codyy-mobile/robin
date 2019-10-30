package com.codyy.release.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.codyy.release.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "MainActivity";
    private static final int MY_PERMISSION_REQUEST_CAMERA = 1;
    private static final int MY_PERMISSION_REQUEST_RECORD_AUDIO = 2;
    private static final int MY_PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 3;

    private boolean mPermissionPass = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        askCameraPermission();
    }

    private void initView() {
        TextView publisherAndPlayTextView = (TextView) findViewById(R.id.publish_and_play);
        TextView cameraDemonTextView = (TextView) findViewById(R.id.camera_and_audio_demon);
        TextView multiplexPlayer = (TextView) findViewById(R.id.multiplex_player);
        TextView twoPublish = (TextView) findViewById(R.id.two_publisher);
        TextView screenCaptureEncode = (TextView)findViewById(R.id.screen_capture_encode);
        TextView camera2Test = (TextView)findViewById(R.id.camera_2_test);
        TextView TextureViewTest = (TextView)findViewById(R.id.texture_view_test);

        publisherAndPlayTextView.setOnClickListener(this);
        cameraDemonTextView.setOnClickListener(this);
        multiplexPlayer.setOnClickListener(this);
        twoPublish.setOnClickListener(this);
        screenCaptureEncode.setOnClickListener(this);
        camera2Test.setOnClickListener(this);
        TextureViewTest.setOnClickListener(this);
    }

    public void onClick(View view) {
        if(!mPermissionPass) {
            FunctionBounced("we can't get enough permission");
            return;
        }

        Intent intent = null;
        switch (view.getId()){
            case R.id.publish_and_play:
                intent = new Intent(MainActivity.this, PublisherAndPlayActivity.class);
                break;
            case R.id.camera_and_audio_demon:
                intent = new Intent(MainActivity.this, CameraAndAudioDemon.class);
                break;
            case R.id.multiplex_player:
                intent = new Intent(MainActivity.this, MultiplexPlayerActivity.class);
                break;
            case R.id.two_publisher:
                intent = new Intent(MainActivity.this, TwoPublisherActivity.class);
                break;
            case R.id.screen_capture_encode:
                intent = new Intent(MainActivity.this, ScreenCaptureEncode.class);
                break;
            case R.id.camera_2_test:
                intent = new Intent(MainActivity.this, Camera2Test.class);
                break;
            case R.id.texture_view_test:
                intent = new Intent(MainActivity.this, TextureViewTest.class);
        }

        if(null != intent){
            startActivity(intent);
        }
    }

    // Ask for Permissions
    private void askCameraPermission()
    {
        if(Build.VERSION.SDK_INT >= 23){
            if(this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                this.requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_PERMISSION_REQUEST_CAMERA);
            }
        }
    }

    private void askRecordAudioPermission()
    {
        if (Build.VERSION.SDK_INT >= 23){
            if(this.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
                this.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSION_REQUEST_RECORD_AUDIO);
            }
        }
    }

    private void askWriteExternalStoragePermission(){
        if (Build.VERSION.SDK_INT >= 23){
            if(this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case MY_PERMISSION_REQUEST_CAMERA:{
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "REQUEST_CAMERA, Permission Granted");
                    askRecordAudioPermission();
                } else {
                    mPermissionPass = false;
                    FunctionBounced("REQUEST_CAMERA, Permission Denied");
                }
                break;
            }

            case MY_PERMISSION_REQUEST_RECORD_AUDIO:{
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "REQUEST_RECORD_AUDIO, Permission Granted");
                    askWriteExternalStoragePermission();
                } else {
                    mPermissionPass = false;
                    FunctionBounced("REQUEST_RECORD_AUDIO, Permission Denied");
                }
                break;
            }

            case MY_PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE:{
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "REQUEST_WRITE_EXTERNAL_STORAGE, Permission Granted");
                } else {
                    mPermissionPass = false;
                    FunctionBounced("REQUEST_WRITE_EXTERNAL_STORAGE, Permission Denied");
                }
                break;
            }
        }
    }

    private void FunctionBounced(CharSequence text)
    {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

    public void ttCameraClick(View view) {
        Intent  intent = new Intent(MainActivity.this, Publisher2AndPlay4Activity.class);
        startActivity(intent);
    }
}
