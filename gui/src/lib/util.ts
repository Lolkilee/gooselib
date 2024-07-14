import { getToastStore, type ToastStore } from "@skeletonlabs/skeleton";
import { invoke } from "@tauri-apps/api/tauri";
import { appRemovedToast } from "./toasts";
import { sendDownloadReq } from "./download";

export function formatBytes(bytes: any, decimals = 2) {
    if (!+bytes) return "0 Bytes";

    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = [
        "Bytes",
        "KB",
        "MB",
        "GB",
        "TB",
        "PB",
        "EB",
        "ZB",
        "YB",
    ];

    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]
        }`;
}

export async function removeApp(toatsStore: ToastStore, path: string) {
    await invoke("remove_dir", {
        dir: path
    });
    toatsStore.trigger(appRemovedToast);
}

export async function updateApp(toatsStore: ToastStore, path: string, name: string, version: string) {
    await invoke("remove_dir", {
        dir: path
    });
    toatsStore.trigger(appRemovedToast);
    await sendDownloadReq(name, version, path);
}