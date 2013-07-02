/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.xenix.lib.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * 
 * @author Kim Young Soo (yskim6217@gmail.com)
 * @date 2013. 07. 03.
 * @version 1.0
 */
public class XCParallelListView extends ListView {
    private int mLeft;
    private int mTop;
    private int mRight;
    private int mBottom;
    
    private int mLastListScrollTop;
    
    
    private Drawable mParallelDrawable;
    private int mParallelDrawableWidth;
    private int mParallelDrawableHeight;
	private int mParallelDrawableDefaultTop;

	private int mParallelViewPortMinHeight = -1;
	private int mParallelViewPortMaxHeight = -1;
	private int mParallelNormalizeMaxValue;
	
	private float mLastRawY;
	private boolean mPulling;

	// SmoothScroll
	private int mMoveYEndPosition = 0;
	private boolean mSmoothYScrolling = false;
	private final Handler mSmoothScrollHandler = new Handler();
	
	private View mParallelHeaderView;
	
	// Listener
	private OnScrollListener mOnScrollListener;
	private OnItemClickListener mOnItemClickListener;
	private OnItemSelectedListener mOnItemSelectedListener;
	
	private OnRefreshListner mOnRefreshListner;
	private OnChangeParallelViewPortListner mOnChangeParallelViewPortListner;
	
    public XCParallelListView(Context context) {
        super(context);
        init(context);
    }
    
    public XCParallelListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public XCParallelListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
    	
       	// Listener Wrap
    	super.setOnScrollListener(mInnerScrollListener);
    	super.setOnItemClickListener(mInnerItemClickListener);
    	super.setOnItemSelectedListener(mInnerItemSelectedListener);
    	super.setOnItemLongClickListener(mInnerItemLongClickListener);
    	
