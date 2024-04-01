package com.example.doan.adapter;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.content.Intent;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doan.activity.FullscreenImageActivity;
import com.example.doan.R;
import com.example.doan.activity.FullscreenImage_bin;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
public class BinAdapter extends RecyclerView.Adapter<BinAdapter.BinViewHolder> {
    private List<String> mImageUrls;
    private Context mContext;
    private Picasso mPicasso;
    public SparseBooleanArray mSelectedItems;
    private ActionMode actionMode;
    public BinAdapter(Context context, List<String> imageUrls) {
        mImageUrls = imageUrls;
        mContext = context;
        mPicasso = Picasso.get();
        mSelectedItems = new SparseBooleanArray();
    }
    public ActionMode.Callback getCallback() {
        return callback;
    }
    @NonNull
    @Override
    public BinViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.my_item_layout, parent, false);
        return new BinViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull BinViewHolder holder, int position) {
        String imageUrl = mImageUrls.get(position);
        mPicasso.load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .into(holder.myImageView);

        // Xác định trạng thái của ảnh
        boolean isSelected = mSelectedItems.get(position);
        updateSelection(holder, isSelected);

        // Bắt sự kiện click vào ImageView
        holder.myImageView.setOnClickListener(view -> {
            Intent intent = new Intent(mContext, FullscreenImage_bin.class);
            intent.putStringArrayListExtra("imageUrls",new ArrayList<>(mImageUrls));
            intent.putExtra("position",position);
            mContext.startActivity(intent);
        });

        // Bắt sự kiện long click vào ImageView
        holder.myImageView.setOnLongClickListener(view -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return false;
            }

            boolean currentSelection = mSelectedItems.get(adapterPosition);
            updateSelection(holder, currentSelection);

            if (currentSelection) {
                // Nếu đã được chọn lần trước đó, ẩn checkView và xóa khỏi danh sách các item đã chọn
                holder.checkView.setVisibility(View.GONE);
                mSelectedItems.delete(adapterPosition);
            } else {
                // Nếu chưa được chọn, đánh dấu là đã chọn và hiển thị checkView
                currentSelection = true;
                mSelectedItems.put(adapterPosition, currentSelection);
                holder.checkView.setVisibility(View.VISIBLE);
                holder.myImageView.setTag("selected");
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
        });
    }

    private void updateSelection(BinViewHolder holder, boolean isSelected) {
        holder.checkView.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.myImageView.setTag(isSelected ? "selected" : null);
    }

    private ActionMode.Callback callback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.contextual_action_bar_1, menu);
            // Check if at least one item is selected
            boolean hasSelection = hasSelectedItems();
            if (!hasSelection) {
                mode.finish();
                return false;
            }

            return true;
        }

        private boolean hasSelectedItems() {
            for (int i = 0; i < mSelectedItems.size(); i++) {
                if (mSelectedItems.valueAt(i)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.delete_1:
                    showDeleteConfirmationDialog(mode);
                    return true;

                case R.id.khoi_phuc_anh:
                    restoreSelectedImages(mode);
                    return true;

                default:
                    return false;
            }
        }

        private void showDeleteConfirmationDialog(ActionMode mode) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage("Bạn có chắc muốn xóa ảnh đã chọn không?")
                    .setPositiveButton("Yes", (dialog, which) -> deleteSelectedImages(mode))
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        }

        private void deleteSelectedImages(ActionMode mode) {
            ProgressDialog progressDialog = createProgressDialog("Đang xóa ảnh...");
            progressDialog.show();

            for (int i = mSelectedItems.size() - 1; i >= 0; i--) {
                int position = mSelectedItems.keyAt(i);
                if (mSelectedItems.get(position)) {
                    String imageUrl = mImageUrls.get(position);
                    StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                    storageRef.delete()
                            .addOnSuccessListener(aVoid -> {
                                if (position < mImageUrls.size()) {
                                    mImageUrls.remove(position);
                                    mSelectedItems.delete(position);
                                    notifyDataSetChanged();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(mContext, "Lỗi xóa ảnh", Toast.LENGTH_SHORT).show();
                            })
                            .addOnCompleteListener(task -> progressDialog.dismiss());
                }
            }

            mode.finish();
        }

        private void restoreSelectedImages(ActionMode mode) {
            ProgressDialog progressDialog = createProgressDialog("Đang hoàn tác ảnh...");
            progressDialog.show();

            for (int i = mSelectedItems.size() - 1; i >= 0; i--) {
                int position = mSelectedItems.keyAt(i);
                if (mSelectedItems.get(position)) {
                    String imageUrl = mImageUrls.get(position);
                    StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                    restoreImage(storageRef, position, progressDialog);
                }
            }

            mode.finish();
        }

        private void restoreImage(StorageReference storageRef, int position, ProgressDialog progressDialog) {
            storageRef.getBytes(Long.MAX_VALUE)
                    .addOnSuccessListener(bytes -> {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        StorageReference restoreRef = FirebaseStorage.getInstance()
                                .getReference()
                                .child("image/" + user.getUid() + "/" + storageRef.getName());

                        restoreRef.putBytes(bytes)
                                .addOnSuccessListener(taskSnapshot -> {
                                    storageRef.delete()
                                            .addOnSuccessListener(aVoid -> {
                                                if (position < mImageUrls.size()) {
                                                    mImageUrls.remove(position);
                                                    mSelectedItems.delete(position);
                                                    notifyDataSetChanged();
                                                    Toast.makeText(mContext, "Hoàn tác ảnh thành công!", Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(mContext, "Lỗi xóa ảnh", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnCompleteListener(task -> progressDialog.dismiss());
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(mContext, "Lỗi hoàn tác ảnh", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(mContext, "Lỗi đường dẫn ảnh", Toast.LENGTH_SHORT).show();
                    });
        }

        private ProgressDialog createProgressDialog(String message) {
            ProgressDialog progressDialog = new ProgressDialog(mContext);
            progressDialog.setCancelable(false);
            progressDialog.setMessage(message);
            return progressDialog;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Clear all selected items
            mSelectedItems.clear();
            // Update the UI
            notifyDataSetChanged();
        }
    };

    @Override
    public int getItemCount() {
        return mImageUrls.size();
    }

    public static class BinViewHolder extends RecyclerView.ViewHolder {
        ImageView myImageView;
        ImageView checkView;

        public BinViewHolder(View itemView) {
            super(itemView);
            myImageView = itemView.findViewById(R.id.my_image_view);
            checkView = itemView.findViewById(R.id.check_view);
        }
    }
}