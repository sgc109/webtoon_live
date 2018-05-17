package com.example.sgc109.webtoonlive;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.sgc109.webtoonlive.CustomView.EmotionView;
import com.example.sgc109.webtoonlive.CustomView.FixedSizeImageView;
import com.example.sgc109.webtoonlive.dto.EmotionModel;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import es.dmoral.toasty.Toasty;

public class LiveActivity extends AppCompatActivity {
    private static final String TAG = "LiveActivity";
    protected static final String EXTRA_LIVE_KEY = "extra_live_key";

    private LiveInfo liveInfo;

    protected RecyclerView mRecyclerView;
    protected DatabaseReference mDatabase;
    private LinearLayoutManager mLayoutManager;
    protected String mLiveKey;

    protected int mDeviceWidth;
    protected EmotionView mEmotionView;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);

        setTitle("　");
        mLiveKey = getIntent().getStringExtra(EXTRA_LIVE_KEY);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mRecyclerView = findViewById(R.id.activity_live_recycler_view);
        mLayoutManager = new LinearLayoutManager(this);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mDeviceWidth = displayMetrics.widthPixels;
        mEmotionView = findViewById(R.id.emotionView);
        setToasty();
        getLiveInfo();

        RecyclerView.Adapter<SceneImageViewHolder> adapter = new RecyclerView.Adapter<SceneImageViewHolder>() {
            @NonNull
            @Override
            public SceneImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_scene, parent, false);
                return new SceneImageViewHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull SceneImageViewHolder holder, int position) {
                holder.bindImage(position, getItemCount() - 1);
            }

            @Override
            public int getItemCount() {
                return 35;
            }
        };

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(adapter);

        setLiveEmotionListen();
    }

    private void setToasty() {
        Toasty.Config.getInstance()
                .setTextColor(Color.WHITE)
                .apply();
    }

    class SceneImageViewHolder extends RecyclerView.ViewHolder {
        FixedSizeImageView mImageView;

        public SceneImageViewHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.list_item_scene_image_view);
            mImageView.setLastPosition(false);
        }

        public void bindImage(int position, int lastPosition) {
            Glide.with(LiveActivity.this)
                    .load(getResources().getIdentifier("cut" + (position + 1), "drawable", getPackageName()))
                    .apply(new RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .skipMemoryCache(false))
                    .into(mImageView);
            if (position == lastPosition) {
                Log.d("mydebug", "last item bind! set true");
                mImageView.setLastPosition(true);

            }
        }
    }

    protected void showEmotion(EmotionModel emotion) {
        mEmotionView.showEmotion(emotion, false);
    }

    private void setLiveEmotionListen() {
        // 감정표현 관련 코드입니다.
        mDatabase.child(getString(R.string.firebase_db_emotion_history))
                .child(mLiveKey).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                EmotionModel emotion = dataSnapshot.getValue(EmotionModel.class);
                long pastTime = System.currentTimeMillis() - liveInfo.startDate;
                long diff = pastTime - emotion.timeStamp;
                long oneSec = 1000;
                Log.d("AA", "diff: "+ diff);
                if (!mEmotionView.keySet.contains(dataSnapshot.getKey()) && diff < oneSec) {
                    showEmotion(emotion);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getLiveInfo() {
        mDatabase
                .child(getString(R.string.firebase_db_live_list))
                .child(mLiveKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        liveInfo = dataSnapshot.getValue(LiveInfo.class);
                        // 감정표현 입력 레이아웃 초기화
                        mEmotionView.setLiveInfo(liveInfo);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }


}
