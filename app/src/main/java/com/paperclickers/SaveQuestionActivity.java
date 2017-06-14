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

import com.paperclickers.camera.CameraEmulator;
import com.paperclickers.camera.CameraMain;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class SaveQuestionActivity extends Activity {

    static String TAG = "SaveQuestionsActivity";

    CustomEditText mEditText;
    TextView       mCharacterCountView;
    int            mCharacterCount = 0;
    CharSequence   mQuestionTag;

	boolean mUseRegularCamera;
    

    
    CharSequence cleanupQuestionTagForCSV(CharSequence enteredTag) {
        
        CharSequence cleanedTag;
                
        if ((enteredTag != null) && (enteredTag.length() > 0)) {
            
            int copyBegin = 0;
            
            while ((enteredTag.charAt(copyBegin) == '=') || (enteredTag.charAt(copyBegin) == ' ')) {
                copyBegin++;
            }
            
            cleanedTag = "\"" + enteredTag.subSequence(copyBegin, enteredTag.length()) + "\"";
        } else {
            cleanedTag = "\"\"";
        }
        
        return cleanedTag;
    }
    
    
    
    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        
        imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);        
    }
    
    
    
    @Override
    public void onBackPressed() {
        hideSoftKeyboard();
        
        finish();
    }
    
    
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.save_question);

		Intent whichIntent = getIntent();

		mUseRegularCamera = whichIntent.getBooleanExtra("useRegularCamera", true);

		mEditText = (CustomEditText) findViewById(R.id.questionTag);
		
		mEditText.setHorizontallyScrolling(false);
		mEditText.setMaxLines(getResources().getInteger(R.integer.question_tag_max_length));
		
		mEditText.addTextChangedListener(new TextWatcher() {
		    
            @Override
            public void afterTextChanged(Editable s) {}
            
            @Override    
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
		    
		    @Override
		    public void onTextChanged(CharSequence s, int start, int before, int count) {

                mCharacterCount = s.length();

                if (mCharacterCountView != null) {
                    mCharacterCountView.setText(String.valueOf(mCharacterCount) + "/" + getResources().getInteger(R.integer.question_tag_max_length));
                }
		    }
		});
		
		mEditText.setOnEditorActionListener(new OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        boolean handled = false;
		        
		        mQuestionTag = cleanupQuestionTagForCSV(mEditText.getText());
		        
		        PreferenceManager.getDefaultSharedPreferences(SaveQuestionActivity.this).edit().putString("question_tag", mQuestionTag.toString()).commit();
		        
		        if (actionId == EditorInfo.IME_ACTION_DONE) {
		            
		            hideSoftKeyboard();

					Intent i = null;

					if (mUseRegularCamera) {
						i = new Intent(getApplicationContext(), CameraMain.class);
					} else {
						i = new Intent(getApplicationContext(), CameraEmulator.class);
					}
	                
	                startActivity(i);
		            
	                handled = true;
		        }
		        
		        return handled;
		    }
		});
		
		showSoftKeyboard();
	}
	
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    
	    mCharacterCountView = new TextView(this);
	    
	    mCharacterCountView.setText(String.valueOf(mCharacterCount) + "/" + getResources().getInteger(R.integer.question_tag_max_length));
	    mCharacterCountView.setTextColor(Color.BLACK);
	    mCharacterCountView.setPadding(20, 0, 20, 0);
	    
	    menu.add(0, 0, 1, null).setActionView(mCharacterCountView).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	    
	    return (true);
	}
	
	
	
	@Override
	public void onPause() {
	    super.onPause();
	    
	    hideSoftKeyboard();
	}
	
	
	
	@Override
    protected void onResume() {
        super.onResume();
        
        showSoftKeyboard();
    }
	
	

	private void showSoftKeyboard() {
        mEditText.requestFocus();
        
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_NOT_ALWAYS);
	}
}
