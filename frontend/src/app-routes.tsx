/**
 * Application route registry.
 *
 * The only file that changes when adding, removing or renaming a business module.
 * Paths are plain strings — there is no `pathFor()` indirection; link to them directly.
 *
 * Every module is code-split with `React.lazy` — the initial bundle carries only the shell
 * and shared vendor code; each screen's JS loads on first navigation. The `<Suspense>`
 * boundary is in `lib/router.tsx` (one per guarded section), falling back to `RoutePending`.
 */
import { lazy } from 'react'
import type { RouteObject } from 'react-router-dom'

const SignIn = lazy(() => import('@/pages/auth/sign-in/sign-in-2'))
const ForgotPasswordPage = lazy(() => import('@/pages/auth/forgot-password'))

const DashboardRoute = lazy(() => import('@/routes/dashboard-route'))
const ProductsRoute = lazy(() => import('@/routes/products-route'))
const CustomersRoute = lazy(() => import('@/routes/customers-route'))
const OrdersRoute = lazy(() => import('@/routes/orders-route'))
const UsersRoute = lazy(() => import('@/routes/users-route'))
const RolesRoute = lazy(() => import('@/routes/roles-route'))
const PermissionsRoute = lazy(() => import('@/routes/permissions-route'))
const ApiClientsRoute = lazy(() => import('@/routes/api-clients-route'))
const SettingsRoute = lazy(() => import('@/routes/settings-route'))

// ── Guest routes (unauthenticated only) ───────────────────────────────────────

export const guestRoutes: RouteObject[] = [
  { path: '/login', element: <SignIn canResetPassword={true} /> },
  { path: '/forgot-password', element: <ForgotPasswordPage /> },
]

// ── Private routes (authenticated only) ──────────────────────────────────────

export const privateRoutes: RouteObject[] = [
  { path: '/admin', element: <DashboardRoute /> },

  { path: '/admin/products', element: <ProductsRoute /> },
  { path: '/admin/products/create', element: <ProductsRoute /> },
  { path: '/admin/products/:id', element: <ProductsRoute /> },
  { path: '/admin/products/:id/edit', element: <ProductsRoute /> },

  { path: '/admin/customers', element: <CustomersRoute /> },
  { path: '/admin/customers/create', element: <CustomersRoute /> },
  { path: '/admin/customers/:id', element: <CustomersRoute /> },
  { path: '/admin/customers/:id/edit', element: <CustomersRoute /> },

  { path: '/admin/orders', element: <OrdersRoute /> },
  { path: '/admin/orders/create', element: <OrdersRoute /> },
  { path: '/admin/orders/:id', element: <OrdersRoute /> },

  { path: '/admin/users', element: <UsersRoute /> },
  { path: '/admin/users/create', element: <UsersRoute /> },
  { path: '/admin/users/:id', element: <UsersRoute /> },
  { path: '/admin/users/:id/edit', element: <UsersRoute /> },

  { path: '/admin/roles', element: <RolesRoute /> },
  { path: '/admin/roles/:id', element: <RolesRoute /> },

  { path: '/admin/permissions', element: <PermissionsRoute /> },

  { path: '/admin/api-clients', element: <ApiClientsRoute /> },

  { path: '/admin/settings', element: <SettingsRoute /> },
  { path: '/admin/settings/:section', element: <SettingsRoute /> },
]
