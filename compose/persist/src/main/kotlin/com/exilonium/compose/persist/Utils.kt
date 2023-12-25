package com.exilonium.compose.persist

import android.content.Context
import android.content.ContextWrapper

val Context.persistMap: PersistMap?
    get() = findOwner<PersistMapOwner>()?.persistMap

internal inline fun <reified T> Context.findOwner(): T? {
    var context = this
    while (context is ContextWrapper) {
        if (context is T) return context
        context = context.baseContext
    }
    return findApplicationOwner()
}

internal inline fun <reified T> Context.findApplicationOwner(): T? {
    return if (this is T) this else applicationContext as? T
}
