# Manage Staff Routing Fix — Bugfix Design

## Overview

The `adminGuard` in `admin.guard.ts` performs a strict equality check `userRole() === 'Admin'` (title-case) against the role string stored by `AuthService`. The backend's `AuthService.login()` populates the role field using `staff.getRole().name()`, which returns the raw Java enum constant name `"ADMIN"` (upper-case). This mismatch causes the guard to always return `false` for admin users, redirecting them to `/dashboard`.

The fix makes the guard comparison case-insensitive so it works regardless of how the backend serializes the role string.

## Glossary

- **Bug_Condition (C)**: The condition that triggers the bug — when `adminGuard` receives a role string whose casing differs from the hard-coded `'Admin'` literal
- **Property (P)**: The desired behavior — admin users can access `/admin/staff` and `/admin/staff/create` routes regardless of role string casing
- **Preservation**: Non-admin users must still be blocked; other routes and navigation must remain unaffected
- **`adminGuard`**: The `CanActivateFn` in `frontend/src/app/auth/guards/admin.guard.ts` that protects admin routes
- **`AuthService.userRole()`**: Computed signal in `auth.service.ts` returning the role string from the stored `AuthResponse`
- **`StaffRole`**: Java enum with values `ADMIN` and `STAFF`; `name()` returns `"ADMIN"`/`"STAFF"`, `getDisplayName()` returns `"Admin"`/`"Staff"`

## Bug Details

### Bug Condition

The bug manifests when an authenticated admin user attempts to access any route protected by `adminGuard`. The guard evaluates `authService.userRole() === 'Admin'` using strict equality. Because `AuthService.login()` on the backend calls `staff.getRole().name()` (yielding `"ADMIN"`), the comparison fails.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type { userRole: string, targetRoute: string }
  OUTPUT: boolean

  RETURN input.targetRoute IN ['/admin/staff', '/admin/staff/create']
         AND input.userRole.toUpperCase() === 'ADMIN'
         AND input.userRole !== 'Admin'
