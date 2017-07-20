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

import java.util.List;

import com.paperclickers.TopCodeValidator;
import com.paperclickers.log;
import com.paperclickers.fiducial.PaperclickersScanner;
import com.paperclickers.fiducial.TopCode;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.SurfaceView;

public class DrawView extends SurfaceView {

	final static String TAG = "DrawView";

	// Use this constant to enable drawing the topcodes validation countdown during camera scan
	// Enabling this disables the CameraMain.ONLY_PREVIEW_VALIDATED_CODES feature
	final static boolean DRAW_VALIDATION_COUNTDOWN = true;

	final static float REDUCED_STROKE_USAGE_DIAMETER_LIMIT = 15.0f;
	final static float VERY_REDUCED_STROKE = 3.0f;

	final static int NORMAL_TEXT_SIZE  = 40;
	final static int REDUCED_TEXT_SIZE = 20;
	final static int VERY_REDUCED_TEXT_SIZE = 10;
	
	private SparseArray<TopCodeValidator> mValidatorsList;
	
	private int mWidth;
	private int mHeight;
	
	private List<TopCode> mValidTopcodesList;
	
	private Paint mPaintA;
	private Paint mPaintB;
	private Paint mPaintC;
	private Paint mPaintD;

	private float mStrokeWidth;
	private float mTextStrokeWidth;

	private boolean mShowingValidation;

	
	
	private void drawRectangle(Canvas whichCanvas, float x, float y, float halfWidth, float halfHeight, boolean rounded, Paint whichPaint) {

		if (halfHeight <= REDUCED_STROKE_USAGE_DIAMETER_LIMIT) {
			whichPaint.setStrokeWidth(VERY_REDUCED_STROKE);
		} else {
			whichPaint.setStrokeWidth(mStrokeWidth);
		}


        RectF corners = new RectF(x - halfWidth,  y - halfHeight, x + halfWidth, y + halfHeight);
        
        if (rounded) {  
        	float roundRadius = halfHeight / 2;
        	
        	whichCanvas.drawRoundRect(corners, roundRadius, roundRadius, whichPaint);
        } else {
        	whichCanvas.drawRect(corners, whichPaint);        	
        }
	}
	
	
	
	private void drawTriangle(Canvas whichCanvas, float x, float y, float radius, Paint whichPaint) {

		float whichStroke = mStrokeWidth;

		if (radius <= REDUCED_STROKE_USAGE_DIAMETER_LIMIT) {
			whichStroke = VERY_REDUCED_STROKE;

			whichPaint.setStrokeWidth(VERY_REDUCED_STROKE);
		} else {
			whichPaint.setStrokeWidth(mStrokeWidth);
		}

		whichCanvas.drawLine(x - radius - whichStroke / 2, y + radius, x + radius + whichStroke / 2, y + radius, whichPaint);
		whichCanvas.drawLine(x - radius, y + radius, x + whichStroke / 8, y - radius, whichPaint);
		whichCanvas.drawLine(x + radius, y + radius, x - whichStroke / 8, y - radius, whichPaint);
	}
	
	
	
	public DrawView(Context context, 
	                SparseArray<TopCodeValidator> whichValidatorsList,
	                int width, 
	                int height,
	                boolean showingValidation) {
	    
		super(context);
		init();
		setWillNotDraw(false);
		
		mValidatorsList = whichValidatorsList;
		
		mValidTopcodesList = null;
		
		mWidth  = width;
		mHeight = height;
		
		mShowingValidation = showingValidation;
	}	
	
	
	
	private void init(){

		this.setZOrderOnTop(true);
		getHolder().setFormat(PixelFormat.TRANSPARENT);

		mPaintA = new Paint();
		
		mPaintA.setColor(PaperclickersScanner.COLOR_A);
		mPaintA.setStyle(Style.STROKE);
		
		DisplayMetrics display = getResources().getDisplayMetrics();

		if (display.densityDpi > DisplayMetrics.DENSITY_XHIGH) {
			mStrokeWidth = 10.0f;
			mTextStrokeWidth = 5.0f;
		} else if (display.densityDpi > DisplayMetrics.DENSITY_HIGH) {
			mStrokeWidth = 3.5f;
			mTextStrokeWidth = 3.5f;
		} else {
			mStrokeWidth = 2.0f;
			mTextStrokeWidth = 1.0f;
		}

		log.d(TAG, "StrokeWidth: " + mStrokeWidth);

		mPaintA.setStrokeWidth(mStrokeWidth);

		
		mPaintA.setAntiAlias(true);
		mPaintA.setDither(true);
		
		mPaintB = new Paint(mPaintA);
		mPaintC = new Paint(mPaintA);
		mPaintD = new Paint(mPaintA);

		mPaintB.setColor(PaperclickersScanner.COLOR_B);
		mPaintC.setColor(PaperclickersScanner.COLOR_C);
		mPaintD.setColor(PaperclickersScanner.COLOR_D);
	}
	
	
		
