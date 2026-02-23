package com.example.campus_sphere;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ListSectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_section);
        
        String title = getIntent().getStringExtra("SECTION_TITLE");
        if (title == null) title = "Section List";
        
        TextView sectionTitle = findViewById(R.id.sectionTitle);
        sectionTitle.setText(title);
        
        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());
        
        // This is where real lists would be fetched from Firestore based on the title, e.g. "Bookmarks", "Receipts", etc.
    }
}
