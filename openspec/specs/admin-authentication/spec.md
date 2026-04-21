# admin-authentication Specification

## Purpose
TBD - created by archiving change add-admin-auth-and-permission. Update Purpose after archive.
## Requirements
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

### Requirement: Authenticated admin session can be restored and terminated
The system SHALL support login state restoration and logout for the management side using the Token issued by `exam-service`.

#### Scenario: Restore current user from valid token
- **WHEN** `exam-web` starts with a locally stored valid Bearer Token
- **THEN** `exam-web` SHALL call `GET /api/admin/auth/me` and `exam-service` SHALL return the current administrator profile, roles, permissions, and menus for session restoration

#### Scenario: Reject expired or invalid token during restoration
- **WHEN** `exam-web` calls `GET /api/admin/auth/me` with an expired, invalid, or revoked Token
- **THEN** `exam-service` SHALL reject the request as unauthenticated and `exam-web` SHALL clear the local login state and return to the login page

#### Scenario: Logout invalidates current session
- **WHEN** an authenticated administrator calls `POST /api/admin/auth/logout`
- **THEN** `exam-service` SHALL mark the current session invalid and `exam-web` SHALL clear the local Token and authenticated user state

### Requirement: Protected management routes require authentication
The system SHALL prevent unauthenticated access to protected management routes and protected management APIs.

#### Scenario: Unauthenticated user opens protected route
- **WHEN** a user directly visits a protected management route in `exam-web` without a valid login state
- **THEN** `exam-web` SHALL redirect the user to the login page

#### Scenario: Unauthenticated request calls protected API
- **WHEN** a request reaches a protected management API in `exam-service` without a valid Bearer Token
- **THEN** `exam-service` SHALL reject the request as unauthenticated

