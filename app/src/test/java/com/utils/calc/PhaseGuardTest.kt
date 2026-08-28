package com.utils.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guarda das regras invioláveis do CLAUDE.md. Se alguém reintroduzir uma
 * permissão de ligação ou mensagem, ou ligar a comunicação externa, a build
 * quebra aqui — antes de chegar no aparelho de alguém.
 */
class PhaseGuardTest {

    private val moduleRoot: File by lazy {
        listOf(File("src/main"), File("app/src/main")).first { it.isDirectory }
    }

    private val manifest: String by lazy {
        File(moduleRoot, "AndroidManifest.xml").readText()
    }

    @Test
    fun `comunicacao externa continua desligada em toda build`() {
        assertFalse(BuildConfig.OUTBOUND_COMMS_ENABLED)
        assertTrue(BuildConfig.DRY_RUN)
    }

    @Test
    fun `manifesto nao declara permissao de ligacao nem de mensagem`() {
        FORBIDDEN_PERMISSIONS.forEach { permission ->
            val declared = Regex(
                """<uses-permission[^>]*android\.permission\.$permission"""",
            ).containsMatchIn(manifest)
            assertFalse("Permissão proibida declarada: $permission", declared)
        }
    }

    @Test
    fun `manifesto remove a permissao de internet`() {
        val block = Regex("""<uses-permission[^>]*android\.permission\.INTERNET"[^>]*/>""")
            .find(manifest)
            ?.value

        assertTrue("A entrada de INTERNET sumiu do manifesto", block != null)
        assertTrue(
            "INTERNET precisa continuar marcada com tools:node=remove",
            block!!.contains("""tools:node="remove""""),
        )
    }

    @Test
    fun `codigo nao usa as apis de ligacao, sms ou rede`() {
        val offenders = File(moduleRoot, "java")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .mapNotNull { file ->
                val text = file.readText()
                val found = FORBIDDEN_APIS.filter { text.contains(it) }
                if (found.isEmpty()) null else "${file.name}: $found"
            }
            .toList()

        assertEquals(emptyList<String>(), offenders)
    }

    /**
     * A regra que permite trocar o fake por algo real um dia sem reescrever o
     * fluxo de emergência: nenhuma feature enxerga a camada de dados, só as
     * interfaces de core:domain.
     */
    @Test
    fun `nenhuma feature importa implementacao da camada de dados`() {
        val offenders = File(moduleRoot, "java/com/utils/calc/feature")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.readText().contains("import com.utils.calc.core.data") }
            .map { it.name }
            .toList()

        assertEquals(emptyList<String>(), offenders)
    }

    private companion object {
        val FORBIDDEN_PERMISSIONS = listOf(
            "CALL_PHONE",
            "SEND_SMS",
            "READ_SMS",
            "PROCESS_OUTGOING_CALLS",
        )

        val FORBIDDEN_APIS = listOf(
            "SmsManager",
            "ACTION_CALL",
            "ACTION_DIAL",
            "placeCall",
            "TelecomManager",
            "HttpURLConnection",
            "OkHttpClient",
            "Retrofit",
            "FirebaseApp",
        )
    }
}
