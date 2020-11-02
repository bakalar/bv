package example.micronaut

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("s3")
class S3ConfigurationProperties : S3Configuration {
    protected val DEFAULT_ENDPOINT = "http://localhost:8001"
    protected val DEFAULT_REGION = "eu-central-1"

    private var endpoint = DEFAULT_ENDPOINT
    private var region = DEFAULT_REGION

    override fun getEndpoint(): String {
        return endpoint
    }

    fun setEndpoint(endpoint: String) {
        this.endpoint = endpoint
    }

    override fun getRegion(): String {
        return region
    }

    fun setRegion(region: String) {
        this.region = region
    }
}
