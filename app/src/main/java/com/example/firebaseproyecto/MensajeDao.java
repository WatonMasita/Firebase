import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MensajeDao {

    @Insert
    void insertar(MensajeEntidad m);

    @Update
    void actualizar(MensajeEntidad m);

    @Delete
    void eliminar(MensajeEntidad m);

    @Query("SELECT * FROM mensajes")
    List<MensajeEntidad> obtenerTodos();
}
