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

import java.util.List;
import java.util.Map.Entry;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.paperclickers.R;
import com.paperclickers.log;
import com.paperclickers.fiducial.PaperclickersScanner;

public class GridViewAdapter extends BaseAdapter {
    
	private static final String TAG = "GridViewAdapter";
	
	private LayoutInflater mInflater;
	private List<Entry> mTopCodes;
	Context mContext;

    
    
    @Override
    public int getCount() {
        return mTopCodes.size();
    }

	
	
	@Override
	public Object getItem(int position) {
		return mTopCodes.get(position);
	}

	
	
	@Override
	public long getItemId(int position) {
		return position;
	}

	
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View view;

		log.d(TAG, "getView. Position: " + position);
		
		
		if (convertView == null) {
			view = mInflater.inflate(R.layout.row_grid, null);
		} else {
			view = convertView;
		}

		ImageView imageview = (ImageView) view.findViewById(R.id.imgAnswer);
		TextView textView   = (TextView) view.findViewById(R.id.imgText);

		Entry topCode = mTopCodes.get(position);


		String answer = (String) topCode.getValue();
		
		log.d(TAG, "answer: " + answer);

		int whichColor = Color.WHITE;
				
		if (answer.equals(PaperclickersScanner.NO_ANSWER_STRING)) {
			imageview.setImageResource(R.drawable.answer_notdetected);
			
			whichColor = Color.BLACK;
			
		} else if (answer.equals(PaperclickersScanner.ANSWER_A)) {
			imageview.setImageResource(R.drawable.answer_a);
			
		} else if (answer.equals(PaperclickersScanner.ANSWER_B)) {
			imageview.setImageResource(R.drawable.answer_b);
			
		} else if (answer.equals(PaperclickersScanner.ANSWER_C)) {
			imageview.setImageResource(R.drawable.answer_c);
			
		} else{
			imageview.setImageResource(R.drawable.answer_d);
		}

		textView.setTextColor(whichColor);
		textView.setText((String) topCode.getKey().toString());

		return view;
	}

    
    
    public GridViewAdapter(Context context, List<Entry> topCodes) {

        this.mContext  = context;
        this.mTopCodes = topCodes;

        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
}