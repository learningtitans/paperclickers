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


package com.paperclickers.fiducial;

import java.util.List;
import com.paperclickers.log;
import android.graphics.Color;

/**
 * 
 * @author Eduardo Seiti de Oliveira
 * 
 * Scanner class specialization to allow providing ARGB buffer for the topcodes
 * scanning process.
 *
 */

public class PaperclickersScanner extends Scanner {

	final static String TAG = "paperclickers.scanner";
	
	// Use this constant to enable logging the number of candidates found
	public static final boolean CANDIDATES_LOG = true;
    
    // Use this constant to enable logging the main methods execution time
    public static final boolean LOG_EXECUTION_TIMES = true;
    
	// Use this constant to enable testing vertically the image for topcode candidates
	public static final boolean TEST_VERTICAL_CANDIDATES = true;
	
	public static final float INVALID_TOPCODE_ORIENTATION = Float.NaN;
	
	public static final float SAME_ANSWER_THRESHOLD = 1.0f;
	
	public static final float ANSWER_A_VALID_ORIENTATION = 0;
	public static final float ANSWER_D_VALID_ORIENTATION = (float) (Math.PI / 2.0f);
	public static final float ANSWER_C_VALID_ORIENTATION = (float) (Math.PI);
	public static final float ANSWER_B_VALID_ORIENTATION = (float) (Math.PI * 3.0f / 2.0f);
	
	public static final String ANSWER_A = "A";
	public static final String ANSWER_B = "B";
	public static final String ANSWER_C = "C";
	public static final String ANSWER_D = "D";
	public static final String NO_ANSWER_STRING = "";
	
	private static final String answerIDToString[] = {ANSWER_A, ANSWER_B, ANSWER_C, ANSWER_D, NO_ANSWER_STRING};
	
	public static final int NUMBER_OF_VALID_ANSWERS = answerIDToString.length;
	
	public static final int COLOR_A = Color.parseColor("#E63E2B");
	public static final int COLOR_B = Color.parseColor("#74C353");
	public static final int COLOR_C = Color.parseColor("#005485");
	public static final int COLOR_D = Color.parseColor("#8EDDDF");

	public static final int ID_ANSWER_A  = 0;
	public static final int ID_ANSWER_B  = 1;
	public static final int ID_ANSWER_C  = 2;
	public static final int ID_ANSWER_D  = 3;
	
	public static final int NUM_OF_VALID_ANSWERS = 4;
	
	public static final int ID_NO_ANSWER = NUM_OF_VALID_ANSWERS;	
	
	long mStartThresholdTime;
	long mEndThresholdTime;
	long mStartFindCodesTime;
	long mEndFindCodesTime;

	
	
	/**
	 * Scan the image line by line looking for marked topcodes
	 * candidates
	 */
	protected List<TopCode> findCodes(boolean hasRotated) {
		
		int effectiveCandidatesCount = 0;
		
		this.tcount = 0;
		List<TopCode> spots = new java.util.ArrayList<TopCode>();

		TopCode spot = new TopCode();
		
		int k = w * 2;
		for (int j = 2; j < h - 2; j++) {
			for (int i = 0; i < w; i++) {

				final int CANDIDATE_MASK;
				
				if (TEST_VERTICAL_CANDIDATES) {
					CANDIDATE_MASK = 0x6000000;
				} else {
					CANDIDATE_MASK = 0x2000000;					
				}
				
				if ((data[k] & CANDIDATE_MASK) == CANDIDATE_MASK) {
					if ((data[k - 1] & CANDIDATE_MASK) == CANDIDATE_MASK &&
						(data[k + 1] & CANDIDATE_MASK) == CANDIDATE_MASK &&
						(data[k - w] & CANDIDATE_MASK) == CANDIDATE_MASK &&
						(data[k + w] & CANDIDATE_MASK) == CANDIDATE_MASK) {
						
						effectiveCandidatesCount++;
						
						if (overlaps(spots, i, j) == null) {
		                     this.tcount++;
		                     spot.decode(this, i, j);
		                     
		                     if (spot.isValid()) {
		                    	 
		                         if (hasRotated) {
		                             spot.setLocation(h - spot.getCenterY(), spot.getCenterX());
		                             
		                             log.d(TAG, String.format(">>> Previous orientation: %f", spot.getOrientation()));
		                             
		                             float newOrientation = (float) (-spot.getOrientation() - Math.PI / 2.0f);
		                             
		                             spot.setOrientation((float) (newOrientation < 0 ? -(2.0f * Math.PI + newOrientation) : -newOrientation));
		                             
                                     log.d(TAG, String.format(">>> New orientation: %f", spot.getOrientation()));
		                         }
		                         
		                    	 // Make sure there is only one instance of a given topcode in the list
		                    	 
		                    	 int existingIndex = spots.indexOf(spot);
		                    	 
		                    	 if (existingIndex != -1) {
		                    		 spots.set(existingIndex, spot);
		                    	 } else {
		                    		 spots.add(spot);
		                    	 }
		                    	 
		                         spot = new TopCode();
		                     }
						}
					}
				}
				k++;
			}
		}

		if (TEST_VERTICAL_CANDIDATES) {
			ccount = effectiveCandidatesCount;
		}
		
		if (CANDIDATES_LOG) {
			log.d(TAG,"findCodes. Effective candidates count: " + effectiveCandidatesCount);
		}
		
		return spots;
	}
	
	
	
