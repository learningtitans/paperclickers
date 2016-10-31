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

import com.paperclickers.camera.CameraMain;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class SaveQuestionActivity extends Activity {

    static String TAG = "SaveQuestionsActivity";

    CustomEditText mEditText;
    
    
    
    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        
        imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);        
    }
    
    
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.save_question);
		
		mEditText = (CustomEditText) findViewById(R.id.questionTag);
		mEditText.setOnEditorActionListener(new OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        boolean handled = false;
		        
		        if (actionId == EditorInfo.IME_ACTION_DONE) {
		            
		            hideSoftKeyboard();

		            Intent i = new Intent(getApplicationContext(), CameraMain.class);
	                
	                startActivity(i);
		            
	                handled = true;
		        }
		        return handled;
		    }
		});
		
		showSoftKeyboard();
	}

	
	
	@Override
	public void onBackPressed() {
	    hideSoftKeyboard();
	    
	    finish();
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
