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

package com.paperclickers.result;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import com.paperclickers.R;
import com.paperclickers.fiducial.PaperclickersScanner;
import com.paperclickers.log;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class AnswersLog {

	final static long NO_OPEN_LOG_ENTRY = -1;

	static int mSessionQuestionsSeqNum = 0;

	static long mOpenLogEntryOffset = NO_OPEN_LOG_ENTRY;
	
	static String TAG          = "AnswersLog";
	static String LOG_FILENAME = "AnswersLog.csv";
	static String LOG_FILEPATH = "/PaperClickers";

	private Context mActivityContext = null;
	
	
	
	public AnswersLog(Context whichContext) {
		mActivityContext = whichContext;
	}
	
	
	
	public static boolean checkIfAnswersLogExists() {
		File answersLog = new File(Environment.getExternalStorageDirectory() + LOG_FILEPATH, LOG_FILENAME);
		
		return answersLog.exists();
	}
    
    
    
    void createAndWriteLog(HashMap<Integer, String> detectedAnswers) {

		log.d(TAG, "createAndWriteLog: " + mOpenLogEntryOffset);

        StringBuilder answersLog;

		if (mOpenLogEntryOffset < 0) {

			// Just increment the sequence number if there isn't an open log entry...

			mSessionQuestionsSeqNum++;
		}
        
        answersLog = new StringBuilder();
        
        answersLog.append(mSessionQuestionsSeqNum + "," + getDateTime() + ",");
        
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mActivityContext);
        
        if (preferences.getString("questions_tagging", "0").equals("1")) {
            answersLog.append(preferences.getString("question_tag", ""));
        } else {
            answersLog.append(mSessionQuestionsSeqNum);
        }

        for (int i = 0; i < com.paperclickers.SettingsActivity.validTopCodes.length; i++) {
            answersLog.append(",");
            
            String answer = detectedAnswers.get(i + 1);
            
            if (answer != null) {
                if (answer.equals(PaperclickersScanner.NO_ANSWER_STRING)) {
                    answersLog.append("");
                } else {
                    answersLog.append(answer);
                }
            }
        }

        answersLog.append(System.getProperty("line.separator"));
        
		writeToFileSeekeable(answersLog.toString());
	}
    

    
    private StringBuilder createLogFileHeader() {
        StringBuilder answersLogHeader;

        answersLogHeader = new StringBuilder();
        
        answersLogHeader.append("SEQ,TIMESTAMP,TAG");
        
        for (int i = 0; i < com.paperclickers.SettingsActivity.validTopCodes.length; i++) {
            answersLogHeader.append("," + (i + 1));
        }
        
        answersLogHeader.append(System.getProperty("line.separator"));
        
        return answersLogHeader;
    }
	

    
	public static boolean deleteAnswersLog() {

		File answersLog = new File(Environment.getExternalStorageDirectory() + LOG_FILEPATH, LOG_FILENAME);

		return answersLog.delete();
	}
	
	
	
	public static Uri getAnswersLogUri() {

		File answersLog = new File(Environment.getExternalStorageDirectory() + LOG_FILEPATH, LOG_FILENAME);
		
		return Uri.fromFile(answersLog);
	}
	
	
	
	private String getDateTime() {
	    
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
		
		Date date = new Date();
		
		return dateFormat.format(date);
	}
    


	public static String getPaperclickersFolder() {

		return Environment.getExternalStorageDirectory() + LOG_FILEPATH + "/";
	}



	public static void resetOpenLogEntry() {

		mOpenLogEntryOffset = NO_OPEN_LOG_ENTRY;
	}



    public static void resetQuestionsSequenceNumber() {
        
        mSessionQuestionsSeqNum = 0;
    }
	
	
	
	private void writeToFileSeekeable(String data) {

		RandomAccessFile fOut = null;
		File directory        = new File(Environment.getExternalStorageDirectory() + LOG_FILEPATH);

		boolean needToWriteHeader = true;


		if (!directory.exists()) {
			directory.mkdirs();
		} else {
			File logFile = new File(Environment.getExternalStorageDirectory() + LOG_FILEPATH, LOG_FILENAME);

			if (logFile.exists()) {
				needToWriteHeader = false;
			}
		}

		try {
			// Create the stream pointing at the file location
			fOut = new RandomAccessFile(new File(directory, LOG_FILENAME), "rwd");

			if (needToWriteHeader) {
				fOut.writeBytes(createLogFileHeader().toString());

				log.d(TAG, "Writing the logfile header: " + mOpenLogEntryOffset);
			}

			if (mOpenLogEntryOffset < 0) {
				mOpenLogEntryOffset = fOut.length();

				log.d(TAG, "Going to the end of file: " + mOpenLogEntryOffset);
			}

			log.d(TAG, "Seek to open log position: " + mOpenLogEntryOffset);

			fOut.seek(mOpenLogEntryOffset);

			fOut.writeBytes(data);

			fOut.setLength(fOut.getFilePointer());

			fOut.close();

			Toast.makeText(mActivityContext, mActivityContext.getResources().getText(R.string.saved_answers_log), Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			Toast.makeText(mActivityContext, mActivityContext.getResources().getText(R.string.error_saving_log), Toast.LENGTH_SHORT).show();

			e.printStackTrace();
		}
	}
}
