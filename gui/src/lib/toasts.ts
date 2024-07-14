import type { ToastSettings } from "@skeletonlabs/skeleton";

export const loginErrToast: ToastSettings = {
    message: 'Error during login',
    background: 'variant-filled-warning',
    hideDismiss: true,
    timeout: 2000
}

export const loginSuccToast: ToastSettings = {
    message: 'Successfully logged in',
    hideDismiss: true,
    timeout: 2000
}

export const loginCacheFail: ToastSettings = {
    message: 'Failed to login with saved data',
    background: 'variant-filled-warning',
    hideDismiss: true,
    timeout: 2000
}

export const logoutToast: ToastSettings = {
    message: 'Logged out from server',
    hideDismiss: true,
    timeout: 2000
}

export const downloadCompeleteToast: ToastSettings = {
    message: 'Download complete',
    hideDismiss: true,
    timeout: 2000
}

export const appRemovedToast: ToastSettings = {
    message: 'Successfully removed app',
    background: 'variant-filled-warning',
    hideDismiss: true,
    timeout: 2000
}