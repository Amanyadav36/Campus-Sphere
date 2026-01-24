package com.example.campus_sphere;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ClubListFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Placeholder view until we build the real Club layout
        TextView textView = new TextView(getContext());
        textView.setText("Club Page Coming Soon");
        textView.setTextSize(24);
        textView.setGravity(android.view.Gravity.CENTER);
        return textView;
    }
}