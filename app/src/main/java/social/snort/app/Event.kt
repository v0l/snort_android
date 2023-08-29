package social.snort.app

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class Event(
    val id: HexKey,
    @SerialName("pubkey")
    val pubKey: HexKey,
    @SerialName("created_at")
    val createdAt: Long,
    val kind: Int,
    val tags: List<List<String>>,
    val content: String,

    val sig: HexKey? = null
)