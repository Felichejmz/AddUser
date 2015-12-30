package com.example.feliche.adduser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.StrictMode;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;

import servicio.XmppConnection;
import servicio.XmppService;

public class MainActivity extends Activity {

    private static MainActivity inst;
    private static boolean statusBroadcastReceiver;

    User user = new User();

    EditText etNombre, etNoCelular, etNoIMEI, etEmail;
    EditText etCuentaXMPP, etPassXMPP, etBirthday;
    Button btnEnviar;
    TextView tvLog;
    String nameUser, numberCell, numberIMEI, emailUser, birthday;
    String accountXmpp, passwordXmpp;
    ImageView imageView;

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
                    // mensaje XMPP
                    case XmppService.NEW_MESSAGE:
                        String from = intent.getStringExtra(XmppService.BUNDLE_FROM_JID);
                        String message = intent.getStringExtra(XmppService.BUNDLE_MESSAGE_BODY);
                        etCuentaXMPP.setText(from.split("@")[0]);
                        etPassXMPP.setText(message);
                        break;
                    // Estado de la conexión XMPP
                    case XmppService.UPDATE_CONNECTION:
                        String status = intent.getStringExtra(XmppService.CONNECTION);
                        tvLog.append("\n" + status);
                        if (status.contains("AUTHENTICATE")) {
                            if (accountXmpp.contains(Def.NEW_USER_ACCOUNT))
                                sendMessage();
                            else
                                tvLog.append("\n" + "CONECTADO CON USUARIO REGISTRADO");
                        }
                        btnEnviar.setText(status);
                        break;
                    // Mensaje SMS
                    case XmppService.SMS_CONNECTION:
                        Bundle bundle = intent.getExtras();
                        if(bundle != null) {
                            Object[] sms = (Object[]) bundle.get("pdus");
                            String smsMsg = null;
                            for (int i = 0; i < sms.length; i++) {
                                SmsMessage msgSMS = SmsMessage.createFromPdu((byte[]) sms[i]);
                                String strMsgFrom = msgSMS.getOriginatingAddress();
                                String strMsgBody = msgSMS.getMessageBody();
                                smsMsg = "De:" + strMsgFrom + "\n" + "Mensaje:" + strMsgBody;
                                String aleatorio = strMsgBody.split(":")[1];
                                etPassXMPP.setText(aleatorio);
                                passwordXmpp = calculatePassword(aleatorio, numberIMEI);
                                accountXmpp = numberCell;
                                saveUserPass(accountXmpp, passwordXmpp);
                                //saveUserPass(numberCell, numberCell);
                                connectAccount();
                            }
                            tvLog.append(smsMsg);
                        }
                        break;
                    // Cambio de la conexión a Internet
                    case XmppService.CHANGE_CONNECTIVITY:
                        ConnectivityManager conn = (ConnectivityManager)context.
                                getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo networkInfo = conn.getActiveNetworkInfo();
                        if(networkInfo == null)
                            tvLog.append("Sin conexion\n");
                        else
                            tvLog.append(" :) Conectado");
                        break;

                    case XmppService.NEW_VCARD:
                        Bundle bundleVcard = intent.getBundleExtra(XmppService.VCARD);
                        if (bundleVcard != null) {
                            byte[] avatar = bundleVcard.getByteArray("avatar");
                            String nameAvatar = bundleVcard.getString("emailHome");
                            Bitmap bmp = BitmapFactory.decodeByteArray(
                                    avatar, 0, avatar.length);
                            imageView.setImageBitmap(bmp);
                        }
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter(XmppService.UPDATE_CONNECTION);
        filter.addAction(XmppService.NEW_MESSAGE);
        filter.addAction(XmppService.SMS_CONNECTION);
        filter.addAction(XmppService.CHANGE_CONNECTIVITY);

