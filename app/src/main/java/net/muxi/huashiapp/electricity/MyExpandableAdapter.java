package net.muxi.huashiapp.electricity;


import android.content.Context;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import net.muxi.huashiapp.R;
import net.muxi.huashiapp.common.util.Logger;

/**
 * Created by december on 16/6/29.
 */
public class MyExpandableAdapter extends BaseExpandableListAdapter {

    private String[][] mChildStrings = new String[2][19];
    private String[] mGroupStrings = new String[2];

    private OnRbClickListener mOnRbClickListener;

    private AppCompatRadioButton childRb1;
    private AppCompatRadioButton childRb2;


    private Context mContext;

    public MyExpandableAdapter(Context context, String[] groupStrings, String[][] childStrings) {
        mContext = context;
        mGroupStrings = groupStrings;
        mChildStrings = childStrings;
    }

    public void updateData(String[] groupStrings, String[][] childStrings) {
        mGroupStrings = groupStrings;
        mChildStrings = childStrings;
        notifyDataSetChanged();
    }


    //获取分组个数
    @Override
    public int getGroupCount() {
        Logger.d(mGroupStrings.length + "");
        return mGroupStrings.length;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        Logger.d("getchildcount");
        Logger.d(mChildStrings[groupPosition].length + "");
        return mChildStrings[groupPosition].length;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mGroupStrings[groupPosition];
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mChildStrings[groupPosition][childPosition];
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        GroupViewHolder groupViewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_expand_group, parent, false);
            groupViewHolder = new GroupViewHolder();
            groupViewHolder.tvTitle = (TextView) convertView.findViewById(R.id.label_expand_group);
            convertView.setTag(groupViewHolder);
        } else {
            groupViewHolder = (GroupViewHolder) convertView.getTag();
        }
        groupViewHolder.tvTitle.setText(mGroupStrings[groupPosition]);
        Logger.d(groupPosition + "");
        Logger.d(mGroupStrings[groupPosition]);
        return convertView;

    }


    @Override
    public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, final ViewGroup parent) {
        final ChildViewHolder childViewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_expand_child, parent, false);
            childViewHolder = new ChildViewHolder();
            childViewHolder.tvTitle = (TextView) convertView.findViewById(R.id.tv_child);
            childViewHolder.mRadioButton = (AppCompatRadioButton) convertView.findViewById(R.id.rb_child);
            convertView.setTag(childViewHolder);
        } else {
            childViewHolder = (ChildViewHolder) convertView.getTag();
        }
        childViewHolder.tvTitle.setText(mChildStrings[groupPosition][childPosition]);
        childViewHolder.mRadioButton.setChecked(false);
        if (childRb1 != null){
            childRb1.setChecked(true);
        }
        if (childRb2 != null){
            childRb2.setChecked(true);
        }
        childViewHolder.mRadioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.d(groupPosition + "");
                if (groupPosition == 0) {
                    if (childRb1 != null && childRb1 != childViewHolder.mRadioButton) {
                        childRb1.setChecked(false);
                    }
                    if (childRb2 != null){
                        childRb2.setChecked(false);
                    }
                    childRb1 = childViewHolder.mRadioButton;
                } else {
                    if (childRb2 != null && childRb2 != childViewHolder.mRadioButton) {
                        childRb2.setChecked(false);
                    }
                    childRb2 = childViewHolder.mRadioButton;
                }
                mOnRbClickListener.onRbClick(groupPosition, childPosition);
            }
        });
//        childViewHolder.mCheckBox.setText(childStrings[groupPosition][childPosition]);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }


    static class GroupViewHolder {
        TextView tvTitle;
    }

    static class ChildViewHolder {
        TextView tvTitle;
        AppCompatRadioButton mRadioButton;
    }

    public void setOnRbClickListener(OnRbClickListener onRbClickListener) {
        mOnRbClickListener = onRbClickListener;
    }

    //child view radio button click listener
    public interface OnRbClickListener {
        void onRbClick(int groupPosition, int rbPosition);
    }

}
