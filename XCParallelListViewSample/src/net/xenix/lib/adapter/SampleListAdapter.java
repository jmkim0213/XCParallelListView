package net.xenix.lib.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class SampleListAdapter extends BaseAdapter {
	private Context mContext;
	
	public SampleListAdapter(Context context) {
		mContext = context;
	}
	
	@Override
	public int getCount() {
		return 100;
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView textView = (TextView)convertView;
		if ( textView == null ) {
			textView = new TextView(mContext);
			textView.setTextSize(15);
		}
		
		textView.setText(getItem(position).toString());
		
		return textView;
	}

}
