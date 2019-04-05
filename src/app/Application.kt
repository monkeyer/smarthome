package app


import app.routes.*
import com.netguru.db.Database
import di.ConfigModule
import di.DbModule
import di.MainModule
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.routing.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mqtt.MqttWorker
import org.koin.ktor.ext.inject
import org.koin.ktor.ext.installKoin
import java.text.DateFormat
import org.eclipse.paho.client.mqttv3.MqttAsyncClient as PahoAsync


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    installKoin(listOf(ConfigModule, DbModule, MainModule))

    install(CORS) {
        anyHost()
        method(HttpMethod.Get)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.AccessControlAllowOrigin)
        header(HttpHeaders.AccessControlAllowMethods)
    }

    install(ContentNegotiation) {
        gson {
            setDateFormat(DateFormat.SHORT)
        }
    }

    val db by inject<Database>()
    val worker by inject<MqttWorker>()

    GlobalScope.launch {
        worker.connectAndRun()
    }

    routing {
        trace { application.log.trace(it.buildText()) }

        getAllSensors(db)
        addSensor(db, worker)
        removeSensor(db, worker)
        getEventsForTransform(db)
        addEvent(worker)
        modifySensor(db,worker)
        saveSettings()
    }
}
