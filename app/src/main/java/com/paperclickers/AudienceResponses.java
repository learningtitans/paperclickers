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


package com.paperclickers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.SparseArray;

import com.paperclickers.fiducial.PaperclickersScanner;
import com.paperclickers.fiducial.TopCode;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;


/**
 * Implements the audience responses core processing, regardless of the image frames provider.
 *
 * @author eduseiti on 05/06/17.
 */

public class AudienceResponses {


    // Intent used to restart the TopCodes capturing process passing back a list of previously validated TopCodes
    public static String RECALL_CODES_INTENT = "com.paperclickers.intent.action.RECALL_CODES";

    final static String TAG = "paperclickers.AudienceResponses";

    // Use this constant to enable saving the last analyzed image, right after user requesting to
    // carry on to the Grid View
    public final static boolean SAVE_LAST_IMAGE = true;

    // Use this constant to enable showing the class topcodes detection raw (regardless validation)
    // frequencies as an overlay in camera capture
    public final static boolean SHOW_CODE_FREQUENCY_DEBUG = false;

    // Use this constant to enable the overall topcodes validation mechanism
    public final static boolean AVOID_PARTIAL_READINGS = true;

    // Use this constant to enable the detailed debug log, showing every topcodes detection and validation
    // data
    final static boolean DEBUG_DETECTION_CYCLE_RAW_DATA = false;

    // Defined by TopcodeValidator constant: refers to the moving validation threshold mechanism
    final static boolean MOVING_VALIDATION_THRESHOLD = TopCodeValidator.MOVING_VALIDATION_THRESHOLD;

    // Use this constant to enable the frame drop
    final static boolean DROP_EVERY_OTHER_FRAME = true;

    // Use this constant to enable previewing only validated codes as an overlay in the camera capture
    final static boolean ONLY_PREVIEW_VALIDATED_CODES = true;


    public final static int COMPLETLY_IGNORE_CYCLE = -1;
    public final static int DO_NOT_REDRAW = 0;
    public final static int NEED_TO_REDRAW = 1;


    private int[] mLuma = null;
    private int mImageWidth;
    private int mImageHeight;

    private int mRecognizedTopCodesCount;
    private int mValidTopCodesCount;

    private SharedPreferences mSharedPreferences = null;

    long mStartOnPreviewTime;
    long mEndOnPreviewTime;
    long mStartFiducialTime;
    long mEndFiducialTime;

    int mScanCycle;

    Context mContext;

    private boolean mIgnoreCall;
    private boolean mShowingValidation = false;

    private SparseArray<TopCode> mFinalTopCodes;
    private SparseArray<TopCodeValidator> mFinalTopCodesValidator;
    private Integer[] mFinalTopCodesFrequency;

    PaperclickersScanner mScan = null;

    int mPreviouslyDetectedTopCodesCount = 0;



    private class SaveLastProcessedFrame extends AsyncTask<Void, Void, Void> {

        String mFilename;
        int[] mThresholdData;
        int mWidth;
        int mHeight;



        public SaveLastProcessedFrame(String filename, int[] threshold, int width, int height) {

            super();

            mFilename      = filename;
            mThresholdData = threshold.clone();
            mWidth         = width;
            mHeight        = height;
        }



        @Override
        protected Void doInBackground(Void... params) {

            writePGMAfterThreshold (mFilename, mThresholdData, mWidth, mHeight);

            return null;
        }



