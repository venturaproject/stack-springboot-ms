import { describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '@/test/msw/server'
import { productsApi } from './products-api'

describe('productsApi.list (axios + MSW)', () => {
  it('returns the PagedResponse as-is', async () => {
    const page = await productsApi.list()
    expect(page.count).toBe(1)
    expect(page.data[0].sku).toBe('TSH-1001')
  })

  it('rejects on a 422 problem response', async () => {
    server.use(
      http.get('*/api/products', () =>
        HttpResponse.json({ title: 'Business rule violation', detail: 'nope' }, { status: 422 }),
      ),
    )
    await expect(productsApi.list()).rejects.toMatchObject({ response: { status: 422 } })
  })
})
