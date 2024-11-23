package mx.edu.uttt
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
        return
    }

    // Configuración del servidor
    val app = Javalin.create { config ->
        config.staticFiles.add("src/main/resources/public", io.javalin.http.staticfiles.Location.EXTERNAL)
    }.start(7000)

    // Endpoint para la página principal
    app.get("/") { ctx ->
        ctx.redirect("/index.html")
    }

    // Endpoint para listar elementos del menú
    app.get("/menu") { ctx ->
        try {
            connectToDatabase().use { connection ->
                val menuItems = mutableListOf<Map<String, Any>>()
                val sql = "SELECT id, itemName, price FROM MenuItems"
                connection.prepareStatement(sql).use { statement ->
                    val resultSet = statement.executeQuery()
                    while (resultSet.next()) {
                        menuItems.add(
                            mapOf(
                                "id" to resultSet.getInt("id"),
                                "itemName" to resultSet.getString("itemName"),
                                "price" to resultSet.getDouble("price")
                            )
                        )
                    }
                }
                ctx.json(menuItems)
            }
        } catch (e: Exception) {
            ctx.status(500).result("Error al obtener el menú: ${e.message}")
            e.printStackTrace()
        }
    }

    // Endpoint para agregar un elemento al menú
    app.post("/menu") { ctx ->
        try {
            val itemName = ctx.formParam("itemName")
                ?: throw IllegalArgumentException("El nombre del elemento es obligatorio")
            val price = ctx.formParam("price")?.toDoubleOrNull()
                ?: throw IllegalArgumentException("El precio debe ser un número válido")

            connectToDatabase().use { connection ->
                val sql = "INSERT INTO MenuItems (itemName, price) VALUES (?, ?)"
                connection.prepareStatement(sql).use { statement ->
                    statement.setString(1, itemName)
                    statement.setDouble(2, price)
                    statement.executeUpdate()
                }
            }

            ctx.status(201).result("Elemento agregado al menú correctamente.")
        } catch (e: Exception) {
            ctx.status(500).result("Error al agregar el elemento al menú: ${e.message}")
            e.printStackTrace()
        }
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
                val total = orderItems.sumOf {
                    val price = when (val p = it["price"]) {
                        is Int -> p.toDouble() // Convierte Int a Double si es necesario
                        is Double -> p
                        else -> throw IllegalArgumentException("Precio inválido: $p")
                    }
                    val quantity = when (val q = it["quantity"]) {
                        is Int -> q
                        is Double -> q.toInt() // Convierte Double a Int si es necesario
                        else -> throw IllegalArgumentException("Cantidad inválida: $q")
                    }
                    price * quantity
                }

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
                        val price = when (val p = item["price"]) {
                            is Int -> p.toDouble()
                            is Double -> p
                            else -> throw IllegalArgumentException("Precio inválido: $p")
                        }
                        val quantity = when (val q = item["quantity"]) {
                            is Int -> q
                            is Double -> q.toInt()
                            else -> throw IllegalArgumentException("Cantidad inválida: $q")
                        }
                        statement.setInt(1, orderId)
                        statement.setString(2, item["itemName"].toString())
                        statement.setDouble(3, price)
                        statement.setInt(4, quantity)
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
