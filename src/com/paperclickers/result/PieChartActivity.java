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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import com.paperclickers.R;
import com.paperclickers.log;
import com.paperclickers.fiducial.PaperclickersScanner;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.utils.ValueFormatter;

import com.paperclickers.MainActivity;


public class PieChartActivity extends Activity {

	static String TAG          = "PieChartActivity";
	
	//Information about the saved logfile
	static String LOG_FILENAME = "AnswersLog.csv";
	static String LOG_FILEPATH = "/PaperClickers";

	private HashMap<Integer, String> mDetectedAnswers;

	private PieChart mChart;
	private Typeface mTypeFace;
	private AnswersLog mAnswersLogHandler;

	int[] mColorsValue = { PaperclickersScanner.COLOR_A, 
						   PaperclickersScanner.COLOR_B, 
						   PaperclickersScanner.COLOR_C, 
						   PaperclickersScanner.COLOR_D };

    
    
    private int calculateTotal(int[] data_values) {
        int total = 0;

        for (int datum : data_values)
            total += datum;

        return total;
    }

	
	
	public static boolean checkIfAnswersLogExists() {
		File answersLog = new File(Environment.getExternalStorageDirectory() 
								   + LOG_FILEPATH + LOG_FILENAME);
		
		return answersLog.exists();
	}
	
	
    
    private int[] countAnswers() {

        int answerA = 0, answerB = 0, answerC = 0, answerD = 0;

        for (Object o : mDetectedAnswers.values()) {

            if (o != null) {
                if (o.equals(PaperclickersScanner.ANSWER_A)) {
                    answerA++;
                } else if (o.equals(PaperclickersScanner.ANSWER_B)) {
                    answerB++;
                } else if (o.equals(PaperclickersScanner.ANSWER_C)) {
                    answerC++;
                } else if (o.equals(PaperclickersScanner.ANSWER_D)) {
                    answerD++;
                }
            }
        }

        int count[] = { answerA, answerB, answerC, answerD };

        return count;
    }

    
    
    private void nextActivity() {
        
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        i.putExtra("restartingInternally", true);
        
        startActivity(i);
    }
    
    
    
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.pie_chart);

		mAnswersLogHandler = new AnswersLog(getApplicationContext());

		Intent i = getIntent();
		
		Serializable obj = i.getSerializableExtra("detectedAnswers");
		
		if (obj != null) {
			mDetectedAnswers = (HashMap<Integer, String>) obj;
		} else {
			log.e(TAG, "PieChart activity called with no codes hashmap!!!");
			
			finish();
		}				
		
		// pie chart parameters
		final int data_values[] = countAnswers();
		
		// get Total textView
		TextView total = (TextView) findViewById(R.id.txtTotal);
		int tt = calculateTotal(data_values);
		
		total.setText(Integer.toString(tt) + " " + getResources().getText(R.string.pie_chart_answers));

		
		Button finish = (Button) findViewById(R.id.button_finish);
		finish.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mAnswersLogHandler.createAndWriteLog(mDetectedAnswers);
				
				nextActivity();
			}
		});

		
		Button back = (Button) findViewById(R.id.button_back);
		back.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		
		mChart = (PieChart) findViewById(R.id.chart1);
		
		mChart.setUsePercentValues(true);
		mChart.setDescription("");

		mTypeFace = Typeface.createFromAsset(getAssets(), "Roboto-Medium.ttf");

		mChart.setCenterTextTypeface(Typeface.createFromAsset(getAssets(), "Roboto-Medium.ttf"));

		mChart.setDrawHoleEnabled(false);
		mChart.setTransparentCircleRadius(Color.WHITE);

		setData(4, data_values);

		Legend l = mChart.getLegend();

		l.setEnabled(false);
	}


	
	@Override
	public void onBackPressed() {
		
		finish();
	}


    
    private void setData(int count, final int[] data_values) {

        ArrayList<Entry> yVals1 = new ArrayList<Entry>();

        // IMPORTANT: In a PieChart, no values (Entry) should have the same
        // xIndex (even if from different DataSets), since no values can be
        // drawn above each other.
        
        for (int i = 0; i < count; i++) {
            if (data_values[i] != 0) {
                yVals1.add(new Entry(data_values[i], i));
            }
        }

        ArrayList<String> xVals = new ArrayList<String>();

        for (int i = 0; i < count; i++) {
            if (data_values[i] != 0) {

                String value = PaperclickersScanner.translateOrientationIDToString(i % PaperclickersScanner.NUMBER_OF_VALID_ANSWERS);
                xVals.add(value);
            }
        }

        PieDataSet dataSet = new PieDataSet(yVals1, "");
        dataSet.setSelectionShift(5f);

        // add a lot of colors
        ArrayList<Integer> colors = new ArrayList<Integer>();

        for (int i = 0; i < count; i++) {
            if (data_values[i] != 0) {
                colors.add(mColorsValue[i]);
            }
        }

        dataSet.setColors(colors);

        PieData data = new PieData(xVals, dataSet);

        data.setValueFormatter(new ValueFormatter() {
            
            @Override
            public String getFormattedValue(float value) {
                String newValue = null;

                newValue = ((int) value) + "\u200A%";

                return newValue;
            }
        });

        data.setValueTextSize(16f);
        data.setValueTextColor(Color.BLACK);
        data.setValueTypeface(mTypeFace);
        
        mChart.setData(data);

        // undo all highlights
        mChart.highlightValues(null);

        mChart.invalidate();
    }
}
