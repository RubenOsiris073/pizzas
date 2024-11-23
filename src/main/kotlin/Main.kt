import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.javalin.Javalin
import java.sql.Connection
import java.sql.DriverManager

fun main() {
    // Configuración de la base de datos
    val url = "jdbc:firebirdsql://localhost:3050/dessert?encoding=UTF8"
    val user = "sysdba"
    val password = "MyPass123"

    // Función para conectar a la base de datos
    fun connectToDatabase(): Connection = DriverManager.getConnection(url, user, password)

    // Verificar conexión al inicio
    try {
        connectToDatabase().use {
            println("Conexión exitosa a la base de datos.")
        }
    } catch (e: Exception) {
        println("Error al conectar a la base de datos: ${e.message}")
        e.printStackTrace()
        return // Detener el programa si no se puede conectar
    }

    // Configuración del servidor
    val app = Javalin.create { config ->
        config.staticFiles.add("src/main/resources/public", io.javalin.http.staticfiles.Location.EXTERNAL)
    }.start(7000)

    // Endpoint para la página principal
    app.get("/") { ctx ->
        ctx.redirect("/index.html") // Redirige al archivo estático index.html
    }

    // Endpoint para procesar una orden
    app.post("/order") { ctx ->
        try {
            val orderData = ctx.formParam("orderData")
                ?: throw IllegalArgumentException("Los datos de la orden son obligatorios")

            println("Datos recibidos: $orderData")

            // Parsear los datos de la orden
            val orderItems = jacksonObjectMapper().readValue<List<Map<String, Any>>>(orderData)

            connectToDatabase().use { connection ->
                // Calcular el total
                val total = orderItems.sumOf { (it["price"] as Double) * (it["quantity"] as Int) }

                // Insertar la orden en la base de datos
                val orderId = connection.prepareStatement(
                    "INSERT INTO Orders (total) VALUES (?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS
                ).use { statement ->
                    statement.setDouble(1, total)
                    statement.executeUpdate()
                    val keys = statement.generatedKeys
                    if (keys.next()) keys.getInt(1) else throw Exception("No se pudo obtener el ID de la orden")
                }

                // Insertar los ítems de la orden
                val sqlItem = "INSERT INTO OrderItems (orderId, itemName, price, quantity) VALUES (?, ?, ?, ?)"
                connection.prepareStatement(sqlItem).use { statement ->
                    for (item in orderItems) {
                        statement.setInt(1, orderId)
                        statement.setString(2, item["itemName"].toString())
                        statement.setDouble(3, item["price"] as Double)
                        statement.setInt(4, item["quantity"] as Int)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
            }

            ctx.status(201).result("Orden procesada correctamente.")
        } catch (e: Exception) {
            ctx.status(500).result("Error al procesar la orden: ${e.message}")
            e.printStackTrace()
        }
    }

    // Endpoint para listar órdenes
    app.get("/orders") { ctx ->
        try {
            connectToDatabase().use { connection ->
                val orders = mutableListOf<Map<String, Any>>()
                val sqlOrders = "SELECT id, total FROM Orders"
                connection.prepareStatement(sqlOrders).use { statementOrder ->
                    val resultSet = statementOrder.executeQuery()
                    while (resultSet.next()) {
                        val orderId = resultSet.getInt("id")
                        val total = resultSet.getDouble("total")

                        val items = mutableListOf<Map<String, Any>>()
                        val sqlItems = "SELECT itemName, price, quantity FROM OrderItems WHERE orderId = ?"
                        connection.prepareStatement(sqlItems).use { statementItem ->
                            statementItem.setInt(1, orderId)
                            val resultSetItems = statementItem.executeQuery()
                            while (resultSetItems.next()) {
                                items.add(
                                    mapOf(
                                        "itemName" to resultSetItems.getString("itemName"),
                                        "price" to resultSetItems.getDouble("price"),
                                        "quantity" to resultSetItems.getInt("quantity")
                                    )
                                )
                            }
                        }

                        orders.add(
                            mapOf(
                                "orderId" to orderId,
                                "total" to total,
                                "items" to items
                            )
                        )
                    }
                }

                ctx.json(orders)
            }
        } catch (e: Exception) {
            ctx.status(500).result("Error al obtener las órdenes: ${e.message}")
            e.printStackTrace()
        }
    }
}
