import org.slf4j.LoggerFactory
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
        // Cargar el archivo desde el CLASSPATH (dentro del JAR)
        val properties = Properties()
        val classLoader = Config::class.java.classLoader
        val configStream = classLoader.getResourceAsStream("config/server.properties")
            ?: throw IllegalArgumentException("No se pudo cargar config/server.properties desde el JAR")

        properties.load(configStream)

        // Inicializar la configuración del servidor
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
