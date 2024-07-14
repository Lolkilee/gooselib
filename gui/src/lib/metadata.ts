import { fetch } from '@tauri-apps/api/http';
import { BASE_URL } from './login';

export type MetaLibrary = MetaData[]

export interface MetaData {
    name: string
    latestVersion: string
    chunkCount: number
    bytesCount: number
}

let latestLib: MetaLibrary | null = null;

export async function cacheMetaData() {
    const resp = await fetch(BASE_URL + 'meta');
    const data = resp.data as MetaLibrary;
    if (data != null) {
        latestLib = data;
        latestLib.forEach((val) => {
            sessionStorage.setItem(val.name + '-meta', JSON.stringify(val));
        })
    }
}

export function getMetaData() {
    return latestLib;
}

export function getAppMetaData(name: string): MetaData | null {
    const jStr = sessionStorage.getItem(name + '-meta')
    return jStr ? JSON.parse(jStr) : null;
}