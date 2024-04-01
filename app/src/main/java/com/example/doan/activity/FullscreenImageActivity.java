package com.example.doan.activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.util.SparseBooleanArray;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.Button;
import android.widget.ImageView;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan.NetworkChangeListener;
import com.example.doan.R;
import com.example.doan.adapter.MyAdapter;
import com.example.doan.fragment.HomeFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FullscreenImageActivity extends AppCompatActivity {
    private PhotoView mImageView;
    private Button back,back_left,back_right;
    private int position;
    private Context context;
    private MyAdapter mAdapter;
    private ArrayList<String> imageUrls;
    private String imageUrl;
    public SparseBooleanArray mSelectedItems;
    private BottomNavigationView bottom_nav_image;
    private HomeFragment homeFragment;
    NetworkChangeListener networkChangeListener = new NetworkChangeListener();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);
        back = (Button) findViewById(R.id.back);
        back_left = (Button) findViewById(R.id.back_left);
        back_right = (Button) findViewById(R.id.back_right);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        initializeView();
        // Thanh điều hướng lựa chọn
        bottom_nav_image = (BottomNavigationView) findViewById(R.id.bottom_nav_image);
        bottom_nav_image.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.share_image:
                        shareImage(imageUrl);
                        return true;
                    case R.id.edit_image:
                        return true;
                    case R.id.delete_image:
                        deleteImage(imageUrl);
                        return true;
                }
                return false;
            }
        });

    }

    private void initializeView() {
        mImageView = findViewById(R.id.image_view);
        // Lấy đường dẫn đến ảnh từ Intent
        ArrayList<String> imageUrls = getIntent().getStringArrayListExtra("imageUrls");
        position = getIntent().getIntExtra("position",0);
        // Load ảnh vào ImageView sử dụng Picasso
        imageUrl =  imageUrls.get(position);
        Picasso.get()
                .load(imageUrl)
                .into(mImageView);
        // Thiết lập khả năng zoom cho ImageView sử dụng PhotoView
        mImageView.setMaximumScale(10);
        mImageView.setMediumScale(5);
        mImageView.setMinimumScale(1);
        mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mImageView.setZoomable(true);

        back_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (position > 0) {
                    position -= 1;
                    imageUrl = imageUrls.get(position);
                    Picasso.get().load(imageUrl).into(mImageView);
                }
            }
        });

        back_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (position < imageUrls.size() - 1) {
                    position += 1;
                    imageUrl = imageUrls.get(position);
                    Picasso.get().load(imageUrl).into(mImageView);
                }
            }
        });
    }

    private void shareImage(String imageUrl) {
        // Thêm dữ liệu ảnh vào intent
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
        try {
            final File localFile = File.createTempFile("image", ".jpg");
            storageRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Uri imageUri = FileProvider.getUriForFile(FullscreenImageActivity.this, getPackageName() + ".provider", localFile);
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/*");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                    startActivity(Intent.createChooser(shareIntent, "Chia sẻ hình ảnh"));
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Toast.makeText(FullscreenImageActivity.this, "Chia sẻ ảnh thất bại", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void deleteImage(String imageUrl1) {
        // Tạo một danh sách tạm thời để sao lưu các giá trị của imageUrls
        ArrayList<String> tempImageUrls = new ArrayList<>();

        // Tạo progressDialog
        ProgressDialog progressDialog = new ProgressDialog(FullscreenImageActivity.this);
        progressDialog.setMessage("Đang xóa ảnh...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Xóa ảnh từ Firebase Storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
        FirebaseUser user  = FirebaseAuth.getInstance().getCurrentUser();
        StorageReference deleteRef = FirebaseStorage.getInstance().getReference().child("delete/" + user.getUid() + "/" + storageRef.getName());

        // Copy file to deleteRef
        storageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                deleteRef.putBytes(bytes).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            // Delete the original file
                            storageRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        // Xóa URL của ảnh khỏi danh sách imageUrls
                                        tempImageUrls.remove(imageUrl);

                                        //Cập nhật Recycleview
                                        if (homeFragment != null) {
                                            homeFragment.updateRecyclerView();
                                        }
                                        // Cập nhật imageUrls và position
                                        imageUrls = tempImageUrls;
                                        if (position >= imageUrls.size()) {
                                            position = imageUrls.size() - 1;
                                        }

                                        // Hiển thị ảnh mới
                                        if (position >= 0) {
                                            imageUrl = imageUrls.get(position);
                                            Picasso.get().load(imageUrl).into(mImageView);
                                        } else {
                                            // Không còn ảnh trong danh sách, kết thúc FullscreenImageActivity
                                            finish();
                                        }

                                        // Ẩn progressDialog
                                        progressDialog.dismiss();

                                        // Hiển thị thông báo xóa thành công
                                        Toast.makeText(FullscreenImageActivity.this, "Xóa ảnh thành công", Toast.LENGTH_SHORT).show();
                                    } else {
                                        // Xóa ảnh không thành công, hiển thị thông báo lỗi
                                        progressDialog.dismiss();
                                        Toast.makeText(FullscreenImageActivity.this, "Xóa ảnh thất bại", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        } else {
                            // Copy file không thành công, hiển thị thông báo lỗi
                            progressDialog.dismiss();
                            Toast.makeText(FullscreenImageActivity.this, "Xóa ảnh thất bại", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeListener, filter);
        super.onStart();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(networkChangeListener);
        super.onStop();
    }
}