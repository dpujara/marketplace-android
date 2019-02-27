package com.pujara.dhaval.forsale.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.pujara.dhaval.forsale.R;
import com.pujara.dhaval.forsale.SearchActivity;
import com.pujara.dhaval.forsale.SearchFragment;
import com.pujara.dhaval.forsale.WatchListFragment;
import com.pujara.dhaval.forsale.models.Post;

import java.util.ArrayList;

public class PostListAdapter  extends  RecyclerView.Adapter<PostListAdapter.ViewHolder>{

    private static final String TAG = "PostListAdapter";
    private static final int NUM_GRID_COLUMNS = 3;

    private ArrayList<Post> mPosts;
    private Context mContext;

    public class ViewHolder extends RecyclerView.ViewHolder{
        SquareImageView mImageView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.post_image);
            int gridWidth = mContext.getResources().getDisplayMetrics().widthPixels;
            int imageWidth = gridWidth / NUM_GRID_COLUMNS;
            mImageView.setMaxHeight(imageWidth);
            mImageView.setMaxWidth(imageWidth);
        }
    }

    public PostListAdapter(ArrayList<Post> mPosts, Context mContext) {
        this.mPosts = mPosts;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.layout_view_post,viewGroup,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        UniversalImageLoader.setImage(mPosts.get(i).getImage(),viewHolder.mImageView);
        final int pos = i;
        viewHolder.mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: selected a post");

                //view the post in more detail

                Fragment fragment = (Fragment) ((SearchActivity) mContext).getSupportFragmentManager()
                        .findFragmentByTag("android:switcher:" + R.id.viewpager_container + ":" + ((SearchActivity)mContext).mViewPager.getCurrentItem());

                if(fragment!=null){
                    //search fragment (#0)
                    if(fragment.getTag().equals("android:switcher:" + R.id.viewpager_container+":0")){
                        Log.d(TAG, "onClick: switching to" + mContext.getString(R.string.fragment_post));

                        SearchFragment searchFragment = (SearchFragment) ((SearchActivity) mContext).getSupportFragmentManager()
                                .findFragmentByTag("android:switcher:" + R.id.viewpager_container + ":" + ((SearchActivity)mContext).mViewPager.getCurrentItem());
                        searchFragment.viewPost(mPosts.get(pos).getPost_id());
                    }
                    //watch list (#1)
                    else if(fragment.getTag().equals("android:switcher:" + R.id.viewpager_container+":1")){
                        Log.d(TAG, "onClick: switching to" + mContext.getString(R.string.fragment_watch_list));
                        WatchListFragment watchListFragment = (WatchListFragment) ((SearchActivity) mContext).getSupportFragmentManager()
                                .findFragmentByTag("android:switcher:" + R.id.viewpager_container + ":" + ((SearchActivity)mContext).mViewPager.getCurrentItem());
                        watchListFragment.viewPost(mPosts.get(pos).getPost_id());
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPosts.size();
    }
}
