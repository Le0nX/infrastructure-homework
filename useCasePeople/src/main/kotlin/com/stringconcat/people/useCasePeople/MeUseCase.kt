package com.stringconcat.people.useCasePeople

import com.stringconcat.people.businessPeople.Person
import java.time.LocalDate
import java.util.*
import javax.inject.Named

@Named
class MeUseCase(
        private val persistPerson: PersistPerson
) {
    companion object {
        private const val MOCK_YEAR_OF_BIRTH = 1987
        private const val MOCK_MONTH_OF_BIRTH = 12
        private const val MOCK_DAY_OF_BIRTH_MONTH = 1
    }

    operator fun invoke(): Person {
        val me = Person(
                id = UUID.fromString("29f4d7e3-fd7c-4664-ad07-763326215ec4"),
                firstName = "Sergey",
                secondName = "Bukharov",
                birthDate = LocalDate.of(MOCK_YEAR_OF_BIRTH, MOCK_MONTH_OF_BIRTH, MOCK_DAY_OF_BIRTH_MONTH),
                sex = Person.Sex.MAN,
                avatartUrl = "https://avatars.dicebear.com/v2/male/my-somffething.svg",
                favoriteQuote = "make the easy things easy, and the hard things possible"
        )
        persistPerson.persist(me)
        return me
    }
}