import { http, HttpResponse } from 'msw'

/**
 * Default happy-path handlers. Override per-test with `server.use(...)` for error cases.
 * Paths are wildcarded on host so they match whatever base URL axios resolves against.
 */
export const handlers = [
  http.get('*/api/products', () =>
    HttpResponse.json({
      data: [
        {
          id: '00000000-0000-0000-0000-000000000001',
          sku: 'TSH-1001',
          name: 'Test Tee',
          description: null,
          priceAmountMinorUnits: 1999,
          priceCurrency: 'USD',
          stockQuantity: 5,
          imageUrl: null,
          createdAt: '2026-01-01T00:00:00Z',
        },
      ],
      page: 1,
      perPage: 20,
      count: 1,
      totalPages: 1,
    }),
  ),
]
