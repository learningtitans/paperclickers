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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import android.preference.PreferenceManager;
import android.renderscript.RenderScript;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.Script;
import android.renderscript.Type;

import static java.lang.Math.min;


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

	public static final boolean USE_RENDERSCRIPT = true;

	// The median filter has been implemented as an option for dealing with corrupted TopCodes; however, morphological
	// operations ended up with better performance.

	public static final boolean APPLY_MEDIAN_FILTER = false;



	public static final float INVALID_TOPCODE_ORIENTATION = Float.NaN;
	
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
	
	
	public static final int MORPHO_DILATION_STRUCT_SIZE = 3;
	public static final int MORPHO_EROSION_STRUCT_SIZE  = 3;

	public static final int MORPHO_HALF_DILATION_STRUCT_SIZE = (MORPHO_DILATION_STRUCT_SIZE - 1) / 2;
	public static final int MORPHO_HALF_EROSION_STRUCT_SIZE  = (MORPHO_EROSION_STRUCT_SIZE - 1) / 2;

	// Median filter size can be up to 7 - there is a constant defined in renderscript code.

	public static final int MEDIAN_FILTER_ELEMENT_SIZE = 5;
	public static final int MEDIAN_FILTER_HALF_ELEMENT_SIZE = (MEDIAN_FILTER_ELEMENT_SIZE - 1) / 2;

	public static final int PIXEL_COLOR_MASK = 0x01000000;
	

	boolean mUseMorphoOperations = true;
	int mMorphoElementSize = MORPHO_DILATION_STRUCT_SIZE;


	
	long mStartThresholdTime;
	long mEndThresholdTime;
	long mStartFindCodesTime;
	long mEndFindCodesTime;

	long mStartDilationTime;
	long mEndDilationTime;
	long mStartErosionTime;
	long mEndErosionTime;

	long mStartDilation2Time;
	long mEndDilation2Time;
	long mStartErosion2Time;
	long mEndErosion2Time;

	long mStartOpeningTime;
	long mEndOpeningTime;
	long mStartClosingTime;
	long mEndClosingTime;

	long mStartMedianFilterTime;
	long mEndMedianFilterTime;

	long mStartHorizontalScanTime;
	long mEndHorizontalScanTime;
	
	long mStartVerticalScanTime;
	long mEndVerticalScanTime;



	protected int[] mWorkingDataInt;

	RenderScript mRs = null;
	ScriptC_morphoOperations mMorphoOperationsScript = null;
	Allocation mMorphoData = null;
	Allocation mTmpData = null;
	Script.LaunchOptions mLaunchOptions = null;

	
	protected void adaptiveThreshold() {
        int a;
        int threshold, sum = 128;
        int s = 30;
        int k;

        double f = 0.975;

        this.ccount = 0;

		int currentLineOffset = 0;
		int nextPos;

		boolean invert = false;

        for (int j = 0; j < h; j++) {

            // ----------------------------------------
            // Process rows back and forth (alternating
            // left-to-right, right-to-left)
            // ----------------------------------------

			if (invert) {
				k = w - 1;
				nextPos = -1;
			} else {
				k = 0;
				nextPos = 1;
			}

//            k = (j % 2 == 0) ? 0 : w - 1;
            k += currentLineOffset;
            
            for (int i = 0; i < w; i++) {

                // ----------------------------------------
                // Calculate pixel intensity (0-255)
                // ----------------------------------------

                a = (data[k] & 0xFF000000) >>> 24;
                        

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

				k += nextPos;
//                k += (j % 2 == 0) ? 1 : -1;
            }

            currentLineOffset += w;
        }
	}
	


	public void finalize() {
		if (USE_RENDERSCRIPT) {
			renderscriptFinalize();
		}
    }


	
	/**
	 * Scan the image line by line looking for marked topcodes
	 * candidates
	 */
	protected List<TopCode> findCodes(boolean hasRotated, List<TopCode> spots) {
		
		int effectiveCandidatesCount = 0;
		
		this.tcount = 0;

		TopCode spot = new TopCode();
		
		int k = w * 2;

		int posX, posY;

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

						posX = i;
						posY = j;

						if (hasRotated) {
							posX = h - j;
							posY = i;
						}


						if (overlaps(spots, posX, posY) == null) {
		                     this.tcount++;
		                     spot.decode(this, i , j);
		                     
		                     if (spot.isValid()) {
		                    	 
		                         if (hasRotated) {
		                             spot.setLocation(h - spot.getCenterY(), spot.getCenterX());
		                             
//		                             log.d(TAG, String.format(">>> Previous orientation: %f", spot.getOrientation()));
		                             
		                             float newOrientation = (float) (-spot.getOrientation() - Math.PI / 2.0f);
		                             
		                             spot.setOrientation((float) (newOrientation < 0 ? -(2.0f * Math.PI + newOrientation) : -newOrientation));
		                             
//                                     log.d(TAG, String.format(">>> New orientation: %f", spot.getOrientation()));
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



	
	protected void morphoDilation() {

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {

                boolean hasHit = false;

                int startHeight = i - MORPHO_HALF_DILATION_STRUCT_SIZE;
                int startWidth  = j - MORPHO_HALF_DILATION_STRUCT_SIZE;

                int totalHeight = MORPHO_DILATION_STRUCT_SIZE;
                int totalWidth  = MORPHO_DILATION_STRUCT_SIZE;


                if (startHeight < 0) {
                    totalHeight += startHeight;
                    startHeight = i;
                } else if (i + MORPHO_HALF_DILATION_STRUCT_SIZE >= h) {
                	totalHeight = totalHeight - (MORPHO_HALF_DILATION_STRUCT_SIZE + 1 - h + i);
                }

                if (startWidth < 0) {
                    totalWidth += startWidth;
                    startWidth = j;
                } else if (j + MORPHO_HALF_DILATION_STRUCT_SIZE >= w) {
                    totalWidth = totalWidth - (MORPHO_HALF_DILATION_STRUCT_SIZE + 1 - w + j);
                }

                // Analyze the pixels under the mask, looking for a hit

                for (int y = 0; y < totalHeight; y++) {
                    for (int x = 0; x < totalWidth; x++) {

                        // Check if the pixel under the mask position is BLACK...

//                        log.d(TAG, String.format("i=%d, j=%d, startHeight=%d, startWidth=%d, totalHeight=%d, totalWidth=%d, y=%d, x=%d", i, j, startHeight, startWidth, totalHeight, totalWidth, y, x));


                        if ((data[(startHeight + y) * w + startWidth + x] & PIXEL_COLOR_MASK) == 0){

                            // ...if so, has a hit

                            hasHit = true;

                            break;
                        }
                    }
                }

                if (hasHit) {
                    // If it has a hit, set the current image pixel as BLACK
					mWorkingDataInt[i * w + j] = 0;
                } else {
                    // Otherwise, the pixel is WHITE
					mWorkingDataInt[i * w + j] = PIXEL_COLOR_MASK;
                }
            }
        }
	}



    protected void morphoErosion() {

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {

                boolean hasFit = true;

                int startHeight = i - MORPHO_HALF_EROSION_STRUCT_SIZE;
                int startWidth  = j - MORPHO_HALF_EROSION_STRUCT_SIZE;

                int totalHeight = MORPHO_EROSION_STRUCT_SIZE;
                int totalWidth  = MORPHO_EROSION_STRUCT_SIZE;


                if (startHeight < 0) {
                    totalHeight += startHeight;
                    startHeight = i;
                } else if (i + MORPHO_HALF_EROSION_STRUCT_SIZE >= h) {
                    totalHeight = totalHeight - (MORPHO_HALF_EROSION_STRUCT_SIZE + 1 - h + i);
                }

                if (startWidth < 0) {
                    totalWidth += startWidth;
                    startWidth = j;
                } else if (j + MORPHO_HALF_EROSION_STRUCT_SIZE >= w) {
                    totalWidth = totalWidth - (MORPHO_HALF_EROSION_STRUCT_SIZE + 1 - w + j);
                }

                // Analyze the pixels under the mask, looking for a hit

                for (int y = 0; y < totalHeight; y++) {
                    for (int x = 0; x < totalWidth; x++) {

                        // Check if the pixel under the mask position is WHITE...

                        if ((data[(startHeight + y) * w + startWidth + x] & PIXEL_COLOR_MASK) != 0){

                            // ...if so, does not has a fit

                            hasFit = false;

                            break;
                        }
                    }
                }

                if (hasFit) {
                    // If it has a fit, set the current image pixel as BLACK
					mWorkingDataInt[i * w + j] = 0;
                } else {
                    // Otherwise, the pixel is WHITE
					mWorkingDataInt[i * w + j] = PIXEL_COLOR_MASK;
                }
            }
        }
    }



    protected void medianFilterRenderscript(Allocation initialInput, Allocation auxiliaryData) {

		mMorphoOperationsScript.set_currentInput(initialInput);
		mMorphoOperationsScript.forEach_median(initialInput, auxiliaryData, mLaunchOptions);
	}



    protected void morphoRenderscriptClosing(Allocation initialInput, Allocation auxiliaryData) {

		mMorphoOperationsScript.set_currentInput(initialInput);
		mMorphoOperationsScript.forEach_dilation(initialInput, auxiliaryData, mLaunchOptions);

		mMorphoOperationsScript.set_currentInput(auxiliaryData);
		mMorphoOperationsScript.forEach_erosion(auxiliaryData, initialInput, mLaunchOptions);
	}



	protected void morphoRenderscriptOpening(Allocation initialInput, Allocation auxiliaryData) {

		mMorphoOperationsScript.set_currentInput(initialInput);
		mMorphoOperationsScript.forEach_erosion(initialInput, auxiliaryData, mLaunchOptions);

		mMorphoOperationsScript.set_currentInput(auxiliaryData);
		mMorphoOperationsScript.forEach_dilation(auxiliaryData, initialInput, mLaunchOptions);
	}


	public PaperclickersScanner(int width, int height, Context context) {
		super();

		this.maxu = min(width / TopCode.WIDTH, height / TopCode.WIDTH);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

		mUseMorphoOperations = preferences.getBoolean("development_use_morpho", true);

		if (mUseMorphoOperations) {
			String elementSize = preferences.getString("development_morpho_element_size", String.valueOf(MORPHO_DILATION_STRUCT_SIZE));
			mMorphoElementSize = Integer.parseInt(elementSize);
		}

		log.d(TAG, String.format("PaperclickersScanner: %d x %d, morpho: %b, morpho size: %d, maxu: %d", width, height, mUseMorphoOperations, mMorphoElementSize, maxu));

		this.w = width;
		this.h = height;

		if (USE_RENDERSCRIPT) {
            renderscriptInitialize(context);
		} else {
			mWorkingDataInt = new int[width * height];
		}
	}



    protected void renderscriptFinalize() {

		if (mMorphoData != null) {
			mMorphoData.destroy();

			mMorphoData = null;
		}

		if (mTmpData != null) {
			mTmpData.destroy();

			mTmpData = null;
		}

        mLaunchOptions = null;

		if (mMorphoOperationsScript != null) {
			mMorphoOperationsScript.destroy();

			mMorphoOperationsScript = null;
		}

		if (mRs != null) {
			mRs.destroy();
			mRs = null;
		}
    }



    protected void renderscriptInitialize(Context context) {

        mRs = RenderScript.create(context);

        mMorphoOperationsScript = new ScriptC_morphoOperations(mRs);

        mMorphoOperationsScript.set_width(w);
        mMorphoOperationsScript.set_height(h);

        mLaunchOptions = new Script.LaunchOptions();

        if (APPLY_MEDIAN_FILTER) {
			mMorphoOperationsScript.set_elementSize(MEDIAN_FILTER_ELEMENT_SIZE);
			mMorphoOperationsScript.set_halfElementSize(MEDIAN_FILTER_HALF_ELEMENT_SIZE);

            mLaunchOptions.setX(MEDIAN_FILTER_HALF_ELEMENT_SIZE, w - MEDIAN_FILTER_HALF_ELEMENT_SIZE);
            mLaunchOptions.setY(MEDIAN_FILTER_HALF_ELEMENT_SIZE, h - MEDIAN_FILTER_HALF_ELEMENT_SIZE);
        } else {
			mMorphoOperationsScript.set_elementSize(mMorphoElementSize);
			mMorphoOperationsScript.set_halfElementSize((mMorphoElementSize - 1)/2);

            mLaunchOptions.setX((mMorphoElementSize - 1)/2, w - (mMorphoElementSize - 1)/2);
            mLaunchOptions.setY((mMorphoElementSize - 1)/2, h - (mMorphoElementSize - 1)/2);
        }


        Type.Builder array2DBuilder = new Type.Builder(mRs, Element.U32(mRs));

        array2DBuilder.setX(w);
        array2DBuilder.setY(h);

        Type array2D = array2DBuilder.create();

        mMorphoData = Allocation.createTyped(mRs, array2D);
        mTmpData    = Allocation.createTyped(mRs, array2D);
    }

	
	
    public void scanProcessing(int[] image, boolean hasRotated, List<TopCode> codesFound) {
        
        this.data = image;
		this.ccount = 0;


		//log.d(TAG, String.format("++++> START SCAN", ccount));


        if (USE_RENDERSCRIPT) {
			if (APPLY_MEDIAN_FILTER) {
				if (LOG_EXECUTION_TIMES) {
					mStartMedianFilterTime = System.currentTimeMillis();
				}

				mTmpData.copyFromUnchecked(data);

				medianFilterRenderscript(mTmpData, mMorphoData);

				if (LOG_EXECUTION_TIMES) {
					mEndMedianFilterTime = System.currentTimeMillis();
					mStartThresholdTime  = System.currentTimeMillis();
				}
			} else {
				if (LOG_EXECUTION_TIMES) {
					mStartThresholdTime = System.currentTimeMillis();
				}

				mMorphoData.copyFromUnchecked(data);
			}

			mMorphoOperationsScript.set_currentInput(mMorphoData);

			mMorphoOperationsScript.invoke_adaptiveThreshold();

			mRs.finish();
		} else {
			if (LOG_EXECUTION_TIMES) {
				mStartThresholdTime = System.currentTimeMillis();
			}

			adaptiveThreshold(); // run the adaptive threshold filter
		}

		if (mUseMorphoOperations) {
			if (USE_RENDERSCRIPT) {
				if (LOG_EXECUTION_TIMES) {
					mEndThresholdTime = System.currentTimeMillis();
					mStartClosingTime = System.currentTimeMillis();
				}

				morphoRenderscriptClosing(mMorphoData, mTmpData);

				if (LOG_EXECUTION_TIMES) {
					mEndClosingTime   = System.currentTimeMillis();
					mStartOpeningTime = System.currentTimeMillis();
				}

				morphoRenderscriptOpening(mMorphoData, mTmpData);

				mMorphoData.copyTo(data);

				if (LOG_EXECUTION_TIMES) {
					mEndOpeningTime          = System.currentTimeMillis();
					mStartHorizontalScanTime = System.currentTimeMillis();
				}
			} else {
				if (LOG_EXECUTION_TIMES) {
					mEndThresholdTime  = System.currentTimeMillis();
					mStartDilationTime = System.currentTimeMillis();
				}

				morphoDilation();

				System.arraycopy(mWorkingDataInt, 0, data, 0, mWorkingDataInt.length);

				if (LOG_EXECUTION_TIMES) {
					mEndDilationTime  = System.currentTimeMillis();
					mStartErosionTime = System.currentTimeMillis();
				}

				morphoErosion();

				System.arraycopy(mWorkingDataInt, 0, data, 0, mWorkingDataInt.length);

				if (LOG_EXECUTION_TIMES) {
					mEndErosionTime    = System.currentTimeMillis();
					mStartErosion2Time = System.currentTimeMillis();
				}

				morphoErosion();

				System.arraycopy(mWorkingDataInt, 0, data, 0, mWorkingDataInt.length);

				if (LOG_EXECUTION_TIMES) {
					mEndErosion2Time    = System.currentTimeMillis();
					mStartDilation2Time = System.currentTimeMillis();
				}

				morphoDilation();

				System.arraycopy(mWorkingDataInt, 0, data, 0, mWorkingDataInt.length);

				if (LOG_EXECUTION_TIMES) {
					mEndDilation2Time        = System.currentTimeMillis();
					mStartHorizontalScanTime = System.currentTimeMillis();
				}
			}
		} else {
			if (LOG_EXECUTION_TIMES) {
				mEndThresholdTime        = System.currentTimeMillis();
				mStartHorizontalScanTime = System.currentTimeMillis();
			}

			if (USE_RENDERSCRIPT) {
				mMorphoData.copyTo(data);
			}
		}

		scanCandidatesHorizontal();

		if (LOG_EXECUTION_TIMES) {
			mEndHorizontalScanTime = System.currentTimeMillis();

			log.d(TAG, String.format("Horizontal candidates: %d", ccount));

			mStartVerticalScanTime = System.currentTimeMillis();
		}

		ccount = scanCandidatesVertical();

		if (LOG_EXECUTION_TIMES) {
			mEndVerticalScanTime = System.currentTimeMillis();

			log.d(TAG, String.format("Vertical candidates: %d", ccount));

			mStartFindCodesTime = System.currentTimeMillis();
		}

        // scan for topcodes
        findCodes(hasRotated, codesFound);

        if (LOG_EXECUTION_TIMES) {
            mEndFindCodesTime = System.currentTimeMillis();
            
            log.d(TAG, String.format("Threshold execution time(ms): %d", mEndThresholdTime - mStartThresholdTime));

			if (mUseMorphoOperations) {
				if (USE_RENDERSCRIPT) {
					log.d(TAG, String.format("Closing execution time(ms): %d, Opening execution time(ms): %d",
							mEndClosingTime - mStartClosingTime, mEndOpeningTime - mStartOpeningTime));
				} else {
					log.d(TAG, String.format("Closing Dilation execution time(ms): %d, Closing Erosion execution time(ms): %d",
							mEndDilationTime - mStartDilationTime, mEndErosionTime - mStartErosionTime));

					log.d(TAG, String.format("Opening Dilation execution time(ms): %d, Opening Erosion execution time(ms): %d",
							mEndDilation2Time - mStartDilation2Time, mEndErosion2Time - mStartErosion2Time));
				}
			}

			if (APPLY_MEDIAN_FILTER) {
				log.d(TAG, String.format("Median filter execution time(ms): %d", mEndMedianFilterTime - mStartMedianFilterTime));
			}

            log.d(TAG, String.format("Horizontal scan execution time(ms): %d, Vertical scan execution time(ms): %d", mEndHorizontalScanTime - mStartHorizontalScanTime, mEndVerticalScanTime - mStartVerticalScanTime));
            log.d(TAG, String.format("FindCodes execution time(ms): %d", mEndFindCodesTime - mStartFindCodesTime));
        }
    }
	
	
	
    protected void scanCandidatesHorizontal() {

        int a, b1, w1, b2, level, dk;

        int k;

        this.ccount = 0;

        for (int j = 0; j < h; j++) {

            level = b1 = b2 = w1 = 0;

            k = j * w;
            
            for (int i = 0; i < w; i++) {

                a = data[k + i] & PIXEL_COLOR_MASK;
                
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
                            dk = (k + i) - dk;

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
            }
        }
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

			k = j;

			for (int i = 0; i < h; i++) {
				
				a = data[k] & PIXEL_COLOR_MASK;
				
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

				// Go to the pixel in the next line, same column

				k += w;
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
