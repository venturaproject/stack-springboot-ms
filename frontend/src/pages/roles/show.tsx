import { useNavigate } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { AuthenticatedLayout } from '@/layouts'
import { Main } from '@/components/layout'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { RoleDto } from '@/services/roles-api'

export default function RoleShow({ role }: { role: RoleDto }) {
  const navigate = useNavigate()

  return (
    <AuthenticatedLayout>
      <Main>
        <div className="mb-6 flex items-center gap-3">
          <Button variant="outline" size="icon" onClick={() => navigate('/admin/roles')}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <h1 className="text-2xl font-semibold tracking-tight">{role.name}</h1>
        </div>

        <Card className="max-w-xl">
          <CardHeader><CardTitle>Permissions</CardTitle></CardHeader>
          <CardContent className="space-y-3">
            {role.description && <p className="text-sm text-muted-foreground">{role.description}</p>}
            <div className="flex flex-wrap gap-1">
              {role.permissions.length === 0
                ? <span className="text-sm text-muted-foreground">None</span>
                : role.permissions.map((p) => <Badge key={p} variant="outline">{p}</Badge>)}
            </div>
          </CardContent>
        </Card>
      </Main>
    </AuthenticatedLayout>
  )
}
