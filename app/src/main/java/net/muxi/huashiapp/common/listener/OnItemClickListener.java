package net.muxi.huashiapp.common.listener;

import android.view.View;

import net.muxi.huashiapp.common.data.BookSearchResult;

/**
 * Created by ybao on 16/5/4.
 * RecyclerView 的监听接口
 */
public interface OnItemClickListener {
    void onItemClick(View view, BookSearchResult.ResultsBean resultsBean);
}

