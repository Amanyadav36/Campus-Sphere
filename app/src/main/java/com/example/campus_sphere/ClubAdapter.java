package com.example.campus_sphere;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.List;

public class ClubAdapter extends RecyclerView.Adapter<ClubAdapter.ClubViewHolder> {

    public interface OnClubClickListener {
        void onClubClick(Club club);
    }

    private List<Club> clubList;
    private OnClubClickListener listener;

    public ClubAdapter(List<Club> clubList, OnClubClickListener listener) {
        this.clubList = clubList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ClubViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_club, parent, false);
        return new ClubViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClubViewHolder holder, int position) {
        Club club = clubList.get(position);

        String name = club.getName();
        String handle = club.getHandle();
        String bio = club.getBio();
        String logoUrl = club.getLogoUrl();
        String headerUrl = club.getHeaderUrl();

        holder.clubName.setText(name != null ? name : "Unnamed Club");
        holder.clubHandle.setText(handle != null ? "@" + handle : "@club");
        holder.clubBio.setText(bio != null ? bio : "No description available.");

        if (logoUrl != null && !logoUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(logoUrl)
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .into(holder.clubLogo);
        }

        if (headerUrl != null && !headerUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(headerUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.clubBackgroundImage);
        } else {
            holder.clubBackgroundImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        if (club.isJoined()) {
            holder.btnJoinCard.setText("Leave");
            holder.btnJoinCard.setBackgroundColor(Color.parseColor("#E5E7EB")); // Light Gray 
            holder.btnJoinCard.setTextColor(Color.parseColor("#374151")); // Dark Gray Text
        } else {
            holder.btnJoinCard.setText("Join");
            holder.btnJoinCard.setBackgroundColor(Color.parseColor("#0984E3")); // Original Blue
            holder.btnJoinCard.setTextColor(Color.parseColor("#FFFFFF"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClubClick(club);
            }
        });
    }


    @Override
    public int getItemCount() {
        return clubList.size();
    }

    public static class ClubViewHolder extends RecyclerView.ViewHolder {
        TextView clubName, clubHandle, clubBio;
        CircleImageView clubLogo;
        ImageView clubBackgroundImage;
        Button btnJoinCard;

        public ClubViewHolder(@NonNull View itemView) {
            super(itemView);
            clubName = itemView.findViewById(R.id.clubName);
            clubHandle = itemView.findViewById(R.id.clubHandle);
            clubBio = itemView.findViewById(R.id.clubBio);
            clubLogo = itemView.findViewById(R.id.clubLogo);
            clubBackgroundImage = itemView.findViewById(R.id.clubBackgroundImage);
            btnJoinCard = itemView.findViewById(R.id.btnJoinCard);
        }
    }
}
