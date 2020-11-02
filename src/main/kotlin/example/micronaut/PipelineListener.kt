package example.micronaut

import io.micronaut.configuration.kafka.annotation.*
import io.micronaut.messaging.Acknowledgement
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO
import javax.inject.Singleton
import javax.persistence.EntityManager

@Singleton
@KafkaListener(offsetReset = OffsetReset.EARLIEST, offsetStrategy = OffsetStrategy.DISABLED, groupId = "bv")
open class PipelineListener internal constructor(
        private val entityManager: EntityManager,
        private val s3Client: S3Client,
        private val bookRepository: BookRepository) {

    /**
     * This will listen for messages in "books" Kafka topic.
     * <p>
     *     Each message in "books" Kafka topic represents a book that needs processing.
     *     Processing is converting PDF to pages in JPEG format.
     * <p>
     *
     * @isbn                ISBN of the book.
     * @acknowledgement     Used to acknowledge that a message has being successfully received.
     */
    @Topic("books")
    @Throws(IOException::class)
    open fun receive(@KafkaKey isbn: String, body: String, acknowledgement: Acknowledgement) {
        val processed = processPages(isbn)
        if (processed) {
            acknowledgement.ack()
        }
    }

    /**
     * Creates pages in S3 and updates processing status in database.
     *
     * @isbn                ISBN of the book.
     */
    @Throws(IOException::class)
    private fun processPages(isbn: String): Boolean {
        // Retrieve PDF from S3
        val pdfDocument = s3Client.getBook(isbn)

        // Process each page
        val pageCount = pdfDocument.numberOfPages
        var pageNumber = 1
        var processedAllPages = false
        try {
            val pdfRenderer = PDFRenderer(pdfDocument)
            while (pageNumber <= pageCount) {
                savePageToObjectStorage(pdfRenderer, isbn, pageNumber)
                pageNumber++
            }
            processedAllPages = true
        } catch (e: Exception) {
            // Processing failed, so collect garbage
            val lastPageNumberToDelete = pageNumber - 1
            s3Client.deletePages(isbn, 1, lastPageNumberToDelete)
        } finally {
            pdfDocument.close()
        }

        if (processedAllPages) {
            bookRepository.markProcessedBookInDB(isbn)
            s3Client.deleteBook(isbn)
        }

        return processedAllPages
    }

    /**
     * Saves a page in JPEG format to a S3 bucket.
     *
     * @page                Page content.
     * @isbn                ISBN of the book.
     */
    private fun savePageToObjectStorage(pdfRenderer: PDFRenderer, isbn: String, pageNumber: Int) {
        val bufferedImage = pdfRenderer.renderImageWithDPI(pageNumber - 1, 300F, ImageType.ARGB)
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(bufferedImage, "jpg", outputStream)
        val bytes = outputStream.toByteArray()
        s3Client.putPage(isbn, pageNumber, bytes)
    }
}
