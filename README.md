# Subaru Player

Player de vídeos para Android com o visual do [Subaru DL](../subaru-dl-mobile) ("Rally Precision Dark": azul de rally,
dourado, Space Grotesk / Manrope). O motor de reprodução é o **libVLC** (o mesmo do VLC, da VideoLAN); a interface é toda
nossa, em Kotlin + Jetpack Compose. Só vídeo, só Android.

## O que faz

**Biblioteca que segue o caminho real das pastas.** O app lê os vídeos pelo MediaStore (que informa a pasta de cada um) e
monta a árvore exatamente como está no aparelho: `Início › Movies › Subaru DL › Minha Playlist`. Os vídeos baixados pelo
Subaru DL (`Filmes/Subaru DL/<playlist>/…`) aparecem sozinhos, na pasta da playlist.

- Ordem **natural** dos nomes: `1, 2, 9, 10` (e não `1, 10, 2, 9`), para playlists baixadas com numeração ficarem na ordem certa.
- Lista ou grade, e ordenar por nome, data, duração ou tamanho.
- **Tocar pasta** e **a fila segue a pasta**: tocar um vídeo coloca na fila os vídeos da mesma pasta, na ordem mostrada;
  ao terminar, o próximo da pasta toca sozinho. Favoritos e playlists são filas próprias.
- Busca por nome ou por pasta (cada resultado mostra onde o vídeo está).
- Miniaturas, barra dourada de quanto já foi assistido e estrela nos favoritos.

**Player estilo YouTube.**

- Toque simples mostra/esconde os controles (somem sozinhos em ~3,5 s; sobra uma linha fina de progresso).
- **Toque duplo** na metade esquerda volta, na direita avança (5, 10, 15 ou 30 s, nos Ajustes). Toques seguidos **somam**
  (20 s, 30 s…), com a meia-lua e o "segundos" do lado tocado.
- **Segurar a tela** toca em 2x (1,5x/2x/3x) só enquanto segura.
- **Deslizar para cima/baixo** entra/sai da tela cheia. **Pinça** preenche a tela ou ajusta ao tamanho.
- Sem gestos de brilho e volume, de propósito: o volume fica nos botões do celular.
- Barra de progresso com tempo de destino ao arrastar e cortes nos capítulos.
- Anterior/próximo, velocidade (0,25x a 3x), faixa de áudio e atraso do áudio, legendas (faixa, atraso, tamanho, cor e
  carregar `.srt/.ass/.vtt`), capítulos, marcadores por vídeo, zoom, fila ("A seguir"), favoritos e playlists.
- **Retomar de onde parou**, histórico com o caminho completo de cada vídeo, **picture-in-picture** (com anterior/play/próximo
  na janelinha) e decodificação Automática/Hardware/Software.
- **Som do Boxer** (o mesmo do Subaru DL) ao abrir o app; dá para desligar nos Ajustes.
- "Abrir com Subaru Player" a partir de qualquer gerenciador de arquivos.

## Telas

Biblioteca · Playlists (com Favoritos) · Histórico · Ajustes. O player abre por cima de tudo.

## Como funciona por dentro

```
app/src/main/java/com/subaruplayer/app/
  MainActivity.kt        PiP, "Abrir com", som de abertura, permissão
  AppViewModel.kt        biblioteca, navegação por pastas, fila, retomada
  data/                  modelos, MediaStore, configurações, favoritos/histórico/playlists (JSON)
  player/PlayerEngine    o libVLC: estado, trilhas, capítulos, atrasos, foco de áudio
  ui/library|playlists|history|settings
  ui/player/             PlayerScreen, PlayerGestures (todos os gestos num detector só), SeekBar, menus, fila
  ui/components|theme
app/src/androidTest/     testes dos gestos (Compose), rodam no emulador/celular
```

- Os vídeos são abertos por descritor de arquivo (`content://` do MediaStore). Depois do fim, o arquivo já foi lido até o
  final, então "reiniciar" reabre o vídeo do zero.
- Tamanho e cor da legenda só são lidos na criação do libVLC; ao mudar, o player é recriado e continua do mesmo ponto.
- Na **primeira** abertura, o libVLC monta o cache de fontes (≈12 s no emulador). O app faz isso em segundo plano ao abrir,
  uma única vez, para não travar o primeiro vídeo.
- Quando o Android 14+ oferece "acesso limitado", o app funciona só com os vídeos liberados e mostra um aviso para liberar todos.

## Como compilar e testar (Windows)

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=C:/Users/Administrator/jt -Djdk.net.unixdomain.tmpdir=C:/Users/Administrator/jt"
./gradlew.bat :app:assembleDebug            # APK de teste
./gradlew.bat :app:assembleRelease          # APK assinado (usa keystore.properties + subaru-player.jks)
./gradlew.bat :app:connectedDebugAndroidTest   # testes dos gestos no emulador
emulator -avd Medium_Phone_API_37.0 -no-snapshot-save
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Observações de teste:

- No Git Bash, use `MSYS_NO_PATHCONV=1` com o `adb` (senão `/sdcard/...` vira um caminho do Windows).
- Os testes instrumentados desinstalam o app do aparelho; depois, `adb shell pm grant com.subaruplayer.app android.permission.READ_MEDIA_VIDEO`.
- O decodificador de hardware do **emulador** (`c2.goldfish.h264`) mostra quadros quebrados no primeiro segundo e depois de girar a
  tela; com "Software" nos Ajustes a imagem sai limpa. Em celular de verdade o hardware funciona normalmente.
- Quando o VLC troca de legenda no meio de um trecho (ou você pula para dentro dele), só aparece o próximo trecho. É o
  comportamento do libVLC com legendas em MKV de trechos longos; em vídeos reais os trechos são curtos.

## Licença e nome

O libVLC é LGPL 2.1. O app não usa o nome nem o cone do VLC. O APK de release é assinado com `subaru-player.jks`
(**não vai para o git**; guarde uma cópia, é ela que permite atualizar o app já instalado).