        mParallelHeaderView = new View(context);
        super.addHeaderView(mParallelHeaderView, null, false);
        setParallelViewPortMinHeight(0);
        super.setHapticFeedbackEnabled(false);
    }

    // TODO Override
    @Override
    protected void drawableStateChanged() {
    	super.drawableStateChanged();
        if (mParallelDrawable != null && mParallelDrawable.isStateful()) {
        	mParallelDrawable.setState(getDrawableState());
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    	super.onLayout(changed, left, top, right, bottom);
    	
    	mLeft 	= left;
    	mTop 	= top;
        mRight 	= right;
        mBottom = bottom;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    	super.onSizeChanged(w, h, oldw, oldh);
    	
    	if ( mParallelDrawable == null ) 
    		return ;
    	
    	int drawableWidth  = mParallelDrawable.getIntrinsicWidth();
    	int drawableHeight = mParallelDrawable.getIntrinsicHeight();
    	
    	int viewWidth = w;
    	
    	if ( drawableWidth > viewWidth ) {
    		float ratio = drawableWidth / (float)viewWidth;
    		mParallelDrawableWidth  = viewWidth;
    		mParallelDrawableHeight = (int)(drawableHeight / ratio);
    	}
    	else {
    		mParallelDrawableWidth = drawableWidth;
    		mParallelDrawableHeight = drawableHeight;
    	}
    	calulateParallelDrawableDefaultTop();
    }

    @Override
    public void draw(Canvas canvas) {
        if ( mParallelDrawable != null ) {
        	canvas.save();
            final Drawable parallel = mParallelDrawable;

            final int drawableWidth  = mParallelDrawableWidth;
   
            int viewWidth  = mRight - mLeft;
            int scrollY = Math.abs(getScrollY());
            
            // Image Height
            int clipHeight = mParallelViewPortMinHeight + scrollY;
            
			int normalizeParallelTop = (int)((float)-scrollY * (float)mParallelNormalizeMaxValue / (float)mParallelViewPortMaxHeight);
			
			int parallelLeft = (int)((viewWidth - drawableWidth) / 2.0F);
			int parallelRight = parallelLeft + mParallelDrawableWidth;
			
			int parallelTop = -scrollY - normalizeParallelTop + mParallelDrawableDefaultTop + mLastListScrollTop;
			int parallelBottom = parallelTop + mParallelDrawableHeight;
			

			
			parallel.setBounds(
            		parallelLeft, 
            		parallelTop, 
            		parallelRight, 
            		parallelBottom);
			
            int locationY = -scrollY + mLastListScrollTop;
            canvas.clipRect(mLeft, locationY, mRight, clipHeight + locationY);
            
            parallel.draw(canvas);
            canvas.restore();
        }
       
        super.draw(canvas);
    }
    
    
    
	@Override
	public boolean onTouchEvent(MotionEvent event) {

		onChangeParallelViewPort();
		
		switch ( event.getAction() ) {
		case MotionEvent.ACTION_DOWN:
			cancelSmoothScrollY();
			mLastRawY = event.getRawY();
			mPulling = false;

			break;
			
		case MotionEvent.ACTION_MOVE:

			float rawY = event.getRawY();
			float diff = rawY - mLastRawY;
			
			// 아래로 당김.
			if ( diff > 0 ) {				
				if ( mLastListScrollTop == 0 ) {
					mPulling = true;
					
					int viewHeight = mTop + mBottom;
					int normalizeScrollY = (int)((float)-diff * (float)mParallelNormalizeMaxValue / (float)viewHeight);
					
					scrollBy(0, normalizeScrollY);
				}
			}
			
			// 위로 올림
			else if ( diff < 0 ) {
				mPulling = true;
				int scrollY = getScrollY();
				final int afterScrollY = scrollY - (int)diff;

				if ( 0 <= afterScrollY ) {
					mPulling = false;
					scrollTo(0, 0);
				}
				else {
					scrollTo(0, afterScrollY);

				}
			}
			
			mLastRawY = rawY;
			
			{
				int currentScrollY = -getScrollY();
				int currentViewPortHeight = mParallelViewPortMinHeight + currentScrollY;
				float remainViewPortHeight = (float)mParallelNormalizeMaxValue / currentViewPortHeight;
	
				if ( mOnRefreshListner != null ) {
					if ( remainViewPortHeight < 0.7F ) {
						mOnRefreshListner.onRefreshable(true);
					}
					else {
						mOnRefreshListner.onRefreshable(false);
					}
				}
			}
		
			break;
			
		case MotionEvent.ACTION_UP:
			int currentScrollY = -getScrollY();
			
			int currentViewPortHeight = mParallelViewPortMinHeight + currentScrollY;
			float remainViewPortHeight = (float)mParallelNormalizeMaxValue / currentViewPortHeight;


			if ( remainViewPortHeight < 0.7F ) {
				if ( mOnRefreshListner != null ) {
					mOnRefreshListner.onRefresh();
				}
			}
			
			if ( currentScrollY > 0 )
				smoothScrollY(0);
		}
		
		
		if ( mPulling ) {
			return true;
		}
		else {
			return super.onTouchEvent(event);
		}
	}
	
	
	@Override
	public void setOnItemClickListener(OnItemClickListener listener) {
		mOnItemClickListener = listener;
	}
	
	
	@Override
	public void setOnItemSelectedListener(OnItemSelectedListener listener) {
		mOnItemSelectedListener = listener;
	}
	
	@Override
	public void setOnScrollListener(OnScrollListener listener) {
		mOnScrollListener = listener;
	}
	

	
	// TODO deprecate
	@Deprecated
	@Override
	public void setHapticFeedbackEnabled(boolean hapticFeedbackEnabled) {
		throw new UnsupportedOperationException("setHapticFeedbackEnabled(boolean hapticFeedbackEnabled) is not supported");
	}
	
	@Deprecated
	@Override
	public void addHeaderView(View v) {
		throw new UnsupportedOperationException("addHeaderView(View v) is not supported");
	}
	
	@Deprecated
	@Override
	public void addHeaderView(View v, Object data, boolean isSelectable) {
		throw new UnsupportedOperationException("addHeaderView(View v, Object data, boolean isSelectable) is not supported");
	}
	
	@Deprecated
	@Override
	public void setOnItemLongClickListener(OnItemLongClickListener listener) {
		throw new UnsupportedOperationException("setOnItemLongClickListener(OnItemLongClickListener listener) is not supported");
	}
    
    
    // TODO public method
	public void setOnRefreshListner(OnRefreshListner listener) {
		mOnRefreshListner = listener;
	}
	
	public void setOnChangeParallelViewPortListner(OnChangeParallelViewPortListner listener) {
		mOnChangeParallelViewPortListner = listener;
	}
	
	public void setParallelHeaderView(View view) {
		if ( view == null || view.equals(mParallelHeaderView) ) 
			return ;
		
		int headerCount = getHeaderViewsCount();
		if ( mParallelHeaderView != null && headerCount > 0 )
    		removeHeaderView(mParallelHeaderView);
		
		
    	mParallelHeaderView = view;
    	
    	if ( mParallelHeaderView != null ) {
			mParallelHeaderView.setClickable(true);
			mParallelHeaderView.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, mParallelViewPortMinHeight));
	
	    	super.addHeaderView(mParallelHeaderView, null, false);
    	}
    }
	
	
	
	
    public void setParallelImageDrawable(Drawable drawable) {
        if (mParallelDrawable != drawable) {
            if (mParallelDrawable != null) {
            	mParallelDrawable.setCallback(null);
                unscheduleDrawable(mParallelDrawable);
            }

            mParallelDrawable = drawable;
            
            if (drawable != null) {
            	mParallelDrawableWidth  = mParallelDrawable.getIntrinsicWidth();
            	mParallelDrawableHeight = mParallelDrawable.getIntrinsicHeight(); 
            	
            	int viewWidth = mRight - mLeft;
            	
            	if ( mParallelDrawableWidth > viewWidth ) {
            		float ratio = mParallelDrawableWidth / (float)viewWidth;
            		mParallelDrawableWidth = viewWidth;
            		mParallelDrawableHeight = (int)(mParallelDrawableHeight / ratio);
            	}
            	
            	
            	
                setWillNotDraw(false);
                drawable.setCallback(this);
                
                if (drawable.isStateful()) {
                    drawable.setState(getDrawableState());
                }
                
                calulateParallelDrawableDefaultTop();
                
            }  
            else {
                setWillNotDraw(true);
            }
            requestLayout();
            invalidate();
        }
    }
    
    public void setParallelViewPortMinHeight(int height) {
    	if ( mParallelViewPortMinHeight == height ) 
    		return ;
    	
    	mParallelViewPortMinHeight = height;
    	mParallelViewPortMaxHeight = mParallelViewPortMinHeight * 2;

    	
    	calulateNormalizeMaxValue();
    	calulateParallelDrawableDefaultTop();
    	
    	if ( mParallelHeaderView != null ) {
    		mParallelHeaderView.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, mParallelViewPortMinHeight));
    	}
    }

    
    public void setParallelImageResource(int res){
    	Drawable d = getResources().getDrawable(res);
    	
    	setParallelImageDrawable(d);
    }
    
    public void setParallelImageBitmap(Bitmap bitmap){
    	if ( bitmap == null ) {
    		setParallelImageDrawable(null);
    		return ;
    	}
    	
    	BitmapDrawable d = new BitmapDrawable(getResources(), bitmap);
    	setParallelImageDrawable(d);
    }

    public Drawable getParallelDrawable() {
        return mParallelDrawable;
    }

	
    // TODO private method
    private void calulateParallelDrawableDefaultTop() {    	
    	mParallelDrawableDefaultTop = (int)-((mParallelDrawableHeight - mParallelNormalizeMaxValue) / (mParallelViewPortMaxHeight / (float)mParallelViewPortMinHeight));
    }
    
    private void calulateNormalizeMaxValue() {
    	mParallelNormalizeMaxValue = mParallelViewPortMaxHeight - mParallelViewPortMinHeight;
    }
    
    private void onChangeParallelViewPort() {
    	int currentScrollY = -getScrollY();
		int currentViewPortHeight = mParallelViewPortMinHeight + currentScrollY;
		
    	if ( mOnChangeParallelViewPortListner != null ) {
    		mOnChangeParallelViewPortListner.onChangeParallelViewPort(currentViewPortHeight, mParallelViewPortMinHeight, mParallelViewPortMaxHeight);
    	}
    }
	
	 // TODO SmoothScroll
    private void smoothScrollY(int y) {
    	mSmoothYScrolling = true;
    	mMoveYEndPosition = y;
    	mSmoothScrollHandler.postDelayed(mSmoothScrollRunable, 10);
    }
    
    private void endSmoothScrollY() {
    	mSmoothYScrolling = false;
    	mSmoothScrollHandler.removeCallbacks(mSmoothScrollRunable);
    }
    
    private void cancelSmoothScrollY() {
    	if ( mSmoothYScrolling ) {
			mSmoothYScrolling = false;
			mSmoothScrollHandler.removeCallbacks(mSmoothScrollRunable);
		}
    }
    
    // mLastViewScrollY를 사용하면 오작동.
    private Runnable mSmoothScrollRunable = new Runnable() {
		
		@Override
		public void run() {

			float start = getScrollY();
			if ( start != mMoveYEndPosition ) {
				float acceletor = (float) Math.ceil( Math.abs(mMoveYEndPosition-start) / 5);
				if ( mMoveYEndPosition < start ) 
					acceletor = -acceletor;
				
				scrollBy(0, (int)acceletor);
			}
			else {
				mSmoothYScrolling = false;
			}
			
			if ( mSmoothYScrolling ) {
				if ( mMoveYEndPosition == Math.floor(getScrollY()) ) { 
					scrollTo(0, mMoveYEndPosition);
				}
				
				mSmoothScrollHandler.postDelayed(mSmoothScrollRunable, 10);
			}
			else {
				endSmoothScrollY();
			}
			
			onChangeParallelViewPort();
		}
	};
	



	private OnScrollListener mInnerScrollListener = new OnScrollListener() {
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			if ( mOnScrollListener != null ) {
				mOnScrollListener.onScrollStateChanged(view, scrollState);
			}
		}
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			
			if ( view.getChildCount() > 0 ) {
				View visibleTopView = view.getChildAt(0);
				int visibleTop = (visibleTopView == null) ? 0 : (visibleTopView.getTop() - getPaddingTop());

				if ( firstVisibleItem == 0 ) {
					mLastListScrollTop = visibleTop;
				}	
			
				else if ( firstVisibleItem >= 1 ) {
					mLastListScrollTop = -mParallelViewPortMinHeight + visibleTop;
				} 
			}
			
			if ( mOnScrollListener != null ) {
				int headerCount = getHeaderViewsCount();
				if ( firstVisibleItem > 0 ) {
					firstVisibleItem = firstVisibleItem - headerCount;
				}
				
				visibleItemCount = visibleItemCount - headerCount;
				totalItemCount = totalItemCount - headerCount;
				
				
				mOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
			}
		}
	};
	
	private OnItemClickListener mInnerItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
			view.setPressed(false);
			if ( mOnItemClickListener != null ) {
				position = position - getHeaderViewsCount();
				if ( position >= 0 ) {
					mOnItemClickListener.onItemClick(adapter, view, position, id);
				}
			}
		}
	};
	
	private OnItemLongClickListener mInnerItemLongClickListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> adapter, View view, int position, long id) {
			view.setPressed(false);
			return true;
		}
	};
	
	private OnItemSelectedListener mInnerItemSelectedListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> adapter, View view, int position, long id) {
			if ( mOnItemSelectedListener != null ) {
				position = position - getHeaderViewsCount();
				if ( position >= 0 ) {
					mOnItemSelectedListener.onItemSelected(adapter, view, position, id);
				}
			}	
		}

		@Override
		public void onNothingSelected(AdapterView<?> adapter) {
			if ( mOnItemSelectedListener != null ) {
				mOnItemSelectedListener.onNothingSelected(adapter);
			}
		}
	};
	
	public static interface OnRefreshListner {
		public void onRefresh();
		public void onRefreshable(boolean able);
	}
	
	public static interface OnChangeParallelViewPortListner {
		public void onChangeParallelViewPort(int currentHeight, int minHeight, int maxHeight);
	}
	
}