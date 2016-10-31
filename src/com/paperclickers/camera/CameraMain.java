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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.paperclickers.fiducial.PaperclickersScanner;
import com.paperclickers.fiducial.TopCode;
import com.paperclickers.result.GridViewActivity;
import com.paperclickers.R;
import com.paperclickers.SettingsActivity;
import com.paperclickers.log;

public class CameraMain extends Activity implements Camera.PreviewCallback, CameraChangeListener {

    // Intent used to call CameraMain passing back a list of previously validated topcodes
	public static String RECALL_CODES_INTENT = "com.paperclickers.intent.action.RECALL_CODES";
	
	final static String TAG = "CameraMain";

	// Use this constant to enable saving the last analyzed image, right after user requesting to
	// carry on to the Grid View
	final static boolean SAVE_LAST_IMAGE = true;

	// Use this constant to enable showing the class topcodes detection raw (regardless validation) 
	// frequencies as an overlay in camera capture
	final static boolean SHOW_CODE_FREQUENCY_DEBUG = false;
	
	// Use this constant to enable the overall topcodes validation mechanism
	public final static boolean AVOID_PARTIAL_READINGS = true;
	
	// Use this constant to enable the detailed debug log, showing every topcodes detection and validation
	// data
	final static boolean DEBUG_DETECTION_CYCLE_RAW_DATA = false;
	
	// Defined by TopcodeValidator constant: refers to the moving validation threshold mechanism
	final static boolean MOVING_VALIDATION_THRESHOLD = TopcodeValidator.MOVING_VALIDATION_THRESHOLD;
	
	// Use this constant to enable the frame drop
	final static boolean DROP_EVERY_OTHER_FRAME = true;
	
	// Use this constant to enable previewing only validated codes as an overlay in the camera capture
	final static boolean ONLY_PREVIEW_VALIDATED_CODES = true ;
	
	final static int SCAN_DIMISS_TIMEOUT = 1500;
	
	private Camera        mCamera;
	private CameraPreview mCameraPreview;
	private DrawView      mDraw;
	private FrameLayout   mPreview;
	
	private Vibrator 	  mVibrator;
	private long          mTouchStart = -1;
	
	private int[] mLuma;
	private int mImageWidth;
	private int mImageHeight;
	
	private int mRecognizedTopcodesCount;
	private int mValidTopcodesCount;
	
	private boolean mUserRequestedEnd = false;
	
	long mStartScanTime;
	long mEndScanTime;
	long mStartOnPreviewTime;
	long mEndOnPreviewTime;
    long mStartFiducialTime;
    long mEndFiducialTime;	
	
	int mScanCycle;
	
	Context mContext;
	
	TextView mHint1TextView;
	TextView mFreqDebugTextView;
	TextView mDevelopmentData;
	
	String mDevelopmentCurrentCycle;
	String mDevelopmentCurrentThreshold;

	String mHint1StringStart;
	String mHint1StringEnd;

	private boolean mIgnoreCall;
	private boolean mShowingValidation = false;

	private SparseArray<TopCode> mFinalTopcodes;
    private SparseArray<TopcodeValidator> mFinalTopcodesValidator;
	private Integer[] mFinalTopcodesFrequency;	

