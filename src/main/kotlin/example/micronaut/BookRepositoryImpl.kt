package example.micronaut

import example.micronaut.domain.Book
import io.micronaut.transaction.annotation.ReadOnly
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.IOException
import java.net.URL
import javax.inject.Singleton
import javax.persistence.EntityManager
import javax.transaction.Transactional
import javax.validation.constraints.NotBlank

@Singleton
@Transactional
open class BookRepositoryImpl internal constructor(private val entityManager: EntityManager, private val s3Client: S3Client, private val pipelineSender: PipelineSender) : BookRepository {

    @Throws(IOException::class)
    override fun createOrReplace(isbn: @NotBlank String, pdfBytes: ByteArray) {
        validateIsbn(isbn)
        val pageCount = numberOfPages(pdfBytes)

        s3Client.putBook(isbn, pdfBytes)
        saveBookToDB(isbn, pageCount)
        pipelineSender.sendBook(isbn, "")
        //pipelineListener.receive(isbn, pageCount)
    }

    private fun numberOfPages(pdfBytes: ByteArray): Int {
        val document = PDDocument.load(pdfBytes)
        val pageCount = document.numberOfPages
        document.close();

        return pageCount
    }

    @ReadOnly
    override fun find(offset: Int, limit: Int): List<Book>? {
        val qlString = "SELECT b FROM Book as b ORDER BY b.isbn"
        val query = entityManager.createQuery(qlString, Book::class.java)
        query.firstResult = offset
        query.maxResults = limit
        return query.resultList
    }

    @ReadOnly
    override fun pageURL(isbn: @NotBlank String, pageNumber: Int): URL? {
        validateIsbn(isbn)
        validatePageNumber(pageNumber)

        // Book data is not validated:
        // Since validation of book existence, page count or processing status is not cheap (requires a database query)
        // and if we assume that it is not required to return URLs only for processed books
        // then such validation can be avoided.
        return s3Client.pageUrl(isbn, pageNumber)
    }

    /**
     * Saves book to database.
     *
     * @isbn:       13-digit ISBN of the book.
     * pageCount:   Number of pages in the book.
     */
    @Transactional
    private fun saveBookToDB(isbn: @NotBlank String?, pageCount: Int) {
        var book = entityManager.find(Book::class.java, isbn)
        if (book == null) {
            book = Book(isbn)
            book.pageCount = pageCount
            book.isProcessed = false
            entityManager.persist(book)
        } else {
            book.pageCount = pageCount
            book.isProcessed = false
            entityManager.merge(book)
        }
    }

    /**
     * Set processed flag to true in the database.
     *
     * @isbn                ISBN of the book.
     */
    @Transactional
    override fun markProcessedBookInDB(isbn: String) {
        val book = entityManager.find(Book::class.java, isbn)
        book.isProcessed = true
        entityManager.merge(book)
    }

    companion object {
        /**
         * Validates 13-digit ISBN.
         * <p>
         *     If ISBN is invalid, RuntimeException is thrown.
         * <p>
         *
         * @isbn:   13-digit ISBN of the book.
        */
        private fun validateIsbn(isbn: String) {
            val mustBe13Digits = "ISBN must be 13-digit"
            val invalidChecksum = "ISBN has invalid checksum"
            if (isbn.length != 13) {
                throw RuntimeException(mustBe13Digits)
            }
            var sum = 0
            var multiplier = 1
            var i = 0
            while (i < isbn.length) {
                val ch = isbn[i]
                if (!Character.isDigit(ch)) {
                    throw RuntimeException(mustBe13Digits)
                }
                val digit = Character.getNumericValue(ch)
                sum += multiplier * digit
                i++
                multiplier = if (multiplier == 1) 3 else 1
            }
            if (sum % 10 != 0) {
                throw RuntimeException(invalidChecksum)
            }
        }

        /**
         * Validates page number. Page numbers begin with 1.
         * <p>
         *     If page number is invalid, RuntimeException is thrown.
         * <p>
         *
         * @pageNumber: Page number.
         */
        private fun validatePageNumber(pageNumber: Int) {
            if (pageNumber < 1) {
                throw RuntimeException("Invalid page number")
            }
        }
    }
}
