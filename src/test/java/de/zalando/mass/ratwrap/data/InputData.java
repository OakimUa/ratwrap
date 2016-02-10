package de.zalando.mass.ratwrap.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.io.Serializable;
import java.util.Objects;

public class InputData implements Serializable {
    private final String field1;
    private final Integer field2;

    @JsonCreator
    public InputData(
            @JsonProperty("field1") String field1,
            @JsonProperty("field2") Integer field2) {
        this.field1 = field1;
        this.field2 = field2;
    }

    public String getField1() {
        return field1;
    }

    public Integer getField2() {
        return field2;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("field1", field1)
                .add("field2", field2)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InputData)) return false;
        InputData inputData = (InputData) o;
        return Objects.equals(field1, inputData.field1) &&
                Objects.equals(field2, inputData.field2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field1, field2);
    }
}
