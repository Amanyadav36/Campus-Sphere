package com.example.campus_sphere;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AdminActionFragment extends Fragment {

    private static final String ID_CLUB = "club";
    private static final String ID_EVENT = "event";
    private static final String ID_USER = "user";
    private static final String ID_PAYMENT = "payment";
    private static final String ID_REPORTS = "reports";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_actions, container, false);

        RecyclerView rv = view.findViewById(R.id.adminActionsRecycler);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        List<AdminActionItem> items = new ArrayList<>();
        items.add(new AdminActionItem(ID_CLUB, "Club Management", "Create club, assign leader, manage clubs", android.R.drawable.ic_menu_share));
        items.add(new AdminActionItem(ID_EVENT, "Event Management", "Create, edit, delete events", android.R.drawable.ic_menu_agenda));
        items.add(new AdminActionItem(ID_USER, "User Management", "View users and change roles", android.R.drawable.ic_menu_myplaces));
        items.add(new AdminActionItem(ID_PAYMENT, "Payment Management", "Verify or reject payments", android.R.drawable.ic_menu_send));
        items.add(new AdminActionItem(ID_REPORTS, "Reports / Analytics", "CSV export and club analytics", android.R.drawable.ic_menu_save));

        AdminActionAdapter adapter = new AdminActionAdapter(items, this::onActionClick);
        rv.setAdapter(adapter);

        return view;
    }

    private void onActionClick(AdminActionItem item) {
        if (item == null || getContext() == null) return;
        switch (item.id) {
            case ID_CLUB:
                showClubDialog();
                break;
            case ID_EVENT:
                showEventDialog();
                break;
            case ID_USER:
                showUserDialog();
                break;
            case ID_PAYMENT:
                showPaymentDialog();
                break;
            case ID_REPORTS:
                showReportsDialog();
                break;
        }
    }

    private void showClubDialog() {
        List<AdminOptionItem> options = new ArrayList<>();
        options.add(new AdminOptionItem("create_club", "Create Club", android.R.drawable.ic_input_add));
        options.add(new AdminOptionItem("assign_leader", "Assign Leader", android.R.drawable.ic_menu_set_as));
        options.add(new AdminOptionItem("manage_club", "Manage Clubs", android.R.drawable.ic_menu_edit));

        AdminOptionsBottomSheet.create("Club Management", options, item -> {
            if (item == null) return;
            if ("create_club".equals(item.id)) {
                startActivity(new Intent(requireContext(), AdminCreateClubActivity.class));
            } else if ("assign_leader".equals(item.id)) {
                startActivity(new Intent(requireContext(), AdminAssignLeaderActivity.class));
            } else if ("manage_club".equals(item.id)) {
                startActivity(new Intent(requireContext(), AdminManageClubsActivity.class));
            }
        }).show(getParentFragmentManager(), "club_sheet");
    }

    private void showEventDialog() {
        List<AdminOptionItem> options = new ArrayList<>();
        options.add(new AdminOptionItem("create_event", "Create Event", android.R.drawable.ic_input_add));
        options.add(new AdminOptionItem("manage_event", "Edit / Delete Events", android.R.drawable.ic_menu_agenda));

        AdminOptionsBottomSheet.create("Event Management", options, item -> {
            if (item == null) return;
            if ("create_event".equals(item.id)) {
                startActivity(new Intent(requireContext(), AdminCreateEventActivity.class));
            } else if ("manage_event".equals(item.id)) {
                startActivity(new Intent(requireContext(), AdminEventsActivity.class));
            }
        }).show(getParentFragmentManager(), "event_sheet");
    }

    private void showUserDialog() {
        List<AdminOptionItem> options = new ArrayList<>();
        options.add(new AdminOptionItem("view_users", "View Users", android.R.drawable.ic_menu_view));
        options.add(new AdminOptionItem("change_roles", "Change Roles", android.R.drawable.ic_menu_manage));

        AdminOptionsBottomSheet.create("User Management", options, item ->
                startActivity(new Intent(requireContext(), AdminUsersActivity.class))
        ).show(getParentFragmentManager(), "user_sheet");
    }

    private void showPaymentDialog() {
        List<AdminOptionItem> options = new ArrayList<>();
        options.add(new AdminOptionItem("view_tickets", "View Tickets", android.R.drawable.ic_menu_view));
        options.add(new AdminOptionItem("verify_reject", "Verify / Reject Payments", android.R.drawable.ic_menu_send));

        AdminOptionsBottomSheet.create("Payment Management", options, item ->
                startActivity(new Intent(requireContext(), AdminPaymentsActivity.class))
        ).show(getParentFragmentManager(), "payment_sheet");
    }

    private void showReportsDialog() {
        List<AdminOptionItem> options = new ArrayList<>();
        options.add(new AdminOptionItem("download_csv", "Download Event Registrations (CSV)", android.R.drawable.ic_menu_save));
        options.add(new AdminOptionItem("analytics", "Club Analytics", android.R.drawable.ic_menu_sort_by_size));

        AdminOptionsBottomSheet.create("Reports / Analytics", options, item ->
                startActivity(new Intent(requireContext(), AdminAnalyticsActivity.class))
        ).show(getParentFragmentManager(), "reports_sheet");
    }
}
