package com.stringconcat.people.businessPeople

import java.time.LocalDate
import java.time.Period
import java.util.*

const val DEFAULT_ROBOT_AVATAR = "https://avatars.dicebear.com/v2/bottts/not%20found.svg"

data class Person(
        val id: UUID = UUID.randomUUID(),
        val firstName: String,
        val secondName: String,
        val birthDate: LocalDate,
        val sex: Sex,
        var avatartUrl: String = DEFAULT_ROBOT_AVATAR,
        val favoriteQuote: String
) {

    companion object {
        private const val IMMATURE_MAX = 40
    }

    fun isMature(forDate: LocalDate = LocalDate.now()): Boolean =
            age(forDate) > IMMATURE_MAX

    fun age(forDate: LocalDate = LocalDate.now()): Year =
            Period.between(forDate, birthDate).years

    enum class Sex {
        MAN, WOMAN
    }

    fun changeAvatar(pictureUrl: String) {
        avatartUrl = pictureUrl
    }
}

typealias Year = Int