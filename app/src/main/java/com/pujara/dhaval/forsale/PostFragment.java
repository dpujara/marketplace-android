package com.pujara.dhaval.forsale;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.pujara.dhaval.forsale.models.Post;
import com.pujara.dhaval.forsale.util.UniversalImageLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

public class PostFragment extends Fragment implements SelectPhotoDialog.OnPhotoSelectedListener {
    private static final String TAG = "PostFragment";

    //widgets
    private ImageView mPostImage;
    private EditText mTitle, mDescription, mPrice, mCountry, mStateProvince, mCity, mContactEmail;
    private Button mPost;
    private ProgressBar mProgressBar;
    private Bitmap mSelectedBitmap;
    private Uri mSelectedUri;
    private byte[] mUploadBytes;
    private double mProgress = 0;
    //vars
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_post, container, false);
        mPostImage = view.findViewById(R.id.post_image);
        mTitle = view.findViewById(R.id.input_title);
        mDescription = view.findViewById(R.id.input_description);
        mPrice = view.findViewById(R.id.input_price);
        mCountry = view.findViewById(R.id.input_country);
        mStateProvince = view.findViewById(R.id.input_state_province);
        mCity = view.findViewById(R.id.input_city);
        mContactEmail = view.findViewById(R.id.input_email);
        mPost = view.findViewById(R.id.btn_post);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        Objects.requireNonNull(getActivity()).getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        init();
        return view;
    }

    private void init(){

        mPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: opening dialog to choose new photo");
                SelectPhotoDialog dialog = new SelectPhotoDialog();
                dialog.show(getFragmentManager(),getString(R.string.dialog_select_photo));
                dialog.setTargetFragment(PostFragment.this,1);
            }
        });

        mPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Attempting to Post");
                if(!isEmpty(mTitle.getText().toString()) && !isEmpty(mDescription.getText().toString())
                && !isEmpty(mPrice.getText().toString()) && !isEmpty(mCountry.getText().toString())
                && !isEmpty(mStateProvince.getText().toString()) && !isEmpty(mCity.getText().toString())
                && !isEmpty(mContactEmail.getText().toString())){
                    //We have bitmap but no uri
                    if(mSelectedBitmap!=null && mSelectedUri == null){
                        uploadNewPhoto(mSelectedBitmap);
                    }else if(mSelectedBitmap == null && mSelectedUri !=null){
                        uploadNewPhoto(mSelectedUri);
                    }else {
                        Toast.makeText(getActivity(),"You must fill all the fields",Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }


    private void uploadNewPhoto(Uri imagePath) {
        Log.d(TAG, "uploadNewPhoto: uploading a new image to storage");
        BackGroundImageResize resize = new BackGroundImageResize(null);
        resize.execute(imagePath);
    }

    private void uploadNewPhoto(Bitmap bitmap) {
        Log.d(TAG, "uploadNewPhoto: bitmap");
        BackGroundImageResize resize = new BackGroundImageResize(bitmap);
        Uri uri = null;
        resize.execute(uri);
    }

    public class BackGroundImageResize extends AsyncTask<Uri,Integer,byte[]>{
        Bitmap mBitmap;

        public BackGroundImageResize(Bitmap mBitmap) {
            this.mBitmap = mBitmap;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(getActivity(),"Coompressing Image",Toast.LENGTH_LONG).show();
            showProgressBar();
        }

        @Override
        protected byte[] doInBackground(Uri... uris) {
            Log.d(TAG, "doInBackground: Started");
            if(mBitmap==null){
                try {
                    mBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(),uris[0]);
                }catch (IOException e){
                    Log.e(TAG, "doInBackground: : IO Exception" + e.getMessage() );
                }
            }
            byte[] bytes = null;
            Log.d(TAG, "doInBackground: mega bytes " + mBitmap.getByteCount() / 1000000 );
            bytes = getBytesFromBitmap(mBitmap,100);
            Log.d(TAG, "doInBackground: mega bytes " + bytes.length / 1000000 );
            return bytes;
        }

        @Override
        protected void onPostExecute(byte[] bytes) {
            super.onPostExecute(bytes);
            mUploadBytes = bytes;
            hideProgressBar();
            //execute the upload task

            executeUploadTask();
        }
    }


    private void executeUploadTask(){
        Toast.makeText(getActivity(),"Uploading Image",Toast.LENGTH_LONG).show();

        final String postId = FirebaseDatabase.getInstance().getReference().push().getKey();

        final StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("posts/users/"
                + FirebaseAuth.getInstance().getCurrentUser().getUid() + "/" + postId + "/post_image");
        UploadTask uploadTask = storageReference.putBytes(mUploadBytes);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(getActivity(),"Post Success",Toast.LENGTH_LONG).show();

                //isert the download url in firebase database
                final Uri[] firebaseUri = new Uri[1];

                storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        firebaseUri[0] = uri;
                        Log.d(TAG, "onSuccess: Firebase Uri " + firebaseUri[0].toString());

                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

                        Post post  = new Post();
                        post.setImage(firebaseUri[0].toString());
                        post.setCity(mCity.getText().toString());
                        post.setContact_email(mContactEmail.getText().toString());
                        post.setCountry(mCountry.getText().toString());
                        post.setDescription(mDescription.getText().toString());
                        post.setPost_id(postId);
                        post.setPrice(mPrice.getText().toString());
                        post.setState_province(mStateProvince.getText().toString());
                        post.setTitle(mTitle.getText().toString());
                        post.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());

                        reference.child(getString(R.string.node_posts)).child(postId).setValue(post);
                        resetFields();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getActivity(),"Could not upload photo",Toast.LENGTH_LONG).show();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double currentProgress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                if(currentProgress > mProgress + 15){
                    mProgress =(100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    Log.d(TAG, "onProgress: upload is " + mProgress + " % done");
                    Toast.makeText(getActivity(),"Progress = " + mProgress,Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    public static byte[] getBytesFromBitmap(Bitmap bitmap,int quality){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,quality,stream);
        return stream.toByteArray();
    }



    private void resetFields(){
        UniversalImageLoader.setImage("", mPostImage);
        mTitle.setText("");
        mDescription.setText("");
        mPrice.setText("");
        mCountry.setText("");
        mStateProvince.setText("");
        mCity.setText("");
        mContactEmail.setText("");
    }

    private void showProgressBar(){
        mProgressBar.setVisibility(View.VISIBLE);

    }

    private void hideProgressBar(){
        if(mProgressBar.getVisibility() == View.VISIBLE){
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Return true if the @param is null
     * @param string
     * @return
     */
    private boolean isEmpty(String string){
        return string.equals("");
    }

    @Override
    public void getImagePath(Uri imagePath) {
        Log.d(TAG, "getImagePath: setting image to image view");
        UniversalImageLoader.setImage(imagePath.toString(),mPostImage);
        //assign to global variable
        mSelectedBitmap = null;
        mSelectedUri = imagePath;
    }

    @Override
    public void getImageBitmap(Bitmap bitmap) {
        mPostImage.setImageBitmap(bitmap);
        //assign to global variable
        mSelectedUri = null;
        mSelectedBitmap = bitmap;
    }
}
