# Testing Guidelines

This document extends the `build_validation` rule with additional testing standards and best practices.

## Test Output Format Standards

### Success Indication
- Tests should output only `"S"` for successful test steps/assertions
- Use `System.out.print("S");` for each successful test component
- Avoid verbose success messages like "PASSED" or "Test completed successfully"

### Failure Details
- Only failures should include descriptive error messages
- Use `System.out.println("Test description FAILED");` for failures
- Include specific failure reasons to aid debugging

### Implementation Examples

#### ✅ Correct Pattern
```java
// Good: Simple success indicator
if (testCondition) {
    System.out.print("S");
    return true;
} else {
    System.out.println("Specific test description FAILED");
    return false;
}
```

#### ❌ Avoid This Pattern
```java
// Bad: Verbose success messages
if (testCondition) {
    System.out.println("Test XYZ PASSED");
    return true;
} else {
    System.out.println("FAILED");
    return false;
}
```

### Reference Implementations
- **`BattleFlowTest.java`** - Excellent example with comments: `System.out.print("S"); // Profile test passed`
- **`CoreGameTest.java`** - Helper method pattern: `assertEquals()` function
- **`ClientTest.java`** - Assertion-based pattern for unit tests
- **`SimpleEndToEndTest.java`** - End-to-end test step validation
- **`VictoryMessageTest.java`** - Comprehensive single-test validation

## Test Structure Standards

### Single Responsibility
- Each test method should focus on one specific functionality
- Break complex tests into smaller, focused test methods
- Use descriptive test method names

### Test Independence
- Tests should not depend on execution order
- Each test should set up its own required state
- Clean up resources after test completion

### Assertion Quality
- Use meaningful assertion messages
- Test both positive and negative cases
- Validate expected behavior, not implementation details

## Integration with Build System

### Build Script Integration
- All test files are automatically compiled by `./build.sh`
- Test execution is integrated into the build process
- Failed tests cause build failure (non-zero exit code)

### Continuous Validation
- Run `./build.sh` before making changes (baseline)
- Run `./build.sh` after making changes (validation)
- Address any test failures immediately

### Output Consistency
- Successful build shows series of "S" characters
- Failed tests show specific error messages
- Clean separation between success indicators and failure details

## Best Practices

### Test Coverage
- Cover main functionality paths
- Include edge cases and error conditions
- Test user-facing features end-to-end

### Mock Usage
- Use `MockStorage` and `MockTelegram` for isolated testing
- Avoid external dependencies in tests
- Create predictable test scenarios

### Performance
- Keep tests fast and focused
- Avoid unnecessary delays (e.g., `Thread.sleep()` unless testing timing)
- Use minimal test data sets

### Maintainability
- Keep test code clean and readable
- Document complex test scenarios
- Update tests when functionality changes

## Compliance Checklist

When creating or modifying tests:

- [ ] Uses `System.out.print("S");` for success
- [ ] Provides descriptive failure messages
- [ ] Follows naming conventions
- [ ] Is independent of other tests
- [ ] Covers both success and failure cases
- [ ] Compiles and runs with `./build.sh`
- [ ] Has clear, focused responsibility
- [ ] Uses appropriate mocks
- [ ] Includes meaningful assertions
- [ ] Maintains existing test compatibility

## Migration Guide

For existing tests that don't follow these guidelines:

1. Replace verbose success messages with `System.out.print("S");`
2. Ensure failure messages are descriptive and specific
3. Verify test independence and proper cleanup
4. Run `./build.sh` to validate changes
5. Update documentation if test behavior changes

---

*This document should be consulted alongside the `build_validation` rule for comprehensive testing standards.*