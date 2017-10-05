package com.werb.pickphotoview;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.werb.pickphotoview.adapter.PickGridAdapter;
import com.werb.pickphotoview.adapter.SpaceItemDecoration;
import com.werb.pickphotoview.model.GroupImage;
import com.werb.pickphotoview.model.PickData;
import com.werb.pickphotoview.model.PickHolder;
import com.werb.pickphotoview.util.PickConfig;
import com.werb.pickphotoview.util.PickPhotoHelper;
import com.werb.pickphotoview.util.PickPhotoListener;
import com.werb.pickphotoview.util.PickPreferences;
import com.werb.pickphotoview.util.PickUtils;
import com.werb.pickphotoview.widget.MyToolbar;

import java.util.ArrayList;

/**
 * Created by wanbo on 2016/12/30.
 */

public class PickPhotoActivity extends AppCompatActivity {

    private PickData pickData;
    private RecyclerView photoList;
    private PickGridAdapter pickGridAdapter;
    private MyToolbar myToolbar;
    private TextView selectText, selectImageSize;
    private ArrayList<String> allPhotos;
    private Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pick_activity_pick_photo);
        mContext = getApplicationContext();
        pickData = (PickData) getIntent().getSerializableExtra(PickConfig.INTENT_PICK_DATA);
        if (pickData != null) {
            PickPreferences.getInstance(PickPhotoActivity.this).savePickData(pickData);
        } else {
            pickData = PickPreferences.getInstance(PickPhotoActivity.this).getPickData();
        }
        initToolbar();
        initRecyclerView();
        initSelectLayout();
    }

    @Override
    public void finish() {
        super.finish();
        PickHolder.newInstance(); //Reset stored selected image paths.
        overridePendingTransition(0, R.anim.pick_finish_slide_out_bottom);
    }

    private void initToolbar() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(pickData.getStatusBarColor());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(pickData.isLightStatusBar()) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
        selectText = findViewById(R.id.tv_pick_photo);
        selectImageSize = findViewById(R.id.tv_preview_photo);
        selectImageSize.setText(String.valueOf("0"));
        myToolbar = findViewById(R.id.toolbar);
        myToolbar.setBackgroundColor(pickData.getToolbarColor());
        myToolbar.setIconColor(pickData.getToolbarIconColor());
        myToolbar.setLeftIcon(R.mipmap.pick_ic_open);
        myToolbar.setRightIcon(R.mipmap.pick_ic_close);
        myToolbar.setPhotoDirName(getString(R.string.pick_all_photo));
        myToolbar.setLeftLayoutOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PickPhotoActivity.this.startPhotoListActivity();
            }
        });
        myToolbar.setRightLayoutOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PickPhotoActivity.this.finish();
            }
        });

        selectText.setOnClickListener(selectClick);
    }

    private void initRecyclerView() {
        photoList = findViewById(R.id.photo_list);
        photoList.setItemAnimator(new DefaultItemAnimator());
        GridLayoutManager layoutManager = new GridLayoutManager(this, pickData.getSpanCount());
        photoList.setLayoutManager(layoutManager);
        photoList.addItemDecoration(new SpaceItemDecoration(PickUtils.getInstance(PickPhotoActivity.this).dp2px(PickConfig.ITEM_SPACE), pickData.getSpanCount()));
        PickPhotoHelper helper = new PickPhotoHelper(PickPhotoActivity.this, new PickPhotoListener() {
            @Override
            public void pickSuccess() {
                GroupImage groupImage = PickPreferences.getInstance(PickPhotoActivity.this).getListImage();
                allPhotos = groupImage.mGroupMap.get(PickConfig.ALL_PHOTOS);
                if(allPhotos == null){
//                    Log.d("PickPhotoView","Image is Empty");
                }else {
//                    Log.d("All photos size:", String.valueOf(allPhotos.size()));
                }
                if (allPhotos != null && !allPhotos.isEmpty()) {
                    pickGridAdapter = new PickGridAdapter(PickPhotoActivity.this, allPhotos, pickData, imageClick);
                    photoList.setAdapter(pickGridAdapter);
                }
            }
        });
        helper.getImages(pickData.isShowGif());
    }

    private void initSelectLayout() {
        LinearLayout selectLayout = findViewById(R.id.select_layout);
        selectLayout.setVisibility(View.VISIBLE);
    }

    public void updateSelectText(String selectSize) {
        if (selectSize.equals("0")) {
            selectImageSize.setText(String.valueOf(0));
            selectText.setTextColor(ContextCompat.getColor(mContext, R.color.pick_gray));
            selectText.setEnabled(false);
        } else {
            selectImageSize.setText(String.valueOf(selectSize));
            selectText.setTextColor(pickData.getSelectIconColor());
            selectText.setEnabled(true);
        }
    }

    private void startPhotoListActivity() {
        Intent intent = new Intent();
        intent.setClass(PickPhotoActivity.this, PickListActivity.class);
        intent.putExtra(PickConfig.INTENT_PICK_DATA, pickData);
        startActivityForResult(intent, PickConfig.LIST_PHOTO_DATA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 0) {
            return;
        }
        if (requestCode == PickConfig.LIST_PHOTO_DATA) {
            if (data != null) {
                String dirName = data.getStringExtra(PickConfig.INTENT_DIR_NAME);
                GroupImage listImage = PickPreferences.getInstance(PickPhotoActivity.this).getListImage();
                allPhotos = listImage.mGroupMap.get(dirName);
                pickGridAdapter.updateData(allPhotos);
                myToolbar.setPhotoDirName(dirName);
                selectText.setText(getString(R.string.pick_pick));
                selectText.setTextColor(ContextCompat.getColor(mContext, R.color.pick_black));
            }
        } else if (requestCode == PickConfig.CAMERA_PHOTO_DATA) {
            String path;
            if (data != null) {
                path = data.getData().getPath();
                if (path.contains("/pick_camera")) {
                    path = path.replace("/pick_camera", "/storage/emulated/0/DCIM/Camera");
                }
            } else {
                path = PickUtils.getInstance(PickPhotoActivity.this).getFilePath(PickPhotoActivity.this);
            }
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + path)));
            Intent intent = new Intent();
            ArrayList<String> list = new ArrayList<>();
            list.add(path);
            intent.putExtra(PickConfig.INTENT_IMG_LIST_SELECT, list);
            setResult(PickConfig.PICK_PHOTO_DATA, intent);
            finish();
        }else if(requestCode == PickConfig.PREVIEW_PHOTO_DATA){
            if (data != null) {
                ArrayList<String> selectPath = (ArrayList<String>) data.getSerializableExtra(PickConfig.INTENT_IMG_LIST_SELECT);
                pickGridAdapter.setSelectPath(selectPath);
                pickGridAdapter.notifyDataSetChanged();
                updateSelectText(String.valueOf(selectPath.size()));
            }
        }
    }

    View.OnClickListener imageClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String imgPath = (String) v.getTag(R.id.pick_image_path);
            Intent intent = new Intent();
            intent.setClass(PickPhotoActivity.this, PickPhotoPreviewActivity.class);
            intent.putExtra(PickConfig.INTENT_IMG_PATH, imgPath);
            intent.putExtra(PickConfig.INTENT_IMG_LIST, allPhotos);
            intent.putExtra(PickConfig.INTENT_IMG_LIST_SELECT, pickGridAdapter.getSelectPath());
            intent.putExtra(PickConfig.INTENT_PICK_DATA, pickData);
            startActivityForResult(intent, PickConfig.PREVIEW_PHOTO_DATA);
        }
    };

    private View.OnClickListener selectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            select();
        }
    };

    public void select(){
        if(pickGridAdapter == null){
            return;
        }

        if (!pickGridAdapter.getSelectPath().isEmpty()) {
            Intent intent = new Intent();
            intent.putExtra(PickConfig.INTENT_IMG_LIST_SELECT, pickGridAdapter.getSelectPath());
            setResult(PickConfig.PICK_PHOTO_DATA, intent);
            finish();
        }
    }
}
