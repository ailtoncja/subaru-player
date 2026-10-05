package com.subaruplayer.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subaruplayer.app.ui.theme.C
import com.subaruplayer.app.ui.theme.T

private val Stadium = RoundedCornerShape(50)

// ---------- Cartões e rótulos ----------

/** Cartão tonal do Material 3. Com [accent], ganha uma faixa de cor na lateral. */
@Composable
fun SCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = C.Card)
    val border = BorderStroke(1.dp, C.Border)
    val inner: @Composable () -> Unit = {
        Box {
            Column(Modifier.padding(start = if (accent != null) padding + 4.dp else padding, end = padding, top = padding, bottom = padding), content = content)
            if (accent != null) Box(Modifier.align(Alignment.CenterStart).width(4.dp).fillMaxHeight().background(accent))
        }
    }
    if (onClick != null) Card(onClick, modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = colors, border = border) { inner() }
    else Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = colors, border = border) { inner() }
}

@Composable
fun Mono(text: String, modifier: Modifier = Modifier, color: Color = C.Muted, style: TextStyle = T.labelSm, maxLines: Int = 1) {
    Text(text, modifier, color = color, style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

/** Números que mudam (tempo, velocidade), em fonte de largura fixa. */
@Composable
fun Num(text: String, modifier: Modifier = Modifier, color: Color = C.Text, size: Int = 13, maxLines: Int = 1) {
    Text(text, modifier, color = color, style = T.number.copy(fontSize = size.sp), maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = C.Muted) {
    Text(text.uppercase(), modifier, color = color, style = T.labelMd.copy(letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold))
}

@Composable
fun Badge(text: String, modifier: Modifier = Modifier, color: Color = C.Gold, background: Color = Color(0x26FACC15), border: Color = Color(0x4DFACC15)) {
    Text(
        text,
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(background).border(1.dp, border, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
        color = color, style = T.labelSm.copy(fontWeight = FontWeight.SemiBold), maxLines = 1,
    )
}

/** Selo sobre imagens (fundo escuro translúcido para dar leitura). */
@Composable
fun DarkBadge(text: String, modifier: Modifier = Modifier, color: Color = Color.White) =
    Text(
        text,
        modifier = modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xCC000000)).padding(horizontal = 6.dp, vertical = 2.dp),
        color = color, style = T.number.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), maxLines = 1,
    )

// ---------- Botões ----------

@Composable
private fun BtnContent(text: String, icon: ImageVector?, textStyle: TextStyle = T.button) {
    if (icon != null) {
        Icon(icon, null, Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, style = textStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun BtnPrimary(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, height: Dp = 52.dp) {
    Button(
        onClick, modifier.heightIn(min = height), enabled = enabled, shape = Stadium,
        colors = ButtonDefaults.buttonColors(containerColor = C.Blue, contentColor = Color.White, disabledContainerColor = C.Blue.copy(alpha = 0.3f), disabledContentColor = Color.White.copy(alpha = 0.5f)),
    ) { BtnContent(text, icon) }
}

@Composable
fun BtnGold(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    Button(
        onClick, modifier.heightIn(min = 52.dp), enabled = enabled, shape = Stadium,
        colors = ButtonDefaults.buttonColors(containerColor = C.GoldDeep, contentColor = C.OnGold, disabledContainerColor = C.GoldDeep.copy(alpha = 0.3f), disabledContentColor = C.OnGold.copy(alpha = 0.5f)),
    ) { BtnContent(text, icon) }
}

@Composable
fun BtnGhost(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, color: Color = C.Text, enabled: Boolean = true, height: Dp = 44.dp) {
    FilledTonalButton(
        onClick, modifier.heightIn(min = height), enabled = enabled, shape = Stadium,
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = C.CardHigh, contentColor = color, disabledContainerColor = C.Container, disabledContentColor = C.Dim),
        contentPadding = ButtonDefaults.ContentPadding,
    ) { BtnContent(text, icon, T.labelLg.copy(fontWeight = FontWeight.Bold)) }
}

@Composable
fun IconBtn(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = C.Text, background: Color = C.CardHigh, size: Dp = 40.dp, description: String? = null) {
    FilledTonalIconButton(
        onClick, modifier.size(size),
        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = background, contentColor = tint),
    ) { Icon(icon, description, Modifier.size(size * 0.5f)) }
}

// ---------- Controles ----------

@Composable
fun SwitchSubaru(checked: Boolean, onChange: (Boolean) -> Unit, gold: Boolean = false) {
    Switch(
        checked, onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = if (gold) C.OnGold else Color(0xFF1D3FB8), checkedTrackColor = if (gold) C.Gold else C.BluePale,
            uncheckedThumbColor = C.Dim, uncheckedTrackColor = C.CardHighest, uncheckedBorderColor = C.Outline,
        ),
    )
}

data class Seg<T>(val value: T, val label: String)

@Composable
fun <T> Segmented(options: List<Seg<T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { i, o ->
            SegmentedButton(
                selected = o.value == selected, onClick = { onSelect(o.value) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = C.Blue, activeContentColor = Color.White, activeBorderColor = C.Blue,
                    inactiveContainerColor = Color.Transparent, inactiveContentColor = C.Muted, inactiveBorderColor = C.Outline,
                ),
                icon = { Icon(Icons.Filled.Check, null, Modifier.size(0.dp)) },
                contentPadding = PaddingValues(horizontal = 2.dp),
                modifier = Modifier.heightIn(min = 44.dp),
            ) { Text(o.label, style = if (options.size >= 5) T.labelMd.copy(fontWeight = FontWeight.SemiBold, color = Color.Unspecified) else T.labelLg, maxLines = 1, softWrap = false) }
        }
    }
}

