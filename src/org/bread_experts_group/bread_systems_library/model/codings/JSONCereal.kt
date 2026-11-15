package org.bread_experts_group.bread_systems_library.model.codings

import org.bread_experts_group.bread_systems_library.model.*
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField
import kotlin.reflect.typeOf

interface IJSONDeserializer<INPUT, OUTPUT> {
    fun jsonDeserialize(input: INPUT?): OUTPUT
}

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class JSONDeserializer(val deserializer: KClass<out IJSONDeserializer<*, *>>)

annotation class JSONName(val name: String)

fun jsonDecode(type: KType, primitive: JSONPrimitive?): Any? {
    val clazz = type.classifier as KClass<*>
    return if (clazz.isData) {
        if (primitive !is JSONObject) return null
        val constructor = clazz.primaryConstructor!!
        constructor.callBy(
            buildMap {
                val elementsMutable = primitive.elements.toMutableMap()
                constructor.parameters.forEach {
                    val name = it.findAnnotation<JSONName>()?.name ?: it.name
                    val removed = elementsMutable.remove(name)
                    if (removed != null) put(it, jsonDecode(it.type, removed))
                }
                if (elementsMutable.isNotEmpty()) {
                    println("$type")
                    elementsMutable.forEach { (key, element) ->
                        println("  $key ... $element")
                    }
                    println("===")
                }
            }
        )
    } else if (clazz.isValue) {
        val constructor = clazz.primaryConstructor!!
        constructor.call(jsonDecode(constructor.parameters[0].type, primitive))
    } else when (clazz) {
        BigDecimal::class -> (primitive as JSONNumber).number
        BigInteger::class -> (primitive as JSONNumber).number.toBigIntegerExact()
        String::class -> (primitive as? JSONString)?.value
        Boolean::class -> (primitive as? JSONBoolean)?.value
        List::class -> (primitive as? JSONArray)?.elements?.map { jsonDecode(type.arguments[0].type!!, it) }
        Map::class -> (primitive as? JSONObject)?.elements?.entries?.associate { (key, element) ->
            key to jsonDecode(type.arguments[1].type!!, element)
        }

        JSONObject::class -> primitive as? JSONObject
        JSONPrimitive::class -> primitive

        else -> {
            val deserializerKind = (type.findAnnotation<JSONDeserializer>() ?: clazz.findAnnotation<JSONDeserializer>())?.deserializer ?: throw IllegalArgumentException(
                "Unable to deserialize $type"
            )
            val deserializerInput = deserializerKind.supertypes.first { it.classifier as KClass<*> == IJSONDeserializer::class }.arguments[0].type!!
            @Suppress("UNCHECKED_CAST")
            (deserializerKind.objectInstance as IJSONDeserializer<Any?, Any>).jsonDeserialize(
                if ((deserializerInput.classifier as KClass<*>).isInstance(primitive)) primitive
                else jsonDecode(deserializerInput, primitive)
            )
        }
    }
}

inline fun <reified T> jsonDecode(primitive: JSONPrimitive?): T = jsonDecode(typeOf<T>(), primitive) as T

fun jsonEncode(type: KType, data: Any?): JSONPrimitive? {
    if (data == null) return null
    val clazz = type.classifier as KClass<*>
    return if (clazz.isData) {
        val elements = mutableMapOf<String, JSONPrimitive?>()
        clazz.memberProperties.forEach {
            if (it.javaField == null) return@forEach
            @Suppress("UNCHECKED_CAST")
            elements[it.name] = jsonEncode(it.returnType, (it as KProperty1<Any, Any>).get(data))
        }
        JSONObject(elements)
    } else when (clazz) {
        String::class -> JSONString(data as String)
        Boolean::class -> JSONBoolean(data as Boolean)
        else -> throw NotImplementedError("Other class $clazz")
    }
}

inline fun <reified T> jsonEncode(data: T?): JSONPrimitive? = jsonEncode(typeOf<T>(), data)