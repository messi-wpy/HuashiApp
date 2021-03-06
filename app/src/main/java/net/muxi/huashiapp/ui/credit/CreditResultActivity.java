package net.muxi.huashiapp.ui.credit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.TextView;

import net.muxi.huashiapp.App;
import net.muxi.huashiapp.Constants;
import net.muxi.huashiapp.R;
import net.muxi.huashiapp.common.base.ToolbarActivity;
import net.muxi.huashiapp.common.data.Score;
import net.muxi.huashiapp.net.CampusFactory;
import net.muxi.huashiapp.ui.login.LoginPresenter;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by ybao on 17/2/9.
 */

public class CreditResultActivity extends ToolbarActivity {

    @BindView(R.id.tv_credit_all)
    TextView mTvCreditAll;
    @BindView(R.id.tv_zb)
    TextView mTvZb;
    @BindView(R.id.tv_tb)
    TextView mTvTb;
    @BindView(R.id.tv_th)
    TextView mTvTh;
    @BindView(R.id.tv_zx)
    TextView mTvZx;
    @BindView(R.id.tv_tx)
    TextView mTvTx;

    private int start;
    private int end;

    private float zb, zx, tb, tx, th;

    public static void start(Context context, int start, int end) {
        Intent starter = new Intent(context, CreditResultActivity.class);
        starter.putExtra("start", start);
        starter.putExtra("ending", end);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credit_result);
        ButterKnife.bind(this);

        start = getIntent().getIntExtra("start", 0);
        end = getIntent().getIntExtra("ending", 0);
        setTitle(String.format("%d-%d学年", start, end));
        loadCredit();
    }

    public void loadCredit() {
        loadCredit(getScoreRequest(start, end));
    }

    public void loadCredit(Observable<List<Score>>[] listObservable) {
        showLoading();
        Observable<List<Score>> scoreObservable = Observable.merge(listObservable, 5)
                .flatMap(Observable::from)
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());


                scoreObservable.subscribe(scores -> {
                    addCredit(scores);
                    float all = zb + zx + tb + tx + th;
                    mTvZb.setText(String.valueOf(zb));
                    mTvZx.setText(String.valueOf(zx));
                    mTvTb.setText(String.valueOf(tb));
                    mTvTx.setText(String.valueOf(tx));
                    mTvTh.setText(String.valueOf(th));
                    mTvCreditAll.setText(String.valueOf(all));
                    hideLoading();
                }, throwable -> {
                    hideLoading();
                    throwable.printStackTrace();
                    new LoginPresenter().login(App.sUser)
                            .flatMap(aubBoolean -> scoreObservable)
                            .subscribe(scores -> {
                                    addCredit(scores);
                                    float all = zb + zx + tb + tx + th;
                                    mTvZb.setText(String.valueOf(zb));
                                    mTvZx.setText(String.valueOf(zx));
                                    mTvTb.setText(String.valueOf(tb));
                                    mTvTx.setText(String.valueOf(tx));
                                    mTvTh.setText(String.valueOf(th));
                                    mTvCreditAll.setText(String.valueOf(all));
                                    hideLoading();
                            });
                }, () -> {});
    }

    public Observable<List<Score>>[] getScoreRequest(int start, int end) {
        Observable<List<Score>>[] observables = new Observable[(end - start)];
        for (int i = 0; i < (end - start) ; i++) {
            observables[i] = CampusFactory.getRetrofitService()
                    .getScores(String.valueOf(start + i), "");
        }
        return observables;
    }

    public void addCredit(List<Score> scores) {
        for (Score score : scores) {
            for (int i = 0; i < Constants.CREDIT_CATEGORY.length; i++) {
                if (Constants.CREDIT_CATEGORY[i].equals(score.kcxzmc)) {
                    switch (i) {
                        case 0:
                            zb += Float.parseFloat(score.credit);
                            break;
                        case 1:
                            zx += Float.parseFloat(score.credit);
                            break;
                        case 2:
                            tb += Float.parseFloat(score.credit);
                            break;
                        case 3:
                            tx += Float.parseFloat(score.credit);
                            break;
                        case 4:
                            th += Float.parseFloat(score.credit);
                            break;
                    }
                    break;
                }
            }
        }
    }

}
