package mx.edu.uttt
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.vue.VueComponent
import kotliquery.HikariCP

fun main() {
    val app= Javalin.create { config ->
        config.staticFiles.apply {
            enableWebjars()
            add("public", Location.CLASSPATH)
        }
        config.vue.apply {
            vueInstanceNameInJs = "app"
            rootDirectory("/", Location.CLASSPATH)
        }
        config.router.mount {
        }.apiBuilder{ //Entry Points
            get("/", VueComponent("home-page"))
            }
        }.start()
    }