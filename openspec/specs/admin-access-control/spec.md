# admin-access-control Specification

## Purpose
TBD - created by archiving change add-admin-auth-and-permission. Update Purpose after archive.
## Requirements
### Requirement: `exam-service` SHALL manage administrator authorization through roles and permissions
The system SHALL model administrator authorization in `exam-service` using users, roles, permissions, user-role associations, and role-permission associations, and SHALL keep the API contract independent from persistence entities.

#### Scenario: Current user authorization is assembled from role assignments
- **WHEN** `exam-service` resolves the authenticated administrator for `GET /api/admin/auth/me`
- **THEN** it SHALL aggregate the administrator's roles and permissions from the user-role and role-permission relationships and return them through DTO responses rather than exposing persistence entities directly

#### Scenario: Administrator without assigned role has no protected access
- **WHEN** an authenticated administrator has no role assignment or no effective permission assignment
- **THEN** `exam-service` SHALL treat the administrator as lacking protected management permissions

### Requirement: `exam-web` SHALL enforce menu or route visibility from granted permissions
The system SHALL let `exam-web` show only the basic management menus and protected routes that the current authenticated administrator is allowed to access.

#### Scenario: Visible menu is filtered by granted permissions
- **WHEN** `exam-web` receives the current administrator's menus or permission codes from `GET /api/admin/auth/me`
- **THEN** `exam-web` SHALL render only the menu items allowed for that administrator

#### Scenario: Authenticated administrator opens a route without permission
- **WHEN** an authenticated administrator navigates to a protected management route whose required permission is not granted
- **THEN** `exam-web` SHALL show a no-permission state instead of rendering the target page content

### Requirement: `exam-service` SHALL deny protected interfaces without matching permission
The system SHALL enforce interface-level permission control for protected management APIs in `exam-service`.

#### Scenario: Administrator calls API with required permission
- **WHEN** an authenticated administrator calls a protected management API and the administrator holds the required permission
- **THEN** `exam-service` SHALL allow the request to proceed

#### Scenario: Administrator calls API without required permission
- **WHEN** an authenticated administrator calls a protected management API and the administrator does not hold the required permission
- **THEN** `exam-service` SHALL reject the request as forbidden

#### Scenario: Public authentication endpoints remain accessible without permission checks
- **WHEN** a client calls a public authentication endpoint such as login
- **THEN** `exam-service` SHALL allow the request without requiring an authenticated permission context

