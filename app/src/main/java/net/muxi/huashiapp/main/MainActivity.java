
package net.muxi.huashiapp.main;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.tencent.android.tpush.XGIOperateCallback;
import com.tencent.android.tpush.XGPushConfig;
import com.tencent.android.tpush.XGPushManager;

import net.muxi.huashiapp.App;
import net.muxi.huashiapp.BuildConfig;
import net.muxi.huashiapp.Constants;
import net.muxi.huashiapp.R;
import net.muxi.huashiapp.common.base.ToolbarActivity;
import net.muxi.huashiapp.common.data.BannerData;
import net.muxi.huashiapp.common.data.News;
import net.muxi.huashiapp.common.data.PatchData;
import net.muxi.huashiapp.common.data.ProductData;
import net.muxi.huashiapp.common.data.VersionData;
import net.muxi.huashiapp.common.db.HuaShiDao;
import net.muxi.huashiapp.common.net.CampusFactory;
import net.muxi.huashiapp.service.DownloadService;
import net.muxi.huashiapp.ui.AboutActivity;
import net.muxi.huashiapp.ui.CalendarActivity;
import net.muxi.huashiapp.ui.SettingActivity;
import net.muxi.huashiapp.ui.apartment.ApartmentActivity;
import net.muxi.huashiapp.ui.card.CardActivity;
import net.muxi.huashiapp.ui.electricity.ElectricityActivity;
import net.muxi.huashiapp.ui.electricity.ElectricityDetailActivity;
import net.muxi.huashiapp.ui.library.LibraryLoginActivity;
import net.muxi.huashiapp.ui.library.MineActivity;
import net.muxi.huashiapp.ui.login.LoginActivity;
import net.muxi.huashiapp.ui.main.MainAdapter;
import net.muxi.huashiapp.ui.news.NewsActivity;
import net.muxi.huashiapp.ui.schedule.CourseEditActivity;
import net.muxi.huashiapp.ui.score.ScoreActivity;
import net.muxi.huashiapp.ui.website.WebsiteActivity;
import net.muxi.huashiapp.ui.webview.WebViewActivity;
import net.muxi.huashiapp.util.AlarmUtil;
import net.muxi.huashiapp.util.DownloadUtils;
import net.muxi.huashiapp.util.FrescoUtil;
import net.muxi.huashiapp.util.Logger;
import net.muxi.huashiapp.util.NetStatus;
import net.muxi.huashiapp.util.PreferenceUtil;
import net.muxi.huashiapp.util.ToastUtil;
import net.muxi.huashiapp.util.ZhugeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.ResponseBody;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends ToolbarActivity {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;
    //app 原有的功能
//    private String[] pics = {R.drawable.ic_main_curschedule + "", R.drawable.ic_main_idcard + "",
//            R.drawable.ic_main_mark + "", R.drawable.ic_main_power_rate + "",
//            R.drawable.ic_main_school_calendar + "", R.drawable.ic_main_workschedule + "",
//            R.drawable.ic_main_library + "",R.drawable.ic_main_website + ""};
    private String[] desc = {"课程表", "学生卡", "成绩查询", "电费查询", "校历查询", "部门信息", "图书馆","常用网站"};

    private List<String> mpic;
    private List<String> mdesc;
    private MainAdapter mAdapter;

    private long exitTime = 0;
    private ProductData mProductData;
    private String mProductJson;
    private PreferenceUtil sp;

    private HuaShiDao dao;
    private List<BannerData> mBannerDatas;

    private Context context;
    private static final int WEB_POSITION = 9;
    private String downloadUrl;

    private boolean hasLatestNews = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        sp = new PreferenceUtil();

        mpic = new ArrayList<>();
        mdesc = new ArrayList<>();

//        mpic.addAll(Arrays.asList(pics));
        mdesc.addAll(Arrays.asList(desc));

        initXGPush();

        //检查本地是否有补丁包
//        try {
//            if (!DownloadUtils.isFileExists(AppConstants.CACHE_DIR + "/" + AppConstants.APATCH_NAME)) {
//                downloadPatch();
//                Logger.d("download patch");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            Logger.d("cache dir not found");
//        }

        dao = new HuaShiDao();

        mBannerDatas = dao.loadBannerData();
        if (mBannerDatas.size() > 0) {
            initRecyclerView();
            Logger.d("init recyclerview");
        } else {
            initRecyclerView();
            Logger.d("please link the net");
        }
        //获取服务器上的 banner 数据
        getBannerDatas();
        //获取本地的 product 数据
        Gson gson = new Gson();
        mProductData = gson.fromJson(sp.getString(PreferenceUtil.PRODUCT_DATA, Constants.PRODUCT_JSON), ProductData.class);
        updateProductDisplay(mProductData);
        getProduct();
        checkNews();
        checkNewVersion();
        AlarmUtil.register(this);
        Log.d("alarm", "register");
    }

    private void checkNewVersion() {
        Logger.d("begin check new version");
        CampusFactory.getRetrofitService().getLatestVersion()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<VersionData>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(VersionData versionData) {
//                        if (!versionData.getVersion().equals(BuildConfig.VERSION_NAME)){
//                            Logger.d("has new version!!");
//                            if (!sp.getString(PreferenceUtil.LAST_NOT_REMIND_VERSION,BuildConfig.VERSION_NAME).equals(versionData.getVersion())
//                                    || sp.getBoolean(PreferenceUtil.REMIND_UPDATE,true)){
//                                if (!sp.getString(PreferenceUtil.LAST_NOT_REMIND_VERSION,BuildConfig.VERSION_NAME).equals(versionData.getVersion())){
////                                    sp.saveBoolean(PreferenceUtil.REMIND_UPDATE,true);
//                                }
//                                Logger.d("init dialog");
//                                sp.saveString(PreferenceUtil.LAST_NOT_REMIND_VERSION,versionData.getVersion());
//                                downloadUrl = versionData.getDownload();
//                                final MaterialDialog materialDialog = new MaterialDialog(MainActivity.this);
//                                final UpdateView updateView = new UpdateView(MainActivity.this);
//                                updateView.setContentText(App.sContext.getString(R.string.tip_update_intro) + versionData.getIntro() + "\n" + App.sContext.getString(R.string.tip_update_size) + versionData.getSize());
//                                materialDialog.setView(updateView);
//                                materialDialog.setTitle(versionData.getName() + versionData.getVersion() + App.sContext.getString(R.string.title_update));
//                                materialDialog.setNegativeButtonColor(App.sContext.getResources().getColor(R.color.colorPrimary));
//                                materialDialog.setPositiveButtonColor(App.sContext.getResources().getColor(R.color.colorPrimary));
//                                materialDialog.setPositiveButton(App.sContext.getString(R.string.btn_update), new View.OnClickListener() {
//                                    @Override
//                                    public void onClick(View v) {
//                                        if (isStorgePermissionGranted()){
//                                            beginUpdate(downloadUrl);
//                                        }
//                                        materialDialog.dismiss();
//                                    }
//                                });
//                                materialDialog.setNegativeButton(App.sContext.getString(R.string.btn_not_now), new View.OnClickListener() {
//                                    @Override
//                                    public void onClick(View v) {
////                                        sp.saveBoolean(PreferenceUtil.REMIND_UPDATE,!updateView.isRemindClose());
//                                        materialDialog.dismiss();
//                                    }
//                                });
//
//                                materialDialog.show();
//                            }
//                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Logger.d("permission " + permissions[0] + "is" + grantResults[0]);
            Logger.d(downloadUrl);
            if (downloadUrl != null && downloadUrl.length() != 0) {
                beginUpdate(downloadUrl);
            }
        }
    }

    private void beginUpdate(String download) {
        deleteApkBefore();
        Intent intent = new Intent(this, DownloadService.class);
        intent.putExtra("url", download);
        intent.putExtra("fileType", "apk");
        intent.putExtra("fileName", "ccnubox.apk");
        startService(intent);
        Logger.d("start download");
        ToastUtil.showShort(getString(R.string.tip_start_download_apk));
    }

    private void deleteApkBefore() {
        String path = Environment.getExternalStorageDirectory() + "/Download/" + "ccnubox.apk";
        File file = new File(path);
        if (file.exists()){
            file.delete();
            Logger.d("apk file delete");
        }
        Logger.d("file not exists");
    }

    public boolean isStorgePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            return true;
        }
    }

    //信鸽注册和启动
    private void initXGPush() {
        context = getApplicationContext();
        XGPushConfig.enableDebug(this, true);
        XGPushConfig.getToken(this);
        XGPushManager.registerPush(context, "users"
                , new XGIOperateCallback() {
                    @Override
                    public void onSuccess(Object data, int i) {
                        Log.d("TPush", "注册成功，设备token为：" + data);

                    }

                    @Override
                    public void onFail(Object data, int errCode, String msg) {
                        Log.d("TPush", "注册失败，错误码：" + errCode + ",错误信息：" + msg);

                    }
                });
    }

    private void downloadPatch() {
        CampusFactory.getRetrofitService().getPatch()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<List<PatchData>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(List<PatchData> patchDatas) {
                        for (PatchData patchData : patchDatas) {
                            if (BuildConfig.VERSION_NAME.equals(patchData.getVersion())) {
                                CampusFactory.getRetrofitService().downloadFile(patchData.getDownload())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribeOn(Schedulers.newThread())
                                        .subscribe(new Observer<ResponseBody>() {
                                            @Override
                                            public void onCompleted() {

                                            }

                                            @Override
                                            public void onError(Throwable e) {
                                                e.printStackTrace();
                                            }

                                            @Override
                                            public void onNext(ResponseBody responseBody) {
                                                DownloadUtils.writeResponseBodyToDisk(responseBody, Constants.APATCH_NAME);
                                            }
                                        });
                            }
                        }
                    }
                });

    }

    //更新首页视图
    public void updateProductDisplay(ProductData productData) {
        List<String> picList = new ArrayList<>();
        List<String> descList = new ArrayList<>();
        for (int i = 0; i < productData.get_products().size(); i++) {
            picList.add(productData.get_products().get(i).getIcon());
            descList.add(productData.get_products().get(i).getName());
        }
        mpic.clear();
        mdesc.clear();
//        mpic.addAll(Arrays.asList(pics));
        mpic.addAll(picList);
        mdesc.addAll(Arrays.asList(desc));
        mdesc.addAll(descList);
        mAdapter.swapProduct(mpic, mdesc);
    }


    private void getBannerDatas() {
        if (NetStatus.isConnected()) {
            //本地保存的更新时间
            CampusFactory.getRetrofitService().getBanner()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(new Observer<List<BannerData>>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onNext(List<BannerData> bannerDatas) {
                            if (getTheLastUpdateTime(bannerDatas) > getTheLastUpdateTime(mBannerDatas) || bannerDatas.size() != mBannerDatas.size()) {
                                mBannerDatas.clear();
                                mBannerDatas.addAll(bannerDatas);
                                dao.deleteAllBannerData();
                                for (int i = 0; i < mBannerDatas.size(); i++) {
                                    dao.insertBannerData(mBannerDatas.get(i));
                                }
                                updateRecyclerView(bannerDatas);
                                Logger.d("update recyclerview");
                            }
                            Logger.d("get bannerdatas");
                        }
                    });
        }

    }


    /**
     * 在 list 中 获取最近的更新时间
     *
     * @param bannerDatas
     * @return
     */
    public long getTheLastUpdateTime(List<BannerData> bannerDatas) {
        long lastTime = -1;
        if (bannerDatas.size() > 0) {
            for (int i = 0; i < bannerDatas.size(); i++) {
                if (lastTime < bannerDatas.get(i).getUpdate()) {
                    lastTime = bannerDatas.get(i).getUpdate();
                }
            }
        }
        return lastTime;
    }


    public void initRecyclerView() {
        final GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        mAdapter = new MainAdapter(mpic, mdesc, mBannerDatas);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return mAdapter.isBannerPosition(position) ? layoutManager.getSpanCount() : 1;
            }
        });
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mAdapter);
//        mRecyclerView.addItemDecoration(new MyItemDecoration());
        mAdapter.setOnBannerItemClickListener(new MainAdapter.OnBannerItemClickListener() {
            @Override
            public void onBannerItemClick(BannerData bannerData) {
                ZhugeUtils.sendEvent("点击 banner", bannerData.getUrl());
                try {
//                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(bannerData.getUrl()));
//                    startActivity(browserIntent);
                    //更改为 app 内部打开
                    Intent intent = WebViewActivity.newIntent(MainActivity.this, bannerData.getUrl());
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mAdapter.setItemClickListener(new MainAdapter.ItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Intent intent;
                Logger.d(position + "");
                switch (position) {
                    case 0:
                        if (App.sUser.getSid() != "0") {
                            ZhugeUtils.sendEvent("课表查询", "课表查询");
                            intent = new Intent(MainActivity.this, CourseEditActivity.class);
                            startActivity(intent);
                            break;
                        } else {
                            ToastUtil.showShort(getString(R.string.tip_login_first));
                            intent = new Intent(MainActivity.this, LoginActivity.class);
                            startActivity(intent);
                            break;
                        }
                    case 1:
                        if (App.sUser.getSid() != "0") {
                            ZhugeUtils.sendEvent("学生卡查询", "学生卡查询");
                            intent = new Intent(MainActivity.this, CardActivity.class);
                            startActivity(intent);
                            break;
                        } else {
                            ToastUtil.showShort(getString(R.string.tip_login_first));
                            intent = new Intent(MainActivity.this, LoginActivity.class);
                            startActivity(intent);
                            break;
                        }
                    case 2:
                        if (App.sUser.getSid() != "0") {
                            intent = new Intent(MainActivity.this, ScoreActivity.class);
                            startActivity(intent);
                            break;
                        } else {
                            ToastUtil.showShort(getString(R.string.tip_login_first));
                            intent = new Intent(MainActivity.this, LoginActivity.class);
                            startActivity(intent);
                            break;
                        }
                    case 3:
                        PreferenceUtil sp = new PreferenceUtil();
                        String eleQuery = sp.getString(PreferenceUtil.ELE_QUERY_STRING);
                        if (eleQuery.equals("")) {
                            intent = new Intent(MainActivity.this, ElectricityActivity.class);
                            startActivity(intent);
                        } else {
                            intent = new Intent(MainActivity.this, ElectricityDetailActivity.class);
                            intent.putExtra("query", eleQuery);
                            startActivity(intent);
                        }
                        break;
                    case 4:
                        ZhugeUtils.sendEvent("查询校历", "查询校历");
                        intent = new Intent(MainActivity.this, CalendarActivity.class);
                        startActivity(intent);
                        break;
                    case 5:
                        ZhugeUtils.sendEvent("查看部门信息", "查看部门信息");
                        intent = new Intent(MainActivity.this, ApartmentActivity.class);
                        startActivity(intent);
                        break;
                    case 7:
                        ZhugeUtils.sendEvent("进入图书馆", "进入图书馆");
                        if (!App.sLibrarayUser.getSid().equals("0")) {
                            intent = new Intent(MainActivity.this, MineActivity.class);
                            startActivity(intent);
                        } else {
                            intent = new Intent(MainActivity.this, LibraryLoginActivity.class);
                            startActivity(intent);
                        }
                        break;

//                    case 8:
//                        ZhugeUtils.sendEvent("进入学而","进入学而");
//                        intent = WebViewActivity.newIntent(MainActivity.this, "https://xueer.muxixyz.com/", "学而","华师选课经验平台","http://f.hiphotos.baidu.com/image/h%3D200/sign=6f05c5f929738bd4db21b531918a876c/6a600c338744ebf8affdde1bdef9d72a6059a702.jpg");
//                        startActivity(intent);
//                        break;
                    case 8:
//                        ZhugeUtils.sendEvent("查询常用网站","查询常用网站");
                        intent = new Intent(MainActivity.this, WebsiteActivity.class);
                        startActivity(intent);
                        break;
                }
                Logger.d(position + "");
                if (position >= WEB_POSITION) {
                    int productPos = position - WEB_POSITION;
                    Logger.d(productPos + "");
                    ZhugeUtils.sendEvent(mProductData.get_products().get(productPos).getName(), mProductData.get_products().get(productPos).getName());
                    intent = WebViewActivity.newIntent(MainActivity.this, mProductData.get_products().get(productPos).getUrl(),
                            mProductData.get_products().get(productPos).getName(),
                            mProductData.get_products().get(productPos).getIntro(),
                            mProductData.get_products().get(productPos).getIcon());
                    startActivity(intent);
                }

            }
        });

    }

    public void getProduct() {
        CampusFactory.getRetrofitService().getProduct()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<ProductData>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(ProductData productData) {
                        if (productData.getUpdate() != mProductData.getUpdate()) {
                            mProductData = productData;
                            Gson gson = new Gson();
                            mProductJson = gson.toJson(mProductData);
                            sp.saveString(PreferenceUtil.PRODUCT_DATA, mProductJson);
                            sp.saveFloat(PreferenceUtil.PRODUCT_UPDATE, (float) productData.getUpdate());
                            for (int i = WEB_POSITION; i < productData.get_products().size(); i++) {
                                FrescoUtil.savePicture(productData.get_products().get(i).getIcon(), MainActivity.this, productData.get_products().get(i).getIcon());
                            }
                            updateProductDisplay(productData);
                        }

                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public void updateRecyclerView(List<BannerData> bannerDatas) {
        mAdapter.swapBannerData(bannerDatas);
//        mAdapter = new MainAdapter(mdesc,mpics,bannerDatas);
//        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void checkNews(){
        CampusFactory.getRetrofitService().getNews()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(new Observer<List<News>>() {
                @Override public void onCompleted() {

                }

                @Override public void onError(Throwable e) {
                    e.printStackTrace();
                }

                @Override public void onNext(List<News> newses) {
                    if (!sp.getString(PreferenceUtil.LATEST_NEWS_DATE,"2016-11-01").equals(newses.get(0).getDate())){
                        hasLatestNews = true;
                        sp.saveString(PreferenceUtil.LATEST_NEWS_DATE,newses.get(0).getDate());
                       invalidateOptionsMenu();
                    }
                }
            });
    }

    @Override public boolean onPrepareOptionsMenu(Menu menu) {
        if (hasLatestNews){
            MenuItem item = menu.findItem(R.id.action_news);
//            item.setIcon(R.drawable.ic_message_reddot);
        }else {
            MenuItem item = menu.findItem(R.id.action_news);
//            item.setIcon(R.drawable.ic_message);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - exitTime) > 3000) {
                Toast.makeText(getApplicationContext(), "再按一次后退键退出应用程序", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        int id = item.getItemId();
        switch (id) {
            case R.id.action_news:
                ZhugeUtils.sendEvent("查看消息公告", "查看消息公告");
                intent = new Intent(MainActivity.this, NewsActivity.class);
                startActivity(intent);
                hasLatestNews = false;
                invalidateOptionsMenu();
                break;
            case R.id.action_settings:
                intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
                break;
            case R.id.action_about:
                intent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean canBack() {
        return false;
    }

}
