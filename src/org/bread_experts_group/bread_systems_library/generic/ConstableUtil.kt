package org.bread_experts_group.bread_systems_library.generic

import java.lang.classfile.ClassBuilder
import java.lang.classfile.CodeBuilder
import java.lang.classfile.MethodBuilder
import java.lang.constant.ClassDesc
import java.lang.constant.MethodTypeDesc
import java.lang.reflect.AccessFlag
import java.lang.reflect.Method
import kotlin.reflect.KClass

val Class<*>.desc: ClassDesc
    get() = this.describeConstable().get()
val KClass<*>.desc: ClassDesc
    get() = this.java.desc

inline fun <reified T> desc(): ClassDesc = T::class.java.desc

val Method.desc: MethodTypeDesc
    get() = MethodTypeDesc.of(
        this.returnType.desc,
        this.parameterTypes.map(Class<*>::desc)
    )

fun ClassBuilder.implements(method: Method, builder: MethodBuilder.() -> Unit) {
    this.withMethod(method.name, method.desc, method.modifiers, builder)
}

fun ClassBuilder.implementsBody(method: Method, builder: CodeBuilder.() -> Unit) {
    if (
        method.accessFlags().containsAll(
            listOf(AccessFlag.NATIVE, AccessFlag.ABSTRACT)
        )
    ) throw IllegalArgumentException("$method cannot have a body")
    this.withMethodBody(
        method.name, method.desc,
        method.modifiers and AccessFlag.ABSTRACT.mask().inv(), builder
    )
}