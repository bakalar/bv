package example.micronaut

import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.SignedJWT
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.security.token.jwt.render.BearerAccessRefreshToken
import io.micronaut.test.annotation.MicronautTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import javax.inject.Inject

@MicronautTest
class GetBooksTest {

    @Inject
    lateinit var appClient : AppClient // <1>

    @Test
    fun verifyGetBooksResponse() {

        val creds: UsernamePasswordCredentials = UsernamePasswordCredentials("sherlock", "password")
        val loginRsp : BearerAccessRefreshToken = appClient.login(creds) // <2>

        Assertions.assertNotNull(loginRsp)
        Assertions.assertNotNull(loginRsp.accessToken)
        Assertions.assertTrue(JWTParser.parse(loginRsp.accessToken) is SignedJWT)

        val msg = appClient.getBooks(
                "Bearer ${loginRsp.accessToken}",
                0,
                1) // <3>

        Assertions.assertNotNull(msg);
        if (msg != null) {
            Assertions.assertEquals(0, msg.size)
        };
    }
}
