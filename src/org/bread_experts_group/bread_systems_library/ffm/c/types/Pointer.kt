package org.bread_experts_group.bread_systems_library.ffm.c.types

import org.bread_experts_group.bread_systems_library.ffm.NativeMethodFilters
import org.bread_experts_group.bread_systems_library.ffm.Structure
import org.bread_experts_group.bread_systems_library.ffm.Union
import org.bread_experts_group.bread_systems_library.ffm.memoryLayout
import org.bread_experts_group.bread_systems_library.generic.Order
import org.bread_experts_group.bread_systems_library.generic.ValueProvider
import java.lang.foreign.*
import java.lang.foreign.MemoryLayout.PathElement.groupElement
import java.lang.invoke.VarHandle
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaSetter

open class Pointer<T : Any>(private val encoded: KClass<T>, val segment: MemorySegment) {
    companion object {
        fun computeLayout(clazz: KClass<*>, linker: Linker): MemoryLayout {
            val layouts = mutableListOf<MemoryLayout>()
            clazz.memberProperties.sortedBy { it.findAnnotation<Order>()!!.order }.forEach {
                layouts.add((it.returnType.classifier as KClass<*>).memoryLayout(linker).withName(it.name))
            }
            val layout = if (Union::class.isSuperclassOf(clazz)) MemoryLayout.unionLayout(
                *layouts.toTypedArray()
            ) else MemoryLayout.structLayout(
                *layouts.toTypedArray()
            )
            return layout
        }

        inline fun <reified T : Structure> structure(arena: Arena, linker: Linker = Linker.nativeLinker()): Pointer<T> {
            return Pointer(T::class, arena.allocate(computeLayout(T::class, linker)))
        }
    }

    val value: T by lazy {
        if (encoded == Nothing::class) throw UnsupportedOperationException("Opaque pointer")
        if (Structure::class.isSuperclassOf(encoded)) {
            val layout = computeLayout(encoded, Linker.nativeLinker())

            val methods = mutableMapOf<Method, Triple<Boolean, VarHandle, KClass<*>>>()
            encoded.memberProperties.forEach {
                val handle = layout.varHandle(groupElement(it.name))
                if (it is KMutableProperty<*>) methods[it.javaSetter!!] = Triple(true, handle, it.returnType.classifier as KClass<*>)
                methods[it.javaGetter!!] = Triple(false, handle, it.returnType.classifier as KClass<*>)
            }

            @Suppress("UNCHECKED_CAST")
            return@lazy Proxy.newProxyInstance(
                this::class.java.classLoader,
                arrayOf(encoded.java)
            ) { _, method, args ->
                val triple = methods[method]
                if (triple != null) {
                    val (setter, handle, provider) = triple
                    if (setter) {
                        when (val layout = provider.memoryLayout(Linker.nativeLinker())) {
                            ValueLayout.JAVA_INT -> handle.set(
                                segment,
                                0L, NativeMethodFilters.fixProviderInt(args[0] as ValueProvider)
                            )
                            else -> throw NotImplementedError("$layout")
                        }
                        return@newProxyInstance null
                    } else {
                        return@newProxyInstance when (val value = handle.get(segment, 0L)) {
                            is Short -> NativeMethodFilters.fixShort(value, provider)
                            is Int -> NativeMethodFilters.fixInt(value, provider)
                            else -> throw NotImplementedError("$value")
                        }
                    }
                }
                throw IllegalStateException("$method")
            } as T
        }
        throw NotImplementedError("$encoded")
    }
}