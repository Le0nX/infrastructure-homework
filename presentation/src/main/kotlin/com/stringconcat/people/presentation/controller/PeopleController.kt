package com.stringconcat.people.presentation.controller

import com.stringconcat.people.presentation.model.PersonRespectfullViewModel
import com.stringconcat.people.presentation.view.personDetailsForm
import com.stringconcat.people.presentation.view.renderDetailedView
import com.stringconcat.people.useCasePeople.CreateNewPersonUseCase
import com.stringconcat.people.useCasePeople.GetPersonUseCase
import com.stringconcat.people.useCasePeople.MeUseCase
import com.stringconcat.people.useCasePeople.PersonCreationSummary
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import java.net.URI
import java.util.*

@Controller
class PeopleController(
    val getPerson: GetPersonUseCase,
    val createNew: CreateNewPersonUseCase,
    val getMe: MeUseCase,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @RequestMapping(value = ["/me"], method = [RequestMethod.GET])
    @ResponseBody
    fun me(): String {
        return renderDetailedView(person = PersonRespectfullViewModel(getMe()))
    }

    @RequestMapping(value = ["/id/{id}"])
    fun get(@PathVariable id: String): ResponseEntity<String> {
        val idUUD = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            log.error(e.toString())
            return ResponseEntity.badRequest().build()
        }

        return when (val person = getPerson(idUUD)) {
            null -> ResponseEntity.badRequest().build()
            else -> ResponseEntity.ok(
                renderDetailedView(PersonRespectfullViewModel(person))
            )
        }
    }

    @RequestMapping(value = ["/generate"], method = [RequestMethod.GET])
    @ResponseBody
    fun showCreationForm(): String {
        return personDetailsForm()
    }

    @RequestMapping(
            value = ["/generate"],
            method = [RequestMethod.POST],
            consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE]
    )
    @ResponseBody
    fun create(personInput: PersonCreationSummary): ResponseEntity<String> {
        val generatedPerson = createNew(personInput)

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create("/id/${generatedPerson.id}"))
                .build()
    }
}