import { fetch, ResponseType } from '@tauri-apps/api/http';
import { cacheMetaData } from './metadata';

export const BASE_URL = 'http://localhost:7123/';

async function setReq(param: string, paramVal: string) {
    const res = await fetch(BASE_URL + 'set-' + param + '/' + paramVal, {
        method: 'POST',
        responseType: ResponseType.Text,
    });
    console.log(res);
}

export async function tryHandshake(username: string,
    password: string, ip: string): Promise<boolean> {

    await setReq('user', username);
    await setReq('password', password);

    const handshakeResp = await fetch(BASE_URL + 'handshake/' + ip, {
        method: 'POST',
        responseType: ResponseType.JSON,
    })

    console.log(handshakeResp);

    //@ts-ignore
    if (handshakeResp.data.sessionKey) {
        localStorage.setItem('username', username);
        localStorage.setItem('password', password);
        localStorage.setItem('ip', ip);
        cacheMetaData();
        return true;
    }
    else {
        return false;
    }
}