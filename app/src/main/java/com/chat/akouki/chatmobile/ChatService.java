package com.chat.akouki.chatmobile;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.chat.akouki.chatmobile.helpers.Globals;
import com.chat.akouki.chatmobile.helpers.User;
import com.chat.akouki.chatmobile.modelviews.MessageViewModel;
import com.chat.akouki.chatmobile.modelviews.RoomViewModel;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import microsoft.aspnet.signalr.client.Action;
import microsoft.aspnet.signalr.client.Platform;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.http.android.AndroidPlatformComponent;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler1;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler2;
import microsoft.aspnet.signalr.client.transport.ClientTransport;
import microsoft.aspnet.signalr.client.transport.ServerSentEventsTransport;

public class ChatService extends Service {

    private final IBinder mBinder = new LocalBinder();

    private HubConnection connection;
    private HubProxy proxy;
    private Handler handler;

    private String serverUrl = "http://localhost:2325";
    private String hubName = "ChatHub";

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // We are using binding. do we really need this...?
        if (!StartHubConnection()) {
            ExitWithMessage("Chat Service failed to start!");
        }
        if (!RegisterEvents()) {
            ExitWithMessage("End-point error: Failed to register Events!");
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        connection.stop();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (!StartHubConnection()) {
            ExitWithMessage("Chat Service failed to start!");
        }

        if (!RegisterEvents()) {
            ExitWithMessage("End-point error: Failed to register Events!");
        }

        return mBinder;
    }

    // https://developer.android.com/guide/components/bound-services.html
    public class LocalBinder extends Binder {
        public ChatService getService() {
            return ChatService.this;
        }
    }

    private boolean StartHubConnection() {
        Platform.loadPlatformComponent(new AndroidPlatformComponent());

        // Create Connection
        connection = new HubConnection(serverUrl);
        connection.setCredentials(User.loginCredentials);
        connection.getHeaders().put("Device", "Mobile");

        // Create Proxy
        proxy = connection.createHubProxy(hubName);

        // Establish Connection
        ClientTransport clientTransport = new ServerSentEventsTransport(connection.getLogger());
        SignalRFuture<Void> signalRFuture = connection.start(clientTransport);

        try {
            signalRFuture.get();
        } catch (InterruptedException e) {
            return false;
        } catch (ExecutionException e) {
            return false;
        }

        return true;
    }

    private boolean RegisterEvents() {

        Handler mHandler = new Handler(Looper.getMainLooper());
        try {
            proxy.on("newMessage", new SubscriptionHandler1<MessageViewModel>() {
                @Override
                public void run(final MessageViewModel msg) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            msg.IsMine = msg.From.equals(User.DisplayName) ? 1 : 0;
                            Globals.Messages.add(msg);
                            sendBroadcast(new Intent().setAction("notifyAdapter"));
                        }
                    });
                }
            }, MessageViewModel.class);

            proxy.on("addChatRoom", new SubscriptionHandler1<RoomViewModel>() {
                @Override
                public void run(final RoomViewModel room) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Globals.Rooms.add(room.Name);
                            sendBroadcast(new Intent().setAction("notifyAdapter"));
                        }
                    });
                }
            }, RoomViewModel.class);

            proxy.on("removeChatRoom", new SubscriptionHandler1<RoomViewModel>() {
                @Override
                public void run(final RoomViewModel room) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Globals.Rooms.remove(room.Name);
                            sendBroadcast(new Intent().setAction("notifyAdapter"));
                        }
                    });
                }
            }, RoomViewModel.class);

            proxy.on("getProfileInfo", new SubscriptionHandler2<String, String>() {
                @Override
                public void run(final String displayName, final String avatar) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            User.DisplayName = displayName;
                            User.Avatar = avatar;
                        }
                    });
                }
            }, String.class, String.class);

            proxy.on("onError", new SubscriptionHandler1<String>() {
                @Override
                public void run(final String error) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ChatService.this, error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }
            }, String.class);

            proxy.on("onRoomDeleted", new SubscriptionHandler1<String>() {
                @Override
                public void run(final String info) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ChatService.this, info, Toast.LENGTH_SHORT).show();
                                    sendBroadcast(new Intent().setAction("joinLobby"));
                                }
                            });
                        }
                    });
                }
            }, String.class);

        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public void Send(String roomName, String message) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                proxy.invoke("Send", roomName, message);
                return null;
            }
        }.execute();
    }

    public void Join(String roomName) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                User.CurrentRoom = roomName;
                proxy.invoke("Join", roomName);
                return null;
            }
        }.execute();
    }

    public void CreateRoom(String roomName) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                proxy.invoke("CreateRoom", roomName);
                return null;
            }
        }.execute();
    }

    public void DeleteRoom(String roomName) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                proxy.invoke("DeleteRoom", roomName);
                return null;
            }
        }.execute();
    }

    public void GetMessageHistory(String roomName) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                proxy.invoke(MessageViewModel[].class, "GetMessageHistory", roomName).done(
                        new Action<MessageViewModel[]>() {
                            @Override
                            public void run(MessageViewModel[] messageViewModels) throws Exception {
                                Globals.Messages.clear();
                                Globals.Messages.addAll(Arrays.asList(messageViewModels));

                                for (MessageViewModel m : Globals.Messages) {
                                    m.IsMine = m.From.equals(User.DisplayName) ? 1 : 0;
                                }
                                sendBroadcast(new Intent().setAction("notifyAdapter"));
                            }
                        });
                return null;
            }
        }.execute();
    }

    public void GetRooms() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                proxy.invoke(RoomViewModel[].class, "GetRooms").done(
                        new Action<RoomViewModel[]>() {
                            @Override
                            public void run(RoomViewModel[] rooms) throws Exception {
                                Globals.Rooms.clear();

                                for (RoomViewModel room : rooms)
                                    Globals.Rooms.add(room.Name);

                                sendBroadcast(new Intent().setAction("notifyAdapter"));
                            }
                        });
                return null;
            }
        }.execute();
    }

    private void ExitWithMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stopSelf();
            }
        }, 3000);
    }

}
