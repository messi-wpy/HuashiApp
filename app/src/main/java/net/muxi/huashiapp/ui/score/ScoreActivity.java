package net.muxi.huashiapp.ui.score;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.muxistudio.appcommon.Constants;
import com.muxistudio.appcommon.appbase.ToolbarActivity;
import com.muxistudio.appcommon.data.Score;
import com.muxistudio.appcommon.net.CampusFactory;
import com.muxistudio.appcommon.net.ccnu.CcnuCrawler2;
import com.muxistudio.appcommon.presenter.LoginPresenter;
import com.muxistudio.appcommon.user.UserAccountManager;
import com.muxistudio.multistatusview.MultiStatusView;

import net.muxi.huashiapp.R;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by ybao on 16/4/26.
 */
public class ScoreActivity extends ToolbarActivity {

    private ScoresAdapter mScoresAdapter;
    private List<Score> mScoresList = new ArrayList<>();
    private String year;
    private String term;
    private MultiStatusView mMultiStatusView;

    public static void start(Context context, String year, String term) {
        Intent starter = new Intent(context, ScoreActivity.class);
        starter.putExtra("year", year);
        starter.putExtra("term", term);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score);
        initView();
        year = getIntent().getStringExtra("year");
        term = getIntent().getStringExtra("term");
        if (term == null) {
            term = "0";
        }
        if (!term.equals("0")) {
            setTitle(String.format("%s-%d学年第%d学期", year, Integer.parseInt(year) + 1,
                    getTermOrder(Constants.TERMS, term)));
        } else {
            setTitle(String.format("%s-%d学年", year, Integer.parseInt(year) + 1));
        }
        mMultiStatusView.setOnRetryListener(v -> retryLoadGrade(term));
        loadGrade(term);
    }


    private void retryLoadGrade(String term) {
        String termTemp = "";
        if (term.equals("0"))
            termTemp = "";
        else
            termTemp = term;
        String finalTermTemp = termTemp;
        showLoading();
        new LoginPresenter()
                .login(UserAccountManager.getInstance().getInfoUser())
                .subscribeOn(Schedulers.io())
                .flatMap(aBoolean -> CampusFactory.getRetrofitService()
                        .getScores(year, finalTermTemp))
                .subscribe(this::renderScoreList,
                        throwable -> {
                            throwable.printStackTrace();
                            mMultiStatusView.showNetError();
                            CcnuCrawler2.clearCookieStore();
                        }, this::hideLoading);
    }

    private void loadGrade(String term) {
        showLoading();
        String termTemp = "";
        if (term.equals("0"))
            termTemp = "";
        else
            termTemp = term;
        String finalTermTemp = termTemp;
        CampusFactory
                .getRetrofitService()
                .getScores(year, termTemp)
                .subscribeOn(Schedulers.io())
                .onErrorResumeNext(throwable -> {
                    CcnuCrawler2.clearCookieStore();
                    return new LoginPresenter().login(UserAccountManager.getInstance().getInfoUser())
                            .flatMap(aBoolean -> CampusFactory.getRetrofitService()
                                    .getScores(year, finalTermTemp));
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::renderScoreList,
                        throwable -> {
                            throwable.printStackTrace();
                            mMultiStatusView.showNetError();
                            hideLoading();
                        }, this::hideLoading);
    }


    private void renderScoreList(List<Score> scores) {
        if (scores == null || scores.size() == 0) {
            mMultiStatusView.showEmpty();
            return;
        }
        mMultiStatusView.showContent();
        mScoresList.addAll(scores);
        try {
            mScoresAdapter = new ScoresAdapter(mScoresList);
        } catch (Exception e) {
            e.printStackTrace();
            mScoresAdapter = new ScoresAdapter(new ArrayList<>());
        }
        RecyclerView recyclerView = (RecyclerView) mMultiStatusView
                .getContentView();
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(ScoreActivity
                .this));
        recyclerView.setAdapter(mScoresAdapter);
    }

    public int getTermOrder(String[] termStrings, String term) {
        for (int i = 0; i < termStrings.length; i++) {
            if (termStrings[i].equals(term)) {
                return i + 1;
            }
        }
        return 1;
    }

    private void initView() {
        mMultiStatusView = findViewById(R.id.multi_status_view);
    }
}

