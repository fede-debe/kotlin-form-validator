# Kotlin Form Validator

A declarative, type-safe form validation library for Android with Jetpack Compose.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Features

- 🔒 **Type-safe DSL** using Kotlin property references
- ⚡ **Compile-time safety** with KSP-based code generation
- 🔄 **Reactive validation** with StateFlow for Compose integration
- 🎯 **Conditional fields** with `activeIf` lambda conditions
- 👥 **Group validation** for collective constraints
- 📚 **Extensive built-in validators**
- 🎨 **Compose-ready** with minimal boilerplate

## Installation

### Step 1: Add GitHub Packages Repository

In your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/fede-debe/kotlin-form-validator")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GPR_USER")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GPR_KEY")
            }
        }
    }
}
```

### Step 2: Add Credentials

In your `gradle.properties`:

```properties
gpr.user=your-github-username
gpr.key=ghp_your_personal_access_token
```

### Step 3: Add Dependencies

In your `libs.versions.toml`:

```toml
[versions]
formValidator = "1.0.0"
ksp = "2.0.21-1.0.20"

[libraries]
form-validation = { group = "com.github.fede-debe", name = "form-validation", version.ref = "formValidator" }
form-codegen = { group = "com.github.fede-debe", name = "form-codegen", version.ref = "formValidator" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

In your app's `build.gradle.kts`:

```kotlin
plugins {
    // ... other plugins
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.form.validation)
    ksp(libs.form.codegen)
}
```

## Quick Start

### 1. Define Your State

```kotlin
@GenerateFormMapper
data class RegistrationState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val isMarried: Boolean = false,
    val spouseName: String = ""
)
```

### 2. Create a Form

```kotlin
class RegistrationViewModel : ViewModel() {
    val form = createForm(RegistrationState()) {
        field(RegistrationState::fullName) {
            required(R.string.error_required)
            minLength(2, R.string.error_min_length)
        }
        
        field(RegistrationState::email) {
            required(R.string.error_required)
            email(R.string.error_invalid_email)
        }
        
        field(RegistrationState::password) {
            required(R.string.error_required)
            minLength(8, R.string.error_password_length)
        }
        
        field(RegistrationState::isMarried)
        
        field(RegistrationState::spouseName) {
            val marriedField = getField(RegistrationState::isMarried)
            activeIf { marriedField.value.value }
            required(R.string.error_spouse_required)
        }
    }
    
    fun submit() {
        if (form.validate()) {
            val data = form.getDataAsRegistrationState()
            // Submit data...
        }
    }
}
```

### 3. Use in Compose UI

```kotlin
@Composable
fun RegistrationScreen(viewModel: RegistrationViewModel) {
    val form = viewModel.form
    val fullNameField = form[RegistrationState::fullName]
    val fullName by fullNameField.value.collectAsState()
    val fullNameError by fullNameField.error.collectAsState()
    
    Column {
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullNameField.update(it) },
            label = { Text("Full Name") },
            isError = fullNameError != null,
            supportingText = {
                fullNameError?.let { error ->
                    Text(stringResource(error.errorResId))
                }
            }
        )
        
        Button(onClick = { viewModel.submit() }) {
            Text("Register")
        }
    }
}
```

## Built-in Validators

### Field-Level

- `required(errorResId)` - Non-blank strings, non-null values
- `minLength(length, errorResId)` - Minimum character length
- `maxLength(length, errorResId)` - Maximum character length
- `email(errorResId)` - Valid email format
- `matches(regex, errorResId)` - Matches regular expression
- `isDate(errorResId)` - Valid ISO-8601 date
- `isTime(errorResId)` - Valid ISO-8601 time
- `mustBeTrue(errorResId)` - Boolean must be true
- `custom { value -> ValidationResult }` - Custom validation

### Group-Level

```kotlin
group("interests", 
    State::interest1, 
    State::interest2,
    validator = Validators.atLeastSelection(2, R.string.error_select_at_least_two)
)
```

## Advanced Features

### Conditional Fields

```kotlin
field(State::dependentField) {
    val toggleField = getField(State::toggle)
    activeIf { toggleField.value.value }
    required(R.string.error_required)
}
```

### Custom Validators

```kotlin
field(State::age) {
    custom { value ->
        if (value.toIntOrNull() ?: 0 >= 18) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(R.string.error_must_be_adult)
        }
    }
}
```

### Cross-Field Validation

```kotlin
field(State::endDate) {
    val startField = getField(State::startDate)
    custom { endDate ->
        if (endDate > startField.value.value) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(R.string.error_end_before_start)
        }
    }
}
```

## Documentation

For complete documentation, see [AGENTS.md](./AGENTS.md)

## License

```
Copyright 2024 fede-debe

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.