	@SuppressLint("DrawAllocation")
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if (mValidTopcodesList != null) {
  
            Rect textBounds = new Rect();
		    
			for (TopCode topCode : mValidTopcodesList) {
			    
			    boolean showingDuplicate = false;
			    
				TopCodeValidator whichValidator = mValidatorsList.get(topCode.getCode());
				
				int bestAnswer = PaperclickersScanner.ID_NO_ANSWER;
				
				if (DRAW_VALIDATION_COUNTDOWN && mShowingValidation) {
				    
				    bestAnswer = whichValidator.getDuplicatedAnswerInLastScanCycle();
				    
                    log.d(TAG, String.format("Testing duplicate; duplicated last cycle: %d; current topcode: %d", 
                          bestAnswer, PaperclickersScanner.translateOrientationToID(topCode.getOrientation()))); 
				    
				    if ((bestAnswer != PaperclickersScanner.ID_NO_ANSWER) &&
				        (bestAnswer == PaperclickersScanner.translateOrientationToID(topCode.getOrientation()))) {
				        
				        showingDuplicate = true;
				    } else {
				        bestAnswer = whichValidator.getLastDetectedAnswer();
				    }
				} else {
				    if (whichValidator != null) {
				        bestAnswer = whichValidator.getBestValidAnswer();
				    }
				}
    								
				if (bestAnswer != PaperclickersScanner.ID_NO_ANSWER) {
				
					float codeX = topCode.getCenterX();
					float codeY = topCode.getCenterY();
					
					float codeDiameter = topCode.getDiameter();
					
					codeX = codeX * canvas.getWidth() / mWidth;
		            codeY = codeY * canvas.getHeight() / mHeight;
		            
		            codeDiameter = codeDiameter * canvas.getWidth() / mWidth * 0.5f;
		            
		            Paint textPaint = null;
		            
		            switch(bestAnswer) {
		            case PaperclickersScanner.ID_ANSWER_A:
		            	drawTriangle(canvas, codeX, codeY, codeDiameter, mPaintA);
		            	
		            	textPaint = new Paint(mPaintA);
		            	
		            	break;
		            	
		            case PaperclickersScanner.ID_ANSWER_B:
		            	drawRectangle(canvas, codeX, codeY, codeDiameter * 0.8f, codeDiameter, false, mPaintB);
		            	
                        textPaint = new Paint(mPaintB);
                        
		            	break;
		            	
		            case PaperclickersScanner.ID_ANSWER_C:

		            	if (codeDiameter <= REDUCED_STROKE_USAGE_DIAMETER_LIMIT) {
							mPaintC.setStrokeWidth(VERY_REDUCED_STROKE);
						} else {
							mPaintC.setStrokeWidth(mStrokeWidth);
						}

		            	canvas.drawCircle(codeX, codeY, codeDiameter, mPaintC);
		            	
                        textPaint = new Paint(mPaintC);
                        
		            	break;
		            	
		            case PaperclickersScanner.ID_ANSWER_D:
		            	drawRectangle(canvas, codeX, codeY, codeDiameter, codeDiameter, true, mPaintD);
		            	
                        textPaint = new Paint(mPaintD);
                        
		            	break;
		            }
		            
		            if (DRAW_VALIDATION_COUNTDOWN && mShowingValidation) {
		                
		                String answerCountdown = null;
		                
		                if (showingDuplicate) {
		                    answerCountdown ="X";
		                } else if (whichValidator.isAnswerValid(bestAnswer)) {
		                    answerCountdown = "\u2713";
		                } else {
		                    answerCountdown = String.valueOf(TopCodeValidator.getCurrentValidationThrehshold() - whichValidator.getAnswerValidationCounter(bestAnswer));
		                }
		                    
		                textPaint.setTextSize(NORMAL_TEXT_SIZE);
		                textPaint.setTextAlign(Align.CENTER);

						if (textPaint.getStrokeWidth() == VERY_REDUCED_STROKE) {
							textPaint.setTextSize(VERY_REDUCED_TEXT_SIZE);
						} else {
							textPaint.setStrokeWidth(mTextStrokeWidth);

							if (textBounds.height() > codeDiameter) {
								textPaint.setTextSize(REDUCED_TEXT_SIZE);
							}
						}

		                textPaint.getTextBounds(answerCountdown, 0, 1, textBounds);

		                canvas.drawText(answerCountdown, codeX, codeY + codeDiameter / 3, textPaint);
		            }
				}
			}
		}
	}
	
	
	
	public void updateScreenSize(int width, int height) {
		
		mWidth  = width;
		mHeight = height;	
	}
	
	
	
	public void updateValidTopcodesList(List<TopCode> whichValidTopcodesList) {
		mValidTopcodesList = whichValidTopcodesList;
	}
}
