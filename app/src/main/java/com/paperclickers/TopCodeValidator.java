/*
 * Paperclickers - Affordable solution for classroom response system.
 * 
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

package com.paperclickers;

import com.paperclickers.fiducial.PaperclickersScanner;
import com.paperclickers.fiducial.TopCode;

public class TopCodeValidator {
	
	final static String TAG = "TopCodeValidator";
	
	// Use this constant to enable the topcode validation using the most frequent answer criteria, in
	// Opposition to the last valid answer criteria
	final static boolean VALID_BY_FREQUENCY = AudienceResponses.AVOID_PARTIAL_READINGS & false;
	
	// Use this constant to enable the moving validation threshold, which is increased according to the
	// scan time
	public final static boolean MOVING_VALIDATION_THRESHOLD = AudienceResponses.AVOID_PARTIAL_READINGS & true;
		
	final static int INITIAL_VALIDATION_THRESHOLD = 3;
	final static int MAXIMUM_VALIDATION_THRESHOLD = 100;
	
	public final static int VALIDATION_THRESHOLD_INCREASE_STEP = 32;
	
	final static int INVALID_DUPLICATED_ANSWER = -1;
    
    // Response codes for "checkContinousDetection()" method
    public final static int TURNED_VALID        = -2;
	public final static int VALID_ALREADY       = -1;
	public final static int IGNORED_DUPLICATE   = 0;
	public final static int CONTINUOS_DETECTION = 1;
	public final static int MISSED_CYCLES       = 2;
	public final static int CHANGED_ANSWER      = 3;
    
    // Absolute number of cycles this topcode has been detected, by valid answer
	private int[] mFrequency;
	
	// Number of continuous detection the given answers has been detected for this topcode; resets every time
	// the topcode is not detected in the given cycle or the answer changes.
	// Used to defined the answer validation for this topcode, based on the continuous detection threshold.
	private int[] mNumberOfContinuousDetection;
	
	// Greatest number of continuous detection the given answer has been detected for this topcode;
	private int[] mLongestNumberOfContinuousDetection;
	
	// Last scan cycle in which this answer has been detected for the given topcode
	private int[] mLastDetectedScanCycle;
	
	// Indicates this answer is valid for this topcode
	private boolean[] mValidTopcode;
	
	private int mDuplicateAnswerInLastScanCycle;
	
	private int mPreviousTranslatedAnswer;
	
	static int mCurrentValidationThreshold;
	static float mCurrentValidationThresholdStep;
	
	private TopCode mHandledTopcode;

	
	
	public int checkContinousDetection(int currentScanCycle, int translatedAnswer, TopCode whichTopCode) {
		
		int result = CONTINUOS_DETECTION;

		mHandledTopcode = whichTopCode;
		
		if (AudienceResponses.AVOID_PARTIAL_READINGS) {
		    
            // Resets this duplicate indication for this scan cycle
            mDuplicateAnswerInLastScanCycle = PaperclickersScanner.ID_NO_ANSWER;
            		    
			if (!mValidTopcode[translatedAnswer]) {
			    
				if (mPreviousTranslatedAnswer != translatedAnswer) {
	
					// New orientation; restart validation process
	
				    // But first, check if the current code has already been detect in this same scan cycle - make
				    // detection robust against duplicated codes

				    if ((mPreviousTranslatedAnswer != PaperclickersScanner.ID_NO_ANSWER) && 
				        (mLastDetectedScanCycle[mPreviousTranslatedAnswer] == currentScanCycle)) {
				        
				        mNumberOfContinuousDetection[translatedAnswer] = INVALID_DUPLICATED_ANSWER;
				        
				        result = IGNORED_DUPLICATE;
				        
				        mDuplicateAnswerInLastScanCycle = translatedAnswer;
				        
	                    log.d(TAG, String.format("Duplicated code detected for not validated answer (cycle %d); answers: %d (1st - considered) to %d (2nd - discarded)", 
	                          currentScanCycle, mPreviousTranslatedAnswer, translatedAnswer));                    
				    } else {				    
				        result = CHANGED_ANSWER;

				        log.d(TAG, String.format("Code changed answer: %d to %d", mPreviousTranslatedAnswer, translatedAnswer));                    
				    }
					
				} else if (currentScanCycle == mLastDetectedScanCycle[translatedAnswer] + 1) {
				    
				    // this is a continuous detection for this answer on this topcode
				    
					if (++mNumberOfContinuousDetection[translatedAnswer] >= mCurrentValidationThreshold) {
						if (!isValid()) {
							result = TURNED_VALID;
						} else {
							result = VALID_ALREADY;
						}
						
						mValidTopcode[translatedAnswer] = true;					
					}
					
					if (MOVING_VALIDATION_THRESHOLD) {
						if (mLongestNumberOfContinuousDetection[translatedAnswer] < mNumberOfContinuousDetection[translatedAnswer]) {
							mLongestNumberOfContinuousDetection[translatedAnswer] = mNumberOfContinuousDetection[translatedAnswer];
						}
					}
				} else {
					
					// Missed some scan cycles; restart validation process
					
					result = MISSED_CYCLES;
					
					log.d(TAG, String.format("last scan with answer %d was: %d; current: %d", translatedAnswer, mLastDetectedScanCycle[translatedAnswer], currentScanCycle));
				}
				
				if (result > CONTINUOS_DETECTION) {
					mNumberOfContinuousDetection[translatedAnswer] = 0;				
				}
			} else {
			    
			    // This answer is already valid for this topcode; just update the continuous detection counters

			    // First, just check if this is not a duplicated topcode...
			    
                if ((mPreviousTranslatedAnswer != PaperclickersScanner.ID_NO_ANSWER) && 
                    (mLastDetectedScanCycle[mPreviousTranslatedAnswer] == currentScanCycle)) {
                    
                    mNumberOfContinuousDetection[translatedAnswer] = INVALID_DUPLICATED_ANSWER;
                    
                    result = IGNORED_DUPLICATE;
                    
                    mDuplicateAnswerInLastScanCycle = translatedAnswer;
                    
                    log.d(TAG, String.format("Duplicated code detected for validated answer (cycle %d); answers: %d (1st - considered) to %d (2nd - discarded)", 
                          currentScanCycle, mPreviousTranslatedAnswer, translatedAnswer));                    
                } else {
                    if (currentScanCycle == mLastDetectedScanCycle[translatedAnswer] + 1) {
                
    					++mNumberOfContinuousDetection[translatedAnswer];
    				} else {
    					mNumberOfContinuousDetection[translatedAnswer] = 0;	
    				}
    				
    				if (MOVING_VALIDATION_THRESHOLD) {
    					if (mLongestNumberOfContinuousDetection[translatedAnswer] < mNumberOfContinuousDetection[translatedAnswer]) {
    						mLongestNumberOfContinuousDetection[translatedAnswer] = mNumberOfContinuousDetection[translatedAnswer];
    					}
    				}			
    				
    				result = VALID_ALREADY;
                }
			}
		}
		
		if (result != IGNORED_DUPLICATE) {
    		mLastDetectedScanCycle[translatedAnswer] = currentScanCycle;
    		
    		mPreviousTranslatedAnswer = translatedAnswer;
		}
		
		return result;
	}
	
	
	
	public void forceValid(int whichAnswer) {
		mNumberOfContinuousDetection[whichAnswer] = MAXIMUM_VALIDATION_THRESHOLD;
		
		mValidTopcode[whichAnswer] = true;
	}
	
	
	
    public int getAnswerValidationCounter(int whichAnswer) {
        return mNumberOfContinuousDetection[whichAnswer];
    }
    
    

	public int getBestValidAnswer() {
		
		int bestAnswer = PaperclickersScanner.ID_NO_ANSWER;
		int bestAnswerFrequency = 0;
		int bestAnswerLastDetectedScanCycle = -1;
		
		if (AudienceResponses.AVOID_PARTIAL_READINGS) {
			for (int i = 0; i < PaperclickersScanner.NUM_OF_VALID_ANSWERS; i++) {
				if (mValidTopcode[i]) {
					if (VALID_BY_FREQUENCY) {
						if (mFrequency[i] >= bestAnswerFrequency) {
							bestAnswer = i;
							bestAnswerFrequency = mFrequency[i];
						}
					} else {
						if (mLastDetectedScanCycle[i] >= bestAnswerLastDetectedScanCycle) {
							bestAnswer = i;
							bestAnswerLastDetectedScanCycle = mLastDetectedScanCycle[i];
						}
					}
				}
			}
		} else {
			bestAnswer = mPreviousTranslatedAnswer;
		}

		return bestAnswer;
	}				
	
	
	
	public static int getCurrentValidationThrehshold() {
	    return mCurrentValidationThreshold;
	}
	
	
	
	public int getDuplicatedAnswerInLastScanCycle() {
	    return mDuplicateAnswerInLastScanCycle;
	}
	
	
	
    public int getFrequency(int whichAnswer) {
		return mFrequency[whichAnswer];
	}
	
	
	
	TopCode getHandledTopcode() {
		return mHandledTopcode;
	}
	
	
	
	public int getLastDetectedAnswer() {
	    return mPreviousTranslatedAnswer;
	}
	
	
	
	public int getLastDetectedScanCycle(int whichAnswer) {
		return mLastDetectedScanCycle[whichAnswer];
	}
	
	
	
	public int getNumberOfContinuousDetection(int whichAnswer) {
		return mNumberOfContinuousDetection[whichAnswer];
	}
	

	
	public int getNumberOfLongestContinuousDetection(int whichAnswer) {
		return mLongestNumberOfContinuousDetection[whichAnswer];
	}

	
	
	public void incFrequency(int whichAnswer) {
		mFrequency[whichAnswer]++;
	}


	
	public boolean isAnswerValid(int whichAnswer) {
	    return mValidTopcode[whichAnswer];
	}
	
	

	public boolean isValid() {
		
		boolean alreadyValidated = false;
		
		for (int i = 0; i < PaperclickersScanner.NUM_OF_VALID_ANSWERS; i++) {
			alreadyValidated |= mValidTopcode[i];
		}
		
		return alreadyValidated;
	}
	
	
	
	public TopCodeValidator(TopCode whichTopCode, float currentValidationThreshold) {
					
		mFrequency                   = new int[PaperclickersScanner.NUM_OF_VALID_ANSWERS];
		mNumberOfContinuousDetection = new int[PaperclickersScanner.NUM_OF_VALID_ANSWERS];
		mLastDetectedScanCycle       = new int[PaperclickersScanner.NUM_OF_VALID_ANSWERS];
		mValidTopcode                = new boolean[PaperclickersScanner.NUM_OF_VALID_ANSWERS];
		
		if (MOVING_VALIDATION_THRESHOLD) {
			mLongestNumberOfContinuousDetection = new int[PaperclickersScanner.NUM_OF_VALID_ANSWERS];;
		}		
		
		for (int i = 0; i < PaperclickersScanner.NUM_OF_VALID_ANSWERS; i++) {
			mFrequency[i]                   = 0;
			mNumberOfContinuousDetection[i] = 0;
			mLastDetectedScanCycle[i]       = -1;
			mValidTopcode[i]                = false;
			
			if (MOVING_VALIDATION_THRESHOLD) {
				mLongestNumberOfContinuousDetection[i] = 0;
			}
		}
		
		mPreviousTranslatedAnswer   = PaperclickersScanner.ID_NO_ANSWER;
		mHandledTopcode             = whichTopCode;
		mCurrentValidationThreshold = INITIAL_VALIDATION_THRESHOLD;
		
		mDuplicateAnswerInLastScanCycle = PaperclickersScanner.ID_NO_ANSWER;

		mCurrentValidationThresholdStep = currentValidationThreshold;
	}

	
	
	static public void updateValidationThreshold(int cycleCount) {
		
		int newValidationThreshold = Math.max(INITIAL_VALIDATION_THRESHOLD,
		                                      (int) Math.floor((float) INITIAL_VALIDATION_THRESHOLD / (float) mCurrentValidationThresholdStep * cycleCount));
		
		log.d(TAG, String.format("newValidationThreshold: %d; mCurrentValidationThreshold: %d", newValidationThreshold, mCurrentValidationThreshold));
		
		if (newValidationThreshold > mCurrentValidationThreshold) {
			mCurrentValidationThreshold = newValidationThreshold;
		}
	}
}
