import { create } from 'zustand';
import type { Anomaly, Filters } from '../types/anomaly';
import { DEFAULT_FILTERS } from '../utils/filters';

const BBOX_FILTER_ENABLED = (import.meta.env.VITE_FILTER_BY_BBOX || 'false').toLowerCase() === 'true';

interface UiState {
  filters: Filters;
  liveEnabled: boolean;
  selectedAnomaly: Anomaly | null;
  sidePanelOpen: boolean;
  setFilters: (next: Partial<Filters>) => void;
  setLiveEnabled: (enabled: boolean) => void;
  setSelectedAnomaly: (anomaly: Anomaly | null) => void;
  setSidePanelOpen: (open: boolean) => void;
}

export const useUiStore = create<UiState>((set) => ({
  filters: DEFAULT_FILTERS,
  liveEnabled: true,
  selectedAnomaly: null,
  sidePanelOpen: true,
  setFilters: (next) => {
    const normalizedNext = BBOX_FILTER_ENABLED ? next : { ...next, bbox: undefined };
    set((state) => ({
      filters: {
        ...state.filters,
        ...normalizedNext
      }
    }));
  },
  setLiveEnabled: (enabled) => set({ liveEnabled: enabled }),
  setSelectedAnomaly: (anomaly) => set({ selectedAnomaly: anomaly }),
  setSidePanelOpen: (open) => set({ sidePanelOpen: open })
}));