	public int getCandidatesCount() {
		return ccount;
	}
	
	
	
	public static boolean isValidOrientation(Float orientation) {
		return !Float.isNaN(orientation);
	}
	
	
	
	public PaperclickersScanner() {
		super();
	}
	
	
	
    /**
     * New scan method to allow directly sending lumma buffer instead of a Bitmap
     * 
     * @param image
     * @param width
     * @param height
     * @return
     */
	public List<TopCode> scan(int[] image, int width, int height, boolean hasRotated) {
	    
		this.w    = width;
		this.h    = height;
		this.data = image;
	
		
		if (LOG_EXECUTION_TIMES) {
			mStartThresholdTime = System.currentTimeMillis();
		}		
			
		threshold(); // run the adaptive threshold filter

		if (LOG_EXECUTION_TIMES) {
			mEndThresholdTime = System.currentTimeMillis();
			
			mStartFindCodesTime = System.currentTimeMillis();
		}
		
		// scan for topcodes
		List<TopCode> codesFound = findCodes(hasRotated);

		if (LOG_EXECUTION_TIMES) {
			mEndFindCodesTime = System.currentTimeMillis();
			
			log.d(TAG, String.format("Threshold execution time(ms): %d, FindCodes execution time(ms): %d", mEndThresholdTime - mStartThresholdTime, 
					                 mEndFindCodesTime - mStartFindCodesTime));
		}
		
		return codesFound; 
	}
		
	
	
	/**
	 * Vertically look for topcades candidates in the image
	 */
	protected int scanCandidatesVertical() {
		
		int a, b1, w1, b2, level, dk;
		int k;
		
		int candidates = 0;
		
		for (int j = 0; j < w; j++) {
			
			level = b1 = b2 = w1 = 0;
			
			for (int i = 0; i < h; i++) {
				
				k = j + (i * w);
				
				a = data[k] & 0x01000000;
				
				switch (level) {

				// On a white region. No black pixels yet
				case 0:
					if (a == 0) { // First black encountered
						level = 1;
						b1 = 1;
						w1 = 0;
						b2 = 0;
					}
					break;

				// On first black region
				case 1:
					if (a == 0) {
						b1++;
					} else {
						level = 2;
						w1 = 1;
					}
					break;

				// On second white region (bulls-eye of a code?)
				case 2:
					if (a == 0) {
						level = 3;
						b2 = 1;
					} else {
						w1++;
					}
					break;

				// On second black region
				case 3:
					if (a == 0) {
						b2++;
					}
					// This could be a top code
					else {
						int mask;
						
						if (b1 >= 2	&& b2 >= 2 // otherwise less than 2 pixels... not interested
							&& (b1 <= maxu && b2 <= maxu && w1 <= (maxu + maxu))
							&& (Math.abs(b1 + b2 - w1) <= (b1 + b2))
							&& (Math.abs(b1 + b2 - w1) <= w1)
							&& (Math.abs(b1 - b2) <= b1)
							&& (Math.abs(b1 - b2) <= b2)) {
							
							mask = 0x4000000;

							dk = 1 + b2 + (w1 / 2);
							dk = k - (dk * w);

							data[dk - w] |= mask;
							data[dk]     |= mask;
							data[dk + w] |= mask;
							
							candidates += 3; // count candidate codes
						}
						
						b1 = b2;
						w1 = 1;
						b2 = 0;
						
						level = 2;
					}
					break;
				}
			}
		}
		
		return candidates;
	}
	
	
	