        private void writePGMAfterThreshold(String filename, int[] threshold, int width, int height) {

            long startTime = System.currentTimeMillis();

            log.d(TAG, "Saving last processed image frame");

            int maxVal = 236;

            try {
                FileWriter fstream = new FileWriter(Environment.getExternalStorageDirectory() + "/PaperClickers/" + filename);

                BufferedWriter fout = new BufferedWriter(fstream);

                if (width > height) {
                    fout.write(String.format("P2\n%d\n%d\n%d\n", width, height, maxVal));
                } else {
                    fout.write(String.format("P2\n%d\n%d\n%d\n", height, width, maxVal));
                }

                log.d(TAG,String.format("Writing file - size: %d x %d", width, height));

                int total = 0;

                for (int i = 0; i < width * height; i++) {
                    int a = threshold[i] >>> 24;
                    int r = maxVal;

                    if (a == 0) {
                        r = 0;
                    }

                    fout.write(String.format("%d ", r));

                    total++;
                }

                fout.close();

                log.d(TAG, String.format("Total %d (%d) - time: %d", total, width * height, System.currentTimeMillis() - startTime));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    public AudienceResponses(Context context, SharedPreferences sharedPreferences) {

        mContext = context;
        mSharedPreferences = sharedPreferences;

        mIgnoreCall = true;
    }



    public Integer[] getFinalTopCodesFrequency() {
        return mFinalTopCodesFrequency;
    }



    public int getPreviouslyDetectedTopCodesCount() {
        return mPreviouslyDetectedTopCodesCount;
    }



    public int getRecognizedTopCodesCount() {
        return mRecognizedTopCodesCount;
    }



    public int getScanCycleCount() {
        return mScanCycle;
    }



    public SparseArray<TopCodeValidator> getTopCodesValidator() {
        return mFinalTopCodesValidator;
    }



    public int getTotalTopCodesCount() {
        return mFinalTopCodes.size();
    }



    public int getValidTopCodesCount() {
        return mValidTopCodesCount;
    }



    public void initialize(Serializable receivedTopcodes) {

        log.d(TAG, ">>>>> initialize()");

        initializeTopCodesList((HashMap<Integer, String>) receivedTopcodes);
    }



    public void initializeTopCodesList(HashMap<Integer, String> receivedTopcodes) {

        String studentsStr = mSharedPreferences.getString("students_number", "40");
        int studentsNum    = Integer.parseInt(studentsStr);

        mFinalTopCodes          = new SparseArray<TopCode>(studentsNum);
        mFinalTopCodesValidator = new SparseArray<TopCodeValidator>(studentsNum);

        if (SHOW_CODE_FREQUENCY_DEBUG) {
            mFinalTopCodesFrequency = new Integer[studentsNum * PaperclickersScanner.NUM_OF_VALID_ANSWERS];
        }

        mRecognizedTopCodesCount = 0;
        mValidTopCodesCount      = 0;
        mScanCycle               = 0;

        mPreviouslyDetectedTopCodesCount = 0;

        log.d(TAG, "initializeTopCodesList - students number: " + studentsNum);

        for (int i = 0; i < studentsNum; i++) {

            TopCode newTopCode = new TopCode(SettingsActivity.validTopCodes[i]);

            newTopCode.setOrientation(PaperclickersScanner.INVALID_TOPCODE_ORIENTATION);

            mFinalTopCodes.put(SettingsActivity.validTopCodes[i], newTopCode);

            float currentValidationStep = (float) TopCodeValidator.VALIDATION_THRESHOLD_INCREASE_STEP;

            if (SettingsActivity.DEVELOPMENT_OPTIONS) {
                String validationThresholdStr = mSharedPreferences.getString("validation_threshold", String.valueOf(TopCodeValidator.VALIDATION_THRESHOLD_INCREASE_STEP));
                currentValidationStep         = (float) Integer.parseInt(validationThresholdStr);
            }

            TopCodeValidator validator = new TopCodeValidator(newTopCode, currentValidationStep);
            mFinalTopCodesValidator.put(SettingsActivity.validTopCodes[i], validator);

            log.d(TAG, "> Adding topcode (" + SettingsActivity.validTopCodes[i] + ") to the valid list. Total of: " + mFinalTopCodes.size());

            if (receivedTopcodes != null && receivedTopcodes.size() != 0) {
                String answer = receivedTopcodes.get(i + 1);

                log.d(TAG, "initializeTopCodesList - code: " + SettingsActivity.validTopCodes[i] + " orientation: " + answer);

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
                        mRecognizedTopCodesCount++;
                        mValidTopCodesCount++;

                        validator.forceValid(answerID);
                    }

                    mPreviouslyDetectedTopCodesCount++;
                }
            }
        }
    }



