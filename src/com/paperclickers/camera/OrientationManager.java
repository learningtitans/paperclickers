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

package com.paperclickers.camera;

import android.content.Context;
import android.view.OrientationEventListener;

public class OrientationManager extends OrientationEventListener {

    public enum ScreenOrientation {
        REVERSED_LANDSCAPE, LANDSCAPE, PORTRAIT, REVERSED_PORTRAIT
    }

    
    
    public ScreenOrientation screenOrientation; 
    private OrientationListener listener;

    
    
    public OrientationManager(Context context, int rate, OrientationListener listener) {
        super(context, rate);
        setListener(listener);
    }

    
    
    public OrientationManager(Context context, int rate) {
        super(context, rate);
    }

    
    
    public OrientationManager(Context context) {
        super(context);
    }

    
    
    @Override
    public void onOrientationChanged(int orientation) {
        
        if (orientation == -1){
            return;
        }
        
        ScreenOrientation newOrientation;
        
        if (orientation >= 60 && orientation <= 140){
            newOrientation = ScreenOrientation.REVERSED_LANDSCAPE;
        } else if (orientation >= 140 && orientation <= 220) {
            newOrientation = ScreenOrientation.REVERSED_PORTRAIT;
        } else if (orientation >= 220 && orientation <= 300) {
            newOrientation = ScreenOrientation.LANDSCAPE;
        } else {
            newOrientation = ScreenOrientation.PORTRAIT;                    
        }
        
        if(newOrientation != screenOrientation){
            screenOrientation = newOrientation;
            if(listener != null){
                listener.onOrientationChange(screenOrientation);
            }           
        }
    }

    
    
    public void setListener(OrientationListener listener){
        this.listener = listener;
    }

    
    
    public ScreenOrientation getScreenOrientation(){
        return screenOrientation;
    }

    
    
    public interface OrientationListener {

        public void onOrientationChange(ScreenOrientation screenOrientation);
    }
}