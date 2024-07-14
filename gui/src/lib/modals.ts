import type { ModalSettings, ToastStore } from "@skeletonlabs/skeleton";
import { removeApp, updateApp } from "./util";

export const loginModalSettings: ModalSettings = {
    type: 'component',
    component: 'loginModal'
}

export function triggerRemoveAppModal(toastStore: ToastStore, path: string) {
    const removeAppModal: ModalSettings = {
        type: "confirm",
        title: "Please Confirm",
        body: "Are you sure you wish to remove the app?",
        // TRUE if confirm pressed, FALSE if cancel pressed
        response: (r: boolean) => {
            if (r) removeApp(toastStore, path);
        },
    };
    return removeAppModal;
}


export function triggerUpdateAppModal(toastStore: ToastStore, path: string, name: string, version: string) {
    const removeAppModal: ModalSettings = {
        type: "confirm",
        title: "Please Confirm",
        body: "Are you sure you wish to update the app?",
        // TRUE if confirm pressed, FALSE if cancel pressed
        response: (r: boolean) => {
            if (r) updateApp(toastStore, path, name, version);
        },
    };
    return removeAppModal;
}