package com.example.campus_sphere;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AdminOptionsBottomSheet extends BottomSheetDialogFragment {

    public interface Listener {
        void onOptionSelected(AdminOptionItem item);
    }

    private String title;
    private final List<AdminOptionItem> items = new ArrayList<>();
    private Listener listener;

    public static AdminOptionsBottomSheet create(String title, List<AdminOptionItem> items, Listener listener) {
        AdminOptionsBottomSheet sheet = new AdminOptionsBottomSheet();
        sheet.title = title;
        if (items != null) sheet.items.addAll(items);
        sheet.listener = listener;
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_admin_options, container, false);

        TextView tvTitle = view.findViewById(R.id.sheetTitle);
        if (tvTitle != null) tvTitle.setText(title != null ? title : "Options");

        RecyclerView rv = view.findViewById(R.id.sheetRecycler);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(new AdminOptionAdapter(items, item -> {
            dismiss();
            if (listener != null) listener.onOptionSelected(item);
        }));

        return view;
    }
}