	private Timer scanDismissTimer = null;
	
	
	private void callNextActivity() {

		HashMap<Integer, String> detectedTopcodes = new HashMap<Integer, String>();

		if (mFinalTopcodes != null) {
			
			// Build a hashmap containing only code and detected answer of each topcode
			
			for (int i = 0; i < mFinalTopcodes.size(); i++) {
				
				TopCode t = mFinalTopcodes.valueAt(i);
				
				TopcodeValidator validator = mFinalTopcodesValidator.valueAt(i);

				String translatedAnswer;
				
				if (AVOID_PARTIAL_READINGS) {
					int bestAnswer = validator.getBestValidAnswer();

					translatedAnswer = PaperclickersScanner.translateOrientationIDToString(bestAnswer);
				} else {
					translatedAnswer = PaperclickersScanner.translateOrientationToString(t.getOrientation());
				}
				
				detectedTopcodes.put(i + 1, translatedAnswer);
				
				if (AVOID_PARTIAL_READINGS) {				
					log.d(TAG, String.format("> Translated code: %d, Answer: %s, Orientation: %s, Frequency: %d(A), %d(B), %d(C), %d(D), isValid: %b, lastDetectedScanCycle: "
							                 + "%d(A), %d(B), %d(C), %d(D), numberOfContinuousDetection: %d(A), %d(B), %d(C), %d(D)", 
											 i + 1, translatedAnswer, String.valueOf(t.getOrientation()), 
											 validator.getFrequency(PaperclickersScanner.ID_ANSWER_A), validator.getFrequency(PaperclickersScanner.ID_ANSWER_B), 
											 validator.getFrequency(PaperclickersScanner.ID_ANSWER_C), validator.getFrequency(PaperclickersScanner.ID_ANSWER_D), 
											 validator.isValid(), 
											 validator.getLastDetectedScanCycle(PaperclickersScanner.ID_ANSWER_A), validator.getLastDetectedScanCycle(PaperclickersScanner.ID_ANSWER_B), 
											 validator.getLastDetectedScanCycle(PaperclickersScanner.ID_ANSWER_C), validator.getLastDetectedScanCycle(PaperclickersScanner.ID_ANSWER_D), 
											 validator.getNumberOfContinuousDetection(PaperclickersScanner.ID_ANSWER_A), validator.getNumberOfContinuousDetection(PaperclickersScanner.ID_ANSWER_B), 
											 validator.getNumberOfContinuousDetection(PaperclickersScanner.ID_ANSWER_C), validator.getNumberOfContinuousDetection(PaperclickersScanner.ID_ANSWER_D)));
				} else {
					log.d(TAG, String.format("> Translated code: %d, Answer: %s, Orientation: %s, Frequency: %d(A), %d(B), %d(C), %d(D)", 
							 i + 1, translatedAnswer, String.valueOf(t.getOrientation()), 
							 validator.getFrequency(PaperclickersScanner.ID_ANSWER_A), validator.getFrequency(PaperclickersScanner.ID_ANSWER_B),
							 validator.getFrequency(PaperclickersScanner.ID_ANSWER_C), validator.getFrequency(PaperclickersScanner.ID_ANSWER_D)));
				}
			}
			
			mEndScanTime = System.currentTimeMillis();
						
			log.d(TAG, String.format("Total scan cycles: %d, total scan time (ms): %d", mScanCycle, mEndScanTime - mStartScanTime));
		} else {
			log.d(TAG, "Empy class!!! Shouldn't have happen; applicaton shared preferences might have been tampered!!!");
		}

		log.d(TAG, "callNextActivity with topcodes list of size: " + detectedTopcodes.size());		

		finish();
		
		Intent i = new Intent(getApplicationContext(), GridViewActivity.class);

		i.putExtra("detectedAnswers", detectedTopcodes);
		
		startActivity(i);
	}

	
	
