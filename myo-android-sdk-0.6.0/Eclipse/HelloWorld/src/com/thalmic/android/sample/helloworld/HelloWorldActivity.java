/*
 * Copyright (C) 2014 Thalmic Labs Inc.
 * Distributed under the Myo SDK license agreement. See LICENSE.txt for details.
 */

package com.thalmic.android.sample.helloworld;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.XDirection;
import com.thalmic.myo.scanner.ScanActivity;


public class HelloWorldActivity extends Activity {

    // This code will be returned in onActivityResult() when the enable Bluetooth activity exits.
    private static final int REQUEST_ENABLE_BT = 1;

    private TextView mTextView;

    RequestQueue queue;
    
    private void signal(String message) {
    	String url ="http://172.26.10.254:8080/" + message;
    	
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
        		new Response.Listener<String>() {
					@Override
					public void onResponse(String response) {
						Log.w("http", "got a response");
						Log.w("http", response);
					}
        		}, 
        		new Response.ErrorListener() {
        			@Override
        			public void onErrorResponse(VolleyError error) {
			            Log.w("http", "failed response");
			        }
        		});

        this.queue.add(stringRequest);

    }
    
    // Classes that inherit from AbstractDeviceListener can be used to receive events from Myo devices.
    // If you do not override an event, the default behavior is to do nothing.
    private DeviceListener mListener = new AbstractDeviceListener() {

        private Arm mArm = Arm.UNKNOWN;
        private XDirection mXDirection = XDirection.UNKNOWN;
        private String signalState = "rest";

        @Override
        public void onConnect(Myo myo, long timestamp) {
            mTextView.setTextColor(Color.CYAN);
        }

        @Override
        public void onDisconnect(Myo myo, long timestamp) {
            mTextView.setTextColor(Color.RED);
        }

        @Override
        public void onArmRecognized(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
            mArm = arm;
            mXDirection = xDirection;
        }

        @Override
        public void onArmLost(Myo myo, long timestamp) {
            mArm = Arm.UNKNOWN;
            mXDirection = XDirection.UNKNOWN;
        }

        @Override
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
            float roll = (float) Math.toDegrees(Quaternion.roll(rotation));
            float pitch = (float) Math.toDegrees(Quaternion.pitch(rotation));
            float yaw = (float) Math.toDegrees(Quaternion.yaw(rotation));
            
            if (mXDirection == XDirection.TOWARD_ELBOW) {
                roll *= -1;
                // pitch *= -1;
            }
            
            if (pitch > 35) {
            	mTextView.setText("->");
            	if (this.signalState != "->") {
                	this.signalState = ("->");
                	Log.w("myo", "->");
                	signal("->");
            	}
            } else if (pitch < -45) {
            	if (this.signalState != "X") {
                	this.signalState = ("X");
                	Log.w("myo", "X");
                	mTextView.setText("X");
                	signal("X");
            	}
            } else if (-15 < pitch && pitch < 15) {
            	if (this.signalState == "->" || this.signalState == "X") {
            		this.signalState = "_";
            		Log.w("myo", "_");
            		mTextView.setText("_");
            		signal("_");
            	}
            }
            
        }
        
        @Override
        public void onAccelerometerData(Myo myo, long timestamp, Vector3 accel) {
        	
        	if (accel.length() > 5) {
        		Log.w("fall", "falling?");
        		Log.w("fall", String.valueOf(accel.length()));
        		signal("fall");
        	}
        	
        }

        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {
            switch (pose) {
                case UNKNOWN:
                    mTextView.setText(getString(R.string.hello_world));
                    break;
                case REST:
//                    int restTextId = R.string.hello_world;
//                    switch (mArm) {
//                        case LEFT:
//                            restTextId = R.string.arm_left;
//                            break;
//                        case RIGHT:
//                            restTextId = R.string.arm_right;
//                            break;
//                    }
                	if (this.signalState == "<-") {
                        this.signalState = "_";
                		Log.w("myo", "_");
                        mTextView.setText("_");
                        signal("_");
                	}
                    break;
                case FIST:
                    // mTextView.setText(getString(R.string.pose_fist));
                    break;
                case WAVE_IN:
                    // mTextView.setText(getString(R.string.pose_wavein));
                    break;
                case WAVE_OUT:
                	if (this.signalState == "_") {
                    	this.signalState = "<-";
                    	mTextView.setText("<-");
                        Log.w("myo", "<-");
                        signal("<-");
                	}
                    break;
                case FINGERS_SPREAD:
                    // mTextView.setText(getString(R.string.pose_fingersspread));
                    break;
                case THUMB_TO_PINKY:
                    // mTextView.setText(getString(R.string.pose_thumbtopinky));
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.activity_hello_world);
        
        mTextView = (TextView) findViewById(R.id.text);
        
        this.queue = Volley.newRequestQueue(this);
        this.signal("yhack14");

        // First, we initialize the Hub singleton with an application identifier.
        Hub hub = Hub.getInstance();
        if (!hub.init(this, getPackageName())) {
            // We can't do anything with the Myo device if the Hub can't be initialized, so exit.
            Toast.makeText(this, "Couldn't initialize Hub", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Next, register for DeviceListener callbacks.
        hub.addListener(mListener);
        
        this.onScanActionSelected();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If Bluetooth is not enabled, request to turn it on.
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // We don't want any callbacks when the Activity is gone, so unregister the listener.
        Hub.getInstance().removeListener(mListener);

        if (isFinishing()) {
            // The Activity is finishing, so shutdown the Hub. This will disconnect from the Myo.
            Hub.getInstance().shutdown();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth, so exit.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (R.id.action_scan == id) {
            onScanActionSelected();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void onButtonClick(View v) {
    	Log.w("button", "button clicked");
    	onScanActionSelected();
    }

    public void onScanActionSelected() {
        // Launch the ScanActivity to scan for Myos to connect to.
        Intent intent = new Intent(this, ScanActivity.class);
        startActivity(intent);
    }
}
