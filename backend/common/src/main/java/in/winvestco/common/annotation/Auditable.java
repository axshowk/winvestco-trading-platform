package in.winvestco.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking methods that should be logged for audit purposes.
 * These methods will have their calls logged to a separate audit log file.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * The action being performed (e.g., "USER_CREATED", "PASSWORD_CHANGED")
     */
    String action();

    /**
     * Additional context information to include in the audit log
     */
    String context() default "";
}
