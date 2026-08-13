package dev.pubfleet.controlplane.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two boundaries that a reviewer cannot hold by reading diffs.
 *
 * <p>Both fail the build rather than a code review, because both erode one convenient
 * shortcut at a time.
 */
class ArchitectureTest {

    private static JavaClasses controlPlane;
    private static JavaClasses contracts;

    @BeforeAll
    static void importClasses() {
        var importer = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS);

        controlPlane = importer.importPackages("dev.pubfleet.controlplane");

        // contracts arrives as a jar on the test classpath, so that importer must keep
        // jars. A rule that checks nothing passes, so the count is asserted below.
        contracts = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("dev.pubfleet.contracts");
    }

    @Test
    void theImportFoundSomethingToCheck() {
        assertThat(controlPlane).as("control plane classes").isNotEmpty();
        assertThat(contracts).as("contract classes").isNotEmpty();
        assertThat(controlPlane.stream().filter(ArchitectureTest::isController).count())
                .as("controllers to check").isPositive();
    }

    /**
     * A JPA entity in a controller signature makes the database schema the API. A
     * column rename then becomes a breaking change for the console, and a lazy
     * association turns into a serialization error at response time.
     */
    @Test
    void controllersNeverExposeJpaEntities() {
        ArchRule rule = noMethods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .or().areDeclaredInClassesThat().areAnnotatedWith(Controller.class)
                .should(haveAJpaEntityInTheirSignature())
                .because("the HTTP surface is libs/contracts, not the database schema");

        rule.check(controlPlane);
    }

    /**
     * The contracts module is shared by two deployables and is meant to stay a set of
     * wire shapes. One Spring import there drags a framework into every consumer and
     * turns the shared module into a shared runtime.
     */
    @Test
    void contractsDependOnNoSpringClass() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                .because("libs/contracts holds wire shapes only, with no framework behind them");

        rule.check(contracts);
    }

    private static boolean isController(JavaClass type) {
        return type.isAnnotatedWith(RestController.class) || type.isAnnotatedWith(Controller.class);
    }

    private static ArchCondition<JavaMethod> haveAJpaEntityInTheirSignature() {
        return new ArchCondition<>("have a JPA entity in their signature") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                Set<JavaClass> entities = signatureTypes(method).stream()
                        .filter(type -> type.isAnnotatedWith(Entity.class))
                        .collect(Collectors.toCollection(LinkedHashSet::new));

                boolean found = !entities.isEmpty();
                String message = found
                        ? method.getFullName() + " exposes " + entities.stream()
                                .map(JavaClass::getName).collect(Collectors.joining(", "))
                        : method.getFullName() + " exposes no JPA entity";

                events.add(new SimpleConditionEvent(method, found, message));
            }
        };
    }

    /**
     * Every raw type in the signature, including the ones nested inside generics, so
     * {@code ResponseEntity<List<JobEntity>>} is caught as well as a bare return type.
     */
    private static Set<JavaClass> signatureTypes(JavaMethod method) {
        Set<JavaClass> types = new LinkedHashSet<>(
                method.getReturnType().getAllInvolvedRawTypes());

        for (JavaType parameter : method.getParameterTypes()) {
            types.addAll(parameter.getAllInvolvedRawTypes());
        }

        return types;
    }
}
