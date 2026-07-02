# Implementation Plan

## Overview

Fix a case-sensitivity mismatch in `adminGuard` where the backend sends role `"ADMIN"` but the guard checks `=== 'Admin'`, blocking admin users from `/admin/staff` routes. The fix uses `.toLowerCase() === 'admin'` for case-insensitive comparison.

## Tasks

- [x] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - Admin role casing mismatch blocks admin routes
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the bug exists
  - **Scoped PBT Approach**: Scope property to concrete failing cases: role `"ADMIN"` (backend value), `"admin"`, `"AdMiN"` with target routes `/admin/staff` and `/admin/staff/create`
  - Create test file `frontend/src/app/auth/guards/admin.guard.spec.ts`
  - Set up Angular TestBed with mocked `AuthService` (provide `userRole` signal returning test role string) and mocked `Router`
  - Test case 1: Set `userRole()` to `"ADMIN"` (actual backend value), call `adminGuard`, assert returns `true`
  - Test case 2: Set `userRole()` to `"admin"` (lowercase), call `adminGuard`, assert returns `true`
  - Test case 3: Set `userRole()` to `"AdMiN"` (mixed-case), call `adminGuard`, assert returns `true`
  - Test case 4: Set `userRole()` to `"Admin"` (title-case), call `adminGuard`, assert returns `true` (only case that passes on unfixed code)
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests 1-3 FAIL (guard returns `false` and redirects to `/dashboard`), Test 4 passes (confirms case-sensitivity is the root cause)
  - Document counterexamples: `adminGuard` returns `false` for role `"ADMIN"` because `"ADMIN" === 'Admin'` evaluates to `false`
  - Mark task complete when tests are written, run, and failures documented
  - _Requirements: 1.1, 1.2_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Non-admin users blocked from admin routes
  - **IMPORTANT**: Follow observation-first methodology
  - Observe: `adminGuard` with role `"STAFF"` returns `false` and redirects to `/dashboard` on unfixed code
  - Observe: `adminGuard` with role `"Staff"` returns `false` and redirects to `/dashboard` on unfixed code
  - Observe: `adminGuard` with role `""` (empty) returns `false` and redirects to `/dashboard` on unfixed code
  - Observe: `adminGuard` with role `"MANAGER"` (arbitrary non-admin) returns `false` and redirects to `/dashboard` on unfixed code
  - Write property-based tests in `frontend/src/app/auth/guards/admin.guard.spec.ts`:
    - Property: for all role strings that are NOT case-insensitively equal to `"admin"`, `adminGuard` returns `false` and triggers `router.navigate(['/dashboard'])`
    - Generate random non-admin strings (e.g., `"STAFF"`, `"Staff"`, `"staff"`, `""`, `"user"`, `"MANAGER"`, random alphanumeric) and verify guard blocks access
    - Use fast-check library for property-based generation of arbitrary non-admin role strings
  - Verify all preservation tests PASS on UNFIXED code (non-admin users are already blocked correctly)
  - **EXPECTED OUTCOME**: Tests PASS (confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 3. Fix for admin route case-sensitivity mismatch in adminGuard

  - [x] 3.1 Implement the fix
    - Open `frontend/src/app/auth/guards/admin.guard.ts`
    - Change `authService.userRole() === 'Admin'` to `authService.userRole().toLowerCase() === 'admin'`
    - This mirrors the existing pattern in `layout.component.ts` sidebar visibility logic
    - Single-line change, minimal scope
    - _Bug_Condition: isBugCondition(input) where input.userRole.toUpperCase() === 'ADMIN' AND input.userRole !== 'Admin' AND targetRoute IN ['/admin/staff', '/admin/staff/create']_
    - _Expected_Behavior: adminGuard returns true for any case-variant of "admin" role string_
    - _Preservation: Non-admin users (role NOT case-insensitively "admin") still blocked and redirected to /dashboard_
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 3.1, 3.2, 3.3, 3.4_

  - [x] 3.2 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - Admin role casing mismatch no longer blocks admin routes
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior (guard returns `true` for all admin case variants)
    - When this test passes, it confirms the expected behavior is satisfied
    - Run bug condition exploration test from step 1
    - **EXPECTED OUTCOME**: All 4 test cases PASS (confirms bug is fixed — guard accepts `"ADMIN"`, `"admin"`, `"AdMiN"`, `"Admin"`)
    - _Requirements: 2.1, 2.2_

  - [x] 3.3 Verify preservation tests still pass
    - **Property 2: Preservation** - Non-admin users still blocked from admin routes
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run preservation property tests from step 2
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions — non-admin users still blocked)
    - Confirm all property-based tests still pass after fix (no regressions introduced)

- [x] 4. Checkpoint - Ensure all tests pass
  - Run full frontend test suite to confirm no regressions
  - Verify bug condition tests pass (admin access works for all role casings)
  - Verify preservation tests pass (non-admin users still blocked)
  - Ensure no other tests broken by the change
  - Ask the user if questions arise


## Task Dependency Graph

```json
{
  "waves": [
    {"tasks": ["1", "2"]},
    {"tasks": ["3.1"]},
    {"tasks": ["3.2", "3.3"]},
    {"tasks": ["4"]}
  ]
}
```

## Notes

- File to modify: `frontend/src/app/auth/guards/admin.guard.ts`
- Test file: `frontend/src/app/auth/guards/admin.guard.spec.ts`
- Property-based testing uses `fast-check` library for generating arbitrary non-admin role strings
- Tasks 1 and 2 MUST be completed BEFORE task 3 (exploration-first methodology)
- The fix is a single-line change mirroring the existing pattern in `layout.component.ts`
