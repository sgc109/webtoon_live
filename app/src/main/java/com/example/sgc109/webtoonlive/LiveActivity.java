package com.example.sgc109.webtoonlive;

import android.content.Context;
import android.content.Intent;
import android.graphics.DrawFilter;
import android.graphics.PointF;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.provider.ContactsContract.CommonDataKinds.Website.URL;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_DRAGGING;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_SETTLING;

public class LiveActivity extends AppCompatActivity {
    private static final String EXTRA_IS_WRITER = "extra_is_writer";
    private boolean mIsWriter;
    protected RecyclerView mRecyclerView;
    protected DatabaseReference mDatabase;
    private LinearLayoutManager mLayoutManager;
    private int mCurY;
    protected ChildEventListener mChildEventListenerHandle;
    protected int mDeviceWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mRecyclerView = findViewById(R.id.activity_live_recycler_view);
        mLayoutManager = new LinearLayoutManager(this);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mDeviceWidth = displayMetrics.widthPixels;

        RecyclerView.Adapter<SceneImageViewHolder> adapter = new RecyclerView.Adapter<SceneImageViewHolder>() {
            @NonNull
            @Override
            public SceneImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_scene, parent, false);
                return new SceneImageViewHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull SceneImageViewHolder holder, int position) {
                holder.bindImage(position);
            }

            @Override
            public int getItemCount() {
                return 35;
            }
        };
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(adapter);
    }

    public void pushScrollPosToDB() {
        int offset = mRecyclerView.computeVerticalScrollOffset();
        Log.d("scroll_debug", "offset : " + offset);
        double posPercent = (double) offset / mDeviceWidth;

        DatabaseReference ref = mDatabase.child(getString(R.string.firebase_db_scroll_history));
        ref.push()
                .setValue(new VerticalPositionChanged(posPercent, new Date()));
    }

    class SceneImageViewHolder extends RecyclerView.ViewHolder {
        ImageView mImageView;

        public SceneImageViewHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.list_item_scene_image_view);
        }

        public void bindImage(int position) {
//            Log.d("debug","cut" + (position + 1));
//            mImageView.setImageResource(getResources().getIdentifier("cut" + (position + 1), "drawable", getPackageName()));
            Glide.with(LiveActivity.this)
                    .load(getResources().getIdentifier("cut" + (position + 1), "drawable", getPackageName()))
                    .apply(new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false))
                    .into(mImageView);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mChildEventListenerHandle != null) {
            mDatabase.child(getString(R.string.firebase_db_scroll_history)).removeEventListener(mChildEventListenerHandle);
        }
//        if (mIsWriter) {
//            mHandler.removeCallbacks(mPeriodicScrollPosCheck);
//        }
    }
}
