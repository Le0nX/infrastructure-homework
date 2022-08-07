package com.stringconcat.people.avatarsDiceBear

import com.stringconcat.people.businessPeople.AvatarProvider
import com.stringconcat.people.businessPeople.Person
import javax.inject.Named

@Named
class DiceBearAvatarProvider: AvatarProvider {
    override fun createForPerson(person: Person): String {
        val baseUrl = "https://avatars.dicebear.com/v2"
        val maleOrFemale = if (person.sex == Person.Sex.MAN) "male" else "female"
        val uniqueValue = person.firstName + person.secondName
        return "$baseUrl/$maleOrFemale/$uniqueValue.svg"
    }
}