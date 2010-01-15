package i4nc4mp.customLock;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

//copied direct from launcher source - not used yet
//looks like this extension is just to add their long press functionality. i view that as cosmetic only
public class LockscreenAppWidgetHostView extends AppWidgetHostView {
    private boolean mHasPerformedLongPress;
    
    private CheckForLongPress mPendingCheckForLongPress;
    
    private LayoutInflater mInflater;
    
    public LockscreenAppWidgetHostView(Context context) {
        super(context);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    
    /*
    @Override
    protected View getErrorView() {
        return mInflater.inflate(R.layout.appwidget_error, this, false);
    }
	*/
    
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Consume any touch events for ourselves after longpress is triggered
        if (mHasPerformedLongPress) {
            mHasPerformedLongPress = false;
            return true;
        }
            
        // Watch for longpress events at this level to make sure
        // users can always pick up this widget
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                postCheckForLongClick();
                break;
            }
            
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mHasPerformedLongPress = false;
                if (mPendingCheckForLongPress != null) {
                    removeCallbacks(mPendingCheckForLongPress);
                }
                break;
        }
        
        // Otherwise continue letting touch events fall through to children
        return false;
    }
    
    class CheckForLongPress implements Runnable {
        private int mOriginalWindowAttachCount;

        public void run() {//if((mParent != null) it can't resolve mParent and I can't find what it is
            if ((hasWindowFocus()
                    && mOriginalWindowAttachCount == getWindowAttachCount()
                    && !mHasPerformedLongPress)) {
                if (performLongClick()) {
                    mHasPerformedLongPress = true;
                }
            }
        }

        public void rememberWindowAttachCount() {
            mOriginalWindowAttachCount = getWindowAttachCount();
        }
    }

    private void postCheckForLongClick() {
        mHasPerformedLongPress = false;

        if (mPendingCheckForLongPress == null) {
            mPendingCheckForLongPress = new CheckForLongPress();
        }
        mPendingCheckForLongPress.rememberWindowAttachCount();
        postDelayed(mPendingCheckForLongPress, ViewConfiguration.getLongPressTimeout());
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mHasPerformedLongPress = false;
        if (mPendingCheckForLongPress != null) {
            removeCallbacks(mPendingCheckForLongPress);
        }
    }
}