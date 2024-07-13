// src/lib/stores.ts
import { writable } from 'svelte/store';
import type { MetaLibrary } from '$lib/metadata';

export const metaLibStore = writable<MetaLibrary>([]);