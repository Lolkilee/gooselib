// src/lib/stores.ts
import { writable } from 'svelte/store';
import type { MetaData, MetaLibrary } from '$lib/metadata';

export const metaLibStore = writable<MetaLibrary>([]);
export const selectedMetaStore = writable<MetaData | null>(null);