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

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfDocument.Page;
import android.graphics.pdf.PdfDocument.PageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.paperclickers.R;
import com.paperclickers.camera.CameraMain;
import com.paperclickers.fiducial.TopCode;


/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See
 * <a href="http://developer.android.com/design/patterns/settings.html"> Android
 * Design: Settings</a> for design guidelines and the
 * <a href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {
	
	static String TAG = "SettingsActivity";
	
	// Use this constant to enable a development options in settings
	public static final boolean DEVELOPMENT_OPTIONS = CameraMain.AVOID_PARTIAL_READINGS & true;
	
	// Use this constant to enable the holding area print on codes verso
	public static final boolean SHOW_CODES_HOLD_AREA = true;
	
	// Use this constant to enable the self promotion on topcodes verso
	public static final boolean PRINT_PROMOTION_ON_VERSO = true;
	
	// Internal intents for handling execution options
	public static String PRINT_CODES_INTENT = "com.paperclickers.intent.action.PRINT_CODES";
	public static String SHARE_ANSWERS_LOG  = "com.paperclickers.intent.action.SHARE_ANSWERS_LOG";
	public static String DELETE_ANSWERS_LOG = "com.paperclickers.intent.action.DELETE_ANSWERS_LOG";
	
	
	// Used TopCodes defined according to the list provided by Michael Horn in
	// http://users.eecs.northwestern.edu/~mhorn/topcodes/topcodes.pdf 
	// (accessed in 2015-09-27 11:40 AM GMT-3)
	
	public static int validTopCodes[] = {  31,   47,   55,   59,   61,   79,   87,   91,   93, 
									      103,  107,  109,  115,  117,  121,  143,  151,  155,
									      157,  167,  171,  173,  179,  181,  185,  199,  203,
									      205,  211,  213,  217,  227,  229,  233,  241,  271,
									      279,  283,  285,  295,  299,  301,  307,  309,  313,
									      327,  331,  333,  339,  341,  345,  355,  357,  361,
									      369,  391,  395,  397,  403,  405,  409,  419,  421,
									      425,  433,  453,  457,  465,  551,  555,  557,  563,
									      565,  569,  583,  587,  589,  595,  597,  601,  611,
									      613,  617,  651,  653,  659,  661,  665,  675,  677,
									      681,  713,  793,  805,  809,  841, 1171, 1173, 1189};
	
	// Development mode status
	static boolean mDevelopmentMode = false;
	
	static DevelopmentPreferenceFragment mDevelopmentFragment = null;
	static SettingsActivity mDevelopmentActivity = null;
	

	
	// Page specifications for PDF generation
	
	static float PS_POINT_PER_POL = 72;
	
	static float A4_WIDTH  =  8.267F;
	static float A4_HEIGHT = 11.692F; 
	
	static float LETTER_WIDTH  = 8.5F;
	static float LETTER_HEIGHT = 11F;
	
	static float A4_PS_WIDTH  = A4_WIDTH * PS_POINT_PER_POL;
	static float A4_PS_HEIGHT = A4_HEIGHT * PS_POINT_PER_POL;
	
	static float LETTER_PS_WIDTH  = LETTER_WIDTH * PS_POINT_PER_POL;
	static float LETTER_PS_HEIGHT = LETTER_HEIGHT * PS_POINT_PER_POL;
	
	static float REFERENCE_TEXT = 10;
	
	static float ONE_PER_PAGE_TEXT        = 10;
	static float ONE_PER_PAGE_TEXT_MARGIN = 5f;
	
	static float TWO_PER_PAGE_TEXT        = 10;
	static float TWO_PER_PAGE_TEXT_MARGIN = 3f;

	static float FOUR_PER_PAGE_TEXT        = 10;
	static float FOUR_PER_PAGE_TEXT_MARGIN = 2f;
	
	static float TOP_CODE_SCALE = 0.8f;
	static float TOUCH_AREA_WIDTH_FACTOR = 0.4f;
	
    static float PROMOTION_LINES_SPACING = 1.5f;
    
    static float PROMOTION_TEXT_SIZE_PROPORTION = 0.8f;
    
	static String TOPCODES_FILE_PATH       = "/Download";
	
	static String TOPCODES_SINGLE_FILENAME = "paperclickers_topCodes.pdf";
	
	static String TOPCODES_RECTO_FILENAME  = "paperclickers_topCodes_recto.pdf";
	static String TOPCODES_VERSO_FILENAME  = "paperclickers_topCodes_verso.pdf";
	
	static String PAPERCLICKERS_GITHUB_TOPCODES = "https://github.com/learningtitans/paperclickers/tree/master/topcodes";
	
	/**
	 * Determines whether to always show the simplified settings UI, where
	 * settings are presented in a single list. When false, settings are shown
	 * as a master/detail two-pane view on tablets. When true, a single pane is
	 * shown on tablets.
	 */
	private static final boolean ALWAYS_SIMPLE_PREFS = false;



	public static class AnswerLogPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			log.d(TAG, "----> AnswerLogPreferenceFragment");

			addPreferencesFromResource(R.xml.pref_answers_log);
		}
	}

	
	
    public static class DevelopmentPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            log.d(TAG, "----> DevelopmentPreferenceFragment");
            
            if (DEVELOPMENT_OPTIONS) {
                
                mDevelopmentFragment = this;
                
                if (mDevelopmentMode) {
            
                    addPreferencesFromResource(R.xml.pref_development);
                    
                    bindPreferenceSummaryToValue(findPreference("development_validation_threshold"));
                    bindPreferenceSummaryToValue(findPreference("development_show_validation"));
                }
            }
        }
    }	

    

	public static class GeneralPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			log.d(TAG, "----> GeneralPreferenceFragment");

			addPreferencesFromResource(R.xml.pref_general);

			bindPreferenceSummaryToValue(findPreference("students_number"));
		}
	}



	public static class PrintCodesPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			log.d(TAG, "----> PrintCodesPreferenceFragment");
			
			addPreferencesFromResource(R.xml.pref_print_codes);

			bindPreferenceSummaryToValue(findPreference("print_codes_page_format"));
			bindPreferenceSummaryToValue(findPreference("print_codes_per_page"));
			bindPreferenceSummaryToValue(findPreference("print_recto_verso_sequence"));
			
			if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
			    findPreference("print_codes_page_format").setEnabled(false);
			    findPreference("print_codes_per_page").setEnabled(false);
			    findPreference("print_recto_verso_sequence").setEnabled(false);
			}
		}
	}

	
	
	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 *
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private static void bindPreferenceSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's
		// current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager
				.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
	}

	
	
	public static boolean getDevelopmentMode() {
	    return mDevelopmentMode;
	}
	
	
	
	/**
	 * Determines whether the simplified settings UI should be shown. This is
	 * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
	 * doesn't have newer APIs like {@link PreferenceFragment}, or the device
	 * doesn't have an extra-large screen. In these cases, a single-pane
	 * "simplified" settings UI should be shown.
	 */
	private static boolean isSimplePreferences(Context context) {
		return ALWAYS_SIMPLE_PREFS || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB || !isXLargeTablet(context);
	}

	
	
	@Override
	public boolean isValidFragment(String fragmentName) {
		return true;
	}

	
	
	/**
	 * Helper method to determine if the device has an extra-large screen. For
	 * example, 10" tablets are extra-large.
	 */
	private static boolean isXLargeTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	
	
	/** {@inheritDoc} */
	@Override

	public void onBuildHeaders(List<Header> target) {
		
		if (!isSimplePreferences(this)) {
			loadHeadersFromResource(R.xml.pref_headers, target);
			
            if (DEVELOPMENT_OPTIONS && mDevelopmentMode) {
                loadHeadersFromResource(R.xml.pref_header_development, target);
            }
		}
	}

	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupActionBar();
	}

	
	
	/** {@inheritDoc} */
	@Override
	public boolean onIsMultiPane() {
		return isXLargeTablet(this) && !isSimplePreferences(this);
	}

	
	
    @Override
    protected void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        
        log.d(TAG,"+++++ New intent: " + newIntent + " " + getCallingActivity() + " " + getCallingPackage() + 
              " " + getComponentName());
        
        if (newIntent.getAction().equals(PRINT_CODES_INTENT)) {

        	printAndShareCodes();
        	
        } else if (newIntent.getAction().equals(SHARE_ANSWERS_LOG)) {
        	
        	if (com.paperclickers.result.AnswersLog.checkIfAnswersLogExists()) {
        		
        		Intent shareIntent = new Intent();
        		
        		shareIntent.setAction(Intent.ACTION_SEND);
        		shareIntent.putExtra(Intent.EXTRA_STREAM, com.paperclickers.result.AnswersLog.getAnswersLogUri());
        		shareIntent.setType("text/csv");
        		
        		startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share_answers_log_using)));
        		
        	} else {
    			Toast.makeText(getApplicationContext(), getResources().getText(R.string.no_answers_log),
						       Toast.LENGTH_LONG).show();        		
        	}
        } else if (newIntent.getAction().equals(DELETE_ANSWERS_LOG)) {
        	
        	if (com.paperclickers.result.AnswersLog.checkIfAnswersLogExists()) {
        		
        		new AlertDialog.Builder(this)
	    			.setTitle(R.string.delete_answers_log_dialog_title)
	    			.setMessage(R.string.delete_answers_log_dialog_text)
	    			.setPositiveButton(R.string.yes,
		    			new DialogInterface.OnClickListener() {
		    				@Override
		    				public void onClick(DialogInterface dialog, int which) {
		    					com.paperclickers.result.AnswersLog.deleteAnswersLog();
		    				}
    			}).show();
        		
        	} else {
    			Toast.makeText(getApplicationContext(), getResources().getText(R.string.no_answers_log),
						       Toast.LENGTH_LONG).show();        		
        	}        	
        }
    }

		
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		
		log.d(TAG, "onOptionsItemSelected: " + id);

		
		if (id == android.R.id.home) {
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			// TODO: If Settings has multiple levels, Up should navigate up
			// that hierarchy.
		    
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		setupSimplePreferencesScreen();
	}

	
	
	@TargetApi(Build.VERSION_CODES.KITKAT)
	
	/**
	 * printAndShareCode()
	 * 
	 * TopCodes will be printed in portrait PDF pages, always in portrait orientation, in sets of:
	 * 
	 */
	private void printAndShareCodes() {
		
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            
            AlertDialog.Builder alert = new AlertDialog.Builder(this); 
            alert.setTitle(R.string.cannot_generate_PDF);
            alert.setMessage(R.string.visit_project_page_for_topcodes);
            alert.setPositiveButton(R.string.go_github, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    
                    dialog.dismiss();
                    
                    Intent openTopcodesIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(PAPERCLICKERS_GITHUB_TOPCODES));
                    startActivity(openTopcodesIntent);
                }
            });
            alert.show();
            
        } else {  
    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    		
    		String studentsStr = prefs.getString("students_number", "10");
    		int studentsNum    = Integer.parseInt(studentsStr);
    		
    		if (studentsNum > validTopCodes.length) {
    			studentsNum = validTopCodes.length;
    		}
    		
    		String codesPerPageStr = prefs.getString("print_codes_per_page", "1");
    		int codesPerPage       = Integer.parseInt(codesPerPageStr);
    		
    		String pageFormatStr = prefs.getString("print_codes_page_format", "0");
    		int pageFormat       = Integer.parseInt(pageFormatStr);
    		
    		String rectoVersoSequenceStr = prefs.getString("print_recto_verso_sequence", "0");
    		int rectoVersoSequence       = Integer.parseInt(rectoVersoSequenceStr);
    		
    		
    		// Allocate PDF documents according to the recto/verso printing sequence
    		
    		PdfDocument document = new PdfDocument();
    		PdfDocument verso    = null;
    		
    		if (rectoVersoSequence == 1) {
    			verso = new PdfDocument();
    		}
    		
    		
    		// Create the PDF page format according to the page format dimensions
    		
    		int height;
    		int width;
    		
    		if (pageFormat == 0) {
    
    			height = (int) A4_PS_HEIGHT;
    			width  = (int) A4_PS_WIDTH;	
    	
    		} else {
    
    			height = (int) LETTER_PS_HEIGHT;
    			width  = (int) LETTER_PS_WIDTH;
    		}		
    		
    		
    		PageInfo pageInfo = 
    		    new PageInfo.Builder(width, height, (int) Math.ceil(studentsNum / codesPerPage)).create();
    		
    		
    		switch(codesPerPage) {
    		case 1:
    			printCodes1PerPage(studentsNum, document, verso, pageInfo);
    			
    			break;
    		case 2:
    			printCodes2PerPage(studentsNum, document, verso, pageInfo);
    			
    			break;
    		case 4:
    			printCodes4PerPage(studentsNum, document, verso, pageInfo);
    			
    			break;
    		}
    		
    		Uri savedFileUri;
    		Intent shareIntent = new Intent();
    		
    		if (verso == null) {
    			savedFileUri = saveTopCodesFile(document, TOPCODES_SINGLE_FILENAME);
    			
    			if (savedFileUri != null) {
    				shareIntent.setAction(Intent.ACTION_SEND);
    				
    				shareIntent.putExtra(Intent.EXTRA_STREAM, savedFileUri);
                    shareIntent.setType("application/pdf");
    			}
    		} else {
    			savedFileUri = saveTopCodesFile(document, TOPCODES_RECTO_FILENAME);
    			
    			if (savedFileUri != null) {
    				ArrayList<Uri> files = new ArrayList<Uri>();
    				
    				shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
    				
    				files.add(savedFileUri);
    				
    				savedFileUri = saveTopCodesFile(verso, TOPCODES_VERSO_FILENAME);
    				
    				if (savedFileUri != null) {
    					files.add(savedFileUri);
    										
    					shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
                        shareIntent.setType("application/pdf");
    				}
    			}
    		}
    		
    		if (savedFileUri != null) {
    			startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share_topcodes_using)));
    		}
        }
	}
	
	
	
	@TargetApi(Build.VERSION_CODES.KITKAT)
	private void printCodes1PerPage(int howManyCodes, PdfDocument docRecto, PdfDocument docVerso, PageInfo pageInfo) {
		
		int pageNumber  = howManyCodes;
		int currentCode = 0;

		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.BLACK);
		
		if (docVerso == null) {
			docVerso = docRecto;
		}

		Canvas whichCanvas;
		
		
		for (int i = 0; i < pageNumber; i++) {
			Page page   = docRecto.startPage(pageInfo);
			whichCanvas = page.getCanvas();
			
			float width  = pageInfo.getPageWidth();
			float height = pageInfo.getPageHeight();
			
			printRecto(currentCode + 1, validTopCodes[currentCode], whichCanvas, 
					   width/2, height/2, 
					   width, height, 
					   REFERENCE_TEXT, ONE_PER_PAGE_TEXT_MARGIN);
						
			docRecto.finishPage(page);
			
			page        = docVerso.startPage(pageInfo);
			whichCanvas = page.getCanvas();
			
			printVerso(currentCode + 1, validTopCodes[currentCode], whichCanvas, 
					   width/2, height/2, 
					   width, height, 
					   ONE_PER_PAGE_TEXT, ONE_PER_PAGE_TEXT_MARGIN);

			currentCode++;
			
			docVerso.finishPage(page);
		}
	}
	
	
	
	@TargetApi(Build.VERSION_CODES.KITKAT)
	private void printCodes2PerPage(int howManyCodes, PdfDocument docRecto, PdfDocument docVerso, PageInfo pageInfo) {
		
		int pageNumber  = (int) Math.ceil(((float)howManyCodes) / 2);
		int currentCode = 0;

		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.BLACK);
		
		if (docVerso == null) {
			docVerso = docRecto;
		}

		Canvas whichCanvas;
		
		
		for (int i = 0; i < pageNumber; i++) {
			Page page   = docRecto.startPage(pageInfo);
			whichCanvas = page.getCanvas();
			
			float width  = pageInfo.getPageWidth();
			float height = pageInfo.getPageHeight();		
			
			// Rotate the canvas 90 degrees in order to achieve portrait codes in a portrait page
			// After rotating, compensate the position changing with the "difference" value and 
			// invert X and Y references.
			
			whichCanvas.rotate(90, width / 2, height / 2);
			
			float difference = height / 2 - width / 2;

			
			printRecto(currentCode + 1, validTopCodes[currentCode], whichCanvas, 
					   height / 4 - difference, width / 2 + difference,  
					   height / 2, width,  
					   REFERENCE_TEXT, TWO_PER_PAGE_TEXT_MARGIN);
			
			whichCanvas.drawLine(height / 2 - difference, difference, height / 2 - difference, width + difference, paint);
			
			if (currentCode + 1 < howManyCodes) {
				printRecto(currentCode + 2, validTopCodes[currentCode + 1], whichCanvas, 
						   height * 3 / 4 - difference, width / 2 + difference, 
						   height / 2, width, 
						   REFERENCE_TEXT, TWO_PER_PAGE_TEXT_MARGIN);				
			}			
			
			docRecto.finishPage(page);
			
			page        = docVerso.startPage(pageInfo);
			whichCanvas = page.getCanvas();
			
			
			// Same 90 degrees rotation for the verso page, in order to achieve portrait codes in a portrait page
			// But for verso it is also required to flip horizontally (before rotating) to compensate the page 
			// long edge inversion.
			
			whichCanvas.scale(-1, -1, width / 2, height / 2);
			whichCanvas.rotate(90, width / 2, height / 2);
			
			
			
						
			printVerso(currentCode + 1, validTopCodes[currentCode], whichCanvas, 
					   height * 3 / 4 - difference, width / 2 + difference,  
					   height / 2, width,  
					   TWO_PER_PAGE_TEXT, TWO_PER_PAGE_TEXT_MARGIN);

			whichCanvas.drawLine(height / 2 - difference, difference, height / 2 - difference, width + difference, paint);
			
			if (currentCode + 1 < howManyCodes) {
				printVerso(currentCode + 2, validTopCodes[currentCode + 1], whichCanvas,
						   height / 4 - difference, width / 2 + difference, 
						   height / 2, width, 						
						   TWO_PER_PAGE_TEXT, TWO_PER_PAGE_TEXT_MARGIN);
				
				currentCode += 2;
			} else {
				currentCode++;
			}
						
			docVerso.finishPage(page);
		}
	}
	
	
	
	@TargetApi(Build.VERSION_CODES.KITKAT)
	private void printCodes4PerPage(int howManyCodes, PdfDocument docRecto, PdfDocument docVerso, PageInfo pageInfo) {
		
		
		int pageNumber  = (int) Math.ceil(((float) howManyCodes) / 4);
		int currentCode = 0;

		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.BLACK);
		
		if (docVerso == null) {
			docVerso = docRecto;
		}

		Canvas whichCanvas;
		
		for (int i = 0; i < pageNumber; i++) {
			Page page   = docRecto.startPage(pageInfo);
			whichCanvas = page.getCanvas();
			
			float width  = pageInfo.getPageWidth();
			float height = pageInfo.getPageHeight();

			whichCanvas.drawLine(0, height/2, width, height/2, paint);
			whichCanvas.drawLine(width/2, 0, width/2, height, paint);
			
			
			printRecto(currentCode + 1, validTopCodes[currentCode], whichCanvas, 
					   width/4, height/4, 
					   width/2, height/2, 
					   REFERENCE_TEXT, FOUR_PER_PAGE_TEXT_MARGIN);
						
			if (currentCode + 1 < howManyCodes) {
				printRecto(currentCode + 2, validTopCodes[currentCode + 1], whichCanvas, 
						   width*3/4, height/4, 
						   width/2, height/2, 
						   REFERENCE_TEXT, FOUR_PER_PAGE_TEXT_MARGIN);		
				
				if (currentCode + 2 < howManyCodes) {
					printRecto(currentCode + 3, validTopCodes[currentCode + 2], whichCanvas, 
							   width/4, height*3/4, 
							   width/2, height/2, 
							   REFERENCE_TEXT, FOUR_PER_PAGE_TEXT_MARGIN);
					
					if (currentCode + 3 < howManyCodes) {
						printRecto(currentCode + 4, validTopCodes[currentCode + 3], whichCanvas, 
								   width*3/4, height*3/4, 
								   width/2, height/2, 
								   REFERENCE_TEXT, FOUR_PER_PAGE_TEXT_MARGIN);
					}
				}
			}
			
			docRecto.finishPage(page);
			
			page        = docVerso.startPage(pageInfo);
			whichCanvas = page.getCanvas();
			
			printVerso(currentCode + 1, validTopCodes[currentCode], whichCanvas, 
					   width*3/4, height/4, 
					   width/2, height/2, 
					   FOUR_PER_PAGE_TEXT, FOUR_PER_PAGE_TEXT_MARGIN);
			
			
			if (currentCode + 1 < howManyCodes) {
				printVerso(currentCode + 2, validTopCodes[currentCode + 1], whichCanvas, 
						   width/4, height/4, 
						   width/2, height/2, 
						   FOUR_PER_PAGE_TEXT, FOUR_PER_PAGE_TEXT_MARGIN);
				
				if (currentCode + 2 < howManyCodes) {
					printVerso(currentCode + 3, validTopCodes[currentCode + 2], whichCanvas, 
							   width*3/4, height*3/4, 
							   width/2, height/2, 
							   FOUR_PER_PAGE_TEXT, FOUR_PER_PAGE_TEXT_MARGIN);
					
					if (currentCode + 3 < howManyCodes) {
						printVerso(currentCode + 4, validTopCodes[currentCode + 3], whichCanvas, 
								   width/4, height*3/4, 
								   width/2, height/2, 
								   FOUR_PER_PAGE_TEXT, FOUR_PER_PAGE_TEXT_MARGIN);
						
						currentCode += 4;
					} else {
						currentCode += 3;
					}
				} else {
					currentCode += 2;
				}
			}else {
				currentCode++;
			}
			

            whichCanvas.drawLine(0, height/2, width, height/2, paint);
            whichCanvas.drawLine(width/2, 0, width/2, height, paint);
			
			docVerso.finishPage(page);
		}
	}

	
	
	private void printRecto(int topCodeTranslation, int whichTopCode, Canvas whichCanvas, float centerX, float centerY, 
							float width, float height, float textSize, float textMargin) {
		
		TopCode topCode  = new TopCode(whichTopCode);
		
		topCode.setLocation(centerX, centerY);
		topCode.setDiameter(width * TOP_CODE_SCALE);	
		
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		
		paint.setColor(Color.BLACK);
		paint.setTextSize(textSize);
		paint.setTextAlign(Align.CENTER);	
		
		topCode.draw(whichCanvas);
		
		whichCanvas.drawText(String.valueOf(topCodeTranslation), centerX + width/2 * 3/4, centerY - height/2 + textSize * textMargin, paint);		
	}
	
	
	
	private void printVerso(int topCodeTranslation, int whichTopCode, Canvas whichCanvas, float centerX, float centerY, 
							float width, float height, float textSize, float textMargin) {
	
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		
		paint.setColor(Color.BLACK);
		paint.setTextSize(textSize);
		paint.setTextAlign(Align.CENTER);
		
        Paint paintTouchArea      = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint paintTouchAreaLeft  = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint paintTouchAreaRight = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint paintPromotion      = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        String holdHere;
        String auto_promotion_1;
        String auto_promotion_2;
        
		if (SHOW_CODES_HOLD_AREA) {
    		paintTouchArea.setColor(Color.BLACK);
            paintTouchArea.setTextSize(textSize);
            paintTouchArea.setTextAlign(Align.LEFT);
            paintTouchArea.setStyle(Style.STROKE);
            
            paintTouchAreaLeft.setColor(Color.BLACK);
            paintTouchAreaLeft.setTextSize(textSize);
            paintTouchAreaLeft.setTextAlign(Align.LEFT);

            paintTouchAreaRight.setColor(Color.BLACK);
            paintTouchAreaRight.setTextSize(textSize);
            paintTouchAreaRight.setTextAlign(Align.RIGHT);
            
            holdHere = getResources().getText(R.string.hold_area).toString();
		}
		
		if (PRINT_PROMOTION_ON_VERSO) {
		    paintPromotion.setColor(Color.BLACK);
		    paintPromotion.setTextSize(textSize);
		    paintPromotion.setTextAlign(Align.CENTER);
		    paintPromotion.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
            
            auto_promotion_1 = getResources().getText(R.string.auto_promotion_1).toString();
            auto_promotion_2 = getResources().getText(R.string.auto_promotion_2).toString();
            
            // adjust final text size, depeding on the printing format
            
            Rect textBounds = new Rect();
            paintPromotion.getTextBounds(auto_promotion_2, 0, auto_promotion_2.length(), textBounds);
            
            float desiredWidth = (height - width * TOUCH_AREA_WIDTH_FACTOR * 2) * PROMOTION_TEXT_SIZE_PROPORTION;
            
            paintPromotion.setTextSize(textSize * (desiredWidth / textBounds.width()));
		}
		    
		
		whichCanvas.drawText(String.valueOf(topCodeTranslation), centerX, centerY + textSize / 2, paint);
		
	    // use this value to compensate the center coordinates rotation in order to print the text with the proper orientation
		float difference = centerY - centerX;   
		
        if (SHOW_CODES_HOLD_AREA) {
            
            Path data = new Path(); 
            
            data.addArc(new RectF(centerX - width / 2 - width * TOUCH_AREA_WIDTH_FACTOR, centerY - height / 2 - width * TOUCH_AREA_WIDTH_FACTOR, 
                                  centerX - width / 2 + width * TOUCH_AREA_WIDTH_FACTOR, centerY - height / 2 + width * TOUCH_AREA_WIDTH_FACTOR), 
                        0f, 90f);
            data.lineTo(centerX - width / 2, centerY - height / 2);
            data.close();
            
            whichCanvas.drawPath(data, paintTouchArea);
            
            data.reset();
            
            data.addArc(new RectF(centerX + width / 2 - (width * TOUCH_AREA_WIDTH_FACTOR), centerY - height / 2 - (width * TOUCH_AREA_WIDTH_FACTOR), 
                                  centerX + width / 2 + (width * TOUCH_AREA_WIDTH_FACTOR), centerY - height / 2 + (width * TOUCH_AREA_WIDTH_FACTOR)), 
                        90f, 90f);
            data.lineTo(centerX + width / 2, centerY - height / 2);
            data.close();
            
            whichCanvas.drawPath(data, paintTouchArea);
            
            data.reset();
            
            data.addArc(new RectF(centerX - width / 2 - width * TOUCH_AREA_WIDTH_FACTOR, centerY + height / 2 - width * TOUCH_AREA_WIDTH_FACTOR, 
                                  centerX - width / 2 + width * TOUCH_AREA_WIDTH_FACTOR, centerY + height / 2 + width * TOUCH_AREA_WIDTH_FACTOR), 
                        270f, 90f);
            data.lineTo(centerX - width / 2, centerY + height / 2);
            data.close();
            
            whichCanvas.drawPath(data, paintTouchArea);
            
            data.reset();
            
            data.addArc(new RectF(centerX + width / 2 - width * TOUCH_AREA_WIDTH_FACTOR, centerY + height / 2 - width * TOUCH_AREA_WIDTH_FACTOR, 
                                  centerX + width / 2 + width * TOUCH_AREA_WIDTH_FACTOR, centerY + height / 2 + width * TOUCH_AREA_WIDTH_FACTOR), 
                        180f, 90f);
            data.lineTo(centerX + width / 2, centerY + height / 2);
            data.close();

            whichCanvas.drawPath(data, paintTouchArea);
            
        }
	
		float centerDistance = width / 6;
		
		whichCanvas.drawText("A", centerX, centerY - centerDistance, paint);

		if (SHOW_CODES_HOLD_AREA) {
		    whichCanvas.drawText(holdHere, centerX - width / 2 + 2 * textSize * textMargin, centerY - height / 2 + textSize * textMargin, paintTouchAreaLeft);
            whichCanvas.drawText(holdHere, centerX + width / 2 - 2 * textSize * textMargin, centerY - height / 2 + textSize * textMargin, paintTouchAreaRight);
		}

		whichCanvas.save();
		
		whichCanvas.rotate(90, centerX, centerY);
				
		whichCanvas.drawText("B", centerY - difference, centerX - centerDistance + difference, paint);	

        if (SHOW_CODES_HOLD_AREA) {
            whichCanvas.drawText(holdHere, centerY - difference - height / 2 + 2 * textSize * textMargin, centerX + difference - width / 2 + textSize * textMargin, paintTouchAreaLeft);
            whichCanvas.drawText(holdHere, centerY - difference + height / 2 - 2 * textSize * textMargin, centerX + difference - width / 2 + textSize * textMargin, paintTouchAreaRight);
        }
        
        if (PRINT_PROMOTION_ON_VERSO) {
            whichCanvas.drawText(auto_promotion_1, centerY - difference, centerX + difference - width / 2 + textSize * textMargin, paintPromotion);
            whichCanvas.drawText(auto_promotion_2, centerY - difference, centerX + difference - width / 2 + textSize * textMargin + textSize * PROMOTION_LINES_SPACING, paintPromotion);
        }

		whichCanvas.rotate(90, centerX, centerY);
		
		whichCanvas.drawText("C", centerX, centerY - centerDistance, paint);

		if (SHOW_CODES_HOLD_AREA) {
		    whichCanvas.drawText(holdHere, centerX - width / 2 + 2 * textSize * textMargin, centerY - height / 2 + textSize * textMargin, paintTouchAreaLeft);
            whichCanvas.drawText(holdHere, centerX + width / 2 - 2 * textSize * textMargin, centerY - height / 2 + textSize * textMargin, paintTouchAreaRight);
		}

		whichCanvas.rotate(90, centerX, centerY);

		whichCanvas.drawText("D", centerY - difference, centerX - centerDistance + difference, paint);	

        if (SHOW_CODES_HOLD_AREA) {
            whichCanvas.drawText(holdHere, centerY - difference - height / 2 + 2 * textSize * textMargin, centerX + difference - width / 2 + textSize * textMargin, paintTouchAreaLeft);
            whichCanvas.drawText(holdHere, centerY - difference + height / 2 - 2 * textSize * textMargin, centerX + difference - width / 2 + textSize * textMargin, paintTouchAreaRight);
        }

        if (PRINT_PROMOTION_ON_VERSO) {
            whichCanvas.drawText(auto_promotion_1, centerY - difference, centerX + difference - width / 2 + textSize * textMargin, paintPromotion);
            whichCanvas.drawText(auto_promotion_2, centerY - difference, centerX + difference - width / 2 + textSize * textMargin + textSize * PROMOTION_LINES_SPACING, paintPromotion);
        }

		whichCanvas.restore();
	}
	
	
	
	@TargetApi(Build.VERSION_CODES.KITKAT)
	private Uri saveTopCodesFile(PdfDocument whichDocument, String fileName) {
		
		Uri savedFileUri = null;
		
		File directory = new File(Environment.getExternalStorageDirectory() + TOPCODES_FILE_PATH);
		
		if (!directory.exists()) {
			directory.mkdirs();
		}
		
		FileOutputStream fOut = null;
		
		try {
			// Create the stream pointing at the file location
		
			File topCodesFile = new File(Environment.getExternalStorageDirectory()
			         				 + TOPCODES_FILE_PATH, fileName);
			
			fOut = new FileOutputStream(topCodesFile);
			
			// write the document content
			whichDocument.writeTo(fOut);
			
			// close the document
			whichDocument.close();
			 
			fOut.close();
			
			Toast.makeText(getApplicationContext(), fileName + " " + getResources().getText(R.string.file_saved),
					   Toast.LENGTH_SHORT).show();
			
			savedFileUri = Uri.fromFile(topCodesFile);
		
		} catch (FileNotFoundException e) {
			Toast.makeText(getApplicationContext(), getResources().getText(R.string.error_saving_pdf),
						   Toast.LENGTH_SHORT).show();
			
			e.printStackTrace();
		} catch (IOException e) {
			Toast.makeText(getApplicationContext(), getResources().getText(R.string.error_saving_pdf),
						   Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
		
		return savedFileUri;
	}

	
	
	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			
			boolean updateResult = true;
			
			String stringValue = value.toString();

			log.d(TAG, "Changed preference: " + preference.getKey() + "value: " + stringValue);
			
			if (preference instanceof ListPreference) {
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(stringValue);

				// Set the summary to reflect the new value.
				preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

			} else {
				
				// For all other preferences, set the summary to the value's
				// simple string representation.
				
				if (preference.getKey().equals("students_number")) {
					
					int studentsNum = Integer.parseInt(stringValue);
					
					if ((studentsNum < 1) || (studentsNum > 99)) {
						
						Toast.makeText(preference.getContext(), preference.getContext().getResources().getText(R.string.invalid_students_number),
								       Toast.LENGTH_LONG).show();
						
						updateResult = false;
					}
				}
				
				if (updateResult) {
					preference.setSummary(stringValue);
				}
			}
			return updateResult;
		}
	};
	
	
	
	public static void setDevelopmentMode(boolean active) {
	    
	    if (mDevelopmentMode != active) {
	    
	        mDevelopmentMode = active;
	    
            if (DEVELOPMENT_OPTIONS && mDevelopmentMode) {
                
                if (mDevelopmentFragment != null) {                
                
                    mDevelopmentFragment.addPreferencesFromResource(R.xml.pref_development);
                    
                    bindPreferenceSummaryToValue(mDevelopmentFragment.findPreference("development_validation_threshold"));
                    bindPreferenceSummaryToValue(mDevelopmentFragment.findPreference("development_show_validation"));
                } else if (mDevelopmentActivity != null) {
                    mDevelopmentActivity.addPreferencesFromResource(R.xml.pref_development);
                    
                    bindPreferenceSummaryToValue(mDevelopmentActivity.findPreference("development_validation_threshold"));
                    bindPreferenceSummaryToValue(mDevelopmentActivity.findPreference("development_show_validation"));                    
                }
            } else {
                if (mDevelopmentFragment != null) {
                    mDevelopmentFragment.getPreferenceScreen().removePreference(mDevelopmentFragment.findPreference("development_validation_threshold"));
                    mDevelopmentFragment.getPreferenceScreen().removePreference(mDevelopmentFragment.findPreference("development_show_validation"));
                } else if (mDevelopmentActivity != null) {
                    mDevelopmentActivity.getPreferenceScreen().removePreference(mDevelopmentActivity.findPreference("development_validation_threshold"));
                    mDevelopmentActivity.getPreferenceScreen().removePreference(mDevelopmentActivity.findPreference("development_show_validation"));                    
                }
            }	    
	    }
	}
	
	
	
	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */

	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Show the Up button in the action bar.
			ActionBar actionBar = getActionBar();
			
			if (actionBar != null) {
				actionBar.setDisplayHomeAsUpEnabled(true);
			}
		}
	}
	
	
	
	/**
	 * Shows the simplified settings UI if the device configuration if the
	 * device configuration dictates that a simplified, single-pane UI should be
	 * shown.
	 */
	private void setupSimplePreferencesScreen() {
	    
	    log.d(TAG, "setupSimplePreferencesScreen");
	    
		if (!isSimplePreferences(this)) {
			return;
		}

		// In the simplified UI, fragments are not used at all and we instead
		// use the older PreferenceActivity APIs.

		addPreferencesFromResource(R.xml.pref_general);

		addPreferencesFromResource(R.xml.pref_answers_log);

		addPreferencesFromResource(R.xml.pref_print_codes);
		
		// Bind the summaries of EditText/List/Dialog/Ringtone preferences to
		// their values. When their values change, their summaries are updated
		// to reflect the new value, per the Android Design guidelines.
		bindPreferenceSummaryToValue(findPreference("students_number"));
        bindPreferenceSummaryToValue(findPreference("questions_tagging"));		
		bindPreferenceSummaryToValue(findPreference("print_codes_page_format"));
		bindPreferenceSummaryToValue(findPreference("print_codes_per_page"));
		bindPreferenceSummaryToValue(findPreference("print_recto_verso_sequence"));
		
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
		    findPreference("print_codes_page_format").setEnabled(false);
		    findPreference("print_codes_per_page").setEnabled(false);
		    findPreference("print_recto_verso_sequence").setEnabled(false);
		}

		
		if (DEVELOPMENT_OPTIONS) {

		    mDevelopmentActivity = this;
            
		    if (mDevelopmentMode) {
                addPreferencesFromResource(R.xml.pref_development);
                
                bindPreferenceSummaryToValue(findPreference("development_validation_threshold"));
                bindPreferenceSummaryToValue(findPreference("development_show_validation"));
		    }
		}
	}
	
}