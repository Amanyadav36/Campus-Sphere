package com.example.campus_sphere;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {

    private final List<User> users;
    private final FirebaseFirestore db;
    private final String[] roles = {"user", "leader", "admin"};

    public AdminUserAdapter(List<User> users, FirebaseFirestore db) {
        this.users = users;
        this.db = db;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);

        holder.name.setText(user.getName() != null ? user.getName() : "Unknown");
        holder.email.setText(user.getEmail() != null ? user.getEmail() : "No email");

        String branch = user.getBranch() != null ? user.getBranch() : "Branch";
        String enrollment = user.getEnrollment() != null ? user.getEnrollment() : "Enrollment";
        holder.meta.setText(branch + " • " + enrollment);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                holder.itemView.getContext(),
                android.R.layout.simple_spinner_item,
                roles
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.roleSpinner.setAdapter(adapter);

        String role = user.getRole() != null ? user.getRole() : "student";
        int selectedIndex = 0;
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equals(role)) {
                selectedIndex = i;
                break;
            }
        }
        holder.roleSpinner.setSelection(selectedIndex, false);

        holder.roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selectedRole = roles[pos];
                if (!selectedRole.equals(role)) {
                if (user.getUid() == null || user.getUid().isEmpty()) {
                    Toast.makeText(holder.itemView.getContext(), "User ID missing", Toast.LENGTH_SHORT).show();
                    return;
                }
                db.collection("users").document(user.getUid())
                        .update("role", selectedRole)
                        .addOnSuccessListener(aVoid ->
                                {
                                    Toast.makeText(holder.itemView.getContext(), "Role updated", Toast.LENGTH_SHORT).show();
                                    AdminAuditLogger.log("ROLE_UPDATE", "user", user.getUid(), role, selectedRole);
                                }
                        )
                        .addOnFailureListener(e ->
                                Toast.makeText(holder.itemView.getContext(), "Role update failed", Toast.LENGTH_SHORT).show()
                        );
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView email;
        TextView meta;
        Spinner roleSpinner;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.adminUserName);
            email = itemView.findViewById(R.id.adminUserEmail);
            meta = itemView.findViewById(R.id.adminUserMeta);
            roleSpinner = itemView.findViewById(R.id.adminRoleSpinner);
        }
    }
}
