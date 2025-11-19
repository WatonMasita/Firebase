package com.example.firebaseproyecto;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    EditText txtNombre, txtMensaje;
    Button btnAgregar;
    ListView listView;

    ArrayAdapter<String> adapter;
    List<MensajeEntidad> listaMensajes;

    ChatBaseDatos base;
    MensajeDao dao;

    DatabaseReference refFirebase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtNombre = findViewById(R.id.txtNombre);
        txtMensaje = findViewById(R.id.txtMensaje);
        btnAgregar = findViewById(R.id.btnAgregar);
        listView = findViewById(R.id.listViewMensajes);


        // INICIAR ROOM

        base = Room.databaseBuilder(
                getApplicationContext(),
                ChatBaseDatos.class,
                "ChatBD.db"
        ).allowMainThreadQueries().build();

        dao = base.mensajeDao();


        // INICIAR FIREBASE

        refFirebase = FirebaseDatabase.getInstance().getReference("Chat");


        // MOSTRAR MENSAJES LOCALES

        mostrarMensajes();


        // BOTÓN AGREGAR

        btnAgregar.setOnClickListener(v -> {
            String nombre = txtNombre.getText().toString();
            String texto = txtMensaje.getText().toString();

            if (nombre.isEmpty() || texto.isEmpty()) {
                Toast.makeText(this, "Complete ambos campos", Toast.LENGTH_SHORT).show();
                return;
            }

            // Generar ID desde Firebase
            String id = refFirebase.push().getKey();

            // Crear entidad
            MensajeEntidad nuevo = new MensajeEntidad(id, nombre, texto);

            // Guardar en Firebase
            refFirebase.child(id).setValue(nuevo);

            // Guardar en Room
            dao.insertar(nuevo);

            // Refrescar pantalla
            mostrarMensajes();

            txtMensaje.setText("");
        });


        // ELIMINAR MENSAJE (Long-click)

        listView.setOnItemLongClickListener((parent, view, position, id) -> {

            MensajeEntidad seleccionado = listaMensajes.get(position);

            // Eliminar en Room
            dao.eliminar(seleccionado);

            // Eliminar en Firebase
            refFirebase.child(seleccionado.id).removeValue();

            mostrarMensajes();

            Toast.makeText(this, "Mensaje eliminado", Toast.LENGTH_SHORT).show();

            return true;
        });
    }


    // MOSTRAR MENSAJES EN LISTVIEW

    private void mostrarMensajes() {

        listaMensajes = dao.obtenerTodos();

        List<String> textos = new ArrayList<>();

        for (MensajeEntidad m : listaMensajes) {
            textos.add(m.nombre + ": " + m.texto);
        }

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                textos);

        listView.setAdapter(adapter);
    }
}
