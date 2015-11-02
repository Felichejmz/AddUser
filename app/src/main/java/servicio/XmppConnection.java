package servicio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import com.example.feliche.adduser.Def;
import com.example.feliche.adduser.MainActivity;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ChatMessageListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.provided.SASLDigestMD5Mechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by feliche on 31/08/15.
 */
public class XmppConnection implements ConnectionListener, ChatManagerListener, RosterListener, ChatMessageListener,PingFailedListener{

    private final Context mApplicationContext;

    private XMPPTCPConnection mConnection;
    private ArrayList<String> mRoster;
    private BroadcastReceiver mReceiver;

    //ConnectionListener
    @Override
    public void connected(XMPPConnection connection) {
        XmppService.sConnectionState = ConnectionState.CONNECTED;
    }

    @Override
    public void authenticated(XMPPConnection connection) {
        XmppService.sConnectionState = ConnectionState.CONNECTED;
    }
    @Override
    public void connectionClosed() {
        XmppService.sConnectionState = ConnectionState.DISCONNECTED;
    }
    @Override
    public void connectionClosedOnError(Exception e) {
        XmppService.sConnectionState = ConnectionState.DISCONNECTED;
    }
    @Override
    public void reconnectingIn(int seconds) {
        XmppService.sConnectionState = ConnectionState.RECONNECTING;
    }
    @Override
    public void reconnectionSuccessful() {
        XmppService.sConnectionState = ConnectionState.CONNECTED;
    }
    @Override
    public void reconnectionFailed(Exception e) {
        XmppService.sConnectionState = ConnectionState.DISCONNECTED;
    }

    @Override
    public void chatCreated(Chat chat, boolean createdLocally) {
        chat.addMessageListener(this);

    }

    // Roaster Listener
    @Override
    public void entriesAdded(Collection<String> addresses) {
        rebuildRoster();
    }
    @Override
    public void entriesUpdated(Collection<String> addresses) {
        rebuildRoster();
    }

    @Override
    public void entriesDeleted(Collection<String> addresses) {
        rebuildRoster();
    }

    @Override
    public void presenceChanged(Presence presence) {
        rebuildRoster();
    }

    @Override
    public void processMessage(Chat chat, Message message) {
        if(message.getType().equals(Message.Type.chat)
                || message.getType().equals(Message.Type.normal)){
            if(message.getBody() != null){
                Intent intent = new Intent(XmppService.NEW_MESSAGE);
                intent.setPackage(mApplicationContext.getPackageName());
                intent.putExtra(XmppService.BUNDLE_MESSAGE_BODY, message.getBody());
                intent.putExtra(XmppService.BUNDLE_FROM_XMPP, message.getFrom());
                mApplicationContext.sendBroadcast(intent);
            }
        }
    }

    @Override
    public void pingFailed() {

    }

    public static enum ConnectionState{
        CONNECTED,
        CONNECTING,
        RECONNECTING,
        DISCONNECTED;
    }

    public XmppConnection(Context mContext){
        mApplicationContext = mContext.getApplicationContext();
    }

    // Desconectar
    public void disconnect(){
        if(mConnection != null){
            try {
                mConnection.disconnect();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }
        }
        mConnection = null;
        if(mReceiver != null){
            mApplicationContext.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    // Conectar
    public void connect() throws IOException, XMPPException, SmackException{
        XMPPTCPConnectionConfiguration.XMPPTCPConnectionConfigurationBuilder builder
                = XMPPTCPConnectionConfiguration.builder();

        builder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        SASLMechanism mechanism = new SASLDigestMD5Mechanism();
        SASLAuthentication.registerSASLMechanism(mechanism);
        SASLAuthentication.blacklistSASLMechanism("SCRAM-SHA-1");
        SASLAuthentication.blacklistSASLMechanism("DIGEST-MD5");

        // configuración de la conexión XMPP
        builder.setHost(Def.SERVER_NAME);
        builder.setServiceName(Def.SERVER_NAME);
        builder.setResource(MainActivity.numberIMEI);
        builder.setUsernameAndPassword(Def.NEW_USER, Def.NEW_USER_PASS);
        builder.setSendPresence(true);
        builder.setRosterLoadedAtLogin(true);

        // crea la conexión
        mConnection = new XMPPTCPConnection(builder.build());
        // Configura el listener
        mConnection.addConnectionListener(this);
        // se conecta al servidor
        mConnection.connect();
        // envía la autenticación
        mConnection.login();

        // Envía un Ping cada 6 minutos
        PingManager.setDefaultPingInterval(600);
        PingManager pingManager = PingManager.getInstanceFor(mConnection);
        pingManager.registerPingFailedListener(this);

        setUpSendMessageReceiver();

        ChatManager.getInstanceFor(mConnection).addChatListener(this);
        mConnection.getRoster().addRosterListener(this);
    }

    private void setUpSendMessageReceiver(){
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(action.equals(XmppService.SEND_MESSAGE)){
                    sendMessage(intent.getStringExtra(XmppService.BUNDLE_MESSAGE_BODY),intent.getStringExtra(XmppService.BUNDLE_TO));

                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(XmppService.SEND_MESSAGE);
        mApplicationContext.registerReceiver(mReceiver, filter);
    }

    // Envía un mensaje
    private void sendMessage(String mensaje, String toJabberId){
        Chat chat = ChatManager.getInstanceFor(mConnection).createChat(toJabberId,this);

        try {
            try {
                chat.sendMessage(mensaje);
            } catch (XMPPException e) {
                e.printStackTrace();
            }
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }

    private void rebuildRoster(){
        mRoster = new ArrayList<>();
        String status;
        for(RosterEntry entry: mConnection.getRoster().getEntries()){
            if(mConnection.getRoster().getPresence(entry.getUser()).isAvailable()) {
                status = "En línea";
            }
            else
            {
                status = "Fuera de línea";
            }
            mRoster.add(entry.getUser()+ ": "+status);
        }
        Intent intent = new Intent(XmppService.NEW_ROSTER);
        intent.setPackage(mApplicationContext.getPackageName());
        intent.putStringArrayListExtra(XmppService.BUNDLE_ROSTER,mRoster);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
        mApplicationContext.sendBroadcast(intent);
    };
}