package com.example.firebaseproyecto;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "mensajes")
public class MensajeEntidad {

    @PrimaryKey
    @NonNull
    public String id;     // ID del mensaje (Firebase)

    public String uid;    // UID del usuario autenticado
    public String nombre; // Nombre visible
    public String texto;  // Mensaje

    // Constructor vacío (Room lo necesita)
    public MensajeEntidad() {
    }

    // Constructor completo (lo usas tú al crear mensajes nuevos)
    public MensajeEntidad(@NonNull String id, String uid, String nombre, String texto) {
        this.id = id;
        this.uid = uid;
        this.nombre = nombre;
        this.texto = texto;
    }
}
