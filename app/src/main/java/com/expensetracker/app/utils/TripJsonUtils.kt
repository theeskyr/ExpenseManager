package com.expensetracker.app.utils

import com.expensetracker.app.data.model.SplitMember
import org.json.JSONArray
import org.json.JSONObject

object TripJsonUtils {

    fun membersToJson(members: List<SplitMember>): String {
        val array = JSONArray()
        for (m in members) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("name", m.name)
            obj.put("isCurrentUser", m.isCurrentUser)
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToMembers(json: String): List<SplitMember> {
        if (json.isBlank()) return emptyList()
        return try {
            val list = mutableListOf<SplitMember>()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SplitMember(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        isCurrentUser = obj.optBoolean("isCurrentUser", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun stringListToJson(list: List<String>): String = JSONArray(list).toString()

    fun jsonToStringList(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val result = mutableListOf<String>()
            for (i in 0 until array.length()) {
                result.add(array.getString(i))
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
}
