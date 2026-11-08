import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '@/test/msw/server'
import { SpringHttpOnlyAdapter } from './auth-adapter'

const user = {
  id: 'a7a699ea-2690-4c43-bd42-c25ce2a03b76',
  email: 'admin@example.com',
  firstName: 'Ada',
  lastName: 'Lovelace',
  avatarUrl: '/media/ada.png',
  roles: ['ADMIN'],
  permissions: ['users:read', 'products:read'],
}

describe('SpringHttpOnlyAdapter', () => {
  const adapter = new SpringHttpOnlyAdapter()

  it('posts login credentials and maps the Spring user DTO', async () => {
    server.use(
      http.get('*/api/auth/csrf', () => HttpResponse.json({ token: 'csrf-test-token' })),
      http.post('*/api/auth/login', async ({ request }) => {
        expect(request.headers.get('X-XSRF-TOKEN')).toBe('csrf-test-token')
        expect(await request.json()).toEqual({ email: 'admin@example.com', password: 'secret' })
        return HttpResponse.json({ user })
      }),
    )

    const session = await adapter.login({ login: 'admin@example.com', password: 'secret' })

    expect(session.user).toMatchObject({
      id: user.id,
      name: 'Ada Lovelace',
      email: user.email,
      avatar: user.avatarUrl,
      roles: user.roles,
      permissions: user.permissions,
    })
    expect(session.permissions).toEqual(user.permissions)
  })

  it('fetches the session and treats only unauthorized responses as signed out', async () => {
    server.use(http.get('*/api/auth/me', () => HttpResponse.json(user)))
    await expect(adapter.fetchSession()).resolves.toMatchObject({ user: { id: user.id } })

    server.use(http.get('*/api/auth/me', () => new HttpResponse(null, { status: 401 })))
    await expect(adapter.fetchSession()).resolves.toBeNull()
  })

  it('uses the Spring refresh and logout endpoints', async () => {
    let refreshCalls = 0
    let logoutCalls = 0
    server.use(
      http.post('*/api/auth/refresh', ({ request }) => {
        expect(request.headers.get('X-XSRF-TOKEN')).toBe('csrf-test-token')
        refreshCalls += 1
        return HttpResponse.json(user)
      }),
      http.post('*/api/auth/logout', () => {
        logoutCalls += 1
        return new HttpResponse(null, { status: 204 })
      }),
    )

    await expect(adapter.refresh()).resolves.toBe(true)
    await adapter.logout()
    expect(refreshCalls).toBe(1)
    expect(logoutCalls).toBe(1)
    expect(adapter.getRequestHeaders()).toEqual({ 'X-XSRF-TOKEN': 'csrf-test-token' })
  })
})
