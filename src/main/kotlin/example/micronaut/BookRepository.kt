package example.micronaut

import example.micronaut.domain.Book
import java.io.IOException
import java.net.URL
import javax.validation.constraints.NotBlank

interface BookRepository {

    /**
     * Add new book or replace existing book that has same ISBN.
     *
     * @isbn        13-digit ISBN of the book.
     * @pdfBytes    Book content in PDF format.
     */
    @Throws(IOException::class)
    fun createOrReplace(isbn: @NotBlank String, pdfBytes: ByteArray)

    /**
     * Return list of all uploaded books.
     * <p>
     *     For each book it returns ISBN, number of pages and processing status.
     * <p>
     *
     * @offset  Books returned start from this offset.
     * @limit   Maximum number of books returned.
     * @return  List of uploaded books.
     */
    fun find(offset: Int, limit: Int): List<Book>?

    /**
     * Return expirable signed URL for a page in the uploaded book.
     * <p>
     *     URL will direct to a page in JPEG format.
     * <p>
     *
     * @isbn        13-digit ISBN of the book.
     * @pageNumber  Page number (starting from 1) in the book.
     * @return      Expirable signed URL for a page.
     */
    fun pageURL(isbn: @NotBlank String, pageNumber: Int): URL?

    /**
     * Set processed flag to true in the database.
     *
     * @isbn                ISBN of the book.
     */
    fun markProcessedBookInDB(isbn: String);
}
