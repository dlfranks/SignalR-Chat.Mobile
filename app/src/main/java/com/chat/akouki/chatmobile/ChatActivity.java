package com.chat.akouki.chatmobile;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;

import com.chat.akouki.chatmobile.adapters.MessageAdapter;
import com.chat.akouki.chatmobile.helpers.Globals;
import com.chat.akouki.chatmobile.helpers.User;

public class ChatActivity extends AppCompatActivity {

    // Used to receive messages from ChatService
    MyReceiver myReceiver;

    // Chat Service
    ChatService chatService;
    boolean mBound = false;

    MessageAdapter adapter;

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, ChatService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        //Register events we want to receive from Chat Service
        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("newMessage");
        intentFilter.addAction("notifyAdapter");
        intentFilter.addAction("joinLobby");
        registerReceiver(myReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(myReceiver);
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(User.CurrentRoom);

        // Setup Grid View
        GridView gridView = (GridView) findViewById(R.id.gvMessages);
        gridView.setTranscriptMode(GridView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        adapter = new MessageAdapter(this, Globals.Messages);
        gridView.setAdapter(adapter);

        // Listeners
        EditText editText = (EditText) findViewById(R.id.editMessage);
        ImageButton btnSend = (ImageButton) findViewById(R.id.btnSend);
        btnSend.setOnClickListener(v -> {
            chatService.Send(User.CurrentRoom, editText.getText().toString());
            editText.setText("");
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_delete_room:
                // If he is not the owner of the room, he will get back fail message :)
                chatService.DeleteRoom(User.CurrentRoom);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Globals.Messages.clear();
        super.onBackPressed();
    }

    //https://developer.android.com/guide/components/bound-services.html
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ChatService.LocalBinder binder = (ChatService.LocalBinder) service;
            chatService = binder.getService();
            mBound = true;

            chatService.GetMessageHistory(User.CurrentRoom);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()) {
                case "notifyAdapter":
                    adapter.notifyDataSetChanged();
                    break;
                case "joinLobby":
                    User.CurrentRoom = "Lobby";
                    chatService.Join(User.CurrentRoom);
                    chatService.GetMessageHistory(User.CurrentRoom);
                    getSupportActionBar().setTitle("Lobby");
                    break;
            }
        }
    } // MyReceiver

}
