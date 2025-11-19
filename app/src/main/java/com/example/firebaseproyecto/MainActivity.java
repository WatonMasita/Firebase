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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    EditText txtNombre, txtMensaje;
    Button btnAgregar, btnSalir;
    ListView listView;

    ChatBaseDatos base;
    MensajeDao dao;
    DatabaseReference refFirebase;
    FirebaseAuth auth;

    List<MensajeEntidad> listaMensajes = new ArrayList<>();
    MensajeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtNombre = findViewById(R.id.txtNombre);
        txtMensaje = findViewById(R.id.txtMensaje);
        btnAgregar = findViewById(R.id.btnAgregar);
        btnSalir = findViewById(R.id.btnSalir);
        listView = findViewById(R.id.listViewMensajes);

        // AUTENTICACIÓN
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // ROOM
        base = Room.databaseBuilder(
                        getApplicationContext(),
                        ChatBaseDatos.class,
                        "ChatBD.db"
                ).fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();

        dao = base.mensajeDao();

        // FIREBASE
        refFirebase = FirebaseDatabase.getInstance().getReference("Chat");

        // ADAPTER
        adapter = new MensajeAdapter();
        listView.setAdapter(adapter);

        // 🔥 TIEMPO REAL
        activarTiempoReal();

        // AGREGAR MENSAJE
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

            // Guardar en Firebase
            refFirebase.child(id).setValue(nuevo);

            txtMensaje.setText("");
        });

        // SALIR
        btnSalir.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(MainActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }

    // 🔥 TIEMPO REAL (CORREGIDO)
    private void activarTiempoReal() {
        refFirebase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                dao.clearAll(); // limpiar local

                for (DataSnapshot child : snapshot.getChildren()) {

                    MensajeEntidad m = child.getValue(MensajeEntidad.class);

                    if (m != null) {
                        // 🔥 SOLUCIÓN AL CRASH
                        m.id = child.getKey();  // ← AGREGADO

                        dao.insertar(m);
                    }
                }

                cargarMensajes();
            }

            @Override
            public void onCancelled(DatabaseError error) { }
        });
    }

    private void cargarMensajes() {
        listaMensajes.clear();
        listaMensajes.addAll(dao.obtenerTodos());
        adapter.notifyDataSetChanged();
    }

    // ADAPTER
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

    // EDITAR
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

    // ELIMINAR
    private void eliminarMensaje(MensajeEntidad m) {

        new AlertDialog.Builder(this)
                .setTitle("Eliminar mensaje")
                .setMessage("¿Seguro que deseas eliminarlo?")
                .setPositiveButton("Sí", (dialog, which) -> {

                    refFirebase.child(m.id).removeValue();

                    Toast.makeText(this, "Mensaje eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
