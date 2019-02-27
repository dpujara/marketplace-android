package com.pujara.dhaval.forsale;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.hotspot2.pps.Credential;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.pujara.dhaval.forsale.models.HitsList;
import com.pujara.dhaval.forsale.models.HitsObject;
import com.pujara.dhaval.forsale.models.Post;
import com.pujara.dhaval.forsale.util.ElasticSearchAPI;
import com.pujara.dhaval.forsale.util.PostListAdapter;
import com.pujara.dhaval.forsale.util.RecyclerViewMargin;

import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import okhttp3.Credentials;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.HeaderMap;

public class SearchFragment extends Fragment {
    private static final String TAG = "SearchFragment";
    private static final String baseUrl = "http://34.73.40.107//elasticsearch/posts/post/";
    //widget

    private ImageView mFilters;
    private EditText mSearchText;
    private FrameLayout mFrameLayout;

    private String mElasticSearchPassword;
    private String mPreferredCity;
    private String mPreferredProvince;
    private String mPreferredCountry;
    private static final int NUM_GRID_COLUMNS = 3;
    private static final int GRID_ITEN_MARGIN = 5;
    private RecyclerView mRecyclerView;
    private PostListAdapter postListAdapter;
    private ArrayList<Post> mPosts;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search,container,false);
        mFilters = view.findViewById(R.id.ic_search);
        mSearchText = view.findViewById(R.id.input_search);
        mRecyclerView = view.findViewById(R.id.recyclerView);
        mFrameLayout = view.findViewById(R.id.container);
        getElasticSearchPassword();
        getFilters();
        init();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getFilters();
    }

    private void init(){
        mFilters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Navigating to filters acitivity");
                Intent intent = new Intent(getActivity(),FiltersActivity.class);
                startActivity(intent);
            }
        });

        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    Log.d(TAG, "onEditorAction: Pressed Enter Search begins");
                    mPosts = new ArrayList<>();

                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();

                    ElasticSearchAPI searchAPI = retrofit.create(ElasticSearchAPI.class);

                    HashMap<String,String> headerMap = new HashMap<>();
                    headerMap.put("Authorization", Credentials.basic("user",mElasticSearchPassword));

                    String searchString = "";
                    if(!mSearchText.equals("")){
                        searchString = searchString + mSearchText.getText().toString() +"*";
                    }

                    if(!mPreferredCity.equals("")){
                        searchString = searchString +" city:" + mPreferredCity;
                    }

                    if(!mPreferredCountry.equals("")){
                        searchString = searchString +" country:" + mPreferredCountry;
                    }

                    if(!mPreferredProvince.equals("")){
                        searchString = searchString +" state_province:" + mPreferredProvince;
                    }
                    Call<HitsObject> call = searchAPI.search(headerMap,"AND",searchString);
                    call.enqueue(new Callback<HitsObject>() {
                        @Override
                        public void onResponse(Call<HitsObject> call, Response<HitsObject> response) {
                            HitsList hitsList = new HitsList();
                            String jsonResponse = "";

                            try {
                                Log.d(TAG, "onResponse: server response " + response.toString());

                                if(response.isSuccessful()){
                                    hitsList = response.body().getHits();
                                }else {
                                    jsonResponse = response.errorBody().string();
                                }
                                Log.d(TAG, "onResponse: hits " + hitsList);

                                for(int i = 0 ; i < hitsList.getPostIndex().size(); i++){
                                    Log.d(TAG, "onResponse: data " + hitsList.getPostIndex().get(i).getPost().toString());
                                    mPosts.add(hitsList.getPostIndex().get(i).getPost());
                                }
                                Log.d(TAG, "onResponse: size " + mPosts.size());

                                //setup the list of posts
                                setUpPostList();
                            }catch (NullPointerException e){
                                Log.e(TAG, "onResponse: NullException " + e.getMessage());
                            }catch (IndexOutOfBoundsException e){
                                Log.e(TAG, "onResponse: IndexOutOfBoundOException " + e.getMessage());
                            }catch (IOException e){
                                Log.e(TAG, "onResponse: IOException " + e.getMessage());
                            }
                        }
                        @Override
                        public void onFailure(Call<HitsObject> call, Throwable t) {
                            Log.e(TAG, "onFailure: " + t.getMessage());
                        }
                    });
                return false;
            }
        });
    }

    private void getElasticSearchPassword(){
        Query query = FirebaseDatabase.getInstance().getReference().child(getString(R.string.node_elasticsearch)).orderByValue();
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                DataSnapshot dataSnapshot1 = dataSnapshot.getChildren().iterator().next();
                mElasticSearchPassword = Objects.requireNonNull(dataSnapshot1.getValue()).toString();
                Log.d(TAG, "getElasticSearchPassword: " + mElasticSearchPassword);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getFilters(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mPreferredCity = preferences.getString(getString(R.string.preferences_city),"");
        mPreferredCountry = preferences.getString(getString(R.string.preferences_country),"");
        mPreferredProvince = preferences.getString(getString(R.string.preferences_state_province),"");
        Log.d(TAG, "getFilters: city " + mPreferredCity + " Country " + mPreferredCountry + " Prov " + mPreferredProvince);
    }

    public void viewPost(String postId){
        ViewPostFragment fragment = new ViewPostFragment();
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();

        Bundle args = new Bundle();
        args.putString(getString(R.string.arg_post_id),postId);
        fragment.setArguments(args);
        transaction.replace(R.id.container,fragment,getString(R.string.fragment_post));
        transaction.addToBackStack(getString(R.string.fragment_post));
        transaction.commit();
        mFrameLayout.setVisibility(View.VISIBLE);
    }

    private void setUpPostList(){
        RecyclerViewMargin itemDecorator = new RecyclerViewMargin(GRID_ITEN_MARGIN,NUM_GRID_COLUMNS);
        mRecyclerView.addItemDecoration(itemDecorator);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(),NUM_GRID_COLUMNS);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        postListAdapter = new PostListAdapter(mPosts,getActivity());
        mRecyclerView.setAdapter(postListAdapter);
    }
}
