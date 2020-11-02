package example.micronaut

import com.amazonaws.auth.*
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.profile.path.cred.CredentialsDefaultLocationProvider
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.DeleteObjectsRequest
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.util.IOUtils
import io.findify.s3mock.S3Mock
import io.micronaut.http.MediaType
import org.apache.pdfbox.pdmodel.PDDocument
import org.joda.time.DateTime
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.*
import javax.inject.Singleton

@Singleton
class S3Client internal constructor(private val s3Configuration: S3Configuration) {
    private val s3: AmazonS3

    companion object {
        private const val bucketName = "books"

        private const val s3MockPort = 8001
        private val s3mock = S3Mock.Builder().withPort(s3MockPort).withInMemoryBackend().build()

        /**
         * S3 key used for a book in PDF format.
         */
        private fun bookKey(isbn: String): String {
            return "$isbn.pdf"
        }

        /**
         * S3 key used for a page in the book (using JPEG format).
         */
        private fun pageKey(isbn: String, pageNumber: Int): String {
            return isbn + '_' + pageNumber + ".jpeg"
        }

        init {
            s3mock.start()
        }
    }

    init {
        val endpoint = AwsClientBuilder.EndpointConfiguration(s3Configuration.getEndpoint(), s3Configuration.getRegion())
        val builder = AmazonS3ClientBuilder
                .standard()
                .withPathStyleAccessEnabled(true)
        if (s3Configuration.getEndpoint().isEmpty()) {
            builder.region = s3Configuration.getRegion()
        } else {
            builder.setEndpointConfiguration(endpoint)
        }
        s3 = builder.build()
        s3.createBucket(bucketName)
    }

    /**
     * Generates expiring signed URL for a page.
     */
    fun pageUrl(isbn: String, pageNumber: Int): URL {
        val expiresAt = DateTime.now().plusHours(1).toDate()
        val key = pageKey(isbn, pageNumber)
        return s3.generatePresignedUrl(bucketName, key, expiresAt)
    }

    /**
     * Retrieves a book in PDF format from S3.
     */
    @Throws(IOException::class)
    fun getBook(isbn: String): PDDocument {
        val key = bookKey(isbn)
        val `object` = s3.getObject(bucketName, key)
        val inputStream = `object`.objectContent
        val bytes = IOUtils.toByteArray(inputStream)
        val pdfDocument = PDDocument.load(bytes)
        return pdfDocument
    }

    /**
     * Saves a book in PDF format to S3.
     */
    fun putBook(isbn: String, bytes: ByteArray) {
        val objectMetadata = ObjectMetadata()
        objectMetadata.contentType = "application/pdf"
        objectMetadata.contentLength = bytes.size.toLong()
        val key = bookKey(isbn)
        val inputStream: InputStream = ByteArrayInputStream(bytes)
        s3.putObject(bucketName, key, inputStream, objectMetadata)
    }

    /**
     * Saves a page in JPEG format to S3.
     */
    fun putPage(isbn: String, pageNumber: Int, bytes: ByteArray) {
        val objectMetadata = ObjectMetadata()
        objectMetadata.contentType = MediaType.IMAGE_JPEG
        objectMetadata.contentLength = bytes.size.toLong()
        val key = pageKey(isbn, pageNumber)
        val inputStream: InputStream = ByteArrayInputStream(bytes)
        s3.putObject(bucketName, key, inputStream, objectMetadata)
    }

    /**
     * Delete a book in PDF format from S3.
     */
    fun deleteBook(isbn: String) {
        s3.deleteObject(bucketName, bookKey(isbn))
    }

    /**
     * Delete a list of pages in JPEG format from S3.
     */
    fun deletePages(isbn: String, firstPageNumber: Int, lastPageNumber: Int) {
        val keys: MutableList<KeyVersion> = ArrayList()
        for (pageNumber in firstPageNumber until lastPageNumber) {
            val key = isbn + "_" + pageNumber
            val keyVersion = KeyVersion(key)
            keys.add(keyVersion)
        }
        val request = DeleteObjectsRequest(bucketName)
        request.keys = keys
        s3.deleteObjects(request)
    }
}
