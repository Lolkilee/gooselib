import type { PageLoad } from './$types';
import { ResponseType, getClient } from "@tauri-apps/api/http";

function loadAppDef(appName: string): AppDefinition {
    let loadedApp: AppDefinition;
    const jString = sessionStorage.getItem("app-data");
    if (jString != null) {
        const lib: Library = JSON.parse(jString);
        loadedApp = lib.apps[0];

        for (let i = 0; i < lib.apps.length; i++) {
            const tmp = lib.apps[i];
            if (tmp.name == appName) {
                loadedApp = tmp;
            }
        }
    } else {
        loadedApp = new AppDefinition("Internal error", []);
    }

    return loadedApp;
}

async function loadAppInfo(appName: string, version: string): Promise<AppInfo> {
    const address = localStorage.getItem("saved-address");
    const client = await getClient();
    const reqHeaders: Record<string, any> = {
        pw: localStorage.getItem("server-password"),
    };
    const res = await client.get<AppInfo>(
        "http://" + address + ":" + 8765 + "/" + appName + "/" + version + ".app.json",
        {
            responseType: ResponseType.JSON,
            headers: reqHeaders,
        }
    );

    console.log(res);

    if (res.status == 200)
        return res.data;
    else
        return {exec: ""};
}

export const load: PageLoad = async ({ params }) => {
    let loadedApp: AppDefinition = loadAppDef(params.slug);
    let appInfo: AppInfo = await loadAppInfo(params.slug, loadedApp.versions[0]);
        
    return {
        app: loadedApp,
        selectedVersion: loadedApp.versions[0],
        info: appInfo,
    };
};