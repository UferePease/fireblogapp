package com.android.firstfirebase;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class PostActivity extends AppCompatActivity {

    private ImageButton mSelectImage;
    private EditText mPostTitle;
    private EditText mPostDesc;

    private Button mSubmitBtn;

    private Uri mImageUri = null;

    private static final int GALLERY_REQUEST = 1;

    // FireBase storage and Database
    private StorageReference mStorage;
    private DatabaseReference mDatabase;

    private ProgressDialog mProgress;

    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mDatabaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();

        // point to the root directory of our storage and database
        mStorage = FirebaseStorage.getInstance().getReference();
        // we are storing to the root of the database here
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Blog");

        mDatabaseUser = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid());

        mSelectImage = (ImageButton) findViewById(R.id.imageSelect);

        mPostTitle = (EditText) findViewById(R.id.titleField);
        mPostDesc = (EditText) findViewById(R.id.descField);

        mSubmitBtn = (Button) findViewById(R.id.submitBtn);

        mProgress = new ProgressDialog(this);

        mSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_REQUEST);
            }
        });

        mSubmitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startPosting();
            }
        });
    }

    private void startPosting(){

        mProgress.setMessage("Submitting blog");

        final String title_val = mPostTitle.getText().toString().trim();
        final String desc_val = mPostDesc.getText().toString().trim();

        if (!TextUtils.isEmpty(title_val) && !TextUtils.isEmpty(desc_val) && mImageUri != null){

            mProgress.show();

            // create the file path to store files.
            // Note: The getLastPathSegment() method returns the name of the image file
            StorageReference filepath = mStorage.child("Blog_Images").child(mImageUri.getLastPathSegment());

            filepath.putFile(mImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    // get download url
                    final Uri downloadUrl = taskSnapshot.getDownloadUrl();

                    // create another database reference and create a unique random id with push()
                    final DatabaseReference newPost = mDatabase.push();



                    // use ValueEventListener to retrieve the name of the user
                    mDatabaseUser.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            // only post the data to database if the user is obztained
                            newPost.child("title").setValue(title_val);
                            newPost.child("desc").setValue(desc_val);
                            newPost.child("image").setValue(downloadUrl.toString());
                            newPost.child("uid").setValue(mCurrentUser.getUid());

                            // now assign this post to the user
                            newPost.child("username").setValue(dataSnapshot.child("name")
                                    .getValue()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    if (task.isSuccessful()){

                                        // redirect to the main activity after posting
                                        startActivity(new Intent(PostActivity.this, MainActivity.class));
                                    }

                                }
                            });

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                    mProgress.dismiss();

                }
            });
        }
    }


    // when the user have selected an image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        // recall GALLERY_REQUEST is the variable we created above
        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK){

            mImageUri = data.getData();  // returns the data selected

            mSelectImage.setImageURI(mImageUri);
        }
    }
}
