/*
 * Spec for api tests.
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

package testmodel.micronaut_java;

import java.util.Objects;
import java.util.Arrays;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.*;

import javax.validation.constraints.*;
import javax.validation.Valid;
import io.micronaut.core.annotation.*;
import javax.annotation.Generated;

/**
 * OneOfImplementor2
 */
@JsonPropertyOrder({
  OneOfImplementor2.JSON_PROPERTY_PROPERTY
})
@JsonTypeName("OneOfImplementor2")
@java.lang.SuppressWarnings("all")
@Generated(value="org.openapitools.codegen.languages.JavaMicronautServerCodegen")
@Introspected
public class OneOfImplementor2 {
    public static final String JSON_PROPERTY_PROPERTY = "property";
    private BigDecimal property;

    public OneOfImplementor2() {
    }

    public OneOfImplementor2 property(BigDecimal property) {
        this.property = property;
        return this;
    }

    /**
     * Get property
     * @return property
     **/
    @Nullable
    @JsonProperty(JSON_PROPERTY_PROPERTY)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public BigDecimal getProperty() {
        return property;
    }

    @JsonProperty(JSON_PROPERTY_PROPERTY)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setProperty(BigDecimal property) {
        this.property = property;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OneOfImplementor2 oneOfImplementor2 = (OneOfImplementor2) o;
        return Objects.equals(this.property, oneOfImplementor2.property);
    }

    @Override
    public int hashCode() {
        return Objects.hash(property);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class OneOfImplementor2 {\n");
        sb.append("    property: ").append(toIndentedString(property)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

}

