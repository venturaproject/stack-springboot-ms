import { AxiosError, isAxiosError } from "axios"

export { AxiosError, isAxiosError }

/**
 * RFC 7807 problem document — the shape every backend error response uses
 * (see backend GlobalExceptionHandler). `errors` is present on validation failures.
 */
export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  traceId?: string
  errors?: Record<string, string>
}

function problem(error: unknown): ProblemDetail | undefined {
  if (isAxiosError(error)) {
    const data = error.response?.data
    if (data && typeof data === "object") return data as ProblemDetail
  }
  return undefined
}

export function getStatus(error: unknown): number | null {
  if (isAxiosError(error)) return error.response?.status ?? null
  const s = (error as { statusCode?: unknown })?.statusCode
  return typeof s === "number" ? s : null
}

export function isNotFound(error: unknown): boolean {
  return getStatus(error) === 404
}

/** True when the resource changed under us (optimistic-lock conflict) — caller should reload. */
export function isStaleWrite(error: unknown): boolean {
  return getStatus(error) === 409 && problem(error)?.type === "urn:problem-type:stale-write"
}

/** Field -> message map from a 400 validation problem, or `{}`. */
export function getFieldErrors(error: unknown): Record<string, string> {
  return problem(error)?.errors ?? {}
}

/** The trace id to quote to support, if the backend attached one. */
export function getTraceId(error: unknown): string | undefined {
  return problem(error)?.traceId
}

export function getErrorMessage(error: unknown, fallback = "Server error, please try again!"): string {
  const p = problem(error)
  if (p?.detail) return p.detail
  if (p?.title) return p.title
  // Legacy `{ message }` bodies and non-Axios errors.
  if (isAxiosError(error)) {
    const legacy = (error.response?.data as { message?: string } | undefined)?.message
    if (legacy) return legacy
  } else if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}
