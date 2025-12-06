--- 
title: Form Validation Module
description: Declarative, type-safe form validation library for Android with Jetpack Compose
version: 2.0.0
author: Form Module Team
tags: [android, kotlin, compose, form-validation, dsl, ksp]
---

# Form Validation Module

A declarative, type-safe form validation library for Android with Jetpack Compose. This module provides centralized form state management with reactive validation, conditional field logic, and compile-time data reconstruction.

## Key Features

- Type-safe DSL for form definition using Kotlin property references
- **Compile-time safety** with KSP-based code generation (no reflection)
- Reactive validation with `StateFlow` for easy Compose integration
- Conditional field visibility with `activeIf` lambda conditions
- Group validation for collective constraints (e.g., "select at least 2 items")
- An extensive library of built-in, reusable validators
- Compose-ready with minimal boilerplate

## Core Architecture

### Data-Driven Design
Forms are defined using Kotlin data classes. Annotate your state class with `@GenerateFormMapper` to enable code generation.

```kotlin
@GenerateFormMapper
data class RegistrationState(
    val fullName: String = "",
    val email: String = "",
    val isMarried: Boolean = false,
    val spouseName: String = ""
)
```

### Reactive State Management
- `FormField<T>` exposes `StateFlow<T>` for values and `StateFlow<ValidationResult?>` for errors.
- UI components collect state with `collectAsState()` for automatic recomposition.

### DSL-Based Configuration
Forms are created declaratively using the `createForm()` builder function:

```kotlin
val form = createForm(RegistrationState()) {
    field(RegistrationState::fullName) {
        required(R.string.error_required)
        minLength(2, R.string.error_min_length)
    }
    field(RegistrationState::email) {
        required(R.string.error_required)
        email(R.string.error_invalid_email)
    }
}
```

## Rules

### Field Definition Order

**CRITICAL:** Fields must be defined before they are referenced by other fields or groups.

**Pattern:**
```kotlin
val form = createForm(DataClass()) {
    // 1. Define independent fields first
    field(DataClass::basicProperty) { /* validators */ }
    
    // 2. Define toggle/switch fields for conditional logic
    field(DataClass::isFeatureEnabled)
    
    // 3. Define dependent fields that reference earlier fields
    field(DataClass::dependentProperty) {
        val toggleField = getField(DataClass::isFeatureEnabled)
        activeIf { toggleField.value.value } // Condition based on toggle
        required(R.string.error_required)
    }
    
    // 4. Define groups last
    group("groupName", DataClass::field1, DataClass::field2, validator = someGroupValidator)
}
```
**Rationale:** `getField()` and `group()` look up previously registered fields. Forward references will throw a build-time or runtime exception.

---

### Conditional Field Logic

Use `activeIf { }` to make a field's validation rules active only when a certain condition is met.

**Behavior:**
- When `activeIf` returns `false`, the field's validators are skipped, errors are cleared, and the generated `getDataAs...()` function returns the field's **initial value**.
- When `activeIf` returns `true`, the field is enabled and validates normally.

**Pattern:**
```kotlin
field(RegistrationState::spouseName) {
    val marriedField = getField(RegistrationState::isMarried)
    
    // This entire validation block is only active when the condition is true
    activeIf({ marriedField.value.value }) {
        required(R.string.error_spouse_required)
    }
}
```

**Data Extraction Behavior:**
```kotlin
// When isMarried = false:
form.getDataAsRegistrationState().spouseName // Returns "" (initial value)

// When isMarried = true and user entered "Jane":
form.getDataAsRegistrationState().spouseName // Returns "Jane"
```

---

### Built-in Validators

The library provides a rich set of reusable validators.

**Field-Level (inside `field { }` blocks):**
```kotlin
// General
required(errorResId)          // Non-blank strings, non-null values
mustBeTrue(errorResId)         // Boolean must be true

// String
minLength(length, errorResId) // Minimum character length
maxLength(length, errorResId) // Maximum character length
email(errorResId)              // Valid email format
matches(regex, errorResId)     // Matches a regular expression

// Date & Time (for String fields)
isDate(errorResId)             // Valid ISO-8601 date format (e.g., "2024-01-01")
isTime(errorResId)             // Valid ISO-8601 time format (e.g., "14:30:00")

// Custom
custom { value -> ValidationResult } // Inline custom logic
```

**Group-Level (passed to `group()`):**
```kotlin
// Function type: (List<Any?>) -> ValidationResult

// Ensures at least a minimum number of Boolean fields are true
Validators.atLeastSelection(minCount, errorResId)

// Ensures all Boolean fields in the group are true
Validators.allSelected(errorResId)
```

---

### Data Extraction (KSP Code Generation)

Data is extracted in a type-safe manner without reflection by using a KSP-generated extension function.

**Setup:**
1. Annotate your state data class with `@GenerateFormMapper`.
2. Build the project to trigger KSP code generation.

**Usage:**
```kotlin
@GenerateFormMapper
data class RegistrationState()

// In ViewModel:
fun submit() {
    if (form.validate()) {
        // Calls the generated extension function
        val submittedData = form.getDataAsRegistrationState()
        
        // ... proceed with the type-safe data
    }
}
```

**Rule:** Every property in your data class MUST be registered in the form builder using `field()` or `optionalField()`.
- **Benefit:** If a field is missed, the project will fail to build with an "Unresolved reference" error on the constructor parameter in the generated file, preventing runtime crashes.

---

### ViewModel Integration

**Standard Pattern:**
```kotlin
@HiltViewModel
class RegistrationViewModel @Inject constructor() : ViewModel() {
    
    private val initialState = RegistrationState()
    
    val form = createForm(initialState) {
        field(RegistrationState::fullName) {
            required(R.string.error_required)
        }
        field(RegistrationState::email) {
            required(R.string.error_required)
            email(R.string.error_invalid_email)
        }
        // ... other fields
    }
    
    fun submit() {
        if (form.validate()) {
            val data = form.getDataAsRegistrationState() // Generated function
            viewModelScope.launch {
                // ...
            }
        } else {
            // Errors are automatically shown in the UI
        }
    }
}
```
