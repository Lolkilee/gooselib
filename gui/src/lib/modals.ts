import type { ModalSettings } from "@skeletonlabs/skeleton";
import { removeApp } from "./util";

export const loginModalSettings: ModalSettings = {
    type: 'component',
    component: 'loginModal'
}

export function triggerRemoveAppModal(path: string) {
    const removeAppModal: ModalSettings = {
        type: "confirm",
        title: "Please Confirm",
        body: "Are you sure you wish to remove the app?",
        // TRUE if confirm pressed, FALSE if cancel pressed
        response: (r: boolean) => {
            if (r) removeApp(path);
        },
    };
    return removeAppModal;
}