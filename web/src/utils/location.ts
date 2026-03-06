const LOCATION_PRESETS: Record<string, [number, number]> = {
  aalborg: [57.048, 9.9187],
  aarhus: [56.164, 10.2032],
  'copenhagen central': [55.672, 12.5646],
  'copenhagen airport': [55.6124, 12.6477]
};

const DK_BOUNDS = {
  minLat: 54.5,
  maxLat: 57.9,
  minLon: 8.0,
  maxLon: 12.9
};

function hash(input: string): number {
  let h = 0;
  for (let i = 0; i < input.length; i += 1) {
    h = (h << 5) - h + input.charCodeAt(i);
    h |= 0;
  }
  return Math.abs(h);
}

function randomInRange(seed: number, min: number, max: number): number {
  const normalized = (seed % 100000) / 100000;
  return min + normalized * (max - min);
}

export function resolveLatLon(location: string | undefined, idSeed: string): [number, number] {
  if (!location) {
    const h = hash(idSeed);
    return [
      randomInRange(h, DK_BOUNDS.minLat, DK_BOUNDS.maxLat),
      randomInRange(h / 10, DK_BOUNDS.minLon, DK_BOUNDS.maxLon)
    ];
  }

  const match = location.match(/(-?\d{1,2}\.\d+)\s*,\s*(-?\d{1,3}\.\d+)/);
  if (match) {
    const lat = Number(match[1]);
    const lon = Number(match[2]);
    if (!Number.isNaN(lat) && !Number.isNaN(lon)) {
      return [lat, lon];
    }
  }

  const key = location.toLowerCase().trim();
  if (LOCATION_PRESETS[key]) {
    return LOCATION_PRESETS[key];
  }

  const h = hash(`${location}:${idSeed}`);
  return [
    randomInRange(h, DK_BOUNDS.minLat, DK_BOUNDS.maxLat),
    randomInRange(h / 10, DK_BOUNDS.minLon, DK_BOUNDS.maxLon)
  ];
}