@Composable
fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = C.BluePale, unfocusedBorderColor = C.Outline, disabledBorderColor = C.Border,
    focusedContainerColor = C.SurfaceLowest, unfocusedContainerColor = C.SurfaceLowest, disabledContainerColor = C.SurfaceLowest,
    focusedTextColor = C.Text, unfocusedTextColor = C.Text, disabledTextColor = C.Dim, cursorColor = C.Gold,
    focusedLabelColor = C.BluePale, unfocusedLabelColor = C.Muted, focusedLeadingIconColor = C.BluePale, unfocusedLeadingIconColor = C.Muted,
    focusedTrailingIconColor = C.BluePale, unfocusedTrailingIconColor = C.Muted,
    focusedPlaceholderColor = C.Dim, unfocusedPlaceholderColor = C.Dim,
)

@Composable
fun ChipSmall(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected, onClick, label = { Text(text, style = T.labelLg) }, modifier = modifier, shape = RoundedCornerShape(10.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent, labelColor = C.Muted,
            selectedContainerColor = Color(0x33FACC15), selectedLabelColor = C.Gold,
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = C.Outline, selectedBorderColor = Color(0x80FACC15)),
    )
}

// ---------- Diversos ----------

@Composable
fun HRow(modifier: Modifier = Modifier, gap: Dp = 8.dp, content: @Composable RowScope.() -> Unit) =
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically, content = content)

@Composable
fun Divider(modifier: Modifier = Modifier) = HorizontalDivider(modifier, color = C.Border)

@Composable
fun Empty(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, title: String? = null) {
    Column(modifier.fillMaxWidth().padding(vertical = 30.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(C.Container), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(32.dp), tint = C.BluePale)
            }
            Spacer(Modifier.height(14.dp))
        }
        if (title != null) {
            Text(title, style = T.headlineSm, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
        }
        Text(text, style = T.bodyMd, color = C.Muted, textAlign = TextAlign.Center)
    }
}

/** Círculo tonal com ícone, usado nas linhas de pasta e de ajustes. */
@Composable
fun IconTile(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = C.BluePale, size: Dp = 40.dp) {
    Box(modifier.size(size).clip(CircleShape).background(Color(0x1F1D4ED8)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size * 0.55f), tint = tint)
    }
}