	private void checkIncommingIntentAndInitialize(Intent whichIntent) {
		
		log.d(TAG, "===> checkIncommingIntentAndInitialize()");

        String intentAction = whichIntent.getAction(); 
        
        Serializable receivedTopcodes = null;
        
        if ((intentAction != null) && 
        	(intentAction.equals(CameraMain.RECALL_CODES_INTENT))) {
        	
        	receivedTopcodes = whichIntent.getSerializableExtra("detectedAnswers");

        }
        
    	initializeTopcodesList((HashMap<Integer, String>) receivedTopcodes);	
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

	
	
	private boolean hasDetectedEnd(boolean timeoutOccurred) {
		
		boolean hasDetected = false;
	
    	if (mTouchStart != -1) {
        	if ((timeoutOccurred) || (System.currentTimeMillis() - mTouchStart > SCAN_DIMISS_TIMEOUT)) {
				if (mVibrator != null) {
				    mVibrator.vibrate(50);
				}
				
				mTouchStart = -1;
				
				log.d(TAG, String.format("Last scan time: %d", mEndOnPreviewTime - mStartOnPreviewTime));
				
	            if (SAVE_LAST_IMAGE) {
	                writePGMAfterThreshold("lastfile.pgm", mLuma, mImageWidth, mImageHeight);               
	            }
				
				callNextActivity();
				
				hasDetected = true;
        	}
    	}
    	
        if (scanDismissTimer != null) {
            scanDismissTimer.cancel();
            scanDismissTimer = null;
        }
    	
    	return hasDetected;
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
	
	
	
	private void initializeTopcodesList(HashMap<Integer, String> receivedTopcodes) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		String studentsStr = prefs.getString("students_number", "40");
		int studentsNum    = Integer.parseInt(studentsStr);		
		
		mFinalTopcodes          = new SparseArray<TopCode>(studentsNum);
		mFinalTopcodesValidator = new SparseArray<TopcodeValidator>(studentsNum);
		
		if (SHOW_CODE_FREQUENCY_DEBUG) {
			mFinalTopcodesFrequency = new Integer[studentsNum * PaperclickersScanner.NUM_OF_VALID_ANSWERS];
		}
		
		mRecognizedTopcodesCount = 0;
		mValidTopcodesCount      = 0;
		mScanCycle               = 0;
		
		log.d(TAG, "initializeTopcodesList - students number: " + studentsNum);
		
		for (int i = 0; i < studentsNum; i++) {
			
			TopCode newTopCode = new TopCode(SettingsActivity.validTopCodes[i]);
			
			newTopCode.setOrientation(PaperclickersScanner.INVALID_TOPCODE_ORIENTATION);
			
			mFinalTopcodes.put(SettingsActivity.validTopCodes[i], newTopCode);
			
			TopcodeValidator validator = new TopcodeValidator(newTopCode, getApplicationContext());
			mFinalTopcodesValidator.put(SettingsActivity.validTopCodes[i], validator);

			log.d(TAG, "> Adding topcode (" + SettingsActivity.validTopCodes[i] + ") to the valid list. Total of: " + mFinalTopcodes.size());
			
			if (receivedTopcodes != null && receivedTopcodes.size() != 0) {
				String answer = receivedTopcodes.get(i + 1);
				
				log.d(TAG, "initializeTopcodesList - code: " + SettingsActivity.validTopCodes[i] + " orientation: " + answer);
				
				if (answer != null) {
					
					float orientation = PaperclickersScanner.INVALID_TOPCODE_ORIENTATION;
					int   answerID    = PaperclickersScanner.ID_NO_ANSWER;
					
					if (answer.equals(PaperclickersScanner.ANSWER_A)) {
						orientation = -PaperclickersScanner.ANSWER_A_VALID_ORIENTATION;
						answerID    = PaperclickersScanner.ID_ANSWER_A;
					} else if (answer.equals(PaperclickersScanner.ANSWER_B)) {
						orientation = -PaperclickersScanner.ANSWER_B_VALID_ORIENTATION;
						answerID    = PaperclickersScanner.ID_ANSWER_B;
					} else if (answer.equals(PaperclickersScanner.ANSWER_C)) {
						orientation = -PaperclickersScanner.ANSWER_C_VALID_ORIENTATION;
						answerID    = PaperclickersScanner.ID_ANSWER_C;
					} else if (answer.equals(PaperclickersScanner.ANSWER_D)) {
						orientation = -PaperclickersScanner.ANSWER_D_VALID_ORIENTATION;
						answerID    = PaperclickersScanner.ID_ANSWER_D;
					}
					
					newTopCode.setOrientation(orientation);
					
					if (PaperclickersScanner.isValidOrientation(orientation)) {
						mRecognizedTopcodesCount++;
						mValidTopcodesCount++;
						
						validator.forceValid(answerID);
					}
				}
			}
		}
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
		
		if (!SHOW_CODE_FREQUENCY_DEBUG) {
			mFreqDebugTextView.setVisibility(View.GONE);
		}
		
		mPreview = (FrameLayout) findViewById(R.id.camera_preview);
		
		checkIncommingIntentAndInitialize(getIntent());
		
		mUserRequestedEnd = false;
	}

	
	
    protected void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        
		log.e(TAG, "Shouldn't be entering on onNewIntent(), since CameraMain should have been finished");
    }
	
	
	
