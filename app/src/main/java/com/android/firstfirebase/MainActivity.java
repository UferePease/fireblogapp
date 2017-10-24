package com.android.firstfirebase;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
//import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MAIN_ACTIVITY";

    private RecyclerView mBlogList;

    private DatabaseReference mDatabase;
    private DatabaseReference mDatabaseUsers;
    private DatabaseReference mDatabaseLike;

    private DatabaseReference mDatabaseCurrentUser;

    private Query mQueryCurrentUser;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private boolean mProcessLike = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                if (firebaseAuth.getCurrentUser() == null){

                    Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                    // add the flag FLAG_ACTIVITY_CLEAR_TOP so the user cannot go back
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(loginIntent);
                }
//                else {
//                    checkUserExist();
//                }
            }
        };

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Blog");
        mDatabaseUsers = FirebaseDatabase.getInstance().getReference().child("Users");
        mDatabaseLike = FirebaseDatabase.getInstance().getReference().child("Likes");

//        String currentUserId = mAuth.getCurrentUser().getUid();
//
//        mDatabaseCurrentUser = FirebaseDatabase.getInstance().getReference().child("Blog");
//
//        mQueryCurrentUser = mDatabaseCurrentUser.orderByChild("uid").equalTo(currentUserId);

        mDatabase.keepSynced(true);
        mDatabaseUsers.keepSynced(true);
        mDatabaseLike.keepSynced(true);

