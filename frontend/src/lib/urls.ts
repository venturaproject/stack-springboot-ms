import { env } from "@/config"

export function assetUrl(path: string | undefined) {
  if (!path) return ""
  if (path.startsWith("http")) return path

  if (path.startsWith("/")) {
    path = path.slice(1)
  }

  return `${env.ASSET_URL}/${path}`
}

export function appUrl(
  path: string | Record<string, string | number | boolean | null> = "",
  params: Record<string, string | number | boolean | null> | undefined = undefined
) {
  if (typeof path === "object") {
    params = params ?? path
    path = ""
  }

  if (params) {
    const entries = Object.entries(params).filter(([, v]) => v !== null)
    const query = new URLSearchParams(entries.map(([k, v]) => [k, String(v)]))
    path = `${path}?${query.toString()}`
  }

  if (path.startsWith("/")) path = path.slice(1)

  return `${env.APP_URL}/${path}`.replace(/\/$/, "")
}
