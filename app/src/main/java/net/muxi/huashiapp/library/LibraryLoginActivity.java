package net.muxi.huashiapp.library;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import net.muxi.huashiapp.App;
import net.muxi.huashiapp.AppConstants;
import net.muxi.huashiapp.R;
import net.muxi.huashiapp.common.base.ToolbarActivity;
import net.muxi.huashiapp.common.data.User;
import net.muxi.huashiapp.common.data.VerifyResponse;
import net.muxi.huashiapp.common.db.HuaShiDao;
import net.muxi.huashiapp.common.net.CampusFactory;
import net.muxi.huashiapp.common.util.Base64Util;
import net.muxi.huashiapp.common.util.DimensUtil;
import net.muxi.huashiapp.common.util.NetStatus;
import net.muxi.huashiapp.common.util.ToastUtil;
import net.muxi.huashiapp.common.util.ZhugeUtils;
import net.muxi.huashiapp.login.SimpleTextWatcher;
import net.muxi.huashiapp.main.MainActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Response;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by ybao on 16/5/15.
 */
public class LibraryLoginActivity extends ToolbarActivity {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.edit_user_name)
    EditText mEditUserName;
    @BindView(R.id.edit_password)
    EditText mEditPassword;
    @BindView(R.id.btn_login)
    Button mBtnLogin;
    @BindView(R.id.tv_search)
    TextView mTvSearch;
    @BindView(R.id.search_view)
    MySearchView mSearchView;
    @BindView(R.id.accout_layout)
    LinearLayout mAccoutLayout;
    @BindView(R.id.img_bg)
    ImageView mImgBg;
    @BindView(R.id.scroll_view)
    ScrollView mScrollView;

    private String[] mSuggestions;
    private HuaShiDao dao;

    private TextWatcher mTextWatcher = new SimpleTextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            updateBtn();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_login);
//        getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.img_lib_background));
//        getWindow().setStatusBarColor(Color.TRANSPARENT);
        ButterKnife.bind(this);
        dao = new HuaShiDao();
        mSuggestions = dao.loadSearchHistory().toArray(new String[0]);
        initView();
    }

    private void initView() {
        mToolbar.setTitle("图书馆");
//        setSupportActionBar(mToolbar);
//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null){
//            actionBar.setDisplayHomeAsUpEnabled(true);
//        }
        //避免键盘弹起时可滑动
        mScrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                DimensUtil.getScreenHeight() - DimensUtil.getStatusBarHeight()
        );
        mImgBg.setLayoutParams(params);
        mEditUserName.addTextChangedListener(mTextWatcher);
        mEditPassword.addTextChangedListener(mTextWatcher);
        mEditUserName.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2;
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (mEditUserName.getRight() - mEditUserName.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        mEditUserName.setText("");
                        return true;
                    }
                }
                return false;
            }
        });
        mEditPassword.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2;
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (mEditPassword.getRight() - mEditPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        mEditPassword.setText("");
                        return true;
                    }
                }
                return false;
            }
        });
        mSearchView.setSuggestions(mSuggestions);
        mSearchView.setTintViewBackground(Color.TRANSPARENT);
        mSearchView.setIsVisibleWithAnimation(false);
        mSearchView.setOnSearchViewListener(new MySearchView.OnSearchViewListener() {
            @Override
            public void onSearchShown() {
                mSuggestions = dao.loadSearchHistory().toArray(new String[0]);
                mSearchView.setSuggestions(mSuggestions);
            }

            @Override
            public void onSeachClose() {

            }
        });
        mSearchView.setOnQueryTextListener(new MySearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String queryText) {
                dao.insertSearchHistory(queryText);
                Intent intent = new Intent(LibraryLoginActivity.this, LibraryActivity.class);
                intent.putExtra(AppConstants.LIBRARY_QUERY_TEXT, queryText);
                mSearchView.closeSearchView();
                startActivity(intent);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        mSearchView.setOnSearchViewListener(new MySearchView.OnSearchViewListener() {
            @Override
            public void onSearchShown() {

            }

            @Override
            public void onSeachClose() {
                mAccoutLayout.setClickable(true);
//                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        });
        mTvSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchView.showSearchView();
                mAccoutLayout.setClickable(false);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void updateBtn() {
        if (mEditUserName.length() != 0 && mEditPassword.length() != 0) {
            mBtnLogin.setEnabled(true);
        } else {
            mBtnLogin.setEnabled(false);
        }
    }


    @Override
    public void onBackPressed() {
        if (mSearchView.isSearchOpen()) {
            mSearchView.closeSearchView();
            return;
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }

    @OnClick(R.id.btn_login)
    public void onClick() {
        final User libUser = new User();
        libUser.setSid(mEditUserName.getText().toString());
        libUser.setPassword(mEditPassword.getText().toString());
        if (libUser.getSid().equals("") || libUser.getPassword().equals("")){
            ToastUtil.showShort(getString(R.string.tip_err_account));
            return;
        }
        if (NetStatus.isConnected()) {
            showProgressBarDialog(true, "登录中");
            CampusFactory.getRetrofitService().libLogin(Base64Util.createBaseStr(libUser))
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Response<VerifyResponse>>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            showProgressBarDialog(false);
                        }

                        @Override
                        public void onNext(Response<VerifyResponse> verifyResponseResponse) {
                            showProgressBarDialog(false);
                            if (verifyResponseResponse.code() == 200) {
                                ZhugeUtils.sendEvent("图书馆登录", "登陆成功");
                                App.saveLibUser(libUser);
                                Intent intent = new Intent(LibraryLoginActivity.this, MineActivity.class);
                                startActivity(intent);
                            } else if (verifyResponseResponse.code() == 403) {
                                ToastUtil.showLong(getResources().getString(R.string.tip_err_account));
                            } else {
                                ToastUtil.showLong(getResources().getString(R.string.tip_err_server));
                            }
                        }
                    });
        } else {
            ToastUtil.showShort(getString(R.string.tip_check_net));
        }
    }


}
