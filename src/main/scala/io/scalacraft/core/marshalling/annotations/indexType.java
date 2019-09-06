package io.scalacraft.core.marshalling.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Specify the index type of a field of an entity.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface indexType {
    int index();
}
