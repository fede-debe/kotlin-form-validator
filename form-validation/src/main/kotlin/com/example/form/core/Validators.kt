package com.example.form.core

import android.util.Patterns
import java.time.LocalDate
import java.time.LocalTime

object Validators {

    // --- Core Implementations ---

    fun <T> required(errorResId: Int): Validator<T> {
        return Validator { value ->
            val isValid = when (value) {
                is String -> value.isNotBlank()
                else -> value != null
            }
            if (isValid) ValidationResult.Valid else ValidationResult.Invalid(errorResId)
        }
    }

    fun <T> activeIf(
        condition: () -> Boolean,
        validator: Validator<T>
    ): Validator<T> {
        return Validator { value ->
            if (condition()) validator.validate(value) else ValidationResult.Valid
        }
    }

    fun minLength(length: Int, errorResId: Int): Validator<String> {
        return Validator { value ->
            if (value.length >= length) ValidationResult.Valid else ValidationResult.Invalid(errorResId)
        }
    }

    fun maxLength(length: Int, errorResId: Int): Validator<String> {
        return Validator { value ->
            if (value.length <= length) ValidationResult.Valid else ValidationResult.Invalid(errorResId)
        }
    }

    fun email(errorResId: Int): Validator<String> {
        return Validator { value ->
            if (Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid(errorResId)
            }
        }
    }

    fun matches(regex: Regex, errorResId: Int): Validator<String> {
        return Validator { value ->
            if (regex.matches(value)) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid(errorResId)
            }
        }
    }

    fun isDate(errorResId: Int): Validator<String> {
        return Validator { value ->
            try {
                LocalDate.parse(value)
                ValidationResult.Valid
            } catch (e: Exception) {
                ValidationResult.Invalid(errorResId)
            }
        }
    }

    fun isTime(errorResId: Int): Validator<String> {
        return Validator { value ->
            try {
                LocalTime.parse(value)
                ValidationResult.Valid
            } catch (e: Exception) {
                ValidationResult.Invalid(errorResId)
            }
        }
    }

    fun mustBeTrue(errorResId: Int): Validator<Boolean> {
        return Validator { value ->
            if (value) ValidationResult.Valid else ValidationResult.Invalid(errorResId)
        }
    }

    fun atLeastSelection(minSelection: Int, errorResId: Int): (List<Any?>) -> ValidationResult {
        return { values ->
            // Counts how many items are strictly true (Booleans)
            val count = values.count { (it as? Boolean) == true }
            if (count >= minSelection) ValidationResult.Valid else ValidationResult.Invalid(errorResId)
        }
    }

    fun allSelected(errorResId: Int): (List<Any?>) -> ValidationResult {
        return { values ->
            // Checks if all items are strictly true (Booleans)
            if (values.all { (it as? Boolean) == true }) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid(errorResId)
            }
        }
    }
}

// --- DSL Extension Functions ---
// These allow you to write 'required(id)' inside the validator block.

fun <T> ValidatorScope<T>.required(errorResId: Int) {
    add(Validators.required(errorResId))
}

fun ValidatorScope<String>.minLength(length: Int, errorResId: Int) {
    add(Validators.minLength(length, errorResId))
}

fun ValidatorScope<String>.maxLength(length: Int, errorResId: Int) {
    add(Validators.maxLength(length, errorResId))
}

fun ValidatorScope<String>.email(errorResId: Int) {
    add(Validators.email(errorResId))
}

fun ValidatorScope<String>.matches(regex: Regex, errorResId: Int) {
    add(Validators.matches(regex, errorResId))
}

fun ValidatorScope<String>.isDate(errorResId: Int) {
    add(Validators.isDate(errorResId))
}

fun ValidatorScope<String>.isTime(errorResId: Int) {
    add(Validators.isTime(errorResId))
}

fun ValidatorScope<Boolean>.mustBeTrue(errorResId: Int) {
    add(Validators.mustBeTrue(errorResId))
}

fun <T> ValidatorScope<T>.custom(block: (T) -> ValidationResult) {
    add(Validator(block))
}

fun <T> ValidatorScope<T>.activeIf(condition: () -> Boolean, block: ValidatorScope<T>.() -> Unit) {
    val nestedScope = ValidatorScope<T>().apply(block)
    nestedScope.validators.forEach { validator ->
        add(Validators.activeIf(condition, validator))
    }
}
