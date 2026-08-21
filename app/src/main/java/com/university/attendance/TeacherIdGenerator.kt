package com.university.attendance

import java.util.UUID

object TeacherIdGenerator {

    fun generate(): String {
        val randomPart = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .uppercase()
            .take(8)

        return "TCH-$randomPart"
    }
}