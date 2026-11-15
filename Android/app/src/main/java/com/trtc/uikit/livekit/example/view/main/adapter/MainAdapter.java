package com.trtc.uikit.livekit.example.view.main.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.trtc.uikit.livekit.example.R;
import com.trtc.uikit.livekit.example.view.main.AgoraVoiceActivity;
import com.trtc.uikit.livekit.example.view.main.model.MainItemData;

import java.util.List;

/** The only live-room entry point in Party HMO is Agora voice. */
public class MainAdapter extends RecyclerView.Adapter<MainAdapter.ItemViewHolder> {
    private final Context context;
    private final List<MainItemData> data;

    public MainAdapter(Context context, List<MainItemData> data) {
        this.context = context;
        this.data = data;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.app_adapter_main_item, parent, false);
        return new ItemViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.bind(data.get(position));
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final Context context;
        private final ImageView icon;
        private final TextView title;
        private final TextView subtitle;

        ItemViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            this.context = context;
            icon = itemView.findViewById(R.id.iv_icon);
            title = itemView.findViewById(R.id.tv_title);
            subtitle = itemView.findViewById(R.id.tv_subtitle);
        }

        void bind(MainItemData item) {
            title.setText(item.getTitle());
            subtitle.setText(item.getSubTitle());
            icon.setImageResource(item.getResId());
            itemView.setOnClickListener(v ->
                    context.startActivity(new Intent(context, AgoraVoiceActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        }
    }
}
