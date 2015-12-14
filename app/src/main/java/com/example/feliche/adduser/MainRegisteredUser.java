package com.example.feliche.adduser;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import servicio.XmppConnection;
import servicio.XmppService;

public class MainRegisteredUser extends Activity {

    private static final String LOGTAG = "REGISTERED USER";

    EditText etUser, etPassword;
    TextView tvEstado;
    Button btnConnect;
    String accountXmpp, passwordXmpp;
    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_registered_user);

        accountXmpp = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(Def.XMPP_ACCOUNT, Def.NEW_USER_ACCOUNT);
        passwordXmpp = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(Def.XMPP_PASSWORD, Def.NEW_USER_PASS);

        etUser = (EditText) findViewById(R.id.etRegisteredUser);
        etPassword = (EditText) findViewById(R.id.etRegisteredPassword);
        tvEstado = (TextView) findViewById(R.id.tvStatusUserRegistered);
        btnConnect = (Button) findViewById(R.id.btnConnectRegisteredUser);

        etUser.setText(accountXmpp);
        etPassword.setText(passwordXmpp);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                tvEstado.append("\n" + action);
                Log.d(LOGTAG, "accion = " + action);
                if (action.matches(XmppService.UPDATE_CONNECTION)) {
                    String status = intent.getStringExtra(XmppService.CONNECTION);
                    tvEstado.append("\n" + status);
                }
            }
        };

        IntentFilter filter = new IntentFilter(XmppService.NEW_MESSAGE);
        filter.addAction(XmppService.UPDATE_CONNECTION);
        filter.addAction(XmppService.CHANGE_CONNECTIVITY);
        this.registerReceiver(mReceiver, filter);
    }

    private void connectXmpp() {
        if (XmppService.getState().equals(XmppConnection.ConnectionState.DISCONNECTED)) {
            Intent intent = new Intent(this, XmppService.class);
            this.startService(intent);
        }
    }

    public void onBtnConnect(View v) {
        connectXmpp();
    }
}
