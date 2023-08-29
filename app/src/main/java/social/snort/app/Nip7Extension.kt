package social.snort.app

import android.webkit.JavascriptInterface
import fr.acinq.secp256k1.Secp256k1
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class Nip7Extension {
    private val secp256k1 = Secp256k1.get()
    private val h02 = ByteArray(1) { 0x02 }
    private val random = SecureRandom()

    object PrefKeys {
        const val PrivateKey = "private_key"
    }

    @JavascriptInterface
    fun getPublicKey(): String {
        val key = EncryptedStorage.preferences().getString(PrefKeys.PrivateKey, null)
        if (key != null) {
            return Hex.encode(
                secp256k1.pubKeyCompress(secp256k1.pubkeyCreate(Hex.decode(key))).copyOfRange(1, 33)
            )
        }
        throw error("Missing private key")
    }

    @JavascriptInterface
    fun signEvent(ev: String): String {
        val key = EncryptedStorage.preferences().getString(PrefKeys.PrivateKey, null)
            ?: throw error("Missing private key")

        val json = Json { ignoreUnknownKeys = true }
        val evParsed = json.decodeFromString<Event>(ev)

        val idArray = buildJsonArray {
            add(0)
            add(evParsed.pubKey)
            add(evParsed.createdAt)
            add(evParsed.kind)
            add(buildJsonArray {
                evParsed.tags.forEach { x -> add(buildJsonArray { x.forEach { y -> add(y) } }) }
            })
            add(evParsed.content)
        }.toString()

        val evId = MessageDigest.getInstance("SHA-256").digest(idArray.encodeToByteArray())
        var sig = Hex.encode(secp256k1.signSchnorr(evId, Hex.decode(key), null))

        return json.encodeToString(
            Event(
                Hex.encode(evId),
                evParsed.pubKey,
                evParsed.createdAt,
                evParsed.kind,
                evParsed.tags,
                evParsed.content,
                sig
            )
        )
    }

    @JavascriptInterface
    fun nip04_encrypt(msg: String, toKey: String): String {
        val key = EncryptedStorage.preferences().getString(PrefKeys.PrivateKey, null)
            ?: throw error("Missing private key")
        val sharedSecret = getSharedSecretNIP04(Hex.decode(key), Hex.decode(toKey))
        val iv = ByteArray(16)
        random.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(sharedSecret, "AES"), IvParameterSpec(iv))
        val ivBase64 = Base64.getEncoder().encodeToString(iv)
        val encryptedMsg = cipher.doFinal(msg.toByteArray())
        val encryptedMsgBase64 = Base64.getEncoder().encodeToString(encryptedMsg)
        return "$encryptedMsgBase64?iv=$ivBase64"
    }

    @JavascriptInterface
    fun nip04_decrypt(msg: String, fromKey: String): String {
        val key = EncryptedStorage.preferences().getString(PrefKeys.PrivateKey, null)
            ?: throw error("Missing private key")
        val sharedSecret = getSharedSecretNIP04(Hex.decode(key), Hex.decode(fromKey))

        val msgSplit = msg.split("?iv=")
        val cipherText = Base64.getDecoder().decode(msgSplit[0])
        val iv = Base64.getDecoder().decode(msgSplit[1])

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(sharedSecret, "AES"), IvParameterSpec(iv))
        return String(cipher.doFinal(cipherText))
    }

    @JavascriptInterface
    fun saveKey(key: String) {
        EncryptedStorage.preferences().edit().apply {
            putString(PrefKeys.PrivateKey, key)
        }.commit()
    }

    /**
     * @return 32B shared secret
     */
    private fun getSharedSecretNIP04(privateKey: ByteArray, pubKey: ByteArray): ByteArray {
        return secp256k1.pubKeyTweakMul(h02 + pubKey, privateKey)
            .copyOfRange(1, 33)
    }
}