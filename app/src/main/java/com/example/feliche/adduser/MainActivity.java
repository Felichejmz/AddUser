package com.example.feliche.adduser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
    EditText etNombre;
    EditText etNoCelular;
    EditText etNoIMEI;
    EditText etEmail;

    EditText etCuentaXMPP;
    EditText etPassXMPP;

    Button btnEnviar;

    String nameUser, numberCell;
    public static String numberIMEI;
    String emailUser;

    String cuentaXMPP, passXMPP;

    private BroadcastReceiver mReceiver;

    @Override
    protected void onResume(){
        super.onResume();
        mReceiver =     new BroadcastReceiver() {
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
                }
            }
        };

        IntentFilter filter = new IntentFilter(XmppService.NEW_ROSTER);
        filter.addAction(XmppService.NEW_MESSAGE);
        this.registerReceiver(mReceiver, filter);
    }

    public void onClickaddUSer(View v){
        nameUser = etNombre.getText().toString();
        numberCell = etNoCelular.getText().toString();
        numberIMEI = etNoIMEI.getText().toString();
        emailUser = etEmail.getText().toString();

        // los últimos 10 digitos
        // quitar el + y otros números adicionales si el usuario edita el campo
        if(numberCell.length() > Def.SIZE_CELL_NUMBER){
            numberCell = numberCell.substring(
                    numberCell.length() - Def.SIZE_CELL_NUMBER);
        }

        // valida que solo sean números
        if((!numberCell.matches("[0-9]+")) || (numberCell.length() != 10)){
            AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
            builder1.setMessage("Verifique que el número sea de 10 dígitos");
            builder1.setCancelable(true);
            builder1.setPositiveButton("Aceptar",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert11 = builder1.create();
            alert11.show();
            return;
        }

        String adminNewUser = Def.ADMIN_NEW_USER + "@" + Def.SERVER_NAME;
        String datos =
                "Nombre " + nameUser + "\n" +
                "noCell " + numberCell + "\n" +
                "noIMEI " + numberIMEI +"\n" +
                "email " + emailUser + "\n";
        Intent intent = new Intent(XmppService.SEND_MESSAGE);
        intent.setPackage(this.getPackageName());
        intent.putExtra(XmppService.BUNDLE_MESSAGE_BODY, datos);
        intent.putExtra(XmppService.BUNDLE_TO, adminNewUser);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
        this.sendBroadcast(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etNombre = (EditText)findViewById(R.id.etNombre);
        etNoCelular = (EditText)findViewById(R.id.etNoCelular);
        etNoIMEI = (EditText)findViewById(R.id.etNoIMEI);
        etEmail = (EditText)findViewById(R.id.etEmail);

        etCuentaXMPP = (EditText)findViewById(R.id.etCuenta);
        etPassXMPP = (EditText)findViewById(R.id.etPassword);

        btnEnviar = (Button)findViewById(R.id.btnAddUser);

        getCellInfo();

        etNoCelular.setText(numberCell);
        etNoIMEI.setText(numberIMEI);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if(XmppService.getState().equals(XmppConnection.ConnectionState.DISCONNECTED)){
            btnEnviar.setText("Desconectado");
            connectXmpp();
        }
    }

    public void connectXmpp(){
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

        // los últimos 10 digitos
        // quitar el + y otros números adicionales
        if(numberCell.length() > Def.SIZE_CELL_NUMBER){
            numberCell = numberCell.substring(
                    numberCell.length() - Def.SIZE_CELL_NUMBER);
        }
        numberIMEI = tf.getDeviceId();
    }

    @Override
    public void onStop() {
        super.onStop();
        Intent intent = new Intent(this, XmppService.class);
        this.stopService(intent);
    }
}
