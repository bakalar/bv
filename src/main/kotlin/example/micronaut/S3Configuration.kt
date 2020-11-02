package example.micronaut

import javax.validation.constraints.NotNull

interface S3Configuration {
    @NotNull fun getEndpoint(): String
    @NotNull fun getRegion(): String
}
