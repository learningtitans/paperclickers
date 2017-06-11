/*
 * Paperclickers - Affordable solution for classroom response system.
 * 
 * Copyright (C) 2015 Jomara Bindá <jbinda@dca.fee.unicamp.br>
 * Copyright (C) 2015-2016 Eduardo Valle Jr <dovalle@dca.fee.unicamp.br>
 * Copyright (C) 2015-2016 Eduardo Seiti de Oliveira <eduseiti@dca.fee.unicamp.br>
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *   
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *   
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * 
 */

package com.paperclickers.camera;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.paperclickers.AudienceResponses;
import com.paperclickers.TopCodeValidator;
import com.paperclickers.fiducial.TopCode;
import com.paperclickers.result.GridViewActivity;
import com.paperclickers.R;
import com.paperclickers.SettingsActivity;
import com.paperclickers.log;
import com.paperclickers.camera.OrientationManager.OrientationListener;
import com.paperclickers.camera.OrientationManager.ScreenOrientation;

public class CameraMain extends Activity implements Camera.PreviewCallback, CameraChangeListener, OrientationListener {

	final static String TAG = "paperclickers.CameraMain";

	final static int SCAN_DISMISS_TIMEOUT = 1000;

	private Camera mCamera;
	private CameraPreview mCameraPreview;
	private DrawView mDraw;
	private FrameLayout mPreview;

	private Vibrator mVibrator;
	private long mTouchStart = -1;

	private int mImageWidth;
	private int mImageHeight;

	private boolean mUserRequestedEnd = false;

	long mStartScanTime;
	long mEndScanTime;

	Context mContext;

	TextView mHint1TextView;
	TextView mFreqDebugTextView;
	TextView mDevelopmentData;

	String mDevelopmentCurrentCycle;
	String mDevelopmentCurrentThreshold;

	String mHint1StringStart;
	String mHint1StringEnd;

	private boolean mShowingValidation = false;

	private Timer scanDismissTimer = null;

	boolean mHasRotated = false;

	OrientationManager mOrientationManager = null;

	AudienceResponses mAudienceResponses = null;



	private void callNextActivity() {

		log.d(TAG,">>>>> callNextActivity");

		Intent i = new Intent(getApplicationContext(), GridViewActivity.class);

		i.putExtra("detectedAnswers", mAudienceResponses.returnDetectedTopCodesList());

		mEndScanTime = System.currentTimeMillis();

		log.d(TAG, String.format("Total scan cycles: %d, total scan time (ms): %d", mAudienceResponses.getScanCycleCount(), mEndScanTime - mStartScanTime));

		mAudienceResponses.finalize();

		finish();

		startActivity(i);
	}

	
	
	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance

