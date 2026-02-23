package com.example.campus_sphere;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminAuditLogger {

    private AdminAuditLogger() {}

    public static void log(String action, String entity, String entityId, String previousValue, String newValue) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String adminId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> log = new HashMap<>();
        log.put("adminId", adminId);
        log.put("action", action);
        log.put("entity", entity);
        log.put("entityId", entityId);
        log.put("previousValue", previousValue);
        log.put("newValue", newValue);
        log.put("timestamp", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("admin_audit_logs")
                .add(log);
    }
}
