package com.appknot.gtr_test;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class ThumbnailItemDecoration extends RecyclerView.ItemDecoration {

    private int spanCount;
    private int spacing;
    private boolean includeEdge;

    public ThumbnailItemDecoration(int spanCount, int spacing, boolean includeEdge) {
        this.spanCount = spanCount;
        this.spacing = spacing;
        this.includeEdge = includeEdge;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view); // item position
        int column = position % spanCount; // item column

        if (includeEdge) {
            outRect.left = spacing - column * spacing / spanCount; // rvDecorationspacing - column * ((1f / rvDecorationspanCount) * rvDecorationspacing)
            outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / rvDecorationspanCount) * rvDecorationspacing)

            if (position < spanCount) { // top edge
                outRect.top = spacing;
            }
            outRect.bottom = spacing; // item bottom
        } else {
            outRect.left = column * spacing / spanCount; // column * ((1f / rvDecorationspanCount) * rvDecorationspacing)
            outRect.right = spacing - (column + 1) * spacing / spanCount; // rvDecorationspacing - (column + 1) * ((1f /    rvDecorationspanCount) * rvDecorationspacing)
            if (position >= spanCount) {
                outRect.top = spacing / 2; // item top
            }
        }
    }
}