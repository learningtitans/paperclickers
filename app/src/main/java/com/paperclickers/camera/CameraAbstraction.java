/*
 * Paperclickers - Affordable solution for classroom response system.
 *
 * Copyright (C) 2015-2017 Eduardo Valle Jr <dovalle@dca.fee.unicamp.br>
 * Copyright (C) 2015-2017 Eduardo Seiti de Oliveira <eduseiti@dca.fee.unicamp.br>
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

import android.app.Activity;
import android.content.pm.ActivityInfo;

import com.paperclickers.camera.OrientationManager.OrientationListener;
import com.paperclickers.camera.OrientationManager.ScreenOrientation;

/**
 * Created by eduseiti on 10/06/17.
 */

public class CameraAbstraction extends Activity implements OrientationManager.OrientationListener {


    @Override
    public void onOrientationChange(ScreenOrientation screenOrientation) {

        switch(screenOrientation){
            case PORTRAIT:
            case REVERSED_PORTRAIT:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;

            case LANDSCAPE:
            case REVERSED_LANDSCAPE:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
        }
    }


}