	protected void threshold() {

		int a;
		int threshold, sum = 128;
		int s = 30;
		int k;
		int b1, w1, b2, level, dk;
		
		double f = 0.975;

		this.ccount = 0;

		for (int j = 0; j < h; j++) {
			level = b1 = b2 = w1 = 0;

			// ----------------------------------------
			// Process rows back and forth (alternating
			// left-to-right, right-to-left)
			// ----------------------------------------
			k = (j % 2 == 0) ? 0 : w - 1;
			k += (j * w);
			
			for (int i = 0; i < w; i++) {

				// ----------------------------------------
				// Calculate pixel intensity (0-255)
				// ----------------------------------------

				a = (data[k] & 0xFF000000) >>> 24;
						
				// a = r;

				// ----------------------------------------
				// Calculate sum as an approximate sum
				// of the last s pixels
				// ----------------------------------------		
				sum += a - (sum / s);

				// ----------------------------------------
				// Factor in sum from the previous row
				// ----------------------------------------
				if (k >= w) {
					threshold = (sum + (data[k - w] & 0xffffff)) / (2 * s);
				} else {
					threshold = sum / s;
				}

				// ----------------------------------------
				// Compare the average sum to current pixel
				// to decide black or white
				// ----------------------------------------

				a = (a < threshold * f) ? 0 : 1;				
				
				// ----------------------------------------
				// Repack pixel data with binary data in
				// the alpha channel, and the running sum
				// for this pixel in the RGB channels
				// ----------------------------------------
				data[k] = (a << 24) + (sum & 0xffffff);

				switch (level) {

				// On a white region. No black pixels yet
				case 0:
					if (a == 0) { // First black encountered
						level = 1;
						b1 = 1;
						w1 = 0;
						b2 = 0;
					}
					break;

				// On first black region
				case 1:
					if (a == 0) {
						b1++;
					} else {
						level = 2;
						w1 = 1;
					}
					break;

				// On second white region (bulls-eye of a code?)
				case 2:
					if (a == 0) {
						level = 3;
						b2 = 1;
					} else {
						w1++;
					}
					break;

				// On second black region
				case 3:
					if (a == 0) {
						b2++;
					}
					// This could be a top code
					else {
						int mask;
						if ((b1 >= 2 && b2 >= 2) // less than 2 pixels... not interested
							&& 
							b1 <= maxu && b2 <= maxu && w1 <= (maxu + maxu)
							&& Math.abs(b1 + b2 - w1) <= (b1 + b2)
							&& Math.abs(b1 + b2 - w1) <= w1
							&& Math.abs(b1 - b2) <= b1
							&& Math.abs(b1 - b2) <= b2) {
							mask = 0x2000000;

							dk = 1 + b2 + (w1 / 2);

							if (j % 2 == 0) {
								dk = k - dk;
							} else {
								dk = k + dk;
							}

							data[dk - 1] |= mask;
							data[dk]     |= mask;
							data[dk + 1] |= mask;
							
							ccount += 3; // count candidate codes
						}
						b1 = b2;
						w1 = 1;
						b2 = 0;
						
						level = 2;
					}
					break;
				}

				k += (j % 2 == 0) ? 1 : -1;
			}
		}
		
		if (CANDIDATES_LOG) {
			log.d(TAG, String.format("Original candidates: %d", ccount));
		}
		
		if (TEST_VERTICAL_CANDIDATES) {
			ccount = scanCandidatesVertical();
			
			if (CANDIDATES_LOG) {
				log.d(TAG, String.format("Vertical candidates: %d", ccount));
			}
		}
	}
		
	
	
    public static String translateOrientationIDToString(int orientationID) {
        
        return answerIDToString[orientationID];
    }

    
    
	public static int translateOrientationToID(Float orientation) {

		if (!isValidOrientation(orientation)) {
			return ID_NO_ANSWER;
		}
		
		float angle = -orientation;   
		
		if (angle > ((1.0f / 4.0f) * Math.PI) && angle <= ((3.0f / 4.0f) * Math.PI) ) {
        	return ID_ANSWER_D;
        	
        } else if (angle > ((3.0f / 4.0f) * Math.PI) && angle <= ((5.0f / 4.0f) * Math.PI) ) {
        	return ID_ANSWER_C;    
        	
        } else if (angle > ((5.0f / 4.0f) * Math.PI) && angle <= ((7.0f / 4.0f) * Math.PI) ) {
        	return ID_ANSWER_B;    
        	
        } else {
        	return ID_ANSWER_A;
        }			
	}		
	
	
	
	public static String translateOrientationToString(Float orientation) {
	    
	    return answerIDToString[PaperclickersScanner.translateOrientationToID(orientation)];
	}
}
