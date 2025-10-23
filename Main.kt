import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class Card(
    val id: String,
    val name: String,
    val rarity: String
)

fun main() {
    // === 7 links configuráveis ===
    val link1 = "https://www.ligapokemon.com.br/?view=cards%2Fsearch&tipo=1&card=ed%3DSV1"
//    val link2 = "https://www.ligapokemon.com.br/?view=cards%2Fsearch&tipo=1&card=ed%3DSV2"
//    val link3 = "https://www.ligapokemon.com.br/?view=cards%2Fsearch&tipo=1&card=ed%3DSV3"
//    val link4 = "https://www.ligapokemon.com.br/?view=cards%2Fsearch&tipo=1&card=ed%3DSV4"
//    val link5 = "https://www.ligapokemon.com.br/?view=cards%2Fsearch&tipo=1&card=ed%3DSV5"
//    val link6 = "https://www.ligapokemon.com.br/?view=cards%2Fsearch&tipo=1&card=ed%3DSV6"
//    val link7 = "https://www.ligapokemon.com.br/?view=cards%2Fsearch&tipo=1&card=ed%3DSV7"

    val links = listOf(link1)

    println("⬇️  Baixando ${links.size} páginas...")

    links.forEachIndexed { i, link ->
        val nomeArquivo = "${i + 1}.html"
        try {
            println("🔹 Baixando $link -> $nomeArquivo")
            downloadComCabecalhos(link, nomeArquivo)
            println("✅ Salvo: $nomeArquivo")
        } catch (e: Exception) {
            println("❌ Erro no link ${i + 1}: ${e.message}")
        }
    }

    println("📖 Lendo arquivos e extraindo dados...")

    val cards = mutableListOf<Card>()

    for (i in 1..7) {
        val file = File("$i.html")
        if (!file.exists()) continue
        val doc = Jsoup.parse(file, "UTF-8")

        val rows = doc.select("tr[id^=item_]")
        for (row in rows) {
            val cols = row.select("td")
            if (cols.size >= 3) {
                val id = cols[0].text().trim()
                val name = cols[1].select("a").text().trim()
                val rarity = cols[2].text().trim()
                cards.add(Card(id, name, rarity))
            }
        }
    }

    val csvFile = File("cartas.csv")
    csvFile.bufferedWriter().use { writer ->
        writer.write("id,name,rarity\n")
        for (c in cards) writer.write("${c.id},\"${c.name}\",${c.rarity}\n")
    }

    println("💾 CSV criado com ${cards.size} cartas: ${csvFile.absolutePath}")
}

/** Faz o download com cabeçalhos padrão de navegador **/
fun downloadComCabecalhos(link: String, destino: String) {
    val url = URL(link)
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
    conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    conn.setRequestProperty("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
    conn.setRequestProperty("Connection", "keep-alive")
    conn.instanceFollowRedirects = true

    if (conn.responseCode != HttpURLConnection.HTTP_OK) {
        throw Exception("HTTP ${conn.responseCode} - ${conn.responseMessage}")
    }

    conn.inputStream.use { input ->
        FileOutputStream(destino).use { output ->
            input.copyTo(output)
        }
    }

    conn.disconnect()
}
