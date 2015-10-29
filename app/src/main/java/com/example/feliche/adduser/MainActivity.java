package com.example.feliche.adduser;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.StrictMode;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;

import servicio.XmppConnection;
import servicio.XmppService;


public class MainActivity extends Activity {
    EditText etNoCelular;
    EditText etIMEI;
    EditText etDeviceID;
    EditText etSerialSIM;

    EditText etCuentaXMPP;
    EditText etPassXMPP;

    Button btnEnviar;

    String deviceID,serialSIM,numberCell,numberIMEI;
    String cuentaXMPP, passXMPP;

    private BroadcastReceiver mReceiver;

    String ADMIN_ADD_USER = "feliche";
    String CUENTA = "cuenta";
    String PASSWORD = "password";

    @Override
    protected void onResume(){
        super.onResume();
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action){
                    case XmppService.NEW_MESSAGE:
                        String from = intent.getStringExtra(XmppService.BUNDLE_FROM_JID);
                        String message = intent.getStringExtra(XmppService.BUNDLE_MESSAGE_BODY);
                        etCuentaXMPP.setText(from.split("@")[0]);
                        etPassXMPP.setText(message);
                        break;
                    case XmppService.NEW_ROSTER:
                        ArrayList<String> roster = intent.getStringArrayListExtra(XmppService.BUNDLE_ROSTER);
                        if(roster == null){
                            return;
                        }
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter(XmppService.NEW_ROSTER);
        filter.addAction(XmppService.NEW_MESSAGE);
        this.registerReceiver(mReceiver, filter);
    }

    public void onClickaddUSer(View v){
        String datos = "noCell " + numberCell + "\n";
                datos += "serialSIM " + serialSIM +"\n";
                datos += "deviceID " + deviceID + "\n";
        Intent intent = new Intent(XmppService.SEND_MESSAGE);
        intent.setPackage(this.getPackageName());
        intent.putExtra(XmppService.BUNDLE_MESSAGE_BODY, datos);
        intent.putExtra(XmppService.BUNDLE_TO, "cachorro@feliche.xyz");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
        this.sendBroadcast(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etNoCelular = (EditText)findViewById(R.id.etNumeroTelefono);
        etIMEI = (EditText)findViewById(R.id.etIMEI);
        etDeviceID = (EditText)findViewById(R.id.etDeviceID);
        etSerialSIM = (EditText)findViewById(R.id.etSerialSIM);

        etCuentaXMPP = (EditText)findViewById(R.id.etCuenta);
        etPassXMPP = (EditText)findViewById(R.id.etPassword);

        btnEnviar = (Button)findViewById(R.id.btnAddUser);

        getCellInfo();

        etNoCelular.setText(numberCell);
        etIMEI.setText(numberIMEI);
        etDeviceID.setText(deviceID);
        etSerialSIM.setText(serialSIM);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if(XmppService.getState().equals(XmppConnection.ConnectionState.DISCONNECTED)){
            btnEnviar.setText("Desconectado");
            connectXmpp();
        }
    }

    public void connectXmpp(){
        btnEnviar.setText("123");
        if(XmppService.getState().equals(XmppConnection.ConnectionState.DISCONNECTED)){
            btnEnviar.setText("Conectar");
            Intent intent = new Intent(this, XmppService.class);
            this.startService(intent);

        } else {
            btnEnviar.setText("Desconectar");
            Intent intent = new Intent(this, XmppService.class);
            this.stopService(intent);
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void getCellInfo(){

        TelephonyManager tf = (TelephonyManager)getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        numberCell = tf.getLine1Number();
        numberIMEI = tf.getDeviceId();
        deviceID = numberIMEI;
        serialSIM = tf.getSimSerialNumber();
    }

    @Override
    public void onStop() {
        super.onStop();
        Intent intent = new Intent(this, XmppService.class);
        this.stopService(intent);
    }

}
