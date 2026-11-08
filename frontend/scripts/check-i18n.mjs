import { readdir, readFile } from 'node:fs/promises'
import path from 'node:path'

const root = process.cwd()
const authRoot = path.join(root, 'src/pages/auth')
const localeRoot = path.join(root, 'src/i18n/locales')
const locales = Object.fromEntries(
  await Promise.all(
    ['en', 'es'].map(async (language) => [
      language,
      JSON.parse(await readFile(path.join(localeRoot, `${language}.json`), 'utf8')),
    ]),
  ),
)

async function sourceFiles(directory) {
  const files = []
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const entryPath = path.join(directory, entry.name)
    if (entry.isDirectory()) files.push(...(await sourceFiles(entryPath)))
    else if (/\.(tsx?|jsx?)$/.test(entry.name) && !/\.(test|spec)\./.test(entry.name)) files.push(entryPath)
  }
  return files
}

const issues = []
const usedKeys = new Set()
const translatableAttributes = new Set(['alt', 'aria-label', 'aria-description', 'placeholder', 'title'])
const hasWords = (text) => /[\p{L}\p{N}]/u.test(text)

for (const file of await sourceFiles(authRoot)) {
  const source = await readFile(file, 'utf8')
  const relativeFile = path.relative(root, file)

  for (const match of source.matchAll(/>([^<>{}]+)</g)) {
    const text = match[1].trim()
    if (hasWords(text)) {
      issues.push(`${relativeFile}: visible JSX text must use a translation key: ${JSON.stringify(text)}`)
    }
  }

  for (const attribute of translatableAttributes) {
    const pattern = new RegExp(`\\b${attribute}\\s*=\\s*([\"'])(.*?)\\1`, 'gs')
    for (const match of source.matchAll(pattern)) {
      if (hasWords(match[2])) {
        issues.push(`${relativeFile}: ${attribute} must use a translation key: ${JSON.stringify(match[2])}`)
      }
    }
  }

  for (const match of source.matchAll(/\bmessage\s*:\s*(["'])(.*?)\1/gs)) {
    if (hasWords(match[2])) {
      issues.push(`${relativeFile}: validation message must use a translation key: ${JSON.stringify(match[2])}`)
    }
  }

  for (const match of source.matchAll(/\bt\(\s*(["'])(.*?)\1/g)) usedKeys.add(match[2])
}

for (const key of usedKeys) {
  for (const language of ['en', 'es']) {
    if (typeof locales[language][key] !== 'string' || !locales[language][key].trim()) {
      issues.push(`src/i18n/locales/${language}.json: missing translation for auth key "${key}"`)
    }
  }
}

for (const prefix of ['sign_in_', 'forgot_password_']) {
  const keysByLocale = Object.fromEntries(
    ['en', 'es'].map((language) => [
      language,
      Object.keys(locales[language]).filter((key) => key.startsWith(prefix)).sort(),
    ]),
  )
  if (JSON.stringify(keysByLocale.en) !== JSON.stringify(keysByLocale.es)) {
    issues.push(`Auth translation keys with prefix "${prefix}" differ between en.json and es.json`)
  }
}

if (issues.length > 0) {
  console.error(issues.map((issue) => `- ${issue}`).join('\n'))
  process.exitCode = 1
} else {
  console.log(`i18n check passed: ${usedKeys.size} auth translation keys; no hardcoded auth UI text.`)
}