    public void finalize() {

        if (mScan != null) {
            mScan.finalize();

            mScan = null;
        }
    }



    public int onNewFrame(byte[] data, boolean hasRotated, List<TopCode> recognizedValidTopCodes, List<TopCode> topCodes) {

        int result = DO_NOT_REDRAW;

        // Check if should drop this frame
        if (DROP_EVERY_OTHER_FRAME) {
            mIgnoreCall = !mIgnoreCall;

            if (mIgnoreCall) {
                return COMPLETLY_IGNORE_CYCLE;
            }
        }

        if (mLuma == null) {
            // Still initializing; ignore call...

            return COMPLETLY_IGNORE_CYCLE;
        }

        mStartOnPreviewTime = System.currentTimeMillis();

        stripLumaFromYUV420SP(mLuma, data, mImageWidth, mImageHeight);

        result = processNewFrame(hasRotated, recognizedValidTopCodes, topCodes);

        return result;
    }



    public int onNewFrame(int[] data, boolean hasRotated, List<TopCode> recognizedValidTopCodes, List<TopCode> topCodes) {

        int result = DO_NOT_REDRAW;

        // Check if should drop this frame
        if (DROP_EVERY_OTHER_FRAME) {
            mIgnoreCall = !mIgnoreCall;

            if (mIgnoreCall) {
                return COMPLETLY_IGNORE_CYCLE;
            }
        }

        if (mLuma == null) {
            // Still initializing; ignore call...

            return COMPLETLY_IGNORE_CYCLE;
        }

        mStartOnPreviewTime = System.currentTimeMillis();

        stripLumaFromYUV420SP(mLuma, data, mImageWidth, mImageHeight);

        result = processNewFrame(hasRotated, recognizedValidTopCodes, topCodes);

        return result;
    }



