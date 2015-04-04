package in.ac.lnmiit.wimic;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * @author Shivam Dixit <shivamd001 at gmail.com>
 */
public class RVAdapter extends RecyclerView.Adapter<RVAdapter.RoomViewHolder> {

    List<Room> rooms;

    RVAdapter(List<Room> rooms) {
        this.rooms = rooms;
    }

    public static class RoomViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView name;
        TextView ip;
        ImageView mic;

        RoomViewHolder(View itemView) {
            super(itemView);
            cv = (CardView) itemView.findViewById(R.id.card_view);
            name = (TextView) itemView.findViewById(R.id.info_text);
            ip = (TextView) itemView.findViewById(R.id.ip_addr);
            mic = (ImageView) itemView.findViewById(R.id.mic_photo);
        }
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    @Override
    public RoomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.cards_layout, viewGroup, false);
        return new RoomViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RoomViewHolder roomViewHolder, int i) {
        roomViewHolder.name.setText(rooms.get(i).getName());
        roomViewHolder.ip.setText(rooms.get(i).getIpAddress());
        roomViewHolder.mic.setImageResource(R.drawable.ic_action_mic);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }
}
