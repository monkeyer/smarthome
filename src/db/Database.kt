package com.netguru.db


import com.github.mjdbc.Db
import com.netguru.AddSensorReq
import com.netguru.SensorResp
import com.netguru.TransformAction
import com.netguru.TransformReq


class Database(db: Db) {

    private val sensorSql = db.attachSql(SensorSql::class.java)
    private val transformSql = db.attachSql(TransformSql::class.java)
    private val eventSql = db.attachSql(EventSql::class.java)

    fun createTables() {
        //TODO:
        // 1. check if table version exists
        // 2. check if table version row is lower than hardcoded
        // 3. update db according to migration functions
    }

    private fun addTransforms(sensorId: Int, transforms: List<TransformReq>): List<Int> {
        val addTransforms = transforms.filter { it.action == TransformAction.ADD }
        if (addTransforms.isNotEmpty()) {
            return transformSql.insertBulk(
                addTransforms.map { TransormInsertReq(sensorId, it.name, it.transform, it.returnType, it.icon) }
            )
        }
        return emptyList()
    }

    fun getAllSensors(): List<SensorResp> {
        val result = sensorSql.getAllSensors()
        return result.map {
            it.toResp(transformSql.getAllForSensor(it.id))
        }.toList()
    }

    fun removeSensor(id: Int) {
        sensorSql.removeSensor(id)
    }

    fun getSensor(id: Int): SensorResp {
        val result = sensorSql.getSensor(id)
        return result.toResp(transformSql.getAllForSensor(id))
    }

    fun saveEvent(sensorId: Int, transformed: String, transformId: Int) {
        eventSql.insert(sensorId, transformId, transformed)
    }

    fun getEventsForTransform(transformId: Int, limit: Int): List<EventEntity> {
        return eventSql.getForTransform(transformId,limit)
    }

    fun modifySensor(id: Int?, sensorData: AddSensorReq): SensorResp {
        val sensorId = if(id == null){
            sensorSql.insertSensor(sensorData)
        } else {
            sensorSql.update(id, sensorData.name, sensorData.topic)
            id
        }

        addTransforms(sensorId, sensorData.transforms)
        removeTransforms(sensorData.transforms)
        modifyTransforms(sensorData.transforms)

        return getSensor(sensorId)
    }

    private fun removeTransforms(transforms: List<TransformReq>) {
        val toRemove = transforms
            .filter { it.action == TransformAction.REMOVE && it.id != null }
        if (toRemove.isNotEmpty()) {
            transformSql.remove(toRemove.map { it.id!! })
        }
    }

    private fun modifyTransforms(transforms: List<TransformReq>) {
        val toModify = transforms
            .filter { it.action == TransformAction.UPDATE && it.id != null}

        if( toModify.isNotEmpty()) {
            transformSql.modify(toModify)
        }
    }

}