    public int processNewFrame(boolean hasRotated, List<TopCode> recognizedValidTopCodes, List<TopCode> topCodes) {

        int result = DO_NOT_REDRAW;

        try {
            mStartFiducialTime = System.currentTimeMillis();

            topCodes = mScan.scanProcessing(mLuma, hasRotated);

            mEndFiducialTime = System.currentTimeMillis();
        } catch (Resources.NotFoundException e1) {
            log.e(TAG, e1.toString());
        }

        if (topCodes != null) {
            if (topCodes.size() > 0) {

                // Update the existing topcodes list with the information found, either including new ones or
                // updating the orientation of the existing ones.

                for (TopCode t : topCodes) {

                    TopCode validTopCode = mFinalTopCodes.get(t.getCode());

                    String scanDebug;

                    if (validTopCode != null) {
                        TopCodeValidator currentValidator = mFinalTopCodesValidator.get(t.getCode());

                        int translatedAnswer = PaperclickersScanner.translateOrientationToID(t.getOrientation());
                        int continuousDetectionResult;

                        currentValidator.incFrequency(translatedAnswer);

                        if (AVOID_PARTIAL_READINGS) {
                            continuousDetectionResult = currentValidator.checkContinousDetection(mScanCycle, translatedAnswer, validTopCode);

                            if (continuousDetectionResult == TopCodeValidator.CHANGED_ANSWER) {
                                log.d(TAG, String.format("Code %d changed orientation: %f to %f", t.getCode(), validTopCode.getOrientation(), t.getOrientation()));

                                if (!ONLY_PREVIEW_VALIDATED_CODES || mShowingValidation) {
                                    recognizedValidTopCodes.add(t);
                                }
                            } else if (continuousDetectionResult == TopCodeValidator.TURNED_VALID ||
                                    continuousDetectionResult == TopCodeValidator.VALID_ALREADY) {

                                if (continuousDetectionResult == TopCodeValidator.TURNED_VALID) {

                                    mValidTopCodesCount++;
                                }

                                recognizedValidTopCodes.add(t);
                            } else if (!ONLY_PREVIEW_VALIDATED_CODES || mShowingValidation) {
                                recognizedValidTopCodes.add(t);
                            }
                        } else {
                            currentValidator.checkContinousDetection(mScanCycle, translatedAnswer, validTopCode);

                            mValidTopCodesCount++;
                        }

                        if (!PaperclickersScanner.isValidOrientation(validTopCode.getOrientation())) {
                            mRecognizedTopCodesCount++;
                        }

                        // Update topcode with the information recognized in this scan cycle
                        validTopCode.setDiameter(t.getDiameter());
                        validTopCode.setLocation(t.getCenterX(), t.getCenterY());
                        validTopCode.setOrientation(t.getOrientation());

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

                if (SHOW_CODE_FREQUENCY_DEBUG) {
                    for (int i = 0; i < mFinalTopCodesValidator.size(); i++) {
                        for (int j = 0; j < PaperclickersScanner.NUM_OF_VALID_ANSWERS; j++) {
                            mFinalTopCodesFrequency[(i * PaperclickersScanner.NUM_OF_VALID_ANSWERS) + j] = mFinalTopCodesValidator.valueAt(i).getFrequency(j);
                        }
                    }
                }
            }

            // Now that the validators list have already been updated, indicate the need of redrawing the markers

            result = NEED_TO_REDRAW;
        }


        mEndOnPreviewTime = System.currentTimeMillis();

        log.d(TAG, String.format("Cycle: %d, overall onPreview time(ms): %d, fiducial time(ms): %d - candidates points found: %d",
                mScanCycle, mEndOnPreviewTime - mStartOnPreviewTime, mEndFiducialTime - mStartFiducialTime, mScan.getCandidatesCount()));

        mScanCycle++;

        if (MOVING_VALIDATION_THRESHOLD) {
            TopCodeValidator.updateValidationThreshold(mScanCycle);
        }

        return result;
    }



    public HashMap<Integer, String> returnDetectedTopCodesList() {


        log.d(TAG,">>>>> returnDetectedTopcodesList");

        HashMap<Integer, String> detectedTopcodes = new HashMap<Integer, String>();

        if (mFinalTopCodes != null) {


            log.d(TAG,String.format("mFinalTopcodes.size: %d", mFinalTopCodes.size()));


            // Build a hashmap containing only code and detected answer of each topcode

            for (int i = 0; i < mFinalTopCodes.size(); i++) {

                TopCode t = mFinalTopCodes.valueAt(i);

                TopCodeValidator validator = mFinalTopCodesValidator.valueAt(i);

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
        } else {
            log.d(TAG, "Empty class!!! Shouldn't have happen; application shared preferences might have been tampered!!!");
        }

        return detectedTopcodes;
    }



    public void saveLastImage() {
        SaveLastProcessedFrame saveImage = new SaveLastProcessedFrame("lastfile.pgm", mLuma, mImageWidth, mImageHeight);

        saveImage.execute();
    }



    public void setImageSize(int newWidth, int newHeight) {

        mImageWidth = newWidth;
        mImageHeight = newHeight;

        mLuma = new int[mImageWidth * mImageHeight];

        mScan = new PaperclickersScanner(mImageWidth, mImageHeight, mContext);
    }



    private void stripLumaFromYUV420SP(int[] greyscale, byte[] yuv420sp, int width, int height) {

        for (int i = 0; i < width * height; i++) {
            int y = (((int) yuv420sp[i]) & 0xff) - 16;

            if (y < 0) {
                y = 0;
            }

            greyscale[i] = y << 24;
        }
    }



    private void stripLumaFromYUV420SP(int[] greyscale, int[] yuv420sp, int width, int height) {

        for (int i = 0; i < width * height; i++) {
            int y = (yuv420sp[i] & 0xff) - 16;

            if (y < 0) {
                y = 0;
            }

            greyscale[i] = y << 24;
        }
    }
}
