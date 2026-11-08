package com.androidtech.data.utils

import java.lang.reflect.AccessibleObject
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties

inline fun <reified T> Any.getFieldByAnnotationValue(annotationValue: String) {
    for (member in this::class.declaredMemberProperties) {
//        if (member.getter.findAnnotation<Field>() != null) {    //add .getter
//            println(member)
//        }
        println(member)
        println(member.getter.annotations)    //add .getter
    }
}

//@Throws(IllegalAccessException::class, ClassCastException::class)
inline fun <reified T> Any.getField(fieldName: String): T? {
    try {
        val kCallable =  this::class.memberProperties.firstOrNull { it.name == fieldName } ?: return null
        return kCallable.getter.call(this) as T?
    } catch (_ : Exception) {
        return null
    }
}

private val cache = mutableMapOf<String, AccessibleObject>()

/**
 * Get a method from a class
 *
 * @param obj
 *     the object or class.
 * @param name
 *     the requested method's name
 * @param types
 *     the parameter types of the requested method.
 * @return a [Method] object which represents the method matching the specified name and parameter types
 */
fun getMethod(obj: Any?, name: String, vararg types: Class<*>): Method? {
    if (obj == null) return null
    val key = cacheKey(obj, name, *types)
    return fromCache(key) ?: run {
        var klass: Class<*>? = obj as? Class<*> ?: obj.javaClass
        while (klass != null) {
            try {
                klass.getDeclaredMethod(name, *types).let { method ->
                    if (!method.isAccessible) method.isAccessible = true
                    cache[key] = method
                    return method
                }
            } catch (ignored: NoSuchMethodException) {
            }
            klass = klass.superclass
        }
        null
    }
}

/**
 * Get a field from a class.
 *
 * @param obj
 *     the object or class
 * @param name
 *     the name of the field
 * @return a [Field] object for the field with the given name which is declared in the class.
 */
fun getField(obj: Any?, name: String): Field? {
    if (obj == null) return null
    val key = cacheKey(obj, name)
    return fromCache(key) ?: run {
        var klass: Class<*>? = obj as? Class<*> ?: obj.javaClass
        while (klass != null) {
            try {
                klass.getDeclaredField(name).let { field ->
                    if (!field.isAccessible) field.isAccessible = true
                    cache[key] = field
                    return field
                }
            } catch (ignored: NoSuchFieldException) {
            }
            klass = klass.superclass
        }
        null
    }
}

/**
 * Returns a [Constructor] object
 *
 * @param parameterTypes the parameter array
 * @return  The [Constructor] object for the constructor with the specified parameter list
 */
inline fun <reified T> getConstructor(vararg parameterTypes: Class<*>): Constructor<T>? = try {
    T::class.java.getDeclaredConstructor(*parameterTypes).also {
        if (!it.isAccessible) it.isAccessible = true
    }
} catch (e: Exception) {
    null
}

/**
 * Dynamically invoke a method.
 *
 * @param obj
 *     The object or class to get the method from.
 * @param name
 *     The name of the method to invoke.
 * @param types
 *     the parameter types of the requested method.
 * @param args
 *     the arguments to the method
 * @param <T>
 *     the method's return type
 * @return the result of dynamically invoking this method.
 */
inline fun <reified T> invoke(
    obj: Any?,
    name: String,
    types: Array<Class<*>> = emptyArray(),
    vararg args: Any
): T? =
    try {
        getMethod(obj, name, *types)?.run {
            invoke(obj, *args) as? T
        }
    } catch (e: Exception) {
        null
    }

/**
 * Get the value from a field in an object/class.
 *
 * @param obj
 *     the object or class
 * @param name
 *     the name of the field
 * @param <T>
 *     The field's type
 * @return the value of the field in the specified object.
 */
inline fun <reified T> getFieldValue(obj: Any?, name: String): T? = getField(obj, name)?.let { field ->
    try {
        if (!field.isAccessible) field.isAccessible = true
        field.get(obj) as? T
    } catch (e: IllegalAccessException) {
        null
    }
}

/**
 * Set the field value to the specified value.
 *
 * @param field
 *     the field
 * @param obj
 *     the object whose field should be modified
 * @param value
 *     the new value for the field of `obj` being modified
 * @return `true` if the field was set successfully.
 */
fun setFieldValue(field: Field, obj: Any?, value: Any?): Boolean = try {
    if (!field.isAccessible) field.isAccessible = true
    if (Modifier.isFinal(field.modifiers)) {
        val modifiersField = Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
    }
    field.set(obj, value)
    true
} catch (e: IllegalAccessException) {
    false
}

private inline fun <reified T> fromCache(key: String): T? = cache[key] as? T

private fun cacheKey(obj: Any, name: String, vararg types: Class<*>) = StringBuilder().apply {
    val klass = obj as? Class<*> ?: obj::class.java
    append(klass.name).append('#').append(name)
    if (types.isNotEmpty()) {
        append('(')
        var separator = ""
        types.forEach {
            append(separator).append(it.name)
            separator = ", "
        }
        append(')')
    }
}.toString()