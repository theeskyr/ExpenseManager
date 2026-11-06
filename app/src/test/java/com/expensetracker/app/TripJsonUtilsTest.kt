package com.expensetracker.app

import com.expensetracker.app.data.model.SplitMember
import com.expensetracker.app.utils.TripJsonUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripJsonUtilsTest {

    @Test
    fun testMembersSerializationAndDeserialization() {
        val originalMembers = listOf(
            SplitMember(id = "m1", name = "Nguyễn Văn A", isCurrentUser = true),
            SplitMember(id = "m2", name = "Trần Thị B", isCurrentUser = false)
        )

        val json = TripJsonUtils.membersToJson(originalMembers)
        assertTrue(json.isNotBlank())
        assertTrue(json.contains("Nguyễn Văn A"))
        assertTrue(json.contains("Trần Thị B"))

        val parsedMembers = TripJsonUtils.jsonToMembers(json)
        assertEquals(2, parsedMembers.size)
        assertEquals("m1", parsedMembers[0].id)
        assertEquals("Nguyễn Văn A", parsedMembers[0].name)
        assertTrue(parsedMembers[0].isCurrentUser)

        assertEquals("m2", parsedMembers[1].id)
        assertEquals("Trần Thị B", parsedMembers[1].name)
        assertEquals(false, parsedMembers[1].isCurrentUser)
    }

    @Test
    fun testEmptyOrInvalidMembersJson() {
        assertEquals(0, TripJsonUtils.jsonToMembers("").size)
        assertEquals(0, TripJsonUtils.jsonToMembers("invalid json").size)
    }

    @Test
    fun testStringListSerializationAndDeserialization() {
        val originalList = listOf("m1", "m2", "m3")
        val json = TripJsonUtils.stringListToJson(originalList)
        val parsedList = TripJsonUtils.jsonToStringList(json)

        assertEquals(3, parsedList.size)
        assertEquals("m1", parsedList[0])
        assertEquals("m2", parsedList[1])
        assertEquals("m3", parsedList[2])
    }

    @Test
    fun testEmptyOrInvalidStringListJson() {
        assertEquals(0, TripJsonUtils.jsonToStringList("").size)
        assertEquals(0, TripJsonUtils.jsonToStringList("invalid json").size)
    }
}
