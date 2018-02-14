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

package com.paperclickers.result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.paperclickers.Analytics;
import com.paperclickers.AudienceResponses;
import com.paperclickers.SettingsActivity;
import com.paperclickers.camera.CameraEmulator;
import com.paperclickers.fiducial.PaperclickersScanner;
import com.paperclickers.overlay.OverlayManager;
import com.paperclickers.result.GridViewAdapter;
import com.paperclickers.R;
import com.paperclickers.log;
import com.paperclickers.camera.CameraMain;

public class GridViewActivity extends Activity {

	private static final String TAG = "GridViewActivity";

	private HashMap<Integer, String> mDetectedAnswers;
	
	private ArrayList<Entry> mAnswers;
	private ArrayList<Entry> mManuallyChangedAnswers;

	private AnswersLog mAnswersLog = null;

	private Analytics mAnalytics = null;

	private boolean mHasChangedAnswers = false;

	private SharedPreferences mPreferenceManager = null;

	private OverlayManager mOverlayManager = null;



	private void checkChangesAndSaveAnswersLog() {

		if (mAnswersLog != null) {
			if (!mManuallyChangedAnswers.isEmpty() && mHasChangedAnswers) {
				mAnswersLog.createAndWriteLog(mDetectedAnswers);

				mAnalytics.send_enteredAnswers(mManuallyChangedAnswers.size());
			}
		}
	}



	@Override
	public void onBackPressed() {
		mOverlayManager.markOverlayAsShown();

		super.onBackPressed();
	}



	@Override
	public void onCreate(Bundle savedInstanceState) {
	    
	    GridViewAdapter imageAdapter;
	    GridView        imagegrid;	    
	    
		super.onCreate(savedInstanceState);

		setContentView(R.layout.grid_view);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		Intent i = getIntent();

		mDetectedAnswers = new HashMap<Integer, String>();		
		mDetectedAnswers = (HashMap<Integer, String>)i.getSerializableExtra("detectedAnswers");
		
		if (mDetectedAnswers == null) {
			
			log.e(TAG, "GridView activity called with empty codes mDetectedAnswers!!!");
			
			finish();
		}		
		
		imagegrid = (GridView) findViewById(R.id.gridView);		
		
		Map<Integer, String> sortedMap = new TreeMap<Integer, String>(mDetectedAnswers);
		
		mAnswers                = new ArrayList<Entry>(sortedMap.entrySet());
		mManuallyChangedAnswers = new ArrayList<Entry>();


		// Save received list of detected answers in the answers log

		mAnswersLog = new AnswersLog(getApplicationContext());
		mAnswersLog.createAndWriteLog(mDetectedAnswers);

		mAnalytics = new Analytics(getApplicationContext());

		mOverlayManager = new OverlayManager(getApplicationContext(), getFragmentManager());

		mPreferenceManager = PreferenceManager.getDefaultSharedPreferences(this);

		if (mPreferenceManager.getBoolean("development_allow_answers_changing", false)) {
			imagegrid.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
					ImageView imageview = (ImageView) v.findViewById(R.id.imgAnswer);
					TextView textView = (TextView) v.findViewById(R.id.imgText);

					Entry givenAnswer = mAnswers.get(position);

					log.d(TAG, "clickListener: " + position + " : " + givenAnswer.getValue());


					//
					// Allows manually defining an answer
					//

					String answerValue = (String) givenAnswer.getValue();

					int whichColor = Color.WHITE;

					if (answerValue.equals(PaperclickersScanner.NO_ANSWER_STRING)) {
						if (!mManuallyChangedAnswers.contains(givenAnswer)) {
							mManuallyChangedAnswers.add(givenAnswer);
						}

						imageview.setImageResource(R.drawable.answer_a);

						givenAnswer.setValue("A");

					} else if (mManuallyChangedAnswers.contains(givenAnswer)) {
						if (answerValue.equals("A")) {
							imageview.setImageResource(R.drawable.answer_b);

							givenAnswer.setValue("B");

						} else if (answerValue.equals("B")) {
							imageview.setImageResource(R.drawable.answer_c);

							givenAnswer.setValue("C");

						} else if (answerValue.equals("C")) {
							imageview.setImageResource(R.drawable.answer_d);

							givenAnswer.setValue("D");

						} else {
							imageview.setImageResource(R.drawable.answer_notdetected);

							givenAnswer.setValue(PaperclickersScanner.NO_ANSWER_STRING);

							whichColor = Color.BLACK;
						}
					}

					textView.setTextColor(whichColor);

					mDetectedAnswers.put((Integer) givenAnswer.getKey(), (String) givenAnswer.getValue());

					mHasChangedAnswers = true;
				}
			});
		}
		
		imageAdapter = new GridViewAdapter(GridViewActivity.this, mAnswers);
		imagegrid.setAdapter(imageAdapter);
		
		Button done = (Button) findViewById(R.id.button_pie_chart);
		done.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				checkChangesAndSaveAnswersLog();

				mOverlayManager.markOverlayAsShown();

				Intent i = new Intent(getApplicationContext(), PieChartActivity.class);
				i.putExtra("detectedAnswers", mDetectedAnswers);

				startActivity(i);
			}
		});


		Button back = (Button) findViewById(R.id.button_back);
		back.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				mOverlayManager.markOverlayAsShown();

				finish();
			}
		});

		mOverlayManager.checkAndTurnOnOverlayTimer(OverlayManager.ANSWERS_SCREEN);
	}

	
	
	@Override
	protected void onPause() {

		super.onPause();
		
		if (isFinishing()) {
			
			log.d(TAG, "Finishing...");

			checkChangesAndSaveAnswersLog();

			Intent i;

			boolean useRegularCamera = true;

			if (SettingsActivity.isDevelopmentMode()) {
				useRegularCamera = !mPreferenceManager.getBoolean("development_use_camera_emulation", false);
			}

			if (useRegularCamera) {
				i = new Intent(getApplicationContext(), CameraMain.class);
			} else {
				i = new Intent(getApplicationContext(), CameraEmulator.class);
			}

			i.setAction(AudienceResponses.RECALL_CODES_INTENT);
			i.putExtra("detectedAnswers", mDetectedAnswers);

			startActivity(i);			
		}

		mOverlayManager.removeOverlay();
	}
	
	
	
	@Override
	protected void onResume() {

		super.onResume();
		
		log.d(TAG, "Resuming...");

		mHasChangedAnswers = false;

		mOverlayManager.checkAndTurnOnOverlayTimer(OverlayManager.ANSWERS_SCREEN);
	}
}
