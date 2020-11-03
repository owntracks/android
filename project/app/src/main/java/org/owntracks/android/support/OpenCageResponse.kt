package org.owntracks.android.support

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
internal class OpenCageResponse {
    val results: List<OpenCageResult>? = null
    val formatted: String?
        get() = if (results != null && results.isNotEmpty()) results[0].formatted else null
}