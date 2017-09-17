/*
 * Paperclickers - Affordable solution for classroom response system.
 *
 * Copyright (C) 2015-2017 Eduardo Valle Jr <dovalle@dca.fee.unicamp.br>
 * Copyright (C) 2015-2017 Eduardo Seiti de Oliveira <eduseiti@dca.fee.unicamp.br>
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.paperclickers.Analytics;
import com.paperclickers.AudienceResponses;
import com.paperclickers.R;
import com.paperclickers.SettingsActivity;
import com.paperclickers.TopCodeValidator;
import com.paperclickers.camera.OrientationManager.ScreenOrientation;
import com.paperclickers.fiducial.TopCode;
import com.paperclickers.log;
import com.paperclickers.result.GridViewActivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This is the base class for the camera Activity implementations - real camera hardware ("CameraMain" class)
 * and camera emulation via MediaPlayer ("CameraEmulator" class)
 *
 * @author eduseiti, on 10/06/2017
 */

public class CameraAbstraction extends Activity implements OrientationManager.OrientationListener {


    final static String TAG = "paperclickers.CameraAbstraction";

    final static int SCAN_DISMISS_TIMEOUT = 1000;

    protected DrawView mDraw;
    protected FrameLayout mPreview;

    protected Vibrator mVibrator;
    protected long mTouchStart = -1;

    protected int mImageWidth  = 0;
    protected int mImageHeight = 0;

    protected boolean mUserRequestedEnd = false;

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

    protected boolean mShowingValidation = false;
    boolean mTerminating = false;

    protected Timer mScanDismissTimer = null;

    boolean mHasRotated = false;

    OrientationManager mOrientationManager = null;

    AudienceResponses mAudienceResponses = null;

    Analytics mAnalytics = null;

    SharedPreferences mSharedPreferences = null;


    protected class TouchListener implements View.OnTouchListener {

        View mViewToClick = null;



        public TouchListener(View whichView) {
            mViewToClick = whichView;
        }



        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:

                    mTouchStart = System.currentTimeMillis();

                    mScanDismissTimer = new Timer();

                    mScanDismissTimer.schedule(new TimerTask() {
                        public void run() {

                            if (!isFinishing()) {
                                hasDetectedEnd(true);
                            }
                        }
                    }, (long) SCAN_DISMISS_TIMEOUT);

                    break;

                case MotionEvent.ACTION_UP:

                    hasDetectedEnd(false);

                    if (!mUserRequestedEnd) {
                        mTouchStart = -1;

                        mViewToClick.performClick();
                    }

                    break;

                case MotionEvent.ACTION_MOVE:

                    hasDetectedEnd(false);

                    break;
                case MotionEvent.ACTION_CANCEL:
                default:

                    if (mScanDismissTimer != null) {
                        mScanDismissTimer.cancel();
                        mScanDismissTimer = null;
                    }

                    break;
            }

