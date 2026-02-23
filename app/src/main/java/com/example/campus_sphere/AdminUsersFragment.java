package com.example.campus_sphere;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminUsersFragment extends Fragment {

    private EditText searchInput;
    private RecyclerView recyclerView;
    private AdminUserAdapter adapter;
    private FirebaseFirestore db;
    private final List<User> allUsers = new ArrayList<>();
    private final List<User> filteredUsers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_admin_users, container, false);

        db = FirebaseFirestore.getInstance();
        searchInput = view.findViewById(R.id.adminUserSearch);
        recyclerView = view.findViewById(R.id.adminUsersRecycler);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminUserAdapter(filteredUsers, db);
        recyclerView.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        fetchUsers();
        return view;
    }

    private void fetchUsers() {
        db.collection("users").get()
                .addOnSuccessListener(snapshot -> {
                    allUsers.clear();
                    for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                        User user = snapshot.getDocuments().get(i).toObject(User.class);
                        if (user == null) continue;
                        user.setUid(snapshot.getDocuments().get(i).getId());
                        allUsers.add(user);
                    }
                    filterUsers(searchInput.getText().toString());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load users", Toast.LENGTH_SHORT).show()
                );
    }

    private void filterUsers(String query) {
        String lowered = query == null ? "" : query.toLowerCase().trim();
        filteredUsers.clear();
        for (User user : allUsers) {
            String name = user.getName() == null ? "" : user.getName().toLowerCase();
            String enrollment = user.getEnrollment() == null ? "" : user.getEnrollment().toLowerCase();
            if (name.contains(lowered) || enrollment.contains(lowered)) {
                filteredUsers.add(user);
            }
        }
        adapter.notifyDataSetChanged();
    }
}
