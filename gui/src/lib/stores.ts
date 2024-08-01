// src/lib/stores.ts
import { writable } from 'svelte/store';
import type { MetaData, MetaLibrary } from '$lib/metadata';
import type { DownloadInfo } from './download';

export const metaLibStore = writable<MetaLibrary>([]);
export const selectedMetaStore = writable<MetaData | null>(null);
export const downloadInfoStore = writable<DownloadInfo[]>([]);
export const consoleLineStore = writable<string[]>([]);
export const javaLineStore = writable<string[]>([]);