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

package com.paperclickers;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.paperclickers.camera.CameraMain;
import com.paperclickers.result.AnswersLog;
import com.paperclickers.R;

public class MainActivity extends Activity {

	static String TAG = "MainActivity";

	static final int DEVELOPMENT_OPTIONS_ACTIVATION_THRESHOLD = 5;
	static final int DEVELOPMENT_OPTIONS_ACTIVATION_INTERVAL  = 300;
	
	int mDebugOptionsActivationTapCounter   = 0;
	long mDebugOptionsActivationLastTapTime = 0;
	
	boolean mManualQuestionsTagging = false;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.start_activity);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		Bundle intentExtras = getIntent().getExtras();
		
		boolean restartedInternally = false;
		
		if (intentExtras != null) {
	        
	        restartedInternally = intentExtras.getBoolean("restartingInternally");   
		}
		
		log.d(TAG, "onCreate - Received restarted internally indication: " + restartedInternally);
		
		if (!restartedInternally) {
		    AnswersLog.resetQuestionsSequenceNumber();
		    
		    log.d(TAG, ">>> New execution sequence; restart sequence number in answer log");
		}
		
		// Adding listener for "about" button
		Button about = (Button) findViewById(R.id.appIcon);
		about.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent i = new Intent(getApplicationContext(), AboutActivity.class);
				startActivity(i);
			}
		});

		
		// Adding listener for "settings" button
		Button settingsButton = (Button) findViewById(R.id.settingsIcon);
		settingsButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent i = new Intent(getApplicationContext(), SettingsActivity.class);

				startActivity(i);
			}
		});
		
		
		// Adding listener for "start" button
		Button startButton = (Button) findViewById(R.id.button_start);
		startButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

			    Intent newActivity;
			    
			    if (mManualQuestionsTagging) {
	                newActivity = new Intent(getApplicationContext(), SaveQuestionActivity.class);
			    } else {

					//TODO: !!! AQUI precisa direcionar qual a classe correta !!!

			        newActivity = new Intent(getApplicationContext(), CameraMain.class);
			    }
				
				startActivity(newActivity);
			}
		});
		
		
        
		if (SettingsActivity.DEVELOPMENT_OPTIONS) {
		    
    		// Adding listener for Debug mode activation
		    
    		TextView appNameText = (TextView) findViewById(R.id.appNameText);
    		
    		appNameText.setOnClickListener(new View.OnClickListener() {
    
                @Override
                public void onClick(View v) {
    
                    long currentTime = System.currentTimeMillis();
                    
                    if (currentTime - mDebugOptionsActivationLastTapTime < DEVELOPMENT_OPTIONS_ACTIVATION_INTERVAL) {
                        mDebugOptionsActivationTapCounter++;
                        
                        if (mDebugOptionsActivationTapCounter >= DEVELOPMENT_OPTIONS_ACTIVATION_THRESHOLD) {
                            
                            CharSequence activationText; 
                            boolean newDevelopmentModeStatus;
                            
                            if (SettingsActivity.getDevelopmentMode()) {
                                activationText = getResources().getText(R.string.development_mode_off);
                                
                                newDevelopmentModeStatus = false;
                            } else {
                                activationText = getResources().getText(R.string.development_mode_on);
                                
                                newDevelopmentModeStatus = true;
                            }
                            
                            Toast.makeText(getApplicationContext(), 
                                           activationText,
                                           Toast.LENGTH_SHORT).show();
                            
                            SettingsActivity.setDevelopmentMode(newDevelopmentModeStatus);
                            
                            mDebugOptionsActivationTapCounter = 0;
                        }
                    } else {
                        mDebugOptionsActivationTapCounter = 1;
                    }
                    
                    mDebugOptionsActivationLastTapTime = currentTime;
                }
            });
		}
	}
	

	
    @Override
    protected void onResume() {
        super.onResume();
        
        mDebugOptionsActivationTapCounter  = 0;
        mDebugOptionsActivationLastTapTime = 0;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        String questionsTaggingStr = prefs.getString("questions_tagging", "0");

        mManualQuestionsTagging = questionsTaggingStr.equals("1");
    }
}
