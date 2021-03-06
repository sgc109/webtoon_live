package com.example.sgc109.webtoonlive;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.example.sgc109.webtoonlive.CustomView.CommentView;
import com.example.sgc109.webtoonlive.dialog.LiveEndConfirmDialog;
import com.example.sgc109.webtoonlive.dto.Comment;
import com.example.sgc109.webtoonlive.dto.EmotionModel;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import static com.example.sgc109.webtoonlive.WriterLiveActivity.convertPixelsToDp;

public class ReaderLiveActivity extends LiveActivity {
    private ProgressBar mProgressBar;
    private LiveInfo mLiveInfo;
    private ChildEventListener mNewScrollAddedListener;
    private ValueEventListener mLiveStateChangeListener;
    private ChildEventListener mWriterCommentAddedListener;
    private ChildEventListener mWriterCommentShowListener;
    private Long mStartedTime;

    public static Intent newIntent(Context context, String liveKey) {
        Intent intent = new Intent(context, ReaderLiveActivity.class);
        intent.putExtra(EXTRA_LIVE_KEY, liveKey);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStartedTime = System.currentTimeMillis();
        mProgressBar = findViewById(R.id.live_progress_bar);
        mProgressBar.setVisibility(View.VISIBLE);

        commentFieldScroll.setVisibility(View.VISIBLE);
        commentInfoScroll.setVisibility(View.VISIBLE);
        commentFieldScroll.setBackgroundColor(Color.alpha(0));



        mDatabase
                .child(getString(R.string.firebase_db_live_list))
                .child(mLiveKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Log.d("DEBUG", "read LiveInfo from FirebaseDB by mLiveKey");
                        mLiveInfo = dataSnapshot.getValue(LiveInfo.class);
                        String STATE_ON_AIR = getString(R.string.live_state_on_air);


                        if (mLiveInfo != null) {
                            if (mLiveInfo.state.equals(STATE_ON_AIR)) {
                                addDataChangeListeners();
                                settingCommentListeners();
                                mProgressBar.setVisibility(View.GONE);
                                findViewById(R.id.blink_live).startAnimation(AnimationUtils.loadAnimation(ReaderLiveActivity.this, R.anim.blink_animation));
                            } else {
                                addCommentIndicatorListener();
                                getRecordingDatas();

                                mEmotionView.inputBar.setVisibility(View.GONE);

                                Long timeAfter = mLiveInfo.endDate - mLiveInfo.startDate;
//                                mEndHandle
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(this != null && !isDestroyed() && !isFinishing()) {
                                            new LiveEndConfirmDialog(ReaderLiveActivity.this).show();
                                        }
                                    }
                                }, timeAfter);
                                findViewById(R.id.blink_live).setVisibility(View.GONE);
                            }
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("DEBUG", "failed to read LiveInfo from FirebaseDB by mLiveKey");
                    }
                });


        setEmotionView();
    }

    private void addCommentIndicatorListener(){
        mDatabase
                .child(getString(R.string.comment_history))
                .child(mLiveKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            final Comment comment = child.getValue(Comment.class);
                            addIndicator(comment);
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private void addIndicator(Comment comment){
        final RelativeLayout infoView = new RelativeLayout(this);
        double rate = mRecyclerView.computeVerticalScrollRange()/comment.getScrollLength();

        LinearLayout.LayoutParams infoViewParams = new LinearLayout.LayoutParams(10, 40);
        infoViewParams.setMargins( 0
                ,  (int)(comment.getPosY()*rate)-30
                ,0,0);

        infoView.setLayoutParams(infoViewParams);
        infoView.setBackgroundColor(Color.parseColor("#00C73C"));

        commentInfo.addView(infoView);
    }


    private void getRecordingDatas() {
        mDatabase
                .child(getString(R.string.firebase_db_scroll_history))
                .child(mLiveKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            final VerticalPositionChanged scrollHistory = child.getValue(VerticalPositionChanged.class);
                            Long passedTime = System.currentTimeMillis() - mStartedTime;
                            Long timeAfter = scrollHistory.time - passedTime;

                            if (timeAfter < 0) {
                                continue;
                            }
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    double percentage = scrollHistory.offsetProportion;

                                    int nextY = (int) (percentage * mDeviceWidth);
                                    int curY = mRecyclerView.computeVerticalScrollOffset();
                                    mRecyclerView.smoothScrollBy(0, nextY - curY);
                                }
                            }, timeAfter);
                        }
                        Long latestTime = mLiveInfo.endDate - mLiveInfo.startDate;
                        ObjectAnimator animation = ObjectAnimator.ofInt(mProgressBar, "progress", 10000);
                        animation.setDuration(latestTime);
                        animation.setInterpolator(new LinearInterpolator());
                        animation.start();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        mDatabase
                .child(getString(R.string.comment_history))
                .child(mLiveKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Long latestTime = 0L;
                        for (final DataSnapshot child : dataSnapshot.getChildren()) {
                            final Comment comment = child.getValue(Comment.class);
                            latestTime = Math.max(latestTime, comment.getTime());
                            Long passedTime = System.currentTimeMillis() - mStartedTime;
                            Long timeAfter = comment.getTime() - passedTime;
                            if (timeAfter < 0) {
                                continue;
                            }
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                   addComment(comment, child.getKey());
                                }
                            }, timeAfter);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


        mDatabase
                .child(getString(R.string.firebase_db_emotion_history))
                .child(mLiveKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            final EmotionModel emotionHistory = child.getValue(EmotionModel.class);
                            long timeAfter = emotionHistory.timeStamp;
                            if (timeAfter < 0) {
                                continue;
                            }
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    showEmotion(emotionHistory);
                                }
                            }, timeAfter);
                        }
                    }


                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


    }

    public void addDataChangeListeners() {

        mLiveStateChangeListener =
                mDatabase
                        .child(getString(R.string.firebase_db_live_list))
                        .child(mLiveKey)
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                LiveInfo liveInfo = dataSnapshot.getValue(LiveInfo.class);
                                if (liveInfo.state.equals(getString(R.string.live_state_over))) {
                                    new LiveEndConfirmDialog(ReaderLiveActivity.this).show();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

        mNewScrollAddedListener =
                mDatabase
                        .child(getString(R.string.firebase_db_scroll_history))
                        .child(mLiveKey)
                        .addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                                VerticalPositionChanged data = dataSnapshot.getValue(VerticalPositionChanged.class);
                                double percentage = data.offsetProportion;

                                int nextY = (int) (percentage * mDeviceWidth);
                                int curY = mRecyclerView.computeVerticalScrollOffset();
                                Log.d("scroll_debug", " percentage : "+ percentage + " ,  nextY : " + nextY + ", curY : " + curY + "diff" + (nextY - curY));
                                mRecyclerView.smoothScrollBy(0, nextY - curY);
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


    private void settingCommentListeners() {

        mWriterCommentAddedListener =
                mDatabase.child(getString(R.string.comment_history))
                        .child(mLiveKey)
                        .addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                                final Comment comment = dataSnapshot.getValue(Comment.class);
                                addComment(comment, dataSnapshot.getKey());

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


    private void addComment(final Comment comment, final String tmpKey){
        Comment tmp = new Comment();
        tmp = comment;

        final CommentView commentPointView = new CommentView(this);
        float widthRate = (float) deviceWidth / comment.getDeviceWidth();
        double rate = mRecyclerView.computeVerticalScrollRange()/comment.getScrollLength();

        commentPointView.setCommentText(tmp.getContent());
        commentPointView.setTag(tmpKey);
        commentPointView.hideOrShowView();
        commentPointView.setArrowImgPos((int)(comment.getPosX() * widthRate)-(int)convertPixelsToDp(80,this));

        RelativeLayout.LayoutParams commentPointParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        commentPointParams.setMargins( 0
                ,  (int)(comment.getPosY()*rate)-(int)convertPixelsToDp(130,this)
                ,0,0);

        commentPointView.setLayoutParams(commentPointParams);
        commentField.addView(commentPointView);

    }

    /*
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return true;
    }
    */

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mNewScrollAddedListener != null) {
            mDatabase.child(getString(R.string.firebase_db_scroll_history))
                    .child(mLiveKey)
                    .removeEventListener(mNewScrollAddedListener);
        }
        if (mLiveStateChangeListener != null) {
            mDatabase.child(getString(R.string.firebase_db_live_list))
                    .child(mLiveKey)
                    .removeEventListener(mLiveStateChangeListener);
        }
        if (mWriterCommentAddedListener != null) {
            mDatabase.child(getString(R.string.comment_history))
                    .child(mLiveKey)
                    .removeEventListener(mWriterCommentAddedListener);
        }
        if (mWriterCommentShowListener != null) {
            mDatabase.child(getString(R.string.firebase_db_comment_click_history))
                    .child(mLiveKey)
                    .removeEventListener(mWriterCommentShowListener);
        }
    }

    private void setEmotionView() {
        mEmotionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLiveInfo.state.matches(getResources().getString(R.string.live_state_on_air))) {
                    mEmotionView.inputBar.toggleShowing();
                }
            }
        });
    }

}