			log.d(TAG, "> getCameraInstance - size: " + c.getParameters().getPreviewSize());
			log.d(TAG, "> getCameraInstance - frame rate: " + c.getParameters().getPreviewFrameRate());
			
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		    log.e(TAG, e.toString());
		}
		return c; // returns null if camera is unavailable
	}

	
	
	private void hasDetectedEnd(boolean timeoutOccurred) {


    	if (mTouchStart != -1) {
			if ((timeoutOccurred) || (System.currentTimeMillis() - mTouchStart > SCAN_DISMISS_TIMEOUT)) {
				if (mVibrator != null) {
					mVibrator.vibrate(50);
				}

				mUserRequestedEnd = true;

				mTouchStart = -1;

				log.d(TAG, ">>>>> hasDetectedEnd. Registered termination");
			} else {
				mUserRequestedEnd = false;
			}
    	}
    	
        if (scanDismissTimer != null) {
            scanDismissTimer.cancel();
            scanDismissTimer = null;
        }
	}
	
	
	
	void hideStatusBar() {
	    Window w = getWindow();
	    
		View decorView = w.getDecorView();
		// Hide the status bar.
		
		int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN + View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN + View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
		
		if (Build.VERSION.SDK_INT >= 19) {
		    uiOptions += View.SYSTEM_UI_FLAG_HIDE_NAVIGATION + View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		}

		decorView.setSystemUiVisibility(uiOptions);
	}
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		
		mContext  = getApplicationContext();
		mVibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
		
		hideStatusBar();
		
		mHint1StringStart  = (String) getResources().getText(R.string.hint1Start);
		mHint1StringEnd    = (String) getResources().getText(R.string.hint1End);
		mHint1TextView     = (TextView) findViewById(R.id.txt_hint1);
		mFreqDebugTextView = (TextView) findViewById(R.id.txt_freqDebug);
		mDevelopmentData   = (TextView) findViewById(R.id.txt_development_data);
		
		mDevelopmentCurrentCycle     = (String) getResources().getText(R.string.development_current_cycle);
		mDevelopmentCurrentThreshold = (String) getResources().getText(R.string.development_current_threshold);
		
		if (!AudienceResponses.SHOW_CODE_FREQUENCY_DEBUG) {
			mFreqDebugTextView.setVisibility(View.GONE);
		}
		
		mPreview = (FrameLayout) findViewById(R.id.camera_preview);

		mAudienceResponses = new AudienceResponses(mContext, PreferenceManager.getDefaultSharedPreferences(this));

		mAudienceResponses.checkIncomingIntentAndInitialize(getIntent());
		
		mUserRequestedEnd = false;
	}

	
	
    protected void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        
		log.e(TAG, "Shouldn't be entering on onNewIntent(), since CameraMain should have been finished");
    }
	
	
	
    @Override
    public void onOrientationChange(ScreenOrientation screenOrientation) {
        
        switch(screenOrientation){
            case PORTRAIT:
            case REVERSED_PORTRAIT:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
                
            case LANDSCAPE:
            case REVERSED_LANDSCAPE:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
        }
    }
    
    
    
	@Override
	protected void onPause() {
		super.onPause();
		
		log.d(TAG, "===> onPause()");
		
		releaseCamera();

	    mOrientationManager.disable();
	}

	
	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {

		List<TopCode> recognizedValidTopCodes = new ArrayList<TopCode>();
		List<TopCode> topCodes = null;

		int cycleResult = mAudienceResponses.onNewFrame(data, mHasRotated, mUserRequestedEnd, recognizedValidTopCodes, topCodes);

		if (cycleResult == AudienceResponses.COMPLETLY_IGNORE_CYCLE) {
			return;
		} else if (cycleResult == AudienceResponses.NEED_TO_REDRAW) {

			if (AudienceResponses.AVOID_PARTIAL_READINGS) {
				mHint1TextView.setText(mHint1StringStart + mAudienceResponses.getValidTopCodesCount() + mHint1StringEnd);
			} else {
				mHint1TextView.setText(mHint1StringStart + mAudienceResponses.getRecognizedTopCodesCount() + mHint1StringEnd);
			}

			if (AudienceResponses.SHOW_CODE_FREQUENCY_DEBUG) {
				mFreqDebugTextView.setText(Arrays.toString(mAudienceResponses.getFinalTopCodesFrequency()));
			}

			if (AudienceResponses.AVOID_PARTIAL_READINGS) {
				mDraw.updateValidTopcodesList(recognizedValidTopCodes);
			} else {
				mDraw.updateValidTopcodesList(topCodes);
			}

			mDraw.postInvalidate();
		}

		if (mShowingValidation) {
			mDevelopmentData.setText(String.format("%s %d\n%s %d", mDevelopmentCurrentCycle, mAudienceResponses.getScanCycleCount(), mDevelopmentCurrentThreshold, TopCodeValidator.getCurrentValidationThrehshold()));
		}

		if (mUserRequestedEnd) {
			callNextActivity();
		}
	}

	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		log.d(TAG, "===> onResume()");
		
		hideStatusBar(); 

        if (AudienceResponses.AVOID_PARTIAL_READINGS) {
            mHint1TextView.setText(mHint1StringStart + mAudienceResponses.getValidTopCodesCount() + mHint1StringEnd);
        } else {
            mHint1TextView.setText(mHint1StringStart + mAudienceResponses.getRecognizedTopCodesCount() + mHint1StringEnd);
        }
            		
		mStartScanTime = System.currentTimeMillis();

		if (SettingsActivity.DEVELOPMENT_OPTIONS) {
		    if (SettingsActivity.getDevelopmentMode()) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                
                String validationThresholdStr = prefs.getString("development_show_validation", "1");
    
                mShowingValidation = validationThresholdStr.equals("1");
		    } else {
		        mShowingValidation = false;
		    }

		    if (mShowingValidation) {
		        mDevelopmentData.setVisibility(View.VISIBLE);
		    } else {
                mDevelopmentData.setVisibility(View.GONE);
		    }
		}
		
		setCamera();
		setCameraPreview();
		
		mOrientationManager = new OrientationManager(mContext, SensorManager.SENSOR_DELAY_NORMAL, this);
		mOrientationManager.enable();
	}
	
	
	
	@Override
	protected void onStart() {
		super.onStart();
		
		log.d(TAG, "===> onStart()");
		
		setCamera();
	}
	
	
	
    @Override
    protected void onStop() {
        super.onStop();
        
        log.d(TAG, "===> onStop()");
        
        // Make sure the camera is always released, even if the activity hasn't being shown yet
        releaseCamera();
    }
	
	
	
	private void releaseCamera() {

		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
		}

		
		mPreview.removeView(mDraw);
		
		
		if (mCameraPreview != null) {
			mPreview.removeView(mCameraPreview);
			
			mCameraPreview = null;
		}
	}


	
	private void setCamera() {

		if (mCamera == null) {
			try {
				mCamera = getCameraInstance();
				
				Camera.Parameters cameraParameters = mCamera.getParameters();
				
				Camera.Size cameraSize = cameraParameters.getPreviewSize();
				
				mImageWidth  = cameraSize.width;
				mImageHeight = cameraSize.height;
				
				log.d(TAG, String.format("Size %d x %d", mImageWidth, mImageHeight));
				
                List<String> supportedFocusModes = cameraParameters.getSupportedFocusModes();
                
                if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                } else {
                    cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
				
				mCamera.setParameters(cameraParameters);
				
				int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
				int cameraRotation = 0;
				
				if ((displayRotation == Surface.ROTATION_0) || (displayRotation == Surface.ROTATION_180)) {
				    
				    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				    
				    cameraRotation = 90;
				} else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				}
				
				if (cameraRotation != 0) {
				    
				    mCamera.setDisplayOrientation(cameraRotation);
				    
				    mImageWidth  = cameraSize.height;
				    mImageHeight = cameraSize.width;
				    
				    mHasRotated = true;
				    
				    log.d(TAG, String.format("Changing camera orientation (%d); width=%d, height=%d", cameraRotation, mImageWidth, mImageHeight));
				} else {
                    mHasRotated = false;
                }

				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
		
		
	
	private void setCameraPreview() {	
		
		if ((mCamera != null) && (mCameraPreview == null)) {
			
			mCameraPreview = new CameraPreview(this, mCamera, this);
			mPreview.addView(mCameraPreview);

			mDraw = new DrawView(mContext, mAudienceResponses.getTopCodesValidator(), mImageWidth, mImageHeight, mShowingValidation);

			mPreview.addView(mDraw);
			
			mCameraPreview.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					mCameraPreview.focusCamera();
				}
			});
			
			
			mCameraPreview.setOnTouchListener(new OnTouchListener() {
				
				@Override
				public boolean onTouch(View v, MotionEvent event) {
										
				    switch (event.getAction() & MotionEvent.ACTION_MASK) {
				        case MotionEvent.ACTION_DOWN:
				        	
				        	mTouchStart = System.currentTimeMillis();
				        	
				        	scanDismissTimer = new Timer();
				        	
				        	scanDismissTimer.schedule(new TimerTask() {
				        	    
				        	    public void run() {
				        	        hasDetectedEnd(true);
				        	    }
				        	}, (long) SCAN_DISMISS_TIMEOUT);
				        	
				        	break;
				        	
				        case MotionEvent.ACTION_UP:
				        	
				        	hasDetectedEnd(false);
				        	
				        	if (!mUserRequestedEnd) {
				        		mTouchStart = -1;
				        		
				        		mCameraPreview.performClick();
				        	}
				        	
				        	break;
				        	
				        case MotionEvent.ACTION_MOVE:
				        	
				        	hasDetectedEnd(false);
					        	
				            break;
				        case MotionEvent.ACTION_CANCEL:
				        default:
				            
                            if (scanDismissTimer != null) {
                                scanDismissTimer.cancel();
                                scanDismissTimer = null;
                            }
                            				            
				            break;
				    }
					
					return mUserRequestedEnd;
				}
			});
		}
	}

	

	public void updateChangedCameraSize(int width, int height) {
			 
        log.d(TAG, String.format("New camera size: %d x %d. Null camera reference? %b", width, height, (mCamera == null))); 
        
        if (width < height) {
            mHasRotated = true;
            
            // Camera preview always comes in landscape format...
            
            mImageWidth  = height;
            mImageHeight = width;
        } else {
            mHasRotated = false;

            mImageWidth  = width;
            mImageHeight = height;
        }

        if (mAudienceResponses == null) {
		} else {
			mAudienceResponses.setImageSize(mImageWidth, mImageHeight);
		}

        if (mDraw != null) {
            if (mHasRotated) {
                mDraw.updateScreenSize(mImageHeight, mImageWidth);
            } else {
                mDraw.updateScreenSize(mImageWidth, mImageHeight);
                
            }
        }		
	}
	 
	 
	 
}