//        mQuery = FirebaseDatabase.getInstance().getReference().child("Blog").limitToLast(50);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);

        mBlogList = (RecyclerView) findViewById(R.id.blog_list);
        mBlogList.setHasFixedSize(true);
        mBlogList.setLayoutManager(layoutManager);

        checkUserExist();
    }

    @Override
    protected void onStart(){
        super.onStart();

        mAuth.addAuthStateListener(mAuthListener);

//        checkUserExist();   // check that user account has been setup. Else redirect to setup page

//        FirebaseRecyclerOptions<Blog> options = new FirebaseRecyclerOptions.Builder<Blog>()
//                                                            .setQuery(mQuery, Blog.class)
//                                                            .build();
//
//        FirebaseRecyclerAdapter<Blog, BlogViewHolder> firebaseRecyclerAdapter =
//                                        new FirebaseRecyclerAdapter<Blog, BlogViewHolder>(options) {
//
//            @Override
//            protected void onBindViewHolder(BlogViewHolder holder, int position, Blog model) {
//                Log.d(TAG, "inside onBindViewHolder");
//
//                holder.setTitleString(model.getTitle());
//                holder.setDesc(model.getDesc());
//                holder.setImage(getApplicationContext(), model.getImage());
//
//                Log.d(TAG, "Title: " + model.getTitle());
////                Toast.makeText(MainActivity.this, model.getTitle(), Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public BlogViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//                Log.d(TAG, "inside onCreateViewHolder");
//                // Create a new instance of the ViewHolder, in this case we are using a custom
//                // layout called R.layout.message for each item
//                View view = LayoutInflater.from(parent.getContext())
//                        .inflate(R.layout.blog_row, parent, false);
//
//                return new BlogViewHolder(view);
//            }
//        };

        FirebaseRecyclerAdapter<Blog, BlogViewHolder> firebaseRecyclerAdapter =
                new FirebaseRecyclerAdapter<Blog, BlogViewHolder>(
                        Blog.class,
                        R.layout.blog_row,
                        BlogViewHolder.class,
                        mDatabase
//                        mQueryCurrentUser
        ) {

            @Override
            protected void populateViewHolder(BlogViewHolder viewHolder, Blog model, int position) {

                // get the key of item on the recyclerview
                final String post_key = getRef(position).getKey();

                viewHolder.setTitleString(model.getTitle());
                viewHolder.setDesc(model.getDesc());
                viewHolder.setImage(getApplicationContext(), model.getImage());
                viewHolder.setUsername(model.getUsername());

                // remember to call this function to set the appropriate like button at start up
                viewHolder.setLikeBtn(post_key);

                // onClicklistener for a blog item - go to detailed view
                viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Intent detailBlogIntent = new Intent(MainActivity.this, BlogDetailActivity.class);
                        detailBlogIntent.putExtra("blog_id", post_key);
                        startActivity(detailBlogIntent);
                    }
                });

                // onClicklistener for the like button
                viewHolder.mLikeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        mProcessLike = true;

                        // use valueEventListener to retrieve like data from db

                        mDatabaseLike.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                if (mProcessLike) {

                                    // check if user already liked the post
                                    if (dataSnapshot.child(post_key).hasChild(mAuth.getCurrentUser().getUid())){

                                        // if already liked, unlike the post, by removing the child
                                        // Note: removing a child's value in firebase automatically removes the key too
                                        mDatabaseLike.child(post_key).child(mAuth.getCurrentUser().getUid()).removeValue();

                                        mProcessLike = false;

                                    }else {
                                        // if not store the new like
                                        mDatabaseLike.child(post_key).child(mAuth.getCurrentUser().getUid()).setValue("Liked");

                                        mProcessLike = false;
                                    }
                                }

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                });
            }
        };

        mBlogList.setAdapter(firebaseRecyclerAdapter);

        Log.d(TAG, "onStart done");
    }


    private void checkUserExist() {

        if (mAuth.getCurrentUser() != null){

            // this should only run when the has signed in, else app will crash
            final String user_id = mAuth.getCurrentUser().getUid();

            // use addValueEventListener() to check if user exists
            mDatabaseUsers.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    if (!dataSnapshot.hasChild(user_id)){    // if the snapshot contains this user ID

                        Intent setupIntent = new Intent(MainActivity.this, SetupActivity.class);
                        // add the flag FLAG_ACTIVITY_CLEAR_TOP so the user cannot go back
                        setupIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(setupIntent);

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

//        catch (NullPointerException nullEx){
//
//            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
//            // add the flag FLAG_ACTIVITY_CLEAR_TOP so the user cannot go back
//            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            startActivity(loginIntent);
//        }


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_add ){
            startActivity(new Intent(MainActivity.this, PostActivity.class));
        }

        if (item.getItemId() == R.id.action_logout ){
            logout();
        }

        return super.onOptionsItemSelected(item);
    }

    private void logout() {

        // When using signOut() method, ensure that the stateListener is added
        // like we did in the onStart method and that it verifies whether user is signed in or not
        mAuth.signOut();
    }


    public static class BlogViewHolder extends RecyclerView.ViewHolder {

        View mView;

        ImageButton mLikeBtn;

        TextView post_username;     // we want to set an onClickListener to a post's username

        DatabaseReference mDatabaseLike;
        FirebaseAuth mAuth;

        public BlogViewHolder(View itemView) {
            super(itemView);

            mView = itemView;

            mLikeBtn = (ImageButton) mView.findViewById(R.id.like_btn);

            mDatabaseLike = FirebaseDatabase.getInstance().getReference().child("Likes");
            mAuth = FirebaseAuth.getInstance();

            mDatabaseLike.keepSynced(true);     // helps work faster with slower connection

            post_username = (TextView) mView.findViewById(R.id.post_username);

            // onclicklistener for a post's user/owner
            post_username.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Log.d(TAG, "Username Clicked");
                }
            });
        }

        public void setLikeBtn(final String post_key){

            // for realtime operation, we use the addValueEventListener
            mDatabaseLike.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    // check that user has liked the post
                    if (dataSnapshot.child(post_key).hasChild(mAuth.getCurrentUser().getUid())){

                        // change the button image (blue tint)
                        mLikeBtn.setImageResource(R.mipmap.ic_action_like_blue);
                    }else {

                        // change the button image (gray tint)
                        mLikeBtn.setImageResource(R.mipmap.ic_action_like);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        public void setTitleString(String title){

            TextView post_title = (TextView) mView.findViewById(R.id.post_title);
            post_title.setText(title);
        }

        public void setDesc(String desc){

            TextView post_desc = (TextView) mView.findViewById(R.id.post_desc);
            post_desc.setText(desc);
        }

        public void setUsername(String username){

            post_username.setText(username);
        }

        public void setImage(Context ctx, String img){

            ImageView post_image = (ImageView) mView.findViewById(R.id.post_image);
            // load image with Picasso
            Picasso.with(ctx).load(img).into(post_image);
        }
    }
}
