package com.example.campus_sphere;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class PaymentMethodActivity extends AppCompatActivity {

    private TextView displayAmountTop;
    private TextView displayAmountBottom;
    private Button payNowBtn;
    private Event event;
    private long amountInPaise;

    private GridLayout upiAppsContainer;
    private String selectedUpiAppPackage = null;
    private View lastSelectedUpiView = null;
    private String selectedMethod = "upi";
    private ActivityResultLauncher<Intent> upiLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_method);

        displayAmountTop = findViewById(R.id.displayAmountTop);
        displayAmountBottom = findViewById(R.id.displayAmountBottom);
        payNowBtn = findViewById(R.id.payNowBtn);
        upiAppsContainer = findViewById(R.id.upiAppsContainerGrid);

        event = (Event) getIntent().getSerializableExtra("event_data");
        if (event != null) {
            amountInPaise = event.getAmountInPaise();
            String amountText = "₹ " + (amountInPaise / 100);
            displayAmountTop.setText(amountText);
            displayAmountBottom.setText(amountText);
        }

        payNowBtn.setOnClickListener(v -> startPayment());
        setupSectionToggles();
        loadUpiApps();

        upiLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String response = result.getData().getStringExtra("response");
                        handleUpiResponse(response);
                    } else {
                        Toast.makeText(this, "Payment cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupSectionToggles() {
        findViewById(R.id.upiSectionHeader).setOnClickListener(v -> {
            selectedMethod = "upi";
            toggleSection(R.id.upiSectionContent);
        });
        findViewById(R.id.cardsSectionHeader).setOnClickListener(v -> {
            selectedMethod = "card";
            toggleSection(R.id.cardsSectionContent);
        });
        findViewById(R.id.netbankingSectionHeader).setOnClickListener(v -> {
            selectedMethod = "netbanking";
            toggleSection(R.id.netbankingSectionContent);
        });
        findViewById(R.id.emiSectionHeader).setOnClickListener(v -> toggleSection(R.id.emiSectionContent));
        findViewById(R.id.walletSectionHeader).setOnClickListener(v -> toggleSection(R.id.walletSectionContent));
        findViewById(R.id.payLaterSectionHeader).setOnClickListener(v -> toggleSection(R.id.payLaterSectionContent));
    }

    private void toggleSection(int contentId) {
        View content = findViewById(contentId);
        if (content.getVisibility() == View.VISIBLE) {
            content.setVisibility(View.GONE);
        } else {
            content.setVisibility(View.VISIBLE);
        }
    }

    private void loadUpiApps() {
        upiAppsContainer.removeAllViews();
        Uri uri = Uri.parse("upi://pay?pa=test@upi&pn=Test&am=1.00&cu=INR");
        Intent upiIntent = new Intent(Intent.ACTION_VIEW, uri);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(upiIntent, 0);

        if (resolveInfoList != null && !resolveInfoList.isEmpty()) {
            for (ResolveInfo info : resolveInfoList) {
                View appView = createUpiAppIcon(info, pm);
                upiAppsContainer.addView(appView);
            }
        } else {
            TextView noUpiText = new TextView(this);
            noUpiText.setText("No UPI apps found");
            noUpiText.setPadding(32, 32, 32, 32);
            upiAppsContainer.addView(noUpiText);
        }
    }

    private View createUpiAppIcon(ResolveInfo info, PackageManager pm) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        int padding = (int) (8 * getResources().getDisplayMetrics().density);
        int margin = (int) (6 * getResources().getDisplayMetrics().density);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(margin, margin, margin, margin);
        layout.setLayoutParams(params);
        layout.setPadding(padding, padding, padding, padding);
        layout.setBackgroundResource(R.drawable.bg_rounded_border);

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(info.loadIcon(pm));
        int iconSize = (int) (36 * getResources().getDisplayMetrics().density);
        icon.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));

        TextView name = new TextView(this);
        name.setText(info.loadLabel(pm));
        name.setTextSize(10);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(1);

        layout.addView(icon);
        layout.addView(name);

        layout.setOnClickListener(v -> {
            selectedUpiAppPackage = info.activityInfo.packageName;
            if (lastSelectedUpiView != null) {
                lastSelectedUpiView.setBackgroundResource(R.drawable.bg_rounded_border);
            }
            layout.setBackgroundColor(0xFFE7EDFF);
            lastSelectedUpiView = layout;
            selectedMethod = "upi";
        });

        return layout;
    }

    private void startPayment() {
        if (event == null) {
            Toast.makeText(this, "Event details missing", Toast.LENGTH_SHORT).show();
            return;
        }
        if (amountInPaise <= 0) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("upi".equals(selectedMethod)) {
            startUpiPayment();
        } else {
            Toast.makeText(this, "Only UPI is available in custom checkout", Toast.LENGTH_SHORT).show();
        }
    }

    private void startUpiPayment() {
        double amount = amountInPaise / 100.0;
        String upiId = getString(R.string.upi_id);
        String upiName = getString(R.string.upi_name);
        String note = "Event: " + event.getTitle();

        Uri uri = Uri.parse("upi://pay")
                .buildUpon()
                .appendQueryParameter("pa", upiId)
                .appendQueryParameter("pn", upiName)
                .appendQueryParameter("am", String.format("%.2f", amount))
                .appendQueryParameter("cu", "INR")
                .appendQueryParameter("tn", note)
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        if (selectedUpiAppPackage != null) {
            intent.setPackage(selectedUpiAppPackage);
        }

        if (intent.resolveActivity(getPackageManager()) != null) {
            upiLauncher.launch(intent);
        } else {
            Toast.makeText(this, "No UPI app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleUpiResponse(String response) {
        if (response == null) {
            Toast.makeText(this, "Payment failed", Toast.LENGTH_SHORT).show();
            return;
        }

        String status = "";
        String txnId = "";
        String[] parts = response.split("&");
        for (String part : parts) {
            String[] kv = part.split("=");
            if (kv.length < 2) continue;
            String key = kv[0].toLowerCase();
            String value = kv[1];
            if ("status".equals(key)) {
                status = value;
            } else if ("txnref".equals(key) || "txnId".equalsIgnoreCase(key) || "transactionid".equalsIgnoreCase(key)) {
                txnId = value;
            }
        }

        if ("success".equalsIgnoreCase(status)) {
            String paymentId = txnId.isEmpty() ? "UPI_SUCCESS" : txnId;
            Intent resultIntent = new Intent();
            resultIntent.putExtra("payment_id", paymentId);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "Payment failed", Toast.LENGTH_SHORT).show();
        }
    }
}
