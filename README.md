# CEA Android

CEA, ou Calisthenics Exercise Analysis, é um protótipo Android em Kotlin com Jetpack Compose para recomendação, acompanhamento e gamificação de treinos de calistenia.

## O que já existe no projeto

- App Android em Kotlin com `MainActivity` usando Jetpack Compose.
- Visual baseado no protótipo enviado: tema escuro, cards, chips clicáveis, ações em verde e navegação inferior.
- Telas principais: informações pessoais, início, criar treino, meus treinos, explorar treinos, progresso, exercícios, calendário e perfil.
- Persistência local em SQLite por `CeaDatabaseHelper.kt`.
- Serviços de domínio em Kotlin para recomendação de treino, IMC, analytics, JSON/API e lembrete de água.
- Internacionalização básica em `values/strings.xml` e `values-en/strings.xml`.
- Permissões no Manifest para Internet, notificações e alarmes.

## Como abrir

1. Abra a pasta `outputs/CEAAndroid` no Android Studio.
2. Aguarde o Gradle Sync baixar o Android Gradle Plugin, se necessário.
3. Execute o app em um emulador ou aparelho Android.

## Observação

Este é um protótipo implementável para trabalho acadêmico. A integração real com servidor, Firebase Analytics e IA pode ser conectada posteriormente nas classes de serviço já criadas.
