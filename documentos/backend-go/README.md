# CEA Image Backend

Backend Go simples para servir as imagens dos exercícios baixadas em `app/src/main/assets/images`.

## Rodar localmente

```powershell
cd documentos\backend-go
go run .
```

O servidor sobe em `http://localhost:8080`.

Exemplo:

```text
http://localhost:8080/exercise-image/3_4_Sit-Up/0.jpg
```

No emulador Android, o app usa `http://10.0.2.2:8080` para acessar o `localhost` da máquina.

Se quiser apontar para outra pasta de imagens:

```powershell
go run . -images-dir "C:\caminho\para\images"
```
