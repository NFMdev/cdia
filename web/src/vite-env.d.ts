/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_USE_MOCK_API?: string;
  readonly VITE_ENABLE_HEATMAP?: string;
  readonly VITE_FILTER_BY_BBOX?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
