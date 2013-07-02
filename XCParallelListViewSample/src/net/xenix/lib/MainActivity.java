package net.xenix.lib;

import net.xenix.lib.adapter.SampleListAdapter;
import net.xenix.lib.view.XCParallelListView;
import net.xenix.lib.view.XCParallelListView.OnChangeParallelViewPortListner;
import net.xenix.lib.view.XCParallelListView.OnRefreshListner;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private TextView mTextView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		XCParallelListView listView = new XCParallelListView(this);
		setContentView(listView);
		
		Drawable parallelDrawable = getResources().getDrawable(R.drawable.androboy);
		listView.setParallelImageDrawable(parallelDrawable);
		listView.setParallelViewPortMinHeight((int)(parallelDrawable.getIntrinsicHeight() / 2.0F));
		listView.setOnItemClickListener(mItemClickListener);
		listView.setOnRefreshListner(mOnRefreshListner);
		listView.setOnChangeParallelViewPortListner(mChangeParallelViewPortListner);
		listView.setAdapter(new SampleListAdapter(this));
		
		mTextView = new TextView(this);
		mTextView.setBackgroundColor(Color.argb(125, 0, 0, 0));
		mTextView.setVisibility(View.INVISIBLE);
		mTextView.setGravity(Gravity.CENTER);
		mTextView.setTextColor(Color.WHITE);
		
		listView.setParallelHeaderView(mTextView);
	}
	
	private OnItemClickListener mItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
			Toast.makeText(MainActivity.this, "Click Position: " + position, Toast.LENGTH_SHORT).show();
		}
	};
	
	private OnRefreshListner mOnRefreshListner = new OnRefreshListner() {
		
		@Override
		public void onRefresh() {
			mTextView.setText("Refreshing...");
			
			new Handler().postDelayed(new Runnable() {
				
				@Override
				public void run() {
					mTextView.setVisibility(View.INVISIBLE);
				}
			}, 2000);
		}

		@Override
		public void onRefreshable(boolean able) {
			if ( able ) {
				mTextView.setVisibility(View.VISIBLE);
				mTextView.setText("Refreshable...");
			}
			else {
				mTextView.setVisibility(View.INVISIBLE);
			}
		}
	};
	
	private OnChangeParallelViewPortListner mChangeParallelViewPortListner = new OnChangeParallelViewPortListner() {
		
		@Override
		public void onChangeParallelViewPort(int currentHeight, int minHeight, int maxHeight) {
			
		}
	};
}
