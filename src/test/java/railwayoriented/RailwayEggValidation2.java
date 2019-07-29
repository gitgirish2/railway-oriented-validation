package railwayoriented;

import common.DataSet;
import domain.ImmutableEgg;
import domain.validation.ValidationFailure;
import io.vavr.collection.List;
import io.vavr.control.Validation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static common.DataSet.getExpectedImmutableEggValidationResults;
import static domain.validation.ValidationFailureConstants.VALIDATION_FAILURE_1;
import static domain.validation.ValidationFailureConstants.VALIDATION_FAILURE_2;
import static domain.validation.ValidationFailureConstants.VALIDATION_FAILURE_CHILD_3;
import static domain.validation.ValidationFailureConstants.VALIDATION_FAILURE_PARENT_3;

/**
 * This class contains validations as values.
 */
public class RailwayEggValidation2 {
    public RailwayEggValidation2() {
        super();
    }

    @Test
    void plainOldImperative() {
        var validationList = List.of(validate1, validate2, validateChild3);
        final var eggCarton = DataSet.getImmutableEggCarton();
        var validationResults = new ArrayList<Validation<ValidationFailure, ImmutableEgg>>();
        for (ImmutableEgg egg : eggCarton) {
            var validatedEgg = Validation.<ValidationFailure, ImmutableEgg>valid(egg);
            for (UnaryOperator<Validation<ValidationFailure, ImmutableEgg>> validation : validationList) {
                validatedEgg = validation.apply(validatedEgg);
            }
            validationResults.add(validatedEgg); // mutation
        }
        for (Validation<ValidationFailure, ImmutableEgg> validationResult : validationResults) {
            System.out.println(validationResult);
        }
        Assertions.assertEquals(getExpectedImmutableEggValidationResults().toJavaList(), validationResults);
    }

    @Test
    void railwayCode() {
        final var validationResults = DataSet.getImmutableEggCarton()
                .map(Validation::<ValidationFailure, ImmutableEgg>valid)
                .map(validate1)
                .map(validate2)
                .map(validateChild3);
        validationResults.forEach(System.out::println);
        Assertions.assertEquals(getExpectedImmutableEggValidationResults(), validationResults);
    }

    @Test
    void railwayCodeElegant() {
        var validationList = List.of(validate1, validate2, validateChild3);
        final var validationResults = DataSet.getImmutableEggCarton()
                .map(Validation::<ValidationFailure, ImmutableEgg>valid)
                .map(eggToBeValidated -> validationList
                        .foldLeft(eggToBeValidated, (validatedEgg, currentValidation) -> currentValidation.apply(validatedEgg)));
        validationResults.forEach(System.out::println);
        Assertions.assertEquals(getExpectedImmutableEggValidationResults(), validationResults);
    }

    @Test
    void railwayCodeElegantParallel() {
        var validationList = List.of(validate1, validate2, validateChild3);
        final var immutableEggCarton = DataSet.getImmutableEggCarton();
        var eggStream = immutableEggCarton.size() >= 10000
                ? immutableEggCarton.toJavaStream()
                : immutableEggCarton.toJavaParallelStream();
        final var validationResults = eggStream
                .map(Validation::<ValidationFailure, ImmutableEgg>valid)
                .map(eggToBeValidated -> validationList
                        .foldLeft(eggToBeValidated, (validatedEgg, currentValidation) -> currentValidation.apply(validatedEgg)))
                .collect(Collectors.toList());
        validationResults.forEach(System.out::println);
        Assertions.assertEquals(getExpectedImmutableEggValidationResults().toJavaList(), validationResults);
    }

    private static UnaryOperator<Validation<ValidationFailure, ImmutableEgg>> validate1 = validatedEgg -> validatedEgg
            .filter(Operations::simpleOperation1)
            .getOrElse(() -> Validation.invalid(VALIDATION_FAILURE_1));

    private static UnaryOperator<Validation<ValidationFailure, ImmutableEgg>> validate2 = validatedEgg -> validatedEgg
            .map(Operations::throwableOperation2)
            .flatMap(tryResult -> tryResult.toValidation(cause -> ValidationFailure.withErrorMessage(cause.getMessage())))
            .filter(Boolean::booleanValue)
            .getOrElse(() -> Validation.invalid(VALIDATION_FAILURE_2))
            .flatMap(ignore -> validatedEgg);

    private static UnaryOperator<Validation<ValidationFailure, ImmutableEgg>> validateParent3 = validatedEgg -> validatedEgg
            .map(Operations::throwableOperation3)
            .flatMap(tryResult -> tryResult.toValidation(cause -> ValidationFailure.withErrorMessage(cause.getMessage())))
            .filter(Boolean::booleanValue)
            .getOrElse(() -> Validation.invalid(VALIDATION_FAILURE_PARENT_3))
            .flatMap(ignore -> validatedEgg);

    private static UnaryOperator<Validation<ValidationFailure, ImmutableEgg>> validateChild3 = validatedEgg ->
            validateParent3.apply(validatedEgg)
                    .map(ImmutableEgg::getYolk)
                    .map(Operations::throwableNestedOperation3)
                    .flatMap(tryResult -> tryResult.toValidation(cause -> ValidationFailure.withErrorMessage(cause.getMessage())))
                    .filter(Boolean::booleanValue)
                    .getOrElse(() -> Validation.invalid(VALIDATION_FAILURE_CHILD_3))
                    .flatMap(ignore -> validatedEgg);

}
