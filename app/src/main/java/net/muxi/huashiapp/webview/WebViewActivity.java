package net.muxi.huashiapp.webview;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.api.share.BaseResponse;
import com.sina.weibo.sdk.api.share.IWeiboHandler;
import com.sina.weibo.sdk.api.share.IWeiboShareAPI;
import com.sina.weibo.sdk.api.share.SendMultiMessageToWeiboRequest;
import com.sina.weibo.sdk.api.share.WeiboShareSDK;
import com.sina.weibo.sdk.constant.WBConstants;
import com.tencent.connect.share.QzoneShare;
import com.tencent.open.utils.ThreadManager;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.tauth.Tencent;

import net.muxi.huashiapp.App;
import net.muxi.huashiapp.AppConstants;
import net.muxi.huashiapp.R;
import net.muxi.huashiapp.common.BaseUiListener;
import net.muxi.huashiapp.common.base.ToolbarActivity;
import net.muxi.huashiapp.common.util.AppUtil;
import net.muxi.huashiapp.common.util.Logger;
import net.muxi.huashiapp.common.util.ToastUtil;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by ybao on 16/5/18.
 */
public class WebViewActivity extends ToolbarActivity implements IWeiboHandler.Response {

    private static final String WEB_URL = "url";
    private static final String WEB_TITLE = "title";
    private static final String WEB_INTRO = "intro";
    private static final String WEB_ICON_URL = "icon_url";
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.appbar_layout)
    AppBarLayout mAppbarLayout;
    @BindView(R.id.webview)
    WebView mWebview;
    @BindView(R.id.custom_progress_bar)
    NumberProgressBar mCustomProgressBar;

    private Tencent mTencent;
    private IWeiboShareAPI mWeiboShareAPI;
    private BaseUiListener mBaseUiListener;

    //对应网站应用的各项属性
    private String url;
    private String title;
    private String iconUrl;
    private String intro;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        ButterKnife.bind(this);

        title = getIntent().getStringExtra(WEB_TITLE);
        url = getIntent().getStringExtra(WEB_URL);
        setTitle(title);

        WebSettings webSettings = mWebview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAppCacheEnabled(true);
        mWebview.setWebChromeClient(new BrowserClient());
        mWebview.loadUrl(url);

        //register weibo
        mWeiboShareAPI = WeiboShareSDK.createWeiboAPI(this,AppConstants.WEIBO_KEY);
        mWeiboShareAPI.registerApp();
    }


    public static Intent newIntent(Context context, String url, String title,String intro,String iconUrl) {
        Intent intent = new Intent(context, WebViewActivity.class);
        intent.putExtra(WEB_URL, url);
        intent.putExtra(WEB_TITLE, title);
        intent.putExtra(WEB_INTRO,intro);
        intent.putExtra(WEB_ICON_URL,iconUrl);
        return intent;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.action_refresh:
                mWebview.reload();
                return true;
            case R.id.action_copy_url:
                AppUtil.clipToClipBoard(WebViewActivity.this, mWebview.getUrl());
                break;
            case R.id.action_open_browser:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mWebview.getUrl()));
                startActivity(browserIntent);
                break;
            case R.id.action_share_qq:
                shareToQzone(title,intro,url,iconUrl);
                break;
            case R.id.action_share_wechat:
                break;
            case R.id.action_share_weibo:
                sendMultiMessage(true,false,false,false,false,false);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_webview, menu);
        return super.onCreateOptionsMenu(menu);
    }


    private class BrowserClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);

            mCustomProgressBar.setProgress(newProgress);
            if (newProgress == 100) {
                mCustomProgressBar.setVisibility(View.GONE);
            } else {
                mCustomProgressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mWebview.canGoBack()) {
                mWebview.goBack();
                return true;
            }
            onBackPressed();
        }
        return super.onKeyDown(keyCode, event);
    }

    public void shareToQzone(String title, String content, String url, String picUrl) {
        mTencent = Tencent.createInstance(AppConstants.QQ_KEY, App.getContext());
        final Bundle params = new Bundle();
        params.putString(QzoneShare.SHARE_TO_QQ_TITLE, title);//必填
        if (content != null) {
            params.putString(QzoneShare.SHARE_TO_QQ_SUMMARY, content);//选填
        }
        params.putString(QzoneShare.SHARE_TO_QQ_TARGET_URL, url);//必填
        ArrayList<String> imgUrlList = new ArrayList<>();
        imgUrlList.add(picUrl);
        params.putStringArrayList(QzoneShare.SHARE_TO_QQ_IMAGE_URL, imgUrlList);// 图片地址
        Log.d("share", "start share to zone");
        ThreadManager.getMainHandler().post(new Runnable() {
            @Override

            public void run() {
                // TODO Auto-generated method stub
                Logger.d("start share");
                mTencent.shareToQzone(WebViewActivity.this, params, mBaseUiListener);
            }

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Tencent.onActivityResultData(requestCode, resultCode, data, mBaseUiListener);
    }

    public TextObject getTextObj(){
        TextObject textObject = new TextObject();
        textObject.text = title + " " + intro + " " + url + " (更多方便的校园应用尽在华师匣子 下载地址: " + AppConstants.APP_DOWNLOAD_URL + ")";
        return textObject;
    }

    private void sendMultiMessage(boolean hasText, boolean hasImage, boolean hasWebpage,
                                  boolean hasMusic, boolean hasVideo, boolean hasVoice) {
        WeiboMultiMessage weiboMessage = new WeiboMultiMessage();//初始化微博的分享消息
        if (hasText) {
            weiboMessage.textObject = getTextObj();
        }
        SendMultiMessageToWeiboRequest request = new SendMultiMessageToWeiboRequest();
        request.transaction = String.valueOf(System.currentTimeMillis());
        request.multiMessage = weiboMessage;
        mWeiboShareAPI.sendRequest(this,request); //发送请求消息到微博，唤起微博分享界面
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mWeiboShareAPI.handleWeiboResponse(intent,this); //当前应用唤起微博分享后，返回当前应用
        ToastUtil.showShort(getString(R.string.tip_share_success));
    }

    @Override
    public void onResponse(BaseResponse baseResp) {//接收微客户端博请求的数据。
        switch (baseResp.errCode) {
            case WBConstants.ErrorCode.ERR_OK:
                ToastUtil.showShort(getString(R.string.tip_share_success));
                break;
            case WBConstants.ErrorCode.ERR_CANCEL:
                Logger.d("cancel");
                break;
            case WBConstants.ErrorCode.ERR_FAIL:
                ToastUtil.showShort(getString(R.string.tip_share_fail));
                break;
        }
    }



    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
