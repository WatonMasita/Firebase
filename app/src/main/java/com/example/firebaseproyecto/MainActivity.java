package com.example.firebaseproyecto;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import info.mqtt.android.service.MqttAndroidClient;
import info.mqtt.android.service.Ack;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    EditText txtNombre, txtMensaje, txtTopicMQTT;
    Button btnAgregar, btnSalir, btnSuscribirseMQTT;
    ListView listView;

    ChatBaseDatos base;
    MensajeDao dao;
    DatabaseReference refFirebase;
    FirebaseAuth auth;

    List<MensajeEntidad> listaMensajes = new ArrayList<>();
    MensajeAdapter adapter;

    // MQTT
    MqttAndroidClient mqttClient;
    String topicMQTT = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtNombre = findViewById(R.id.txtNombre);
        txtMensaje = findViewById(R.id.txtMensaje);
        txtTopicMQTT = findViewById(R.id.txtTopicMQTT);
        btnAgregar = findViewById(R.id.btnAgregar);
        btnSalir = findViewById(R.id.btnSalir);
        btnSuscribirseMQTT = findViewById(R.id.btnSuscribirseMQTT);
        listView = findViewById(R.id.listViewMensajes);

        // --- AUTH ---
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // --- ROOM ---
        base = Room.databaseBuilder(
                        getApplicationContext(),
                        ChatBaseDatos.class,
                        "ChatBD.db"
                ).fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();

        dao = base.mensajeDao();

        // --- FIREBASE ---
        refFirebase = FirebaseDatabase.getInstance().getReference("Chat");

        // --- ADAPTER ---
        adapter = new MensajeAdapter();
        listView.setAdapter(adapter);

        activarTiempoReal();

        // -----------------------
        // BOTÓN SUSCRIBIR MQTT
        // -----------------------
        btnSuscribirseMQTT.setOnClickListener(v -> {
            topicMQTT = txtTopicMQTT.getText().toString();
            if (topicMQTT.isEmpty()) {
                Toast.makeText(this, "Ingrese un topic MQTT", Toast.LENGTH_SHORT).show();
                return;
            }
            conectarMQTT();
        });

        // -----------------------
        // BOTÓN AGREGAR MENSAJE
        // -----------------------
        btnAgregar.setOnClickListener(v -> {

            String nombre = txtNombre.getText().toString();
            String texto = txtMensaje.getText().toString();

            if (nombre.isEmpty() || texto.isEmpty()) {
                Toast.makeText(this, "Complete los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            String id = refFirebase.push().getKey();
            String uid = auth.getCurrentUser().getUid();

            MensajeEntidad nuevo = new MensajeEntidad(id, uid, nombre, texto);

            // Firebase
            refFirebase.child(id).setValue(nuevo);

            // MQTT
            if (mqttClient != null && mqttClient.isConnected()) {
                try {
                    mqttClient.publish(topicMQTT, texto.getBytes(), 0, false);
                } catch (Exception e) { e.printStackTrace(); }
            }

            txtMensaje.setText("");
        });

        // -----------------------
        // BOTÓN SALIR
        // -----------------------
        btnSalir.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }

    // ===============================
    // FIREBASE -> ROOM (TIEMPO REAL)
    // ===============================
    private void activarTiempoReal() {
        refFirebase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                dao.clearAll();
                for (DataSnapshot child : snapshot.getChildren()) {
                    MensajeEntidad m = child.getValue(MensajeEntidad.class);
                    if (m != null) {
                        m.id = child.getKey();
                        dao.insertar(m);
                    }
                }
                cargarMensajes();
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void cargarMensajes() {
        listaMensajes.clear();
        listaMensajes.addAll(dao.obtenerTodos());
        adapter.notifyDataSetChanged();
    }

    // ===============================
    // MQTT: CONECTAR
    // ===============================
    private void conectarMQTT() {

        String clientId = MqttClient.generateClientId();

        mqttClient = new MqttAndroidClient(
                getApplicationContext(),
                "tcp://broker.hivemq.com:1883",
                clientId,
                Ack.AUTO_ACK
        );

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);

        mqttClient.connect(options, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Toast.makeText(MainActivity.this, "MQTT CONECTADO", Toast.LENGTH_SHORT).show();
                suscribirseMQTT(topicMQTT);
                escucharMQTT();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Toast.makeText(MainActivity.this, "Fallo al conectar MQTT", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ===============================
    // MQTT: SUSCRIBIRSE
    // ===============================
    private void suscribirseMQTT(String topic) {
        try {
            mqttClient.subscribe(topic, 0);
        } catch (Exception e) { e.printStackTrace(); }

        Toast.makeText(this, "Suscrito a: " + topic, Toast.LENGTH_SHORT).show();
    }

    // ===============================
    // MQTT: ESCUCHAR
    // ===============================
    private void escucharMQTT() {

        mqttClient.setCallback(new MqttCallback() {
            @Override public void connectionLost(Throwable cause) {}

            @Override
            public void messageArrived(String topic, MqttMessage message) {

                String texto = message.toString();
                String id = "mqtt_" + System.currentTimeMillis();

                MensajeEntidad m = new MensajeEntidad(id, "mqtt_user", "MQTT", texto);
                dao.insertar(m);

                runOnUiThread(() -> cargarMensajes());
            }

            @Override public void deliveryComplete(IMqttDeliveryToken token) {}
        });
    }

    // ===============================
    // ADAPTER
    // ===============================
    class MensajeAdapter extends ArrayAdapter<MensajeEntidad> {

        public MensajeAdapter() {
            super(MainActivity.this, 0, listaMensajes);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_mensaje, parent, false);
            }

            TextView txtMensajeReal = convertView.findViewById(R.id.txtMensajeReal);
            TextView txtIdMensaje = convertView.findViewById(R.id.txtIdMensaje);
            Button btnEditar = convertView.findViewById(R.id.btnEditar);
            Button btnEliminar = convertView.findViewById(R.id.btnEliminar);

            MensajeEntidad m = listaMensajes.get(position);

            txtMensajeReal.setText(m.nombre + ": " + m.texto);
            txtIdMensaje.setText("ID: " + m.id);

            btnEditar.setOnClickListener(v -> mostrarDialogEditar(m));
            btnEliminar.setOnClickListener(v -> eliminarMensaje(m));

            return convertView;
        }
    }

    private void mostrarDialogEditar(MensajeEntidad m) {
        EditText txtNuevo = new EditText(this);
        txtNuevo.setText(m.texto);

        new AlertDialog.Builder(this)
                .setTitle("Editar mensaje")
                .setView(txtNuevo)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    m.texto = txtNuevo.getText().toString();
                    refFirebase.child(m.id).setValue(m);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarMensaje(MensajeEntidad m) {

        new AlertDialog.Builder(this)
                .setTitle("Eliminar mensaje")
                .setMessage("¿Seguro que deseas eliminarlo?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    refFirebase.child(m.id).removeValue();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
