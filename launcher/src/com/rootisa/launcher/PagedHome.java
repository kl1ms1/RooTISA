package com.rootisa.launcher;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;

/**
 * Horizontal, page-snapping scroller — the iOS-like side-swiping home surface.
 * Holds one horizontal child whose sub-views are each one screen wide.
 */
public class PagedHome extends HorizontalScrollView {

    public interface OnPageChange { void onPage(int page, int count); }

    private OnPageChange cb;

    public PagedHome(Context c) {
        super(c);
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    public void setOnPageChange(OnPageChange c) { cb = c; }

    private int pageCount() {
        int w = getWidth();
        if (w <= 0 || getChildCount() == 0) return 1;
        return Math.max(1, Math.round((float) getChildAt(0).getWidth() / w));
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int a = ev.getActionMasked();
        if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
            boolean handled = super.onTouchEvent(ev);
            snap();
            return handled;
        }
        return super.onTouchEvent(ev);
    }

    public void snap() {
        int w = getWidth();
        if (w <= 0) return;
        int page = Math.round((float) getScrollX() / w);
        int max = Math.max(0, pageCount() - 1);
        if (page < 0) page = 0;
        if (page > max) page = max;
        smoothScrollTo(page * w, 0);
        if (cb != null) cb.onPage(page, pageCount());
    }

    @Override
    protected void onScrollChanged(int l, int t, int ol, int ot) {
        super.onScrollChanged(l, t, ol, ot);
        int w = getWidth();
        if (w > 0 && cb != null) cb.onPage(Math.round((float) l / w), pageCount());
    }
}
