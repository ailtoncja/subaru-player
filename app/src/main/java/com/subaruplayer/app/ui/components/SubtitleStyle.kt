package com.subaruplayer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subaruplayer.app.ui.theme.C
import com.subaruplayer.app.ui.theme.T

/** Tamanhos de legenda do libVLC (--freetype-rel-fontsize): número menor = letra maior. */
private val SubSizes = listOf(Seg(20, "Menor"), Seg(18, "Peq."), Seg(16, "Normal"), Seg(13, "Grande"), Seg(10, "Enorme"))

private val SubColors = listOf(0xFFFFFF to "Branca", 0xFACC15 to "Dourada", 0x7BD0FF to "Azul-clara", 0x4ADE80 to "Verde", 0xF9A8D4 to "Rosa")

/** Tamanho, cor e uma prévia da legenda. Usado nos Ajustes e dentro do player. */
@Composable
fun SubtitleStyleControls(size: Int, color: Int, onSize: (Int) -> Unit, onColor: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("Tamanho", style = T.labelMd, modifier = Modifier.padding(bottom = 6.dp))
        Segmented(SubSizes, size, onSize)
        Spacer(Modifier.height(14.dp))
        Text("Cor", style = T.labelMd, modifier = Modifier.padding(bottom = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SubColors.forEach { (rgb, name) ->
                val selected = color == rgb
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onColor(rgb) }) {
                    Spacer(
                        Modifier.size(38.dp).clip(CircleShape).background(Color(0xFF000000 or rgb.toLong()))
                            .border(if (selected) 3.dp else 1.dp, if (selected) C.Gold else C.Outline, CircleShape),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(name, style = T.labelSm, color = if (selected) C.Gold else C.Muted, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Exemplo de legenda",
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Black).padding(vertical = 14.dp),
            style = TextStyle(color = Color(0xFF000000 or color.toLong()), fontSize = (34 - size).coerceAtLeast(14).sp, fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
        )
    }
}
