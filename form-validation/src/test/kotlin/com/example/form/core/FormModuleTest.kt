package com.example.form.core

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FormModuleTest {

    // --- Test Data Classes (No @GenerateFormMapper needed for library tests) ---
    data class TestState(
        val name: String = "",
        val age: String = "",
        val isMarried: Boolean = false,
        val spouseName: String = "",
        val termsAccepted: Boolean = false,
        val privacyAccepted: Boolean = false,
        val interestSports: Boolean = false,
        val interestMusic: Boolean = false,
        val interestCode: Boolean = false
    )

    data class SpouseData(
        val isMarried: Boolean = false,
        val spouseName: String = ""
    )

    data class SimpleState(
        val firstName: String = "",
        val lastName: String = ""
    )

    data class RangeState(
        val start: Int = 0,
        val end: Int = 0
    )

    // --- Mock Resource IDs ---
    private val ERR_REQUIRED = 1
    private val ERR_MIN_LENGTH = 2
    private val ERR_SPOUSE_REQ = 3
    private val ERR_CONSENTS = 4
    private val ERR_INTERESTS = 5
    private val ERR_ORDER = 6



    @Test
    fun `test basic field validation flow`() = runTest {
        val form = createForm(TestState()) {
            field(TestState::name) {
                required(ERR_REQUIRED)
                minLength(3, ERR_MIN_LENGTH)
            }
            field(TestState::age)
            field(TestState::isMarried)
            field(TestState::spouseName)
            field(TestState::termsAccepted)
            field(TestState::privacyAccepted)
            field(TestState::interestSports)
            field(TestState::interestMusic)
            field(TestState::interestCode)
        }

        val nameField = form[TestState::name]

        assertNull(nameField.error.value)

        assertFalse("Form should be invalid when empty", form.validate())
        assertEquals(ValidationResult.Invalid(ERR_REQUIRED), nameField.error.value)

        nameField.update("Ab")
        assertNull("Update should clear error visually", nameField.error.value)

        assertFalse("Form should be invalid due to min length", form.validate())
        assertEquals(ValidationResult.Invalid(ERR_MIN_LENGTH), nameField.error.value)

        nameField.update("Bob")
        assertTrue("Form should be valid", form.validate())
        assertNull(nameField.error.value)
    }

    @Test
    fun `test activeIf logic (bug fix scenario)`() = runTest {
        val form = createForm(SpouseData()) {
            field(SpouseData::isMarried)
            field(SpouseData::spouseName) {
                val marriedField = getField(SpouseData::isMarried)
                activeIf { marriedField.value.value }
                required(ERR_SPOUSE_REQ)
            }
        }

        val marriedField = form[SpouseData::isMarried]
        val spouseField = form[SpouseData::spouseName]

        assertTrue("Form should be valid when isMarried=false", form.validate())

        marriedField.update(true)
        assertTrue(marriedField.value.value)

        val isValidAfterMarried = form.validate()
        assertFalse("Form should be INVALID when isMarried=true and spouse is empty", isValidAfterMarried)
        assertEquals(ValidationResult.Invalid(ERR_SPOUSE_REQ), spouseField.error.value)

        spouseField.update("Jane")
        assertTrue("Form should be valid with spouse name", form.validate())

        // --- THE BUG REPRODUCTION STEPS ---
        marriedField.update(false)

        // Use valueFor to check the effective value
        val isMarried = form.valueFor(SpouseData::isMarried)
        val spouseName = form.valueFor(SpouseData::spouseName)

        assertEquals("Should be not married", false, isMarried)
        assertEquals("Spouse name should be empty when field is inactive", "", spouseName)
    }

    @Test
    fun `test group validation - all required`() = runTest {
        val form = createForm(TestState()) {
            field(TestState::name)
            field(TestState::age)
            field(TestState::isMarried)
            field(TestState::spouseName)
            field(TestState::termsAccepted)
            field(TestState::privacyAccepted)
            field(TestState::interestSports)
            field(TestState::interestMusic)
            field(TestState::interestCode)

            group("consents", TestState::termsAccepted, TestState::privacyAccepted) { values ->
                if (values.all { it == true }) ValidationResult.Valid
                else ValidationResult.Invalid(ERR_CONSENTS)
            }
        }

        val terms = form[TestState::termsAccepted]
        val privacy = form[TestState::privacyAccepted]
        val group = form.getGroup("consents")

        assertFalse(form.validate())
        assertEquals(ValidationResult.Invalid(ERR_CONSENTS), group.groupError.value)

        terms.update(true)
        assertFalse(form.validate())

        privacy.update(true)
        assertTrue(form.validate())
        assertNull(group.groupError.value)
    }

    @Test
    fun `test group validation - at least selection`() = runTest {
        val form = createForm(TestState()) {
            field(TestState::name)
            field(TestState::age)
            field(TestState::isMarried)
            field(TestState::spouseName)
            field(TestState::termsAccepted)
            field(TestState::privacyAccepted)
            field(TestState::interestSports)
            field(TestState::interestMusic)
            field(TestState::interestCode)

            group(
                "interests",
                TestState::interestSports,
                TestState::interestMusic,
                TestState::interestCode
            ) { values ->
                Validators.atLeastSelection(2, ERR_INTERESTS)(values)
            }
        }

        val sports = form[TestState::interestSports]
        val music = form[TestState::interestMusic]
        val group = form.getGroup("interests")

        assertFalse(form.validate())
        assertEquals(ValidationResult.Invalid(ERR_INTERESTS), group.groupError.value)

        sports.update(true)
        assertFalse(form.validate())

        music.update(true)
        assertTrue(form.validate())
        assertNull(group.groupError.value)
    }

    @Test
    fun `test optional field does not block validation`() = runTest {
        val form = createForm(TestState()) {
            optionalField(TestState::name)
            field(TestState::age)
            field(TestState::isMarried)
            field(TestState::spouseName)
            field(TestState::termsAccepted)
            field(TestState::privacyAccepted)
            field(TestState::interestSports)
            field(TestState::interestMusic)
            field(TestState::interestCode)
        }

        val field = form[TestState::name]

        field.update("")
        assertTrue(form.validate())

        field.update("Some Value")
        assertTrue(form.validate())
    }

    @Test
    fun `test error clearing on update`() = runTest {
        val form = createForm(TestState()) {
            field(TestState::name) { required(ERR_REQUIRED) }
            field(TestState::age)
            field(TestState::isMarried)
            field(TestState::spouseName)
            field(TestState::termsAccepted)
            field(TestState::privacyAccepted)
            field(TestState::interestSports)
            field(TestState::interestMusic)
            field(TestState::interestCode)
        }
        val field = form[TestState::name]

        field.update("")
        form.validate()
        assertTrue(field.error.value is ValidationResult.Invalid)

        field.update("X")
        assertNull(field.error.value)
    }

    @Test
    fun `test data reconstruction using valueFor`() = runTest {
        val form = createForm(SimpleState()) {
            field(SimpleState::firstName)
            field(SimpleState::lastName)
        }

        form[SimpleState::firstName].update("John")
        form[SimpleState::lastName].update("Doe")

        // Use valueFor helper to extract individual values
        val firstName = form.valueFor(SimpleState::firstName)
        val lastName = form.valueFor(SimpleState::lastName)

        assertEquals("John", firstName)
        assertEquals("Doe", lastName)
    }

    @Test
    fun `test explicit reset functionality`() = runTest {
        val form = createForm(SimpleState("Init", "Init")) {
            field(SimpleState::firstName) { required(ERR_REQUIRED) }
            field(SimpleState::lastName)
        }
        val first = form[SimpleState::firstName]

        // Make dirty and invalid
        first.update("")
        form.validate()

        assertTrue(first.error.value != null)
        assertEquals("", first.value.value)

        // RESET
        form.reset()

        // Verify return to initial
        assertEquals("Init", first.value.value)
        assertNull(first.error.value)
    }

    @Test
    fun `test cross field validation rule`() = runTest {
        val form = createForm(RangeState()) {
            field(RangeState::start)
            field(RangeState::end) {
                val startField = getField(RangeState::start)
                custom { endVal ->
                    if (endVal > startField.value.value) ValidationResult.Valid
                    else ValidationResult.Invalid(ERR_ORDER)
                }
            }
        }

        val start = form[RangeState::start]
        val end = form[RangeState::end]

        // Invalid: 5 is not > 10
        start.update(10)
        end.update(5)
        assertFalse(form.validate())
        assertEquals(ValidationResult.Invalid(ERR_ORDER), end.error.value)

        // Valid: 15 > 10
        end.update(15)
        assertTrue(form.validate())
    }

    @Test
    fun `test disabling a field clears its error`() = runTest {
        val form = createForm(SpouseData()) {
            field(SpouseData::isMarried)
            field(SpouseData::spouseName) {
                val married = getField(SpouseData::isMarried)
                activeIf { married.value.value }
                required(ERR_SPOUSE_REQ)
            }
        }

        val married = form[SpouseData::isMarried]
        val spouse = form[SpouseData::spouseName]

        // 1. Enable and trigger error
        married.update(true)
        form.validate()
        assertNotNull("Should have error", spouse.error.value)

        // 2. Disable field (uncheck box)
        married.update(false)

        // 3. Re-validate
        assertTrue(form.validate())

        // 4. Error should be GONE because field is disabled
        assertNull("Error should be cleared when field disabled", spouse.error.value)
    }

    @Test
    fun `test valueFor returns initial value when field is inactive`() = runTest {
        val form = createForm(SpouseData(isMarried = false, spouseName = "InitialValue")) {
            field(SpouseData::isMarried)
            field(SpouseData::spouseName) {
                val married = getField(SpouseData::isMarried)
                activeIf { married.value.value }
                required(ERR_SPOUSE_REQ)
            }
        }

        val married = form[SpouseData::isMarried]
        val spouse = form[SpouseData::spouseName]

        // User enters spouse name
        spouse.update("Jane")
        assertEquals("Jane", spouse.value.value)

        // But then unchecks married
        married.update(false)

        assertTrue(form.validate())

        // valueFor should return initial value for inactive field
        val spouseName = form.valueFor(SpouseData::spouseName)
        assertEquals("InitialValue", spouseName) // Should be initial value, not "Jane"
    }

    @Test
    fun `test all fields can be extracted using valueFor`() = runTest {
        val form = createForm(TestState()) {
            field(TestState::name) { required(ERR_REQUIRED) }
            field(TestState::age)
            field(TestState::isMarried)
            field(TestState::spouseName)
            field(TestState::termsAccepted)
            field(TestState::privacyAccepted)
            field(TestState::interestSports)
            field(TestState::interestMusic)
            field(TestState::interestCode)
        }

        form[TestState::name].update("Test User")
        form[TestState::age].update("30")
        form[TestState::isMarried].update(true)
        form[TestState::spouseName].update("Jane Doe")

        assertTrue(form.validate())

        // Extract using valueFor to verify each field
        val name = form.valueFor(TestState::name)
        val age = form.valueFor(TestState::age)
        val isMarried = form.valueFor(TestState::isMarried)
        val spouseName = form.valueFor(TestState::spouseName)

        assertEquals("Test User", name)
        assertEquals("30", age)
        assertEquals(true, isMarried)
        assertEquals("Jane Doe", spouseName)
    }
}