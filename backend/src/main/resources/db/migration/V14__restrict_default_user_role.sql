-- The default user is not an administrator or an organization-wide auditor.
DELETE FROM role_permissions rp
USING roles r, permissions p
WHERE rp.role_id = r.id
  AND rp.permission_id = p.id
  AND r.name = 'USER'
  AND p.name IN ('users:read', 'customers:read', 'orders:read', 'orders:write');