            return mUserRequestedEnd;
        }
    }



    private void callNextActivity() {

        log.d(TAG,">>>>> callNextActivity");

        int topCodesFound = 0;

        if (AudienceResponses.AVOID_PARTIAL_READINGS) {
            topCodesFound = mAudienceResponses.getValidTopCodesCount();
        } else {
            topCodesFound = mAudienceResponses.getRecognizedTopCodesCount();
        }

        Intent i = new Intent(getApplicationContext(), GridViewActivity.class);

        i.putExtra("detectedAnswers", mAudienceResponses.returnDetectedTopCodesList());

        mEndScanTime = System.currentTimeMillis();


        mAnalytics.send_scanCycle(mEndScanTime - mStartScanTime, topCodesFound, mAudienceResponses.getPreviouslyDetectedTopCodesCount(), mAudienceResponses.getTotalTopCodesCount());


        log.d(TAG, String.format("Total scan cycles: %d, total scan time (ms): %d", mAudienceResponses.getScanCycleCount(), mEndScanTime - mStartScanTime));


        mAudienceResponses.finalize();

        finish();

        startActivity(i);
    }



    private void hasDetectedEnd(boolean timeoutOccurred) {

        if ((mTouchStart != -1) && (!mTerminating)) {
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

        if (mScanDismissTimer != null) {
            mScanDismissTimer.cancel();
            mScanDismissTimer = null;
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
    public void onBackPressed() {
        super.onBackPressed();

        mTerminating = true;
    }



    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);

        log.d(TAG, "onConfigurationChanged");
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mTerminating = false;

        mContext = getApplicationContext();
        mVibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

        hideStatusBar();

        mHint1StringStart = (String) getResources().getText(R.string.hint1Start);
        mHint1StringEnd   = (String) getResources().getText(R.string.hint1End);

        mHint1TextView     = (TextView) findViewById(R.id.txt_hint1);
        mFreqDebugTextView = (TextView) findViewById(R.id.txt_freqDebug);

        mDevelopmentData = (TextView) findViewById(R.id.txt_development_data);

        mDevelopmentCurrentCycle = (String) getResources().getText(R.string.development_current_cycle);
        mDevelopmentCurrentThreshold = (String) getResources().getText(R.string.development_current_threshold);

        if (!AudienceResponses.SHOW_CODE_FREQUENCY_DEBUG) {
            mFreqDebugTextView.setVisibility(View.GONE);
        }

        mPreview = (FrameLayout) findViewById(R.id.camera_preview);

        mAudienceResponses = new AudienceResponses(mContext, PreferenceManager.getDefaultSharedPreferences(this));

        Intent whichIntent = getIntent();

        String intentAction = whichIntent.getAction();

        Serializable receivedTopcodes = null;

        if ((intentAction != null) && (intentAction.equals(AudienceResponses.RECALL_CODES_INTENT))) {

            receivedTopcodes = whichIntent.getSerializableExtra("detectedAnswers");

        }

        mAudienceResponses.initialize(receivedTopcodes);

        mUserRequestedEnd = false;

        mAnalytics = new Analytics(getApplicationContext());

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
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

                log.d(TAG, "Orientation changed to PORTRAIT");

                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;

            case LANDSCAPE:
            case REVERSED_LANDSCAPE:

                log.d(TAG, "Orientation changed to LANDSCAPE");

                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
        }
    }



    @Override
    protected void onPause() {
        super.onPause();

        log.d(TAG, "===> onPause()");

        mOrientationManager.disable();

        if (mScanDismissTimer != null) {
            mScanDismissTimer.cancel();
            mScanDismissTimer = null;
        }
    }



    public void onNewFrame(byte[] data) {

        List<TopCode> recognizedValidTopCodes = new ArrayList<TopCode>();
        List<TopCode> topCodes = null;

        int cycleResult = mAudienceResponses.onNewFrame(data, mHasRotated, recognizedValidTopCodes, topCodes, mShowingValidation);

        processNewFrameResult(cycleResult, recognizedValidTopCodes, topCodes);
    }



    public void onNewFrame(int[] data) {

        List<TopCode> recognizedValidTopCodes = new ArrayList<TopCode>();
        List<TopCode> topCodes = null;

        int cycleResult = mAudienceResponses.onNewFrame(data, mHasRotated, recognizedValidTopCodes, topCodes, mShowingValidation);

        processNewFrameResult(cycleResult, recognizedValidTopCodes, topCodes);
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
            if (SettingsActivity.isDevelopmentMode()) {
                String showValidation = mSharedPreferences.getString("development_show_validation", "1");

                mShowingValidation = showValidation.equals("1");
            } else {
                mShowingValidation = false;
            }

            if (mShowingValidation) {
                mDevelopmentData.setVisibility(View.VISIBLE);
            } else {
                mDevelopmentData.setVisibility(View.GONE);
            }
        }

        mOrientationManager = new OrientationManager(mContext, SensorManager.SENSOR_DELAY_NORMAL, this, getWindowManager().getDefaultDisplay().getRotation());
        mOrientationManager.enable();
    }



    @Override
    protected void onStart() {

        super.onStart();

        log.d(TAG, "===> onStart()");
    }



    @Override
    protected void onStop() {

        super.onStop();

        log.d(TAG, "===> onStop()");

        if (mScanDismissTimer != null) {
            mScanDismissTimer.cancel();
            mScanDismissTimer = null;
        }
    }



    public void processNewFrameResult(int cycleResult, List<TopCode> recognizedValidTopCodes, List<TopCode> topCodes) {

        if (cycleResult == AudienceResponses.COMPLETELY_IGNORE_CYCLE) {
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
            if (AudienceResponses.SAVE_LAST_IMAGE || SettingsActivity.isDevelopmentMode()) {
                mAudienceResponses.saveLastImage();
            }

            callNextActivity();
        }
    }



    protected void releaseTopCodesFeedbackPreview() {

        mPreview.removeView(mDraw);
    }



    protected void setTopCodesFeedbackPreview(boolean forceSize) {

        mDraw = new DrawView(mContext, mAudienceResponses.getTopCodesValidator(), mImageWidth, mImageHeight, mShowingValidation);

        mPreview.addView(mDraw);

        if (forceSize) {
            ViewGroup.LayoutParams layoutParams = mDraw.getLayoutParams();

            layoutParams.width = mImageWidth;
            layoutParams.height = mImageHeight;

            mDraw.setLayoutParams(layoutParams);
        }
    }
}

