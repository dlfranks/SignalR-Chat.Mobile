package com.chat.akouki.chatmobile;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.chat.akouki.chatmobile.helpers.Globals;
import com.chat.akouki.chatmobile.helpers.PrefsManager;

public class MainActivity extends AppCompatActivity {

    // Used to receive messages from ChatService
    MyReceiver myReceiver;

    // Chat Service
    ChatService chatService;
    boolean mBound = false;

    ArrayAdapter<String> adapter;

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, ChatService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        //Register events we want to receive from Chat Service
        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("notifyAdapter");
        registerReceiver(myReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Chat Rooms");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_power_settings_new_white_24dp);

        // Setup ListView
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Globals.Rooms);
        ListView lv = (ListView) findViewById(R.id.lvRooms);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener((parent, view, position, id) -> {
            final String text = ((TextView) view).getText().toString();
            chatService.Join(text);
            startActivity(new Intent(MainActivity.this, ChatActivity.class));
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                PrefsManager.clearPrefs(this);
                startActivity(new Intent(this, LoginActivity.class));
                break;
            case R.id.action_create_room:
                ShowCreateRoomDialog();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void ShowCreateRoomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create new Room");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                chatService.CreateRoom(input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //https://developer.android.com/guide/components/bound-services.html
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ChatService.LocalBinder binder = (ChatService.LocalBinder) service;
            chatService = binder.getService();
            chatService.GetRooms();
            mBound = true;
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
            }
        }
    } // MyReceiver

}
