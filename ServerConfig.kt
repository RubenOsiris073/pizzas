import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.util.*

class ServerConfig(
    val jdbcProtocol: String,
    val dbHost: String,
    val dbPort: String,
    val dbName: String,
    val dbUser: String,
    val dbPwd: String,
    val dbEncoding: String
) {
    val dbURL get() = "$jdbcProtocol://$dbHost:$dbPort/$dbName?encoding=$dbEncoding"
}

object Config {

    private val log = LoggerFactory.getLogger(this::class.java)
    val SVR_CONF: ServerConfig

    init {
        // Ruta al archivo de configuración fuera del CLASSPATH
        val configFile = File("config/server.properties")

        // Verifica si el archivo existe
        if (!configFile.exists()) {
            throw IllegalArgumentException("El archivo de configuración no se encuentra en: ${configFile.absolutePath}")
        }

        // Cargar propiedades desde el archivo externo
        val properties = Properties().apply {
            FileInputStream(configFile).use { load(it) }
        }

        // Inicializar configuración del servidor
        SVR_CONF = ServerConfig(
            jdbcProtocol = properties.getProperty("database.jdbc.protocol"),
            dbHost = properties.getProperty("database.host"),
            dbPort = properties.getProperty("database.port"),
            dbName = properties.getProperty("database.name"),
            dbUser = properties.getProperty("database.user"),
            dbPwd = properties.getProperty("database.password"),
            dbEncoding = properties.getProperty("database.encoding")
        )

        log.info("Database URL: {}", SVR_CONF.dbURL)
        log.info("Database User: {}", SVR_CONF.dbUser)
    }
}
