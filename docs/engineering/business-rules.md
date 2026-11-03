# Personal finance business rules

## Project and implementation status

The application is in development. There is no production data that must be
preserved, but database changes must follow the repository's Flyway practices.

Stage 0 (initial backend inspection and business-rule agreement) is complete.
Stage 1 (name normalization) is complete. Confirm the current code before
assuming that any stage's implementation is present.

The user makes all code changes. Copilot must only guide the user and must not
edit repository files or run commands that modify the repository.

## 1. Names

- Account and category names are trimmed, stored in uppercase, and returned by
  the API in uppercase.
- Apply the same normalization to create, update, and duplicate-name checks.
- Archived names remain reserved; a name cannot be recreated with a different
  letter case.
- Account names are unique per user.
- Category names are unique per user, transaction type, and parent. Root
  category names are unique per user and transaction type.
- Do not uppercase unrelated values such as email, transaction description, or
  password.

## 2. Accounts: initial and current balance

- Accounts have separate `initialBalance` and current `balance` values.
- The user may correct `initialBalance`. The current balance is not directly
  editable; it changes through transactions and through the effect of correcting
  `initialBalance`.
- When `initialBalance` changes, adjust current `balance` by the difference
  between the new and old initial balances. Do not modify existing transactions.
- API and database monetary values are signed `BigDecimal` values.
- Negative balances are allowed for all account types.
- For a `CREDIT` account, the UI asks for a positive amount labelled
  “Initial debt” and sends its negative value to the API.
- For other account types, the UI uses “Initial balance” and allows a signed
  amount.
- Changing `accountType` never automatically changes the sign or value of
  `initialBalance` or current `balance`. The user corrects the initial balance
  separately if needed.
- The default `CASH` account allows only `initialBalance` changes. Its name and
  type cannot be changed, and it cannot be archived.
- These restrictions apply to editing or archiving the default `CASH` account.
  The account remains available for normal transactions, including as the
  default destination for income.

## 3. Accounts: type changes and archive

- `accountType` may change only when the account has no active transactions.
- Soft-deleted transactions count as inactive. Users cannot restore transactions.
- Accounts may be archived only when their current balance is exactly zero.
- Accounts are archived/restored, not normally deleted. Their transaction
  history is retained.
- Archived accounts cannot be selected for new transactions.
- Editing or deleting a transaction must not leave an archived account with a
  non-zero balance. Check the final balance atomically for every affected
  account, including both sides of transfers.
- An edit may retain an existing archived account association if the final
  operation preserves the archived account's zero-balance rule. A new or
  changed association must use an active account.
- Preserve correct balance reversal and recalculation for transaction
  create/update/delete.

## 4. Transactions, income, expenses, and transfers

- Transaction amounts are positive. Account balances are signed.
- Income credits a user-selected account. Default `CASH` is the default choice,
  not a mandatory destination.
- The frontend sends the UUID of the selected destination account for each
  income transaction. The backend must not silently choose an account when the
  destination is missing.
- This allows a refund to be recorded as a separate positive `INCOME`
  transaction credited directly to a `CREDIT` account. A refund does not modify
  the original expense.
- A `REFUNDS` category may be suggested, but it is not a mandatory model change.
- The frontend handles presentation, such as displaying negative values in red.
  The backend owns signed amounts and financial calculations.
- Transfers do not count as income or expenses.
- There is no user-facing transaction archive/restore feature. A soft-deleted
  transaction is inactive and cannot be restored by the user.

## 5. Categories: hierarchy and lifecycle

- Keep exactly one parent/child level.
- Both parent and child categories can be selected for transactions, even when a
  parent has children.
- Each transaction is linked to exactly one category.
- Creating children does not automatically recategorize existing transactions.
- Archiving a child archives only that child.
- Archiving a root archives the root and all its active children atomically.
- Restoring a root restores only the root. The user restores children manually.
- A child cannot be restored while its parent is archived.
- Archived categories cannot be selected for new transactions.
- Existing transactions keep their archived category associations and remain in
  historical data.
- An edit that retains an existing archived category may modify unrelated
  transaction fields. A new or changed category association must use an active
  category.

## 6. Financial totals and reports

All financial calculations are performed by the backend using `BigDecimal`.
The frontend displays the returned values and does not calculate financial
totals from paginated transaction data.

### Calendar-month boundaries

- Determine the current calendar month using the application's configured
  timezone and transaction date. Do not use transaction creation timestamps to
  decide which month a transaction belongs to.

### Dashboard

- `totalBalance` is the signed sum of all the user's account balances.
- `incomeThisMonth` is the sum of active income transactions in the current
  calendar month.
- `expenseThisMonth` is the sum of active expense transactions in the current
  calendar month.
- `netCashFlowThisMonth` is income minus expenses.
- Transfers are excluded from these monthly income/expense/net-flow totals.
- Refunds count as income.

### Account totals

- Return a signed balance total for each account type: `LIQUIDITY`, `SAVINGS`,
  `INVESTMENT`, and `CREDIT`.
- The `CREDIT` total remains signed; debt is generally negative.
- Do not convert credit balances to positive values in backend totals.

### Category monthly totals

- Calculate totals for the current calendar month using transaction date.
- Include active transactions only; soft-deleted transactions are excluded.
- Do not exclude a transaction just because its category or account is archived.
- Return monthly direct amounts for all categories, active and archived.
- A category's direct amount includes only transactions assigned directly to it.
- A parent's inclusive group total is its direct amount plus transactions
  assigned to all its children, active and archived.
- Each transaction counts once in a given total. Make the API/UI distinguish a
  parent's direct amount from its inclusive group total to avoid double counting.
- Calculate totals in grouped database queries or another efficient batch
  approach; do not issue one query per category.
- Do not persist derived booleans or counters such as
  `hasActiveTransactionsThisMonth`.

### Reports MVP

- Allow the user to select a date period.
- Show three summary values: income, expenses, and net cash flow.
- Provide separate `EXPENSE`, `INCOME`, and `TRANSFER` tabs.
- Expense and income views show totals by parent/child category.
- The transfer view shows transactions and source/target accounts; it does not
  need a grand total of transfer amounts.
- Refunds count as income.
- The first version assumes one common currency. Do not implement currency
  conversion or add partial multi-currency behavior.

## 7. Suggested implementation stages

Follow the stage requested by the user and re-check dependencies against the
current code before proceeding.

0. Initial code/business/API/test inspection — complete.
1. Name normalization and duplicate checks — complete.
2. Account model/API: persisted `initialBalance`, initial-balance correction,
   default CASH behavior, account type rules, and focused tests.
3. Account archive/restore; safeguards for transactions affecting archived
   accounts; selectable income destination on create and update.
4. Category archive/restore, including cascading archive of active children
   when a root is archived.
5. Backend aggregations: dashboard, account totals, category monthly totals, and
   report period aggregations. First agree API/DTO/query contracts if the
   requested scope leaves choices open.
6. Frontend integration in the separate frontend repository, when its code is
   available. Do not assume the frontend exists in this backend repository.
7. Final focused and integration tests, full-suite run at a meaningful
   milestone, and related documentation.

Stages are a dependency guide, not permission to implement all stages in one
task. Implement only the scope requested by the user.