	@Override
	protected void onPause() {
		super.onPause();
		
		log.d(TAG, "===> onPause()");
		
		
		releaseCamera();

	}

	
	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {

		List<TopCode> recognizedValidTopcodes = new ArrayList<TopCode>();
        List<TopCode> topcodes = null;
        PaperclickersScanner scan = new PaperclickersScanner();		

        // Check if should drop this frame 
		if (DROP_EVERY_OTHER_FRAME) {
			mIgnoreCall = !mIgnoreCall;
		
			if (mIgnoreCall) {
				return;
			}
		}
		
		mStartOnPreviewTime = System.currentTimeMillis();
		
		stripLumaFromYUV420SP(mLuma, data, mImageWidth, mImageHeight);
		
		try {
			mStartFiducialTime = System.currentTimeMillis();
			topcodes           = scan.scan(mLuma, mImageWidth, mImageHeight);
			mEndFiducialTime   = System.currentTimeMillis();
			
			if (SAVE_LAST_IMAGE && mUserRequestedEnd) {
				writePGMAfterThreshold("lastfile.pgm", mLuma, mImageWidth, mImageHeight);				
			}
		} catch (NotFoundException e1) {
			log.e(TAG, e1.toString());
		}
		
		if (topcodes != null) {			
			if (topcodes.size() > 0) {
				
				// Update the existing topcodes list with the information found, either including new ones or
				// updating the orientation of the existing ones.
				
				for (TopCode t : topcodes) {
					
					TopCode validTopcode = mFinalTopcodes.get(t.getCode());
					
					String scanDebug; 
					
					if (validTopcode != null) {
						TopcodeValidator currentValidator = mFinalTopcodesValidator.get(t.getCode());
						
						int translatedAnswer = PaperclickersScanner.translateOrientationToID(t.getOrientation());
						int continuousDetectionResult; 
						
						currentValidator.incFrequency(translatedAnswer);
						
						if (AVOID_PARTIAL_READINGS) {
							continuousDetectionResult = currentValidator.checkContinousDetection(mScanCycle, translatedAnswer, validTopcode);
							
							if (continuousDetectionResult == TopcodeValidator.CHANGED_ANSWER) {
								log.d(TAG, String.format("Code %d changed orientation: %f to %f", t.getCode(), validTopcode.getOrientation(), t.getOrientation()));
								
								if (!ONLY_PREVIEW_VALIDATED_CODES || mShowingValidation) {
								    recognizedValidTopcodes.add(t);
								}
							} else if (continuousDetectionResult == TopcodeValidator.TURNED_VALID || 
							           continuousDetectionResult == TopcodeValidator.VALID_ALREADY) {
							    
							    if (continuousDetectionResult == TopcodeValidator.TURNED_VALID) {
    							
                                    mValidTopcodesCount++;
							    }

                                recognizedValidTopcodes.add(t);
							} else if (!ONLY_PREVIEW_VALIDATED_CODES || mShowingValidation) {
							    recognizedValidTopcodes.add(t);
							}
						} else {
							currentValidator.checkContinousDetection(mScanCycle, translatedAnswer, validTopcode);
							
                            mValidTopcodesCount++;							
						}
						
						if (!PaperclickersScanner.isValidOrientation(validTopcode.getOrientation())) {
							mRecognizedTopcodesCount++;
						}								
	
						// Update topcode with the information recognized in this scan cycle
						validTopcode.setDiameter(t.getDiameter());
						validTopcode.setLocation(t.getCenterX(), t.getCenterY());
						validTopcode.setOrientation(t.getOrientation());
						
						// Only logging debug information...
						if (DEBUG_DETECTION_CYCLE_RAW_DATA) {
							if (AVOID_PARTIAL_READINGS) {
								scanDebug = String.format("Cycle: %d, Code: %d,  orientation: %f, frequency: %d(A), %d(B), %d(C), %d(D), isValid: %b, lastDetectedScanCycle: "
										                  + "%d(A), %d(B), %d(C), %d(D), numberOfContinuousDetection: %d(A), %d(B), %d(C), %d(D)",
										                  mScanCycle, t.getCode(), t.getOrientation(), 
										                  currentValidator.getFrequency(PaperclickersScanner.ID_ANSWER_A), currentValidator.getFrequency(PaperclickersScanner.ID_ANSWER_B),
										                  currentValidator.getFrequency(PaperclickersScanner.ID_ANSWER_C), currentValidator.getFrequency(PaperclickersScanner.ID_ANSWER_D),
										                  currentValidator.isValid(), 
										                  currentValidator.getLastDetectedScanCycle(PaperclickersScanner.ID_ANSWER_A), currentValidator.getLastDetectedScanCycle(PaperclickersScanner.ID_ANSWER_B), 
										                  currentValidator.getLastDetectedScanCycle(PaperclickersScanner.ID_ANSWER_C), currentValidator.getLastDetectedScanCycle(PaperclickersScanner.ID_ANSWER_D),											                  currentValidator.getNumberOfContinuousDetection(PaperclickersScanner.ID_ANSWER_A), currentValidator.getNumberOfContinuousDetection(PaperclickersScanner.ID_ANSWER_B), 
										                  currentValidator.getNumberOfContinuousDetection(PaperclickersScanner.ID_ANSWER_C), currentValidator.getNumberOfContinuousDetection(PaperclickersScanner.ID_ANSWER_D));
								
								if (MOVING_VALIDATION_THRESHOLD) {
									scanDebug += String.format("longestContinuous: %d(A), %d(B), %d(C), %d(D)", 
											                   currentValidator.getNumberOfLongestContinuousDetection(PaperclickersScanner.ID_ANSWER_A),
											                   currentValidator.getNumberOfLongestContinuousDetection(PaperclickersScanner.ID_ANSWER_B),
											                   currentValidator.getNumberOfLongestContinuousDetection(PaperclickersScanner.ID_ANSWER_C),
											                   currentValidator.getNumberOfLongestContinuousDetection(PaperclickersScanner.ID_ANSWER_D));
								}
							} else {
								scanDebug = String.format("Cycle: %d, Code: %d,  orientation: %f, frequency: %d(A), %d(B), %d(C), %d(D)",
                						                  mScanCycle, t.getCode(), t.getOrientation(), 
                						                  currentValidator.getFrequency(PaperclickersScanner.ID_ANSWER_A), currentValidator.getFrequency(PaperclickersScanner.ID_ANSWER_B),
                						                  currentValidator.getFrequency(PaperclickersScanner.ID_ANSWER_C), currentValidator.getFrequency(PaperclickersScanner.ID_ANSWER_D));							
							}
							
							log.d(TAG, scanDebug);
						}
					} else {
						// according to the predefined class size this is not a valid topcode; ignore it
						
						log.d(TAG, "onPreviewFrame - invalid topcode (" + t.getCode() + ") for defined class !");
					}
				}
				
				if (AVOID_PARTIAL_READINGS) {
					mHint1TextView.setText(mHint1StringStart + mValidTopcodesCount + mHint1StringEnd);
				} else {
					mHint1TextView.setText(mHint1StringStart + mRecognizedTopcodesCount + mHint1StringEnd);						
				}
									
				if (SHOW_CODE_FREQUENCY_DEBUG) {
					for (int i = 0; i < mFinalTopcodesValidator.size(); i++) {
						for (int j = 0; j < PaperclickersScanner.NUM_OF_VALID_ANSWERS; j++) {
							mFinalTopcodesFrequency[(i * PaperclickersScanner.NUM_OF_VALID_ANSWERS) + j] = mFinalTopcodesValidator.valueAt(i).getFrequency(j);							
						}
					}
					
					mFreqDebugTextView.setText(Arrays.toString(mFinalTopcodesFrequency));
				}
			}
			
			// Now that the validators list have already been updated, request drawing in the DrawView
			
			if (AVOID_PARTIAL_READINGS) {
				mDraw.updateValidTopcodesList(recognizedValidTopcodes);
			} else {
				mDraw.updateValidTopcodesList(topcodes);					
			}
			
			mDraw.postInvalidate();
		}
		
        
        if (mShowingValidation) {
          mDevelopmentData.setText(String.format("%s %d\n%s %d", mDevelopmentCurrentCycle, mScanCycle, mDevelopmentCurrentThreshold, TopcodeValidator.getCurrentValidationThrehshold()));
        }

        mEndOnPreviewTime = System.currentTimeMillis();
		
		log.d(TAG, String.format("Cycle: %d, overall onPreview time(ms): %d, fiducial time(ms): %d - candidates points found: %d", 
			  mScanCycle, mEndOnPreviewTime - mStartOnPreviewTime, mEndFiducialTime - mStartFiducialTime, scan.getCandidatesCount()));
		
		mScanCycle++;
		
		if (MOVING_VALIDATION_THRESHOLD) {
			TopcodeValidator.updateValidationThreshold(mScanCycle);
		}
	}

	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		log.d(TAG, "===> onResume()");
		
