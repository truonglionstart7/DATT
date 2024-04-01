package com.example.doan.adapter;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doan.activity.FullscreenVideoActivity;
import com.example.doan.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class BinVideoAdapter extends RecyclerView.Adapter<BinVideoAdapter.BinVideoViewHolder> {
    private List<String> mVideoUrls;
    private Context mContext;
    private Picasso mPicasso;
    public SparseBooleanArray mSelectedItems;
    private ActionMode actionMode;
    private String videoTitle;

    public BinVideoAdapter(Context context, List<String> videoUrls) {
        mVideoUrls = videoUrls;
        mContext = context;
        mPicasso = Picasso.get();
        mSelectedItems = new SparseBooleanArray();
    }
    public ActionMode.Callback getCallback() {
        return callback;
    }
    @NonNull
    @Override
    public BinVideoAdapter.BinVideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.my_item_layout1, parent, false);
        return new BinVideoAdapter.BinVideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BinVideoAdapter.BinVideoViewHolder holder, int position) {
        //Tên video
        String videoUrl = mVideoUrls.get(position);
        getVideoTitleFromFirebaseStorage(videoUrl).addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String videoTitle) {
                BinVideoAdapter.this.videoTitle = videoTitle; // Gán giá trị cho biến thành viên
                holder.videoName.setText(videoTitle);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Xử lý khi không thể lấy tên nhạc
            }
        });
        // Tải thumbnail của video sử dụng Picasso
        // Tải video từ Firebase Storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(videoUrl);
        File localFile;
        try {
            localFile = File.createTempFile("video", "mp4");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        storageRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                // Lấy thumbnail của video từ video đã tải xuống
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(localFile.getAbsolutePath());
                Bitmap thumbnail = retriever.getFrameAtTime();

                // Hiển thị thumbnail trong ImageView
                if (thumbnail != null) {
                    holder.myVideoView.setImageBitmap(thumbnail);
                } else {
                    // Nếu không có thumbnail, hiển thị placeholder
                    holder.myVideoView.setImageResource(R.drawable.placeholder_video);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Xử lý khi không thể tải video từ Firebase Storage
            }
        });
        mPicasso.load(videoUrl)
                .placeholder(R.drawable.placeholder_video)
                .into(holder.myVideoView);

        // Xác định trạng thái của ảnh
        boolean isSelected = mSelectedItems.get(position);
        if (isSelected) {
            holder.checkView.setVisibility(View.VISIBLE);
            holder.myVideoView.setTag("selected");
        } else {
            holder.checkView.setVisibility(View.GONE);
            holder.myVideoView.setTag(null);
        }

        // Bắt sự kiện click vào ImageView
        holder.myVideoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Chuyển sang một Activity khác và truyền đường dẫn của ảnh được click qua Intent
                Intent intent = new Intent(mContext, FullscreenVideoActivity.class);
                intent.putExtra("videoUrl", videoUrl);
                mContext.startActivity(intent);
            }
        });

        // Bắt sự kiện long click vào ImageView
        holder.myVideoView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                // Kiểm tra xem ảnh được nhấn có được chọn hay không
                boolean isSelected = mSelectedItems.get(position);
                if (isSelected) {
                    // Nếu đã được chọn lần trước đó, ẩn checkView và xóa khỏi danh sách các item đã chọn
                    holder.checkView.setVisibility(View.GONE);
                    mSelectedItems.delete(position);
                } else {
                    // Nếu chưa được chọn, đánh dấu là đã chọn và hiển thị checkView
                    isSelected = true;
                    mSelectedItems.put(position, isSelected);
                    holder.checkView.setVisibility(View.VISIBLE);
                    holder.myVideoView.setTag("selected");
                    // Kích hoạt Contextual action bar nếu chưa có và chưa có item nào được chọn
                    if (actionMode == null && mSelectedItems.size() == 1) {
                        actionMode = view.startActionMode(callback);
                    }
                }

                // Kiểm tra xem có còn item nào được chọn hay không
                if (mSelectedItems.size() == 0) {
                    // Nếu không còn, kết thúc Contextual action bar
                    actionMode.finish();
                    actionMode = null;
                }

                return true;
            }
        });
    }
    private ActionMode.Callback callback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.contextual_action_bar_1, menu);
            // Check if at least one item is selected
            boolean hasSelection = false;
            for (int i = 0; i < mSelectedItems.size(); i++) {
                if (mSelectedItems.valueAt(i)) {
                    hasSelection = true;
                    break;
                }
            }

            // If no items are selected, hide the Contextual action bar
            if (!hasSelection) {
                mode.finish();
                return false;
            }

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {

                case R.id.delete_1:
                    // Tạo một hộp thoại AlertDialog.Builder mới
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setMessage("Bạn có chắc muốn xóa video đã chọn không?");

                    // Thêm nút Yes vào hộp thoại
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Xóa các ảnh được chọn khỏi Firebase Storage và danh sách mImageUrls
                            ProgressDialog progressDialog =new ProgressDialog(mContext);
                            progressDialog.setCancelable(false);
                            progressDialog.setMessage("Đang xóa video...");
                            progressDialog.show();
                            for (int i = mSelectedItems.size() - 1; i >= 0; i--) {
                                int position = mSelectedItems.keyAt(i);
                                if (mSelectedItems.get(position)) {
                                    String videoUrl = mVideoUrls.get(position);
                                    // Tạo một StorageReference từ URL ảnh
                                    StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(videoUrl);
                                    storageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            if (position < mVideoUrls.size()) { // Kiểm tra xem position có hợp lệ không
                                                mVideoUrls.remove(position);
                                                mSelectedItems.delete(position);
                                                notifyDataSetChanged();
                                                Toast.makeText(mContext, "Đã xóa video thành công!", Toast.LENGTH_SHORT).show();
                                                progressDialog.dismiss();
                                            }
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(mContext, "Lỗi xóa video", Toast.LENGTH_SHORT).show();
                                            progressDialog.dismiss();
                                        }
                                    });
                                }
                            }
                            mode.finish(); // Kết thúc ActionMode
                        }
                    });

                    // Thêm nút No vào hộp thoại
                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Không làm gì cả
                        }
                    });

                    // Hiển thị hộp thoại
                    AlertDialog dialog = builder.create();
                    dialog.show();

                    return true;
                case R.id.khoi_phuc_anh:
                    // Xóa các ảnh được chọn khỏi Firebase Storage và danh sách mImageUrls
                    ProgressDialog progressDialog1 = new ProgressDialog(mContext);
                    progressDialog1.setCancelable(false);
                    progressDialog1.setMessage("Đang hoàn tác video...");
                    progressDialog1.show();
                    for (int i = mSelectedItems.size() - 1; i >= 0; i--) {
                        int position = mSelectedItems.keyAt(i);
                        if (mSelectedItems.get(position)) {
                            String videoUrl = mVideoUrls.get(position);
                            // Tạo một StorageReference từ URL ảnh
                            StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(videoUrl);
                            storageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                @Override
                                public void onSuccess(byte[] bytes) {
                                    // Tạo một StorageReference mới tới thư mục "image"
                                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                    StorageReference deleteRef = FirebaseStorage.getInstance().getReference().child("video/" +user.getUid()+"/"+ storageRef.getName());
                                    // Copy ảnh vào thư mục "image" trên Firebase Storage
                                    deleteRef.putBytes(bytes).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                            // Xóa ảnh khỏi Firebase Storage
                                            storageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    if (position < mVideoUrls.size()) { // Kiểm tra xem position có hợp lệ không
                                                        // Xóa URL ảnh khỏi danh sách mImageUrls
                                                        mVideoUrls.remove(position);
                                                        // Xóa phần tử tương ứng trong SparseBooleanArray
                                                        mSelectedItems.delete(position);
                                                        // Cập nhật lại giao diện người dùng
                                                        notifyDataSetChanged();
                                                        progressDialog1.dismiss();
                                                        Toast.makeText(mContext, "Hoàn tác video thành công!", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    // Xử lý lỗi nếu xóa không thành công
                                                    Toast.makeText(mContext, "Lỗi video", Toast.LENGTH_SHORT).show();
                                                    progressDialog1.dismiss();
                                                }
                                            });
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Xử lý lỗi nếu copy không thành công
                                            Toast.makeText(mContext, "Lỗi hoàn tác video", Toast.LENGTH_SHORT).show();
                                            progressDialog1.dismiss();
                                        }
                                    });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Xử lý lỗi nếu không tải được tệp tin ảnh
                                    Toast.makeText(mContext, "Lỗi đường dẫn video", Toast.LENGTH_SHORT).show();
                                    progressDialog1.dismiss();
                                }
                            });
                        }
                    }
                    mode.finish(); // Kết thúc ActionMode
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Clear all selected items
            mSelectedItems.clear();
            // Update the UI
            notifyDataSetChanged();
        }
    };

    private Task<String> getVideoTitleFromFirebaseStorage(String videoUrl) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl(videoUrl);

        return storageRef.getMetadata().continueWith(task -> {
            if (task.isSuccessful()) {
                StorageMetadata storageMetadata = task.getResult();
                if (storageMetadata != null) {
                    return storageMetadata.getName();
                } else {
                    // Xử lý khi không có thông tin metadata
                    return null;
                }
            } else {
                // Xử lý khi không thể lấy thông tin metadata
                Exception exception = task.getException();
                // ...
                return null;
            }
        });
    }
    @Override
    public int getItemCount() {
        return mVideoUrls.size();
    }

    public static class BinVideoViewHolder extends RecyclerView.ViewHolder {
        ImageView myVideoView;
        ImageView checkView;
        TextView videoName;
        public BinVideoViewHolder(View itemView) {
            super(itemView);
            myVideoView = itemView.findViewById(R.id.my_video_view);
            checkView = itemView.findViewById(R.id.check_view);
            videoName = itemView.findViewById(R.id.video_name);
        }
    }
}