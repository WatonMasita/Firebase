package com.example.firebaseproyecto;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    EditText txtNombre, txtMensaje;
    Button btnAgregar;
    TextView txtLista;

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
        txtLista = findViewById(R.id.txtLista);

        //--------------------------
        // INICIAR ROOM
        //--------------------------
        base = Room.databaseBuilder(
                getApplicationContext(),
                ChatBaseDatos.class,
                "ChatBD.db"
        ).allowMainThreadQueries().build();

        dao = base.mensajeDao();

        //--------------------------
        // INICIAR FIREBASE
        //--------------------------
        refFirebase = FirebaseDatabase.getInstance().getReference("Chat");

        //--------------------------
        // MOSTRAR MENSAJES LOCALES
        //--------------------------
        mostrarMensajes();

        //--------------------------
        // BOTÓN AGREGAR
        //--------------------------
        btnAgregar.setOnClickListener(v -> {
            String nombre = txtNombre.getText().toString();
            String texto = txtMensaje.getText().toString();

            // Generar ID desde Firebase
            String id = refFirebase.push().getKey();

            // Guardar en Firebase
            MensajeEntidad nuevo = new MensajeEntidad(id, nombre, texto);
            refFirebase.child(id).setValue(nuevo);

            // Guardar en Room
            dao.insertar(nuevo);

            // Refrescar pantalla
            mostrarMensajes();

            txtMensaje.setText("");
        });
    }

    private void mostrarMensajes() {
        List<MensajeEntidad> mensajes = dao.obtenerTodos();

        StringBuilder builder = new StringBuilder();
        for (MensajeEntidad m : mensajes) {
            builder.append("ID: ").append(m.id).append("\n");
            builder.append("Nombre: ").append(m.nombre).append("\n");
            builder.append("Texto: ").append(m.texto).append("\n\n");
        }

        txtLista.setText(builder.toString());
    }
}
