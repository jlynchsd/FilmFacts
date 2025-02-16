package com.movietrivia.filmfacts.model

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import java.util.*
import javax.inject.Inject
import kotlin.collections.LinkedHashMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class RecentElementsDataSource @Inject constructor(
    private val context: Context,
    elementsName: String,
    private val cacheSize: Int = CACHE_SIZE
) {
    private val elementsKey = stringPreferencesKey(elementsName)

    private val elements = Collections.synchronizedMap(object : LinkedHashMap<Int, Boolean>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Boolean>?): Boolean {
            return size > cacheSize
        }
    })

    suspend fun loadElements() {
        context.dataStore.data.firstOrNull()?.let { preferences ->
            preferences[elementsKey]?.let { serialized ->
                ElementSerializer.deserialize(serialized).elementIds.forEach {
                    addElement(it)
                }
            }
        }
    }

    suspend fun saveElements() {
        context.dataStore.edit {
            it[elementsKey] = ElementSerializer(elements.keys.toList()).serialize()
        }
    }

    suspend fun resetElements() {
        context.dataStore.edit {
            it.remove(elementsKey)
        }
    }

    fun isRecentElement(elementId: Int): Boolean {
        return elements.contains(elementId)
    }

    fun addElement(elementId: Int) {
        elements[elementId] = true
    }

    fun resetRecentElements() {
        elements.clear()
    }

    private companion object {
        const val CACHE_SIZE = 40
        val Context.dataStore by preferencesDataStore(name = "recent_elements")
    }
}

@Serializable
private data class ElementSerializer(
    val elementIds: List<Int>
) {
    fun serialize() = kotlinx.serialization.json.Json.encodeToString(this)

    companion object {
        fun deserialize(serialized: String) = kotlinx.serialization.json.Json.decodeFromString<ElementSerializer>(serialized)
    }
}