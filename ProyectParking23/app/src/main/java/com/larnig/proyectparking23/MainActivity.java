package com.larnig.proyectparking23;


import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mAddressDevices;
    private ArrayAdapter<String> mNameDevices;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket mBluetoothSocket = null;
    private boolean isConnected = false;
    private String address;

    private TextView textViewReceived;
    private TextView textViewDateTime;
    private TextView textViewDateTimeHours;
    private TextView textView4;
    private Handler handler; // multi hilos
    private Runnable runnable; // fecha y hora

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAddressDevices = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        mNameDevices = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        Button idBtnOnBT = findViewById(R.id.idBtnOnBT);
        Button idBtnOffBT = findViewById(R.id.idBtnOffBT);
        Button idBtnConect = findViewById(R.id.idBtnConect);
        Button idBtnDispBT = findViewById(R.id.idBtnDispBT);
        Spinner idSpinDisp = findViewById(R.id.idSpinDisp);
        textViewReceived = findViewById(R.id.textView3);
        textView4 = findViewById(R.id.textView4);



        handler = new Handler();

        ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Log.i("MainActivity", "Bluetooth Enabled");
                    }
                }
        );

        // Initialize Bluetooth adapter
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = bluetoothManager.getAdapter();

        // Check if Bluetooth is supported
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth no está disponible en este dispositivo", Toast.LENGTH_LONG).show();
            return;
        }

        // Enable Bluetooth button
        idBtnOnBT.setOnClickListener(v -> {
            if (mBtAdapter.isEnabled()) {
                Toast.makeText(this, "Bluetooth ya está habilitado", Toast.LENGTH_LONG).show();
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_ENABLE_BT);
                    return;
                }
                enableBluetoothLauncher.launch(enableBtIntent);
            }
        });

        // Disable Bluetooth button
        idBtnOffBT.setOnClickListener(v -> {
            if (!mBtAdapter.isEnabled()) {
                Toast.makeText(this, "Bluetooth ya está desactivado", Toast.LENGTH_LONG).show();
            } else {
                mBtAdapter.disable();
                Toast.makeText(this, "Bluetooth ha sido desactivado", Toast.LENGTH_LONG).show();
            }
        });

        // Paired devices button
        idBtnDispBT.setOnClickListener(v -> {
            if (mBtAdapter.isEnabled()) {
                Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
                mAddressDevices.clear();
                mNameDevices.clear();

                if (pairedDevices != null) {
                    for (BluetoothDevice device : pairedDevices) {
                        String deviceName = device.getName();
                        String deviceHardwareAddress = device.getAddress();
                        mAddressDevices.add(deviceHardwareAddress);
                        mNameDevices.add(deviceName);
                    }
                }

                idSpinDisp.setAdapter(mNameDevices);
            } else {
                String noDevices = "No se pudo emparejar ningún dispositivo";
                mAddressDevices.add(noDevices);
                mNameDevices.add(noDevices);
                Toast.makeText(this, "Primero empareje un dispositivo Bluetooth", Toast.LENGTH_LONG).show();
            }
        });

        // Connect button
        idBtnConect.setOnClickListener(v -> {
            try {
                if (mBluetoothSocket == null || !isConnected) {
                    int selectedDevicePosition = idSpinDisp.getSelectedItemPosition();
                    if (selectedDevicePosition < 0) {
                        Toast.makeText(this, "Seleccione un dispositivo para conectarse", Toast.LENGTH_LONG).show();
                        return;
                    }
                    address = mAddressDevices.getItem(selectedDevicePosition);

                    Toast.makeText(this, "Conectado a: " + address, Toast.LENGTH_LONG).show();

                    mBtAdapter.cancelDiscovery();
                    BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
                    mBluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                    mBluetoothSocket.connect();
                    isConnected = true;
                    startListeningForData();
                }

                Toast.makeText(this, "Conexión exitosa", Toast.LENGTH_LONG).show();
                Log.i("MainActivity", "Connection Successful");

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error de Conexión", Toast.LENGTH_LONG).show();
                Log.i("MainActivity", "Connection Error");
            }
        });

        // Date and time setup
        textViewDateTime = findViewById(R.id.textViewDateTime);
        textViewDateTimeHours = findViewById(R.id.textViewDateTimeHours);

        runnable = new Runnable() {
            @Override
            public void run() {
                String currentDateTime = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
                String currentDateTimeHours = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                textViewDateTime.setText(currentDateTime);
                textViewDateTimeHours.setText(currentDateTimeHours);
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);

        // Abrir pestaña pequeña
        Button showDialogButton = findViewById(R.id.show_dialog_button);
        showDialogButton.setOnClickListener(v -> showDialog());

        // boton restablecer contador

        ImageButton buttonRestor = findViewById(R.id.buttonRestor);
        buttonRestor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restorConta();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        if (mBluetoothSocket != null) {
            try {
                mBluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startListeningForData() {
        new Thread(() -> {
            while (isConnected) {
                try {
                    InputStream inputStream = mBluetoothSocket.getInputStream();
                    byte[] buffer = new byte[1024];
                    int bytes;
                    if (inputStream.available() > 0) {
                        bytes = inputStream.read(buffer);
                        String incomingMessage = new String(buffer, 0, bytes).trim();

                        handler.post(() -> {
                            //7textViewReceived.setText(incomingMessage);
                            try {
                                int contador = Integer.parseInt(incomingMessage);
                                int espaciosLibres;

                                if (contador < 9) {
                                    textViewReceived.setText(String.valueOf(contador));
                                    espaciosLibres = 9 - contador;
                                    textView4.setText(String.valueOf(espaciosLibres));
                                } else if (contador == 9) {
                                    textViewReceived.setText("Capacidad máxima");
                                    textView4.setText("0");
                                    sendCommand("A");
                                    Toast.makeText(MainActivity.this, "Límite de aparcamiento alcanzado", Toast.LENGTH_LONG).show();

                                }
//                                if(contador > 9 ) {
//                                    sendCommand("B");
//                                    //textViewReceived.setText("Capacidad excedida");
//                                    //textView4.setText("Sin espacio disponible");
//                                    Toast.makeText(MainActivity.this, "Sin  espacio disponible, se restablecera contador", Toast.LENGTH_LONG).show();
//                                }


                                Log.i("MainActivity", "Received: " + incomingMessage);
                            } catch (NumberFormatException e) {
                                Log.e("MainActivity", "Error parsing incoming message: " + incomingMessage);
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // restablecer contador
    private void restorConta(){
        sendCommand("B");
        Toast.makeText(MainActivity.this, "Se ha restablecido el contador", Toast.LENGTH_LONG).show();
    }

    private void sendCommand(String input) {
        if (mBluetoothSocket != null) {
            try {
                mBluetoothSocket.getOutputStream().write(input.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // ventana modal
    private void showDialog() {
        // Crear el diálogo
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout. tabla_registro, null);
        builder.setView(dialogView);

        // Crear el diálogo
        AlertDialog dialog = builder.create();

        // Configurar el botón aceptar de la ventana pequeña
        Button dialogButton = dialogView.findViewById(R.id.dialog_button);
        dialogButton.setOnClickListener(v -> dialog.dismiss());

        // Mostrar el diálogo
        dialog.show();

        // Ajustar el tamaño del diálogo
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 1); // 80% del ancho de la pantalla
        params.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.8); // 80% del alto de la pantalla
        dialog.getWindow().setAttributes(params);
    }



}