        filter.addAction(XmppService.NEW_VCARD);
        this.registerReceiver(mReceiver, filter);
        statusBroadcastReceiver = true;
    }

    private void sendMessage() {
        // se requiere de un tiempo para establecer la
        // conexión antes de enviar el mensaje
        try {
            Thread.sleep(Def.TIME_CONNECT_TO_XMPP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // envia el dato al administrador
        String adminNewUser = Def.ADMIN_NEW_USER + "@" + Def.SERVER_NAME;
        // forma la cadena a enviar en formato json de google
        Gson gson = new Gson();
        gson.toJson(user);

        Intent intent = new Intent(XmppService.SEND_MESSAGE);
        intent.setPackage(this.getPackageName());
        intent.putExtra(XmppService.BUNDLE_MESSAGE_BODY, gson.toJson(user));
        intent.putExtra(XmppService.BUNDLE_TO, adminNewUser);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
        this.sendBroadcast(intent);

        // solicitar VCard

        Intent intentVCard = new Intent(XmppService.GET_VCARD);
        intentVCard.setPackage(this.getPackageName());
        intentVCard.putExtra(XmppService.ACCOUNT, "feliche@htu.isramoon.xyz");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
        this.sendBroadcast(intentVCard);


        // espera a que el mensaje sea enviado antes de cerrar la conexión
        try {
            Thread.sleep(Def.TIME_TO_SEND_MESSAGE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // cierra la conexión
        connectXmpp();
    }

    public void onClickaddUser(View v) {
        if (!accountXmpp.matches(Def.NEW_USER_ACCOUNT)) {
            connectAccount();
            return;
        }

        numberCell = etNoCelular.getText().toString();

        // los últimos 10 digitos
        // quitar el + y otros números adicionales
        // verificar en caso que el usuario edite el campo
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
        // Si el numero es "validado" lo asigna como la cuenta
        // y coloco los valores al usuario
        user.setAccount(numberCell);
        user.setUserName(etNombre.getText().toString());
        user.setImei(etNoIMEI.getText().toString());
        user.setEmail(etEmail.getText().toString());
        user.setBirthday(etBirthday.getText().toString());

        connectXmpp();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // lee el usuario y password de memoria no volátil
        // si no existe coloca los default de New user
        accountXmpp = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(Def.XMPP_ACCOUNT, Def.NEW_USER_ACCOUNT);
        passwordXmpp = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(Def.XMPP_PASSWORD, Def.NEW_USER_PASS);


        etNombre = (EditText)findViewById(R.id.etNombre);
        etNoCelular = (EditText)findViewById(R.id.etNoCelular);
        etNoIMEI = (EditText)findViewById(R.id.etNoIMEI);
        etEmail = (EditText)findViewById(R.id.etEmail);
        etBirthday = (EditText) findViewById(R.id.etBirthday);

        etCuentaXMPP = (EditText)findViewById(R.id.etCuenta);
        etPassXMPP = (EditText)findViewById(R.id.etPassword);

        btnEnviar = (Button)findViewById(R.id.btnAddUser);

        tvLog = (TextView)this.findViewById(R.id.tvLog);

        imageView = (ImageView) this.findViewById(R.id.imageView);

        //imageView.setImageDrawable();

        // obtiene el numero celular
        numberCell = getCellNumber();
        etNoCelular.setText(numberCell);
        // obtiene el id del dispositivo
        // (solo 6 digitos)
        numberIMEI = getNumberIMEI();
        etNoIMEI.setText(numberIMEI);

        statusBroadcastReceiver = false;

        // modo depuración de la conexión
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // coloca el teto del botón de acuerdo al último estado
        // pausa, minimiza, sleep, etc.
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
    public void onStop() {
        super.onStop();
        if (statusBroadcastReceiver)
            this.unregisterReceiver(mReceiver);
    }

    @Override
    public void onPause(){
        super.onPause();
        //if(statusBroadcastReceiver == true)
        //    this.unregisterReceiver(mReceiver);
    }

    private void connectAccount() {
        if (XmppService.getState().equals(XmppConnection.ConnectionState.DISCONNECTED)) {
            Intent intent = new Intent(this, XmppService.class);
            this.startService(intent);
        } else {
            Intent intent = new Intent(this, XmppService.class);
            this.stopService(intent);
            return;
        }
    }

    private String calculatePassword(String numberRandom, String numberIMEI) {
        // cadenas de IMEI y RANDOM de cadenas a enteros para calcular XOR
        int intIMEI = Integer.parseInt(numberIMEI);
        int intRandom = Integer.parseInt(numberRandom);
        // XOR para obtener el password
        return String.valueOf(intIMEI ^ intRandom);
    }

    public String getCellNumber() {
        String numeroCelular;
        TelephonyManager tf = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        numeroCelular = tf.getLine1Number();
        // los últimos 10 digitos
        // quitar el + y otros números adicionales
        if (numeroCelular.length() > Def.SIZE_CELL_NUMBER) {
            numeroCelular = numeroCelular.substring(
                    numeroCelular.length() - Def.SIZE_CELL_NUMBER);
        }
        return numeroCelular;
    }

    private String getNumberIMEI() {
        String numeroIMEI;
        TelephonyManager tf = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        // lee el ID del dispositivo
        numeroIMEI = tf.getDeviceId();
        // En caso de ser cero asigno un numero aleatorio entre 111,111 y 999,999
        // o numero fijo para el IMEI, despues lo decido
        if (Integer.parseInt(numeroIMEI) == 0) {
            numeroIMEI = "641735";
            /*
            Random random = new Random();
            int imei = random.nextInt(999999 - 111111) + 111111;
            numeroIMEI = String.valueOf(imei);
            */
        }
        // obtengo solo los últimos 6 digitos
        if (numeroIMEI.length() > 6)
            numeroIMEI = numeroIMEI.substring(numeroIMEI.length() - 6);
        return numeroIMEI;
    }

    private void saveUserPass(String accountXmpp, String passwordXmpp) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit()
                .putString(Def.XMPP_ACCOUNT, accountXmpp)
                .putString(Def.XMPP_PASSWORD, String.valueOf(passwordXmpp))
                .apply();
    }
}
