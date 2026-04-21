---
name: unit-testing
description: Writes unit tests
---

When writing unit tests for a system (SUT) in this project follow the following testing guidelines:

# Testing Guidelines

## Tech Stack
- *Testing framework*: JUnit4
- *Assertion*: Kotest
- *Mocking*: MockK
- *Flow testing*: Turbine

## Test Strategy
- Focus on testing the behaviours
- Cover exception, positive, and negative paths
- Do not test the internals of the framework or related dependencies
- Run the tests before submitting the result to make sure all tests pass

## Test Doubles
- Use dummies when the SUT requires an object that is irrelevant to the test
- Use fakes when you need a lightweight, functional version of a component, which is faster and easier to manage during testing (i.e. in-memory database)
- Use stubs when you need to control the output of a dependency to test specific scenarios within the SUT
- Use mocks when you need to verify interactions between the SUT and its dependencies, such as ensuring a certain method is called only once
- Use spies when you need to track method calls but still want to use the actual implementation for other methods
- Use ObjectMother pattern as a factory to create ready-to-use input data

## Test Structure
- Follow the Given-When-Then pattern in test method name and method body (i.e. `givenValidCredentials_whenLoginIsCalled_thenSuccessEventEmitted`)
- Separate the logical parts in the method body with a new line
- Do not add comments unless it is necessary to explain non-obvious cases
- Add prefix to variables naming to indicate it purpose (i.e. `dummy*`, `mock*`, `fake*`, etc.)
- Use `MainDispatcherRule` class as a `@Rule` to control dispatcher lifecycle and state
- Utilize test hooks (i.e. `@Before`, `@After`, etc.) for setup and cleanup
- Use `sut` variable name the class under test
