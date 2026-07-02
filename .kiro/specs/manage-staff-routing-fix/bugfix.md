# Bugfix Requirements Document

## Introduction

When an admin user clicks "Manage Staff" in the sidebar navigation, the application redirects to the Dashboard instead of displaying the Staff List page. The root cause is a case-sensitivity mismatch in the `adminGuard`: the backend's `AuthService` sends the role as `"ADMIN"` (using Java enum `.name()`), but the guard compares against the string `'Admin'` (title-case). The comparison fails, the guard returns `false`, and the router redirects to `/dashboard`. The navigation link itself displays correctly because the layout template uses a case-insensitive comparison (`.toLowerCase() === 'admin'`).

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN an authenticated user with role value `"ADMIN"` (as returned by the backend) clicks "Manage Staff" THEN the system redirects to the Dashboard because `adminGuard` compares `userRole() === 'Admin'` which evaluates to `false` for the value `"ADMIN"`

1.2 WHEN an authenticated user with role value `"ADMIN"` navigates directly to `/admin/staff` via the URL bar THEN the system redirects to the Dashboard because the same case-sensitive guard check fails

### Expected Behavior (Correct)

2.1 WHEN an authenticated user with admin role clicks "Manage Staff" THEN the system SHALL navigate to `/admin/staff` and display the Staff List component regardless of whether the backend sends `"ADMIN"`, `"Admin"`, or `"admin"`

2.2 WHEN an authenticated user with admin role navigates directly to `/admin/staff` via the URL bar THEN the system SHALL display the Staff List component regardless of the role string casing from the backend

### Unchanged Behavior (Regression Prevention)

3.1 WHEN a non-admin user (role `"STAFF"` or `"Staff"`) attempts to access `/admin/staff` THEN the system SHALL CONTINUE TO redirect them to `/dashboard`

3.2 WHEN an unauthenticated user attempts to access any protected route THEN the system SHALL CONTINUE TO redirect them to `/login`

3.3 WHEN an admin user clicks other navigation items (Dashboard, Employees, My Tasks, Skills Register, Interactions) THEN the system SHALL CONTINUE TO navigate to those routes correctly

3.4 WHEN an admin user accesses `/admin/staff/create` THEN the system SHALL CONTINUE TO display the Staff Create component (protected by the same guard fix)
