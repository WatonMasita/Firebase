package com.example.firebaseproyecto;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {MensajeEntidad.class}, version = 2)  //
public abstract class ChatBaseDatos extends RoomDatabase {
    public abstract MensajeDao mensajeDao();
}
