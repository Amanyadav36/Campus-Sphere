package com.example.campus_sphere;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private List<User> studentList;

    public StudentAdapter(List<User> studentList) {
        this.studentList = studentList;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        User student = studentList.get(position);

        holder.name.setText(student.getName());

        // ✅ Combine Branch and Enrollment
        String details = (student.getBranch() != null ? student.getBranch() : "N/A")
                + " • " +
                (student.getEnrollment() != null ? student.getEnrollment() : "N/A");
        holder.details.setText(details);

        if (student.getProfileImage() != null && !student.getProfileImage().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(student.getProfileImage())
                    .circleCrop()
                    .into(holder.image);
        }
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView name, details; // ✅ Changed 'email' to 'details'
        ImageView image;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.studentName);
            details = itemView.findViewById(R.id.studentDetails); // ✅ Matches XML ID
            image = itemView.findViewById(R.id.studentImage);
        }
    }
}