package br.com.cea.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import kotlin.math.max

class ProfileImageStorage(
    context: Context
) {

    companion object {
        private const val IMAGE_SIZE = 512
        private const val JPEG_QUALITY = 85
        private const val PROFILE_DIRECTORY = "profile"
        private const val TEMP_FILE_PREFIX = "profile_temp_"
        private const val FINAL_FILE_PREFIX = "profile_"
        private const val FILE_EXTENSION = ".jpg"
    }

    /*
     * Usar applicationContext evita que esta classe mantenha uma referência
     * desnecessária a uma Activity ou tela.
     */
    private val appContext = context.applicationContext

    private val profileDirectory: File
        get() = File(appContext.filesDir, PROFILE_DIRECTORY)

    /**
     * Processa e salva uma nova foto de perfil.
     *
     * O arquivo anterior não é excluído aqui. Ele deverá ser removido somente
     * depois que o novo caminho tiver sido salvo com sucesso no SQLite.
     *
     * Este método deverá ser chamado fora da thread principal.
     */
    @Throws(IOException::class)
    fun saveProfileImage(uri: Uri): String {
        validateImageUri(uri)
        ensureProfileDirectoryExists()

        val decodedBitmap = decodeSampledBitmap(uri)
            ?: throw IOException("Não foi possível interpretar a imagem selecionada.")

        var processedBitmap: Bitmap? = null
        var temporaryFile: File? = null

        try {
            processedBitmap = cropAndResize(decodedBitmap)

            temporaryFile = File.createTempFile(
                TEMP_FILE_PREFIX,
                FILE_EXTENSION,
                profileDirectory
            )

            FileOutputStream(temporaryFile).use { outputStream ->
                val compressedSuccessfully = processedBitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    JPEG_QUALITY,
                    outputStream
                )

                if (!compressedSuccessfully) {
                    throw IOException("Não foi possível comprimir a imagem.")
                }

                outputStream.flush()
                outputStream.fd.sync()
            }

            if (!temporaryFile.exists() || temporaryFile.length() == 0L) {
                throw IOException("A imagem processada não pôde ser salva.")
            }

            val finalFile = File(
                profileDirectory,
                "$FINAL_FILE_PREFIX${UUID.randomUUID()}$FILE_EXTENSION"
            )

            if (!temporaryFile.renameTo(finalFile)) {
                temporaryFile.copyTo(
                    target = finalFile,
                    overwrite = false
                )

                if (!temporaryFile.delete()) {
                    temporaryFile.deleteOnExit()
                }
            }

            if (!finalFile.exists() || finalFile.length() == 0L) {
                finalFile.delete()
                throw IOException("A imagem final não pôde ser criada.")
            }

            return finalFile.absolutePath
        } catch (error: SecurityException) {
            temporaryFile?.delete()

            throw IOException(
                "O aplicativo não conseguiu acessar a imagem selecionada.",
                error
            )
        } catch (error: OutOfMemoryError) {
            temporaryFile?.delete()

            throw IOException(
                "A imagem selecionada é grande demais para ser processada.",
                error
            )
        } catch (error: IOException) {
            temporaryFile?.delete()
            throw error
        } catch (error: Exception) {
            temporaryFile?.delete()

            throw IOException(
                "Ocorreu um erro ao processar a imagem.",
                error
            )
        } finally {
            if (
                processedBitmap != null &&
                processedBitmap !== decodedBitmap &&
                !processedBitmap.isRecycled
            ) {
                processedBitmap.recycle()
            }

            if (!decodedBitmap.isRecycled) {
                decodedBitmap.recycle()
            }
        }
    }

    /**
     * Remove uma foto criada pelo próprio aplicativo.
     *
     * Por segurança, o método só exclui arquivos localizados dentro da pasta
     * privada filesDir/profile.
     */
    fun deleteProfileImage(path: String?): Boolean {
        val file = getValidInternalImageFile(path) ?: return false

        return runCatching {
            !file.exists() || file.delete()
        }.getOrDefault(false)
    }

    /**
     * Verifica se o caminho aponta para uma imagem existente dentro da pasta
     * privada de fotos de perfil.
     */
    fun imageExists(path: String?): Boolean {
        val file = getValidInternalImageFile(path) ?: return false

        return file.exists() &&
                file.isFile &&
                file.length() > 0L
    }

    private fun validateImageUri(uri: Uri) {
        val mimeType = appContext.contentResolver.getType(uri)

        /*
         * Alguns provedores não informam o MIME type. Nesse caso, não
         * rejeitamos imediatamente: a validação real ocorrerá ao decodificar.
         */
        if (mimeType != null && !mimeType.startsWith("image/")) {
            throw IOException("O arquivo selecionado não é uma imagem válida.")
        }
    }

    private fun ensureProfileDirectoryExists() {
        if (profileDirectory.exists()) {
            if (!profileDirectory.isDirectory) {
                throw IOException(
                    "Não foi possível acessar a pasta da foto de perfil."
                )
            }

            return
        }

        if (!profileDirectory.mkdirs()) {
            throw IOException(
                "Não foi possível criar a pasta da foto de perfil."
            )
        }
    }

    /**
     * Faz uma primeira leitura somente das dimensões e, depois, abre a imagem
     * novamente usando inSampleSize.
     *
     * Dessa forma, uma foto de 8.000 x 6.000 pixels não é carregada
     * integralmente na memória.
     */
    private fun decodeSampledBitmap(uri: Uri): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(
                inputStream,
                null,
                boundsOptions
            )
        } ?: throw IOException("Não foi possível abrir a imagem selecionada.")

        if (
            boundsOptions.outWidth <= 0 ||
            boundsOptions.outHeight <= 0
        ) {
            throw IOException("O arquivo selecionado não contém uma imagem válida.")
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(
                originalWidth = boundsOptions.outWidth,
                originalHeight = boundsOptions.outHeight,
                requestedSize = IMAGE_SIZE
            )

            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        return appContext.contentResolver
            .openInputStream(uri)
            ?.use { inputStream ->
                BitmapFactory.decodeStream(
                    inputStream,
                    null,
                    decodeOptions
                )
            }
            ?: throw IOException("Não foi possível ler a imagem selecionada.")
    }

    /**
     * Calcula uma amostragem em potências de 2.
     *
     * Exemplo:
     * 8000 x 6000 pode ser decodificada inicialmente perto de 1000 x 750,
     * em vez de ocupar memória como bitmap completo.
     */
    private fun calculateInSampleSize(
        originalWidth: Int,
        originalHeight: Int,
        requestedSize: Int
    ): Int {
        var sampleSize = 1

        val smallestDimension = minOf(
            originalWidth,
            originalHeight
        )

        while (smallestDimension / (sampleSize * 2) >= requestedSize) {
            sampleSize *= 2
        }

        return max(1, sampleSize)
    }

    /**
     * Recorta a área central em formato quadrado e gera o bitmap final
     * com 512 x 512 pixels.
     */
    private fun cropAndResize(source: Bitmap): Bitmap {
        if (source.width <= 0 || source.height <= 0) {
            throw IOException("A imagem possui dimensões inválidas.")
        }

        val cropSize = minOf(
            source.width,
            source.height
        )

        val startX = (source.width - cropSize) / 2
        val startY = (source.height - cropSize) / 2

        val croppedBitmap = Bitmap.createBitmap(
            source,
            startX,
            startY,
            cropSize,
            cropSize
        )

        if (
            croppedBitmap.width == IMAGE_SIZE &&
            croppedBitmap.height == IMAGE_SIZE
        ) {
            return croppedBitmap
        }

        return try {
            Bitmap.createScaledBitmap(
                croppedBitmap,
                IMAGE_SIZE,
                IMAGE_SIZE,
                true
            )
        } finally {
            /*
             * Se createBitmap devolveu o próprio source, não podemos reciclá-lo
             * aqui, pois ele ainda será descartado pelo bloco finally externo.
             */
            if (
                croppedBitmap !== source &&
                !croppedBitmap.isRecycled
            ) {
                croppedBitmap.recycle()
            }
        }
    }

    /**
     * Impede que um caminho arbitrário seja excluído.
     *
     * Somente arquivos cujo caminho canônico esteja dentro de
     * filesDir/profile são considerados válidos.
     */
    private fun getValidInternalImageFile(path: String?): File? {
        if (path.isNullOrBlank()) return null

        return runCatching {
            val directory = profileDirectory.canonicalFile
            val candidate = File(path).canonicalFile

            val isInsideProfileDirectory =
                candidate.parentFile == directory

            if (isInsideProfileDirectory) {
                candidate
            } else {
                null
            }
        }.getOrNull()
    }
}