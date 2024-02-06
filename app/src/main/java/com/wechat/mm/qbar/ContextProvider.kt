package com.wechat.mm.qbar

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import java.util.concurrent.atomic.AtomicReference

object ContextProvider {

    private val contextAtomicReference = AtomicReference<Context>()


    @Synchronized
    @JvmStatic
    fun get(): Context {
        if (contextAtomicReference.get() != null) {
            return contextAtomicReference.get()
        }

        synchronized(contextAtomicReference) {
            try {
                @SuppressLint("PrivateApi")
                val activityThread =
                    Class.forName("android.app.ActivityThread")

                @SuppressLint("DiscouragedPrivateApi")
                val currentApplicationMethod =
                    activityThread.getDeclaredMethod("currentApplication")
                val app = currentApplicationMethod.invoke(null)
                if (setValue(app)) {
                    return contextAtomicReference.get()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                @SuppressLint("PrivateApi")
                val app =
                    Class.forName("android.app.AppGlobals").getMethod("getInitialApplication")
                        .invoke(null)
                if (setValue(app)) {
                    return contextAtomicReference.get()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return contextAtomicReference.get()
        }
    }

    @JvmStatic
    fun setValue(app: Any?): Boolean {
        if (app is Application) {
            val context = app.applicationContext
            if (context != null) {
                contextAtomicReference.set(context)
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun getAppSharedPreferences(): SharedPreferences {
        return Holder.sSharedPreferences
    }

    private object Holder {
        val sSharedPreferences: SharedPreferences = fetchAppSharedPreferences()
    }

    @Suppress("DEPRECATION")
    private fun fetchAppSharedPreferences(): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(get())
    }
}