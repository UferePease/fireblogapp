package com.android.firstfirebase;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class BlogDetailActivity extends AppCompatActivity {

    private String mPostKey = null;

    // create reference to the database
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    // create references to the views
    private ImageView mBlogDetailImage;
    private TextView mBlogDetailTitle;
    private TextView mBlogDetailDesc;

    private Button mRemoveBlogBtn;

    private String post_uid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blog_detail);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Blog");

        mAuth = FirebaseAuth.getInstance();

        mPostKey = getIntent().getExtras().getString("blog_id");

        mBlogDetailDesc = (TextView) findViewById(R.id.singleBlogDesc);
        mBlogDetailImage = (ImageView) findViewById(R.id.singleBlogImage);
        mBlogDetailTitle = (TextView) findViewById(R.id.singleBlogTitle);

        mRemoveBlogBtn = (Button) findViewById(R.id.removeBlogBtn);

        // Toast.makeText(this, mPostKey, Toast.LENGTH_LONG).show();

        // reference the specific blog the user has clicked
        // and use addValueEventListener to get the blog details
        mDatabase.child(mPostKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String post_title = (String) dataSnapshot.child("title").getValue();
                String post_desc = (String) dataSnapshot.child("desc").getValue();
                String post_image = (String) dataSnapshot.child("image").getValue();
                post_uid = (String) dataSnapshot.child("uid").getValue();

                // populate the views
                mBlogDetailTitle.setText(post_title);
                mBlogDetailDesc.setText(post_desc);

                Picasso.with(BlogDetailActivity.this).load(post_image).into(mBlogDetailImage);

                // show remove post button if the current user is the owner of the post
                if (mAuth.getCurrentUser().getUid().equals(post_uid)){

                    mRemoveBlogBtn.setVisibility(View.VISIBLE);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mRemoveBlogBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // remove post if the current user is the owner of the post
                if (mAuth.getCurrentUser().getUid().equals(post_uid)){

                    mDatabase.child(mPostKey).removeValue();

                    // redirect to MainActivity
                    Intent mainIntent = new Intent(BlogDetailActivity.this, MainActivity.class);
                    startActivity(mainIntent);
                }
            }
        });
    }
}
