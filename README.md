# CEA Android

CEA, ou Calisthenics Exercise Analysis, e um prototipo Android em Kotlin com Jetpack Compose para recomendacao, acompanhamento e gamificacao de treinos de calistenia.

## O que ja existe no projeto

- App Android em Kotlin com `MainActivity` usando Jetpack Compose.
- Visual baseado no prototipo enviado: tema escuro, cards, chips clicaveis, acoes em verde e navegacao inferior.
- Telas principais: informacoes pessoais, inicio, criar treino, meus treinos, explorar treinos, progresso, exercicios, calendario e perfil.
- Persistencia local em SQLite por `CeaDatabaseHelper.kt`.
- Servicos de dominio em Kotlin para recomendacao de treino, IMC, analytics, JSON/API e lembrete de agua.
- Internacionalizacao basica em `values/strings.xml` e `values-en/strings.xml`.
- Permissoes no Manifest para Internet, notificacoes e alarmes.

## Como abrir

1. Abra a pasta `outputs/CEAAndroid` no Android Studio.
2. Aguarde o Gradle Sync baixar o Android Gradle Plugin, se necessario.
3. Execute o app em um emulador ou aparelho Android.

## Observacao

Este e um prototipo implementavel para trabalho academico. A integracao real com servidor, Firebase Analytics e IA pode ser conectada posteriormente nas classes de servico ja criadas.
