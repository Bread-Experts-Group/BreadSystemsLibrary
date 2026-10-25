package org.bread_experts_group.bread_systems_library.model.codings

import org.bread_experts_group.bread_systems_library.generic.BlockingInput
import org.bread_experts_group.bread_systems_library.generic.FlushingOutput
import java.lang.constant.ClassDesc
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteOrder
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf

interface IBSLGPSerializer<T> {
    fun bslGpSerialize(data: T, output: FlushingOutput, context: EncodingContext)
}

interface IBSLGPDeserializer<T> {
    fun bslGpDeserialize(input: BlockingInput, context: DecodingContext): T
}

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class BSLGPSerializer(val serializer: KClass<out IBSLGPSerializer<*>>)

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class BSLGPDeserializer(val deserializer: KClass<out IBSLGPDeserializer<*>>)

class EncodingContext(
    val writtenTypes: MutableMap<KType, Entry> = mutableMapOf()
) {
    class Entry(
        var lastID: Int = Int.MIN_VALUE,
        val map: IdentityHashMap<Any, Int> = IdentityHashMap(),
        val preMap: IdentityHashMap<Any, Int> = IdentityHashMap()
    )
}

fun bslGPEncode(type: KType, data: Any?, buffer: FlushingOutput, context: EncodingContext = EncodingContext()) {
    val clazz = type.classifier as KClass<*>
    if (type.isMarkedNullable && data == null) {
        buffer.writeByte(0)
        return
    } else data!!

    val written = context.writtenTypes.getOrPut(type) { EncodingContext.Entry() }
    val previous = written.map[data]
    if (previous != null) {
        buffer.writeByte(2)
        buffer.writeInt(previous)
        return
    }

    fun capture(run: () -> Unit) {
        if (written.preMap.containsKey(data)) {
            throw IllegalArgumentException("Cannot encode: $type self-references itself.")
        }
        buffer.writeByte(1)
        written.preMap[data] = 0
        run()
        written.preMap.remove(data)
        written.map[data] = written.lastID++
    }

    val serializerKind = type.findAnnotation<BSLGPSerializer>()?.serializer ?: clazz.findAnnotation<BSLGPSerializer>()?.serializer
    if (serializerKind != null) {
        capture {
            @Suppress("UNCHECKED_CAST")
            (serializerKind.objectInstance as IBSLGPSerializer<Any>).bslGpSerialize(data, buffer, context)
        }
        return
    }

    if (clazz.isData || clazz.isValue) {
        capture {
            val constructor = clazz.primaryConstructor!!
            constructor.parameters.forEach { parameter ->
                @Suppress("UNCHECKED_CAST")
                val property = clazz.memberProperties.first {
                    it.name == parameter.name && it.returnType == parameter.type
                } as KProperty1<Any, *>
                bslGPEncode(property.returnType, property.get(data), buffer, context)
            }
        }
        return
    }

    capture {
        when (type.classifier as KClass<*>) {
            String::class -> {
                data as String
                val data = data.toByteArray()
                buffer.writeInt(data.size)
                buffer.writeData(data)
            }

            ClassDesc::class -> {
                data as ClassDesc
                bslGPEncode(data.descriptorString(), buffer, context)
            }

            Int::class -> {
                data as Int
                buffer.writeInt(data)
            }

            Long::class -> {
                data as Long
                buffer.writeLong(data)
            }

            BigDecimal::class -> {
                data as BigDecimal
                bslGPEncode(data.scale(), buffer, context)
                bslGPEncode(data.unscaledValue(), buffer, context)
            }

            BigInteger::class -> {
                data as BigInteger
                bslGPEncode(data.toByteArray(), buffer, context)
            }

            ByteArray::class -> {
                data as ByteArray
                bslGPEncode(data.size, buffer, context)
                buffer.writeData(data)
            }

            List::class -> {
                @Suppress("UNCHECKED_CAST")
                data as List<Any>
                bslGPEncode(data.size, buffer, context)
                data.forEach { element ->
                    bslGPEncode(type.arguments.first().type!!, element, buffer, context)
                }
            }

            Map::class -> {
                @Suppress("UNCHECKED_CAST")
                data as Map<Any, Any>
                bslGPEncode(data.size, buffer, context)
                val t0 = type.arguments[0].type!!
                val t1 = type.arguments[1].type!!
                data.forEach { (key, value) ->
                    bslGPEncode(t0, key, buffer, context)
                    bslGPEncode(t1, value, buffer, context)
                }
            }

            else if clazz.isSealed -> {
                val index = clazz.sealedSubclasses.indexOf(data::class)
                if (index == -1) TODO("open polymorphism")
                buffer.writeInt(index)
                if (data::class.objectInstance != null) return@capture
                bslGPEncode(clazz.sealedSubclasses[index].createType(), data, buffer, context)
            }

            else -> throw IllegalArgumentException("Unable to serialize $type")
        }
    }
}

