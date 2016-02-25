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

import android.util.Log;


public class log {
	
	static final boolean LOG_ENABLED = true;
	
	
	
    /**
     * Send a {@link #DEBUG} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int d(String tag, String msg) {
    	
    	int writtenBytes = 0;
    	
    	if (LOG_ENABLED) {
    		writtenBytes = Log.d(tag, msg);
    	}
    	
        return writtenBytes;
    }

    
    
    /**
     * Send a {@link #DEBUG} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int d(String tag, String msg, Throwable tr) {
    	
    	int writtenBytes = 0;
    	
    	if (LOG_ENABLED) {
    		writtenBytes = Log.d(tag, msg, tr);
    	}
    	
        return writtenBytes;
    }
	
	
    
    /**
     * Send an {@link #ERROR} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int e(String tag, String msg) {
    	
    	int writtenBytes = 0;
    	
    	if (LOG_ENABLED) {
    		writtenBytes = Log.e(tag, msg);
    	}
    	
        return writtenBytes;
    }

    
    
    /**
     * Send a {@link #ERROR} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int e(String tag, String msg, Throwable tr) {
    	
    	int writtenBytes = 0;
    	
    	if (LOG_ENABLED) {
    		writtenBytes = Log.e(tag, msg, tr);
    	}
    	
        return writtenBytes;
    }
    
    
   
	private log() {
	}
}
