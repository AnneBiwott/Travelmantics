package com.example.travelmantics;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.ui.auth.data.model.Resource;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;


public class Insert extends AppCompatActivity {

    private FirebaseDatabase firebaseDatabase;
    private static final int PICTURE_RESULT = 42;
    private DatabaseReference databaseReference;
    private EditText mTitle, mDesc, mPrice;
    TravelDeal deal;
    Button mUpload;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.insert_activity);

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_chev);

        FirebaseUtil.openFbReference("traveldeals", this);
        firebaseDatabase = FirebaseUtil.firebaseDatabase;
        databaseReference = FirebaseUtil.databaseReference;


        mTitle = findViewById(R.id.title);
        mDesc = findViewById(R.id.desc);
        mPrice = findViewById(R.id.price);
        imageView = findViewById(R.id.imageDeal);
        mUpload = findViewById(R.id.uploadDeal);


        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertDeal();
            }
        });

        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("Deal");

        if (deal == null) {
            deal = new TravelDeal();
        }
//        this.deal = deal;

        mTitle.setText(deal.getTitle());
        mDesc.setText(deal.getDescription());
        mPrice.setText(deal.getPrice());
        showImage(deal.getImageUrl());


    }

    private void insertDeal() {
        deal.setTitle(mTitle.getText().toString().trim());
        deal.setDescription(mDesc.getText().toString().trim());
        deal.setPrice(mPrice.getText().toString().trim());

        if (deal.getId() == null) {
            databaseReference.push().setValue(deal);
            Intent intent = getIntent();
            upload(mUpload);

            Toast.makeText(this, "Saved Succesfully", Toast.LENGTH_SHORT).show();
            home();
        } else {
            databaseReference.child(deal.getId()).setValue(deal);
            Toast.makeText(this, "Edited Succesfully", Toast.LENGTH_SHORT).show();
            home();
        }

    }

    private void upload(Button mUpload) {

        mUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(intent.createChooser(intent, "add images"), PICTURE_RESULT);
            }
        });
    }

    private void deleteDeal() {
        if (deal == null) {
            Toast.makeText(this, "Does Not Exist ", Toast.LENGTH_SHORT).show();
            return;
        }
        databaseReference.child(deal.getId()).removeValue();
        Toast.makeText(this, "Removed Successfully", Toast.LENGTH_SHORT).show();
        home();
    }

    private void home() {
        startActivity(new Intent(this, TravelDeals.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICTURE_RESULT && resultCode == RESULT_OK) {
            assert data != null;
            final Uri imageUri = data.getData();
            final StorageReference
                    reference = FirebaseUtil.mStorageRef.child(imageUri.getLastPathSegment());
            UploadTask uploadTask = reference.putFile(imageUri);
            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return reference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        assert downloadUri != null;
                        String imageUrl = downloadUri.toString();
                        String imageName = task.getResult().getLastPathSegment();
                        Log.d("imageUrl", "onSuccess: "+downloadUri.toString());
                        deal.setImageUrl(imageUrl);
                        deal.setImageName(imageName);
                        showImage(imageUrl);
                    } else {
                        Toast.makeText(Insert.this, "Picture couldn't be uploaded", Toast.LENGTH_LONG).show();
                    }
                }
            });

        }
    }
    public void editImage(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY,true);
        startActivityForResult(Intent.createChooser(intent, "Insert Picture"), PICTURE_RESULT);
    }

    private void showImage(String url) {
        if (url != null && url.isEmpty() == false) {
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.get()
                    .load(url)
                    .resize(width, width * 2 / 3)
                    .centerCrop()
                    .into(imageView);
        }
    }

}
