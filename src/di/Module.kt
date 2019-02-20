package com.netguru.di

import com.github.mjdbc.DbFactory
import com.netguru.db.Database
import com.netguru.MqttClient
import com.netguru.MqttWorker
import com.netguru.WorkerCmd
import com.netguru.db.*
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.channels.Channel
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.koin.dsl.module.module


val DbModule = module {
    single {
        val source = HikariDataSource().apply {
            jdbcUrl = "jdbc:postgresql://192.168.0.21:5432/pi"
            username = "pi"
            password = "spitulis"
            minimumIdle = 1
            isAutoCommit = false
        }
        DbFactory.wrap(source).apply {
            registerMapper(SensorEntity::class.java, SensorMapper())
            registerMapper(TransformEntity::class.java, TransformMapper())
            registerMapper(EventEntity::class.java, EventMapper())
        }
    }
}

val MainModule = module {
    single("workerChannel") { Channel<WorkerCmd>() }
    single { MqttClient(MqttAsyncClient("tcp://192.168.0.21:1883", "ktor-server")) }
    single { Database(get()) }
    single { MqttWorker(get(), get(), get("workerChannel")) }
}