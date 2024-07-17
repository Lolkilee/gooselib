import { ResponseType, fetch } from '@tauri-apps/api/http';
import { BASE_URL } from "./login";

export interface DownloadInfo {
    appName: string
    writeProgress: number
    netwProgress: number
    speed: number
}

export async function getDownloadInfos(): Promise<DownloadInfo[]> {
    const resp = await fetch(BASE_URL + 'download-progress');
    const data = resp.data as DownloadInfo[]
    if (data != null) return data
    else return [];
}

export async function sendDownloadReq(name: string, version: string, path: string) {
    let convPath = path.replaceAll('/', '!');
    convPath = convPath.replaceAll('\\', '!');
    const url = BASE_URL + 'download/' + name + '/[' + convPath + ']';
    const resp = await fetch(url, {
        method: 'POST',
        responseType: ResponseType.Text
    })

    localStorage.setItem('installed-ver-' + name, version);
    console.log(resp.data);
}