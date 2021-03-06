package net.muxi.huashiapp.ui.timeTable;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.muxi.huashiapp.Constants;
import net.muxi.huashiapp.R;
import net.muxi.huashiapp.RxBus;
import net.muxi.huashiapp.common.base.ToolbarActivity;
import net.muxi.huashiapp.event.CurWeekChangeEvent;
import net.muxi.huashiapp.util.TimeTableUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by ybao on 17/2/5.
 */



public class CurweekSetActivity extends ToolbarActivity {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.lv)
    ListView mLv;

    public static void start(Context context) {
        Intent starter = new Intent(context, CurweekSetActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_curweek_set);
        ButterKnife.bind(this);
        setTitle("选择当前周");
        String[] s = new String[Constants.WEEKS_LENGTH];
        for (int i = 0; i < Constants.WEEKS_LENGTH; i++) {
            if (i < 9) {
                s[i] = String.format("第0%d周", i + 1);
            } else {
                s[i] = String.format("第%d周", i + 1);
            }
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
                R.layout.item_curweek_set, s);
        mLv.setDivider(null);
        mLv.setAdapter(arrayAdapter);
        mLv.setOnItemClickListener((adapterView, view, i, l) -> {
            TimeTableUtil.saveCurWeek(i + 1);
            RxBus.getDefault().send(new CurWeekChangeEvent());
            CurweekSetActivity.this.finish();
        });
    }
}
