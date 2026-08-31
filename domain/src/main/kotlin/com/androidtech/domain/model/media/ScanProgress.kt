package com.androidtech.domain.model.media

/**
 * State machine describing the lifecycle of a media/duplicate scan.
 *
 * Modeled as a sealed interface so consumers (ViewModels, use cases) can
 * exhaustively `when`-match without an `else` branch, and so new terminal or
 * intermediate states can be added later without silently compiling callers
 * that forgot to handle them.
 *
 * Expected transitions: [Idle] -> [Scanning] (zero or more, self-transitions
 * as progress updates) -> [Completed] | [Error]. A scan is not expected to
 * resume from [Completed] or [Error] without first returning to [Idle].
 */
sealed interface ScanProgress {

    /** No scan has started, or the previous scan's result has been consumed/reset. */
    data object Idle : ScanProgress

    /**
     * A scan is actively running.
     *
     * @property currentFile Path or display name of the file currently being
     *   processed, primarily for showing live progress text to the user.
     * @property percentage Overall completion, `0..100`. Detectors that
     *   cannot estimate total work up front may hold this at 0 until a total
     *   count is known.
     * @property itemsFound Count of items discovered/processed so far.
     */
    data class Scanning(
        val currentFile: String,
        val percentage: Int,
        val itemsFound: Int
    ) : ScanProgress {
        init {
            require(percentage in 0..100) { "percentage must be within 0..100, was $percentage" }
        }
    }

    /**
     * The scan finished successfully.
     *
     * @property duplicateGroups All duplicate groups found during the scan.
     *   An empty list is a valid, successful outcome (no duplicates found).
     */
    data class Completed(
        val duplicateGroups: List<DuplicateGroup>
    ) : ScanProgress

    /**
     * The scan terminated before completion.
     *
     * @property message Human-readable (but not necessarily user-facing as-is)
     *   description of the failure, for logging and UI error mapping.
     * @property cause Optional underlying throwable for diagnostics.
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : ScanProgress
}