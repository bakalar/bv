package example.micronaut

import example.micronaut.domain.Book
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.security.annotation.Secured
import java.io.IOException

@Secured("isAuthenticated()")
@ExecuteOn(TaskExecutors.IO)
@Controller("/books")
class BooksController internal constructor(private val bookRepository: BookRepository) {

    @Put("/{isbn}")
    @Consumes("application/pdf")
    @Throws(IOException::class)
    fun createOrReplaceBook(isbn: String, bytes: ByteArray) {
        // This will block until all PDF bytes are received, but that is ok since we need the whole file before further processing
        bookRepository.createOrReplace(isbn, bytes)
    }

    @Get
    fun getBooks(@QueryValue offset: Int, @QueryValue limit: Int): List<Book?>? {
        return bookRepository.find(offset, limit)
    }

    @Get("/{isbn}/{pageNumber}/url")
    @Produces(MediaType.TEXT_PLAIN)
    fun getPageURL(isbn: String, pageNumber: Int): String {
        return bookRepository.pageURL(isbn, pageNumber).toString()
    }
}
