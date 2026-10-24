# Sonic Player — projeto inicial Android

Projeto nativo Kotlin + Jetpack Compose + Material 3 + AndroidX Media3.

## Requisitos
- Android Studio recente
- JDK 17
- Android SDK 35 instalado
- Acesso à internet na primeira sincronização do Gradle para baixar dependências

## Abrir e executar
1. Extraia o ZIP.
2. Abra a pasta `SonicPlayer` no Android Studio.
3. Aguarde a sincronização do Gradle.
4. Execute em um dispositivo Android ou emulador.
5. Conceda acesso a músicas e áudio quando solicitado.

## Gerar APK de depuração
No Android Studio: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

Ou no terminal, após configurar Gradle/SDK:
`gradle :app:assembleDebug`

APK de depuração esperado: `app/build/outputs/apk/debug/app-debug.apk`.

## Estado desta entrega
Este é um ponto de partida compilável para a base do player: leitura de músicas via MediaStore, reprodução Media3, serviço de mídia e interface inicial. Não representa ainda todos os itens do escopo original. Temas, playlists persistentes, equalizador, editor avançado de cores, testes em aparelhos e assinatura de distribuição precisam ser completados e verificados antes de uma publicação.

## Compilação na nuvem pelo celular (GitHub Actions)
O projeto inclui um workflow em `.github/workflows/android-apk.yml`.
Depois de colocar o conteúdo do projeto em um repositório GitHub:
1. Abra a aba **Actions** do repositório.
2. Se necessário, habilite workflows.
3. Selecione **Build Sonic Player APK** e toque em **Run workflow**.
4. Quando terminar, abra a execução e baixe o artefato **SonicPlayer-debug-apk**.
5. Extraia o ZIP do artefato e instale o APK no Android, autorizando a instalação dessa origem se solicitado.

A compilação na nuvem requer uma conta GitHub. A execução pode falhar se houver erros no código; o projeto ainda não foi compilado nem testado nesta entrega.
