## MODIFIED Requirements

### Requirement: Admin user can sign in from `exam-web`
The system SHALL allow an enabled administrator to submit username and password from `exam-web` and complete login through `exam-service`. The `exam-web` login page SHALL present a focused single-entry sign-in experience, while using the same theme language as the management console for brand identity, color system, form controls, and primary action styling.

#### Scenario: Successful login with valid credentials
- **WHEN** an enabled administrator submits a valid username and password on the `exam-web` login page
- **THEN** `exam-service` returns a successful login response containing a Bearer Token, token expiry information, and basic current-user summary

#### Scenario: Login fails with invalid credentials
- **WHEN** a user submits an unknown username or incorrect password on the `exam-web` login page
- **THEN** `exam-service` rejects the request with an authentication failure response and `exam-web` SHALL display a login failure message without entering the management area

#### Scenario: Login page remains a focused entry page
- **WHEN** an unauthenticated user opens the `exam-web` login page
- **THEN** the page SHALL use the same theme language as the management console
- **THEN** the page SHALL render the login form within a centered single-card layout and SHALL NOT render a left-side introduction panel
- **THEN** the page SHALL NOT render the post-login sidebar navigation or authenticated user toolbar

#### Scenario: Disabled administrator cannot log in
- **WHEN** a disabled administrator submits otherwise valid credentials
- **THEN** `exam-service` SHALL reject the login and `exam-web` SHALL keep the user on the login page