		hideStatusBar(); 

        if (AVOID_PARTIAL_READINGS) {
            mHint1TextView.setText(mHint1StringStart + mValidTopcodesCount + mHint1StringEnd);
        } else {
            mHint1TextView.setText(mHint1StringStart + mRecognizedTopcodesCount + mHint1StringEnd);                     
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
	}
	
	
	
	@Override
	protected void onStart() {
		super.onStart();
		
		log.d(TAG, "===> onStart()");
		
		mIgnoreCall = true;
		
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
				
				cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
				
				mCamera.setParameters(cameraParameters);				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
		
		
	
	private void setCameraPreview() {	
		
		if ((mCamera != null) && (mCameraPreview == null)) {
			
			mCameraPreview = new CameraPreview(this, mCamera, this);
			mPreview.addView(mCameraPreview);
			
			mDraw = new DrawView(mContext, mFinalTopcodesValidator, mImageWidth, mImageHeight, mShowingValidation);
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
				        	        mUserRequestedEnd = hasDetectedEnd(true);
				        	    }
				        	}, (long) SCAN_DIMISS_TIMEOUT);
				        	
				        	break;
				        	
				        case MotionEvent.ACTION_UP:
				        	
				        	mUserRequestedEnd = hasDetectedEnd(false);
				        	
