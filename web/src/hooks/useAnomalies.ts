import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo } from 'react';
import { fetchAnomalies } from '../api/anomalies';
import { startAnomalyStream } from '../api/stream';
import { useUiStore } from '../store/uiStore';
import type { Filters } from '../types/anomaly';
const QUERY_KEY = ['anomalies'];
const BBOX_FILTER_ENABLED = (import.meta.env.VITE_FILTER_BY_BBOX || 'false').toLowerCase() === 'true';

export function useAnomalies() {
  const filters = useUiStore((state) => state.filters);
  const effectiveFilters = useMemo<Filters>(
    () => (BBOX_FILTER_ENABLED ? filters : { ...filters, bbox: undefined }),
    [filters]
  );

  return useQuery({
    queryKey: [...QUERY_KEY, effectiveFilters],
    queryFn: () => fetchAnomalies(effectiveFilters),
    placeholderData: keepPreviousData,
    staleTime: 15_000,
    refetchInterval: 30_000
  });
}

export function useLiveAnomalies() {
  const liveEnabled = useUiStore((state) => state.liveEnabled);
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!liveEnabled) {
      return undefined;
    }

    const stop = startAnomalyStream(() => {
      // Refetch filtered queries so type/date/status constraints stay authoritative.
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    });

    const pollTimer = window.setInterval(() => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    }, 30_000);

    return () => {
      stop();
      window.clearInterval(pollTimer);
    };
  }, [liveEnabled, queryClient]);
}
