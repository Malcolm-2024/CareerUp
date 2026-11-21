# Fuzzy Search Proposed Draft

**Status: proposed for team review.** A working local trial has been tested on the personal computer. This document does not merge application code or imply team approval.

## What it does

- Keeps normal exact and partial-text SQL searches first.
- Tries close spellings only when normal search returns no matches.
- Uses the existing Search button without extra controls, libraries or database changes.
- Retains the original role scopes, visibility rules and selected filters.

## Why propose it

Partial-text search helps with incomplete text. Typo tolerance helps with misspelled text. This is optional polish to an existing function; the five required Sprint 5 write actions remain the priority.

## Matching rules

Queries must contain 4–150 ASCII letters, digits or spaces. Matching ignores case and normalizes accents. Every query word must match or be close to a word in the searchable fields. Words shorter than four characters receive no typo correction. Words of 4–7 characters allow one edit; longer words allow two. Edits include insertion, deletion, substitution and adjacent transposition.

For example, `cumpus` and `campas` can match Campus. `campa` currently does not: it requires two edits for a short word. This is constrained typo matching, not semantic search or a guarantee that every misspelling matches.

The fallback reads at most 501 scoped candidate rows. If more than 500 exist, it retains ordinary search rather than returning incomplete fuzzy results. Fallback results retain existing SQL ordering; similarity ranking is not implemented. The limit suits a small demo and needs revisiting for larger inventories.

## Code

Paths below are relative to `docs/Sprint-5/deliverables`.

### Search and pagination

File: `backend/src/main/java/io/careerup/ApiController.java`, section 3, Shared Search and Pagination.

The normal count/query runs first. On zero results, the fallback reruns the same SQL with only the text term broadened. Other filters and authorization scopes remain. Matches are paginated after typo matching. A `fuzzy` response field identifies fallback results; the current interface has no separate fuzzy-search notice.

### Typo helper

File: `backend/src/main/java/io/careerup/TypoSearch.java`.

The helper compares words using edit distance against available name, email, title, description, bio, major and skills columns. SQL still determines which records the signed-in account is allowed to search. No external AI or network service is involved.

### Standalone checks

The trial includes `checks/TypoSearchCheck.java`. Checks cover deletion/substitution examples, multiword queries, unrelated terms, short/wildcard inputs and excluding non-search fields. Both modified Java files compiled against the supplied Spring Boot dependencies.

## Local results

Direct authenticated API checks against the running Student discovery route returned:

| Search | Observed result |
| --- | --- |
| `Campus` | Campus Companion through normal search |
| `Camp` | Campus Companion through partial search |
| `cumpus` | Campus Companion through typo fallback |
| `campas` | Campus Companion through typo fallback |
| `campa` | No results under the current edit limit |
| `zzzzzzzz` | No results |

These are local Student API checks, not proof that every role or device has passed regression testing.

## Before accepting it

1. Check normal results and Reset in the browser.
2. Test all role search screens, filters and pagination.
3. Verify advisor assignments, company ownership and hidden/draft exclusions remain enforced during fallback.
4. Check long/special-character inputs and the candidate limit.
5. Repeat all five Sprint 5 writes and original retrieval checks.
6. Have a teammate pull, rebuild and test, including the Mac.

## Apply or remove the trial

Preserve the finished submission first. Compare the existing controller before applying to newer code. Install the controller and helper together, rebuild and test before merging. To revert, restore the prior ApiController.java, remove TypoSearch.java and rebuild. No database rows or schema are changed.

## Presenter wording

Our original search finds matching text fragments. This optional fallback also handles small spelling mistakes when the normal search finds nothing, while retaining the same account permissions and filters.
