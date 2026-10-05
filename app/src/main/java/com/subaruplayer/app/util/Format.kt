package com.subaruplayer.app.util

import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** 3:07 ou 1:02:45. */
fun formatTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0) / 1000)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
}

fun formatSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> String.format(Locale.forLanguageTag("pt-BR"), "%.2f GB", bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> String.format(Locale.forLanguageTag("pt-BR"), "%.0f MB", bytes / (1L shl 20).toDouble())
    bytes >= 1L shl 10 -> String.format(Locale.forLanguageTag("pt-BR"), "%.0f KB", bytes / (1L shl 10).toDouble())
    else -> "$bytes B"
}

/** Data curta (dd/MM/aaaa) a partir de segundos desde 1970 (como o MediaStore informa). */
fun formatDate(epochSeconds: Long): String =
    DateFormat.getDateInstance(DateFormat.SHORT, Locale.forLanguageTag("pt-BR")).format(Date(epochSeconds * 1000))

/** "Movies/Subaru DL/Minha Playlist/" vira "Movies › Subaru DL › Minha Playlist". */
fun pathLabel(relPath: String): String =
    relPath.trim('/').split('/').filter { it.isNotEmpty() }.joinToString(" › ").ifEmpty { "Armazenamento interno" }

/**
 * Ordem "natural": 2 vem antes de 10, "Episódio 9" antes de "Episódio 10". É o que mantém os vídeos de uma playlist
 * baixada com numeração (01, 02… ou 1, 2…) na ordem em que ela foi montada.
 */
object NaturalOrder : Comparator<String> {
    override fun compare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val ca = a[i]
            val cb = b[j]
            if (ca.isDigit() && cb.isDigit()) {
                var ie = i
                while (ie < a.length && a[ie].isDigit()) ie++
                var je = j
                while (je < b.length && b[je].isDigit()) je++
                val na = a.substring(i, ie).trimStart('0')
                val nb = b.substring(j, je).trimStart('0')
                if (na.length != nb.length) return na.length - nb.length
                val c = na.compareTo(nb)
                if (c != 0) return c
                i = ie
                j = je
            } else {
                val c = ca.lowercaseChar().compareTo(cb.lowercaseChar())
                if (c != 0) return c
                i++
                j++
            }
        }
        return (a.length - i) - (b.length - j)
    }
}