END FUNCTION
```

### Examples

- User role `"ADMIN"` navigates to `/admin/staff` → guard evaluates `"ADMIN" === 'Admin'` → `false` → redirected to `/dashboard` (BUG)
- User role `"admin"` navigates to `/admin/staff` → guard evaluates `"admin" === 'Admin'` → `false` → redirected to `/dashboard` (BUG)
- User role `"Admin"` navigates to `/admin/staff` → guard evaluates `"Admin" === 'Admin'` → `true` → allowed (works by coincidence only)
- User role `"STAFF"` navigates to `/admin/staff` → guard evaluates `"STAFF" === 'Admin'` → `false` → redirected to `/dashboard` (CORRECT — non-admin blocked)

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Non-admin users (role `"STAFF"` in any casing) must still be redirected to `/dashboard` when accessing admin routes
- Unauthenticated users must still be redirected to `/login` by `authGuard` (separate guard, not modified)
- All non-admin routes (Dashboard, Employees, Tasks, Skills, Interactions, Portfolio) must continue to function identically
- The sidebar "Manage Staff" link visibility logic (`.toLowerCase() === 'admin'`) must remain unchanged
- `AuthService` signal behavior, token storage, and logout flow must remain unchanged

**Scope:**
All inputs where the user role is NOT any case-variant of `"admin"` should be completely unaffected by this fix. This includes:
- Navigation by non-admin users to any route
- Navigation by admin users to non-admin-guarded routes
- Login/logout/refresh token flows
- Sidebar rendering and navigation for all users

## Hypothesized Root Cause

Based on the code analysis, the root cause is definitively identified:

1. **Backend `AuthService.login()` uses `.name()` instead of `.getDisplayName()`**: Line `staff.getRole().name()` returns the raw enum constant `"ADMIN"`, bypassing the `@JsonValue`-annotated `getDisplayName()` method that would return `"Admin"`. The same issue exists in `AuthService.refresh()`.

2. **Frontend `adminGuard` uses strict equality with a specific casing**: The comparison `userRole() === 'Admin'` is fragile — it only works when the backend sends exactly `"Admin"`.

3. **Layout component already handles this correctly**: The sidebar uses `.toLowerCase() === 'admin'` which is resilient to casing differences. The guard was not written with the same defensive approach.

The most robust fix is in the frontend guard (case-insensitive comparison) because:
- It makes the guard resilient to any future backend serialization changes
- It mirrors the existing pattern already used in the layout component
- It does not require backend changes or redeployment

## Correctness Properties

Property 1: Bug Condition - Admin users can access admin routes regardless of role casing

_For any_ input where the user has a role that is case-insensitively equal to `"admin"` and attempts to navigate to an admin-guarded route (`/admin/staff` or `/admin/staff/create`), the fixed `adminGuard` SHALL return `true` and allow navigation without redirecting to `/dashboard`.

**Validates: Requirements 2.1, 2.2**

Property 2: Preservation - Non-admin users are still blocked from admin routes

_For any_ input where the user's role is NOT case-insensitively equal to `"admin"` (e.g., `"STAFF"`, `"Staff"`, or any other value), the fixed `adminGuard` SHALL return `false` and redirect to `/dashboard`, preserving the existing access control behavior.

**Validates: Requirements 3.1, 3.3, 3.4**

## Fix Implementation

### Changes Required

**File**: `frontend/src/app/auth/guards/admin.guard.ts`

**Function**: `adminGuard` (CanActivateFn)

**Specific Changes**:
1. **Replace strict equality with case-insensitive comparison**: Change `authService.userRole() === 'Admin'` to `authService.userRole().toLowerCase() === 'admin'`
   - This matches the existing pattern used in `layout.component.ts`
   - Handles `"ADMIN"`, `"Admin"`, `"admin"`, or any mixed casing

**Before:**
```typescript
if (authService.userRole() === 'Admin') {
  return true;
}
```

**After:**
```typescript
if (authService.userRole().toLowerCase() === 'admin') {
  return true;
}
```

This is a single-line change, minimal in scope, and mirrors the existing convention in the codebase.

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that demonstrate the bug on unfixed code, then verify the fix works correctly and preserves existing behavior.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix. Confirm the root cause is specifically the case-sensitive comparison.

**Test Plan**: Write unit tests for `adminGuard` that inject an `AuthService` returning role `"ADMIN"` (as the backend sends it). Run on unfixed code to observe the guard incorrectly returning `false`.

**Test Cases**:
1. **Admin uppercase role test**: Set `userRole()` to `"ADMIN"`, assert guard returns `true` (will fail on unfixed code)
2. **Admin lowercase role test**: Set `userRole()` to `"admin"`, assert guard returns `true` (will fail on unfixed code)
3. **Admin mixed-case role test**: Set `userRole()` to `"AdMiN"`, assert guard returns `true` (will fail on unfixed code)
4. **Admin title-case role test**: Set `userRole()` to `"Admin"`, assert guard returns `true` (will pass on unfixed code — only case that works)

**Expected Counterexamples**:
- Guard returns `false` and redirects to `/dashboard` for role `"ADMIN"` (the actual backend value)
- Confirms the case-sensitivity is the sole root cause

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed guard allows access.

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  result := adminGuard_fixed(input)
  ASSERT result === true
  ASSERT router.navigate NOT called
END FOR
```

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed guard still blocks access.

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT adminGuard_original(input) === adminGuard_fixed(input)
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because:
- It can generate many random non-admin role strings to verify they are all blocked
- It catches edge cases like empty strings, null, or unexpected values
- It provides strong guarantees that only admin users gain access

**Test Plan**: Observe behavior on unfixed code for non-admin roles (should redirect to `/dashboard`), then write property-based tests confirming this continues after the fix.

**Test Cases**:
1. **Staff role preservation**: Verify `"STAFF"`, `"Staff"`, `"staff"` are all redirected to `/dashboard`
2. **Empty role preservation**: Verify empty string `""` is redirected to `/dashboard`
3. **Arbitrary string preservation**: Verify any string that is not case-insensitively `"admin"` causes redirect
4. **Other route preservation**: Verify admin users can still access Dashboard, Employees, Tasks without issue

### Unit Tests

- Test `adminGuard` with role `"ADMIN"` (backend value) → should return `true` after fix
- Test `adminGuard` with role `"Admin"` (display name) → should return `true`
- Test `adminGuard` with role `"admin"` (lowercase) → should return `true`
- Test `adminGuard` with role `"STAFF"` → should return `false` and redirect
- Test `adminGuard` with role `""` (empty) → should return `false` and redirect
- Test `adminGuard` with role `"Staff"` → should return `false` and redirect

### Property-Based Tests

- Generate random strings that are case-insensitive matches for `"admin"` → verify guard returns `true`
- Generate random strings that are NOT case-insensitive matches for `"admin"` → verify guard returns `false` and redirects to `/dashboard`
- Generate random non-admin role strings → verify guard behavior is identical before and after fix

### Integration Tests

- Test full login flow with backend role `"ADMIN"` → navigate to `/admin/staff` → verify Staff List component renders
- Test full login flow with role `"STAFF"` → navigate to `/admin/staff` → verify redirect to `/dashboard`
- Test that sidebar visibility and guard behavior are consistent (both use case-insensitive comparison after fix)
