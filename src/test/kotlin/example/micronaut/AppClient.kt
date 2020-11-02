package example.micronaut

import example.micronaut.domain.Book
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.client.annotation.Client
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.security.token.jwt.render.BearerAccessRefreshToken
import java.io.IOException

@Client("/")
interface AppClient {
    @Post("/login")
    fun login(@Body credentials : UsernamePasswordCredentials): BearerAccessRefreshToken

    @Consumes(MediaType.TEXT_PLAIN)
    @Get("/")
    fun home(@Header authorization: String): String

    @Put("/books/{isbn}")
    @Consumes("application/pdf")
    fun createOrReplaceBook(@Header authorization: String, isbn: String, bytes: ByteArray);

    @Get("/books")
    fun getBooks(@Header authorization: String, @QueryValue offset: Int, @QueryValue limit: Int): List<Book?>?;

    @Get("/books/{isbn}/{pageNumber}/url")
    @Produces(MediaType.TEXT_PLAIN)
    fun getPageURL(@Header authorization: String, isbn: String, pageNumber: Int): String;
}
