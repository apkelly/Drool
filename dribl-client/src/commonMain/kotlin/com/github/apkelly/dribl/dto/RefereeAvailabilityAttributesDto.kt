package com.github.apkelly.dribl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class RefereeAvailabilityAttributesDto(
    @SerialName("start") val start: JsonPrimitive,
    @SerialName("end") val end: JsonPrimitive,
    @SerialName("club_id") val clubId: JsonPrimitive? = null,
    @SerialName("referee_availability_template_period")
    val templatePeriod: String? = null,
    @SerialName("referee_availability_template_end")
    val templateEnd: JsonPrimitive? = null,
    @SerialName("clubs") val clubs: List<ClubDto> = emptyList(),
)
