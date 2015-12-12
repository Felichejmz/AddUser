package com.example.feliche.adduser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.StrictMode;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

import servicio.XmppConnection;
import servicio.XmppService;

import static android.support.v4.content.WakefulBroadcastReceiver.startWakefulService;


public class MainActivity extends Activity {

    private static MainActivity inst;
    private static boolean statusBroadcastReceiver;

    EditText etNombre;
    EditText etNoCelular;
    EditText etNoIMEI;
    EditText etEmail;

    EditText etCuentaXMPP;
    EditText etPassXMPP;
    EditText etBirthday;

    Button btnEnviar;
    TextView tvLog;

    String nameUser;
    String numberCell;
    public static String numberIMEI;
    String emailUser;
    String birthday;

    String cuentaXMPP, passXMPP;


    String log = "log";

    private BroadcastReceiver mReceiver;

    private static final String LOGTAG = "MainActivity:BR";
    @Override
    protected void onResume(){
        super.onResume();
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(LOGTAG, "action=" + action);
                switch (action){
                    case XmppService.NEW_MESSAGE:
                        String from = intent.getStringExtra(XmppService.BUNDLE_FROM_JID);
                        String message = intent.getStringExtra(XmppService.BUNDLE_MESSAGE_BODY);
                        etCuentaXMPP.setText(from.split("@")[0]);
                        etPassXMPP.setText(message);
                        break;
                    case XmppService.UPDATE_CONNECTION:
                        String status = intent.getStringExtra(XmppService.CONNECTION);
                        if (status.contains("AUTHENTICATE"))
                            sendMessage();
                        btnEnviar.setText(status);
                        tvLog.append("\n" + status);
                        break;
                    case XmppService.SMS_CONNECTION:
                        Bundle bundle = intent.getExtras();
                        if(bundle != null) {
                            Object[] sms = (Object[]) bundle.get("pdus");
                            String smsMsg = null;
                            for (int i = 0; i < sms.length; i++) {
                                SmsMessage msgSMS = SmsMessage.createFromPdu((byte[]) sms[i]);
                                String strMsgFrom = msgSMS.getOriginatingAddress();
                                String strMsgBody = msgSMS.getMessageBody();
                                smsMsg = "De:" + strMsgFrom + "\n" + "Mensaje:" + strMsgBody + "\n";
                            }
                            tvLog.append(smsMsg);
                        }
                        break;
                    case XmppService.CHANGE_CONNECTIVITY:
                        ConnectivityManager conn = (ConnectivityManager)context.
                                getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo networkInfo = conn.getActiveNetworkInfo();
                        if(networkInfo == null)
                            tvLog.append("Sin conexion\n");
                        else
                            tvLog.append("Conectado\n");
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter(XmppService.UPDATE_CONNECTION);
        filter.addAction(XmppService.NEW_MESSAGE);
        //filter.addAction(XmppService.SMS_CONNECTION);
        //filter.addAction(XmppService.CHANGE_CONNECTIVITY);
        this.registerReceiver(mReceiver, filter);
        statusBroadcastReceiver = true;
    }

    private void sendMessage() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String adminNewUser = Def.ADMIN_NEW_USER + "@" + Def.SERVER_NAME;
        String datos =
                "Nombre " + nameUser + "\n" +
                        "noCell " + numberCell + "\n" +
                        "noIMEI " + numberIMEI + "\n" +
                        "email " + emailUser + "\n" +
                        "birthday " + birthday + "\n";
        Intent intent = new Intent(XmppService.SEND_MESSAGE);
        intent.setPackage(this.getPackageName());
        intent.putExtra(XmppService.BUNDLE_MESSAGE_BODY, datos);
        intent.putExtra(XmppService.BUNDLE_TO, adminNewUser);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
        this.sendBroadcast(intent);
    }

    public void onClickaddUser(View v) {
        nameUser = etNombre.getText().toString();
        numberCell = etNoCelular.getText().toString();
        numberIMEI = etNoIMEI.getText().toString();
        emailUser = etEmail.getText().toString();
        birthday = etBirthday.getText().toString();

        connectXmpp();

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
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etNombre = (EditText)findViewById(R.id.etNombre);
        etNoCelular = (EditText)findViewById(R.id.etNoCelular);
        etNoIMEI = (EditText)findViewById(R.id.etNoIMEI);
        etEmail = (EditText)findViewById(R.id.etEmail);
        etBirthday = (EditText) findViewById(R.id.etBirthday);

        etCuentaXMPP = (EditText)findViewById(R.id.etCuenta);
        etPassXMPP = (EditText)findViewById(R.id.etPassword);

        btnEnviar = (Button)findViewById(R.id.btnAddUser);

        tvLog = (TextView)this.findViewById(R.id.tvLog);
        tvLog.setText(log);

        getCellInfo();
        statusBroadcastReceiver = false;

        etNoCelular.setText(numberCell);
        etNoIMEI.setText(numberIMEI);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if(XmppService.getState().equals(XmppConnection.ConnectionState.DISCONNECTED)){
            btnEnviar.setText("Desconectado");
        }
    }

    public void connectXmpp(){
        if(XmppService.getState().equals(XmppConnection.ConnectionState.DISCONNECTED)){
            btnEnviar.setText("Conectando");
            Intent intent = new Intent(this, XmppService.class);
            //startWakefulService(this,intent);
            this.startService(intent);
        }else{
            btnEnviar.setText("Desconectando");
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
    public boolean onOptionsItemSelected(MenuItem item){
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
        if(statusBroadcastReceiver == true)
            this.unregisterReceiver(mReceiver);
    }

    @Override
    public void onPause(){
        super.onPause();
        //if(statusBroadcastReceiver == true)
        //    this.unregisterReceiver(mReceiver);
    }
}
