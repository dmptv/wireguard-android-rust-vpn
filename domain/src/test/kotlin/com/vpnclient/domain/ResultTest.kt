package com.vpnclient.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private enum class TestError : Error {
    BOOM,
}

class ResultTest {

    @Test
    fun `onSuccess runs the action and returns the same result on Success`() {
        var captured: Int? = null
        val result: Result<Int, TestError> = Result.Success(42)

        val returned = result.onSuccess { captured = it }

        assertEquals(42, captured)
        assertEquals(result, returned)
    }

    @Test
    fun `onSuccess does not run the action on Error`() {
        var called = false
        val result: Result<Int, TestError> = Result.Error(TestError.BOOM)

        result.onSuccess { called = true }

        assertFalse(called)
    }

    @Test
    fun `onError runs the action and returns the same result on Error`() {
        var captured: TestError? = null
        val result: Result<Int, TestError> = Result.Error(TestError.BOOM)

        val returned = result.onError { captured = it }

        assertEquals(TestError.BOOM, captured)
        assertEquals(result, returned)
    }

    @Test
    fun `onError does not run the action on Success`() {
        var called = false
        val result: Result<Int, TestError> = Result.Success(1)

        result.onError { called = true }

        assertFalse(called)
    }

    @Test
    fun `onSuccess and onError chain and only the matching branch runs`() {
        var successRan = false
        var errorRan = false
        val result: Result<Int, TestError> = Result.Success(5)

        result.onSuccess { successRan = true }.onError { errorRan = true }

        assertTrue(successRan)
        assertFalse(errorRan)
    }

    @Test
    fun `map transforms the success value`() {
        val result: Result<Int, TestError> = Result.Success(2)

        val mapped = result.map { it * 10 }

        assertEquals(Result.Success(20), mapped)
    }

    @Test
    fun `map propagates the error unchanged`() {
        val result: Result<Int, TestError> = Result.Error(TestError.BOOM)

        val mapped = result.map { it * 10 }

        assertEquals(Result.Error(TestError.BOOM), mapped)
    }

    @Test
    fun `asEmptyDataResult discards the success value`() {
        val result: Result<Int, TestError> = Result.Success(99)

        val empty = result.asEmptyDataResult()

        assertEquals(Result.Success(Unit), empty)
    }

    @Test
    fun `asEmptyDataResult propagates the error unchanged`() {
        val result: Result<Int, TestError> = Result.Error(TestError.BOOM)

        val empty: EmptyResult<TestError> = result.asEmptyDataResult()

        assertEquals(Result.Error(TestError.BOOM), empty)
    }
}
