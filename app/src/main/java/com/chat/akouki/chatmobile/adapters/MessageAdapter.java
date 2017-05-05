package com.chat.akouki.chatmobile.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.chat.akouki.chatmobile.R;
import com.chat.akouki.chatmobile.modelviews.MessageViewModel;

import java.util.ArrayList;

public class MessageAdapter extends BaseAdapter {

    ArrayList<MessageViewModel> messages;
    Context context;
    public MessageAdapter(Context context, ArrayList<MessageViewModel> messages) {
        this.context = context;
        this.messages = messages;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).IsMine;
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Object getItem(int position) {
        return messages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View rowView = convertView;
        ViewHolder holder = null;
        final MessageViewModel temp = messages.get(position);

        int listViewItemType = getItemViewType(position);

        if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            if (listViewItemType == 1)
                rowView = inflater.inflate(R.layout.item_message_mine, parent, false);
            else
                rowView = inflater.inflate(R.layout.item_message_other, parent, false);
            holder = new ViewHolder(rowView);
            rowView.setTag(holder);
        } else {
            holder = (ViewHolder) rowView.getTag();
        }

        try {
            byte[] decodedString = Base64.decode(temp.Avatar.replace("data:image/false;base64,", ""), Base64.DEFAULT);
            Bitmap avatarBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            holder.avatar.setImageBitmap(avatarBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }

        holder.from.setText(temp.From);
        holder.content.setText(temp.Content);

        return rowView;
    }
}

class ViewHolder {
    ImageView avatar;
    TextView from, content;

    ViewHolder(View v) {
        avatar = (ImageView) v.findViewById(R.id.imgMessageAvatar);
        from = (TextView) v.findViewById(R.id.txtMessageOwner);
        content = (TextView) v.findViewById(R.id.txtMessageContent);
    }

}