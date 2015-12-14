package servicio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.feliche.adduser.Def;
import com.example.feliche.adduser.MainActivity;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.RosterListener;
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
public class XmppConnection implements ConnectionListener, ChatManagerListener, PingFailedListener, ChatMessageListener {

    private final Context mApplicationContext;

    private XMPPTCPConnection mConnection;
    private BroadcastReceiver mReceiver;
    private String user;
    private String password;

    private static final String LOGTAG = "XmppConnection:";

    // envía el estado de la conexión
    private void connectionStatus(ConnectionState status){
        Intent intent = new Intent(XmppService.UPDATE_CONNECTION);
        intent.setPackage(mApplicationContext.getPackageName());
        intent.putExtra(XmppService.CONNECTION, status.toString());
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
        mApplicationContext.sendBroadcast(intent);
    }


    @Override
    public void chatCreated(Chat chat, boolean createdLocally) {
        chat.addMessageListener(this);
    }

    @Override
    public void processMessage(Chat chat, Message message) {
        if (message.getType().equals(Message.Type.chat)
                || message.getType().equals(Message.Type.normal)) {
            if (message.getBody() != null) {
                Intent intent = new Intent(XmppService.NEW_MESSAGE);
                intent.setPackage(mApplicationContext.getPackageName());
                intent.putExtra(XmppService.BUNDLE_MESSAGE_BODY, message.getBody());
                intent.putExtra(XmppService.BUNDLE_FROM_XMPP, message.getFrom());
                mApplicationContext.sendBroadcast(intent);
            }
        }
    }

    public static enum ConnectionState{
        AUTHENTICATE,
        ERROR,
        AUTH_ERROR,
        IO_ERROR,
        HOSTNAME_ERROR,
        SECURITY_ERROR,
        CONNECTED,
        CLOSED_ERROR,
        RECONNECTING,
        RECONNECTED,
        RECONNECTED_ERROR,
        DISCONNECTED;
    }

    //ConnectionListener
    @Override
    public void connected(XMPPConnection connection) {
        XmppService.sConnectionState = ConnectionState.CONNECTED;
        connectionStatus(XmppService.sConnectionState);
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        XmppService.sConnectionState = ConnectionState.AUTHENTICATE;
        connectionStatus(XmppService.sConnectionState);
    }

    @Override
    public void connectionClosed() {
        XmppService.sConnectionState = ConnectionState.DISCONNECTED;
        connectionStatus(XmppService.sConnectionState);
    }
    @Override
    public void connectionClosedOnError(Exception e) {
        XmppService.sConnectionState = ConnectionState.CLOSED_ERROR;
        connectionStatus(XmppService.sConnectionState);
    }
    @Override
    public void reconnectingIn(int seconds) {
        XmppService.sConnectionState = ConnectionState.RECONNECTING;
        connectionStatus(XmppService.sConnectionState);
    }
    @Override
    public void reconnectionSuccessful() {
        XmppService.sConnectionState = ConnectionState.RECONNECTED;
        connectionStatus(XmppService.sConnectionState);
    }
    @Override
    public void reconnectionFailed(Exception e) {
        XmppService.sConnectionState = ConnectionState.RECONNECTED_ERROR;
        connectionStatus(XmppService.sConnectionState);
    }

    @Override
    public void pingFailed() {

    }

    public XmppConnection(Context mContext){
        mApplicationContext = mContext.getApplicationContext();
        // obtiene los valores, si no existen entonces coloca los default
        // de New user
        user = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString(Def.XMPP_ACCOUNT, Def.NEW_USER_ACCOUNT);
        password = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString(Def.XMPP_PASSWORD, Def.NEW_USER_PASS);
    }

    // Desconectar
    public void disconnect(){
        if(mConnection != null){
            mConnection.disconnect();
        }
        mConnection = null;
        if(mReceiver != null){
            mApplicationContext.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    // Conectar
    public void connect() throws IOException, XMPPException, SmackException {
        XMPPTCPConnectionConfiguration.Builder builder
                = XMPPTCPConnectionConfiguration.builder();

        builder.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
        SASLMechanism mechanism = new SASLDigestMD5Mechanism();
        SASLAuthentication.registerSASLMechanism(mechanism);
        SASLAuthentication.blacklistSASLMechanism("SCRAM-SHA-1");
        SASLAuthentication.blacklistSASLMechanism("DIGEST-MD5");

        // configuración de la conexión XMPP
        builder.setHost(Def.SERVER_NAME);
        builder.setServiceName(Def.SERVER_NAME);
        builder.setResource(Def.APP_NAME);
        builder.setSendPresence(true);
        builder.setPort(5222);

        // crea la conexión
        mConnection = new XMPPTCPConnection(builder.build());
        // Configura el listener
        mConnection.addConnectionListener(this);
        // se conecta al servidor
        mConnection.connect();
        // envía la autenticación
        mConnection.login(user, password);

        // Envía un Ping cada 6 minutos
        PingManager.setDefaultPingInterval(600);
        PingManager pingManager = PingManager.getInstanceFor(mConnection);
        pingManager.registerPingFailedListener(this);

        setUpSendMessageReceiver();

        ChatManager.getInstanceFor(mConnection).addChatListener(this);
    }

    public void onConnectionError(ConnectionState error){
        connectionStatus(error);
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
        Log.i("XmppConnection", "Enviando mensaje");
        Chat chat = ChatManager.getInstanceFor(mConnection).createChat(toJabberId);
        try {
            chat.sendMessage(mensaje);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }
}