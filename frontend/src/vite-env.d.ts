/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL: string
  readonly VITE_COMPANY_NIT?: string
  readonly VITE_COMPANY_OWNER?: string
  readonly VITE_COMPANY_CITY?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
