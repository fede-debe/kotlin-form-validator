package com.example.form.core

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.reflect.KProperty1

// -----------------------------------------------------------------------------
// 1. Core Interfaces
// -----------------------------------------------------------------------------

/**
 * Represents the outcome of a validation check.
 */
@Stable
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val errorResId: Int, val formatArgs: List<Any> = emptyList()) : ValidationResult
}

/**
 * Functional interface for single-rule validation.
 */
@Stable
fun interface Validator<T> {
    fun validate(value: T): ValidationResult
}

// -----------------------------------------------------------------------------
// 2. State Holders
// -----------------------------------------------------------------------------

/**
 * Manages the state (Value, Error, Dirty) of a single input field.
 *
 * @param initialValue The default value (used for reset and when disabled).
 * @param validators List of rules to check against.
 * @param isEnabledCondition Lambda determining if the field is active. If false, validation is skipped.
 */
@Stable
class FormField<T>(
    val initialValue: T,
    private val validators: List<Validator<T>> = emptyList(),
    private val isEnabledCondition: () -> Boolean = { true }
) {
    private val _value = MutableStateFlow(initialValue)
    val value: StateFlow<T> = _value.asStateFlow()

    private val _error = MutableStateFlow<ValidationResult?>(null)
    val error: StateFlow<ValidationResult?> = _error.asStateFlow()

    // Tracks if the field has been interacted with or validated at least once
    private val _isDirty = MutableStateFlow(false)

    /**
     * Determines if the field is currently active based on the [activeIf] condition defined in the DSL.
     */
    fun isEnabled(): Boolean = isEnabledCondition()

    /**
     * Updates the current value.
     * Clears error immediately if currently invalid to improve UX.
     */
    fun update(newValue: T) {
        _value.update { newValue }
        if (_error.value is ValidationResult.Invalid) {
            _error.update { null }
        }
    }

    /**
     * Runs validation.
     * @return true if valid (or disabled), false if invalid.
     */
    fun validate(): Boolean {
        if (!isEnabled()) {
            _error.update { null }
            return true
        }

        _isDirty.update { true }

        for (validator in validators) {
            val result = validator.validate(_value.value)
            if (result is ValidationResult.Invalid) {
                _error.update { result }
                return false
            }
        }

        _error.update { null }
        return true
    }

    fun reset() {
        _value.update { initialValue }
        _error.update { null }
        _isDirty.update { false }
    }
}

/**
 * Manages the state of a logical group of fields (e.g., "Select at least 2 items").
 *
 * @param validator The logic to validate the aggregate state of the group.
 */
@Stable
class FormGroup(
    private val validator: () -> ValidationResult
) {
    private val _groupError = MutableStateFlow<ValidationResult?>(null)
    val groupError: StateFlow<ValidationResult?> = _groupError.asStateFlow()

    fun validate(): Boolean {
        val result = validator()
        if (result is ValidationResult.Invalid) {
            _groupError.update { result }
            return false
        }
        _groupError.update { null }
        return true
    }

    fun reset() {
        _groupError.update { null }
    }
}

/**
 * The Centralized Form Manager.
 * Acts as the single source of truth for the UI state.
 */
@Stable
class Form<T>(
    val fields: Map<KProperty1<T, *>, FormField<*>>,
    private val groups: Map<String, FormGroup>
) {
    /**
     * Type-safe access to fields using property references.
     * Usage: form[MyState::email]
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <V> get(property: KProperty1<T, V>): FormField<V> {
        return fields[property] as? FormField<V>
            ?: throw IllegalArgumentException("Field '${property.name}' not registered in the Form.")
    }

    /**
     * Access a group by its string key.
     */
    fun getGroup(key: String): FormGroup {
        return groups[key]
            ?: throw IllegalArgumentException("Group '$key' not found.")
    }

    /**
     * Validates all fields and groups.
     * @return true if the entire form is valid.
     */
    fun validate(): Boolean {
        var allValid = true
        // Validate all fields
        fields.values.forEach { if (!it.validate()) allValid = false }
        // Validate all groups
        groups.values.forEach { if (!it.validate()) allValid = false }
        return allValid
    }

    /**
     * Resets the entire form to initial state.
     */
    fun reset() {
        fields.values.forEach { it.reset() }
        groups.values.forEach { it.reset() }
    }
}

// -----------------------------------------------------------------------------
// 3. DSL Construction
// -----------------------------------------------------------------------------

/**
 * Scope for configuring a single field.
 */
class ValidatorScope<T> {
    val validators = mutableListOf<Validator<T>>()
    var enabledCondition: () -> Boolean = { true }

    /**
     * Define a dynamic condition for this field.
     * If false, the field is disabled, validation is skipped, and the generated getData returns the initial value.
     */
    fun activeIf(condition: () -> Boolean) {
        enabledCondition = condition
    }

    fun add(validator: Validator<T>) {
        validators.add(validator)
    }

    /**
     * Inline custom validation rule.
     */
    fun custom(block: (T) -> ValidationResult) {
        add { block(it) }
    }
}

/**
 * Builder class used by [createForm].
 */
class FormBuilder<T>(private val initialData: T) {
    private val fields = mutableMapOf<KProperty1<T, *>, FormField<*>>()
    private val groups = mutableMapOf<String, FormGroup>()

    fun <V> field(
        property: KProperty1<T, V>,
        block: ValidatorScope<V>.() -> Unit = {}
    ) {
        val initialValue = property.get(initialData)
        val scope = ValidatorScope<V>().apply(block)
        fields[property] = FormField(initialValue, scope.validators, scope.enabledCondition)
    }

    // Syntactic sugar for optional fields
    fun <V> optionalField(
        property: KProperty1<T, V>,
        block: ValidatorScope<V>.() -> Unit = {}
    ) {
        field(property, block)
    }

    fun group(
        key: String,
        vararg properties: KProperty1<T, *>,
        validator: (List<Any?>) -> ValidationResult
    ) {
        // We capture the dependent fields to extract their values at validation time
        val logic = {
            val currentValues = properties.map { prop ->
                val field = fields[prop]
                    ?: throw IllegalStateException("Field '${prop.name}' used in group '$key' must be defined before the group.")
                field.value.value
            }
            validator(currentValues)
        }
        groups[key] = FormGroup(logic)
    }

    /**
     * Retrieves a field strictly for cross-field validation dependencies inside the DSL.
     */
    @Suppress("UNCHECKED_CAST")
    fun <V> getField(property: KProperty1<T, V>): FormField<V> {
        return fields[property] as? FormField<V>
            ?: throw IllegalStateException("Field '${property.name}' not defined yet. Ensure declarative order.")
    }

    fun build() = Form(fields, groups)
}

/**
 * Entry point to create a Form.
 */
fun <T> createForm(
    initialData: T,
    block: FormBuilder<T>.() -> Unit
): Form<T> {
    val builder = FormBuilder(initialData)
    builder.block()
    return builder.build()
}

// -----------------------------------------------------------------------------
// 4. Data Extraction Extensions
// -----------------------------------------------------------------------------

/**
 * Helper to retrieve the effective value of a field (User Value vs Initial Value).
 */
fun <T, V> Form<T>.valueFor(property: KProperty1<T, V>): V {
    val field = this[property]
    return if (field.isEnabled()) field.value.value else field.initialValue
}
