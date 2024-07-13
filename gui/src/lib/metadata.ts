import { fetch, ResponseType } from '@tauri-apps/api/http';
import { BASE_URL } from './login';

export type MetaLibrary = MetaData[]

export interface MetaData {
    name: string
    latestVersion: string
    chunkCount: number
}

let latestLib: MetaLibrary | null = null;

export async function cacheMetaData() {
    const resp = await fetch(BASE_URL + 'meta');
    const data = resp.data as MetaLibrary;
    if (data != null) {
        latestLib = data;
    }
}

export function getMetaData() {
    return latestLib;
}