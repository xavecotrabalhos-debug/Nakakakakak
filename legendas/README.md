# Legendas Dinâmicas

Aplicativo Android para gerar legendas palavra por palavra em português e exportar o vídeo com as legendas aplicadas.

## Como gerar o APK pelo GitHub

1. Abra a aba **Actions** deste repositório.
2. Selecione **Build APK - Legendas Dinâmicas**.
3. Aguarde a execução terminar com sucesso.
4. Abra a execução concluída e baixe o artefato **LegendasDinamicas-apk**.
5. Extraia o ZIP e instale o arquivo `app-debug.apk` no Android.

O workflow baixa o modelo compacto de português do Vosk durante a compilação e o inclui nos assets do aplicativo. Assim, o reconhecimento de voz é executado localmente no telefone, sem enviar o áudio para um serviço externo. A primeira compilação pode demorar mais por causa do download do modelo.

## Requisitos

- Android 10 (API 29) ou superior.
- Espaço livre para instalar o aplicativo e armazenar temporariamente o modelo de reconhecimento.
