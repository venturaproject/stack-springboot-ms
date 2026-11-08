import { describe, expect, it } from 'vitest'
import { AxiosError, type AxiosResponse } from 'axios'
import { getErrorMessage, getFieldErrors, isNotFound, isStaleWrite } from './errors'

function axiosError(status: number, data: unknown): AxiosError {
  const response = { status, data, statusText: '', headers: {}, config: { headers: {} } } as unknown as AxiosResponse
  return new AxiosError('request failed', 'ERR_BAD_RESPONSE', undefined, undefined, response)
}

describe('getErrorMessage', () => {
  it('prefers the problem-detail `detail`', () => {
    const err = axiosError(422, { title: 'Business rule violation', detail: 'Only a PLACED order can be confirmed' })
    expect(getErrorMessage(err, 'fallback')).toBe('Only a PLACED order can be confirmed')
  })

  it('falls back to `title` when there is no `detail`', () => {
    expect(getErrorMessage(axiosError(403, { title: 'Access denied' }), 'fallback')).toBe('Access denied')
  })

  it('still reads a legacy `{ message }` body', () => {
    expect(getErrorMessage(axiosError(400, { message: 'legacy' }), 'fallback')).toBe('legacy')
  })

  it('uses the fallback for a non-axios error with no message', () => {
    expect(getErrorMessage({}, 'fallback')).toBe('fallback')
  })
})

describe('field errors and status helpers', () => {
  it('extracts the validation field map', () => {
    const err = axiosError(400, { title: 'Validation failed', errors: { email: 'must be a well-formed email address' } })
    expect(getFieldErrors(err)).toEqual({ email: 'must be a well-formed email address' })
  })

  it('detects a stale-write conflict by problem type', () => {
    expect(isStaleWrite(axiosError(409, { type: 'urn:problem-type:stale-write' }))).toBe(true)
    expect(isStaleWrite(axiosError(409, { type: 'urn:problem-type:duplicate' }))).toBe(false)
  })

  it('detects a 404', () => {
    expect(isNotFound(axiosError(404, {}))).toBe(true)
    expect(isNotFound(axiosError(500, {}))).toBe(false)
  })
})
