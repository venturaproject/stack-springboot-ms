import { describe, expect, it } from 'vitest'
import { emptyPage, toPaginated, unwrap } from './api-utils'

describe('toPaginated', () => {
  it('passes a PagedResponse through field-for-field', () => {
    const raw = { data: [1, 2], page: 2, perPage: 10, count: 42, totalPages: 5 }
    expect(toPaginated<number>(raw)).toEqual(raw)
  })

  it('wraps a bare array as a single full page', () => {
    expect(toPaginated<string>(['a', 'b', 'c'])).toEqual({
      data: ['a', 'b', 'c'],
      page: 1,
      perPage: 3,
      count: 3,
      totalPages: 1,
    })
  })

  it('falls back to an empty page for a shape it does not recognise', () => {
    expect(toPaginated(null)).toEqual(emptyPage())
    expect(toPaginated({ nope: true })).toEqual(emptyPage())
  })
})

describe('unwrap', () => {
  it('returns the nested data object when present', () => {
    expect(unwrap<{ id: number }>({ data: { id: 7 } })).toEqual({ id: 7 })
  })

  it('returns the object itself when there is no data envelope', () => {
    expect(unwrap<{ id: number }>({ id: 7 })).toEqual({ id: 7 })
  })

  it('returns null for a non-object', () => {
    expect(unwrap('x')).toBeNull()
  })
})
