package com.blakequ.androidblemanager.widget;

import android.content.Context;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Found at http://stackoverflow.com/questions/7814017/is-it-possible-to-disable-scrolling-on-a-viewpager.
 * Convenient way to temporarily disable ViewPager navigation while interacting with ImageView.
 * 
 * Julia Zudikova
 */

/**
 * 
 *  * <br>在使用的时候注意几个问题：
 * <br>1.让ViewPager不自动销毁，一直保持在内存setOffscreenPageLimit(保持的个数)
 * <br>2.禁止ViewPager滑动，使用库ScrollViewPager调用setLocked方法(此时使用setCurrentItem切换)
 * <br>
 * Hacky fix for Issue #4 and
 * http://code.google.com/p/android/issues/detail?id=18990
 * <p/>
 * ScaleGestureDetector seems to mess up the touch events, which means that
 * ViewGroups which make use of onInterceptTouchEvent throw a lot of
 * IllegalArgumentException: pointerIndex out of range.
 * <p/>
 * There's not much I can do in my code for now, but we can mask the result by
 * just catching the problem and ignoring it.
 *
 * @author Chris Banes
 */
public class ScrollViewPager extends ViewPager {

	private boolean isLocked;
	
    public ScrollViewPager(Context context) {
        super(context);
        isLocked = false;
    }

    public ScrollViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        isLocked = false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
    	if (!isLocked) {
	        try {
	            return super.onInterceptTouchEvent(ev);
	        } catch (IllegalArgumentException e) {
	            e.printStackTrace();
	            return false;
	        }
    	}
    	return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isLocked) {
            return super.onTouchEvent(event);
        }
        return false;
    }
    
    /**
     * reset the scroll status
     * <p>Title: toggleLock
     * <p>Description:
     */
	public void toggleLock() {
		isLocked = !isLocked;
	}

	/**
	 * add lock to refuse or allow scroll view pager
	 * <p>Title: setLocked
	 * <p>Description: 
	 * @param isLocked
	 */
	public void setLocked(boolean isLocked) {
		this.isLocked = isLocked;
	}

	public boolean isLocked() {
		return isLocked;
	}
	
}
