import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "mensajes")
public class MensajeEntidad {

    @PrimaryKey
    public String id;

    public String nombre;
    public String texto;

    public MensajeEntidad(String id, String nombre, String texto) {
        this.id = id;
        this.nombre = nombre;
        this.texto = texto;
    }
}