				        	if (!mUserRequestedEnd) {
				        		mTouchStart = -1;
				        		
				        		mCameraPreview.performClick();
				        	}
				        	
				        	break;
				        	
				        case MotionEvent.ACTION_MOVE:
				        	
				        	mUserRequestedEnd = hasDetectedEnd(false);
					        	
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

	
	
    void stripLumaFromYUV420SP(int[] greyscale, byte[] yuv420sp, int width, int height) {  
        
        for (int i = 0; i < width * height; i++) {
            int y = (((int) yuv420sp[i]) & 0xff) - 16;  
            
            if (y < 0) { 
                y = 0;
            }
            
            greyscale[i] = y << 24;
        }
    }  	

    
    
	public void updateChangedCameraSize(int width, int height) {
			 
        log.d(TAG, String.format("New camera size: %d x %d. Null camera reference? %b", width, height, (mCamera == null))); 
         
        mImageWidth  = width;
        mImageHeight = height;		
        
        mLuma = new int[mImageWidth * mImageHeight];
        		
        if (mDraw != null) {
        	mDraw.updateScreenSize(mImageWidth, mImageHeight);
        }		
	}
	 
	 
	 
    void writePGMAfterThreshold(String filename, int[] threshold, int width, int height) {
    	
    	int maxVal = 236;
    	
		try {
			FileWriter fstream = new FileWriter(Environment.getExternalStorageDirectory() + "/PaperClickers/" + filename);
			
			BufferedWriter fout = new BufferedWriter(fstream);
			
			fout.write(String.format("P2\n%d\n%d\n%d\n", width, height, maxVal));
			
			for (int i = 0; i < width * height; i++) {
				int a = threshold[i] >>> 24 ;
		    	int r = maxVal;
		    	
		    	if (a == 0) {
		    		r = 0;
		    	}
		    	
				fout.write(String.format("%d ", r));
			}
			
			fout.close();
			
		} catch (IOException e) {
			Toast.makeText(getApplicationContext(), "Error saving PGM file.", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}			
    }
}