inline fun <reified T : Any> bslGPEncode(
    data: T, buffer: FlushingOutput, context: EncodingContext = EncodingContext()
) = bslGPEncode(typeOf<T>(), data, buffer, context)

class DecodingContext(
    val readTypes: MutableMap<KType, Entry> = mutableMapOf()
) {
    class Entry(
        var lastID: Int = Int.MIN_VALUE,
        val map: MutableMap<Int, Any?> = mutableMapOf()
    )
}

fun bslGPDecode(type: KType, input: BlockingInput, context: DecodingContext = DecodingContext()): Any? {
    input.endian = ByteOrder.nativeOrder()
    val clazz = type.classifier as KClass<*>

    val entryKind = input.readByte()
    val written = context.readTypes.getOrPut(type) { DecodingContext.Entry() }
    when (entryKind) {
        0.toByte() -> return null
        1.toByte() -> {}
        2.toByte() -> return written.map[input.readInt()]!!
        else -> throw IllegalStateException("Illegal object code $entryKind reading $type at #${input.reads - 1}")
    }

    fun decode(): Any? {
        val deserializerKind = type.findAnnotation<BSLGPDeserializer>()?.deserializer ?: clazz.findAnnotation<BSLGPDeserializer>()?.deserializer
        if (deserializerKind != null) {
            @Suppress("UNCHECKED_CAST")
            return (deserializerKind.objectInstance as IBSLGPDeserializer<Any>).bslGpDeserialize(input, context)
        }

        if (clazz.isData || clazz.isValue) {
            val constructor = clazz.primaryConstructor!!
            return constructor.callBy(
                buildMap {
                    constructor.parameters.forEach { parameter ->
                        @Suppress("UNCHECKED_CAST")
                        val property = clazz.memberProperties.first {
                            it.name == parameter.name && it.returnType == parameter.type
                        } as KProperty1<Any, *>
                        put(parameter, bslGPDecode(property.returnType, input, context))
                    }
                }
            )
        }

        return when (type.classifier as KClass<*>) {
            String::class -> input.readBytes(input.readInt()).decodeToString()

            ClassDesc::class -> ClassDesc.ofDescriptor(bslGPDecode(input, context))

            BigDecimal::class -> {
                val scale = bslGPDecode<Int>(input, context)
                BigDecimal(bslGPDecode<BigInteger>(input, context), scale)
            }

            BigInteger::class -> bslGPDecode<ByteArray>(input, context).let {
                if (it.isEmpty()) BigInteger.ZERO else BigInteger(it)
            }

            ByteArray::class -> input.readBytes(bslGPDecode<Int>(input, context))

            Int::class -> input.readInt()

            List::class -> List(bslGPDecode<Int>(input, context)) {
                bslGPDecode(type.arguments.first().type!!, input, context)
            }

            Map::class -> buildMap {
                val t0 = type.arguments[0].type!!
                val t1 = type.arguments[1].type!!
                repeat(bslGPDecode<Int>(input, context)) {
                    put(bslGPDecode(t0, input, context), bslGPDecode(t1, input, context))
                }
            }.toMutableMap()

            else if clazz.isSealed -> {
                val index = input.readInt()
                val subclass = clazz.sealedSubclasses[index]
                subclass.objectInstance?.let { return it }
                bslGPDecode(subclass.createType(), input, context)
            }

            else -> throw IllegalArgumentException("Unable to deserialize $type")
        }
    }

    val value = decode()
    written.map[written.lastID++] = value
    return value
}

inline fun <reified T> bslGPDecode(
    input: BlockingInput, context: DecodingContext = DecodingContext()
): T = bslGPDecode(typeOf<T>(), input, context) as T